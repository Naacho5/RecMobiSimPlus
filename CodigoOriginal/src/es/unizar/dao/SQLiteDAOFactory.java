package es.unizar.dao;

import es.unizar.database.Database;

public class SQLiteDAOFactory extends DAOFactory {

	/**
	 * Creates a SQLiteQueue DB using the @param dbURL to make CRUD ops
	 * @param dbURL
	 * @return
	 */
	@Override
	public DataManagementQueueDB getQueueDB (String dbURL, Database db) {
		return new SQLiteDataManagementQueueDB(dbURL, db);
	}
	
	/**
	 * Creates a User DB using the @param dbURL to make CRUD ops
	 * @param dbURL
	 * @return
	 */
	@Override
	public DataManagementUserDB getUserDB (String dbURL, Database db) {
		return new SQLiteDataManagementUserDB(dbURL, db);
	}
}
