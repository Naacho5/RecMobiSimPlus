package es.unizar.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
// import es.unizar.gui.simulation.Simulation.SystemRangeData;

/**
 * Centralized system to manage ID ranges for different types of elements.
 * Provides consistency between internal and external IDs throughout the system.
 * 
 * @author Nacho Palacio
 */
public class ElementIdMapper {
    
    // ID ranges for each type of element
    public static long ROOM_ID_START = 0;          // 1-999
    public static long ITEM_ID_START = 1000;       // 1000-1999
    public static long DOOR_ID_START = 2000;       // 2000-2999
    public static long STAIRS_ID_START = 3000;     // 3000-3999
    public static long CORNER_ID_START = 4000;     // 4000-4999
    public static long SEPARATOR_ID_START = 5000;  // 5000-5999
    public static long USER_ID_START = 6000;       // 6000-6999

    // Añadido por Nacho Palacio 2025-06-03
    // Flags
    private static boolean isDynamicallyConfigured = false;
    private static SystemRangeData systemData = null;
    
    // Constants for element categories
    public static final int CATEGORY_ROOM = 1;
    public static final int CATEGORY_ITEM = 2;
    public static final int CATEGORY_DOOR = 3;
    public static final int CATEGORY_STAIRS = 4;
    public static final int CATEGORY_CORNER = 5;
    public static final int CATEGORY_SEPARATOR = 6;
    public static final int CATEGORY_USER = 7;
    
    // Current counters for each type of element
    private long nextRoomId = ROOM_ID_START;
    private long nextItemId = ITEM_ID_START;
    private long nextDoorId = DOOR_ID_START;
    private long nextStairsId = STAIRS_ID_START;
    private long nextCornerId = CORNER_ID_START;
    private long nextSeparatorId = SEPARATOR_ID_START;
    
    // Maps for conversion between internal and external IDs
    private Map<Long, ElementIdPair> internalToExternal;
    private Map<ElementIdPair, Long> externalToInternal;
    
    /**
     * Class to store pairs of category and external ID
     */
    public static class ElementIdPair {
        private int category;
        private long externalId;
        
        public ElementIdPair(int category, long externalId) {
            this.category = category;
            this.externalId = externalId;
        }
        
        public int getCategory() {
            return category;
        }
        
        public long getExternalId() {
            return externalId;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ElementIdPair))
                return false;
            ElementIdPair other = (ElementIdPair) obj;
            return this.category == other.category && this.externalId == other.externalId;
        }
        
        @Override
        public int hashCode() {
            return 31 * (int) externalId + category;
        }
        
        @Override
        public String toString() {
            return "Category: " + category + ", ExternalId: " + externalId;
        }
    }
    
    public ElementIdMapper() {
        internalToExternal = new HashMap<>();
        externalToInternal = new HashMap<>();
    }
    
    /**
     * Gets the next available ID for rooms.
     * @return Unique ID for a new room.
     */
    public synchronized long getNextRoomId() {
        return nextRoomId++;
    }
    
    /**
     * Gets the next available ID for items.
     * @return Unique ID for a new item.
     */
    public synchronized long getNextItemId() {
        return nextItemId++;
    }
    
    /**
     * Gets the next available ID for doors.
     * @return Unique ID for a new door.
     */
    public synchronized long getNextDoorId() {
        return nextDoorId++;
    }
    
    /**
     * Gets the next available ID for stairs.
     * @return Unique ID for new stairs.
     */
    public synchronized long getNextStairsId() {
        return nextStairsId++;
    }
    
    /**
     * Gets the next available ID for corners.
     * @return Unique ID for a new corner.
     */
    public synchronized long getNextCornerId() {
        return nextCornerId++;
    }
    
    /**
     * Gets the next available ID for separators.
     * @return Unique ID for a new separator.
     */
    public synchronized long getNextSeparatorId() {
        return nextSeparatorId++;
    }
    
    /**
     * Registers an element with an external ID and category, assigning it a unique internal ID.
     * @param category Element category.
     * @param externalId External ID of the element.
     * @return Assigned internal ID.
     */
    public synchronized long registerElement(int category, long externalId) {
        ElementIdPair externalKey = new ElementIdPair(category, externalId);
        
        if (externalToInternal.containsKey(externalKey)) {
            return externalToInternal.get(externalKey);
        }

        long internalId;
        switch (category) {
            case CATEGORY_ROOM:
                internalId = nextRoomId++;
                break;
            case CATEGORY_ITEM:
                internalId = nextItemId++;
                break;
            case CATEGORY_DOOR:
                internalId = nextDoorId++;
                break;
            case CATEGORY_STAIRS:
                internalId = nextStairsId++;
                break;
            case CATEGORY_CORNER:
                internalId = nextCornerId++;
                break;
            case CATEGORY_SEPARATOR:
                internalId = nextSeparatorId++;
                break;
            default:
                throw new IllegalArgumentException("Categoría de elemento desconocida: " + category);
        }
        
        // Registrar en los mapas
        internalToExternal.put(internalId, externalKey);
        externalToInternal.put(externalKey, internalId);
        
        return internalId;
    }
    
    /**
     * Gets the internal ID of an element given its category and external ID.
     * @param category Element category.
     * @param externalId External ID of the element.
     * @return Internal ID of the element.
     */
    public long getInternalId(int category, long externalId) {
        ElementIdPair externalKey = new ElementIdPair(category, externalId);
        if (!externalToInternal.containsKey(externalKey)) {
            return registerElement(category, externalId);
        }
        return externalToInternal.get(externalKey);
    }
    
    /**
     * Gets the external information of an element given its internal ID.
     * @param internalId Internal ID of the element.
     * @return Pair with category and external ID, or null if it does not exist.
     */
    public ElementIdPair getExternalId(long internalId) {
        return internalToExternal.get(internalId);
    }
    
    /**
     * Gets the category of an element given its internal ID.
     * @param internalId Internal ID of the element.
     * @return Category of the element, or -1 if it does not exist.
     */
    public int getCategory(long internalId) {
        ElementIdPair external = internalToExternal.get(internalId);
        return external != null ? external.getCategory() : -1;
    }
    
    /**
     * Checks if an internal ID corresponds to an element of a specific category.
     * @param internalId Internal ID of the element.
     * @param category Category to check.
     * @return true if the element belongs to the specified category, false otherwise.
     */
    public boolean isCategory(long internalId, int category) {
        return getCategory(internalId) == category;
    }
    
    /**
     * Determines the category of an element based on its internal ID.
     * @param internalId Internal ID of the element.
     * @return Category of the element based on its ID range.
     */
    public static int determineCategoryFromInternalId(long internalId) {
        if (internalId >= ROOM_ID_START && internalId < ITEM_ID_START)
            return CATEGORY_ROOM;
        if (internalId >= ITEM_ID_START && internalId < DOOR_ID_START)
            return CATEGORY_ITEM;
        if (internalId >= DOOR_ID_START && internalId < STAIRS_ID_START)
            return CATEGORY_DOOR;
        if (internalId >= STAIRS_ID_START && internalId < CORNER_ID_START)
            return CATEGORY_STAIRS;
        if (internalId >= CORNER_ID_START && internalId < SEPARATOR_ID_START)
            return CATEGORY_CORNER;
        if (internalId >= SEPARATOR_ID_START)
            return CATEGORY_SEPARATOR;
        return -1;
    }
    
    /**
     * Resets the counters based on the lists of existing elements.
     */
    public void resetCounters(List<Room> rooms, List<Item> items, List<Door> doors, 
                            List<Stairs> stairs, List<Corner> corners, List<RoomSeparator> separators) {
        nextRoomId = ROOM_ID_START;
        nextItemId = ITEM_ID_START;
        nextDoorId = DOOR_ID_START;
        nextStairsId = STAIRS_ID_START;
        nextCornerId = CORNER_ID_START;
        nextSeparatorId = SEPARATOR_ID_START;
        
        if (rooms != null) {
            for (Room r : rooms) {
                if (r.getLabel() >= nextRoomId) nextRoomId = r.getLabel() + 1;
            }
        }
        
        if (items != null) {
            for (Item i : items) {
                if (i.getVertex_label() >= nextItemId && i.getVertex_label() < DOOR_ID_START) 
                    nextItemId = i.getVertex_label() + 1;
            }
        }
        
        if (doors != null) {
            for (Door d : doors) {
                if (d.getVertex_label() >= nextDoorId && d.getVertex_label() < STAIRS_ID_START) 
                    nextDoorId = d.getVertex_label() + 1;
            }
        }
        
        if (stairs != null) {
            for (Stairs s : stairs) {
                if (s.getVertex_label() >= nextStairsId && s.getVertex_label() < CORNER_ID_START) 
                    nextStairsId = s.getVertex_label() + 1;
            }
        }
        
        if (corners != null) {
            for (Corner c : corners) {
                if (c.getVertex_label() >= nextCornerId && c.getVertex_label() < SEPARATOR_ID_START) 
                    nextCornerId = c.getVertex_label() + 1;
            }
        }
        
        if (separators != null) {
            for (RoomSeparator s : separators) {
                if (s.getVertex_label() >= nextSeparatorId) 
                    nextSeparatorId = s.getVertex_label() + 1;
            }
        }
    }
    
    /**
     * Converts an existing ID to the correct range according to its category.
     * @param currentId Current ID.
     * @param category Element category.
     * @return ID converted to the correct range.
     */
    public static long convertToRangeId(long currentId, int category) {
        // Añadido por Nacho Palacio 2025-06-05
        if (currentId == 0) {
            new Exception("Stack trace para currentId=0").printStackTrace();
            return 0;
        }

        if (isDynamicallyConfigured) {
            int newCategory = determineActualCategory(currentId); // Añadido por Nacho Palacio 2025-06-22

            if (newCategory != category && category == CATEGORY_ITEM) {
                category = newCategory;
            }

            if (isInCorrectRange(currentId, category)) {
                return currentId;
            }
            
            switch (category) {
                case CATEGORY_ITEM:
                    if (currentId >= 1 && currentId <= systemData.totalItems) {
                        long convertedId = systemData.minItemId + currentId - 1;
                        return convertedId;
                    }
                    
                    if (currentId >= systemData.minItemId && currentId <= systemData.maxItemId) {
                        return currentId;
                    }
                    break;
                    
                case CATEGORY_DOOR:
                    int startInvisibleDoorId = systemData.totalItems + systemData.totalDoors + systemData.totalStairs; // Añadido por Nacho Palacio 2025-07-02
                    int endInvisibleDoorId = startInvisibleDoorId + systemData.totalInvisibleDoors; // Añadido por Nacho Palacio 2025-07-02
                    boolean isInvisibleDoor = currentId > startInvisibleDoorId && currentId <= endInvisibleDoorId; // REVISAR
                    boolean isDoor = currentId > systemData.totalItems && currentId <= (systemData.totalItems + systemData.totalDoors);
                    if (isDoor || isInvisibleDoor) {
                        long externalOffset = currentId - systemData.totalItems;
                        long convertedId = systemData.minDoorId + externalOffset - 1;
                        return convertedId;
                    }

                    if (currentId >= systemData.minDoorId && currentId <= systemData.maxDoorId) {
                        return currentId;
                    }
                    break;
                    
                case CATEGORY_STAIRS:
                    if (currentId >= 1 && currentId <= systemData.totalStairs) {
                        long convertedId = systemData.minStairsId + currentId - 1;
                        return convertedId;
                    }
                    
                    if (currentId >= systemData.minStairsId && currentId <= systemData.maxStairsId) {
                        return currentId;
                    }
                    break;
                    
                case CATEGORY_ROOM:
                    if (currentId >= 1 && currentId <= systemData.maxRoomId) {
                        return currentId;
                    }
                    break;
            }
        }
        
        // Fallback
        switch (category) {
            case CATEGORY_ROOM:
                if (currentId < ITEM_ID_START) return currentId;
                return currentId % (ITEM_ID_START - 1) + ROOM_ID_START;
                
            case CATEGORY_ITEM:
                if (currentId >= ITEM_ID_START && currentId < DOOR_ID_START) return currentId;
                
                if (isDynamicallyConfigured && systemData != null && 
                    currentId >= 1 && currentId <= systemData.totalItems) {
                    long convertedId = ITEM_ID_START + currentId - 1;
                    return convertedId;
                }

                return currentId % 1000 + ITEM_ID_START;
                
            case CATEGORY_DOOR:
                if (currentId >= DOOR_ID_START && currentId < STAIRS_ID_START) return currentId;

                if (isDynamicallyConfigured && systemData != null &&
                    currentId > systemData.totalItems && currentId <= (systemData.totalItems + systemData.totalDoors)) {
                    long doorIndex = currentId - systemData.totalItems;
                    long convertedId = DOOR_ID_START + doorIndex - 1;
                    return convertedId;
                }

                return currentId % 1000 + 2284; // Modificado por Nacho Palacio 2025-06-24
                // return currentId % 1000 + DOOR_ID_START; // Modificado por Nacho Palacio 2025-06-24


                
            case CATEGORY_STAIRS:
                if (currentId >= STAIRS_ID_START && currentId < CORNER_ID_START) return currentId;
                
                if (isDynamicallyConfigured && systemData != null &&
                    currentId >= 1 && currentId <= systemData.totalStairs) {
                    long convertedId = STAIRS_ID_START + currentId - 1;
                    return convertedId;
                }
                
                return currentId % 1000 + STAIRS_ID_START;
                
            case CATEGORY_CORNER:
                if (currentId >= CORNER_ID_START && currentId < SEPARATOR_ID_START) return currentId;
                return currentId % 1000 + CORNER_ID_START;
                
            case CATEGORY_SEPARATOR:
                if (currentId >= SEPARATOR_ID_START) return currentId;
                return currentId % 1000 + SEPARATOR_ID_START;
                
            default:
                return currentId;
        }
    }
    
    /**
     * Checks if an ID is in the correct range for its category.
     * @param id ID to check.
     * @param category Element category.
     * @return true if the ID is in the correct range, false otherwise.
     */
    public static boolean isInCorrectRange(long id, int category) {
        switch (category) {
            case CATEGORY_ROOM:
                return id >= ROOM_ID_START && id < ITEM_ID_START;
            case CATEGORY_ITEM:
                return id >= ITEM_ID_START && id < DOOR_ID_START;
            case CATEGORY_DOOR:
                return id >= DOOR_ID_START && id < STAIRS_ID_START;
            case CATEGORY_STAIRS:
                return id >= STAIRS_ID_START && id < CORNER_ID_START;
            case CATEGORY_CORNER:
                return id >= CORNER_ID_START && id < SEPARATOR_ID_START;
            case CATEGORY_SEPARATOR:
                return id >= SEPARATOR_ID_START;
            default:
                return false;
        }
    }

    /**
     * Gets the base (external) ID from an internal ID.
     * Modificada por Nacho Palacio 2025-06-21
     * @param internalId Internal ID of the element.
     * @return Base (external) ID of the element.
     */
    public static long getBaseId(long internalId) {
        if (isDynamicallyConfigured && systemData != null) {
            
            if (internalId >= ITEM_ID_START && internalId < DOOR_ID_START) {
                if (internalId >= systemData.minItemId && internalId <= systemData.maxItemId) {
                    return internalId - systemData.minItemId + 1;
                }

                return internalId - ITEM_ID_START + 1;
                
            } else if (internalId >= DOOR_ID_START && internalId < STAIRS_ID_START) {
                if (internalId >= systemData.minDoorId && internalId <= systemData.maxDoorId) {
                    long doorOffset = internalId - systemData.minDoorId + 1;
                    return systemData.totalItems + doorOffset;
                }

                long doorIndex = internalId - DOOR_ID_START + 1;
                return systemData.totalItems + doorIndex;
                
            } else if (internalId >= STAIRS_ID_START && internalId < CORNER_ID_START) {
                if (internalId >= systemData.minStairsId && internalId <= systemData.maxStairsId) {
                    return internalId - STAIRS_ID_START + systemData.minStairsId;
                }

                return internalId - STAIRS_ID_START + 1;
                
            } else if (internalId >= CORNER_ID_START && internalId < SEPARATOR_ID_START) {
                return internalId - CORNER_ID_START + 1;
                
            } else if (internalId >= SEPARATOR_ID_START) {
                return internalId - SEPARATOR_ID_START + 1;
                
            } else if (internalId >= ROOM_ID_START && internalId < ITEM_ID_START) {
                return internalId;
            }
        }
        
        if (internalId >= ITEM_ID_START && internalId < DOOR_ID_START) {
            return internalId - ITEM_ID_START + 1;
        } else if (internalId >= DOOR_ID_START && internalId < STAIRS_ID_START) {
            return internalId - DOOR_ID_START + 1;
        } else if (internalId >= STAIRS_ID_START && internalId < CORNER_ID_START) {
            return internalId - STAIRS_ID_START + 1;
        } else if (internalId >= CORNER_ID_START && internalId < SEPARATOR_ID_START) {
            return internalId - CORNER_ID_START + 1;
        } else if (internalId >= SEPARATOR_ID_START) {
            return internalId - SEPARATOR_ID_START + 1;
        } else {
            return internalId;
        }
    }

    /**
     * Configura los rangos dinámicamente basándose en datos reales del sistema.
     * Añadido por Nacho Palacio 2025-06-03.
     * Modificado por Nacho Palacio 2025-06-25.
     * @param data Datos reales del sistema.
     */
    public static void configureDynamicRanges(Object rangeData) {
        
        // Verificar si ya está configurado
        if (isDynamicallyConfigured) {
            System.out.println("⚠️  ElementIdMapper ya está configurado dinámicamente - omitiendo reconfiguración");
            return;
        }
        
        try {
            processRangeDataReflection(rangeData);
            isDynamicallyConfigured = true;
        } catch (Exception e) {
            System.err.println(" ERROR en configuración dinámica: " + e.getMessage());
        }
    }

    
    /**
     * Determina la categoría real de un ID basándose en los datos del sistema.
     * @param currentId ID a analizar
     * @return Categoría real del elemento
     */
    private static int determineActualCategory(long currentId) {
        if (systemData == null) {
            return CATEGORY_ITEM;
        }

        if (currentId >= CORNER_ID_START) {
            if (currentId >= SEPARATOR_ID_START) {
                return CATEGORY_SEPARATOR;
            } else {
                return CATEGORY_CORNER;
            }
        }

        // Version simplificada para determinar la categoría real
        if (currentId >= 1 && currentId <= systemData.totalItems) {
                return CATEGORY_ITEM;
        }  
        else {
            return CATEGORY_DOOR;
        }
    }

    /**
     * Procesa los datos de rango usando reflexión para evitar dependencias circulares.
     * AÑADIDO por Nacho Palacio 2025-06-25.
     */
    private static void processRangeDataReflection(Object rangeData) throws Exception {
        Class<?> dataClass = rangeData.getClass();

        systemData = new SystemRangeData();
        
        // Obtener campos usando reflexión
        int totalItems = getIntField(dataClass, rangeData, "totalItems");
        int totalDoors = getIntField(dataClass, rangeData, "totalDoors");
        int totalStairs = getIntField(dataClass, rangeData, "totalStairs");
        int totalRooms = getIntField(dataClass, rangeData, "totalRooms");
        int totalInvisibleDoors = countInvisibleDoors(); // Añadido por Nacho Palacio 2025-07-02 
        
        long minItemId = getLongField(dataClass, rangeData, "minItemId");
        long maxItemId = getLongField(dataClass, rangeData, "maxItemId");
        long minDoorId = getLongField(dataClass, rangeData, "minDoorId");
        long maxDoorId = getLongField(dataClass, rangeData, "maxDoorId");
        long minStairsId = getLongField(dataClass, rangeData, "minStairsId");
        long maxStairsId = getLongField(dataClass, rangeData, "maxStairsId");
        long minExternalStairsId = getLongField(dataClass, rangeData, "minExternalStairsId");
        long maxExternalStairsId = getLongField(dataClass, rangeData, "maxExternalStairsId");
        long minRoomId = getLongField(dataClass, rangeData, "minRoomId");
        long maxRoomId = getLongField(dataClass, rangeData, "maxRoomId");

        systemData.totalItems = totalItems;
        systemData.totalDoors = totalDoors;
        systemData.totalStairs = totalStairs;
        systemData.totalRooms = totalRooms;
        systemData.totalInvisibleDoors = totalInvisibleDoors;
        
        systemData.minItemId = minItemId;
        systemData.maxItemId = maxItemId;
        systemData.minDoorId = minDoorId;
        systemData.maxDoorId = maxDoorId;
        systemData.minStairsId = minStairsId;
        systemData.maxStairsId = maxStairsId;
        systemData.minExternalStairsId = minExternalStairsId;
        systemData.maxExternalStairsId = maxExternalStairsId;
        systemData.minRoomId = minRoomId;
        systemData.maxRoomId = maxRoomId;


        ROOM_ID_START = 1;
        
        if (totalItems > 0 && minItemId < Long.MAX_VALUE) {
            ITEM_ID_START = minItemId;
        }
        
        if (totalDoors > 0 && minDoorId < Long.MAX_VALUE) {
            DOOR_ID_START = minDoorId;
        }
        
        if (totalStairs > 0 && minStairsId < Long.MAX_VALUE) {
            STAIRS_ID_START = minStairsId;
        }
        
        long maxSystemId = Math.max(Math.max(maxItemId, maxDoorId), maxStairsId);
        if (maxSystemId > 0 && maxSystemId != Long.MIN_VALUE) {
            long safetyMargin = 1000;
            CORNER_ID_START = maxSystemId + safetyMargin;
            SEPARATOR_ID_START = CORNER_ID_START + 1000;
            USER_ID_START = SEPARATOR_ID_START + 1000;
        }
    }

    /**
     * Clase interna para evitar dependencias circulares.
     * AÑADIDA por Nacho Palacio 2025-06-25.
     */
    public static class SystemRangeData {
        public int totalItems = 0;
        public int totalDoors = 0;
        public int totalStairs = 0;
        public int totalRooms = 0;
        public int totalInvisibleDoors = 0;
        
        
        public long minItemId = Long.MAX_VALUE;
        public long maxItemId = Long.MIN_VALUE;
        
        public long minDoorId = Long.MAX_VALUE;
        public long maxDoorId = Long.MIN_VALUE;

        public long minStairsId = Long.MAX_VALUE;
        public long maxStairsId = Long.MIN_VALUE;

        public long minExternalStairsId = Long.MAX_VALUE;
        public long maxExternalStairsId = Long.MIN_VALUE;

        public long minRoomId = 1;
        public long maxRoomId = 1;

        
    }

    public static SystemRangeData getSystemRangeData() { return systemData; }

    /**
     * Métodos auxiliares para reflexión.
     * Añadido por Nacho Palacio 2025-06-25.
     */
    private static int getIntField(Class<?> clazz, Object obj, String fieldName) throws Exception {
        return (Integer) clazz.getField(fieldName).get(obj);
    }

    private static long getLongField(Class<?> clazz, Object obj, String fieldName) throws Exception {
        return (Long) clazz.getField(fieldName).get(obj);
    }

    /**
     * Verifica si ElementIdMapper ya está configurado dinámicamente.
     * Añadido por Nacho Palacio 2025-06-25.
     */
    public static boolean isDynamicallyConfigured() {
        return isDynamicallyConfigured;
    }

    /**
     * Cuenta el número total de puertas invisibles en el sistema.
     * Añadido por Nacho Palacio 2025-07-02.
     * 
     * @return Número total de puertas invisibles
     */
    private static int countInvisibleDoors() {
        int totalInvisibleDoors = 0;
        
        try {
            es.unizar.access.DataAccessGraphFile dataAccessGraphFile = new es.unizar.access.DataAccessGraphFile(new java.io.File(es.unizar.util.Literals.GRAPH_FLOOR_COMBINED));
            
            int totalRooms = dataAccessGraphFile.getNumberOfRoom();
            
            for (int roomId = 1; roomId <= totalRooms; roomId++) {
                try {
                    int numSubrooms = dataAccessGraphFile.getRoomNumberSubrooms(roomId);
                    
                    for (int subRoomPos = 1; subRoomPos <= numSubrooms; subRoomPos++) {
                        try {
                            int numInvisibleDoors = dataAccessGraphFile.getNumberOfInvisibleDoorsBySubroom(subRoomPos, roomId);
                            totalInvisibleDoors += numInvisibleDoors;
                        } catch (Exception e) {
                            // Continuar con la siguiente subhabitación
                        }
                    }
                } catch (Exception e) {
                    // Continuar con la siguiente habitación
                }
            } 
        } catch (Exception e) {
            System.err.println("Error contando puertas invisibles: " + e.getMessage());
            return 74; // Valor por defecto para gran casa
        }
        
        return totalInvisibleDoors;
    }

}