package es.unizar.recommendation.path;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.unizar.access.DataAccessItemFile;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.util.ElementIdMapper;

public class RandomPath extends Path {

	public RandomPath() {
		super();
	}

	/**
	 * Generate a non-RS user path, applying the random strategy and
	 * considering the specified time (in the Configuration form).
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String generatePath(long startVertex) {

		// Añadido por Nacho Palacio 2025-04-23.
		// Verificar si el startVertex está en el formato interno correcto
		// Si no lo está, convertirlo al formato interno
		long originalId = startVertex;
		startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);
		if (originalId != startVertex) {
			System.out.println("RandomPath: Usando ID interno " + startVertex + " (original: " + originalId + ")");
		}


		// Añadido por Nacho Palacio 2025-04-22.
		System.out.println("RandomPath: Generando ruta para item interno " + startVertex + 
                          " (rango correcto: " + ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM) + ")");
					
		String finalPath = "";
		long endVertex = 0;
		long itemToVisit = 0;
		// Store the visited rooms.
		List<Integer> roomVisited = new LinkedList<>();
		// Store the visited items (sculptures and paintings).
		List<Long> itemVisited = new LinkedList<>();
		String vertex = null;
		double currentTime = 0;
		// Time for the paths (of hour to second).
		double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
		int numberOfItemsInMuseum = accessItemFile.getNumberOfItems();

		// Get the room to which the item (or non-RS user) belongs.
		int room = getRoomFromItem(startVertex);
		roomVisited.add(room);
		// Get the items (sculptures, paintings and doors) from a specified room.
		Map<Object, Object> itemsDoorVisited_cloned = itemsDoorVisited.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<Object>(e.getValue())));
		LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
		
		// Añadido por Nacho Palacio 2025-04-22.
		// Verify null value
        if (itemsByRoom == null) {
            System.out.println("ADVERTENCIA: itemsByRoom es null para room=" + room + ". Inicializando lista vacía.");
            itemsByRoom = new LinkedList<>();
            itemsDoorVisited_cloned.put(room, itemsByRoom);
        }

		itemsByRoom.remove(startVertex);
		itemToVisit = startVertex;
		itemVisited = addItemVisited(itemToVisit, itemVisited);

		// While the visit time does not finish.
		while (currentTime < inputTime && itemVisited.size() < numberOfItems) {
			
			// Añadido por Nacho Palacio 2025-04-22.
			// Verificar que itemsByRoom no sea nulo antes de acceder a size()
			if (itemsByRoom == null) {
				System.out.println("ADVERTENCIA: itemsByRoom es null en la línea 65. Inicializando lista vacía.");
				itemsByRoom = new LinkedList<>();
				break; // Salir del bucle ya que no hay más ítems para visitar
			}

			int items = itemsByRoom.size();
			
			if (items > 0) {
				// Get an item to visit by the non-RS user randomly (without repeating).
				int indexRandom = random.nextInt(itemsByRoom.size());
				itemToVisit = itemsByRoom.get(indexRandom);
				// If the next item to visit is a painting or sculpture (range: 1-240):
				if (itemToVisit <= numberOfItemsInMuseum) {
					// Remove the chosen item from the available items (sculptures, paintings, doors
					// and stairs). The user cannot visit seen items (sculptures and paintings) but
					// can pass again through stairs or doors to move between rooms or floors.
					itemsByRoom.remove(itemToVisit);
					endVertex = itemToVisit;
					// Get a new vertex.
					vertex = getCurrentVertex(startVertex, endVertex);
					// Add the new vertex to the final path.
					finalPath += vertex;
					// Stores the visited item (painting or sculpture).
					itemVisited = addItemVisited(itemToVisit, itemVisited);
					// Get the sum of: time the non-RS user needs to move from one item
					// (painting or sculpture) to another + time the non-RS user needs to
					// observe the item (painting or sculpture) to be visited.
					currentTime += getCurrentTime(startVertex, endVertex)
							+ Configuration.simulation.getDelayObservingPaintingInSecond();
					startVertex = endVertex;
				} else {
					// If the next item to visit is a door or stairs (range: 241-312):
					// Get the connection of the current door.
					long connectedDoor = getConnectedDoor(itemToVisit);
					if (connectedDoor > 0) {
						// Get the sub-path necessary to go from one item (sculpture or painting) to
						// another through doors.
						String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
						// Get the room to which the item (or non-RS user) or door belongs.
						room = getRoomFromItem(connectedDoor);
						// Get the items (sculptures, paintings and doors) from a specified room.
						itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
						// Get the time the user needs to move from an item (sculpture, painting, door
						// or stairs) to a door or stair.
						currentTime += getCurrentTimeConnectedDoors(subpath);
						// Add the new sub-path to the final path.
						finalPath += subpath;
						startVertex = getEndVertex(subpath);
					}
					else {
						// This element is wrong -> remove
						itemsByRoom.remove(itemToVisit);
					}
				}
			}
			else {
				break;
			}
		}
		
		finalPath = eraseRepeatedObjects(finalPath);
		
		if (finalPath.length() >= 2) {
			// To remove the "," at the end of the generated path.
			return finalPath.substring(0, finalPath.length() - 2);
		}
		else {
			return finalPath;
		}
	}


	/**
	 * Añadido por Nacho Palacio 2025-04-22.
	 * Modificado por Nacho Palacio 2025-04-23.
	 * Inicializa el mapa de ítems por habitación usando IDs externos (compatible con el resto del código).
	 */
	public void initializeItemsByRoom(Map<Integer, List<Long>> roomItems) {
		System.out.println("RandomPath: Inicializando mapa de ítems por habitación");
		
		// Crear nuevo mapa si es nulo
		if (itemsDoorVisited == null) {
			itemsDoorVisited = new HashMap<>();
		}
		
		// Inicializar itemsDoorVisited con los datos pasados
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			int roomId = entry.getKey();
			List<Long> items = entry.getValue();
			List<Long> validItems = new LinkedList<>();
			
			// Verificar cada ítem para asegurarnos que tienen ubicaciones válidas
			for (Long itemId : items) {
				// if (itemId != null && itemId > 0 && diccionaryItemLocation.containsKey(itemId)) {
				if (itemId != null && itemId > 0 && hasValidLocation(itemId)) {
					validItems.add(itemId);
				} else {
					System.out.println("  - Ignorando ítem " + itemId + " en habitación " + roomId + " (sin ubicación válida)");
				}
			}
			
			// Convertir la lista a LinkedList y guardarla en el mapa
			itemsDoorVisited.put(roomId, new LinkedList<>(validItems));
			System.out.println("  - Habitación " + roomId + ": " + validItems.size() + " ítems válidos");
		}
	}

	/**
	 * Añadido por Nacho Palacio 2025-04-23.
	 * Verifica si un ítem tiene coordenadas válidas.
	 */
	private boolean hasValidLocation(long itemId) {
		try {
			// 1. Verificar primero en diccionaryItemLocation
			String location = diccionaryItemLocation.get(itemId);
			
			// 2. Si no existe, intentar obtenerlo del objeto floor
			if (location == null && MainSimulator.floor != null) {
				location = MainSimulator.floor.diccionaryItemLocation.get(itemId);
				
				// Si lo encontramos en floor, actualizamos nuestro diccionario local
				if (location != null) {
					diccionaryItemLocation.put(itemId, location);
					System.out.println("Recuperada ubicación de ítem " + itemId + " desde MainSimulator.floor");
				}
			}
			
			// 3. Como último recurso, intentar acceder desde accessItemFile
			if (location == null) {
				// Convertir a ID externo para usar con los métodos de accessItemFile
				int externalId = DataAccessItemFile.internalToExternalId(itemId, ElementIdMapper.CATEGORY_ITEM);
				
				// Obtener coordenadas desde el formato de texto de ubicación (XY)
				String vertexXY = accessItemFile.getVertexXY(externalId);
				if (vertexXY != null && !vertexXY.isEmpty()) {
					// vertexXY normalmente tiene el formato "x,y"
					location = vertexXY;
					diccionaryItemLocation.put(itemId, location);
					System.out.println("Reconstruida ubicación de ítem " + itemId + " (externo: " + externalId + ") desde getVertexXY: " + location);
				}
			}
			
			if (location == null) {
				System.out.println("ADVERTENCIA: Ubicación nula para ítem " + itemId + " después de intentar todas las fuentes");
				return false;
			}
			
			return true;
		} catch (Exception e) {
			System.out.println("Error verificando ubicación para ítem " + itemId + ": " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

    /**
     * Añadido por Nacho Palacio 2025-04-23.
     * Asegura que un ID esté en formato interno, convirtiéndolo si es necesario.
     */
    private long ensureInternalId(long id, int category) {
        if (!ElementIdMapper.isInCorrectRange(id, category)) {
            long internalId = ElementIdMapper.convertToRangeId(id, category);
            System.out.println("Convirtiendo ID " + id + " a formato interno: " + internalId);
            return internalId;
        }
        return id;
    }
}
