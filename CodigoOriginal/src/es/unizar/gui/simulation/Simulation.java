package es.unizar.gui.simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import com.opencsv.CSVWriter;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.dao.DAOFactory;
import es.unizar.dao.DataAccessLayer;
import es.unizar.dao.DataManagementQueueDB;
import es.unizar.dao.DataManagementUserDB;
import es.unizar.dao.SQLiteDataManagementQueueDB;
import es.unizar.dao.SQLiteDataManagementUserDB;
import es.unizar.database.DBDataModel;
import es.unizar.database.Database;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.UserInfo;
import es.unizar.gui.graph.GraphForSpecialUser;
import es.unizar.recommendation.ExhaustiveRecommendation;
import es.unizar.recommendation.IdealRecommendation;
import es.unizar.recommendation.RandomRecommendation;
import es.unizar.recommendation.contextaware.trajectory.ShortestTrajectoryStrategy;
import es.unizar.recommendation.contextaware.trajectory.TrajectoryPostfilteringBasedRecommendation;
import es.unizar.recommendation.path.NearestPath;
import es.unizar.recommendation.path.Path;
import es.unizar.util.DebugFilter;
import es.unizar.util.DebugFormatter;
import es.unizar.util.Distance;
import es.unizar.util.DistancesBetweenUsersAndTime;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;
import es.unizar.util.Pair;
import es.unizar.util.PredictedRatingsInfo;
import es.unizar.util.Seed;
import es.unizar.recommendation.path.RandomPath;

/**
 * Configuration parameters of the simulation.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class Simulation {

	// Simulation
	private int timeAvailableUser; // =1;
	private int delayObservingPainting; // =30;
	// Cu�nto tiempo representan en la realidad -> Velocidad de la simulaci�n
	private double timeForIteration; // =1; *In order to go faster: =30 seconds or =2 seconds ("screenRefreshTime"=0.5 seconds)
	// Cada cu�ntos segundos se itera -> Duraci�n de la iteraci�n en simulaci�n
	private double screenRefreshTime; // =1; *Would change if "timeForIteration" is modified
	private double timeForThePaths; // =1;
	private double userVelocity; // =3;
	private double kmToPixel; // =6597;
	private int ttl; // Propagation of items: =4000; // Options: 180; // 900; //1800; //2700; seconds
	private int timeOnStairs; // =60;
	private int minimumTimeToUpdateRecommendation; // =30;
	private int communicationRange; // =250
	private int maxKnowledgeBaseSize; // =1;
	private int communicationBandwidth; // =54;
	private int latencyOfTransmission; // =1;
	private int timeToChangeMood; // =1800; // 1800 = 30 minutes
	private Seed simulationSeed; // if specified in config, that seed value; if not specified, System.currentTimeMillis()
	
	public boolean registerSimInDB = false;

	// Experiment
	private int numberOfSpecialUser; // =1;
	private int numberOfNonSpecialUser; // =175;
	private String nonSpecialUserPaths; // ="non_special_user_paths.txt";
	private String pathStrategy; // ="NOIP";
	private String recommendationAlgorithm; // "UBCF"; Options:"SVD";"Random";"Exhaustive";"Near POI";"Know-It-All";"K-Ideal";
	private float thresholdRecommendation; // =(float) 2.5;
	private double thresholdSimilarity; // =0.5;
	private int howMany; // =10;
	private String networkType; // ="Peer To Peer (P2P)";
	private String propagationStrategy; // ="Opportunistic";
	private double probabilityUserDisobedience; // =0.4;
	private int numberVoteReceived; // =40;

	// =========== Auxiliary parameters================================:
	// Number total of users.
	private int numberOfUser;
	private int numberOfITems;
	// Users:
	public ArrayList<User> userList;
	public List<User> ended;

	/**
	 * Database instances
	 */
	// Data management user and queue databases:
	public LinkedList<Database> dataInstanceUserDBList_P2P;
	public LinkedList<Database> dataInstanceQueueDBList_P2P;
	public Database dataInstanceUserDB_Centralized;
	// Data access in the database of all users.
	public Database dataInstanceMuseumDB;
	/**
	 * DataAccesses
	 */
	// Data management user and queue databases:
	public LinkedList<DataManagementUserDB> dataManagementUserDBList_P2P;
	public LinkedList<DataManagementQueueDB> dataManagementQueueDBList_P2P;
	public DataManagementUserDB dataManagementUserDB_Centralized;
	// Data access in the database of all users.
	public DBDataModel dataModelMuseumDB;

	// Build a graph for the RS user.
	public GraphForSpecialUser graphSpecialUser;
	// Object to access to data from graph file (GRAPH_FLOOR_COMBINED):
	public DataAccessGraphFile dataAccessGraphFile;
	// CSV writter for storing ratings predicted in P2P networks
	public CSVWriter csvWriter;
	// PATH -> For checking door-stairs connected
	public Path pathStrategyUsed;

	// Random.
	public Random random;
	// Mood array: 10--> happy, 11--> neutral, 12--> sad.
	int[] moodValues = { 10, 11, 12 };
	// Period of time.
	public long elapsedTime = 0;
	public long startTime = 0;
	public long stopTime = 0;

	// Array that save the current user position o his/her path.
	public int[] userPositionInPath;
	// Items that are being watched by users.
	public long[] itemsBeingWatched;
	// Save the next location in an iteration.
	public String[] locationNextIteration;
	public List<Long> itemUpdated;
	public boolean[] voting;
	public boolean isChangedItemByRecommender;
	public List<String> oldPathUserSpecial = new LinkedList<>();
	// Count when the users finish.
	public int finish = 0;
	public int numberItemsPropagated;
	public Map<Long, Integer> numberOfReceivedItems;
	public static Map<Integer, List<Long>> itemRatedOfUsers;
	public List<String> path;
	public String locationStartVertex;
	public HashMap<Long, Integer> countItemsTTPByUser;

	public double[] availableTimeOfUsers;
	public int[] currentTimeOfUsers;
	public int[] moodOfUsers;
	
	/**
	 * Statistics.
	 */
	public Map<Long, PredictedRatingsInfo> predictedRatings;
	public Set<Long> idUsersWatchingSameItem;
	public List<DistancesBetweenUsersAndTime> distancesBetweenUsers;
	public List<DistancesBetweenUsersAndTime> completedDistancesBetweenUsers;
	
	// =========== Logger ================================:
	public static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	public static final Logger logRecommender = Logger.getLogger("RECOMMENDER");

	public Simulation(int timeAvailableUser, int delayObservingPainting, double timeForIteration, double screenRefreshTime, double timeForThePaths, double userVelocity, double kmToPixel, int ttl,
			int timeOnStairs, int minimumTimeToUpdateRecommendation, int communicationRange, int maxKnowledgeBaseSize, int communicationBandwidth, int latencyOfTransmission, int numberOfSpecialUser,
			int numberOfNonSpecialUser, String nonSpecialUserPaths, String pathStrategy, String recommendationAlgorithm, float thresholdRecommendation, int howMany, String propagationStrategy,
			double probabilityUserDisobedience, int numberVoteReceived, double thresholdSimilarity, String networkType, int timeToChangeMood, boolean useFixedSeed, long seed) {
		
		MainSimulator.printConsole("Creating simulation", Level.WARNING);
		currentTime();
		
		// Logger configuration 
		logRecommender.setUseParentHandlers(false);
		logRecommender.setLevel(Literals.DEBUG_DEFAULT_LEVEL);
		
		DebugFormatter df = new DebugFormatter();
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(df);
		// If not, log messages under INFO level aren't printed in console
		ch.setLevel(Literals.DEBUG_DEFAULT_LEVEL);
		logRecommender.addHandler(ch);
		
		// Just set filter if the level specified is lower than severe
		// SEVERE = Skip printing only times (adds delay for every message check)
		if (!Literals.DEBUG_DEFAULT_LEVEL.equals(Level.SEVERE))
			logRecommender.setFilter(new DebugFilter());
		
		// Simulation:
		this.timeAvailableUser = timeAvailableUser;
		this.delayObservingPainting = delayObservingPainting;
		this.timeForIteration = timeForIteration;
		this.screenRefreshTime = screenRefreshTime;
		this.timeForThePaths = timeForThePaths;
		this.userVelocity = userVelocity;
		this.kmToPixel = kmToPixel;
		this.ttl = ttl;
		this.timeOnStairs = timeOnStairs;
		this.minimumTimeToUpdateRecommendation = minimumTimeToUpdateRecommendation;
		this.communicationRange = communicationRange;
		this.maxKnowledgeBaseSize = maxKnowledgeBaseSize;
		this.communicationBandwidth = communicationBandwidth;
		this.latencyOfTransmission = latencyOfTransmission;
		this.timeToChangeMood = timeToChangeMood;
		if (useFixedSeed) {
			this.simulationSeed = new Seed(seed);
		} else {
			this.simulationSeed = new Seed();
		}
		
		MainSimulator.printConsole("Using seed: " + getSeed(), Level.WARNING);
		//log.log(Level.SEVERE, "Using seed: " + getSeed());
		
		random = new Random(getSeed());

		// Experiment:
		this.numberOfSpecialUser = numberOfSpecialUser;
		this.numberOfNonSpecialUser = numberOfNonSpecialUser;
		this.nonSpecialUserPaths = nonSpecialUserPaths;
		this.pathStrategy = pathStrategy;
		this.recommendationAlgorithm = recommendationAlgorithm;
		this.thresholdRecommendation = thresholdRecommendation;
		this.thresholdSimilarity = thresholdSimilarity;
		this.howMany = howMany;
		this.propagationStrategy = propagationStrategy;
		this.networkType = networkType;
		this.probabilityUserDisobedience = probabilityUserDisobedience;
		this.numberVoteReceived = numberVoteReceived;

		// Auxiliary parameters:
		this.numberOfUser = numberOfSpecialUser + numberOfNonSpecialUser;
		this.userList = new ArrayList<User>(numberOfUser);
		this.ended = new ArrayList<User>();
		// Non-RS users:
		boolean isSpecialUser = false;
		for (int i = 0; i < numberOfNonSpecialUser; i++) {
			int userID = i + 1;
			
			try {
				User user = new User(userID, isSpecialUser);
				this.userList.add(user);
			} catch (IOException e) {
				log.log(Level.SEVERE, e.toString());
				log.log(Level.SEVERE, "Special/non-RS user image files not correct");
			}
			
		}
		// RS users:
		isSpecialUser = true;
		int specialUser = numberOfNonSpecialUser;
		for (int i = numberOfNonSpecialUser; i < numberOfUser; i++) {
			specialUser += 1;
			
			try {
				User user = new User(specialUser, isSpecialUser);
				this.userList.add(user);
			} catch (IOException e) {
				log.log(Level.SEVERE, e.toString());
				log.log(Level.SEVERE, "Special/non-RS user image files not correct");
			}
		}
		
		// User available time for iteration in seconds.
		this.availableTimeOfUsers = new double[this.numberOfUser];
		Arrays.fill(availableTimeOfUsers, 0.0); // Initially the availableTime for all users is 0.
		
		// User current time in the simulation.
		this.currentTimeOfUsers = new int[this.numberOfUser];
		Arrays.fill(currentTimeOfUsers, 0); // Initially the currentTime for all users is 0.
		
		// Initialize the mood of users.
		this.moodOfUsers = new int[this.numberOfUser];
		initializeMoodOfUsers();

		// Array that save the current user position o his/her path.
		this.userPositionInPath = new int[this.numberOfUser];
		// Items that are being watched by users.
		this.itemsBeingWatched = new long[this.numberOfUser];
		// Save the next location in an iteration.
		this.locationNextIteration = new String[this.numberOfUser];
		this.voting = new boolean[this.numberOfUser];
		this.isChangedItemByRecommender = false;
		this.itemUpdated = new LinkedList<>();
		this.numberItemsPropagated = 0;
		this.numberOfReceivedItems = new HashMap<Long, Integer>();
		Simulation.itemRatedOfUsers = new HashMap<Integer, List<Long>>();
		this.path = new LinkedList<>();
		this.locationStartVertex = null;

		this.countItemsTTPByUser = new HashMap<>();
		for (int userPosition = 1; userPosition <= userList.size(); userPosition++) {
			countItemsTTPByUser.put((long) userPosition, 1);
		}

		// Create DB managements.
		// Data management user and queue databases:
		dataManagementUserDBList_P2P = new LinkedList<>();
		dataManagementQueueDBList_P2P = new LinkedList<>();
		// Data access in the database of all users.
		try {
			dataInstanceMuseumDB = new Database();
			dataModelMuseumDB = new DBDataModel(Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH, dataInstanceMuseumDB, this.numberOfUser-1);
		} catch (SQLException ex) {
			Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Build a graph for the RS user.
		graphSpecialUser = new GraphForSpecialUser();
		// Object to access to data from graph file (GRAPH_FLOOR_COMBINED):
		dataAccessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		// Get the number of items from dataAccessItemFile (in graph RS user)
		this.numberOfITems = graphSpecialUser.accessItemFile.getNumberOfItems();
		
		
		/*
		 * Statistics
		 */
		
		// Predicted ratings
		predictedRatings = new HashMap<Long, PredictedRatingsInfo>();
		try {
			// Create CSV Writter
			FileWriter output = new FileWriter(Literals.CSV_RATINGS);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			// Write header
	        String[] header = { "id_item", "Rating", "Rating predicted", "Time (seconds)" };
	        csvWriter.writeNext(header);
	        
	        csvWriter.close();
		}
		catch (IOException ioexception) {
			MainSimulator.printConsole(ioexception.getMessage(), Level.SEVERE);
			ioexception.printStackTrace();
		}
		
		// Number users watching same item
		idUsersWatchingSameItem = new HashSet<>();
		try {
			// Create CSV Writter
			FileWriter output = new FileWriter(Literals.CSV_USERS_SAME_ITEM);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			// Write header
	        String[] header = { "id_item", "Number of users", "Rating", "Time (seconds)" };
	        csvWriter.writeNext(header);
	        
	        csvWriter.close();
		}
		catch (IOException ioexception) {
			MainSimulator.printConsole(ioexception.getMessage(), Level.SEVERE);
			ioexception.printStackTrace();
		}
		
		distancesBetweenUsers = new ArrayList<DistancesBetweenUsersAndTime>();
		completedDistancesBetweenUsers = new ArrayList<DistancesBetweenUsersAndTime>();
	}

	/**
	 * Randomly initialize the mood of each user. Mood: 10--> happy, 11--> neutral, 12--> sad.
	 */
	public void initializeMoodOfUsers() {
		int[] moodValues = { 10, 11, 12 };
		for (int userPosition = 0; userPosition < userList.size(); userPosition++) {
			int pos = random.nextInt(moodValues.length);
			moodOfUsers[userPosition] = moodValues[pos];
		}
	}

	/**
	 * Randomly changes the mood of each user. Mood: 10--> happy, 11--> neutral, 12--> sad.
	 */
	public void changeMoodOfUsers(User currentUser) {
		int[] moodValues = { 10, 11, 12 };
		int pos = random.nextInt(moodValues.length);
		moodOfUsers[currentUser.userID - 1] = moodValues[pos];
	}

	/**
	 * It initializes the initial position of users.
	 * 
	 */
	public void initializeUsers() {

		MainSimulator.printConsole("Initializing users: ", Level.WARNING);
		System.out.println("Total users: " + userList.size()); // Depuración: Verificar el número total de usuarios
		// Get the non-special and RS user paths. The non-RS user path is obtained from generated path file (e.g., nearest_non_special_user_paths.txt), by using the strategy (Nearest,
		// Random or Exhaustive) specified in the Configuration form. While the RS user path (initially null) is generated with the recommender specified in the Configuration form.
		
		graphSpecialUser.getPathsFromFile();

		String edge = null;
		for (int i = 0; i < userList.size(); i++) {
			User currentUser = userList.get(i);
			// Identify to RS users to generate their paths.
			if (currentUser.isSpecialUser) {
				System.out.println("User " + (i + 1) + " is a special user."); // Depuración: Usuario especial
				// Gets the randomly door where RS users will enter.
				long startVertex = dataAccessGraphFile.getRandomDoor();
				// The RS user path is updated with the hybrid recommendation algorithm.
				//System.out.println(currentUser.userID);
				updateSpecialUserPath(startVertex, startVertex, false, 0, false, currentUser);
			}
			path = graphSpecialUser.paths.get(i);

			// Añadido por Nacho Palacio 2025-04-24
			if (path == null || path.isEmpty() || (path.size() == 1 && path.get(0).isEmpty())) {
				// Crear una ruta simple por defecto
				path = new ArrayList<>();
				path.add("(1 : 2)");
				graphSpecialUser.paths.set(i, path);
			}

			// System.out.println("Path of user " + (i + 1) + ": " + path); // Depuración: Verificar la ruta del usuario
			MainSimulator.printConsole("Path of user " + (i + 1) + ": " + path, Level.WARNING);
			// Get the current edge.
			edge = path.get(this.userPositionInPath[i]);
			if (edge != null) {
				String[] array = cleanEdge(edge);
				// Get the vertices.
				long v1 = Long.valueOf(array[0]).longValue();
				// Gets the position user where he/she will start the simulation.
				currentUser.getRoomOfTheUser();
				// Stores the initial location of the current user.
				locationNextIteration[i] = MainSimulator.floor.diccionaryItemLocation.get(v1);
				// Initialize the user start position.
				currentUser.move(locationNextIteration[i], currentUser.room);
			}
		}
	}

	/**
	 * Generate non-RS user paths.
	 * 
	 * @param strategy The path generation strategy.
	 */
	public void generate_non_special_user_path(Path strategy) {
		// Añadido por Nacho Palacio 2025-04-22
		// Si la estrategia es RandomPath, inicializar itemsByRoom con IDs internos
		if (strategy instanceof RandomPath) {
			RandomPath randomPath = (RandomPath) strategy;
			
			try {
				// Cargar ítems por habitación con IDs internos
				Map<Integer, List<Long>> roomItems = new HashMap<>();
				
				// Para cada habitación, obtener sus ítems
				for (int i = 1; i <= dataAccessGraphFile.getNumberOfRoom(); i++) {
					List<Long> items = new LinkedList<>();
					
					// Obtener todos los ítems de la habitación actual
					for (int j = 1; j <= dataAccessGraphFile.getNumberOfItemsByRoom(i); j++) {
						// Obtenemos directamente el ID externo (sin convertir)
						long itemId = dataAccessGraphFile.getItemOfRoom(j, i);
						
						// Si el ID es válido, lo añadimos a la lista
						if (itemId > 0) {
							items.add(itemId);
						}
					}
					
					// Obtener también puertas y escaleras para esta habitación si es necesario
					for (int j = 1; j <= dataAccessGraphFile.getNumberOfDoorsByRoom(i); j++) {
						long doorId = dataAccessGraphFile.getDoorOfRoom(j, i);
						if (doorId > 0) {
							items.add(doorId);
						}
					}
					
					// Si la habitación tiene escaleras
					long stairsId = dataAccessGraphFile.getStairsOfRoom(i);
					if (stairsId > 0) {
						items.add(stairsId);
					}
					
					// Guardar la lista de ítems para esta habitación
					if (!items.isEmpty()) {
						roomItems.put(i, items);
					}
				}
				
				// Inicializar RandomPath con los ítems (mantiene IDs externos para compatibilidad)
				// Usa la función de inicialización que debes añadir a RandomPath
				randomPath.initializeItemsByRoom(roomItems);
				
			} catch (Exception e) {
				// System.out.println("Error al inicializar RandomPath: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		this.pathStrategyUsed = strategy;
		
		RandomAccessFile pw;
		try {
			// Remove the file if exist.
			File otherPath = new File(nonSpecialUserPaths);
			if (otherPath.exists()) {
				otherPath.delete();
			}
			otherPath = new File(nonSpecialUserPaths);
			pw = new RandomAccessFile(otherPath, "rw");

			// Generate a path by each non-RS user, by using specified path strategy in
			// the Configuration form.
			MainSimulator.printConsole("Generating non RS user paths:", Level.INFO);

			Random random = new Random(Configuration.simulation.getSeed());
			
			for (int i = 0; i < numberOfNonSpecialUser; i++) {
				// Choose a random number in the range of NUMBER OF items. IN MUSEUM: (1-240).
				int start_item = random.nextInt((strategy.accessItemFile.getNumberOfItems() - 1) + 1) + 1;
				if (start_item <= 0)
					System.out.println("ERROR: " + start_item);

				// Añadido por Nacho Palacio 2025-04-23
				// Convertir a ID interno para coherencia
				long internalStartItem = ElementIdMapper.convertToRangeId(start_item, ElementIdMapper.CATEGORY_ITEM);

				// Verificar que el ID interno esté en el rango correcto
				int tryCount = 0;
				while(strategy.generatePath(start_item).toString().isEmpty() && tryCount < 20) {
					tryCount++;
					start_item = random.nextInt((strategy.accessItemFile.getNumberOfItems() - 1) + 1) + 1;
					if (start_item <= 0)
						System.out.println("ERROR: " + start_item);
				}
				// MainSimulator.printConsole("User generated path: " + (i + 1) + "; " + "Starting point: " + start_item, Level.INFO);
				// pw.writeBytes(strategy.generatePath(start_item) + "\n");

				// Modificado por Nacho Palacio 2025-04-23
				MainSimulator.printConsole("User generated path: " + (i + 1) + "; " + "Starting point interno: " + internalStartItem + " (externo: " + start_item + ")", Level.INFO);
				// pw.writeBytes(strategy.generatePath(internalStartItem) + "\n");

				// Modificado por Nacho Palacio 2025-04-24
				String generatedPath = strategy.generatePath(internalStartItem);

				// System.out.println("Ruta generada: '" + generatedPath + "', longitud=" + generatedPath.length()); // Añadido por Nacho Palacio 2025-04-25
				// Convertir todos los IDs internos a externos en el path antes de guardar
				generatedPath = convertPathIdsToExternal(generatedPath);

				// System.out.println("Escribiendo ruta en archivo: '" + generatedPath + "'"); // Añadido por Nacho Palacio 2025-04-25
				pw.writeBytes(generatedPath + "\n");
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException: \n" + e);
			e.printStackTrace();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception: \n" + ex);
			ex.printStackTrace();
		}
	}

	/**
	 * The RS user path is updated with the recommendation algorithm.
	 * 
	 * @param startVertex:      The entrance door.
	 * @param endVertex:        Initially, it is equals to startVertex.
	 * @param disobedience:     If the algorithm will consider the user disobedience.
	 * @param nextItemSelected: The next selected item.
	 * @param finishPath:       If finish the RS user path.
	 * @param specialUserID:    The RS user ID.
	 */
	public void updateSpecialUserPathOld(long startVertex, long endVertex, boolean disobedience, long nextItemSelected, boolean finishPath, User currentUser) {
		long initialTimeTotal = 0, finalTimeTotal = 0, initialTimeNetwork = 0, finalTimeNetwork = 0;
		initialTimeTotal = System.currentTimeMillis();
		
		List<String> finalPath = null;
		List<RecommendedItem> recommendedItems = null;
		String recommendationType = null;
		String currentPath = null;
		TrajectoryPostfilteringBasedRecommendation postfiltering = null;
		initialTimeNetwork = System.currentTimeMillis();
		Path pathStrategy = new NearestPath();
		finalTimeNetwork = System.currentTimeMillis();
		String special_user_dbURL = null;
		Database db_special_user = null;


		if (getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
			special_user_dbURL = Literals.SQL_DRIVER + Literals.DB_CENTRALIZED_USER_PATH;
			db_special_user = dataInstanceUserDB_Centralized;
		}
		else if (getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
			special_user_dbURL = Literals.SQL_DRIVER + Literals.DB_P2P_USER_PATH + currentUser.userID + ".db";
			db_special_user = dataInstanceUserDBList_P2P.get(currentUser.userID - 1);
		}

		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, "[updateSpecialUserPath]: PRE - " + (finalTimeTotal - initialTimeTotal));
		log.log(Level.FINER, "[updateSpecialUserPath]: PRE NETWORK - " + (finalTimeNetwork - initialTimeNetwork));

		// DBDataModel and DataAccessLayer that are going to open DB connections
		// Declared before try block so that they can be disconnected from db in finally method
		DBDataModel dataModelSpecialUser = null;
		DataAccessLayer dataAccesLayerDBMuseum = null;
		
		try {
			long initialTimeTry = 0, finalTimeTry = 0;
			long initialTimeDBDataModel = 0, finalTimeDBDataModel = 0, initialTimeDataAccessLayer = 0, finalTimeDataAccessLayer = 0;
			
			initialTimeTotal = System.currentTimeMillis();
			
			initialTimeTry = System.currentTimeMillis();
			// For the database connection of the current RS user.
			initialTimeDBDataModel = System.currentTimeMillis();
			dataModelSpecialUser = new DBDataModel(special_user_dbURL, db_special_user, this.numberOfUser - 1); // Modificado por Nacho Palacio 2025-05-08 antes this.numberOfUser-1

			verifyThresholdSimilarity(dataModelSpecialUser); // Añadido por Nacho Palacio 2025-05-08

			finalTimeDBDataModel = System.currentTimeMillis();
			
			initialTimeDataAccessLayer = System.currentTimeMillis();
			dataAccesLayerDBMuseum = new DataAccessLayer(Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH, dataInstanceMuseumDB);
			finalTimeDataAccessLayer = System.currentTimeMillis();
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - DB connection: " + (finalTimeTry - initialTimeTry));	
					//+ " -> DBDataModel: " + (finalTimeDBDataModel - initialTimeDBDataModel) + 
					//"; DataAccessLayer: "+ (finalTimeDataAccessLayer - initialTimeDataAccessLayer));
			
			
			initialTimeTry = System.currentTimeMillis();
			// Build a graph for the RS user.
			graphSpecialUser.graphRecommender = graphSpecialUser.buildGraphForSpecialUser();
			ShortestTrajectoryStrategy trajectoryStrategy = new ShortestTrajectoryStrategy(graphSpecialUser.graphRecommender, MainSimulator.floor.diccionaryItemLocation);

			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Build graph: " + (finalTimeTry - initialTimeTry));

			
			initialTimeTry = System.currentTimeMillis();
			// The recommendation threshold.
			float threshold = getThresholdRecommendation();
			if (finishPath) {
				// When the path is finished.
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(dataModelSpecialUser, special_user_dbURL, trajectoryStrategy, endVertex, threshold);
			} else {
				// When the path is not finished.
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(dataModelSpecialUser, special_user_dbURL, trajectoryStrategy, startVertex, threshold);
			}
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Threshold: " + (finalTimeTry - initialTimeTry));
			
			
			initialTimeTry = System.currentTimeMillis();
			// Recommendation type
			recommendationType = getRecommendationAlgorithm();
			if (recommendationType.equalsIgnoreCase("Completely-random (FULLY-RAND)")) {
				RandomRecommendation recommender = new RandomRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				recommendedItems = recommender.recommend(currentUser.userID, getHowMany());
				//log.log(Level.WARNING, "Recommended items: " + recommendedItems.toString());
				// The path is obtained from the recommended items.
				postfiltering.recommendBaseline(recommendedItems);	// NoSuchUserException -> SOLUCIONADO (Check de l�mites en funci�n recommend
																	// IndexOutOfBoundsException -> Index: 0, Size: 0
				currentPath = postfiltering.getFinalPath();
				
				finalTimeTry = System.currentTimeMillis();
				log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Recommendation (FULLY-RAND): " + (finalTimeTry - initialTimeTry));
				log.log(Level.SEVERE, "Finished: FULLY-RAND");
				
				
			} else if (recommendationType.equalsIgnoreCase("User-Based Collaborative Filtering (UBCF)") || recommendationType.equalsIgnoreCase("Know-It-All (Know-It-All)")) {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModelSpecialUser);
				UserNeighborhood neighborhood = new ThresholdUserNeighborhood(getThresholdSimilarity(), similarity, dataModelSpecialUser);
				// For debugging UserNeighborhood
				//System.out.println(Arrays.toString(neighborhood.getUserNeighborhood(currentUser.userID)));
				
				GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(dataModelSpecialUser, neighborhood, similarity);
				postfiltering.setRecommender(recommender);
				recommendedItems = postfiltering.recommend(currentUser.userID, getHowMany()); // NoSuchUserException
				// The path is obtained from the recommended items.
				currentPath = postfiltering.getFinalPath();
				
				if (recommendationType.equalsIgnoreCase("User-Based Collaborative Filtering (UBCF)")) {
					log.log(Level.SEVERE, "Finished: UBCF");
				}
				else {
					log.log(Level.SEVERE, "Finished: Know-It-All");
				}
				
			} else if (recommendationType.equalsIgnoreCase("K-Ideal (K-Ideal)")) { // Baseline
				IdealRecommendation recommender = new IdealRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				long currentContext = getCurrentContext(currentUser);
				List<RecommendedItem> candidateItemsFromRecommender = recommender.recommend(currentUser.userID, getHowMany(), currentContext);
				recommendedItems = postfiltering.recommendIdeal(candidateItemsFromRecommender);
				// The path is obtained from the recommended items.
				currentPath = postfiltering.getFinalPath();
				
				log.log(Level.SEVERE, "Finished: K-Ideal");
			} else {
				if (recommendationType.equalsIgnoreCase("Exhaustive visit (ALL)")) {
					ExhaustiveRecommendation recommender = new ExhaustiveRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
					recommendedItems = recommender.recommend(currentUser.userID, 1);
					// The path is obtained from the recommended items.
					postfiltering.recommendBaseline(recommendedItems); // NoSuchUserException
					// The path is obtained from the recommended items.
					currentPath = postfiltering.getFinalPath();
					
					log.log(Level.SEVERE, "Finished: Exhaustive (ALL)");
				} else {
					if (recommendationType.equalsIgnoreCase("Near POI (NPOI)")) {
						// NPOI exception -> to catch block
						//System.out.println("Executing: NPOI (not exception)");
						throw new Exception("Debe ejecutarse solo NPOI");
					}
				}
			}
			
			// Añadido por Nacho Palacio 2025-05-03
			if (recommendedItems == null || recommendedItems.isEmpty()) {
                // System.out.println("ADVERTENCIA: No hay recomendaciones para el usuario " + 
                //                 currentUser.userID + ". Generando ruta por defecto.");
                
                // Generar una ruta mínima con al menos un ítem válido
                List<String> defaultPath = new LinkedList<>();
                defaultPath.add("(" + startVertex + " : " + startVertex + ")");

                finalPath = defaultPath;
                graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
                
                System.out.println("Finalizado updateSpecialUserPath con ruta por defecto para usuario " + 
                                currentUser.userID);
                return;
            }

			/* Añadido por Nacho Palacio 2025-04-16. */
			if (currentPath == null) {
				log.log(Level.WARNING, "Generated path is null for user " + currentUser.userID + ". Using default path.");
				
				if (!(pathStrategy instanceof NearestPath)) {
					pathStrategy = new NearestPath();
				}
				
				if (finishPath) {
					currentPath = pathStrategy.generatePath(endVertex);
				} else {
					currentPath = pathStrategy.generatePath(startVertex);
				}
				
				// Si aún así el camino es nulo, usar un camino vacío
				if (currentPath == null) {
					log.log(Level.SEVERE, "Failed to generate default path. Using empty path.");
					currentPath = "";
				}
			}
			List<String> pathSpecialUser = Arrays.asList(currentPath.split(", "));
			if (disobedience) {
				finalPath = combinePathsDisobedience(nextItemSelected, startVertex, endVertex, pathSpecialUser, currentUser.userID);
			} else {
				finalPath = combinePaths(startVertex, endVertex, pathSpecialUser, finishPath);
			}
			
			// Almacenar valoraciones predichas y tiempos para cada id_item
			storePredictedRatings(recommendedItems, currentUser);
			
			/*
			 * Connection reused -> Don't disconnect till the end of the simulation
			// Close DB connections
			dataModelSpecialUser.disconnect();
			dataAccesLayerDBMuseum.disconnect();
			*/
			
			finalTimeTotal = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - " + (finalTimeTotal - initialTimeTotal));
			
		} // end try
		catch (Exception ex) {
			ex.printStackTrace();
			if (recommendationType.equalsIgnoreCase("Near POI (NPOI)")) {
				log.log(Level.SEVERE, "NPOI");
				
				//ex.printStackTrace();
			}
			
			/*
			 * NPOI STRATEGY IN CASE OF EXCETION
			 */
			// Prints exception without trace
			// log.log(Level.SEVERE, ex.toString()); // + "\n" + ex.getStackTrace().toString());
			
			/*
			 * https://stackoverflow.com/questions/6822968/print-the-stack-trace-of-an-exception
			 * 
			 * For printing stacktrace
			 * 
			 * The 5 following lines
			 */
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter( writer );
			ex.printStackTrace( printWriter );
			printWriter.flush();

			String stackTrace = writer.toString();
			log.log(Level.SEVERE, stackTrace);
			
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - TIME TILL IT REACHES CATCH: " + (System.currentTimeMillis() - initialTimeTotal));
			
			initialTimeTotal = System.currentTimeMillis();
			
			long catchCurrentPath = 0, catchCurrentPathFinal = 0, catchPathSpecialUser = 0, catchPathSpecialUserFinal = 0, catchFinalPath = 0, catchFinalPathFinal = 0;
			catchCurrentPath = System.currentTimeMillis();
			
			
			// If there is not information to apply the specified recommender, then the path is generated by using the NPOI strategy.
			try {
				if (finishPath) {
					currentPath = pathStrategy.generatePath(endVertex);
				} else {
					currentPath = pathStrategy.generatePath(startVertex);
				}
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
			
			catchCurrentPathFinal = System.currentTimeMillis();
			
			catchPathSpecialUser = System.currentTimeMillis();
			List<String> pathSpecialUser = Arrays.asList(currentPath.split(", "));
			catchPathSpecialUserFinal = System.currentTimeMillis();
			
			catchFinalPath = System.currentTimeMillis();
			// If the first time, is not necessary to combine the old path with the updated path.
			if (UserRunnable.firstTime) {
				finalPath = pathSpecialUser;
				UserRunnable.firstTime = false;
			} else {
				// In order not to repeat, for example (22 : 22), which is only for the first
				// time.
				List<String> pathSpecialUserTemp = new ArrayList<String>(pathSpecialUser);
				String edge[] = cleanEdge(pathSpecialUserTemp.get(0));
				if (edge.length > 1) {
					if (edge[0] == edge[1]) {
						pathSpecialUserTemp.remove(0);
					}
				}
				// If is the second time, is necessary to combine the old path with the updated path.
				finalPath = combinePaths(startVertex, endVertex, pathSpecialUserTemp, finishPath);
				
			}
			catchFinalPathFinal = System.currentTimeMillis();
			
			log.log(Level.WARNING, "[updateSpecialUserPath / CURRENTPATH]: CATCH - " + (catchCurrentPathFinal - catchCurrentPath));
			log.log(Level.WARNING, "[updateSpecialUserPath / PATHSPECIALUSER]: CATCH - " + (catchPathSpecialUserFinal - catchPathSpecialUser));
			log.log(Level.WARNING, "[updateSpecialUserPath / FINALPATH]: CATCH - " + (catchFinalPathFinal - catchFinalPath));
			
			finalTimeTotal = System.currentTimeMillis();
			log.log(Level.FINE, "[updateSpecialUserPath]: CATCH - " + (finalTimeTotal - initialTimeTotal));
			
			
		} // End catch
		
		// Close db connections (if opened) to reduce db delays
		/*
		 * Connection reused -> Don't disconnect till the end of the simulation
		 * finally {
			
			try {
				// Close DB connections
				dataModelSpecialUser.disconnect();
				dataAccesLayerDBMuseum.disconnect();
			} catch (SQLException disconnectEX) {
				System.out.println(disconnectEX);
			} catch (Exception e) {
				System.out.println(e);
			}
		}*/
		
		initialTimeTotal = System.currentTimeMillis();
		
		/*
		 * THE PURPOSE OF THIS FUNCTION: SET RS user'S PATH
		 */
		graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
		
		// System.out.println(graphSpecialUser.paths.get(((int) currentUser.userID) - 1));

		// Print in file the paths.
		stopTime = System.currentTimeMillis();
		// Divide by 1000 to print the result in seconds.
		elapsedTime = (stopTime - startTime) / 1000;

		if (!UserRunnable.firstTime) {
			// Check if the user changed from the item he was evaluating to a new item.
			path = graphSpecialUser.paths.get((int) ((int) currentUser.userID - 1));
			String edge = path.get(userPositionInPath[(int) ((int) currentUser.userID - 1)]);
			long currentEndVertex = -1;
			if (edge.length() > 1) {
				currentEndVertex = Long.valueOf(cleanEdge(edge)[1]).longValue();
			}
			isChangedItemByRecommender = false;
			if (endVertex != currentEndVertex && endVertex <= this.numberOfITems && currentEndVertex <= this.numberOfITems && voting[(int) ((int) currentUser.userID - 1)] == true) {
				isChangedItemByRecommender = true;
				log.log(Level.SEVERE, "ITEM CHANGED BY RECOMMENDER");
			}
		}
		
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, "[updateSpecialUserPath]: POST - " + (finalTimeTotal - initialTimeTotal));
		
		// System.out.println("End recommendation");
	}

	// Añadido por Nacho Palacio 2025-05-11
	public void updateSpecialUserPath(long startVertex, long endVertex, boolean disobedience, long nextItemSelected, boolean finishPath, User currentUser) {
		
		long initialTimeTotal = 0, finalTimeTotal = 0, initialTimeNetwork = 0, finalTimeNetwork = 0;
		initialTimeTotal = System.currentTimeMillis();
		
		List<String> finalPath = null;
		List<RecommendedItem> recommendedItems = null;
		String recommendationType = null;
		String currentPath = null;
		TrajectoryPostfilteringBasedRecommendation postfiltering = null;
		initialTimeNetwork = System.currentTimeMillis();
		Path pathStrategy = new NearestPath();
		finalTimeNetwork = System.currentTimeMillis();
		String special_user_dbURL = null;
		Database db_special_user = null;

		if (getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
			special_user_dbURL = Literals.SQL_DRIVER + Literals.DB_CENTRALIZED_USER_PATH;
			db_special_user = dataInstanceUserDB_Centralized;
		}
		else if (getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
			special_user_dbURL = Literals.SQL_DRIVER + Literals.DB_P2P_USER_PATH + currentUser.userID + ".db";
			db_special_user = dataInstanceUserDBList_P2P.get(currentUser.userID - 1);
		}

		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, "[updateSpecialUserPath]: PRE - " + (finalTimeTotal - initialTimeTotal));
		log.log(Level.FINER, "[updateSpecialUserPath]: PRE NETWORK - " + (finalTimeNetwork - initialTimeNetwork));
		
		// DBDataModel and DataAccessLayer that are going to open DB connections
		// Declared before try block so that they can be disconnected from db in finally method
		DBDataModel dataModelSpecialUser = null;
		DataAccessLayer dataAccesLayerDBMuseum = null;
		
		try {
			long initialTimeTry = 0, finalTimeTry = 0;
			long initialTimeDBDataModel = 0, finalTimeDBDataModel = 0, initialTimeDataAccessLayer = 0, finalTimeDataAccessLayer = 0;
			
			initialTimeTotal = System.currentTimeMillis();
			
			initialTimeTry = System.currentTimeMillis();
			// For the database connection of the current RS user.
			initialTimeDBDataModel = System.currentTimeMillis();
			dataModelSpecialUser = new DBDataModel(special_user_dbURL, db_special_user, this.numberOfUser-1);
			finalTimeDBDataModel = System.currentTimeMillis();
			
			initialTimeDataAccessLayer = System.currentTimeMillis();
			dataAccesLayerDBMuseum = new DataAccessLayer(Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH, dataInstanceMuseumDB);
			finalTimeDataAccessLayer = System.currentTimeMillis();
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - DB connection: " + (finalTimeTry - initialTimeTry));	
					//+ " -> DBDataModel: " + (finalTimeDBDataModel - initialTimeDBDataModel) + 
					//"; DataAccessLayer: "+ (finalTimeDataAccessLayer - initialTimeDataAccessLayer));
			
			
			initialTimeTry = System.currentTimeMillis();
			// Build a graph for the RS user.
			graphSpecialUser.graphRecommender = graphSpecialUser.buildGraphForSpecialUser();
			ShortestTrajectoryStrategy trajectoryStrategy = new ShortestTrajectoryStrategy(graphSpecialUser.graphRecommender, MainSimulator.floor.diccionaryItemLocation);
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Build graph: " + (finalTimeTry - initialTimeTry));

			
			initialTimeTry = System.currentTimeMillis();
			// The recommendation threshold.
			float threshold = getThresholdRecommendation();
			if (finishPath) {
				// When the path is finished.
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(dataModelSpecialUser, special_user_dbURL, trajectoryStrategy, endVertex, threshold);
			} else {
				// When the path is not finished.
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(dataModelSpecialUser, special_user_dbURL, trajectoryStrategy, startVertex, threshold);
			}
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Threshold: " + (finalTimeTry - initialTimeTry));
			
			
			initialTimeTry = System.currentTimeMillis();
			// Recommendation type
			recommendationType = getRecommendationAlgorithm();
			if (recommendationType.equalsIgnoreCase("Completely-random (FULLY-RAND)")) {
				RandomRecommendation recommender = new RandomRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				recommendedItems = recommender.recommend(currentUser.userID, getHowMany());
				//log.log(Level.WARNING, "Recommended items: " + recommendedItems.toString());
				// The path is obtained from the recommended items.
				postfiltering.recommendBaseline(recommendedItems);	// NoSuchUserException -> SOLUCIONADO (Check de l�mites en funci�n recommend
																	// IndexOutOfBoundsException -> Index: 0, Size: 0
				currentPath = postfiltering.getFinalPath();
				
				finalTimeTry = System.currentTimeMillis();
				log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Recommendation (FULLY-RAND): " + (finalTimeTry - initialTimeTry));
				log.log(Level.SEVERE, "Finished: FULLY-RAND");
				
				
			} else if (recommendationType.equalsIgnoreCase("User-Based Collaborative Filtering (UBCF)") || recommendationType.equalsIgnoreCase("Know-It-All (Know-It-All)")) {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModelSpecialUser);
				UserNeighborhood neighborhood = new ThresholdUserNeighborhood(getThresholdSimilarity(), similarity, dataModelSpecialUser);
				// For debugging UserNeighborhood
				//System.out.println(Arrays.toString(neighborhood.getUserNeighborhood(currentUser.userID)));
				
				GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(dataModelSpecialUser, neighborhood, similarity);
				postfiltering.setRecommender(recommender);
				recommendedItems = postfiltering.recommend(currentUser.userID, getHowMany()); // NoSuchUserException
				// The path is obtained from the recommended items.
				currentPath = postfiltering.getFinalPath();
				
				if (recommendationType.equalsIgnoreCase("User-Based Collaborative Filtering (UBCF)")) {
					log.log(Level.SEVERE, "Finished: UBCF");
				}
				else {
					log.log(Level.SEVERE, "Finished: Know-It-All");
				}
				
			} else if (recommendationType.equalsIgnoreCase("K-Ideal (K-Ideal)")) { // Baseline
				IdealRecommendation recommender = new IdealRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				long currentContext = getCurrentContext(currentUser);
				List<RecommendedItem> candidateItemsFromRecommender = recommender.recommend(currentUser.userID, getHowMany(), currentContext);
				recommendedItems = postfiltering.recommendIdeal(candidateItemsFromRecommender);
				// The path is obtained from the recommended items.
				currentPath = postfiltering.getFinalPath();
				
				log.log(Level.SEVERE, "Finished: K-Ideal");
			} else {
				if (recommendationType.equalsIgnoreCase("Exhaustive visit (ALL)")) {
					ExhaustiveRecommendation recommender = new ExhaustiveRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
					recommendedItems = recommender.recommend(currentUser.userID, 1);
					// The path is obtained from the recommended items.
					postfiltering.recommendBaseline(recommendedItems); // NoSuchUserException
					// The path is obtained from the recommended items.
					currentPath = postfiltering.getFinalPath();
					
					log.log(Level.SEVERE, "Finished: Exhaustive (ALL)");
				} else {
					if (recommendationType.equalsIgnoreCase("Near POI (NPOI)")) {
						// NPOI exception -> to catch block
						//System.out.println("Executing: NPOI (not exception)");
						throw new Exception("Debe ejecutarse solo NPOI");
					}
				}
			}
			// Añadido por Nacho Palacio 2025-05-13
			if (currentPath == null) {
				try {
					if (finishPath) {
						currentPath = pathStrategy.generatePath(endVertex);
					} else {
						currentPath = pathStrategy.generatePath(startVertex);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			
			List<String> pathSpecialUser = new LinkedList<>();
			if (currentPath != null) {
				pathSpecialUser = Arrays.asList(currentPath.split(", "));
				if (disobedience) {
					finalPath = combinePathsDisobedience(nextItemSelected, startVertex, endVertex, pathSpecialUser, currentUser.userID);
				} else {
					finalPath = combinePaths(startVertex, endVertex, pathSpecialUser, finishPath);
				}
			}
			else { // Añadido por Nacho Palacio 2025-05-12
				try {
					if (finishPath) {
						currentPath = pathStrategy.generatePath(endVertex);
					} else {	
						// Modificado por Nacho Palacio 2025-05-17
						try {
							currentPath = pathStrategy.generatePath(startVertex);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			// Añadido por Nacho Palacio 2025-05-17
			if (currentPath != null) {
				List<String> pathSpecialUser1 = Arrays.asList(currentPath.split(", "));
				
				// Combinar rutas si es necesario
				if (disobedience) {
					finalPath = combinePathsDisobedience(nextItemSelected, startVertex, endVertex, pathSpecialUser1, currentUser.userID);
				} else {
					finalPath = combinePaths(startVertex, endVertex, pathSpecialUser1, finishPath);
				}
			} else {
				finalPath = new LinkedList<>(); // Ruta vacía como fallback final
			}
			
			// Asignar la ruta generada al usuario especial
			graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);


			// List<String> pathSpecialUser = Arrays.asList(currentPath.split(", "));
			// if (disobedience) {
			// 	finalPath = combinePathsDisobedience(nextItemSelected, startVertex, endVertex, pathSpecialUser, currentUser.userID);
			// } else {
			// 	finalPath = combinePaths(startVertex, endVertex, pathSpecialUser, finishPath);
			// }
			
			// Almacenar valoraciones predichas y tiempos para cada id_item
			storePredictedRatings(recommendedItems, currentUser);
			
			/*
			 * Connection reused -> Don't disconnect till the end of the simulation
			// Close DB connections
			dataModelSpecialUser.disconnect();
			dataAccesLayerDBMuseum.disconnect();
			*/
			
			finalTimeTotal = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - " + (finalTimeTotal - initialTimeTotal));
			
		} // end try
		catch (Exception ex) {
			ex.printStackTrace();
			if (recommendationType.equalsIgnoreCase("Near POI (NPOI)")) {
				log.log(Level.SEVERE, "NPOI");
				
				//ex.printStackTrace();
			}
			
			/*
			 * NPOI STRATEGY IN CASE OF EXCETION
			 */
			// Prints exception without trace
			// log.log(Level.SEVERE, ex.toString()); // + "\n" + ex.getStackTrace().toString());
			
			/*
			 * https://stackoverflow.com/questions/6822968/print-the-stack-trace-of-an-exception
			 * 
			 * For printing stacktrace
			 * 
			 * The 5 following lines
			 */
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter( writer );
			ex.printStackTrace( printWriter );
			printWriter.flush();

			String stackTrace = writer.toString();
			log.log(Level.SEVERE, stackTrace);
			
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - TIME TILL IT REACHES CATCH: " + (System.currentTimeMillis() - initialTimeTotal));
			
			initialTimeTotal = System.currentTimeMillis();
			
			long catchCurrentPath = 0, catchCurrentPathFinal = 0, catchPathSpecialUser = 0, catchPathSpecialUserFinal = 0, catchFinalPath = 0, catchFinalPathFinal = 0;
			catchCurrentPath = System.currentTimeMillis();
	
			// If there is not information to apply the specified recommender, then the path is generated by using the NPOI strategy.
			try {
				if (finishPath) {
					currentPath = pathStrategy.generatePath(endVertex);
				} else {
					currentPath = pathStrategy.generatePath(startVertex);
				}
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
			
			catchCurrentPathFinal = System.currentTimeMillis();
			
			catchPathSpecialUser = System.currentTimeMillis();


			List<String> pathSpecialUser = new LinkedList<>();
			if (currentPath != null) {
				pathSpecialUser = Arrays.asList(currentPath.split(", "));
			}

			// List<String> pathSpecialUser = Arrays.asList(currentPath.split(", "));


			catchPathSpecialUserFinal = System.currentTimeMillis();
			
			catchFinalPath = System.currentTimeMillis();
			// If the first time, is not necessary to combine the old path with the updated path.
			if (UserRunnable.firstTime) {
				finalPath = pathSpecialUser;
				UserRunnable.firstTime = false;
			} else {
				// In order not to repeat, for example (22 : 22), which is only for the first
				// time.
				List<String> pathSpecialUserTemp = new ArrayList<String>(pathSpecialUser);
				String edge[] = cleanEdge(pathSpecialUserTemp.get(0));
				if (edge.length > 1) {
					if (edge[0] == edge[1]) {
						pathSpecialUserTemp.remove(0);
					}
				}
				// If is the second time, is necessary to combine the old path with the updated path.
				finalPath = combinePaths(startVertex, endVertex, pathSpecialUserTemp, finishPath);
				
			}
			catchFinalPathFinal = System.currentTimeMillis();
			
			log.log(Level.WARNING, "[updateSpecialUserPath / CURRENTPATH]: CATCH - " + (catchCurrentPathFinal - catchCurrentPath));
			log.log(Level.WARNING, "[updateSpecialUserPath / PATHSPECIALUSER]: CATCH - " + (catchPathSpecialUserFinal - catchPathSpecialUser));
			log.log(Level.WARNING, "[updateSpecialUserPath / FINALPATH]: CATCH - " + (catchFinalPathFinal - catchFinalPath));
			
			finalTimeTotal = System.currentTimeMillis();
			log.log(Level.FINE, "[updateSpecialUserPath]: CATCH - " + (finalTimeTotal - initialTimeTotal));
			
			
		} // End catch
		
		// Close db connections (if opened) to reduce db delays
		/*
		 * Connection reused -> Don't disconnect till the end of the simulation
		 * finally {
			
			try {
				// Close DB connections
				dataModelSpecialUser.disconnect();
				dataAccesLayerDBMuseum.disconnect();
			} catch (SQLException disconnectEX) {
				System.out.println(disconnectEX);
			} catch (Exception e) {
				System.out.println(e);
			}
		}*/
		
		initialTimeTotal = System.currentTimeMillis();
		
		/*
		 * THE PURPOSE OF THIS FUNCTION: SET RS user'S PATH
		 */
		graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
		
		// System.out.println(graphSpecialUser.paths.get(((int) currentUser.userID) - 1));

		// Print in file the paths.
		stopTime = System.currentTimeMillis();
		// Divide by 1000 to print the result in seconds.
		elapsedTime = (stopTime - startTime) / 1000;

		// Modificado por Nacho Palacio 2025-05-11
		if (!UserRunnable.firstTime) {
			// Check if the user changed from the item he was evaluating to a new item.
			if (graphSpecialUser != null && graphSpecialUser.paths != null && 
				((int) currentUser.userID - 1) < graphSpecialUser.paths.size() && 
				graphSpecialUser.paths.get((int) currentUser.userID - 1) != null) {
				
				path = graphSpecialUser.paths.get((int) ((int) currentUser.userID - 1));
				
				if (path != null && !path.isEmpty() && 
					userPositionInPath != null && 
					userPositionInPath[(int) ((int) currentUser.userID - 1)] < path.size()) {
					
					String edge = path.get(userPositionInPath[(int) ((int) currentUser.userID - 1)]);
					long currentEndVertex = -1;
					
					if (edge != null && edge.length() > 1) {
						String[] cleanedEdge = cleanEdge(edge);
						if (cleanedEdge != null && cleanedEdge.length > 1) {
							currentEndVertex = Long.valueOf(cleanedEdge[1]).longValue();
						}
					}
					
					isChangedItemByRecommender = false;
					if (endVertex != currentEndVertex && 
						endVertex <= this.numberOfITems && 
						currentEndVertex <= this.numberOfITems && 
						voting != null &&
						voting[(int) ((int) currentUser.userID - 1)] == true) {
						
						isChangedItemByRecommender = true;
						log.log(Level.SEVERE, "ITEM CHANGED BY RECOMMENDER");
					}
				}
			}
		}
		
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, "[updateSpecialUserPath]: POST - " + (finalTimeTotal - initialTimeTotal));
		
		// System.out.println("End recommendation");
	}

	/**
	 * Updates the position of users.
	 */
	public synchronized void updateUsers(Map<Integer,UserInfo.UserState> stateOfUsers,Map<Pair<Integer,Integer>,Double> timeUsersInRooms) {
		long initialTimeTotal = 0, finalTimeTotal = 0;
		initialTimeTotal = System.currentTimeMillis();
		MainSimulator.printConsole("Updating user positions: ", Level.INFO);
		log.log(Level.INFO, "Updating user positions: ");
		long initialTime = 0, finalTime = 0;
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, " -TIEMPO TOTAL MENSAJE INICIO: " + (finalTimeTotal - initialTimeTotal));

		initialTimeTotal = System.currentTimeMillis();
		int countFinishedSpecialUsers = 0;
		
		// Create a new array equivalent to the user list in order to operate
		//List<User> userList = new ArrayList<User>(Arrays.asList(Configuration.simulation.userList)); // java.util.ConcurrentModificationException
		//User[] userList = Configuration.simulation.userList.clone();
		
		// Loop for each user still left (that hasn't finished)
		for (User u : userList) {
			//System.out.println("Usuario: " + u.userID);
			
			//
//			Long moveTime = System.currentTimeMillis();
			//
			
			// New userPosition -> Equivalent to previous loop's variable
			int userPosition = u.userID - 1;
			
			initialTime = System.currentTimeMillis();
			
			User currentUser = Configuration.simulation.userList.get(userPosition);
			availableTimeOfUsers[userPosition] += Configuration.simulation.getTimeForIterationInSecond();
			
			MainSimulator.printConsole("User: " + currentUser.userID, Level.INFO);
			MainSimulator.printConsole("Available time for iteration in seconds: " + availableTimeOfUsers[userPosition], Level.INFO);
			
			finalTime = System.currentTimeMillis();
			log.log(Level.FINE, "   Usuario: " + currentUser.userID + " iterando");
			log.log(Level.INFO, "   Tiempo en repintar: " + (finalTime - initialTime));
			
			if (currentUser.hasFinishedVisit) {
				log.log(Level.FINEST, "Skipping user's " + currentUser.userID + " iteration"); // -> Working
				continue;
			}
			
			long lastV2 = -1;
			double visitDuration = 0;
			
			// The user will be moving while he has time available.
			while ((availableTimeOfUsers[userPosition] > 0) && (currentTimeOfUsers[userPosition] < getTimeAvailableUserInSecond())) {
				int previousRoomOfUser = currentUser.room;
				currentUser.getRoomOfTheUser();
//				int roomOfUser = currentUser.room;
//				if(roomOfUser > -1) {
//					UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
////					System.out.println(roomOfUser+" "+MainSimulator.floor.roomLabels.size());
//					if(ui == null) {
//						ui = new UserInfo.UserState(MainSimulator.floor.roomLabels.get(roomOfUser));
//						stateOfUsers.put(currentUser.userID,ui);
//					}else {
//						ui.room = MainSimulator.floor.roomLabels.get(roomOfUser);
//					}
//				}
				
				log.log(Level.FINEST, "   TENGO TIEMPO TODAV�A: " + availableTimeOfUsers[userPosition]);
				// The current path.
				path = graphSpecialUser.paths.get(userPosition);
				
				//boolean emptyLast = ((path.size() - 1) == userPositionInPath[userPosition]) && ((path.get(path.size()-1) != "")  || (path.get(path.size()-1) != null));
				// CHECK && !emptyLast IN IF
				
				// Añadido por Nacho Palacio 2025-05-11
				if (path == null) {
					log.log(Level.WARNING, "Path nulo encontrado para usuario " + currentUser.userID + ", saltando iteración.");
					continue;
				}
				
				// If the path has not finished.
				if ((path.size() - 1) >= userPositionInPath[userPosition]) {
					
					log.log(Level.FINEST, "   Path NO acabado");
					
					// The current edge.
					String edge = path.get(userPositionInPath[userPosition]);
					String[] array = cleanEdge(edge);
					// The vertices. (V1 = CURRENT ITEM; V2 = NEXT ITEM TO VISIT)
					long v1 = 1;
					long v2 = 1;
					
					try {
						v1 = Long.valueOf(array[0]).longValue();
						v2 = Long.valueOf(array[1]).longValue();
					}
					catch (Exception e) {
						//e.printStackTrace();
					}
					
					MainSimulator.printConsole("It is moved from item: " + v1 + " to " + v2, Level.INFO);
					itemsBeingWatched[userPosition] = v2;

					// Next idea: When the RS user sees the last available item to visit in the current room (the item has been voted and in the rest of the recommended path there is no other
					// item to visit in that room), the path is updated by using the recommendation algorithm.
					initialTime = System.currentTimeMillis();
					
					if (currentUser.isSpecialUser) {
						
						// If it is an item and it is already been voted on. The path is not updated again if it has already been done (because v2 was a door).
						if (v1 <= this.numberOfITems && v2 > this.numberOfITems) {
							// If v2>240 is because the RS user goes to a door in order to leave the room.
							if (!itemUpdated.contains(v1)) {
								// The path is updated with the recommendation algorithm to see if it suggests another item within the same room.
								long startVertex = v1;
								long endVertex = v2;
								// The RS user path is updated with the recommendation algorithm.
								long initialTimeSpecial = 0, finalTimeSpecial = 0;
								
								initialTimeSpecial = System.currentTimeMillis();
								updateSpecialUserPath(startVertex, endVertex, false, 0, false, currentUser);
								finalTimeSpecial = System.currentTimeMillis();
								
								
								if (itemUpdated.isEmpty()) {
									itemUpdated.add(v1);
								} else {
									if (!itemUpdated.contains(v1)) {
										itemUpdated.add(v1);
									}
								}

								// The current edge.
								path = graphSpecialUser.paths.get(userPosition);
								edge = path.get(userPositionInPath[userPosition]);
								array = cleanEdge(edge);
								// The vertices.
								v1 = Long.valueOf(array[0]).longValue();
								v2 = Long.valueOf(array[1]).longValue();
								itemsBeingWatched[userPosition] = v2;
								
								// Check users watching same item as RS user
								checkUsersWatchingSameItem(itemsBeingWatched[userPosition]);
								
								finalTime = System.currentTimeMillis();
								log.log(Level.FINE, "      *** Tiempo en updateSpecialUserPath: " + (finalTimeSpecial - initialTimeSpecial));
								log.log(Level.INFO, "      Tiempo en actualizar ruta (usuario especial tiene m�s cosas que ver en la sala): " + (finalTime - initialTime));
							}
						}
					}

					// Initial point.
					String location_v1 = MainSimulator.floor.diccionaryItemLocation.get(v1);
					// Final point.

					String location_v2 = MainSimulator.floor.diccionaryItemLocation.get(v2);

					// If it is an item (not a door).
					if (v2 <= this.numberOfITems) {
						// Generate rating. If the user has time and was rating an item, then he or she is done rating.
						if ((availableTimeOfUsers[userPosition] >= 0) && (voting[userPosition] == true)) {
							
							log.log(Level.FINEST, "      ITEM " + v2 + " SIENDO VOTADO");
							long id_user = currentUser.userID;
							long user = id_user;
							long item = itemsBeingWatched[userPosition];
							
							initialTime = System.currentTimeMillis();
							long context = getCurrentContext(currentUser);
							finalTime = System.currentTimeMillis();
							log.log(Level.INFO, "      Tiempo en obtener el contexto: " + (finalTime - initialTime));
							
							initialTime = System.currentTimeMillis();
							float rating = generateRating(user, item, context);
							finalTime = System.currentTimeMillis();
							log.log(Level.INFO, "      Tiempo en generar valoraci�n: " + (finalTime - initialTime));
							
							String location = currentUser.x + ", " + currentUser.y;
							// The user-generated rating is stored in the sending queue (queue.db) to be propagated.
							InformationToPropagate informationToPropagate = new InformationToPropagate(id_user, user, item, context, rating, Configuration.simulation.getTtl(), location,
									currentTimeOfUsers[currentUser.userID - 1]);
							
//							UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
//							ui.action = "Observing item";
//							ui.item = v2;

							if (!isChangedItemByRecommender) {
								initialTime = System.currentTimeMillis();
								
								long initialTimeRecommender = 0, finalTimeRecommender = 0;
								initialTimeRecommender = System.currentTimeMillis();
								// The generated rating is inserted into the database (db_user.db) of the RS user.
								specialUserListenTheInformation(informationToPropagate, currentUser); // Lot of time consumed
								finalTimeRecommender = System.currentTimeMillis();
								
								// Update item stats
								if (currentUser.isSpecialUser && Literals.COMPILE_ITEM_STATS) {
									//System.out.println("RS user voted: " + itemsBeingWatched[userPosition]);
									updateSpecialUserItemStatistics(informationToPropagate);
								}
								
								logRecommender.log(Level.INFO, "[specialUserListenTheInformation]: " + (finalTimeRecommender - initialTimeRecommender));
								
								initialTimeRecommender = System.currentTimeMillis();
								// Increased time: for the RS user to arrive at the item and observe it.
								currentTimeOfUsers[currentUser.userID - 1] += getCurrentTime(location_v1, location_v2) + Configuration.simulation.getDelayObservingPaintingInSecond();
								if (itemRatedOfUsers.containsKey(currentUser.userID)) {
									logRecommender.log(Level.INFO, "    ENTRO IF");
									List<Long> itemList = itemRatedOfUsers.get(currentUser.userID);
									itemList.add(item);
									itemRatedOfUsers.put(currentUser.userID, itemList);
								} else {
									logRecommender.log(Level.INFO, "    ENTRO ELSE");
									List<Long> itemList = new LinkedList<>();
									itemList.add(item);
									itemRatedOfUsers.put(currentUser.userID, itemList);
								}
								finalTimeRecommender = System.currentTimeMillis();
								
								logRecommender.log(Level.INFO, "[currentTimeOfUsers]: " + (finalTimeRecommender - initialTimeRecommender));

								initialTimeRecommender = System.currentTimeMillis();
								// The information to be propagated is inserted in a db_p2p_queue_user_XXX.db.
								if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
									logRecommender.log(Level.WARNING, "    Entro IF P2P");
									dataManagementQueueDBList_P2P.get(userPosition).insertInformation(informationToPropagate);
								}
								finalTimeRecommender = System.currentTimeMillis();
								
								logRecommender.log(Level.INFO, "[currentTimeOfUsers]: " + (finalTimeRecommender - initialTimeRecommender));
								
								finalTime = System.currentTimeMillis();
								log.log(Level.INFO, "      IS CHANGED BY RECOMMENDER: " + (finalTime - initialTime));

							} else {
								// If the item to rate by user has been changed by the recommender (after the user is in the item), then the user should not rate on it. This would avoid rating the
								// wrong item.
								voting[userPosition] = false;
								isChangedItemByRecommender = false;
							}

							voting[userPosition] = false;
						}
					} else {
						// The value is changed to false because the item that was going to be rated is pending for the next iteration, but when this method finishes the recommender
						// is updated, which changes the item (by another item) that was going to be rated, but as voting[i] = true; and casually the next item is a door,
						// so this item will not be rated and the user will not walk. Hence, it is set to false to ensure that you can enter the next if.
						voting[userPosition] = false;
					}

					/**
					 * MOVEMENT
					 */
					// If the user is not rating, it is because he is still moving.
					if (voting[userPosition] == false) {
						
						initialTime = System.currentTimeMillis();
						long initialTimeMovement = System.currentTimeMillis();
						long finalTimeMovement;
						
						// If the current location is v2, it is because the user arrived at the destination item.
						if (locationNextIteration[userPosition].equalsIgnoreCase(location_v2)) {
							UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
							if(v2 <= this.numberOfITems && ui != null) {
								ui.action = "Observing item";
								ui.item = v2;
							}
							
							// Moving to other item.
							userPositionInPath[userPosition] += 1;
							// If is a RS user.
							if (currentUser.isSpecialUser) {
								// The path traveled is stored.
								oldPathUserSpecial.add("(" + v1 + " : " + v2 + ")");
								// Before the user goes to another item, consider the problem of user disobedience.
								specialUserDisobedience(currentUser);
							}
							// The time to go from one door (or stairs) to another is increased.
							if (v2 > this.numberOfITems) {
								/*if (((v1 == 241 || v1 == 242 || v1 == 275 || v1 == 276) && (v2 == 279 || v2 == 280 || v2 == 308 || v2 == 309))
										|| ((v1 == 279 || v1 == 280 || v1 == 308 || v1 == 309) && (v2 == 241 || v2 == 242 || v2 == 275 || v2 == 276))) {
									currentTimeOfUsers[userPosition] += getTimeOnStairs();
								}*/
								if ((this.pathStrategyUsed != null && this.pathStrategyUsed.checkDoorsConnectedByStairs(v1, v2)) || checkDoorsConnectedByStairs(v1, v2)) {
									currentTimeOfUsers[userPosition] += getTimeOnStairs();
								} else {
									currentTimeOfUsers[userPosition] += getCurrentTime(location_v1, location_v2);
								}
							}
							
							finalTimeMovement = System.currentTimeMillis();
							log.log(Level.INFO, "      - TIME MOVING ARRIVED DESTINATION ITEM: " + (finalTimeMovement - initialTimeMovement));
//							moveTime = finalTimeMovement - initialTimeMovement;
						} else {
							initialTimeMovement = System.currentTimeMillis();
							// If it is a door of stairs, then the next movement of the user will be directly to the door input of stairs.
							boolean connectedStairs = false;
							try {
								connectedStairs = (this.pathStrategyUsed == null) ? checkDoorsConnectedByStairs(v1, v2) : this.pathStrategyUsed.checkDoorsConnectedByStairs(v1, v2);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							
							if (connectedStairs) {
								locationStartVertex = locationNextIteration[userPosition];
								locationNextIteration[userPosition] = location_v2;
							} else {
								// If the user has not arrived at v2, and it is not a door of stairs, then the user will move to the next position in the direction of v2.
								locationStartVertex = locationNextIteration[userPosition];
								long timeNextMovementInit = System.currentTimeMillis();
								locationNextIteration[userPosition] = nextMovement(locationNextIteration[userPosition], location_v2, currentUser, (int) v2);
								long timeNextMovementEnd = System.currentTimeMillis();
								log.log(Level.FINE, "      - *** NEXT MOVEMENT: " + (timeNextMovementEnd - timeNextMovementInit));
							}
							
							finalTimeMovement = System.currentTimeMillis();
							log.log(Level.INFO, "      - CHANGING LOCATIONS: " + (finalTimeMovement - initialTimeMovement));
							
							initialTimeMovement = System.currentTimeMillis();
							int room = getRoom(v2);
							finalTimeMovement = System.currentTimeMillis();
							log.log(Level.FINE, "      - GET ROOM: " + (finalTimeMovement - initialTimeMovement));
							
							initialTimeMovement = System.currentTimeMillis();
							currentUser.move(locationNextIteration[userPosition], room);
							finalTimeMovement = System.currentTimeMillis();
							
							currentUser.getRoomOfTheUser();
							int roomOfUser = currentUser.room;
							if(roomOfUser > -1) {
								UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
//								System.out.println(roomOfUser+" "+MainSimulator.floor.roomLabels.size());
								if(ui == null) {
									ui = new UserInfo.UserState(MainSimulator.floor.roomLabels.get(roomOfUser));
									stateOfUsers.put(currentUser.userID,ui);
								}else {
									ui.room = MainSimulator.floor.roomLabels.get(roomOfUser);
								}
								
//								UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
								ui.action = "Moving";
//								ui.item = null;
								Pair<Integer,Integer> user_room = new Pair<Integer,Integer>(currentUser.userID,MainSimulator.floor.roomLabels.get(roomOfUser));
								Double pastTime = timeUsersInRooms.get(user_room);
								timeUsersInRooms.put(user_room,pastTime == null ? timeForIteration : pastTime + timeForIteration);
								if(MainSimulator.db.isConnected() && registerSimInDB) {
									if(previousRoomOfUser != roomOfUser) {
										MainSimulator.db.registerVisit(MainSimulator.floor.roomLabels.get(roomOfUser), currentUser.userID, currentUser.isSpecialUser);
										visitDuration = 0;
									}
									MainSimulator.db.addPositionToPath(currentUser.userID,locationNextIteration[userPosition],MainSimulator.floor.roomLabels.get(roomOfUser));
									visitDuration += timeForIteration;
									if(v2 != lastV2) {
										MainSimulator.db.registerItemObservation(room, currentUser.userID, (int)v2);
										MainSimulator.db.registerVisitDuration(MainSimulator.floor.roomLabels.get(roomOfUser), currentUser.userID, visitDuration);
									}
								}
							}
							
							log.log(Level.FINE, "      - FUNCI�N MOVE: " + (finalTimeMovement - initialTimeMovement));
						}
						
						finalTime = System.currentTimeMillis();
						log.log(Level.INFO, "    - TIME MOVING: " + (finalTime - initialTime));
//						moveTime = finalTime - initialTime;
					}
					lastV2 = v2;
				} else {
					initialTime = System.currentTimeMillis();
					log.log(Level.INFO, "   Path S� acabado");
					// If the RS user's path ends and he still has time for the visit, then the user's path is updated with the recommender.
					if (currentUser.isSpecialUser) {
						try {
							String start = cleanEdge(path.get(path.size() - 1))[0];
							String end = cleanEdge(path.get(path.size() - 1))[1];
							long startVertex = Long.valueOf(start).longValue();
							long endVertex = Long.valueOf(end).longValue();
							boolean finishPath = true;
							// The RS user path is updated with the recommendation algorithm.
							updateSpecialUserPath(startVertex, endVertex, false, 0, finishPath, currentUser);
						}
						catch (Exception e) {
							e.printStackTrace();
							currentTimeOfUsers[userPosition] = Configuration.simulation.getTimeAvailableUserInSecond();
							countFinishedSpecialUsers++;
							
							// Add the RS user to the finished users
							currentUser.hasFinishedVisit = true;
							this.ended.add(u);
						}
						path = graphSpecialUser.paths.get(userPosition);
					} else {
						finish++;
						// MainSimulator.printConsole("[The user " + currentUser.userID + " has finished his visit]", Level.WARNING);
						availableTimeOfUsers[userPosition] = 0;
						
						// Non-RS user has finished the visit
						currentUser.hasFinishedVisit = true;
						// Add the user to the "ended" (visit) list
						this.ended.add(u);
						
						// MainSimulator.printConsole("Remaining: " + (userList.size() - this.ended.size()) + " users", Level.WARNING);
					}
					
					/* Take out user from list -> He/she has finished
					if (!userList.remove(u)) {
						log.log(Level.WARNING, "ELEMENT WASN'T REMOVED FROM USER LIST");
					}
					 * ^
					 * |  NOT POSSIBLE -> CONCURRENT EXCEPTION: Cannot be removed while the 
					 * 		list is being used in iterations because then the loop gets in trouble
					*/

					// Modificado por Nacho Palacio 2025-05-10
					UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
					if (ui != null) {
						ui.action = "Finished";
						ui.item = null;
					} else {
						// Crear un nuevo objeto UserState pasando una habitación válida
						ui = new UserInfo.UserState(MainSimulator.floor.roomLabels.get(currentUser.room));
						ui.action = "Finished";
						ui.item = null;
						stateOfUsers.put(currentUser.userID, ui);
					}
					
					finalTime = System.currentTimeMillis();
					log.log(Level.INFO, "    Tiempo en actualizar PATH de " + (userPosition+1) + " (acabado): " + (finalTime - initialTime));
				}
				
				//
//				int roomOfUser = MainSimulator.floor.getRoomFromPosition((int)u.x,(int)u.y);
//				if(roomOfUser > -1) {
//					stateOfUsers.put(u.userID, MainSimulator.floor.roomLabels.get(roomOfUser));
//					Pair<Integer,Integer> user_room = new Pair<Integer,Integer>(u.userID,MainSimulator.floor.roomLabels.get(roomOfUser));
//					Long pastTime = timeUsersInRooms.get(user_room);
//					timeUsersInRooms.put(user_room,pastTime == null ? moveTime : pastTime + moveTime);
//				}
				//
				
			}

			initialTime = System.currentTimeMillis();
			
			// If RS users consumed the time of the visit, then the visit will be terminated for all users.
			MainSimulator.printConsole("Current time: " + currentTimeOfUsers[userPosition] + "/ " + Configuration.simulation.getTimeAvailableUserInSecond(), Level.INFO);
			if (currentTimeOfUsers[userPosition] >= Configuration.simulation.getTimeAvailableUserInSecond() && currentUser.isSpecialUser) {
				countFinishedSpecialUsers++;
				
				// Add the RS user to the finished users
				currentUser.hasFinishedVisit = true;
				this.ended.add(u);
			}
			
			finalTime = System.currentTimeMillis();
			log.log(Level.FINE, "   Tiempo en imprimir tiempos en consola: " + (finalTime - initialTime));
		}
		
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.INFO, " -TIEMPO TOTAL BUCLE: " + (finalTimeTotal - initialTimeTotal));
		
		initialTimeTotal = System.currentTimeMillis();
		// The criterion for stopping the simulation is that all users have finished their time.
		if (countFinishedSpecialUsers >= getNumberOfSpecialUser()) {
			// The thread is killed because the visit is over.
			MainSimulator.userRunnable.setRunning(false);
			MainSimulator.printConsole("[Finished visits]", Level.WARNING);
			currentTime();
			
			disconnect();
			
		}
		
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.INFO, " -TIEMPO TOTAL COMPROBACION ULTIMO IF: " + (finalTimeTotal - initialTimeTotal));
		
	}

	public void disconnect() {
		// Commit info in databases.
		// CENTRALIZED
		if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
			SQLiteDataManagementUserDB sqlite = (SQLiteDataManagementUserDB) dataManagementUserDB_Centralized;
			
			try {
				sqlite.commit();
				MainSimulator.printConsole("[SQLITE DATABASE]: COMMITTED", Level.WARNING);
				
				sqlite.disconnect();
				MainSimulator.printConsole("[SQLITE DATABASE]: DISCONNECTED", Level.WARNING);
			}
			catch (Exception e) {
				//MainSimulator.printConsole("EXIT - " + e.getMessage(), Level.SEVERE);
			}
		}
		// P2P
		else {
			for (DataManagementUserDB userDBList_P2P : dataManagementUserDBList_P2P ) {
				SQLiteDataManagementUserDB userMemo = (SQLiteDataManagementUserDB) userDBList_P2P;
				userMemo.commit();
				
				try {
					userMemo.disconnect();
				}
				catch (Exception e) {
					MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
				}
			}
			MainSimulator.printConsole("[SQLITE DATABASE]: User DBs COMMITTED", Level.WARNING);
			for (DataManagementQueueDB queueDBList_P2P : dataManagementQueueDBList_P2P ) {
				SQLiteDataManagementQueueDB queueMemo = (SQLiteDataManagementQueueDB) queueDBList_P2P;
				queueMemo.commit();
				
				try {
					queueMemo.disconnect();
				}
				catch (Exception e) {
					MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
				}
			}
			MainSimulator.printConsole("[SQLITE DATABASE]: Queue DBs COMMITTED", Level.WARNING);
		}
		
		// Disconnect from museum db too
		dataInstanceMuseumDB.disconnect();
		
		// Empty visited items
		Configuration.simulation.oldPathUserSpecial.clear();
		
		// Update distancesBetweenUsers file (if Literals.COMPILE_DISTANCES_STATS)
		if (Literals.COMPILE_DISTANCES_STATS) {
			// Update distances ended
			updateUserDistances(getTimeAvailableUserInSecond());
			
			// Write to file
			writeDistancesStats();
		}
		
		MainSimulator.userRunnable.running = false;
	}

	/**
	 * The user generates a rating to the item seen in a specific context.
	 * 
	 * @param uID:       The ID of the current user.
	 * @param itemID:    The ID of the item seen.
	 * @param contextID: The ID of the user's context.
	 * @return The rating.
	 */
	public float generateRating(long uID, long itemID, long contextID) {
		float rating = 0;
		try {
			long init, ifCheck, getRating, print;
			init = System.currentTimeMillis();
			if (dataModelMuseumDB.getItemIDsFromUser(uID).contains(itemID)) {
				ifCheck = System.currentTimeMillis();
				rating = dataModelMuseumDB.getPreferenceValue(uID, itemID, contextID); // LOTS OF DB ACCESSES -> SOLVED
				getRating = System.currentTimeMillis();
				MainSimulator.printConsole("Generating rating: [User: " + uID + " | Item: " + itemID + " | Item | Rating: " + rating + "]", Level.INFO);
				print = System.currentTimeMillis();
				log.log(Level.INFO, "Valoraci�n: " + (ifCheck - init) + ";" + (getRating - ifCheck) + ";" + (print - getRating) + "; Total: " + ((ifCheck - init) + (getRating - ifCheck) + (print - getRating)));
			} else {
				log.log(Level.INFO, "ELSE");
				MainSimulator.printConsole("Generating rating: [User: " + uID + " | Item: " + itemID + " | Door or Stairs]", Level.INFO);
			}
		} catch (TasteException ex) {
			log.log(Level.SEVERE, "EXCEPTION: " + ex);
			Logger.getLogger(UserRunnable.class.getName()).log(Level.SEVERE, null, ex);
		}
		return rating;
	}

	/**
	 * Get the current time.
	 * 
	 * @param locationStartVertex: The initial vertex location.
	 * @param locationEndVertex:   The final vertex location.
	 * @return The current time.
	 */
	public double getCurrentTime(String locationStartVertex, String locationEndVertex) {
		String[] arrayStartVertex = locationStartVertex.split(", ");
		double x1 = Double.valueOf(arrayStartVertex[0]).doubleValue();
		double y1 = Double.valueOf(arrayStartVertex[1]).doubleValue();
		String[] arrayEndVertex = locationEndVertex.split(", ");
		double x2 = Double.valueOf(arrayEndVertex[0]).doubleValue();
		double y2 = Double.valueOf(arrayEndVertex[1]).doubleValue();
		double distance = Distance.distanceBetweenTwoPoints(x1, y1, x2, y2);
		double currentTime = distance / Configuration.simulation.getUserVelocityInPixelBySecond();
		return currentTime;
	}

	/**
	 * The RS user listen the information.
	 * 
	 * @param informationToPropagate: The information to propagate.
	 */
	public void specialUserListenTheInformation(InformationToPropagate informationToPropagate, User currentUser) {
 		long initialTimeRecommender = 0, finalTimeRecommender = 0;
		initialTimeRecommender = System.currentTimeMillis();
		DataManagementUserDB dataManagementUserDB = null;
		// Insert rating in the data base, considering the type of network (Centralized or P2P).
		if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
			dataManagementUserDB = dataManagementUserDB_Centralized;
		} else if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
			dataManagementUserDB = dataManagementUserDBList_P2P.get((int) currentUser.userID - 1);
		}
		finalTimeRecommender = System.currentTimeMillis();
		log.log(Level.INFO, "[DatabaseConnection]: " + (finalTimeRecommender - initialTimeRecommender));

		initialTimeRecommender = System.currentTimeMillis();
		
		boolean ifInsertOK = dataManagementUserDB.insertInformation(informationToPropagate, this.numberOfITems); // Lot of time consumed
		
		
		finalTimeRecommender = System.currentTimeMillis();
		log.log(Level.INFO, "[insertInformation]: " + (finalTimeRecommender - initialTimeRecommender));
		
		initialTimeRecommender = System.currentTimeMillis();
		if (ifInsertOK) {
			log.log(Level.INFO, "    Insert OK!!!"); // Info is inserted OK
			int numberOfItems = 0;
			if (!numberOfReceivedItems.isEmpty() && numberOfReceivedItems.containsKey((long) currentUser.userID)) {
				numberOfItems = numberOfReceivedItems.get((long) currentUser.userID);
				numberOfItems++;
				numberOfReceivedItems.put((long) currentUser.userID, numberOfItems);
			} else {
				numberOfItems++;
				numberOfReceivedItems.put((long) currentUser.userID, numberOfItems);
			}
		}
		finalTimeRecommender = System.currentTimeMillis();
		logRecommender.log(Level.INFO, "[if(InsertOK)]: " + (finalTimeRecommender - initialTimeRecommender));
	}

	/**
	 * Gets the room from vertex. Get property directly from DataAccessItemFile (which has properties already loaded).
	 * 
	 * @param vertex: The vertex.
	 * @return The room.
	 */
	public int getRoom(long vertex) {
		/*
		 * PREVIOUS
		 * 
		int room = 0;
		Map<String, Object> map = ((mxGraphModel) MainMuseumSimulator.graphComponent.getGraph().getModel()).getCells();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			mxCell cell = (mxCell) entry.getValue();
			Object object = cell.getValue();
			if (object != null) {
				String[] arrayCell = cell.getValue().toString().split(", ");
				if (arrayCell.length == 3) {
					if (Long.valueOf(arrayCell[2]).longValue() == vertex) {
						// System.out.println("value: " + cell.getValue().toString());
						room = Integer.valueOf(arrayCell[1]).intValue();
					}
				}
			}
		}
		return room;
		*/
		
		/*
		int room = Integer.valueOf(MainMuseumSimulator.floor.getGraphItemRoom((int) vertex));
		System.out.println("Vertex:"+vertex+"; located in room:"+room);
		
		return room;*/
		
		String roomString = MainSimulator.floor.getGraphItemRoom((int) vertex);
		int room = 0;
		
		if (roomString != null)
			room = Integer.valueOf(roomString);
		
		return room;
	}

	/**
	 * The user's path is updated by the recommendation algorithm if he ignore the recommendation.
	 */
	public void specialUserDisobedience(User currentUser) {
		// If the user ignores the recommendation, then the recommended path will be updated.
		double factorDisobedience = 0.5;// (double) random.nextInt(10) /(double) 10;//
		if ((factorDisobedience <= Configuration.simulation.getProbabilityUserDisobedience())) {
			// The current edge.
			String edge = path.get(userPositionInPath[(int) currentUser.userID]);
			// Gets randomly the next item to visit by a user.
			long nextItemSelected = getItemRandomly(edge, currentUser.userID);

			// The RS user path is updated with the recommendation algorithm.
			long startVertex = Long.valueOf(cleanEdge(edge)[0]).longValue();
			long endVertex = Long.valueOf(cleanEdge(edge)[1]).longValue();
			updateSpecialUserPath(startVertex, endVertex, true, nextItemSelected, false, currentUser);
		}
	}

	/**
	 * Gets randomly the next item to visit by a user.
	 * 
	 * @param edge: The current edge.
	 * @return The next item to visit.
	 */
	public long getItemRandomly(String edge, long specialUserID) {
		long itemSelected = 0;
		try {
			String[] array = cleanEdge(edge);
			// The current item.
			long currentItem = Long.valueOf(array[1]).longValue();
			// Get randomly the next item to visit by user.
			int room = graphSpecialUser.getRoomFromItem(currentItem);
			int numberItemsByRoom = graphSpecialUser.accessGraphFile.getNumberOfItemsByRoom(room);
			itemSelected = graphSpecialUser.accessGraphFile.getItemOfRoom(ThreadLocalRandom.current().nextInt(1, numberItemsByRoom), room);
			// If the item has been seen by the RS user, then another item will be chosen to visit within the room.
			FastIDSet itemsFromUser = dataModelMuseumDB.getItemIDsFromUser(specialUserID);
			boolean isItemSeen = false;
			if (itemsFromUser.contains(itemSelected)) {
				for (LongPrimitiveIterator it = itemsFromUser.iterator(); it.hasNext();) {
					long item = it.next().longValue();
					if (!itemsFromUser.contains(item)) {
						itemSelected = item;
						isItemSeen = true;
					}
				}
			}
			// If all items in the current room have been visited, then the user will be moved to another room.
			if (!isItemSeen) {
				// The door closest to the current item is obtained for the user to move to another room.
				List<Long> doorsByRoom = graphSpecialUser.getDoorsByRoom(room);
				itemSelected = graphSpecialUser.getDoorClosestToTheItem(currentItem, doorsByRoom);
			}
		} catch (TasteException ex) {
			Logger.getLogger(UserRunnable.class.getName()).log(Level.SEVERE, null, ex);
		}
		return itemSelected;
	}

	/**
	 * Calculates the next movement of the user.
	 * 
	 * @param location_v1: The initial vertex location.
	 * @param location_v2: The final vertex location.
	 * @param posUser:     The current user position.
	 * @param itemID:      The item ID.
	 * @return The next movement of the user.
	 */
	private String nextMovement(String location_v1, String location_v2, User currentUser, int itemID) {
		//long init = System.currentTimeMillis();
		//long initPoints = System.currentTimeMillis();
		// P0
		double xInitial = Double.valueOf(location_v1.split(", ")[0]);
		double yInitial = Double.valueOf(location_v1.split(", ")[1]);
		// P1
		double xFinal = Double.valueOf(location_v2.split(", ")[0]);
		double yFinal = Double.valueOf(location_v2.split(", ")[1]);
		MainSimulator.printConsole("Move from: [" + xInitial + ", " + yInitial + "] to:  [" + xFinal + ", " + yFinal + "]", Level.INFO);
		//long finalPoints = System.currentTimeMillis();

		//long initAxis = System.currentTimeMillis();
		
		// Moving from P0 to P1
		// Axis X
		double velocityAxisX = getVelocityAxisX(xInitial, yInitial, xFinal, yFinal);
		double maxDistanceToReachTarget = Math.abs(xFinal - xInitial);
		double xF1 = pointFinalToMove(xInitial, velocityAxisX, availableTimeOfUsers[currentUser.userID - 1], maxDistanceToReachTarget);

		// Axis Y
		double velocityAxisY = getVelocityAxisY(xInitial, yInitial, xFinal, yFinal);
		maxDistanceToReachTarget = Math.abs(yFinal - yInitial);
		double yF1 = pointFinalToMove(yInitial, velocityAxisY, availableTimeOfUsers[currentUser.userID - 1], maxDistanceToReachTarget);
		//long finalAxis = System.currentTimeMillis();

		//long initRemainingTime = System.currentTimeMillis();
		double remainingTimeAvailable = getRemainingTimeAvailable(xInitial, yInitial, xFinal, yFinal, availableTimeOfUsers[currentUser.userID - 1]);
		MainSimulator.printConsole("Remaining time available for the user: " + remainingTimeAvailable, Level.INFO);
		if (remainingTimeAvailable < 0) {
			remainingTimeAvailable = 0;
			MainSimulator.printConsole("The user does not have time to get to the item, then the remaining time available: " + remainingTimeAvailable, Level.INFO);
		}

		availableTimeOfUsers[currentUser.userID - 1] = remainingTimeAvailable;
		//long finalRemainingTime = System.currentTimeMillis();
		
		//long end = System.currentTimeMillis();
		
		//log.log(Level.FINER, "!!!! Times next movement: POINTS= " + (finalPoints - initPoints) + ";" + (finalAxis - initAxis) + ";" + (finalRemainingTime - initRemainingTime) + "; TOTAL: " + (end - init));
		
		//long initRating = System.currentTimeMillis();

		// Time of the rating
		if (((xInitial <= xFinal) && (xF1 >= xFinal)) && ((yInitial <= yFinal) && (yF1 >= yFinal)) || ((xInitial >= xFinal) && (xF1 <= xFinal)) && ((yInitial >= yFinal) && (yF1 <= yFinal))
				|| ((xInitial <= xFinal) && (xF1 >= xFinal)) && ((yInitial >= yFinal) && (yF1 <= yFinal)) || ((xInitial >= xFinal) && (xF1 <= xFinal)) && ((yInitial <= yFinal) && (yF1 >= yFinal))
				|| ((xInitial <= xFinal) && (xF1 >= xFinal)) && ((yInitial == yFinal) && (yF1 == yFinal)) || ((xInitial >= xFinal) && (xF1 <= xFinal)) && ((yInitial == yFinal) && (yF1 == yFinal))
				|| ((xInitial == xFinal) && (xF1 == xFinal)) && ((yInitial >= yFinal) && (yF1 <= yFinal)) || ((xInitial == xFinal) && (xF1 == xFinal)) && ((yInitial <= yFinal) && (yF1 >= yFinal))) {
			// If he arrives at a painting or sculpture he must stop to observe it.
			if (itemID <= this.numberOfITems) {
				availableTimeOfUsers[currentUser.userID - 1] -= Configuration.simulation.getDelayObservingPaintingInSecond();
				MainSimulator.printConsole("Remaining time available after to generate rating: " + availableTimeOfUsers[currentUser.userID - 1], Level.INFO);
				voting[currentUser.userID - 1] = true;
			}
		}
		//long endRating = System.currentTimeMillis();
		
		//long initPrints = System.currentTimeMillis();

		String xyF = xF1 + ", " + yF1;
		MainSimulator.printConsole("It is moved from: " + xInitial + ", " + yInitial + " to " + xyF, Level.INFO);
		MainSimulator.printConsole("[Current location: " + xyF + "]", Level.INFO);
		xInitial = xF1;
		yInitial = yF1;
		
		//long finalPrints = System.currentTimeMillis();
		
		//log.log(Level.FINER, "!!!! Times next movement: " + (end - init) + ";" + (endRating - initRating) + ";" + (finalPrints - initPrints) + "; TOTAL: " + ((end - init)+(endRating - initRating)+(finalPrints - initPrints))); // TIME CONSUMED AT THE BEGINNING
		return xyF;
	}

	/**
	 * Gets the velocity from axis X.
	 * 
	 * @param xInitial: The initial X.
	 * @param yInitial: The initial Y.
	 * @param xFinal:   The final X.
	 * @param yFinal:   The final Y.
	 * @return The velocity.
	 */
	public double getVelocityAxisX(double xInitial, double yInitial, double xFinal, double yFinal) {
		double angle = getAngle(xInitial, yInitial, xFinal, yFinal);
		double velocity = Configuration.simulation.getUserVelocityInPixelBySecond() * Math.cos(angle);
		if (xFinal < xInitial) {
			velocity = Math.abs(velocity) * (-1);
		} else {
			velocity = Math.abs(velocity);
		}
		return velocity;
	}

	/**
	 * Gets the velocity from axis Y.
	 * 
	 * @param xInitial: The initial X.
	 * @param yInitial: The initial Y.
	 * @param xFinal:   The final X.
	 * @param yFinal:   The final Y.
	 * @return The velocity.
	 */
	public double getVelocityAxisY(double xInitial, double yInitial, double xFinal, double yFinal) {
		double angle = getAngle(xInitial, yInitial, xFinal, yFinal);
		double velocity = Configuration.simulation.getUserVelocityInPixelBySecond() * Math.sin(angle);
		if (yFinal < yInitial) {
			velocity = Math.abs(velocity) * (-1);
		} else {
			velocity = Math.abs(velocity);
		}
		return velocity;
	}

	/**
	 * Calculate the final point where the user will move.
	 * 
	 * @param pointInitial:             The initial point.
	 * @param velocity:                 The velocity.
	 * @param availableTime:            The available time by user.
	 * @param maxDistanceToReachTarget: The maximum distance to reach the target.
	 * @return
	 */
	public double pointFinalToMove(double pointInitial, double velocity, double availableTime, double maxDistanceToReachTarget) {
		double maxDistanceWithTimeAvailable = Math.abs(velocity * availableTime);
		double distanceToTraverse = Math.min(maxDistanceToReachTarget, maxDistanceWithTimeAvailable);
		double pointFinal = pointInitial + (distanceToTraverse * Math.signum(velocity));
		return pointFinal;
	}

	/**
	 * Gets the angle.
	 * 
	 * @param xInitial: The initial X.
	 * @param yInitial: The initial Y.
	 * @param xFinal:   The final X.
	 * @param yFinal:   The final Y.
	 * @return The velocity.
	 */
	public double getAngle(double xInitial, double yInitial, double xFinal, double yFinal) {
		double angle = 0;
		if (xFinal == xInitial) {
			angle = Math.PI / 2;
		} else {
			angle = Math.atan(Math.abs(yFinal - yInitial) / (xFinal - xInitial));
		}
		return angle;
	}

	/**
	 * Calculates the user's remaining time to complete the visit.
	 * 
	 * @param xInitial:    The initial X.
	 * @param yInitial:    The initial Y.
	 * @param xFinal:      The final X.
	 * @param yFinal:      The final Y.
	 * @param currentTime: The current time.
	 * @return The user's remaining time to complete the visit.
	 */
	public double getRemainingTimeAvailable(double xInitial, double yInitial, double xFinal, double yFinal, double currentTime) {
		double distance = Math.abs(Distance.distanceBetweenTwoPoints(xInitial, yInitial, xFinal, yFinal));
		double velocity = Configuration.simulation.getUserVelocityInPixelBySecond();
		MainSimulator.printConsole("Time consumed: " + distance / velocity, Level.INFO);
		double time = currentTime - (distance / velocity);
		return time;
	}

	/**
	 * The user's path is updated by the recommendation algorithm.
	 */
	public void updatePathRecommender() {
		for (int userPosition = 0; userPosition < Configuration.simulation.userList.size(); userPosition++) {
			User currentUser = Configuration.simulation.userList.get(userPosition);
			if (currentUser.isSpecialUser) {
				int numberOfItems = 0;
				if (!numberOfReceivedItems.isEmpty() && numberOfReceivedItems.containsKey((long) currentUser.userID)) {
					// Number of items received by the current RS user.
					numberOfItems = numberOfReceivedItems.get((long) currentUser.userID);
				}
				// If the number of information received by the RS user is higher than a fixed amount, then his path must be updated by the recommendation algorithm, starting with the last item
				// he saw.
				if ((numberOfItems >= Configuration.simulation.getNumberVoteReceived()) && (currentTimeOfUsers[currentUser.userID - 1] < Configuration.simulation.getTimeAvailableUserInSecond())
						&& (currentTimeOfUsers[currentUser.userID - 1] >= Configuration.simulation.getMinimumTimeToUpdateRecommendation())) {
					path = graphSpecialUser.paths.get((int) (currentUser.userID - 1));
					String edge = path.get(userPositionInPath[(int) (currentUser.userID - 1)]);
					long startVertex = Long.valueOf(cleanEdge(edge)[0]).longValue();
					long endVertex = Long.valueOf(cleanEdge(edge)[1]).longValue();
					// The RS user path is updated with the recommendation algorithm.
					updateSpecialUserPath(startVertex, endVertex, false, 0, false, currentUser);
				}
			}
		}
	}

	/**
	 * Gets the current user's context.
	 * 
	 * @param room:   The room.
	 * @param userID: The user ID.
	 * @return The current user's context.
	 */
	public long getCurrentContext(User currentUser) {
		
		int temperatureRoom = 1;
		int numberPeopleRoom = getNumberPeopleRoom(currentUser.room, currentUser);
		int noiseLevelRoom = 8;
		int mood = moodOfUsers[currentUser.userID - 1];

		LinkedList<Integer> currentContextValues = new LinkedList<>();
		currentContextValues.add(temperatureRoom);
		currentContextValues.add(numberPeopleRoom);
		currentContextValues.add(noiseLevelRoom);
		currentContextValues.add(mood);

		// TIME CONSUMING (DATAACCESSLAYER
		long initialTimeContext = System.currentTimeMillis();
		
		//String new_user_db_file_path = Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH;
		//DataAccessLayer dataAccesLayerDBMuseum = new DataAccessLayer(new_user_db_file_path);
		
		long finalTimeContext = System.currentTimeMillis();
		log.log(Level.INFO, "!!! CONTEXT [new DataAccessLayer(new_user_db_file_path)]: " + (finalTimeContext - initialTimeContext));
		
		
		initialTimeContext = System.currentTimeMillis();
		long currentContext = dataModelMuseumDB.getContextIDFor(currentContextValues);
		finalTimeContext = System.currentTimeMillis();
		
		//System.out.println("CURRENT CONTEXT: " + currentContext);
		log.log(Level.INFO, "!!! CONTEXT [getContextIDFor]: " + (finalTimeContext - initialTimeContext));
		
		return currentContext;
	}

	/**
	 * Gets the number of people in a room.
	 * 
	 * @param room:   The current room.
	 * @param userID: The user ID.
	 * @return The number of people in a room.
	 */
	public int getNumberPeopleRoom(int room, User currentUser) {
		int numberPeopleRoom = 0;
		int numberPeople = 0;
		int currentRoom = room;

		for (int i = 0; i < userList.size(); i++) {
			User otherUser = userList.get(i);
			if ((currentRoom == otherUser.room) && (currentUser.userID != otherUser.userID)) {
				numberPeople++;
			}
		}
		// 4 none, 5 little, 6 much
		if (numberPeople == 0) {
			numberPeopleRoom = 4; // none
		} else {
			if (numberPeople >= 1 && numberPeople <= 7) {
				numberPeopleRoom = 5; // little
			} else {// numberPeople > 7
				numberPeopleRoom = 6; // much
			}
		}
		return numberPeopleRoom;
	}

	/**
	 * Combines the old path with the recommended current path.
	 * 
	 * @param startVertex:     The start vertex.
	 * @param endVertex:       The end vertex.
	 * @param pathSpecialUser: The user path.
	 * @param finishPath:      Is the finish path.
	 * @return The combined path.
	 */
	public List<String> combinePaths(long startVertex, long endVertex, List<String> pathSpecialUser, boolean finishPath) {
		List<String> currentPath = new LinkedList<>();
		for (int i = 0; i < oldPathUserSpecial.size(); i++) {
			currentPath.add(oldPathUserSpecial.get(i));
		}
		for (int j = 0; j < pathSpecialUser.size(); j++) {
			currentPath.add(pathSpecialUser.get(j));
		}
		
		//currentPath = clearPath(currentPath);
		return currentPath;
	}

	public List<String> combinePathsDisobedience(long nextItemSelected, long startVertex, long endVertex, List<String> pathSpecialUser, long specialUserID) {
		List<String> oldPath = graphSpecialUser.paths.get((int) specialUserID - 1);
		List<String> currentPath = new LinkedList<>();
		for (int i = 0; i < oldPath.size(); i++) {
			String path = oldPath.get(i);
			currentPath.add(path);
			if (path.contains(String.valueOf("(" + startVertex + " : " + endVertex + ")"))) {
				currentPath.add("(" + startVertex + " : " + nextItemSelected + ")");
				for (int j = 1; j < pathSpecialUser.size(); j++) {
					currentPath.add(pathSpecialUser.get(j));
				}
				break;
			}
		}
		
		//currentPath = clearPath(currentPath);
		return currentPath;
	}

	private List<String> clearPath(List<String> currentPath) {
		// To remove the "," at the end of the generated path.
		if (currentPath.size() > 0) {
			// To remove the "," at the end of the generated path.
			String last = currentPath.get(currentPath.size()-1);
			while (last == null || last.equalsIgnoreCase("")) { // In case there are more than 1 wrong items
				currentPath.remove(currentPath.size()-1);
				last = currentPath.get(currentPath.size()-1);
			}
		}
		return currentPath;
	}

	/**
	 * Cleans the edge.
	 * 
	 * @param edge The edge.
	 * @return A list with the position X and Y of the edge.
	 */
	public String[] cleanEdge(String edge) {
		String cleanEdge = edge.replace("(", "");
		cleanEdge = cleanEdge.replace(")", "");
		String[] array = cleanEdge.split(" : ");
		return array;
	}

	/**
	 * Exchange of information between users: for a P2P architecture and a opportunistic propagation strategy.
	 */
	public void exchangeDataP2POpportunistic() {
		User farthestUser = null;

		// Users' TTP is initialized where isTTPInitialized=0.
		initializeTTP();
		for (int userPosition = 0; userPosition < userList.size(); userPosition++) {
			// Get users with TTP <= 0 and isTTPInitialized==1
			DataManagementQueueDB dataManagementQueueDB = dataManagementQueueDBList_P2P.get(userPosition);
			LinkedList<Long> idUsers = dataManagementQueueDB.getUsersWithInformationToPropagate();

			for (int i = 0; i < idUsers.size(); i++) {
				long id_user = idUsers.get(i);

				// Get the closest neighbors to currentUser, who match the allowed distance and are in the same room.
				//User currentUser = userList.get(i);
				User currentUser = userList.get((int) id_user - 1); // GET THE CURRENT USER FROM THE LIST OF USERS USING CORRECT INDEX -> id_user, NOT i (which is the index of the list of users with info to propagate)
				List<User> neighborsInTheAllowedDistance = getNeighborsInTheAllowedDistance(currentUser, userList);
				if (!neighborsInTheAllowedDistance.isEmpty()) {
					// The list of information to be propagated is obtained from id_user with TTP <= 0 and isTTPInitialized==1 for id_user.
					LinkedList<InformationToPropagate> informationList = dataManagementQueueDB.getInformation(id_user);

					// Neighbors who match the distance criteria, listen the information sent by the current user.
					neighborsListenTheInformation(informationList, neighborsInTheAllowedDistance);

					// The most distant neighbor (among the users within its radius of action) is identified and will be in charge of propagating the information.
					farthestUser = getFarthestUser(currentUser, neighborsInTheAllowedDistance);

					int countRatingsPropagated = 0;
					if (Configuration.simulation.getPropagationStrategy().equalsIgnoreCase("Opportunistic")) {
						// The information from the currentUser is propagated to the farthestUser.
						countRatingsPropagated = passPropagationToken(farthestUser.userID, id_user);
					} else if (Configuration.simulation.getPropagationStrategy().equalsIgnoreCase("Flooding")) {
						// The information from the currentUser is propagated to all the neighbors.
						countRatingsPropagated = passPropagationToken(neighborsInTheAllowedDistance, id_user);
					}
					countItemsTTPByUser.put(id_user, countRatingsPropagated);
					// Prints the number of times that the information is propagated.
					numberItemsPropagated += countRatingsPropagated;
				}
			}
			// The TTL and TTP values are updated, decreasing their value.
			dataManagementQueueDB.updateTTL();
			dataManagementQueueDB.updateTTP();
		}
	}

	/**
	 * Initializes the TTP value.
	 */
	public void initializeTTP() {
		for (int userPosition = 0; userPosition < userList.size(); userPosition++) {
			// Get the users, as well as the number of possible information to propagate, where isTTPInitialized=0.
			DataManagementQueueDB dataManagementQueue = dataManagementQueueDBList_P2P.get(userPosition);
			LinkedList<String> idUsers_countRatings = dataManagementQueue.getNumberItemsByUserWithoutInitializeTTP();

			for (int i = 0; i < idUsers_countRatings.size(); i++) {
				String id_user_countRatings = idUsers_countRatings.get(i);
				String[] array = id_user_countRatings.split(",");
				long id_user = Long.valueOf(array[0]).longValue();

				// The value of TTP is initialized for the ratings of that user to be
				// propagated, where the isIntializedTTP=0.
				if (countItemsTTPByUser.get(id_user) > 0) {
					int countRatings = Integer.valueOf(array[1]).intValue();
					dataManagementQueue.initializeAllTTP(countRatings, id_user);
					// In order to indicate that the user is busy propagating information.
					countItemsTTPByUser.put(id_user, 0);
				}
			}
		}
	}

	/**
	 * Get the neighbors in the allowed distance.
	 * 
	 * @param currentUser: The current user.
	 * @param users:       All users.
	 * @return The neighbors.
	 */
	public List<User> getNeighborsInTheAllowedDistance(User currentUser, ArrayList<User> users) {
		List<User> neighborsInTheAllowedDistance = new LinkedList<>();
		String locationCurrentUser = currentUser.x + ", " + currentUser.y;
		for (int u = 0; u < users.size(); u++) {
			User user = users.get(u);
			String locationUser = user.x + ", " + user.y;
			if (user.room == currentUser.room && currentUser.userID != user.userID) {
				// Calculate the distance between two users.
				double distance = distanceBetweenUsers(locationCurrentUser, locationUser);
				if (distance <= Configuration.simulation.getCommunicationRange()) {
					neighborsInTheAllowedDistance.add(user);
				}
			}
		}
		return neighborsInTheAllowedDistance;
	}

	/**
	 * Calculate the distance between two users.
	 * 
	 * @param positionUser1: The position of the first user.
	 * @param positionUser2: The position of the second user.
	 * @return The distance between two users.
	 */
	public double distanceBetweenUsers(String positionUser1, String positionUser2) {
		// boolean areNearby = false;
		String[] arrayUser1 = positionUser1.split(", ");
		String[] arrayUser2 = positionUser2.split(", ");
		double u_x1 = Double.valueOf(arrayUser1[0]).doubleValue();
		double u_y1 = Double.valueOf(arrayUser1[1]).doubleValue();
		double u_x2 = Double.valueOf(arrayUser2[0]).doubleValue();
		double u_y2 = Double.valueOf(arrayUser2[1]).doubleValue();
		double distance = Distance.distanceBetweenTwoPoints(u_x1, u_y1, u_x2, u_y2);
		return distance;
	}

	/**
	 * The neighbors listen the information.
	 * 
	 * @param informationToPropagateList:    The list with information to propagate.
	 * @param neighborsInTheAllowedDistance: The list of neighbors in the allowed distance.
	 * @param currentUserID:                 The ID of the current user.
	 */
	public void neighborsListenTheInformation(List<InformationToPropagate> informationToPropagateList, List<User> neighborsInTheAllowedDistance) {
		for (int in = 0; in < informationToPropagateList.size(); in++) {
			InformationToPropagate informationToPropagate = informationToPropagateList.get(in);
			for (int i = 0; i < neighborsInTheAllowedDistance.size(); i++) {
				User currentUser = neighborsInTheAllowedDistance.get(i);
				if (currentUser.isSpecialUser) {
					specialUserListenTheInformation(informationToPropagate, currentUser);
				}
			}
		}
	}

	/**
	 * Get the farthest user to the current user.
	 * 
	 * @param currentUser:               The current user.
	 * @param neighborsOfTheCurrentUser: The neighbors of the current user.
	 * @return
	 */
	public User getFarthestUser(User currentUser, List<User> neighborsOfTheCurrentUser) {
		String locationCurrentUser = currentUser.x + ", " + currentUser.y;
		User farthestUser = null;
		double longestDistance = 0;
		for (int u = 0; u < neighborsOfTheCurrentUser.size(); u++) {
			User user = neighborsOfTheCurrentUser.get(u);
			String locationUser = user.x + ", " + user.y;
			// Calculate the distance between two users.
			double distance = distanceBetweenUsers(locationCurrentUser, locationUser);
			if (distance >= longestDistance) {
				longestDistance = distance;
				farthestUser = user;
			}
		}
		return farthestUser;
	}

	/**
	 * The information is propagated to the farthest neighbor.
	 * 
	 * @param farthestUser: The farthest user.
	 * @param id_user:      The current user.
	 * @return The number of propagated ratings.
	 */
	public int passPropagationToken(long farthestUser, long id_user) {
		int countRatings = dataManagementQueueDBList_P2P.get((int) id_user - 1).updateExchange(farthestUser, id_user);
		return countRatings;
	}

	/**
	 * The information is propagated to the all neighbors.
	 * 
	 * @param neighborsInTheAllowedDistance
	 * @param id_user
	 * @return
	 */
	public int passPropagationToken(List<User> neighborsInTheAllowedDistance, long id_user) {
		LinkedList<InformationToPropagate> informationList = dataManagementQueueDBList_P2P.get((int) id_user - 1).getInformation(id_user);
		int countRatings = informationList.size();
		for (int in = 0; in < informationList.size(); in++) {
			InformationToPropagate information = informationList.get(in);
			dataManagementQueueDBList_P2P.get((int) id_user - 1).deleteInformation(information);

			for (int i = 0; i < neighborsInTheAllowedDistance.size(); i++) {
				User neighbor = neighborsInTheAllowedDistance.get(i);
				long neighborUser = (long) neighbor.userID;
				information.setIsTTPInitialized(0);
				dataManagementQueueDBList_P2P.get((int) id_user - 1).insertInformation(neighborUser, information);
			}
		}
		return countRatings;
	}

	/**
	 * Exchange of information between users: for a P2P architecture and a flooding propagation strategy.
	 */
	public void exchangeDataP2PFlooding() {
		log.log(Level.SEVERE, "TODO: exchangeDataP2PFlooding");
		logRecommender.log(Level.SEVERE, "TODO: exchangeDataP2PFlooding");
	}

	/**
	 * Create a database for all users in order to carry out the simulation process, by considering the specified recommendation algorithm and using a centralized architecture.
	 * 
	 * @param recommendationAlgorithm: The specified recommendation algorithm.
	 * @throws SQLException
	 * @throws TasteException
	 */
	public void initializeUserDB_Centralized(String recommendationAlgorithm) {
		MainSimulator.printConsole("Initializing a centralized database for users:", Level.WARNING);

		// Creates a centralized database for all users.
		String db_file_path = Literals.DB_CENTRALIZED_USER_PATH;
		String user_dbURL = Literals.SQL_DRIVER + db_file_path;

		// Independently of the recommendation strategy chose, a copy of db_user.db is made but with another name (e.g., db_user_all.db).
		File user_source_file = new File(Literals.DB_USER_PATH);
		File user_out_file = new File(db_file_path);
		if (user_out_file.exists()) {
			user_out_file.delete();
		}
		try {
			Files.copy(user_source_file.toPath(), user_out_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Create DB instance
		dataInstanceUserDB_Centralized = new Database();

		// The information contained in the DB will depend of type of recommendation algorithm chosen.
		// Use factory to select userDB according to the wanted db
		DAOFactory factory = DAOFactory.getFactory(Literals.currentDBUsed);
		
		dataManagementUserDB_Centralized = factory.getUserDB(user_dbURL, dataInstanceUserDB_Centralized); // PREVIOUS: new SQLiteDataManagementUserDB(user_dbURL);
		/*if (recommendationAlgorithm.equalsIgnoreCase("K-Ideal (K-Ideal)") || recommendationAlgorithm.equalsIgnoreCase("Know-It-All (Know-It-All)")) {
			for (int userPosition = 0; userPosition < userList.size(); userPosition++) {
				User currentUser = userList.get(userPosition);
				if (currentUser.isSpecialUser) {
					// It cleans the information for the RS user.
					dataManagementUserDB_Centralized.deleteInformationFromTable(currentUser.userID);
					// The RS user will not have any real items but fictitious ones (e.g., the item 400 that has not been evaluated by any user).
					insertFictisuousInformationInUserDBFrom(currentUser.userID, dataManagementUserDB_Centralized);
				}
			}
		}*/ //else {
			// It cleans the user_item_context table from db_user.db, in order to populate it with the required information of the chosen recommendation algorithm.
			dataManagementUserDB_Centralized.deleteAllInformationFromTable();
			// - Initially, db_user.db will not have any real items but fictitious ones (e.g., the item 400 that has not been evaluated by any user).
			// - In addition, the db_museum.db will be used. It contains the ratings of all users.
			insertFictisuousInformationInUserDB(getNumberOfUser(), dataManagementUserDB_Centralized);
		//}
		MainSimulator.printConsole("Database created: " + user_dbURL, Level.WARNING);

	}

	/**
	 * Create a database by user in order to carry out the simulation process, by considering the specified recommendation algorithm and using a P2P architecture.
	 * 
	 * @param recommendationAlgorithm: The specified recommendation algorithm.
	 * @throws SQLException
	 * @throws TasteException
	 */
	public void initializeUserDB_P2P(String recommendationAlgorithm) {
		MainSimulator.printConsole("Initializing P2P databases of users:", Level.WARNING);
		
		// Initialize database instance's lists
		dataInstanceUserDBList_P2P = new LinkedList<>();
		dataInstanceQueueDBList_P2P = new LinkedList<>();

		// Creates a db_user.db and queue.db by user.
		for (int i = 0; i < userList.size(); i++) {
			long userID = userList.get(i).userID;

			// The file path of the RS user database.
			String new_user_db_file_path = Literals.DB_P2P_USER_PATH + userID + ".db";
			String user_dbURL = Literals.SQL_DRIVER + new_user_db_file_path;

			// The queue database file path.
			String new_queue_db_file_path = Literals.DB_NEW_QUEUE_PATH + userID + ".db";
			String queue_dbURL = Literals.SQL_DRIVER + new_queue_db_file_path;

			// Independently of the recommendation strategy chose, a copy of db_user.db is made but with another name (e.g., db_user_175.db).
			File user_source_file = new File(Literals.DB_USER_PATH);
			File user_out_file = new File(new_user_db_file_path);
			if (user_out_file.exists()) {
				user_out_file.delete();
			}
			try {
				Files.copy(user_source_file.toPath(), user_out_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Create database instance
			Database db = new Database();
			// Add it to instances list
			dataInstanceUserDBList_P2P.add(db);
			//////////////////////////////////////
			

			// The information contained in the DB will depend of type of recommendation algorithm chosen.
			// Use factory to select userDB according to the wanted db
			DAOFactory factory = DAOFactory.getFactory(Literals.currentDBUsed);
			
			DataManagementUserDB dataManagementUserDB = factory.getUserDB(user_dbURL, db); // PREVIOUS: new SQLiteDataManagementUserDB(user_dbURL);
			
			dataManagementUserDBList_P2P.add(dataManagementUserDB);
			if (recommendationAlgorithm.equalsIgnoreCase("K-Ideal (K-Ideal)") || recommendationAlgorithm.equalsIgnoreCase("Know-It-All (Know-It-All)")) {
				// It cleans the information for the RS user.
				dataManagementUserDB.deleteInformationFromTable(userID);
				// The RS user will not have any real items but fictitious ones (e.g., the item 400 that has not been evaluated by any user).
				insertFictisuousInformationInUserDBFrom(userID, dataManagementUserDB);
			} else {
				// It cleans the user_item_context table from db_user.db, in order to populate it with the required information of the chosen recommendation algorithm.
				dataManagementUserDB.deleteAllInformationFromTable();
				// - Initially, db_user.db will not have any real items but fictitious ones (e.g., the item 400 that has not been evaluated by any user).
				// - In addition, the db_museum.db will be used. It contains the ratings of all users.
				insertFictisuousInformationInUserDB(getNumberOfUser(), dataManagementUserDB);
			}
			MainSimulator.printConsole("Database created: " + new_user_db_file_path, Level.WARNING);

			// A copy of the DB queue.db is made but with another name (e.g., queue_175.db).
			File queue_source_file = new File(Literals.DB_QUEUE_PATH);
			File queue_out_file = new File(new_queue_db_file_path);
			if (queue_out_file.exists()) {
				queue_out_file.delete();
			}
			try {
				Files.copy(queue_source_file.toPath(), queue_out_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Delete all information stored into queue.db.
			
			// Create database instance
			Database dbQueue = new Database();
			// Add it to instances list
			dataInstanceQueueDBList_P2P.add(dbQueue);
			//////////////////////////////////////
			
			// We already have the factory created
			DataManagementQueueDB dataManagementQueue = factory.getQueueDB(queue_dbURL, dbQueue); // PREVIOUS: new SQLiteDataManagementQueueDB(queue_dbURL);
			dataManagementQueueDBList_P2P.add(dataManagementQueue);
			dataManagementQueue.deleteAllInformationFromTable();
			MainSimulator.printConsole("Database created: " + new_queue_db_file_path, Level.WARNING);
		}
		
		MainSimulator.printConsole("P2P user and queue databases created", Level.WARNING);
	}

	/**
	 * In db_user.db a item fictitious is inserted for the RS user (e.g., the item 400 that has not been evaluated by any user).
	 * 
	 * @param numberOfUsers: The number of users.
	 */
	public void insertFictisuousInformationInUserDBFrom(long userID, DataManagementUserDB dataManagementUserDB) {
		long user = userID;
		long item = Literals.ITEM_FICTITIOUS;
		long context = 1;
		double rating = 5;
		String opinion = null;
		int userProvided = 1;
		dataManagementUserDB.insert(user, item, context, rating, opinion, userProvided);
	}

	/**
	 * In db_user.db items fictitious is inserted by user (e.g., the item 400 that has not been evaluated by any user).
	 * 
	 * @param numberOfUsers: The number of users.
	 */
	public void insertFictisuousInformationInUserDB(int numberOfUsers, DataManagementUserDB dataManagementUserDB) {
		for (int i = 0; i < numberOfUsers; i++) {
			long user = i + 1;
			long item = Literals.ITEM_FICTITIOUS;
			long context = 1;
			double rating = 5;
			String opinion = null;
			int userProvided = 1;
			dataManagementUserDB.insert(user, item, context, rating, opinion, userProvided);
		}
	}

	/**
	 * Prints the current PC time.
	 * 
	 */
	public void currentTime() {
		// PC Time
		Calendar calendario = Calendar.getInstance();
		int hora, minutos, segundos;
		hora = calendario.get(Calendar.HOUR_OF_DAY);
		minutos = calendario.get(Calendar.MINUTE);
		segundos = calendario.get(Calendar.SECOND);
		MainSimulator.printConsole("PC current time: " + hora + ":" + minutos + ":" + segundos, Level.WARNING);
	}
	
	/**
	 * Ratings predicted and number of users watching same item. Called when the RS user is going to vote the item (because he/she has already seen it).
	 * 
	 * @param informationToPropagate
	 */
	private void updateSpecialUserItemStatistics(InformationToPropagate informationToPropagate) {
		updateRatingsPredicted(informationToPropagate);
		updateNumberUsersWatchingSameItem(informationToPropagate);
		
	}

	/**
	 * Stores all the rating predicted for the item stored, its current rating and its time stamp.
	 * 
	 * @param informationToPropagate
	 */
	private void updateRatingsPredicted(InformationToPropagate informationToPropagate) {
		try {
			FileWriter output = new FileWriter(Literals.CSV_RATINGS, true);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			PredictedRatingsInfo currentPredicted = predictedRatings.get(informationToPropagate.getItem());
			
			if (currentPredicted != null) {
				String[] csvEntry = new String[] { Long.toString(currentPredicted.getId_item()), Double.toString(informationToPropagate.getRating()), 
						Float.toString(currentPredicted.getRatingPredicted()), Integer.toString(currentPredicted.getTime()) };
				
				// Write all entries
				csvWriter.writeNext(csvEntry);
				csvWriter.close();
				
				//System.out.println( "Storing rating prediction info: " + Long.toString(currentPredicted.getId_item()) + ", " + 
				//		informationToPropagate.getRating() + ", " + Float.toString(currentPredicted.getRatingPredicted()) + ", " + Integer.toString(currentPredicted.getTime()) + " from user " + informationToPropagate.getId_user());
			}
			
		}
		catch (IOException e) {
			MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	/**
	 * Stores the number of users that were watching the item at the same time as the RS user and its time stamp.
	 * 
	 * @param informationToPropagate
	 */
	private void updateNumberUsersWatchingSameItem(InformationToPropagate informationToPropagate) {
		try {
			FileWriter output = new FileWriter(Literals.CSV_USERS_SAME_ITEM, true);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			int numUsersWatchingSameItem = idUsersWatchingSameItem.size();
			
			String[] csvEntry = new String[] { Long.toString(informationToPropagate.getItem()), Integer.toString(numUsersWatchingSameItem), 
					Double.toString(informationToPropagate.getRating()), Integer.toString(currentTimeOfUsers[(int) informationToPropagate.getId_user()-1]) };
			
			// Write all entries
			csvWriter.writeNext(csvEntry);
			csvWriter.close();
			
			/*System.out.println( "Number of users watching same item: " + Long.toString(informationToPropagate.getItem()) + ", " + 
					Integer.toString(numUsersWatchingSameItem) + ", " + Double.toString(informationToPropagate.getRating()) + ", " +
					Integer.toString(currentTimeOfUsers[(int) informationToPropagate.getId_user()-1]));*/
			
			// IMPORTANT: Clear list after having stored number os users from current item.
			idUsersWatchingSameItem.clear();
			
		}
		catch (IOException e) {
			MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	/**
	 * Check how many users are watching the same item as the RS user.
	 * 
	 * @param specialUserItemWatched	Item id
	 */
	private void checkUsersWatchingSameItem(long specialUserItemWatched) {
		
		if (specialUserItemWatched > 0) {
			for (int user = 0; user < userList.size(); user++) {
				long itemBeingWatched = itemsBeingWatched[user];
				if (itemBeingWatched == specialUserItemWatched)
					idUsersWatchingSameItem.add((long) user+1);
			}
		}
	}
	
	/**
	 * Update user distances checking if they are under DIST_THRESHOLD at least TIME_THRESHOLD seconds. Creates a list of users under DIST_THRESHOLD to call automaton.
	 * 
	 * @param timeStamp		current time to check TIME_THRESHOLD restriction
	 */
	public void updateUserDistances(int timeStamp) {
		
		// System.out.println("Updating with time: " + timeStamp);
		
		if (timeStamp < getTimeAvailableUserInSecond()) {
			List<DistancesBetweenUsersAndTime> currentDistances = new ArrayList<DistancesBetweenUsersAndTime>();
			
			// For every RS user
			for (int i = numberOfNonSpecialUser; i < userList.size(); i++) {
				User special = userList.get(i);
				for (int j = 0; j < numberOfNonSpecialUser; j++) {
					User nonSpecial = userList.get(j);
					
					double distance = Distance.distanceBetweenTwoPoints(special.x, special.y, nonSpecial.x, nonSpecial.y);
					double distanceInMeters = distance * 1000 / getKmToPixel();
					
					if (distanceInMeters <= Literals.DIST_THRESHOLD) {
						DistancesBetweenUsersAndTime distanceUnderThreshold = new DistancesBetweenUsersAndTime(special.userID, nonSpecial.userID, timeStamp, 0);
						currentDistances.add(distanceUnderThreshold);
						// System.out.println("Users " + special + ", " + nonSpecial + " are separated by " + distanceInMeters + " m");
					}
				}
				updateUserDistancesInTime(currentDistances);
			}
		}
		else {
			updateUserDistancesInTime(new LinkedList<DistancesBetweenUsersAndTime>());
		}
	}

	/**
	 * Applies distance's automaton.
	 * 
	 * If was already and has left (distance is greater than DIST_THRESHOLD) -> Check time together and if it's bigger than TIME_THRESHOLD, add it to list to persist
	 * If new -> Add it to list
	 * If was already and it's still under DIST_THRESHOLD -> update times
	 * 
	 * @param currentDistances	List of users that are in distance under threshold
	 */
	private void updateUserDistancesInTime(List<DistancesBetweenUsersAndTime> currentDistances) {
		
		List<DistancesBetweenUsersAndTime> brokenDistances = distancesBetweenUsers.stream().collect(Collectors.toList());
		
		// NOT UNDER DIST_THRESHOLD ANYMORE
		brokenDistances.removeAll(currentDistances);
		// System.out.println("BROKEN: " + brokenDistances);
		for (DistancesBetweenUsersAndTime brokenDist: brokenDistances) {
			if (distancesBetweenUsers.contains(brokenDist)) {
				int timeTogether = distancesBetweenUsers.get(distancesBetweenUsers.indexOf(brokenDist)).getTimeTogether();
				if (timeTogether >= Literals.TIME_THRESHOLD) {
					completedDistancesBetweenUsers.add(brokenDist);
				}
				// No matter what the distance was, remove it from list
				distancesBetweenUsers.remove(brokenDist);
			}
		}
		
		// OTHERWISE, CHECK CURRENT DISTANCES
		for (DistancesBetweenUsersAndTime dist: currentDistances) {
			// If was already and it's still under DIST_THRESHOLD -> update times
			if (distancesBetweenUsers.contains(dist)) {
				distancesBetweenUsers.get(distancesBetweenUsers.indexOf(dist)).setEndTime(dist.getStartTime()); // Update the end time
			}
			// If new -> Add it to list
			else {
				distancesBetweenUsers.add(dist);
			}
		}
	}
	
	/**
	 * Write distance stats file (called at the end of the simulation). Writes all completedDistancesBetweenUsers entries to CSV.
	 */
	private void writeDistancesStats() {
		try {
			FileWriter output = new FileWriter(Literals.CSV_USERS_DISTANCES_STATS);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			String[] header = { "specialUser", "nonSpecialUser", "Start time (seconds)", "End time (seconds)", "Time together (seconds)" };
	        csvWriter.writeNext(header);
			
			for (DistancesBetweenUsersAndTime entry: completedDistancesBetweenUsers) {
				String[] csvEntry = new String[] { Long.toString(entry.getSpecialUser()), Long.toString(entry.getNonSpecialUser()), 
						Integer.toString(entry.getStartTime()), Integer.toString(entry.getEndTime()), Integer.toString(entry.getTimeTogether()) };
				
				csvWriter.writeNext(csvEntry);
			}
			
			csvWriter.close();
			
		}
		catch (IOException e) {
			MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	/**
	 * Stores in CSV file the information about the predicted ratings to calculate MAE.
	 * 
	 * @param recommendedItems
	 * @param currentUser
	 */
	private void storePredictedRatings(List<RecommendedItem> recommendedItems, User currentUser) {
		// If list of recommendedItems isn't empty
		if (!recommendedItems.isEmpty()) {
			
			for (RecommendedItem item: recommendedItems) {
				
				long id_item = item.getItemID();
				float ratingPredicted = item.getValue();
				int time = currentTimeOfUsers[currentUser.userID-1];
				
				PredictedRatingsInfo ratingPredictedInfo = new PredictedRatingsInfo(id_item, ratingPredicted, time);
				
				predictedRatings.put(id_item, ratingPredictedInfo);
				
				//System.out.println(ratingPredictedInfo);
			}
		}
	}

	// ================ Getters and Setters ========================
	public int getTimeAvailableUserInHour() {
		return timeAvailableUser;
	}

	public int getTimeAvailableUserInSecond() {
		return timeAvailableUser * 3600;
	}

	public int getDelayObservingPaintingInHour() {
		return delayObservingPainting / 3600;
	}

	public int getDelayObservingPaintingInSecond() {
		return delayObservingPainting;
	}

	public double getTimeForIterationInHour() {
		return timeForIteration / 3600;
	}

	public double getTimeForIterationInSecond() {
		return timeForIteration;
	}

	public double getScreenRefreshTimeInSecond() {
		return screenRefreshTime;
	}

	public double getTimeForThePathsInHour() {
		return timeForThePaths;
	}

	public double getTimeForThePathsInSecond() {
		return timeForThePaths * 3600;
	}

	public double getUserVelocityInKmByHour() {
		return userVelocity;
	}

	public double getUserVelocityInPixelByHour() {
		return userVelocity * getKmToPixel();
	}

	public double getUserVelocityInPixelBySecond() {
		return userVelocity * getKmToPixel() / 3600;
	}

	public double getKmToPixel() {
		return kmToPixel;
	}

	public int getTtl() {
		return ttl;
	}

	public int getTimeOnStairs() {
		return timeOnStairs;
	}

	public int getMinimumTimeToUpdateRecommendation() {
		return minimumTimeToUpdateRecommendation;
	}

	public int getCommunicationRange() {
		return communicationRange;
	}

	public int getMaxKnowledgeBaseSize() {
		return maxKnowledgeBaseSize;
	}

	public int getCommunicationBandwidth() {
		return communicationBandwidth;
	}

	public int getLatencyOfTransmission() {
		return latencyOfTransmission;
	}

	public int getTimeToChangeMood() {
		return timeToChangeMood;
	}
	
	public long getSeed() {
		return simulationSeed.getSeed();
	}

	// Getters of Experimentation:
	public int getNumberOfSpecialUser() {
		return numberOfSpecialUser;
	}

	public int getNumberOfNonSpecialUser() {
		return numberOfNonSpecialUser;
	}

	public String getNonSpecialUserPaths() {
		return nonSpecialUserPaths;
	}

	public String getPathStrategy() {
		return pathStrategy;
	}

	public String getRecommendationAlgorithm() {
		return recommendationAlgorithm;
	}

	public float getThresholdRecommendation() {
		return thresholdRecommendation;
	}

	public double getThresholdSimilarity() {
		return thresholdSimilarity;
	}

	public int getHowMany() {
		return howMany;
	}

	public String getNetworkType() {
		return networkType;
	}

	public String getPropagationStrategy() {
		return propagationStrategy;
	}

	public double getProbabilityUserDisobedience() {
		return probabilityUserDisobedience;
	}

	public int getNumberVoteReceived() {
		return numberVoteReceived;
	}

	public int getNumberOfUser() {
		return numberOfUser;
	}
	
	/**
	 * Get the connection of the current door TO STAIRS.
	 * 
	 * @param currentDoor: The current door.
	 * @return The connection of the current door TO STAIRS.
	 */
	public List<Long> getConnectedStairs(long currentDoor) {
		List<Long> stairsList = new LinkedList<>();
		// Find a room to which the non-RS user can go from the current door.
		int numberDoorStairsConnected = dataAccessGraphFile.getNumberOfConnectedDoorStairs();
		for (int ds = 1; ds <= numberDoorStairsConnected; ds++) {
			String doorStairsConnected = dataAccessGraphFile.getConnectedDoorStairs(ds);
			String[] array = doorStairsConnected.split(", ");
			String stairs = array[0];
			String door = array[1];
			
			if (dataAccessGraphFile.getDoorOfRoom(door) == currentDoor) {
				// System.out.println("Door " + door + " (" + currentDoor + ") is connected to stairs: " + stairs);
				stairsList.add(dataAccessGraphFile.getStairsOfRoom(stairs));
			}
		}
		
		/*
		if (stairsList.size() <= 0) {
			MainMuseumSimulator.printConsole("NO DOOR NOR STAIRS CONNECTED TO: " + currentDoor, Level.SEVERE);
			System.out.println("NO DOOR NOR STAIRS CONNECTED TO: " + currentDoor);
		}
		*/
		
		return stairsList;
	}
	
	/**
	 * Checks if two doors are connected by stairs (changing floor, adding time on stairs).
	 * 
	 * @param startVertex
	 * @param endVertex
	 * @return
	 */
	public boolean checkDoorsConnectedByStairs(long startVertex, long endVertex) {
		
		boolean connected = false;
		DataAccessItemFile accessItemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
		int numDoors = 0;
		int numberOfRooms = dataAccessGraphFile.getNumberOfRoom();
		for (int i = 1; i <= numberOfRooms; i++) {
			numDoors += dataAccessGraphFile.getNumberOfDoorsByRoom(i);
		}
		int numberOfItems = accessItemFile.getNumberOfItems();
		int idInvisibleDoors = numberOfItems + numDoors + dataAccessGraphFile.getNumberOfStairs() + 1;
		
		if (startVertex > numberOfItems && startVertex < idInvisibleDoors && endVertex > numberOfItems && endVertex < idInvisibleDoors) { // They are doors
			List<Long> connectedStairsStartVertex = getConnectedStairs(startVertex);
			List<Long> connectedStairsEndVertex = getConnectedStairs(endVertex);
			
			if (connectedStairsStartVertex.size() > 0 && connectedStairsEndVertex.size() > 0) {
				
				/*
				for (Long lStart: connectedStairsStartVertex) {
					for (Long lEnd: connectedStairsEndVertex) {
						if (lStart == lEnd) {
							System.out.println("* " + lStart + "is connected to " + lEnd);
							connected = true;
							break;
						}
							
					}
					if (connected)
						break;
				}
				*/
				
				connected = true;
				
				return connected;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	// Añadido por Nacho Palacio 2025-04-24
	private String convertPathIdsToExternal(String path) {
		if (path == null || path.isEmpty())
			return path;
			
		StringBuilder externalPath = new StringBuilder();
		String[] edges = path.split(", ");
		
		for (String edge : edges) {
			if (edge.trim().isEmpty())
				continue;
				
			String[] vertices = cleanEdge(edge);
			if (vertices.length == 2) {
				long v1 = Long.parseLong(vertices[0]);
				long v2 = Long.parseLong(vertices[1]);
				
				// Convertir a IDs externos si son internos
				if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
					v1 = ElementIdMapper.getBaseId(v1);
				} else if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_DOOR)) {
					v1 = ElementIdMapper.getBaseId(v1);
				}
				
				if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_ITEM)) {
					v2 = ElementIdMapper.getBaseId(v2);
				} else if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_DOOR)) {
					v2 = ElementIdMapper.getBaseId(v2);
				}
				
				externalPath.append("(").append(v1).append(" : ").append(v2).append("), ");
			}
		}
		
		return externalPath.toString();
	}

	// Añadido por Nacho Palacio 2025-05-08
	private void verifyThresholdSimilarity(DBDataModel dataModel) {
		// Probar con diferentes umbrales para ver cuándo comienza a encontrar vecinos
		double[] thresholds = {0.5, 0.4, 0.3, 0.2, 0.1, 0.05, 0.01};
		for (double threshold : thresholds) {
			try {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
				UserNeighborhood neighborhood = 
					new ThresholdUserNeighborhood(threshold, similarity, dataModel);
				
				long[] neighbors = neighborhood.getUserNeighborhood(176);
				if (neighbors != null && neighbors.length > 0) {
					break;
				}
			} catch (Exception e) {
				System.out.println("THRESHOLD: Error: " + e.getMessage());
			}
		}
	}
}
