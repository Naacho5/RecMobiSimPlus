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

    private static final String BASE_SCOPE = "BASE";
    private static final long DEFAULT_DAY_OFFSET_SECONDS = 12 * 3600;

    public enum ContactScopeDuration {
        OTROS("Otros", 0.25),
        MEDIO_TRANSPORTE("Medio de transporte", 0.25),
        SOCIAL("Social", 0.5),
        ESCOLAR("Escolar", 2.0),
        LABORAL("Laboral", 2.0),
        CUIDADOR_NO_SANITARIO("Cuidador no sanitario", 2.0),
        DOMICILIO("Domicilio", 4.0),
        DEFAULT("Default", 0.25);

        private final String label;
        private final long seconds;

        ContactScopeDuration(String label, double hours) {
            this.label = label;
            this.seconds = Math.round(hours * 3600.0);
        }

        public long getSeconds() {
            return seconds;
        }

        public static ContactScopeDuration fromScope(String scope) {
            if (scope == null) {
                return DEFAULT;
            }
            String normalized = scope.trim().toLowerCase(Locale.ROOT);
            for (ContactScopeDuration value : values()) {
                if (value.label.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return value;
                }
            }
            return DEFAULT;
        }
    }

    public static class RealChronologyResult {
        public final Map<Integer, List<UserRoomEvent>> userEvents;
        public final long startTime;
        public final long endTime;
        public final long durationSeconds;

        public RealChronologyResult(Map<Integer, List<UserRoomEvent>> userEvents,
                                    long startTime,
                                    long endTime,
                                    long durationSeconds) {
            this.userEvents = userEvents;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = durationSeconds;
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
        
        Map<Integer, List<UserRoomEvent>> userEvents = new HashMap<>();
        
        // Calculate the number of events
        int numEvents = (int) Math.ceil((double) simulationDuration / eventDuration);
        
        // Get unique cliques
        Set<Integer> uniqueCliques = new HashSet<>(userToCliqueMap.values());
        
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
        
        Set<Integer> usersToLoad = priorityUserIds != null ? priorityUserIds : uniqueRealUsers;

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
     * Reads CSV and builds user-room events while preserving real chronology.
     * This method processes contact data from CSV file and creates detailed event trajectories with accurate timing based on real contact times.
     * @param csvPath path to the CSV file containing contact data
     * @param realUserIds set of real user IDs to include in the output (only contacts involving these users will be processed)
     * @return RealChronologyResult containing the map of user IDs to their list of room events, as well as overall timing information
     * @throws IOException
     */
    public static RealChronologyResult buildUserRoomEventsFromCSVRealChronology(
            String csvPath,
            Set<Integer> realUserIds) throws IOException {

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" BUILDING EVENTS FROM CSV (REAL CHRONOLOGY)");
        System.out.println("=".repeat(80));

        if (realUserIds == null || realUserIds.isEmpty()) {
            throw new IllegalArgumentException("realUserIds is empty");
        }

        realToSimulationIdMap.clear();
        nextSimulationId = 1;
        minDate = Long.MAX_VALUE;
        resetRoomAssignments();

        List<Integer> sortedUsers = new ArrayList<>(realUserIds);
        sortedUsers.sort(Integer::compareTo);

        final int totalUsers = sortedUsers.size();

        for (int realUserId : sortedUsers) {
            getSimulationId(realUserId);
        }

        class ContactRecord {
            int simUser1;
            int simUser2;
            long start;
            long end;
            String scope;
            int meetingRoomId;

            ContactRecord(int simUser1, int simUser2, long start, long end, String scope, int meetingRoomId) {
                this.simUser1 = simUser1;
                this.simUser2 = simUser2;
                this.start = start;
                this.end = end;
                this.scope = scope;
                this.meetingRoomId = meetingRoomId;
            }
        }

        List<ContactRecord> contacts = new ArrayList<>();
        int linesProcessed = 0;
        int validLines = 0;
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {
                linesProcessed++;
                // System.out.println("    Processing line " + linesProcessed + ": " + line);

                String[] parts = line.split(",");
                if (parts.length < 6) {
                    // System.out.println("      Skipping line due to insufficient columns");
                    continue;
                }

                int realUser1 = parseInt(parts[0]);
                int realUser2 = parseInt(parts[1]);

                // System.out.println("      Parsed users: " + realUser1 + ", " + realUser2);

                if (realUser1 <= 0 || realUser2 <= 0) {
                    System.out.println("      Skipping line due to invalid user IDs");
                    continue;
                }

                if (!realUserIds.contains(realUser1) || !realUserIds.contains(realUser2)) {
                    // System.out.println("      Skipping line because it does not involve selected users");
                    continue;
                }

                System.out.println("      Line involves selected users, processing line " + linesProcessed + ": " + line);

                long absoluteDay = parseDate(parts[3]);
                System.out.println("      Parsed date: " + parts[3] + " -> " + absoluteDay);
                if (absoluteDay <= 0) {
                    System.out.println("      Skipping line due to invalid date: " + parts[3]);
                    continue;
                }

                if (absoluteDay < minDate) {
                    System.out.println("      Updating minDate: " + minDate + " -> " + absoluteDay);
                    minDate = absoluteDay;
                }

                String scope = parts[5].trim();
                System.out.println("      Parsed scope: " + scope);
                long durationSeconds = ContactScopeDuration.fromScope(scope).getSeconds();
                System.out.println("      Mapped scope to duration (seconds): " + durationSeconds);
                if (durationSeconds <= 0) {
                    System.out.println("      Skipping line due to invalid duration: " + durationSeconds);
                    continue;
                }

                int simUser1 = getSimulationId(realUser1);
                int simUser2 = getSimulationId(realUser2);
                System.out.println("      Mapped real users to simulation IDs: " + simUser1 + ", " + simUser2);

                long start = absoluteDay + DEFAULT_DAY_OFFSET_SECONDS;
                long end = start + durationSeconds;

                int meetingAnchorUser = Math.min(simUser1, simUser2);
                int meetingRoomId = totalUsers + meetingAnchorUser;

                System.out.println("      Calculated contact times: start=" + start + ", end=" + end);
                System.out.println("      Calculated meeting room ID: " + meetingRoomId);

                contacts.add(new ContactRecord(
                        simUser1,
                        simUser2,
                        start,
                        end,
                        scope,
                        meetingRoomId
                ));

                if (start < minStart) {
                    System.out.println("      Updating minStart: " + minStart + " -> " + start);
                    minStart = start;
                }
                if (end > maxEnd) {
                    System.out.println("      Updating maxEnd: " + maxEnd + " -> " + end);
                    maxEnd = end;
                }

                validLines++;
            }
        }

        if (contacts.isEmpty() || minStart == Long.MAX_VALUE || maxEnd == Long.MIN_VALUE) {
            throw new IOException("No valid contacts found for selected users");
        }

        contacts.sort(Comparator
                .comparingLong((ContactRecord c) -> c.start)
                .thenComparingLong(c -> c.end)
                .thenComparingInt(c -> c.meetingRoomId)
                .thenComparingInt(c -> c.simUser1)
                .thenComparingInt(c -> c.simUser2));
        
        Map<Integer, Long> lastEndByUser = new HashMap<>();

        for (ContactRecord contact : contacts) {
            long latestEnd = 0;

            long prevEnd1 = lastEndByUser.getOrDefault(contact.simUser1, 0L);
            long prevEnd2 = lastEndByUser.getOrDefault(contact.simUser2, 0L);
            latestEnd = Math.max(prevEnd1, prevEnd2);

            if (latestEnd > contact.start) {
                long shift = latestEnd - contact.start;
                contact.start += shift;
                contact.end   += shift;
            }

            lastEndByUser.put(contact.simUser1, contact.end);
            lastEndByUser.put(contact.simUser2, contact.end);
        }

        // Recalcular minStart y maxEnd tras la serialización
        minStart = contacts.stream().mapToLong(c -> c.start).min().getAsLong();
        maxEnd   = contacts.stream().mapToLong(c -> c.end).max().getAsLong();

        Map<Integer, List<UserRoomEvent>> userEvents = new HashMap<>();
        Map<Integer, Long> userAvailableFrom = new HashMap<>();

        for (int realUserId : sortedUsers) {
            int simUserId = getSimulationId(realUserId);
            userEvents.put(simUserId, new ArrayList<>());
            userAvailableFrom.put(simUserId, minStart);
            System.out.println("    Initialized user " + simUserId + " with availableFrom=" + minStart);
        }

        for (ContactRecord contact : contacts) {
            int simUser1 = contact.simUser1;
            int simUser2 = contact.simUser2;

            int baseRoom1 = simUser1;
            int baseRoom2 = simUser2;

            System.out.println("    Processing contact between user " + simUser1 + " and user " + simUser2);

            long available1 = userAvailableFrom.get(simUser1);
            if (available1 < contact.start) {
                userEvents.get(simUser1).add(
                        new UserRoomEvent(simUser1, baseRoom1, available1, contact.start, BASE_SCOPE)
                );
                System.out.println("      Added base room event for user " + simUser1 + ": " +
                        "room=" + baseRoom1 + ", start=" + available1 + ", end=" + contact.start);
            }

            long available2 = userAvailableFrom.get(simUser2);
            if (available2 < contact.start) {
                userEvents.get(simUser2).add(
                        new UserRoomEvent(simUser2, baseRoom2, available2, contact.start, BASE_SCOPE)
                );
                System.out.println("      Added base room event for user " + simUser2 + ": " +
                        "room=" + baseRoom2 + ", start=" + available2 + ", end=" + contact.start);
            }

            long effectiveStart1 = Math.max(userAvailableFrom.get(simUser1), contact.start);
            long effectiveStart2 = Math.max(userAvailableFrom.get(simUser2), contact.start);
            long effectiveStart = Math.max(effectiveStart1, effectiveStart2);

            System.out.println("      Calculated effective start time for contact: " + effectiveStart);

            if (effectiveStart < contact.end) {
                userEvents.get(simUser1).add(
                        new UserRoomEvent(simUser1, contact.meetingRoomId, effectiveStart, contact.end, contact.scope)
                );
                userEvents.get(simUser2).add(
                        new UserRoomEvent(simUser2, contact.meetingRoomId, effectiveStart, contact.end, contact.scope)
                );

                userAvailableFrom.put(simUser1, contact.end);
                userAvailableFrom.put(simUser2, contact.end);
                System.out.println("      Added meeting room event for users " + simUser1 + " and " + simUser2 + ": " +
                        "room=" + contact.meetingRoomId + ", start=" + effectiveStart + ", end=" + contact.end);
            } else {
                userAvailableFrom.put(simUser1, Math.max(userAvailableFrom.get(simUser1), contact.end));
                userAvailableFrom.put(simUser2, Math.max(userAvailableFrom.get(simUser2), contact.end));
                System.out.println("      Contact skipped due to effective start time being after contact end time");
            }
        }

        for (int realUserId : sortedUsers) {
            int simUserId = getSimulationId(realUserId);
            long available = userAvailableFrom.get(simUserId);
            int baseRoomId = simUserId;

            System.out.println("    Finalizing events for user " + simUserId + ": availableFrom=" + available);

            if (available < maxEnd) {
                userEvents.get(simUserId).add(
                        new UserRoomEvent(simUserId, baseRoomId, available, maxEnd, BASE_SCOPE)
                );
                System.out.println("      Added final base room event for user " + simUserId + ": " +
                        "room=" + baseRoomId + ", start=" + available + ", end=" + maxEnd);
            }
        }

        for (List<UserRoomEvent> events : userEvents.values()) {
            events.sort(Comparator
                    .comparingLong((UserRoomEvent e) -> e.startTime)
                    .thenComparingLong(e -> e.endTime)
                    .thenComparingInt(e -> e.roomId));

            List<UserRoomEvent> normalizedEvents = new ArrayList<>();
            UserRoomEvent previous = null;

            for (UserRoomEvent event : events) {
                if (event.endTime <= event.startTime) {
                    System.out.println("      Skipping invalid event for user " + event.userId + ": " +
                            "room=" + event.roomId + ", start=" + event.startTime + ", end=" + event.endTime);
                    continue;
                }

                if (previous != null &&
                    previous.roomId == event.roomId &&
                    Objects.equals(previous.scope, event.scope) &&
                    previous.endTime == event.startTime) {

                    previous.endTime = event.endTime;

                    System.out.println("      Merged event for user " + event.userId + ": " +
                            "room=" + event.roomId + ", start=" + previous.startTime + ", end=" + previous.endTime);
                } else {
                    UserRoomEvent copy = new UserRoomEvent(
                            event.userId,
                            event.roomId,
                            event.startTime,
                            event.endTime,
                            event.scope
                    );
                    normalizedEvents.add(copy);
                    previous = copy;

                    System.out.println("      Added event for user " + event.userId + ": " +
                            "room=" + event.roomId + ", start=" + event.startTime + ", end=" + event.endTime);
                }
            }

            events.clear();
            events.addAll(normalizedEvents);

            for (UserRoomEvent e : events) {
                e.startTime -= minStart;
                e.endTime -= minStart;

                System.out.println("      Normalized event for user " + e.userId + ": " +
                        "room=" + e.roomId + ", start=" + e.startTime + ", end=" + e.endTime);
            }
        }

        long totalDuration = maxEnd - minStart;

        System.out.println("\n LOADING STATISTICS:");
        System.out.println(" - Lines processed: " + linesProcessed);
        System.out.println(" - Valid lines: " + validLines);
        System.out.println(" - Simulation IDs mapped: " + realToSimulationIdMap.size());
        System.out.println(" - Users with events: " + userEvents.size());
        System.out.println(" - Total events created: " +
                userEvents.values().stream().mapToInt(List::size).sum());
        System.out.println(" - Base rooms: 1.." + totalUsers);
        System.out.println(" - Meeting rooms: " + (totalUsers + 1) + ".." + (2 * totalUsers));
        System.out.println("=".repeat(80) + "\n");

        return new RealChronologyResult(userEvents, 0, totalDuration, totalDuration);
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
    public static long parseDate(String dateStr) {
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
        
        Map<Integer, Integer> cliqueSizeDistribution = new HashMap<>();
        for (List<String> clique : sortedCliques) {
            int size = clique.size();
            cliqueSizeDistribution.put(size, cliqueSizeDistribution.getOrDefault(size, 0) + 1);
        }
        
        List<Integer> sizes = new ArrayList<>(cliqueSizeDistribution.keySet());
        sizes.sort((a, b) -> Integer.compare(b, a));
        
        Set<Integer> selectedUsers = new HashSet<>();
        List<List<String>> selectedCliques = new ArrayList<>();
        
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
                }
                
                break;
            }
        }
        
        System.out.println("\n    FINAL SUMMARY:");
        System.out.println("      - Selected users: " + selectedUsers.size());
        System.out.println("      - Selected cliques: " + selectedCliques.size());
        
        return new SelectedUsersResult(selectedUsers, selectedCliques);
    }

    /**
     * Selects users from a specific clique by its ID.
     * @param cliquesJsonPath path to JSON file containing clique data
     * @param cliqueId ID of the clique to select users from
     * @return SelectedUsersResult containing selected users and their cliques
     * @throws IOException if there's an error reading the cliques file or if the clique ID is invalid
     */
    public static SelectedUsersResult selectUsersFromCliqueId(
            String cliquesJsonPath, int cliqueId) throws IOException {

        CliqueData cliqueData = loadCliquesFromJson(cliquesJsonPath);

        if (cliqueId < 0 || cliqueId >= cliqueData.cliques.size()) {
            throw new IOException("Invalid cliqueId: " + cliqueId +
                " (valid range: 0.." + (cliqueData.cliques.size() - 1) + ")");
        }

        List<String> clique = cliqueData.cliques.get(cliqueId);

        Set<Integer> selectedUsers = new HashSet<>();
        List<List<String>> selectedCliques = new ArrayList<>();
        selectedCliques.add(clique);

        for (String userIdStr : clique) {
            int realUserId = parseInt(userIdStr);
            if (realUserId > 0) {
                selectedUsers.add(realUserId);
            }
        }

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

