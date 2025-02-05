package es.unizar.editor.model;

import java.util.LinkedList;
import java.util.List;

import es.unizar.util.Literals;

public class Door extends Drawable implements Connectable {

	// List can contain more than one connectable.
	private List<Connectable> connectedTo;
	
	
	public Door(Room room, long vertex_label, Point vertex_xy) {
		super(room, vertex_label, vertex_xy);
		this.setUrlIcon(Literals.IMAGES_PATH + "door.png");
		connectedTo = new LinkedList<Connectable>();
	}

	@Override
	public List<Connectable> getConnectedTo() {
		return connectedTo;
	}

	@Override
	public boolean connectTo(Connectable connectTo) {
		boolean connected = false;
		if (connectTo != null && !connectedTo.contains(connectTo) && connectTo.addConnection(this))
			connected = connectedTo.add(connectTo);
		
		return connected;
	}

	@Override
	public boolean addConnection(Connectable connection) {
		boolean connected = false;
		if (connection != null && !connectedTo.contains(connection))
			connected = connectedTo.add(connection);
		
		return connected;
	}

	@Override
	public boolean disconnectFrom(Connectable disconnectFrom) {
		boolean disconnected = false;
		if (disconnectFrom != null && disconnectFrom instanceof Door) {
			if (((Door) disconnectFrom).disconnect(this)) {
				disconnected = connectedTo.remove(disconnectFrom);
			}
		}
		else {
			disconnected = connectedTo.remove(disconnectFrom);
		}
		
		return disconnected;
	}
	
	private boolean disconnect(Connectable disconnectFrom) {
		return connectedTo.remove(disconnectFrom);
	}
	
	@Override
	public boolean disconnectFromAll() {
		for (Connectable c: this.connectedTo) {
			if (!((Door) c).disconnect(this)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		return "Door: " + super.toString() + ", " + connectedTo.size();
	}
	
}
