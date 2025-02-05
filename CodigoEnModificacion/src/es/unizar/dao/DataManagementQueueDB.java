package es.unizar.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import es.unizar.gui.simulation.InformationToPropagate;

public interface DataManagementQueueDB {

	int NUMBER_RATINGS_TO_TRANSFER = 1;
	String INFORMATION_TB = "information";
	
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
	 * Insert the information of a user in the queue.db database, if it does not
	 * contain it.
	 * 
	 * @param information: The information to propagate (id_user, user, item,
	 *                     context, rating, TTL, TTP, isTTPinitialized, location and
	 *                     currentTime).
	 * @return True if the information was correctly inserted and False in the
	 *         otherwise.
	 */
	boolean insertInformation(InformationToPropagate information);

	/**
	 * Insert the information of a user in the queue.db database, if it does not
	 * contain it.
	 * 
	 * @param id_user:     The user ID.
	 * @param information: The information to propagate (id_user, user, item,
	 *                     context, rating, TTL, TTP, isTTPinitialized, location and
	 *                     currentTime).
	 * @return True if the information was correctly inserted and False in the
	 *         otherwise.
	 */
	boolean insertInformation(long id_user, InformationToPropagate information);

	/**
	 * Get a list of InformationToPropagate that contains TTP <= 0.
	 * 
	 * @return A list of InformationToPropagate that contains TTP <= 0.
	 */
	LinkedList<InformationToPropagate> getInformation();

	/**
	 * Get a list of InformationToPropagate that contains TTP <= 0 for a specific
	 * non-RS user.
	 * 
	 * @param id_user
	 * @return
	 */
	LinkedList<InformationToPropagate> getInformation(long id_user);

	/**
	 * Get a list of users with TTP <= 0 and isTTPInitialized == 1.
	 * 
	 * @return A list of users with TTP <= 0 and isTTPInitialized == 1.
	 */
	LinkedList<Long> getUsersWithInformationToPropagate();

	/**
	 * Get a list that contains the number of items per user without the TTP
	 * initialized.
	 * 
	 * @return A list that contains the number of items per user without the TTP
	 *         initialized.
	 */
	LinkedList<String> getNumberItemsByUserWithoutInitializeTTP();

	/**
	 * Delete a rows with the specific information.
	 * 
	 * @param information: The information to propagate.
	 */
	void deleteInformation(InformationToPropagate information);

	/**
	 * Delete all information stored into queue.db.
	 */
	void deleteAllInformationFromTable();

	/**
	 * Updates the exchange between users.
	 * 
	 * @param farthestUser: The farthest user.
	 * @param id_user:      The non-RS user ID.
	 * @return The number of updating.
	 */
	int updateExchange(long farthestUser, long id_user);

	/**
	 * Updates the TTL values.
	 */
	void updateTTL();

	/**
	 * Updates the TTP values.
	 */
	void updateTTP();

	/**
	 * Initializes all TTPs.
	 */
	void initializeAllTTP();

	/**
	 * Initializes all TTPs for a specific non-RS user.
	 * 
	 * @param countRatings: Number of ratings.
	 * @param id_user:      The non-RS user ID.
	 */
	void initializeAllTTP(int countRatings, long id_user);

	/**
	 * Initializes the TTP from a specific information.
	 * 
	 * @param information: The information to propagate.
	 */
	void initializeOneTTP(InformationToPropagate information);

}