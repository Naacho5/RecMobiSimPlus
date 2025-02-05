package es.unizar.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.unizar.gui.simulation.InformationToPropagate;
import es.unizar.gui.simulation.Simulation;
import es.unizar.util.Literals;

/**
 * EXCLUDED FROM BUILD PATH -> DO NOT USE.
 * 
 * @author apied
 *
 */
public class MemoryDataManagementUserDB implements DataManagementUserDB {
	
	// Info stored in memory before persistance (saving)
	private Set<InformationToPropagate> infoToPropagate = new HashSet<InformationToPropagate>();
	
	// DB where data will be persisted when "save" button pressed or when finished simulation
	DataManagementUserDB userDB;
	
	private static final Logger log = LoggerFactory.getLogger(MemoryDataManagementUserDB.class);
	
	public MemoryDataManagementUserDB(String dbURL) {
		
		super();
		
		// Cannot use "Literals.currentDBUsed" because current db being used is "Memory"
		DAOFactory factory = DAOFactory.getFactory(Literals.Databases.SQLITE);
		userDB = factory.getUserDB(dbURL);
		
		try {
			this.connect(dbURL);
			load();
		}
		catch (ClassNotFoundException | SQLException e) {
			log.error(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	

	@Override
	public void connect(String dbURL) throws ClassNotFoundException, SQLException {
		userDB.connect(dbURL);

	}

	@Override
	public Connection getConnection() {
		return this.userDB.getConnection();
	}

	@Override
	public void disconnect() throws SQLException {
		this.userDB.disconnect();

	}

	@Override
	public boolean insertInformation(InformationToPropagate information) {
		// Check if the info already exists
		boolean ifExist = infoToPropagate.contains(information);
		
		// Return variable
		boolean ifInsertOK = false;
		
		// If it doesn't exist and the item is a valid item
		if (!ifExist && information.getItem() <= Literals.NUM_MAX_ITEMS) {
			
			// NO NEED TO CALL INSERT, IT'S DIRECTLY ADDED TO THE SET -> INSERT WHEN PERSIST OP IS CALLED
			//insert(information.getUser(), information.getItem(), information.getContext(), information.getRating(), null, 1);
			infoToPropagate.add(information);
			ifInsertOK = true;
			
			/* WORKING!
			infoToPropagate.forEach(System.out::println);
			System.out.println("------\n");
			*/
		}
		
		return ifInsertOK;
	}

	// NOT USED IN THIS IMPLEMENTATION
	@Override
	public boolean insert(long user, long item, long context, double rating, String opinion, int userProvided) {
		Simulation.log.log(Level.SEVERE, "MEMORY INSERT OPERATION -> NO EFFECT");
		return false;
	}
	
	@Override
	public void deleteAllInformationFromTable() {
		infoToPropagate.clear();
		userDB.deleteAllInformationFromTable();
	}

	@Override
	public void deleteInformationFromTable(long userID) {
		
		// See get instance where userID == @param userID
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getId_user() == userID) {
	            infoToPropagate.remove(currentInfo);
	        }
	    }

	}

	// NEVER USED !!!!!!!!!!!!!!!!
	@Override
	public void createNewDatabase() {
	}
	
	/**
	 * Save operation for persisting user_context_item info at the end of the simulation
	 */
	public void save() {
		
		//System.out.println("*Saving user data*");
		
		Connection conn = getConnection();
		
		int count = 0;
		
		//System.out.println("Set elements: " + infoToPropagate.size());
		
		try {
			
			conn.setAutoCommit(false);
			conn.commit();
			
			// Prepared Statement creation (for only 1 plan for all insert ops)
			PreparedStatement p = conn
					.prepareStatement("INSERT INTO " + USER_ITEM_CONTEXT_TB + " VALUES(?, ?, ?, ?, ?, ?)");
			
			for (InformationToPropagate i: infoToPropagate) {
				
				// Set prepared statement parameters
				p.setString(1, Long.toString(i.getUser()));
				p.setString(2, Long.toString(i.getItem()));
				p.setString(3, Long.toString(i.getContext()));
				p.setString(4, Double.toString(i.getRating()));
				p.setString(5, null);
				p.setString(6, "1");
				
				// Prepared statement execute update
				p.executeUpdate();
				
				p.clearParameters();
				
				count++;
			}
			
			//System.out.println("Inserted in database: " + count);
			
			p.close();
			
			conn.commit();
		}
		catch (SQLException e) {
			log.error(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * NOT USED. NO INFO AT THE BEGINNING.
	 * 
	 * Clears set elements and adds all file information to it.
	 */
	private void load() {
		//try {
			infoToPropagate.clear();
			//infoToPropagate.addAll(getInfoToPropagate());
		/*}
		catch (SQLException e) {
			log.error(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}*/
	}
	
	/**
	 * NOT USED, INFO PERISTED IS FEWER THAN ALL INFOTOPROPAGATE.
	 * 
	 * @return
	 * @throws SQLException
	 */
	private Set<InformationToPropagate> getInfoToPropagate() throws SQLException{
		
		Set<InformationToPropagate> info = new HashSet<InformationToPropagate>();
		
		String sql = "SELECT * FROM " + USER_ITEM_CONTEXT_TB;
		Statement selectStatement = getConnection().createStatement();

		ResultSet results = selectStatement.executeQuery(sql);

		/*
		ResultSetMetaData rsmd = results.getMetaData();
		int columnsNumber = rsmd.getColumnCount();
		
		System.out.println(columnsNumber);
		
		
		while (results.next()) {
		    for (int i = 1; i <= columnsNumber; i++) {
		        if (i > 1) System.out.print(",  ");
		        String columnValue = results.getString(i);
		        System.out.print(columnValue + " " + rsmd.getColumnName(i));
		    }
		    System.out.println("");
		}*/
		
		
		// id_user, id_item, id_context, rating, opinion, user_provided
		while (results.next()) {
			long id_user = results.getLong("id_user");
			long user = results.getLong("user");
			long item = results.getLong("item");
			long context = results.getLong("context");
			double rating = results.getDouble("rating");
			int TTL = results.getInt("TTL");
			double TTP = results.getDouble("TTP");
			int isTTPInitialized = results.getInt("isTTPInitialized");
			String location = results.getString("location");
			long currentTime = results.getLong("currentTime");
			info.add(new InformationToPropagate(id_user, user, item, context, rating, TTL, TTP,
					isTTPInitialized, location, currentTime));
		}

		results.close();
		selectStatement.close();
		
		System.out.println("FIN LOAD");

		return info;
		
	}

}
