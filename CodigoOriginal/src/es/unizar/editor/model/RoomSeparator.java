package es.unizar.editor.model;

import java.awt.geom.Line2D;

import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

public class RoomSeparator extends Drawable {
	
	private Line2D line;
	
	private Corner c1, c2;

	public RoomSeparator(Room room, long vertex_label, Corner c1, Corner c2) {
		/* Añadido por Nacho Palacio 2025-04-17. */
    	super(room, ElementIdMapper.convertToRangeId(vertex_label, ElementIdMapper.CATEGORY_SEPARATOR), c1.getVertex_xy());
		this.setUrlIcon(Literals.IMAGES_PATH + "dashedLine.png");
		
		if (c1.getVertex_label() < c2.getVertex_label()) {
			this.c1 = c1;
			this.c2 = c2;
		}
		else {
			this.c1 = c2;
			this.c2 = c1;
		}
		
		this.line = new Line2D.Double(this.c1.getVertex_xy().toPoint2D(), this.c2.getVertex_xy().toPoint2D());
	}
	
	public Line2D getLine() {
		return line;
	}
	
	public void updateLine() {
		this.line.setLine(this.c1.getVertex_xy().toPoint2D(), this.c2.getVertex_xy().toPoint2D());
	}

	public Corner getC1() {
		return c1;
	}

	public void setC1(Corner c1) {
		this.c1 = c1;
	}

	public Corner getC2() {
		return c2;
	}

	public void setC2(Corner c2) {
		this.c2 = c2;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((c1 == null) ? 0 : c1.hashCode());
		result = prime * result + ((c2 == null) ? 0 : c2.hashCode());
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
		RoomSeparator other = (RoomSeparator) obj;
		if (c1 == null) {
			if (other.c1 != null)
				return false;
		} else if (!c1.equals(other.c1))
			return false;
		if (c2 == null) {
			if (other.c2 != null)
				return false;
		} else if (!c2.equals(other.c2))
			return false;
		return true;
	}
	
	/**
	 * 
	 * @return		Point at the middle of the line (invisible door)
	 */
	public Point getMiddlePoint() {
		double x = (getC1().getVertex_xy().getX() + getC2().getVertex_xy().getX()) / 2;
		double y = (getC1().getVertex_xy().getY() + getC2().getVertex_xy().getY()) / 2;
		
		return new Point(x, y);
	}

	/**
	 * Overrided function to check if element intersects line.
	 */
	@Override
	public boolean pointCollidesItem(Point p, int currentIconDimension) {
		return this.line.contains(p.getX(), p.getY());
	}
}
