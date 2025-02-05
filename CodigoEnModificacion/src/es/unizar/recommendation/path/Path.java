package es.unizar.recommendation.path;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	public int numberOfRooms;
	public int numberOfRoomsAndSubrooms;
	public int numberOfItems;
	public int numberOfDoors;
	public int numberOfInvisibleDoors;
	public int numberOfStairs;
	
	// =========== Logger ================================:
	public static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);

	public Path() {
		this.diccionaryItemLocation = new HashMap<>();
		this.mapDoorConnected = new HashMap<>();
		this.itemsDoorVisited = new HashMap<>();
		this.accessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		this.accessItemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		this.accessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
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
	 * 
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
		log.log(Level.FINEST, " [mapDoorConnected] : \n   - " + mon);
		
		try {
			int numberInvisibleDoorConnected = accessGraphFile.getNumberOfConnectedInvisibleDoor();
			for (int posInvisibleDoor = 1; posInvisibleDoor <= numberInvisibleDoorConnected; posInvisibleDoor++) {
				String invisibleDoors = accessGraphFile.getConnectedInvisibleDoor(posInvisibleDoor);
				long invisibleDoor1 = accessGraphFile.getInvisibleDoorOfSubroom(invisibleDoors.split(", ")[0]);
				long invisibleDoor2 = accessGraphFile.getInvisibleDoorOfSubroom(invisibleDoors.split(", ")[1]);
				dictionary.put(invisibleDoor1, invisibleDoor2);
				dictionary.put(invisibleDoor2, invisibleDoor1);
			}
		}
		catch (Exception e) {}
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
//					System.out.println("Values in trouble --> " + posItem + " - " + posRoom);
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
			int r = getRoomFromItem(item);
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
		int numberOfRooms = this.numberOfRooms;
		int currentRoom = 0;
		
		for (int i = 1; i <= numberOfRooms;) {
			
			int numberOfSubrooms = accessGraphFile.getRoomNumberSubrooms(i);
			// If there are no subrooms
			if (numberOfSubrooms <= 0) {
				// Get number of objects depending on starVertex
				int numberOfObjects = (startVertex <= this.numberOfItems) ? accessGraphFile.getNumberOfItemsByRoom(i) : accessGraphFile.getNumberOfDoorsByRoom(i);
				for (int j = 1; j <= numberOfObjects; j++) {
					// Get object depending on startVertex
					long object =  (startVertex <= this.numberOfItems) ? accessGraphFile.getItemOfRoom(j, i) : accessGraphFile.getDoorOfRoom(j, i);
					if (object == startVertex) {
						currentRoom = i;
						i = numberOfRooms;
						break;
					}
				}
			}
			// There are subrooms
			else {
				for (int posSubroom = 1; posSubroom <= numberOfSubrooms; posSubroom++) {
					// Get number of objects depending on starVertex
					int numberOfObjects = -1;
					if (startVertex <= this.numberOfItems) {
						numberOfObjects = accessGraphFile.getNumberOfItemsBySubroom(posSubroom, i);
					}
					else if (startVertex <= this.numberOfItems + this.numberOfDoors) {
						numberOfObjects = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, i);
					}
					else {
						numberOfObjects = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, i);
					}
					
					for (int j = 1; j <= numberOfObjects; j++) {
						// Get object depending on startVertex
						long object = -1;
						if (startVertex <= this.numberOfItems) {
							object = accessGraphFile.getItemOfSubroom(j, posSubroom, i);
						}
						else if (startVertex <= this.numberOfItems + this.numberOfDoors) {
							object = accessGraphFile.getDoorOfSubroom(j, posSubroom, i);
						}
						else {
							object = accessGraphFile.getInvisibleDoorOfSubroom(j, posSubroom, i);
						}
						
						// Check object
						if (object == startVertex) {
							
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
		
		// System.out.println("Room from item " + startVertex + " = " + currentRoom);
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

	/**
	 * Get the items (objects and doors) from specified room.
	 * 
	 * @param room: The specified room. If higher than numberOfRooms, it's a subroom.
	 * @return List of items from specified room.
	 */
	public List<Long> getItemsByRoom(int room) {
		List<Long> itemsByRoomAll = new LinkedList<>();
		List<Long> itemsByRoom = new LinkedList<>();
		
		if (room <= this.numberOfRooms) {
			// Get the number of items from specified room.
			int numberOfItemsByRoom = accessGraphFile.getNumberOfItemsByRoom(room);
			for (int j = 1; j <= numberOfItemsByRoom; j++) {
				itemsByRoomAll.add(accessGraphFile.getItemOfRoom(j, room));
			}
			// Get the doors from specified room.
			int numberOfDoorsByRoom = accessGraphFile.getNumberOfDoorsByRoom(room);
			for (int j = 1; j <= numberOfDoorsByRoom; j++) {
				itemsByRoomAll.add(accessGraphFile.getDoorOfRoom(j, room));
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
				itemsByRoomAll.add(accessGraphFile.getItemOfSubroom(j, posSubroom, posRoom));
			}
			// Get the doors from specified subroom.
			int numberOfDoorsBySubroom = accessGraphFile.getNumberOfDoorsBySubroom(posSubroom, posRoom);
			for (int j = 1; j <= numberOfDoorsBySubroom; j++) {
				itemsByRoomAll.add(accessGraphFile.getDoorOfSubroom(j, posSubroom, posRoom));
			}
			// Get the invisible doors from specified subroom.
			int numberOfInvisibleDoorsBySubroom = accessGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
			for (int j = 1; j <= numberOfInvisibleDoorsBySubroom; j++) {
				itemsByRoomAll.add(accessGraphFile.getInvisibleDoorOfSubroom(j, posSubroom, posRoom));
			}
		}
		
		itemsByRoom = itemsByRoomAll;
		
		return itemsByRoom;
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
		long itemToVisit = 0;
		double initialDistance = 9999999;
		String startVertexLocation = diccionaryItemLocation.get(startVertex);
		String[] arrayStartVertex = startVertexLocation.split(", ");
		double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
		double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();
		
		/*
		System.out.println("Repeated: ");
		for (Long l: repeated) {
			System.out.print(l + ", ");
		}
		System.out.println();
		*/
		for (Long endVertex : itemsByRoom) {
			
			if (!(repeated.contains(endVertex) && Collections.frequency(repeated, endVertex) > 5)) {
				// System.out.println(" --- IF " + startVertex);
				String endVertexLocation = diccionaryItemLocation.get(endVertex);
				String[] arrayEndVertex = endVertexLocation.split(", ");
				double x2 = Double.valueOf(arrayEndVertex[0]).doubleValue();
				double y2 = Double.valueOf(arrayEndVertex[1]).doubleValue();
				double currentDistance = Distance.distanceBetweenTwoPoints(x1, y1, x2, y2);
	
				if (currentDistance < initialDistance) {
					if ((startVertex != endVertex) && (!itemVisited.contains(endVertex))) {
						initialDistance = currentDistance;
						itemToVisit = endVertex;
					}
				}
			}
			else {
				// System.out.println("ENTRO AL ELSE PARA " + startVertex + " que está " + Collections.frequency(repeated, endVertex) + " veces");
			}
		}
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
	 * 
	 * @param currentDoor: The current door.
	 * @return The connection of the current door.
	 */
	public long getConnectedDoor(long currentDoor) {
		List<Long> doors = new LinkedList<>();
		long connectedDoor = 0;
		// Find a room to which the non-RS user can go from the current door.
		int numberDoorConnected = accessGraphFile.getNumberOfConnectedDoor();
		for (int d = 1; d <= numberDoorConnected; d++) {
			String doorsConnected = accessGraphFile.getConnectedDoor(d);
			String[] array = doorsConnected.split(", ");
			String door1 = array[0];
			String door2 = array[1];
			
			// System.out.println("Door: " + currentDoor + "; door of room: " + door1 + "; taken from: " + doorsConnected);
			
			if (accessGraphFile.getDoorOfRoom(door1) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door2));
			} else {
				if (accessGraphFile.getDoorOfRoom(door2) == currentDoor) {
					doors.add(accessGraphFile.getDoorOfRoom(door1));
				}
			}
		}
		
		// Invisible doors
		int numberInvisibleDoorConnected = accessGraphFile.getNumberOfConnectedInvisibleDoor();
		for (int d = 1; d <= numberInvisibleDoorConnected; d++) {
			String invisibleDoorsConnected = accessGraphFile.getConnectedInvisibleDoor(d);
			String[] array = invisibleDoorsConnected.split(", ");
			String door1 = array[0];
			String door2 = array[1];
			
			// System.out.println("Door: " + currentDoor + "; door of room: " + door1 + "; taken from: " + doorsConnected);
			
			if (accessGraphFile.getDoorOfRoom(door1) == currentDoor) {
				doors.add(accessGraphFile.getDoorOfRoom(door2));
			} else {
				if (accessGraphFile.getDoorOfRoom(door2) == currentDoor) {
					doors.add(accessGraphFile.getDoorOfRoom(door1));
				}
			}
		}
		
		if (doors.size() <= 0) {
			// connectedDoor = getConnectedStairs(currentDoor);
		}
		else if (doors.size() == 1) {
			// There is only one staircase to go.
			connectedDoor = doors.get(0);
		} else {
			// Consider the different door options to go (from the stairs).
			connectedDoor = doors.get(random.nextInt(doors.size()));
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
				// System.out.println("Door " + door + " (" + currentDoor + ") is connected to stairs: " + stairs);
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

		long startVertex = start;
		long endVertex = itemToVisit;
		
		if (endVertex < numberOfItems)
			itemVisited.add(endVertex);
		
		subpath += getCurrentVertex(startVertex, endVertex);
		startVertex = endVertex;

		endVertex = connectedDoor;
		
		if (endVertex < numberOfItems)
			itemVisited.add(endVertex);
		
		subpath += getCurrentVertex(startVertex, endVertex);
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
		
		String edges[] = subpath.split(", ");
		for (int j = 0; j < edges.length; j++) {
			String edge = edges[j];
			if (!(edge == null || edge.equalsIgnoreCase(""))) {
				long startVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[0]).longValue();
				long endVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[1]).longValue();
				
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
		String edges[] = subpath.split(", ");
		String edge = edges[edges.length - 1];
		long endVertex = Long.valueOf(Configuration.simulation.cleanEdge(edge)[1]).longValue();
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
		
		int idInvisibleDoors = numberOfItems + numberOfDoors + numberOfStairs + 1;
		
		if (startVertex > numberOfItems && startVertex < idInvisibleDoors && endVertex > numberOfItems && endVertex < idInvisibleDoors) { // They are doors
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
}
