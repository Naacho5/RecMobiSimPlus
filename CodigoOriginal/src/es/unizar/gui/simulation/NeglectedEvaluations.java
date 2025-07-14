package es.unizar.gui.simulation;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import es.unizar.access.DataAccessSimulations;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.util.Literals;

/**
 * 
 * @author apied
 *
 */
public class NeglectedEvaluations extends javax.swing.JDialog{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private File simulations;
	private JFileChooser simulationsChooser;
	private File neglectedEvaluationsFolder;
	private JFileChooser neglectedEvaluationsFolderChooser;

	private File directory = new File(Literals.PATH_SIMULATIONS);
	
	public NeglectedEvaluations(java.awt.Frame parent) {
		
		super(parent);
		
		/*
		 * Window configurations
		 */
		setAlwaysOnTop(true);
		
		setTitle("Neglected Evaluations");

		initComponents();
		pack();
		
		setDialogLocation(parent);
	}

	private void initComponents() {

		/*
		 * Initialize file choosers
		 */
		simulationsChooser = new JFileChooser();
		simulationsChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.txt", "txt"));
		simulationsChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		neglectedEvaluationsFolderChooser = new JFileChooser();
		neglectedEvaluationsFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow choosing the directory (to save the 3 files)
		neglectedEvaluationsFolderChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		neglectedEvaluationsPanel = new JPanel();
		getContentPane().add(neglectedEvaluationsPanel);
		
		lblSimulationsFile = new JLabel("Simulations file: ");
		neglectedEvaluationsPanel.add(lblSimulationsFile);
		
		textfieldSimulationsFile = new JTextField();
		textfieldSimulationsFile.setFont(new Font("SansSerif", Font.PLAIN, 14));
		textfieldSimulationsFile.setColumns(10);
		textfieldSimulationsFile.setEditable(false);
		neglectedEvaluationsPanel.add(textfieldSimulationsFile);
		
		btnSimulationsButton = new JButton("Select");
		btnSimulationsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				simulationsChooser.setCurrentDirectory(directory);
				simulationsChooser.setDialogTitle("Select file which contains all simulations with their corresponding parameters");
				
				switch(simulationsChooser.showOpenDialog(neglectedEvaluationsPanel)) {
				
					case JFileChooser.APPROVE_OPTION:
						simulations = simulationsChooser.getSelectedFile();
						textfieldSimulationsFile.setText(simulations.getPath());
						break;
						
					default:
						MainSimulator.printConsole("FILE NOT CHOSEN", Level.WARNING);
						break;
						
				}
			}
			
		});
		neglectedEvaluationsPanel.add(btnSimulationsButton);
		
		lblNeglectedEvaluationsFolder = new JLabel("Store results in: ");
		neglectedEvaluationsPanel.add(lblNeglectedEvaluationsFolder);
		
		textfieldNeglectedEvaluationsFolder = new JTextField();
		textfieldNeglectedEvaluationsFolder.setFont(new Font("SansSerif", Font.PLAIN, 14));
		textfieldNeglectedEvaluationsFolder.setColumns(10);
		textfieldNeglectedEvaluationsFolder.setEditable(false);
		neglectedEvaluationsPanel.add(textfieldNeglectedEvaluationsFolder);
		
		btnNeglectedEvaluationsFolderButton = new JButton("Select");
		btnNeglectedEvaluationsFolderButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				neglectedEvaluationsFolderChooser.setCurrentDirectory(directory);
				neglectedEvaluationsFolderChooser.setDialogTitle("Select folder to store all simulation results");
				
				switch(neglectedEvaluationsFolderChooser.showOpenDialog(neglectedEvaluationsPanel)) {
				
					case JFileChooser.APPROVE_OPTION:
						neglectedEvaluationsFolder = neglectedEvaluationsFolderChooser.getSelectedFile();
						textfieldNeglectedEvaluationsFolder.setText(neglectedEvaluationsFolder.getPath());
						
					default:
						break;
						
				}
			}
			
		});
		neglectedEvaluationsPanel.add(btnNeglectedEvaluationsFolderButton);
		
		runNeglectedEvaluationsButton = new JButton("RUN NEGLECTED EVALUATIONS");
		runNeglectedEvaluationsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				run();
			}
		});
		neglectedEvaluationsPanel.add(runNeglectedEvaluationsButton);
	}
	
	/**
	 * GUI FIELDS.
	 */
	private JPanel neglectedEvaluationsPanel;
	
	private JLabel lblSimulationsFile, lblNeglectedEvaluationsFolder;
	private JTextField textfieldSimulationsFile, textfieldNeglectedEvaluationsFolder;
	private JButton btnSimulationsButton, btnNeglectedEvaluationsFolderButton;
	private JButton runNeglectedEvaluationsButton;
	
	
	/**
	 * Set dialog's location centering its x position and placing it on top (y position = 0).
	 * @param parent 
	 */
	private void setDialogLocation(Frame parent) {
		
		Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new java.awt.Point(x, y));
	}
	
	/**
	 * Check files and start thread.
	 */
	private void run() {
		if (simulations != null && neglectedEvaluationsFolder != null && simulations.isFile() && neglectedEvaluationsFolder.isDirectory()) {
			Thread thread = new Thread(new Runnable() {
			    @Override
			    public void run() {
			    	runNeglectedEvaluations();
			    }
			});
			thread.start();
			
			this.dispose();
		}
	}
	
	
	/**
	 * CODE FOR NEGLECTED EVALUATIONS:
	 * 
	 * - Unselect and disable gui
	 * - Checks (simulations) file and (neglectedEvaluations) folder exist
	 * - Reads file with simulations' parameters (fileChooser)
	 * - For every simulation:
	 * 		- Creates configuration (which creates simulation)
	 * 		- Creates UserRunnable and starts thread
	 * 		- Waits till the end of the simulation
	 * 		- Copy and rename DB files to "neglectedEvaluations" folder with its corresponding number
	 * 
	 * - Select and enable gui again
	 */
	private void runNeglectedEvaluations() {
		// Unselect and disable gui
		MainSimulator.gui.setSelected(false);
		MainSimulator.gui.setEnabled(false);
		
		// Check (simulations) file and (neglectedEvaluations) folder exist
		if (simulations.exists() && neglectedEvaluationsFolder.exists()) {
			
			try {
				// Read file with simulations' parameters (fileChooser)
				DataAccessSimulations simulationsAccess = new DataAccessSimulations(this.simulations);
				simulationsAccess.printAllProperties(); // Añadido por Nacho Palacio 2025-04-13.
				
				// For every simulation
				int numSimulations = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.NUMBER_SIMULATIONS)).intValue();
				for (int i = 1; i <= numSimulations; i++) {
					
					// Create configuration (which creates simulation)
					createConfiguration(simulationsAccess, i);
					
					MainSimulator.printConsole("Starting neglected simulation " + i, Level.WARNING);
					System.out.println("Starting neglected simulation " + i);
					
					// Create UserRunnable and start thread
					MainSimulator.startMenuItem.doClick(); // Emulate action of pressing start button -> Logic to start simulation (userRunnable).
					
					// Wait till the end of the simulation
					try {
						MainSimulator.userRunnableThread.join();
						
						System.out.println("Finished simulation " + i + "!");
						
					} catch (InterruptedException e) {
						e.printStackTrace();
						MainSimulator.printConsole(e.getMessage(), Level.WARNING);
						MainSimulator.printConsole("Exitting neglected evaluation", Level.WARNING);
						break;
					}
					
					System.out.println("After finish, copy files to folder: " + neglectedEvaluationsFolder);
					
					// Copy and rename DB files to "neglectedEvaluations" folder with its corresponding number
					storeSimulationResults(i);
					
					Configuration.simulation = null;
					MainSimulator.userRunnableThread.interrupt();
				}
			}
			catch (Exception e) {
				MainSimulator.printConsole("Neglected evaluations exception: " + e.getMessage(), Level.SEVERE);
				e.printStackTrace();
			}
		} // END if (simulation.exists())
		
		// Select and enable gui again
		MainSimulator.gui.setSelected(true);
		MainSimulator.gui.setEnabled(true);
		
	}

	/**
	 * Read all properties from DataAccessSimulations and create configuration for "numSimulation" simulation.
	 * 
	 * @param simulationsAccess		DataAccess which contains simulations' properties
	 * @param numSimulation			number of simulation to read
	 */
	private void createConfiguration(DataAccessSimulations simulationsAccess, int numSimulation) {

		// Parameters for the simulation:
		// Time available for the user [hour].
		int timeAvailableUser = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.TIME_AVAILABLE_USER + numSimulation)).intValue();
		// Delay observing painting [seconds].
		int delayObservingPainting = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.DELAY_OBSERVING_PAINTING + numSimulation)).intValue();
		// Real time per iteration [seconds].
		double timeForIteration = Double.valueOf(simulationsAccess.getPropertyValue(Literals.TIME_FOR_ITERATION + numSimulation)).doubleValue();
		// Iteration time/Screen refresh time [seconds].
		double screenRefreshTime = Double.valueOf(simulationsAccess.getPropertyValue(Literals.SCREEN_REFRESH_TIME + numSimulation)).doubleValue();
		// Time for the paths [hour].
		double timeForThePaths = Double.valueOf(simulationsAccess.getPropertyValue(Literals.TIME_FOR_PATHS + numSimulation)).doubleValue();
		// User velocity [km/h].
		double userVelocity = Double.valueOf(simulationsAccess.getPropertyValue(Literals.USER_VELOCITY + numSimulation)).doubleValue();
		// 1 km represents in pixels [pixel].
		double kmToPixel = Double.valueOf(simulationsAccess.getPropertyValue(Literals.KM_TO_PIXEL + numSimulation)).doubleValue();
		// TTL of the items to propagate [seconds].
		int ttl = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.TTL + numSimulation)).intValue(); // Propagation of items
		// Time on the stairs [seconds].
		int timeOnStairs = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.TIME_ON_STAIRS + numSimulation)).intValue();
		// Minimum time to update recommendation [seconds].
		int minimumTimeToUpdateRecommendation = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.MINIMUM_TIME_TO_UPDATE_RECOMMENDATION + numSimulation)).intValue();
		// Communication range [m].
		int communicationRange = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.COMMUNICATION_RANGE + numSimulation)).intValue();
		// Maximum knowledge base size [Mb].
		int maxKnowledgeBaseSize = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.MAX_KNOWLEDGE_BASE_SIZE + numSimulation)).intValue();
		// Communication bandwidth [Mbps].
		int communicationBandwidth = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.COMMUNICATION_BANDWIDTH + numSimulation)).intValue();
		// Latency of transmission [s].
		int latencyOfTransmission = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.LATENCY_OF_TRANSMISSION + numSimulation)).intValue();
		// Time to change the mood [seconds].
		int timeToChangeMood = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.TIME_TO_CHANGE_MOOD + numSimulation)).intValue();
		// Is seed selected
		boolean useFixedSeed = Boolean.valueOf(simulationsAccess.getPropertyValue(Literals.USE_FIXED_SEED + numSimulation));
		// Seed value for random numbers
		long seed = Long.valueOf(simulationsAccess.getPropertyValue(Literals.SEED + numSimulation)).longValue();

		// Parameters for the experimentation:
		// Number of RS users.
		int numberOfSpecialUser = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.NUMBER_OF_SPECIAL_USERS + numSimulation)).intValue();
		// Number of non-RS users.
		int numberOfNonSpecialUser = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.NUMBER_OF_NON_SPECIAL_USERS + numSimulation)).intValue();
		// File name of non-RS users.
		String nonSpecialUserPaths = Literals.PATH_MAPS + simulationsAccess.getPropertyValue(Literals.NON_SPECIAL_USER_PATHS + numSimulation);
		// Path strategy of non-RS users.
		String pathStrategy = (String) simulationsAccess.getPropertyValue(Literals.PATH_STRATEGY + numSimulation);
		// Recommendation algorithm.
		String recommendationAlgorithm = (String) simulationsAccess.getPropertyValue(Literals.RECOMMENDATION_ALGORITHM + numSimulation);
		// Threshold of recommendation.
		float thresholdRecommendation = Float.valueOf(simulationsAccess.getPropertyValue(Literals.THRESHOLD_RECOMMENDATION + numSimulation)).floatValue();
		// Threshold of similarity.
		double thresholdSimilarity = Double.valueOf(simulationsAccess.getPropertyValue(Literals.THRESHOLD_SIMILARITY + numSimulation)).doubleValue();
		// How many items to recommend.
		int howMany = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.HOW_MANY + numSimulation)).intValue();
		// Type of network.
		String networkType = (String) simulationsAccess.getPropertyValue(Literals.NETWORK_TYPE + numSimulation);
		// Propagation strategy:
		String propagationStrategy = (String) simulationsAccess.getPropertyValue(Literals.PROPAGATION_STRATEGY + numSimulation);
		// Probability of user disobedience.
		double probabilityUserDisobedience = Double.valueOf(simulationsAccess.getPropertyValue(Literals.PROBABILITY_USER_DISOBEDIENCE + numSimulation)).doubleValue();
		// Number of votes received.
		int numberVoteReceived = Integer.valueOf(simulationsAccess.getPropertyValue(Literals.NUMBER_VOTES_RECEIVED + numSimulation)).intValue();
		// Checkbox which determines if paths for users must be generated or reused.
		boolean generateUserPaths = Boolean.valueOf(simulationsAccess.getPropertyValue(Literals.GENERATE_USER_PATHS + numSimulation));

		// Create configuration (which creates simulation)
		/*Configuration config = new Configuration(1, 30, 60, 1, 1, 3, 6597, 180, 60, 30, 250, 1, 54, 1, 1800, true, 13, 1, 175, "rand_non_special_user_paths_175.txt",
				"Completely-random (FULLY-RAND)", "Completely-random (FULLY-RAND)", (float) 2.5, 0.1, 10, null, null, 0.4, 40, true);*/
		
		Configuration config =  new Configuration(timeAvailableUser, delayObservingPainting, timeForIteration, screenRefreshTime, timeForThePaths, userVelocity, kmToPixel, ttl, timeOnStairs,
					minimumTimeToUpdateRecommendation, communicationRange, maxKnowledgeBaseSize, communicationBandwidth, latencyOfTransmission, timeToChangeMood, useFixedSeed, seed,
					numberOfSpecialUser, numberOfNonSpecialUser, nonSpecialUserPaths, pathStrategy, recommendationAlgorithm, thresholdRecommendation, thresholdSimilarity, howMany, networkType,
					propagationStrategy, probabilityUserDisobedience, numberVoteReceived, generateUserPaths);
	}
	
	/**
	 * Store the simulation results 
	 * @param id
	 */
	private void storeSimulationResults(int id) {
		
		if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
			
			File db = new File(Literals.DB_CENTRALIZED_USER_PATH);
			if (db.exists()) {
				
				if (storeResults(db, id)) {
					MainSimulator.printConsole("Results of simulation " + id + " stored correctly", Level.SEVERE);
					if (storeStats(id)) {
						MainSimulator.printConsole("Statistics of simulation " + id + " stored correctly", Level.SEVERE);
					}
					else {
						MainSimulator.printConsole("Incorrect move option saving statistics for: " + id, Level.SEVERE);
					}
				}
				else {
					MainSimulator.printConsole("Incorrect move option: " + id, Level.SEVERE);
				}
			}
		}
		
		else if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
			
			for (int i = Configuration.simulation.getNumberOfNonSpecialUser() + 1; i <= Configuration.simulation.userList.size(); i++) {
				
				File p2pUser = new File(Literals.DB_P2P_USER_PATH + i + ".db");
				File p2pQueue = new File(Literals.DB_NEW_QUEUE_PATH + i + ".db");
				
				if (p2pUser.exists() && p2pQueue.exists()) {
					
					// Store P2P User DB
					if (storeResults(p2pUser, id)) {
						MainSimulator.printConsole("Results of user " + i + "'s simulation " + id + " stored correctly", Level.SEVERE);
						
						if (storeStats(id)) {
							MainSimulator.printConsole("Statistics of simulation " + id + " stored correctly", Level.SEVERE);
						}
						else {
							MainSimulator.printConsole("Incorrect move option saving statistics for: " + id, Level.SEVERE);
						}
					}
					else {
						MainSimulator.printConsole("Incorrect move option in P2P user DB: " + id, Level.SEVERE);
					}
					
					// Store P2P Queue DB
					if (storeResults(p2pQueue, id)) {
						MainSimulator.printConsole("Results of queue " + i + "'s simulation " + id + " stored correctly", Level.SEVERE);
					}
					else {
						MainSimulator.printConsole("Incorrect move option in P2P queue DB: " + id, Level.SEVERE);
					}
				}
			}
		}
		
		else {
			System.out.println("NOT CORRECT");
		}
	}

	/**
	 * Moves db file to "neglectedEvaluations" folder with new name (db name + id)
	 * 
	 * @param db	DB file to rename
	 * @param id	id number
	 * @return		T/F
	 */
	private boolean storeResults(File db, int id) {
		
		String newName = db.getName().replace(".db", "_" + id + ".db");
		
		File newDest = new File(neglectedEvaluationsFolder.getPath() + File.separator + newName);
		
		/*
		System.out.println(db);
		System.out.println(newDest);
		*/
		
		return db.renameTo(newDest);
	}
	
	/**
	 * Moves stats file (CSV) to "neglectedEvaluations" folder with new name (csv name + id)
	 * 
	 * @param file	CSV file to rename
	 * @param id	id number
	 * @return		T/F
	 */
	private boolean storeStats(int id) {
		
		boolean stored = false;
		
		File file = null;
		
		if (Literals.COMPILE_ITEM_STATS) {
			
			/**
			 * RATINGS PREDICTED
			 */
			file = new File(Literals.CSV_RATINGS);
			
			String newName = file.getName().replace(".csv", "_" + id + ".csv");
			File newDest = new File(neglectedEvaluationsFolder.getPath() + File.separator + newName);
			
			stored = file.renameTo(newDest);
			
			/**
			 * USERS WATCHING SAME ITEM
			 */
			if (stored) {
				file = new File(Literals.CSV_USERS_SAME_ITEM);
				
				newName = file.getName().replace(".csv", "_" + id + ".csv");
				newDest = new File(neglectedEvaluationsFolder.getPath() + File.separator + newName);
				
				stored = file.renameTo(newDest);
			}
		}
		
		if (Literals.COMPILE_DISTANCES_STATS && !(Literals.COMPILE_ITEM_STATS && !stored)) {
			/**
			 * RATINGS PREDICTED
			 */
			file = new File(Literals.CSV_USERS_DISTANCES_STATS);
			
			String newName = file.getName().replace(".csv", "_" + id + ".csv");
			File newDest = new File(neglectedEvaluationsFolder.getPath() + File.separator + newName);
			
			stored = file.renameTo(newDest);
		}
		
		return stored;
	}

	/**
	 * Rename file adding id but keeping extension.
	 * 
	 * @param file	File to rename
	 * @param id	id number
	 * @return		T/F
	 */
	/*
	private boolean renameFile(File file, int id) {

		String newName = file.getPath().replace(".db", "_" + id + ".db");
		
		File newFile = new File(newName); // If already exists, override
		
		// In case we don't want to override:
		// if (newFile.exists())
		// 		throw new IOException("File already exists");
		
		return file.renameTo(newFile);
	}
	*/
	
	/**
	 * Cut original "db" file and paste it in "neglectedEvaluations" folder.
	 * 
	 * @param file	Original file name
	 * @param id	id number
	 * @return		T/F
	 */
	/*
	private boolean cutPaste(File file, int id) {

		// Origin file
		String renamed = file.getPath().replace(".db", "_" + id + ".db");
		File originFile = new File(renamed);
		
		System.out.println(renamed);
		String[] splittedFile = renamed.split("\\");
		
		// Destination file
		String dest = neglectedEvaluationsFolder.getPath() + File.separator + originFile.getName();
		File destinationFile = new File(dest);
		
		System.out.println(dest);
		try {
			Files.copy(originFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			MainMuseumSimulator.printConsole(e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
		
		return destinationFile.exists();
	}
	*/
}
