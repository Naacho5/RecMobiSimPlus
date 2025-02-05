package es.unizar.editor.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import org.apache.mahout.math.Arrays;

import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomPolygon;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.MapEditorModel.ToolButtons;
import es.unizar.util.Literals;

public class MapPanel extends JPanel implements MouseInputListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MapEditorModel model;
	
	private MapPanelListener listener;
	
	private Point pointPressed;
	private boolean wasDoubleClick = false;
	private Timer timer;
	private int buttonPressed;


	
	
	/**
	 * Calls constructor with minimum dimensions.
	 */
	public MapPanel(MapEditorModel model) {
		this(model.MIN_MAP_W, model.MIN_MAP_H, model);
	}

	/**
	 * Builds JPanel, sets dimensions and style and initializes mouse listeners.
	 * @param map_W
	 * @param map_H
	 */
	public MapPanel(int map_W, int map_H, MapEditorModel model) {
		
		super();
		
		this.model = model;
		
		initialize();
		
		modifySize(map_W, map_H, 1.0);
		
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.setBackground(Color.WHITE);
		
	}
	
	/**
	 * Initializes mouse listeners.
	 * 
	 * MouseListener -> mouseClicked, mouseEntered, mouseReleased, mousePressed, mouseReleased
	 * MouseMotionListener -> 	mouseMoved, mouseDragged
	 */
	private void initialize() {
		// Add listeners
		addMouseListener(this);
		addMouseMotionListener(this);
	}


	/**
	 * Adds a MapPanel Listener (with map panel ops).
	 * 
	 * @param listener
	 */
	public void addMapPanelListener(MapPanelListener listener) {
		this.listener = listener;
	}
	
	public void setMAP_W(int map_W) {
		model.setMAP_W(map_W);
		
		// Modify preferred size
		modifySize(model.getMAP_W(), model.getMAP_H(), model.getZOOM());
		this.repaint();
	}
	
	public void setMAP_H(int map_H) {
		model.setMAP_H(map_H);
		
		// Modify preferred size
		modifySize(model.getMAP_W(), model.getMAP_H(), model.getZOOM());
		this.repaint();
	}
	
	public void setZOOM(double zoom) {
		model.setZOOM(zoom);
		
		// Modify preferred size
		modifySize(model.getMAP_W(), model.getMAP_H(), model.getZOOM());
		this.repaint();
	}
	
	public void setPixelRepresentsInMeters(double pixelRepresentsInMeters) {
		model.setPixelRepresentsInMeters(pixelRepresentsInMeters);
		this.repaint();
	}


	/**
	 * Apply size changes to panel.
	 * @param map_W
	 * @param map_H
	 */
	private void modifySize(int map_W, int map_H, double zoom) {
		
		this.setPreferredSize(new Dimension((int) (map_W*zoom), (int) (map_H*zoom)));
		//graphComponent.setPreferredSize(new Dimension((int) (MAP_W*ZOOM), (int) (MAP_H*ZOOM)));
		// BoxLayout auto-resizes if size is lower than parent container, that's why maxSize changed
		// this.setMaximumSize(new Dimension(MAP_W, MAP_H));
		
		this.revalidate();
		this.repaint();
	}


	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		for (Drawable d: model.getPaintedElements()) {
			
			// -- ELEMENTS --
			ImageIcon icon = new ImageIcon(d.getUrlIcon());
			if (d instanceof Door && (((Door) d).getRoom() == null || ((Door) d).getConnectedTo().isEmpty())){
				g.drawImage(icon.getImage(), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) (d.getVertex_xy().getY() * model.getZOOM()),
						(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), Color.RED, this);
			}
			else if (d instanceof RoomSeparator) {
				g.setColor(Color.BLUE);
				
				RoomSeparator rs = (RoomSeparator) d;
				
				g.fillOval((int) (rs.getC1().getVertex_xy().getX() * model.getZOOM()), (int) (rs.getC1().getVertex_xy().getY() * model.getZOOM()),
						(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()));
				
				g.fillOval((int) (rs.getC2().getVertex_xy().getX() * model.getZOOM()), (int) (rs.getC2().getVertex_xy().getY() * model.getZOOM()),
						(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()));
				
				drawSubroomSeparator(g, rs);
				
				g.setColor(Color.BLACK);
			}
			else {
				g.drawImage(icon.getImage(), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) (d.getVertex_xy().getY() * model.getZOOM()),
						(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), this);
			}
			// -- END ELEMENTS --
			
			// -- IDS --
			if (d instanceof Door) {
				g.setColor(Color.GREEN);
				g.drawString(Integer.toString((int) d.getVertex_label() + model.getNumItems()), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
				g.setColor(Color.BLACK);
			}
			
			if (d instanceof Item) {
				g.setColor(Color.RED);
				g.drawString(Integer.toString((int) d.getVertex_label()), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
				g.setColor(Color.BLACK);
			}
			// -- END IDS --
		}
		
		// -- DRAW WALLS --
		// - ALL ROOMS
		for (Room r: model.getRooms()) {
			drawWalls(g, new LinkedList<>(r.getCorners()), true);
			
			// Draw room label in first room corner
			Point roomPoint = r.getCorners().get(0).getVertex_xy();
			g.drawString(Integer.toString(r.getLabel()), (int) (roomPoint.getX() * model.getZOOM()), (int) ((roomPoint.getY() -1) * model.getZOOM()));
			
			// Draw subrooms
			g.setColor(Color.YELLOW);
			//System.out.println(r.getNumSubRooms());
			for(RoomPolygon rp: r.getSubRooms()) {
				Polygon poly = new Polygon();
				for (int point = 0; point < rp.getPolygon().npoints; point++) {
					poly.addPoint((int) ((rp.getPolygon().xpoints[point] + (model.getDRAWING_ICON_DIMENSION()/2)) * model.getZOOM()),
							(int) ((rp.getPolygon().ypoints[point] + (model.getDRAWING_ICON_DIMENSION()/2)) * model.getZOOM()));
				}
				g.drawPolygon(poly);
				//System.out.println(rp.getCorners().size());
			}
			g.setColor(Color.BLACK);
			//System.out.println();
		}
		
		// - CURRENT ROOM
		// Draw walls for current room (being drawn but not yet completed)
		drawWalls(g, model.getCurrentRoomCorners(), false);
		// -- END WALLS --
		
		// Draw current roomSeparator if exists
		if (model.getCurrentRoomSeparatorCorner() != null) {
			g.setColor(Color.BLUE);
			g.fillOval((int) ((model.getCurrentRoomSeparatorCorner().getVertex_xy().getX()) * model.getZOOM()),
					(int) ((model.getCurrentRoomSeparatorCorner().getVertex_xy().getY()) * model.getZOOM()),
					(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()));
			g.setColor(Color.BLACK);
		}
	}
	
	/**
	 * Draws the walls between the list of corners. If it's a completed room, it also draws a wall between the first and the last corner.
	 * 
	 * @param g					Graphics
	 * @param corners			List of corners in room (completed or being drawn)
	 * @param isCompletedRoom	If it's a completed room, draw last wall
	 */
	private void drawWalls(Graphics g, List<Corner> corners, boolean isCompletedRoom) {
		Corner previous = null;
		
		for (Corner c: corners) {
			if (c == corners.get(0)) { // If first element
				previous = c;
			}
			else {
				drawWallBetweenCorners(g, previous, c);
				
				previous = c;
				
				// If it's the last corner and it's a completed room
				if (c == corners.get(corners.size()-1) && isCompletedRoom) {
					// Draw wall between last and first corner
					drawWallBetweenCorners(g, c, corners.get(0));
				}
			}
		}
		
	}
	
	/**
	 * Draws a line that represents a wall between corners
	 * 
	 * @param g		Graphics
	 * @param c1	First corner
	 * @param c2	Second corner
	 */
	private void drawWallBetweenCorners(Graphics g, Corner c1, Corner c2) {
		// Calculate c1 and c2 positions
		double previousCornerX = c1.getVertex_xy().getX() * model.getZOOM();
		double previousCornerY = c1.getVertex_xy().getY() * model.getZOOM();
		double currentCornerX = c2.getVertex_xy().getX() * model.getZOOM();
		double currentCornerY = c2.getVertex_xy().getY() * model.getZOOM();
		
		double cornerLimit = model.getDRAWING_ICON_DIMENSION() * model.getZOOM();
		
		g.drawLine(
				(int) (2*previousCornerX+cornerLimit)/2,
				(int) (2*previousCornerY+cornerLimit)/2,
				(int) (2*currentCornerX+cornerLimit)/2,
				(int) (2*currentCornerY+cornerLimit)/2);
	}

	/**
	 * Draw dashed line between RoomSeparator points that represents the limit between subrooms.
	 * 
	 * @param g		Graphics
	 * @param rs	RoomSeparator
	 */
	private void drawSubroomSeparator(Graphics g, RoomSeparator rs) {
		
		// DASHED LINE: https://stackoverflow.com/questions/21989082/drawing-dashed-line-in-java
		// Create a copy of the Graphics instance
		Graphics2D newGraphics = (Graphics2D) g.create();
		
		// Set the stroke of the copy, not the original 
		Stroke dashedLine = new BasicStroke((int) (model.getDRAWING_ICON_DIMENSION()/2*model.getZOOM()), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
		                                  0, new float[]{9}, 0);
		newGraphics.setStroke(dashedLine);
		  
		// Calculate c1 and c2 positions
		double firstCornerX = rs.getC1().getVertex_xy().getX() * model.getZOOM();
		double firstCornerY = rs.getC1().getVertex_xy().getY() * model.getZOOM();
		double secondCornerX = rs.getC2().getVertex_xy().getX() * model.getZOOM();
		double secondCornerY = rs.getC2().getVertex_xy().getY() * model.getZOOM();
		
		double cornerLimit = model.getDRAWING_ICON_DIMENSION() * model.getZOOM();
		
		newGraphics.drawLine(
				(int) (2*firstCornerX+cornerLimit)/2,
				(int) (2*firstCornerY+cornerLimit)/2,
				(int) (2*secondCornerX+cornerLimit)/2,
				(int) (2*secondCornerY+cornerLimit)/2);
		
		// Get rid of the copy
		  newGraphics.dispose();
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//	OBJECT PROPERTIES																		//
	//////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Open window that shows object's properties. They can be modified in this window.
	 * 
	 * @param d		Object
	 */
	public void objectProperties(Drawable d) {
		ObjectPropertiesWindow objectPropertiesWindow = new ObjectPropertiesWindow(this, d, model);
		objectPropertiesWindow.addMapPanelListener(listener);
		objectPropertiesWindow.setVisible(true);
		
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//	MOUSE EVENTS																			//
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Get the mouse position in panel taking into account the zoom.
	 * 
	 * @param e	(MouseEvent)
	 * @return	Point of the MouseEvent's REAL POSITION -> DIVIDED BY ZOOM.
	 */
	private Point getMousePosition(MouseEvent e) {
		if (e.getX() >= 0 && e.getY() >= 0 && e.getX() <= model.getMAP_W()*model.getZOOM() && e.getY() <= model.getMAP_H()*model.getZOOM()) {
			// Click was correct (inside the panel)
			return new Point(e.getX()/model.getZOOM(), e.getY()/model.getZOOM());
		}
		else
			return null;
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		
		if (model.getToolClicked() == ToolButtons.mover && model.getObjectSelected() != null && !(model.getObjectSelected() instanceof RoomSeparator)) { // Call only when mover is selected
			Point p = getMousePosition(e);
			listener.move(p);
		}
	}

	/**
	 * Mouse moved and no button is pushed. Apply hovered.
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		Point p = getMousePosition(e);
		listener.hovered(p);
	}

	/**
	 * User clicks.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		pointPressed = getMousePosition(e);
		buttonPressed = e.getButton();
		
		/*
		 * Inspiration from:
		 * https://www.generacodice.com/en/articolo/1080826/Distinguish+between+a+single+click+and+a+double+click+in+Java
		 * 
		 * Adapted to this case (timer.stop(), delete else in actionPerformed, timerDelay reduced...
		 */
		if (e.getClickCount() >= 2) {
			wasDoubleClick = true;
			timer.stop();
			
			if (e.getClickCount() == 2) // If button clicked more than twice, ignore.
				// Accesses manageClick(...) ONLY when it's DOUBLE CLICK (due to timer.stop())
				listener.manageClick(pointPressed, buttonPressed, wasDoubleClick);
		}
		else {
			// Use one of the two delays created below
			Integer multiClickInterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval"); // Value = 500ms
			int timerDelay = 200; // Value = 200ms -> To wait less
			
			// Change first arg to multiClickInterval.intValue() if wanted -> more delay
            timer = new Timer(timerDelay, new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if (wasDoubleClick)
                        wasDoubleClick = false; // reset flag
                    
                    // Accesses manageClick(...) ONLY when it's SIMPLE CLICK (due to timer.stop())
                    listener.manageClick(pointPressed, buttonPressed, wasDoubleClick);
                }
                
            });
            timer.setRepeats(false);

            timer.start();
		}
		
	}

	/**
	 * User presses click.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		
		Point p = getMousePosition(e);
		if (model.getToolClicked() == ToolButtons.mover) { // Call only when mover is selected
			
			listener.manageClick(p, e.getButton(), false);
			if (model.getObjectSelected() != null)
				model.setOldObjectCoordinates(model.getObjectSelected().getVertex_xy());
		}
		
	}

	/**
	 * User stops clicking (stops pressing the click button).
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		
		if (model.getToolClicked() == ToolButtons.mover) { // Call only when mover is selected
			listener.validateMovement();
		}
	}

	/**
	 * When mouse enters the panel (wasn't in the panel and gets in).
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
		// Nothing to do
	}

	/**
	 * When mouse leaves the panel (was in the panel and gets out).
	 */
	@Override
	public void mouseExited(MouseEvent e) {
		
		// Do the same as mouseReleased when moving an object
		/*if (model.getToolClicked() == ToolButtons.mover) { // Call only when mover is selected
			listener.validateMovement();
		}*/
	}
	
}
