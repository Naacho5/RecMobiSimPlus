package es.unizar.recommendation.path;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/* http://www.javaperformancetuning.com/tools/jamon/index.shtml */
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;
import es.unizar.util.Distance;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

/**
 * 
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public abstract class Path {
	public Map<Long, String> diccionaryItemLocation;
	public Map<Long, Long> mapDoorConnected;
	public Map<Integer, LinkedList<Long>> itemsDoorVisited;
	public DataAccessGraphFile accessGraphFile;
	public DataAccessItemFile accessItemFile;
	public DataAccessRoomFile accessRoomFile;
	public Random random;

	public Map<Integer, LinkedList<Long>> doorsByRoomMap; // Añadido por Nacho Palacio 2025-04-25
	
	public int numberOfRooms;
	public int numberOfRoomsAndSubrooms;
	public int numberOfItems;
	public int numberOfDoors;
	public int numberOfInvisibleDoors;
	public int numberOfStairs;

	private Map<Long, Integer> itemToRoomMap; // Añadido por Nacho Palacio 2025-04-23
	
	// =========== Logger ================================:
	public static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);

	public Path() {
		this.diccionaryItemLocation = new HashMap<>();
		this.mapDoorConnected = new HashMap<>();
		this.itemsDoorVisited = new HashMap<>();
		this.accessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		this.accessItemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		this.accessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
		this.doorsByRoomMap = new HashMap<>(); // Añadido por Nacho Palacio 2025-04-25
		random = new Random(Configuration.simulation.getSeed());
		//random.setSeed(100);

		accessGraphFile.reset();
		Monitor mon = MonitorFactory.start("pathOperations");
		// Load a map that contains the item locations (sculptures, paintings, doors and
		// stairs).
		this.diccionaryItemLocation = MainSimulator.floor.diccionaryItemLocation;
		// Load a map that contains the connected doors.
		this.mapDoorConnected = mapDoorConnected();
		// Load a map that contains the items by room. Initially, it is assumed that not
		// items have been visited.
		this.itemsDoorVisited = mapItemRoomVisited();
		mon.stop();
		log.log(Level.FINEST, mon.toString());
		
		log.log(Level.FINEST, "GETPROPERTYVALUE MONITORIZATION: ");
		log.log(Level.FINEST, accessGraphFile.getDataAccessMon().toString());
		
		getNumberRoomsItemsDoorsStairs();

		initializeItemToRoomMap(); // Añadido por Nacho Palacio 2025-04-23
	}

	private void getNumberRoomsItemsDoorsStairs() {
		
		this.numberOfRooms = accessGraphFile.getNumberOfRoom();
		this.numberOfRoomsAndSubrooms = accessGraphFile.getNumberOfRoomsAndSubrooms();
		this.numberOfItems = accessItemFile.getNumberOfItems();
		
		int numDoors = 0;
		int numInvisibleDoors = 0;
		for (int i = 1; i <= this.numberOfRooms; i++) {
			// Add doors per room
			numDoors += accessGraphFile.getNumberOfDoorsByRoom(i);
			// Add invisible doors per room
			for (int j = 1; j <= accessGraphFile.getRoomNumberSubrooms(i); j++) {
				numInvisibleDoors += accessGraphFile.getNumberOfInvisibleDoorsBySubroom(j, i);
			}
		}
		
		this.numberOfDoors = numDoors;
		this.numberOfInvisibleDoors = numInvisibleDoors;
		this.numberOfStairs = accessGraphFile.getNumberOfStairs();
	}

	/**
	 * Abstract method to implement for sub-classes, that allows generate a path for the non-RS user.
	 * 
	 * @param startVertex: The start vertex.
	 * @return The generated path for the non-RS user.
	 */
	public abstract String generatePath(long startVertex);

	/**
	 * Load a map that contains the connected doors.
	 * Modificado por Nacho Palacio 2025-05-31
	 * @return
	 */
	public Map<Long, Long> mapDoorConnected() {
		Map<Long, Long> dictionary = new HashMap<>();
		int numberDoorConnected = accessGraphFile.getNumberOfConnectedDoor();
		Monitor mon = null;
		
		for (int posDoor = 1; posDoor <= numberDoorConnected; posDoor++) {
			mon = MonitorFactory.start("mapDoorConnected");
			String doors = accessGraphFile.getConnectedDoor(posDoor);
			long door1 = accessGraphFile.getDoorOfRoom(doors.split(", ")[0]);
			long door2 = accessGraphFile.getDoorOfRoom(doors.split(", ")[1]);
			
			dictionary.put(door1, door2);
			dictionary.put(door2, door1);
			
			mon.stop();
		}
		
		try {
			int numberInvisibleDoorConnected = accessGraphFile.getNumberOfConnectedInvisibleDoor();
			for (int posInvisibleDoor = 1; posInvisibleDoor <= numberInvisibleDoorConnected; posInvisibleDoor++) {
				String invisibleDoors = accessGraphFile.getConnectedInvisibleDoor(posInvisibleDoor);
				long invisibleDoor1 = accessGraphFile.getInvisibleDoorOfSubroom(invisibleDoors.split(", ")[0]);
				long invisibleDoor2 = accessGraphFile.getInvisibleDoorOfSubroom(invisibleDoors.split(", ")[1]);
				dictionary.put(invisibleDoor1, invisibleDoor2);
				dictionary.put(invisibleDoor2, invisibleDoor1);
			}
		} catch (Exception e) {}
		
		return dictionary;
	}

	/**
	 * Load a map that contains the items by room. Initially, it is assumed that not items have been visited.
	 * 
	 * @return
	 */
	public Map<Integer, LinkedList<Long>> mapItemRoomVisited() {
		Map<Integer, LinkedList<Long>> itemsDoor = new HashMap<>();
		int numberRoom = accessGraphFile.getNumberOfRoom();
		Monitor mon = null;
		for (int posRoom = 1; posRoom <= numberRoom; posRoom++) {
			
			LinkedList<Long> items = new LinkedList<>();
			
			int subrooms = accessGraphFile.getRoomNumberSubrooms(posRoom);
			// As before
			if (subrooms <= 0) {
				mon = MonitorFactory.start("mapItemRoomVisited");
				int numberItems = accessGraphFile.getNumberOfItemsByRoom(posRoom);
				for (int posItem = 1; posItem <= numberItems; posItem++) {
					long item = accessGraphFile.getItemOfRoom(posItem, posRoom);
					items.add(item);
				}
				int numberDoor = accessGraphFile.getNumberOfDoorsByRoom(posRoom);
				for (int posDoor = 1; posDoor <= numberDoor; posDoor++) {
					long door = accessGraphFile.getDoorOfRoom(posDoor, posRoom);
					items.add(door);
				}
				itemsDoor.put(posRoom, items);
				mon.stop();
			}
			// Map Items for each subroom
			else {
				for (int posSubroom = 1; posSubroom <= subrooms; posSubroom++) {
					// Get subroomID
					int subroomID = accessGraphFile.getSubroom(posSubroom, posRoom);
					
					int numberItems = accessGraphFile.getNumberOfItemsBySubroom(posSubroom, posRoom);
					for (int posItem = 1; posItem <= numberItems; posItem++) {
						long item = accessGraphFile.getItemOfSubroom(posItem, posSubroom, posRoom);
						items.add(item);
					}
					
					int numberDoor = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, posRoom);
					for (int posDoor = 1; posDoor <= numberDoor; posDoor++) {
						long door = accessGraphFile.getDoorOfSubroom(posDoor, posSubroom, posRoom);
						items.add(door);
					}
					int numberInvisibleDoor = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
					
					for (int posInvisibleDoor = 1; posInvisibleDoor <= numberInvisibleDoor; posInvisibleDoor++) {
						long door = accessGraphFile.getInvisibleDoorOfSubroom(posInvisibleDoor, posSubroom, posRoom);
						items.add(door);
					}
					
					itemsDoor.put(subroomID, items);
					
					items = new LinkedList<>();
				}
			}
		}
		log.log(Level.FINEST, " [mapItemRoomVisited] : \n   - " + mon);
		
		return itemsDoor;
	}

	/**
	 * Reverse the X and Y values of a given location.
	 * 
	 * @param location: The specified location.
	 * @return The X and Y values reversed.
	 */
	public String reverseLocation(String location) {
		String x_y[] = location.split(", ");
		double X = Double.valueOf(x_y[0]);
		double Y = Double.valueOf(x_y[1]);
		return Y + ", " + X;
	}

	/**
	 * Get a new vertex to visit by the non-RS user, specifying the start and end vertices.
	 * 
	 * @param startVertex: The start vertex.
	 * @param endVertex:   The end vertex.
	 * @return A pair of vertices.
	 */
	public String getCurrentVertex(long startVertex, long endVertex) {
		// Añadido por Nacho Palacio 2025-07-05
		System.out.println("🎯 DEBUG getCurrentVertex:");
		System.out.println("  - startVertex: " + startVertex);
		System.out.println("  - endVertex: " + endVertex);

		// Modificado por Nacho Palacio 2025-05-31
		if (!isValidConnection(startVertex, endVertex)) {
			System.out.println("  - ❌ Conexión inválida, devolviendo cadena vacía");
        	return "";
		}
		
		return "(" + startVertex + " : " + endVertex + "), ";
	}

	/**
	 * Get the time the non-RS user needs to move from one item (painting or sculpture) to another.
	 * 
	 * @param startVertex: The start vertex of the non-RS user.
	 * @param endVertex:   The end vertex of the non-RS user.
	 * @return The current time required by non-RS user to move from one item to another.
	 */
	public double getCurrentTime(long startVertex, long endVertex) throws NullPointerException{
		double currentTime = 0;
		try {
			String locationStartVertex = diccionaryItemLocation.get(startVertex);
			String locationEndVertex = diccionaryItemLocation.get(endVertex);

			// Añadido por Nacho Palacio 2025-04-23			
			if (locationStartVertex == null && ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR)) {
				long externalId = ElementIdMapper.getBaseId(startVertex);
				locationStartVertex = diccionaryItemLocation.get(externalId);
				if (locationStartVertex != null) {
					diccionaryItemLocation.put(startVertex, locationStartVertex);
				}
			}
			// Añadido por Nacho Palacio 2025-04-23
			if (locationEndVertex == null && ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_DOOR)) {
				long externalId = ElementIdMapper.getBaseId(endVertex);
				locationEndVertex = diccionaryItemLocation.get(externalId);
				if (locationEndVertex != null) {
					diccionaryItemLocation.put(endVertex, locationEndVertex);
				}
			}

			/* Añadido por Nacho Palacio 2025-04-16. */
			if (locationStartVertex == null || locationEndVertex == null) {
				return 0.0;
			}
			
//			System.out.println(startVertex + ", " + endVertex);
//			System.out.println(locationStartVertex + " - " +locationEndVertex + "\n");
			
			String[] arrayStartVertex = locationStartVertex.split(", ");
			double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
			double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();
		
			String[] arrayEndVertex = locationEndVertex.split(", ");
			double x2 = Double.valueOf(arrayEndVertex[0]).doubleValue();
			double y2 = Double.valueOf(arrayEndVertex[1]).doubleValue();
			double distance = Distance.distanceBetweenTwoPoints(x1, y1, x2, y2);
			
			currentTime = distance / Configuration.simulation.getUserVelocityInPixelBySecond();
			// CORRECT!
			// System.out.println("Distance: " + distance + " with speed: " + Configuration.simulation.getUserVelocityInPixelBySecond() + " takes " + currentTime + " s");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return currentTime;
	}

	/**
	 * Get the visited rooms by the user.
	 * 
	 * @return List of visited rooms.
	 */
	public List<Integer> getRoomVisited(User currentUser) {
		List<Integer> roomVisited = new LinkedList<>();
		for (long item : Simulation.itemRatedOfUsers.get(currentUser.userID)) {
			// int r = getRoomFromItem(item);
			int r = Configuration.simulation.getRoom(item); // Modificado por Nacho Palacio 2025-06-19
			if (roomVisited.isEmpty() || !roomVisited.contains(r)) {
				roomVisited.add(r);
			}
		}
		return roomVisited;
	}

	/**
	 * Get the room to which the item (or non-RS user) or door belongs.
	 * 
	 * @param startVertex: The start vertex.
	 * @return The room of the specified item or door.
	 */
	public int getRoomFromItem(long startVertex) {
		// Añadido por Nacho Palacio 2025-04-23.
		long originalId = startVertex;
    
		// Mirar en el mapa
		Integer roomFromMap = itemToRoomMap.get(startVertex);
		if (roomFromMap != null && roomFromMap > 0) {
			return roomFromMap;
		}
		
		// Si no está, probar a convertir el id
		long externalId = startVertex;
		if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM)) {
			externalId = ElementIdMapper.getBaseId(startVertex);
			
			roomFromMap = itemToRoomMap.get(externalId);
			if (roomFromMap != null && roomFromMap > 0) {
				itemToRoomMap.put(originalId, roomFromMap);
				return roomFromMap;
			}
		} else if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR)) {
			externalId = ElementIdMapper.getBaseId(startVertex);

			roomFromMap = itemToRoomMap.get(externalId);
			if (roomFromMap != null && roomFromMap > 0) {
				itemToRoomMap.put(originalId, roomFromMap);
				return roomFromMap;
			}
		} else if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_STAIRS)) {
			externalId = ElementIdMapper.getBaseId(startVertex);
			
			roomFromMap = itemToRoomMap.get(externalId);
			if (roomFromMap != null && roomFromMap > 0) {
				itemToRoomMap.put(originalId, roomFromMap);
				return roomFromMap;
			}
		}

		// Sino, buscar manuealmente
		int numberOfRooms = this.numberOfRooms;
		int currentRoom = 0;
		
		long idToSearch = externalId;

		for (int i = 1; i <= numberOfRooms;) {
			
			int numberOfSubrooms = accessGraphFile.getRoomNumberSubrooms(i);
			// If there are no subrooms
			if (numberOfSubrooms <= 0) {
				// Get number of objects depending on starVertex
				// Modificado por Nacho Palacio 2025-04-23
				int numberOfObjects = (idToSearch <= this.numberOfItems) ? 
                    accessGraphFile.getNumberOfItemsByRoom(i) : 
                    accessGraphFile.getNumberOfDoorsByRoom(i);

				for (int j = 1; j <= numberOfObjects; j++) {
					// Get object depending on startVertex
					// Modificado por Nacho Palacio 2025-04-23
					long object = (idToSearch <= this.numberOfItems) ? 
                        accessGraphFile.getItemOfRoom(j, i) : 
                        accessGraphFile.getDoorOfRoom(j, i);

					if (object == idToSearch) { // Modificado por Nacho Palacio 2025-04-23
						currentRoom = i;
						// i = numberOfRooms;
						// break;
						
						// Añadido por Nacho Palacio 2025-04-23
						itemToRoomMap.put(originalId, currentRoom);
						itemToRoomMap.put(idToSearch, currentRoom);
						
						return currentRoom;
					}
				}
			}
			// There are subrooms
			else {
				// Modificado por Nacho Palacio 2025-06-06. Cambiados startVertex por idToSearch
				for (int posSubroom = 1; posSubroom <= numberOfSubrooms; posSubroom++) {
					// Get number of objects depending on starVertex
					int numberOfObjects = -1;
					if (idToSearch <= this.numberOfItems) { 
						numberOfObjects = accessGraphFile.getNumberOfItemsBySubroom(posSubroom, i);
					}
					else if (idToSearch <= this.numberOfItems + this.numberOfDoors) {
						numberOfObjects = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, i);
					}
					else {
						numberOfObjects = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, i);
					}
					
					for (int j = 1; j <= numberOfObjects; j++) {
						// Get object depending on startVertex
						long object = -1;
						if (idToSearch <= this.numberOfItems) {
							object = accessGraphFile.getItemOfSubroom(j, posSubroom, i);
						}
						else if (idToSearch <= this.numberOfItems + this.numberOfDoors) {
							object = accessGraphFile.getDoorOfSubroom(j, posSubroom, i);
						}
						else {
							object = accessGraphFile.getInvisibleDoorOfSubroom(j, posSubroom, i);
						}
						
						// Check object
						if (object == idToSearch) {
							
							currentRoom = accessGraphFile.getSubroom(posSubroom, i);
							i = numberOfRooms;
							posSubroom = numberOfSubrooms;
							break;
						}
					}
				}
			}
			
			i++;
		}

		// Añadido por Nacho Palacio 2025-06-24
		if (currentRoom == 0) {
			currentRoom = searchInAllSubrooms(idToSearch, originalId);
			
			if (currentRoom > 0) {
				return currentRoom;
			}
			else {
				currentRoom = searchInAllSubrooms(originalId, originalId);
			
				if (currentRoom > 0) {
					return currentRoom;
				}
			}
		}

		// Añadido por Nacho Palacio 2025-04-23
		if (currentRoom == 0) {
			return 1;
		}

		return currentRoom;
	}

	/**
	 * Store the visited rooms by user.
	 * 
	 * @param room:        The current room that has been visited by the non-RS user.
	 * @param roomVisited: The list of rooms visited by non-RS user in the past.
	 */
	public List<Integer> addRoomVisited(int room, List<Integer> roomVisited) {
		if (!roomVisited.contains(room)) {
			roomVisited.add(room);
		}
		return roomVisited;
	}


	// Modificado por Nacho Palacio 2025-04-25
	public List<Long> getItemsByRoom(int room) {
		List<Long> itemsByRoom = new LinkedList<>();
		List<Long> doorsByRoom = new LinkedList<>();  // Lista separada para puertas
		
		if (room <= this.numberOfRooms) {
			// Get the number of items from specified room.
			int numberOfItemsByRoom = accessGraphFile.getNumberOfItemsByRoom(room);
			for (int j = 1; j <= numberOfItemsByRoom; j++) {
				long itemId = accessGraphFile.getItemOfRoom(j, room);
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM)) {  // Modificado por Nacho Palacio 2025-05-17
					itemsByRoom.add(itemId);
					
					// También id interno
					long internalId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_ITEM);

					if (!itemsByRoom.contains(internalId)) {
						itemsByRoom.add(internalId);
					}
				}
			}
			
			// Get the doors from specified room (conservadas separadamente).
			int numberOfDoorsByRoom = accessGraphFile.getNumberOfDoorsByRoom(room);
			for (int j = 1; j <= numberOfDoorsByRoom; j++) {
				long doorId = accessGraphFile.getDoorOfRoom(j, room);
				
				if (ElementIdMapper.isInCorrectRange(doorId, ElementIdMapper.CATEGORY_DOOR)) { // Modificado por Nacho Palacio 2025-05-17
					doorsByRoom.add(doorId);
					
					// También id interno
					long internalId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);

					if (!doorsByRoom.contains(internalId)) {
						doorsByRoom.add(internalId);
					}
				}
			}
		}
		else {
			int posRoom = 0, posSubroom = 0;
			for (int i = 1; i <= numberOfRooms; i++) {
				int numberOfSubrooms = accessGraphFile.getRoomNumberSubrooms(i);
				for (int sub = 1; sub <= numberOfSubrooms; sub++) {
					if (accessGraphFile.getSubroom(sub, i) == room) {
						posRoom = i;
						posSubroom = sub;
						break;
					}
				}
				if (posRoom > 0 || posSubroom > 0) {
					break;
				}
			}
			
			// Get the number of items from specified subroom.
			int numberOfItemsBySubroom = accessGraphFile.getNumberOfItemsBySubroom(posSubroom, posRoom);
			for (int j = 1; j <= numberOfItemsBySubroom; j++) {
				long itemId = accessGraphFile.getItemOfSubroom(j, posSubroom, posRoom);
				
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM)) { // Modificado por Nacho Palacio 2025-05-17
					itemsByRoom.add(itemId);
					
					// También id interno
					long internalId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_ITEM);
					if (!itemsByRoom.contains(internalId)) {
						itemsByRoom.add(internalId);
					}
				}
			}
			
			// Get the doors from specified subroom (conservadas separadamente).
			int numberOfDoorsBySubroom = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, posRoom);
			for (int j = 1; j <= numberOfDoorsBySubroom; j++) {
				long doorId = accessGraphFile.getDoorOfSubroom(j, posSubroom, posRoom);
				
				if (ElementIdMapper.isInCorrectRange(doorId, ElementIdMapper.CATEGORY_DOOR)) { // Modificado por Nacho Palacio 2025-05-17
					doorsByRoom.add(doorId);
					
					// También id interno
					long internalId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);
					if (!doorsByRoom.contains(internalId)) {
						doorsByRoom.add(internalId);
					}
				}
			}
			
			// Get the invisible doors from specified subroom.
			int numberOfInvisibleDoorsBySubroom = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
			for (int j = 1; j <= numberOfInvisibleDoorsBySubroom; j++) {
				long doorId = accessGraphFile.getInvisibleDoorOfSubroom(j, posSubroom, posRoom);
				doorsByRoom.add(doorId);
				
				// También id interno
				long internalId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);
				if (!doorsByRoom.contains(internalId)) {
					doorsByRoom.add(internalId);
				}
			}
		}
		
		itemsDoorVisited.put(room, new LinkedList<>(itemsByRoom));
		
		if (this.doorsByRoomMap == null) {
			this.doorsByRoomMap = new HashMap<>();
		}
		
		doorsByRoomMap.put(room, new LinkedList<>(doorsByRoom));
		
		List<Long> combinedList = new LinkedList<>(itemsByRoom);
		combinedList.addAll(doorsByRoom);
		
		return combinedList;
	}

	/**
	 * Get the item (most likely or nearest) to visit by non-RS user (without repeating).
	 * 
	 * @param itemsByRoom: List of items from specified room.
	 * @param currentUser: User to remove the items by room.
	 * @return The item most likely or nearest to visit.
	 */

	public List<Long> removeItemRated(List<Long> itemsByRoom, User currentUser) {
		for (long item : Simulation.itemRatedOfUsers.get(currentUser.userID)) {
			if (itemsByRoom.contains(item)) {
				itemsByRoom.remove(item);
			}
		}
		return itemsByRoom;
	}

	public long getItemToVisit(long startVertex, List<Long> itemsByRoom, List<Integer> roomVisited, List<Long> itemVisited, List<Long> repeated) {
		// Añadido por Nacho Palacio 2025-07-05
		// System.out.println("🎯 DEBUG getItemToVisit:");
		// System.out.println("  - startVertex: " + startVertex);
		// System.out.println("  - itemsByRoom: " + itemsByRoom);
		// System.out.println("  - itemVisited: " + itemVisited);
		// System.out.println("  - repeated: " + repeated);

		long itemToVisit = 0;
		double initialDistance = 9999999;

		// Añadido por Nacho Palacio 2025-07-05
		double closestDistance = initialDistance;
    	long closestItem = 0;

		String startVertexLocation = diccionaryItemLocation.get(startVertex);

		// System.out.println("  - startVertexLocation: " + startVertexLocation); // Añadido por Nacho Palacio 2025-7-05

		// Añadido por Nacho Palacio 2025-05-17
		if (startVertexLocation == null) {
			// return 0;

			// System.out.println("  - ❌ startVertexLocation es NULL para startVertex=" + startVertex);
        
			long externalStartVertex = startVertex;
			if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM)) {
				externalStartVertex = ElementIdMapper.getBaseId(startVertex);
				startVertexLocation = diccionaryItemLocation.get(externalStartVertex);
				// System.out.println("  - Probando con ID externo " + externalStartVertex + ": " + startVertexLocation);
			}
			
			if (startVertexLocation == null) {
				// System.out.println("  - ❌ No se pudo obtener ubicación, devolviendo 0");
				return 0;
			}
		}

		String[] arrayStartVertex = startVertexLocation.split(", ");
		double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
		double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();

		// Añadido por Nacho Palacio 2025-07-05
		// System.out.println("  - Coordenadas de inicio: (" + x1 + ", " + y1 + ")");
    	// System.out.println("  - Evaluando " + itemsByRoom.size() + " elementos candidatos:");
		
		/*
		System.out.println("Repeated: ");
		for (Long l: repeated) {
			System.out.print(l + ", ");
		}
		System.out.println();
		*/
		for (Long endVertex : itemsByRoom) {
			// Añadido por Nacho Palacio 2025-07-05
			// System.out.println("    🔍 Evaluando endVertex: " + endVertex);
			boolean isRepeated = repeated.contains(endVertex) && Collections.frequency(repeated, endVertex) > 5;
			// System.out.println("      - ¿Repetido más de 5 veces? " + isRepeated);

			if (!(repeated.contains(endVertex) && Collections.frequency(repeated, endVertex) > 5)) {
				// Añadido por Nacho Palacio 2025-06-28
				long externalEndVertex = endVertex;
				if (ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_ITEM)) {
					externalEndVertex = ElementIdMapper.getBaseId(endVertex);
					// System.out.println("      - ID externo para ubicación: " + externalEndVertex); // Añadido por Nacho Palacio 2025-07-05
				} else if (ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_DOOR)) {
					externalEndVertex = ElementIdMapper.getBaseId(endVertex);
					// System.out.println("      - ID externo para ubicación (puerta): " + externalEndVertex); // Añadido por Nacho Palacio 2025-07-05
				}

				String endVertexLocation = diccionaryItemLocation.get(externalEndVertex);
				System.out.println("      - endVertexLocation: " + endVertexLocation); // Añadido por Nacho Palacio 2025-07-05
				if (endVertexLocation == null) {
					// System.out.println("      - ❌ Ubicación NULL, saltando"); // Añadido por Nacho Palacio 2025-07-05
					continue;
            	}

				String[] arrayEndVertex = endVertexLocation.split(", ");
				double x2 = Double.valueOf(arrayEndVertex[0]).doubleValue();
				double y2 = Double.valueOf(arrayEndVertex[1]).doubleValue();
				double currentDistance = Distance.distanceBetweenTwoPoints(x1, y1, x2, y2);

				// Añadido por Nacho Palacio 2025-07-05
				// System.out.println("      - Coordenadas: (" + x2 + ", " + y2 + ")");
				// System.out.println("      - Distancia: " + currentDistance);
				// System.out.println("      - ¿startVertex != endVertex? " + (startVertex != endVertex));
				// System.out.println("      - ¿No visitado? " + (!itemVisited.contains(endVertex)));
	
				if (currentDistance < initialDistance) {
					if ((startVertex != endVertex) && (!itemVisited.contains(endVertex))) {
						initialDistance = currentDistance;
						itemToVisit = endVertex;

						closestDistance = currentDistance;
                    	closestItem = endVertex;
					} // Añadido por Nacho Palacio 2025-07-05
					else {
                    	// System.out.println("      - ❌ Mismo vértice o ya visitado");
                	}
				}
				else {
                	// System.out.println("      - ❌ Distancia mayor que el actual más cercano");
            	}
			}
		}

		// Añadido por Nacho Palacio 2025-07-05
		// System.out.println("  - 🏆 RESULTADO FINAL:");
		// System.out.println("    - itemToVisit: " + itemToVisit);
		// System.out.println("    - distancia: " + closestDistance);
		// System.out.println("    - de " + itemsByRoom.size() + " candidatos evaluados");

		return itemToVisit;
	}

	/**
	 * Stores the visited item (painting or sculpture).
	 * 
	 * @param itemToVisit: The item to visit by non-RS user.
	 * @param itemVisited: List of items visited in the past.
	 * @return List of visited items.
	 */
	public List<Long> addItemVisited(long itemToVisit, List<Long> itemVisited) {
		// Verify the visited items.
		if (!itemVisited.contains(itemToVisit)) { // && itemToVisit <= this.numberOfItems) {
			// The item is marked as visited.
			itemVisited.add(itemToVisit);
		}
		return itemVisited;
	}

	/**
	 * Get the connection of the current door.
	 * Modificado por Nacho Palacio 2025-05-31
	 * @param currentDoor: The current door.
	 * @return The connection of the current door.
	 */
	public long getConnectedDoor(long currentDoor) {
		Long connectedDoorFromMap = mapDoorConnected.get(currentDoor);
		if (connectedDoorFromMap != null && connectedDoorFromMap > 0) {
			return connectedDoorFromMap;
		}
		
		List<Long> doors = new LinkedList<>();
		long connectedDoor = 0;
		
		int numberDoorConnected = accessGraphFile.getNumberOfConnectedDoor();
		for (int d = 1; d <= numberDoorConnected; d++) {
			String doorsConnected = accessGraphFile.getConnectedDoor(d);
			String[] array = doorsConnected.split(", ");
			String door1 = array[0];
			String door2 = array[1];
			
			if (accessGraphFile.getDoorOfRoom(door1) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door2));
			} else if (accessGraphFile.getDoorOfRoom(door2) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door1));
			}
		}
		
		int numberInvisibleDoorConnected = accessGraphFile.getNumberOfConnectedInvisibleDoor();
		for (int d = 1; d <= numberInvisibleDoorConnected; d++) {
			String invisibleDoorsConnected = accessGraphFile.getConnectedInvisibleDoor(d);
			String[] array = invisibleDoorsConnected.split(", ");
			String door1 = array[0];
			String door2 = array[1];
			
			if (accessGraphFile.getDoorOfRoom(door1) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door2));
			} else if (accessGraphFile.getDoorOfRoom(door2) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door1));
			}
		}
		
		if (doors.size() == 1) {
			connectedDoor = doors.get(0);
		} else if (doors.size() > 1) {
			connectedDoor = doors.get(random.nextInt(doors.size()));
		}
		
		// Añadido por Nacho Palacio 2025-05-31
		if (connectedDoor <= 0) {
			return 0;
		}

		return connectedDoor;
	}
	
	/**
	 * Get the connection of the current door TO STAIRS.
	 * 
	 * @param currentDoor: The current door.
	 * @return The connection of the current door TO STAIRS.
	 */
	public List<Long> getConnectedStairs(long currentDoor) {
		List<Long> stairsList = new LinkedList<>();
		// Find a room to which the non-RS user can go from the current door.
		int numberDoorStairsConnected = accessGraphFile.getNumberOfConnectedDoorStairs();
		for (int ds = 1; ds <= numberDoorStairsConnected; ds++) {
			String doorStairsConnected = accessGraphFile.getConnectedDoorStairs(ds);
			String[] array = doorStairsConnected.split(", ");
			String stairs = array[0];
			String door = array[1];
			
			if (accessGraphFile.getDoorOfRoom(door) == currentDoor) {
				stairsList.add(accessGraphFile.getStairsOfRoom(stairs));
			}
		}
		
		/*
		if (stairsList.size() <= 0) {
			MainMuseumSimulator.printConsole("NO DOOR NOR STAIRS CONNECTED TO: " + currentDoor, Level.SEVERE);
			System.out.println("NO DOOR NOR STAIRS CONNECTED TO: " + currentDoor);
		}
		*/
		
		return stairsList;
	}

	/**
	 * Get the sub-path necessary to go from one item (sculpture or painting) to another through doors.
	 * 
	 * @param start:         The start vertex.
	 * @param itemToVisit:   The item to visit by non-RS user.
	 * @param itemVisited:   List of visited items.
	 * @param connectedDoor: The connecting door that allows you to go from one room to another.
	 * @return The sub-path necessary to go from one item (sculpture or painting) to another through doors.
	 */

	public String getToConnectedDoor(long start, long itemToVisit, List<Long> itemVisited, long connectedDoor) {
		String subpath = "";

		// Añadido por Nacho Palacio 2025-07-05
		System.out.println("🔧 DEBUG getToConnectedDoor:");
		System.out.println("  - start: " + start);
		System.out.println("  - itemToVisit: " + itemToVisit);
		System.out.println("  - connectedDoor: " + connectedDoor);

		// Añadido por Nacho Palacio 2025-05-31
		if (connectedDoor <= 0) {
			System.out.println("  - ❌ connectedDoor <= 0, devolviendo subpath vacío");
			return subpath;
		}

		long startVertex = start;
		long endVertex = itemToVisit;

		if (endVertex < numberOfItems)
			itemVisited.add(endVertex);

		// subpath += getCurrentVertex(startVertex, endVertex);
		// startVertex = endVertex;

		// Añadido por Nacho Palacio 2025-07-05
		String vertex1 = getCurrentVertex(startVertex, endVertex);
		System.out.println("  - vertex1: " + vertex1);
		subpath += vertex1;
		startVertex = endVertex;

		endVertex = connectedDoor;

		if (endVertex < numberOfItems)
			itemVisited.add(endVertex);

		// subpath += getCurrentVertex(startVertex, endVertex);
		// startVertex = endVertex;

		// Añadido por Nacho Palacio 2025-07-05
		String vertex2 = getCurrentVertex(startVertex, endVertex);
		System.out.println("  - vertex2: " + vertex2); // AÑADIR ESTE DEBUG
		subpath += vertex2;
		startVertex = endVertex;

		return subpath;
	}

	/**
	 * Get the time the user needs to move from an item (sculpture, painting, door or stairs) to a door or stair.
	 * 
	 * @param subpath: The sub-path.
	 * @return The time the user needs to move from an item to a door or stair.
	 */

	public double getCurrentTimeConnectedDoors(String subpath) {
		double currentTime = 0;

		/* Añadido por Nacho Palacio 2025-04-16. */
		if (subpath == null || subpath.trim().isEmpty()) {
			return 0.0;
		}
		
		String edges[] = subpath.split(", ");
		for (int j = 0; j < edges.length; j++) {
			String edge = edges[j];
			if (!(edge == null || edge.equalsIgnoreCase(""))) {
				long startVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[0]).longValue();
				long endVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[1]).longValue();
				
				String locationStartVertex = diccionaryItemLocation.get(startVertex);
                String locationEndVertex = diccionaryItemLocation.get(endVertex);

				// Añadido por Nacho Palacio 2025-04-23.
				if (locationStartVertex == null && ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR)) {
					long externalId = ElementIdMapper.getBaseId(startVertex);
					locationStartVertex = diccionaryItemLocation.get(externalId);
				}
				
				if (locationEndVertex == null && ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_DOOR)) {
					long externalId = ElementIdMapper.getBaseId(endVertex);
					locationEndVertex = diccionaryItemLocation.get(externalId);
				}

                /* Añadido por Nacho Palacio 2025-04-16. */
                if (locationStartVertex == null || locationEndVertex == null) {
                    continue;
                }

				if (checkDoorsConnectedByStairs(startVertex, endVertex)) {
					currentTime += Configuration.simulation.getTimeOnStairs();
				}
				else {
					currentTime += getCurrentTime(startVertex, endVertex);
				}
			}
			
			/*
			if (((startVertex == 241 || startVertex == 242 || startVertex == 275 || startVertex == 276) && (endVertex == 279 || endVertex == 280 || endVertex == 308 || endVertex == 309))
					|| ((startVertex == 279 || startVertex == 280 || startVertex == 308 || startVertex == 309) && (endVertex == 241 || endVertex == 242 || endVertex == 275 || endVertex == 276))) {
				currentTime += Configuration.simulation.getTimeOnStairs();
			} else {
				currentTime += getCurrentTime(startVertex, endVertex);
			}
			*/
		}
		return currentTime;
	}

	/**
	 * Get the end vertex from sub-path.
	 * 
	 * @param subpath: The sub-path.
	 * @return The end vertex from sub-path.
	 */
	public long getEndVertex(String subpath) {
		// Añadido por Nacho Palacio 2025-07-05
		System.out.println("🏁 DEBUG getEndVertex:");
    	System.out.println("  - subpath recibido: '" + subpath + "'");

		// Añadido por Nacho Palacio 2025-05-17
		String edges[] = subpath.split(", ");

		if (edges.length == 0) { // Añadido por Nacho Palacio 2025-05-17
			System.out.println("  - ❌ edges.length == 0, devolviendo 0"); // Añadido por Nacho Palacio 2025-07-05
			return 0;
		}

		String edge = edges[edges.length - 1];

		// Añadido por Nacho Palacio 2025-05-17
		String[] cleanedEdge = Configuration.simulation.cleanEdge(edge);
		System.out.println("  - cleanedEdge length: " + cleanedEdge.length); // Añadido por Nacho Palacio 2025-07-05
		if (cleanedEdge.length < 2) {
			return 0;
		}

		long endVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[1]).longValue();
		System.out.println("  - endVertex calculado: " + endVertex); // Añadido por Nacho Palacio 2025-07-05
		
		return endVertex;
	}

	/**
	 * Update the available items to visit by non-RS user in the current room.
	 * 
	 * @param room:        The current room.
	 * @param itemVisited: List of visited items.
	 * @param roomVisited: List of visit rooms.
	 * @return The available items to visit by non-RS user in the current room.
	 */
	public List<Long> updateItemsByRoom(int room, List<Long> itemVisited, List<Integer> roomVisited) {
		
		if (room > 0 ) {
			List<Long> itemsByRoom = getItemsByRoom(room);
			
			if (!itemVisited.containsAll(itemsByRoom)) {
				//itemsByRoom = getItemsByRoom(room);
				// For the user to see only the unseen items.
				if (roomVisited.contains(room)) {
					List<Long> itemsNotVisitedYet = getItemsNotVisitedYet(itemVisited, itemsByRoom);
					itemsByRoom = itemsNotVisitedYet;
				}
				if (!roomVisited.contains(room)) {
					roomVisited.add(room);
				}
			}
			return itemsByRoom;
		}
		else {
			return new LinkedList<Long>();
		}
	}

	/**
	 * Get the items that have not yet been visited by non-RS user.
	 * 
	 * @param itemVisited: List of visited items.
	 * @param itemsByRoom: List of items in a room.
	 * @return List of items that have not yet been visited by non-RS user.
	 */
	public List<Long> getItemsNotVisitedYet(List<Long> itemVisited, List<Long> itemsByRoom) {
		List<Long> itemsNotVisitedYet = new LinkedList<>();
		for (int i = 0; i < itemsByRoom.size(); i++) {
			long item = itemsByRoom.get(i);
			if (!itemVisited.contains(item)) {
				itemsNotVisitedYet.add(item);
			}
		}
		return itemsNotVisitedYet;
	}
	
	/**
	 * Checks if two doors are connected by stairs (changing floor, adding time on stairs).
	 * 
	 * @param startVertex
	 * @param endVertex
	 * @return
	 */
	public boolean checkDoorsConnectedByStairs(long startVertex, long endVertex) {
		
		boolean connected = false;
		
		// int idInvisibleDoors = numberOfItems + numberOfDoors + numberOfStairs + 1;

		long externalStartVertex = startVertex;
		long externalEndVertex = endVertex;
		

		if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR)) {
			externalStartVertex = ElementIdMapper.getBaseId(startVertex);
		}
		
		if (ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_DOOR)) {
			externalEndVertex = ElementIdMapper.getBaseId(endVertex);
		}

		int numberOfItems = this.numberOfItems;
		int numberOfDoors = this.numberOfDoors;
		int numberOfStairs = this.numberOfStairs;
		
		int idInvisibleDoors = numberOfItems + numberOfDoors + numberOfStairs + 1;
		

		// if (startVertex > numberOfItems && startVertex < idInvisibleDoors && endVertex > numberOfItems && endVertex < idInvisibleDoors) { // They are doors
		if (externalStartVertex > numberOfItems && externalStartVertex < idInvisibleDoors && externalEndVertex > numberOfItems && externalEndVertex < idInvisibleDoors) { // Modificado por Nacho Palacio 2025-06-27
			List<Long> connectedStairsStartVertex = getConnectedStairs(startVertex);
			List<Long> connectedStairsEndVertex = getConnectedStairs(endVertex);
			
			if (connectedStairsStartVertex.size() > 0 && connectedStairsEndVertex.size() > 0) {
				
				/*
				for (Long lStart: connectedStairsStartVertex) {
					for (Long lEnd: connectedStairsEndVertex) {
						if (lStart == lEnd) {
							System.out.println("* " + lStart + "is connected to " + lEnd);
							connected = true;
							break;
						}
							
					}
					if (connected)
						break;
				}
				*/
				
				connected = true;
		
				return connected;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 * For deleting repeated objects, for example: "(1 : 1)"
	 * 
	 * @param path
	 * @return
	 */
	public String eraseRepeatedObjects(String path) {
		
		if (path != null && !path.equalsIgnoreCase("")) {
			// Split path into movements
			List<String> pathList = Arrays.asList(path.split(", "));
			
			// In order not to repeat, for example (22 : 22)
			// Reset path
			String newPath = "";
			// Add all movements that aren't repeated
			for (int i = 0; i < pathList.size(); i++) {
				String edge[] = Configuration.simulation.cleanEdge(pathList.get(i));
				if (!edge[0].equalsIgnoreCase(edge[1])) {
					newPath += pathList.get(i) + ", ";
				}
			}
			return newPath;
		}
		else {
			return path;
		}
		
	}

	/**
	 * Inicializa un mapa para acceso rápido a la habitación de un ítem.
	 * Añadido por Nacho Palacio 2025-04-24.
	 */
	private void initializeItemToRoomMap() {
		itemToRoomMap = new HashMap<>();
		
		for (int roomId = 1; roomId <= accessGraphFile.getNumberOfRoom(); roomId++) {
			// Items
			int numItems = accessGraphFile.getNumberOfItemsByRoom(roomId);
			for (int j = 1; j <= numItems; j++) {
				long itemId = accessGraphFile.getItemOfRoom(j, roomId);
				
				// Se guardan tanto ID externo como interno
				itemToRoomMap.put(itemId, roomId);
				long internalId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_ITEM);
				itemToRoomMap.put(internalId, roomId);
			}
			
			// Puertas
			int numDoors = accessGraphFile.getNumberOfDoorsByRoom(roomId);
			for (int j = 1; j <= numDoors; j++) {
				long doorId = accessGraphFile.getDoorOfRoom(j, roomId);
				
				itemToRoomMap.put(doorId, roomId);
				long internalId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);
				itemToRoomMap.put(internalId, roomId);
			}
			
			// Subhabitaciones
			int numSubrooms = accessGraphFile.getRoomNumberSubrooms(roomId);
			for (int subRoomPos = 1; subRoomPos <= numSubrooms; subRoomPos++) {
				int subRoomId = accessGraphFile.getSubroom(subRoomPos, roomId);
				
				int numSubItems = accessGraphFile.getNumberOfItemsBySubroom(subRoomPos, roomId);
				for (int j = 1; j <= numSubItems; j++) {
					long itemId = accessGraphFile.getItemOfSubroom(j, subRoomPos, roomId);
					
					itemToRoomMap.put(itemId, subRoomId);
					long internalId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_ITEM);
					itemToRoomMap.put(internalId, subRoomId);
				}
				
				// Añadir puertas de subhabitación
				int numSubDoors = accessGraphFile.getNumberOfDoorsBySubroom(subRoomPos, roomId);
				for (int j = 1; j <= numSubDoors; j++) {
					long doorId = accessGraphFile.getDoorOfSubroom(j, subRoomPos, roomId);
					
					itemToRoomMap.put(doorId, subRoomId);
					long internalId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);
					itemToRoomMap.put(internalId, subRoomId);
				}
			}
		}

		// Añadido por Nacho Palacio 2025-04-23
		// Añadir información de puertas conectadas
		int numberDoorConnected = accessGraphFile.getNumberOfConnectedDoor();

		for (int posDoor = 1; posDoor <= numberDoorConnected; posDoor++) {
			String doors = accessGraphFile.getConnectedDoor(posDoor);
			String[] doorParts = doors.split(", ");
			
			if (doorParts.length == 2) {
				long door1 = 0, door2 = 0;
				try {
					door1 = accessGraphFile.getDoorOfRoom(doorParts[0]);
					door2 = accessGraphFile.getDoorOfRoom(doorParts[1]);
				} catch (Exception e) {
					continue;
				}
				
				// Puerta 1: si no tiene habitación asignada, asignarle una
				long internalDoor1 = ElementIdMapper.convertToRangeId(door1, ElementIdMapper.CATEGORY_DOOR);
				if (!itemToRoomMap.containsKey(door1) && !itemToRoomMap.containsKey(internalDoor1)) {
					// Buscar habitación para puerta 1
					Integer room1 = null;
					for (int roomId = 1; roomId <= accessGraphFile.getNumberOfRoom(); roomId++) {
						int numDoors = accessGraphFile.getNumberOfDoorsByRoom(roomId);
						for (int j = 1; j <= numDoors; j++) {
							try {
								if (accessGraphFile.getDoorOfRoom(j, roomId) == door1) {
									room1 = roomId;
									break;
								}
							} catch (Exception e) {
								// Ignorar errores
							}
						}
						if (room1 != null) break;
					}
					
					if (room1 == null) room1 = 1;
					
					itemToRoomMap.put(door1, room1);
					itemToRoomMap.put(internalDoor1, room1);
				}
				
				// Puerta 2
				long internalDoor2 = ElementIdMapper.convertToRangeId(door2, ElementIdMapper.CATEGORY_DOOR);
				if (!itemToRoomMap.containsKey(door2) && !itemToRoomMap.containsKey(internalDoor2)) {
					Integer room2 = null;
					for (int roomId = 1; roomId <= accessGraphFile.getNumberOfRoom(); roomId++) {
						int numDoors = accessGraphFile.getNumberOfDoorsByRoom(roomId);
						for (int j = 1; j <= numDoors; j++) {
							try {
								if (accessGraphFile.getDoorOfRoom(j, roomId) == door2) {
									room2 = roomId;
									break;
								}
							} catch (Exception e) {
								// Ignorar errores
							}
						}
						if (room2 != null) break;
					}
					
					if (room2 == null) room2 = 1;
					
					itemToRoomMap.put(door2, room2);
					itemToRoomMap.put(internalDoor2, room2);
				}
			}
		}

		for (int doorId = 1; doorId <= numberOfDoors; doorId++) {
			long door = doorId + numberOfItems;  // ID externo de puerta
			long internalDoor = ElementIdMapper.convertToRangeId(door, ElementIdMapper.CATEGORY_DOOR);
			
			if (!itemToRoomMap.containsKey(door) && !itemToRoomMap.containsKey(internalDoor)) {
				// Habitación 1 por defecto
				itemToRoomMap.put(door, 1);
				itemToRoomMap.put(internalDoor, 1);
			}
		}		
	}

	/**
	 * Valida si una puerta existe realmente en el sistema.
	 * Añadido por Nacho Palacio 2025-05-29.
	 */
	protected boolean isValidDoor(long doorId) {
		try {
			long actualDoorId = doorId;
			if (ElementIdMapper.isInCorrectRange(doorId, ElementIdMapper.CATEGORY_DOOR)) {
				actualDoorId = ElementIdMapper.getBaseId(doorId);
			}
			
			int numberOfRooms = accessGraphFile.getNumberOfRoom();
			for (int room = 1; room <= numberOfRooms; room++) {
				int numDoors = accessGraphFile.getNumberOfDoorsByRoom(room);
				for (int j = 1; j <= numDoors; j++) {
					long doorInRoom = accessGraphFile.getDoorOfRoom(j, room);
					if (doorInRoom == actualDoorId) {
						return true;
					}
				}
			}
			
			return false;
			
		} catch (Exception e) {
			System.err.println("ERROR validating door " + doorId + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Obtiene una puerta válida aleatoria del sistema.
	 * Añadido por Nacho Palacio 2025-05-29.
	 */
	protected long getValidRandomDoor() {
		try {
			List<Long> allValidDoors = new ArrayList<>();
			
			int numberOfRooms = accessGraphFile.getNumberOfRoom();
			for (int room = 1; room <= numberOfRooms; room++) {
				int numDoors = accessGraphFile.getNumberOfDoorsByRoom(room);
				for (int j = 1; j <= numDoors; j++) {
					long doorId = accessGraphFile.getDoorOfRoom(j, room);
					allValidDoors.add(doorId);
				}
			}
			
			if (!allValidDoors.isEmpty()) {
				Random random = new Random();
				return allValidDoors.get(random.nextInt(allValidDoors.size()));
			}
			
		} catch (Exception e) {
			System.err.println("ERROR getting valid random door: " + e.getMessage());
		}
		
		return 1;
	}

	/**
	 * Obtiene todas las puertas válidas de una habitación específica.
	 * Añadido por Nacho Palacio 2025-05-29.
	 */
	protected List<Long> getValidDoorsForRoom(int room) {
		List<Long> validDoors = new ArrayList<>();
		
		try {
			int numDoors = accessGraphFile.getNumberOfDoorsByRoom(room);
			for (int j = 1; j <= numDoors; j++) {
				long doorId = accessGraphFile.getDoorOfRoom(j, room);
				validDoors.add(doorId);
			}
		} catch (Exception e) {
			System.err.println("ERROR getting doors for room " + room + ": " + e.getMessage());
		}
		
		return validDoors;
	}

	// Añadido por Nacho Palacio 2025-05-31
	private boolean isValidConnection(long startVertex, long endVertex) {
		if (endVertex <= 0) {
			return false;
		}

		if (startVertex == endVertex) {
			return true;
		}

		int startRoom = Configuration.simulation.getRoom(startVertex); // Modificado por Nacho Palacio 2025-06-19
		int endRoom = Configuration.simulation.getRoom(endVertex); // Modificado por Nacho Palacio 2025-06-19
		
		if (startRoom == endRoom) {
			return true;
		}
		
		boolean startIsDoor = isDoor(startVertex);
		boolean endIsDoor = isDoor(endVertex);
		
		if (!startIsDoor && !endIsDoor) {
			return false;
		}
		
		return true;
	}

	// Añadido por Nacho Palacio 2025-05-31
	private boolean isDoor(long vertex) {
		return vertex > this.numberOfItems;
	}



	/**
	 * Búsqueda exhaustiva en todas las subhabitaciones del sistema.
	 * Añadido por Nacho Palacio 2025-06-23.
	 * 
	 * @param idToSearch ID a buscar
	 * @param originalId ID original para caché
	 * @return ID de la subhabitación donde se encuentra, o 0 si no se encuentra
	 */
	private int searchInAllSubrooms(long idToSearch, long originalId) {	
		try {
			int totalRooms = accessGraphFile.getNumberOfRoom();
			for (int mainRoom = 1; mainRoom <= totalRooms; mainRoom++) {
				try {
					int numberOfSubrooms = accessGraphFile.getRoomNumberSubrooms(mainRoom);
					
					if (numberOfSubrooms > 0) {
						
						for (int posSubroom = 1; posSubroom <= numberOfSubrooms; posSubroom++) {
							try {
								int subroomId = accessGraphFile.getSubroom(posSubroom, mainRoom);
													
								// Listas para recopilar todos los elementos de esta subhabitación
								List<Long> itemsInSubroom = new ArrayList<>();
								List<Long> doorsInSubroom = new ArrayList<>();
								List<Long> invisibleDoorsInSubroom = new ArrayList<>();
								
								try {
									int numSubItems = accessGraphFile.getNumberOfItemsBySubroom(posSubroom, mainRoom);
									
									for (int itemPos = 1; itemPos <= numSubItems; itemPos++) {
										try {
											long itemId = accessGraphFile.getItemOfSubroom(itemPos, posSubroom, mainRoom);
											itemsInSubroom.add(itemId);
											
											if (itemId == idToSearch) {
												itemToRoomMap.put(originalId, subroomId);
												itemToRoomMap.put(idToSearch, subroomId);
												return subroomId;
											}
										} catch (Exception e) {
											// System.out.println("         Error obteniendo ítem " + itemPos + ": " + e.getMessage());
										}
									}
									
								} catch (Exception e) {
								}
								
								try {
									int numSubDoors = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, mainRoom);
									
									for (int doorPos = 1; doorPos <= numSubDoors; doorPos++) {
										try {
											long doorId = accessGraphFile.getDoorOfSubroom(doorPos, posSubroom, mainRoom);
											doorsInSubroom.add(doorId);
											
											if (doorId == idToSearch) {
												itemToRoomMap.put(originalId, subroomId);
												itemToRoomMap.put(idToSearch, subroomId);
												return subroomId;
											}
										} catch (Exception e) {
											// System.out.println("         Error obteniendo puerta " + doorPos + ": " + e.getMessage());
										}
									}
									
									if (!doorsInSubroom.isEmpty()) {
										// System.out.println("          " + doorsInSubroom);
									} else {
										// System.out.println("          (sin puertas)");
									}
									
								} catch (Exception e) {
									// System.out.println("         Puertas: Error accediendo - " + e.getMessage());
								}
								
								// PUERTAS INVISIBLES
								try {
									int numInvisibleDoors = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, mainRoom);
									
									for (int invDoorPos = 1; invDoorPos <= numInvisibleDoors; invDoorPos++) {
										try {
											long invDoorId = accessGraphFile.getInvisibleDoorOfSubroom(invDoorPos, posSubroom, mainRoom);
											invisibleDoorsInSubroom.add(invDoorId);
											
											if (invDoorId == idToSearch) {
												itemToRoomMap.put(originalId, subroomId);
												itemToRoomMap.put(idToSearch, subroomId);
												return subroomId;
											}
										} catch (Exception e) {
											// System.out.println("         Error obteniendo puerta invisible " + invDoorPos + ": " + e.getMessage());
										}
									}
									
								} catch (Exception e) {
									// System.out.println("         Puertas invisibles: Error accediendo - " + e.getMessage());
								}
								
							} catch (Exception e) {
								// System.out.println("       Error accediendo a subhabitación " + posSubroom + " de habitación " + mainRoom + ": " + e.getMessage());
							}
						}
					}
				} catch (Exception e) {
				}
			}
			
			for (int roomId = 1; roomId <= totalRooms; roomId++) {
				try {
					int numberOfSubrooms = accessGraphFile.getRoomNumberSubrooms(roomId);
					
					if (numberOfSubrooms <= 0) {
						List<Long> roomItems = new ArrayList<>();
						List<Long> roomDoors = new ArrayList<>();
						
						try {
							int numItems = accessGraphFile.getNumberOfItemsByRoom(roomId);
							for (int j = 1; j <= numItems; j++) {
								long itemId = accessGraphFile.getItemOfRoom(j, roomId);
								roomItems.add(itemId);
								if (itemId == idToSearch) {
									itemToRoomMap.put(originalId, roomId);
									itemToRoomMap.put(idToSearch, roomId);
									return roomId;
								}
							}
						} catch (Exception e) {
							// Continuar
						}
						
						// Puertas de habitación principal
						try {
							int numDoors = accessGraphFile.getNumberOfDoorsByRoom(roomId);
							for (int j = 1; j <= numDoors; j++) {
								long doorId = accessGraphFile.getDoorOfRoom(j, roomId);
								roomDoors.add(doorId);
								if (doorId == idToSearch) {
									itemToRoomMap.put(originalId, roomId);
									itemToRoomMap.put(idToSearch, roomId);
									return roomId;
								}
							}
						} catch (Exception e) {
							// Continuar
						}
						
					}
				} catch (Exception e) {
					// Continuar
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	/**
	 * Añadido por Nacho Palacio 2025-06-28.
	 * Inicializa el mapa de ítems por habitación usando IDs externos.
	 */
	public void initializeItemsByRoom(Map<Integer, List<Long>> roomItems) {	
		if (itemsDoorVisited == null) {
			itemsDoorVisited = new HashMap<>();
		}

		// Añadido por Nacho Palacio 2025-04-26.
		if (doorsByRoomMap == null) {
			doorsByRoomMap = new HashMap<>();
		}
		
		// Modificado por Nacho Palacio 2025-06-08
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			int roomId = entry.getKey();
			List<Long> items = entry.getValue();
			
			List<Long> allRoomElements = new LinkedList<>();
			
			for (Long itemId : items) {
				if (itemId != null && itemId > 0) {
					allRoomElements.add(itemId);
				}
			}
			
			itemsDoorVisited.put(roomId, new LinkedList<>(allRoomElements));
			
			if (allRoomElements.isEmpty()) {
				LinkedList<Long> defaultItems = new LinkedList<>();
				defaultItems.add(1001L);
				itemsDoorVisited.put(roomId, defaultItems);
			}
		}
	}

	/**
     * Añadido por Nacho Palacio 2025-06-28.
     * Asegura que un ID esté en formato interno, convirtiéndolo si es necesario.
     */
    protected long ensureInternalId(long id, int category) {
        if (!ElementIdMapper.isInCorrectRange(id, category)) {
            long internalId = ElementIdMapper.convertToRangeId(id, category);
			
            return internalId;
        }
        return id;
    }

	/**
	 * Convierte y valida todos los IDs de una lista al formato interno.
	 * Añadido por Nacho Palacio 2025-06-28.
	 */
	protected LinkedList<Long> convertAndValidateItems(LinkedList<Long> itemsList, String listType, int numberOfItems) {
		if (itemsList == null || itemsList.isEmpty()) {
			return new LinkedList<>();
		}

		LinkedList<Long> validatedItems = new LinkedList<>();
		
		for (Long itemId : itemsList) {
			if (itemId == null || itemId <= 0) {
				continue;
			}
			
			long convertedId = itemId;
			boolean wasConverted = false;

			
			// Si ya está en formato interno correcto, mantenerlo
			// Modificado por Nacho Palacio 2025-06-16
			int determinedCategory = ElementIdMapper.determineCategoryFromInternalId(itemId);
			if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM) || 
				ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR) ||
				ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_STAIRS) ||
				determinedCategory == ElementIdMapper.CATEGORY_ITEM ||
				determinedCategory == ElementIdMapper.CATEGORY_DOOR ||
				determinedCategory == ElementIdMapper.CATEGORY_STAIRS) {
				validatedItems.add(itemId);
				
				if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_STAIRS) || 
					determinedCategory == ElementIdMapper.CATEGORY_STAIRS) {
				} else if (ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_DOOR) || 
						determinedCategory == ElementIdMapper.CATEGORY_DOOR) {
				}
				
				continue;
			}
			
			if (itemId <= numberOfItems) {
				// Es un ítem externo
				convertedId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_ITEM);
				wasConverted = true;
			} else if (itemId <= numberOfItems + numberOfDoors) {
				// Es una puerta externa
				convertedId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_DOOR);
				wasConverted = true;
			} else {
				// Modificado por Nacho Palacio 2025-06-15
				convertedId = ElementIdMapper.convertToRangeId(itemId, ElementIdMapper.CATEGORY_STAIRS);
				wasConverted = true;
			}
			
			if (wasConverted) {
				validatedItems.add(convertedId);
			}
		}
		
		
		return validatedItems;
	}
}
