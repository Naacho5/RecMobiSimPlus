/*
 * @(#)Context.java  1.0.0  27/09/14
 *
 * MOONRISE
 * Webpage: http://webdiis.unizar.es/~maria/?page_id=250
 * 
 * University of Zaragoza - Distributed Information Systems Group (SID)
 * http://sid.cps.unizar.es/
 *
 * The contents of this file are subject under the terms described in the
 * MOONRISE_LICENSE file included in this distribution; you may not use this
 * file except in compliance with the License.
 *
 * Contributor(s):
 *  RODRIGUEZ-HERNANDEZ, MARIA DEL CARMEN <692383[3]unizar.es>
 *  ILARRI, SERGIO <silarri[3]unizar.es>
 */
package es.unizar.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.mahout.cf.taste.common.TasteException;

import es.unizar.database.DBConnection;
import es.unizar.database.Database;
import es.unizar.gui.Configuration;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;

import java.util.logging.Level;
import java.util.logging.Logger;
import es.unizar.util.ElementIdMapper;

/**
 * Access to the data from a database. File taken from MOONRISE.jar (and optimized).
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class DataAccessLayer extends DBConnection implements DataAccess {

	private static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	
	private Database dbInstance = null;

	/**
	 * Constructor.
	 *
	 * @param dbURL
	 *            Database URL.
	 */
	public DataAccessLayer(String dbURL, Database db) {
		super(dbURL);
		dbInstance = db;
		try {
			connect(dbURL);
		}
		catch (ClassNotFoundException | SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Connects to database if not connected yet.
	 * 
	 * @param dbURL
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void connect(String dbURL) throws ClassNotFoundException, SQLException {
		
		dbInstance.connect(dbURL);
		
		//System.out.println(getConnection() + " -> " + dbURL);
		
		/*
		 * PREVIOUS - Now there's only one connection
		 * First implementation and info. Now only one connection and connecting (if it isn't open yet) to that connection)
		 * 
		// If already connected, skip
		if (connection != null)
			return;
		
		// Connection to the DB
		Class.forName(SQLITE);
		connection = DriverManager.getConnection(dbURL);
		
		// Write-Ahead Logging in order to improve db r/w performance
		// @see https://www.sqlite.org/wal.html
		
		// WARNING: Sqlite library must be >= 3.7.0.
		// @see https://stackoverflow.com/questions/6653648/how-to-implement-write-ahead-logging-of-sqlite-in-java-program
		
		// PreparedStatement pr = connection.prepareStatement("PRAGMA journal_mode=WAL");
		// boolean pragmaWAL = pr.execute();
		// System.out.println(connection + ";PRAGMA journal_mode=WAL: "+pragmaWAL);
		
		// VITAL -> DB PERFORMANCE SUPER OPTIMIZED
		// @see https://dba.stackexchange.com/questions/252445/how-does-autocommit-off-affects-bulk-inserts-performance-in-mysql-using-innodb
		// @see http://www.w3big.com/es/sqlite/sqlite-java.html
		//connection.setAutoCommit(false);
		//connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		*/
	}
	
	/**
	 * Returns the database connection.
	 * 
	 * @return connection
	 */
	public Connection getConnection() {
		
		return dbInstance.getConnection();
	}
	
	/**
	 * Disconnects from database.
	 * 
	 * @throws SQLException
	 */
	public void disconnect() throws SQLException {
		
		dbInstance.disconnect();
	}

	/**
	 * Gets a list with all the item identifiers.
	 *
	 * @return A list with all the item identifiers.
	 */
	public List<Integer> getItemIDs() {
		ResultSet resultSet = null;
		List<Integer> listOfIdItems = new LinkedList<Integer>();
		
		try {
		
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_item FROM item");
			
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				listOfIdItems.add(resultSet.getInt("id_item"));
			}
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return listOfIdItems;
	}

	/**
	 * Gets the number of items.
	 *
	 * @return Number of items.
	 */
	public int getNumberOfItems() {
		int numberOfItems = 0;
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT count(id_item) as ItemCount FROM item");
			ResultSet resultSet = select.executeQuery();
			numberOfItems = resultSet.getInt("ItemCount");
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return numberOfItems;
	}

	/**
	 * Gets a list with all the user identifiers.
	 *
	 * @return A list with all the user identifiers.
	 */
	public long[] getUserIDs() {
		ResultSet resultSet = null;
		long[] userIDs = new long[getNumberOfUsers()];
		int i = 0;
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_user FROM user");
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				userIDs[i] = resultSet.getInt("id_user");
				i++;
			}
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return userIDs;
	}

	/**
	 * Gets the number of users.
	 *
	 * @return Number of users.
	 */
	public int getNumberOfUsers() {
		int numberOfUsers = 0;
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT count(id_user) as UserCount FROM user");
			ResultSet resultSet = select.executeQuery();
			
			numberOfUsers = resultSet.getInt("UserCount");
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return numberOfUsers;
	}

	/**
	 * Gets the users, items and ratings.
	 *
	 * @return ResultSet.
	 */
	@Override
	public List<String> getUserItemRating() {
		List<String> list = new LinkedList<>();
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_user,id_item,rating FROM user_item_context");
			ResultSet resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				long userID = resultSet.getLong(1);
				long itemID = resultSet.getLong(2);
				float rating = resultSet.getLong(3);
				list.add(userID + ";" + itemID + ";" + rating);
			}
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

	/**
	 * Gets the users, items and ratings.
	 *
	 * @return ResultSet.
	 */
	@Override
	public List<String> getUserItemRatingFrom(long userID) {
		List<String> list = new LinkedList<>();
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_user,id_item,rating FROM user_item_context WHERE id_user== ?");
			select.setLong(1, userID);
			ResultSet resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				long userId = resultSet.getLong(1);
				long itemId = resultSet.getLong(2);
				float rating = resultSet.getLong(3);
				list.add(userId + ";" + itemId + ";" + rating);
			}
			
			resultSet.close();
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

	/**
	 * Gets the users, items, contexts and ratings for a specific user.
	 *
	 * @return ResultSet.
	 */
	@Override
	public List<String> getUserItemContextRatingFor(long userID) {
		ResultSet resultSet = null;
		List<String> list = new LinkedList<>();
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_user,id_item,id_context,rating FROM user_item_context WHERE id_user== ? ORDER BY rating DESC");
			select.setLong(1, userID);
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				long userId = resultSet.getLong(1);
				long itemId = resultSet.getLong(2);
				long context = resultSet.getLong(3);
				float rating = resultSet.getLong(4);
				list.add(userId + ";" + itemId + ";" + context + ";" + rating);
			}
			
			resultSet.close();
			select.close();

		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

	@Override
	public List<String> getUserItemContextRatingRandomFor(long userID) {
		ResultSet resultSet = null;
		List<String> list = new LinkedList<>();
		try {
			
			List<String> notOrdered = new LinkedList<>();
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT DISTINCT id_user,id_item,id_context,rating FROM user_item_context WHERE id_user== ?");	// Order by random() in SQLite doesn't allow parameters -> Order list afterwards
																																										// ORDER BY RANDOM(" + Configuration.simulation.getSeed() + ")");
			select.setLong(1, userID);
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				long userId = resultSet.getLong(1);
				long itemId = resultSet.getLong(2);
				long context = resultSet.getLong(3);
				float rating = resultSet.getLong(4);
				notOrdered.add(userId + ";" + itemId + ";" + context + ";" + rating);
			}
			
			resultSet.close();
			select.close();
			
			// Order list by rand
			list = orderListByRand(notOrdered);

		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

	// SELECT id_user,id_item,id_context,rating FROM user_item_context WHERE
	// id_user==176 ORDER BY RANDOM() LIMIT 10;

	private List<String> orderListByRand(List<String> notOrdered) {
		Collections.shuffle(notOrdered, new Random(Configuration.simulation.getSeed()));
		return notOrdered;
		
	}


	/**
	 * Gets the users, items, contexts and ratings.
	 *
	 * @return ResultSet.
	 */
	@Override
	public ResultSet getUserItemContextRating() {
		ResultSet resultSet = null;
		try {
			
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_user,id_item,id_context,rating FROM user_item_context");
			resultSet = select.executeQuery();
			
			select.close();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return resultSet;
	}

	/**
	 * Gets the context ID from a list of context values.
	 *
	 * @param currentContextValues
	 *            A list of current context values.
	 * @return long.
	 */
	@Override
	public long getContextIDFor(List<Integer> currentContextValues) {
		ResultSet resultSet = null;
		long contextID = 0;
		String values = String.valueOf(currentContextValues.get(0));
		for (int i = 1; i < currentContextValues.size(); i++) {
			values += "," + currentContextValues.get(i);
		}
		try {
			// Query
			PreparedStatement select = getConnection().prepareStatement("SELECT id_context FROM context_variable WHERE id_variable  IN (" + values + ") GROUP BY id_context HAVING COUNT(id_variable) = " + currentContextValues.size());
			resultSet = select.executeQuery();
			
			contextID = resultSet.getLong(1);
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return contextID;
	}

	/**
	 * Gets a HashMap with the number of items by user. The key is the user.
	 *
	 * @return A HashMap with the number of items by user.
	 */
	public Map<Long, Integer> getHashWithNumberItemsByUser() {
		// Map<Long, Integer> hashWithNumberItemsByUser = new TreeMap<Long, Integer>();
		// try {
			
		// 	// Query
		// 	PreparedStatement select = getConnection()
		// 			.prepareStatement("SELECT id_user, count(id_item) AS ItemCount FROM user_item_context GROUP BY id_user");
		// 	ResultSet resultSet = select.executeQuery();
			
		// 	while (resultSet.next()) {
		// 		hashWithNumberItemsByUser.put(resultSet.getLong("id_user"), resultSet.getInt("ItemCount"));
		// 	}
			
		// } catch (SQLException e) {
		// 	log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
		// 	e.printStackTrace();
		// }
		
		// return hashWithNumberItemsByUser;

		/* Añadido por Nacho Palacio 2025-04-15. */
		Map<Long, Integer> hashWithNumberItemsByUser = new TreeMap<>();
		Connection conn = getConnection();
		try {
			// Primero, obtener todos los IDs de usuario
			Set<Long> allUserIds = new HashSet<>();
			try (PreparedStatement stmt = conn.prepareStatement("SELECT id_user FROM user")) {
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					allUserIds.add(rs.getLong("id_user"));
				}
			}
			
			// Luego, obtener el número de items por usuario
			try (PreparedStatement stmt = conn.prepareStatement(
					"SELECT id_user, count(id_item) AS ItemCount FROM user_item_context GROUP BY id_user")) {
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					hashWithNumberItemsByUser.put(rs.getLong("id_user"), rs.getInt("ItemCount"));
				}
			}
			
			// Finalmente, añadir usuarios sin preferencias con un contador de 0
			for (Long userId : allUserIds) {
				if (!hashWithNumberItemsByUser.containsKey(userId)) {
					hashWithNumberItemsByUser.put(userId, 0);
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return hashWithNumberItemsByUser;
	}

	/**
	 * Gets a list with the context variable names.
	 *
	 * @return A list with the context variable names.
	 */
	public List<String> getVariableNames() {
		ResultSet resultSet = null;
		List<String> variableNames = new LinkedList<String>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT name FROM variable_name");
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				variableNames.add(resultSet.getString("name"));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return variableNames;
	}

	/**
	 * Gets the variable name from a variable value.
	 *
	 * @param variableValue
	 *            The variable value.
	 * @return The variable name from a variable value.
	 */
	public List<String> getVariableNameFromVariableValue(String variableValue) {
		List<String> variableNames = new LinkedList<String>();
		ResultSet resultSet = null;
		
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT DISTINCT name FROM variable WHERE variable.value= ?");
			select.setString(1, variableValue);
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				variableNames.add(resultSet.getString("name"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return variableNames;
	}

	/**
	 * Gets a list with the possible values of a context variable.
	 *
	 * @param variableName
	 *            The name of the a variable.
	 * @return A list with the possible values of a context variable.
	 */
	public List<String> getPossibleVariableValues(String variableName) {
		ResultSet resultSet = null;
		List<String> possibleVariableValues = new LinkedList<String>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT value FROM variable WHERE name= ?");
			select.setString(1, variableName);
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				possibleVariableValues.add(resultSet.getString("value"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return possibleVariableValues;
	}

	/**
	 * Gets a list with the names and values of context variables from userID and itemID.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @return A list with the names and values of context variables from userID and itemID.
	 */
	public List<String> getVariableNameAndValue(long userID, long itemID) {
		ResultSet resultSet = null;
		List<String> variableNameAndValue = new LinkedList<String>();
		
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT name, value FROM variable, context_variable,user_item_context WHERE user_item_context.id_user= ? and user_item_context.id_item= ? and user_item_context.id_context=context_variable.id_context and variable.id_variable=context_variable.id_variable");
			select.setString(1, Long.toString(userID));
			select.setString(2,  Long.toString(itemID));
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				variableNameAndValue.add(resultSet.getString("name") + "__" + resultSet.getString("value"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return variableNameAndValue;
	}

	/**
	 * Gets a list with the names and weights of context variables.
	 *
	 * @return A list with the names and weights of context variables.
	 */
	public List<String> getVariableNameAndWeight(long userID) {
		ResultSet resultSet = null;
		List<String> variableNameAndWeight = new LinkedList<String>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT name, weight FROM user_variable_name WHERE id_user= ?");
			select.setInt(1, (int) userID);
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				variableNameAndWeight.add(resultSet.getString("name") + "__" + resultSet.getString("weight"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return variableNameAndWeight;
	}

	/**
	 * Determine the distance between two variable values that are similar
	 *
	 * @param variableValueX
	 * @param variableValueY
	 * @return
	 */
	public double distanceSoftVariableValues(String variableValueX, String variableValueY) {
		double distance = -1;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT A.distance FROM variable_variable A INNER JOIN variable F ON A.variable1=F.id_variable WHERE F.value= ? UNION SELECT A.distance FROM variable_variable A INNER JOIN variable F ON A.variable2=F.id_variable WHERE F.value= ?");
			select.setString(1, variableValueX);
			select.setString(2, variableValueY);
			ResultSet resultSet = select.executeQuery();
			
			distance = resultSet.getDouble(1);
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return distance;
	}

	/**
	 * Gets the user radius of action, according to the current transport way.
	 *
	 * @param profileId
	 *            The profile identifier.
	 * @param transportWayValue
	 *            The value of variable transport way.
	 * @return The user radius of action, according to the current transport way.
	 */
	public long getRadius(long userID, String transportWayValue) {
		long radius = 0;
		ResultSet resultSet = null;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT distance FROM distance, user, ca_profile, ca_profile_attribute WHERE ca_profile.id_ca_profile=distance.id_ca_profile and distance.id_ca_profile=user.id_ca_profile and user.id_user= ? and ca_profile_attribute.transportway= ? and distance.id_ca_profile_attribute=ca_profile_attribute.id_ca_profile_attribute");
			select.setLong(1, userID);
			select.setString(2, transportWayValue);
			resultSet = select.executeQuery();
			
			radius = resultSet.getInt(1);
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return radius;
	}

	/**
	 * The item latitude.
	 *
	 * @param itemID
	 *            Item identifier.
	 * @return The item latitude.
	 */
	public long getItemLatitude(long itemID) {
		// Añadido por Nacho Palacio 2025-04-22
		long externalItemId = convertToExternalId(itemID, ElementIdMapper.CATEGORY_ITEM);

		long latitude = 0;
		ResultSet resultSet = null;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT latitude_gps FROM item WHERE id_item= ?");
			// select.setInt(1, (int) itemID);
			select.setInt(1, (int) externalItemId); // Modificado por Nacho Palacio 2025-04-22
			resultSet = select.executeQuery();
			
			latitude = resultSet.getInt(1);
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return latitude;
	}

	/**
	 * The item longitude.
	 *
	 * @param itemID
	 *            Item identifier.
	 * @return The item longitude.
	 */
	public long getItemLongitude(long itemID) {
		// Añadido por Nacho Palacio 2025-04-22
		long externalItemId = convertToExternalId(itemID, ElementIdMapper.CATEGORY_ITEM);

		long latitude = 0;
		ResultSet resultSet = null;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT longitude_gps FROM item WHERE id_item= ?");
			// select.setInt(1, (int) itemID);
			select.setInt(1, (int) externalItemId); // Modificado por Nacho Palacio 2025-04-22
			resultSet = select.executeQuery();
			
			latitude = resultSet.getInt(1);
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return latitude;
	}

	/**
	 * Gets the maximum rating value.
	 *
	 * @return The maximum rating value.
	 */
	public float getMaximumRating() {
		float maximumRating = 0;
		ResultSet resultSet = null;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT MAX (rating) FROM user_item_context");
			resultSet = select.executeQuery();
			
			maximumRating = resultSet.getInt(1);
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return maximumRating;
	}

	/**
	 * Gets the number of item features.
	 *
	 * @return The number of item features.
	 */
	public int getNumberItemFeatures() {
		int numFeatures = 0;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT Count(name) FROM feature");
			ResultSet resultSet = select.executeQuery();
			
			numFeatures = resultSet.getInt(1);
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return numFeatures;
	}

	/**
	 * Gets the feature names and values of an item.
	 *
	 * @param itemId
	 *            Item identifier.
	 * @return The features of an item.
	 * @throws TasteException
	 */
	public List<String> getNamesAndValuesOfFeaturesFromItem(long itemID) throws TasteException {
		List<String> listFeatures = new LinkedList<String>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT name, value FROM item_feature WHERE item_feature.id_item= ?");
			select.setInt(1, (int) itemID);
			ResultSet resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				listFeatures.add(resultSet.getString("name") + "__" + resultSet.getString("value"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return listFeatures;
	}

	/**
	 * Gets a list with all the feature names.
	 *
	 * @return List with all the feature names.
	 */
	public List<String> getItemFeatureNames() {
		List<String> listFeatures = new LinkedList<String>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT value FROM item_feature");
			ResultSet resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				listFeatures.add(resultSet.getString("value"));
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return listFeatures;
	}

	/**
	 * Gets a ResultSet with the user rating about an item.
	 *
	 * @param userId
	 *            User identifier.
	 * @param itemId
	 *            Item identifier.
	 * @param contextId
	 *            Context identifier.
	 * @return ResultSet.
	 */
	@Override
	public float getPreferenceFor(long userId, long itemId, long contextId) {
		float rating = 0;
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT rating FROM user_item_context WHERE id_user== ? AND id_item== ? AND id_context== ?");
			select.setInt(1, (int) userId);
			select.setInt(2, (int) itemId);
			select.setInt(3, (int) contextId);
			
			rating = (float) select.executeQuery().getDouble("rating");
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return rating;
	}

	public List<Long> getItemsOrderByRoom() {
		ResultSet resultSet = null;
		List<Long> list = new LinkedList<>();
		try {
			
			// Query
			PreparedStatement select = getConnection()
					.prepareStatement("SELECT id_item FROM item_feature WHERE name='Room' ORDER BY cast(value as unsigned)");
			resultSet = select.executeQuery();
			
			while (resultSet.next()) {
				// long itemId = resultSet.getLong(1);
				// list.add(itemId);

				// Modificado por Nacho Palacio 2025-04-22
				long externalItemId = resultSet.getLong(1);
				long internalItemId = convertToInternalId(externalItemId, ElementIdMapper.CATEGORY_ITEM);
				System.out.println("DB: Convirtiendo Item ID externo " + externalItemId + " a interno " + internalItemId);
				list.add(internalItemId);;
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

		/**
	 * Añadido por Nacho Palacio 2025-04-14. 
	 * Ensures the database has enough users for the simulation.
	 * If there are fewer users than needed, it adds new ones.
	 * 
	 * @param totalRequiredUsers The total number of users required for the simulation
	 * @throws SQLException if a database error occurs
	 */
	public boolean ensureRequiredUsers(int totalRequiredUsers) throws SQLException {
		System.out.println("Ensuring required users in the database...");

		Connection conn = getConnection();
		if (conn == null) {
			System.out.println("Error: Database connection is null");
			return false;
		}

		int currentUserCount = 0;
		
		// Obtener el número actual de usuarios
		try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM user");
			ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				currentUserCount = rs.getInt(1);
			}
			System.out.println("Current user count: " + currentUserCount);
		}
		
		System.out.println("Current users in database: " + currentUserCount);
		System.out.println("Required users for simulation: " + totalRequiredUsers);
		
		// Si ya hay suficientes usuarios, no hacer nada
		if (currentUserCount >= totalRequiredUsers) {
			System.out.println("Database already has enough users.");
			return false;
		}
		
		// Comenzar una transacción para insertar los nuevos usuarios
		conn.setAutoCommit(false);
		try {
			// Determinar el último id_ca_profile usado para distribuir perfiles de manera equitativa
			int[] profileDistribution = {0, 0, 0, 0}; // Para contar cuántos usuarios hay de cada perfil (1-4)
			
			try (PreparedStatement profileStmt = conn.prepareStatement("SELECT id_ca_profile, COUNT(*) FROM user GROUP BY id_ca_profile");
				ResultSet profileRs = profileStmt.executeQuery()) {
				while (profileRs.next()) {
					int profileId = profileRs.getInt(1);
					int count = profileRs.getInt(2);
					if (profileId >= 1 && profileId <= 4) {
						profileDistribution[profileId-1] = count;
					}
				}
			}
			
			// Insertar nuevos usuarios
			try (PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO user (id_user, age, sex, city_numeric, country, id_ca_profile) VALUES (?, ?, ?, ?, ?, ?)")) {
				
				for (int id = currentUserCount + 1; id <= totalRequiredUsers; id++) {
					// Determinar qué perfil usar (el menos representado)
					int minIndex = 0;
					for (int i = 1; i < 4; i++) {
						if (profileDistribution[i] < profileDistribution[minIndex]) {
							minIndex = i;
						}
					}
					int profileToUse = minIndex + 1; // Perfiles van de 1 a 4
					profileDistribution[minIndex]++; // Incrementar el contador de este perfil
					
					insertStmt.setInt(1, id);
					insertStmt.setNull(2, java.sql.Types.VARCHAR); // age
					insertStmt.setNull(3, java.sql.Types.VARCHAR); // sex
					insertStmt.setInt(4, 0);       // city_numeric
					insertStmt.setNull(5, java.sql.Types.VARCHAR); // country
					insertStmt.setInt(6, profileToUse); // id_ca_profile - alternar entre 1 y 3 como en los ejemplos
					
					insertStmt.executeUpdate();
					System.out.println("Added user with ID: " + id + " and profile: " + profileToUse);
				}
			}
			
			// Si todo va bien, hacer commit
			conn.commit();
			System.out.println("Successfully added " + (totalRequiredUsers - currentUserCount) + " new users to the database.");
		} catch (SQLException e) {
			// Si hay un error, hacer rollback
			conn.rollback();
			System.out.println("Error adding new users: " + e.getMessage());
			throw e;
		} finally {
			// Restaurar autocommit
			conn.setAutoCommit(true);
		}

		for (int id = currentUserCount + 1; id <= totalRequiredUsers; id++) {
			ensureUserHasPreferences(id);
		}
		return true;
	}

		/**
	 * Añadido por Nacho Palacio 2025-04-14.
	 * Ensures the user has basic preferences in the database.
	 * This is important for recommendation algorithms to work.
	 * 
	 * @param userId The ID of the user to check/create preferences for
	 * @throws SQLException if a database error occurs
	 */
	public void ensureUserHasPreferences(long userId) throws SQLException {
		Connection conn = getConnection();
		if (conn == null) {
			System.out.println("Error: Database connection is null in ensureUserHasPreferences");
			return;
		}
		
		// Nombre correcto de la tabla en la base de datos
		String tableName = "user_item_context";
		
		// Verificar si el usuario ya tiene preferencias
		System.out.println("Checking if user " + userId + " already has preferences...");
		int preferenceCount = 0;
		
		try (PreparedStatement stmt = conn.prepareStatement(
				"SELECT COUNT(*) FROM " + tableName + " WHERE id_user = ?")) {
			stmt.setLong(1, userId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					preferenceCount = rs.getInt(1);
				}
			}
		}
		
		System.out.println("User " + userId + " has " + preferenceCount + " preferences");
		
		// Si no tiene preferencias, añadir algunas básicas
		if (preferenceCount == 0) {
			// Obtener algunos items aleatorios
			List<Long> items = new ArrayList<>();
			try (PreparedStatement stmt = conn.prepareStatement(
					"SELECT id_item FROM item ORDER BY RANDOM() LIMIT 5");
				 ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					items.add(rs.getLong(1));
				}
			}
			
			System.out.println("Selected " + items.size() + " items for user " + userId);
			
			// Obtener contextos disponibles
			List<Integer> contexts = new ArrayList<>();
			try (PreparedStatement stmt = conn.prepareStatement(
					"SELECT id_context FROM context LIMIT 3");
				 ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					contexts.add(rs.getInt(1));
				}
			}
			
			if (contexts.isEmpty()) {
				System.out.println("No contexts found in the database. Adding default context 1.");
				contexts.add(1);
			}
			
			// Añadir preferencias para estos items
			if (!items.isEmpty()) {
				conn.setAutoCommit(false);
				try {
					try (PreparedStatement insertStmt = conn.prepareStatement(
							"INSERT INTO " + tableName + " (id_user, id_item, id_context, rating) VALUES (?, ?, ?, ?)")) {
						
						// Usar contexto disponible (por defecto el primero)
						int contextId = contexts.get(0);
						
						for (Long itemId : items) {
							insertStmt.setLong(1, userId);
							insertStmt.setLong(2, itemId);
							insertStmt.setInt(3, contextId);
							
							// Rating aleatorio entre 2.5 y 5.0
							float rating = 2.5f + (float)(Math.random() * 2.5);
							insertStmt.setFloat(4, rating);
							
							insertStmt.executeUpdate();
							System.out.println("Added preference: User " + userId + ", Item " + itemId + 
											  ", Context " + contextId + ", Rating " + rating);
						}
					}
					conn.commit();
					System.out.println("Added basic preferences for user " + userId);
				} catch (SQLException e) {
					conn.rollback();
					System.out.println("Error adding preferences for user " + userId + ": " + e.getMessage());
					throw e;
				} finally {
					conn.setAutoCommit(true);
				}
			} else {
				System.out.println("No items found to add preferences for user " + userId);
			}
		}
	}

	// Añadido por Nacho Palacio 2025-04-22.
	/**
	 * Convierte un ID externo (de base de datos) a ID interno (usado en el modelo)
	 * @param externalId ID almacenado en la base de datos
	 * @param category Categoría del elemento (CATEGORY_ITEM, CATEGORY_DOOR, etc.)
	 * @return ID interno en el rango correcto
	 */
	public long convertToInternalId(long externalId, int category) {
		return ElementIdMapper.convertToRangeId(externalId, category);
	}

	/**
	 * Convierte un ID interno (usado en el modelo) a ID externo (para almacenar en base de datos)
	 * @param internalId ID utilizado en el modelo
	 * @param category Categoría del elemento
	 * @return ID externo para usar en la base de datos
	 */
	public long convertToExternalId(long internalId, int category) {
		// Si el ID ya está en el rango correcto, extraer el ID base
		if (ElementIdMapper.isInCorrectRange(internalId, category)) {
			long rangeStart = 0;
			switch (category) {
				case ElementIdMapper.CATEGORY_ITEM:
					rangeStart = ElementIdMapper.ITEM_ID_START;
					break;
				case ElementIdMapper.CATEGORY_DOOR:
					rangeStart = ElementIdMapper.DOOR_ID_START;
					break;
				case ElementIdMapper.CATEGORY_STAIRS:
					rangeStart = ElementIdMapper.STAIRS_ID_START;
					break;
				case ElementIdMapper.CATEGORY_ROOM:
					rangeStart = ElementIdMapper.ROOM_ID_START;
					break;
				case ElementIdMapper.CATEGORY_CORNER:
					rangeStart = ElementIdMapper.CORNER_ID_START;
					break;
				case ElementIdMapper.CATEGORY_SEPARATOR:
					rangeStart = ElementIdMapper.SEPARATOR_ID_START;
					break;
			}
			
			// Extraer el ID base, asegurando un valor mínimo de 1
			long baseId = internalId - rangeStart;
			return baseId > 0 ? baseId : 1;
		}
		
		// Si no está en el rango correcto, devolverlo tal cual
		return internalId;
	}

	/**
	 * Convierte una lista de IDs externos a sus equivalentes internos
	 * @param externalIds Lista de IDs externos
	 * @param category Categoría de los elementos
	 * @return Lista de IDs internos en los rangos correctos
	 */
	public List<Long> convertToInternalIds(List<Long> externalIds, int category) {
		List<Long> internalIds = new ArrayList<>(externalIds.size());
		for (Long externalId : externalIds) {
			internalIds.add(convertToInternalId(externalId, category));
		}
		return internalIds;
	}

	/**
	 * Convierte una lista de IDs internos a sus equivalentes externos
	 * @param internalIds Lista de IDs internos
	 * @param category Categoría de los elementos
	 * @return Lista de IDs externos para usar en la base de datos
	 */
	public List<Long> convertToExternalIds(List<Long> internalIds, int category) {
		List<Long> externalIds = new ArrayList<>(internalIds.size());
		for (Long internalId : internalIds) {
			externalIds.add(convertToExternalId(internalId, category));
		}
		return externalIds;
	}


	/**
	 * Método para verificar la conversión de IDs en la capa de acceso a datos
	 */
	public void verifyIdConversion() {
		System.out.println("\n=== VERIFICACIÓN DE CONVERSIÓN DE IDs EN DataAccessLayer ===");
		
		// Crear algunos IDs de prueba
		long externalItemId = 25;  // ID externo de ejemplo
		long internalItemId = convertToInternalId(externalItemId, ElementIdMapper.CATEGORY_ITEM);
		
		System.out.println("Item: externo " + externalItemId + " -> interno " + internalItemId + 
						" -> convertido de nuevo " + convertToExternalId(internalItemId, ElementIdMapper.CATEGORY_ITEM));
		
		// Verificar conversión en getItemsOrderByRoom
		List<Long> items = getItemsOrderByRoom();
		if (!items.isEmpty()) {
			System.out.println("\nVerificando IDs de ítems obtenidos por getItemsOrderByRoom:");
			for (int i = 0; i < Math.min(5, items.size()); i++) {
				long itemId = items.get(i);
				long externalId = convertToExternalId(itemId, ElementIdMapper.CATEGORY_ITEM);
				System.out.println("Item #" + i + ": ID interno " + itemId + 
								", ID externo " + externalId + 
								", rango correcto: " + ElementIdMapper.isInCorrectRange(itemId, ElementIdMapper.CATEGORY_ITEM));
			}
		}
		
		// Verificar conversión en otras funciones
		System.out.println("\nPrueba de getItemLatitude con conversión de ID:");
		if (!items.isEmpty()) {
			long itemId = items.get(0);
			long latitude = getItemLatitude(itemId);
			System.out.println("Item ID " + itemId + " (externo: " + convertToExternalId(itemId, ElementIdMapper.CATEGORY_ITEM) + 
							") tiene latitud: " + latitude);
		}
		
		System.out.println("====================================================\n");
	}
}
