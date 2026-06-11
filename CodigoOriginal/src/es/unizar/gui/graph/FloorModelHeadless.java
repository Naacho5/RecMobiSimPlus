package es.unizar.gui.graph;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.util.Literals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless implementation of FloorModel.
 * Provides room detection logic without GUI dependencies.
 * 
 * @author Nacho Palacio
 */
public class FloorModelHeadless implements FloorModel {
    
    // Room geometry stored as simple polygons (list of points)
    private List<SimplePolygon> rooms;
    private List<Integer> roomLabels;

    private Map<Long, String> diccionaryItemLocation = new HashMap<>();
    
    // Data access
    private DataAccessRoomFile dataAccessRoomFile;
    private DataAccessItemFile dataAccessItemFile;
    
    /**
     * Simple polygon class without AWT dependencies
     */
    private static class SimplePolygon {
        private List<Point> points;
        
        public SimplePolygon() {
            this.points = new ArrayList<>();
        }
        
        public void addPoint(int x, int y) {
            points.add(new Point(x, y));
        }
        
        /**
         * Point-in-polygon test using ray casting algorithm
         */
        public boolean contains(int x, int y) {
            if (points.size() < 3) return false;
            
            boolean inside = false;
            int n = points.size();
            
            for (int i = 0, j = n - 1; i < n; j = i++) {
                Point pi = points.get(i);
                Point pj = points.get(j);
                
                if ((pi.y > y) != (pj.y > y) &&
                    (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x)) {
                    inside = !inside;
                }
            }
            
            return inside;
        }
        
        /**
         * Calculate minimum distance from point to polygon edges
         */
        public double getMinDistanceToEdge(int x, int y) {
            double minDistance = Double.MAX_VALUE;
            int n = points.size();
            
            for (int i = 0; i < n; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get((i + 1) % n);
                
                double distance = distanceToLineSegment(x, y, p1.x, p1.y, p2.x, p2.y);
                minDistance = Math.min(minDistance, distance);
            }
            
            return minDistance;
        }
        
        private double distanceToLineSegment(int px, int py, int x1, int y1, int x2, int y2) {
            double A = px - x1;
            double B = py - y1;
            double C = x2 - x1;
            double D = y2 - y1;
            
            double dot = A * C + B * D;
            double lenSq = C * C + D * D;
            double param = -1;
            
            if (lenSq != 0) {
                param = dot / lenSq;
            }
            
            double xx, yy;
            
            if (param < 0) {
                xx = x1;
                yy = y1;
            } else if (param > 1) {
                xx = x2;
                yy = y2;
            } else {
                xx = x1 + param * C;
                yy = y1 + param * D;
            }
            
            double dx = px - xx;
            double dy = py - yy;
            
            return Math.sqrt(dx * dx + dy * dy);
        }
    }
    
    /**
     * Simple point class
     */
    private static class Point {
        int x, y;
        
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * Constructor: loads room geometry from files
     */
    public FloorModelHeadless() {
        this.rooms = new ArrayList<>();
        this.roomLabels = new ArrayList<>();
        
        loadRoomData();
    }
    
    /**
     * Loads room geometry from room file
     */
    private void loadRoomData() {
        File roomFile = new File(Literals.ROOM_FLOOR_COMBINED);
        File itemFile = new File(Literals.ITEM_FLOOR_COMBINED);
        
        this.dataAccessRoomFile = new DataAccessRoomFile(roomFile);
        this.dataAccessItemFile = new DataAccessItemFile(itemFile);
        
        int numberOfRooms = dataAccessRoomFile.getNumberOfRoom();
        
        for (int r = 1; r <= numberOfRooms; r++) {
            String roomLabel = dataAccessRoomFile.getRoomLabel(r);
            
            // Create polygon for this room
            SimplePolygon polygon = new SimplePolygon();
            
            int numberOfCorners = dataAccessRoomFile.getRoomNumberCorner(r);
            for (int c = 1; c <= numberOfCorners; c++) {
                String cornerXY = dataAccessRoomFile.getRoomCornerXY(c, r);
                String[] coords = cornerXY.split(", ");
                int x = (int) Double.parseDouble(coords[0]);
                int y = (int) Double.parseDouble(coords[1]);
                polygon.addPoint(x, y);
            }
            
            rooms.add(polygon);
            roomLabels.add(Integer.parseInt(roomLabel));
        }
        loadItemLocations();
    }
    
    /**
     * Returns the room ID for given coordinates
     */
    @Override
    public int getRoomFromPosition(int x, int y) {
        if (rooms == null || rooms.isEmpty()) {
            System.err.println("[FloorModelHeadless] ERROR: rooms not loaded");
            return -1;
        }
        
        final int TOLERANCE = 20;
        
        // First pass: exact containment
        for (int r = 0; r < rooms.size(); r++) {
            if (rooms.get(r).contains(x, y)) {
                return r;
            }
        }
        
        // Second pass: proximity to edges
        int closestRoom = -1;
        double closestDistance = Double.MAX_VALUE;
        
        for (int r = 0; r < rooms.size(); r++) {
            double distance = rooms.get(r).getMinDistanceToEdge(x, y);
            
            if (distance <= TOLERANCE && distance < closestDistance) {
                closestRoom = r;
                closestDistance = distance;
            }
        }
        
        return closestRoom;
    }
    
    /**
     * Returns total number of rooms
     */
    @Override
    public int getRoomCount() {
        return rooms != null ? rooms.size() : 0;
    }


    /**
     * Loads item locations from item file and stores them in a dictionary for quick access
     */
    private void loadItemLocations() {
        int numberOfItems = dataAccessItemFile.getNumberOfItems();
        long doorID = numberOfItems + 1;
        long itemID = 0;
        long stairsID = 0;
        long invisibleDoorID = 0;
        
        List<VirtualVertex> virtualVertices = buildVirtualVertices();
        
        for (int i = 0; i < virtualVertices.size(); i++) {
            VirtualVertex vertex = virtualVertices.get(i);
            String type = vertex.type;
            String location = vertex.location;
            
            if (!type.equalsIgnoreCase("door") && 
                !type.equalsIgnoreCase("stairs") && 
                !type.equalsIgnoreCase("corner") && 
                !type.equalsIgnoreCase("invisibleDoor")) {
                
                // Es un item
                itemID = vertex.id;
                diccionaryItemLocation.put(itemID, location);
                
                long internalId = es.unizar.util.ElementIdMapper.convertToRangeId(
                    itemID, 
                    es.unizar.util.ElementIdMapper.CATEGORY_ITEM
                );
                diccionaryItemLocation.put(internalId, location);
                
            } else if (type.equalsIgnoreCase("door")) {
                diccionaryItemLocation.put(doorID, location);
                
                long internalId = es.unizar.util.ElementIdMapper.convertToRangeId(
                    doorID, 
                    es.unizar.util.ElementIdMapper.CATEGORY_DOOR
                );
                diccionaryItemLocation.put(internalId, location);
                
                doorID++;
                
            } else if (type.equalsIgnoreCase("stairs")) {
                stairsID = doorID;
                diccionaryItemLocation.put(stairsID, location);
                
                doorID++;
                
            } else if (type.equalsIgnoreCase("invisibleDoor")) {
                if (doorID > stairsID) {
                    invisibleDoorID = doorID;
                    doorID++;
                } else {
                    stairsID++;
                    invisibleDoorID = stairsID;
                }
                
                diccionaryItemLocation.put(invisibleDoorID, location);
            }
        }
    }

    /**
     * Internal class to represent virtual vertices (items, doors, stairs, corners) with their type and location
     */
    private static class VirtualVertex {
        String type;      // "item", "door", "stairs", "corner", "invisibleDoor"
        long id;          // ID del item (solo para items)
        String location;  // "x, y"
        
        VirtualVertex(String type, long id, String location) {
            this.type = type;
            this.id = id;
            this.location = location;
        }
    }

    /**
     * Builds a list of virtual vertices in the same order as DrawFloorGraph:
     */
    private List<VirtualVertex> buildVirtualVertices() {
        List<VirtualVertex> vertices = new ArrayList<>();
        int numberOfRooms = dataAccessRoomFile.getNumberOfRoom();
        int numberOfItems = dataAccessItemFile.getNumberOfItems();
        
        for (int r = 1; r <= numberOfRooms; r++) {
            String room = dataAccessRoomFile.getRoomLabel(r);
            
            // 1. Corners (esquinas)
            int numberOfCorners = dataAccessRoomFile.getRoomNumberCorner(r);
            for (int c = 1; c <= numberOfCorners; c++) {
                String location = dataAccessRoomFile.getRoomCornerXY(c, r);
                vertices.add(new VirtualVertex("corner", -1, location));
            }
            
            // 2. Doors (puertas)
            int numberOfDoors = dataAccessRoomFile.getRoomNumberDoor(r);
            for (int d = 1; d <= numberOfDoors; d++) {
                String location = dataAccessRoomFile.getRoomDoorXY(d, r);
                vertices.add(new VirtualVertex("door", -1, location));
            }
            
            // 3. Items (objetos visitables)
            for (int it = 1; it <= numberOfItems; it++) {
                String roomLabel = dataAccessItemFile.getItemRoom(it);
                if (room.equalsIgnoreCase(roomLabel)) {
                    String location = dataAccessItemFile.getVertexXY(it);
                    vertices.add(new VirtualVertex("item", it, location));
                }
            }
        }
        
        // 4. Stairs (escaleras) - DESPUÉS de todas las habitaciones
        int numberOfStairs = dataAccessRoomFile.getNumberStairs();
        for (int j = 1; j <= numberOfStairs; j++) {
            String location = dataAccessRoomFile.getStairs(j);
            if (location != null) {
                vertices.add(new VirtualVertex("stairs", -1, location));
            }
        }
        
        // 5. Invisible doors (puertas invisibles) - AL FINAL
        for (int r = 1; r <= numberOfRooms; r++) {
            int numberOfInvisibleDoors = dataAccessRoomFile.getRoomNumberInvisibleDoor(r);
            for (int invDoor = 1; invDoor <= numberOfInvisibleDoors; invDoor++) {
                String location = dataAccessRoomFile.getRoomInvisibleDoorXY(invDoor, r);
                vertices.add(new VirtualVertex("invisibleDoor", -1, location));
            }
        }
        
        return vertices;
}



    /**
     * Returns the location of the item given its ID
     * 
     * @param itemId
     * @return String location (e.g., "x, y")
     */
    @Override
    public String getItemLocation(long itemId) {
        return diccionaryItemLocation.get(itemId);
    }

    /**
     * Returns the room label given its index in the rooms list
     * 
     * @param roomIndex
     * @return int Room label || -1 if error
     */
    @Override
    public int getRoomLabel(int roomIndex) {
        if (roomLabels != null && roomIndex >= 0 && roomIndex < roomLabels.size()) {
            return roomLabels.get(roomIndex);
        }
        return -1;
    }

    /**
     * Returns the dictionary of item locations
     * 
     * @return Map<Long, String>
     */
    @Override
    public Map<Long, String> getItemLocationDictionary() {
        return diccionaryItemLocation;
    }

    /**
     * Returns the room of a graph item given its ID
     * 
     * @param itemId
     * @return String room ID || null if not found
     */
    @Override
    public String getGraphItemRoom(int itemId) {
        if (dataAccessItemFile != null) {
            return dataAccessItemFile.getItemRoom(itemId);
        }
        return null;
    }
}