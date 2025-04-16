package es.unizar.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.InformationToPropagate;
import es.unizar.util.Literals;

/**
 * EXCLUDED FROM BUILD PATH -> DO NOT USE.
 * 
 * @author apied
 *
 */
public class MemoryDataManagementQueueDB implements DataManagementQueueDB {
	
	// Info stored in memory before persistance (saving)
	private Set<InformationToPropagate> infoToPropagate = new HashSet<InformationToPropagate>();
	
	// DB where data will be persisted when "save" button pressed or when finished simulation
	DataManagementQueueDB queueDB;
	
	private static final Logger log = LoggerFactory.getLogger(MemoryDataManagementQueueDB.class);
	
	public MemoryDataManagementQueueDB(String dbURL) {
		
		super();
		
		// Cannot use "Literals.currentDBUsed" because current db being used is "Memory"
		DAOFactory factory = DAOFactory.getFactory(Literals.Databases.SQLITE);
		queueDB = factory.getQueueDB(dbURL);
		
		try {
			this.connect(dbURL);
		}
		catch (ClassNotFoundException | SQLException e) {
			log.error(e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	

	@Override
	public void connect(String dbURL) throws ClassNotFoundException, SQLException {
		queueDB.connect(dbURL);
		
	}

	@Override
	public Connection getConnection() {
		return queueDB.getConnection();
	}

	@Override
	public void disconnect() throws SQLException {
		queueDB.disconnect();
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#insertInformation(es.unizar.gui.simulation.InformationToPropagate)
	 */
	@Override
	public boolean insertInformation(InformationToPropagate information) {
		
		boolean ifExist = verifyIfExist(information);
		boolean ifInsertOK = false;
		
		if (!ifExist) {
			insert(information);
			ifInsertOK = true;
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
	 * @param information: The information to be inserted (propagated)
	 * @return True if the information was correctly inserted and False in the
	 *         otherwise.
	 */
	private void insert(InformationToPropagate information) {
		infoToPropagate.add(information);
	}
	
	/**
	 * Verify if the information is not into queue.db.
	 * 
	 * @param information: The information to be checked if it's already contained.
	 * @return True if the information is into queue.db and False in the otherwise.
	 */
	private boolean verifyIfExist(InformationToPropagate information) {
		return infoToPropagate.contains(information);
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getInformation()
	 */
	@Override
	public LinkedList<InformationToPropagate> getInformation() {

		LinkedList<InformationToPropagate> informationQueue = new LinkedList<>();
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getTTP() <= 0 && currentInfo.isIsTTPInitialized() == 1) {
	        	informationQueue.add(currentInfo);
	        }
	    }
		
		return informationQueue;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getInformation(long)
	 */
	@Override
	public LinkedList<InformationToPropagate> getInformation(long id_user) {

		LinkedList<InformationToPropagate> informationQueue = new LinkedList<>();
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getTTP() <= 0 && currentInfo.isIsTTPInitialized() == 1 && currentInfo.getId_user() == id_user) {
	        	informationQueue.add(currentInfo);
	        }
	    }
		
		return informationQueue;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getUsersWithInformationToPropagate()
	 */
	@Override
	public LinkedList<Long> getUsersWithInformationToPropagate() {

		LinkedList<Long> idUsers = new LinkedList<>();
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getTTP() <= 0 && currentInfo.isIsTTPInitialized() == 1) {
	        	idUsers.add(currentInfo.getId_user());
	        }
	    }
		
		// GROUP BY id_user
		Collections.sort(idUsers);
		
		return idUsers;
	}

	/* (non-Javadoc)
	 * @see es.unizar.dao.DataManagementQueueDB#getNumberItemsByUserWithoutInitializeTTP()
	 */
	@Override
	public LinkedList<String> getNumberItemsByUserWithoutInitializeTTP() {

		LinkedList<String> list = new LinkedList<>();
		LinkedList<Long> idUsers = new LinkedList<>();
		
		// SELECT id_user FROM information WHERE isTTPInitialized==0
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.isIsTTPInitialized() == 0) {
	        	idUsers.add(currentInfo.getId_user());
	        }
	    }
		
		// GROUP BY id_user
		Collections.sort(idUsers);
		
		// SELECT id_user, COUNT(*) FROM V1 (GROUP BY id_user) ALREADY GROUPED
		Set<Long> uniqueSet = new HashSet<Long>(idUsers);  
        for (Long i : uniqueSet) {  
        	list.add(Long.toString(i) + "," + Integer.toString(Collections.frequency(list, i)));
        }
		
		return list;
	}

	@Override
	public void deleteInformation(InformationToPropagate information) {
		// See get instance where information matches set element
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getId_user() == information.getId_user() && currentInfo.getUser() == information.getUser()
	        		&& currentInfo.getItem() == information.getItem() && currentInfo.getContext() == information.getContext()
	        		&& currentInfo.getRating() == information.getRating()) {
	            infoToPropagate.remove(currentInfo);
	        }
	    }
	}

	@Override
	public void deleteAllInformationFromTable() {
		infoToPropagate.clear();
		queueDB.deleteAllInformationFromTable();
	}

	@Override
	public int updateExchange(long farthestUser, long id_user) {
		int countUpdated = 0;
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        if (currentInfo.getId_user() == id_user && currentInfo.getTTP()<=0 && currentInfo.isIsTTPInitialized() == 1) {
	        	
	        	/*
	        	InformationToPropagate newInfo = new InformationToPropagate(farthestUser, farthestUser, 
	        			currentInfo.getItem(), currentInfo.getContext(), currentInfo.getRating(), currentInfo.getTTL(), currentInfo.getTTP(),
						0, currentInfo.getLocation(), currentInfo.getCurrentTime());
	        	
	            infoToPropagate.remove(currentInfo);
	            infoToPropagate.add(newInfo);
	            */
	            
	            currentInfo.setId_user(farthestUser);
	            currentInfo.setIsTTPInitialized(0);
	            
	            countUpdated++;
	        }
	    }
		
		return countUpdated;
	}

	@Override
	public void updateTTL() {
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        
	        currentInfo.setTTL(currentInfo.getTTL()-1);
	        
	        if (currentInfo.getTTL() <= 0) {
	        	infoToPropagate.remove(currentInfo);
	        }
	    }
	}

	@Override
	public void updateTTP() {
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        
	        if (currentInfo.isIsTTPInitialized() == 1)
	        	currentInfo.setTTP(currentInfo.getTTP() - Configuration.simulation.getTimeForIterationInSecond());
	    }
	}

	@Override
	public void initializeAllTTP() {
		double TTP = getInitialTimeToPropagate(NUMBER_RATINGS_TO_TRANSFER);
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        
	        if (currentInfo.isIsTTPInitialized() == 0) {
	        	currentInfo.setTTP(TTP);
	        	currentInfo.setIsTTPInitialized(1);
	        }
	    }

	}

	@Override
	public void initializeAllTTP(int countRatings, long id_user) {
		
		double TTP = getInitialTimeToPropagate(countRatings);
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        
	        if (currentInfo.isIsTTPInitialized() == 0 && currentInfo.getId_user() == id_user) {
	        	currentInfo.setTTP(TTP);
	        	currentInfo.setIsTTPInitialized(1);
	        }
	    }
	}

	@Override
	public void initializeOneTTP(InformationToPropagate information) {
		
		double TTP = getInitialTimeToPropagate(NUMBER_RATINGS_TO_TRANSFER);
		
		for (Iterator<InformationToPropagate> it = infoToPropagate.iterator(); it.hasNext(); ) {
	        InformationToPropagate currentInfo = it.next();
	        
	        if (currentInfo.getId_user() == information.getId_user() && currentInfo.getUser() == information.getUser()
	        		&& currentInfo.getItem() == information.getItem() && currentInfo.getContext() == information.getContext()
	        		&& currentInfo.getRating() == information.getRating() && currentInfo.isIsTTPInitialized() == 0) {
	        	currentInfo.setTTP(TTP);
	        	currentInfo.setIsTTPInitialized(1);
	        }
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
	
	/**
	 * Save operation for persisting user_context_item info at the end of the simulation
	 */
	public void save() {
		
		//System.out.println("*Saving QUEUE data*");
		
		Connection conn = getConnection();
		
		int count = 0;
		
		//System.out.println("Set elements: " + infoToPropagate.size());
		
		try {
			
			conn.setAutoCommit(false);
			conn.commit();
			
			// Prepared Statement creation (for only 1 plan for all insert ops)
			PreparedStatement p = conn
					.prepareStatement("INSERT INTO information VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			for (InformationToPropagate i: infoToPropagate) {
				
				//System.out.println(i+i.getLocation()+";"+i.getCurrentTime());
				
				// Set prepared statement parameters
				p.setString(1, Long.toString(i.getId_user()));
				p.setString(2, Long.toString(i.getUser()));
				p.setString(3, Long.toString(i.getItem()));
				p.setString(4, Long.toString(i.getContext()));
				p.setString(5, Double.toString(i.getRating()));
				p.setString(6, Integer.toString(i.getTTL()));
				p.setString(7, Double.toString(i.getTTP()));
				p.setString(8, Integer.toString(i.isIsTTPInitialized()));
				p.setString(9, i.getLocation());
				p.setString(10, Long.toString(i.getCurrentTime()));
				
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
			try {
				conn.rollback();
			} catch (SQLException ex) {
				log.error(e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
			System.out.println(e.getMessage());
		}
		
	}

}
