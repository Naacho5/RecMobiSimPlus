package es.unizar.util;

import java.awt.Polygon;
import java.awt.geom.Line2D;

import es.unizar.gui.MainSimulator;

/**
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class Distance {

	/**
	 * Returns the shortest distance between two points. If it's 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
    public static double distanceBetweenTwoPoints(double x1, double y1, double x2, double y2) {
    	
    	return euclideanDistance(x1, y1, x2, y2);
    	
    	/*
    	int room1 = MainMuseumSimulator.floor.getRoomFromPosition((int) x1, (int) y1);
    	int room2 = MainMuseumSimulator.floor.getRoomFromPosition((int) x2, (int) y2);
    	
    	if (room1 < 0) {
    		//System.out.println("Room < 0");
    		return euclideanDistance(x1, y1, x2, y2);
    	}
    	else if (room1 != room2 || !collides(x1, y1, x2, y2, MainMuseumSimulator.floor.rooms.get(room1))) {
    		//System.out.println("Rooms distintas (" + room1 + ";" + room2 + ") o no collides");
    		return euclideanDistance(x1, y1, x2, y2);
    	}
    	else {
    		//System.out.println("¡¡¡¡¡¡¡¡¡¡¡ COLLIDES !!!!!!!!!!!!!!!!!!!!!!!!!!!");
    		return distanceInsidePolygon(x1, y1, x2, y2, MainMuseumSimulator.floor.rooms.get(room1));
    	}
    	*/
    }
    
    /**
     * Checks if trajectory collides with room (polygon).
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param polygon
     * @return
     */
    private static boolean collides(double x1, double y1, double x2, double y2, Polygon polygon) {
    	
    	boolean intersects = false;
    	int i = 0;
    	while (i < polygon.npoints && !intersects) {
    		if (i == (polygon.npoints - 1)) {
    			intersects = Line2D.linesIntersect(x1, y1, x2, y2, polygon.xpoints[polygon.npoints - 1], polygon.ypoints[polygon.npoints - 1], polygon.xpoints[0], polygon.ypoints[0]);
    		}
    		else {
    			intersects = Line2D.linesIntersect(x1, y1, x2, y2, polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[i+1], polygon.ypoints[i+1]);
    		}
    		
    		i++;
    	}
		
		return intersects;
	}

    /**
     * Returns the Euclidean distance between two points.
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
	private static double euclideanDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}
	
	private static double distanceInsidePolygon(double x1, double y1, double x2, double y2, Polygon polygon) {
		// TODO Auto-generated method stub
		return euclideanDistance(x1, y1, x2, y2); // TODO: change to movement inside Polygon!!!
	}
}
