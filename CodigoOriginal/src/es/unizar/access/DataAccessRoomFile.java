package es.unizar.access;

import java.io.File;

import es.unizar.util.Literals;

/**
 * Access to the values of the properties stored in the room files. Getters and setters.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class DataAccessRoomFile extends DataAccess {

    public DataAccessRoomFile(File file) {
        super(file);
        if(file != null && file.getName().endsWith(".svg")) {
			new SVGParserSimulation(this,file);
			loadProperties();
		}
    }

	//--------------------------------------------------------------------------------------------------------------------
	//-- GETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
	
    public String getName() {
        return getPropertyValue(Literals.NAME);
    }
    
    public int getMapWidth() {
    	return Integer.valueOf(getPropertyValue(Literals.MAP_WIDTH));
    }
    
    public int getMapHeight() {
    	return Integer.valueOf(getPropertyValue(Literals.MAP_HEIGHT));
    }
    
    public double getMapPixelRepresentsMeters() {
    	return Double.valueOf(getPropertyValue(Literals.MAP_PIXEL_REPRESENTS_METERS));
    }

    public int getNumberOfRoom() {
        return Integer.valueOf(getPropertyValue(Literals.NUMBER_ROOM));
    }

    public String getRoomLabel(int roomPosition) {
        return getPropertyValue(Literals.LABEL + roomPosition);
    }

    public int getRoomNumberCorner(int roomPosition) {
    	return Integer.valueOf(getPropertyValue(Literals.NUMBER_CORNER + roomPosition));
    }
    
    public String getRoomCornerXY(int cornerPosition, int roomPosition) {
        return getPropertyValue(Literals.CORNER + cornerPosition + "_" + roomPosition);
    }

    public int getRoomNumberDoor(int roomPosition) {
        return Integer.valueOf(getPropertyValue(Literals.NUMBER_DOOR + roomPosition));
    }

    public String getRoomDoorXY(int doorPosition, int roomPosition) {
        return getPropertyValue(Literals.DOOR + doorPosition + "_" + roomPosition);
    }

    public int getNumberStairs() {
        return Integer.valueOf(getPropertyValue(Literals.NUMBER_STAIRS));
    }

    public String getStairs(int position) {
        return getPropertyValue(Literals.STAIRS + position);
    }

    /**
     * INVISIBLE DOORS
     */
    
    public int getRoomNumberInvisibleDoor(int roomPosition) {
        return Integer.valueOf(getPropertyValue(Literals.ROOM_NUMBER_INVISIBLE_DOOR + roomPosition));
    }

    public String getRoomInvisibleDoorXY(int invisibleDoorPosition, int roomPosition) {
        return getPropertyValue(Literals.ROOM_INVISIBLE_DOOR + invisibleDoorPosition + "_" + roomPosition);
    }
    
    /**
     * ROOM SEPARATORS
     */
    public int getRoomNumberRoomSeparators(int roomPosition) {
    	return Integer.valueOf(getPropertyValue(Literals.ROOM_NUMBER_ROOMSEPARATORS + roomPosition));
    }
    
    public String getRoomRoomseparatorCorners(int roomSeparator, int roomPosition) {
        return getPropertyValue(Literals.ROOM_ROOMSEPARATOR_CORNERS + roomSeparator + "_" + roomPosition);
    }
    
    //--------------------------------------------------------------------------------------------------------------------
	//-- END GETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
	
	
	//--------------------------------------------------------------------------------------------------------------------
	//-- SETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
	
    public void setName(String name) {
        setPropertyValue(Literals.NAME, name);
    }
    
    public void setMapWidth(int width) {
    	setPropertyValue(Literals.MAP_WIDTH, Integer.toString(width));
    }
    
    public void setMapHeight(int height) {
    	setPropertyValue(Literals.MAP_HEIGHT, Integer.toString(height));
    }
    
    public void setMapPixelRepresentsMeters(double pixelRepresentsMeters) {
    	setPropertyValue(Literals.MAP_PIXEL_REPRESENTS_METERS, Double.toString(pixelRepresentsMeters));
    }

    public void setNumberOfRoom(int numberOfRooms) {
        setPropertyValue(Literals.NUMBER_ROOM, Integer.toString(numberOfRooms));
    }

    /* Añadido por Nacho Palacio 2025-04-17. */
    public void setRoomLabel(int roomPosition, long roomLabel) {
        setPropertyValue(Literals.LABEL + roomPosition, Long.toString(roomLabel));
    }
    
    public void setRoomNumberCorner(int roomPosition, int numberOfCorners) {
    	setPropertyValue(Literals.NUMBER_CORNER + roomPosition, Integer.toString(numberOfCorners));
    }

    public void setRoomCorner(int cornerPosition, int roomPosition, String cornerLocation) {
        setPropertyValue(Literals.CORNER + cornerPosition + "_" + roomPosition, cornerLocation);
    }

    public void setRoomNumberDoor(int roomPosition, int numberOfDoors) {
        setPropertyValue(Literals.NUMBER_DOOR + roomPosition, Integer.toString(numberOfDoors));
    }

    public void setRoomDoorXY(int doorPosition, int roomPosition, String doorLocation) {
        setPropertyValue(Literals.DOOR + doorPosition + "_" + roomPosition, doorLocation);
    }

    public void setNumberStairs(int numberOfStairs) {
        setPropertyValue(Literals.NUMBER_STAIRS, Integer.toString(numberOfStairs));
    }

    public void setStairs(int position, String stairsLocation) {
        setPropertyValue(Literals.STAIRS + position, stairsLocation);
    }

    /**
     * INVISIBLE DOORS
     */
    
    public void setRoomNumberInvisibleDoor(int roomPosition, int numberInvisibleDoor) {
        setPropertyValue(Literals.ROOM_NUMBER_INVISIBLE_DOOR + roomPosition, Integer.toString(numberInvisibleDoor));
    }

    public void setRoomInvisibleDoorXY(int invisibleDoorPosition, int roomPosition, String doorLocation) {
        setPropertyValue(Literals.ROOM_INVISIBLE_DOOR + invisibleDoorPosition + "_" + roomPosition, doorLocation);
    }
    
    /**
     * ROOM SEPARATORS
     */
    
    public void setRoomNumberRoomSeparators(int roomPosition, int numRoomSeparators) {
    	setPropertyValue(Literals.ROOM_NUMBER_ROOMSEPARATORS + roomPosition, Integer.toString(numRoomSeparators));
    }
    
    public void setRoomRoomseparatorCorners(int roomSeparator, int roomPosition, String roomSeparatorCorners) {
        setPropertyValue(Literals.ROOM_ROOMSEPARATOR_CORNERS + roomSeparator + "_" + roomPosition, roomSeparatorCorners);
    }
    
	//--------------------------------------------------------------------------------------------------------------------
	//-- END SETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
}
