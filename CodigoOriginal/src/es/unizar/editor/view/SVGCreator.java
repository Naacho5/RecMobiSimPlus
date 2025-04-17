package es.unizar.editor.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.ImageIcon;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomPolygon;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

public class SVGCreator{

    MapEditorModel model;

    DOMImplementation impl;
    String svgNS;
    Document doc;
    Element svgRoot;
    int indexItems = 1;

    public SVGCreator(MapEditorModel m){
        model = m;
        impl = SVGDOMImplementation.getDOMImplementation();
        svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        doc = impl.createDocument(svgNS, "svg", null);

        // Get the root element (the 'svg' element).
        svgRoot = doc.getDocumentElement();
    }

    public void crear(String path){

        // Set the width and height attributes on the root 'svg' element.
        svgRoot.setAttributeNS(null, "width", Integer.toString(model.getMAP_W()));
        svgRoot.setAttributeNS(null, "height", Integer.toString(model.getMAP_H()));
        
//        svgRoot.setAttribute("zoom", Double.toString(model.getZOOM()));
        svgRoot.setAttribute("drawIconDim", Integer.toString(model.getDRAWING_ICON_DIMENSION()));
        svgRoot.setAttribute("name", model.getName());
        svgRoot.setAttribute("pixelRepresentsInMeters", Double.toString(model.getPixelRepresentsInMeters()));
        
        for (Drawable d: model.getPaintedElements()) {        	
			// -- ELEMENTS --
			ImageIcon icon = new ImageIcon(d.getUrlIcon());
			if (d instanceof Door && (((Door) d).getRoom() == null || ((Door) d).getConnectedTo().isEmpty())){
				Element im = doc.createElementNS(svgNS, "image");
				im.setAttributeNS(null, "href", d.getUrlIcon());
				im.setAttributeNS(null, "x", Integer.toString((int) (d.getVertex_xy().getX())));
				im.setAttributeNS(null, "y", Integer.toString((int) (d.getVertex_xy().getY())));
				im.setAttributeNS(null, "width", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				im.setAttributeNS(null, "height", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				im.setAttributeNS(null, "style", "background-color:red");
				im.setAttributeNS(null, "label", Integer.toString((int) d.getVertex_label()));
				svgRoot.appendChild(im);
			}
			else if (d instanceof RoomSeparator) {
				//g.setColor(Color.BLUE);
				
				RoomSeparator rs = (RoomSeparator) d;
				
//				g.fillOval((int) (rs.getC1().getVertex_xy().getX()), (int) (rs.getC1().getVertex_xy().getY()),
//						(int) (model.getDRAWING_ICON_DIMENSION()), (int) (model.getDRAWING_ICON_DIMENSION()));
				
				Element el = doc.createElementNS(svgNS, "ellipse");
				el.setAttributeNS(null, "x", Integer.toString((int) (rs.getC1().getVertex_xy().getX())));
				el.setAttributeNS(null, "y", Integer.toString((int) (rs.getC1().getVertex_xy().getY())));
				el.setAttributeNS(null, "width", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				el.setAttributeNS(null, "height", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				el.setAttributeNS(null, "style", "stroke:blue");
				svgRoot.appendChild(el);
				
//				g.fillOval((int) (rs.getC2().getVertex_xy().getX()), (int) (rs.getC2().getVertex_xy().getY()),
//						(int) (model.getDRAWING_ICON_DIMENSION()), (int) (model.getDRAWING_ICON_DIMENSION()));
				
				Element el2 = doc.createElementNS(svgNS, "ellipse");
				el2.setAttributeNS(null, "x", Integer.toString((int) (rs.getC2().getVertex_xy().getX())));
				el2.setAttributeNS(null, "y", Integer.toString((int) (rs.getC2().getVertex_xy().getY())));
				el2.setAttributeNS(null, "width", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				el2.setAttributeNS(null, "height", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				el2.setAttributeNS(null, "style", "stroke:blue");
				svgRoot.appendChild(el2);
				
				drawSubroomSeparator(rs);
				
				//g.setColor(Color.BLACK);
			}
			else {
//				g.drawImage(icon.getImage(), (int) (d.getVertex_xy().getX()), (int) (d.getVertex_xy().getY()),
//						(int) (model.getDRAWING_ICON_DIMENSION()), (int) (model.getDRAWING_ICON_DIMENSION()), this);
				
				Element im = doc.createElementNS(svgNS, "image");
				im.setAttributeNS(null, "href", d.getUrlIcon());
				im.setAttributeNS(null, "x", Integer.toString((int) (d.getVertex_xy().getX())));
				im.setAttributeNS(null, "y", Integer.toString((int) (d.getVertex_xy().getY())));
				im.setAttributeNS(null, "width", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				im.setAttributeNS(null, "height", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION())));
				im.setAttributeNS(null, "label", Integer.toString((int) d.getVertex_label()));
				
				if (d instanceof Item) {
					Item i = (Item)d;
					im.setAttributeNS(null, "itemWidth", Double.toString(i.getWidth()));
					im.setAttributeNS(null, "itemHeight", Double.toString(i.getHeight()));
					im.setAttributeNS(null, "title", i.getTitle());
					im.setAttributeNS(null, "nationality", i.getNationality());
					im.setAttributeNS(null, "beginDate", i.getBeginDate());
					im.setAttributeNS(null, "endDate", i.getEndDate());
					im.setAttributeNS(null, "date", i.getDate());
					// im.setAttributeNS(null, "room", Integer.toString(i.getRoom().getLabel()));
					im.setAttributeNS(null, "room", Long.toString(i.getRoom().getLabel())); // Añadido por Nacho Palacio 2025-04-17
					im.setAttributeNS(null, "itemLabel", i.getItemLabel());
					im.setAttributeNS(null, Literals.ITEM_ID, Integer.toString(indexItems));
					indexItems++;
				}
				
				if (d instanceof Door) {
					Door door = (Door)d;
					// im.setAttributeNS(null, "room", Integer.toString(door.getRoom().getLabel()));
					im.setAttributeNS(null, "room", Long.toString(door.getRoom().getLabel())); // Añadido por Nacho Palacio 2025-04-17

					List<Door> connectedDoors = new ArrayList<Door>();
					List<Stairs> connectedStairs = new ArrayList<Stairs>();
					for (Connectable c: door.getConnectedTo()) {
						// If both elements aren't null
						if (door != null && c != null) {
							// Check type
							if (c instanceof Door) { // If it's a door, add it to connectedDoors
								Door other = (Door) c;	
								// If it doesn't exist yet, add pair
								if(!connectedDoors.contains(other)){
									connectedDoors.add(other);
								}
							}
							else if (c instanceof Stairs){ // If it's stairs, add it to connectedDoorStairs
								Stairs other = (Stairs) c;										
								if(!connectedStairs.contains(other))
									connectedStairs.add(other);
							}
						}
					}
					im.setAttributeNS(null, "numConnectedDoors", Integer.toString(connectedDoors.size()));
					int pos = 1;
					for(Door door2: connectedDoors) {						
//						int roomDoorF = model.getRooms().indexOf(door2.getRoom());
//						int doorF = model.getRooms().get(roomDoorF).getDoors().indexOf(door2) + 1;						
//						roomDoorF += 1;
						
						// int roomDoorF = door2.getRoom().getLabel();
						long roomDoorF = door2.getRoom().getLabel(); // 

						long doorF = door2.getVertex_label();
						
						// connectedDoor_+pos=numDoorInRoom_numRoom
						// im.setAttributeNS(null, "connectedDoor_"+pos, Long.toString(doorF)+"_"+Integer.toString(roomDoorF));
						im.setAttributeNS(null, "connectedDoor_"+pos, Long.toString(doorF)+"_"+Long.toString(roomDoorF)); // Añadido por Nacho Palacio 2025-04-17

						//im.setAttributeNS(null, "connectedDoor_"+pos+"_room", Integer.toString(roomDoorF));
												
						pos++;
					}
					
					
					im.setAttributeNS(null, "numConnectedStairs", Integer.toString(connectedStairs.size()));
					
					int roomDoor = model.getRooms().indexOf(door.getRoom());
					int numDoor = model.getRooms().get(roomDoor).getDoors().indexOf(door) + 1;
					im.setAttributeNS(null, "numDoorInRoom", Integer.toString(numDoor));
					
					pos = 1;
					for(Stairs connStairs: connectedStairs) {						
						//int roomDoor = model.getRooms().indexOf(door.getRoom());
						//int stairs = model.getStairs().indexOf(connStairs) + 1;
						//int numDoor = model.getRooms().get(roomDoor).getDoors().indexOf(door) + 1;
						long stairs = connStairs.getVertex_label();
										
						//roomDoor += 1;
						
						im.setAttributeNS(null, "connectedStairs_"+pos, Long.toString(stairs));												
						pos++;
					}
					
				}
				
				
				if (d instanceof Stairs) {											
					Stairs currentStairs = (Stairs)d;
					// Set stairs locations
					// if(currentStairs.getRoom() != null) im.setAttributeNS(null, "room", Integer.toString(currentStairs.getRoom().getLabel()));
					if(currentStairs.getRoom() != null) im.setAttributeNS(null, "room", Long.toString(currentStairs.getRoom().getLabel())); // Añadido por Nacho Palacio 2025-04-17

										
					int pos = 1;
					for (Connectable c: currentStairs.getConnectedTo()) { // There will be only 1 iteration (only connected to 1 stairs)						
						if(c instanceof Stairs) {
							Stairs connStairs = (Stairs) c;
							if (currentStairs != null && connStairs != null) {
								long stairs = connStairs.getVertex_label();							
								im.setAttributeNS(null, "connectedStairs_"+pos, Long.toString(stairs));												
								pos++;
							}
						}
					}
					
					im.setAttributeNS(null, "numConnectedStairs", Integer.toString(pos-1));
				}
				
				svgRoot.appendChild(im);
			}
			// -- END ELEMENTS --
			
			// -- IDS --
			if (d instanceof Door) {
				//g.setColor(Color.GREEN);
				//g.drawString(Integer.toString((int) d.getVertex_label() + model.getNumItems()), (int) (d.getVertex_xy().getX()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2)));
				
				Element txt = doc.createElementNS(svgNS, "text");
				txt.setTextContent(Integer.toString((int) d.getVertex_label() /*+ model.getNumItems()*/));
				txt.setAttributeNS(null, "x", Integer.toString((int) (d.getVertex_xy().getX())));
				txt.setAttributeNS(null, "y", Integer.toString((int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2))));
				txt.setAttributeNS(null, "style", "stroke:green");
				svgRoot.appendChild(txt);
				
				//g.setColor(Color.BLACK);
			}
			
			if (d instanceof Item) {
//				g.setColor(Color.RED);
//				g.drawString(Integer.toString((int) d.getVertex_label()), (int) (d.getVertex_xy().getX()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2)));
//				g.setColor(Color.BLACK);
				
				Element txt = doc.createElementNS(svgNS, "text");
				txt.setTextContent(Integer.toString((int) d.getVertex_label()));
				txt.setAttributeNS(null, "x", Integer.toString((int) (d.getVertex_xy().getX())));
				txt.setAttributeNS(null, "y", Integer.toString((int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2))));
				txt.setAttributeNS(null, "style", "stroke:red");
				//System.out.println(((Item)d).getWidth());
				svgRoot.appendChild(txt);
			}
			// -- END IDS --
		}
        
        for (Room r: model.getRooms()) {
			drawWalls(new LinkedList<>(r.getCorners()), true, r.getLabel());
			
			// Draw room label in first room corner
			Point roomPoint = r.getCorners().get(0).getVertex_xy();
			//g.drawString(Integer.toString(r.getLabel()), (int) (roomPoint.getX()), (int) ((roomPoint.getY() -1)));
			
			Element txt = doc.createElementNS(svgNS, "text");
			// txt.setTextContent(Integer.toString(r.getLabel()));
			txt.setTextContent(Long.toString(r.getLabel())); // Añadido por Nacho Palacio 2025-04-17

			txt.setAttributeNS(null, "x", Integer.toString((int) (roomPoint.getX())));
			txt.setAttributeNS(null, "y", Integer.toString((int) ((roomPoint.getY() -1))));
			txt.setAttributeNS(null, "style", "stroke:black");
			svgRoot.appendChild(txt);
			
			// Draw subrooms
			//g.setColor(Color.YELLOW);
			int subroom = 1;
			for(RoomPolygon rp: r.getSubRooms()) {
				//Polygon poly = new Polygon();
				String s = "";
				for (int point = 0; point < rp.getPolygon().npoints; point++) {
//					poly.addPoint((int) ((rp.getPolygon().xpoints[point] + (model.getDRAWING_ICON_DIMENSION()/2))),
//							(int) ((rp.getPolygon().ypoints[point] + (model.getDRAWING_ICON_DIMENSION()/2))));
					
					s = s + Integer.toString((int) ((rp.getPolygon().xpoints[point] + (model.getDRAWING_ICON_DIMENSION()/2))));
					s = s + ",";
					s = s + Integer.toString((int) ((rp.getPolygon().ypoints[point] + (model.getDRAWING_ICON_DIMENSION()/2))));
					if(point < rp.getPolygon().npoints-1) s = s + " ";
				}
				//g.drawPolygon(poly);
				
				Element pol = doc.createElementNS(svgNS, "polygon");
				pol.setAttributeNS(null, "points", s);
				pol.setAttributeNS(null, "style", "stroke:yellow;fill-opacity:0");
				// pol.setAttributeNS(null, "room", Integer.toString(r.getLabel()));
				pol.setAttributeNS(null, "room", Long.toString(r.getLabel())); // Añadido por Nacho Palacio 2025-04-17
				pol.setAttributeNS(null, "numSubroom", Integer.toString(subroom));
				subroom++;
												
				svgRoot.appendChild(pol);
				
			}
			//g.setColor(Color.BLACK);
		}
        
        SVGGraphics2D svgGenerator = new SVGGraphics2D(doc);
        
        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes
        try {
			FileOutputStream f = new FileOutputStream(path + File.separator + "map.svg");
			OutputStreamWriter out = new OutputStreamWriter(f, "UTF-8");
	        svgGenerator.stream(svgRoot, out);
//	        System.out.println("SVG creado");
	        f.close();
	        
	        File file = new File(path + File.separator + "map.svg");
	        String data = "";
	        Scanner scanner = new Scanner(file);
	        while (scanner.hasNextLine()) {
	          data += scanner.nextLine().replace(" PUBLIC '-//W3C//DTD SVG 1.0//EN'", "").replace("          'http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd'", "");
	          data += System.lineSeparator();
	        }
	        scanner.close();
	        
	        FileWriter myWriter = new FileWriter(file);
	        myWriter.write(data);
	        myWriter.close();
		} catch (FileNotFoundException | UnsupportedEncodingException | SVGGraphics2DIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    }

    /**
	 * Draws the walls between the list of corners. If it's a completed room, it also draws a wall between the first and the last corner.
	 * 
	 * @param corners			List of corners in room (completed or being drawn)
	 * @param isCompletedRoom	If it's a completed room, draw last wall
	 */
	private void drawWalls(List<Corner> corners, boolean isCompletedRoom, long label) { // Modificado por Nacho Palacio 2025-04-17 (intlabel -> long label)
		Corner previous = null;
		
		for (Corner c: corners) {
			if (c == corners.get(0)) { // If first element
				previous = c;
			}
			else {
				drawWallBetweenCorners(previous, c, label);
				
				previous = c;
				
				// If it's the last corner and it's a completed room
				if (c == corners.get(corners.size()-1) && isCompletedRoom) {
					// Draw wall between last and first corner
					drawWallBetweenCorners(c, corners.get(0), label);
				}
			}
		}
		
	}

    /**
	 * Draws a line that represents a wall between corners
	 * 
	 * @param c1	First corner
	 * @param c2	Second corner
	 */
	private void drawWallBetweenCorners(Corner c1, Corner c2, long label) { // Modificado por Nacho Palacio 2025-04-17 (intlabel -> long label)
		// Calculate c1 and c2 positions
//		double previousCornerX = c1.getVertex_xy().getX();
//		double previousCornerY = c1.getVertex_xy().getY();
//		double currentCornerX = c2.getVertex_xy().getX();
//		double currentCornerY = c2.getVertex_xy().getY();
//		
//		double cornerLimit = model.getDRAWING_ICON_DIMENSION();
		        
        Element line = doc.createElementNS(svgNS, "line");
        line.setAttributeNS(null, "x1", Double.toString((2*c1.getVertex_xy().getX()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "y1", Double.toString((2*c1.getVertex_xy().getY()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "x2", Double.toString((2*c2.getVertex_xy().getX()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "y2", Double.toString((2*c2.getVertex_xy().getY()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "style", "stroke:rgb(0,0,0);stroke-width:1");
        // line.setAttributeNS(null, "roomLabel", Integer.toString(label));
		line.setAttributeNS(null, "roomLabel", Long.toString(label)); // Añadido por Nacho Palacio 2025-04-17
        line.setAttributeNS(null, "labelCorner1", Long.toString(c1.getVertex_label()));
        line.setAttributeNS(null, "labelCorner2", Long.toString(c2.getVertex_label()));
//        line.setAttributeNS(null, "c1", c1.getVertex_xy().toString());
//        line.setAttributeNS(null, "c2", c2.getVertex_xy().toString());

		svgRoot.appendChild(line);
	}
	
	
	/**
	 * Draw dashed line between RoomSeparator points that represents the limit between subrooms.
	 * 
	 * @param g		Graphics
	 * @param rs	RoomSeparator
	 */
	private void drawSubroomSeparator(RoomSeparator rs) {
		
		// DASHED LINE: https://stackoverflow.com/questions/21989082/drawing-dashed-line-in-java
		// Create a copy of the Graphics instance
//		Graphics2D newGraphics = (Graphics2D) g.create();
//		
//		// Set the stroke of the copy, not the original 
//		Stroke dashedLine = new BasicStroke((int) (model.getDRAWING_ICON_DIMENSION()/2*model.getZOOM()), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
//		                                  0, new float[]{9}, 0);
//		newGraphics.setStroke(dashedLine);
				  
		// Calculate c1 and c2 positions
//		double firstCornerX = rs.getC1().getVertex_xy().getX();
//		double firstCornerY = rs.getC1().getVertex_xy().getY();
//		double secondCornerX = rs.getC2().getVertex_xy().getX();
//		double secondCornerY = rs.getC2().getVertex_xy().getY();
//		
//		double cornerLimit = model.getDRAWING_ICON_DIMENSION();
		
//		newGraphics.drawLine(
//				(int) (2*firstCornerX+cornerLimit)/2,
//				(int) (2*firstCornerY+cornerLimit)/2,
//				(int) (2*secondCornerX+cornerLimit)/2,
//				(int) (2*secondCornerY+cornerLimit)/2);
		
		Element line = doc.createElementNS(svgNS, "line");
        line.setAttributeNS(null, "stroke-width", Integer.toString((int) (model.getDRAWING_ICON_DIMENSION()/2*model.getZOOM())));
        line.setAttributeNS(null, "stroke-linecap", Integer.toString(BasicStroke.CAP_BUTT));
        line.setAttributeNS(null, "stroke-linejoin", Integer.toString(BasicStroke.JOIN_BEVEL));
        line.setAttributeNS(null, "stroke-miterlimit", "0");
        line.setAttributeNS(null, "stroke-dasharray", "9");
        line.setAttributeNS(null, "stroke-dashoffset", "0");
        line.setAttributeNS(null, "x1", Double.toString((2*rs.getC1().getVertex_xy().getX()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "y1", Double.toString((2*rs.getC1().getVertex_xy().getY()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "x2", Double.toString((2*rs.getC2().getVertex_xy().getX()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "y2", Double.toString((2*rs.getC2().getVertex_xy().getY()+model.getDRAWING_ICON_DIMENSION())/2));
        line.setAttributeNS(null, "style", "stroke:blue");
        line.setAttributeNS(null, "separatorLabel", Long.toString(rs.getVertex_label()));
        // line.setAttributeNS(null, "room", Integer.toString(rs.getRoom().getLabel()));
        line.setAttributeNS(null, "room", Long.toString(rs.getRoom().getLabel())); // Añadido por Nacho Palacio 2025-04-17

//        line.setAttributeNS(null, "c1", rs.getC1().getVertex_xy().toString());
//        line.setAttributeNS(null, "c2", rs.getC2().getVertex_xy().toString());
        svgRoot.appendChild(line);
		
		// Get rid of the copy
		  //newGraphics.dispose();
	}
}