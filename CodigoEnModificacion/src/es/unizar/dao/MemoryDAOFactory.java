package es.unizar.dao;

/**
 * EXCLUDED FROM BUILD PATH -> DO NOT USE.
 * 
 * @author apied
 *
 */
public class MemoryDAOFactory extends DAOFactory {

	@Override
	public DataManagementQueueDB getQueueDB(String dbURL) {
		return new MemoryDataManagementQueueDB(dbURL);
	}

	@Override
	public DataManagementUserDB getUserDB(String dbURL) {
		return new MemoryDataManagementUserDB(dbURL);
	}

}
