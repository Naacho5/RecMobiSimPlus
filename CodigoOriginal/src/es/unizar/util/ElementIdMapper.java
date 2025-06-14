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

/**
 * Centralized system to manage ID ranges for different types of elements.
 * Provides consistency between internal and external IDs throughout the system.
 * 
 * @author Nacho Palacio
 */
public class ElementIdMapper {
    
    // ID ranges for each type of element
    public static final long ROOM_ID_START = 0;          // 1-999
    public static final long ITEM_ID_START = 1000;       // 1000-1999
    public static final long DOOR_ID_START = 2000;       // 2000-2999
    public static final long STAIRS_ID_START = 3000;     // 3000-3999
    public static final long CORNER_ID_START = 4000;     // 4000-4999
    public static final long SEPARATOR_ID_START = 5000;  // 5000-5999
    public static final long USER_ID_START = 6000;       // 6000-6999
    
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
        switch (category) {
            case CATEGORY_ROOM:
                if (currentId < ITEM_ID_START) return currentId;
                return currentId % (ITEM_ID_START - 1) + ROOM_ID_START;
            case CATEGORY_ITEM:
                if (currentId >= ITEM_ID_START && currentId < DOOR_ID_START) return currentId;
                return currentId % 1000 + ITEM_ID_START;
            case CATEGORY_DOOR:
                if (currentId >= DOOR_ID_START && currentId < STAIRS_ID_START) return currentId;
                return currentId % 1000 + DOOR_ID_START;
            case CATEGORY_STAIRS:
                if (currentId >= STAIRS_ID_START && currentId < CORNER_ID_START) return currentId;
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
     * @param internalId Internal ID of the element.
     * @return Base (external) ID of the element.
     */
    public static long getBaseId(long internalId) {
        if (internalId >= ITEM_ID_START && internalId < DOOR_ID_START) {
            return internalId - ITEM_ID_START;
        } else if (internalId >= DOOR_ID_START && internalId < STAIRS_ID_START) {
            return internalId - DOOR_ID_START;
        } else if (internalId >= STAIRS_ID_START && internalId < CORNER_ID_START) {
            return internalId - STAIRS_ID_START;
        } else if (internalId >= CORNER_ID_START && internalId < SEPARATOR_ID_START) {
            return internalId - CORNER_ID_START;
        } else if (internalId >= SEPARATOR_ID_START) {
            return internalId - SEPARATOR_ID_START;
        } else {
            return internalId;
        }
    }
}