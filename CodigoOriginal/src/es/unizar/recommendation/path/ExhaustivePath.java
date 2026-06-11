package es.unizar.recommendation.path;

import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import es.unizar.gui.Configuration;
import es.unizar.util.ElementIdMapper;

public class ExhaustivePath extends Path {

    // Rooms connected by doors.
    public static String[] roomsConnectedByDoorsInitial = { "1-2;243", "2-3;246", "3-4;248", "4-5;250,249", "5-6;252",
            "6-7;256", "7-8;259", "8-9;264", "9-10;267", "10-11;269", "11-12;271,270", "12-13;274", "13-14;277,273,262",
            "14-15;278,261,274,275", "15-16;281", "16-17;284,287", "17-18;286", "18-19;289", "19-20;291", "20-21;293",
            "21-22;295", "22-23;299,296", "23-24;301", "24-25;304", "25-26;307,298", "26-1;310,297,308" };

    // Rooms connected by doors GC.
    public static String[] roomsConnectedByDoorsInitialGC = { "1-2;243", "2-3;246", "3-4;248", "4-5;250,249", "5-6;252",
            "6-7;256", "7-8;259", "8-9;264", "9-10;267", "10-11;269", "11-12;271,270", "12-13;274", "13-14;277,273,262",
            "14-15;278,261,274,275", "15-16;281", "16-17;284,287", "17-18;286", "18-19;289", "19-20;291", "20-21;293",
            "21-22;295", "22-23;299,296", "23-24;301", "24-25;304", "25-26;307,298", "26-1;310,297,308" };

    public ExhaustivePath() {
        super();
    }

    /**
     * Generate a non-RS user path, applying the sequential strategy and
     * considering the specified time (in the Configuration form).
     *
     * @param startVertex the initial position (it will be the position of an item,
     *                    chosen randomly) of the non-RS user.
     */
    @Override
    public String generatePath(long startVertex) {
        startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);

        String finalPath = "";
        long endVertex = 0;
        double currentTime = 0;
        long itemToVisit = 0;
        String[] roomsConnectedByDoors = new String[roomsConnectedByDoorsInitial.length];

        int room = getRoomFromItem(startVertex);
        roomsConnectedByDoors = orderByCurrentPosition(room, roomsConnectedByDoorsInitial);

        double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
        int numberOfItemsInMuseum = accessItemFile.getNumberOfItems();
        itemToVisit = startVertex;

        Map<Integer, LinkedList<Long>> itemsDoorVisitedCloned = itemsDoorVisited.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<>(e.getValue())));

        for (int r = 0; r < roomsConnectedByDoors.length; r++) {
            if (currentTime >= inputTime) {
                break;
            }

            String[] rooms = roomsConnectedByDoors[r].split(";");
            room = Integer.valueOf(rooms[0].split("-")[0]).intValue();

            LinkedList<Long> itemsByRoomTemp = itemsDoorVisitedCloned.get(room);
            if (itemsByRoomTemp == null) {
                itemsByRoomTemp = new LinkedList<>();
                itemsDoorVisitedCloned.put(room, itemsByRoomTemp);
            }

            @SuppressWarnings("unchecked")
            LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsByRoomTemp.clone();

            itemsByRoom = convertAndValidateItems(itemsByRoom, "itemsByRoom_room_" + room, numberOfItemsInMuseum);
            if (itemsByRoom == null) {
                itemsByRoom = new LinkedList<>();
            }

            if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM)) {
                itemsByRoom.remove(startVertex);
            }

            for (int i = 0; i < itemsByRoom.size(); i++) {
                if (currentTime >= inputTime) {
                    break;
                }

                itemToVisit = itemsByRoom.get(i);

                if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM)) {
                    endVertex = itemToVisit;
                    String vertex = getCurrentVertex(startVertex, endVertex);
                    finalPath += vertex;

                    currentTime += getCurrentTime(startVertex, endVertex)
                            + Configuration.simulation.getDelayObservingPaintingInSecond();

                    startVertex = endVertex;
                } else if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR)
                        || ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_STAIRS)) {

                    startVertex = endVertex;

                    String[] doors = rooms[1].split(",");
                    for (int d = 0; d < doors.length; d++) {
                        if (currentTime >= inputTime) {
                            break;
                        }

                        long rawDoorId = Long.valueOf(doors[d]).longValue();
                        itemToVisit = ElementIdMapper.convertToRangeId(rawDoorId, ElementIdMapper.CATEGORY_DOOR);
                        endVertex = itemToVisit;

                        String vertex = getCurrentVertex(startVertex, endVertex);
                        finalPath += vertex;

                        if (checkDoorsConnectedByStairs(startVertex, endVertex)) {
                            currentTime += 60;
                        } else {
                            currentTime += getCurrentTime(startVertex, endVertex);
                        }

                        startVertex = endVertex;

                        if (room == 26 && ElementIdMapper.getBaseId(startVertex) == 308) {
                            itemToVisit = ElementIdMapper.convertToRangeId(242, ElementIdMapper.CATEGORY_DOOR);
                        } else if (room == 14 && ElementIdMapper.getBaseId(startVertex) == 275) {
                            itemToVisit = ElementIdMapper.convertToRangeId(280, ElementIdMapper.CATEGORY_DOOR);
                        } else {
                            long connectedDoor = getConnectedDoor(startVertex);
                            if (connectedDoor <= 0) {
                                break;
                            }
                            itemToVisit = connectedDoor;
                        }

                        endVertex = itemToVisit;
                        vertex = getCurrentVertex(startVertex, endVertex);
                        finalPath += vertex;

                        if (checkDoorsConnectedByStairs(startVertex, endVertex)) {
                            currentTime += 60;
                        } else {
                            currentTime += getCurrentTime(startVertex, endVertex);
                        }

                        startVertex = endVertex;
                    }

                    break;
                }
            }
        }

        if (finalPath.length() < 2) {
            return finalPath;
        }

        return finalPath.substring(0, finalPath.length() - 2);
    }

    /**
     * Sorts the rooms taking into account the current position of the non-special
     * user.
     *
     * @param room The current room of the non-special user.
     * @param roomsConnectedByDoorsInitial Rooms connected by doors.
     * @return An array with the sorted rooms.
     */
    public String[] orderByCurrentPosition(int room, String[] roomsConnectedByDoorsInitial) {
        String[] roomsConnectedByDoors = new String[roomsConnectedByDoorsInitial.length];
        int pos = 0;

        for (int i = 0; i < roomsConnectedByDoorsInitial.length; i++) {
            String text = roomsConnectedByDoorsInitial[i];
            if (text.startsWith(String.valueOf(room))) {
                pos = i;
                break;
            }
        }

        int j = 0;
        for (int i = pos; i < roomsConnectedByDoorsInitial.length; i++) {
            roomsConnectedByDoors[j] = roomsConnectedByDoorsInitial[i];
            j++;
        }

        for (int i = 0; i < pos; i++) {
            roomsConnectedByDoors[j] = roomsConnectedByDoorsInitial[i];
            j++;
        }

        return roomsConnectedByDoors;
    }
}