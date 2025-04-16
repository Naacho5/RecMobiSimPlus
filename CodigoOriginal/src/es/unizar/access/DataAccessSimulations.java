package es.unizar.access;

import java.io.File;

import es.unizar.util.Literals;

/**
 * Access to the values of the properties stored in the (neglected) simulations file.
 *
 * @author Alejandro Piedrafita Barrantes
 */
public class DataAccessSimulations extends DataAccess {

	public DataAccessSimulations(File file) {
		super(file);
	}

	public int getNumberOfSimulations() {
		return Integer.valueOf(getPropertyValue(Literals.NUMBER_ROOM)).intValue();
	}
	
}
