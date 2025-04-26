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
		System.out.println("***DEBUG-RandomPath: Inicio de generatePath con startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-04-24.

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

		System.out.println("***DEBUG-RandomPath: Obtenida habitación " + room + " para ítem " + startVertex); // Añadido por Nacho Palacio 2025-04-24.
		
		roomVisited.add(room);
		// Get the items (sculptures, paintings and doors) from a specified room.
		Map<Object, Object> itemsDoorVisited_cloned = itemsDoorVisited.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<Object>(e.getValue())));
		
		LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
		LinkedList<Long> doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room); // Añadido por Nacho Palacio 2025-04-26.

		System.out.println("***DEBUG-RandomPath: itemsByRoom para room=" + room + " tiene " + 
                  (itemsByRoom != null ? itemsByRoom.size() : "NULL") + " ítems"); // Añadido por Nacho Palacio 2025-04-24.

		System.out.println("***DEBUG-RandomPath: doorsByRoom para room=" + room + " tiene " + 
		(doorsInRoom != null ? doorsInRoom.size() : "NULL") + " puertas"); // Añadido por Nacho Palacio 2025-04-26.
		
		// Añadido por Nacho Palacio 2025-04-22.
		// Verify null value
        if (itemsByRoom == null) {
            System.out.println("ADVERTENCIA: itemsByRoom es null para room=" + room + ". Inicializando lista vacía.");
            itemsByRoom = new LinkedList<>();
            itemsDoorVisited_cloned.put(room, itemsByRoom);
        }

		// Añadido por Nacho Palacio 2025-04-26.
		if (doorsInRoom == null) {
			System.out.println("ADVERTENCIA: doorsInRoom es null para room=" + room + ". Inicializando lista vacía.");
			doorsInRoom = new LinkedList<>();
			if (doorsByRoomMap == null) doorsByRoomMap = new HashMap<>();
			doorsByRoomMap.put(room, doorsInRoom);
		}

		// Añadido por Nacho Palacio 2025-04-25.
		if (itemsByRoom != null && itemsByRoom.size() == 1 && itemsByRoom.contains(startVertex)) {
			System.out.println("***DEBUG-RandomPath: El ítem inicial " + startVertex + 
						   " es el único disponible en la habitación " + room + 
						   ". Generando una ruta mínima.");
			// Generar una ruta mínima con el ítem mismo
			finalPath = "(" + startVertex + " : " + startVertex + "), ";
			System.out.println("***DEBUG-RandomPath: Finalizado generatePath con " + 
							 (finalPath.length() >= 2 ? finalPath.length() : 0) + " caracteres");
			return finalPath.substring(0, finalPath.length() - 2); // Eliminar la coma final
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
			
			// if (items > 0) {
			// 	// Get an item to visit by the non-RS user randomly (without repeating).
			// 	int indexRandom = random.nextInt(itemsByRoom.size());
			// 	itemToVisit = itemsByRoom.get(indexRandom);
			// 	// If the next item to visit is a painting or sculpture (range: 1-240):
			// 	if (itemToVisit <= numberOfItemsInMuseum) {
			// 		// Remove the chosen item from the available items (sculptures, paintings, doors
			// 		// and stairs). The user cannot visit seen items (sculptures and paintings) but
			// 		// can pass again through stairs or doors to move between rooms or floors.
			// 		itemsByRoom.remove(itemToVisit);
			// 		endVertex = itemToVisit;
			// 		// Get a new vertex.
			// 		vertex = getCurrentVertex(startVertex, endVertex);
			// 		// Add the new vertex to the final path.
			// 		finalPath += vertex;
			// 		// Stores the visited item (painting or sculpture).
			// 		itemVisited = addItemVisited(itemToVisit, itemVisited);
			// 		// Get the sum of: time the non-RS user needs to move from one item
			// 		// (painting or sculpture) to another + time the non-RS user needs to
			// 		// observe the item (painting or sculpture) to be visited.
			// 		currentTime += getCurrentTime(startVertex, endVertex)
			// 				+ Configuration.simulation.getDelayObservingPaintingInSecond();
			// 		startVertex = endVertex;
			// 	} else {
			// 		// If the next item to visit is a door or stairs (range: 241-312):
			// 		// Get the connection of the current door.
			// 		long connectedDoor = getConnectedDoor(itemToVisit);

			// 		// Añadido por Nacho Palacio 2025-04-25.
			// 		System.out.println("***DEBUG-RandomPath: Verificando puerta conectada para ítem " + 
            //    			itemToVisit + ": connectedDoor=" + connectedDoor);
			// 		if (connectedDoor > 0) {
			// 			// Get the sub-path necessary to go from one item (sculpture or painting) to
			// 			// another through doors.
			// 			String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);

			// 			// Añadido por Nacho Palacio 2025-04-25.
			// 			System.out.println("***DEBUG-RandomPath: Generado subpath para puerta conectada: '" + 
            //    				subpath + "', longitud=" + subpath.length());
			// 			// Get the room to which the item (or non-RS user) or door belongs.
			// 			room = getRoomFromItem(connectedDoor);
			// 			// Get the items (sculptures, paintings and doors) from a specified room.
			// 			itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
			// 			// Get the time the user needs to move from an item (sculpture, painting, door
			// 			// or stairs) to a door or stair.
			// 			currentTime += getCurrentTimeConnectedDoors(subpath);
			// 			// Add the new sub-path to the final path.
			// 			finalPath += subpath;
			// 			startVertex = getEndVertex(subpath);
			// 		}
			// 		else {
			// 			// Añadido por Nacho Palacio 2025-04-25.
			// 			System.out.println("ADVERTENCIA: Puerta " + itemToVisit + 
            //          		" sin conexión válida. Intentando encontrar otra puerta.");
			// 			// This element is wrong -> remove
			// 			itemsByRoom.remove(itemToVisit);

			// 		}
			// 	}
			// }
			// else {
			// 	// Añadido por Nacho Palacio 2025-04-25.
			// 	System.out.println("***DEBUG-RandomPath: No hay más ítems disponibles en habitación " + 
            //        room + ". Finalizando generación de ruta.");
			// 	break;
			// }


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
					// Es una puerta en la lista de ítems - verificar si tiene conexión
					System.out.println("***DEBUG-RandomPath: Encontrado elemento " + itemToVisit + 
									" en lista de ítems. Verificando si es puerta...");
					
					// Verificar si es realmente una puerta (debe tener ID > numberOfItems)
					//if (itemToVisit > numberOfItemsInMuseum) {
					if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR)) { // Modificado por Nacho Palacio 2025-04-26.
						long connectedDoor = getConnectedDoor(itemToVisit);
						
						System.out.println("***DEBUG-RandomPath: Verificando puerta conectada para ítem " + 
									itemToVisit + ": connectedDoor=" + connectedDoor);
						
						if (connectedDoor > 0) {
							// Get the sub-path necessary to go from one item to another through doors.
							String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
							
							System.out.println("***DEBUG-RandomPath: Generado subpath para puerta conectada: '" + 
										subpath + "', longitud=" + subpath.length());
							
							// Get the room to which the door leads
							room = getRoomFromItem(connectedDoor);
							roomVisited.add(room);
							
							// Update the items and doors for the new room
							itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
							doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room);
							
							// Verify null values in new room
							if (itemsByRoom == null) {
								System.out.println("ADVERTENCIA: itemsByRoom es null para nueva habitación " + 
												room + ". Inicializando lista vacía.");
								itemsByRoom = new LinkedList<>();
								itemsDoorVisited_cloned.put(room, itemsByRoom);
							}
							
							if (doorsInRoom == null) {
								System.out.println("ADVERTENCIA: doorsInRoom es null para nueva habitación " + 
												room + ". Inicializando lista vacía.");
								doorsInRoom = new LinkedList<>();
								doorsByRoomMap.put(room, doorsInRoom);
							}
							
							System.out.println("***DEBUG-RandomPath: Habitación cambiada a " + room + 
										", tiene " + itemsByRoom.size() + " ítems y " + 
										doorsInRoom.size() + " puertas");
							
							// Get the time the user needs to move from an item to a door or stair.
							currentTime += getCurrentTimeConnectedDoors(subpath);
							// Add the new sub-path to the final path.
							finalPath += subpath;
							startVertex = getEndVertex(subpath);
						} else {
							System.out.println("ADVERTENCIA: Puerta " + itemToVisit + 
										" sin conexión válida. Eliminando de la lista.");
							itemsByRoom.remove(itemToVisit);
						}
					} else {
						// Este elemento no es ni ítem ni puerta válida
						System.out.println("ADVERTENCIA: Elemento " + itemToVisit + 
									" no es ni ítem ni puerta válida. Eliminando.");
						itemsByRoom.remove(itemToVisit);
					}
				}
			}
			else if (doorsInRoom != null && !doorsInRoom.isEmpty()) {
				// No quedan ítems, intentar usar una puerta para pasar a otra habitación
				System.out.println("***DEBUG-RandomPath: Sin ítems en habitación " + room + 
								". Intentando usar puertas disponibles: " + doorsInRoom.size());
				
				int indexRandom = random.nextInt(doorsInRoom.size());
				itemToVisit = doorsInRoom.get(indexRandom);
				
				// Verificar conexión para esta puerta
				long connectedDoor = getConnectedDoor(itemToVisit);
				System.out.println("***DEBUG-RandomPath: Verificando puerta conectada para ítem " + 
								itemToVisit + ": connectedDoor=" + connectedDoor);
				
				if (connectedDoor > 0) {
					// Get the sub-path through this door
					String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
					System.out.println("***DEBUG-RandomPath: Generado subpath para puerta conectada: '" + 
									subpath + "', longitud=" + subpath.length());
					
					// Get the room to which the door leads
					room = getRoomFromItem(connectedDoor);
					roomVisited.add(room);
					
					// Update the items and doors for the new room
					itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
					doorsInRoom = (LinkedList<Long>) doorsByRoomMap.get(room); 
					
					System.out.println("***DEBUG-RandomPath: Cambiado a habitación " + room + 
									" a través de puerta " + itemToVisit + "->" + connectedDoor);
					
					// Time to move through connected doors
					currentTime += getCurrentTimeConnectedDoors(subpath);
					finalPath += subpath;
					startVertex = getEndVertex(subpath);
				} else {
					System.out.println("ADVERTENCIA: Puerta " + itemToVisit + 
									" sin conexión válida. Eliminando de puertas disponibles.");
					doorsInRoom.remove(itemToVisit);
				}
			}
			else {
				System.out.println("***DEBUG-RandomPath: No hay más ítems ni puertas disponibles en habitación " + 
								room + ". Finalizando generación de ruta.");
				break;
			}
		}
		
		finalPath = eraseRepeatedObjects(finalPath);

		System.out.println("***DEBUG-RandomPath: Finalizado generatePath con " + 
                  (finalPath.length() >= 2 ? finalPath.length() : 0) + " caracteres"); // Añadido por Nacho Palacio 2025-04-24.
		
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
			
			// Verificar cada ítem para asegurarnos que tienen ubicaciones válidas
			// for (Long itemId : items) { // For antiguo
			// 	// if (itemId != null && itemId > 0 && diccionaryItemLocation.containsKey(itemId)) {
			// 	if (itemId != null && itemId > 0 && hasValidLocation(itemId)) {
			// 		validItems.add(itemId);
			// 	} else {
			// 		System.out.println("  - Ignorando ítem " + itemId + " en habitación " + roomId + " (sin ubicación válida)");
			// 	}
			// }

			for (Long itemId : items) {
				if (itemId == null || itemId <= 0) continue;
				
				// Determinar si es un ítem o una puerta basado en su ID
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM)) {
					// Es un ítem regular
					if (hasValidLocation(itemId)) {
						validItems.add(itemId);
					} else {
						System.out.println("  - Ignorando ítem " + itemId + " en habitación " + roomId + " (sin ubicación válida)");
					}
				} 
				else if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR)) {
					// Es una puerta
					if (hasValidLocation(itemId)) {
						validDoors.add(itemId);
					} else {
						// Intentar con id externo
						long externalId = ElementIdMapper.getBaseId(itemId);
						if (hasValidLocation(externalId)) {
							validDoors.add(itemId);
							// También almacenar ubicación para ID interno
							String location = diccionaryItemLocation.get(externalId);
							diccionaryItemLocation.put(itemId, location);
						} else {
							System.out.println("  - Ignorando puerta " + itemId + " en habitación " + roomId + " (sin ubicación válida)");
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
					} else {
						System.out.println("  - Ignorando puerta externa " + itemId + " en habitación " + roomId + " (sin ubicación válida)");
					}
				}
			}
			
			// Convertir la lista a LinkedList y guardarla en el mapa
			itemsDoorVisited.put(roomId, new LinkedList<>(validItems));
			doorsByRoomMap.put(roomId, new LinkedList<>(validDoors)); // Añadido por Nacho Palacio 2025-04-26.
			// System.out.println("  - Habitación " + roomId + ": " + validItems.size() + " ítems válidos");

			System.out.println("  - Habitación " + roomId + ": " + validItems.size() + " ítems y " + validDoors.size() + " puertas válidas"); // Modificado por Nacho Palacio 2025-04-26.

			// Añadido por Nacho Palacio 2025-04-25.
			// if (validItems.isEmpty()) {
			// 	System.out.println("ADVERTENCIA: La habitación " + roomId + " no tiene ítems válidos, añadiendo ítem genérico");
			// 	// Añadir un ítem genérico para evitar habitaciones vacías
			// 	validItems.add(1001L); // Un ítem genérico que sabemos que existe
			// }

			// Modificado por Nacho Palacio 2025-04-26.
			if (validItems.isEmpty()) {
				System.out.println("ADVERTENCIA: La habitación " + roomId + " no tiene ítems válidos, añadiendo ítem genérico");
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


	/**
	 * Añadido por Nacho Palacio 2025-04-26.
	 * Inicializa las conexiones entre puertas
	 */
	private void initializeDoorConnections() {
		System.out.println("RandomPath: Inicializando conexiones entre puertas");
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
					System.out.println("ADVERTENCIA: Ignorando supuesta conexión entre puertas donde al menos una es un ítem: " 
								+ door1External + " <-> " + door2External);
					continue;
				}
				
				// Convertir a IDs internos (rango 2000+)
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
				
				// Asegurarse de que ambas puertas estén en su respectiva habitación
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
		
		System.out.println("RandomPath: Inicializadas " + connections + " conexiones entre puertas");
		
		// Verificar que todas las habitaciones tengan el mapa de puertas inicializado
		for (int roomId = 1; roomId <= accessGraphFile.getNumberOfRoom(); roomId++) {
			if (!doorsByRoomMap.containsKey(roomId)) {
				doorsByRoomMap.put(roomId, new LinkedList<>());
				System.out.println("Inicializado mapa de puertas vacío para habitación " + roomId);
			}
		}
	}
}
