package es.unizar.editor.model;

import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

public class Corner extends Drawable{

	public Corner(Room room, long vertex_label, Point vertex_xy) {
		/* Añadido por Nacho Palacio 2025-04-17. */
    	super(room, ElementIdMapper.convertToRangeId(vertex_label, ElementIdMapper.CATEGORY_CORNER), vertex_xy);
		this.setUrlIcon(Literals.IMAGES_PATH + "corner.png");
	}
	
	@Override
	public void setVertex_xy(Point vertex_xy) {
		super.setVertex_xy(vertex_xy);
		getRoom().refreshPolygon();
	}
	
	@Override
	public String toString() {
		return "Corner: " + super.toString();
	}

}
