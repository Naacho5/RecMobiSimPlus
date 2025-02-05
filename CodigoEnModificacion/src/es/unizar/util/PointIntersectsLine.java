package es.unizar.util;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import es.unizar.editor.model.Point;
import es.unizar.editor.model.RoomSeparator;

public class PointIntersectsLine {
	
	public static boolean pointIntersectsLine(RoomSeparator rs, Point testPoint, int iconDimension, double zoom) {
		
		// Calculate c1 and c2 positions
		double firstCornerX = rs.getC1().getVertex_xy().getX() * zoom;
		double firstCornerY = rs.getC1().getVertex_xy().getY() * zoom;
		double secondCornerX = rs.getC2().getVertex_xy().getX() * zoom;
		double secondCornerY = rs.getC2().getVertex_xy().getY() * zoom;
		
		double cornerLimit = iconDimension * zoom;
		
		Point2D p1 = new Point2D.Double(
				(int) (2*firstCornerX+cornerLimit)/2,
				(int) (2*firstCornerY+cornerLimit)/2);
		
		Point2D p2 = new Point2D.Double(
				(int) (2*secondCornerX+cornerLimit)/2,
				(int) (2*secondCornerY+cornerLimit)/2);
		
		
		// Line2D line = new Line2D.Double(rs.getC1().getVertex_xy().toPoint2D(), rs.getC2().getVertex_xy().toPoint2D());
		Line2D line = new Line2D.Double(p1, p2);
		//System.out.println("(" + line.getX1() + ", " + line.getY1() + "), (" + line.getX2() + ", " + line.getY2() + ") VS. (" + testPoint.getX() + ", " + testPoint.getY() + ") WIDTH: " + iconDimension/2);
		
		// System.out.println(rs.getLine() + "; x = " + testPoint.getX() + ", y = " + testPoint.getY() + ", w = " + width + ", h = " + width);

		return  line.intersects(testPoint.getX(), testPoint.getY(), iconDimension/2, iconDimension/2);
	
	}

}
