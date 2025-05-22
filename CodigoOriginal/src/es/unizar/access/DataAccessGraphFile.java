package es.unizar.access;

import java.io.File;
import java.util.Random;

import es.unizar.gui.Configuration;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

/**
 * Access to the values of the properties stored in the graph files. Getters and setters.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public class DataAccessGraphFile extends DataAccess {

	public DataAccessGraphFile(File file) {
		super(file);
		if(file != null && file.getName().endsWith(".svg")) {
//			System.out.println("SVG");
			new SVGParserSimulation(this,file);
			loadProperties();
		}
	}

	//--------------------------------------------------------------------------------------------------------------------
	//-- GETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
	
	public int getNumberOfRoom() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_ROOM)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getRoom(int posRoom) {
		int rawId = Integer.valueOf(getPropertyValue(Literals.ROOM + posRoom)).intValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_ROOM);
	}

	public int getNumberOfItemsByRoom(int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_ITEMS_BY_ROOM + posRoom)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getItemOfRoom(int posItem, int posRoom) {
		String itemId = getPropertyValue(Literals.ITEM_OF_ROOM + posItem + "_" + posRoom);
		if (itemId == null || itemId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(itemId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_ITEM);
	}

	public int getNumberOfDoorsByRoom(int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_DOORS_BY_ROOM + posRoom)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getDoorOfRoom(int door, int room) {
		// String doorId = getPropertyValue(Literals.DOOR + door + "_" + room);
		String doorId = getPropertyValue(Literals.DOOR_OF_ROOM + door + "_" + room); // Modificado por Nacho Palacio 2025-05-02
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.parseLong(doorId);
		long internalId = ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
		// System.out.println("GraphFile: Convirtiendo Door ID externo " + rawId + " a interno " + internalId);
		return internalId;
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getDoorOfRoom(String door) {
		String doorId = getPropertyValue(door);
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(doorId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
	}

	public int getNumberOfConnectedDoor() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_CONNECTED_DOOR)).intValue();
	}

	public String getConnectedDoor(int posDoor) {
		return getPropertyValue(Literals.CONNECTED_DOOR + posDoor);
	}

	public int getNumberOfStairs() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_STAIRS_BY_MAP)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getStairsOfRoom(int pos) {
		String stairsId = getPropertyValue(Literals.STAIRS_OF_ROOM + pos);
		if (stairsId == null || stairsId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(stairsId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_STAIRS);
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getStairsOfRoom(String stairs) {
		String stairsId = getPropertyValue(stairs);
		if (stairsId == null || stairsId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(stairsId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_STAIRS);
	}

	public int getNumberOfConnectedDoorStairs() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_CONNECTED_DOOR_STAIRS)).intValue();
	}

	public String getConnectedDoorStairs(int posDoorStairs) {
		return getPropertyValue(Literals.CONNECTED_DOOR_STAIRS + posDoorStairs);
	}

	public int getNumberOfConnectedStairs() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_CONNECTED_STAIRS)).intValue();
	}

	public String getConnectedStairs(int posStairs) {
		return getPropertyValue(Literals.CONNECTED_STAIRS + posStairs);
	}

	public String getConnectedStairs(String stairs) {
		return getPropertyValue(stairs);
	}

	public long getRandomDoor() {
		int totalRoom = this.getNumberOfRoom();
		int posRoom = randInt(1, totalRoom);
		long door = -1;
		if (this.getRoomNumberSubrooms(posRoom) > 0) {
			int posSubroom = randInt(1, this.getRoomNumberSubrooms(posRoom));
			//posRoom = this.getSubroom(posSubroom, posRoom);
			int totalDoor = this.getNumberOfDoorsBySubroom(posSubroom, posRoom) + this.getNumberOfInvisibleDoorsBySubroom(posSubroom, posRoom);
			int posDoor = randInt(1, totalDoor);
			if (posDoor > this.getNumberOfDoorsBySubroom(posSubroom, posRoom)) {
				door = this.getInvisibleDoorOfSubroom(posDoor - this.getNumberOfDoorsBySubroom(posSubroom, posRoom), posSubroom, posRoom);
			}
			else {
				door = this.getDoorOfSubroom(posDoor, posSubroom, posRoom);
			}
		}
		else {
			int totalDoor = this.getNumberOfDoorsByRoom(posRoom);
			int posDoor = randInt(1, totalDoor);
			door = this.getDoorOfRoom(posDoor, posRoom);
		}
		
		return door;
	}

	public static int randInt(int min, int max) {
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		Random random = new Random(Configuration.simulation.getSeed());
		int randomNum = random.nextInt((max - min) + 1) + min;
		return randomNum;
	}
	
	public int getNumberOfRoomsAndSubrooms() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_ROOM_SUBROOMS)).intValue();
	}
	
	/**
     * 
     * @param roomPosition
     * @return		NUMBER OF SUBROOMS BY ROOM
     */
    public int getRoomNumberSubrooms(int roomPosition) {
    	return Integer.valueOf(getPropertyValue(Literals.NUMBER_SUBROOMS + roomPosition));
    }

	public int getSubroom(int posSubroom, int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.SUBROOM + posSubroom + "_" + posRoom)).intValue();
	}

	public int getNumberOfItemsBySubroom(int posSubroom, int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_ITEMS_BY_SUBROOM + posSubroom + "_" + posRoom)).intValue();
	}

	public long getItemOfSubroom(int posItem, int posSubroom, int posRoom) {
		return Long.valueOf(getPropertyValue(Literals.ITEM_OF_SUBROOM + posItem + "_" + posSubroom + "_" + posRoom)).longValue();
	}

	public int getNumberOfDoorsBySubroom(int posSubroom, int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_DOORS_BY_SUBROOM + posSubroom + "_" + posRoom)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getDoorOfSubroom(int posDoor, int posSubroom, int posRoom) {
		String doorId = getPropertyValue(Literals.DOOR_OF_SUBROOM + posDoor + "_" + posSubroom + "_" + posRoom);
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(doorId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getDoorOfSubroom(String doorOfSubroom) {
		String doorId = getPropertyValue(doorOfSubroom);
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(doorId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
	}
	
	// Invisible doors for graph file -> simulator
	public int getNumberOfInvisibleDoorsBySubroom(int posSubroom, int posRoom) {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_INVISIBLE_DOORS_BY_SUBROOM + posSubroom + "_" + posRoom)).intValue();
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getInvisibleDoorOfSubroom(int posInvisibleDoor, int posSubroom, int posRoom) {
		String doorId = getPropertyValue(Literals.INVISIBLE_DOOR_OF_SUBROOM + posInvisibleDoor + "_" + posSubroom + "_" + posRoom);
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(doorId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public long getInvisibleDoorOfSubroom(String invisibleDoorOfSubroom) {
		String doorId = getPropertyValue(invisibleDoorOfSubroom);
		if (doorId == null || doorId.isEmpty()) {
			return -1;
		}
		
		long rawId = Long.valueOf(doorId).longValue();
		return ElementIdMapper.convertToRangeId(rawId, ElementIdMapper.CATEGORY_DOOR);
	}
	
	public int getNumberOfConnectedInvisibleDoor() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_CONNECTED_INVISIBLE_DOOR)).intValue();
	}

	public String getConnectedInvisibleDoor(int posInvisibleDoor) {
		return getPropertyValue(Literals.CONNECTED_INVISIBLE_DOOR + posInvisibleDoor);
	}
	
	//--------------------------------------------------------------------------------------------------------------------
	//-- END GETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
	
	
	//--------------------------------------------------------------------------------------------------------------------
	//-- SETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
	
	public void setNumberOfRoom(int numberOfRoom) {
		setPropertyValue(Literals.NUMBER_ROOM, Integer.toString(numberOfRoom));
	}

	/* Añadido por Nacho Palacio 2025-04-17. */
	public void setRoom(int posRoom, long roomLabel) {
		setPropertyValue(Literals.ROOM + posRoom, Long.toString(roomLabel));
	}

	public void setNumberOfItemsByRoom(int posRoom, int numberOfItems) {
		setPropertyValue(Literals.NUMBER_ITEMS_BY_ROOM + posRoom, Integer.toString(numberOfItems));
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public void setItemOfRoom(int posItem, int posRoom, String itemLabel) {
		try {
			long id = Long.parseLong(itemLabel);
			if (ElementIdMapper.isInCorrectRange(id, ElementIdMapper.CATEGORY_ITEM)) {
				id = id - ElementIdMapper.ITEM_ID_START;
				itemLabel = String.valueOf(id);
			}
		} catch (NumberFormatException e) {
			// Don't do anything
		}
		
		setPropertyValue(Literals.ITEM_OF_ROOM + posItem + "_" + posRoom, itemLabel);
	}

	public void setNumberOfDoorsByRoom(int posRoom, int numberOfDoors) {
		setPropertyValue(Literals.NUMBER_DOORS_BY_ROOM + posRoom, Integer.toString(numberOfDoors));
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public void setDoorOfRoom(int door, int room, String doorId) {
		try {
			long id = Long.parseLong(doorId);
			if (ElementIdMapper.isInCorrectRange(id, ElementIdMapper.CATEGORY_DOOR)) {
				id = id - ElementIdMapper.DOOR_ID_START;
				doorId = String.valueOf(id);
			}
		} catch (NumberFormatException e) {
			// Don't do anything
		}
		
		setPropertyValue(Literals.DOOR + door + "_" + room, doorId);
	}

	public void setNumberOfConnectedDoor(int numberOfConnectedDoor) {
		setPropertyValue(Literals.NUMBER_CONNECTED_DOOR, Integer.toString(numberOfConnectedDoor));
	}

	public void setConnectedDoor(int posDoor, String door1Label, String door2Label) {
		setPropertyValue(Literals.CONNECTED_DOOR + posDoor, door1Label + ", " + door2Label);
	}

	public void setNumberOfStairs(int numberOfStairs) {
		setPropertyValue(Literals.NUMBER_STAIRS_BY_MAP, Integer.toString(numberOfStairs));
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public void setStairsOfRoom(int pos, String stairsLabel) {
		try {
			long id = Long.parseLong(stairsLabel);
			if (ElementIdMapper.isInCorrectRange(id, ElementIdMapper.CATEGORY_STAIRS)) {
				id = id - ElementIdMapper.STAIRS_ID_START;
				stairsLabel = String.valueOf(id);
			}
		} catch (NumberFormatException e) {
			// Don't do anything
		}
		
		setPropertyValue(Literals.STAIRS_OF_ROOM + pos, stairsLabel);
	}

	public void setNumberOfConnectedDoorStairs(int numberOfConnectedDoorStairs) {
		setPropertyValue(Literals.NUMBER_CONNECTED_DOOR_STAIRS, Integer.toString(numberOfConnectedDoorStairs));
	}

	public void setConnectedDoorStairs(int posDoorStairs, String stairsLabel, String doorLabel) {
		setPropertyValue(Literals.CONNECTED_DOOR_STAIRS + posDoorStairs, stairsLabel + ", " + doorLabel);
	}

	public void setNumberOfConnectedStairs(int numberOfConnectedStairs) {
		setPropertyValue(Literals.NUMBER_CONNECTED_STAIRS, Integer.toString(numberOfConnectedStairs));
	}

	public void setConnectedStairs(int posStairs, String stairs1Label, String stairs2Label) {
		setPropertyValue(Literals.CONNECTED_STAIRS + posStairs, stairs1Label + ", " + stairs2Label);
	}

	/*
	public String getConnectedStairs(String stairs) {
		return getPropertyValue(stairs);
	}
	*/
	
	public void setNumberOfRoomsAndSubrooms(int numberRoomsSubrooms) {
		setPropertyValue(Literals.NUMBER_ROOM_SUBROOMS, Integer.toString(numberRoomsSubrooms));
	}
	
	/**
     * 
     * @param numberRoomSubrooms		PER ROOM
     */
    public void setRoomNumberSubrooms(int roomPosition, int numberRoomSubrooms) {
    	setPropertyValue(Literals.NUMBER_SUBROOMS + roomPosition, Integer.toString(numberRoomSubrooms));
    }

	public void setSubroom(int posSubroom, int posRoom, int subroomLabel) {
		setPropertyValue(Literals.SUBROOM + posSubroom + "_" + posRoom, Integer.toString(subroomLabel));
	}

	public void setNumberOfItemsBySubroom(int posSubroom, int posRoom, int numberItemsSubroom) {
		setPropertyValue(Literals.NUMBER_ITEMS_BY_SUBROOM + posSubroom + "_" + posRoom, Integer.toString(numberItemsSubroom));
	}

	public void setItemOfSubroom(int posItem, int posSubroom, int posRoom, String itemLabel) {
		setPropertyValue(Literals.ITEM_OF_SUBROOM + posItem + "_" + posSubroom + "_" + posRoom, itemLabel);
	}

	public void setNumberOfDoorsBySubroom(int posSubroom, int posRoom, int numberDoorsSubroom) {
		setPropertyValue(Literals.NUMBER_DOORS_BY_SUBROOM + posSubroom + "_" + posRoom, Integer.toString(numberDoorsSubroom));
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public void setDoorOfSubroom(int posDoor, int posSubroom, int posRoom, String doorLabel) {
		try {
			long id = Long.parseLong(doorLabel);
			if (ElementIdMapper.isInCorrectRange(id, ElementIdMapper.CATEGORY_DOOR)) {
				id = id - ElementIdMapper.DOOR_ID_START;
				doorLabel = String.valueOf(id);
			}
		} catch (NumberFormatException e) {
			// Don't do anything
		}
		
		setPropertyValue(Literals.DOOR_OF_SUBROOM + posDoor + "_" + posSubroom + "_" + posRoom, doorLabel);
	}
	
	// Invisible doors for graph file -> simulator
	public void setNumberOfInvisibleDoorsBySubroom(int posSubroom, int posRoom, int numberInvisibleDoorsSubroom) {
		setPropertyValue(Literals.NUMBER_INVISIBLE_DOORS_BY_SUBROOM + posSubroom + "_" + posRoom, Integer.toString(numberInvisibleDoorsSubroom));
	}

	// Modificado por Nacho Palacio 2025-04-22.
	public void setInvisibleDoorOfSubroom(int posDoor, int posSubroom, int posRoom, String invisibleDoorLabel) {
		try {
			long id = Long.parseLong(invisibleDoorLabel);
			if (ElementIdMapper.isInCorrectRange(id, ElementIdMapper.CATEGORY_DOOR)) {
				id = id - ElementIdMapper.DOOR_ID_START;
				invisibleDoorLabel = String.valueOf(id);
			}
		} catch (NumberFormatException e) {
			// Don't do anything
		}
		
		setPropertyValue(Literals.INVISIBLE_DOOR_OF_SUBROOM + posDoor + "_" + posSubroom + "_" + posRoom, invisibleDoorLabel);
	}
	
	public void setNumberOfConnectedInvisibleDoor(int numberOfConnectedInvisibleDoor) {
		setPropertyValue(Literals.NUMBER_CONNECTED_INVISIBLE_DOOR, Integer.toString(numberOfConnectedInvisibleDoor));
	}

	public void setConnectedInvisibleDoor(int posInvisibleDoor, String invisibleDoor1Label, String invisibleDoor2Label) {
		setPropertyValue(Literals.CONNECTED_INVISIBLE_DOOR + posInvisibleDoor, invisibleDoor1Label + ", " + invisibleDoor2Label);
	}
	
	//--------------------------------------------------------------------------------------------------------------------
	//-- END SETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
	
	// Simulator operations
	
	/**
	 * 
	 * @param posRoom
	 * @return
	 */
	public int getNumDoorsByRoom(int posRoom) {
		if (posRoom <= getNumberOfRoom()) {
			return getNumberOfDoorsByRoom(posRoom);
		}
		else {
			int sr = -1, r = -1;
			
			// Find subroom
			for (int room = 1; room <= getNumberOfRoom(); room++) {
				for (int subroom = 1; subroom <= getRoomNumberSubrooms(room); subroom++) {
					if (posRoom == getSubroom(subroom, room)) {
						sr = subroom;
						r = room;
						break;
					}
				}
				if (sr > 0 || r > 0)
					break;
			}
			
			int numDoors = getNumberOfDoorsBySubroom(sr, r) + getNumberOfInvisibleDoorsBySubroom(sr, r);
			// System.out.println("Room " + r + ", subroom " + sr + "; numDoors = " + numDoors);
			return numDoors;
		}
	}
	
	/**
	 * 
	 * @param posDoor
	 * @param posRoom
	 * @return
	 */
	public long getDoorOfRoomWithIndex(int posDoor, int posRoom) {
		
		if (posRoom <= getNumberOfRoom()) {
			return getDoorOfRoom(posDoor, posRoom);
		}
		else {
			int sr = -1, r = -1;
			
			// Find subroom
			for (int room = 1; room <= getNumberOfRoom(); room++) {
				for (int subroom = 1; subroom <= getRoomNumberSubrooms(room); subroom++) {
					if (posRoom == getSubroom(subroom, room)) {
						sr = subroom;
						r = room;
						break;
					}
				}
				if (sr > 0 || r > 0)
					break;
			}
			
			int numDoors = getNumberOfDoorsBySubroom(sr, r);
			// If posDoor > numDoors -> It's an invisible door
			if (posDoor > numDoors) {
				posDoor -= numDoors;
				return getInvisibleDoorOfSubroom(posDoor, sr, r);
			}
			else {
				return getDoorOfSubroom(posDoor, sr, r);
			}
		}
	}

	
}
