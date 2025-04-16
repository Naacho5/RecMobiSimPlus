package es.unizar.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.unizar.database.DBConnection;
import es.unizar.database.Database;
import es.unizar.gui.simulation.InformationToPropagate;
import es.unizar.util.Literals;

/**
 * Manage the information stored in the db_user.db datat base.
 * 
 * @author Maria del Carmen Rodriguez Hernandez.
 *
 */
public class SQLiteDataManagementUserDB extends DBConnection implements DataManagementUserDB {
	
	private static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	
	private Database dbInstance = null;

	/**
	 * Constructor.
	 * 
	 * @param dbURL	Database URL
	 * @param db	Database instance (which holds db connection)
	 */
	public SQLiteDataManagementUserDB(String dbURL, Database db) {
		
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
	
	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#connect()
	 */
	@Override
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
		// https://dba.stackexchange.com/questions/252445/how-does-autocommit-off-affects-bulk-inserts-performance-in-mysql-using-innodb
		// http://www.w3big.com/es/sqlite/sqlite-java.html
		//connection.setAutoCommit(false);
		//connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		*/
	}
	
	/*
	 * (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#getConnection()
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
	
	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#disconnect()
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
	 * @see es.unizar.dao.DataManagementUserDB#insertInformation(es.unizar.gui.simulation.InformationToPropagate, Integer)
	 */
	@Override
	public boolean insertInformation(InformationToPropagate information, int numMaxItems) {
		
		//long initialTimeRecommender = 0, finalTimeRecommender = 0;
		//initialTimeRecommender = System.currentTimeMillis();
		
		// long id_user, long user, long item, long context, double rating, long TTL,
		// double TTP, boolean isTTPInitialized, String location, long currentTime
		boolean ifExist = verifyIfExist(information.getUser(), information.getItem(), information.getContext(), information.getRating());
		
		//finalTimeRecommender = System.currentTimeMillis();
		//System.out.println("[verifyIfExist]: " + (finalTimeRecommender - initialTimeRecommender));
		
		//initialTimeRecommender = System.currentTimeMillis();
		boolean ifInsertOK = false;
		if (!ifExist && information.getItem() <= numMaxItems) {
			//System.out.println("  NO EXISTE, ENTRO IF");
			
			ifInsertOK = insert(information.getUser(), information.getItem(), information.getContext(), information.getRating(), null, 1);
			
			//ifInsertOK = true;
		}
		
		//finalTimeRecommender = System.currentTimeMillis();
		//System.out.println("[insert]: " + (finalTimeRecommender - initialTimeRecommender));
		
		return ifInsertOK;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#insert(long, long, long, double, java.lang.String, int)
	 */
	@Override
	public boolean insert(long user, long item, long context, double rating, String opinion, int userProvided) {
		
		boolean ifInsertOK = true;
		//System.out.println("[insert]:");
		//long initialTimeRecommender = 0, finalTimeRecommender = 0;
		
		try {
			
			/*
			 * PREVIOUS
			 *
			//initialTimeRecommender = System.currentTimeMillis();
			// Connection to the DB
			Connection conn = getConnection();
			statement = conn.createStatement();
			
			//finalTimeRecommender = System.currentTimeMillis();
			//System.out.println("- DB connection: " + (finalTimeRecommender - initialTimeRecommender));
			
			//initialTimeRecommender = System.currentTimeMillis();
			// Query
			statement.executeUpdate("INSERT INTO " + USER_ITEM_CONTEXT_TB + " VALUES('" + user + "','" + item + "','" + context + "','" + rating + "','" + null + "','" + userProvided + "')"); // Operación que consume muchísimo tiempo!!!!
			 *
			 */
			
			// Prepared Statement creation (for only 1 plan for all insert ops)
			PreparedStatement p = getConnection()
					.prepareStatement("INSERT INTO " + USER_ITEM_CONTEXT_TB + " VALUES(?, ?, ?, ?, ?, ?)");
			
			// Set prepared statement parameters
			p.setInt(1, (int) user);
			p.setInt(2, (int) item);
			p.setInt(3, (int) context);
			p.setInt(4, (int) rating);
			p.setString(5, opinion);
			p.setInt(6, (int) userProvided);
			
			p.executeUpdate();
			
			p.clearParameters();
			
			//finalTimeRecommender = System.currentTimeMillis();
			//System.out.println("- DBop update: " + (finalTimeRecommender - initialTimeRecommender));
			
		} catch (SQLException e) {
			ifInsertOK = false;
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		} 
		
		return ifInsertOK;
	}

	/**
	 * Verify if the information is not into DB.
	 * 
	 * @param user:    The user ID.
	 * @param item:    The item ID.
	 * @param context: The context ID.
	 * @param rating:  The rating.
	 * @return True or False.
	 */
	private boolean verifyIfExist(long user, long item, long context, double rating) {
		
		ResultSet result;
		boolean exist = false;
		
		try {
			/*
			 * PREVIOUS
			 *
			// Connection to the DB
			Connection conn = getConnection();
			statement = conn.createStatement();
			// Query
			result = statement.executeQuery("SELECT * FROM " + USER_ITEM_CONTEXT_TB + " WHERE id_user='" + user + "' and id_item='" + item + "' and id_context='" + context + "' and rating='" + rating + "'");
			*/
			
			// Prepared Statement creation (for only 1 plan for all select ops)
			PreparedStatement p = getConnection()
					.prepareStatement("SELECT * FROM " + USER_ITEM_CONTEXT_TB + " WHERE id_user = ? and id_item = ? and id_context = ? and rating = ?");
			
			// Set prepared statement parameters
			p.setInt(1, (int) user);
			p.setInt(2, (int) item);
			p.setInt(3, (int) context);
			p.setInt(4, (int) rating);
			
			result = p.executeQuery();
			
			/*
			 * FOR PRINTING RESULT IF WANTED.
			 * 
			 * WARNING!!!!!! -> NEXT FUNCTION ADVANCES CURSOR -> EXIST VARIABLE WON'T BE OKAY THEN
			 * 
			ResultSetMetaData rsmd = result.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			while (result.next()) {
			    for (int i = 1; i <= columnsNumber; i++) {
			        if (i > 1) System.out.print(",  ");
			        String columnValue = result.getString(i);
			        System.out.print(columnValue + " " + rsmd.getColumnName(i));
			    }
			    System.out.println("");
			}
			 *
			 */
			
			if (result.next()) {
				exist = true;
			} else {
				exist = false;
			}
			
			p.clearParameters();
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		return exist;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#deleteAllInformationFromTable()
	 */
	@Override
	public void deleteAllInformationFromTable() {
		try {
			/*
			 * PREVIOUS
			 *
			// Connection to the DB
			Connection conn = getConnection();
			statement = conn.createStatement();
			// Query
			statement.executeUpdate("DELETE FROM " + USER_ITEM_CONTEXT_TB);
			 *
			 */
			
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("DELETE FROM " + USER_ITEM_CONTEXT_TB);
			
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
	 * @see es.unizar.dao.DataManagementUserDB#deleteInformationFromTable(long)
	 */
	@Override
	public void deleteInformationFromTable(long userID) {
		try {
			/*
			 * PREVIOUS
			 *
			// Connection to the DB
			Connection conn = getConnection();
			statement = conn.createStatement();
			// Query
			statement.executeUpdate("DELETE FROM " + USER_ITEM_CONTEXT_TB + " WHERE id_user==" + userID);
			 *
			 */
			
			// Prepared Statement creation (for only 1 plan for all delete ops)
			PreparedStatement p = getConnection()
					.prepareStatement("DELETE FROM " + USER_ITEM_CONTEXT_TB + " WHERE id_user== ?");
			
			// Set prepared statement parameters
			p.setString(1, Long.toString(userID));
			
			p.execute();
			
			p.clearParameters();
			
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
	}

	/* NEVER USED!!!!!
	 * 
	 *
	 * (non-Javadoc)
	 * @see es.unizar.dao.DataManagementUserDB#createNewDatabase()
	 */
	@Override
	public void createNewDatabase() {
		Connection connection = null;
		try {
			// Connection to the DB
			Class.forName(SQLITE);
			connection = DriverManager.getConnection(dbURL);
			// Create DB:
			DatabaseMetaData meta = connection.getMetaData();
			//System.out.println("The driver name is " + meta.getDriverName());
			//System.out.println("A new database has been created.");
		} catch (ClassNotFoundException | SQLException e) {
			log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Close connection
			try {
				if (statement != null) {
					statement.close();
					connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
