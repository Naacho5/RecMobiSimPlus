package es.unizar.access;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

public class SVGParserSimulation {
	
	List<String> errorList;
	List<Corner> corners;
	List<Room> rooms;
	List<Door> doors;
	List<Stairs> listStairs;
	List<Item> items;
	List<RoomSeparator> roomSeparators;
	NodeList nodeList;
	String filePath;
	double zoom;
	int drawIconDim;
	String visitable = "";
	List<Element> connectables;
	boolean stop = false;
	long lastCorner = 0;
	
	private DataAccess d;
	private File svg;

	public SVGParserSimulation(DataAccess d, File svg) {
		this.d = d;
		this.svg = svg;
		parse();
	}

	private void parse() {
		
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(svg);
			doc.getDocumentElement().normalize();
			
			corners = new ArrayList<Corner>();
			rooms = new ArrayList<Room>();
			errorList = new ArrayList<String>();
			doors = new ArrayList<Door>();
			listStairs = new ArrayList<Stairs>();
			rooms = new ArrayList<Room>();
			items = new ArrayList<Item>();
			roomSeparators = new ArrayList<RoomSeparator>();
						
		    Node first = doc.getElementsByTagName("svg").item(0);		    
		    
		    if(first.getNodeType() == Node.ELEMENT_NODE) {
//		    	zoom = Double.parseDouble(((Element)first).getAttribute("zoom"));
		    	drawIconDim = Integer.parseInt(((Element)first).getAttribute("drawIconDim"));
		    	nodeList = first.getChildNodes();
			    int n = nodeList.getLength();
			    //System.out.println(n);
			    			    
			    int i = 0;
			    List<Element> walls = new ArrayList<Element>();
				List<Element> separators = new ArrayList<Element>();
				List<Element> images = new ArrayList<Element>();
				connectables = new ArrayList<Element>();
				while(i < n) {
					Node node = nodeList.item(i);
					if(node.getNodeType() == Node.ELEMENT_NODE) {
						Element e = (Element)node;
						//if(e.getNodeName() == "line" && e.getAttribute("roomLabel").equals("81")) System.out.println(e.getAttribute("labelCorner1")+" "+e.getAttribute("c1")+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("c2"));
						if(e.getNodeName() == "line") {
							if(e.hasAttribute("stroke-dasharray")) separators.add(e);
							else walls.add(e);
						}
						else if(e.getNodeName() == "image") images.add(e);
					}
					i++;
				}
				
				Element current;
			    for(Element w : walls) {		
		            saveElement(w);
			    }
			    
			    while(walls.size() > 0) {
			    	Element e = walls.get(0);
			    	List<Element> roomWalls = lookForRoom(walls,e);
					if(roomWalls != null) {
						List<Corner> roomCorners = new ArrayList<Corner>();
						sortCorners(roomWalls);
						long lastCorner = 0;
						for(i = 0; i < roomWalls.size(); i++) {
							Element el = roomWalls.get(i);
														
							double c1_x = (Double.parseDouble(el.getAttribute("x1"))*2-drawIconDim)/2;
							double c1_y = (Double.parseDouble(el.getAttribute("y1"))*2-drawIconDim)/2;
							double c2_x = (Double.parseDouble(el.getAttribute("x2"))*2-drawIconDim)/2;
							double c2_y = (Double.parseDouble(el.getAttribute("y2"))*2-drawIconDim)/2;
																
							if(findCorner(roomCorners,c1_x,c1_y) == null) {
								Corner c1 = findCorner(corners,c1_x,c1_y);
								if(c1 != null) {											
									c1.setVertex_label((c1.getVertex_label() <= lastCorner) ? (lastCorner+1) : c1.getVertex_label());
									roomCorners.add(c1);
									lastCorner = c1.getVertex_label();
								}
							}
							if(findCorner(roomCorners,c2_x,c2_y) == null) {
								Corner c2 = findCorner(corners,c2_x,c2_y);
								if(c2 != null) {
									c2.setVertex_label((c2.getVertex_label() <= lastCorner) ? (lastCorner+1) : c2.getVertex_label());
									roomCorners.add(c2);
									lastCorner = c2.getVertex_label();
								}
							}	
						}
						if(roomCorners.size() == roomWalls.size()) {
							Room room;
							if(roomWalls.get(0).hasAttribute("roomLabel")) {
								room = new Room(Integer.parseInt(roomWalls.get(0).getAttribute("roomLabel")), roomCorners);
							}else {
								room = new Room(rooms.size()+1, roomCorners);
							}
							if(rooms.stream().filter(r -> sameRoom(room,r)).findAny().orElse(null) == null) {
								//System.out.println(room.getCorners());
								if(!rooms.add(room)) {
									errorList.add("Couldn't add room to map");
								}
							}
						}
						for(Element w : roomWalls) {
							walls.remove(w);
						}
					}else {
						walls.remove(e);
					}
					
			    	
			    }
	
			    for (Element sep : separators) {
			    	saveElement(sep);
			    }
			    
			    for (i=0; i<images.size(); i++) {
			        current = images.get(i);
			        //System.out.println(i);
		            saveElement(current);			 
			    }
			    
			    for (i=0; i<connectables.size(); i++) {
			        current = connectables.get(i);
			        //System.out.println(i);
			        connectConnectable(current);			 
			    }
				
				if(d instanceof DataAccessGraphFile) {
					DataAccessGraphFile accessGraphFile = (DataAccessGraphFile)d;
				    
				    int numberRoomsAndSubrooms = 0;
				    Set<Pair<Door, Door>> connectedDoors = new HashSet<Pair<Door, Door>>();
					Set<Pair<Door, Stairs>> connectedDoorStairs = new HashSet<Pair<Door, Stairs>>();
					Set<Pair<Stairs, Stairs>> connectedStairs = new HashSet<Pair<Stairs, Stairs>>();
					
					accessGraphFile.setNumberOfRoom(rooms.size());
					
					int numItems = items.size();
					int idStairs = numItems + doors.size() + 1;
					int idSubrooms = rooms.size() + 1;
					int numObjects = numItems + doors.size() + listStairs.size();
					// Number of connected invisible doors = num invisible doors * 2 (connected to themselves)
					int numConnectedInvisibleDoors = roomSeparators.size()*2;
					// Create array
					RoomSeparator invisibleDoorsArray[] = new RoomSeparator[numConnectedInvisibleDoors];
					// Fill array with RoomSeparators (every rs = invisibleDoors). Their position in array will be the future invisible-door's id
					for (int connectedInvisible = 0; connectedInvisible < numConnectedInvisibleDoors; connectedInvisible=connectedInvisible+2) { // Connect consecutive invisible doors
						int id = connectedInvisible/2;
						invisibleDoorsArray[connectedInvisible] = roomSeparators.get(id);
						invisibleDoorsArray[connectedInvisible+1] = roomSeparators.get(id);
						
						//System.out.println("invisibleDoorsArray[" + connectedInvisible + "] = " + getRoomSeparators().get(id));
						//System.out.println("invisibleDoorsArray[" + (connectedInvisible+1) + "] = " + getRoomSeparators().get(id));
					}
					
					// Array will be filled with strings (door_numDoorInRoom_Room) when invisible door get their respective id's assigned
					String connectedInvisibleDoors[] = new String[numConnectedInvisibleDoors];
					
				    for(int room = 0; room < rooms.size(); room++) {
				    	accessGraphFile.setRoom(room+1, rooms.get(room).getLabel());
				    	accessGraphFile.setNumberOfItemsByRoom(room+1, rooms.get(room).getNumItems());
				    	for(int item = 0; item < rooms.get(room).getNumItems(); item++) {
							// Set item id/label
							accessGraphFile.setItemOfRoom(item+1, room+1, Long.toString(rooms.get(room).getItems().get(item).getVertex_label()));
						}
				    	accessGraphFile.setNumberOfDoorsByRoom(room+1, rooms.get(room).getNumDoors());				    							
				    	for(int door = 0; door < rooms.get(room).getNumDoors(); door++) {
							
							Door currentDoor = rooms.get(room).getDoors().get(door);
							// Set door id/label
							accessGraphFile.setDoorOfRoom(door+1, room+1, Long.toString(currentDoor.getVertex_label() + numItems));
							
							/**
							 * CONNECTED DOOR-DOOR, DOOR-STAIRS
							 */
							// For every connected to door
							for (Connectable c: currentDoor.getConnectedTo()) {
								// If both elements aren't null
								if (currentDoor != null && c != null) {
									// Check type
									if (c instanceof Door) { // If it's a door, add it to connectedDoors
										Door other = (Door) c;
										Pair<Door, Door> pairDoors = new Pair<Door, Door>(currentDoor, other);
										Pair<Door, Door> pairDoorsSwitched = new Pair<Door, Door>(other, currentDoor);
										
										// If it doesn't exist yet, add pair
										if(!connectedDoors.contains(pairDoors) && !connectedDoors.contains(pairDoorsSwitched))
											connectedDoors.add(pairDoors);
									}
									else { // If it's stairs, add it to connectedDoorStairs
										Stairs other = (Stairs) c;
										Pair<Door, Stairs> pairDoorStairs = new Pair<Door, Stairs>(currentDoor, other);
										
										if(!connectedDoorStairs.contains(pairDoorStairs))
											connectedDoorStairs.add(pairDoorStairs);
									}
								}
							}														
						}
				    	
				    	int numSubrooms = rooms.get(room).getNumSubRooms();
						// Set number of subrooms in room
						accessGraphFile.setRoomNumberSubrooms(room+1, numSubrooms);
						
						// If room contains subrooms
						if (numSubrooms > 0) {
							// Add number of subrooms
							numberRoomsAndSubrooms += numSubrooms;
							
							// For every subroom
							for (int subroom = 0; subroom < rooms.get(room).getSubRooms().size(); subroom++) {
								// Set subroom
								accessGraphFile.setSubroom(subroom+1, room+1, idSubrooms);
								idSubrooms++;
								
								int items = 0, doors = 0, invisibleDoors = 0;
								// Set subroom's items
								for (int item = 0; item < rooms.get(room).getNumItems(); item++) {
									// If item intersects polygon, it's inside the subroom
									if (objectIntesectsRoomPolygon(rooms.get(room).getItems().get(item).getVertex_xy(),
											rooms.get(room).getSubRooms().get(subroom).getPolygon())) {
										
										// Add item to subroom
										accessGraphFile.setItemOfSubroom(items+1, subroom+1, room+1, Long.toString(rooms.get(room).getItems().get(item).getVertex_label()));
										items++;
									}
								}
								// Set number of subroom's items
								accessGraphFile.setNumberOfItemsBySubroom(subroom+1, room+1, items);
								
								
								// Set subroom's doors
								for (int door = 0; door < rooms.get(room).getNumDoors(); door++) {
									// If door intersects polygon, it's inside the subroom
									if (objectIntesectsRoomPolygon(rooms.get(room).getDoors().get(door).getVertex_xy(),
											rooms.get(room).getSubRooms().get(subroom).getPolygon())) {
										
										// Add door to subroom
										accessGraphFile.setDoorOfSubroom(doors+1, subroom+1, room+1, Long.toString(rooms.get(room).getDoors().get(door).getVertex_label() + numItems));
										
										doors++;
									}
								}
								// Set number of subroom's doors
								accessGraphFile.setNumberOfDoorsBySubroom(subroom+1, room+1, doors);
								
								
								// Set subroom's invisible doors
								for (int invisibleDoor = 0; invisibleDoor < rooms.get(room).getNumRoomSeparators(); invisibleDoor++) {
									
									RoomSeparator rs = rooms.get(room).getRoomSeparators().get(invisibleDoor);
									// If invisible door intersects polygon, subroom contains invisible door
									if (objectIntesectsRoomPolygon(rs.getMiddlePoint(),
											rooms.get(room).getSubRooms().get(subroom).getPolygon())) {
										
										// Get index, set its value as null and use it to create invisible-door's id
										int index = Arrays.asList(invisibleDoorsArray).indexOf(rs);
										invisibleDoorsArray[index] = null;
										int id = index + 1 + numObjects;
//										System.out.println(id);
										// Add invisible door to subroom
										accessGraphFile.setInvisibleDoorOfSubroom(invisibleDoors+1, subroom+1, room+1, Integer.toString(id));
										invisibleDoors++;
										
										// Set connected invisible doors
										connectedInvisibleDoors[index] = Literals.INVISIBLE_DOOR_OF_SUBROOM + (invisibleDoors) + "_" + (subroom+1) + "_" + (room+1);
									}
								}
								// Set number of subroom's doors
								accessGraphFile.setNumberOfInvisibleDoorsBySubroom(subroom+1, room+1, invisibleDoors);
							}
						}
						else {
							// Add 1 (whole room)
							numberRoomsAndSubrooms += 1;
						}
				    }
				    
				    // Set number of rooms and subrooms (all "rooms" for graph)
					accessGraphFile.setNumberOfRoomsAndSubrooms(numberRoomsAndSubrooms);
					
					/**
					 * STAIRS
					 */
					// Set number of stairs
					accessGraphFile.setNumberOfStairs(listStairs.size());
					// For each stairs
					for(int stairs = 0; stairs < listStairs.size(); stairs++) {
						
						Stairs currentStairs = listStairs.get(stairs);
						// Set stairs locations
						accessGraphFile.setStairsOfRoom(stairs+1, Integer.toString(idStairs));
						idStairs++;
						
						/**
						 * CONNECTED STAIRS-STAIRS
						 */
						// For every connected to stairs
						for (Connectable c: currentStairs.getConnectedTo()) { // There will be only 1 iteration (only connected to 1 stairs)
							Stairs other = (Stairs) c;
							if (currentStairs != null && other != null) {
								Pair<Stairs, Stairs> pairStairs = new Pair<Stairs, Stairs>(currentStairs, other);
								Pair<Stairs, Stairs> pairStairsSwitched = new Pair<Stairs, Stairs>(other, currentStairs);
								
								// If it doesn't exist yet, add pair
								if(!connectedStairs.contains(pairStairs) && !connectedStairs.contains(pairStairsSwitched))
									connectedStairs.add(pairStairs);
							}
						}
					}
					
					accessGraphFile.setNumberOfConnectedDoor(connectedDoors.size());
					
					// For every pair of connected doors
					int pos = 1;
					for(Pair<Door, Door> pair: connectedDoors) {
						
						int roomDoorF = rooms.indexOf(pair.getF().getRoom());
						int roomDoorS = rooms.indexOf(pair.getS().getRoom());
						int doorF = rooms.get(roomDoorF).getDoors().indexOf(pair.getF()) + 1;
						int doorS = rooms.get(roomDoorS).getDoors().indexOf(pair.getS()) + 1;
						
						roomDoorF += 1;
						roomDoorS += 1;
						
						accessGraphFile.setConnectedDoor(pos, 
							Literals.DOOR_OF_ROOM + doorF + "_" + roomDoorF,
							Literals.DOOR_OF_ROOM + doorS + "_" + roomDoorS);
						
						pos++;
					}

					accessGraphFile.setNumberOfConnectedDoorStairs(connectedDoorStairs.size());

					// For every pair of connected door-stairs
					pos = 1;
					for(Pair<Door, Stairs> pair: connectedDoorStairs) {
						
						int roomDoor = rooms.indexOf(pair.getF().getRoom());
						int stairs = listStairs.indexOf(pair.getS()) + 1;
						int door = rooms.get(roomDoor).getDoors().indexOf(pair.getF()) + 1;
						
						roomDoor += 1;
						
						accessGraphFile.setConnectedDoorStairs(pos, 
							Literals.STAIRS_OF_ROOM + stairs,
							Literals.DOOR_OF_ROOM + door + "_" + roomDoor);
						
						pos++;
					}

					accessGraphFile.setNumberOfConnectedStairs(connectedStairs.size());

					// For every pair of connected stairs
					pos = 1;
					for(Pair<Stairs, Stairs> pair: connectedStairs) {
						
						int stairsF = listStairs.indexOf(pair.getF()) + 1;
						int stairsS = listStairs.indexOf(pair.getS()) + 1;
						
						accessGraphFile.setConnectedStairs(pos, 
							Literals.STAIRS_OF_ROOM + stairsF,
							Literals.STAIRS_OF_ROOM + stairsS);
						
						pos++;
					}
					
					/**
					 * CONNECTED INVISIBLE DOORS
					 */
					accessGraphFile.setNumberOfConnectedInvisibleDoor(numConnectedInvisibleDoors/2);
					for (int connectedInvisible = 0; connectedInvisible < numConnectedInvisibleDoors; connectedInvisible=connectedInvisible+2) { // Connect consecutive invisible doors
						int id = connectedInvisible/2;
						accessGraphFile.setConnectedInvisibleDoor(id+1, connectedInvisibleDoors[connectedInvisible], connectedInvisibleDoors[connectedInvisible+1]);
					}
				}
				else if(d instanceof DataAccessRoomFile) {
					DataAccessRoomFile accessRoomFile = (DataAccessRoomFile)d;
//					System.out.println("ROOM");    
			    	int width = Integer.parseInt(((Element)first).getAttribute("width"));
			    	int height = Integer.parseInt(((Element)first).getAttribute("height"));
			    	String name = ((Element)first).getAttribute("name");
			    	Double pixelRepresentsInMeters = Double.parseDouble(((Element)first).getAttribute("pixelRepresentsInMeters"));
//			    	System.out.println("ROOM1");
			    	// Set map's name
					accessRoomFile.setName(name);
//					System.out.println("ROOM2");
					// Set map's width
					accessRoomFile.setMapWidth(width);
					// Set map's width
					accessRoomFile.setMapHeight(height);
					// Set map's width
					accessRoomFile.setMapPixelRepresentsMeters(pixelRepresentsInMeters);
					
					int numRooms = rooms.size();
					// Set number of rooms
					accessRoomFile.setNumberOfRoom(numRooms);

					int room = 0;
					// For each room
					while (room < numRooms) {
						// Set room's label
						accessRoomFile.setRoomLabel(room+1, rooms.get(room).getLabel()); // accessRoomFile.setRoomLabel(room+1, room+1);
						
						// Set number of corners in room
						accessRoomFile.setRoomNumberCorner(room+1, rooms.get(room).getNumCorners());
						// For each corner in room
						for(int corner = 0; corner < rooms.get(room).getNumCorners(); corner++) {
							// Set corner locations
							accessRoomFile.setRoomCorner(corner+1, room+1, rooms.get(room).getCorners().get(corner).getVertex_xy().toString());
						}
						
						// Set number of doors in room
						accessRoomFile.setRoomNumberDoor(room+1, rooms.get(room).getNumDoors());
						// For each door in room
						for(int door = 0; door < rooms.get(room).getNumDoors(); door++) {
							// Set door locations
							accessRoomFile.setRoomDoorXY(door+1, room+1, rooms.get(room).getDoors().get(door).getVertex_xy().toString());
						}
						
						room++;
					}
					
					int numStairs = listStairs.size();
					// Set number of stairs
					accessRoomFile.setNumberStairs(numStairs);
					// For each stairs
					for(int stairs = 0; stairs < numStairs; stairs++) {
						// Set stairs locations
						accessRoomFile.setStairs(stairs+1, listStairs.get(stairs).getVertex_xy().toString());
					}
					
					/**
					 * SUBROOMS
					 */
					// For each subroom -> Done afterwards, to store ids correctly
					for (int newRoom = 0; newRoom < numRooms; newRoom++) {
						
						// Num subrooms
						int numSubrooms = rooms.get(newRoom).getNumSubRooms();
						
						// Save RoomSeparators (Editor)
						int roomSeparators = rooms.get(newRoom).getNumRoomSeparators();
						// Number of room separators AND invisible doors
						accessRoomFile.setRoomNumberRoomSeparators(newRoom+1, roomSeparators);
						accessRoomFile.setRoomNumberInvisibleDoor(newRoom+1, roomSeparators*2);
						
						
						int invisible = 1;
						// For every subroom (if there exist)
						for (int sub = 0; sub < numSubrooms; sub++) {
							// For every invisible door (RoomSeparator)
							for (i = 0; i < roomSeparators; i++) { // One invisible door per room separator (in the middle)
								/**
								 * ROOM SEPARATORS
								 */
								// Set corners for roomseparator of room
								RoomSeparator currentRS = rooms.get(newRoom).getRoomSeparators().get(i);
								String roomSeparatorCorners = currentRS.getC1().getVertex_label() + ", " + currentRS.getC2().getVertex_label();
								accessRoomFile.setRoomRoomseparatorCorners(i+1, newRoom+1, roomSeparatorCorners);
								
								/**
								 * INVISIBLE DOORS
								 */
								// Get middle point and set it as door's location
								Point doorLocation = currentRS.getMiddlePoint();
								// Get subroom's polygon
								Polygon polygon = rooms.get(newRoom).getSubRooms().get(sub).getPolygon();
								
								// If subroom contains doorLocation, add Invisible Door to subroom
									if (objectIntesectsRoomPolygon(doorLocation, polygon)) {
										accessRoomFile.setRoomInvisibleDoorXY(invisible, newRoom+1, doorLocation.toString());
										invisible++;
									}
							}
						}
					}
					
				    
				}else if(d instanceof DataAccessItemFile) {
					DataAccessItemFile accessItemFile = (DataAccessItemFile)d;
					
					String name = ((Element)first).getAttribute("name");
					// Set room(s) name
					accessItemFile.setName(name);
					
					// Set number of rooms
					accessItemFile.setNumberOfItems(items.size());
					
					int drawIconDim = Integer.parseInt(((Element)first).getAttribute("drawIconDim"));
					// Set vertex dimensions
					accessItemFile.setVertexDimensionHeight(drawIconDim);
					accessItemFile.setVertexDimensionWidth(drawIconDim);
					
					int index = 1; // index of the current item
					
					// For each item
					for (Item it: items) {
						
						// Set itemID
						accessItemFile.setItemID(index, index);
						it.setVertex_label(index);
						
						// Main attributes
						// Set item room
						accessItemFile.setItemRoom(index, it.getRoom().getLabel());
						// Set item label
						accessItemFile.setVertexLabel(index, it.getItemLabel());
						// Set item position
						accessItemFile.setVertexXY(index, it.getVertex_xy().toString());
						
						// Item attributes
						// Set item title
						accessItemFile.setItemTitle(index, checkItemAttribute(it.getTitle()));
						// Set item vertexURL
						accessItemFile.setVertexURL(index, checkItemAttribute(it.getUrlIcon()));
						// Set item nationality
						accessItemFile.setItemNationality(index, checkItemAttribute(it.getNationality()));
						// Set item beginDate
						accessItemFile.setItemBeginDate(index, checkItemAttribute(it.getBeginDate()));
						// Set item endDate
						accessItemFile.setItemEndDate(index, checkItemAttribute(it.getEndDate()));
						// Set item date
						accessItemFile.setItemDate(index, checkItemAttribute(it.getDate()));
						// Set item department
						//accessItemFile.setItemDepartment(index, checkItemAttribute(i.getDepartment()));
						// Set item height
						accessItemFile.setItemHeight(index, it.getHeight());
						// Set item width
						accessItemFile.setItemWidth(index, it.getWidth());
						
						index++;
					}
				}
			    
				d.storeProperties();
	        }
		    		    		    
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

	
	boolean areAdjacentWalls(Element line1, Element line2, Element lastLine) {
		if(line1 != line2 && line2 != lastLine) {		
			if((Double.parseDouble(line1.getAttribute("x1")) == Double.parseDouble(line2.getAttribute("x1"))
			&&
			Double.parseDouble(line1.getAttribute("y1")) == Double.parseDouble(line2.getAttribute("y1")))
			||
			(Double.parseDouble(line1.getAttribute("x2")) == Double.parseDouble(line2.getAttribute("x2"))
			&&
			Double.parseDouble(line1.getAttribute("y2")) == Double.parseDouble(line2.getAttribute("y2")))	
			||
			(Double.parseDouble(line1.getAttribute("x1")) == Double.parseDouble(line2.getAttribute("x2"))
			&&
			Double.parseDouble(line1.getAttribute("y1")) == Double.parseDouble(line2.getAttribute("y2")))
			||
			(Double.parseDouble(line1.getAttribute("x2")) == Double.parseDouble(line2.getAttribute("x1"))
			&&
			Double.parseDouble(line1.getAttribute("y2")) == Double.parseDouble(line2.getAttribute("y1")))		
			) {
				return true;
			}else {
				return false;
			}																
		}else {return false;}
	}
	
	
	List<Element> lookForRoom(List<Element> SVGElementList, Element startLine) {
	    int n = SVGElementList.size();
	    Element line = SVGElementList.get(0);
	    List<Element> room = new ArrayList<Element>();
	    boolean foundAdjWall = false;
	    boolean finished = false;
	    //System.out.println("start "+startLine.getAttribute("x1")+" "+startLine.getAttribute("y1")+" "+startLine.getAttribute("x2")+" "+startLine.getAttribute("y2"));
	    Element currentLine = startLine;
	    Element firstLine = startLine;
	    Element lastLine = startLine;
	    room.add(startLine);
	    while(!finished){
	        int i = 0;
	        foundAdjWall = false;
	        while(!foundAdjWall && i < n) {					
	            line = SVGElementList.get(i);
	            //if(line.getNodeName() == "line") System.out.println(line.getAttribute("x1")+" "+line.getAttribute("y1")+" "+line.getAttribute("x2")+" "+line.getAttribute("y2"));
	            if(line.getNodeName() == "line") foundAdjWall = areAdjacentWalls(currentLine,line,lastLine);					
	            i++;
	        }
	        if(foundAdjWall){
	            if(line == firstLine) {
	                finished = true;
	            }else {
		            room.add(line);
		            lastLine = currentLine;
		            currentLine = line;
	            }
	        }else{
	            finished = true;
	        }
	    }
	    if(foundAdjWall){
	    	Element e = room.remove(0);
	    	room.add(e);
	        return room;
	    }else{
	        return null;
	    }
	}
	
	class PointPair{
		public Point p1;
		public Point p2;
		PointPair(Point P1, Point P2){
			p1 = P1;
			p2 = P2;
		}
	}
	
	
	void saveElement(Element e) {
		
		List<PointPair> wallsPoints = new ArrayList<PointPair>();				
		
		//if(e.getAttribute("roomLabel").equals("81")) System.out.println(e.getAttribute("labelCorner1")+" "+e.getAttribute("x1")+" "+e.getAttribute("y1")+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("x2")+" "+e.getAttribute("y2"));
		switch(e.getNodeName()) {
			case "line":
				int i = 0;
				int l = nodeList.getLength();
				List<Element> lines = new ArrayList<Element>();
				while(i < l) {					
					Element current = (Element)nodeList.item(i);
					if(current.getNodeName() == "line" && !current.hasAttribute("stroke-dasharray")) lines.add(current);					
					i++;							
				}
												
				double c1_x = (Double.parseDouble(e.getAttribute("x1"))*2-drawIconDim)/2;
				double c1_y = (Double.parseDouble(e.getAttribute("y1"))*2-drawIconDim)/2;
				double c2_x = (Double.parseDouble(e.getAttribute("x2"))*2-drawIconDim)/2;
				double c2_y = (Double.parseDouble(e.getAttribute("y2"))*2-drawIconDim)/2;
				
				Point p1 = new Point(c1_x,c1_y);
				Point p2 = new Point(c2_x,c2_y);
								
				if(e.hasAttribute("stroke-dasharray")) {
					//potentialRoomSeparators.add(new PointPair(p1,p2));
					
					try {						
//						Room r = rooms.stream().filter(room -> room.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
						
						//String[] corner1coord = e.getAttribute("c1").split(",");
						Corner corner1 = findCorner(corners, c1_x, c1_y);
						//String[] corner2coord = e.getAttribute("c2").split(",");
						Corner corner2 = findCorner(corners, c2_x, c2_y);
						
						Room r;
						if(e.hasAttribute("room")) r = rooms.stream().filter(room -> room.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
						else r = corner1.getRoom();
						
						// Create roomSeparator using corners
						RoomSeparator rs;
						if(e.hasAttribute("separatorLabel")) {
							rs = new RoomSeparator(r, Long.parseLong(e.getAttribute("separatorLabel")), corner1, corner2);
						}else {
							rs = new RoomSeparator(r, r.getNumRoomSeparators()+1, corner1, corner2);
						}
						if(r != null) {
							r.addRoomSeparator(rs);
						}
						roomSeparators.add(rs);
			            //System.out.println(r.getNumSubRooms());
						//System.out.println("ok");
					}
					catch(Exception ex) {
						ex.printStackTrace();
					}					
				}else {
					//if(e.getAttribute("roomLabel").equals("1")) System.out.println(lastCorner+" "+Long.parseLong(e.getAttribute("labelCorner1"))+" "+(Long.parseLong(e.getAttribute("labelCorner1")) == lastCorner));													
					//if(e.getAttribute("roomLabel").equals("1")) System.out.println(lastCorner+" "+Long.parseLong(e.getAttribute("labelCorner2"))+" "+(Long.parseLong(e.getAttribute("labelCorner2")) == lastCorner));
					Corner corner1, corner2;
					corner1 = new Corner(null, e.getAttribute("labelCorner1").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner1")), p1);
					corner2 = new Corner(null, e.getAttribute("labelCorner2").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner2")), p2);
					//if(e.getAttribute("roomLabel").equals("81")) System.out.println(e.getAttribute("labelCorner1")+" "+e.getAttribute("c1")+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("c2"));
					if(!wallCollidesCorners(corner1,corner2,wallsPoints)) {
						wallsPoints.add(new PointPair(p1,p2));
						//if(e.getAttribute("roomLabel").equals("81")) System.out.println(corner1.getVertex_label()+" "+e.getAttribute("labelCorner1")+" "+e.getAttribute("x1")+" "+e.getAttribute("y1")); 
						if (findCorner(corners,c1_x,c1_y) == null) {
//							corner1.setVertex_label((Long.parseLong(e.getAttribute("labelCorner1")) == lastCorner) ? (lastCorner+1) : Long.parseLong(e.getAttribute("labelCorner1")));
//							lastCorner = corner1.getVertex_label();
							corners.add(corner1);
							//if(e.getAttribute("roomLabel").equals("1")) System.out.println("in "+findCorner(model.getCorners(),c1_x,c1_y).getVertex_label()+" "+e.getAttribute("labelCorner1")+lastCorner+" "+e.getAttribute("c1")); 
						}else {
							errorList.add("COULDN'T ADD CORNER TO MAP");
						}
						//if(e.getAttribute("roomLabel").equals("81")) System.out.println(corner2.getVertex_label()+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("x2")+" "+e.getAttribute("y2")); 
						if (findCorner(corners,c2_x,c2_y) == null) {
//							corner2.setVertex_label((Long.parseLong(e.getAttribute("labelCorner2")) == lastCorner) ? (lastCorner+1) : Long.parseLong(e.getAttribute("labelCorner2")));
//							lastCorner = corner2.getVertex_label();
							corners.add(corner2);
							//if(e.getAttribute("roomLabel").equals("1")) System.out.println("in "+findCorner(model.getCorners(),c2_x,c2_y).getVertex_label()+" "+e.getAttribute("labelCorner2")+lastCorner+" "+e.getAttribute("c2")); 
						}else {
							errorList.add("COULDN'T ADD CORNER TO MAP");
						}
					}else {
						errorList.add("Walls colliding");
					}					
				}
				break;
				
			case "image":
				String path = e.getAttribute("href");
				if(path.endsWith("\\door.png")) {
					Door d;
					Room doorRoom = null;
					if(e.hasAttribute("room")) doorRoom = rooms.stream().filter(r -> r.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
					else doorRoom = rooms.stream().filter(r -> r.getPolygon().contains(Double.parseDouble(e.getAttribute("x")), Double.parseDouble(e.getAttribute("y")))).findAny().orElse(null);
					if(e.hasAttribute("label")) {
						d = new Door(doorRoom, Long.parseLong(e.getAttribute("label")), new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}else {
						d = new Door(doorRoom, doors.size()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}
					//Door d = new Door(null, model.getNumDoors()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					if(!doors.add(d)) {
						errorList.add("COULDN'T ADD DOOR TO MAP");
					}else {
						connectables.add(e);
						if(doorRoom != null) {
							doorRoom.addDoor(d);
						}
					}
				}else if(path.endsWith("\\stairs.png")) {
					Stairs stairs;
					Room stairsRoom = null;
					if(e.hasAttribute("room")) stairsRoom = rooms.stream().filter(r -> r.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
					else stairsRoom = rooms.stream().filter(r -> r.getPolygon().contains(Double.parseDouble(e.getAttribute("x")), Double.parseDouble(e.getAttribute("y")))).findAny().orElse(null);
					if(e.hasAttribute("label")) {
						stairs = new Stairs(stairsRoom, Long.parseLong(e.getAttribute("label")), new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}else {
						stairs = new Stairs(stairsRoom, listStairs.size()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}
					//Stairs stairs = new Stairs(null, model.getNumStairs()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					if (!listStairs.add(stairs)) {
						errorList.add("COULDN'T ADD STAIRS TO MAP");
					}else {
						connectables.add(e);
						if(stairsRoom != null) {
							stairsRoom.addStairs(stairs);
						}
					}
				}else if(!path.endsWith("\\corner.png")){				
					double itemX = Double.parseDouble(e.getAttribute("x"));
					double itemY = Double.parseDouble(e.getAttribute("y"));
					// Main attributes
					// Get item room
//					int roomLabel = Integer.parseInt(e.getAttribute("room")); // -> Room associated in graph file
					//roomLabel += model.getNumRooms();
					Room r;
					if(e.hasAttribute("room")) r = rooms.stream().filter(room -> room.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
					else r = rooms.stream().filter(room -> room.getPolygon().contains(itemX, itemY)).findAny().orElse(null);
					
					if(r != null) {
						// Get item label
						Long vertexLabel = (e.hasAttribute("label")) ? Long.parseLong(e.getAttribute("label")) : (long)items.size()+1;
						//vertexLabel += model.getNumItems();
						
						// CREATE ITEM AND ADD IT TO MODEL
						Item it = new Item(r, vertexLabel, new Point(itemX, itemY));
						//System.out.println(roomLabel+" "+e.getAttribute("href"));
						
						if(r != null) {r.addItem(it);}
						items.add(it);
						
						// Item attributes
						// Get item label (located in vertexLabel field) and assign it to item
						String attribute = e.getAttribute("itemLabel");
						if (attribute == null)
							System.out.println("Item doesn't have ITEM LABEL"); // Throw exception
						else
							it.setItemLabel(attribute);
						
						// Get item url and assign it to item
						attribute = path;
						if (attribute == null)
							System.out.println("Item doesn't have ICON URL");
						else
							it.setUrlIcon(attribute);
						
						// Get item title and assign it to item
						it.setTitle(e.getAttribute("title"));
						// Get item nationality and assign it to item
						it.setNationality(e.getAttribute("nationality"));
						// Get item beginDate and assign it to item
						it.setBeginDate(e.getAttribute("beginDate"));
						// Get item endDate and assign it to item
						it.setEndDate(e.getAttribute("endDate"));
						// Get item date and assign it to item
						it.setDate(e.getAttribute("date"));
						// Get item department and assign it to item
						//i.setDepartment(accessItemFile.getItemDepartment(item+1));
						// Get item width and height and assign it to item
						try {
							it.setHeight(Double.valueOf(e.getAttribute("itemHeight")));
							it.setWidth(Double.valueOf(e.getAttribute("itemWidth")));
						}
						catch (Exception ex) {
							System.out.println(ex);
							it.setHeight(0.0);
							it.setWidth(0.0);
						}
				
					}
				}

		}
				
	}
	
	
	
	List<Element> sortCorners(List<Element> listWalls){
		int i = 0;
		for(Element e : listWalls) {	
			if(i < listWalls.size()-1) {
				Element e2 = listWalls.get(i+1);
				if(e.getAttribute("c1").equals(e2.getAttribute("c1"))) {				
					String c1 = e.getAttribute("c1");
					e.setAttribute("c1", e.getAttribute("c2"));
					e.setAttribute("c2", c1);					
				}else if(e.getAttribute("c2").equals(e2.getAttribute("c2"))) {
					String c1 = e2.getAttribute("c1");
					e2.setAttribute("c1", e2.getAttribute("c2"));
					e2.setAttribute("c2", c1);
				}else if(e.getAttribute("c1").equals(e2.getAttribute("c2"))) {					
					String c1 = e.getAttribute("c1");
					e.setAttribute("c1", e.getAttribute("c2"));
					e.setAttribute("c2", c1);
					c1 = e2.getAttribute("c1");
					e2.setAttribute("c1", e2.getAttribute("c2"));
					e2.setAttribute("c2", c1);
				}
			
			}
			
			i++;
		}
		return listWalls;
	}
	
	
	boolean sameRoom(Room r1, Room r2) {
		List<Corner> listCorners1 = new ArrayList<Corner>();
		List<Corner> listCorners1Copy = new ArrayList<Corner>();
		List<Corner> listCorners2 = new ArrayList<Corner>();
		listCorners1.addAll(r1.getCorners());
		listCorners1Copy.addAll(r1.getCorners());
		listCorners2.addAll(r2.getCorners());
		if(listCorners1.size() == listCorners2.size()) {
			for(Corner c : listCorners1Copy) {
				if(listCorners2.contains(c)) {
					listCorners1.remove(c);
				}else {
					return false;
				}
			}
			return true;
		}else {
			return false;
		}
	}
				
	Corner findCorner(List<Corner> cornerList, double x, double y) {
		//System.out.println(cornerList.size()+" "+cornerList.isEmpty());
		if(!cornerList.isEmpty()) {
			for(Corner c : cornerList) {
				//System.out.println(c);
				if(c.getVertex_xy().getX() == x && c.getVertex_xy().getY() == y) {
					return c;
				}
			}
		}
		//System.out.println("fin");
		return null;
	}
	
	
	boolean wallCollidesCorners(Corner wallFirstEndpoint, Corner wallSecondEndpoint, List<PointPair> wallsPoints) {
		if (!wallsPoints.isEmpty()) {		
			for (PointPair p : wallsPoints) {
				
				Point p1 = p.p1;
				Point p2 = p.p2;
				
				if(Line2D.linesIntersect(wallFirstEndpoint.getVertex_xy().getX(), wallFirstEndpoint.getVertex_xy().getY(), 
						wallSecondEndpoint.getVertex_xy().getX(), wallSecondEndpoint.getVertex_xy().getY(), 
						p.p1.getX(), p.p1.getY(),
						p.p2.getX(), p.p2.getY())) {
					
					System.out.println("It intersects!!");
					return true;
				}
					
			}		
		}
		return false;
	}
	
	
	void connectConnectable(Element e) {
		
		String path = e.getAttribute("href");
		
		if(path.endsWith("\\door.png")) {
			Door d = (e.hasAttribute("label")) ? doors.stream().filter(door -> door.getVertex_label() == Long.parseLong(e.getAttribute("label"))).findAny().orElse(null) : doors.stream().filter(door -> door.getVertex_xy().getX() == Double.parseDouble(e.getAttribute("x")) && door.getVertex_xy().getY() == Double.parseDouble(e.getAttribute("y"))).findAny().orElse(null);;  
			int numConnectedDoors = Integer.parseInt(e.getAttribute("numConnectedDoors"));			
			for(int i = 1; i <= numConnectedDoors; i++) {
				
				String connectedDoor = e.getAttribute("connectedDoor_"+i);
							
				// door_"numDoorInRoom"_"numRoom"
				String[] arrayDoor = connectedDoor.split("_");
				long labelDoor = Long.valueOf(arrayDoor[0]);
				int labelRoom = Integer.valueOf(arrayDoor[1]);
								
				//Room room = model.getRooms().stream().filter(r -> r.getLabel() == labelRoom).findAny().orElse(null);
				Room room = rooms.stream().filter(r -> r.getLabel() == labelRoom).findAny().orElse(null);
				if(room != null) {
					Door d2 = room.getDoor(labelDoor);
					// Add connection (connectTo creates a connection for both doors)
					d.connectTo(d2);
				}				
			}
						
			int numConnectedStairs = Integer.parseInt(e.getAttribute("numConnectedStairs"));
			for(int i = 1; i <= numConnectedStairs; i++) {								
				String connectedStairs = e.getAttribute("connectedStairs_"+i);
				long label = Long.parseLong(connectedStairs);

				Stairs s = listStairs.stream().filter(st -> st.getVertex_label() == label).findAny().orElse(null);
				d.addConnection(s);
			}
		}else if(path.endsWith("\\stairs.png")) {
			int numConnectedStairs = Integer.parseInt(e.getAttribute("numConnectedStairs"));
			Stairs s = (e.hasAttribute("label")) ? listStairs.stream().filter(st -> st.getVertex_label() == Long.parseLong(e.getAttribute("label"))).findAny().orElse(null) : listStairs.stream().filter(stairs -> stairs.getVertex_xy().getX() == Double.parseDouble(e.getAttribute("x")) && stairs.getVertex_xy().getY() == Double.parseDouble(e.getAttribute("y"))).findAny().orElse(null);
			for(int i = 1; i <= numConnectedStairs; i++) {
								
				String connectedStairs = e.getAttribute("connectedStairs_"+i);
				long label = Long.parseLong(connectedStairs);

				Stairs s2 = listStairs.stream().filter(st -> st.getVertex_label() == label).findAny().orElse(null);
				s.connectTo(s2);
			}
			
		}
											
	}
	
	
	/**
	 * Write "null" in attributes with null or empty values.
	 * 
	 * @param attribute
	 * @return
	 */
	private String checkItemAttribute(String attribute) {
		
		String newValue = "null";
		if (attribute != null && !attribute.equals(""))
			newValue = attribute;
		
		return newValue;
	}
	
	
	/**
	 * Get object's dimensions and check if it collides with a room's polygon.
	 * 
	 * @param doorLocation		Point
	 * @param polygon			Polygon (room's polygon)
	 * @return T/F
	 */
	public boolean objectIntesectsRoomPolygon(Point doorLocation, Polygon polygon) {
		// Get object's rectangle
		Rectangle2D rect = new Rectangle2D.Double(doorLocation.getX()-(drawIconDim/2), doorLocation.getY()-(drawIconDim/2),
				(double) drawIconDim, (double) drawIconDim);
		
		return polygon.intersects(rect);
		
	}

}

