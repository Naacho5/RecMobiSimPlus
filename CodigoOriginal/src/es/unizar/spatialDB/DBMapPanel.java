package es.unizar.spatialDB;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomPolygon;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.view.MapPanelListener;
import es.unizar.editor.view.ObjectPropertiesWindow;
import es.unizar.util.EditorLiterals;

public class DBMapPanel extends JPanel implements MouseInputListener {

	private static final long serialVersionUID = 1L;
	
	DBViewerModel model;
	DBViewer viewer;
	private MapPanelListener listener;
	
	private Point pointPressed;
	private boolean wasDoubleClick = false;
	private Timer timer;
	private int buttonPressed;
		
	public DBMapPanel(DBViewerModel model, DBViewer viewer) {
		super();
		this.model = model;
		this.viewer = viewer;
		addMouseListener(this);
		addMouseMotionListener(this);
		modifySize(DBViewerModel.getMIN_MAP_W(), DBViewerModel.getMIN_MAP_H(), 1.0);
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
						
		// -- DRAW WALLS --
		// - ALL ROOMS
		for (DBRoom r: model.getRooms()) {
			if(model.getRoomSelectedLabel() == Integer.parseInt(r.getLabel())) {
				g.setColor(Color.RED);
//				g.drawPolygon(r.getPol());
				drawWalls(g, r.getPol(), true);
				g.setColor(Color.BLACK);
			}else {
//				g.drawPolygon(r.getPol());
				drawWalls(g, r.getPol(), true);
			}
			g.setColor(Color.BLUE);
			g.drawString(r.getLabel(), (int) (r.getLabelLocation().getX() * model.getZOOM()), (int) ((r.getLabelLocation().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
			g.setColor(Color.BLACK);
			model.addRoomNumber(new Drawable(null,0,new Point((int) (r.getLabelLocation().getX() * model.getZOOM()),(int) ((r.getLabelLocation().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM())),r.getLabel()));
		}
		
		for(Drawable d : model.getDrawables()) {
			ImageIcon icon = new ImageIcon(d.getUrlIcon());
			g.drawImage(icon.getImage(), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) (d.getVertex_xy().getY() * model.getZOOM()),
					(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), this);
			if(d instanceof Door) {
				g.setColor(Color.GREEN);
				g.drawString(Integer.toString((int) d.getVertex_label()), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
				g.setColor(Color.BLACK);
			}else {
				g.setColor(Color.RED);
				g.drawString(Integer.toString((int) d.getVertex_label()), (int) (d.getVertex_xy().getX() * model.getZOOM()), (int) ((d.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
				g.setColor(Color.BLACK);
			}
		}
		
		for(Item i : model.getItems()) {
			ImageIcon icon = new ImageIcon(i.getUrlIcon());
			g.drawImage(icon.getImage(), (int) (i.getVertex_xy().getX() * model.getZOOM()), (int) (i.getVertex_xy().getY() * model.getZOOM()),
					(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), this);
			g.setColor(Color.RED);
			g.drawString(Integer.toString((int) i.getVertex_label()), (int) (i.getVertex_xy().getX() * model.getZOOM()), (int) ((i.getVertex_xy().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM()));
			g.setColor(Color.BLACK);
		}
		
		g.setColor(Color.BLUE);
		for(RoomSeparator rs : model.getSeparators()) {						
			g.fillOval((int) (rs.getC1().getVertex_xy().getX() * model.getZOOM()), (int) (rs.getC1().getVertex_xy().getY() * model.getZOOM()),
					(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()));
			
			g.fillOval((int) (rs.getC2().getVertex_xy().getX() * model.getZOOM()), (int) (rs.getC2().getVertex_xy().getY() * model.getZOOM()),
					(int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()), (int) (model.getDRAWING_ICON_DIMENSION() * model.getZOOM()));
			
			drawSubroomSeparator(g, rs);
		}
		g.setColor(Color.BLACK);		
	}
	
	
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
	
	/**
	 * Draws the walls between the list of corners. If it's a completed room, it also draws a wall between the first and the last corner.
	 * 
	 * @param g					Graphics
	 * @param corners			List of corners in room (completed or being drawn)
	 * @param isCompletedRoom	If it's a completed room, draw last wall
	 */
	private void drawWalls(Graphics g, Polygon pol, boolean isCompletedRoom) {
		Corner previous = null;
		boolean first = true;
//		for (Corner c: corners) {
		for (int i = 0; i < pol.npoints; i++) {
			Corner c = new Corner(null,0,new Point(pol.xpoints[i],pol.ypoints[i]));
			if (first) { // If first element
				previous = c;
				first = false;
			}
			else {
				drawWallBetweenCorners(g, previous, c);
				
				previous = c;
				// If it's the last corner and it's a completed room
				if ((int)c.getVertex_xy().getX() == pol.xpoints[pol.npoints-1] && (int)c.getVertex_xy().getY() == pol.ypoints[pol.npoints-1] && isCompletedRoom) {
					// Draw wall between last and first corner
					drawWallBetweenCorners(g, c, new Corner(null,0,new Point(pol.xpoints[0],pol.ypoints[0])));
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
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//	OBJECT PROPERTIES																		//
	//////////////////////////////////////////////////////////////////////////////////////////////
	/**
	* Open window that shows object's properties. They can be modified in this window.
	* 
	* @param d		Object
	*/
	public void objectProperties(Drawable d) {
		ObjectPropertiesWindowDB objectPropertiesWindow = new ObjectPropertiesWindowDB(model,d);
		objectPropertiesWindow.setVisible(true);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Adds a MapPanel Listener (with map panel ops).
	 * 
	 * @param listener
	 */
	public void addMapPanelListener(MapPanelListener listener) {
		this.listener = listener;
	}

}
