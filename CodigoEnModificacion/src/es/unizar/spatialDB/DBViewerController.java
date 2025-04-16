package es.unizar.spatialDB;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.Point;
import es.unizar.editor.view.MapPanelListener;
import es.unizar.util.Distance;
import es.unizar.util.EditorLiterals;

public class DBViewerController implements ActionListener, FocusListener, MapPanelListener {

	DBViewer viewer;
	DBViewerModel model;
		
	public DBViewerController(DBViewer viewer, DBViewerModel model) {
		this.viewer = viewer;
		this.model = model;
	}

	@Override
	public void hovered(Point p) {
//		model.setCurrentCursorPoint(p);
		
		viewer.statusBarX.setText("x:" + p.getX() + " (" + EditorLiterals.decimalFormat.format(p.getX()*model.getPixelRepresentsInMeters()) +" m)");
        viewer.statusBarY.setText("y:" + p.getY() + " (" + EditorLiterals.decimalFormat.format(p.getY()*model.getPixelRepresentsInMeters()) +" m)");
        
        String hoveredItem = "None";
        boolean elementCollides = false;
        for (Drawable d: model.getRoomNumbers()) {
        	elementCollides = d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION());
        	
        	if (elementCollides) {
        		viewer.hoveredElement.setIcon(new ImageIcon(new ImageIcon(d.getUrlIcon()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)));
        		hoveredItem = d.getClass().getSimpleName();
        		break;
        	}
        }
        
        // If previously hovered was drawable with icon and now no drawable is hovered -> Remove icon
        if (!elementCollides) {
        	
        	for (Drawable d: model.getItems()) {
            	elementCollides = d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION());
            	
            	if (elementCollides) {
            		viewer.hoveredElement.setIcon(new ImageIcon(new ImageIcon(d.getUrlIcon()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)));
            		hoveredItem = d.getClass().getSimpleName();
            		break;
            	}
            }
        	
        	if (!elementCollides) {
        		viewer.hoveredElement.setIcon(null);
        	}
        }
        
        viewer.hoveredElement.setText("Hovered: " + hoveredItem);
		
	}

	@Override
	public void manageClick(Point pointPressed, int buttonPressed, boolean wasDoubleClick) {
//		System.out.println(model.getObjectSelected()+" "+model.getPreviousObjectSelected()+" "+buttonPressed+" "+wasDoubleClick);
		String selectedType = select(pointPressed);
		if (buttonPressed == MouseEvent.BUTTON1) { // Single, left click
			cursor(pointPressed);
			// If it was double click, open object properties window
			if (wasDoubleClick && model.getObjectSelected() != null) {
				if(selectedType == "room") {
					if(model.getPreviousObjectSelected() != null) {
						viewer.deselectRoomInList(model.getPreviousObjectSelected());
//						model.setPreviousObjectSelected(null);
					}
					viewer.showRoomInList(model.getObjectSelected());
					model.setPreviousObjectSelected(model.getObjectSelected());
				}
				else viewer.mapPanel.objectProperties(model.getObjectSelected());
			}else if(wasDoubleClick && model.getObjectSelected() == null && model.getPreviousObjectSelected() != null) {
				viewer.deselectRoomInList(model.getPreviousObjectSelected());
				model.setPreviousObjectSelected(null);
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
		
		viewer.selectedElement.setText("Selected: " + selectedObject);
		viewer.selectedElement.setIcon(icon);
		
		viewer.refresh();
		
	}
	
	/**
	 * If any painted element selected, set it as selected.
	 * 
	 * @param p		Click's position (which may collide with some selected element)
	 */
	public String select(Point p) {
		
		// Set object selected (if any)
		model.setObjectSelected(null);
		for (Drawable d: model.getRoomNumbers()) {
//        	if(d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION())) {
    		if(Distance.distanceBetweenTwoPoints(p.getX(), p.getY(), d.getVertex_xy().getX(), d.getVertex_xy().getY()) <= 10) {
    			model.setObjectSelected(d);
        		model.diffPoint = new Point(p.getX() - d.getVertex_xy().getX(), p.getY() - d.getVertex_xy().getY());
        		break;
        	}
		}
		if(model.getObjectSelected() != null) return "room";
		for (Drawable d: model.getItems()) {
        	if(d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION())) {
//    		if(Distance.distanceBetweenTwoPoints(p.getX(), p.getY(), d.getVertex_xy().getX(), d.getVertex_xy().getY()) <= 17) {
    			model.setObjectSelected(d);
        		model.diffPoint = new Point(p.getX() - d.getVertex_xy().getX(), p.getY() - d.getVertex_xy().getY());
        		break;
        	}
		}
		if(model.getObjectSelected() != null) return "item";
		for (Drawable d: model.getDrawables()) {
//        	if(d.pointCollidesItem(p, model.getDRAWING_ICON_DIMENSION())) {
    		if(Distance.distanceBetweenTwoPoints(p.getX(), p.getY(), d.getVertex_xy().getX(), d.getVertex_xy().getY()) <= 10) {
    			model.setObjectSelected(d);
        		model.diffPoint = new Point(p.getX() - d.getVertex_xy().getX(), p.getY() - d.getVertex_xy().getY());
        		break;
        	}
		}
		if(model.getObjectSelected() != null) return "connectable";
		else return null;
		
	}

	@Override
	public void move(Point p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validateMovement() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getSource().getClass().getName()) {
		case "javax.swing.JTextField":
			try {
        		double z = Double.valueOf(((JTextField) e.getSource()).getText());
        		viewer.mapPanel.setZOOM(z);
        	}
        	catch (Exception ex) {
        		JOptionPane.showMessageDialog(DBViewer.dbViewer, "Incorrect format: value must be double", "ERROR", JOptionPane.ERROR_MESSAGE);
        	}
			break;
			
		}
		
	}

}
