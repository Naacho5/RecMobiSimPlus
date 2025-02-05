package es.unizar.spatialDB;

import java.awt.Polygon;

import es.unizar.editor.model.Point;

public class DBRoom {
	Polygon pol;
	String label;
	Point labelLocation;
	
	public DBRoom(Polygon pol, String label, Point labelLocation) {
		super();
		this.pol = pol;
		this.label = label;
		this.labelLocation = labelLocation;
	}

	public Polygon getPol() {
		return pol;
	}

	public void setPol(Polygon pol) {
		this.pol = pol;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Point getLabelLocation() {
		return labelLocation;
	}

	public void setLabelLocation(Point labelLocation) {
		this.labelLocation = labelLocation;
	}	
}
