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
		startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);
					
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
		LinkedList<Long> doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room); // Añadido por Nacho Palacio 2025-04-26.
		
		// Añadido por Nacho Palacio 2025-04-22.
		// Verify null value
        if (itemsByRoom == null) {
            // System.out.println("ADVERTENCIA: itemsByRoom es null para room=" + room + ". Inicializando lista vacía.");
            itemsByRoom = new LinkedList<>();
            itemsDoorVisited_cloned.put(room, itemsByRoom);
        }

		// Añadido por Nacho Palacio 2025-04-26.
		if (doorsInRoom == null) {
			// System.out.println("ADVERTENCIA: doorsInRoom es null para room=" + room + ". Inicializando lista vacía.");
			doorsInRoom = new LinkedList<>();
			if (doorsByRoomMap == null) doorsByRoomMap = new HashMap<>();
			doorsByRoomMap.put(room, doorsInRoom);
		}

		itemsByRoom.remove(startVertex);
		itemToVisit = startVertex;
		itemVisited = addItemVisited(itemToVisit, itemVisited);

		// While the visit time does not finish.
		while (currentTime < inputTime && itemVisited.size() < numberOfItems) {
			
			// Añadido por Nacho Palacio 2025-04-22.
			if (itemsByRoom == null) {
				itemsByRoom = new LinkedList<>();
				break; // No hay más ítems para visitar
			}

			int items = itemsByRoom.size();

			// Modificado por Nacho Palacio 2025-04-26.
			if (!itemsByRoom.isEmpty()) {
				// Visitar un ítem normal
				int indexRandom = random.nextInt(itemsByRoom.size());
				itemToVisit = itemsByRoom.get(indexRandom);
				
				// Si es un ítem (no una puerta)
				// if (itemToVisit <= numberOfItemsInMuseum) {
				if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM)) { // Modificado por Nacho Palacio 2025-04-26.
					// Remove the chosen item from the available items
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
					currentTime += getCurrentTime(startVertex, endVertex) + 
								Configuration.simulation.getDelayObservingPaintingInSecond();
					startVertex = endVertex;
				} else {
					// Verificar si es realmente una puerta (debe tener ID > numberOfItems)
					//if (itemToVisit > numberOfItemsInMuseum) {
					if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR)) { // Modificado por Nacho Palacio 2025-04-26.
						long connectedDoor = getConnectedDoor(itemToVisit);
						
						if (connectedDoor > 0) {
							// Get the sub-path necessary to go from one item to another through doors.
							String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
							
							// Get the room to which the door leads
							room = getRoomFromItem(connectedDoor);
							roomVisited.add(room);
							
							// Update the items and doors for the new room
							itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
							doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room);
							
							// Verify null values in new room
							if (itemsByRoom == null) {
								itemsByRoom = new LinkedList<>();
								itemsDoorVisited_cloned.put(room, itemsByRoom);
							}
							
							if (doorsInRoom == null) {
								doorsInRoom = new LinkedList<>();
								doorsByRoomMap.put(room, doorsInRoom);
							}
							
							// Get the time the user needs to move from an item to a door or stair.
							currentTime += getCurrentTimeConnectedDoors(subpath);
							// Add the new sub-path to the final path.
							finalPath += subpath;
							startVertex = getEndVertex(subpath);
						} else {
							itemsByRoom.remove(itemToVisit);
						}
					} else {
						itemsByRoom.remove(itemToVisit);
					}
				}
			}
			else if (doorsInRoom != null && !doorsInRoom.isEmpty()) {
				// No quedan ítems, intentar usar una puerta para pasar a otra habitación
				int indexRandom = random.nextInt(doorsInRoom.size());
				itemToVisit = doorsInRoom.get(indexRandom);
				
				// Verificar conexión para esta puerta
				long connectedDoor = getConnectedDoor(itemToVisit);
				
				if (connectedDoor > 0) {
					// Get the sub-path through this door
					String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
					
					// Get the room to which the door leads
					room = getRoomFromItem(connectedDoor);
					roomVisited.add(room);
					
					// Update the items and doors for the new room
					itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
					doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room); 
					
					// Time to move through connected doors
					currentTime += getCurrentTimeConnectedDoors(subpath);
					finalPath += subpath;
					startVertex = getEndVertex(subpath);
				} else {
					doorsInRoom.remove(itemToVisit);
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
	 * Inicializa el mapa de ítems por habitación usando IDs externos.
	 */
	public void initializeItemsByRoom(Map<Integer, List<Long>> roomItems) {
		// Añadido por Nacho Palacio 2025-05-02.
		int totalExternalIds = 0;
		int totalInternalIds = 0;
		int totalItemsToAnalyze = 0;
		
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			List<Long> items = entry.getValue();
			for (Long itemId : items) {
				if (itemId == null || itemId <= 0) continue;
				totalItemsToAnalyze++;
				
				if (isProbablyExternalId(itemId)) {
					totalExternalIds++;
				} else {
					totalInternalIds++;
				}
			}
		}
		
		// Añadido por Nacho Palacio 2025-05-02.
		int totalDoorIds = 0;
		int totalDoorIdsInRange = 0;

		int num = this.numberOfItems + this.numberOfDoors;
		// Se buscan puertas
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			int roomId = entry.getKey();
			List<Long> items = entry.getValue();
			
			for (Long itemId : items) {
				if (itemId == null || itemId <= 0) continue;
				
				// Verificar si es una puerta según su rango numérico
				if (itemId > this.numberOfItems && itemId <= this.numberOfItems + this.numberOfDoors) {
					totalDoorIds++;
				}
				
				// Verificar si es una puerta según ElementIdMapper
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR)) {
					totalDoorIdsInRange++;
				}
			}
		}
		
		int totalItemsProcessed = 0;
		int totalValidItems = 0;
		int totalValidDoors = 0;
		int totalInvalidItems = 0;
		int totalRoomsWithNoItems = 0;
		
		// Crear nuevo mapa si es nulo
		if (itemsDoorVisited == null) {
			itemsDoorVisited = new HashMap<>();
		}

		// Añadido por Nacho Palacio 2025-04-26.
		if (doorsByRoomMap == null) {
			doorsByRoomMap = new HashMap<>();
		}
		
		// Inicializar itemsDoorVisited con los datos pasados
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			int roomId = entry.getKey();
			List<Long> items = entry.getValue();
			List<Long> validItems = new LinkedList<>();
			List<Long> validDoors = new LinkedList<>();

			totalItemsProcessed += items.size();
			
			for (Long itemId : items) {
				if (itemId == null || itemId <= 0) continue;
				
				// Determinar si es un ítem o una puerta basado en su ID
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM)) {
					// Es un ítem regular
					if (hasValidLocation(itemId)) {
						validItems.add(itemId);
						// Añadido por Nacho Palacio 2025-05-01.
						totalValidItems++;
					} else {
						// Añadido por Nacho Palacio 2025-05-01.
						totalInvalidItems++;
					}
				} 
				else if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR)) {
					// Es una puerta
					if (hasValidLocation(itemId)) {
						validDoors.add(itemId);
						// Añadido por Nacho Palacio 2025-05-01.
						totalValidDoors++;
					} else {
						// Intentar con id externo
						long externalId = ElementIdMapper.getBaseId(itemId);
						if (hasValidLocation(externalId)) {
							validDoors.add(itemId);
							totalValidDoors++; // Añadido por Nacho Palacio 2025-05-01.
							// También almacenar ubicación para ID interno
							String location = diccionaryItemLocation.get(externalId);
							diccionaryItemLocation.put(itemId, location);
						}
					}
				}
				else if (itemId > numberOfItems && itemId <= numberOfItems + numberOfDoors) {
					// Puerta con ID externo
					if (hasValidLocation(itemId)) {
						validDoors.add(itemId);
						// También añadir versión con ID interno
						long internalId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_DOOR);
						validDoors.add(internalId);
					}
				}
			}
			
			// Convertir la lista a LinkedList y guardarla en el mapa
			itemsDoorVisited.put(roomId, new LinkedList<>(validItems));
			doorsByRoomMap.put(roomId, new LinkedList<>(validDoors)); // Añadido por Nacho Palacio 2025-04-26.
			// System.out.println("  - Habitación " + roomId + ": " + validItems.size() + " ítems válidos");

			// Modificado por Nacho Palacio 2025-04-26.
			if (validItems.isEmpty()) {
				// Añadido por Nacho Palacio 2025-05-01.
				totalRoomsWithNoItems++;
    		
				LinkedList<Long> defaultItems = new LinkedList<>();
				defaultItems.add(1001L); // Un ítem genérico que sabemos que existe
				itemsDoorVisited.put(roomId, defaultItems);
			}
		}
		initializeDoorConnections();
	}

	/**
	 * Añadido por Nacho Palacio 2025-04-23.
	 * Verifica si un ítem tiene coordenadas válidas.
	 */
	private boolean hasValidLocation(long itemId) {
		try {
			// Verificar primero en diccionaryItemLocation
			String location = diccionaryItemLocation.get(itemId);
			
			// Si no existe, intentar obtenerlo del objeto floor
			if (location == null && MainSimulator.floor != null) {
				location = MainSimulator.floor.diccionaryItemLocation.get(itemId);
				
				if (location != null) {
					diccionaryItemLocation.put(itemId, location);
				}
			}
			
			// Sino, se intenta acceder desde accessItemFile
			if (location == null) {
				int externalId = DataAccessItemFile.internalToExternalId(itemId, ElementIdMapper.CATEGORY_ITEM);
				
				// Obtener coordenadas desde el formato de texto de ubicación (XY)
				String vertexXY = accessItemFile.getVertexXY(externalId);
				if (vertexXY != null && !vertexXY.isEmpty()) {
					location = vertexXY;
					diccionaryItemLocation.put(itemId, location);
				}
				else { // Añadido por Nacho Palacio 2025-05-01.
					if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR)) {
						externalId = DataAccessItemFile.internalToExternalId(itemId, ElementIdMapper.CATEGORY_DOOR);
						vertexXY = accessItemFile.getVertexXY(externalId);
						if (vertexXY != null && !vertexXY.isEmpty()) {
							location = vertexXY;
							diccionaryItemLocation.put(itemId, location);
						}
					}
					
					if (location == null && ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_STAIRS)) {
						externalId = DataAccessItemFile.internalToExternalId(itemId, ElementIdMapper.CATEGORY_STAIRS);
						vertexXY = accessItemFile.getVertexXY(externalId);
						if (vertexXY != null && !vertexXY.isEmpty()) {
							location = vertexXY;
							diccionaryItemLocation.put(itemId, location);
						}
					}
				}
			}
			
			if (location == null) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
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
            return internalId;
        }
        return id;
    }


	/**
	 * Añadido por Nacho Palacio 2025-04-26.
	 * Inicializa las conexiones entre puertas
	 */
	private void initializeDoorConnections() {
		int numberDoorConnected = accessGraphFile.getNumberOfConnectedDoor();
		int connections = 0;
		
		for (int posDoor = 1; posDoor <= numberDoorConnected; posDoor++) {
			try {
				String doors = accessGraphFile.getConnectedDoor(posDoor);
				
				// Obtener IDs externos (originales)
				long door1External = accessGraphFile.getDoorOfRoom(doors.split(", ")[0]);
				long door2External = accessGraphFile.getDoorOfRoom(doors.split(", ")[1]);
				
				// Solo procesar si estas son realmente puertas (ID > numberOfItems)
				if (door1External <= this.numberOfItems || door2External <= this.numberOfItems) {
					continue;
				}
				
				// Convertir a IDs internos
				long door1Internal = ElementIdMapper.convertToRangeId(door1External, ElementIdMapper.CATEGORY_DOOR);
				long door2Internal = ElementIdMapper.convertToRangeId(door2External, ElementIdMapper.CATEGORY_DOOR);
				
				// Almacenar la conexión en el mapDoorConnected
				this.mapDoorConnected.put(door1External, door2External);
				this.mapDoorConnected.put(door2External, door1External);
				this.mapDoorConnected.put(door1Internal, door2Internal);
				this.mapDoorConnected.put(door2Internal, door1Internal);
				
				// También almacenar conexiones cruzadas
				this.mapDoorConnected.put(door1External, door2Internal);
				this.mapDoorConnected.put(door2External, door1Internal);
				this.mapDoorConnected.put(door1Internal, door2External);
				this.mapDoorConnected.put(door2Internal, door1External);
				
				// Comprobar que ambas puertas estén en su respectiva habitación
				int room1 = getRoomFromItem(door1External);
				int room2 = getRoomFromItem(door2External);
				
				if (room1 > 0) {
					LinkedList<Long> roomDoors1 = (LinkedList<Long>)doorsByRoomMap.get(room1);
					if (roomDoors1 == null) {
						roomDoors1 = new LinkedList<>();
						doorsByRoomMap.put(room1, roomDoors1);
					}
					if (!roomDoors1.contains(door1External)) roomDoors1.add(door1External);
					if (!roomDoors1.contains(door1Internal)) roomDoors1.add(door1Internal);
				}
				
				if (room2 > 0) {
					LinkedList<Long> roomDoors2 = (LinkedList<Long>)doorsByRoomMap.get(room2);
					if (roomDoors2 == null) {
						roomDoors2 = new LinkedList<>();
						doorsByRoomMap.put(room2, roomDoors2);
					}
					if (!roomDoors2.contains(door2External)) roomDoors2.add(door2External);
					if (!roomDoors2.contains(door2Internal)) roomDoors2.add(door2Internal);
				}
				
				connections++;
			} catch (Exception e) {
				System.out.println("Error al procesar conexión de puerta #" + posDoor + ": " + e.getMessage());
			}
		}
		// Verificar que todas las habitaciones tengan el mapa de puertas inicializado
		for (int roomId = 1; roomId <= accessGraphFile.getNumberOfRoom(); roomId++) {
			if (!doorsByRoomMap.containsKey(roomId)) {
				doorsByRoomMap.put(roomId, new LinkedList<>());
			}
		}
	}


	/**
	 * Añadido por Nacho Palacio 2025-05-02.
	 * Diagnostica si un ID corresponde a formato interno o externo
	 * @param itemId ID a verificar
	 * @return true si es probablemente un ID externo, false si es interno
	 */
	private boolean isProbablyExternalId(long itemId) {
		if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR)) {
			return false;
		}
		
		if (itemId >= ElementIdMapper.ITEM_ID_START && itemId < ElementIdMapper.DOOR_ID_START) {
			return false; // Es un ID interno de ítem
		}
		
		if (itemId > 0 && itemId < 1000) {
			return true; // Probablemente es un ID externo de ítem
		}
		
		int category = ElementIdMapper.determineCategoryFromInternalId(itemId);
		return category == -1; // Si no se identifica una categoría, probablemente es un ID externo
	}
}
