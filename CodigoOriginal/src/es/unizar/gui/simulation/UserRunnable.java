package es.unizar.gui.simulation;

import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.UserInfo;
import es.unizar.util.DebugFilter;
import es.unizar.util.DebugFormatter;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

/**
 * Main algorithm for the simulation.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class UserRunnable implements Runnable { // , Cloneable

	public volatile boolean running;
	public static boolean firstTime;
	
	//public User[] user;
	
	// Semaphore to pause/play
	public volatile boolean paused;
	private final Object pauseLock = new Object();
	
	// Logger that prints times (for debugging)
	public static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	
	Map<Integer,UserInfo.UserState> stateOfUsers;
	Map<Pair<Integer,Integer>,Double> timeUsersInRooms;
	
	public UserRunnable(User[] user,Map<Integer,UserInfo.UserState> stateOfUsers,Map<Pair<Integer,Integer>,Double> timeUsersInRooms) {
		//this.user = user;
		this.running = true;
		
		this.paused = false;
		
		this.timeUsersInRooms = timeUsersInRooms;
		this.stateOfUsers = stateOfUsers;
	}

	@Override
	public void run() {
		System.out.println("***DEBUG-UserRunnable: Iniciando thread de simulación"); // Añadido por Nacho Palacio 2025-04-24
		
		/* Logger configuration */
		log.setUseParentHandlers(false);
		// Set logger's level
		log.setLevel(Literals.DEBUG_DEFAULT_LEVEL);
		
		// Add formatter
		DebugFormatter df = new DebugFormatter();
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(df);
		log.addHandler(ch);
		// If not, log messages under INFO level aren't printed in console
		// https://newbedev.com/why-are-the-level-fine-logging-messages-not-showing
		ch.setLevel(Literals.DEBUG_DEFAULT_LEVEL);
		
		// Add filter (which messages should be shown
		log.setFilter(new DebugFilter());
		
		/* Simulation algorithm */
		firstTime = true;
		//Configuration.simulation.currentTime();
		
		int iteration = 1;
		
		if(MainSimulator.db.isConnected() && Configuration.simulation.registerSimInDB) {
			MainSimulator.db.registerSimulation(Configuration.simulation.getTimeAvailableUserInSecond(),Configuration.simulation.getDelayObservingPaintingInSecond(),Configuration.simulation.getUserVelocityInKmByHour(),Configuration.simulation.getKmToPixel(),Configuration.simulation.getTimeOnStairs());
		}
		
		while (running) {
			
			// Wait if simulation is paused
			// https://stackoverflow.com/questions/16758346/how-pause-and-then-resume-a-thread
			synchronized (pauseLock) {
				// Is it really necessary?
				if (!running) { // may have changed while waiting to
	                // synchronize on pauseLock
	                break;
	            }
				if (this.paused) {
	                try {
	                    synchronized (pauseLock) {
	                    	if (MainSimulator.gui.isSelected())
	                    		repaint(); // If not, the scenario isn't printed when paused (white screen)
	                        pauseLock.wait(); // Block
	                    }
	                } catch (InterruptedException ex) {
	                	log.log(Level.SEVERE, ex.toString());
	                    break;
	                }
	                if (!running) { // running might have changed since we paused
	                    break;
	                }
	            }
			}
			
			// UserRunnable's code
			if (firstTime) {
				// Create required databases:
				String recommendationAlgorithm = Configuration.simulation.getRecommendationAlgorithm();
				if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
					Configuration.simulation.initializeUserDB_Centralized(recommendationAlgorithm);
				} else if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
					Configuration.simulation.initializeUserDB_P2P(recommendationAlgorithm);
				}

				// It initializes the initial position of users.
				System.out.println("***DEBUG-UserRunnable: Antes de initializeUsers()"); // Añadido por Nacho Palacio 2025-04-24 

				Configuration.simulation.initializeUsers();

				System.out.println("***DEBUG-UserRunnable: Después de initializeUsers(), tamaño de userList: " + 
                  (Configuration.simulation.userList != null ? Configuration.simulation.userList.size() : "NULL")); // Añadido por Nacho Palacio 2025-04-24

				// UserRunnable "user" list is null; Same for MainMuseumSimulator
				MainSimulator.floor.addUsersToFloorGraph(Configuration.simulation.userList);

				System.out.println("***DEBUG-UserRunnable: Después de addUsersToFloorGraph"); // 
				
				if (MainSimulator.gui.isSelected()) {
					System.out.println("***DEBUG-UserRunnable: Antes de repaint()"); // Añadido por Nacho Palacio 2025-04-24
					repaint();
					System.out.println("***DEBUG-UserRunnable: Después de repaint()"); // Añadido por Nacho Palacio 2025-04-24
				}
				
				if (Literals.COMPILE_DISTANCES_STATS) {
					Configuration.simulation.updateUserDistances(0);
				}

				firstTime = false;
				
				// Print time when finished initializing users and dbs
				Configuration.simulation.currentTime();

				System.out.println("***DEBUG-UserRunnable: Finalizada inicialización (firstTime)"); // Añadido por Nacho Palacio 2025-04-24
				
			} else {
				
				long initialTime = 0, finalTime = 0;
				
				log.log(Level.INFO, Literals.BEGINNING_DEBUG_MESSAGE);
				
				
				// Randomly changes the mood of each user: Mood: 10--> happy, 11--> neutral, 12--> sad.
				
				initialTime = System.currentTimeMillis();
				
				for (int userPosition = 0; userPosition < Configuration.simulation.userList.size(); userPosition++) {
					User currentUser = Configuration.simulation.userList.get(userPosition);
					if (Configuration.simulation.currentTimeOfUsers[currentUser.userID - 1] >= Configuration.simulation.getTimeToChangeMood()) {
						Configuration.simulation.changeMoodOfUsers(currentUser);
						MainSimulator.printConsole("User " + currentUser.userID + " has changed his/her mood.", Level.INFO);
					}
				}
				
				finalTime = System.currentTimeMillis();
				
				log.log(Level.INFO, "- Time to change users' mood: " + (finalTime-initialTime));
				
				// Update the position of users.
				
				initialTime = System.currentTimeMillis();
				Configuration.simulation.updateUsers(stateOfUsers,timeUsersInRooms);
				finalTime = System.currentTimeMillis();
				
				log.log(Level.INFO, "- Time to update users' position: " + (finalTime-initialTime));
				
				initialTime = System.currentTimeMillis();
				int timeForIteration = (int) Configuration.simulation.getTimeForIterationInSecond() * iteration;
				int totalTime = (int) Configuration.simulation.getTimeAvailableUserInSecond();
				
				int timeSpent = (timeForIteration > totalTime) ? totalTime : timeForIteration;
				
				// Update user distances with time
				if (Literals.COMPILE_DISTANCES_STATS) {
					//System.out.println("DISTANCE STATS " + timeSpent + ": ");
					Configuration.simulation.updateUserDistances(timeSpent);
					//System.out.println();
				}
				
				// Print time spent in console
				if (Configuration.simulation.getTimeForIterationInSecond() >= 10 || (Configuration.simulation.getTimeForIterationInSecond() < 10 && timeSpent % 5 == 0)) {
					MainSimulator.printConsole( "Time spent: " + timeSpent + "/" + totalTime, Level.WARNING);
					// Print in console too if wanted
					//System.out.println("Time spent: " + timeSpent + "/" + totalTime);
				}
				finalTime = System.currentTimeMillis();
				log.log(Level.INFO, "PRINT CURRENT TIME: " + (finalTime-initialTime));
				
				// Paints on the screen the current position of the users.
				
				initialTime = System.currentTimeMillis();
				if (MainSimulator.gui.isSelected()) {
					repaint();
				}
				finalTime = System.currentTimeMillis();
				log.log(Level.INFO, "- Time to repaint: " + (finalTime-initialTime));

				if (running) {
					// Exchange of information between users, taking into account the selected network type and propagation strategy.
					if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
						String propagationStrategy = Configuration.simulation.getPropagationStrategy();
						if (propagationStrategy.equalsIgnoreCase("Opportunistic")) {
							Configuration.simulation.exchangeDataP2POpportunistic();
						} else if (propagationStrategy.equalsIgnoreCase("Flooding")) {
							Configuration.simulation.exchangeDataP2PFlooding();
						}
					}
	
					// If the user receives new information, then the recommended path is updated. Without the neighbors, only if it is necessary to update for the RS user.
					Configuration.simulation.updatePathRecommender();
				}
				
				log.log(Level.INFO, Literals.ENDING_DEBUG_MESSAGE);
				
				iteration++;
			}

			// Refresh the screen.
			double millis = Configuration.simulation.getScreenRefreshTimeInSecond() * 1000;
			sleep(millis);
			
			MainSimulator.userInfo.reloadTables();
		}
		
		MainSimulator.printConsole("Finished simulation\n", Level.WARNING);
	}

	/**
	 * Set the pause/play state.
	 * 
	 * @param paused: The current state (pause/play).
	 */
	public synchronized void setPaused(boolean paused) {
		this.paused = paused;
	}

	/**
	 * Get the lock.
	 * 
	 */
	public synchronized Object getPauseLock() {
		return pauseLock;
	}

	/**
	 * Set the state of the executing.
	 * 
	 * @param running: The current state.
	 */
	public synchronized void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * The thread sleeps for a time.
	 * 
	 * @param millis: Milliseconds.
	 */
	public static void sleep(double millis) {
		try {
			Thread.sleep((long) millis);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Repaint the current position of the users on the floor panel.
	 */
	public static void repaint() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MainSimulator.repaintFloorPanelCombined();
			}
		});
	}
}
