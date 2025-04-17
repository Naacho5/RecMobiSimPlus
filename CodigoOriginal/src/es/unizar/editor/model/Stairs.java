package es.unizar.editor.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

public class Stairs extends Drawable implements Connectable {
	
	// List can contain more than one connectable.
	private Connectable connectedTo;

	public Stairs(Room room, long vertex_label, Point vertex_xy) {
		// super(room, vertex_label, vertex_xy);

		/* Añadido por Nacho Palacio 2025-04-17. */
    	super(room, ElementIdMapper.convertToRangeId(vertex_label, ElementIdMapper.CATEGORY_STAIRS), vertex_xy);
		this.setUrlIcon(Literals.IMAGES_PATH + "stairs.png");
	}

	@Override
	public List<Connectable> getConnectedTo() {
		return new ArrayList<>(Arrays.asList(connectedTo));
	}

	/**
	 * Connect only to another stairs.
	 */
	@Override
	public boolean connectTo(Connectable connectTo) {
		boolean connected = false;
		
		if (connectTo != null && connectTo instanceof Stairs && connectTo.addConnection(this)) {
			// If connectedTo not null, disconnect it from previous connection
			// If this disconnect is not done, connectedTo will still be connected to this, which is not correct
			Stairs s = (Stairs) this.connectedTo;
			if (s != null)
				s.disconnectExcept(this);
			
			this.connectedTo = connectTo;
			connected = true;
		}
		
		return connected;
	}

	@Override
	public boolean addConnection(Connectable connection) {
		boolean connected = false;
		
		if (connection != null && connection instanceof Stairs) {
			// If connectedTo not null, disconnect it from previous connection
			// If this disconnect is not done, connectedTo will still be connected to this, which is not correct
			Stairs s = (Stairs) this.connectedTo;
			if (s != null)
				s.disconnectExcept(this);
			
			this.connectedTo = connection;
			connected = true;
		}
		
		return connected;
	}

	@Override
	public boolean disconnectFrom(Connectable disconnectFrom) {
		boolean disconnected = false;
		
		if (disconnectFrom != null && disconnectFrom instanceof Stairs) {
			((Stairs) disconnectFrom).disconnect();
			this.connectedTo = null;
			disconnected = true;
		}
		
		return disconnected;
	}
	
	private void disconnect() {
		connectedTo = null;
	}
	
	private void disconnectExcept(Stairs stairs) {
		if (stairs != null && stairs != this.connectedTo)
			this.connectedTo = null;
		
	}
	
	@Override
	public boolean disconnectFromAll() {
		if (this.connectedTo != null) {
			Stairs s = (Stairs) this.connectedTo;
			s.disconnect();
			
			return true;
		}
		else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		String s = "Stairs: " + super.toString() + ", ";
		if (connectedTo != null) {
			s += ((Drawable) connectedTo).getVertex_label();
		}
		else {
			s += "[None]";
		}
		
		return s;
	}
	
}
