package es.unizar.editor.model;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.editor.view.SVGCreator;
import es.unizar.util.EditorLiterals;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

/**
 * Editor's model.
 *
 * @author Alejandro Piedrafita Barrantes
 */
public class MapEditorModel {
	
	public static int MIN_MAP_W = 20;
	public static int MIN_MAP_H = 20;
	private int MAP_W = MIN_MAP_W;
	private int MAP_H = MIN_MAP_H;
	private double ZOOM = 1.0;
	private double pixelRepresentsInMeters;
	
	private List<Room> rooms;
	private List<Corner> corners;
	private List<Item> items;
	private List<Door> doors;
	private List<Stairs> stairs;
	private List<RoomSeparator> roomSeparators;
	
	private String name; // Name of the whole scenario -> Different floors of the same scenario will have the same name
	private String floor; // Number/name of floor in scenario
	private int DRAWING_ICON_DIMENSION = 10; // Size of the icons DRAWN IN MAP PANEL
	private Point currentCursorPoint; // Current point
	
	// All elements that have been painted in the MapPanel
	private List<Drawable> paintedElements;
	// Element of the paintedElements that has been selected currently
	private Drawable objectSelected;
	// New position from object being moved
	private Point oldObjectCoordinates;
	public Point diffPoint;
	
	// Current corners created (painted) before closing/creating a room;
	private LinkedList<Corner> currentRoomCorners; // For corners to be added in order of insertion;
	// Current RoomSeparator line before setting end point.
	private Corner currentRoomSeparatorCorner;

	private ElementIdMapper idMapper; // Añadido por Nacho Palacio 2025-04-18
	
	/**
	 * DataAccess
	 */
	public DataAccessGraphFile accessGraphFile;
	public DataAccessItemFile accessItemFile;
	public DataAccessRoomFile accessRoomFile;
	
	/**
	 * Tools
	 */
	public enum ToolButtons {
		none, pencil, eraser, cursor, mover
	}
	
	private ToolButtons toolClicked = ToolButtons.none;
	
	/**
	 * Icons
	 */
	public enum IconButtons {
		none, corner, door, stairs, roomSeparator, visitable
	}
	
	private IconButtons selected;
	
	/**
	 * Map which contains the different type of visitable objects
	 */
	private Map<String, Properties> visitableObjects;
	private String visitableSelected = "";
	
	//----------------------------------------------------------

	/**
	 * Model Constructor. Initialize all elements.
	 */
	public MapEditorModel() {
		super();
		
		rooms = new LinkedList<Room>();
		corners = new LinkedList<Corner>();
		items = new LinkedList<Item>();
		doors = new LinkedList<Door>();
		stairs = new LinkedList<Stairs>();
		roomSeparators = new LinkedList<RoomSeparator>();
		
		name = EditorLiterals.DEFAULT_MAP_NAME;
		floor = EditorLiterals.DEFAULT_FLOOR_NAME;
		currentCursorPoint = new Point(0,0);
		
		this.accessGraphFile = new DataAccessGraphFile(null);
		this.accessItemFile = new DataAccessItemFile(null);
		this.accessRoomFile = new DataAccessRoomFile(null);
		this.idMapper = new ElementIdMapper(); // Añadido por Nacho Palacio 2025-04-18
		
		selected = IconButtons.none;
		paintedElements = new LinkedList<Drawable>();
		objectSelected = null;
		oldObjectCoordinates = null;
		
		currentRoomCorners = new LinkedList<Corner>();
		
		toolClicked = ToolButtons.none;
		
		visitableObjects = new HashMap<String, Properties>();
	}

	/**
	 * BASIC OPS FOR ELEMENTS.
	 * 
	 * Getters, setters, add, remove, removeAll, getNumberOfElements
	 */
	// -- MAP SETTINGS ------------------------------------------------------------
	public int getMAP_W() {
		return MAP_W;
	}

	public void setMAP_W(int map_W) {
		if (map_W < MIN_MAP_W)
			MAP_W = MIN_MAP_W;
		else
			MAP_W = map_W;
	}

	public int getMAP_H() {
		return MAP_H;
	}

	public void setMAP_H(int map_H) {
		if (map_H < MIN_MAP_H)
			MAP_H = MIN_MAP_H;
		else
			MAP_H = map_H;
	}

	public double getZOOM() {
		return ZOOM;
	}

	public void setZOOM(double zoom) {
		if (zoom > 0)
			ZOOM = zoom;
		else
			ZOOM = 1.0;
	}
	
	public double getPixelRepresentsInMeters() {
		return pixelRepresentsInMeters;
	}

	public void setPixelRepresentsInMeters(double pixelRepresentsInMeters) {
		this.pixelRepresentsInMeters = pixelRepresentsInMeters;
	}
	// ---------------------------------------------------------------------
	
	// -- ROOMS ------------------------------------------------------------
	public List<Room> getRooms() {
		return rooms;
	}

	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}
	
	public int getNumRooms() {
		return this.rooms.size();
	}
	
	/**
	 * Get room with label:
	 * 
	 * @param label
	 * @return
	 */
	public Room getRoom(int label) {
		for(Room r: getRooms()) {
			if (r.getLabel() == label) {
				return r;
			}
		}
		
		return null;
	}
	
	public boolean addRoom(Room room) {
		boolean added = this.rooms.add(room);
		if (added) {
			for(Corner c: room.getCorners()) {
				addCorner(c);
			}
		}
		return added;
	}
	
	public boolean removeRoom(Room room) {
		return this.rooms.remove(room);
	}
	
	public void clearRooms() {
		this.rooms.clear();
	}
	// ---------------------------------------------------------------------
	
	
	// -- CORNERS ------------------------------------------------------------
	
	public List<Corner> getCorners() {
		return corners;
	}

	public void setCorners(List<Corner> corners) {
		this.corners = corners;
	}
	
	public int getNumCorners() {
		return this.corners.size();
	}
	
//	/**
//	 * Get corner with label:
//	 * 
//	 * @param label
//	 * @return
//	 */
//	public Corner getCorner(long label) {
//		for(Corner c: getCorners()) {
//			if (c.getVertex_label() == label) {
//				return c;
//			}
//		}
//		
//		return null;
//	}
	
	public int getCornerRoomIndex(Corner c) {
		int index = -1;
		
		int i = 0;
		boolean found = false;
		
		while (!found && i < getNumRooms()) {
			if (getRooms().get(i).getCorners().contains(c)) {
				index = i;
				found = true;
			}
			
			i++;
		}
		
		return index;
	}
	
	public boolean addCorner(Corner corner) {
		boolean added = false;
		
		//if (!elementCollides(corner)) {
			added = this.corners.add(corner);
			paintDrawable(corner);
		//}
		
		return added;
	}
	
	public boolean removeCorner(Corner corner) {
		return this.corners.remove(corner);
	}
	
	public void clearCorners() {
		this.corners.clear();
	}
	
	public void modifyCorner(Corner c, Point newPosition) {
		/*
		boolean cornerFound = false;
		Room rAux = null;
		*/
		
		getCorners().get(getCorners().indexOf(c)).setVertex_xy(newPosition);
		
		/*
		// Find room which contains corner and modify corner
		for (Room r: getRooms()) {
			int index = r.getCorners().indexOf(c);
			if (index != -1) {
				cornerFound = true;
				r.getCorners().get(index).setVertex_xy(newPosition);
				break;
			}
		}
		
		// Modify room's corner and corner from corner list and painted elements
		if (cornerFound) {
			getCorners().get(getCorners().indexOf(c)).setVertex_xy(newPosition);
			getPaintedElements().get(getPaintedElements().indexOf(c)).setVertex_xy(vertex_xy);
		}
		*/
	}
	// ---------------------------------------------------------------------
	
	// -- ITEMS ------------------------------------------------------------
	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
	
	public int getNumItems() {
		return this.items.size();
	}
	
	/**
	 * Get item with label:
	 * 
	 * @param label
	 * @return
	 */
	public Item getItem(long label) {
		for(Item i: getItems()) {
			if (i.getVertex_label() == label) {
				return i;
			}
		}
		
		return null;
	}
	
	public boolean addItem(Item item) {
		boolean added = false;
		
		//if (!elementCollides(item)) {
			added = this.items.add(item);
			item.getRoom().addItem(item);
			paintDrawable(item);
		//}
		
		return added;
	}
	
	public boolean removeItem(Item item, boolean removingFullRoom) {
		boolean removed = this.items.remove(item);
		
		if (removed && !removingFullRoom)
			removed = item.getRoom().removeItem(item);
		
		return removed;
	}
	
	public void clearItems() {
		this.items.clear();
	}
	// ---------------------------------------------------------------------
	
	// -- DOORS ------------------------------------------------------------
	public List<Door> getDoors() {
		return doors;
	}

	public void setDoors(List<Door> doors) {
		this.doors = doors;
	}
	
	public int getNumDoors() {
		return this.doors.size();
	}
	
	/**
	 * Get door with label:
	 * 
	 * @param label
	 * @return
	 */
	public Door getDoor(long label) {
		for(Door d: getDoors()) {
			if (d.getVertex_label() == label) {
				return d;
			}
		}
		
		return null;
	}
	
	public boolean addDoor(Door door) {
		boolean added = false;
		
		//if (!elementCollides(door)) {
			added = this.doors.add(door);
			if (door.getRoom() != null)
				door.getRoom().addDoor(door);
			paintDrawable(door);
		//}
		
		return added;
	}
	
	public boolean setDoorRoom(Door door, Room room) {
		
		boolean set = door != null && room != null;
		
		if (set) {
			if (room.addDoor(door)) {
				door.setRoom(room);
				set = true;
			}
			else {
				set = false;
			}
		}
		
		return set;
	}
	
	public void updateDoorRoom(Door d, Room r){
		// If door already was in another room, remove door from that room
		if (d.getRoom() != null)
			d.getRoom().removeDoor(d);
		
		// Set room to that door and vice versa
		if (!setDoorRoom(d, r))
			System.out.println("Room not added to door");
	}
	
	public boolean removeDoor(Door door, boolean removingFullRoom) {
		boolean removed = this.doors.remove(door);
		
		if (removed && door.getRoom() != null) {
			if (door.disconnectFromAll() && !removingFullRoom)
				removed = door.getRoom().removeDoor(door);
			else
				removed = false;
		}
		
		return removed;
	}
	
	public void clearDoors() {
		this.doors.clear();
	}
	// ---------------------------------------------------------------------
	
	// -- STAIRS ------------------------------------------------------------
	public List<Stairs> getStairs() {
		return stairs;
	}

	public void setStairs(List<Stairs> stairs) {
		this.stairs = stairs;
	}
	
	public int getNumStairs() {
		return this.stairs.size();
	}
	
	/**
	 * Get room separator with label:
	 * 
	 * @param label
	 * @return
	 */
	public Stairs getStairs(long label) {
		for(Stairs s: getStairs()) {
			if (s.getVertex_label() == label) {
				return s;
			}
		}
		
		return null;
	}
	
	public boolean addStairs(Stairs stairs) {
		boolean added = false;
		
		//if (!elementCollides(stairs)) {
			added = this.stairs.add(stairs);
			paintDrawable(stairs);
		//}
		
		return added;
	}
	
	public boolean removeStairs(Stairs stairs) {
		boolean removed = this.stairs.remove(stairs);
		
		if (removed)
			removed = stairs.disconnectFromAll();
		
		for (Door d: this.doors) {
			d.disconnectFrom(stairs);
		}
		
		return removed;
	}
	
	public void clearStairs() {
		this.stairs.clear();
	}
	// ---------------------------------------------------------------------
	
	// -- ROOM SEPARATORS ------------------------------------------------------------
	public List<RoomSeparator> getRoomSeparators() {
		return roomSeparators;
	}

	public void setRoomSeparators(List<RoomSeparator> roomSeparators) {
		this.roomSeparators = roomSeparators;
	}
	
	public int getNumRoomSeparators() {
		return this.roomSeparators.size();
	}

	/**
	 * Get stairs with label:
	 * 
	 * @param label
	 * @return
	 */
	public RoomSeparator getRoomSeparator(long label) {
		for(RoomSeparator rs: getRoomSeparators()) {
			if (rs.getVertex_label() == label) {
				return rs;
			}
		}
		
		return null;
	}
	
	public boolean addRoomSeparator(RoomSeparator roomSeparator) {
		boolean added = this.roomSeparators.add(roomSeparator);
		
		if (roomSeparator.getRoom() != null)
			roomSeparator.getRoom().addRoomSeparator(roomSeparator);
		paintDrawable(roomSeparator);
		
		return added;
	}
	
	public boolean removeRoomSeparator(RoomSeparator roomSeparator, boolean removingFullRoom) {
		boolean removed = this.roomSeparators.remove(roomSeparator);
		
		if (removed && !removingFullRoom)
			removed = roomSeparator.getRoom().removeRoomSeparator(roomSeparator);
		
		return removed;
	}
	
	public void clearRoomSeparators() {
		this.roomSeparators.clear();
	}
	// ---------------------------------------------------------------------

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFloor() {
		return floor;
	}

	public void setFloor(String floor) {
		this.floor = floor;
	}
	
	public Point getCurrentCursorPoint() {
		return currentCursorPoint;
	}

	public void setCurrentCursorPoint(Point currentCursorPoint) {
		this.currentCursorPoint = currentCursorPoint;
	}

	public Corner getCurrentRoomSeparatorCorner() {
		return currentRoomSeparatorCorner;
	}

	public void setCurrentRoomSeparatorCorner(Corner currentRoomSeparatorCorner) {
		this.currentRoomSeparatorCorner = currentRoomSeparatorCorner;
	}

	public DataAccessGraphFile getAccessGraphFile() {
		return accessGraphFile;
	}

	public void setAccessGraphFile(DataAccessGraphFile accessGraphFile) {
		this.accessGraphFile = accessGraphFile;
	}

	public DataAccessItemFile getAccessItemFile() {
		return accessItemFile;
	}

	public void setAccessItemFile(DataAccessItemFile accessItemFile) {
		this.accessItemFile = accessItemFile;
	}

	public DataAccessRoomFile getAccessRoomFile() {
		return accessRoomFile;
	}

	public void setAccessRoomFile(DataAccessRoomFile accessRoomFile) {
		this.accessRoomFile = accessRoomFile;
	}
	
	public int getDRAWING_ICON_DIMENSION() {
		return DRAWING_ICON_DIMENSION;
	}

	public void setDRAWING_ICON_DIMENSION(int newDim) {
		DRAWING_ICON_DIMENSION = newDim;
	}

	public IconButtons getSelected() {
		return selected;
	}

	public void setSelected(IconButtons selected) {
		this.selected = selected;
	}

	// -- PAINTED ELEMENTS ------------------------------------------------------------
	public List<Drawable> getPaintedElements() {
		return paintedElements;
	}

	public void setPaintedElements(List<Drawable> paintedElements) {
		this.paintedElements = paintedElements;
	}
	
	public void clearPaintedElements() {
		this.paintedElements.clear();
	}
	
	/**
	 * Painted element is persisted.
	 * 
	 * @param element
	 * @return T/F (added or not)
	 */
	public boolean paintDrawable(Drawable element) {
		return paintedElements.add(element);
	}
	
	/**
	 * Erases an element from the painted elements.
	 * 
	 * @param element
	 * @return T/F (erased or not)
	 */
	public boolean eraseDrawable(Drawable element) {
		return paintedElements.remove(element);
	}
	
	/**
	 * Erases a list of drawable elements from the painted elements.
	 * @param elements
	 * @return
	 */
	public boolean eraseDrawableList(List<Drawable> elements) {
		return paintedElements.removeAll(elements);
	}
	// ---------------------------------------------------------------------	
	
	// -- OBJECT SELECTED ------------------------------------------------------------
	public Drawable getObjectSelected() {
		return objectSelected;
	}

	public void setObjectSelected(Drawable objectSelected) {
		this.objectSelected = objectSelected;
	}
	// ---------------------------------------------------------------------	
	
	// -- MOVED OBJECT'S OLD COORDINATES ------------------------------------------------------------

	public Point getOldObjectCoordinates() {
		return oldObjectCoordinates;
	}

	public void setOldObjectCoordinates(Point oldObjectCoordinates) {
		this.oldObjectCoordinates = oldObjectCoordinates;
	}
	
	// ---------------------------------------------------------------------	
	
	// -- CURRENT ROOM CORNERS ------------------------------------------------------------
	public LinkedList<Corner> getCurrentRoomCorners() {
		return currentRoomCorners;
	}

	public void setCurrentRoomCorners(LinkedList<Corner> currentRoomCorners) {
		this.currentRoomCorners = currentRoomCorners;
	}
	
	public int getNumCurrentRoomCorners() {
		return this.currentRoomCorners.size();
	}
	
	public void clearCurrentRoomCorners() {
		this.currentRoomCorners.clear();
	}
	
	public boolean addCornerToCurrentRoomCorners(Corner corner) {
		boolean added = false;
		// PREVIOUS: if (!elementCollides(corner) && !collidesRooms(corner, -1)) {
		if (!collidesRooms(corner, -1)) {
			if (this.currentRoomCorners.isEmpty()) {
				added = this.currentRoomCorners.add(corner);
				paintDrawable(corner);
			}
			else {
				// Corner c = this.currentRoomCorners.getLast();
				if (!this.currentRoomCorners.contains(corner)) {
					added = this.currentRoomCorners.add(corner);
					paintDrawable(corner);
				}
			}
		}
		
		return added;
	}

	/**
	 * Clears rooms from current room corners
	 */
	public void emptyCurrentRoomCorners() {
		this.currentRoomCorners.clear();
	}
	
	/**
	 * Clears rooms from current room corners and erases them from painted elements.
	 */
	public void eraseCurrentRoomCorners() {
		eraseDrawableList(new ArrayList<>(this.currentRoomCorners));
		emptyCurrentRoomCorners();
	}
	// ---------------------------------------------------------------------	

	// -- TOOL CLICKED ------------------------------------------------------------
	public ToolButtons getToolClicked() {
		return toolClicked;
	}

	public void setToolClicked(ToolButtons toolClicked) {
		this.toolClicked = toolClicked;
	}
	// ---------------------------------------------------------------------
	
	// -- VISITABLE OBJECTS (DIFFERENT OBJECT BUTTONS) ------------------------------------------------------------
	public Map<String, Properties> getVisitableObjects() {
		return visitableObjects;
	}

	public void setVisitableObjects(Map<String, Properties> visitableObjects) {
		this.visitableObjects = visitableObjects;
	}
	
	public boolean alreadyExistsVisitableObject(String key) {
		return this.visitableObjects.containsKey(key);
	}
	
	public boolean addVisitableObject(String key, Properties value) {
		return this.visitableObjects.putIfAbsent(key, value) == null;
	}
	
	public boolean removeVisitableObject(String key) {
		return this.visitableObjects.remove(key) != null;
	}
	
	public String getVisitableSelected() {
		return visitableSelected;
	}

	public void setVisitableSelected(String visitableSelected) {
		this.visitableSelected = visitableSelected;
	}
	// ---------------------------------------------------------------------
	
	
	
	/**
	 * Remove all existing elements
	 */
	public void clearAll() {
		clearRooms();
		clearCorners();
		clearItems();
		clearDoors();
		clearStairs();
		
		clearPaintedElements();
		clearCurrentRoomCorners();
	}
	
	/**
	 * Checks if a point collides an element.
	 * 
	 * @param point
	 * @param drawable
	 * @return
	 */
	public boolean pointCollidesElement(Point point, Drawable drawable) {
		return Math.abs(point.getX() - drawable.getVertex_xy().getX()) < getDRAWING_ICON_DIMENSION() 
				&& Math.abs(point.getY() - drawable.getVertex_xy().getY()) < getDRAWING_ICON_DIMENSION();
	}
	
	/**
	 * Checks if two drawable elements collide.
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean twoElementsCollide(Drawable first, Drawable second) {
		return Math.abs(first.getVertex_xy().getX() - second.getVertex_xy().getX()) < getDRAWING_ICON_DIMENSION() 
				&& Math.abs(first.getVertex_xy().getY() - second.getVertex_xy().getY()) < getDRAWING_ICON_DIMENSION();
	}
	
	/**
	 * Checks if element collides with the already created elements.
	 * 
	 * @param d		element
	 * @return T/F	(collides or not)
	 */
	public boolean elementCollides(Drawable d) {
		boolean collides = false;
		for(Drawable drawn: paintedElements) {
			
			if (drawn != getObjectSelected()) {
				if (twoElementsCollide(d, drawn))
					break;
			}
		}
		return collides;
		
	}
	
	/**
	 * Returns a list without the current corner (for checking operations). The list returned is ordered so that
	 * the remaining corners in list start from the following corner to currentCorner and the last corner in list
	 * is the previous corner.
	 * 
	 * @param corners			List of corners
	 * @param currentCorner		Current corner we want to exclude from list
	 * @return					List of corners without current corner
	 */
	private List<Corner> getCornerListWithoutCurrentCorner(List<Corner> corners, Corner currentCorner) {
		
		if (corners.contains(currentCorner)) {
			List<Corner> newCorners = new LinkedList<Corner>();
			int index = corners.indexOf(currentCorner);
			int indexHigher = index + 1;
			while (indexHigher < corners.size()) {
				newCorners.add(corners.get(indexHigher));
				indexHigher++;
			}
			
			for (int i = 0; i < index; i++) {
				newCorners.add(corners.get(i));
			}
			
			/*
			newCorners.addAll(corners);
			newCorners.remove(currentCorner);
			*/
			
			return newCorners;
		}
		else {
			return corners;
		}
	}
	
	/**
	 * Checks if an element is inside a room.
	 * 
	 * @param 	d
	 * @return	T/F isInside
	 */
	public Room isInsideRoom(Point p, int excludeCurrentRoomIndex) {
		
		Room r = null;
		
		int i = 0;
		while(i < getRooms().size() && r == null) {
			if (i != excludeCurrentRoomIndex && getRooms().get(i).getPolygon().contains(p.getX(), p.getY())) {
				r = getRooms().get(i);
			}
			i++;
		}
		
		/*
		if (r != null) {
			System.out.println("POINT INSIDE ROOM");
		}
		*/
		
		return r;
	}
	
	/**
	 * Checks if the wall (line) between corners collides with an existing list of corners (can be full room or current room being drawn).
	 * 
	 * @param wallFirstEndpoint
	 * @param wallSecondEndpoint
	 * @param corners
	 * @param isCompletedRoom
	 * @return T/F
	 */
	private boolean wallCollidesCorners(Corner wallFirstEndpoint, Corner wallSecondEndpoint, List<Corner> corners, boolean isCompletedRoom) {
		
		if (!corners.isEmpty()) {
			Corner previous = null;
			
			for (Corner c: corners) { // For all room corners
				if (c == corners.get(0)) {
					previous = c;
				}
				else {
					if(Line2D.linesIntersect(wallFirstEndpoint.getVertex_xy().getX(), wallFirstEndpoint.getVertex_xy().getY(), 
							wallSecondEndpoint.getVertex_xy().getX(), wallSecondEndpoint.getVertex_xy().getY(), 
							previous.getVertex_xy().getX(), previous.getVertex_xy().getY(),
							c.getVertex_xy().getX(), c.getVertex_xy().getY())) {
						
						System.out.println("It intersects!!");
						return true;
					}
					
					// If not returned, keep searching
					previous = c;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the wall (line) between corners collides with the current room walls being drawn.
	 * 
	 * @param wallFirstEndpoint
	 * @param wallSecondEndpoint
	 * @return T/F
	 */
	private boolean wallCollidesCurrentCompletedRoom(Corner previous, Corner current, Corner next, Room room) {
		
		boolean collides = false;
		
		// Get lists without previous and last corners
		List<Corner> listWithoutPrevious = getCornerListWithoutCurrentCorner(room.getCorners(), previous);
		List<Corner> listWithoutNext = getCornerListWithoutCurrentCorner(room.getCorners(), next);
		
		// Check the two walls connected to the moved corner
		collides =  wallCollidesCorners(previous, current, getCornerListWithoutCurrentCorner(listWithoutPrevious, current), true);
		if (!collides) { // Just check if the previous check didn't collide (not needed in that case)
			collides =  wallCollidesCorners(current, next, getCornerListWithoutCurrentCorner(listWithoutNext, current), true);
		}
		
		return collides;
	}
	
	/**
	 * Checks if the wall (line) between corners collides with the current room walls being drawn.
	 * 
	 * @param wallFirstEndpoint
	 * @param wallSecondEndpoint
	 * @return T/F
	 */
	private boolean wallCollidesCurrentRoom(Corner wallFirstEndpoint, Corner wallSecondEndpoint) {
		return wallCollidesCorners(wallFirstEndpoint, wallSecondEndpoint, 
				getCornerListWithoutCurrentCorner(getCurrentRoomCorners(), wallSecondEndpoint), false);
	}
	
	/**
	 * Checks if the wall (line) between corners collides with an existing wall.
	 * 
	 * @param wallFirstEndpoint
	 * @param wallSecondEndpoint
	 * @return T/F
	 */
	private boolean wallCollidesRooms(Corner wallFirstEndpoint, Corner wallSecondEndpoint, int excludeCurrentRoomIndex) {
		
		boolean collides = false;
		int i = 0;
		
		while (i < getRooms().size() && !collides) { // For all room corners
			if (i != excludeCurrentRoomIndex) {
				collides = wallCollidesCorners(wallFirstEndpoint, wallSecondEndpoint, 
						getCornerListWithoutCurrentCorner(getRooms().get(i).getCorners(), wallSecondEndpoint), true);
			}
			i++;
        }
		
        return collides;
	}
	
	/**
	 * Checks if corner is inside an existing room or if it collides with existing or current room (using last current room corner to create a wall).
	 * 
	 * @param corner
	 * @return
	 */
	public boolean collidesRooms(Corner corner, int excludeCurrentRoomIndex) {
		
		boolean collides = false;
		
		if (excludeCurrentRoomIndex < 0) {
			if (getNumCurrentRoomCorners() < 1) { // If it's the first corner from the new room
				// Check if element's coordinates are inside some of the already created rooms
				
				// DELETED: ROOMS CAN BE INSIDE A BIG ROOM (SHOPS INSIDE A SHOPPING MALL)
				//collides = (isInsideRoom(corner.getVertex_xy(), excludeCurrentRoomIndex) != null);
			}
			else { // If there are more corners drawn from current room
				// Check current room being drawn
				collides = wallCollidesCurrentRoom(corner, getCurrentRoomCorners().get(getNumCurrentRoomCorners()-1));
				
				if (!collides) {
					// Check all the already existing walls (from all walls)
					collides = wallCollidesRooms(corner, getCurrentRoomCorners().get(getNumCurrentRoomCorners()-1), excludeCurrentRoomIndex);
				}
			}
		}
		else {
			// No new room is being created. Check if collides with existing rooms or with herself.
			List<Corner> corners = getRooms().get(excludeCurrentRoomIndex).getCorners();
			int cornerIndex = corners.indexOf(corner);
			int numberOfCornersIndexesInRoom = corners.size() - 1;
			
			// Get previous and next indexes
			int previousIndex, nextIndex;
			
			if (cornerIndex == 0) {
				previousIndex = numberOfCornersIndexesInRoom;
				nextIndex = cornerIndex + 1;
			}
			else if (cornerIndex == numberOfCornersIndexesInRoom) {
				previousIndex = cornerIndex-1;
				nextIndex = 0;
			}
			else {
				previousIndex = cornerIndex-1;
				nextIndex = cornerIndex+1;
			}
			
			Corner previousCorner = corners.get(previousIndex);
			Corner nextCorner = corners.get(nextIndex);
			
			// Current completed room
			collides = wallCollidesCurrentCompletedRoom(previousCorner, corner, nextCorner, getRooms().get(excludeCurrentRoomIndex));
			
			if (!collides) {
				
				// Check all the already existing rooms (except current one) with both possible walls
				collides = wallCollidesRooms(previousCorner, corner, excludeCurrentRoomIndex);
				if(!collides) {
					collides = wallCollidesRooms(corner, nextCorner, excludeCurrentRoomIndex);
				}
			}
		}
		return collides;
	}
	
	/**
	 * Check if there is any concave room. Used for warning the user, so that he/she adds invisible doors.
	 *  
	 * @param rooms		List of rooms
	 * @return			number of concave rooms
	 */
	public int checkConcaveRooms() {
		
		int numConcaveRooms = 0;
		// CHECK IF CONCAVE ROOMS
		for (Room r: getRooms()) {
			if (r.checkConcave())
				numConcaveRooms++;
		}
		
		return numConcaveRooms;
	}
	
	/**
	 * Revert all painted element's coordinates.
	 */
	public void revertCoordinates() {
		for (Drawable d: getPaintedElements()) {
			d.setVertex_xy(new Point(d.getVertex_xy().getY(), d.getVertex_xy().getX()));
		}
	}
	
	/**
	 * Set data accesses' files with the specified directory and default names for each file.
	 * 
	 * @param directory
	 */
	public void setFiles(String directory) {
		
		accessGraphFile.setFile(new File(directory + File.separator + Literals.GRAPH_FILE_NAME));
		accessRoomFile.setFile(new File(directory + File.separator + Literals.ROOM_FILE_NAME));
		accessItemFile.setFile(new File(directory + File.separator + Literals.ITEM_FILE_NAME));
	}
	
	public void setFiles(File graphFile, File roomFile, File itemFile) {		
		accessGraphFile.setFile(graphFile);
		accessRoomFile.setFile(roomFile);
		accessItemFile.setFile(itemFile);
	}
	
	/**
	 * Creates the dataAccesses' files
	 */
	public void createFiles() {
		
		try {
			accessGraphFile.getFile().createNewFile();
			accessRoomFile.getFile().createNewFile();
			accessItemFile.getFile().createNewFile();
			
			accessGraphFile.clearProperties();
			accessRoomFile.clearProperties();
			accessItemFile.clearProperties();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	// -- SAVE ------------------------------------------------------------------------------------
	
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
	 * Save all existing items with their attributes in item file of the selected directory.
	 */
	//private void saveItemFile() {
	private void saveItemFile(boolean saveFile) {
		
		// Set properties
		
		// Set room(s) name
		accessItemFile.setName(getName());
		
		// Set number of rooms
		accessItemFile.setNumberOfItems(getNumItems());
		
		// Set vertex dimensions
		accessItemFile.setVertexDimensionHeight(DRAWING_ICON_DIMENSION);
		accessItemFile.setVertexDimensionWidth(DRAWING_ICON_DIMENSION);
		
		int index = 1; // index of the current item
		
		// For each item
		for (Item i: getItems()) {
			
			// Set itemID
			// accessItemFile.setItemID(index, index);
			// i.setVertex_label(index);

			// Añadido por Nacho Palacio 2025-04-18.
			accessItemFile.setItemID(index, (int)(i.getVertex_label() % ElementIdMapper.ITEM_ID_START));
			
			// Main attributes
			// Set item room
			accessItemFile.setItemRoom(index, i.getRoom().getLabel());
			// Set item label
			accessItemFile.setVertexLabel(index, i.getItemLabel());
			// Set item position
			accessItemFile.setVertexXY(index, i.getVertex_xy().toString());
			
			// Item attributes
			// Set item title
			accessItemFile.setItemTitle(index, checkItemAttribute(i.getTitle()));
			// Set item vertexURL
			accessItemFile.setVertexURL(index, checkItemAttribute(i.getUrlIcon()));
			// Set item nationality
			accessItemFile.setItemNationality(index, checkItemAttribute(i.getNationality()));
			// Set item beginDate
			accessItemFile.setItemBeginDate(index, checkItemAttribute(i.getBeginDate()));
			// Set item endDate
			accessItemFile.setItemEndDate(index, checkItemAttribute(i.getEndDate()));
			// Set item date
			accessItemFile.setItemDate(index, checkItemAttribute(i.getDate()));
			// Set item department
			//accessItemFile.setItemDepartment(index, checkItemAttribute(i.getDepartment()));
			// Set item height
			accessItemFile.setItemHeight(index, i.getHeight());
			// Set item width
			accessItemFile.setItemWidth(index, i.getWidth());
			
			index++;
		}
		
		// Store all properties
		//accessItemFile.storeProperties();
		if(saveFile) accessItemFile.storeProperties();
		
	}
	
	/**
	 * Save all map settings (name, width, height, 1 pixel represents in meters...)
	 * 
	 * Save all room corners, doors and stairs' locations.
	 */
	private void saveRoomFile(boolean saveFile) {
		
		// Set properties
		
		// Set map's name
		accessRoomFile.setName(getName());
		// Set map's width
		accessRoomFile.setMapWidth(getMAP_W());
		// Set map's width
		accessRoomFile.setMapHeight(getMAP_H());
		// Set map's width
		accessRoomFile.setMapPixelRepresentsMeters(getPixelRepresentsInMeters());
		
		// Set number of rooms
		accessRoomFile.setNumberOfRoom(getNumRooms());
//		System.out.println("save room");
		int room = 0;
		// For each room
		while (room < getNumRooms()) {
//			System.out.println(getRooms().get(room).getLabel());
			// Set room's label
			accessRoomFile.setRoomLabel(room+1, getRooms().get(room).getLabel()); // accessRoomFile.setRoomLabel(room+1, room+1);
			
			// Set number of corners in room
			accessRoomFile.setRoomNumberCorner(room+1, getRooms().get(room).getNumCorners());
			// For each corner in room
			for(int corner = 0; corner < getRooms().get(room).getNumCorners(); corner++) {
				// Set corner locations
				accessRoomFile.setRoomCorner(corner+1, room+1, getRooms().get(room).getCorners().get(corner).getVertex_xy().toString());
			}
			
			// Set number of doors in room
			accessRoomFile.setRoomNumberDoor(room+1, getRooms().get(room).getNumDoors());
			// For each door in room
			for(int door = 0; door < getRooms().get(room).getNumDoors(); door++) {
				// Set door locations
				accessRoomFile.setRoomDoorXY(door+1, room+1, getRooms().get(room).getDoors().get(door).getVertex_xy().toString());
			}
			
			room++;
		}
		
		// Set number of stairs
		accessRoomFile.setNumberStairs(getNumStairs());
		// For each stairs
		for(int stairs = 0; stairs < getNumStairs(); stairs++) {
			// Set stairs locations
			accessRoomFile.setStairs(stairs+1, getStairs().get(stairs).getVertex_xy().toString());
		}
		
		/**
		 * SUBROOMS
		 */
		// For each subroom -> Done afterwards, to store ids correctly
		for (int newRoom = 0; newRoom < getNumRooms(); newRoom++) {
			
			// Num subrooms
			int numSubrooms = getRooms().get(newRoom).getNumSubRooms();
			
			// Save RoomSeparators (Editor)
			int roomSeparators = getRooms().get(newRoom).getNumRoomSeparators();
			// Number of room separators AND invisible doors
			accessRoomFile.setRoomNumberRoomSeparators(newRoom+1, roomSeparators);
			accessRoomFile.setRoomNumberInvisibleDoor(newRoom+1, roomSeparators*2);
			
			
			int invisible = 1;
			// For every subroom (if there exist)
			for (int sub = 0; sub < numSubrooms; sub++) {
				// For every invisible door (RoomSeparator)
				for (int i = 0; i < roomSeparators; i++) { // One invisible door per room separator (in the middle)
					/**
					 * ROOM SEPARATORS
					 */
					// Set corners for roomseparator of room
					RoomSeparator currentRS = getRooms().get(newRoom).getRoomSeparators().get(i);
					String roomSeparatorCorners = currentRS.getC1().getVertex_label() + ", " + currentRS.getC2().getVertex_label();
					accessRoomFile.setRoomRoomseparatorCorners(i+1, newRoom+1, roomSeparatorCorners);
					
					/**
					 * INVISIBLE DOORS
					 */
					// Get middle point and set it as door's location
					Point doorLocation = currentRS.getMiddlePoint();
					// Get subroom's polygon
					Polygon polygon = getRooms().get(newRoom).getSubRooms().get(sub).getPolygon();
					
					// If subroom contains doorLocation, add Invisible Door to subroom
					if (objectIntesectsRoomPolygon(doorLocation, polygon)) {
						accessRoomFile.setRoomInvisibleDoorXY(invisible, newRoom+1, doorLocation.toString());
						invisible++;
					}
				}
			}
		}
		
		// Store all the properties
		if(saveFile) accessRoomFile.storeProperties();
		
	}

	/**
	 * Save all object labels mapped (room with its label and with its corresponding items and doors; stairs with their labels).
	 * 
	 * Also save the connectable element connections (doors connected to doors, doors connected to stairs and stairs connected to stairs).
	 */
	private void saveGraphFile(boolean saveFile) {
		
		int numberRoomsAndSubrooms = 0;
		
		Set<Pair<Door, Door>> connectedDoors = new HashSet<Pair<Door, Door>>();
		Set<Pair<Door, Stairs>> connectedDoorStairs = new HashSet<Pair<Door, Stairs>>();
		Set<Pair<Stairs, Stairs>> connectedStairs = new HashSet<Pair<Stairs, Stairs>>();
		
		// Set properties
		accessGraphFile.setNumberOfRoom(rooms.size());
		
		int numItems = getNumItems();
		int idStairs = numItems + getNumDoors() + 1;
		int idSubrooms = getNumRooms() + 1;
		int numObjects = numItems + getNumDoors() + getNumStairs();
		
		// Number of connected invisible doors = num invisible doors * 2 (connected to themselves)
		int numConnectedInvisibleDoors = getRoomSeparators().size()*2;
		// Create array
		RoomSeparator invisibleDoorsArray[] = new RoomSeparator[numConnectedInvisibleDoors];
		// Fill array with RoomSeparators (every rs = invisibleDoors). Their position in array will be the future invisible-door's id
		for (int connectedInvisible = 0; connectedInvisible < numConnectedInvisibleDoors; connectedInvisible=connectedInvisible+2) { // Connect consecutive invisible doors
			int id = connectedInvisible/2;
			invisibleDoorsArray[connectedInvisible] = getRoomSeparators().get(id);
			invisibleDoorsArray[connectedInvisible+1] = getRoomSeparators().get(id);
			
			//System.out.println("invisibleDoorsArray[" + connectedInvisible + "] = " + getRoomSeparators().get(id));
			//System.out.println("invisibleDoorsArray[" + (connectedInvisible+1) + "] = " + getRoomSeparators().get(id));
		}
		
		// Array will be filled with strings (door_numDoorInRoom_Room) when invisible door get their respective id's assigned
		String connectedInvisibleDoors[] = new String[numConnectedInvisibleDoors];
		
		// For each room
		for (int room = 0; room < getNumRooms(); room++) {
			// Set room's label
			accessGraphFile.setRoom(room+1, getRooms().get(room).getLabel());
			
			/**
			 * ITEMS
			 */
			// Set number of items in room
			accessGraphFile.setNumberOfItemsByRoom(room+1, getRooms().get(room).getNumItems());
			// For each item in room
			for(int item = 0; item < getRooms().get(room).getNumItems(); item++) {
				// Set item id/label
				accessGraphFile.setItemOfRoom(item+1, room+1, Long.toString(getRooms().get(room).getItems().get(item).getVertex_label()));
			}
			
			/**
			 * DOORS
			 */
			// Set number of doors in room
			accessGraphFile.setNumberOfDoorsByRoom(room+1, getRooms().get(room).getNumDoors());
			// For each door in room
			for(int door = 0; door < getRooms().get(room).getNumDoors(); door++) {
				
				Door currentDoor = getRooms().get(room).getDoors().get(door);
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
			
			/**
			 * SUBROOMS (IF ROOM CONTAINS SUBROOMS)
			 */
			int numSubrooms = getRooms().get(room).getNumSubRooms();
			// Set number of subrooms in room
			accessGraphFile.setRoomNumberSubrooms(room+1, numSubrooms);
			
			// If room contains subrooms
			if (numSubrooms > 0) {
				// Add number of subrooms
				numberRoomsAndSubrooms += numSubrooms;
				
				// For every subroom
				for (int subroom = 0; subroom < getRooms().get(room).getSubRooms().size(); subroom++) {
					// Set subroom
					accessGraphFile.setSubroom(subroom+1, room+1, idSubrooms);
					idSubrooms++;
					
					int items = 0, doors = 0, invisibleDoors = 0;
					// Set subroom's items
					for (int item = 0; item < getRooms().get(room).getNumItems(); item++) {
						// If item intersects polygon, it's inside the subroom
						if (objectIntesectsRoomPolygon(getRooms().get(room).getItems().get(item).getVertex_xy(),
								getRooms().get(room).getSubRooms().get(subroom).getPolygon())) {
							
							// Add item to subroom
							accessGraphFile.setItemOfSubroom(items+1, subroom+1, room+1, Long.toString(getRooms().get(room).getItems().get(item).getVertex_label()));
							items++;
						}
					}
					// Set number of subroom's items
					accessGraphFile.setNumberOfItemsBySubroom(subroom+1, room+1, items);
					
					
					// Set subroom's doors
					for (int door = 0; door < getRooms().get(room).getNumDoors(); door++) {
						// If door intersects polygon, it's inside the subroom
						if (objectIntesectsRoomPolygon(getRooms().get(room).getDoors().get(door).getVertex_xy(),
								getRooms().get(room).getSubRooms().get(subroom).getPolygon())) {
							
							// Add door to subroom
							accessGraphFile.setDoorOfSubroom(doors+1, subroom+1, room+1, Long.toString(getRooms().get(room).getDoors().get(door).getVertex_label() + numItems));
							
							doors++;
						}
					}
					// Set number of subroom's doors
					accessGraphFile.setNumberOfDoorsBySubroom(subroom+1, room+1, doors);
					
					
					// Set subroom's invisible doors
					for (int invisibleDoor = 0; invisibleDoor < getRooms().get(room).getNumRoomSeparators(); invisibleDoor++) {
						
						RoomSeparator rs = getRooms().get(room).getRoomSeparators().get(invisibleDoor);
						// If invisible door intersects polygon, subroom contains invisible door
						if (objectIntesectsRoomPolygon(rs.getMiddlePoint(),
								getRooms().get(room).getSubRooms().get(subroom).getPolygon())) {
							
							// Get index, set its value as null and use it to create invisible-door's id
							int index = Arrays.asList(invisibleDoorsArray).indexOf(rs);
							invisibleDoorsArray[index] = null;
							int id = index + 1 + numObjects;
							
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
		accessGraphFile.setNumberOfStairs(getNumStairs());
		// For each stairs
		for(int stairs = 0; stairs < getNumStairs(); stairs++) {
			
			Stairs currentStairs = getStairs().get(stairs);
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
			
			int roomDoorF = getRooms().indexOf(pair.getF().getRoom());
			int roomDoorS = getRooms().indexOf(pair.getS().getRoom());
			int doorF = getRooms().get(roomDoorF).getDoors().indexOf(pair.getF()) + 1;
			int doorS = getRooms().get(roomDoorS).getDoors().indexOf(pair.getS()) + 1;
			
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
			
			int roomDoor = getRooms().indexOf(pair.getF().getRoom());
			int stairs = getStairs().indexOf(pair.getS()) + 1;
			int door = getRooms().get(roomDoor).getDoors().indexOf(pair.getF()) + 1;
			
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
			
			int stairsF = getStairs().indexOf(pair.getF()) + 1;
			int stairsS = getStairs().indexOf(pair.getS()) + 1;
			
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
		
		
		// Store all the properties
		if(saveFile) accessGraphFile.storeProperties();
	}
	
	/**
	 * Check the following map constraints:
	 * 
	 * - All rooms have at least one door
	 * - All doors are connected to at least one connectable element (and room isn't null)
	 * - All items are inside one room
	 * @return
	 */
	private boolean checkCorrectConstraints() {
		boolean correctConstraints = true;
		
		// All rooms have at least one door
		for (Room r: getRooms()) {
			correctConstraints = r.getDoors().size() > 0;
			if (!correctConstraints) // If constraints violated, stop searching
				System.out.println("NOT ALL ROOMS HAVE AT LEAST ONE DOOR");
				break;
		}
		
		// All doors are connected to at least one connectable element (and room isn't null)
		if (correctConstraints) {
			for (Door d: getDoors()) {
				correctConstraints = d.getConnectedTo().size() > 0; // Check connected to 1+ connectables
				if (correctConstraints)
					correctConstraints = d.getRoom() != null; // Check room not null
				
				if (!correctConstraints) // If constraints violated, stop searching
					System.out.println("NOT ALL DOORS CONNECTED");
					break;
			}
		}
		
		// All items are inside one room
		if (correctConstraints) {
			for (Item i: getItems()) {
				correctConstraints = i.getRoom() != null;  // This is ensured in item creation/movement, but checked just in case
				if (!correctConstraints) // If constraints violated, stop searching
					System.out.println("NOT ALL ITEMS INSIDE A ROOM");
					break;
			}
		}
		
		
		return correctConstraints;
	}
	
	/**
	 * Save model's data to graph, room and item files.
	 */
	//public boolean save(boolean saveFiles) {
	public boolean save(boolean saveFiles) {
		// Añadido por Nacho Palacio 2025-04-18.
		if(checkCorrectConstraints()) { // Every room has at least one door, every door is connected to at least one connected element.
			boolean allIdsCorrect = true;
        
			for (Room r : rooms) {
				if (!ElementIdMapper.isInCorrectRange(r.getLabel(), ElementIdMapper.CATEGORY_ROOM)) {
					System.out.println("Warning: Room " + r.getLabel() + " has invalid ID range");
					allIdsCorrect = false;
				}
			}
			
			for (Item i : items) {
				if (!ElementIdMapper.isInCorrectRange(i.getVertex_label(), ElementIdMapper.CATEGORY_ITEM)) {
					System.out.println("Warning: Item " + i.getVertex_label() + " has invalid ID range");
					allIdsCorrect = false;
				}
			}
			
			for (Door d : doors) {
				if (!ElementIdMapper.isInCorrectRange(d.getVertex_label(), ElementIdMapper.CATEGORY_DOOR)) {
					System.out.println("Warning: Door " + d.getVertex_label() + " has invalid ID range");
					allIdsCorrect = false;
				}
			}
			
			for (Stairs s : stairs) {
				if (!ElementIdMapper.isInCorrectRange(s.getVertex_label(), ElementIdMapper.CATEGORY_STAIRS)) {
					System.out.println("Warning: Stairs " + s.getVertex_label() + " has invalid ID range");
					allIdsCorrect = false;
				}
			}
			
			if (!allIdsCorrect) {
				System.out.println("Reassigning IDs to ensure correct ranges...");
				reassignIDs(); // Ya has implementado este método con los rangos correctos
			}


			if(saveFiles) {createFiles();}
			
//			saveItemFile();
//			saveRoomFile();
//			saveGraphFile();
			saveItemFile(saveFiles);
			saveRoomFile(saveFiles);
			saveGraphFile(saveFiles);
			
			return true;
		}
		else {
			System.out.println("CONSTRAINTS NOT CORRECT");
			return false;
		}
		
	}
	
	/**
	 * Save model's data to SVG file.
	 */
	public boolean saveSVG(String path) {
		
//		System.out.println("creator");
//		for(Room r : rooms) {
//			System.out.println(r.getLabel()+" "+r.getCorners());
//		}
		
		
		if(checkCorrectConstraints()) { // Every room has at least one door, every door is connected to at least one connected element.
			
			save(false);
			SVGCreator svgCr = new SVGCreator(this);
			svgCr.crear(path);
			
			return true;
		}
		else {
			System.out.println("CONSTRAINTS NOT CORRECT");
			return false;
		}
		
	}
	

	// --------------------------------------------------------------------------------------------
	
	// -- LOAD ------------------------------------------------------------------------------------
	
	private void loadRoomFile(int xDisplacement, int yDisplacement) {
		// Set properties
		
		// Get map's name
		setName(accessRoomFile.getName());
		// Get map's width
		setMAP_W(accessRoomFile.getMapWidth());
		// Set map's width
		setMAP_H(accessRoomFile.getMapHeight());
		// Set map's width
		setPixelRepresentsInMeters(accessRoomFile.getMapPixelRepresentsMeters());
		
		// Get number of rooms
		int numberOfRooms = accessRoomFile.getNumberOfRoom();

		// For each room
		for (int room = 0; room < numberOfRooms; room++) {
			// Get room's label
			// int roomLabel = Integer.parseInt(accessRoomFile.getRoomLabel(room+1));
			int roomLabel = getNumRooms() + 1;
			
			// Get number of corners in room
			int numberOfCorners = accessRoomFile.getRoomNumberCorner(room+1);
			List<Corner> cornerList = new LinkedList<Corner>();
			
			// For each corner in room
			for(int corner = 0; corner < numberOfCorners; corner++) {
				// Get corner locations
				String roomCornerXY = accessRoomFile.getRoomCornerXY(corner+1, room+1);
				String[] array = roomCornerXY.split(", ");
				double cornerX = Double.parseDouble(array[0]);
				double cornerY = Double.parseDouble(array[1]);
				
				// Create corner and add it to corners
				Corner c = new Corner(null, corner+1, new Point(cornerX + xDisplacement, cornerY + yDisplacement));
				cornerList.add(c);
				//addCorner(c);
			}
			// Create room with its corners and add it to rooms
			// Añadido por Nacho Palacio 2025-04-18.
			System.out.println("Creating room with label: " + roomLabel); //  Añadido por Nacho Palacio 2025-06-29
			Room r = new Room(ElementIdMapper.convertToRangeId(roomLabel, ElementIdMapper.CATEGORY_ROOM), cornerList); 
			addRoom(r);
			
			// Get number of doors in room
			int numberOfDoors = accessRoomFile.getRoomNumberDoor(room + 1);
			// For each door in room
			for(int door = 0; door < numberOfDoors; door++) {
				
				// Get door id
				int doorID = (int) accessGraphFile.getDoorOfRoom(door+1, room+1); // Doors can be included in different order -> Must get its id, not just by position
				doorID -= accessItemFile.getNumberOfItems();
				
				// Get door locations
				String roomDoorXY = accessRoomFile.getRoomDoorXY(door+1, room+1);
				String[] array = roomDoorXY.split(", ");
				double doorX = Double.parseDouble(array[0]);
				double doorY = Double.parseDouble(array[1]);
				
				// Create door and add it to doors
				//Door d = new Door(r, accessGraphFile.getDoorOfRoom(door+1, getNumDoors() + 1), new Point(doorX + xDisplacement, doorY + yDisplacement));
				Door d = new Door(r, doorID, new Point(doorX + xDisplacement, doorY + yDisplacement));
				addDoor(d);
			}
			
			try {
				// Get number of roomSeparators in room
				int numberOfRoomSeparators = accessRoomFile.getRoomNumberRoomSeparators(room + 1);
				// For each roomSeparator
				
				for(int roomSeparator = 0; roomSeparator < numberOfRoomSeparators; roomSeparator++) {
					
					try {
						// Get roomSeparator corners
						String[] array = accessRoomFile.getRoomRoomseparatorCorners(roomSeparator+1, room+1).split(", ");
						int c1 = Integer.parseInt(array[0]);
						int c2 = Integer.parseInt(array[1]);
						
						Corner corner1 = r.getCorners().get(c1-1);
						Corner corner2 = r.getCorners().get(c2-1);
						
						// Create roomSeparator using corners
						RoomSeparator rs = new RoomSeparator(r, r.getNumRoomSeparators()+1, corner1, corner2);
						addRoomSeparator(rs);
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			catch (NumberFormatException e) {
				System.out.println("Room Separators not included yet.");
				e.printStackTrace();
			}
		}
		
		// Get number of stairs
		int numberOfStairs = accessRoomFile.getNumberStairs();
		// For each stairs
		for(int stairs = 0; stairs < numberOfStairs; stairs++) {
			// Get stairs locations
			String roomStairsXY = accessRoomFile.getStairs(stairs+1);
			String[] array = roomStairsXY.split(", ");
			double stairsX = Double.parseDouble(array[0]);
			double stairsY = Double.parseDouble(array[1]);
			
			// Create stairs and add them to stairs
			Stairs s = new Stairs(null, getNumStairs()+1, new Point(stairsX + xDisplacement, stairsY + yDisplacement));
			addStairs(s);
		}
	}
	
	private void loadGraphFile(int numPrevRooms, int numPrevDoors, int numPrevStairs) {
		// Load all the properties
		accessGraphFile.loadProperties();

		// Set all connections
		// Add connections for connected doors
		int numConnectedDoors = accessGraphFile.getNumberOfConnectedDoor();
		
		for(int i = 0; i < numConnectedDoors; i++) {
			
			String connectedDoors = accessGraphFile.getConnectedDoor(i+1);
			
			// Get both doors
			String[] array = connectedDoors.split(", ");
			String doorF = array[0];
			String doorS = array[1];
			
			// First door
			// door_"numDoorInRoom"_"numRoom"
			String[] arrayDoor = doorF.split("_");
			int numDoorInRoom = Integer.valueOf(arrayDoor[1]);
			int numRoom = Integer.valueOf(arrayDoor[2]) + numPrevRooms;
			
			Door firstDoor = getRooms().get(numRoom-1).getDoors().get(numDoorInRoom-1);

			System.out.println("Connecting door: " + firstDoor.getVertex_label() + " in room: " + numRoom); // Añadido por Nacho Palacio 2025-06-29
			// Añadido por Nacho Palacio 2025-04-18.
			firstDoor.setVertex_label(ElementIdMapper.convertToRangeId(firstDoor.getVertex_label(), ElementIdMapper.CATEGORY_DOOR)); 
			
			// Second door
			// door_"numDoorInRoom"_"numRoom"
			arrayDoor = doorS.split("_");
			numDoorInRoom = Integer.valueOf(arrayDoor[1]);
			numRoom = Integer.valueOf(arrayDoor[2]) + numPrevRooms;
			
			Door secondDoor = getRooms().get(numRoom-1).getDoors().get(numDoorInRoom-1);

			System.out.println("Connecting second door: " + secondDoor.getVertex_label() + " in room: " + numRoom); // Añadido por Nacho Palacio 2025-06-29
			// Añadido por Nacho Palacio 2025-04-18.
			secondDoor.setVertex_label(ElementIdMapper.convertToRangeId(secondDoor.getVertex_label(), ElementIdMapper.CATEGORY_DOOR));
			
			// Add connection (connectTo creates a connection for both doors)
			firstDoor.connectTo(secondDoor);
		}
		
		// Add connections for connected door-stairs
		int numConnectedDoorStairs = accessGraphFile.getNumberOfConnectedDoorStairs();
		
		for(int i = 0; i < numConnectedDoorStairs; i++) {
			
			String connectedDoorStairs = accessGraphFile.getConnectedDoorStairs(i+1);
			
			// Get both stairs and doors
			String[] array = connectedDoorStairs.split(", ");
			String stairs = array[0];
			String door = array[1];
			
			// Stairs
			// stairs_"numStairs"
			String[] arrayDoorStairs = stairs.split("_");
			int numStairs = Integer.valueOf(arrayDoorStairs[1]) + numPrevStairs;
			
			Stairs stairsConn = getStairs().get(numStairs-1);

			System.out.println("Connecting stairs: " + stairsConn.getVertex_label()); // Añadido por Nacho Palacio 2025-06-29
			// Añadido por Nacho Palacio 2025-04-18.
			stairsConn.setVertex_label(ElementIdMapper.convertToRangeId(stairsConn.getVertex_label(), ElementIdMapper.CATEGORY_STAIRS));
			
			// Door
			// door_"numDoorInRoom"_"numRoom"
			arrayDoorStairs = door.split("_");
			int numDoorInRoom = Integer.valueOf(arrayDoorStairs[1]);
			int numRoom = Integer.valueOf(arrayDoorStairs[2]) + numPrevRooms;
			
			Door doorConn = getRooms().get(numRoom-1).getDoors().get(numDoorInRoom-1);
			
			// Add connection (connectTo creates a connection for both doors)
			doorConn.addConnection(stairsConn); // or connectTo(), because addConnection in stairs must detect it's not of Stairs type and don't add it
		}
		
		// Add connections for connected stairs
		int numConnectedStairs = accessGraphFile.getNumberOfConnectedStairs();
		
		for(int i = 0; i < numConnectedStairs; i++) {
			
			String connectedStairs = accessGraphFile.getConnectedStairs(i+1);
			
			// Get both stairs and doors
			String[] array = connectedStairs.split(", ");
			String stairsF = array[0];
			String stairsS = array[1];
			
			// First stairs
			// stairs_"numStairs"
			String[] arrayStairs = stairsF.split("_");
			int numStairs = Integer.valueOf(arrayStairs[1]) + numPrevStairs;
			
			Stairs firstStairs = getStairs().get(numStairs-1);
			
			// Second stairs
			// stairs_"numStairs"
			arrayStairs = stairsS.split("_");
			numStairs = Integer.valueOf(arrayStairs[1]) + numPrevStairs;
			
			Stairs secondStairs = getStairs().get(numStairs-1);
			
			// Add connection (connectTo creates a connection for both doors)
			firstStairs.connectTo(secondStairs);
		}
		
		// Store all the properties
		accessGraphFile.storeProperties();
	}
	
	private void loadItemFile(int xDisplacement, int yDisplacement, int numExistingRooms, int numExistingItems) {
		
		// Get properties
		accessItemFile.loadProperties();
		
		// Set number of rooms
		int numberOfItems = accessItemFile.getNumberOfItems();
		
		// Set vertex dimensions
		setDRAWING_ICON_DIMENSION((int) accessItemFile.getVertexDimensionHeight());
		setDRAWING_ICON_DIMENSION((int) accessItemFile.getVertexDimensionWidth());
		
		// For each item
		for (int item = 0; item < numberOfItems; item++) {
			
			// Main attributes
			// Get item room
			int roomLabel = Integer.valueOf(accessItemFile.getItemRoom(item+1)); // -> Room associated in graph file
			roomLabel += numExistingRooms;
			Room r = getRoom(roomLabel);
			// Get item label
			int vertexLabel = (int) accessItemFile.getItemID(item+1); // ItemID will be the common vertex_label for all objects
			vertexLabel += numExistingItems;

			System.out.println("Creating item with label: " + vertexLabel + " in room: " + roomLabel); // Añadido por Nacho Palacio 2025-06-29
			// Añadido por Nacho Palacio 2025-04-18.
			long rangedVertexLabel = ElementIdMapper.convertToRangeId(vertexLabel, ElementIdMapper.CATEGORY_ITEM);

			// Get item label
			String itemXY = accessItemFile.getVertexXY(item+1);
			String[] array = itemXY.split(", ");
			double itemX = Double.parseDouble(array[0]);
			double itemY = Double.parseDouble(array[1]);
			
			// CREATE ITEM AND ADD IT TO MODEL
			
			// Añadido por Nacho Palacio 2025-04-18.
			Item i = new Item(r, rangedVertexLabel, new Point(itemX + xDisplacement, itemY + yDisplacement));

			addItem(i);
			
			// Item attributes
			// Get item label (located in vertexLabel field) and assign it to item
			String attribute = accessItemFile.getVertexLabel(item+1);
			if (attribute == null)
				System.out.println("Item doesn't have ITEM LABEL"); // Throw exception
			else
				i.setItemLabel(attribute);
			
			// Get item url and assign it to item
			attribute = accessItemFile.getVisitableVertexURL(item+1);
			if (attribute == null)
				System.out.println("Item doesn't have ICON URL");
			else
				i.setUrlIcon(accessItemFile.getVisitableVertexURL(item+1));
			
			// Get item title and assign it to item
			i.setTitle(accessItemFile.getItemTitle(item+1));
			// Get item nationality and assign it to item
			i.setNationality(accessItemFile.getItemNationality(item+1));
			// Get item beginDate and assign it to item
			i.setBeginDate(accessItemFile.getItemBeginDate(item+1));
			// Get item endDate and assign it to item
			i.setEndDate(accessItemFile.getItemEndDate(item+1));
			// Get item date and assign it to item
			i.setDate(accessItemFile.getItemDate(item+1));
			// Get item department and assign it to item
			//i.setDepartment(accessItemFile.getItemDepartment(item+1));
			// Get item width and height and assign it to item
			try {
				i.setHeight(Double.valueOf(accessItemFile.getItemHeight(item+1)));
				i.setWidth(Double.valueOf(accessItemFile.getItemWidth(item+1)));
			}
			catch (Exception e) {
				System.out.println(e);
				i.setHeight(0.0);
				i.setWidth(0.0);
			}
			
		}
		
	}

	/**
	 * Load files' data to model.
	 */
	public void load(int xDisplacement, int yDisplacement) {
		int numPrevRooms = getNumRooms();
		int numPrevItems = getNumItems();
		int numPrevDoors = getNumDoors();
		int numPrevStairs = getNumStairs();
		//int numPrevRoomSeparators = getNumRoomSeparators();
		
		
		// Load properties
		accessRoomFile.loadProperties();
		accessGraphFile.loadProperties();
		accessItemFile.loadProperties();
		
		// Load functions
		loadRoomFile(xDisplacement, yDisplacement);
		loadGraphFile(numPrevRooms, numPrevDoors, numPrevStairs);
		loadItemFile(xDisplacement, yDisplacement, numPrevRooms, numPrevItems);

		idMapper.resetCounters(rooms, items, doors, stairs, corners, roomSeparators); // Añadido por Nacho Palacio 2025-04-18 para coherencia ids externos e internos
	}
	// --------------------------------------------------------------------------------------------
	
	// -- SAVE visitableObjects ------------------------------------------------------------------------------------
	public void saveVisitableObjectsToFile(File f) {
		DataAccessItemFile accessVisitableItemFile = new DataAccessItemFile(f);
		
		accessVisitableItemFile.setNumberOfItems(visitableObjects.size());
		
		int i = 1;
		for (Map.Entry<String, Properties> entry : visitableObjects.entrySet()) {
			accessVisitableItemFile.setVisitableProperties(i, entry.getKey(), entry.getValue());
			i++;
		}
		
		accessVisitableItemFile.storeProperties();
	}
	
	// --------------------------------------------------------------------------------------------
	
	// -- LOAD visitableObjects ------------------------------------------------------------------------------------
	public void loadVisitableObjectsToFile(File f) {
		DataAccessItemFile accessVisitableItemFile = new DataAccessItemFile(f);
		accessVisitableItemFile.loadProperties();
		
		int numberOfVisitables = accessVisitableItemFile.getNumberOfItems();
		Map<String, Properties> visitablesStored = new HashMap<String, Properties>();
		
		for (int i = 0; i < numberOfVisitables; i++) {
			String visitableKey = accessVisitableItemFile.getVisitable(i+1);
			Properties p = accessVisitableItemFile.getAllVisitableProperties(visitableKey);
			
			visitablesStored.put(visitableKey, p);
		}
		
		setVisitableObjects(visitablesStored);
	}

	public void setItemVisitableProperties(String item, Item itemVisitable) {
		
		Properties p = getVisitableObjects().get(item);
		
		if (p != null) {
			
			// Item Label
	    	String value = p.getProperty(Literals.VERTEX_LABEL + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setItemLabel(value);
	    	
	    	// Item URL
	    	value = p.getProperty(Literals.VERTEX_URL + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setUrlIcon(value);
	    	
	    	// Item Nationality
	    	value = p.getProperty(Literals.ITEM_NATIONALITY + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setNationality(value);
	    	
	    	// Item BeginDate
	    	value = p.getProperty(Literals.ITEM_BEGINDATE + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setBeginDate(value);
	    	
	    	// Item EndDate
	    	value = p.getProperty(Literals.ITEM_ENDDATE + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setEndDate(value);
	    	
	    	// Item Date
	    	value = p.getProperty(Literals.ITEM_DATE + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setDate(value);
	    	
	    	// Item Department
	    	/*
	    	value = p.getProperty(Literals.ITEM_DEPARTMENT + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setDepartment(value);
	    	*/
	    	
	    	// Item Height
	    	value = p.getProperty(Literals.ITEM_HEIGHT + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setHeight(Double.valueOf(value));
	    	
	    	// Item Width
	    	value = p.getProperty(Literals.ITEM_WIDTH + item);
	    	if (value != null && !value.equals(""))
	    		itemVisitable.setWidth(Double.valueOf(value));
    	
		}
	}
	
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Get object's dimensions and check if it collides with a room's polygon.
	 * 
	 * @param doorLocation		Point
	 * @param polygon			Polygon (room's polygon)
	 * @return T/F
	 */
	public boolean objectIntesectsRoomPolygon(Point doorLocation, Polygon polygon) {
		// Get object's rectangle
		Rectangle2D rect = new Rectangle2D.Double(doorLocation.getX()-(getDRAWING_ICON_DIMENSION()/2), doorLocation.getY()-(getDRAWING_ICON_DIMENSION()/2),
				(double) getDRAWING_ICON_DIMENSION(), (double) getDRAWING_ICON_DIMENSION());
		
		return polygon.intersects(rect);
		
	}

	/* Añadido por Nacho Palacio 2025-04-18 para coherencia ids externos e internos */
	/**
	 * Obtiene el siguiente ID disponible para habitaciones
	 * @return ID único en el rango correcto para una nueva habitación
	 */
	public long getNextRoomId() {
		return idMapper.getNextRoomId();
	}

	/**
	 * Obtiene el siguiente ID disponible para ítems
	 * @return ID único en el rango correcto para un nuevo ítem
	 */
	public long getNextItemId() {
		return idMapper.getNextItemId();
	}

	/**
	 * Obtiene el siguiente ID disponible para puertas
	 * @return ID único en el rango correcto para una nueva puerta
	 */
	public long getNextDoorId() {
		return idMapper.getNextDoorId();
	}

	/**
	 * Obtiene el siguiente ID disponible para escaleras
	 * @return ID único en el rango correcto para unas nuevas escaleras
	 */
	public long getNextStairsId() {
		return idMapper.getNextStairsId();
	}

	/**
	 * Obtiene el siguiente ID disponible para esquinas
	 * @return ID único en el rango correcto para una nueva esquina
	 */
	public long getNextCornerId() {
		return idMapper.getNextCornerId();
	}

	/**
	 * Obtiene el siguiente ID disponible para separadores de habitaciones
	 * @return ID único en el rango correcto para un nuevo separador
	 */
	public long getNextSeparatorId() {
		return idMapper.getNextSeparatorId();
	}

	/**
	 * Reassign IDs when elements are erased. As they are contained in an ordered list, IDs can be reassigned in order.
	 */
	// public void reassignIDs() {
	// 	long id = 1;
	// 	for (Item i: this.getItems()) {
	// 		i.setVertex_label(id);
	// 		id++;
	// 	}
	// 	id=1;
	// 	for(Door d: this.getDoors()) {
	// 		d.setVertex_label(id);
	// 		id++;
	// 	}
	// 	id=1;
	// 	for (Stairs s: this.getStairs()) {
	// 		s.setVertex_label(id);
	// 		id++;
	// 	}
	// }

	/**
	 * Reassign IDs when elements are erased. As they are contained in an ordered list, IDs can be reassigned in order.
	 * Añadido por Nacho Palacio 2025-04-18 para coherencia ids externos e internos
	 */
	public void reassignIDs() {
		// Reiniciar el mapeador de IDs con las listas actuales
		idMapper.resetCounters(rooms, items, doors, stairs, corners, roomSeparators);
		
		// Reasignar IDs de habitaciones
		for (int i = 0; i < rooms.size(); i++) {
			rooms.get(i).setLabel(idMapper.getNextRoomId());
		}
		
		// Reasignar IDs de ítems
		for (int i = 0; i < items.size(); i++) {
			items.get(i).setVertex_label(idMapper.getNextItemId());
		}
		
		// Reasignar IDs de puertas
		for (int i = 0; i < doors.size(); i++) {
			doors.get(i).setVertex_label(idMapper.getNextDoorId());
		}
		
		// Reasignar IDs de escaleras
		for (int i = 0; i < stairs.size(); i++) {
			stairs.get(i).setVertex_label(idMapper.getNextStairsId());
		}
		
		// Reasignar IDs de esquinas persistentes
		for (int i = 0; i < corners.size(); i++) {
			corners.get(i).setVertex_label(idMapper.getNextCornerId());
		}
		
		// Reasignar IDs de separadores de habitaciones
		for (int i = 0; i < roomSeparators.size(); i++) {
			roomSeparators.get(i).setVertex_label(idMapper.getNextSeparatorId());
		}
	}
}
