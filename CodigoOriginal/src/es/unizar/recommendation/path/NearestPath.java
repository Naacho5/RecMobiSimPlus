package es.unizar.recommendation.path;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.UserRunnable;
import es.unizar.util.ElementIdMapper;

/**
 * 
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public class NearestPath extends Path {

	public NearestPath() {
		super();
	}

	/**
	 * Generate a non-RS user path, applying the nearest item strategy and
	 * considering the specified time (in the Configuration form).
	 * 
	 * @param startVertex: the initial position (it will be the position of a item,
	 *                     chosen randomly) of the non-RS user.
	 */
	@Override
	public String generatePath(long startVertex) {
		System.out.println("NearestPath.generatePath: Start vertex: " + startVertex); // Añadido por Nacho Palacio 2025-06-28

		startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);
    	System.out.println("NearestPath.generatePath: Start vertex DESPUÉS: " + startVertex);

		String finalPath = "";
		long endVertex = 0;
		List<Integer> roomVisited = new LinkedList<>();
		// Time for the paths (of hour to second).
		double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
		int numberOfItemsInMap = accessItemFile.getNumberOfItems();
		long itemToVisit = 0;
		boolean allRoomVisited = false;
		// Store the visited items (sculptures and paintings).
		List<Long> itemVisited = new LinkedList<>();
		itemVisited = alreadyVisited();
		
		List<Long> repeated = new LinkedList<>();
		
		/*
		System.out.println("Item visited: ");
		for (Long l: itemVisited)
			System.out.println(l + ", ");
		*/
		
		String vertex = null;
		double currentTime = 0;
		boolean ifItemToVisitWasCero = false;

		// Get the room of the initial item (or non-RS user).
		int room = getRoomFromItem(startVertex);
		
		// Store the visited room.
		roomVisited = addRoomVisited(room, roomVisited);
		
		/*
		System.out.println("Room visited: ");
		for (Integer integer: roomVisited)
			System.out.println(integer + ", ");
		*/
		
		// Get the items (sculptures, paintings and doors) of a specified room.
		// LinkedList<Long> itemsByRoom = (LinkedList<Long>) getItemsByRoom(room);
		// Modificado por Nacho Palacio 2025-06-29
		Map<Object, Object> itemsDoorVisited_cloned = itemsDoorVisited.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<Object>(e.getValue())));

		LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
		if (itemsByRoom == null) {
			System.out.println("ADVERTENCIA: itemsByRoom es null para room=" + room + ". Inicializando lista vacía.");
			itemsByRoom = new LinkedList<>();
			itemsDoorVisited_cloned.put(room, itemsByRoom);
		}

		// Añadido por Nacho Palacio 2025-05-28
		itemsByRoom = convertAndValidateItems(itemsByRoom, "itemsByRoom", numberOfItemsInMap);
    
		System.out.println("🔍 DEBUG HABITACIÓN " + room + " EN NearestPath:");
		System.out.println("  itemsByRoom después de conversión: " + itemsByRoom);
		System.out.println("  tamaño de itemsByRoom: " + (itemsByRoom != null ? itemsByRoom.size() : "NULL"));
		
		/*
		System.out.println("Items by room: ");
		for (Long item: itemsByRoom)
			System.out.println(item + ", ");
		*/

		// If the first time of the simulation, the first pair of vertices is composed
		// of (startVertex, startVertex).
		if (UserRunnable.firstTime) {
			// Get a new vertex to visit by the non-RS user, specifying the start and
			// end vertices.
			finalPath += getCurrentVertex(startVertex, startVertex);
			if (startVertex <= this.numberOfItems)
				itemVisited.add(startVertex); // ADD THE START VERTEX AS VISITED!!!
			
			/*
			for (Long l: itemVisited)
				System.out.println(l + ", ");
			*/
		}

		//Monitor monitor = null;
		
		// While the visit time does not finish.
		while (currentTime < inputTime && itemVisited.size() < numberOfItems) {			
			//monitor = MonitorFactory.start("nearestPathWhileTimeAvailable");
			
			// System.out.println("\nCurrent time: " + currentTime);
			
			// Get the item (most likely of nearest) to visit by non-RS user (without
			// repeating).
			if (!ifItemToVisitWasCero) {
				// Añadido por Nacho Palacio 2025-07-05
				// System.out.println("🔄 ANTES de getItemToVisit:");
				// System.out.println("  - startVertex actual: " + startVertex);
				// System.out.println("  - itemsByRoom.size(): " + itemsByRoom.size());
				// System.out.println("  - itemsByRoom: " + itemsByRoom.subList(0, Math.min(5, itemsByRoom.size())) + (itemsByRoom.size() > 5 ? "..." : ""));

				itemToVisit = getItemToVisit(startVertex, itemsByRoom, roomVisited, itemVisited, repeated);

				// Añadido por Nacho Palacio 2025-07-05
				// System.out.println("🔄 DESPUÉS de getItemToVisit:");
				// System.out.println("  - itemToVisit seleccionado: " + itemToVisit);
				// System.out.println("  - ¿Es ítem? " + ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM));
				// System.out.println("  - ¿Es puerta? " + ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR));

				if (itemToVisit == 0) {
					ifItemToVisitWasCero = true;
				}
			}
			
			repeated.add(itemToVisit);
			// System.out.println("Item to visit: " + itemToVisit);

			// If the item to visit is != 0, it is because there are items available and
			// nearby to visit.
			if (itemToVisit > 0 && !itemsByRoom.isEmpty()) {
				
				// System.out.println("Room not empty -> " + itemToVisit);
				// Remove the chosen item from the available items.
				itemsByRoom.remove(itemToVisit);
				
				/*
				System.out.println("Items by room: ");
				for (Long item: itemsByRoom)
					System.out.println(item + ", ");
				*/

				// Añadido por Nacho Palacio 2025-05-17
				// long convertedItemToVisit = ElementIdMapper.convertToRangeId(itemToVisit, ElementIdMapper.CATEGORY_ITEM);
				
				// If the next item to visit is a painting or sculpture (range: 1-240):
				// if (itemToVisit <= numberOfItemsInMap) {
				if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM)) { // Modificado por Nacho Palacio 2025-05-20
					endVertex = itemToVisit;
					// Get a new vertex.
					vertex = getCurrentVertex(startVertex, endVertex);
					// Add the new vertex to the final path.
					//System.out.println(finalPath);
					finalPath += vertex;
					//System.out.println(finalPath + "\n");
					// Stores the visited item (painting or sculpture).
					itemVisited = addItemVisited(itemToVisit, itemVisited);
					// Get the sum of: time the non-RS user needs to move from one item
					// (painting or sculpture) to another + time the non-RS user needs to
					// observe the item (painting or sculpture) to be visited.
					currentTime += getCurrentTime(startVertex, endVertex)
							+ Configuration.simulation.getDelayObservingPaintingInSecond();
					startVertex = endVertex;
				} //else {

				else if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR) ||
           					ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_STAIRS)) {
					
					//System.out.println("Door or stairs");
					
					// If the next item to visit is a door or stairs (range: 241-312):
					// Get the connection of the current door.
					// long connectedDoor = getConnectedDoor(itemToVisit);

					long connectedDoor = getConnectedDoor(itemToVisit);
					// long connectedDoor = getValidConnectedDoor(itemToVisit); // Modificado por Nacho Palacio 2025-05-29
					// ... resto del código ...
					
					
					//System.out.println("Connected door: " + connectedDoor);
					
					// Get the sub-path necessary to go from one item (sculpture or painting) to
					// another through doors.
					String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
					
					//System.out.println("Subpath: " + subpath);
					
					// Get the room to which the connecting door belongs.
					room = getRoomFromItem(connectedDoor);
					
					//System.out.println("Room: " + room);
					
					// Get the time the user needs to move from an item (sculpture, painting, door
					// or stairs) to a door or stair.
					currentTime += getCurrentTimeConnectedDoors(subpath);

					// Add the new sub-path to the final path.
					// System.out.println(finalPath);
					finalPath += subpath;
					//System.out.println(finalPath + "\n");
					// Update the available items to visit by non-RS user in the current room.
					// itemsByRoom = (LinkedList<Long>) updateItemsByRoom(room, itemVisited, roomVisited);

					// Modificado por Nacho Palacio 2025-05-29
					itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
					if (itemsByRoom == null) {
						itemsByRoom = new LinkedList<>();
						itemsDoorVisited_cloned.put(room, itemsByRoom);
					}
					itemsByRoom = convertAndValidateItems(itemsByRoom, "updated_room_" + room, numberOfItemsInMap);
					// Get the end vertex from sub-path.
					startVertex = getEndVertex(subpath);
					ifItemToVisitWasCero = false;//ifItemToVisitWasCero = (startVertex > 0);
					
					/*
					if (room <= 0)
						ifItemToVisitWasCero = true;
					else
						ifItemToVisitWasCero = false;
					*/
				}
				else {
					itemsByRoom.remove(itemToVisit);
				}
			} else {				
				// System.out.println("Room empty or itemToVisit <= 0");
				
				// Treat the case where there are no items to visit (itemToVisit = 0) or
				// (itemsByRoom.isEmpty)
				if (!itemsByRoom.isEmpty() && !allRoomVisited) {
					
					// System.out.println("Items by room not empty");
					
					// All the items have been visited by the non-RS user and there is no other
					// option that to go to that visited room because there is time.
					room = getRoomFromItem(startVertex);
					
					// System.out.println(" - " + startVertex + " vertex in room " + room);
					
					int numberOfDoors = accessGraphFile.getNumDoorsByRoom(room);
					if (numberOfDoors == 1) {
						itemToVisit = accessGraphFile.getDoorOfRoomWithIndex(numberOfDoors, room);
					} else {
						int count = 0;
						// Para evitar que la puerta que se escoja no vaya a una room ya visitada por
						// completo (todos sus items vistos)
						for (int indexDoor = 1; indexDoor <= numberOfDoors; indexDoor++) {
							long itemCandidateToVisit = accessGraphFile.getDoorOfRoomWithIndex(indexDoor, room);

							// Añadido por Nacho Palacio 2025-06-01
							if (itemCandidateToVisit > numberOfItems + numberOfDoors) {
								// System.out.println("*** PUERTA CANDIDATA FUERA DE RANGO EN NearestPath ***");
								// System.out.println("room: " + room);
								// System.out.println("indexDoor: " + indexDoor);
								// System.out.println("itemCandidateToVisit: " + itemCandidateToVisit);
								// System.out.println("numberOfDoors en habitación: " + numberOfDoors);
								// System.out.println("Rango válido: " + (numberOfItems + 1) + " a " + (numberOfItems + numberOfDoors));
								// System.out.println("*** FIN DEBUG ***");

								// Añadido por Nacho Palacio 2025-07-04
								count++;
								continue;
							}

							long connectedDoor = getConnectedDoor(itemCandidateToVisit);
							// long connectedDoor = getValidConnectedDoor(itemCandidateToVisit); // Modificado por Nacho Palacio 2025-05-29

							// if (connectedDoor <= 0 || !isValidDoor(connectedDoor)) {
							// 	System.err.println("WARNING: Connected door " + connectedDoor + " is not valid, skipping candidate " + itemCandidateToVisit);
							// 	count++;
							// 	continue;
							// }

							// Modificado por Nacho Palacio 2025-05-31
							if (connectedDoor <= 0) {
								System.out.println("ADVERTENCIA: Connected door es <= 0 para itemCandidateToVisit=" + itemCandidateToVisit + ". Saltando candidato."); // Añadido por Nacho Palacio 2025-07-04
								count++;
								continue;
							}

							// itemsByRoom = (LinkedList<Long>) getItemsByRoom(getRoomFromItem(connectedDoor));
							
							// Modificado por Nacho Palacio 2025-06-29
							room = getRoomFromItem(connectedDoor);
							itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
							if (itemsByRoom == null) {
								itemsByRoom = new LinkedList<>();
								itemsDoorVisited_cloned.put(room, itemsByRoom);
							}
							itemsByRoom = convertAndValidateItems(itemsByRoom, "updated_room_" + room, numberOfItemsInMap);
							
							if (!itemVisited.containsAll(itemsByRoom)) {
								itemToVisit = itemCandidateToVisit;
								allRoomVisited = false;
								break;
							} else {
								count++;
							}
						}
						// If the items in all the surrounding rooms have been visited.
						if (count == numberOfDoors) {
							allRoomVisited = true;
						}
					}
					ifItemToVisitWasCero = true;
				} else {
					
					// System.out.println("Items by room is empty");
					
					room = getRoomFromItem(startVertex);
					
					/*
					if (room <= 0) {
						System.out.println("Room was <= 0");
						room = random.nextInt(accessGraphFile.getNumberOfRoom() - 1 + 1) + 1;
						System.out.println("New room: " + room);
					}*/
					
					int numberOfDoors = accessGraphFile.getNumDoorsByRoom(room);

					// Modificado por Nacho Palacio 2025-05-31
					int indexDoor = random.nextInt(numberOfDoors - 1 + 1) + 1;
					itemToVisit = accessGraphFile.getDoorOfRoomWithIndex(indexDoor, room);

					// Añadido por Nacho Palacio 2025-06-03
					if (!ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM) && 
						!ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR)) {
						System.out.println("*** ELEMENTO FUERA DE RANGO EN NearestPath ***");
						System.out.println("itemToVisit: " + itemToVisit);
						System.out.println("startVertex: " + startVertex);
						System.out.println("itemsByRoom.size(): " + itemsByRoom.size());
						System.out.println("Validación ElementIdMapper:");
						System.out.println("  - ¿Es ítem válido? " + ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM));
						System.out.println("  - ¿Es puerta válida? " + ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR));
						System.out.println("itemsByRoom contenido: " + itemsByRoom);
						System.out.println("*** FIN DEBUG ***");
					}

					long connectedDoor = getConnectedDoor(itemToVisit);

					// Añadido por Nacho Palacio 2025-05-31
					if (connectedDoor <= 0) {
						break;
					}

					String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
					// Get the room to which the door belongs.
					room = getRoomFromItem(connectedDoor);
					// Get the time the user needs to move from an item (sculpture, painting, door
					// or stairs) to a door or stair.
					currentTime += getCurrentTimeConnectedDoors(subpath);
					// Add the new sub-path to the final path.
					//System.out.println(finalPath);
					finalPath += subpath;
					//System.out.println(finalPath + "\n");
					// itemsByRoom = (LinkedList<Long>) updateItemsByRoom(room, itemVisited, roomVisited);

					// Modificado por Nacho Palacio 2025-05-29
					itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
					if (itemsByRoom == null) {
						itemsByRoom = new LinkedList<>();
						itemsDoorVisited_cloned.put(room, itemsByRoom);
					}
					itemsByRoom = convertAndValidateItems(itemsByRoom, "updated_room_" + room, numberOfItemsInMap);
					startVertex = getEndVertex(subpath);
					ifItemToVisitWasCero = false;
					allRoomVisited = false;
				}

			}
			
			//System.out.println(finalPath);
			//monitor.stop();
		}
		
		finalPath = eraseRepeatedObjects(finalPath);
		
		//System.out.println(monitor);
		
		// To remove the "," at the end of the generated path.
		if (finalPath.length() > 0) {
			// To remove the "," at the end of the generated path.
			char lastChar = finalPath.charAt(finalPath.length() - 1);
			while (Character.compare(lastChar, ')') != 0) {
				finalPath = finalPath.substring(0, finalPath.length() -1);
				lastChar = finalPath.charAt(finalPath.length() - 1);
			}
		}

		return finalPath;
		/*
		System.out.println("Final path: " + finalPath);
		
		if (finalPath.length() >= 2) {
			// To remove the "," at the end of the generated path.
			return finalPath.substring(0, finalPath.length() - 2);
		}
		else {
			return finalPath;
		}
		*/
	}

	/**
	 * Gets the already visited items by RS user from the simulation and returns the item list.
	 * Used for avoiding visiting already seen items.
	 * 
	 * @return List<Long> alreadyVisited	Contains the already visited items.
	 */
	private List<Long> alreadyVisited() {
		List<Long> alreadyVisited = new LinkedList<>();
		
		if (!Configuration.simulation.oldPathUserSpecial.isEmpty()) {
			
			for (int i = 0; i < Configuration.simulation.oldPathUserSpecial.size(); i++) {
				
				String[] array = Configuration.simulation.cleanEdge(Configuration.simulation.oldPathUserSpecial.get(i));
				long v1 = Long.valueOf(array[0]).longValue();
				long v2 = Long.valueOf(array[1]).longValue();
			
				if (i == 0)
					alreadyVisited.add(v1);
				
				if (!alreadyVisited.contains(v2))
					alreadyVisited.add(v2);
				
			}
		}
		
		return alreadyVisited;
	}

	/**
	 * Validación del método getConnectedDoor con puertas reales.
	 * Añadido por Nacho Palacio 2025-05-29.
	 */
	protected long getValidConnectedDoor(long doorId) {
		try {
			long connectedDoor = getConnectedDoor(doorId);
			
			if (connectedDoor > 0 && isValidDoor(connectedDoor)) {
				return connectedDoor;
			}

			
			int currentRoom = getRoomFromItem(doorId);
			for (int adjacentRoom = currentRoom - 1; adjacentRoom <= currentRoom + 1; adjacentRoom++) {
				if (adjacentRoom > 0 && adjacentRoom != currentRoom) {
					List<Long> adjacentDoors = getValidDoorsForRoom(adjacentRoom);
					if (!adjacentDoors.isEmpty()) {
						return adjacentDoors.get(0);
					}
				}
			}
			
			return getValidRandomDoor();
			
		} catch (Exception e) {
			System.err.println("ERROR getting valid connected door for " + doorId + ": " + e.getMessage());
			return getValidRandomDoor();
		}
	}

}
