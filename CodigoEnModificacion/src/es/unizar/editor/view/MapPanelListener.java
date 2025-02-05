package es.unizar.editor.view;

import es.unizar.editor.model.Point;

public interface MapPanelListener {
	
	void hovered(Point p);
	
	void manageClick(Point pointPressed, int buttonPressed, boolean wasDoubleClick);
	
	void move(Point p);

	void validateMovement();

}
