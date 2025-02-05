package es.unizar.dao;

import es.unizar.database.Database;
import es.unizar.util.Literals.Databases;

/**
 * DAO Factory following DAO Pattern.
 * 
 * The following tutorial explains the process: https://youtu.be/G6cHURngWzg
 * @author apied
 *
 */
public abstract class DAOFactory {

	/**
	 * Creates a Queue DB using the @param dbURL to make CRUD ops
	 * @param dbURL
	 * @return
	 */
	public abstract DataManagementQueueDB getQueueDB(String dbURL, Database db);

	/**
	 * Creates a User DB using the @param dbURL to make CRUD ops
	 * @param dbURL
	 * @return
	 */
	public abstract DataManagementUserDB getUserDB(String dbURL, Database db);
	
	/**
	 * Get Database Factory depending on the type of database wanted ( @param db )
	 * @param db
	 * @return
	 */
	public static DAOFactory getFactory(Databases db) {
		
		switch(db) {
			case SQLITE:
				return new SQLiteDAOFactory();
			// case MEMORY:
				// return new MemoryDAOFactory();
			default:
				return null;
		}
		
	}

}