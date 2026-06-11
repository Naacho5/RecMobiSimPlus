package es.unizar.epidemic.validation;

import es.unizar.epidemic.data.ContactTrajectoryBuilder.UserRoomEvent;
import es.unizar.gui.simulation.User;

import java.util.*;

/**
 * Validates that users from contacts.csv actually meet in the same rooms
 * 
 * @author Nacho Palacio
 */
public class ContactValidator {
    
    /**
     * Represents a co-presence event between users in a room
     */
    public static class CoPresence {
        public Set<Integer> userIds;
        public int roomId;
        public long startTime;
        public long endTime;
        
        public CoPresence(Set<Integer> userIds, int roomId, long startTime, long endTime) {
            this.userIds = new HashSet<>(userIds);
            this.roomId = roomId;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        @Override
        public String toString() {
            return String.format("Usuarios %s en Hab %d [%ds -> %ds]", 
                userIds, roomId, startTime, endTime);
        }
    }
    

    /**
     * Detects all co-presences in the loaded trajectories
     */
    public static List<CoPresence> detectCoPresences(Map<Integer, List<UserRoomEvent>> trajectories) {
        System.out.println("\n Detecting co-presences...");
        
        List<CoPresence> coPresences = new ArrayList<>();
        
        List<UserRoomEvent> allEvents = new ArrayList<>();
        for (Map.Entry<Integer, List<UserRoomEvent>> entry : trajectories.entrySet()) {
            allEvents.addAll(entry.getValue());
        }
        
        // Sort by id and start time
        allEvents.sort((e1, e2) -> {
            int roomCompare = Integer.compare(e1.roomId, e2.roomId);
            return roomCompare != 0 ? roomCompare : Long.compare(e1.startTime, e2.startTime);
        });
        
        System.out.println("    Processing " + allEvents.size() + " events...");
        
        // Process events by room
        Map<Integer, List<UserRoomEvent>> eventsByRoom = new HashMap<>();
        for (UserRoomEvent event : allEvents) {
            eventsByRoom.computeIfAbsent(event.roomId, k -> new ArrayList<>()).add(event);
        }
        
        int processedRooms = 0;
        for (Map.Entry<Integer, List<UserRoomEvent>> roomEntry : eventsByRoom.entrySet()) {
            int roomId = roomEntry.getKey();
            List<UserRoomEvent> roomEvents = roomEntry.getValue();
            
            detectRoomCoPresences(roomId, roomEvents, coPresences);
            
            // Cleanup every 50 rooms
            if (processedRooms % 50 == 0) {
                System.gc();
            }
        }
        
        System.out.println("   ✅ Detected " + coPresences.size() + " co-presences");
        return coPresences;
    }

    /**
     * Detects co-presences in a single room's events
     */
    private static void detectRoomCoPresences(int roomId, List<UserRoomEvent> roomEvents, List<CoPresence> coPresences) {
        roomEvents.sort(Comparator.comparingLong(e -> e.startTime));
        
        // Sliding window to detect overlaps
        for (int i = 0; i < roomEvents.size(); i++) {
            UserRoomEvent event1 = roomEvents.get(i);
            Set<Integer> overlappingUsers = new HashSet<>();
            overlappingUsers.add(event1.userId);
            
            long overlapStart = event1.startTime;
            long overlapEnd = event1.endTime;
            
            // Verify overlaps
            for (int j = i + 1; j < roomEvents.size(); j++) {
                UserRoomEvent event2 = roomEvents.get(j);
                
                // If event2 starts after event1 ends, no more overlaps are possible
                if (event2.startTime >= overlapEnd) {
                    break;
                }
                
                if (event2.startTime < event1.endTime && event2.endTime > event1.startTime) {
                    overlappingUsers.add(event2.userId);
                    overlapStart = Math.max(overlapStart, event2.startTime);
                    overlapEnd = Math.min(overlapEnd, event2.endTime);
                }
            }

            // If there is more than one user, create co-presence
            if (overlappingUsers.size() > 1) {
                coPresences.add(new CoPresence(overlappingUsers, roomId, overlapStart, overlapEnd));
            }
            
            // Limit the number of co-presences
            if (coPresences.size() > 10000) {
                System.out.println("   Warning! Limiting detection to 10,000 co-presences to avoid memory issues");
                return;
            }
        }
    }

    /**
     * Prints statistics about detected co-presences
     */
    public static void printCoPresenceStatistics(List<CoPresence> coPresences) {
        System.out.println("\n CO-PRESENCE STATISTICS:");
        
        if (coPresences.isEmpty()) {
            System.out.println("   Warning! No co-presences detected");
            return;
        }
        
        // Distribution by group size
        Map<Integer, Integer> groupSizeDistribution = new HashMap<>();
        for (CoPresence cp : coPresences) {
            int size = cp.userIds.size();
            groupSizeDistribution.put(size, groupSizeDistribution.getOrDefault(size, 0) + 1);
        }
        
        System.out.println("\n   Distribution by group size:");
        groupSizeDistribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.printf("      %d users: %d co-presences\n", entry.getKey(), entry.getValue());
            });
        
        // Most crowded rooms
        Map<Integer, Integer> roomActivity = new HashMap<>();
        for (CoPresence cp : coPresences) {
            roomActivity.put(cp.roomId, roomActivity.getOrDefault(cp.roomId, 0) + 1);
        }
        
        System.out.println("\n   Top 5 rooms with most co-presences:");
        roomActivity.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                System.out.printf("      Room %d: %d co-presences\n", entry.getKey(), entry.getValue());
            });
        
        // Sample of co-presences
        System.out.println("\n   Examples of co-presences:");
        coPresences.stream()
            .limit(500)
            .forEach(cp -> System.out.println("      " + cp));
    }

}