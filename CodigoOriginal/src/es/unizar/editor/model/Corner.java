package es.unizar.editor.model;

import es.unizar.util.Literals;

public class Corner extends Drawable{

	public Corner(Room room, long vertex_label, Point vertex_xy) {
		super(room, vertex_label, vertex_xy);
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
