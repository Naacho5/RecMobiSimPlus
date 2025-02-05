package es.unizar.editor.model;

import es.unizar.util.Literals;

public class Drawable {
	
	private Room room; // IN TRANSLATION TO "item_floorX" -> room.ID
	private long vertex_label; // Object's value
	private Point vertex_xy; // IN TRANSLATION TO "item_floorX" -> vertex_xy.toString()
	private String urlIcon;
	
	/**
	 * Constructor of drawable item must have compulsory data:
	 * @param iD
	 * @param room
	 * @param vertex_label
	 * @param vertex_xy
	 */
	public Drawable(Room room, long vertex_label, Point vertex_xy) {
		super();
		this.room = room;
		this.vertex_label = vertex_label;
		this.vertex_xy = vertex_xy;
		this.urlIcon = Literals.LOGO_PATH;
	}
	
	public Drawable(Room room, long vertex_label, Point vertex_xy, String url) {
		super();
		this.room = room;
		this.vertex_label = vertex_label;
		this.vertex_xy = vertex_xy;
		this.urlIcon = url;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public long getVertex_label() {
		return vertex_label;
	}

	public void setVertex_label(long vertex_label) {
		this.vertex_label = vertex_label;
	}

	public Point getVertex_xy() {
		return vertex_xy;
	}

	public void setVertex_xy(Point vertex_xy) {
		this.vertex_xy = vertex_xy;
	}
	
	public Point getVertex_xy_meters(double pixelRepresentsInMeters) {
		return new Point(vertex_xy.getX() * pixelRepresentsInMeters, vertex_xy.getY() * pixelRepresentsInMeters);
	}

	public void setVertex_xy_meters(Point vertex_xy_meters, double pixelRepresentsInMeters) {
		this.vertex_xy = new Point(vertex_xy_meters.getX()/pixelRepresentsInMeters, vertex_xy_meters.getY()/pixelRepresentsInMeters);
	}

	public String getUrlIcon() {
		return urlIcon;
	}

	public void setUrlIcon(String urlIcon) {
		this.urlIcon = urlIcon;
	}
	
	/**
	 * Checks if the specified point is inside the drawable element.
	 * 
	 * @param p
	 * @param currentIconDimension
	 * @return
	 */
	public boolean pointCollidesItem(Point p, int currentIconDimension) {
		boolean collides = false;
		
		if (p.getX() >= getVertex_xy().getX() && p.getX() <= (getVertex_xy().getX() + currentIconDimension)) {
			if (p.getY() >= getVertex_xy().getY() && p.getY() <= (getVertex_xy().getY() + currentIconDimension)) {
				collides = true;
			}
		}
		
		return collides;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (vertex_label ^ (vertex_label >>> 32));
		result = prime * result + ((vertex_xy == null) ? 0 : vertex_xy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Drawable other = (Drawable) obj;
		if (vertex_label != other.vertex_label)
			return false;
		if (vertex_xy == null) {
			if (other.vertex_xy != null)
				return false;
		} else if (!vertex_xy.equals(other.vertex_xy))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String s = "[";
		if (room != null)
			s += "room=" + room.getLabel() + ", ";
		
		s += "vertex_label=" + vertex_label + ", vertex_xy=" + vertex_xy + "]";
		
		return s;
	}

}
