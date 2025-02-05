package es.unizar.dao;

import java.sql.Connection;
import java.sql.SQLException;

import es.unizar.gui.simulation.InformationToPropagate;

public interface DataManagementUserDB {

	String USER_ITEM_CONTEXT_TB = "user_item_context";
	
	/**
	 * Connects to the database creating connection and statement.
	 * 
	 * @param dbURL
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	void connect(String dbURL) throws ClassNotFoundException, SQLException;
	
	/**
	 * Returns the database connection instance.
	 * 
	 * @return Connection
	 */
	Connection getConnection();
	
	/**
	 * Disconnects from the database.
	 * 
	 * @throws SQLException
	 */
	void disconnect() throws SQLException;

	/**
	 * Insert the rating of a specific user, by using the information to propagate (user, item, context, rating, TTL, TTP, isTTPInitialized, location, currentTime) into db_user.db, if it does not
	 * contain it.
	 * 
	 * @param information: The information to propagate.
	 * @param numMaxItems: Number of items in simulation (to store just info from items -> Constraint)
	 * @return
	 */
	boolean insertInformation(InformationToPropagate information, int numMaxItems);

	/**
	 * Insert information into db_user.db.
	 * 
	 * @param user:         The user ID.
	 * @param item:         The item ID.
	 * @param context:      The context ID.
	 * @param rating:       The rating.
	 * @param opinion:      The textual opinion.
	 * @param userProvided: 1 if the rating was provided by the user and 0 if the rating was generated automatically from the textual opinion.
	 * @return
	 */
	boolean insert(long user, long item, long context, double rating, String opinion, int userProvided);

	/**
	 * Delete all information stored in the user_item_context table from db_user.db.
	 */
	void deleteAllInformationFromTable();

	/**
	 * Delete the information stored in the user_item_context table from db_user.db for a specific user.
	 */
	void deleteInformationFromTable(long userID);

	/**
	 * Creates a new SQLite database.
	 *
	 */
	void createNewDatabase();

}