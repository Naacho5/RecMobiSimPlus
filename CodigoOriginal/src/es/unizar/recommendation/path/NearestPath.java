package es.unizar.recommendation.path;

import java.util.LinkedList;
import java.util.List;

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
		System.out.println("DEBUG-NearestPath: Inicio de generatePath, startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-05-13
		// System.out.println("Start vertex: " + startVertex);
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

		System.out.println("DEBUG-NearestPath: Calculando room para startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-05-13
		// Get the room of the initial item (or non-RS user).
		int room = getRoomFromItem(startVertex);

		System.out.println("DEBUG-NearestPath: Room obtenido=" + room); // Añadido por Nacho Palacio 2025-05-13
		
		// Store the visited room.
		roomVisited = addRoomVisited(room, roomVisited);
		
		/*
		System.out.println("Room visited: ");
		for (Integer integer: roomVisited)
			System.out.println(integer + ", ");
		*/
		
		System.out.println("DEBUG-NearestPath: Obteniendo itemsByRoom para room=" + room); // Añadido por Nacho Palacio 2025-05-13
		// Get the items (sculptures, paintings and doors) of a specified room.
		List<Long> itemsByRoom = getItemsByRoom(room);

		System.out.println("DEBUG-NearestPath: Items en room=" + room + ": " + itemsByRoom); // Añadido por Nacho Palacio 2025-05-13
		
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
			System.out.println("DEBUG: startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-05-13
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
			// System.out.println("DEBUG-NearestPath: startVertex inicio del while=" + startVertex); // Añadido por Nacho Palacio 2025-05-13

			// System.out.println("DEBUG-NearestPath: Inicio del bucle while, currentTime=" + currentTime + ", inputTime=" + inputTime + ", items visitados=" + itemVisited.size()); // Añadido por Nacho Palacio 2025-05-13
			
			//monitor = MonitorFactory.start("nearestPathWhileTimeAvailable");
			
			// System.out.println("\nCurrent time: " + currentTime);
			
			// Get the item (most likely of nearest) to visit by non-RS user (without
			// repeating).
			if (!ifItemToVisitWasCero) {
				// System.out.println("Parametros item to visit: " + startVertex + ", " + itemsByRoom + ", " + roomVisited + ", " + itemVisited + ", " + repeated);
				System.out.println("DEBUG-NearestPath: Calculando itemToVisit, startVertex=" + startVertex + ", itemsByRoom=" + itemsByRoom + ", roomVisited=" + roomVisited + ", itemVisited=" + itemVisited + ", repeated=" + repeated); // Añadido por Nacho Palacio 2025-05-13
				itemToVisit = getItemToVisit(startVertex, itemsByRoom, roomVisited, itemVisited, repeated);
				System.out.println("DEBUG-NearestPath: itemToVisit calculado=" + itemToVisit); // Añadido por Nacho Palacio 2025-05-13
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
				long convertedItemToVisit = ElementIdMapper.convertToRangeId(itemToVisit, ElementIdMapper.CATEGORY_ITEM);
				// System.out.println("Converted item to visit: " + convertedItemToVisit);
				// System.out.println("DEBUG-NearestPath: itemToVisit=" + itemToVisit); // Añadido por Nacho Palacio 2025-05-17
				// System.out.println("DEBUG-NearestPath: numberOfItemsInMap=" + numberOfItemsInMap); // Añadido por Nacho Palacio 2025-05-17
				
				// If the next item to visit is a painting or sculpture (range: 1-240):
				if (itemToVisit <= numberOfItemsInMap) {
				// if (convertedItemToVisit <= numberOfItemsInMap) {
					endVertex = itemToVisit;
					// Get a new vertex.

					// System.out.println("DEBUG-NearestPath: Añadiendo vértice al finalPath, startVertex=" + startVertex + ", endVertex=" + endVertex); // Añadido por Nacho Palacio 2025-05-13
					
					vertex = getCurrentVertex(startVertex, endVertex);
					// System.out.println("DEBUG-NearestPath: vertex=" + vertex); // Añadido por Nacho Palacio 2025-05-18
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
					System.out.println("DEBUG (startVertex = endVertex): startVertex actualizado a " + startVertex + " (desde endVertex)"); // Añadido por Nacho Palacio 2025-05-18
					
				} else {
					
					//System.out.println("Door or stairs");
					
					// If the next item to visit is a door or stairs (range: 241-312):
					// Get the connection of the current door.
					// long connectedDoor = getConnectedDoor(itemToVisit);

					// Modificado por Nacho Palacio 2025-05-19
					// Check if the item to visit is a door.
					if (!ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR)) {
						System.out.println("ERROR: Se intentó buscar puerta conectada para un ID que no es puerta: " + itemToVisit);
						continue;
					}
					long connectedDoor = getConnectedDoor(itemToVisit);
					// ... resto del código ...
				
					
					//System.out.println("Connected door: " + connectedDoor);
					
					// Get the sub-path necessary to go from one item (sculpture or painting) to
					// another through doors.
					System.out.println("DEBUG-NearestPath: StartVertex antes de getConnectedDoor=" + startVertex); // Añadido por Nacho Palacio 2025-05-18
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
					itemsByRoom = updateItemsByRoom(room, itemVisited, roomVisited);
					// Get the end vertex from sub-path.
					// System.out.println("DEBUG-NearestPath: Subpath pasado a getEndVertex= " + subpath); // Añadido por Nacho Palacio 2025-05-18
					startVertex = getEndVertex(subpath);
					System.out.println("DEBUG (startVertex = getEndVertex(subpath).1): startVertex actualizado a " + startVertex + " (desde getEndVertex con subpath=" + subpath + ")"); // Añadido por Nacho Palacio 2025-05-18
					// System.out.println("DEBUG-NearestPath: startVertex actualizado=" + startVertex); // Añadido por Nacho Palacio 2025-05-17
					ifItemToVisitWasCero = false;//ifItemToVisitWasCero = (startVertex > 0);
					
					/*
					if (room <= 0)
						ifItemToVisitWasCero = true;
					else
						ifItemToVisitWasCero = false;
					*/
				}
			} else {
				// System.out.println("DEBUG-NearestPath: else, startVertex= " + startVertex); // Añadido por Nacho Palacio 2025-05-18
				
				// System.out.println("Room empty or itemToVisit <= 0");
				
				// Treat the case where there are no items to visit (itemToVisit = 0) or
				// (itemsByRoom.isEmpty)
				if (!itemsByRoom.isEmpty() && !allRoomVisited) {
					
					// System.out.println("Items by room not empty");
					
					// All the items have been visited by the non-RS user and there is no other
					// option that to go to that visited room because there is time.
					// System.out.println("DEBUG-NearestPath: Calculando room en else->if para startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-05-18
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
							long connectedDoor = getConnectedDoor(itemCandidateToVisit);
							itemsByRoom = getItemsByRoom(getRoomFromItem(connectedDoor));
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
					// System.out.println("DEBUG-NearestPath: Calculando room en else->else para startVertex=" + startVertex); // Añadido por Nacho Palacio 2025-05-18

					
					room = getRoomFromItem(startVertex);
					
					/*
					if (room <= 0) {
						System.out.println("Room was <= 0");
						room = random.nextInt(accessGraphFile.getNumberOfRoom() - 1 + 1) + 1;
						System.out.println("New room: " + room);
					}*/
					
					int numberOfDoors = accessGraphFile.getNumDoorsByRoom(room);
					int indexDoor = random.nextInt(numberOfDoors - 1 + 1) + 1;
					itemToVisit = accessGraphFile.getDoorOfRoomWithIndex(indexDoor, room);
					long connectedDoor = getConnectedDoor(itemToVisit);
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
					itemsByRoom = updateItemsByRoom(room, itemVisited, roomVisited);
					startVertex = getEndVertex(subpath);
					System.out.println("DEBUG (startVertex = getEndVertex(subpath).2): startVertex actualizado a " + startVertex + " (desde else con subpath=" + subpath + ")"); // Añadido por Nacho Palacio 2025-05-18
					ifItemToVisitWasCero = false;
					allRoomVisited = false;
				}

			}
			
			//System.out.println(finalPath);
			//monitor.stop();

			System.out.println("DEBUG-NearestPath: Finalizando iteración del bucle while, currentTime=" + currentTime + ", items visitados=" + itemVisited.size()); // Añadido por Nacho Palacio 2025-05-13
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
		
		System.out.println("DEBUG-NearestPath: finalPath generado=" + finalPath); // Añadido por Nacho Palacio 2025-05-13

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
				// Get the vertices.
				long v1 = Long.valueOf(array[0]).longValue();
				long v2 = Long.valueOf(array[1]).longValue();
				
				// If first one, add initial vertex.
				if (i == 0)
					alreadyVisited.add(v1);
				
				// Add final vertex if not already contained.
				if (!alreadyVisited.contains(v2))
					alreadyVisited.add(v2);
				
			}
		}
		
		return alreadyVisited;
	}

}
