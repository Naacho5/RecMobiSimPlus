package es.unizar.gui.graph;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import es.unizar.access.DataAccessItemFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.util.Distance;
import es.unizar.util.Literals;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.MyGraph;
import es.unizar.gui.simulation.User;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class DrawFloorGraph {

	public static final double CORNER_DIM = 5; // Squared corners
	public static final double HEIGHT = 10;
	public static final double WITDH = 10;
	public static mxCell[] vertices;
	public static mxCell[] users;
	public mxGraph graph;
	public mxGraphModel model;
	public static mxGraphComponent roomGraphComponent;

	public Map<Long, String> diccionaryItemLocation = new HashMap<>();
	
	private DataAccessItemFile itemFile;
	
	public List<Polygon> rooms = new LinkedList<Polygon>();
	public List<Integer> roomLabels = new ArrayList<Integer>();
	
	/**
	 * mxGraphComponent's getter
	 * @return
	 */
	public mxGraphComponent getRoomGraphComponent() {
		return roomGraphComponent;
	}

	/**
	 * Draws floor depending on the roomFile and itemFile params.
	 * For every room in map
	 *   For every corner -> Draws corner (SQUARED ROOMS ARE CONSIDERED)
	 *   For every door -> Draws door
	 *   For every item -> Draws item
	 * For every stairs -> Draws stairs
	 * Draws edges (cornerRelated())
	 * 
	 * @param roomFile
	 * @param itemFile
	 * @param removeVertexLabel
	 * @param removeEdges
	 * @param numberFloor
	 * @return
	 */
	public mxGraphComponent drawFloor(File roomFile, File itemFile, boolean removeVertexLabel, boolean removeEdges, int numberFloor) {
		
		// Graph Room
		graph = new MyGraph();
		
		model = new mxGraphModel();
		
		graph.setModel(model);
		/*graph.getModel().addListener(mxEvent.CHANGE,
				new mxIEventListener()
				{
					public void invoke(Object source, mxEventObject evt)
					{
						DrawFloorGraph.this.graphModelChanged();
					}
				});*/
		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();

		// Data access of rooms and items
		DataAccessRoomFile dataAccessRoomFile = new DataAccessRoomFile(roomFile); // Every drawFloor call can require different files, so it has to be created everytime
		DataAccessItemFile dataAccessItemFile = new DataAccessItemFile(itemFile); // Every drawFloor call can require different files, so it has to be created everytime
		
		int numberOfRooms = dataAccessRoomFile.getNumberOfRoom();
		int numberOfItems = dataAccessItemFile.getNumberOfItems();

		int numberOfVertex = numberOfVertexInRooms(dataAccessRoomFile) + numberOfItems;
//		System.out.println("Number of Rooms: " + numberOfRooms);
//		System.out.println("Number of Items: " + numberOfItems);
//		System.out.println("numberOfVertex of graph: " + numberOfVertex);
		
		// Vertices
		vertices = new mxCell[numberOfVertex];
		int i = 0;
		String vertexLocation = null;
		String vertexLabel = null;
		mxCell vertex = null;
		String[] x_y = null;
		double vertexX = 0;
		double vertexY = 0;
		String icon = null;
		int doorID = dataAccessItemFile.getNumberOfItems() + 1;
		// For each room:
		if (MainSimulator.gui.isSelected())
			MainSimulator.printConsole("Painting rooms:", Level.WARNING);
		for (int r = 1; r <= numberOfRooms; r++) {
			if (MainSimulator.gui.isSelected())
				MainSimulator.printConsole("room " + r, Level.WARNING);
			String room = String.valueOf(Integer.valueOf(dataAccessRoomFile.getRoomLabel(r)));// - 400);
			
			// Corners
			String cornersOfRoom = "";
			
			// Create room's polygon
			Polygon polygon = new Polygon();
			
			int numberOfCorners = dataAccessRoomFile.getRoomNumberCorner(r);
			for (int c = 1; c <= numberOfCorners; c++) {
				vertexLocation = dataAccessRoomFile.getRoomCornerXY(c, r);
				x_y = vertexLocation.split(", ");
				vertexX = Double.valueOf(x_y[0]);
				vertexY = Double.valueOf(x_y[1]);
				icon = "shape=image;image=" + "/resources/images/corner.png";
				vertexLabel = "corner, " + room;
				// mxGraph.insertVertex(parent, id, value, x, y, width, height, style) 
//				if (removeVertexLabel) {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, CORNER_DIM, CORNER_DIM, icon + ";noLabel=1"); //
//				} else {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, CORNER_DIM, CORNER_DIM, icon);
//				}
				if (removeVertexLabel) {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, CORNER_DIM, CORNER_DIM, icon + ";noLabel=1"); //
				} else {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, CORNER_DIM, CORNER_DIM, icon);
				}
				if (vertex == null)
					System.out.println("No puede añadir corner");
					
				vertices[i] = vertex;
				i++;
//				cornersOfRoom += vertexY + ", " + vertexX + ";"; // TODO: switch to vertexX,vertexY; ?????
				cornersOfRoom += vertexX + ", " + vertexY + ";";
				
				// Add corner to polygon
				polygon.addPoint((int) vertexX, (int) vertexY); // Points casted!
			}
			
			rooms.add(polygon);
			roomLabels.add(Integer.parseInt(room));
			
			vertex = (mxCell) graph.insertVertex(parent, null, room, polygon.getBounds().getCenterX(), polygon.getBounds().getCenterY(), WITDH, HEIGHT,"shape=image");
			graph.addCell(vertex, parent);

			// Doors
			int numberDoor = dataAccessRoomFile.getRoomNumberDoor(r);
			for (int d = 1; d <= numberDoor; d++) {
				vertexLocation = dataAccessRoomFile.getRoomDoorXY(d, r);
				x_y = vertexLocation.split(", ");
				vertexX = Double.valueOf(x_y[0]);
				vertexY = Double.valueOf(x_y[1]);
				icon = "shape=image;image=" + "/resources/images/door.png";
				vertexLabel = "door, " + room + ", " + doorID;
//				System.out.println("Vertex label --> " + vertexLabel);
//				if (removeVertexLabel) {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon + ";noLabel=1");
//				} else {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon);
//				}
				if (removeVertexLabel) {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon + ";noLabel=1");
				} else {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon);
				}
				vertex.setId(String.valueOf(doorID));
				graph.addCell(vertex, parent); // Add vertex to floor graph
				if (vertex == null)
					System.out.println("No puede añadir DOOR");
				vertices[i] = vertex;
				i++;
				doorID++;
				// System.out.println("vertexLabel: " + vertexLabel);
			}
			
			// Items
			for (int it = 1; it <= numberOfItems; it++) {
				String roomLabel = dataAccessItemFile.getItemRoom(it);
//				System.out.println("RoomLabel --> " + roomLabel);
				if (room.equalsIgnoreCase(roomLabel)) {
//					System.out.println("RoomLabel --> Goes through");
					vertexLocation = dataAccessItemFile.getVertexXY(it);
					x_y = vertexLocation.split(", ");
					vertexX = Double.valueOf(x_y[0]);
					vertexY = Double.valueOf(x_y[1]);

					// Traslada el item 10 pixels para que quede dentro de la habitacion
					String[] array = cornersOfRoom.split(";");
					
					List<Double> cornersX = new LinkedList<Double>();
					List<Double> cornersY = new LinkedList<Double>();
					
					for (int arrayCornersIndex = 0; arrayCornersIndex < array.length; arrayCornersIndex++) {
						cornersX.add(Double.valueOf(array[arrayCornersIndex].split(", ")[0]).doubleValue());
						cornersY.add(Double.valueOf(array[arrayCornersIndex].split(", ")[1]).doubleValue());
					}
					
					
					for(int line = 0; line < array.length; line++) {
						
						double currentX = cornersX.get(line);
						double currentY = cornersY.get(line);
						double nextX = (line == array.length-1) ? cornersX.get(0) : cornersX.get(line+1);
						double nextY = (line == array.length-1) ? cornersY.get(0) : cornersY.get(line+1);
						
						Line2D line2d = new Line2D.Double(currentX, currentY, nextX, nextY);
						
					}
					
					
					String vertexType = dataAccessItemFile.getVertexLabel(it);
					
					/*
					 * PREVIOUS (MUSEUM ONLY):
					if (vertexType.equalsIgnoreCase("Sculpture")) {
						icon = "shape=image;image=/es/unizar/images/sculpture.png";
					} else {
						icon = "shape=image;image=/es/unizar/images/painting.png";
					}
					*/
					
//					System.out.println("Item causing trouble --> " + it);
					File fileImage = new File(dataAccessItemFile.getVisitableVertexURL(it));
					
//					File fileImage = new File(getClass().getResource(dataAccessItemFile.getVisitableVertexURL(it)).getFile());
					Path path = fileImage.toPath();
					// String pathIcon = "file:/" + path.toString();
					String pathIcon = path.toString();
					pathIcon = pathIcon.replace("\\", "/");
					
					// Get item's linkToImage and add it to style
					if(!pathIcon.startsWith("/") && !pathIcon.startsWith("C:/")) pathIcon = "/" + pathIcon;
					icon = "shape=image;image=" + pathIcon;
					//icon = icon.replace("\\", "/");
					
					vertexLabel = vertexType + ", " + room + ", " + it;// dataAccessItemFile.getItemID(it);
//					System.out.println("Vertex label --> " + vertexLabel);
//					vertexLabel = vertexType + ", " + it;
//					if (removeVertexLabel) {
//						vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon + ";noLabel=1");
//					} else {
//						vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon);
//					}
					if (removeVertexLabel) {
						vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon + ";noLabel=1");
					} else {
						vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon);
					}
					vertex.setId(String.valueOf(it));
					graph.addCell(vertex, parent);
					if (vertex == null)
						System.out.println("No puede añadir ITEM");
					vertices[i] = vertex;
					i++;
//					System.out.println("*****vertexLabel: " + vertexLabel);
				}
			}
		}
		// Stairs
		int numberOfStairs = dataAccessRoomFile.getNumberStairs();
		for (int j = 1; j <= numberOfStairs; j++) {
			vertexLocation = dataAccessRoomFile.getStairs(j);
			if (vertexLocation != null) {
				x_y = vertexLocation.split(", ");
				vertexX = Double.valueOf(x_y[0]);
				vertexY = Double.valueOf(x_y[1]);
				icon = "shape=image;image=" + "/resources/images/stairs.png";
				vertexLabel = "stairs";
//				if (removeVertexLabel) {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon + ";noLabel=1");
//				} else {
//					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon);
//				}
//				System.out.println("Vertex label --> " + vertexLabel);
				if (removeVertexLabel) {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon + ";noLabel=1");
				} else {
					vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon);
				}
				if (vertex == null)
					System.out.println("No puede añadir STAIR");
				vertices[i] = vertex;
				i++;
			}
		}
		
		// Invisible doors -> MUST GO HERE, vertices[] ORDER MATTERS
		int invisibleDoorID = doorID;
		int numInvDoors = 0;
		
		for (int r = 1; r <= numberOfRooms; r++) {
			
			int numberInvisibleDoor = dataAccessRoomFile.getRoomNumberInvisibleDoor(r);
			numInvDoors += numberInvisibleDoor;
			
			// If room doesn't have subrooms, this loop will never happen
			for (int invisibleDoor = 1; invisibleDoor <= numberInvisibleDoor; invisibleDoor++) {
				
				//String room = dataAccessRoomFile.getRoomLabel(r);
				vertexLocation = dataAccessRoomFile.getRoomInvisibleDoorXY(invisibleDoor, r);
				x_y = vertexLocation.split(", ");
				vertexX = Double.valueOf(x_y[0]);
				vertexY = Double.valueOf(x_y[1]);
				vertexLabel = "invisibleDoor, " + invisibleDoorID;
//				System.out.println("Vertex label --> " + vertexLabel);
				//System.out.println(vertexLabel + " [" + vertexX + ", " + vertexY + "]");
				icon = "shape=image;image=" + Literals.RELATIVE_PATH_IMAGES + "door.png";
				
//				vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexY, vertexX, WITDH, HEIGHT, icon+";noLabel=1;opacity=0");
				vertex = (mxCell) graph.insertVertex(parent, null, vertexLabel, vertexX, vertexY, WITDH, HEIGHT, icon+";noLabel=1;opacity=0");
				
				vertex.setId(String.valueOf(invisibleDoorID));
				graph.addCell(vertex, parent); // Add vertex to floor graph
				if (vertex == null)
					System.out.println("No puede añadir INVISIBLE DOOR");
				vertices[i] = vertex;
				i++;
				invisibleDoorID++;
			}
		}

		// Edges
		try {
			for (int r = 1; r <= numberOfRooms; r++) {
				// Add corners related
//				System.out.println("Accessing in room --> " + r);
//				System.out.println("# vertices --> " + vertices.length);
//				System.out.println("++++++ Vertices --> " + vertices);
				addCornerRelated(r, dataAccessRoomFile, vertices, removeEdges, graph, parent, numInvDoors);
			}
		} catch (Exception ex) {
			Logger.getLogger(MainSimulator.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			graph.getModel().endUpdate();
		}
	    
		roomGraphComponent = new mxGraphComponent(graph);
		
		int w = MainSimulator.floorPanel.getWidth();
		int h = MainSimulator.floorPanel.getHeight();
		roomGraphComponent.setPreferredSize(new Dimension(w, h));
		
		roomGraphComponent.setAutoScroll(true); // No effect
		
		roomGraphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mxGraph g = MainSimulator.graphComponent.getGraph();
				for(Object o : g.getChildCells(g.getDefaultParent())) {
					mxCell c = (mxCell)o;
					if(Distance.distanceBetweenTwoPoints(e.getX(),e.getY(),c.getGeometry().getX(), c.getGeometry().getY()) <= 10) {
						if(c.isVertex()) {
							try {
								int room = Integer.parseInt(c.getValue().toString());
								MainSimulator.userInfo.filterTables(room);
								MainSimulator.userInfo.removeFilter.setEnabled(true);
							}catch(NumberFormatException nfe) {
								nfe.printStackTrace();
							}
						}
					}
				}
			}
		});
		
//		System.out.println("********* VERTICES *********");
//		this.printVertices(vertices);
		
		// System.out.println(roomGraphComponent.getGraph().getGraphBounds());
		return roomGraphComponent;
	}

	public void loadDiccionaryItemLocation() {
		itemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		long doorID = itemFile.getNumberOfItems();
		long itemID = 0;
		long stairsID = 0;
		long invisibleDoorID = 0;
		String room = "";

		// System.out.println("type;item;room;location");
		for (int i = 0; i < vertices.length; i++) {
			mxCell cell = vertices[i];
			String vertex = (String) cell.getValue();
			String[] array = vertex.split(", ");
			String type = array[0];
			mxGeometry geometry = cell.getGeometry();
			String location = geometry.getX() + ", " + geometry.getY();
			if (!type.equalsIgnoreCase("door") && !type.equalsIgnoreCase("stairs") && !type.equalsIgnoreCase("corner") && !type.equalsIgnoreCase("invisibleDoor")) {
				itemID = Long.valueOf(array[2]).longValue();
				room = array[1];
				diccionaryItemLocation.put(itemID, location);
				//System.out.println(type + ";" + itemID + ";" + room + ";" + location);
			} else {
				if (type.equalsIgnoreCase("door")) {
					doorID++;
					room = array[1];
					diccionaryItemLocation.put(doorID, location);
					//System.out.println(type + ";" + doorID + ";" + room + ";" + location);
				} else {
					if (type.equalsIgnoreCase("stairs")) {
						doorID++;
						stairsID = doorID;
						diccionaryItemLocation.put(stairsID, location);
						//System.out.println(type + ";" + stairsID + ";" + "0" + ";" + location);
					}
					else {
						if (type.equalsIgnoreCase("invisibleDoor")) {
							if (doorID > stairsID) {
								doorID++;
								invisibleDoorID = doorID;
							}
							else {
								stairsID++;
								invisibleDoorID = stairsID;
							}
							diccionaryItemLocation.put(invisibleDoorID, location);
							//System.out.println(type + ";" + invisibleDoorID + ";" + "0" + ";" + location);
							
						}
					}
				}
			}
		}
	}

	private void addCornerRelated(int room, DataAccessRoomFile dataAccessRoomFile, mxCell[] vertices, boolean removeEdges, mxGraph graph, Object parent, int numInvDoors) {
		String roomLabel = String.valueOf(Integer.valueOf(dataAccessRoomFile.getRoomLabel(room)));// - 400);
		List<mxCell> cornerRelatedList = new LinkedList<mxCell>();

		// The stairs is excluded
		for (int v = 0; v < vertices.length - dataAccessRoomFile.getNumberStairs() - numInvDoors; v++) {
			mxCell vertex = vertices[v];
//			System.out.println("Current vertix" + vertex);
			if (vertex != null) {
				try {
					String[] array = vertex.getValue().toString().split(", ");
					String typeVertex = array[0];
					if (typeVertex.equalsIgnoreCase("stairs"))
						System.out.println("***** Tries STAIRS as CORNERS");
					else {
						String roomVertex = array[1];
						if (roomVertex.equalsIgnoreCase(roomLabel) && typeVertex.equalsIgnoreCase("corner")) {
							cornerRelatedList.add(vertex);
						}
					}
				}
				catch(Exception e) {
					System.out.println(vertex);
					e.printStackTrace();
					System.out.println();
				}
			}
		}
		/*
		mxCell corner1 = cornerRelatedList.get(0);
		mxCell corner2 = cornerRelatedList.get(1);
		mxCell corner3 = cornerRelatedList.get(2);
		mxCell corner4 = cornerRelatedList.get(3);
		*/
		
		// mxGraph.insertEdge(parent, id, value, source, target, style)
		String style = "endArrow=none";
		if (removeEdges) {
			style = "strokeColor=none;endArrow=none";
			/*
			// 1-2 and 2-1
			graph.insertEdge(parent, null, null, corner1, corner2, "strokeColor=none;endArrow=none");
			graph.insertEdge(parent, null, null, corner2, corner1, "strokeColor=none;endArrow=none");
			// 2-3 and 3-2
			graph.insertEdge(parent, null, null, corner2, corner3, "strokeColor=none;endArrow=none");
			graph.insertEdge(parent, null, null, corner3, corner2, "strokeColor=none;endArrow=none");
			// 3-4 and 4-3
			graph.insertEdge(parent, null, null, corner3, corner4, "strokeColor=none;endArrow=none");
			graph.insertEdge(parent, null, null, corner4, corner3, "strokeColor=none;endArrow=none");
			// 4-1 and 1-4
			graph.insertEdge(parent, null, null, corner4, corner1, "strokeColor=none;endArrow=none");
			graph.insertEdge(parent, null, null, corner1, corner4, "strokeColor=none;endArrow=none");
			*/
		} /*else {
			// 1-2 and 2-1
			graph.insertEdge(parent, null, null, corner1, corner2, "endArrow=none");
			graph.insertEdge(parent, null, null, corner2, corner1, "endArrow=none");
			// 2-3 and 3-2
			graph.insertEdge(parent, null, null, corner2, corner3, "endArrow=none");
			graph.insertEdge(parent, null, null, corner3, corner2, "endArrow=none");
			// 3-4 and 4-3
			graph.insertEdge(parent, null, null, corner3, corner4, "endArrow=none");
			graph.insertEdge(parent, null, null, corner4, corner3, "endArrow=none");
			// 4-1 and 1-4
			graph.insertEdge(parent, null, null, corner4, corner1, "endArrow=none");
			graph.insertEdge(parent, null, null, corner1, corner4, "endArrow=none");
			
		}*/
		
		for(int i = 0; i < cornerRelatedList.size(); i++) {
			
			mxCell current = cornerRelatedList.get(i);
			mxCell next = (i == cornerRelatedList.size()-1) ? cornerRelatedList.get(0) : cornerRelatedList.get(i+1);
			
			graph.insertEdge(parent, null, null, current, next, style);
			graph.insertEdge(parent, null, null, next, current, style);
		}
	}

	private int numberOfVertexInRooms(DataAccessRoomFile dataAccessRoomFile) {
		// Rooms
		int numberRooms = dataAccessRoomFile.getNumberOfRoom();
//		List<Integer> roomIDs = new ArrayList<Integer>();
//		for (int i = 1; i <= numberRooms; i++) {
//			roomI
//		}
		// Corners
		// int numberCorners = numberRooms * 4; // NUMBER OF CORNERS IS CONSIDERING ROOMS ARE SQUARED
		int numberOfVertex = 0; // PREVIOUS: numberCorners;
		// Doors
		for (int i = 1; i <= numberRooms; i++) {
			numberOfVertex += dataAccessRoomFile.getRoomNumberCorner(i);
			numberOfVertex += dataAccessRoomFile.getRoomNumberDoor(i);
			numberOfVertex += dataAccessRoomFile.getRoomNumberInvisibleDoor(i);
		}
		// Stairs
		numberOfVertex += dataAccessRoomFile.getNumberStairs();
		return numberOfVertex;
	}
	
	public String floorInformation(File roomFile) {
		DataAccessRoomFile dataAccess = new DataAccessRoomFile(roomFile);
		String floorName = dataAccess.getName();
		return floorName;
	}

	/**
	 * Prints the vertices.
	 *
	 * @param vertices
	 */
	public void printVertices(mxCell[] vertices) {
		for (int j = 0; j < vertices.length; j++) {
			System.out.println(j + "- " + vertices[j].getGeometry().getX() + ", " + vertices[j].getGeometry().getY() + "    [" + vertices[j].getValue().toString() + "]");
		}
	}

	public int searchIndexVertexFromItemID(long itemID) {
		String vertex = null;
		int i = 0;
		for (; i < vertices.length; i++) {
			mxCell cell = vertices[i];
			vertex = (String) cell.getValue();
			String[] array = vertex.split(", ");
			String type = array[0];
			// String room = array[1];
			if (type.equalsIgnoreCase("Painting") || type.equalsIgnoreCase("Sculpture")) {
				long ID = Long.valueOf(array[2]).longValue();
				if (ID == itemID) {
					//System.out.println("Equals");
					break;
				}
			}
		}
		return i;
	}
	
	/**
	 * Change item's dimensiones when map is scaled
	 * 
	 * @param itemID
	 * @param height
	 * @param width
	 */
	public void changeDimensionsOfIconFrom(long itemID, double height, double width) {
		int i = searchIndexVertexFromItemID(itemID);
		mxCell cell = vertices[i];
		cell.getGeometry().setHeight(height);
		cell.getGeometry().setWidth(width);
		roomGraphComponent.refresh();
	}
	
	/**
	 * Add all @param user's users to the users cell array
	 * 
	 * @param user
	 */
	public void addUsersToFloorGraph(ArrayList<User> user) {
		
		// Create users array
		users = new mxCell[user.size()];
		
		// Get graph to begin update (insert all user cells)
		roomGraphComponent.getGraph().getModel().beginUpdate();
		// Object parent = roomGraphComponent.getGraph().getDefaultParent();
		
		// For every user
		for (int i = 0; i < user.size(); i++) {
			// Save user in users[]
			users[i] = user.get(i).userCell;
			
			// Insert vertex in graph
			// mxGraph.insertVertex(parent, id, value, x, y, width, height, style)
			/*roomGraphComponent.getGraph().insertVertex(parent, users[i].getId(), users[i].getValue(),
					users[i].getGeometry().getX(), users[i].getGeometry().getY(),
					users[i].getGeometry().getWidth(), users[i].getGeometry().getHeight(),
					users[i].getStyle()); // Add vertex to floor graph
			*/
			roomGraphComponent.getGraph().addCell(users[i]); // VERTEX DOESN'T MOVE!!!!!!!!!!!!!!
		}
		
		// End update
		roomGraphComponent.getGraph().getModel().endUpdate();
		
		roomGraphComponent.refresh();
	}
	
	/**
	 * Refreshes the Floor Graph
	 * @return
	 */
	public void refreshFloorGraph() {
		
		roomGraphComponent.getGraph().refresh();
		//roomGraphComponent.getSelectionCellsHandler().refresh();
		
		// WORKING!!!
		//System.out.println("CELL: " + users[0].getGeometry());
		//System.out.println("GRAPH'S CELL: " + model.getCell("626")); // 626 is user 1's id
	}
	
	/**
	 * Returns item's room by item number.
	 * 
	 * @param itemID
	 * @return String
	 */
	public String getGraphItemRoom(int itemID) {
		return itemFile.getItemRoom(itemID);
	}
	
	/**
	 * Returns the room from the specified coordinates (x, y).
	 * 
	 * @param x
	 * @param y
	 * @return		CurrentRoom || -1 if error
	 */
	public int getRoomFromPosition(int x, int y) {
		
		int room = -1; // Return variable
		
		int NUMBER_ROOMS = rooms.size();
		
		int r = 0;
		boolean roomFound = false;
		
		while (r < NUMBER_ROOMS && !roomFound) {
			if(rooms.get(r).contains(x, y)) { // Check coordinates for every polygon (room)
				room = r;
				roomFound = true;
			};
			
			r++;
		}
		
		return room;
	}
}
