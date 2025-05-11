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
 * Sistema centralizado para manejar los rangos de IDs de diferentes tipos de elementos.
 * Proporciona coherencia entre IDs internos y externos en todo el sistema.
 * 
 * @author Nacho Palacio
 * @version 1.0
 */
public class ElementIdMapper {
    
    // Rangos de IDs por tipo de elemento
    public static final long ROOM_ID_START = 0;          // 1-999
    public static final long ITEM_ID_START = 1000;       // 1000-1999
    public static final long DOOR_ID_START = 2000;       // 2000-2999
    public static final long STAIRS_ID_START = 3000;     // 3000-3999
    public static final long CORNER_ID_START = 4000;     // 4000-4999
    public static final long SEPARATOR_ID_START = 5000;  // 5000-5999
    public static final long USER_ID_START = 6000;       // 6000-6999 Añadido por Nacho Palacio 2025-05-07
    
    // Constantes para categorías de elementos
    public static final int CATEGORY_ROOM = 1;
    public static final int CATEGORY_ITEM = 2;
    public static final int CATEGORY_DOOR = 3;
    public static final int CATEGORY_STAIRS = 4;
    public static final int CATEGORY_CORNER = 5;
    public static final int CATEGORY_SEPARATOR = 6;
    public static final int CATEGORY_USER = 7;           // Añadido por Nacho Palacio 2025-05-07
    
    // Contadores actuales para cada tipo de elemento
    private long nextRoomId = ROOM_ID_START;
    private long nextItemId = ITEM_ID_START;
    private long nextDoorId = DOOR_ID_START;
    private long nextStairsId = STAIRS_ID_START;
    private long nextCornerId = CORNER_ID_START;
    private long nextSeparatorId = SEPARATOR_ID_START;
    
    // Mapas para conversión entre IDs internos y externos
    private Map<Long, ElementIdPair> internalToExternal;
    private Map<ElementIdPair, Long> externalToInternal;
    
    /**
     * Clase para almacenar pares de categoría y ID externo
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
    
    /**
     * Constructor por defecto
     */
    public ElementIdMapper() {
        internalToExternal = new HashMap<>();
        externalToInternal = new HashMap<>();
    }
    
    /**
     * Obtiene el siguiente ID disponible para habitaciones
     * @return ID único para una nueva habitación
     */
    public synchronized long getNextRoomId() {
        return nextRoomId++;
    }
    
    /**
     * Obtiene el siguiente ID disponible para ítems
     * @return ID único para un nuevo ítem
     */
    public synchronized long getNextItemId() {
        return nextItemId++;
    }
    
    /**
     * Obtiene el siguiente ID disponible para puertas
     * @return ID único para una nueva puerta
     */
    public synchronized long getNextDoorId() {
        return nextDoorId++;
    }
    
    /**
     * Obtiene el siguiente ID disponible para escaleras
     * @return ID único para unas nuevas escaleras
     */
    public synchronized long getNextStairsId() {
        return nextStairsId++;
    }
    
    /**
     * Obtiene el siguiente ID disponible para esquinas
     * @return ID único para una nueva esquina
     */
    public synchronized long getNextCornerId() {
        return nextCornerId++;
    }
    
    /**
     * Obtiene el siguiente ID disponible para separadores
     * @return ID único para un nuevo separador
     */
    public synchronized long getNextSeparatorId() {
        return nextSeparatorId++;
    }
    
    /**
     * Registra un elemento con un ID externo y categoría, asignándole un ID interno único
     * @param category Categoría del elemento
     * @param externalId ID externo del elemento
     * @return ID interno asignado
     */
    public synchronized long registerElement(int category, long externalId) {
        ElementIdPair externalKey = new ElementIdPair(category, externalId);
        
        // Si ya existe, devolver el ID interno existente
        if (externalToInternal.containsKey(externalKey)) {
            return externalToInternal.get(externalKey);
        }
        
        // Asignar nuevo ID interno según la categoría
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
     * Obtiene el ID interno de un elemento dada su categoría y ID externo
     * @param category Categoría del elemento
     * @param externalId ID externo del elemento
     * @return ID interno del elemento
     */
    public long getInternalId(int category, long externalId) {
        ElementIdPair externalKey = new ElementIdPair(category, externalId);
        if (!externalToInternal.containsKey(externalKey)) {
            return registerElement(category, externalId);
        }
        return externalToInternal.get(externalKey);
    }
    
    /**
     * Obtiene la información externa de un elemento dado su ID interno
     * @param internalId ID interno del elemento
     * @return Par con categoría e ID externo, o null si no existe
     */
    public ElementIdPair getExternalId(long internalId) {
        return internalToExternal.get(internalId);
    }
    
    /**
     * Obtiene la categoría de un elemento dado su ID interno
     * @param internalId ID interno del elemento
     * @return Categoría del elemento, o -1 si no existe
     */
    public int getCategory(long internalId) {
        ElementIdPair external = internalToExternal.get(internalId);
        return external != null ? external.getCategory() : -1;
    }
    
    /**
     * Verifica si un ID interno corresponde a un elemento de determinada categoría
     * @param internalId ID interno del elemento
     * @param category Categoría a verificar
     * @return true si el elemento es de la categoría especificada, false en caso contrario
     */
    public boolean isCategory(long internalId, int category) {
        return getCategory(internalId) == category;
    }
    
    /**
     * Determina la categoría de un elemento basándose en su ID interno
     * @param internalId ID interno del elemento
     * @return Categoría del elemento basada en su rango de ID
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
     * Reinicia los contadores basándose en las listas de elementos existentes
     */
    public void resetCounters(List<Room> rooms, List<Item> items, List<Door> doors, 
                            List<Stairs> stairs, List<Corner> corners, List<RoomSeparator> separators) {
        nextRoomId = ROOM_ID_START;
        nextItemId = ITEM_ID_START;
        nextDoorId = DOOR_ID_START;
        nextStairsId = STAIRS_ID_START;
        nextCornerId = CORNER_ID_START;
        nextSeparatorId = SEPARATOR_ID_START;
        
        // Actualizar contadores basados en IDs existentes
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
     * Convierte un ID existente al rango correcto según su categoría
     * @param currentId ID actual
     * @param category Categoría del elemento
     * @return ID convertido al rango correcto
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
     * Verifica si un ID está en el rango correcto para su categoría
     * @param id ID a verificar
     * @param category Categoría del elemento
     * @return true si el ID está en el rango correcto, false en caso contrario
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
     * Obtiene el ID base (externo) a partir de un ID interno
     * @param internalId ID interno del elemento
     * @return ID base (externo) del elemento
     */
    public static long getBaseId(long internalId) {
        // Para cada categoría, obtener el ID base restando el offset correspondiente
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
            // Para habitaciones o IDs desconocidos, devolver tal cual
            return internalId;
        }
    }
}