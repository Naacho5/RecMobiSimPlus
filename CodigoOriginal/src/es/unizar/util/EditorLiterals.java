package es.unizar.util;

import java.io.File;
import java.text.DecimalFormat;
import java.math.RoundingMode;

public class EditorLiterals {
	
	/**
	 * Format for decimal numbers -> Two decimal numbers
	 */
	public static DecimalFormat decimalFormat = new DecimalFormat("#.##");
	
	public static final String PATH_EDITOR = Literals.PATH + "editor" + File.separator;
	public static final String PATH_VISITABLE_ITEMS = Literals.PATH + "visitableItems" + File.separator;
	
	/**
	 * Main EditorView areas. Elements in this areas named after this prefixes
	 */
	public static final String TOOLS_SECTION = "Tools";
	public static final String ICONS_SECTION = "Icons";
	public static final String MAP_SETTINGS_SECTION = "MapSettings";
	public static final String VISITABLE_ICONS_TOOLS = "VisitableIconsTools";
	public static final String SEPARATOR = "_";
	
	/**
	 * Tools (buttons) -> EditorView Tools area.
	 */
	public static final String PENCIL = TOOLS_SECTION + SEPARATOR + "Pencil";
	public static final String ERASER = TOOLS_SECTION + SEPARATOR + "Eraser";
	public static final String CURSOR = TOOLS_SECTION + SEPARATOR + "Cursor";
	public static final String MOVER = TOOLS_SECTION + SEPARATOR + "Mover";
	
	/**
	 * Icons (buttons) -> EditorView Icons area.
	 */
	public static final String CORNER = ICONS_SECTION + SEPARATOR + "Corner";
	public static final String DOOR = ICONS_SECTION + SEPARATOR + "Door";
	public static final String STAIRS = ICONS_SECTION + SEPARATOR + "Stairs";
	public static final String ROOMSEPARATOR = ICONS_SECTION + SEPARATOR + "RoomSeparator";
	public static final String VISITABLE = ICONS_SECTION + SEPARATOR + "VisitableIcon";
	
	/**
	 * Icons (buttons) -> EditorView Icons area.
	 */
	public static final String MAP_NAME = MAP_SETTINGS_SECTION + SEPARATOR + "Name";
	public static final String MAP_FLOOR = MAP_SETTINGS_SECTION + SEPARATOR + "Floor";
	public static final String MAP_WIDTH = MAP_SETTINGS_SECTION + SEPARATOR + "Width";
	public static final String MAP_HEIGHT = MAP_SETTINGS_SECTION + SEPARATOR + "Height";
	public static final String MAP_PIXEL_REPRESENTS_IN_METERS = MAP_SETTINGS_SECTION + SEPARATOR + "1PixelRepresentsInMeters";
	public static final String MAP_ZOOM = MAP_SETTINGS_SECTION + SEPARATOR + "VisitableIcon";
	public static final String MAP_ICONDIMENSIONS = MAP_SETTINGS_SECTION + SEPARATOR + "IconDimensions";
	
	/**
	 * 
	 */
	public static final String ADD_VISITABLE = VISITABLE_ICONS_TOOLS + SEPARATOR + "AddVisitable";
	public static final String DELETE_VISITABLE = VISITABLE_ICONS_TOOLS + SEPARATOR + "DeleteVisitable";
	public static final String EDIT_VISITABLE = VISITABLE_ICONS_TOOLS + SEPARATOR + "EditVisitable";
	public static final String SAVE_VISITABLE = VISITABLE_ICONS_TOOLS + SEPARATOR + "SaveVisitable";
	public static final String LOAD_VISITABLE = VISITABLE_ICONS_TOOLS + SEPARATOR + "LoadVisitable";
	
	/**
	 * Object properties -> Double click window.
	 */
	public static final String OBJECT_PROPERTIES = "ObjectProperties";
	public static final String POSITION_PIXEL_X = OBJECT_PROPERTIES + SEPARATOR + "PositionPixelX";
	public static final String POSITION_PIXEL_Y = OBJECT_PROPERTIES + SEPARATOR + "PositionPixelY";
	public static final String POSITION_METERS_X = OBJECT_PROPERTIES + SEPARATOR + "PositionMetersX";
	public static final String POSITION_METERS_Y = OBJECT_PROPERTIES + SEPARATOR + "PositionMetersY";
	
	
	/**
	 * Default map (model) needed variables
	 */
	public static final String DEFAULT_MAP_NAME = "MAP";
	public static final String DEFAULT_FLOOR_NAME = "1";

}
