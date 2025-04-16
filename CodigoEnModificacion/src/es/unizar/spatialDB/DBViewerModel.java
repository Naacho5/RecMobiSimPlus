package es.unizar.spatialDB;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;

public class DBViewerModel {
	
	private List<DBRoom> rooms;
	private List<RoomSeparator> separators;
	private List<Item> items;
	private List<Drawable> drawables; //doors and stairs
	private List<Drawable> roomNumbers;
	private double ZOOM;
	private int DRAWING_ICON_DIMENSION = 10;
	private double pixelRepresentsInMeters;
	public static int MIN_MAP_W = 20;
	public static int MIN_MAP_H = 20;
	private int MAP_W = MIN_MAP_W;
	private int MAP_H = MIN_MAP_H;
	
	private Drawable objectSelected = null;
	public Point diffPoint;
	private Drawable previousObjectSelected = null;
	
	private int roomSelectedLabel = 0;
	
	public DBViewerModel() {
		rooms = new ArrayList<DBRoom>();
		separators = new ArrayList<RoomSeparator>();
		items = new ArrayList<Item>();
		drawables = new ArrayList<Drawable>();
		roomNumbers = new ArrayList<Drawable>();
	}

	public void clear() {
		rooms.clear();
		separators.clear();
		items.clear();
		drawables.clear();
		roomNumbers.clear();
		objectSelected = null;
		previousObjectSelected = null;
		roomSelectedLabel = 0;
		diffPoint = null;
	}
	
	public List<DBRoom> getRooms() {
		return rooms;
	}
	public void setRooms(List<DBRoom> rooms) {
		this.rooms = rooms;
	}
	public void addRoom(DBRoom room) {
		rooms.add(room);
	}
	public DBRoom findRoom(int label) {
		for(DBRoom r : rooms) {
			if(Integer.parseInt(r.getLabel()) == label) {
				return r;
			}
		}
		return null;
	}
	public List<RoomSeparator> getSeparators() {
		return separators;
	}
	public void setSeparators(List<RoomSeparator> separators) {
		this.separators = separators;
	}
	public void addSeparator(RoomSeparator sep) {
		separators.add(sep);
	}
	public List<Item> getItems() {
		return items;
	}
	public void setItems(List<Item> items) {
		this.items = items;
	}
	public void addItem(Item i) {
		items.add(i);
	}
	public List<Drawable> getDrawables() {
		return drawables;
	}
	public void setDrawables(List<Drawable> drawables) {
		this.drawables = drawables;
	}
	public void addDrawable(Drawable dr) {
		drawables.add(dr);
	}
	public Drawable getDrawable(int label, String type) {
		for(Drawable d : drawables) {
			if(label == d.getVertex_label() && ((type.equals("DOOR") && d instanceof Door) || (type.equals("STAIRS") && d instanceof Stairs))) {
				return d;
			}
		}
		return null;
	}
	public double getZOOM() {
		return ZOOM;
	}
	public void setZOOM(double zoom) {
		ZOOM = zoom;
	}
	public int getDRAWING_ICON_DIMENSION() {
		return DRAWING_ICON_DIMENSION;
	}
	public void setDRAWING_ICON_DIMENSION(int dRAWING_ICON_DIMENSION) {
		DRAWING_ICON_DIMENSION = dRAWING_ICON_DIMENSION;
	}
	public double getPixelRepresentsInMeters() {
		return pixelRepresentsInMeters;
	}
	public void setPixelRepresentsInMeters(double pixelRepresentsInMeters) {
		this.pixelRepresentsInMeters = pixelRepresentsInMeters;
	}
	public static int getMIN_MAP_W() {
		return MIN_MAP_W;
	}
	public static void setMIN_MAP_W(int mIN_MAP_W) {
		MIN_MAP_W = mIN_MAP_W;
	}
	public static int getMIN_MAP_H() {
		return MIN_MAP_H;
	}
	public static void setMIN_MAP_H(int mIN_MAP_H) {
		MIN_MAP_H = mIN_MAP_H;
	}
	public int getMAP_W() {
		return MAP_W;
	}
	public void setMAP_W(int mAP_W) {
		MAP_W = mAP_W;
	}
	public int getMAP_H() {
		return MAP_H;
	}
	public void setMAP_H(int mAP_H) {
		MAP_H = mAP_H;
	}
	public Drawable getObjectSelected() {
		return objectSelected;
	}
	public void setObjectSelected(Drawable objectSelected) {
		this.objectSelected = objectSelected;
	}
	public Point getDiffPoint() {
		return diffPoint;
	}
	public void setDiffPoint(Point diffPoint) {
		this.diffPoint = diffPoint;
	}
	public List<Drawable> getRoomNumbers() {
		return roomNumbers;
	}
	public void setRoomNumbers(List<Drawable> roomNumbers) {
		this.roomNumbers = roomNumbers;
	}
	public void addRoomNumber(Drawable dr) {
		roomNumbers.add(dr);
	}
	public Drawable getPreviousObjectSelected() {
		return previousObjectSelected;
	}
	public void setPreviousObjectSelected(Drawable previousObjectSelected) {
		this.previousObjectSelected = previousObjectSelected;
	}
	public int getRoomSelectedLabel() {
		return roomSelectedLabel;
	}
	public void setRoomSelectedLabel(int roomSelectedLabel) {
		this.roomSelectedLabel = roomSelectedLabel;
	}
		
}
