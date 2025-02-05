package es.unizar.editor.model;

import java.util.List;

public interface Connectable {

	/**
	 * Returns a list of the already connected elements.
	 * 
	 * @return List<Connectable> connected.
	 */
	public List<Connectable> getConnectedTo();

	/**
	 * Connects the connectable element to another connectable element (if possible).
	 * 
	 * @param connectTo
	 * @return T/F (connection was added or not/already existed).
	 */
	public boolean connectTo(Connectable connectTo);
	
	/**
	 * Should be private and only called in connectTo() from the initial Connectable
	 * element which wants to establish a connection.
	 * 
	 * @param connection 
	 * @return T/F (connection was added or not/already existed).
	 */
	boolean addConnection(Connectable connection);
	
	/**
	 * Disconnects, if possible, from connectable element.
	 * 
	 * @param disconnectFrom
	 * @return T/F (disconnected or not)
	 */
	public boolean disconnectFrom(Connectable disconnectFrom);
	
	/**
	 * Disconnects all of his connected elements from itself.
	 * @return T/F (all disconneccted or not).
	 */
	public boolean disconnectFromAll();

}