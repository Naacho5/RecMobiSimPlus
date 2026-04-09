package es.unizar.epidemic.data;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Real-time tracking manager for inter-clique coincidences
 * 
 * @author Nacho Palacio
 */
public class InterCliqueCoincidenceTracker {
    
    /////////////////////////////////////////////////////////////////////////////
    // DATA STRUCTURES
    ////////////////////////////////////////////////////////////////////////////
    
    // User to clique mapping 
    private final Map<Integer, Integer> userToClique;
    
    // Currently active coincidences
    private final List<CoincidenceRecord> activeCoincidences;
    
    // Complete history of finalized coincidences
    private final List<CoincidenceRecord> completedCoincidences;
    
    // Index for fast lookup: "user1-user2-room" -> active CoincidenceRecord
    private final Map<String, CoincidenceRecord> coincidenceMap;
    
    // Simulation parameters
    private final double secondsPerIteration;

    
    /**
     * Constructor that initializes the tracker with clique mapping
     * 
     * @param cliqueUserMapping Clique ID -> List of user IDs mapping
     */
    public InterCliqueCoincidenceTracker(Map<Integer, List<Integer>> cliqueUserMapping) {
        this.userToClique = buildUserToCliqueMap(cliqueUserMapping);
        this.activeCoincidences = new ArrayList<>();
        this.completedCoincidences = new ArrayList<>();
        this.coincidenceMap = new HashMap<>();
        
        // Get seconds per iteration from Configuration
        if (es.unizar.gui.Configuration.simulation != null) {
            this.secondsPerIteration = es.unizar.gui.Configuration.simulation.getTimeForIterationInSecond();
        } else {
            this.secondsPerIteration = 1.0; // Fallback
        }
        
        System.out.println(" InterCliqueCoincidenceTracker initialized:");
        System.out.println("   - Mapped users: " + userToClique.size());
        System.out.println("   - Unique cliques: " + 
                         userToClique.values().stream().distinct().count());
        System.out.println("   - Seconds per iteration: " + secondsPerIteration);
    }
    
    /**
     * Builds inverted mapping from user ID to clique ID.
     * 
     * @param cliqueUserMapping mapping from clique ID to list of user IDs
     * @return map from user ID to their assigned clique ID
     */
    private Map<Integer, Integer> buildUserToCliqueMap(Map<Integer, List<Integer>> cliqueUserMapping) {
        Map<Integer, Integer> map = new HashMap<>();
        
        for (Map.Entry<Integer, List<Integer>> entry : cliqueUserMapping.entrySet()) {
            int cliqueId = entry.getKey();
            for (int userId : entry.getValue()) {
                map.put(userId, cliqueId);
            }
        }
        
        return map;
    }
    
    /**
     * Updates coincidences for the current iteration.
     * Groups users by room, detects new coincidences, and closes inactive ones.
     * 
     * @param currentIteration the current simulation iteration number
     */
    public void updateCoincidences(int currentIteration) {
        // System.out.println(" updateCoincidences() called at iteration " + currentIteration);

        // Group users by room
        Map<Integer, List<es.unizar.gui.simulation.User>> usersByRoom = groupUsersByRoom();
        
        // Detect coincidences in each room
        for (Map.Entry<Integer, List<es.unizar.gui.simulation.User>> entry : usersByRoom.entrySet()) {
            int roomId = entry.getKey();
            List<es.unizar.gui.simulation.User> usersInRoom = entry.getValue();

            // Compare each pair of users in the room
            for (int i = 0; i < usersInRoom.size(); i++) {
                for (int j = i + 1; j < usersInRoom.size(); j++) {
                    es.unizar.gui.simulation.User user1 = usersInRoom.get(i);
                    es.unizar.gui.simulation.User user2 = usersInRoom.get(j);

                    checkAndRecordCoincidence(
                        user1.userID, 
                        user2.userID, 
                        roomId,
                        currentIteration
                    );
                }
            }
        }
        
        closeInactiveCoincidences(currentIteration, usersByRoom);
    }
    
    /**
     * Groups users by room for efficient comparison.
     * Excludes users who have finished their visit.
     * 
     * @return map from room ID to list of users currently in that room
     */
    private Map<Integer, List<es.unizar.gui.simulation.User>> groupUsersByRoom() {
        Map<Integer, List<es.unizar.gui.simulation.User>> usersByRoom = new HashMap<>();
        
        if (es.unizar.gui.Configuration.simulation == null) {
            System.err.println("    Configuration.simulation is NULL");
            return usersByRoom;
        }
        
        for (es.unizar.gui.simulation.User user : es.unizar.gui.Configuration.simulation.userList) {   
            if (user.hasFinishedVisit) {
                continue;
            }
            user.getRoomOfTheUser();
            if (user.room >= 0) {
                usersByRoom.computeIfAbsent(user.room, k -> new ArrayList<>()).add(user);
            }
        }
        
        return usersByRoom;
    }
    
    /**
     * Checks and records a coincidence between two users.
     * If the coincidence already exists, updates its duration; if new, creates it.
     * Only records coincidences between users from different cliques.
     * 
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @param roomId room where the users coincide
     * @param currentIteration current simulation iteration
     */
    private void checkAndRecordCoincidence(int userId1, int userId2, 
                                          int roomId, int currentIteration) {

        // Verify they are from different cliques
        Integer clique1 = userToClique.get(userId1);
        Integer clique2 = userToClique.get(userId2);
        
        if (clique1 == null) {
            System.err.println("      Warning! User " + userId1 + " without assigned clique");
            return;
        }
        
        if (clique2 == null) {
            System.err.println("      Warning! User " + userId2 + " without assigned clique");
            return;
        }

        // Normalize order (user1 < user2)
        int user1 = Math.min(userId1, userId2);
        int user2 = Math.max(userId1, userId2);
        
        // Create unique key
        String key = user1 + "-" + user2 + "-" + roomId;
        
        // Check if an active coincidence already exists
        CoincidenceRecord existing = coincidenceMap.get(key);
        
        if (existing != null) {
            // Update existing coincidence
            existing.updateEndIteration(currentIteration);
        } else {
            // Create new coincidence
            CoincidenceRecord newRecord = new CoincidenceRecord(
                user1, user2, clique1, clique2, roomId, currentIteration
            );
            
            activeCoincidences.add(newRecord);
            coincidenceMap.put(key, newRecord);
        }
    }
    
    /**
     * Closes coincidences that are no longer active.
     * Detects when users have left the room and finalizes their coincidence records.
     * 
     * @param currentIteration current simulation iteration
     * @param usersByRoom users grouped by room ID
     */
    private void closeInactiveCoincidences(int currentIteration, 
                                          Map<Integer, List<es.unizar.gui.simulation.User>> usersByRoom) {
        
        List<CoincidenceRecord> toClose = new ArrayList<>();
        
        for (CoincidenceRecord record : activeCoincidences) {
            // Verify if users are still in the room
            List<es.unizar.gui.simulation.User> usersInRoom = 
                usersByRoom.getOrDefault(record.getRoom(), Collections.emptyList());
            
            boolean user1Present = usersInRoom.stream()
                .anyMatch(u -> u.userID == record.getUser1());
            boolean user2Present = usersInRoom.stream()
                .anyMatch(u -> u.userID == record.getUser2());
            
            if (!user1Present || !user2Present) {
                // At least one of the users left the room
                record.close(currentIteration);
                toClose.add(record);
            }
        }
        
        for (CoincidenceRecord record : toClose) {
            activeCoincidences.remove(record);
            completedCoincidences.add(record);
            coincidenceMap.remove(record.getKey());
        }
    }
    
    /**
     * Closes all active coincidences.
     * Should be called at the end of the simulation to finalize all records.
     * 
     * @param finalIteration the final iteration of the simulation
     */
    public void closeAllActiveCoincidences(int finalIteration) {
        for (CoincidenceRecord record : activeCoincidences) {
            record.close(finalIteration);
            completedCoincidences.add(record);
        }
        
        activeCoincidences.clear();
        coincidenceMap.clear();
        
        System.out.println(" All coincidences closed:");
        System.out.println("   - Total coincidences recorded: " + completedCoincidences.size());
    }

    /**
     * Prints inter-clique isolation metrics.
     * Displays statistics about coincidences between different cliques,
     * including isolation rate and users involved.
     */
    public void printIsolationMetrics() {
        System.out.println("\n INTER-CLIQUE ISOLATION METRICS:");
        
        List<CoincidenceRecord> interCliqueCoincidences = completedCoincidences.stream()
        .filter(r -> r.getClique1() != r.getClique2())
        .collect(Collectors.toList());
    
        if (interCliqueCoincidences.isEmpty()) {
            System.out.println("    No inter-clique coincidences detected");
            System.out.println("    Perfect isolation: 100%");
            return;
        }
        
        // Calculate basic metrics
        int totalCoincidences = interCliqueCoincidences.size();
        double totalSimulationTime = getTotalSimulationTime();
        
        // Calculate total coincidence time
        double totalCoincidenceTime = interCliqueCoincidences.stream()
            .mapToDouble(r -> r.getDurationSeconds(secondsPerIteration))
            .sum();
        
        // Calculate users involved
        Set<Integer> usersInvolved = new HashSet<>();
        for (CoincidenceRecord record : interCliqueCoincidences) {
            usersInvolved.add(record.getUser1());
            usersInvolved.add(record.getUser2());
        }
        
        // Calculate isolation rate
        // Isolation = 1 - (coincidence time / total available time)
        double totalUserTime = totalSimulationTime * userToClique.size();
        double isolationRate = 1.0 - (totalCoincidenceTime / totalUserTime);
        
        // Print metrics
        System.out.println("    General statistics:");
        System.out.println("      - Total coincidences: " + totalCoincidences);
        System.out.println("      - Users involved: " + usersInvolved.size() + "/" + userToClique.size());
        System.out.println("      - Total coincidence time: " + String.format("%.1f", totalCoincidenceTime) + " sec");
        System.out.println("      - Average time per coincidence: " + 
                        String.format("%.1f", totalCoincidenceTime / totalCoincidences) + " sec");
        
        System.out.println("\n    Isolation rate:");
        System.out.printf("      - Effective isolation: %.2f%%\n", isolationRate * 100);
        
        if (isolationRate >= 0.95) {
            System.out.println("      ✅ Success criterion met (≥95%)");
        } else {
            System.out.println("      Warning! Warning: Isolation < 95%");
        }
        
        // Statistics by clique
        Map<Integer, Integer> coincidencesByClique = new HashMap<>();
        for (CoincidenceRecord record : interCliqueCoincidences) {
            coincidencesByClique.merge(record.getClique1(), 1, Integer::sum);
            coincidencesByClique.merge(record.getClique2(), 1, Integer::sum);
        }
        
        if (!coincidencesByClique.isEmpty()) {
            System.out.println("\n    Top 5 cliques with most coincidences:");
            coincidencesByClique.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    System.out.printf("      - Clique %d: %d coincidences\n", 
                                entry.getKey() + 1, entry.getValue());
                });
        }
    }

    /**
     * Prints detailed coincidence information for each user.
     * Shows total coincidence time with each other user,
     * distinguishing between users from the same clique and different cliques.
     */
    public void printDetailedUserCoincidences() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" INDIVIDUAL COINCIDENCES PER USER");
        System.out.println("=".repeat(80));

        // Get list of all simulated users
        Set<Integer> allUserIds = userToClique.keySet();

        // Preprocess: for each user pair, sum total coincidence time
        // Map<userIdA, Map<userIdB, coincidenceTime>>
        Map<Integer, Map<Integer, Double>> coincidenceMatrix = new HashMap<>();
        for (int userA : allUserIds) {
            coincidenceMatrix.put(userA, new HashMap<>());
        }

        // Iterate all recorded coincidences (including intra-clique and inter-clique)
        for (CoincidenceRecord record : completedCoincidences) {
            int u1 = record.getUser1();
            int u2 = record.getUser2();
            double duration = record.getDurationSeconds(secondsPerIteration);

            // Sum time for both directions
            coincidenceMatrix.get(u1).merge(u2, duration, Double::sum);
            coincidenceMatrix.get(u2).merge(u1, duration, Double::sum);
        }

        // Print for each user
        for (int userId : allUserIds) {
            int myClique = userToClique.get(userId);
            System.out.println("\nUser " + userId + " (Clique " + myClique + "):");

            // Group users by clique
            Map<Integer, List<Integer>> cliqueToUsers = new HashMap<>();
            for (int otherId : allUserIds) {
                int clique = userToClique.get(otherId);
                cliqueToUsers.computeIfAbsent(clique, k -> new ArrayList<>()).add(otherId);
            }

            // Iterate clique by clique
            for (Map.Entry<Integer, List<Integer>> cliqueEntry : cliqueToUsers.entrySet()) {
                int clique = cliqueEntry.getKey();
                List<Integer> usersInClique = cliqueEntry.getValue();

                String cliqueLabel = (clique == myClique) ? "🟩 (Own)" : "🟥 (Other)";
                System.out.println("  Clique " + clique + " " + cliqueLabel + ":");

                for (int otherId : usersInClique) {
                    if (otherId == userId) continue; // Don't show coincidence with self

                    double time = coincidenceMatrix.get(userId).getOrDefault(otherId, 0.0);
                    if (time > 0.0) {
                        System.out.printf("    - User %d: %.1f seconds\n", otherId, time);
                    } else {
                        System.out.printf("    - User %d: 0 seconds\n", otherId);
                    }
                }
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("End of individual coincidences report.");
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Prints attack rates by clique.
     * Displays infection statistics for each clique including susceptible users,
     * new infections, and attack rate percentages.
     * 
     * @param userList list of all users in the simulation
     * @param initialSusceptiblesByClique map from clique ID to initial susceptible count
     * @param cliqueUserMapping map from clique ID to list of user IDs in that clique
     */
    public void printAttackRatesByClique(
            List<es.unizar.gui.simulation.User> userList,
            Map<Integer, Integer> initialSusceptiblesByClique,
            Map<Integer, List<Integer>> cliqueUserMapping) {
            
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ATTACK RATES BY CLIQUE");
        System.out.println("=".repeat(80));
        
        if (cliqueUserMapping == null || cliqueUserMapping.isEmpty()) {
            System.out.println(" No clique data available");
            return;
        }
        
        // CALCULATE AND DISPLAY FOR EACH CLIQUE
        int cliquesWithInfections = 0;
        double totalAttackRate = 0.0;
        
        System.out.println("\n┌────────┬─────────┬────────┬──────────┬───────────┬──────────────┐");
        System.out.println("│ Clique │ Users   │ Immune │ Suscept. │ Infected  │ Attack Rate  │");
        System.out.println("│        │ Total   │ Init.  │ Initial  │ New       │              │");
        System.out.println("├────────┼─────────┼────────┼──────────┼───────────┼──────────────┤");
        
        for (Map.Entry<Integer, List<Integer>> entry : cliqueUserMapping.entrySet()) {
            int cliqueId = entry.getKey() + 1;  // +1 para display
            List<Integer> userIds = entry.getValue();
            
            int initialSusceptibles = initialSusceptiblesByClique.getOrDefault(entry.getKey(), 0);
            if (initialSusceptibles == 0) continue;
            
            int newInfections = 0;
            int immuneUsers = 0;
            
            for (int userId : userIds) {
                es.unizar.gui.simulation.User user = userList.stream()
                    .filter(u -> u.userID == userId)
                    .findFirst()
                    .orElse(null);
                
                if (user != null && user.getEpidemicExtension() != null) {
                    es.unizar.epidemic.general.UserEpidemicExtension ext = user.getEpidemicExtension();
                    
                    if (ext.isImmune()) {
                        immuneUsers++;
                    } else if (ext.getHealthStatus() == es.unizar.epidemic.general.HealthStatus.SUSCEPTIBLE) {
                    } else if (ext.getHealthStatus() == es.unizar.epidemic.general.HealthStatus.INFECTIOUS_SYMPTOMATIC ||
                            ext.getHealthStatus() == es.unizar.epidemic.general.HealthStatus.SUPER_SPREADER) {
                        newInfections++;
                    }
                }
            }
            
            double attackRate = (newInfections * 100.0) / initialSusceptibles;
            
            System.out.printf("│ %6d │ %7d │ %6d │ %8d │ %9d │ %11.1f%% │\n",
                            cliqueId, userIds.size(), immuneUsers, 
                            initialSusceptibles, newInfections, attackRate);
            
            if (newInfections > 0) cliquesWithInfections++;
            totalAttackRate += attackRate;
        }
        
        System.out.println("└────────┴─────────┴────────┴──────────┴───────────┴──────────────┘");
        
        int totalCliques = cliqueUserMapping.size();
        double avgAttackRate = totalCliques > 0 ? totalAttackRate / totalCliques : 0.0;
        
        System.out.println("\n SUMMARY:");
        System.out.printf("   - Total cliques: %d\n", totalCliques);
        System.out.printf("   - Cliques with infections: %d (%.1f%%)\n",
                        cliquesWithInfections, (cliquesWithInfections * 100.0) / totalCliques);
        System.out.printf("   - Average attack rate: %.1f%%\n", avgAttackRate);
        
        System.out.println("\n" + "=".repeat(80) + "\n");
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    // METRICS AND STATISTICS
    ////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Calculates coincidence metrics for a specific user.
     * 
     * @param userId the user ID to calculate metrics for
     * @return map containing metrics: totalCoincidenceTime, percentageOfSimulationTime,
     *         distinctUsersCoincided, distinctRoomsCoincided, and coincidences list
     */
    public Map<String, Object> getUserMetrics(int userId) {
        Map<String, Object> metrics = new HashMap<>();
        
        List<CoincidenceRecord> userCoincidences = completedCoincidences.stream()
            .filter(r -> r.involvesUser(userId))
            .collect(Collectors.toList());

        System.out.println("    User " + userId + ": " + userCoincidences.size() + " coincidences");
        
        // Total coincidence time
        double totalTime = userCoincidences.stream()
            .mapToDouble(r -> r.getDurationSeconds(secondsPerIteration))
            .sum();
        
        System.out.println("       Total time: " + totalTime + " sec");
        
        // Percentage of total simulation time
        double totalSimulationTime = getTotalSimulationTime();
        double percentage = (totalTime / totalSimulationTime) * 100.0;
        
        // Unique users coincided with
        Set<Integer> distinctUsers = userCoincidences.stream()
            .flatMap(r -> {
                List<Integer> users = new ArrayList<>();
                if (r.getUser1() == userId) users.add(r.getUser2());
                else users.add(r.getUser1());
                return users.stream();
            })
            .collect(Collectors.toSet());
        
        // Rooms where coincidences occurred
        Set<Integer> distinctRooms = userCoincidences.stream()
            .map(CoincidenceRecord::getRoom)
            .collect(Collectors.toSet());
        
        metrics.put("totalCoincidenceTime", totalTime);
        metrics.put("percentageOfSimulationTime", percentage);
        metrics.put("distinctUsersCoincided", distinctUsers.size());
        metrics.put("distinctRoomsCoincided", distinctRooms.size());
        metrics.put("coincidences", userCoincidences);
        
        return metrics;
    }
    
    /**
     * Calculates global coincidence metrics.
     * 
     * @return map containing global metrics: totalCoincidences, averageCoincidenceTime,
     *         maxCoincidenceTime, usersWithCoincidences, and averagePercentage
     */
    public Map<String, Object> getGlobalMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Total coincidences
        int totalCoincidences = completedCoincidences.size();
        
        // Average duration
        double avgDuration = completedCoincidences.stream()
            .mapToDouble(r -> r.getDurationSeconds(secondsPerIteration))
            .average()
            .orElse(0.0);
        
        // Maximum duration
        double maxDuration = completedCoincidences.stream()
            .mapToDouble(r -> r.getDurationSeconds(secondsPerIteration))
            .max()
            .orElse(0.0);
        
        // Users with at least one coincidence
        Set<Integer> usersWithCoincidences = completedCoincidences.stream()
            .flatMap(r -> Arrays.asList(r.getUser1(), r.getUser2()).stream())
            .collect(Collectors.toSet());
        
        // Average percentage of time
        double totalSimulationTime = getTotalSimulationTime();
        double avgPercentage = completedCoincidences.stream()
            .mapToDouble(r -> (r.getDurationSeconds(secondsPerIteration) / totalSimulationTime) * 100.0)
            .average()
            .orElse(0.0);

        double isolationRate = calculateIsolationRate();
        
        metrics.put("totalCoincidences", totalCoincidences);
        metrics.put("averageCoincidenceTime", avgDuration);
        metrics.put("maxCoincidenceTime", maxDuration);
        metrics.put("usersWithCoincidences", usersWithCoincidences.size());
        metrics.put("averagePercentage", avgPercentage);
        metrics.put("isolationRate", isolationRate);
        
        return metrics;
    }

    /**
    * Calculates the isolation rate based on inter-clique coincidences.
    * Isolation rate = 1 - (total inter-clique coincidence time / total available user time)
    * 
    * @return isolation rate as a value between 0.0 and 1.0
    */
    // private double calculateIsolationRate() {
    //     // Filter only inter-clique coincidences
    //     List<CoincidenceRecord> interCliqueCoincidences = completedCoincidences.stream()
    //         .filter(r -> r.getClique1() != r.getClique2())
    //         .collect(Collectors.toList());
        
    //     if (interCliqueCoincidences.isEmpty()) {
    //         return 1.0; // Perfect isolation
    //     }
        
    //     // Calculate total coincidence time
    //     double totalCoincidenceTime = interCliqueCoincidences.stream()
    //         .mapToDouble(r -> r.getDurationSeconds(secondsPerIteration))
    //         .sum();
        
    //     // Calculate total available time
    //     double totalSimulationTime = getTotalSimulationTime();
    //     double totalUserTime = totalSimulationTime * userToClique.size();
        
    //     // Calculate isolation rate
    //     return 1.0 - (totalCoincidenceTime / totalUserTime);
    // }

    /**
     * Calculates the isolation rate based on inter-clique coincidences.
     * Isolation rate = 1 - (total time users spent in inter-clique coincidences / total available user time)
     * 
     * @return isolation rate as a value between 0.0 and 1.0
     */
    private double calculateIsolationRate() {
        // Filter only inter-clique coincidences
        List<CoincidenceRecord> interCliqueCoincidences = completedCoincidences.stream()
            .filter(r -> r.getClique1() != r.getClique2())
            .collect(Collectors.toList());
        
        if (interCliqueCoincidences.isEmpty()) {
            return 1.0; // Perfect isolation
        }
        
        // ✅ CORRECCIÓN: Contar tiempo de coincidencia POR USUARIO
        Map<Integer, Double> userCoincidenceTime = new HashMap<>();
        
        for (CoincidenceRecord record : interCliqueCoincidences) {
            double duration = record.getDurationSeconds(secondsPerIteration);
            
            // Sumar tiempo para AMBOS usuarios involucrados
            userCoincidenceTime.merge(record.getUser1(), duration, Double::sum);
            userCoincidenceTime.merge(record.getUser2(), duration, Double::sum);
        }
        
        // ✅ CORRECCIÓN: Sumar el tiempo de coincidencia de TODOS los usuarios
        double totalCoincidenceTime = userCoincidenceTime.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        // Calculate total available time
        double totalSimulationTime = getTotalSimulationTime();
        double totalUserTime = totalSimulationTime * userToClique.size();
        
        // Calculate isolation rate
        double isolationRate = 1.0 - (totalCoincidenceTime / totalUserTime);
        
        // ✅ DEBUG: Verificar que la tasa está en el rango válido
        if (isolationRate < 0.0 || isolationRate > 1.0) {
            System.err.println("⚠️  WARNING: Isolation rate out of range: " + isolationRate);
            System.err.println("    Total coincidence time: " + totalCoincidenceTime + "s");
            System.err.println("    Total user time: " + totalUserTime + "s");
            System.err.println("    Users: " + userToClique.size());
            System.err.println("    Simulation time: " + totalSimulationTime + "s");
            
            // Clamp to valid range
            isolationRate = Math.max(0.0, Math.min(1.0, isolationRate));
        }
        
        return isolationRate;
    }
    
    /**
     * Calculates coincidence metrics for a specific clique.
     * 
     * @param cliqueId the clique ID to calculate metrics for
     * @return map containing clique metrics: coincidencesWithOtherCliques,
     *         totalInterCliqueTime, averagePerUserInClique, and usersInClique
     */
    public Map<String, Object> getCliqueMetrics(int cliqueId) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Filter coincidences involving the clique
        List<CoincidenceRecord> cliqueCoincidences = completedCoincidences.stream()
            .filter(r -> r.involvesClique(cliqueId))
            .collect(Collectors.toList());
        
        // Total time with other cliques (aggregated by clique)
        Map<Integer, Double> coincidencesWithOtherCliques = new HashMap<>();
        
        for (CoincidenceRecord record : cliqueCoincidences) {
            int otherClique = (record.getClique1() == cliqueId) ? 
                             record.getClique2() : record.getClique1();
            
            double duration = record.getDurationSeconds(secondsPerIteration);
            coincidencesWithOtherCliques.merge(otherClique, duration, Double::sum);
        }
        
        // Total inter-clique time
        double totalInterCliqueTime = coincidencesWithOtherCliques.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        // Number of users in this clique
        long usersInClique = userToClique.values().stream()
            .filter(c -> c == cliqueId)
            .count();
        
        // Average per user
        double averagePerUserInClique = (usersInClique > 0) ? 
            (totalInterCliqueTime / usersInClique) : 0.0;
        
        metrics.put("coincidencesWithOtherCliques", coincidencesWithOtherCliques);
        metrics.put("totalInterCliqueTime", totalInterCliqueTime);
        metrics.put("averagePerUserInClique", averagePerUserInClique);
        metrics.put("usersInClique", usersInClique);
        
        return metrics;
    }
    
    /**
     * Gets the total simulation time in seconds.
     * Retrieves from Configuration or returns a default value.
     * 
     * @return total simulation time in seconds
     */
    private double getTotalSimulationTime() {
        if (es.unizar.gui.Configuration.simulation != null) {
            return es.unizar.gui.Configuration.simulation.getTimeAvailableUserInSecond();
        }
        return 3600.0; // Fallback: 1 hour
    }
    
    ///////////////////////////////////////////////////////////////////////////////
    // EXPORT
    ///////////////////////////////////////////////////////////////////////////////
    
    /**
     * Exports coincidences to a CSV file.
     * 
     * @param filepath path to the output CSV file
     * @throws IOException if there is an error writing the file
     */
    public void exportToCSV(String filepath) throws IOException {
        try (FileWriter writer = new FileWriter(filepath)) {
            // Write header
            writer.write(CoincidenceRecord.csvHeader() + "\n");
            
            // Write each coincidence
            for (CoincidenceRecord record : completedCoincidences) {
                writer.write(record.toCSV(secondsPerIteration) + "\n");
            }
            
            System.out.println(" Coincidences exported to: " + filepath);
            System.out.println("   - Exported records: " + completedCoincidences.size());
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    // GETTERS
    ////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Gets a copy of the list of active coincidences.
     * 
     * @return list of currently active coincidence records
     */
    public List<CoincidenceRecord> getActiveCoincidences() {
        return new ArrayList<>(activeCoincidences);
    }
    
    /**
     * Gets a copy of the list of completed coincidences.
     * 
     * @return list of all completed coincidence records
     */
    public List<CoincidenceRecord> getCompletedCoincidences() {
        return new ArrayList<>(completedCoincidences);
    }
    
    /**
     * Gets the total number of completed coincidences.
     * 
     * @return count of completed coincidence records
     */
    public int getTotalCoincidences() {
        return completedCoincidences.size();
    }
}