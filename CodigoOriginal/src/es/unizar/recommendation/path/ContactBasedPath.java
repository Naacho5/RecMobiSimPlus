package es.unizar.recommendation.path;

import java.util.*;
import java.util.stream.Collectors;

import es.unizar.epidemic.data.ContactTrajectoryBuilder;
import es.unizar.gui.Configuration;
import es.unizar.util.ElementIdMapper;


/**
 * Generates paths based on user contact events
 * Reuses the logic of RandomPath/NearestPath to generate coherent paths
 * 
 * @author Nacho Palacio
 */
public class ContactBasedPath extends Path {
    
    private Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> trajectories;
    private int userId;
    private static final double MAX_BASE_EVENT_TIME_SECONDS = 0.0;
    
    public ContactBasedPath(
            Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> trajectories, 
            int userId) {
        super();
        this.trajectories = trajectories;
        this.userId = userId;
    }

    private boolean isBaseEvent(ContactTrajectoryBuilder.UserRoomEvent event) {
        return event != null &&
            event.scope != null &&
            event.scope.equalsIgnoreCase("BASE");
    }
    
    /**
     * Generates a path based on user contact events
     * 
     * @param startVertex the starting vertex (item) for the path
     * @return the generated path as a string of edges
     */
    @Override
    public String generatePath(long startVertex) {
        // startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);
        if (!ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM) &&
            !ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR) &&
            !ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_STAIRS)) {
            startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);
        }

        StringBuilder pathBuilder = new StringBuilder();
        
        // Get user events (already sorted)
        List<ContactTrajectoryBuilder.UserRoomEvent> events = trajectories.get(userId);
        
        if (events == null || events.isEmpty()) {
            System.err.println("    User " + userId + " without contact events");
            return "";
        }
        
        
        // Store the visited items (sculptures and paintings).
        List<Long> itemVisited = new LinkedList<>();
        // Store the visited rooms.
        List<Integer> roomVisited = new LinkedList<>();
        double currentTime = 0;
        // Time for the paths (of hour to second).
        double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
        int numberOfItemsInMuseum = accessItemFile.getNumberOfItems();
        
        // Get the items (sculptures, paintings and doors) from a specified room.
        Map<Object, Object> itemsDoorVisited_cloned = itemsDoorVisited.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<Object>(e.getValue())));
        
        // Add initial vertex to the path (same as RandomPath)
        long currentVertex = startVertex;
        itemVisited.add(currentVertex);
        
        // Iterate over contact events
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            System.out.println("Processing event " + (eventIndex + 1) + "/" + events.size() + " for user " + userId);

            ContactTrajectoryBuilder.UserRoomEvent currentEvent = events.get(eventIndex);
            int currentRoom = currentEvent.roomId;

            // Added by Nacho Palacio 2025-11-25
            int effectiveRoom = currentRoom; // Room to use (can be redirected)
            System.out.println("Event " + (eventIndex + 1) + " - Effective room: " + effectiveRoom);
            currentRoom = effectiveRoom;

            // roomVisited.add(currentRoom);
            if (roomVisited.isEmpty() || roomVisited.get(roomVisited.size() - 1) != currentRoom) {
                System.out.println("Visited new room: " + currentRoom);
                roomVisited.add(currentRoom);
            }

            double eventDuration = currentEvent.endTime - currentEvent.startTime;
            if (eventDuration <= 0) {
                System.out.println("      Warning! Event with non-positive duration: " + eventDuration + " seconds");
                continue;
            }

            boolean isBaseEvent = isBaseEvent(currentEvent);

            // BASE = waiting state: no item visits, only possible transition to next room
            if (isBaseEvent) {
                System.out.println("      Base event detected. Duration: " + eventDuration + " seconds");
                if (eventIndex < events.size() - 1) {
                    ContactTrajectoryBuilder.UserRoomEvent nextEvent = events.get(eventIndex + 1);
                    int nextRoom = nextEvent.roomId;

                    if (currentRoom > 0 && nextRoom > 0 && currentRoom != nextRoom) {
                        System.out.println("      Transition from room " + currentRoom + " to room " + nextRoom);
                        int indexDoor = isDirectlyConnected(currentRoom, nextRoom);

                        if (indexDoor == -1) {
                            List<Integer> roomPath = findRoomPathBFS(currentRoom, nextRoom);
                            System.out.println("DEBUG roomPath (BASE) = " + roomPath);

                            if (roomPath == null || roomPath.size() < 2) {
                                System.err.println(" Route not found between rooms " + currentRoom + " and " + nextRoom);
                            } else {
                                for (int i = 0; i < roomPath.size() - 1; i++) {
                                    int fromRoom = roomPath.get(i);
                                    int toRoom = roomPath.get(i + 1);
                                    long doorId = findConnectingDoor(fromRoom, toRoom);
                                    long connectedDoor = getConnectedDoor(doorId);

                                    System.out.println(
                                        "DEBUG BFS step (BASE): " + fromRoom + " -> " + toRoom +
                                        " doorId=" + doorId +
                                        " connectedDoor=" + connectedDoor +
                                        " connectedDoorRoom=" + getRoomFromItem(connectedDoor)
                                    );

                                    if (doorId > 0 && connectedDoor > 0) {
                                        String transition = getToConnectedDoor(currentVertex, doorId, itemVisited, connectedDoor);
                                        pathBuilder.append(transition);
                                        currentTime += getCurrentTimeConnectedDoors(transition);
                                        currentVertex = getEndVertex(transition);
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } else {
                            try {
                                System.out.println("      Direct connection found between rooms " + currentRoom + " and " + nextRoom + " through door index " + indexDoor);
                                int numDoors = accessGraphFile.getNumDoorsByRoom(currentRoom);
                                if (numDoors > 0) {
                                    System.out.println("      Number of doors in current room: " + numDoors);
                                    long doorId = accessGraphFile.getDoorOfRoomWithIndex(indexDoor, currentRoom);
                                    long connectedDoor = getConnectedDoor(doorId);
                                    int connectedDoorRoom = getRoomFromItem(connectedDoor);

                                    if (connectedDoorRoom == nextRoom) {
                                        String transition = getToConnectedDoor(currentVertex, doorId, itemVisited, connectedDoor);
                                        pathBuilder.append(transition);
                                        currentTime += getCurrentTimeConnectedDoors(transition);
                                        currentVertex = getEndVertex(transition);
                                    }
                                }
                            } catch (Exception e) {
                                // Ignore transition error in BASE events
                            }
                        }
                    }
                }

                continue;
            }

            LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(currentRoom);
            
            if (itemsByRoom == null) {
                System.out.println("      Warning! Room " + currentRoom + " without items in cache");
                itemsByRoom = new LinkedList<>();
                itemsDoorVisited_cloned.put(currentRoom, itemsByRoom);
            }

            itemsByRoom = convertAndValidateItems(itemsByRoom, "room_" + currentRoom, numberOfItemsInMuseum);
            
            // Added by Nacho Palacio 2025-12-03
            LinkedList<Long> itemsOnlyInRoom = new LinkedList<>();
            LinkedList<Long> doorsOnlyInRoom = new LinkedList<>();
            
            for (Long element : itemsByRoom) {
                if (ElementIdMapper.isInCorrectRange(element, ElementIdMapper.CATEGORY_ITEM)) {
                    itemsOnlyInRoom.add(element);
                } else if (ElementIdMapper.isInCorrectRange(element, ElementIdMapper.CATEGORY_DOOR) ||
                        ElementIdMapper.isInCorrectRange(element, ElementIdMapper.CATEGORY_STAIRS)) {
                    doorsOnlyInRoom.add(element);
                }
            }

            double timeInRoom = Math.max(0, Math.min(eventDuration, inputTime));
            double startTimeInRoom = currentTime;

            double timePerItem;
            if (Configuration.instance != null && 
                Configuration.instance.getContactTrajectoryMode() == Configuration.ContactTrajectoryMode.REAL_CHRONOLOGY) {
                // REAL_CHRONOLOGY: distribute event time among all items in the room
                int availableItems = itemsOnlyInRoom.size();
                timePerItem = (availableItems > 0) ? (timeInRoom / availableItems) : timeInRoom;
                System.out.println("      REAL_CHRONOLOGY mode: " + availableItems + " items available, " + 
                                   String.format("%.2f", timePerItem) + " seconds per item");
            } else {
                // Traditional modes: use delayObservingPainting
                System.out.println("      TRADITIONAL mode: using fixed time per item: " + Configuration.simulation.getDelayObservingPaintingInSecond() + " seconds");
                timePerItem = Configuration.simulation.getDelayObservingPaintingInSecond();
            }
            
            // Visit items inside the room (RandomPath LOGIC)
            while (currentTime - startTimeInRoom < timeInRoom && 
                itemVisited.size() < numberOfItems && 
                !itemsOnlyInRoom.isEmpty()) {
                
                // double random = Math.random();
                // int indexRandom = (int) (random * itemsOnlyInRoom.size());
                int indexRandom = Configuration.simulation.random.nextInt(itemsOnlyInRoom.size()); // MOdificado por Nacho Palacio 2026-06-04
                long itemToVisit = itemsOnlyInRoom.get(indexRandom);
                
                if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM)) {
                    itemsOnlyInRoom.remove(itemToVisit);
                    itemsByRoom.remove(itemToVisit);
                    
                    String edge = getCurrentVertex(currentVertex, itemToVisit);
                    pathBuilder.append(edge);
                    
                    itemVisited.add(itemToVisit);
                    // currentTime += getCurrentTime(currentVertex, itemToVisit) + 
                    //             Configuration.simulation.getDelayObservingPaintingInSecond();
                    currentTime += getCurrentTime(currentVertex, itemToVisit) + timePerItem;
                    
                    System.out.println("      Visiting item " + itemToVisit + " in room " + currentRoom + 
                        " - Current time in room: " + (currentTime - startTimeInRoom) + "/" + timeInRoom + " seconds, currentTime: " + currentTime + " seconds");
                    
                    currentVertex = itemToVisit;
                } else {
                    // If for some reason there's a door, remove it
                    System.out.println("         Warning! Unexpected element in items pool: " + itemToVisit);
                    itemsOnlyInRoom.remove(itemToVisit);
                }

            }

            // Perform transition between rooms when event ends
            if (eventIndex < events.size() - 1) {
                ContactTrajectoryBuilder.UserRoomEvent nextEvent = events.get(eventIndex + 1);
                int nextRoom = nextEvent.roomId;

                // if (currentRoom != nextRoom) {
                if (currentRoom > 0 && nextRoom > 0 && currentRoom != nextRoom) {
                   // Search for a door connecting both rooms
                    int indexDoor = isDirectlyConnected(currentRoom, nextRoom);
                    System.out.println("      Transition to next room " + nextRoom + " - Direct connection index: " + indexDoor);
                    if (indexDoor == -1) {
                        System.out.println("      No direct connection found between rooms " + currentRoom + " and " + nextRoom + ". Searching for path...");
                        StringBuilder debugPath = new StringBuilder();
                        List<Integer> roomPath = findRoomPathBFS(currentRoom, nextRoom);
                        System.out.println("DEBUG roomPath (END_EVENT) = " + roomPath);

                        if (roomPath == null || roomPath.size() < 2) {
                        System.out.println("          Route not found between rooms " + currentRoom + " and " + nextRoom);
                        } else {
                            for (int i = 0; i < roomPath.size() - 1; i++) {
                                int fromRoom = roomPath.get(i);
                                int toRoom = roomPath.get(i + 1);
                                long doorId = findConnectingDoor(fromRoom, toRoom);
                                long connectedDoor = getConnectedDoor(doorId);

                                System.out.println(
                                    "DEBUG BFS step (END_EVENT): " + fromRoom + " -> " + toRoom +
                                    " doorId=" + doorId +
                                    " connectedDoor=" + connectedDoor +
                                    " connectedDoorRoom=" + getRoomFromItem(connectedDoor)
                                );

                                if (doorId > 0 && connectedDoor > 0) {
                                    String transition = getToConnectedDoor(currentVertex, doorId, itemVisited, connectedDoor);
                                    pathBuilder.append(transition);
                                    debugPath.append(transition);
                                    currentTime += getCurrentTimeConnectedDoors(transition);
                                    currentVertex = getEndVertex(transition);
                                } 
                                else {
                                    break;
                                }
                            }
                        }
                    } else {
                        // Rooms are connected, perform direct transition between rooms
                       try {
                            System.out.println("      Direct connection found between rooms " + currentRoom + " and " + nextRoom + " through door index " + indexDoor);
                            int numDoors = accessGraphFile.getNumDoorsByRoom(currentRoom);
                            if (numDoors > 0) {
                                long doorId = accessGraphFile.getDoorOfRoomWithIndex(indexDoor, currentRoom);
                                long connectedDoor = getConnectedDoor(doorId);
                                int connectedDoorRoom = getRoomFromItem(connectedDoor);

                                if (connectedDoorRoom == nextRoom) {
                                    String transition = getToConnectedDoor(currentVertex, doorId, itemVisited, connectedDoor);
                                    pathBuilder.append(transition);
                                    currentTime += getCurrentTimeConnectedDoors(transition);
                                    currentVertex = getEndVertex(transition);
                                }
                            }
                        } catch (Exception e) {
                            // System.err.println("          Error generating transition: " + e.getMessage());
                        }
                    }
                } else {
                    //System.out.println("Both rooms are: " + currentRoom);
                }
            }
        }
        
        // Remove repeated objects in the path
        String finalPath = pathBuilder.toString();
        
        finalPath = eraseRepeatedObjects(finalPath);

        // Remove the last comma
        if (finalPath.length() >= 2) {
            System.out.println("Generated path for user " + userId + ": " + finalPath);
            return finalPath.substring(0, finalPath.length() - 2);
        }

        System.out.println("Generated path for user " + userId + ": " + finalPath);
        
        return finalPath;
    }

    /**
     * Verifies if two rooms are directly connected by a door
     * 
     * @param fromRoom the starting room
     * @param toRoom the destination room
     * @return the index of the connecting door, or -1 if not connected
     */
    private int isDirectlyConnected(int fromRoom, int toRoom) {
        int numDoors = accessGraphFile.getNumDoorsByRoom(fromRoom);
        for (int i = 1; i <= numDoors; i++) {
            long doorId = accessGraphFile.getDoorOfRoomWithIndex(i, fromRoom);
            long connectedDoor = getConnectedDoor(doorId);
            if (getRoomFromItem(connectedDoor) == toRoom) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds a door connecting two rooms
     * 
     * @param startRoom the starting room
     * @param endRoom the destination room
     * @return the list of room IDs forming the path, or null if no path found
     */
    private List<Integer> findRoomPathBFS(int startRoom, int endRoom) {
        Queue<List<Integer>> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(Arrays.asList(startRoom));
        visited.add(startRoom);

        while (!queue.isEmpty()) {
            List<Integer> path = queue.poll();
            int lastRoom = path.get(path.size() - 1);
            if (lastRoom == endRoom) return path;

            for (int neighbor : getNeighborRooms(lastRoom)) {
                if (!visited.contains(neighbor)) {
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                    visited.add(neighbor);
                }
            }
        }
        return null; // No path found
    }

    /**
     * Gets neighboring rooms connected by doors
     * 
     * @param room the room ID
     * @return list of neighboring room IDs
     */
    private List<Integer> getNeighborRooms(int room) {
        List<Integer> neighbors = new ArrayList<>();
        int numDoors = accessGraphFile.getNumDoorsByRoom(room);
        for (int i = 1; i <= numDoors; i++) {
            long doorId = accessGraphFile.getDoorOfRoomWithIndex(i, room);
            long connectedDoor = getConnectedDoor(doorId);
            int neighborRoom = getRoomFromItem(connectedDoor);
            if (neighborRoom > 0 && neighborRoom != room) {
                neighbors.add(neighborRoom);
            }
        }
        return neighbors;
    }

    /**
     * Finds a door connecting two specific rooms
     * 
     * @param fromRoom the starting room
     * @param toRoom the destination room
     * @return the door ID connecting the rooms, or -1 if not found
     */
    private long findConnectingDoor(int fromRoom, int toRoom) {
        int numDoors = accessGraphFile.getNumDoorsByRoom(fromRoom);
        for (int i = 1; i <= numDoors; i++) {
            long doorId = this.accessGraphFile.getDoorOfRoomWithIndex(i, fromRoom);
            long connectedDoor = getConnectedDoor(doorId);
            if (getRoomFromItem(connectedDoor) == toRoom) {
                return doorId;
            }
        }
        return -1;
    }
}