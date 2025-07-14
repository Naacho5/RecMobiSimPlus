package es.unizar.editor.view;

import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.util.Literals;
import es.unizar.util.ElementIdMapper;

public class SVGParser {
	
	MapEditorModel model;
	MapEditorView view;
	List<String> errorList;
	List<Corner> corners;
	List<Room> rooms;
	NodeList nodeList;
	String filePath;
	double zoom;
	int drawIconDim;
	String visitable = "";
	List<Element> connectables;
	boolean stop = false;

	public SVGParser(MapEditorModel m, MapEditorView v, String path) {
		model = m;
		filePath = path;
		view = v;
	}

	void parse() {
		
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new File(filePath));
			doc.getDocumentElement().normalize();
			
			corners = new ArrayList<Corner>();
			rooms = new ArrayList<Room>();
			errorList = new ArrayList<String>();
			
			
		    Node first = doc.getElementsByTagName("svg").item(0);		    
		    //Node firstCopy = first.cloneNode(true);
		    
		    if(first.getNodeType() == Node.ELEMENT_NODE) {
//		    	zoom = Double.parseDouble(((Element)first).getAttribute("zoom"));
		    	drawIconDim = Integer.parseInt(((Element)first).hasAttribute("drawIconDim") ? ((Element)first).getAttribute("drawIconDim") : "10");
		    	int width = Integer.parseInt(((Element)first).hasAttribute("width") ? ((Element)first).getAttribute("width") : "500");
		    	int height = Integer.parseInt(((Element)first).hasAttribute("height") ? ((Element)first).getAttribute("height") : "500");
		    	String name = ((Element)first).hasAttribute("name") ? ((Element)first).getAttribute("name") : "MAP";
		    	Double pixelRepresentsInMeters = Double.parseDouble(((Element)first).hasAttribute("pixelRepresentsInMeters") ? ((Element)first).getAttribute("pixelRepresentsInMeters") : "1.0");
		    	model.setMAP_W(width);
		    	model.setMAP_H(height);
		    	model.setName(name);
		    	model.setPixelRepresentsInMeters(pixelRepresentsInMeters);
		    	
		    	//view.setMapConf(width, height, zoom, drawIconDim, name);
		    	model.setDRAWING_ICON_DIMENSION(drawIconDim);
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
														
							double c1_x = (Double.parseDouble(el.getAttribute("x1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
							double c1_y = (Double.parseDouble(el.getAttribute("y1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
							double c2_x = (Double.parseDouble(el.getAttribute("x2"))*2-model.getDRAWING_ICON_DIMENSION())/2;
							double c2_y = (Double.parseDouble(el.getAttribute("y2"))*2-model.getDRAWING_ICON_DIMENSION())/2;
																
							if(findCorner(roomCorners,c1_x,c1_y) == null) {
								Corner c1 = findCorner(corners,c1_x,c1_y);
								if(c1 != null) {
									roomCorners.add(c1);
									c1.setVertex_label(lastCorner+1);
									lastCorner = c1.getVertex_label();
								}
							}
							if(findCorner(roomCorners,c2_x,c2_y) == null) {
								Corner c2 = findCorner(corners,c2_x,c2_y);
								if(c2 != null) {
									roomCorners.add(c2);
									c2.setVertex_label(lastCorner+1);
									lastCorner = c2.getVertex_label();
								}
							}	
						}
						if(roomCorners.size() == roomWalls.size()) {
							Room room;
							if(roomWalls.get(0).hasAttribute("roomLabel")) {
								/* Añadido por Nacho Palacio 2025-04-21. */
								long roomId = Long.parseLong(roomWalls.get(0).getAttribute("roomLabel"));
								roomId = ElementIdMapper.convertToRangeId(roomId, ElementIdMapper.CATEGORY_ROOM);
								room = new Room(roomId, roomCorners);
							}else {
								room = new Room(model.getNextRoomId(), roomCorners); // Añadido por Nacho Palacio 2025-04-21.
							}
							if(rooms.stream().filter(r -> sameRoom(room,r)).findAny().orElse(null) == null) {
								//System.out.println(room.getCorners());
								if(!model.addRoom(room)) {
									errorList.add("Couldn't add room to map");
								}else {
									rooms.add(room);
								}
							}
						}
						for(Element w : roomWalls) {
							walls.remove(w);
						}
					}else {
						double c1_x = (Double.parseDouble(e.getAttribute("x1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
						double c1_y = (Double.parseDouble(e.getAttribute("y1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
						double c2_x = (Double.parseDouble(e.getAttribute("x2"))*2-model.getDRAWING_ICON_DIMENSION())/2;
						double c2_y = (Double.parseDouble(e.getAttribute("y2"))*2-model.getDRAWING_ICON_DIMENSION())/2;															
						Corner c1 = findCorner(model.getCorners(),c1_x,c1_y);
						if(c1 != null) {											
							model.removeCorner(c1);
							model.eraseDrawable(c1);
						}
						Corner c2 = findCorner(model.getCorners(),c2_x,c2_y);
						if(c2 != null) {
							model.removeCorner(c2);
							model.eraseDrawable(c2);
						}
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
					if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element current = (Element)nodeList.item(i);
						if(current.getNodeName() == "line" && !current.hasAttribute("stroke-dasharray")) lines.add(current);
					}
					i++;
				}
												
//				String[] corner1coord = e.getAttribute("c1").split(",");
//				String[] corner2coord = e.getAttribute("c2").split(",");
//				double c1_x = Double.parseDouble(corner1coord[0]);
//				double c1_y = Double.parseDouble(corner1coord[1]);
//				double c2_x = Double.parseDouble(corner2coord[0]);
//				double c2_y = Double.parseDouble(corner2coord[1]);
								
				double c1_x = (Double.parseDouble(e.getAttribute("x1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
				double c1_y = (Double.parseDouble(e.getAttribute("y1"))*2-model.getDRAWING_ICON_DIMENSION())/2;
				double c2_x = (Double.parseDouble(e.getAttribute("x2"))*2-model.getDRAWING_ICON_DIMENSION())/2;
				double c2_y = (Double.parseDouble(e.getAttribute("y2"))*2-model.getDRAWING_ICON_DIMENSION())/2;
				
				Point p1 = new Point(c1_x,c1_y);
				Point p2 = new Point(c2_x,c2_y);
								
				if(e.hasAttribute("stroke-dasharray")) {
					//potentialRoomSeparators.add(new PointPair(p1,p2));
					
					try {						
//						Room r = model.getRoom(Integer.parseInt(e.getAttribute("room")));
						
						//String[] corner1coord = e.getAttribute("c1").split(",");
						Corner corner1 = findCorner(model.getCorners(), c1_x, c1_y);
						//String[] corner2coord = e.getAttribute("c2").split(",");
						Corner corner2 = findCorner(model.getCorners(), c2_x, c2_y);
						
						Room r;
						if(e.hasAttribute("room")) r = model.getRoom(Integer.parseInt(e.getAttribute("room")));
						else r = corner1.getRoom();
						// Create roomSeparator using corners
						RoomSeparator rs;
//						System.out.println(corner1.getRoom());
//						System.out.println(corner2.getRoom());
//						System.out.println(r);
//						System.out.println();
//						if(r == null) {System.out.println(e.getAttribute("room"));}
						if(e.hasAttribute("separatorLabel")) {
							/* Añadido por Nacho Palacio 2025-04-21. */
							long separatorId = Long.parseLong(e.getAttribute("separatorLabel"));
							separatorId = ElementIdMapper.convertToRangeId(separatorId, ElementIdMapper.CATEGORY_SEPARATOR);
							rs = new RoomSeparator(r, separatorId, corner1, corner2);
						}else {
							rs = new RoomSeparator(r, model.getNextSeparatorId(), corner1, corner2); // Añadido por Nacho Palacio 2025-04-21.
						}
						if(r != null && r.getRoomSeparators().stream().filter(s -> rs.getVertex_label() == s.getVertex_label()).count() == 0) {
//							r.addRoomSeparator(rs);
							model.addRoomSeparator(rs);
						}
//						model.addRoomSeparator(rs);
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
					// corner1 = new Corner(null, e.getAttribute("labelCorner1").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner1")), p1);
					// corner2 = new Corner(null, e.getAttribute("labelCorner2").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner2")), p2);

					// Añadido por Nacho Palacio 2025-04-21.
					long cornerId1 = e.getAttribute("labelCorner1").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner1"));
					long cornerId2 = e.getAttribute("labelCorner2").equals("") ? (0) : Long.parseLong(e.getAttribute("labelCorner2"));
					cornerId1 = ElementIdMapper.convertToRangeId(cornerId1, ElementIdMapper.CATEGORY_CORNER);
					cornerId2 = ElementIdMapper.convertToRangeId(cornerId2, ElementIdMapper.CATEGORY_CORNER);
					corner1 = new Corner(null, cornerId1, p1);
					corner2 = new Corner(null, cornerId2, p2);
					//if(e.getAttribute("roomLabel").equals("81")) System.out.println(e.getAttribute("labelCorner1")+" "+e.getAttribute("c1")+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("c2"));
					if(!wallCollidesCorners(corner1,corner2,wallsPoints)) {
						wallsPoints.add(new PointPair(p1,p2));
						//if(e.getAttribute("roomLabel").equals("81")) System.out.println(corner1.getVertex_label()+" "+e.getAttribute("labelCorner1")+" "+e.getAttribute("x1")+" "+e.getAttribute("y1")); 
						if (findCorner(corners,c1_x,c1_y) == null && model.addCorner(corner1)) {
//							corner1.setVertex_label((corner1.getVertex_label() == lastCorner) ? (lastCorner+1) : Long.parseLong(e.getAttribute("labelCorner1")));
//							lastCorner = corner1.getVertex_label();
							corners.add(corner1);
							//if(e.getAttribute("roomLabel").equals("1")) System.out.println("in "+findCorner(model.getCorners(),c1_x,c1_y).getVertex_label()+" "+e.getAttribute("labelCorner1")+lastCorner+" "+e.getAttribute("c1")); 
						}else {
							errorList.add("COULDN'T ADD CORNER TO MAP");
						}
						//if(e.getAttribute("roomLabel").equals("81")) System.out.println(corner2.getVertex_label()+" "+e.getAttribute("labelCorner2")+" "+e.getAttribute("x2")+" "+e.getAttribute("y2")); 
						if (findCorner(corners,c2_x,c2_y) == null && model.addCorner(corner2)) {
//							corner2.setVertex_label((corner2.getVertex_label() == lastCorner) ? (lastCorner+1) : Long.parseLong(e.getAttribute("labelCorner2")));
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
					Room doorRoom = model.getRooms().stream().filter(r -> r.getPolygon().contains(Double.parseDouble(e.getAttribute("x")), Double.parseDouble(e.getAttribute("y")))).findAny().orElse(null);
					if(doorRoom == null && e.hasAttribute("room")) doorRoom = model.getRooms().stream().filter(r -> r.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
					if(e.hasAttribute("label")) {
						// d = new Door(doorRoom, Long.parseLong(e.getAttribute("label")), new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
						
						// Añadido por Nacho Palacio 2025-04-21.
						long doorId = Long.parseLong(e.getAttribute("label"));
						doorId = ElementIdMapper.convertToRangeId(doorId, ElementIdMapper.CATEGORY_DOOR);
						d = new Door(doorRoom, doorId, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}else {
						d = new Door(doorRoom, model.getNextDoorId(), new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y")))); // Añadido por Nacho Palacio 2025-04-21.
					}
					//Door d = new Door(null, model.getNumDoors()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					if(!model.addDoor(d)) {
						errorList.add("COULDN'T ADD DOOR TO MAP");
					}else {
						connectables.add(e);
					}
				}else if(path.endsWith("\\stairs.png")) {
					Stairs stairs;
					Room stairsRoom = model.getRooms().stream().filter(r -> r.getPolygon().contains(Double.parseDouble(e.getAttribute("x")), Double.parseDouble(e.getAttribute("y")))).findAny().orElse(null);
					if(stairsRoom == null && e.hasAttribute("room")) stairsRoom = model.getRooms().stream().filter(r -> r.getLabel() == Integer.parseInt(e.getAttribute("room"))).findAny().orElse(null);
					if(e.hasAttribute("label")) {
						// Añadido por Nacho Palacio 2025-04-21.
						long stairsId = Long.parseLong(e.getAttribute("label"));
						stairsId = ElementIdMapper.convertToRangeId(stairsId, ElementIdMapper.CATEGORY_STAIRS);
						stairs = new Stairs(stairsRoom, stairsId, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					}else {
						stairs = new Stairs(stairsRoom, model.getNextStairsId(), new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y")))); // Añadido por Nacho Palacio 2025-04-21.
					}
					//Stairs stairs = new Stairs(null, model.getNumStairs()+1, new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y"))));
					if (!model.addStairs(stairs)) {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD STAIRS TO MAP");
					}else {
						connectables.add(e);
					}
				}else if(!path.endsWith("\\corner.png")){
															
//					Map<String, Properties> buttons = model.getVisitableObjects();
//					//System.out.println(buttons);
//					visitable = "";
//					
//					for (Map.Entry<String, Properties> entry : buttons.entrySet()) {
//						//System.out.println(entry);
//						entry.getValue().forEach((k, v) -> {
//							//System.out.println(k+" "+v+" "+e.getAttribute("href")+" "+(v.toString().equals(e.getAttribute("href"))));
//				            if(k.toString().startsWith(Literals.VERTEX_URL) && v.toString().equals(e.getAttribute("href"))) {
//				            	visitable = entry.getKey();
//				            	//System.out.println(k+" "+v);
//				            }				            
//				        });						
//					}
//						
//					if(!visitable.isEmpty()) {
//						Point p = new Point(Double.parseDouble(e.getAttribute("x")),Double.parseDouble(e.getAttribute("y")));					
//						Item itemVisitable;
//						if(e.hasAttribute("label")) {
//							itemVisitable = new Item(null, Long.parseLong(e.getAttribute("label")), p);
//						}else {
//							itemVisitable = new Item(null, model.getNumItems()+1, p);
//						}
//						//Item itemVisitable = new Item(null, model.getNumItems()+1, p);
//						//System.out.println("hola");
//						Room r = model.isInsideRoom(p, -1);
//						if (r != null) {
//							//System.out.println("hola2");
//							itemVisitable.setRoom(r);
//				
//							// Set specific item's properties (visitableItem's attributes).
//							model.setItemVisitableProperties(visitable, itemVisitable);
//							
//							if (!model.addItem(itemVisitable)) {
//								errorList.add("COULDN'T ADD ITEM TO MAP");
//							}
//						}
//						else {
//							errorList.add("ITEM MUST BE INSIDE A ROOM");
//						}
//					}else {
//						errorList.add("VISITABLE ITEMS MUST BE ADDED");
//					}
					double itemX = Double.parseDouble(e.getAttribute("x"));
					double itemY = Double.parseDouble(e.getAttribute("y"));
					Room r = (e.hasAttribute("room")) ? model.getRoom(Integer.parseInt(e.getAttribute("room"))) : model.getRooms().stream().filter(room -> room.getPolygon().contains(itemX, itemY)).findAny().orElse(null);
					// Main attributes
					// Get item room
					if(r != null) {
//						int roomLabel = Integer.parseInt(e.getAttribute("room"));
						//roomLabel += model.getNumRooms();
//						r = (e.hasAttribute("room")) ? model.getRoom(Integer.parseInt(e.getAttribute("room"))) : r;
						// Get item label

						// Añadido por Nacho Palacio 2025-04-21.
						long vertexLabel;
						if(e.hasAttribute("label")) {
							vertexLabel = Long.parseLong(e.getAttribute("label"));
							// Convertir al rango correcto para Item
							vertexLabel = ElementIdMapper.convertToRangeId(vertexLabel, ElementIdMapper.CATEGORY_ITEM);
						} else {
							vertexLabel = model.getNextItemId();
						}
						//vertexLabel += model.getNumItems();
						// Get item coordinates
//						double itemX = Double.parseDouble(e.getAttribute("x"));
//						double itemY = Double.parseDouble(e.getAttribute("y"));
						
						// CREATE ITEM AND ADD IT TO MODEL
						Item it = new Item(r, vertexLabel, new Point(itemX, itemY));
						//System.out.println(roomLabel+" "+e.getAttribute("href"));
						
						model.addItem(it);
						
						// Item attributes
						// Get item label (located in vertexLabel field) and assign it to item
						String attribute = e.getAttribute("itemLabel");
						if (attribute == null || attribute.isEmpty())
							System.out.println("Item doesn't have ITEM LABEL"); // Throw exception
						else
							it.setItemLabel(attribute);
						
						// Get item url and assign it to item
						attribute = path;
						if (attribute == null || attribute.isEmpty())
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
																						
			default:
//				if(visitable == "1842") {
//					System.out.println("1842 switch");
//				}
		}
		
//		if(visitable == "1842") {
//			System.out.println("1842 termina");
//		}
		
		
	}
	
	
	
	List<Element> sortCorners(List<Element> listWalls){
		int i = 0;
		for(Element e : listWalls) {
			double w1_c1_x = Double.parseDouble(e.getAttribute("x1"));
			double w1_c1_y = Double.parseDouble(e.getAttribute("y1"));
			double w1_c2_x = Double.parseDouble(e.getAttribute("x2"));
			double w1_c2_y = Double.parseDouble(e.getAttribute("y2"));			
			if(i < listWalls.size()-1) {
				Element e2 = listWalls.get(i+1);
				double w2_c1_x = Double.parseDouble(e2.getAttribute("x1"));
				double w2_c1_y = Double.parseDouble(e2.getAttribute("y1"));
				double w2_c2_x = Double.parseDouble(e2.getAttribute("x2"));
				double w2_c2_y = Double.parseDouble(e2.getAttribute("y2"));
				if(w1_c1_x == w2_c1_x && w1_c1_y == w2_c1_y) {
//				if(e.getAttribute("c1").equals(e2.getAttribute("c1"))) {
					e.setAttribute("x1", Double.toString(w1_c2_x));
					e.setAttribute("y1", Double.toString(w1_c2_y));
					e.setAttribute("x2", Double.toString(w1_c1_x));
					e.setAttribute("y2", Double.toString(w1_c1_y));					
//					String c1 = e.getAttribute("c1");
//					e.setAttribute("c1", e.getAttribute("c2"));
//					e.setAttribute("c2", c1);					
				}else if(w1_c2_x == w2_c2_x && w1_c2_y == w2_c2_y) {
//				}else if(e.getAttribute("c2").equals(e2.getAttribute("c2"))) {	
					e2.setAttribute("x1", Double.toString(w2_c2_x));
					e2.setAttribute("y1", Double.toString(w2_c2_y));
					e2.setAttribute("x2", Double.toString(w2_c1_x));
					e2.setAttribute("y2", Double.toString(w2_c1_y));
//					String c1 = e2.getAttribute("c1");
//					e2.setAttribute("c1", e2.getAttribute("c2"));
//					e2.setAttribute("c2", c1);
				}else if(w1_c1_x == w2_c2_x && w1_c1_y == w2_c2_y) {
//				}else if(e.getAttribute("c1").equals(e2.getAttribute("c2"))) {	
					e.setAttribute("x1", Double.toString(w1_c2_x));
					e.setAttribute("y1", Double.toString(w1_c2_y));
					e.setAttribute("x2", Double.toString(w1_c1_x));
					e.setAttribute("y2", Double.toString(w1_c1_y));
					e2.setAttribute("x1", Double.toString(w2_c2_x));
					e2.setAttribute("y1", Double.toString(w2_c2_y));
					e2.setAttribute("x2", Double.toString(w2_c1_x));
					e2.setAttribute("y2", Double.toString(w2_c1_y));					
//					String c1 = e.getAttribute("c1");
//					e.setAttribute("c1", e.getAttribute("c2"));
//					e.setAttribute("c2", c1);
//					c1 = e2.getAttribute("c1");
//					e2.setAttribute("c1", e2.getAttribute("c2"));
//					e2.setAttribute("c2", c1);
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
			Door d = model.getDoors().stream().filter(door -> door.getVertex_xy().getX() == Double.parseDouble(e.getAttribute("x")) && door.getVertex_xy().getY() == Double.parseDouble(e.getAttribute("y"))).findAny().orElse(null);
			if(d != null) {
				if(e.hasAttribute("numConnectedDoors") && Integer.parseInt(e.getAttribute("numConnectedDoors")) > 0) {
					int numConnectedDoors = Integer.parseInt(e.getAttribute("numConnectedDoors"));			
					for(int i = 1; i <= numConnectedDoors; i++) {
						
						String connectedDoor = e.getAttribute("connectedDoor_"+i);
									
						// door_"numDoorInRoom"_"numRoom"
						String[] arrayDoor = connectedDoor.split("_");
						long labelDoor = Long.valueOf(arrayDoor[0]);
						int labelRoom = Integer.valueOf(arrayDoor[1]);

						// Añadido por Nacho Palacio 2025-04-21.
						labelDoor = ElementIdMapper.convertToRangeId(labelDoor, ElementIdMapper.CATEGORY_DOOR);
						long roomId = ElementIdMapper.convertToRangeId(labelRoom, ElementIdMapper.CATEGORY_ROOM);
										
						//Room room = model.getRooms().stream().filter(r -> r.getLabel() == labelRoom).findAny().orElse(null);
						// Room room = model.getRoom(labelRoom);
						Room room = model.getRoom((int)roomId);
						if(room != null) {
							Door d2 = room.getDoor(labelDoor);
							// Add connection (connectTo creates a connection for both doors)
							d.connectTo(d2);
						}				
					}
				}
							
				if(e.hasAttribute("numConnectedStairs") && Integer.parseInt(e.getAttribute("numConnectedStairs")) > 0) {
					int numConnectedStairs = Integer.parseInt(e.getAttribute("numConnectedStairs"));
					for(int i = 1; i <= numConnectedStairs; i++) {								
						String connectedStairs = e.getAttribute("connectedStairs_"+i);
						long label = Long.parseLong(connectedStairs);

						label = ElementIdMapper.convertToRangeId(label, ElementIdMapper.CATEGORY_STAIRS); // Añadido por Nacho Palacio 2025-04-21.
									
						Stairs s = model.getStairs(label);
//						d.connectTo(s);
//						s.connectTo(d);
						d.addConnection(s);
					}
				}
			}
		}else if(path.endsWith("\\stairs.png")) {
			if(e.hasAttribute("numConnectedStairs") && Integer.parseInt(e.getAttribute("numConnectedStairs")) > 0) {
				int numConnectedStairs = Integer.parseInt(e.getAttribute("numConnectedStairs"));			
				Stairs s = model.getStairs().stream().filter(stairs -> stairs.getVertex_xy().getX() == Double.parseDouble(e.getAttribute("x")) && stairs.getVertex_xy().getY() == Double.parseDouble(e.getAttribute("y"))).findAny().orElse(null);
				if(s != null) {
					for(int i = 1; i <= numConnectedStairs; i++) {
										
						String connectedStairs = e.getAttribute("connectedStairs_"+i);
						long label = Long.parseLong(connectedStairs);

						label = ElementIdMapper.convertToRangeId(label, ElementIdMapper.CATEGORY_STAIRS); // Añadido por Nacho Palacio 2025-04-21.
									
						Stairs s2 = model.getStairs(label);
						s.connectTo(s2);
					}
				}
			}
		}
											
	}

}
