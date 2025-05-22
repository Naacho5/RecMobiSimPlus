package es.unizar.editor.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import es.unizar.util.ElementIdMapper;

public class Room extends RoomPolygon{

	private String roomName;
	private List<Door> doors;
	private List<Stairs> stairs;
	private List<Item> items;
	private List<RoomSeparator> roomSeparators;
	private List<RoomPolygon> subRooms;
	
	/**
	 * Room must be created with label and list of corners.
	 * 
	 * @param label
	 * @param corners
	 */
	public Room(long label, List<Corner> corners) { // Modificado por Nacho Palacio 2025-04-18. Antes: public Room(int label, List<Corner> corners) {
		/* Añadido por Nacho Palacio 2025-04-17. */
		super(ElementIdMapper.convertToRangeId(label, ElementIdMapper.CATEGORY_ROOM));
		
		this.roomName = null;
		this.doors = new LinkedList<Door>();
		this.stairs = new LinkedList<Stairs>();
		this.items = new LinkedList<Item>();
		this.roomSeparators = new LinkedList<RoomSeparator>();
		this.subRooms = new LinkedList<RoomPolygon>();
		
		// Add corners to room
		this.corners.addAll(corners);
		
		// Link all room's corners to this room and add them to polygon
		for(Corner c: this.corners) {
			c.setRoom(this);
			this.polygon.addPoint((int) c.getVertex_xy().getX(), (int) c.getVertex_xy().getY());
		}

	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public List<Door> getDoors() {
		return doors;
	}
	
	//
	public Door getDoor(long label) {
		for(Door d: getDoors()) {
			if (d.getVertex_label() == label) {
				return d;
			}
		}
		
		return null;
	}
	//

	public void setDoors(List<Door> doors) {
		this.doors = doors;
	}
	
	public int getNumDoors() {
		return this.doors.size();
	}
	
	public boolean addDoor(Door d) {
		return this.doors.add(d);
	}
	
	public boolean removeDoor(Door d) {
		return this.doors.remove(d);
	}

	public List<Stairs> getStairs() {
		return stairs;
	}

	public void setStairs(List<Stairs> stairs) {
		this.stairs = stairs;
	}
	
	public int getNumStairs() {
		return this.stairs.size();
	}
	
	public boolean addStairs(Stairs s) {
		return this.stairs.add(s);
	}
	
	public boolean removeStairs(Stairs s) {
		return this.stairs.remove(s);
	}
	
	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
	
	public int getNumItems() {
		return this.items.size();
	}
	
	public boolean addItem(Item i) {
		return this.items.add(i);
	}
	
	public boolean removeItem(Item i) {
		return this.items.remove(i);
	}

	public List<RoomSeparator> getRoomSeparators() {
		return roomSeparators;
	}

	public void setRoomSeparators(List<RoomSeparator> roomSeparators) {
		this.roomSeparators = roomSeparators;
	}
	
	public int getNumRoomSeparators() {
		return this.roomSeparators.size();
	}
	
	public boolean addRoomSeparator(RoomSeparator rs) {
		boolean added = this.roomSeparators.add(rs);
		if (added)
			refactorSubRooms(rs);
		
		return added;
	}
	
	public boolean removeRoomSeparator(RoomSeparator rs) {
		boolean deleted = this.roomSeparators.remove(rs);
		if (deleted)
			refactorAllSubRooms();
		
		return deleted;
	}

	public List<RoomPolygon> getSubRooms() {
		return subRooms;
	}

	public void setSubRooms(List<RoomPolygon> subRooms) {
		this.subRooms = subRooms;
	}
	
	@Override
	public void refreshPolygon() {
		super.refreshPolygon();
		for (RoomPolygon subroom: getSubRooms()) {
			subroom.refreshPolygon();
		}
	}
	
	public int getNumSubRooms() {
		return this.subRooms.size();
	}
	
	public boolean addSubRoom(RoomPolygon rp) {
		return this.subRooms.add(rp);
	}
	
	public boolean removeSubRoom(RoomPolygon rp) {
		return this.subRooms.remove(rp);
	}
	
	
	public void refactorSubRooms(RoomSeparator rs) {
		// Get subroom to split
		RoomPolygon subroom = getSubRoom(rs);
		// Divide subroom
		divideSubRoom(rs, subroom);
	}
	
	private void refactorAllSubRooms() {
		this.subRooms.clear();
		for (RoomSeparator rs: getRoomSeparators()) {
			refactorSubRooms(rs);
		}
	}

	/**
	 * Get the common subroom from the two points of the RoomSeparator (one corner can be common to other RoomSeparator, but not the two of them).
	 * 
	 * @param rs		RoomSeparator
	 * @return			RoomPolygon (the common subroom)
	 */
	private RoomPolygon getSubRoom(RoomSeparator rs) {
		Set<RoomPolygon> subroomsC1 = getSubRoomsFromCorner(rs.getC1());
		subroomsC1.retainAll(getSubRoomsFromCorner(rs.getC2()));
		
		return subroomsC1.iterator().next();
	}
	
	/**
	 * Creates two subrooms from big room/subroom. Corners are always ordered (because if not, walls wouldn't be correct).
	 * That's why we can use the following algorithm:
	 * 
	 * While there are corners:
	 * - Assign all corners to a subroom
	 * - When a corner of the roomSeparator is found, add it to both subrooms and switch subroom to assign corners
	 * 
	 * @param rs			RoomSeparator
	 * @param subroom		"Big" room/subroom
	 */
	private void divideSubRoom(RoomSeparator rs, RoomPolygon subroom) {
		boolean assignFirst = true;
		
		List<Corner> firstSubroomCorners = new LinkedList<Corner>();
		List<Corner> secondSubroomCorners = new LinkedList<Corner>();
		
		// Algorithm
		// For all corners in room/subroom
		for (Corner c: subroom.getCorners()) {
			// If corner is one of the room separator corners
			if (c.equals(rs.getC1()) || c.equals(rs.getC2())) {
				// Add corner to both subrooms and switch subroom
				firstSubroomCorners.add(c);
				secondSubroomCorners.add(c);
				assignFirst = !assignFirst;
			}
			// If it's not a room separator corner, add it to the current assigned subroom
			else {
				if (assignFirst) {
					firstSubroomCorners.add(c);
				}
				else {
					secondSubroomCorners.add(c);
				}
			}
		}
		
		// Create subrooms and add corners
		RoomPolygon firstSubroom = new RoomPolygon(this.getSubRooms().size()+1);
		RoomPolygon secondSubroom = new RoomPolygon(this.getSubRooms().size()+2);
		
		firstSubroom.setCorners(firstSubroomCorners);
		secondSubroom.setCorners(secondSubroomCorners);
		
		// Delete "big" room/subroom
		removeSubRoom(subroom);
		
		// Add the 2 resulting subrooms
		addSubRoom(firstSubroom);
		addSubRoom(secondSubroom);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((doors == null) ? 0 : doors.hashCode());
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		result = prime * result + ((stairs == null) ? 0 : stairs.hashCode());
		result = prime * result + ((subRooms == null) ? 0 : subRooms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Room other = (Room) obj;
		if (doors == null) {
			if (other.doors != null)
				return false;
		} else if (!doors.equals(other.doors))
			return false;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		if (stairs == null) {
			if (other.stairs != null)
				return false;
		} else if (!stairs.equals(other.stairs))
			return false;
		if (subRooms == null) {
			if (other.subRooms != null)
				return false;
		} else if (!subRooms.equals(other.subRooms))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "[doors=" + doors + ", stairs=" + stairs + ", items=" + items + "]";
	}
	
	/**
	 * Check if any of the subrooms is concave
	 * 
	 * @param p		Point to check subrooms
	 * @return		T/F
	 */
	@Override
	public boolean checkConcave() {
		
		boolean concave = false;
		
		if (!getSubRooms().isEmpty()) {
			for (RoomPolygon rp: getSubRooms()) {
				if (rp.checkConcave()) { // If any subroom is concave, return true
						concave = true;
						break;
				}
			}
		}
		else {
			concave = super.checkConcave();
		}
		
		return concave;
	}
	
	/**
	 * Get in which subroom(s) is contained the specified point.
	 * 
	 * @param p		Point for checking
	 * @return		Set of subroom(s)
	 */
	public Set<RoomPolygon> getSubRoomsFromCorner(Corner c) {
		
		Set<RoomPolygon> subrooms = new HashSet<RoomPolygon>();
		
		if (!getSubRooms().isEmpty()) {
			for(RoomPolygon rp: getSubRooms()) {
				if (rp.getCorners().contains(c))
					subrooms.add(rp);
			}
		}
		else {
			subrooms.add(this);
		}
		
		return subrooms;
	}

	/**
	 * Check if two corners are consecutive in room.
	 * 
	 * @param c1		First corner to check
	 * @param c2		Second corner to check
	 * @return			T/F
	 */
	public boolean areConsecutive(Corner c1, Corner c2) {
		boolean consecutive = false;
		
		int index = getCorners().indexOf(c1);
		// If point is last point
		if (index == this.getCorners().size()-1) {
			consecutive = (c2.equals(this.getCorners().get(index-1))
					|| c2.equals(this.getCorners().get(0)));
		}
		// If point is first point
		else if (index == 0) {
			consecutive = (c2.equals(this.getCorners().get(this.getCorners().size()-1))
					|| c2.equals(this.getCorners().get(index+1)));
		}
		else {
			consecutive = (c2.equals(this.getCorners().get(index-1))
					|| c2.equals(this.getCorners().get(index+1)));
		}
		
		return consecutive;
	}
	
}
