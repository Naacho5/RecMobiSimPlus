package es.unizar.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.unizar.database.DBConnection;
import es.unizar.database.Database;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.InformationToPropagate;
import es.unizar.util.Literals;

/**
 * Manage the information stored in the queue.db database.
 * 
 * @author Maria del Carmen Rodriguez Hernandez.
 *
 */
public class SQLiteDataManagementQueueDB extends DBConnection implements DataManagementQueueDB {

	private static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	
	private Database dbInstance = null;

	/**
	 * Constructor.
	 * 
	 * @param dbURL	Database URL
	 * @param db	Database instance (which holds db connection)
	 */
	public SQLiteDataManagementQueueDB(String dbURL, Database db) {
		super(dbURL);
		dbInstance = db;
		try {
			connect(dbURL);
		}
		catch (ClassNotFoundException | SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#connect(java.lang.String)
	 */
	@Override
	public void connect(String dbURL) throws ClassNotFoundException, SQLException {
		
		dbInstance.connect(dbURL);
		
		//System.out.println(getConnection());
		
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
		System.out.println(connection);
		
		// VITAL -> DB PERFORMANCE SUPER OPTIMIZED
		// https://dba.stackexchange.com/questions/252445/how-does-autocommit-off-affects-bulk-inserts-performance-in-mysql-using-innodb
		// http://www.w3big.com/es/sqlite/sqlite-java.html
		connection.setAutoCommit(false);
		*/
	}

	/*
	 * (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getConnection()
	 */
	@Override
	public Connection getConnection() {
		return dbInstance.getConnection();
		
		/*
		 * PREVIOUS - Now only one
		 *
		return connection;
		*/
	}

	/*
	 * (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#disconnect()
	 */
	@Override
	public void disconnect() throws SQLException {
		
		commit();
		
		dbInstance.disconnect();

		/*
		 * PREVIOUS - Now only one
		 *
		if (statement != null) {
			statement.close();
			connection.close();
		}
		*/
		
	}
	
	/**
	 * Commits all transactions (before closing connection).
	 */
	public void commit() {
		
		dbInstance.commit();

		/*
		 * PREVIOUS - Now only one db connection
		 *
		try {
			connection.commit();
			//System.out.println("COMMITED");
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
			System.out.println(e.getMessage());
		}
		*/
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#insertInformation(es.unizar.gui.simulation.InformationToPropagate)
	 */
	@Override
	public boolean insertInformation(InformationToPropagate information) {
		boolean ifExist = verifyIfExist(information.getId_user(), information.getUser(), information.getItem(),
				information.getContext(), information.getRating());
		boolean ifInsertOK = false;
		if (!ifExist) {
			ifInsertOK = insert(information.getId_user(), information.getUser(), information.getItem(), information.getContext(),
					information.getRating(), information.getTTL(), information.getTTP(),
					information.isIsTTPInitialized(), information.getLocation(), information.getCurrentTime());
			// ifInsertOK = true;
		}
		return ifInsertOK;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#insertInformation(long, es.unizar.gui.simulation.InformationToPropagate)
	 */
	@Override
	public boolean insertInformation(long id_user, InformationToPropagate information) {
		information.setId_user(id_user);
		return insertInformation(information);
	}

	/**
	 * Insert the information about a user in the queue.db database.
	 * 
	 * @param id_user:          The non-RS user ID that propagates the rating
	 *                          to the RS user.
	 * @param user:             The RS user that receives the rating from
	 *                          non-RS user ID.
	 * @param item:             The item ID that was propagated.
	 * @param context:          The current context of the RS user.
	 * @param rating:           The item rating.
	 * @param TTL:              The Time To Live the item.
	 * @param TTP:              Time needed To Propagate the data.
	 * @param isTTPInitialized: If the TTP was initialized.
	 * @param location:         The current user location.
	 * @param currentTime:      The current time.
	 * @return True if the information was correctly inserted and False in the
	 *         otherwise.
	 */
	private boolean insert(long id_user, long user, long item, long context, double rating, long TTL, double TTP,
			int isTTPInitialized, String location, long currentTime) {
		boolean ifInsertOK = true;
		try {
			// Prepared Statement creation (for only 1 plan for all insert ops)
			PreparedStatement p = getConnection()
					.prepareStatement("INSERT INTO " + INFORMATION_TB + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			// Set prepared statement parameters
			p.setInt(1, (int) id_user);
			p.setInt(2, (int) user);
			p.setInt(3, (int) item);
			p.setInt(4, (int) context);
			p.setDouble(5, rating);
			p.setInt(6, (int) TTL);
			p.setDouble(7, TTP);
			p.setInt(8, isTTPInitialized);
			p.setString(9, location);
			p.setInt(10, (int) currentTime);
			
			p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			ifInsertOK = false;
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ifInsertOK;
	}

	/**
	 * Verify if the information is not into queue.db.
	 * 
	 * @param id_user: The non-RS user ID that propagates the rating to the
	 *                 RS user.
	 * @param user:    The RS user that receives the rating from non-special
	 *                 user ID.
	 * @param item:    The item ID that was propagated.
	 * @param context: The current context of the RS user.
	 * @param rating:  The item rating.
	 * @return True if the information is into queue.db and False in the otherwise.
	 */
	private boolean verifyIfExist(long id_user, long user, long item, long context, double rating) {
		ResultSet result = null;
		boolean exist = false;
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("SELECT * FROM " + INFORMATION_TB + " WHERE id_user = ? and user= ? and item= ? and context= ? and rating= ?");
			
			// Set prepared statement parameters
			p.setInt(1, (int) id_user);
			p.setInt(2, (int) user);
			p.setInt(3, (int) item);
			p.setInt(4, (int) context);
			p.setInt(5, (int) rating);
			
			result = p.executeQuery();
			
			if (result.next()) {
				exist = true;
			}
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		} 
		return exist;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getInformation()
	 */
	@Override
	public LinkedList<InformationToPropagate> getInformation() {
		LinkedList<InformationToPropagate> informationQueue = new LinkedList<>();
		ResultSet resultSet = null;
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("SELECT * FROM " + INFORMATION_TB + " WHERE TTP <= 0 and isTTPInitialized==1");
			// Query
			resultSet = p.executeQuery();
			
			while (resultSet.next()) {
				long id_user = resultSet.getLong("id_user");
				long user = resultSet.getLong("user");
				long item = resultSet.getLong("item");
				long context = resultSet.getLong("context");
				double rating = resultSet.getDouble("rating");
				int TTL = resultSet.getInt("TTL");
				double TTP = resultSet.getDouble("TTP");
				int isTTPInitialized = resultSet.getInt("isTTPInitialized");
				String location = resultSet.getString("location");
				long currentTime = resultSet.getLong("currentTime");
				informationQueue.add(new InformationToPropagate(id_user, user, item, context, rating, TTL, TTP,
						isTTPInitialized, location, currentTime));
			}
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return informationQueue;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getInformation(long)
	 */
	@Override
	public LinkedList<InformationToPropagate> getInformation(long id_user) {
		LinkedList<InformationToPropagate> informationQueue = new LinkedList<>();
		ResultSet resultSet = null;
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("SELECT * FROM " + INFORMATION_TB + " WHERE TTP <= 0 and isTTPInitialized==1 and id_user == ?");
			
			// Set prepared statement parameters
			p.setInt(1, (int) id_user);
			
			// Query
			resultSet = p.executeQuery();
			
			while (resultSet.next()) {
				long user = resultSet.getLong("user");
				long item = resultSet.getLong("item");
				long context = resultSet.getLong("context");
				double rating = resultSet.getDouble("rating");
				int TTL = resultSet.getInt("TTL");
				double TTP = resultSet.getDouble("TTP");
				int isTTPInitialized = resultSet.getInt("isTTPInitialized");
				String location = resultSet.getString("location");
				long currentTime = resultSet.getLong("currentTime");
				informationQueue.add(new InformationToPropagate(id_user, user, item, context, rating, TTL, TTP,
						isTTPInitialized, location, currentTime));
			}
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return informationQueue;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getUsersWithInformationToPropagate()
	 */
	@Override
	public LinkedList<Long> getUsersWithInformationToPropagate() {
		LinkedList<Long> idUsers = new LinkedList<>();
		ResultSet resultSet = null;
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("SELECT id_user FROM " + INFORMATION_TB + " WHERE TTP <= 0 and isTTPInitialized==1 GROUP BY id_user");
			
			// Query
			resultSet = p.executeQuery();
			
			while (resultSet.next()) {
				long id_user = resultSet.getLong("id_user");
				idUsers.add(id_user);
			}
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return idUsers;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getNumberItemsByUserWithoutInitializeTTP()
	 */
	@Override
	public LinkedList<String> getNumberItemsByUserWithoutInitializeTTP() {
		LinkedList<String> list = new LinkedList<>();
		ResultSet resultSet = null;
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			//PreparedStatement pView = getConnection()
			//		.prepareStatement("CREATE VIEW V1 AS SELECT id_user FROM " + INFORMATION_TB + " WHERE isTTPInitialized==0;");
			
			//pView.executeUpdate();
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement pSelect = getConnection()
					.prepareStatement("SELECT id_user, COUNT(*) FROM (SELECT id_user FROM " + INFORMATION_TB + " WHERE isTTPInitialized==0) GROUP BY id_user;");
			
			// Query
			resultSet = pSelect.executeQuery();

			while (resultSet.next()) {
				String user_id = resultSet.getString(1);
				String countItems = resultSet.getString(2);
				list.add(user_id + "," + countItems);
			}
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			//PreparedStatement pDrop = getConnection()
			//		.prepareStatement("DROP VIEW V1;");
			
			//pDrop.execute();
			
			// Not really needed, no parameters
			//pView.clearParameters();
			pSelect.clearParameters();
			//pDrop.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return list;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#deleteInformation(es.unizar.gui.simulation.InformationToPropagate)
	 */
	@Override
	public void deleteInformation(InformationToPropagate information) {
		try {
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("DELETE FROM " + INFORMATION_TB + " WHERE id_user== ? and user== ? and item== ? and context== ? and	rating== ?");
			
			// Set prepared statement parameters
			p.setInt(1, (int) information.getId_user());
			p.setInt(2, (int) information.getUser());
			p.setInt(3, (int) information.getItem());
			p.setInt(4, (int) information.getContext());
			p.setInt(5, (int) information.getRating());
			
			p.execute();
			
			p.clearParameters();
			
						
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#deleteAllInformationFromTable()
	 */
	@Override
	public void deleteAllInformationFromTable() {
		try {
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("DELETE FROM " + INFORMATION_TB);
			
			p.executeUpdate();
			
			// Not really needed, no parameters
			p.clearParameters();
			
			commit();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#updateExchange(long, long)
	 */
	@Override
	public int updateExchange(long farthestUser, long id_user) {
		int countUpdated = 0;
		try {
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("UPDATE information SET id_user= ?, isTTPInitialized==0 WHERE id_user== ? and TTP<=0 and isTTPInitialized==1;");
			
			// Set prepared statement parameters
			p.setInt(1, (int) farthestUser);
			p.setInt(2, (int) id_user);
			
			countUpdated = p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return countUpdated;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#updateTTL()
	 */
	@Override
	public void updateTTL() {
		try {
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement pUpdate = getConnection()
					.prepareStatement("UPDATE information SET TTL = TTL - 1");
			
			PreparedStatement pDelete = getConnection()
					.prepareStatement("DELETE FROM information WHERE TTL<=0");
			
			int updated = pUpdate.executeUpdate();
			pDelete.execute();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#updateTTP()
	 */
	@Override
	public void updateTTP() {
		try {
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("UPDATE information SET TTP = TTP - ? WHERE isTTPInitialized==1");
			
			// Set prepared statement parameter
			p.setDouble(1, Configuration.simulation.getTimeForIterationInSecond());
			
			int updated = p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#initializeAllTTP()
	 */
	@Override
	public void initializeAllTTP() {
		try {
			
			double TTP = getInitialTimeToPropagate(NUMBER_RATINGS_TO_TRANSFER);
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("UPDATE information set TTP = ?, isTTPInitialized = 1 WHERE isTTPInitialized == 0");
			
			// Set prepared statement parameters
			p.setDouble(1, TTP);
			
			p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#initializeAllTTP(int, long)
	 */
	@Override
	public void initializeAllTTP(int countRatings, long id_user) {
		try {
			
			double TTP = getInitialTimeToPropagate(countRatings);
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("UPDATE information set TTP = ?, isTTPInitialized = 1 WHERE isTTPInitialized == 0 AND id_user== ?");
			
			// Set prepared statement parameters
			p.setDouble(1, TTP);
			p.setInt(2, (int) id_user);
			
			p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#initializeOneTTP(es.unizar.gui.simulation.InformationToPropagate)
	 */
	@Override
	public void initializeOneTTP(InformationToPropagate information) {
		try {
			
			double TTP = getInitialTimeToPropagate(NUMBER_RATINGS_TO_TRANSFER);
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("UPDATE information set TTP = ?, isTTPInitialized = 1 WHERE (isTTPInitialized == 0 and id_user== ? and user== ? and item== ? and context== ? and rating== ?)");
			
			// Set prepared statement parameters
			p.setDouble(1, TTP);
			p.setInt(2, (int) information.getId_user());
			p.setInt(3, (int) information.getUser());
			p.setInt(4, (int) information.getItem());
			p.setInt(5, (int) information.getContext());
			p.setInt(6, (int) information.getRating());
			
			p.executeUpdate();
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Time it will take to propagate the information to a user.
	 * 
	 * @param numberOfRatingsToTransfer: Number of ratings to transfer from a
	 *                                   non-RS user to a RS user.
	 * @return The calculated time.
	 */
	private double getInitialTimeToPropagate(int numberOfRatingsToTransfer) {
		double latency = Configuration.simulation.getLatencyOfTransmission();
		double amountOfDataToTransmit = 0.008 * numberOfRatingsToTransfer; // 8 bytes (is occuped by one tuple U_I_C_R)
																			// = 0,008 Kb
		double velocity = convertMbToKb(Configuration.simulation.getCommunicationBandwidth());
		double timeOfExchange = (amountOfDataToTransmit) / velocity; // Math.round(timeOfExchange);
		return latency + (timeOfExchange / (double) numberOfRatingsToTransfer);
	}

	/**
	 * Converts from megabyte to kilobyte.
	 * 
	 * @param Mb: Megabyte.
	 * @return The value in kilobyte.
	 */
	private double convertMbToKb(double Mb) {
		double Kb = Mb * 1000;
		return Kb;
	}
	
}