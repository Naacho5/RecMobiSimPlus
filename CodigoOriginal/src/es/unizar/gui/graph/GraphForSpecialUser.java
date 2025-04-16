package es.unizar.gui.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.util.Distance;
import es.unizar.util.Literals;

/**
 * 
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public class GraphForSpecialUser {

	public Random random;

	/**
	 * DataAccess
	 */
	public DataAccessGraphFile accessGraphFile;
	public DataAccessItemFile accessItemFile;
	public DataAccessRoomFile accessRoomFile;

	/**
	 * Load the dictionary, the graph of recommendation y gets the paths for number of users.
	 */
	public Map<Integer, List<Long>> itemsDoorVisited;
	public SimpleWeightedGraph<Long, DefaultWeightedEdge> graphRecommender;
	public List<List<String>> paths;

	public GraphForSpecialUser() {
		this.random = new Random();
		// this.random.setSeed(100);

		this.accessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		this.accessItemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		this.accessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));

		this.itemsDoorVisited = new HashMap<>();
		this.paths = new LinkedList<>();
		graphRecommender = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
	}

	/**
	 * Build the RS user graph.
	 * 
	 * @return The built graph.
	 */
	public SimpleWeightedGraph<Long, DefaultWeightedEdge> buildGraphForSpecialUser() {
		
		double weight = 0;
		String location1 = null;
		String location2 = null;
		// Graph:
		SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		DataAccessGraphFile dataAccesGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		int numberOfRooms = dataAccesGraphFile.getNumberOfRoom();
		for (int posRoom = 1; posRoom <= numberOfRooms; posRoom++) {
			List<Long> verticesRelated = null;
			
			// CHECK SUBROOMS
			int numSubrooms = dataAccesGraphFile.getRoomNumberSubrooms(posRoom);
			//System.out.println("Room " + posRoom + " has " + numSubrooms + " subrooms");
			// IF ROOM NOT DIVIDED IN SUBROOMS -> ALL OBJECTS BELONG TO SAME ROOM
			if (numSubrooms == 0) {
				// Initialize verticesRelated
				verticesRelated = new LinkedList<>();
				
				// Add vertex: Items
				int numberOfItemByRoom = dataAccesGraphFile.getNumberOfItemsByRoom(posRoom);
				for (int posItem = 1; posItem <= numberOfItemByRoom; posItem++) {
					long itemID = dataAccesGraphFile.getItemOfRoom(posItem, posRoom);
					graph.addVertex(itemID);
					verticesRelated.add(itemID);
				}
				// Doors:
				int numberOfDoorByRoom = dataAccesGraphFile.getNumberOfDoorsByRoom(posRoom);
				for (int posDoor = 1; posDoor <= numberOfDoorByRoom; posDoor++) {
					long doorID = dataAccesGraphFile.getDoorOfRoom(posDoor, posRoom);
					graph.addVertex(doorID);
					verticesRelated.add(doorID);
				}
				
				addEdges(graph, verticesRelated);
			}
			// IF ROOM DIVIDED IN SUBROOMS -> ONLY OBJECTS FROM THE SAME SUBROOM WILL BE RELATED
			// INVISIBLE DOORS WILL BE RELATED TO DIFFERENT SUBROOMS (SUBROOMS CONNECTED BY INVISIBLE DOORS)
			else {
				for (int posSubroom = 1; posSubroom <= numSubrooms; posSubroom++) {
					// New verticesRelated every iteration
					verticesRelated = new LinkedList<>();
					
					//System.out.println("Items");
					// Subroom Items
					int numberOfItemBySubroom = dataAccesGraphFile.getNumberOfItemsBySubroom(posSubroom, posRoom);
					for (int posItemSubroom = 1; posItemSubroom <= numberOfItemBySubroom; posItemSubroom++) {
						long itemID = dataAccesGraphFile.getItemOfSubroom(posItemSubroom, posSubroom, posRoom);
						graph.addVertex(itemID);
						verticesRelated.add(itemID);
						//System.out.println(" - Added item: " + itemID);
					}
					
					//System.out.println("Doors");
					// Subroom doors
					int numberOfDoorBySubroom = dataAccesGraphFile.getNumberOfDoorsBySubroom(posSubroom, posRoom);
					for (int posDoorSubroom = 1; posDoorSubroom <= numberOfDoorBySubroom; posDoorSubroom++) {
						long doorID = dataAccesGraphFile.getDoorOfSubroom(posDoorSubroom, posSubroom, posRoom);
						graph.addVertex(doorID);
						verticesRelated.add(doorID);
						//System.out.println(" - Added door: " + doorID);
					}
					
					//System.out.println("Invisible doors");
					// Subroom invisible doors
					int numberOfInvisibleDoorBySubroom = dataAccesGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
					for (int posInvisibleDoorSubroom = 1; posInvisibleDoorSubroom <= numberOfInvisibleDoorBySubroom; posInvisibleDoorSubroom++) {
						long invisibleDoorID = dataAccesGraphFile.getInvisibleDoorOfSubroom(posInvisibleDoorSubroom, posSubroom, posRoom);
						graph.addVertex(invisibleDoorID);
						verticesRelated.add(invisibleDoorID);
						//System.out.println(" - Added invisibleDoor: " + invisibleDoorID);
					}
					
					//System.out.println("ADD EDGES");
					// Add edges
					addEdges(graph, verticesRelated);
					
				}
			}
		}
		
		// Connected doors:
		int numberOfConnectedDoor = dataAccesGraphFile.getNumberOfConnectedDoor();
		for (int posDoor = 1; posDoor <= numberOfConnectedDoor; posDoor++) {
			String connectedDoor = dataAccesGraphFile.getConnectedDoor(posDoor);
			String[] array = connectedDoor.split(", ");
			String door1 = array[0];
			String door2 = array[1];
			long d1 = dataAccesGraphFile.getDoorOfRoom(door1);
			long d2 = dataAccesGraphFile.getDoorOfRoom(door2);
			location1 = MainSimulator.floor.diccionaryItemLocation.get(d1);
			location2 = MainSimulator.floor.diccionaryItemLocation.get(d2);
			weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
					Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
			graph.setEdgeWeight(graph.addEdge(d1, d2), weight);
		}
		
		// Connected invisible doors:
		int numberOfConnectedInvisibleDoor = dataAccesGraphFile.getNumberOfConnectedInvisibleDoor();
		for (int posInvisibleDoor = 1; posInvisibleDoor <= numberOfConnectedInvisibleDoor; posInvisibleDoor++) {
			String connectedInvisibleDoor = dataAccesGraphFile.getConnectedInvisibleDoor(posInvisibleDoor);
			String[] array = connectedInvisibleDoor.split(", ");
			String invisibleDoor1 = array[0];
			String invisibleDoor2 = array[1];
			long invD1 = dataAccesGraphFile.getInvisibleDoorOfSubroom(invisibleDoor1);
			long invD2 = dataAccesGraphFile.getInvisibleDoorOfSubroom(invisibleDoor2);
			location1 = MainSimulator.floor.diccionaryItemLocation.get(invD1);
			location2 = MainSimulator.floor.diccionaryItemLocation.get(invD2);
			weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
					Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
			graph.setEdgeWeight(graph.addEdge(invD1, invD2), weight);
		}
		return graph;
	}

	/**
	 * Add edges to graph related to "verticesRelated".
	 * 
	 * @param graph
	 * @param verticesRelated
	 */
	private void addEdges(SimpleWeightedGraph<Long, DefaultWeightedEdge> graph, List<Long> verticesRelated) {
		
		double weight = 0;
		String location1 = null;
		String location2 = null;
		
		//System.out.println("Vertices related size: " + verticesRelated.size());
		// Add edges: items and doors:
		for (int k = 0; k < verticesRelated.size(); k++) {
			long v1 = verticesRelated.get(k);
			for (int j = k + 1; j < verticesRelated.size(); j++) {
				long v2 = verticesRelated.get(j);
				//System.out.println("Vertices related: " + v1 + ", " + v2);
				location1 = MainSimulator.floor.diccionaryItemLocation.get(v1);
				location2 = MainSimulator.floor.diccionaryItemLocation.get(v2);
				//System.out.println(location1 + " - " + location2);
				weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
						Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
				graph.setEdgeWeight(graph.addEdge(v1, v2), weight);
				
				// System.out.println(v1 + " connected to " + v2 + " (" + weight + ")");
			}
		}
		
	}

	/**
	 * Get the non-special and RS user paths. The non-RS user path is obtained from generated path file (e.g., nearest_non_special_user_paths.txt), by using the strategy (Nearest, Random or
	 * Exhaustive) specified in the Configuration form. While the RS user path is generated with the recommender specified in the Configuration form.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void getPathsFromFile() {
		try {
			// Load non-RS user paths:
			String path = Configuration.simulation.getNonSpecialUserPaths();
			System.out.println("Attempting to load non-special user paths from: " + path); // Añadido por Nacho Palacio 2025-04-13
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] array = line.split(", ");
				paths.add(Arrays.asList(array));
			}
			// Add RS user paths with null information:
			for (int i = 0; i < Configuration.simulation.getNumberOfSpecialUser(); i++) {
				paths.add(null);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a list of doors from a room.
	 * 
	 * @param room: The current room.
	 * @return The list of doors.
	 */
	public List<Long> getDoorsByRoom(int room) {
		// Obtiene todas las puertas de una habitacion especificada
		List<Long> doorsByRoom = new LinkedList<>();
		int numberOfDoorsByRoom = accessGraphFile.getNumberOfDoorsByRoom(room);
		for (int j = 1; j <= numberOfDoorsByRoom; j++) {
			doorsByRoom.add(accessGraphFile.getDoorOfRoom(j, room));
		}
		return doorsByRoom;
	}

	/**
	 * Get the door closest to the item.
	 * 
	 * @param startVertex: The start vertex.
	 * @param doorsByRoom: List of doors by room.
	 * @return The number of the door closest.
	 */
	public long getDoorClosestToTheItem(long startVertex, List<Long> doorsByRoom) {
		long itemToVisit = 0;
		double initialDistance = Integer.MAX_VALUE;
		String startVertexLocation = MainSimulator.floor.diccionaryItemLocation.get(startVertex);
		String[] arrayStartVertex = startVertexLocation.split(", ");
		double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
		double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();
		for (Long endVertex : doorsByRoom) {
			String endVertexLocation = MainSimulator.floor.diccionaryItemLocation.get(endVertex);
			String[] arrayEndVertex = endVertexLocation.split(", ");
			double x2 = Double.valueOf(arrayEndVertex[0]).doubleValue();
			double y2 = Double.valueOf(arrayEndVertex[1]).doubleValue();
			double currentDistance = Distance.distanceBetweenTwoPoints(x1, y1, x2, y2);

			if (currentDistance < initialDistance) {
				if (endVertex > accessItemFile.getNumberOfItems()) {
					initialDistance = currentDistance;
					itemToVisit = endVertex;
				}
			}
		}
		return itemToVisit;
	}

	/**
	 * Get the room where the item is located.
	 * 
	 * @param startVertex: The start vertex.
	 * @return The room.
	 */
	public int getRoomFromItem(long startVertex) {
		int numberOfRooms = accessGraphFile.getNumberOfRoom();
		int currentRoom = 0;
		// Si startVertex es un item o una puerta
		for (int i = 1; i <= numberOfRooms;) {
			int numberOfItems = accessGraphFile.getNumberOfItemsByRoom(i);
			for (int j = 1; j <= numberOfItems; j++) {
				long item = accessGraphFile.getItemOfRoom(j, i);
				if (item == startVertex) {
					currentRoom = i;
					i = numberOfRooms;
					break;
				}
			}
			int numberOfDoors = accessGraphFile.getNumberOfDoorsByRoom(i);
			for (int j = 1; j <= numberOfDoors; j++) {
				long door = accessGraphFile.getDoorOfRoom(j, i);
				if (door == startVertex) {
					currentRoom = i;
					i = numberOfRooms;
					break;
				}
			}
			i++;
		}
		return currentRoom;
	}
}
