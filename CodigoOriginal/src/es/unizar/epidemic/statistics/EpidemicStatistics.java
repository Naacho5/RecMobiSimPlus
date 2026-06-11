package es.unizar.epidemic.statistics;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Singleton class to collect and manage epidemic simulation statistics
 * Adapted to store exposure iterations on disk for memory efficiency
 * 
 * @author Nacho Palacio
 */
public class EpidemicStatistics {

    private int totalUsers = 0;
    private int initialInfected = 0;
    private int totalInfections = 0;
    private int totalRecoveries = 0;
    private long simulationStartTime;
    private long simulationEndTime;

    private double totalExposureTimeSeconds = 0.0;
    private double integratedConcentration = 0.0; 
    

    private Map<String, Object> modelSpecificStats = new HashMap<>();
    
    // SimpleProximityModel
    private int totalContacts = 0;
    private int infectiousContacts = 0;
    private double averageContactDuration = 0.0;
    
    // Rooms
    private Map<Integer, RoomStatistics> roomStats = new HashMap<>();
    
    // States
    private List<HealthStateSnapshot> stateHistory = new ArrayList<>();
    
    private static EpidemicStatistics instance;

    private EpidemicStatisticsCSVWriter csvWriter;
    
    private EpidemicStatistics() {
        csvWriter = new EpidemicStatisticsCSVWriter();
    }

    /**
     * Gets the singleton instance of EpidemicStatistics.
     * Creates a new instance if one does not already exist.
     * 
     * @return the singleton instance
     */
    public static EpidemicStatistics getInstance() {
        if (instance == null) {
            instance = new EpidemicStatistics();
        }
        return instance;
    }
    
    /**
     * Starts the simulation and initializes statistics.
     * Records the total number of users, initial infected count,
     * and simulation start time.
     * 
     * @param totalUsers total number of users in the simulation
     * @param initialInfected number of initially infected users
     */
    public void startSimulation(int totalUsers, int initialInfected) {
        this.totalUsers = totalUsers;
        this.initialInfected = initialInfected;
        this.simulationStartTime = System.currentTimeMillis();
    }
    
    /**
     * Ends the simulation and finalizes statistics.
     * Records the simulation end time and closes the CSV writer.
     */
    public void endSimulation() {
        this.simulationEndTime = System.currentTimeMillis();
        if (csvWriter != null) {
            // System.out.println("\n🔒 Cerrando CSV writer de EpidemicStatistics...");
            csvWriter.close();
        }
    }

    /**
     * Resets all statistics for a new simulation.
     * Clears all counters, maps, and collections and reinitializes
     * the CSV writer.
     * Added by Nacho Palacio 2025-10-11
     */
    public void reset() {
        this.totalUsers = 0;
        this.initialInfected = 0;
        this.totalInfections = 0;
        this.totalRecoveries = 0;
        
        this.simulationStartTime = 0;
        this.simulationEndTime = 0;

        this.totalExposureTimeSeconds = 0.0;
        this.integratedConcentration = 0.0;
        
        this.totalContacts = 0;
        this.infectiousContacts = 0;
        this.averageContactDuration = 0.0;
        
        if (this.roomStats != null) {
            this.roomStats.clear();
        }
        
        if (this.stateHistory != null) {
            this.stateHistory.clear();
        }
        
        if (this.modelSpecificStats != null) {
            this.modelSpecificStats.clear();
        }
        
        csvWriter = new EpidemicStatisticsCSVWriter();
    }

    /**
     * Resets the singleton instance.
     * Calls reset() on the current instance if it exists.
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.reset();
        }
    }
    
    /**
     * Records a new infection event.
     * Increments the total infections counter.
     * 
     * @param userId the ID of the newly infected user
     * @param transmissionSource description of the transmission source
     */
    public void recordInfection(int userId, String transmissionSource) {
        totalInfections++;
    }
    
    /**
     * Records a recovery event.
     * Increments the total recoveries counter.
     * 
     * @param userId the ID of the recovered user
     */
    public void recordRecovery(int userId) {
        totalRecoveries++;
    }
    
    /**
     * Records a contact event between two users.
     * Updates total contacts, average contact duration, and room-specific
     * contact statistics.
     * 
     * @param user1 the ID of the first user
     * @param user2 the ID of the second user
     * @param duration contact duration in seconds
     * @param roomId the ID of the room where contact occurred
     */
    public void recordContact(int user1, int user2, double duration, int roomId) {
        totalContacts++;
        averageContactDuration = ((averageContactDuration * (totalContacts - 1)) + duration) / totalContacts;
        
        RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
        roomStat.addContact();
    }

    /**
     * Registra un contacto
     * Without disk storage (original in-memory version)
     */
    // public void recordContact(int user1, int user2, double duration, int roomId) {
    //     totalContacts++;
    //     averageContactDuration = ((averageContactDuration * (totalContacts - 1)) + duration) / totalContacts;
        
    //     RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
    //     roomStat.addContact();
        
    //     // Escribir a CSV
    //     csvWriter.recordContact(System.currentTimeMillis(), user1, user2, duration, roomId, false);
    // }
    
    /**
     * Records an infectious contact event.
     * Increments the count of contacts where at least one user was infectious.
     * 
     * @param infectiousUser the ID of the infectious user
     * @param susceptibleUser the ID of the susceptible user
     */
    public void recordInfectiousContact(int infectiousUser, int susceptibleUser) {
        infectiousContacts++;
    }
    
    // Without disk storage (original in-memory version)
    // public void recordRoomAerosolConcentration(int roomId, double concentration) {
    //     // System.out.printf("[EpidemicStatistics] Registrando concentración en habitación %d: %.6f\n", roomId, concentration);
    //     RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
    //     roomStat.addAerosolMeasurement(concentration);
    // }

    /**
     * Records aerosol concentration without duration weighting.
     * Delegates to the overloaded method with a default duration of 1.0 second.
     * 
     * @param roomId the ID of the room
     * @param concentration the aerosol concentration value
     */
    public void recordRoomAerosolConcentration(int roomId, double concentration) {
        recordRoomAerosolConcentration(roomId, concentration, 1.0);
    }

    /**
     * Records aerosol concentration with duration weighting.
     * Updates room-specific statistics and calculates integrated concentration
     * for time-weighted averaging.
     * 
     * @param roomId the ID of the room
     * @param concentration the aerosol concentration value
     * @param durationSeconds duration of exposure in seconds
     */
    public void recordRoomAerosolConcentration(int roomId, double concentration, double durationSeconds) {
        RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
        roomStat.addAerosolMeasurement(concentration);
        
        integratedConcentration += concentration * durationSeconds;
        totalExposureTimeSeconds += durationSeconds;
    }

    // Without disk storage (original in-memory version)
    // public void recordRoomAerosolConcentration(int roomId, double concentration, double durationSeconds) {
    //     // Mantener en memoria para compatibilidad y cálculos rápidos
    //     RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
    //     roomStat.addAerosolMeasurement(concentration);
        
    //     csvWriter.recordAerosolMeasurement(simulationStartTime + System.currentTimeMillis(), roomId, concentration, durationSeconds);
        
    //     integratedConcentration += concentration * durationSeconds;
    //     totalExposureTimeSeconds += durationSeconds;
    // }

    /**
     * Records a health state snapshot.
     * Captures the current distribution of users across health states
     * and adds it to the state history.
     * 
     * @param susceptible number of susceptible users
     * @param infectious number of infectious users
     * @param recovered number of recovered users
     */
    public void recordHealthStateSnapshot(int susceptible, int infectious, int recovered) {
        stateHistory.add(new HealthStateSnapshot(susceptible, infectious, recovered));
    }

    /**
     * Registra un snapshot de estado de salud
     * - Mantiene snapshots en memoria (se guardan periódicamente)
     * - Pero también escribe a CSV
     * // Without disk storage (original in-memory version)
     */
    // public void recordHealthStateSnapshot(int susceptible, int exposed, int infectious, int recovered) {
    //     // Limitar en memoria a últimos 100 snapshots (circular buffer)
    //     if (stateHistory.size() >= 100) {
    //         stateHistory.remove(0);
    //     }
        
    //     HealthStateSnapshot snapshot = new HealthStateSnapshot(susceptible, exposed, infectious, recovered);
    //     stateHistory.add(snapshot);
        
    //     // Escribir a CSV
    //     csvWriter.recordHealthStateSnapshot(
    //         System.currentTimeMillis(),
    //         susceptible, exposed, infectious, recovered
    //     );
    // }
    
    /**
     * Sets a model-specific statistic value.
     * Stores custom statistics that are specific to the transmission model being used.
     * 
     * @param key the statistic key or name
     * @param value the statistic value
     */
    public void setModelSpecificStat(String key, Object value) {
        modelSpecificStats.put(key, value);
    }

    /**
     * Records the volume of a room.
     * @param roomId the ID of the room
     * @param volume the volume of the room in cubic meters
     */
    public void recordRoomVolume(int roomId, double volume) {
        roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId)).setRoomVolume(volume);
    }

    /**
     * Records a visit to a room by an infectious user.
     * @param roomId the ID of the room visited by an infectious user
     */
    public void recordInfectiousVisit(int roomId) {
        roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId)).recordInfectiousVisit();
    }
    
    /**
     * Prints final statistics to the console.
     * Outputs a comprehensive summary including simulation time, population statistics,
     * contact statistics, room statistics, model-specific statistics, and health state evolution.
     */
    public void printFinalStatistics() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" FINAL SIMULATION STATISTICS");
        System.out.println("=".repeat(60));
        
        // Time statistics
        long durationMs = simulationEndTime - simulationStartTime;
        double durationSeconds = durationMs / 1000.0;
        System.out.println("  SIMULATION TIME");
        System.out.println("   Duration: " + String.format("%.2f seconds", durationSeconds));
        System.out.println("   Start: " + new Date(simulationStartTime));
        System.out.println("   End: " + new Date(simulationEndTime));
        
        // Population statistics
        System.out.println("\n POPULATION STATISTICS");
        System.out.println("   Total users: " + totalUsers);
        System.out.println("   Initial infected: " + initialInfected);
        System.out.println("   Total infections: " + totalInfections);
        System.out.println("   Total recoveries: " + totalRecoveries);
        System.out.println("   Attack rate: " + String.format("%.2f%%", (totalInfections * 100.0) / totalUsers));
        
        // Contact statistics
        if (totalContacts > 0) {
            System.out.println("\n🤝 CONTACT STATISTICS");
            System.out.println("   Total contacts: " + totalContacts);
            System.out.println("   Infectious contacts: " + infectiousContacts);
            System.out.println("   Average duration: " + String.format("%.2f seconds", averageContactDuration));
            System.out.println("   Transmission efficiency: " + String.format("%.2f%%", 
                (infectiousContacts > 0 ? (totalInfections * 100.0) / infectiousContacts : 0)));
        }
        
        // Room statistics
        if (!roomStats.isEmpty()) {
            System.out.println("\n ROOM STATISTICS");
            for (RoomStatistics roomStat : roomStats.values()) {
                roomStat.printStats();
            }
        }
        
        // Model-specific statistics
        if (!modelSpecificStats.isEmpty()) {
            System.out.println("\n🔬 MODEL-SPECIFIC STATISTICS");
            for (Map.Entry<String, Object> entry : modelSpecificStats.entrySet()) {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Health state evolution
        if (!stateHistory.isEmpty()) {
            System.out.println("\n HEALTH STATE EVOLUTION");
            HealthStateSnapshot last = stateHistory.get(stateHistory.size() - 1);
            System.out.println("   Final state:");
            System.out.println("     Susceptible: " + last.susceptible);
            System.out.println("     Infectious: " + last.infectious);
            System.out.println("     Recovered: " + last.recovered);
        }
        
        System.out.println("\n" + "=".repeat(60));
    }

    public void printRoomVolumeAnalysis() {
        System.out.println("\n ROOM VOLUME vs CONCENTRATION ANALYSIS:");
        System.out.printf("%-8s %-12s %-14s %-14s %-10s%n",
            "ROOM", "VOLUME(m³)", "AVG_CONC", "MAX_CONC", "INF_VISITS");
        System.out.println("-".repeat(65));

        roomStats.entrySet().stream()
            .filter(e -> !e.getValue().aerosolConcentrations.isEmpty())
            .sorted(Comparator.comparingDouble(e -> e.getValue().roomVolume))
            .forEach(e -> {
                RoomStatistics rs = e.getValue();
                double avg = rs.aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double max = rs.aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);
                System.out.printf("%-8d %-12.1f %-14.6e %-14.6e %-10d%n",
                    e.getKey(), rs.roomVolume, avg, max, rs.infectiousVisits);
            });
    }
    
    
    public static class RoomStatistics {
        private int roomId;
        private int totalContacts = 0;
        protected List<Double> aerosolConcentrations = new ArrayList<>();

        private double roomVolume = 0.0;
        private int infectiousVisits = 0;
        
        public RoomStatistics(int roomId) {
            this.roomId = roomId;
        }
        
        /**
         * Increments the total contact count for this room.
         */
        public void addContact() {
            totalContacts++;
        }
        
        /**
         * Adds an aerosol concentration measurement for this room.
         * 
         * @param concentration the aerosol concentration value
         */
        public void addAerosolMeasurement(double concentration) {
            aerosolConcentrations.add(concentration);
        }
        
        /**
         * Prints statistics for this room.
         * Outputs contact count and aerosol concentration statistics (average and maximum).
         */
        public void printStats() {
            System.out.println("   Room " + roomId + ":");
            System.out.println("     Contacts: " + totalContacts);
            
            if (!aerosolConcentrations.isEmpty()) {
                double avgConcentration = aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double maxConcentration = aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);

                System.out.println("     Average aerosol concentration: " + String.format("%.2e", avgConcentration));
                System.out.println("     Maximum aerosol concentration: " + String.format("%.2e", maxConcentration));
            }
        }

        public void setRoomVolume(double vol) { this.roomVolume = vol; }
        public void recordInfectiousVisit() { this.infectiousVisits++; }
    }
    
    public static class HealthStateSnapshot {
        public final int susceptible, infectious, recovered;
        public final LocalDateTime timestamp;
        
        public HealthStateSnapshot(int susceptible, int infectious, int recovered) {
            this.susceptible = susceptible;
            this.infectious = infectious;
            this.recovered = recovered;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    // Getters
    
    /** Gets the total number of infections. 
     * 
     * @return total infections count 
     */
    public int getTotalInfections() { return totalInfections; }
    
    /** Gets the total number of contacts. 
     * 
     * @return total contacts count
      */
    public int getTotalContacts() { return totalContacts; }
    
    /** Gets the simulation duration in seconds. 
     * 
     * @return simulation duration in seconds 
     */
    public double getSimulationDurationSeconds() { 
        return (simulationEndTime - simulationStartTime) / 1000.0; 
    }

   
    /**
     * Calculates the average aerosol concentration across all rooms.
     * Computes the arithmetic mean of all concentration measurements
     * from all rooms.
     * 
     * @return average aerosol concentration
     */
    public double getAverageAerosolConcentration() {
        if (roomStats.isEmpty()) {
            return 0.0;
        }
        
        double totalConcentration = 0.0;
        int totalMeasurements = 0;
        
        for (RoomStatistics roomStat : roomStats.values()) {
            if (!roomStat.aerosolConcentrations.isEmpty()) {
                for (Double concentration : roomStat.aerosolConcentrations) {
                    totalConcentration += concentration;
                    totalMeasurements++;
                }
            }
        }
        
        return totalMeasurements > 0 ? totalConcentration / totalMeasurements : 0.0;
    }

    /**
     * Calculates the maximum aerosol concentration recorded in any room.
     * Finds the highest concentration value among all measurements
     * across all rooms.
     * 
     * @return maximum aerosol concentration
     */
    public double getMaxAerosolConcentration() {
        double maxConcentration = 0.0;
        
        for (RoomStatistics roomStat : roomStats.values()) {
            if (!roomStat.aerosolConcentrations.isEmpty()) {
                for (Double concentration : roomStat.aerosolConcentrations) {
                    if (concentration > maxConcentration) {
                        maxConcentration = concentration;
                    }
                }
            }
        }
        
        return maxConcentration;
    }

    /**
     * Gets the number of infectious contacts.
     * 
     * @return count of contacts involving infectious users
     */
    public int getInfectiousContacts() {
        return infectiousContacts;
    }

    /**
     * Gets statistics for a specific room.
     * 
     * @param roomId the ID of the room
     * @return room statistics object, or null if not found
     */
    public RoomStatistics getRoomStatistics(int roomId) {
        return roomStats.get(roomId);
    }

    /**
     * Gets statistics for all rooms.
     * 
     * @return copy of the map containing all room statistics
     */
    public Map<Integer, RoomStatistics> getAllRoomStatistics() {
        return new HashMap<>(roomStats);
    }

    /**
     * Gets the time-weighted average aerosol concentration across all rooms.
     * Calculates the average concentration weighted by exposure time,
     * providing a more accurate measure of actual exposure.
     * 
     * @return time-weighted average concentration
     */
    public double getTimeWeightedAverageAerosolConcentration() {
        if (totalExposureTimeSeconds <= 0) {
            return 0.0;
        }
        
        double result = integratedConcentration / totalExposureTimeSeconds;
     
        return result;
    }

    /**
     * Gets the number of rooms with recorded statistics.
     * 
     * @return count of rooms in the statistics map
     */
    public int getRoomStatsSize() {
        return roomStats != null ? roomStats.size() : 0;
    }

    /**
     * Gets the total number of aerosol measurements across all rooms.
     * 
     * @return total count of aerosol concentration measurements
     */
    public int getTotalAerosolMeasurements() {
        if (roomStats == null || roomStats.isEmpty()) return 0;
        int total = 0;
        for (RoomStatistics rs : roomStats.values()) {
            if (rs.aerosolConcentrations != null) {
                total += rs.aerosolConcentrations.size();
            }
        }
        return total;
    }

    /**
     * Gets the number of health state snapshots recorded.
     * 
     * @return count of snapshots in the state history
     */
    public int getStateHistorySize() {
        return stateHistory != null ? stateHistory.size() : 0;
    }

    /**
     * Gets the number of model-specific statistics recorded.
     * 
     * @return count of model-specific statistics
     */
    public int getModelSpecificStatsSize() {
        return modelSpecificStats != null ? modelSpecificStats.size() : 0;
    }

}