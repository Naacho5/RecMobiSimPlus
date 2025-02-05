package es.unizar.editor.model;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.spatial.spatial4j.geo3d.GeoConvexPolygon;
import org.apache.lucene.spatial.spatial4j.geo3d.GeoPoint;

import earcut4j.Earcut;

public class RoomPolygon {
	
	protected int label; // Equivalent to room's id
	protected List<Corner> corners;
	protected Polygon polygon;
	
	public RoomPolygon(int label) {
		// Init
		this.label = label;
		this.corners = new LinkedList<Corner>();
		this.polygon = new Polygon();
	}
	
	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}

	public List<Corner> getCorners() {
		return corners;
	}

	public void setCorners(List<Corner> corners) {
		this.corners = corners;
		refreshPolygon();
	}
	
	public int getNumCorners() {
		return this.corners.size();
	}
	
	public Polygon getPolygon() {
		return polygon;
	}

	public void setPolygon(Polygon polygon) {
		this.polygon = polygon;
	}
	
	public void refreshPolygon() {
		this.polygon.reset();
		for (Corner c: getCorners()) {
			this.polygon.addPoint((int) c.getVertex_xy().getX(), (int) c.getVertex_xy().getY());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((corners == null) ? 0 : corners.hashCode());
		result = prime * result + label;
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
		RoomPolygon other = (RoomPolygon) obj;
		if (corners == null) {
			if (other.corners != null)
				return false;
		} else if (!corners.equals(other.corners))
			return false;
		if (label != other.label)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[label=" + label + ", corners=" + corners + "]";
	}
	
	/**
	 * Triangulates the room (polygon) and returns the list of triangles
	 * @return List<Polygon> triangles
	 */
	private List<Polygon> triangulatePolygon() {
		
		// Add all vertices (corners) to array to triangulate
		List<Double> vertices = new ArrayList<Double>();
		for(Corner c: getCorners()) {
			vertices.add(c.getVertex_xy().getX());
			vertices.add(c.getVertex_xy().getY());
		}
		double[] verticesArray = vertices.stream().mapToDouble(d -> d).toArray();
		
		// TRIANGULATE (Get list of triangles)
		// Return value: [t1_1,t1_2,t1_3, t2_1,t2_2,t2_3, t3_1,t3_2,t3_3, ...]
		List<Integer> triangles = Earcut.earcut(verticesArray, null, 2);
		
		// Create polygons from vertices list
		List<Polygon> polygonTriangles = new ArrayList<Polygon>();
		
		int i = 0;
		while (i < triangles.size()) {
			int t1x, t1y, t2x, t2y, t3x, t3y;
			t1x = (int) getCorners().get(triangles.get(i)).getVertex_xy().getX();
			t1y = (int) getCorners().get(triangles.get(i)).getVertex_xy().getY();
			
			i++;
			t2x = (int) getCorners().get(triangles.get(i)).getVertex_xy().getX();
			t2y = (int) getCorners().get(triangles.get(i)).getVertex_xy().getY();
			
			i++;
			t3x = (int) getCorners().get(triangles.get(i)).getVertex_xy().getX();
			t3y = (int) getCorners().get(triangles.get(i)).getVertex_xy().getY();
			
			Polygon p = new Polygon(new int[] {t1x, t2x, t3x}, new int[] {t1y, t2y, t3y}, 3);
			
			polygonTriangles.add(p);
			
			i++;
		}
		
		return polygonTriangles;
		
	}
	
	private List<Double> weightAreas(List<Polygon> triangles) {

		double totalArea = 0.0;
		List<Double> areas = new ArrayList<Double>();
		// Get all areas
		for (Polygon p: triangles) {
			// Calculate area and add it to list
			double area = calculateArea(p);
			areas.add(area);
			
			// Add value to total area value
			totalArea += area;
		}
		
		// Get all weights
		for (int i = 0; i < areas.size(); i++) {
			areas.set(i, areas.get(i) / totalArea);
		}
		
		return areas;
	}
	
	/**
	 * Calculate area of a triangle. Half the bounding rectangle's area.
	 * @param p	MUST HAVE 3 SIDES
	 * @return area
	 */
	private double calculateArea(Polygon p) {

		double rectangleArea = p.getBounds().getWidth() * p.getBounds().getHeight();
		
		return rectangleArea/2;
	}
	
	/**
	 * Returns a random triangle polygon triangulation using weighted areas for the random selection.
	 * 
	 * @param triangles
	 * @param weightedAreas
	 * @return
	 */
	private Polygon getTriangleUsingWeightedAreas(List<Polygon> triangles, List<Double> weightedAreas) {
		
		// Get random value from 0.0 to 1.0
		Random rand = new Random();
		double randomValue = rand.nextDouble();
		
		// For every weighted area, substract the area to the random value. If value < 0 -> Get that index
		Polygon triangle = null;
		for (int i = 0; i < weightedAreas.size(); i++) {
			
			randomValue -= weightedAreas.get(i);
			
			if (randomValue < 0) {
				triangle = triangles.get(i);
				break;
			}
		}
		
		// If triangle is still null -> Last triangle
		if (triangle == null)
			triangle = triangles.get(triangles.size()-1);
		
		return triangle;
	}
	
	/**
	 * Gets a random point from the selected triangle.
	 * https://stackoverflow.com/questions/19654251/random-point-inside-triangle-inside-java
	 * 
	 * @param p
	 * @return
	 */
	private es.unizar.editor.model.Point getRandomPointInTriangle(Polygon p) {
		
		double r1 = Math.random();
        double r2 = Math.random();

        double sqrtR1 = Math.sqrt(r1);

        double x = (1 - sqrtR1) * p.xpoints[0] + (sqrtR1 * (1 - r2)) * p.xpoints[1] + (sqrtR1 * r2) * p.xpoints[2];
        double y = (1 - sqrtR1) * p.ypoints[0] + (sqrtR1 * (1 - r2)) * p.ypoints[1] + (sqrtR1 * r2) * p.ypoints[2];

        return new es.unizar.editor.model.Point(x, y);
	}


	/**
	 * Get a random point inside room (Polygon).
	 * Theory: https://stackoverflow.com/questions/19481514/how-to-get-a-random-point-on-the-interior-of-an-irregular-polygon
	 * 
	 * @return Point (with random coordinates inside room)
	 */
	public es.unizar.editor.model.Point getRandomPointInRoom() {
		
		// 1st step: triangulate polygon.
		List<Polygon> triangles = triangulatePolygon();
		// 2nd step: get weighted areas (for correct distribution of items)
		List<Double> weightedAreas = weightAreas(triangles);
		// 3rd step: get triangle using weighted areas
		Polygon p = getTriangleUsingWeightedAreas(triangles, weightedAreas);
		// 4th step: get random point in triangle
		es.unizar.editor.model.Point point = getRandomPointInTriangle(p);
		
		// In case null value, try again till it's not null
		while(point == null) {
			point = getRandomPointInTriangle(p);
		}
		
		return point;
	}
	
	/**
	 * Check if room is concave using GeoConvexPolygon (if it cannot build a convex polygon, it is concave).
	 * 
	 * @return		T(concave)/F(convex)
	 */
	public boolean checkConcave() {
		
		boolean concave = false;
		
		try {
			List<GeoPoint> geoPoints = new LinkedList<GeoPoint>();
			for (Corner c: getCorners()) {
				geoPoints.add(new GeoPoint(c.getVertex_xy().getX(), c.getVertex_xy().getY()));
			}
			GeoConvexPolygon poly = new GeoConvexPolygon(geoPoints); // If it cannot create convex polygon -> It's concave
			System.out.println(poly.toString());
		}
		catch (IllegalArgumentException e) {
			concave = true;
		}
		
		return concave;
	}

}
