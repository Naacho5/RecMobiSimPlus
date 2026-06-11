package es.unizar.gui.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import es.unizar.util.ElementIdMapper;
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

	// Added by Nacho Palacio 2025-12-08
	private SimpleWeightedGraph<Long, DefaultWeightedEdge> cachedGraph = null;
    private boolean graphCacheValid = false;


	public GraphForSpecialUser() {
		this.random = new Random();
		// this.random.setSeed(100);

		this.accessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		this.accessItemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		this.accessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
		this.itemsDoorVisited = new HashMap<>();
		this.paths = new LinkedList<>();
		graphRecommender = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		// buildGraphForSpecialUser(); // DEBUG
	}

	/**
	 * Build the RS user graph.
	 * 
	 * @return The built graph.
	 */
	public SimpleWeightedGraph<Long, DefaultWeightedEdge> buildGraphForSpecialUser() {
		// System.out.println("Se llama a buildGraphForSpecialUser");
		// Added by Nacho Palacio 2025-12-08
		if (graphCacheValid && cachedGraph != null) {
            return cachedGraph;
        }

		double weight = 0;
		String location1 = null;
		String location2 = null;
		// Graph:
		SimpleWeightedGraph<Long, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		DataAccessGraphFile dataAccesGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		int numberOfRooms = dataAccesGraphFile.getNumberOfRoom();

		// System.out.println("Building graph for special user with file: " + Literals.GRAPH_FLOOR_COMBINED);
		// System.out.println("Number of rooms: " + numberOfRooms);
		
		try {
			for (int posRoom = 1; posRoom <= numberOfRooms; posRoom++) {
				List<Long> verticesRelated = null;
				
				// CHECK SUBROOMS
				int numSubrooms = dataAccesGraphFile.getRoomNumberSubrooms(posRoom);
				// System.out.println("Room " + posRoom + " has " + numSubrooms + " subrooms");
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
						
						// System.out.println("Items");
						// Subroom Items
						int numberOfItemBySubroom = dataAccesGraphFile.getNumberOfItemsBySubroom(posSubroom, posRoom);
						// System.out.println("Subroom " + posSubroom + " has " + numberOfItemBySubroom + " items");
						for (int posItemSubroom = 1; posItemSubroom <= numberOfItemBySubroom; posItemSubroom++) {
							// System.out.println("Processing item " + posItemSubroom + " of subroom " + posSubroom + " in room " + posRoom);
							long itemID = dataAccesGraphFile.getItemOfSubroom(posItemSubroom, posSubroom, posRoom);
							graph.addVertex(itemID);
							verticesRelated.add(itemID);
							// System.out.println(" - Added item: " + itemID);
						}
						
						// System.out.println("Doors");
						// Subroom doors
						int numberOfDoorBySubroom = dataAccesGraphFile.getNumberOfDoorsBySubroom(posSubroom, posRoom);
						for (int posDoorSubroom = 1; posDoorSubroom <= numberOfDoorBySubroom; posDoorSubroom++) {
							long doorID = dataAccesGraphFile.getDoorOfSubroom(posDoorSubroom, posSubroom, posRoom);
							graph.addVertex(doorID);
							verticesRelated.add(doorID);
							// System.out.println(" - Added door: " + doorID);
						}
						
						// System.out.println("Invisible doors");
						// Subroom invisible doors
						int numberOfInvisibleDoorBySubroom = dataAccesGraphFile.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
						for (int posInvisibleDoorSubroom = 1; posInvisibleDoorSubroom <= numberOfInvisibleDoorBySubroom; posInvisibleDoorSubroom++) {
							long invisibleDoorID = dataAccesGraphFile.getInvisibleDoorOfSubroom(posInvisibleDoorSubroom, posSubroom, posRoom);
							graph.addVertex(invisibleDoorID);
							verticesRelated.add(invisibleDoorID);
							// System.out.println(" - Added invisibleDoor: " + invisibleDoorID);
						}
						
						// System.out.println("ADD EDGES");
						// Add edges
						addEdges(graph, verticesRelated);
						
					}
				}

				// System.out.println(" Vértices en el grafo:");
				// for (Long v : graph.vertexSet()) {
				// 	System.out.println("  - " + v);
				// }
				// System.out.println(" Aristas en el grafo:");
				// for (DefaultWeightedEdge e : graph.edgeSet()) {
				// 	System.out.println("  - " + graph.getEdgeSource(e) + " <-> " + graph.getEdgeTarget(e) + " (peso: " + graph.getEdgeWeight(e) + ")");
				// }
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

				location1 = MainSimulator.floor.getItemLocation(d1);;

				// Added by Nacho Palacio 2025-06-10
				if (location1 == null) {		
					if (ElementIdMapper.isInCorrectRange(d1, ElementIdMapper.CATEGORY_DOOR)) {
						long d1External = ElementIdMapper.getBaseId(d1);
						location1 = MainSimulator.floor.getItemLocation(d1External);
					}
					
					if (location1 == null) {
						continue;
					}
				}

				location2 = MainSimulator.floor.getItemLocation(d2);

				// Added by Nacho Palacio 2025-06-10
				if (location2 == null) {
					if (ElementIdMapper.isInCorrectRange(d2, ElementIdMapper.CATEGORY_DOOR)) {
						long d2External = ElementIdMapper.getBaseId(d2);
						location2 = MainSimulator.floor.getItemLocation(d2External);
					}
					
					if (location2 == null) {
						continue;
					}
				}

				weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
						Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
				// graph.setEdgeWeight(graph.addEdge(d1, d2), weight);
				addOrUpdateWeightedEdge(graph, d1, d2, weight);
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

				location1 = MainSimulator.floor.getItemLocation(invD1);

				// Added by Nacho Palacio 2025-06-10
				if (location1 == null) {		
					if (ElementIdMapper.isInCorrectRange(invD1, ElementIdMapper.CATEGORY_DOOR)) {
						long invD1External = ElementIdMapper.getBaseId(invD1);
						location1 = MainSimulator.floor.getItemLocation(invD1External);
					}
					
					if (location1 == null) {
						continue;
					}
				}

				location2 = MainSimulator.floor.getItemLocation(invD2);

				// Added by Nacho Palacio 2025-06-10
				if (location2 == null) {		
					if (ElementIdMapper.isInCorrectRange(invD2, ElementIdMapper.CATEGORY_DOOR)) {
						long invD2External = ElementIdMapper.getBaseId(invD2);
						location2 = MainSimulator.floor.getItemLocation(invD2External);
					}
					
					if (location2 == null) {
						continue;
					}
				}

				weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
						Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
				// graph.setEdgeWeight(graph.addEdge(invD1, invD2), weight);
				addOrUpdateWeightedEdge(graph, invD1, invD2, weight);
			}

			System.out.println("===== Puertas e ítems por habitación =====");
			for (int roomId = 1; roomId <= dataAccesGraphFile.getNumberOfRoom(); roomId++) {
				System.out.print("Habitación " + roomId + " - Ítems: ");
				int numItems = dataAccesGraphFile.getNumberOfItemsByRoom(roomId);
				for (int i = 1; i <= numItems; i++) {
					long itemId = dataAccesGraphFile.getItemOfRoom(i, roomId);
					long externalItemId = ElementIdMapper.getBaseId(itemId);
					System.out.print(itemId + ":" + externalItemId + " ");
				}
				System.out.print(" | Puertas: ");
				int numDoors = dataAccesGraphFile.getNumberOfDoorsByRoom(roomId);
				for (int i = 1; i <= numDoors; i++) {
					long doorId = dataAccesGraphFile.getDoorOfRoom(i, roomId);
					long externalDoorId = ElementIdMapper.getBaseId(doorId);
					System.out.print(doorId + ":" + externalDoorId + " ");
				}
				System.out.println();
			}
			System.out.println("==========================================");

			// Added by Nacho Palacio 2025-12-08
			this.cachedGraph = graph;
			this.graphCacheValid = true;
		} catch (Exception e) {
			System.err.println("FATAL ERROR in buildGraphForSpecialUser: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);	
		}
		

		return graph;
	}

	/**
	 * Add or update an edge in the graph with the specified weight. If the edge does not exist, it will be created. If it already exists, its weight will be updated.
	 * @param graph
	 * @param source
	 * @param target
	 * @param weight
	 */
	private void addOrUpdateWeightedEdge(SimpleWeightedGraph<Long, DefaultWeightedEdge> graph,
										long source,
										long target,
										double weight) {
		DefaultWeightedEdge e = graph.getEdge(source, target);
		if (e == null) {
			e = graph.addEdge(source, target);
		}
		if (e != null) {
			graph.setEdgeWeight(e, weight);
		}
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

				// boolean v1ExistsInDict = MainSimulator.floor.diccionaryItemLocation.containsKey(v1);
				// boolean v2ExistsInDict = MainSimulator.floor.diccionaryItemLocation.containsKey(v2);

				boolean v1ExistsInDict = MainSimulator.floor.getItemLocation(v1) != null;
				boolean v2ExistsInDict = MainSimulator.floor.getItemLocation(v2) != null;

				
				if (!v1ExistsInDict) {
					if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
						long v1External = ElementIdMapper.getBaseId(v1);;
					} else if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_DOOR)) {
						long v1External = ElementIdMapper.getBaseId(v1);
					}
				}
				
				if (!v2ExistsInDict) {				
					if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_ITEM)) {
						long v2External = ElementIdMapper.getBaseId(v2);
					} else if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_DOOR)) {
						long v2External = ElementIdMapper.getBaseId(v2);
					}
				}

				//System.out.println("Vertices related: " + v1 + ", " + v2);
				location1 = MainSimulator.floor.getItemLocation(v1);;
				location2 = MainSimulator.floor.getItemLocation(v2);;

				// Added by Nacho Palacio 2025-06-09
				if (location1 == null) {
					if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
						long v1External = ElementIdMapper.getBaseId(v1);
						location1 = MainSimulator.floor.getItemLocation(v1External);;
					} else if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_DOOR)) {
						long v1External = ElementIdMapper.getBaseId(v1);
						location1 = MainSimulator.floor.getItemLocation(v1External);;
					}
				}

				if (location2 == null) {
					if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_ITEM)) {
						long v2External = ElementIdMapper.getBaseId(v2);
						location2 = MainSimulator.floor.getItemLocation(v2External);;
					} else if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_DOOR)) {
						long v2External = ElementIdMapper.getBaseId(v2);
						location2 = MainSimulator.floor.getItemLocation(v2External);;
					} else {
						for (int categoryTest = 1; categoryTest <= 10; categoryTest++) {
							if (ElementIdMapper.isInCorrectRange(v2, categoryTest)) {
								long v2Alternative = ElementIdMapper.getBaseId(v2);
								String testLocation = MainSimulator.floor.getItemLocation(v2Alternative);;
								if (testLocation != null) {
									location2 = testLocation;
									break;
								}
							}
						}
					}
				}

				if (location1 == null) {
					return;
				}
				
				if (location2 == null) {
					return;
				}

				//System.out.println(location1 + " - " + location2);
				weight = Distance.distanceBetweenTwoPoints(Double.valueOf(location1.split(", ")[0]).doubleValue(), Double.valueOf(location1.split(", ")[1]).doubleValue(),
						Double.valueOf(location2.split(", ")[0]).doubleValue(), Double.valueOf(location2.split(", ")[1]).doubleValue());
				// graph.setEdgeWeight(graph.addEdge(v1, v2), weight);
				addOrUpdateWeightedEdge(graph, v1, v2, weight);
				
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
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line = null;
			while ((line = br.readLine()) != null) {

				// Added by Nacho Palacio 2025-04-24
				if (line.trim().isEmpty()) {
					// Crear una ruta simple por defecto para líneas vacías
					List<String> defaultPath = new ArrayList<>();
					defaultPath.add("(1 : 2)");  // Arista ficticia mínima
					paths.add(defaultPath);
				}
				else {
					String[] array = line.split(", ");
					// Convertir IDs externos a internos en las rutas leídas
					List<String> internalPathEdges = new ArrayList<>();
					for (String edge : array) {
						if (!edge.trim().isEmpty()) {
							internalPathEdges.add(convertEdgeIdsToInternal(edge));
						}
					}
					paths.add(internalPathEdges);
				}

				// String[] array = line.split(", ");
				// paths.add(Arrays.asList(array));
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
		String startVertexLocation = MainSimulator.floor.getItemLocation(startVertex);
		String[] arrayStartVertex = startVertexLocation.split(", ");
		double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
		double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();
		for (Long endVertex : doorsByRoom) {
			String endVertexLocation = MainSimulator.floor.getItemLocation(endVertex);
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

	// Added by Nacho Palacio 2025-04-24
	private String convertEdgeIdsToInternal(String edge) {
		String[] vertices = edge.replace("(", "").replace(")", "").split(" : ");
		if (vertices.length == 2) {
			long v1 = Long.parseLong(vertices[0]);
			long v2 = Long.parseLong(vertices[1]);
			
			int numberOfItems = accessItemFile.getNumberOfItems();

			if (v1 > 0 && v1 <= numberOfItems) {
				v1 = ElementIdMapper.convertToRangeId(v1, ElementIdMapper.CATEGORY_ITEM);
			} else if (v1 > numberOfItems) {
				v1 = ElementIdMapper.convertToRangeId(v1, ElementIdMapper.CATEGORY_DOOR);
			}
			
			if (v2 > 0 && v2 <= numberOfItems) {
				v2 = ElementIdMapper.convertToRangeId(v2, ElementIdMapper.CATEGORY_ITEM);
			} else if (v2 > numberOfItems) {
				v2 = ElementIdMapper.convertToRangeId(v2, ElementIdMapper.CATEGORY_DOOR);
			}
			
			return "(" + v1 + " : " + v2 + ")";
		}
		return edge;
	}

	/**
     * Invalidates the cached graph.
     */
    public void invalidateGraphCache() {
        this.cachedGraph = null;
        this.graphCacheValid = false;
    }

    /**
     * Gets the cached graph without rebuilding.
     */
    public SimpleWeightedGraph<Long, DefaultWeightedEdge> getCachedGraph() {
        if (cachedGraph == null) {
            return buildGraphForSpecialUser();
        }
        return cachedGraph;
    }
}
