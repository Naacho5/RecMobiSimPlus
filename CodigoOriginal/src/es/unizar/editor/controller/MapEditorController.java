package es.unizar.editor.controller;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.editor.model.MapEditorModel.IconButtons;
import es.unizar.editor.model.MapEditorModel.ToolButtons;
import es.unizar.editor.view.AddVisitableView;
import es.unizar.editor.view.DeleteVisitableView;
import es.unizar.editor.view.EditVisitableObjectView;
import es.unizar.editor.view.MapEditorView;
import es.unizar.editor.view.MapPanelListener;
import es.unizar.util.EditorLiterals;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.PointIntersectsLine;

/**
 * Editor's controller (MVC).
 *
 * @author Alejandro Piedrafita Barrantes
 */
public class MapEditorController implements ActionListener, FocusListener, MapPanelListener {
	
	private MapEditorModel model;
	private MapEditorView view;
	
	public MapEditorController(MapEditorModel model) {
		super();
		this.model = model;
	}
	
	public void setView(MapEditorView v) {
		if (this.view == null)
			this.view = v;
	}
	

	
	/**
	 * @return	The maximum x value allowed in panel
	 */
	private double getMaxX() {
		return model.getMAP_W()-model.getDRAWING_ICON_DIMENSION()*model.getZOOM();
	}
	
	/**
	 * @return	The maximum y value allowed in panel
	 */
	private double getMaxY() {
		return model.getMAP_H()-model.getDRAWING_ICON_DIMENSION()*model.getZOOM();
	}
	
	/**
	 * Set p coordinates to max value if they are higher than max values.
	 * 
	 * @param p
	 */
	private void checkPointPosition(Point p) {
		
		if (p != null) {
		
			// Get max values
			double maxX = getMaxX();
			double maxY = getMaxY();
			
			double min = 0.0;
			
			if (p.getX() > maxX)
				p.setX(maxX);
			if (p.getY() > maxY)
				p.setY(maxY);
			
			if (p.getX() < min)
				p.setX(min);
			if (p.getY() < min)
				p.setY(min);
		
		}
	}

	//Override and private methods
	//-------------------------------------------------------
	@Override
	public void hovered(Point p) {
		
		model.setCurrentCursorPoint(p);
		
		view.statusBarX.setText("x:" + p.getX() + " (" + EditorLiterals.decimalFormat.format(p.getX()*model.getPixelRepresentsInMeters()) +" m)");
        view.statusBarY.setText("y:" + p.getY() + " (" + EditorLiterals.decimalFormat.format(p.getY()*model.getPixelRepresentsInMeters()) +" m)");
        
        String hoveredItem = "None";
        boolean elementCollides = false;
        for (Drawable d: model.getPaintedElements()) {
        	elementCollides = d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION());
        	
        	if (elementCollides) {
        		view.hoveredElement.setIcon(new ImageIcon(new ImageIcon(d.getUrlIcon()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)));
        		hoveredItem = d.getClass().getSimpleName();
        		break;
        	}
        }
        
        // If previously hovered was drawable with icon and now no drawable is hovered -> Remove icon
        if (!elementCollides) {
        	view.hoveredElement.setIcon(null);
        }
        
        view.hoveredElement.setText("Hovered: " + hoveredItem);
        
	}
	
	/**
	 * To establish which operation to do depending on click's state and the tool selected.
	 */
	@Override
	public void manageClick(Point pointPressed, int buttonPressed, boolean wasDoubleClick) {
		
		// Manage pencil
		if (model.getToolClicked() == ToolButtons.pencil) {
			if (!wasDoubleClick && buttonPressed == MouseEvent.BUTTON1) {// Left and not double click -> Pencil
					pencil(pointPressed);
			}
			else if (buttonPressed == MouseEvent.BUTTON3 || (wasDoubleClick && buttonPressed == MouseEvent.BUTTON1)) {// Right click or double left click
					completeRoom();
					cancelCurrentDrawings();
			}
		}
		else {
			// Select object
			select(pointPressed);
			
			// Manage eraser
			if (model.getToolClicked() == ToolButtons.eraser) {
				if (!wasDoubleClick && buttonPressed == MouseEvent.BUTTON1) { // Single, left click
					eraser(pointPressed);
				}
			}
			// Manage cursor
			else if (model.getToolClicked() == ToolButtons.cursor) {
				if (buttonPressed == MouseEvent.BUTTON1) { // Single, left click
					cursor(pointPressed);
					// If it was double click, open object properties window
					if (wasDoubleClick && model.getObjectSelected() != null) {
						view.mapPanel.objectProperties(model.getObjectSelected());
					}
				}
			}
		}
		
	}

	/**
	 * Drawing operation. depending on the selected element, paints that element or a new corner (with the new wall) of a room.
	 * 
	 * @param p		Point that was pressed and the new element's position
	 */
	private void pencil(Point p) {
		
		if (model.getToolClicked() == ToolButtons.pencil) { // For checking
			
			// Check max values
			checkPointPosition(p);
			
			// Delete current room corners (if not drawing corners)
			if (!model.getSelected().equals(IconButtons.corner))
				model.eraseCurrentRoomCorners();
			
			// Delete current room separator (if not drawing room separator)
			if (!model.getSelected().equals(IconButtons.roomSeparator))
				model.setCurrentRoomSeparatorCorner(null);
			
			switch (model.getSelected()) {
				case corner:
					// Create corner and add it to model
					Corner corner = new Corner(null, model.getNextCornerId(), p); // Modificado por Nacho Palacio 2025-04-18
					
					if (!model.addCornerToCurrentRoomCorners(corner)) {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD CORNER TO MAP");
					}
					
				    break;
				    
				case door:
					// Create door and add it to model
					Door door = new Door(null, model.getNextDoorId(), p); // Modificado por Nacho Palacio 2025-04-18
					
					// Add room to door if it is inside a room
					Room doorRoom = model.isInsideRoom(p, -1);
					if (doorRoom != null)
						door.setRoom(doorRoom);
						
					if (!model.addDoor(door)) {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD DOOR TO MAP");
					}
					
					break;
					
				case stairs:
					// Create corner and add it to model
					Stairs stairs = new Stairs(null, model.getNextStairsId(), p); // Modificado por Nacho Palacio 2025-04-18
					
					if (!model.addStairs(stairs)) {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD STAIRS TO MAP");
					}
					
					model.eraseCurrentRoomCorners();
					
					break;
					
				case roomSeparator:
					
					// Check if a corner is pressed
					int i = 0;
					Corner cornerClicked = null;
					while (i < model.getCorners().size() && cornerClicked == null) {
						Corner auxCorner = model.getCorners().get(i);
						if (model.pointCollidesElement(p, auxCorner)) {
							cornerClicked = auxCorner;
						}
						i++;
					}
					
					// If selected a corner
					if (cornerClicked != null) {
						// If it wasn't painted already first roomSeparatorPoint
						if (model.getCurrentRoomSeparatorCorner() == null) {
							model.setCurrentRoomSeparatorCorner(cornerClicked);
						}
						// If there is already a roomSeparator point painted
						else {
							
							if (cornerClicked != model.getCurrentRoomSeparatorCorner()) {
								
								Room roomFirstCorner = model.getCurrentRoomSeparatorCorner().getRoom();
								Room roomSecondCorner = cornerClicked.getRoom();
								
								// If corners belong to the same room -> Keep going
								if (roomFirstCorner == roomSecondCorner) {
									// Check same room/subrooms from points and not consecutive corners
									if (roomFirstCorner.getSubRoomsFromCorner(cornerClicked).equals(roomFirstCorner.getSubRoomsFromCorner(model.getCurrentRoomSeparatorCorner())) 
											&& !roomFirstCorner.areConsecutive(model.getCurrentRoomSeparatorCorner(), cornerClicked)) {
										
										// RoomSeparator rs = new RoomSeparator(roomFirstCorner, model.getNumRoomSeparators()+1,
										// 		model.getCurrentRoomSeparatorCorner(), cornerClicked);
										
										RoomSeparator rs = new RoomSeparator(roomFirstCorner, model.getNextSeparatorId(),
										model.getCurrentRoomSeparatorCorner(), cornerClicked); // Modificado por Nacho Palacio 2025-04-18
										
										if(model.addRoomSeparator(rs)) {
											model.setCurrentRoomSeparatorCorner(null);
										}
										else {
											view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD ROOMSEPARATOR TO MAP.");
										}
									}
									else {
										view.showMessage(JOptionPane.WARNING_MESSAGE, "COULDN'T ADD ROOMSEPARATOR TO MAP. PLEASE CHECK CORNERS AREN'T CONSECUTIVE OR BELONG TO DIFFERENT SUBROOMS.");
									}
								}
								else {
									view.showMessage(JOptionPane.WARNING_MESSAGE, "CORNERS DON'T BELONG TO THE SAME ROOM");
								}
							}
							else {
								view.showMessage(JOptionPane.ERROR_MESSAGE, "CAN'T SELECT THE SAME CORNER");
							}
						}// End second point of separator
					}
					else {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "Please select one corners of a concave room.");
					}
					
					break;
					
				case visitable:
					// Create item and add it to model
					Item itemVisitable = new Item(null, model.getNextItemId(), p); // Modificado por Nacho Palacio 2025-04-18
					
					Room r = model.isInsideRoom(p, -1);
					if (r != null) {
						
						itemVisitable.setRoom(r);
						
						// Set specific item's properties (visitableItem's attributes).
						model.setItemVisitableProperties(model.getVisitableSelected(), itemVisitable);
						
						if (!model.addItem(itemVisitable)) {
							view.showMessage(JOptionPane.ERROR_MESSAGE, "COULDN'T ADD ITEM TO MAP");
						}
					}
					else {
						view.showMessage(JOptionPane.WARNING_MESSAGE, "ITEM MUST BE INSIDE A ROOM");
					}
					
					break;
				    
				default:
					break;
			}
			
			// Always refresh view
			view.refresh();
		}
	}
	
	/**
	 * Erase an element from map. If the element selected is a corner, erases full room.
	 * 
	 * @param pointPressed		Point that was pressed that represents the element to erase
	 */
	private void eraser(Point pointPressed) {
		
		if (model.getToolClicked() == ToolButtons.eraser) { // For checking
			
			// Get selected object
			Drawable drawable = model.getObjectSelected();
			
			if (drawable != null) { // If some painted element has been selected
				
				if (drawable instanceof Corner) { // If corner, remove whole room
					
					Room deleteRoom = null;
					for(Room r: model.getRooms()) { // Check if any room contains that corner
						if (r.getCorners().contains(drawable)) {
							deleteRoom = r; // Corner found in room
							break;
						}
					}
					
					if (deleteRoom != null) { // If room not null
						for (Corner c: deleteRoom.getCorners()) {
							model.removeCorner(c); // Remove all room corners
							model.eraseDrawable(c); // Remove corners from painted elements
						}
						for (Item i: deleteRoom.getItems()) {
							model.removeItem(i, true); // Remove all room items
							model.eraseDrawable(i); // Remove items from painted elements
						}
						for (Door d: deleteRoom.getDoors()) {
							model.removeDoor(d, true); // Remove all room doors
							model.eraseDrawable(d); // Remove doors from painted elements
						}
						for (RoomSeparator rs: deleteRoom.getRoomSeparators()) {
							model.removeRoomSeparator(rs, true);
							model.eraseDrawable(rs);
						}
						model.removeRoom(deleteRoom); // Remove room
					}
					
				}
				else { // Check element and remove it
					if (drawable instanceof Door) {
						model.removeDoor((Door) drawable, false);
					}
					else if (drawable instanceof Stairs) {
						model.removeStairs((Stairs) drawable);
					}
					else if (drawable instanceof Item) {
						model.removeItem((Item) drawable, false);
					}
					else if (drawable instanceof RoomSeparator) {
						model.removeRoomSeparator((RoomSeparator) drawable, false);
					}
					
					// If not null, always delete from paintedElements
					model.eraseDrawable(drawable);
				}
				
				model.reassignIDs();
				
			}// End if selected element is not null
			else { // Check dashed lines (room separators)
				// Transform click to match line
				double cornerLimit = model.getDRAWING_ICON_DIMENSION() / 2 * model.getZOOM();
				pointPressed.setX((2*pointPressed.getX()-cornerLimit)/2);
				pointPressed.setY((2*pointPressed.getY()-cornerLimit)/2);
				
				// Check if any RoomSeparator collides with click
				for(RoomSeparator rs: model.getRoomSeparators()) { 
					if (PointIntersectsLine.pointIntersectsLine(rs, pointPressed, model.getDRAWING_ICON_DIMENSION(), model.getZOOM())) {
						model.removeRoomSeparator(rs, false);
						model.eraseDrawable(rs);
						break;
					}
				}
			}
			
			// Always refresh view
			view.refresh();
			
		} // End if tool was eraser
		
	}
	
	/**
	 * If any painted element selected, set it as selected.
	 * 
	 * @param p		Click's position (which may collide with some selected element)
	 */
	public void select(Point p) {
		
		// Set object selected (if any)
		model.setObjectSelected(null);
		for (Drawable d: model.getPaintedElements()) {
        	if(d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION())) {
        		model.setObjectSelected(d);
        		model.diffPoint = new Point(p.getX() - d.getVertex_xy().getX(), p.getY() - d.getVertex_xy().getY());
        		break;
        	}
		}
	}
	
	/**
	 * Set element as selected (if any selected) and update status bar.
	 * 
	 * @param pointPressed
	 */
	private void cursor(Point pointPressed) {
		
		// Modify status bar
		Drawable d = model.getObjectSelected();
		String selectedObject = "None";
		Icon icon = null;
		
		if (d != null) {
			selectedObject = d.getClass().getSimpleName();
			icon = new ImageIcon(new ImageIcon(d.getUrlIcon()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT));
		}
		
		view.selectedElement.setText("Selected: " + selectedObject);
		view.selectedElement.setIcon(icon);
		
		view.refresh();
		
	}
	
	@Override
	public void move(Point p) {
		
		if (p != null) { // If p isn't null
			
			Point movedPoint = p;
			
			if(model.getToolClicked() == ToolButtons.mover)
				movedPoint = new Point(p.getX() - model.diffPoint.getX(), p.getY() - model.diffPoint.getY());
			
			// Check max values
			checkPointPosition(movedPoint);
			// Modify position
			model.getObjectSelected().setVertex_xy(movedPoint);
			view.refresh();
		}
	}

	@Override
	public void validateMovement() {
		// Get selected object that has been moved
		Drawable d = model.getObjectSelected();
		
		 // Get the type of object
		if (d instanceof Corner) {
			// Get corner with new coordinates and check if the new position is correct
			Corner c = (Corner) d;
			
			int index = model.getCornerRoomIndex(c);
			
			// PREVIOUS: if (model.elementCollides(c) || model.collidesRooms(c, index))
			if (model.collidesRooms(c, index)) { // Check with new coordinates
				// If not correct, modify corner's position -> Return to previous position
				model.getObjectSelected().setVertex_xy(model.getOldObjectCoordinates());
			}
			else {
				// If correct -> update room's polygon
				model.getRooms().get(index).refreshPolygon();
			}
		}
		else if (d instanceof Item) {
			
			Item i = (Item) d;
			
			Room r = model.isInsideRoom(i.getVertex_xy(), -1);
			if (r != null) {
				i.setRoom(r);
			}
			else {
				i.setVertex_xy(model.getOldObjectCoordinates());
			}
		}
		else if (d instanceof Door) {
			
			Door door = (Door) d;
			
			Room r = model.isInsideRoom(door.getVertex_xy(), -1);

			if (r != null) {
				// Update door's room and ex and current room's doors
				model.updateDoorRoom(door, r);
			} // else keep the same room
		}
		
		view.refresh();
	}
	
	/**
	 * Called when we want to complete the room that it's being created. Join the last corner with the first corner (wall) and persist room.
	 */
	private void completeRoom() {
		// If painting corners and there are at least
		if (model.getToolClicked() == ToolButtons.pencil && model.getSelected() == IconButtons.corner) {
			// If there are painted 3 or more corners, you can create a room.
			if (model.getCurrentRoomCorners().size() >= 3) {
				
				Corner c = model.getCurrentRoomCorners().get(0); // Get first corner to check collisions
				model.getCurrentRoomCorners().remove(0); // Remove it from list to check collisions
				boolean collides = model.collidesRooms(c, -1); // Check with the removed corner
				model.getCurrentRoomCorners().add(0, c); // Add corner again to list
				
				// If it doesn't collide with existing rooms
				if(!collides) {
					// Erase current room corners from paintedElements
					model.eraseDrawableList(new ArrayList<>(model.getCurrentRoomCorners()));
					
					// Create a new room with the current room corners
					Room room = new Room(model.getNumRooms()+1, model.getCurrentRoomCorners()); // Modificado por Nacho Palacio 2025-04-18

					if(model.addRoom(room)) {// (this adds the corners to persist to drawable elements)
						model.emptyCurrentRoomCorners();
					}
					else {
						view.showMessage(JOptionPane.ERROR_MESSAGE, "Couldn't add room to map");
					}
				}
				else { // If collides with existing room/ its own walls
					view.showMessage(JOptionPane.WARNING_MESSAGE, "The current room collides with existing/itself. Couldn't add room to map");
				}
			}
			else { // If not painted at least 3 corners to form a room -> WARN the user.
				view.showMessage(JOptionPane.WARNING_MESSAGE, "There must be at least 3 corners painted to create a room");
			}
		} // If not painting or painting but not corners -> Ignore
		
		view.refresh();
	}
	
	/**
	 * Cancel current (unfinished) drawings. Current drawings can be a room (several corners) or a room separator (two corners)
	 */
	private void cancelCurrentDrawings() {
		model.eraseCurrentRoomCorners();
		model.setCurrentRoomSeparatorCorner(null);
	}

	

	/**
	 * When buttons pressed -> ActionEvent.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getSource().getClass().getName()) {
		case "javax.swing.JButton":
			buttonHandler((JButton) e.getSource());
			break;
		case "javax.swing.JTextField":
			textFieldHandler((JTextField) e.getSource());
			break;
			
		}
		
	}
	
	@Override
	public void focusGained(FocusEvent e) {
		// Not needed
	}

	@Override
	public void focusLost(FocusEvent e) {
		switch (e.getSource().getClass().getName()) {
		case "javax.swing.JTextField":
			textFieldHandler((JTextField) e.getSource());
			break;
		default:
			view.mapPanel.mouseReleased(new MouseEvent(view.mapPanel, 0, 0, 0, 100, 100, 1, false));
			break;
		}
	}

	private void buttonHandler(JButton source) {
		//System.out.println(source.getName());
		String sourceName = source.getName();
		String typeOfButton = sourceName.split("\\_")[0];
		
		if (typeOfButton.equals(EditorLiterals.TOOLS_SECTION)) {
	        switch (sourceName) {
	            case EditorLiterals.PENCIL:
	                model.setToolClicked(ToolButtons.pencil);
	                break;
	            case EditorLiterals.ERASER:
	                model.setToolClicked(ToolButtons.eraser);
	                // model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN DOING THAT ACTION?????
	                break;
	            case EditorLiterals.CURSOR:
	                model.setToolClicked(ToolButtons.cursor);
	                // model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN DOING THAT ACTION?????
	                break;
	            case EditorLiterals.MOVER:
	                model.setToolClicked(ToolButtons.mover);
	                // model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN DOING THAT ACTION?????
	                break;
	        }
		}
		else if (typeOfButton.equals(EditorLiterals.ICONS_SECTION)) {
			switch (sourceName) {
	            case EditorLiterals.CORNER:
	                model.setSelected(IconButtons.corner);
	                break;
	            case EditorLiterals.DOOR:
	            	model.setSelected(IconButtons.door);
	            	// model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN PAINTING THAT BUTTON'S OBJECT?????
	                break;
	            case EditorLiterals.STAIRS:
	            	model.setSelected(IconButtons.stairs);
	            	// model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN PAINTING THAT BUTTON'S OBJECT?????
	                break;
	            case EditorLiterals.ROOMSEPARATOR:
	            	model.setSelected(IconButtons.roomSeparator);
	            	// model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN PAINTING THAT BUTTON'S OBJECT?????
	                break;
	            default:
	            	if (sourceName.matches(EditorLiterals.VISITABLE + "_(.*)$")) {
	            		model.setSelected(IconButtons.visitable);
	            		model.setVisitableSelected(sourceName.split("\\_")[2]);
	            	}
	            	// model.eraseCurrentRoomCorners(); ����ERASE SELECTION WHEN PRESSING OTHER BUTTON OR WHEN PAINTING THAT BUTTON'S OBJECT?????
	                break;
	        }
		}
		else if (typeOfButton.equals(EditorLiterals.VISITABLE_ICONS_TOOLS)) {
			switch (sourceName) {
	            case EditorLiterals.ADD_VISITABLE:
	            	AddVisitableView addVisitable = new AddVisitableView(view);
	        		addVisitable.setVisible(true);
	                break;
	            case EditorLiterals.DELETE_VISITABLE:
	            	DeleteVisitableView deleteVisitable = new DeleteVisitableView(view, model);
	            	deleteVisitable.setVisible(true);
	                break;
	            case EditorLiterals.EDIT_VISITABLE:
	            	EditVisitableObjectView editVisitable = new EditVisitableObjectView(view, model);
	            	editVisitable.setVisible(true);
	                break;
	            case EditorLiterals.SAVE_VISITABLE:
	                break;
	            case EditorLiterals.LOAD_VISITABLE:
	                break;
	        }
		}
	}

	private void textFieldHandler(JTextField source) {
		//System.out.println(source.getName());
		String sourceName = source.getName();
		String typeOfTextField = sourceName.split("\\_")[0];
		
		String textValue = source.getText();
		
		if (typeOfTextField.equals(EditorLiterals.MAP_SETTINGS_SECTION)) {
	        switch (sourceName) {
	        
		        case EditorLiterals.MAP_NAME:
	            	model.setName(textValue);
	                break;
	                
		        case EditorLiterals.MAP_FLOOR:
	            	model.setFloor(textValue);
	                break;
	                
	            case EditorLiterals.MAP_WIDTH:
	            	try {
	            		int width = Integer.valueOf(textValue);
	            		view.mapPanel.setMAP_W(width);
	            	}
	            	catch (Exception e) {
	            		view.showMessage(JOptionPane.ERROR_MESSAGE, "Incorrect format: value must be double");
	            	}
	                break;
	                
	            case EditorLiterals.MAP_HEIGHT:
	            	try {
	            		int height = Integer.valueOf(textValue);
	            		view.mapPanel.setMAP_H(height);
	            	}
	            	catch (Exception e) {
	            		view.showMessage(JOptionPane.ERROR_MESSAGE, "Incorrect format: value must be double");
	            	}
	                break;
	                
	            case EditorLiterals.MAP_PIXEL_REPRESENTS_IN_METERS:
	            	try {
	            		double pixelInMeters = Double.valueOf(textValue);
	            		view.mapPanel.setPixelRepresentsInMeters(pixelInMeters);
	            	}
	            	catch (Exception e) {
	            		view.showMessage(JOptionPane.ERROR_MESSAGE, "Incorrect format: value must be double");
	            	}
	                break;
	                
	            case EditorLiterals.MAP_ZOOM:
	            	try {
	            		double z = Double.valueOf(textValue);
	            		view.mapPanel.setZOOM(z);
	            	}
	            	catch (Exception e) {
	            		view.showMessage(JOptionPane.ERROR_MESSAGE, "Incorrect format: value must be double");
	            	}
	            	
	                break;
	                
	            case EditorLiterals.MAP_ICONDIMENSIONS:
	            	try {
	            		int newDim = Integer.valueOf(textValue);
	            		model.setDRAWING_ICON_DIMENSION(newDim);
	            	}
	            	catch (Exception e) {
	            		view.showMessage(JOptionPane.ERROR_MESSAGE, "Incorrect format: value must be double");
	            	}
	            	
	                break;
	        }
		}
	}
	
	/**
	 * If want to use ComboBoxes/CheckBoxes/etc. -> ItemListener (ItemEvent e in itemStateChanged(e))
	 * 
	 * If want to use JTextFields (to change model parameters) -> FocusListener (FocusEvent e in focusGained(e) and focusLost(e)
	 *  ^
	 *  |
	 *  USED!!
	 */
}
