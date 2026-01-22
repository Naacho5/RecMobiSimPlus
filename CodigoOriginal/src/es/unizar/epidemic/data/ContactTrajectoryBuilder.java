package es.unizar.epidemic.data;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import es.unizar.util.ElementIdMapper;
import es.unizar.epidemic.data.TemporalRange;

/**
 * Class that builds contact trajectories from CSV data
 * 
 * @author Nacho Palacio
 */
public class ContactTrajectoryBuilder {

    public static class UserRoomEvent {
        public int userId;
        public int roomId;
        public long startTime;
        public long endTime;
        public String scope;
        
        public UserRoomEvent(int userId, int roomId, long startTime, long endTime, String scope) {
            this.userId = userId;
            this.roomId = roomId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.scope = scope;
        }
    }

    private static Map<Integer, Integer> realToSimulationIdMap = new HashMap<>();
    private static int nextSimulationId = 1;
    private static long minDate = Long.MAX_VALUE;
    private static Set<Integer> assignedRooms = new HashSet<>();

    
    /**
     * Maps real patient IDs to sequential simulation IDs (1, 2, 3...).
     * Creates a new mapping if the real ID hasn't been seen before.
     * 
     * @param realId the real patient ID from the source data
     * @return the corresponding sequential simulation ID
     */
    public static int getSimulationId(int realId) {
        return realToSimulationIdMap.computeIfAbsent(realId, k -> nextSimulationId++);
    }

    /**
     * Checks if a real ID has been mapped to a simulation ID.
     * 
     * @param realId the real patient ID to check
     * @return true if the ID has been mapped, false otherwise
     */
    public static boolean hasSimulationId(int realId) {
        return realToSimulationIdMap.containsKey(realId);
    }

    /**
     * Maps basic zone to simulator room ID using modulo operation.
     * 
     * @param zonaBasica the basic zone identifier from source data
     * @return the mapped room ID within the simulator's room range
     */
    private static int mapToSimulationRoom(int zonaBasica) {
        int numRooms = getNumberOfRoomsInSimulator();
        
        //  DEBUG: Print obtained value
        if (numRooms <= 0) {
            System.err.println("Warning! WARNING: getNumberOfRoomsInSimulator() returned " + numRooms);
            System.err.println("   Using default value: 26 rooms");
            numRooms = 26; // MoMA_Museum has 26 rooms
        }
        
        int mappedRoom = ((zonaBasica - 1) % numRooms) + 1;
        
        return mappedRoom;
    }

    /**
     * Resets room assignments to initial state.
     * Should be called before building new trajectories to ensure clean state.
     */
    public static void resetRoomAssignments() {
        assignedRooms.clear();
        System.out.println("    Room assignments reset");
    }

    /**
     * Gets number of rooms from the current simulation floor.
     * Attempts to retrieve from MainSimulator.floor first, then from ROOM_FLOOR_COMBINED file.
     * Falls back to default value of 26 (MoMA_Museum) if neither source is available.
     * 
     * @return the number of rooms available in the simulator
     */
    private static int getNumberOfRoomsInSimulator() {
        try {
            if (es.unizar.gui.MainSimulator.floor != null) {
                int roomCount = es.unizar.gui.MainSimulator.floor.getRoomCount();
                if (roomCount > 0) {
                    System.out.println("    Rooms obtained from MainSimulator.floor: " + roomCount);
                    return roomCount;
                }
            }
        } catch (Exception e) {
            System.err.println("   Warning! Error obtaining rooms from MainSimulator.floor: " + e.getMessage());
        }
        
        try {
            java.io.File roomFile = new java.io.File(es.unizar.util.Literals.ROOM_FLOOR_COMBINED);
            
            if (roomFile.exists()) {
                es.unizar.access.DataAccessRoomFile roomFileAccess = 
                    new es.unizar.access.DataAccessRoomFile(roomFile);
                
                int roomCount = roomFileAccess.getNumberOfRoom();
                if (roomCount > 0) {
                    System.out.println("    Rooms obtained from ROOM_FLOOR_COMBINED: " + roomCount);
                    return roomCount;
                }
            }
        } catch (Exception e) {
            System.err.println("   Warning! Error reading ROOM_FLOOR_COMBINED: " + e.getMessage());
        }

        System.out.println("   Warning! Could not obtain number of rooms, using default value: 26 (MoMA_Museum)");
        return 26; // MoMA_Museum has 26 rooms
    }

    
    /**
     * Builds user-room events using simplified circular rotation model.
     * Each clique rotates through rooms synchronously at fixed intervals.
     * 
     * @param userToCliqueMap mapping from user IDs to their clique indices
     * @param simulationDuration total duration of the simulation in seconds
     * @param eventDuration duration of each event in seconds
     * @param numRooms number of available rooms in the simulation
     * @return map of user IDs to their list of room events
     */
    public static Map<Integer, List<UserRoomEvent>> buildUserRoomEvents(
            Map<Integer, Integer> userToCliqueMap,
            long simulationDuration,
            int eventDuration,
            int numRooms) {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" GENERATING SIMPLIFIED EVENTS (CIRCULAR ROTATION)");
        System.out.println("=".repeat(80));
        System.out.println("    Parameters:");
        System.out.println("      - Users to process: " + userToCliqueMap.size());
        System.out.println("      - Simulation duration: " + simulationDuration + "s (" + 
                         String.format("%.1f", simulationDuration / 3600.0) + " hours)");
        System.out.println("      - Event duration: " + eventDuration + "s (" + 
                         String.format("%.1f", eventDuration / 60.0) + " min)");
        System.out.println("      - Available rooms: " + numRooms);
        
        Map<Integer, List<UserRoomEvent>> userEvents = new HashMap<>();
        
        // Calculate the number of events
        int numEvents = (int) Math.ceil((double) simulationDuration / eventDuration);
        System.out.println("      - Events per user: " + numEvents);
        
        // Get unique cliques
        Set<Integer> uniqueCliques = new HashSet<>(userToCliqueMap.values());
        System.out.println("      - Unique cliques: " + uniqueCliques.size());
        
        // Generate events for each user
        int usersProcessed = 0;
        Map<Integer, Integer> usersPerClique = new HashMap<>();
        
        for (Map.Entry<Integer, Integer> entry : userToCliqueMap.entrySet()) {
            int userId = entry.getKey();
            int cliqueId = entry.getValue();
            
            List<UserRoomEvent> events = new ArrayList<>();
            
            for (int eventIndex = 0; eventIndex < numEvents; eventIndex++) {
                long startTime = (long) eventIndex * eventDuration;
                long endTime = Math.min((long) (eventIndex + 1) * eventDuration, simulationDuration);
                
                // Each clique rotates through the next room
                int roomId = ((cliqueId + eventIndex) % numRooms) + 1;
                
                events.add(new UserRoomEvent(userId, roomId, startTime, endTime, "synchronized"));
            }
            
            userEvents.put(userId, events);
            usersProcessed++;
            usersPerClique.merge(cliqueId, 1, Integer::sum);
        }
        
        System.out.println("\n    Events generated:");
        System.out.println("      - Users processed: " + usersProcessed);
        System.out.println("      - Total events created: " + (usersProcessed * numEvents));
    
        return userEvents;
    }

    /**
     * Reads CSV and builds complex user-room events with mapped IDs and relative times.
     * This method processes contact data from CSV file and creates detailed event trajectories.
     * 
     * @param csvPath path to the CSV file containing contact data
     * @param maxUsers maximum number of users to process
     * @param priorityUserIds set of user IDs to prioritize (can be null)
     * @param userToRoomMapping mapping from user IDs to room assignments (can be null)
     * @return map of user IDs to their list of room events
     * @throws IOException if there's an error reading the CSV file
     */
    public static Map<Integer, List<UserRoomEvent>> buildUserRoomEventsFromCSV(
            String csvPath, 
            int maxUsers, 
            Set<Integer> priorityUserIds,
            Map<Integer, Integer> userToRoomMapping) throws IOException {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" BUILDING EVENTS FROM CSV (COMPLEX MODEL)");
        System.out.println("=".repeat(80));
        
        BufferedReader br = new BufferedReader(new FileReader(csvPath));
        String line;
        br.readLine(); // header
        
        realToSimulationIdMap.clear();
        nextSimulationId = 1;
        minDate = Long.MAX_VALUE;
        resetRoomAssignments();
        
        Set<Integer> uniqueRealUsers = new HashSet<>();

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 6) continue;

            int realUser1 = parseInt(parts[0]);
            int realUser2 = parseInt(parts[1]);

            if (priorityUserIds != null && !priorityUserIds.isEmpty()) {
                if (!priorityUserIds.contains(realUser1) && !priorityUserIds.contains(realUser2)) {
                    continue;
                }
            }

            long date = parseDate(parts[3]);
            if (date > 0 && date < minDate) {
                minDate = date;
            }
            
            uniqueRealUsers.add(realUser1);
            uniqueRealUsers.add(realUser2);
        }
        br.close();
        
        System.out.println("    Minimum date found: " + minDate);
        System.out.println("    Unique users found: " + uniqueRealUsers.size());

        Set<Integer> usersToLoad = priorityUserIds != null ? priorityUserIds : uniqueRealUsers;
        
        if (priorityUserIds != null) {
            System.out.println("    Using " + priorityUserIds.size() + " priority users from cliques");
        } else {
            System.out.println("   Warning! No priority users provided, using " + uniqueRealUsers.size() + " from CSV");
        }

        Map<Integer, List<UserRoomEvent>> userRoomEvents = new HashMap<>();
        br = new BufferedReader(new FileReader(csvPath));
        br.readLine(); // header
        
        int linesProcessed = 0;
        int validLines = 0;
        
        Map<Integer, Integer> userEventCounters = new HashMap<>();

        while ((line = br.readLine()) != null) {
            linesProcessed++;
            String[] parts = line.split(",");
            if (parts.length < 6) {
                continue;
            }

            int realUser1 = parseInt(parts[0]);
            int realUser2 = parseInt(parts[1]);
            
            if (realUser1 <= 0 || realUser2 <= 0) {
                continue;
            }
            
            if (!usersToLoad.contains(realUser1) && !usersToLoad.contains(realUser2)) {
                continue;
            }
            
            int user1 = -1;
            int user2 = -1;
            
            if (usersToLoad.contains(realUser1)) {
                user1 = getSimulationId(realUser1);
            }
            
            if (usersToLoad.contains(realUser2)) {
                user2 = getSimulationId(realUser2);
            }

            // Get the room for each user
            int roomIdUser1 = -1;
            int roomIdUser2 = -1;
            
            if (userToRoomMapping != null && userToRoomMapping.containsKey(realUser1)) {
                roomIdUser1 = userToRoomMapping.get(realUser1);
            } else {
                roomIdUser1 = mapToSimulationRoom(parseInt(parts[4]));
            }
            
            if (userToRoomMapping != null && userToRoomMapping.containsKey(realUser2)) {
                roomIdUser2 = userToRoomMapping.get(realUser2);
            } else {
                roomIdUser2 = mapToSimulationRoom(parseInt(parts[4]));
            }
            
            long absoluteTime = parseDate(parts[3]);
            long baseRelativeTime = absoluteTime - minDate;
            
            if (baseRelativeTime < 0) {
                baseRelativeTime = 0;
            }
            
            String scope = parts[5].trim();

            if (user1 < 0 && user2 < 0) {
                continue;
            }

            // Create events for each user
            int offset = 240; // 240 seconds per event
            if (user1 >= 0 && roomIdUser1 > 0) {
                int eventCount1 = userEventCounters.getOrDefault(user1, 0);
                
                long relativeStartTime1 = baseRelativeTime + (eventCount1 * offset);
                long relativeEndTime1 = relativeStartTime1 + offset;
                
                if (relativeStartTime1 < 0) {
                    relativeStartTime1 = 0;
                    relativeEndTime1 = 1800;
                }
                
                UserRoomEvent event1 = new UserRoomEvent(user1, roomIdUser1, relativeStartTime1, relativeEndTime1, scope);
                userRoomEvents.computeIfAbsent(user1, k -> new ArrayList<>()).add(event1);
                
                userEventCounters.put(user1, eventCount1 + 1);
            }
            
            if (user2 >= 0 && roomIdUser2 > 0) {
                int eventCount2 = userEventCounters.getOrDefault(user2, 0);
                
                long relativeStartTime2 = baseRelativeTime + (eventCount2 * offset);
                long relativeEndTime2 = relativeStartTime2 + offset;
                
                if (relativeStartTime2 < 0) {
                    relativeStartTime2 = 0;
                    relativeEndTime2 = 1800;
                }
                
                UserRoomEvent event2 = new UserRoomEvent(user2, roomIdUser2, relativeStartTime2, relativeEndTime2, scope);
                userRoomEvents.computeIfAbsent(user2, k -> new ArrayList<>()).add(event2);
                
                userEventCounters.put(user2, eventCount2 + 1);
            }
            
            validLines++;
        }
        br.close();
        
        System.out.println("\n    LOADING STATISTICS:");
        System.out.println("      - Lines processed: " + linesProcessed);
        System.out.println("      - Valid lines: " + validLines);
        System.out.println("      - Simulation IDs mapped: " + realToSimulationIdMap.size());
        System.out.println("      - Users with events: " + userRoomEvents.size());
        System.out.println("      - Total events created: " + 
            userRoomEvents.values().stream().mapToInt(List::size).sum());
        
        System.out.println("=".repeat(80) + "\n");
        
        return userRoomEvents;
    }

    /**
     * Builds a map from simulation user IDs to clique indices based on selected cliques.
     * 
     * @param selectedCliques list of cliques, where each clique is a list of user ID strings
     * @return map from simulation user IDs to their clique index
     */
    public static Map<Integer, Integer> buildUserToCliqueMapFromSelectedCliques(
            List<List<String>> selectedCliques) {
        
        Map<Integer, Integer> userToCliqueMap = new HashMap<>();
        
        System.out.println("    Building user -> clique mapping...");
        
        int totalMapped = 0;
        
        for (int cliqueIndex = 0; cliqueIndex < selectedCliques.size(); cliqueIndex++) {
            List<String> clique = selectedCliques.get(cliqueIndex);
            int mappedInClique = 0;
            
            for (String userIdStr : clique) {
                try {
                    int realId = Integer.parseInt(userIdStr);
                    int simId = getSimulationId(realId);
                    
                    userToCliqueMap.put(simId, cliqueIndex);
                    mappedInClique++;
                    totalMapped++;
                    
                } catch (NumberFormatException e) {
                    System.err.println("      Warning! Invalid ID in clique " + cliqueIndex + ": " + userIdStr);
                }
            }
            
            System.out.println("      - Clique " + cliqueIndex + ": " + mappedInClique + " users mapped");
        }
        
        System.out.println("    Total users mapped: " + totalMapped);
        
        return userToCliqueMap;
    }

    /**
     * Gets the total number of unique users in the CSV file.
     * 
     * @param csvPath path to the CSV file to analyze
     * @return the count of unique user IDs found in the file
     * @throws IOException if there's an error reading the CSV file
     */
    public static int getUniqueUserCount(String csvPath) throws IOException {
        Set<Integer> uniqueUsers = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(csvPath));
        String line;
        br.readLine();
        
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            int user1 = parseInt(parts[0]);
            int user2 = parseInt(parts[1]);
            if (user1 > 0) uniqueUsers.add(user1);
            if (user2 > 0) uniqueUsers.add(user2);
        }
        br.close();
        return uniqueUsers.size();
    }

    /**
     * Resets all mappings to initial state.
     * Clears real-to-simulation ID mappings and resets counters.
     */
    public static void resetMappings() {
        realToSimulationIdMap.clear();
        nextSimulationId = 1;
        minDate = Long.MAX_VALUE;
    }

    /**
     * Safely parses a string to integer.
     * 
     * @param s the string to parse
     * @return the parsed integer, or -1 if parsing fails
     */
    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Parses a date string in format "YYYY-MM-DD" to Unix timestamp.
     * 
     * @param dateStr the date string to parse
     * @return Unix timestamp in seconds, or -1 if parsing fails
     */
    private static long parseDate(String dateStr) {
        try {
            String[] parts = dateStr.trim().split("-");
            if (parts.length != 3) return -1;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]), 0, 0, 0);
            return cal.getTimeInMillis() / 1000;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Determines the room assignment for a clique at a given time.
     * Uses circular rotation based on event index.
     * 
     * @param cliqueId the clique identifier
     * @param timeSeconds current time in seconds
     * @param eventDuration duration of each event in seconds
     * @param numRooms total number of available rooms
     * @return the room ID assigned to this clique at this time
     */
    public static int getRoomForCliqueAtTime(int cliqueId, long timeSeconds, 
                                             int eventDuration, int numRooms) {
        int eventIndex = (int) (timeSeconds / eventDuration);
        return ((cliqueId + eventIndex) % numRooms) + 1;
    }


    // CLIQUES

    /**
     * Represents clique data loaded from JSON
     */
    public static class CliqueData {
        public int total_users;
        public int total_edges;
        public int max_contacts;
        public List<String> users_with_max_contacts;
        public List<List<String>> cliques;
        public int num_cliques;
        public Map<String, ConnectorInfo> connectors;
        
        public CliqueData() {
            users_with_max_contacts = new ArrayList<>();
            cliques = new ArrayList<>();
            connectors = new HashMap<>();
        }
        
        public static class ConnectorInfo {
            public List<Integer> cliques;
            public int num_cliques;
        }
    }

    /**
     * Reads and parses cliques from a JSON file.
     * 
     * @param jsonPath path to the JSON file containing clique data
     * @return CliqueData object containing parsed clique information
     * @throws IOException if there's an error reading or parsing the file
     */
    public static CliqueData loadCliquesFromJson(String jsonPath) throws IOException {
        System.out.println(" Loading cliques from: " + jsonPath);
        
        BufferedReader reader = new BufferedReader(new FileReader(jsonPath));
        Gson gson = new Gson();
        CliqueData data = gson.fromJson(reader, CliqueData.class);
        reader.close();
        
        System.out.println("    Cliques loaded successfully:");
        System.out.println("      - Total users: " + data.total_users);
        System.out.println("      - Cliques found: " + data.num_cliques);
        System.out.println("      - Connectors: " + data.connectors.size());
        
        if (data.cliques == null || data.cliques.isEmpty()) {
            System.err.println("    ERROR: Empty cliques list");
            throw new IOException("Corrupted cliques JSON: empty list");
        }
        
        return data;
    }

    /**
     * Selects users by prioritizing complete cliques to maximize co-presence.
     * Sorts cliques by size (largest first) and selects complete cliques until reaching maxUsers.
     * 
     * @param cliquesJsonPath path to JSON file containing clique data
     * @param maxUsers maximum number of users to select
     * @return SelectedUsersResult containing selected users and their cliques
     * @throws IOException if there's an error reading the cliques file
     */
    public static SelectedUsersResult selectUsersFromCompleteCliquesWithCliques(
            String cliquesJsonPath, int maxUsers) throws IOException {
        
        System.out.println("    Selecting users from complete cliques...");
        
        CliqueData cliqueData = loadCliquesFromJson(cliquesJsonPath);
        
        List<List<String>> sortedCliques = new ArrayList<>(cliqueData.cliques);
        sortedCliques.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));
        
        System.out.println("    Clique size distribution:");
        Map<Integer, Integer> cliqueSizeDistribution = new HashMap<>();
        for (List<String> clique : sortedCliques) {
            int size = clique.size();
            cliqueSizeDistribution.put(size, cliqueSizeDistribution.getOrDefault(size, 0) + 1);
        }
        
        List<Integer> sizes = new ArrayList<>(cliqueSizeDistribution.keySet());
        sizes.sort((a, b) -> Integer.compare(b, a));
        for (Integer size : sizes) {
            int count = cliqueSizeDistribution.get(size);
            System.out.println("      - Cliques of " + size + " users: " + count);
        }
        
        Set<Integer> selectedUsers = new HashSet<>();
        List<List<String>> selectedCliques = new ArrayList<>();
        
        System.out.println("\n    Selecting cliques:");
        
        for (int i = 0; i < sortedCliques.size() && selectedUsers.size() < maxUsers; i++) {
            List<String> clique = sortedCliques.get(i);
            
            List<Integer> cliqueUsers = new ArrayList<>();
            for (String userIdStr : clique) {
                try {
                    cliqueUsers.add(Integer.parseInt(userIdStr));
                } catch (NumberFormatException e) {
                    System.err.println("      Warning! ID inválido en clique: " + userIdStr);
                }
            }
            
            if (selectedUsers.size() + cliqueUsers.size() <= maxUsers) {
                selectedUsers.addAll(cliqueUsers);
                selectedCliques.add(clique);
            } else {
                int remainingSlots = maxUsers - selectedUsers.size();
                
                if (remainingSlots > 0) {
                    List<Integer> partialClique = cliqueUsers.subList(0, remainingSlots);
                    selectedUsers.addAll(partialClique);
                    
                    List<String> partialCliqueStrings = new ArrayList<>();
                    for (int j = 0; j < remainingSlots; j++) {
                        partialCliqueStrings.add(clique.get(j));
                    }
                    selectedCliques.add(partialCliqueStrings);
                    
                    System.out.println("      Warning! Clique " + (selectedCliques.size()) + " PARTIAL (" + remainingSlots + "/" + 
                                    cliqueUsers.size() + " users) -> Total: " + selectedUsers.size() + "/" + maxUsers);
                }
                
                break;
            }
        }
        
        System.out.println("\n    FINAL SUMMARY:");
        System.out.println("      - Selected users: " + selectedUsers.size());
        System.out.println("      - Selected cliques: " + selectedCliques.size());
        
        //  RETURN BOTH
        return new SelectedUsersResult(selectedUsers, selectedCliques);
    }

    public static class SelectedUsersResult {
        public Set<Integer> users;
        public List<List<String>> cliques;
        
        public SelectedUsersResult(Set<Integer> users, List<List<String>> cliques) {
            this.users = users;
            this.cliques = cliques;
        }
    }
    

}

