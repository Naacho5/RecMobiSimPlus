package es.unizar.gui.simulation;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

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
import es.unizar.epidemic.data.ContactTrajectoryBuilder;
import es.unizar.epidemic.data.InterCliqueCoincidenceTracker;
import es.unizar.epidemic.models.AbstractEpidemicModel;
import es.unizar.epidemic.models.EpidemicModel;
import es.unizar.epidemic.models.LelieveldTransmissionModel;
import es.unizar.epidemic.models.PengTransmissionModel;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.UserInfo;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.graph.GraphForSpecialUser;
import es.unizar.recommendation.ExhaustiveRecommendation;
import es.unizar.recommendation.IdealRecommendation;
import es.unizar.recommendation.RandomRecommendation;
import es.unizar.recommendation.contextaware.trajectory.ShortestTrajectoryStrategy;
import es.unizar.recommendation.contextaware.trajectory.TrajectoryPostfilteringBasedRecommendation;
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
import es.unizar.epidemic.statistics.EpidemicRiskCalculator;
import es.unizar.recommendation.path.ContactBasedPath;
import es.unizar.recommendation.RiskAwareRecommendation;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.general.HealthStatus;
import es.unizar.epidemic.general.UserEpidemicExtension;

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
	public double kmToPixel; // =6597;
	private int ttl; // Propagation of items: =4000; // Options: 180; // 900; //1800; //2700; seconds
	private int timeOnStairs; // =60;
	private int minimumTimeToUpdateRecommendation; // =30;
	private int communicationRange; // =250
	private int maxKnowledgeBaseSize; // =1;
	private int communicationBandwidth; // =54;
	private int latencyOfTransmission; // =1;
	private int timeToChangeMood; // =1800; // 1800 = 30 minutes
	private Seed simulationSeed; // if specified in config, that seed value; if not specified, System.currentTimeMillis()
	private static final int BATCH_SIZE = 50;
	private static final int GC_INTERVAL = 100;
	private int simulationIterationCounter = 0;

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
	public es.unizar.epidemic.general.EpidemicSimulationManager epidemicManager; // Added by Nacho Palacio 2025-07-15
	boolean manualSimulation = true;
	private int maxUsersForTest = -1;

	public es.unizar.epidemic.data.InterCliqueCoincidenceTracker coincidenceTracker;

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
	public static Map<Integer, List<Long>> itemObservedOfUsers = new HashMap<>();
	public static Map<Integer, List<String>> actualPathTraveled = new HashMap<>();
	public Map<Integer, List<Float>> userRatings = new HashMap<>();
	public List<String> path;
	public String locationStartVertex;
	public HashMap<Long, Integer> countItemsTTPByUser;

	public double[] availableTimeOfUsers;
	public int[] currentTimeOfUsers;
	public int[] moodOfUsers;

	public int[] userTimesUpdatedPath;
	
	/**
	 * Statistics.
	 */
	public Map<Long, PredictedRatingsInfo> predictedRatings;
	public Set<Long> idUsersWatchingSameItem;
	public List<DistancesBetweenUsersAndTime> distancesBetweenUsers;
	public List<DistancesBetweenUsersAndTime> completedDistancesBetweenUsers;
	public Map<Pair<Integer, Integer>, Double> timeUsersInRoom = new HashMap<>();
	public Map<Integer, Pair<Integer, Long>> userCurrentRoomEntry = new HashMap<>();


	/*
	 * Contacts.
	 */
	public Map<Integer, List<Integer>> cliqueUserMapping;  // Mapping clique -> users
    public Map<Integer, Integer> initialSusceptiblesByClique; // Mapping clique -> initial susceptibles
	
    private static final int EVENT_DURATION_SECONDS = 240;
    private static final int NUM_ROOMS = 26;
    private Map<Integer, Integer> userToCliqueMap;
	private boolean mixCliqueAndIndependentUsers = false;
    private double independentUserRatio = 0.3; // 30% by default
    

	/*
	 * Cached objects for optimization.
	 */
	private ShortestTrajectoryStrategy cachedTrajectoryStrategy = null;
    private SimpleWeightedGraph<Long, DefaultWeightedEdge> lastUsedGraph = null;

	
	// =========== Logger ================================:
	public static final Logger log = Logger.getLogger(Literals.DEBUG_MESSAGES);
	public static final Logger logRecommender = Logger.getLogger("RECOMMENDER");

	public Simulation() {}

	public Simulation(int timeAvailableUser, int delayObservingPainting, double timeForIteration, double screenRefreshTime, double timeForThePaths, double userVelocity, double kmToPixel, int ttl,
			int timeOnStairs, int minimumTimeToUpdateRecommendation, int communicationRange, int maxKnowledgeBaseSize, int communicationBandwidth, int latencyOfTransmission, int numberOfSpecialUser,
			int numberOfNonSpecialUser, String nonSpecialUserPaths, String pathStrategy, String recommendationAlgorithm, float thresholdRecommendation, int howMany, String propagationStrategy,
			double probabilityUserDisobedience, int numberVoteReceived, double thresholdSimilarity, String networkType, int timeToChangeMood, boolean useFixedSeed, long seed, boolean manualSimulation, 
			boolean mixCliqueAndIndependent, double independentRatio) {
		
		MainSimulator.printConsole("Creating simulation", Level.WARNING);
		currentTime();
		
		// Logger configuration 
		logRecommender.setUseParentHandlers(false);
		// logRecommender.setLevel(Literals.DEBUG_DEFAULT_LEVEL);

		log.setLevel(Level.OFF);
		logRecommender.setLevel(Level.OFF);
		
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
		System.out.println("delayObservingPainting: " + delayObservingPainting);
		
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
		System.out.println("Number of special users: " + numberOfSpecialUser);
		System.out.println("Number of non-special users: " + numberOfNonSpecialUser);

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

		this.userTimesUpdatedPath = new int[this.numberOfUser]; // DEBUG

		this.countItemsTTPByUser = new HashMap<>();
		for (int userPosition = 1; userPosition <= userList.size(); userPosition++) {
			countItemsTTPByUser.put((long) userPosition, 1);
		}

		Simulation.actualPathTraveled = new HashMap<>();
		for (int i = 1; i <= numberOfUser; i++) {
			actualPathTraveled.put(i, new ArrayList<>());
		}

		// Create DB managements.
		// Data management user and queue databases:
		dataManagementUserDBList_P2P = new LinkedList<>();
		dataManagementQueueDBList_P2P = new LinkedList<>();
		// Data access in the database of all users.
		try {
			dataInstanceMuseumDB = new Database();
			dataModelMuseumDB = new DBDataModel(Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH, dataInstanceMuseumDB, this.numberOfUser-1);
			System.out.println("Creating dataModelMuseumDB with path: " + Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH);
		} catch (SQLException ex) {
			Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Build a graph for the RS user.
		graphSpecialUser = new GraphForSpecialUser();
		
		// Object to access to data from graph file (GRAPH_FLOOR_COMBINED):
		dataAccessGraphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));

		// Get the number of items from dataAccessItemFile (in graph RS user)
		this.numberOfITems = graphSpecialUser.accessItemFile.getNumberOfItems();
		System.out.println("number of items in museum: " + this.numberOfITems);
		
		
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

		this.epidemicManager = new es.unizar.epidemic.general.EpidemicSimulationManager(); // Added by Nacho Palacio 2025-07-15

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\n=== Shutdown Hook: Finalizing EpidemicStatistics ===");
			
			try {
				es.unizar.epidemic.statistics.EpidemicStatistics.getInstance().endSimulation();

				EpidemicModel epidemicModel = this.epidemicManager.getEpidemicModel();
				if (epidemicModel != null) {
					System.out.println("\n🔒 Cerrando writer de iteraciones...");
					((AbstractEpidemicModel) epidemicModel).closeIterationsWriter();
				}
				System.out.println("\n--- SIMULATION FINISHED: DURATION COMPLETED, WRITER CLOSED ---");

			} catch (Exception e) {
				System.err.println("Warning! Error finalizando EpidemicStatistics en ShutdownHook: " + e.getMessage());
			}
		}));

		this.simulationIterationCounter = 0;
		this.manualSimulation = manualSimulation;

		this.mixCliqueAndIndependentUsers = mixCliqueAndIndependent;
        this.independentUserRatio = independentRatio;
        
        System.out.println(" Mixed mode: " + 
            (mixCliqueAndIndependentUsers ? "ENABLED" : "DISABLED"));
        if (mixCliqueAndIndependentUsers) {
            System.out.println("    Independent users ratio: " + 
                (independentUserRatio * 100) + "%");
        }
	}


    ////////////////////////////////////////////////////////
	// USER INITIALIZATION METHODS

	/**
	 * Initializes the initial position of users using real contact trajectories or traditional initialization.
	 * Supports three modes: simplified rotation (circular room rotation), complex real events (CSV-based),
	 * and traditional path-based initialization. Automatically falls back to traditional mode if
	 * contact trajectory modes fail.
	 */
	public void initializeUsers() {
		MainSimulator.printConsole("Initializing users: ", Level.WARNING);

		if (mixCliqueAndIndependentUsers) {
			System.out.println("\n" + "=".repeat(80));
			System.out.println(" MIXED MODE ACTIVATED");
			System.out.println("=".repeat(80));
			
			try {
				initializeUsersWithMixedMode();
				System.out.println("   ✅ Users initialized in mixed mode");
				return;
			} catch (Exception e) {
				System.err.println("    Error in mixed mode: " + e.getMessage());
				e.printStackTrace();
				System.out.println("    Fallback to traditional initialization...");
			}
		}

		Configuration.ContactTrajectoryMode mode = Configuration.ContactTrajectoryMode.DISABLED;
		
		if (Configuration.instance != null) {
			mode = Configuration.instance.getContactTrajectoryMode();
		}
		
		System.out.println("\n" + "=".repeat(80));
		System.out.println(" INITIALIZING USERS");
		System.out.println("    Selected mode: " + mode.getDisplayName());
		System.out.println("=".repeat(80));
		
		switch (mode) {
			case SIMPLIFIED_ROTATION:
				System.out.println("    Using SIMPLIFIED model (circular rotation)");
				try {
					ContactTrajectoryBuilder.resetMappings();
					initializeUsersWithSimplifiedRotation();
					return;
				} catch (Exception e) {
					System.err.println("Warning! Error in simplified model: " + e.getMessage());
					e.printStackTrace();
					System.err.println("   Fallback to traditional mode...");
				}
				break;
				
			case COMPLEX_REAL_EVENTS:
				System.out.println("    Using COMPLEX model (real events from CSV)");
				try {
					ContactTrajectoryBuilder.resetMappings();
					initializeUsersWithComplexEvents();
					return;
				} catch (Exception e) {
					System.err.println("Warning! Error in complex model: " + e.getMessage());
					e.printStackTrace();
					System.err.println("   Fallback to traditional mode...");
				}
				break;
				
			case DISABLED:
			default:
				System.out.println("    Using TRADITIONAL mode");
				break;
		}

		initializeUsersTraditional();
		System.out.println("   ✅ Users initialized traditionally");
	}

	/**
	 * It initializes the initial position of users.
	 */
	public void initializeUsersTraditional() {
		MainSimulator.printConsole("Initializing users: ", Level.WARNING);

		// Get the non-special and RS user paths. The non-RS user path is obtained from generated path file (e.g., nearest_non_special_user_paths.txt), by using the strategy (Nearest,
		// Random or Exhaustive) specified in the Configuration form. While the RS user path (initially null) is generated with the recommender specified in the Configuration form.
		
		graphSpecialUser.getPathsFromFile();
		String edge = null;
		for (int i = 0; i < userList.size(); i++) {
			User currentUser = userList.get(i);
			if (currentUser.isSpecialUser) {
				// Gets the randomly door where RS users will enter.
				long startVertex = dataAccessGraphFile.getRandomDoor();

				// The RS user path is updated with the hybrid recommendation algorithm.
		
				// Added by Nacho Palacio 2025-06-10
				if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_DOOR)) {
					long externalStartVertex = ElementIdMapper.getBaseId(startVertex);
					startVertex = externalStartVertex;
				}
				
				updateSpecialUserPath(startVertex, startVertex, false, 0, false, currentUser);
			}

			path = graphSpecialUser.paths.get(i);

			// Added by Nacho Palacio 2025-04-24
			if (path == null || path.isEmpty() || (path.size() == 1 && path.get(0).isEmpty())) {
				// Default route
				path = new ArrayList<>();
				path.add("(1 : 2)");
				graphSpecialUser.paths.set(i, path);
			}

			MainSimulator.printConsole("Path of user " + (i + 1) + ": " + path, Level.WARNING);
			// Get the current edge.
			edge = path.get(this.userPositionInPath[i]);

			if (edge != null) {
				String[] array = cleanEdge(edge);

				// Get the vertices.
				long v1 = Long.valueOf(array[0]).longValue();

				// Added by Nacho Palacio 2025-06-09
				long v1External = v1;
				if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
					v1External = ElementIdMapper.getBaseId(v1);
				} else if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_DOOR)) {
					v1External = ElementIdMapper.getBaseId(v1);
				}	

				// Gets the position user where he/she will start the simulation.
				currentUser.getRoomOfTheUser();
				// Stores the initial location of the current user.
				locationNextIteration[i] = MainSimulator.floor.diccionaryItemLocation.get(v1External);

				if (locationNextIteration[i] == null && i > 0) {
					System.out.println("Warning! Null location for user " + currentUser.userID + ". Using previous user's location.");
					locationNextIteration[i] = locationNextIteration[i-1];
				}

				// Initialize the user start position.
				currentUser.move(locationNextIteration[i], currentUser.room);
			}
		}

		if (manualSimulation) {
			epidemicManager.initializeEpidemicSystem(getAllUsers()); // Added by Nacho Palacio 2025-07-15
		}
	}
	
	/**
     * Initializes users with simplified contact trajectories using circular room rotation model.
     * Selects users from complete cliques, builds user-to-clique mappings, generates synthetic
     * room events with circular rotation pattern, creates paths from events, and initializes
     * epidemic system with one infected user per clique.
     * Added by Nacho Palacio 2025-12-03
     * 
     * @throws Exception if cliques file not found or initialization fails
     */
    private void initializeUsersWithSimplifiedRotation() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" INITIALIZATION WITH SIMPLIFIED MODEL (CIRCULAR ROTATION)");
        System.out.println("=".repeat(80));
        
        String cliquesJsonPath = "../src/es/unizar/epidemic/data/cliques.json";
        
        java.io.File cliquesFile = new java.io.File(cliquesJsonPath);
        if (!cliquesFile.exists()) {
            System.err.println(" Cliques file not found: " + cliquesJsonPath);
            throw new RuntimeException("Cliques file not found");
        }
        
        System.out.println("\n STEP 1: Selecting users from cliques...");
        ContactTrajectoryBuilder.SelectedUsersResult selectionResult = 
            ContactTrajectoryBuilder.selectUsersFromCompleteCliquesWithCliques(
                cliquesJsonPath, 
                this.numberOfUser
            );
        
        if (selectionResult == null || selectionResult.cliques == null) {
            throw new RuntimeException("Error selecting cliques");
        }
        
        List<List<String>> selectedCliques = selectionResult.cliques;
        System.out.println("   ✅ Selected cliques: " + selectedCliques.size());
        System.out.println("   ✅ Selected users: " + selectionResult.users.size());
        
        System.out.println("\n STEP 2: Building user -> clique mapping...");
        this.userToCliqueMap = ContactTrajectoryBuilder.buildUserToCliqueMapFromSelectedCliques(
            selectedCliques);
        System.out.println("   ✅ Mapped users: " + userToCliqueMap.size());
        
        System.out.println("\n STEP 3: Generating events with circular rotation...");
        long simulationDuration = (long) this.timeAvailableUser * 3600;
        
        Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> userTrajectories = 
            ContactTrajectoryBuilder.buildUserRoomEvents(
                userToCliqueMap,
                simulationDuration,
                EVENT_DURATION_SECONDS,
                NUM_ROOMS
            );
        
        System.out.println("   ✅ Generated trajectories: " + userTrajectories.size());
        
        int userLimit = Math.min(this.numberOfUser, userTrajectories.size());
        
        if (this.availableTimeOfUsers == null || this.availableTimeOfUsers.length != userLimit) {
            this.availableTimeOfUsers = new double[userLimit];
            Arrays.fill(availableTimeOfUsers, 0.0);
            
            this.currentTimeOfUsers = new int[userLimit];
            Arrays.fill(currentTimeOfUsers, 0);
            
            this.moodOfUsers = new int[userLimit];
            initializeMoodOfUsers();
            
            this.userPositionInPath = new int[userLimit];
            this.itemsBeingWatched = new long[userLimit];
            this.locationNextIteration = new String[userLimit];
            this.voting = new boolean[userLimit];
        }
        
        if (this.userList == null) {
            this.userList = new ArrayList<>();
        }
        
        if (this.userList.size() != userLimit) {
            this.userList.clear();
            for (int i = 1; i <= userLimit; i++) {
                this.userList.add(new User(i, false));
            }
            System.out.println("   ✅ Created " + userLimit + " users");
        }
        
        System.out.println("\n STEP 6: Configuring ElementIdMapper...");
        configureElementIdMapperForCurrentScenario();
        
        System.out.println("\n STEP 7: Assigning trajectories and generating paths...");
        
        int pathsGenerated = 0;
        int pathsSkipped = 0;
        
        while (graphSpecialUser.paths.size() < userLimit) {
            graphSpecialUser.paths.add(new LinkedList<String>());
        }
        
        for (Map.Entry<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> entry : 
                userTrajectories.entrySet()) {
            
            int userId = entry.getKey();
            
            if (userId <= 0 || userId > userList.size()) {
                pathsSkipped++;
                continue;
            }
            
            User user = userList.get(userId - 1);
            
            if (user == null) {
                pathsSkipped++;
                continue;
            }
            
            List<ContactTrajectoryBuilder.UserRoomEvent> events = entry.getValue();
            user.setContactTrajectory(events);
            
            try {
                ContactBasedPath path = new ContactBasedPath(
                    userTrajectories, 
                    userId
                );
                
                int firstRoom = events.get(0).roomId;
                long startDoorId = -1;
                
                try {
                    int numDoors = path.accessGraphFile.getNumDoorsByRoom(firstRoom);
                    if (numDoors > 0) {
                        startDoorId = path.accessGraphFile.getDoorOfRoomWithIndex(1, firstRoom);
                    } else {
                        int numItems = path.accessGraphFile.getNumberOfItemsByRoom(firstRoom);
                        if (numItems > 0) {
                            startDoorId = path.accessGraphFile.getItemOfRoom(1, firstRoom);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("       Error obtaining initial vertex: " + e.getMessage());
                }
                
                String completePath = "";
                if (startDoorId > 0) {
                    completePath = path.generatePath(startDoorId);
				}
                
                if (completePath != null && !completePath.isEmpty()) {
                    user.pathList = pathStringToList(completePath);
                    user.pathString = completePath;
                    
                    if (user.pathList != null && !user.pathList.isEmpty()) {
                        String firstEdge = user.pathList.get(0);
                        String[] edgeParts = cleanEdge(firstEdge);
                        
                        if (edgeParts.length >= 2) {
                            try {
                                long firstVertex = Long.parseLong(edgeParts[0]);
                                String vertexLocation = getVertexLocation(firstVertex);
                                
                                if (vertexLocation != null && !vertexLocation.isEmpty()) {
                                    String[] xy = vertexLocation.split(", ");
                                    if (xy.length >= 2) {
                                        user.x = Double.parseDouble(xy[0]);
                                        user.y = Double.parseDouble(xy[1]);
                                        
                                        int userIndex = userId - 1;
                                        locationNextIteration[userIndex] = vertexLocation;
										user.move(vertexLocation, firstRoom);
                                    }
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                    
                    int userIndex = userId - 1;
                    graphSpecialUser.paths.set(userIndex, user.pathList);
                    
                    pathsGenerated++;
                } else {
					System.out.println("       User " + userId + " could not generate path, using fallback");
                    user.pathList = createFallbackPath(userId);
                    user.pathString = String.join(", ", user.pathList);
                    graphSpecialUser.paths.set(userId - 1, user.pathList);
                    pathsSkipped++;
                }
                
            } catch (Exception e) {
                user.pathList = createFallbackPath(userId);
                user.pathString = String.join(", ", user.pathList);
                graphSpecialUser.paths.set(userId - 1, user.pathList);
                pathsSkipped++;
            }
        }
        
        System.out.println("   ✅ Paths generados: " + pathsGenerated);
        System.out.println("   Warning! Paths con fallback: " + pathsSkipped);
        
        System.out.println("\n STEP 8: Initializing coincidence tracker...");
        this.cliqueUserMapping = buildCliqueUserMappingFromSelectedCliques(selectedCliques);
        this.coincidenceTracker = new es.unizar.epidemic.data.InterCliqueCoincidenceTracker(
            cliqueUserMapping
        );
        System.out.println("   ✅ Tracker initialized");
        
        System.out.println("\n STEP 9: Initializing epidemic system...");
        epidemicManager.initializeEpidemicSystem(getAllUsers());
        infectOneUserPerClique(selectedCliques);
        recordInitialSusceptiblesByClique();
        System.out.println("   ✅ Epidemic system initialized");
        
        printSimplifiedAssignmentSummary(selectedCliques, userTrajectories);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ INITIALIZATION COMPLETED (SIMPLIFIED MODEL)");
        System.out.println("=".repeat(80) + "\n");
    }

	/**
	 * Initializes users with real contact trajectories from CSV file.
	 * Uses actual contact events extracted from "contactosFinal.csv" to build
	 * realistic user trajectories based on observed clique behavior. Selects
	 * complete cliques, maps users to cliques, and generates paths from real events.
	 * 
	 * @throws Exception if CSV or JSON files not found, or initialization fails
	 */
	private void initializeUsersWithComplexEvents() throws Exception {
		System.out.println("\n" + "=".repeat(80));
		System.out.println(" COMPLEX MODEL: REAL EVENTS FROM CSV");
		System.out.println("=".repeat(80));

		
		String csvPath = "/home/nacho/universidad/cuarto/TFG/RecMobiSimPlus/CodigoOriginal/src/es/unizar/epidemic/data/contactosFinal.csv";
		String cliquesJsonPath = "/home/nacho/universidad/cuarto/TFG/RecMobiSimPlus/CodigoOriginal/src/es/unizar/epidemic/data/cliques.json";
		
		File csvFile = new File(csvPath);
		File jsonFile = new File(cliquesJsonPath);
		
		if (!csvFile.exists()) {
			throw new FileNotFoundException("Contacts CSV not found: " + csvPath);
		}
		if (!jsonFile.exists()) {
			throw new FileNotFoundException("Cliques JSON not found: " + cliquesJsonPath);
		}
		
		System.out.println("\n STEP 1: Files verified");
		System.out.println("   - CSV: " + csvFile.getAbsolutePath());
		System.out.println("   - JSON: " + jsonFile.getAbsolutePath());
		
		System.out.println("\n STEP 2: Selecting cliques for " + numberOfUser + " users...");
		
		ContactTrajectoryBuilder.SelectedUsersResult result = 
			ContactTrajectoryBuilder.selectUsersFromCompleteCliquesWithCliques(cliquesJsonPath, numberOfUser);
		
		Set<Integer> priorityUsers = result.users;
		List<List<String>> selectedCliques = result.cliques;
		
		if (selectedCliques == null || selectedCliques.isEmpty()) {
			throw new Exception("Could not load cliques from: " + cliquesJsonPath);
		}
		
		System.out.println("   ✅ Users selected: " + priorityUsers.size());
		System.out.println("   ✅ Cliques selected: " + selectedCliques.size());
		
		System.out.println("\n STEP 3: Building user-clique mapping...");
		Map<Integer, Integer> userToCliqueMapLocal = new HashMap<>();
		
		for (int cliqueIndex = 0; cliqueIndex < selectedCliques.size(); cliqueIndex++) {
			List<String> clique = selectedCliques.get(cliqueIndex);
			
			for (String userIdStr : clique) {
				try {
					int realUserId = Integer.parseInt(userIdStr);
					int simId = ContactTrajectoryBuilder.getSimulationId(realUserId);
					userToCliqueMapLocal.put(simId, cliqueIndex);
				} catch (NumberFormatException e) {
					System.err.println("   Warning! Invalid ID in clique " + (cliqueIndex+1) + ": " + userIdStr);
				}
			}
		}
		
		this.userToCliqueMap = userToCliqueMapLocal;
		
		System.out.println("   ✅ Users mapped: " + userToCliqueMapLocal.size());
		
		System.out.println("\n STEP 4: Building trajectories from CSV...");
		System.out.println("    Rooms are taken DIRECTLY from CSV (zona_basica)");
		
		Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> userTrajectories = 
			ContactTrajectoryBuilder.buildUserRoomEventsFromCSV(
				csvPath, 
				numberOfUser, 
				priorityUsers,
				null
			);
		
		System.out.println("    Trajectories generated for priority users: " + priorityUsers.size());
		for (Integer userTrajectory : userTrajectories.keySet()) {
			System.out.print(userTrajectory + " ");
		}
		System.out.println();

		// Added by Nacho Palacio 2025-12-27 to avoid rooms without items
		for (Map.Entry<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> entry : userTrajectories.entrySet()) {
			List<ContactTrajectoryBuilder.UserRoomEvent> events = entry.getValue();
			if (events == null || events.isEmpty()) continue;

			int roomId = events.get(0).roomId;
			boolean allSameRoom = events.stream().allMatch(e -> e.roomId == roomId);

			// If it has no items, events are reassigned to another room with items
			int numItems = graphSpecialUser.accessGraphFile.getNumberOfItemsByRoom(roomId);
			if (allSameRoom && numItems == 0) {
				int numRooms = graphSpecialUser.accessGraphFile.getNumberOfRoom();
				int newRoomId = (roomId % numRooms) + 1;
				int tries = 0;
				while (graphSpecialUser.accessGraphFile.getNumberOfItemsByRoom(newRoomId) == 0 && newRoomId != roomId && tries < numRooms) {
					newRoomId = (newRoomId % numRooms) + 1;
					tries++;
				}
				for (ContactTrajectoryBuilder.UserRoomEvent e : events) {
					e.roomId = newRoomId;
				}
				System.out.println("Warning! User " + entry.getKey() + ": all events were in a room without items (" + roomId + "). Reassigned to " + newRoomId);
			}
		}
		
		if (userTrajectories == null || userTrajectories.isEmpty()) {
			throw new Exception("Could not generate trajectories from CSV");
		}
		
		System.out.println("   ✅ Trajectories generated: " + userTrajectories.size() + " users");
		
		System.out.println("\n STEP 5: Complex model - spatial validator DISABLED");
		System.out.println("    Las habitaciones vienen directamente del CSV");
		
		System.out.println("\n STEP 6: Generating navigable paths...");

		System.out.println("userList IDs:");
		for (User u : userList) System.out.print(u.userID + " ");
		System.out.println("\nuserTrajectories keys:");
		for (Integer uid : userTrajectories.keySet()) System.out.print(uid + " ");
		System.out.println();

		int pathsGenerated = 0;
		int pathsSkipped = 0;

		while (graphSpecialUser.paths.size() < userTrajectories.size()) {
			graphSpecialUser.paths.add(new LinkedList<String>());
		}

		for (Map.Entry<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> entry : userTrajectories.entrySet()) {
			int userId = entry.getKey();
			
			if (userId <= 0 || userId > userList.size()) {
				System.err.println("   Warning! userId " + userId + " out of range [1-" + userList.size() + "]");
				pathsSkipped++;
				continue;
			}
			
			User user = userList.get(userId - 1);
			
			if (user == null) {
				System.err.println("   Warning! User " + userId + " is null in array");
				pathsSkipped++;
				continue;
			}
			
			List<ContactTrajectoryBuilder.UserRoomEvent> events = entry.getValue();
			user.setContactTrajectory(events);
			
			try {
				ContactBasedPath path = new ContactBasedPath(
					userTrajectories, 
					userId
				);

				int firstRoom = events.get(0).roomId;
				long startDoorId = -1;
				
				try {
					int numDoors = path.accessGraphFile.getNumDoorsByRoom(firstRoom);
					if (numDoors > 0) {
						startDoorId = path.accessGraphFile.getDoorOfRoomWithIndex(1, firstRoom);
					} else {
						int numItems = path.accessGraphFile.getNumberOfItemsByRoom(firstRoom);
						if (numItems > 0) {
							startDoorId = path.accessGraphFile.getItemOfRoom(1, firstRoom);
						}
					}
				} catch (Exception e) {
					System.err.println("       Error obtaining initial vertex: " + e.getMessage());
				}
				
				String completePath = "";
				if (startDoorId > 0) {
					completePath = path.generatePath(startDoorId);
				}

				if (completePath != null && !completePath.isEmpty()) {
					user.pathList = pathStringToList(completePath);
					user.pathString = completePath;
					
					if (user.pathList != null && !user.pathList.isEmpty()) {
						String firstEdge = user.pathList.get(0);
						String[] edgeParts = cleanEdge(firstEdge);
						
						if (edgeParts.length >= 2) {
							try {
								long firstVertex = Long.parseLong(edgeParts[0]);
								String vertexLocation = getVertexLocation(firstVertex);
								
								if (vertexLocation != null && !vertexLocation.isEmpty()) {
									String[] xy = vertexLocation.split(", ");
									if (xy.length >= 2) {
										user.x = Double.parseDouble(xy[0]);
										user.y = Double.parseDouble(xy[1]);
										
										int userIndex = userId - 1;
										locationNextIteration[userIndex] = vertexLocation;
										user.move(vertexLocation, user.room);
									}
								}
							} catch (NumberFormatException e) {
								System.err.println("    Error parsing initial vertex: " + e.getMessage());
							}
						}
					}
					else {
						System.out.println("   Warning! User " + userId + " pathList empty after generating path");
					}

					System.out.printf(
						"User %3d | Initial room: %3d | Initial position: (%.2f, %.2f)\n",
						user.userID,
						(events != null && !events.isEmpty() ? events.get(0).roomId : -1),
						user.x,
						user.y
					);
					
					int userIndex = userId - 1;
					
					if (locationNextIteration[userIndex] == null || locationNextIteration[userIndex].isEmpty()) {
						String fallbackLocation = getFallbackLocationForRoom(firstRoom);
						locationNextIteration[userIndex] = fallbackLocation;
						
						try {
							String[] xy = fallbackLocation.split(", ");
							if (xy.length >= 2) {
								user.x = Double.parseDouble(xy[0]);
								user.y = Double.parseDouble(xy[1]);
							}
						} catch (Exception e) {
							user.x = 500.0;
							user.y = 500.0;
						}
						
						System.out.println("    User " + userId + ": locationNextIteration initialized to " + fallbackLocation);
					}
					
					if (user.pathList == null || user.pathList.isEmpty()) {
						user.pathList = createFallbackPath(userId);
						user.pathString = String.join(", ", user.pathList);
					}
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
					pathsGenerated++;
					
				} else {
					System.err.println("    Error generating path for user " + userId + ", using fallback");

					user.pathList = createFallbackPath(userId);
					user.pathString = String.join(", ", user.pathList);
					
					int userIndex = userId - 1;
					// Fallback
					if (locationNextIteration[userIndex] == null) {
						locationNextIteration[userIndex] = "500.0, 500.0";
						user.x = 500.0;
						user.y = 500.0;
					}
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
					pathsSkipped++;
				}
				
			} catch (Exception e) {
				System.err.println("    Error generando path para usuario " + userId + ": " + e.getMessage());
				
				user.pathList = createFallbackPath(userId);
				user.pathString = String.join(", ", user.pathList);
				
				int userIndex = userId - 1;
				
				// Fallback
				if (locationNextIteration[userIndex] == null) {
					locationNextIteration[userIndex] = "500.0, 500.0";
					user.x = 500.0;
					user.y = 500.0;
				}
				
				graphSpecialUser.paths.set(userIndex, user.pathList);
				pathsSkipped++;
			}
		}

		System.out.println("\n    Paths generados: " + pathsGenerated + "/" + userTrajectories.size());
		if (pathsSkipped > 0) {
			System.out.println("   Warning! Paths con fallback: " + pathsSkipped);
		}
		
		System.out.println("\n STEP 7: Infecting one user per clique...");
		
		try {
			infectOneUserPerClique(selectedCliques);
			
			this.cliqueUserMapping = buildCliqueUserMappingFromSelectedCliques(selectedCliques);
			recordInitialSusceptiblesByClique();
			
			System.out.println("   ✅ Initial infection completed");
		} catch (Exception e) {
			System.err.println("   Warning! Error in initial infection: " + e.getMessage());
		}

		initializeCoincidenceTracker();
		
		System.out.println("\n" + "=".repeat(80));
		System.out.println("✅ INITIALIZATION COMPLETED (COMPLEX MODEL)");
		System.out.println("   - Users: " + pathsGenerated);
		System.out.println("   - Cliques: " + selectedCliques.size());
		System.out.println("   - Spatial validator: DISABLED (CSV rooms)");
		System.out.println("   - Coincidence tracker: ACTIVE");
		System.out.println("=".repeat(80) + "\n");
	}

	/**
	 * Initializes users with mixed clique and independent mode.
	 * Combines clique-based users (who move in groups) with independent users
	 * (who move individually). The ratio of independent users is configurable.
	 * Creates separate trajectories for each user type and initializes epidemic
	 * system accordingly.
	 * Added by Nacho Palacio 2025-01-15
	 * 
	 * @throws Exception if initialization of clique or independent users fails
	 */
	private void initializeUsersWithMixedMode() throws Exception {
		System.out.println("\n" + "=".repeat(80));
		System.out.println(" INITIALIZATION IN MIXED MODE");
		System.out.println("=".repeat(80));
		
		int totalUsers = this.numberOfUser;
		int independentUsers = (int) Math.ceil(totalUsers * independentUserRatio);
		int cliqueUsers = totalUsers - independentUsers;
		
		System.out.println("\n USER DISTRIBUTION:");
		System.out.println("   - Total users: " + totalUsers);
		System.out.println("   - Users in cliques: " + cliqueUsers + " (" + 
						String.format("%.1f", (1.0 - independentUserRatio) * 100) + "%)");
		System.out.println("   - Independent users: " + independentUsers + " (" + 
						String.format("%.1f", independentUserRatio * 100) + "%)");
		
		String cliquesJsonPath = "../src/es/unizar/epidemic/data/cliques.json";
		
		File cliquesFile = new File(cliquesJsonPath);
		if (!cliquesFile.exists()) {
			throw new FileNotFoundException("Cliques file not found: " + cliquesJsonPath);
		}
		
		System.out.println("\n STEP 2: Selecting cliques...");
		ContactTrajectoryBuilder.SelectedUsersResult selectionResult = 
			ContactTrajectoryBuilder.selectUsersFromCompleteCliquesWithCliques(
				cliquesJsonPath, 
				cliqueUsers
			);
		
		if (selectionResult == null || selectionResult.cliques == null) {
			throw new Exception("Could not select cliques");
		}
		
		List<List<String>> selectedCliques = selectionResult.cliques;
		System.out.println("   ✅ Cliques selected: " + selectedCliques.size());
		System.out.println("   ✅ Users in cliques: " + selectionResult.users.size());
		
		System.out.println("\n STEP 3: Building user -> clique mapping...");
		Map<Integer, Integer> userToCliqueMapLocal = 
			ContactTrajectoryBuilder.buildUserToCliqueMapFromSelectedCliques(selectedCliques);
		
		this.userToCliqueMap = new HashMap<>(userToCliqueMapLocal);
		System.out.println("   ✅ Users mapped to cliques: " + userToCliqueMapLocal.size());
		
		System.out.println("\n STEP 4: Generating trajectories for clique users...");
		long simulationDuration = (long) this.timeAvailableUser * 3600; // Convertir horas a segundos
		
		Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> cliqueTrajectories = 
			ContactTrajectoryBuilder.buildUserRoomEvents(
				userToCliqueMapLocal,
				simulationDuration,
				EVENT_DURATION_SECONDS,
				NUM_ROOMS
			);
		
		System.out.println("   ✅ Clique trajectories generated: " + cliqueTrajectories.size());
		
		System.out.println("\n STEP 5: Creating independent users...");
		
		Set<Integer> cliqueUserIds = new HashSet<>(cliqueTrajectories.keySet());
		
		int maxCliqueId = cliqueUserIds.stream().max(Integer::compare).orElse(0);
		List<Integer> independentUserIds = new ArrayList<>();
		
		for (int i = 0; i < independentUsers; i++) {
			int independentId = maxCliqueId + 1 + i;
			independentUserIds.add(independentId);
			
			this.userToCliqueMap.put(independentId, -1);
		}
		
		System.out.println("   ✅ Independent user IDs: " + independentUserIds.size());
		System.out.println("      Range: " + (maxCliqueId + 1) + " - " + (maxCliqueId + independentUsers));
		
		int userLimit = cliqueUsers + independentUsers;

		if (this.availableTimeOfUsers == null || this.availableTimeOfUsers.length != userLimit) {
			this.availableTimeOfUsers = new double[userLimit];
			Arrays.fill(this.availableTimeOfUsers, this.timeAvailableUser * 3600.0);
			
			this.currentTimeOfUsers = new int[userLimit];
			Arrays.fill(this.currentTimeOfUsers, 0);
			
			this.moodOfUsers = new int[userLimit];
			initializeMoodOfUsers();
			
			this.userPositionInPath = new int[userLimit];
			Arrays.fill(this.userPositionInPath, 0);
			
			this.itemsBeingWatched = new long[userLimit];
			Arrays.fill(this.itemsBeingWatched, 0L);
			
			this.locationNextIteration = new String[userLimit];
			
			this.voting = new boolean[userLimit];
			Arrays.fill(this.voting, false);
		}
		
		if (this.userList == null) {
			this.userList = new ArrayList<>();
		}
		
		if (this.userList.size() != userLimit) {
			this.userList.clear();
			for (int i = 0; i < userLimit; i++) {
				User user = new User(i+1, false);
				this.userList.add(user);
			}
		}
		
		System.out.println("\n STEP 8: Configuring ElementIdMapper...");
		configureElementIdMapperForCurrentScenario();
		
		System.out.println("\n STEP 9: Generating navigable paths...");
		
		while (graphSpecialUser.paths.size() < userLimit) {
			graphSpecialUser.paths.add(new ArrayList<>());
		}
		
		int pathsGenerated = 0;
		int pathsSkipped = 0;
		
		System.out.println("\n STEP 9.1: Generating paths for clique users...");

		for (Map.Entry<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> entry : 
				cliqueTrajectories.entrySet()) {
			
			int userId = entry.getKey();
			List<ContactTrajectoryBuilder.UserRoomEvent> events = entry.getValue();
			
			if (userId <= 0 || userId > userLimit) {
				System.err.println("    User " + userId + " out of range, skipping...");
				pathsSkipped++;
				continue;
			}
			
			User user = userList.get(userId - 1);
			
			if (user == null) {
				System.err.println("    User " + userId + " is null, skipping...");
				pathsSkipped++;
				continue;
			}
			
			user.setContactTrajectory(events);
			
			try {
				ContactBasedPath path = new ContactBasedPath(
					cliqueTrajectories,
					userId
				);
				
				// Get the start door
				int firstRoom = events.get(0).roomId;
				long startDoorId = -1;
				try {
					int numDoors = path.accessGraphFile.getNumDoorsByRoom(firstRoom);
					if (numDoors > 0) {
						startDoorId = path.accessGraphFile.getDoorOfRoom(1, firstRoom);
					} else {
						System.err.println("    Room " + firstRoom + " without doors");
					}
				} catch (Exception e) {
					System.err.println("    Error obtaining initial door: " + e.getMessage());
				}
				
				String completePath = "";
				if (startDoorId > 0) {
					completePath = path.generatePath(startDoorId);
				}
				
				if (completePath != null && !completePath.isEmpty()) {
					user.pathList = pathStringToList(completePath);
					user.pathString = completePath;
					
					if (user.pathList != null && !user.pathList.isEmpty()) {
						String firstEdge = user.pathList.get(0);
						String[] coords = cleanEdge(firstEdge);
						long v1 = Long.parseLong(coords[0]);
						
						String location = MainSimulator.floor.diccionaryItemLocation.get(v1);
						if (location != null) {
							user.move(location, firstRoom);
							
							System.out.println("   ✅ Usuario " + userId + " posicionado en habitación " + 
											firstRoom + " (" + location + ")");
						} else {
							System.err.println("    Sin ubicación para vértice " + v1);
						}
					}
					
					int userIndex = userId - 1;
					if (locationNextIteration[userIndex] == null || locationNextIteration[userIndex].isEmpty()) {
						locationNextIteration[userIndex] = user.x + ", " + user.y;
					}
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
					pathsGenerated++;
					
				} else {
					System.err.println("    Empty path for user " + userId + ", using fallback");
					
					user.pathList = createFallbackPath(userId);
					user.pathString = String.join(", ", user.pathList);
					
					int userIndex = userId - 1;
					locationNextIteration[userIndex] = "100.0, 100.0";
					user.x = 100.0;
					user.y = 100.0;
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
					pathsSkipped++;
				}
				
			} catch (Exception e) {
				System.err.println("    Error generating path for user " + userId + ": " + e.getMessage());
				e.printStackTrace();
				
				user.pathList = createFallbackPath(userId);
				user.pathString = String.join(", ", user.pathList);
				
				int userIndex = userId - 1;
				locationNextIteration[userIndex] = "100.0, 100.0";
				user.x = 100.0;
				user.y = 100.0;
				
				graphSpecialUser.paths.set(userIndex, user.pathList);
				pathsSkipped++;
			}
		}

		System.out.println("   ✅ Clique paths generated: " + pathsGenerated);
		if (pathsSkipped > 0) {
			System.out.println("    Paths with fallback: " + pathsSkipped);
		}
		
		System.out.println("\n STEP 9.2: Generating random paths for independent users...");

		RandomPath randomPathGenerator = new RandomPath();

		Map<Integer, List<Long>> roomItems = buildRoomItemsMap();
		randomPathGenerator.initializeItemsByRoom(roomItems);

		int independentPathsGenerated = 0;
		for (int independentId : independentUserIds) {
			try {
				User user = userList.get(independentId - 1);
				
				if (user == null) {
					System.err.println("    Independent user " + independentId + " is null");
					continue;
				}
				
				long startDoor = dataAccessGraphFile.getRandomDoor();
				String randomPath = randomPathGenerator.generatePath(startDoor);
				
				if (randomPath != null && !randomPath.isEmpty()) {
					user.pathList = pathStringToList(randomPath);
					user.pathString = randomPath;
					
					if (user.pathList != null && !user.pathList.isEmpty()) {
						String firstEdge = user.pathList.get(0);
						String[] coords = cleanEdge(firstEdge);
						long v1 = Long.parseLong(coords[0]);
						
						int firstRoom = getRoom(v1);
						String location = MainSimulator.floor.diccionaryItemLocation.get(v1);
						
						if (location != null) {
							user.move(location, firstRoom);
							
							System.out.println("   ✅ Independent user " + independentId + 
											" positioned in room " + firstRoom + 
											" (" + location + ")");
						} else {
							// Fallback
							System.err.println("    No location for vertex " + v1 + 
											", using fallback");
							user.x = 100.0;
							user.y = 100.0;
							user.room = 1;
						}
					}
					
					int userIndex = independentId - 1;
					if (locationNextIteration[userIndex] == null || 
						locationNextIteration[userIndex].isEmpty()) {
						locationNextIteration[userIndex] = user.x + ", " + user.y;
					}
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
					independentPathsGenerated++;
					
				} else {
					// Fallback
					System.err.println("    Empty path for independent user " + 
									independentId + ", using fallback");
					
					user.pathList = createFallbackPath(independentId);
					user.pathString = String.join(", ", user.pathList);
					
					int userIndex = independentId - 1;
					locationNextIteration[userIndex] = "100.0, 100.0";
					user.x = 100.0;
					user.y = 100.0;
					user.room = 1;
					
					graphSpecialUser.paths.set(userIndex, user.pathList);
				}
				
			} catch (Exception e) {
				System.err.println("    Error generating path for independent user " + 
								independentId + ": " + e.getMessage());
				e.printStackTrace();
			}
		}

		System.out.println("   ✅ Independent paths generated: " + independentPathsGenerated);
		System.out.println("\n STEP 10: Initializing epidemic system...");
		epidemicManager.initializeEpidemicSystem(getAllUsers());
		
		// Infect one user per clique
		infectOneUserPerClique(selectedCliques);
		
		System.out.println("\n STEP 11: Configuring coincidence tracker...");

		this.cliqueUserMapping = buildCliqueUserMappingFromSelectedCliques(selectedCliques);

		List<Integer> independentUsersList = new ArrayList<>(independentUserIds);
		this.cliqueUserMapping.put(-1, independentUsersList);

		System.out.println("    Users in cliques: " + 
						cliqueUserMapping.values().stream()
							.filter(list -> !list.isEmpty())
							.mapToInt(List::size)
							.sum());
		System.out.println("    Independent users: " + independentUsersList.size());

		this.coincidenceTracker = new InterCliqueCoincidenceTracker(cliqueUserMapping);

		recordInitialSusceptiblesByClique();

		System.out.println("   ✅ Tracker inicializado");
		
		printMixedModeAssignmentSummary(
			cliqueUsers, 
			independentUsers, 
			selectedCliques, 
			independentUserIds
		);
		
		System.out.println("\n" + "=".repeat(80));
		System.out.println("✅ INITIALIZATION IN MIXED MODE COMPLETED");
		System.out.println("=".repeat(80) + "\n");
	}

	
	////////////////////////////////////////////////////////
	// AUXILIARY INITIALIZATION METHODS
	////////////////////////////////////////////////////////
	
	/**
	 * Configures ElementIdMapper for the current scenario.
	 * Analyzes system ranges from graph and item files to set up proper
	 * ID mapping for doors and items. This ensures correct ID translation
	 * between internal and external representations.
	 * Added by Nacho Palacio 2025-10-08
	 */
	public void configureElementIdMapperForCurrentScenario() {
		try {
			DataAccessGraphFile tempGraphFile = new DataAccessGraphFile(new File(es.unizar.util.Literals.GRAPH_FLOOR_COMBINED));
			DataAccessItemFile tempItemFile = new DataAccessItemFile(new File(es.unizar.util.Literals.ITEM_FLOOR_COMBINED));
			
			MainSimulator.SystemRangeData systemData = MainSimulator.analyzeSystemRangesDirectly(tempGraphFile, tempItemFile);

			ElementIdMapper.configureDynamicRanges(systemData);
			
			System.out.println("   ✅ ElementIdMapper configured:");
			System.out.println("      - DOOR_ID_START: " + ElementIdMapper.DOOR_ID_START);
			System.out.println("      - minDoorId: " + systemData.minDoorId);
			System.out.println("      - maxDoorId: " + systemData.maxDoorId);
			
		} catch (Exception e) {
			System.err.println("    Error configuring ElementIdMapper: " + e.getMessage());
			e.printStackTrace();
		}
	}


	////////////////////////////////////////////////////////
	// MAIN SIMULATION ENGINE METHODS
	////////////////////////////////////////////////////////

	/**
	 * Updates the position of users.
	 * 
	 * @param stateOfUsers       Map of user IDs to their states.
	 * @param timeUsersInRooms   Map of (userID, roomID) pairs to time spent.
	 */
	public synchronized void updateUsers(Map<Integer,UserInfo.UserState> stateOfUsers,Map<Pair<Integer,Integer>,Double> timeUsersInRooms) {
		if (graphSpecialUser.paths == null || graphSpecialUser.paths.isEmpty()) {
			System.err.println(" ERROR: graphSpecialUser.paths is NULL or EMPTY");
			System.err.println("   - Size of paths: " + 
							(graphSpecialUser.paths == null ? "NULL" : graphSpecialUser.paths.size()));
			System.err.println("   - Size of userList: " + userList.size());
			return;
		}
		
		if (graphSpecialUser.paths.size() < userList.size()) {
			System.err.println("Warning! WARNING: graphSpecialUser.paths has fewer elements than userList");
			System.err.println("   - paths.size(): " + graphSpecialUser.paths.size());
			System.err.println("   - userList.size(): " + userList.size());
			
			// Fallback
			while (graphSpecialUser.paths.size() < userList.size()) {
				int userId = graphSpecialUser.paths.size() + 1;
				System.err.println("    Generating fallback for user " + userId);
				graphSpecialUser.paths.add(createFallbackPath(userId));
			}
		}

		long initialTimeTotal = 0, finalTimeTotal = 0;
		initialTimeTotal = System.currentTimeMillis();

		this.timeUsersInRoom = new HashMap<>(timeUsersInRooms);

		incrementSimulationIteration();
		showSimulationProgress();

		if (hasSimulationTimeExpired()) {
			try {
				exportMetricsForPythonRecommender(timeUsersInRooms, String.valueOf(numberOfUser));
				System.out.println("✅ Exported metrics (occupancy.csv and duration.csv).");
			} catch (Exception e) {
				System.err.println("Warning! Error exporting metrics for recommender: " + e.getMessage());
				e.printStackTrace();
			}

			EpidemicModel epidemicModel = this.epidemicManager.getEpidemicModel();
			if (epidemicModel != null && epidemicModel instanceof AbstractEpidemicModel) {
				AbstractEpidemicModel abstractModel = (AbstractEpidemicModel) epidemicModel;
				System.out.println("\n WRITER DIAGNOSTIC:");
				System.out.println("   - Writer null: " + (abstractModel.iterationsWriter == null));
				if (abstractModel.iterationsWriter != null) {
					System.out.println("   - Writer ready: " + abstractModel.iterationsWriter.isReady());
					System.out.println("   - Total records: " + abstractModel.iterationsWriter.getTotalRecords());
				}
			}
			System.out.println("\n--- SIMULATION FINISHED: DURATION COMPLETED ---");

			printFinalEpidemicStatistics();

			MainSimulator.userRunnable.setRunning(false);
			MainSimulator.printConsole("[Simulation finished - Duration completed]", Level.WARNING);
			MainSimulator.printConsole("Final statistics: " + getInfectionStatistics(), Level.WARNING);
			currentTime();
			disconnect();
			return;
		}

		MainSimulator.printConsole("Updating user positions: ", Level.INFO);
		log.log(Level.INFO, "Updating user positions: ");
		long initialTime = 0, finalTime = 0;
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, " -TIEMPO TOTAL MENSAJE INICIO: " + (finalTimeTotal - initialTimeTotal));

		initialTimeTotal = System.currentTimeMillis();
		int countFinishedSpecialUsers = 0;

		
		int validationIntervalIterations = getIterationsForSimulatedSeconds(1000);
		if (simulationIterationCounter % validationIntervalIterations == 0 && simulationIterationCounter > 0) {
			System.out.println("\n RUNTIME VALIDATION (Iteration " + 
							simulationIterationCounter + "):");
			
			Map<Integer, Integer> usersPerRoom = new HashMap<>();
			int usersMoving = 0;
			int usersFinished = 0;
			
			for (User user : userList) {
				if (user.hasFinishedVisit) {
					usersFinished++;
					continue;
				}
				
				if (user.room > 0) {
					usersPerRoom.put(user.room, usersPerRoom.getOrDefault(user.room, 0) + 1);
				}
				
				if (user.x > 0 && user.y > 0) {
					usersMoving++;
				}
			}
			
			System.out.println("    Active users: " + (userList.size() - usersFinished));
			System.out.println("    Moving users: " + usersMoving);
			System.out.println("    Finished users: " + usersFinished);
		}

		// Added by Nacho Palacio 2025-12-14
		if (Configuration.instance != null) {
			try {
				Configuration.ContactTrajectoryMode mode = Configuration.instance.getContactTrajectoryMode();
				
				if (mode != Configuration.ContactTrajectoryMode.DISABLED) {
					long currentTime = getCurrentSimulationTime();
					
					for (User user : userList) {
						// Rotational model
						if (mode == Configuration.ContactTrajectoryMode.SIMPLIFIED_ROTATION) {
							if (userToCliqueMap != null && userToCliqueMap.containsKey(user.userID)) {
								int cliqueId = userToCliqueMap.get(user.userID);
								int calculatedRoom = ContactTrajectoryBuilder.getRoomForCliqueAtTime(
									cliqueId, currentTime, EVENT_DURATION_SECONDS, NUM_ROOMS);
								user.room = calculatedRoom;
							}
						}
						// Fidelity model
						else if (mode == Configuration.ContactTrajectoryMode.COMPLEX_REAL_EVENTS) {
							List<ContactTrajectoryBuilder.UserRoomEvent> trajectory = user.getContactTrajectory();
							if (trajectory != null && !trajectory.isEmpty()) {
								for (ContactTrajectoryBuilder.UserRoomEvent event : trajectory) {
									if (currentTime >= event.startTime && currentTime < event.endTime) {
										
										if (event.roomId < 1 || event.roomId > MainSimulator.floor.getRoomCount()) {
											System.err.println("Warning! User " + user.userID + " has invalid room: " + event.roomId);
											continue;
										}
										
										user.room = event.roomId;
										
										break;
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				MainSimulator.printConsole("Error updating contact trajectories: " + e.getMessage(), Level.SEVERE);
				e.printStackTrace();
			}
		}

		// Create a new array equivalent to the user list in order to operate
		for (int batchStart = 0; batchStart < userList.size(); batchStart += BATCH_SIZE) {
    		int batchEnd = Math.min(batchStart + BATCH_SIZE, userList.size());
			
			// Loop for each user still left (that hasn't finished)
			for (int userIdx = batchStart; userIdx < batchEnd; userIdx++) {
				// New userPosition -> Equivalent to previous loop's variable
				User u = userList.get(userIdx);
				int userPosition = u.userID - 1;

				if (userIdx >= graphSpecialUser.paths.size()) {
					System.err.println("   Warning! User " + u.userID + 
									" without path in graphSpecialUser.paths, skipping");
					continue;
				}

				
				initialTime = System.currentTimeMillis();
				
				User currentUser = Configuration.simulation.userList.get(userPosition);

				availableTimeOfUsers[userPosition] += Configuration.simulation.getTimeForIterationInSecond();
				if (currentUser.hasFinishedVisit) {
					if (simulationIterationCounter <= 2) {
						System.out.println("   - User " + u.userID + " has already finished visit - SKIP");
					}
					log.log(Level.FINEST, "Skipping user's " + currentUser.userID + " iteration");
					continue;
				}

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
				
				if (availableTimeOfUsers[userPosition] <= 0) {
					int roomOfUser = currentUser.room;
					Pair<Integer,Integer> user_room = new Pair<Integer,Integer>(currentUser.userID,MainSimulator.floor.roomLabels.get(roomOfUser));
					Double pastTime = timeUsersInRooms.get(user_room);
					timeUsersInRooms.put(user_room,pastTime == null ? timeForIteration : pastTime + timeForIteration);
				}
				// The user will be moving while he has time available.
				while ((availableTimeOfUsers[userPosition] > 0) && (currentTimeOfUsers[userPosition] < getTimeAvailableUserInSecond())) {
					
					// Added by Nacho Palacio 2025-06-11
					int previousRoomOfUser = currentUser.room;
					if (Configuration.instance != null) {
						if (Configuration.instance.isUseContactTrajectoriesEnabled() && 
							userToCliqueMap != null && 
							userToCliqueMap.containsKey(currentUser.userID)) {
							
							int cliqueId = userToCliqueMap.get(currentUser.userID);
							long currentSimTimeSeconds = getCurrentSimulationTime();
							int calculatedRoom = ContactTrajectoryBuilder.getRoomForCliqueAtTime(
								cliqueId, 
								currentSimTimeSeconds, 
								EVENT_DURATION_SECONDS, 
								NUM_ROOMS
							);
							currentUser.room = calculatedRoom;
						} else {
							// Obtain room from physical position
							getUserRoomWithAdjustment(currentUser);
							
						}
					}
					else {
						// Se obtiene la habitación de la posición física
						getUserRoomWithAdjustment(currentUser);
					}
					
					log.log(Level.FINEST, "   TENGO TIEMPO TODAV�A: " + availableTimeOfUsers[userPosition]);
					// The current path.
					if (graphSpecialUser.paths.size() == 0) {
						System.out.println("   graphSpecialUser.paths.size() == 0 -> BREAK" );
						// break;
					}

					path = graphSpecialUser.paths.get(userPosition);
					
					// Added by Nacho Palacio 2025-05-11
					if (path == null) {
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
							if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM) &&
								ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_DOOR)) { // Modified by Nacho Palacio 2025-11-03
								if (!itemUpdated.contains(v1)) {
									// The path is updated with the recommendation algorithm to see if it suggests another item within the same room.
									long startVertex = v1;
									long endVertex = v2;
									// The RS user path is updated with the recommendation algorithm.
									long initialTimeSpecial = 0, finalTimeSpecial = 0;
									
									initialTimeSpecial = System.currentTimeMillis();

									updateSpecialUserPath(endVertex, endVertex, false, 0, false, currentUser); // Modified by Nacho Palacio 2025-11-03

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
									// edge = path.get(userPositionInPath[userPosition]);
									userPositionInPath[userPosition] = 0; // Added by Nacho Palacio 2025-11-04
									edge = path.get(0); // Modified by Nacho Palacio 2025-11-03
									array = cleanEdge(edge);
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

						if (location_v1 == null) {
							if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
								long v1External = ElementIdMapper.getBaseId(v1);
								location_v1 = MainSimulator.floor.diccionaryItemLocation.get(v1External);
							} else if (ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_DOOR)) {
								long v1External = ElementIdMapper.getBaseId(v1);
								location_v1 = MainSimulator.floor.diccionaryItemLocation.get(v1External);
							}
							
							// Default
							if (location_v1 == null) {
								location_v1 = "500.0, 500.0";
							}
						}
  
						if (location_v2 == null) {
							if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_ITEM)) {
								long v2External = ElementIdMapper.getBaseId(v2);
								location_v2 = MainSimulator.floor.diccionaryItemLocation.get(v2External);
							} else if (ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_DOOR)) {
								long v2External = ElementIdMapper.getBaseId(v2);
								location_v2 = MainSimulator.floor.diccionaryItemLocation.get(v2External);
							}
							
							// Default
							if (location_v2 == null) {
								location_v2 = "500.0, 500.0";
							}
						}

						// If it is an item (not a door).
						long internalV2 = ElementIdMapper.getBaseId(v2);
						if (internalV2 <= this.numberOfITems) { // Modified by Nacho Palacio 2025-10-17 antes: if (v2 <= this.numberOfITems) {

							// Register the item as observed
							if (itemObservedOfUsers.containsKey(currentUser.userID)) {
								List<Long> observedList = itemObservedOfUsers.get(currentUser.userID);
								if (!observedList.contains(internalV2)) {
									observedList.add(internalV2);
								}
							} else {
								List<Long> observedList = new LinkedList<>();
								observedList.add(internalV2);
								itemObservedOfUsers.put(currentUser.userID, observedList);
							}

							// Generate rating. If the user has time and was rating an item, then he or she is done rating.
							if ((availableTimeOfUsers[userPosition] >= 0) && (voting[userPosition] == true)) {
								
								log.log(Level.FINEST, "      ITEM " + v2 + " SIENDO VOTADO");
								long id_user = currentUser.userID;
								long user = id_user;
								long item = itemsBeingWatched[userPosition];
								long itemExternal = ElementIdMapper.getBaseId(item);

								initialTime = System.currentTimeMillis();
								long context = getCurrentContext(currentUser);
								finalTime = System.currentTimeMillis();
								log.log(Level.INFO, "      Tiempo en obtener el contexto: " + (finalTime - initialTime));
								
								initialTime = System.currentTimeMillis();
								
								float rating = generateRating(user, item, itemExternal, context);
								
								userRatings.computeIfAbsent((int) user, k -> new ArrayList<>()).add(rating); // Added by Nacho Palacio 2025-10-18

								finalTime = System.currentTimeMillis();
								log.log(Level.INFO, "      Tiempo en generar valoración: " + (finalTime - initialTime));

								u.totalObservationTime += getDelayObservingPaintingInSecond();
								u.totalItemsObserved += 1;

								String location = currentUser.x + ", " + currentUser.y;
								InformationToPropagate informationToPropagate = new InformationToPropagate(id_user, user, item, context, rating, Configuration.simulation.getTtl(), location,
										currentTimeOfUsers[currentUser.userID - 1]);

								if (!isChangedItemByRecommender) {
									initialTime = System.currentTimeMillis();
									
									long initialTimeRecommender = 0, finalTimeRecommender = 0;
									initialTimeRecommender = System.currentTimeMillis();
									// The generated rating is inserted into the database (db_user.db) of the RS user.
									specialUserListenTheInformation(informationToPropagate, currentUser); // Lot of time consumed
									finalTimeRecommender = System.currentTimeMillis();
									
									// Update item stats
									if (currentUser.isSpecialUser && Literals.COMPILE_ITEM_STATS) {
										updateSpecialUserItemStatistics(informationToPropagate);
									}
									
									logRecommender.log(Level.INFO, "[specialUserListenTheInformation]: " + (finalTimeRecommender - initialTimeRecommender));
									
									initialTimeRecommender = System.currentTimeMillis();
									// Increased time: for the RS user to arrive at the item and observe it.
									currentTimeOfUsers[currentUser.userID - 1] += getCurrentTime(location_v1, location_v2) + Configuration.simulation.getDelayObservingPaintingInSecond();
									if (itemRatedOfUsers.containsKey(currentUser.userID)) {
										List<Long> itemList = itemRatedOfUsers.get(currentUser.userID);
										itemList.add(item);
										itemRatedOfUsers.put(currentUser.userID, itemList);
									} else {
										List<Long> itemList = new LinkedList<>();
										itemList.add(item);
										itemRatedOfUsers.put(currentUser.userID, itemList);
									}
									finalTimeRecommender = System.currentTimeMillis();
									
									logRecommender.log(Level.INFO, "[currentTimeOfUsers]: " + (finalTimeRecommender - initialTimeRecommender));

									initialTimeRecommender = System.currentTimeMillis();
									// The information to be propagated is inserted in a db_p2p_queue_user_XXX.db.
									if (Configuration.simulation.getNetworkType().equalsIgnoreCase("Peer To Peer (P2P)")) {
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
							
							String cur = locationNextIteration[userPosition];
							String dst = location_v2;
							String[] curParts = cur.split(",\\s*");
							String[] dstParts = dst.split(",\\s*");
							if (curParts.length >= 2 && dstParts.length >= 2) {
								if (locationNextIteration[userPosition].equalsIgnoreCase(location_v2)) {

									UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
									if(v2 <= this.numberOfITems && ui != null) {
										ui.action = "Observing item";
										ui.item = v2;
									}

									// Added by Nacho Palacio 2025-11-06
									String edgeTraveled = "(" + v1 + " : " + v2 + ")";
									actualPathTraveled.computeIfAbsent(currentUser.userID, k -> new ArrayList<>()).add(edgeTraveled);
									
									// Moving to other item.
									userPositionInPath[userPosition] += 1;
									// If is a RS user.
									if (currentUser.isSpecialUser) {
										// The path traveled is stored.
										// Before the user goes to another item, consider the problem of user disobedience.
										specialUserDisobedience(currentUser);
									}
									// The time to go from one door (or stairs) to another is increased.
									if (v2 > this.numberOfITems) {
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
									
									if (!(Configuration.instance != null && 
                                          Configuration.instance.isUseContactTrajectoriesEnabled() && 
                                          userToCliqueMap != null && 
                                          userToCliqueMap.containsKey(currentUser.userID))) {
                                        
                                        getUserRoomWithAdjustment(currentUser);
                                    }
									
									int roomOfUser = currentUser.room;

									updateUserRoomEntry(currentUser, previousRoomOfUser, roomOfUser); // Added by Nacho Palacio 2025-12-16

									if(roomOfUser > -1) {
										UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
										if(ui == null) {
											ui = new UserInfo.UserState(MainSimulator.floor.roomLabels.get(roomOfUser));
											stateOfUsers.put(currentUser.userID,ui);
										} else {
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
							}
							else {
								System.out.println("Warning! parsing has failed");
							}
							
							finalTime = System.currentTimeMillis();
							log.log(Level.INFO, "    - TIME MOVING: " + (finalTime - initialTime));
						}
						else {
							int roomOfUser = currentUser.room;
							Pair<Integer,Integer> user_room = new Pair<Integer,Integer>(currentUser.userID,MainSimulator.floor.roomLabels.get(roomOfUser));
							Double pastTime = timeUsersInRooms.get(user_room);
							timeUsersInRooms.put(user_room,pastTime == null ? timeForIteration : pastTime + timeForIteration);

						}
						lastV2 = v2;

					} else {
						System.out.println("User " + currentUser.userID + " has FINISHED his/her PATH.");
						initialTime = System.currentTimeMillis();
						log.log(Level.INFO, "   Path Se ha acabado");
						// If the RS user's path ends and he still has time for the visit, then the user's path is updated with the recommender.
						if (currentUser.isSpecialUser /*&& userTimesUpdatedPath[userPosition] < 10*/) {
							userTimesUpdatedPath[userPosition]++;
							try {
								String start = cleanEdge(path.get(path.size() - 1))[0];
								String end = cleanEdge(path.get(path.size() - 1))[1];
								long startVertex = Long.valueOf(start).longValue();
								long endVertex = Long.valueOf(end).longValue();
								boolean finishPath = true;

								// The RS user path is updated with the recommendation algorithm.
								updateSpecialUserPath(startVertex, endVertex, false, 0, finishPath, currentUser);

								// Reset path position
								userPositionInPath[userPosition] = 0; // Added by Nacho Palacio 2025-11-04
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

							// Added by Nacho Palacio 2025-11-04
							if (path == null || path.isEmpty()) {
								currentUser.hasFinishedVisit = true;
								this.ended.add(currentUser);
								break;
							}

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

						// Modified by Nacho Palacio 2025-05-10
						UserInfo.UserState ui = stateOfUsers.get(currentUser.userID);
						if (ui != null) {
							ui.action = "Finished";
							ui.item = null;
						} else {
							ui = new UserInfo.UserState(MainSimulator.floor.roomLabels.get(currentUser.room));
							ui.action = "Finished";
							ui.item = null;
							stateOfUsers.put(currentUser.userID, ui);
						}
						
						finalTime = System.currentTimeMillis();
						log.log(Level.INFO, "    Tiempo en actualizar PATH de " + (userPosition+1) + " (acabado): " + (finalTime - initialTime));
					}
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
		}

		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.INFO, " -TIEMPO TOTAL BUCLE: " + (finalTimeTotal - initialTimeTotal));
		
		initialTimeTotal = System.currentTimeMillis();

		if (coincidenceTracker != null) {
			coincidenceTracker.updateCoincidences(simulationIterationCounter);
		}

		epidemicManager.updateEpidemicState(getAllUsers(), getCurrentSimulationIteration());

		// Added by Nacho Palacio 2025-07-30
		if (areAllUsersInfected()) {
			System.out.println("All users are infected. Ending simulation.");
			MainSimulator.userRunnable.setRunning(false);
			MainSimulator.printConsole("Ended Simulation, all users are infected]", Level.WARNING);
			MainSimulator.printConsole("Final statistics: " + getInfectionStatistics(), Level.WARNING);
			currentTime();

			disconnect();
			return; 
		}

		// The criterion for stopping the simulation is that all users have finished their time.
		if (countFinishedSpecialUsers >= getNumberOfSpecialUser() && getNumberOfSpecialUser() > 0) {
			// The thread is killed because the visit is over.
			MainSimulator.userRunnable.setRunning(false);
			MainSimulator.printConsole("[Finished visits]", Level.WARNING);
			currentTime();
			
			disconnect();
			
		}
		
		finalTimeTotal = System.currentTimeMillis();
	}

	/**
	 * Verifies if the simulation has finished based on configured duration.
	 * Compares elapsed simulated time against the configured maximum duration
	 * from EpidemicConfiguration.
	 * Added by Nacho Palacio 2025-09-18
	 * 
	 * @return true if simulation time has expired, false otherwise
	 */
	private boolean hasSimulationTimeExpired() {
		try {
			EpidemicConfiguration config = es.unizar.epidemic.general.EpidemicConfiguration.getInstance();
			int maxDurationSeconds = config.getSimulationDurationSeconds();
			
			double timePerIteration = getTimeForIterationInSecond();
			int currentIterationCount = getCurrentSimulationIteration();
			double elapsedSimulatedTime = currentIterationCount * timePerIteration;
			
			boolean timeExpired = elapsedSimulatedTime >= maxDurationSeconds;
			
			if (timeExpired) {
				System.out.println("   \n Simulation time has expired (" + 
								String.format("%.1f", elapsedSimulatedTime) + "/" + maxDurationSeconds + " seconds)");
			}
			
			return timeExpired;
			
		} catch (Exception e) {
			System.err.println("Warning! Error verifying simulation duration: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the current iteration count for the simulation.
	 * Added by Nacho Palacio 2025-09-18
	 * 
	 * @return current simulation iteration number
	 */
	private int getCurrentSimulationIteration() {
		return simulationIterationCounter;
	}

	/**
	 * Increments the simulation iteration counter.
	 * Should be called once per simulation iteration to track progress.
	 * Added by Nacho Palacio 2025-09-18
	 */
	private void incrementSimulationIteration() {
		simulationIterationCounter++;
	}

	/**
	 * Shows simulation progress every simulated minute.
	 * Displays elapsed simulated time and maximum duration at regular intervals
	 * to provide feedback on simulation progress.
	 * Added by Nacho Palacio 2025-09-18
	 */
	private void showSimulationProgress() {
		try {
			EpidemicConfiguration config = es.unizar.epidemic.general.EpidemicConfiguration.getInstance();
			double timePerIteration = getTimeForIterationInSecond();
			double elapsedSimulatedTime = getCurrentSimulationIteration() * timePerIteration;
			int maxDurationSeconds = config.getSimulationDurationSeconds();
			
			int iterationsPerMinute = (int) (60.0 / getTimeForIterationInSecond());
			if (iterationsPerMinute < 1) iterationsPerMinute = 1;

			if (simulationIterationCounter % iterationsPerMinute == 0 && simulationIterationCounter > 0) {
				int minutesElapsed = (int) (elapsedSimulatedTime / 60);
				int totalMinutes = maxDurationSeconds / 60;
				double progressPercent = (elapsedSimulatedTime / maxDurationSeconds) * 100;
				
				MainSimulator.printConsole(String.format(" Simulation progress: %d/%d minutes (%.1f%%)", 
										minutesElapsed, totalMinutes, progressPercent), Level.INFO);
			}
			
		} catch (Exception e) {
		}
	}
	
	/**
	 * Verifies if all users are infected.
	 * Checks epidemic extensions of all users to determine if everyone
	 * has been infected (useful for early termination conditions).
	 * Added by Nacho Palacio 2025-07-30
	 * 
	 * @return true if all users are infected, false otherwise
	 */
	private boolean areAllUsersInfected() {
		int totalUsers = userList.size();
		int infectedUsers = 0;
		
		for (User user : userList) {
			UserEpidemicExtension extension = user.getEpidemicExtension();
			if (extension != null && isUserInfected(extension)) {
				infectedUsers++;
			}
		}

		boolean allInfected = (infectedUsers >= totalUsers);
		
		if (infectedUsers > totalUsers * 0.8) {
			MainSimulator.printConsole(String.format(" Epidemic progress: %d/%d users infected (%.1f%%)", 
									infectedUsers, totalUsers, (infectedUsers * 100.0) / totalUsers), Level.INFO);
		}
		
		return allInfected;
	}

	/**
	 * Verifies if a user is infected.
	 * Checks if user's health status indicates infection (symptomatic, asymptomatic, or superspreader).
	 * Added by Nacho Palacio 2025-07-30
	 * 
	 * @param extension the user's epidemic extension
	 * @return true if the user is infected, false otherwise
	 */
	public boolean isUserInfected(UserEpidemicExtension extension) {
		HealthStatus status = extension.getHealthStatus();
		return status == HealthStatus.INFECTIOUS_SYMPTOMATIC ||
			status == HealthStatus.SUPER_SPREADER;
	}


	////////////////////////////////////////////////////////
	// MÉTODOS DE GESTIÓN DE MOVIMIENTO DE USUARIOS
	////////////////////////////////////////////////////////

	/**
	 * Calculates the next movement of the user.
	 * 
	 * @param location_v1 the initial vertex location
	 * @param location_v2 the final vertex location
	 * @param currentUser the current user
	 * @param itemID the item ID
	 * @return the next movement coordinates as a string "x, y"
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

			long externalItemId = ElementIdMapper.getBaseId(itemID); // Modified by Nacho Palacio 2025-07-06
			if (externalItemId <= this.numberOfITems) { // Modified by Nacho Palacio 2025-07-06
				availableTimeOfUsers[currentUser.userID - 1] -= Configuration.simulation.getDelayObservingPaintingInSecond();
				MainSimulator.printConsole("Remaining time available after to generate rating: " + availableTimeOfUsers[currentUser.userID - 1], Level.INFO);
				voting[currentUser.userID - 1] = true;
			}

			// Added by Nacho Palacio 2025-10-22
			xF1 = xFinal;
            yF1 = yFinal;
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
	 * @param xInitial the initial X coordinate
	 * @param yInitial the initial Y coordinate
	 * @param xFinal the final X coordinate
	 * @param yFinal the final Y coordinate
	 * @return the velocity on X axis in pixels per second
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
	 * Calculates the final point where the user will move.
	 * Computes the destination coordinate based on starting point, velocity,
	 * available time, and maximum distance constraints.
	 * 
	 * @param pointInitial the initial coordinate value
	 * @param velocity the velocity component
	 * @param availableTime the available time for movement
	 * @param maxDistanceToReachTarget the maximum distance to reach the target
	 * @return the final coordinate value
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
	 * @param xInitial the initial X coordinate
	 * @param yInitial the initial Y coordinate
	 * @param xFinal the final X coordinate
	 * @param yFinal the final Y coordinate
	 * @return the angle in radians
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
	 * Subtracts the time required to travel between two points from current available time.
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
		// System.out.println("Remaining time available before updating: " + time);
		return time;
	}

	/**
	 * Get the current time.
	 * 
	 * @param locationStartVertex the initial vertex location
	 * @param locationEndVertex the final vertex location
	 * @return the time in seconds to travel between vertices
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
	 * Calculate the distance between two users.
	 * 
	 * @param positionUser1 the position of the first user as "x, y"
	 * @param positionUser2 the position of the second user as "x, y"
	 * @return the distance in pixels between the two users
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
	 * Gets the (x,y) location of a vertex (item or door)
	 * 
	 * @param vertexId the vertex ID
	 * @return the (x,y) location as a string "x, y"
	 */
	private String getVertexLocation(long vertexId) {
		long mappedVertexId = vertexId;

		if (MainSimulator.floor != null && MainSimulator.floor.diccionaryItemLocation != null) {
			String location = MainSimulator.floor.diccionaryItemLocation.get(mappedVertexId);
			if (location != null) {
				return location;
			}
		}
		
		if (MainSimulator.floor != null && DrawFloorGraph.vertices != null) {
			for (com.mxgraph.model.mxCell cell : DrawFloorGraph.vertices) {
				if (cell != null && cell.getId() != null) {
					try {
						long cellId = Long.parseLong(cell.getId());
						
						if (cellId == mappedVertexId) {
							com.mxgraph.model.mxGeometry geo = cell.getGeometry();
							String location = geo.getX() + ", " + geo.getY();
													
							return location;
						}
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		
		// Fallback
		System.err.println("    Coordinates not found for vertex " + vertexId + 
						" (mapped: " + mappedVertexId + "), using (100, 100)");
		return "100.0, 100.0";
	}

	/**
	 * Gets a fallback location for a room if no items or doors have valid locations.
	 * Searches room's items and doors for the first valid location, or returns
	 * a default location based on room ID.
	 * 
	 * @param roomId the room ID
	 * @return the fallback (x,y) location as a string "x, y"
	 */
	private String getFallbackLocationForRoom(int roomId) {
		try {
			int numItems = graphSpecialUser.accessGraphFile.getNumberOfItemsByRoom(roomId);
			if (numItems > 0) {
				long itemId = graphSpecialUser.accessGraphFile.getItemOfRoom(1, roomId);
				String location = getVertexLocation(itemId);
				if (location != null && !location.isEmpty() && !location.equals("100.0, 100.0")) {
					return location;
				}
			}
			
			int numDoors = graphSpecialUser.accessGraphFile.getNumDoorsByRoom(roomId);
			if (numDoors > 0) {
				long doorId = graphSpecialUser.accessGraphFile.getDoorOfRoomWithIndex(1, roomId);
				String location = getVertexLocation(doorId);
				if (location != null && !location.isEmpty() && !location.equals("100.0, 100.0")) {
					return location;
				}
			}
		} catch (Exception e) {
			System.err.println("   Warning! Error obtaining fallback location for room " + roomId + ": " + e.getMessage());
		}
		
		// Fallback
		return "500.0, 500.0";
	}

	////////////////////////////////////////////////////////
	// MÉTODOS DE GESTIÓN DE ESTADO DE USUARIOS
	////////////////////////////////////////////////////////

	/**
	 * Randomly initializes the mood of each user.
	 * Assigns a random mood value (10=happy, 11=neutral, 12=sad) to each user.
	 */
	public void initializeMoodOfUsers() {
		int[] moodValues = { 10, 11, 12 };
		for (int userPosition = 0; userPosition < userList.size(); userPosition++) {
			int pos = random.nextInt(moodValues.length);
			moodOfUsers[userPosition] = moodValues[pos];
		}
	}

	/**
	 * Randomly changes the mood of a user.
	 * Assigns a new random mood value (10=happy, 11=neutral, 12=sad) to the specified user.
	 * 
	 * @param currentUser the user whose mood will be changed
	 */
	public void changeMoodOfUsers(User currentUser) {
		int[] moodValues = { 10, 11, 12 };
		int pos = random.nextInt(moodValues.length);
		moodOfUsers[currentUser.userID - 1] = moodValues[pos];
	}	

	/**
	 * Updates the user's path when they ignore the recommendation.
	 * Called when a special user exhibits disobedience behavior,
	 * requiring path recalculation.
	 * 
	 * @param currentUser the user exhibiting disobedience
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
	 * Selects a random item connected to the current edge that hasn't been visited.
	 * 
	 * @param edge the current edge
	 * @param specialUserID the ID of the special user
	 * @return the ID of the randomly selected next item
	 */
	public long getItemRandomly(String edge, long specialUserID) {
		long itemSelected = 0;
		try {
			String[] array = cleanEdge(edge);
			// The current item.
			long currentItem = Long.valueOf(array[1]).longValue();
			// Get randomly the next item to visit by user.
			// int room = graphSpecialUser.getRoomFromItem(currentItem);
			int room = getRoom(currentItem); // Modified by Nacho Palacio 2025-06-19
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


	////////////////////////////////////////////////////////
	// MÉTODOS DEL SISTEMA DE RECOMENCACIÓN
	////////////////////////////////////////////////////////

	/**
	 * Updates the RS user path with the recommendation algorithm.
	 * Generates personalized recommendations based on user preferences, context,
	 * and collaborative filtering. Handles various recommendation strategies including
	 * random, exhaustive, UBCF, SVD, and risk-aware approaches.
	 * Modified by Nacho Palacio 2025-05-11
	 * 
	 * @param startVertex the entrance door
	 * @param endVertex initially equals to startVertex
	 * @param disobedience if the algorithm will consider user disobedience
	 * @param nextItemSelected the next selected item
	 * @param finishPath if finish the RS user path
	 * @param currentUser the current user receiving recommendations
	 */
	public void updateSpecialUserPath(long startVertex, long endVertex, boolean disobedience, long nextItemSelected, boolean finishPath, User currentUser) {		
		int idx = (int) currentUser.userID - 1;

		// Added by nacho Palacio 2025-11-04
		if (graphSpecialUser != null) {
			if (graphSpecialUser.paths == null) {
				graphSpecialUser.paths = new ArrayList<>();
			}
			while (graphSpecialUser.paths.size() <= idx) {
				graphSpecialUser.paths.add(new LinkedList<String>());
			}
			if (graphSpecialUser.paths.get(idx) == null) {
				graphSpecialUser.paths.set(idx, new LinkedList<String>());
			}
		}

		long initialTimeTotal = 0, finalTimeTotal = 0, initialTimeNetwork = 0, finalTimeNetwork = 0;
		initialTimeTotal = System.currentTimeMillis();
		
		List<String> finalPath = null;
		List<RecommendedItem> recommendedItems = null;
		String recommendationType = null;
		String currentPath = null;
		TrajectoryPostfilteringBasedRecommendation postfiltering = null;
		initialTimeNetwork = System.currentTimeMillis();
		// Path pathStrategy = new NearestPath();
		Path pathStrategy = new RandomPath(); // Modified by Nacho Palacio 2025-06-10
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
			
			initialTimeTotal = System.currentTimeMillis();
			initialTimeTry = System.currentTimeMillis();
			// For the database connection of the current RS user.
			dataModelSpecialUser = DBDataModel.getFromPool(special_user_dbURL, db_special_user, this.numberOfUser-1); // Modified by Nacho Palacio 2025-12-08
			dataAccesLayerDBMuseum = new DataAccessLayer(Literals.SQL_DRIVER + Literals.DB_ALL_USERS_PATH, dataInstanceMuseumDB);
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - DB connection: " + (finalTimeTry - initialTimeTry));	
					//+ " -> DBDataModel: " + (finalTimeDBDataModel - initialTimeDBDataModel) + 
					//"; DataAccessLayer: "+ (finalTimeDataAccessLayer - initialTimeDataAccessLayer));
			
			
			initialTimeTry = System.currentTimeMillis();
			// Build a graph for the RS user.
			
			// Modified by Nacho Palacio 2025-12-08
            SimpleWeightedGraph<Long, DefaultWeightedEdge> currentGraph = graphSpecialUser.getCachedGraph();
            if (cachedTrajectoryStrategy == null || lastUsedGraph != currentGraph) {
                cachedTrajectoryStrategy = new ShortestTrajectoryStrategy(currentGraph, MainSimulator.floor.diccionaryItemLocation);
                lastUsedGraph = currentGraph;
            }
            ShortestTrajectoryStrategy trajectoryStrategy = cachedTrajectoryStrategy;

			for (Long vertex : graphSpecialUser.graphRecommender.vertexSet()) {
				System.out.print(vertex + " ");
			}

			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Build graph: " + (finalTimeTry - initialTimeTry));
			
			initialTimeTry = System.currentTimeMillis();
			// The recommendation threshold.
			float threshold = getThresholdRecommendation();
			if (finishPath) {
				System.out.println("El path ha terminado");
				// // When the path is finished.
			
				// Modified by Nacho Palacio 2025-12-10
				long exitDoor = endVertex;
				
				// Verify if endVertex is an item (not a door)
				if (endVertex <= numberOfITems || 
					ElementIdMapper.isInCorrectRange(endVertex, ElementIdMapper.CATEGORY_ITEM)) {
					
					// Search for closest door to end item
					int currentRoom = getRoom(endVertex);
					
					if (currentRoom > 0) {
						List<Long> doorsInRoom = graphSpecialUser.getDoorsByRoom(currentRoom);
						
						if (doorsInRoom != null && !doorsInRoom.isEmpty()) {
							// Obtain closest door to item
							exitDoor = graphSpecialUser.getDoorClosestToTheItem(endVertex, doorsInRoom);
						} else {
							System.err.println("   Warning! No doors in room " + currentRoom + 
											", using endVertex as fallback");
						}
					} else {
						System.err.println("   Warning! Could not determine room for item " + endVertex);
					}
				}
				
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(
					dataModelSpecialUser, 
					special_user_dbURL, 
					trajectoryStrategy, 
					exitDoor,
					threshold
				);
			} else {
				// When the path is not finished.
				long exitDoor = startVertex;
				if (startVertex <= numberOfITems || 
					ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM)) {
					
					// Search for closest door to start item
					int currentRoom = getRoom(startVertex);
					if (currentRoom > 0) {
						List<Long> doorsInRoom = graphSpecialUser.getDoorsByRoom(currentRoom);
						
						if (doorsInRoom != null && !doorsInRoom.isEmpty()) {
							// Obtain closest door to item
							exitDoor = graphSpecialUser.getDoorClosestToTheItem(startVertex, doorsInRoom);
						} else {
							System.err.println("   Warning! No doors in room " + currentRoom + 
											", using startVertex as fallback");
						}
					} else {
						System.err.println("   Warning! Could not determine room for item " + startVertex);
					}
				}

				System.out.println("Calling with exitDoor: " + exitDoor);
				postfiltering = new TrajectoryPostfilteringBasedRecommendation(
					dataModelSpecialUser, 
					special_user_dbURL, 
					trajectoryStrategy, 
					exitDoor, 
					threshold
				);
			}
			
			finalTimeTry = System.currentTimeMillis();
			log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Threshold: " + (finalTimeTry - initialTimeTry));
			
			initialTimeTry = System.currentTimeMillis();

			// Recommendation type
			recommendationType = getRecommendationAlgorithm();

			if (recommendationType == null || recommendationType.isEmpty()) {
				recommendationType = "Near POI (NPOI)";
			}

			if (recommendationType.equalsIgnoreCase("Completely-random (FULLY-RAND)")) {
				RandomRecommendation recommender = new RandomRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				recommendedItems = recommender.recommend(currentUser.userID, getHowMany());
				//log.log(Level.WARNING, "Recommended items: " + recommendedItems.toString());
				// The path is obtained from the recommended items.
				recommendedItems = filterAlreadyObservedItems(recommendedItems, itemObservedOfUsers, currentUser.userID);
				
				postfiltering.recommendBaseline(recommendedItems);
				
				currentPath = postfiltering.getFinalPath();
				finalTimeTry = System.currentTimeMillis();
				log.log(Level.WARNING, "[updateSpecialUserPath]: TRY - Recommendation (FULLY-RAND): " + (finalTimeTry - initialTimeTry));
				log.log(Level.SEVERE, "Finished: FULLY-RAND");
				
				
			} else if (recommendationType.equalsIgnoreCase("User-Based Collaborative Filtering (UBCF)") || recommendationType.equalsIgnoreCase("Know-It-All (Know-It-All)")) {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModelSpecialUser);
				UserNeighborhood neighborhood = new ThresholdUserNeighborhood(getThresholdSimilarity(), similarity, dataModelSpecialUser);
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
				System.out.println("Usando K-Ideal (K-Ideal)");
				IdealRecommendation recommender = new IdealRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
				long currentContext = getCurrentContext(currentUser);
				List<RecommendedItem> candidateItemsFromRecommender = recommender.recommend(currentUser.userID, getHowMany(), currentContext);

				recommendedItems = filterAlreadyObservedItems(candidateItemsFromRecommender, itemObservedOfUsers, currentUser.userID);
				recommendedItems = postfiltering.recommendIdeal(candidateItemsFromRecommender);
				// The path is obtained from the recommended items.
				currentPath = postfiltering.getFinalPath();
				log.log(Level.SEVERE, "Finished: K-Ideal");
			} else if (recommendationType.equalsIgnoreCase("Risk-Aware (Risk-Aware)")) {
				// String pythonScriptPath = "../../../../../../../Backups/moma...";
				String pythonScriptPath = "/home/nacho/universidad/cuarto/TFG/RecMobiSimPlus/Backups/moma.recommender-main-snapshot20251005/moma.recommender-main/src/recommender/recommender_bridge.py";
								
				RiskAwareRecommendation recommender = new RiskAwareRecommendation(pythonScriptPath);

				Map<Integer, Integer> occupancyPerRoom = new HashMap<>();
				for (User user : userList) {
					// Modified by Nacho Palacio 2025-11-04
					int realRoom = getUserRoomWithAdjustment(user) + 1;
					occupancyPerRoom.put(realRoom, occupancyPerRoom.getOrDefault(realRoom, 0) + 1);
				}

				// previous method to calculate duration per room
				Map<Integer, Double> durationPerRoom = new HashMap<>();
				for (Map.Entry<Pair<Integer, Integer>, Double> entry : timeUsersInRoom.entrySet()) {
					Pair<Integer, Integer> userRoom = entry.getKey();
					int room = userRoom.getS();
					double durationSeconds = entry.getValue();
					durationPerRoom.merge(room, durationSeconds, Double::sum);
				}

				// Seconds to minutes
				durationPerRoom.replaceAll((room, seconds) -> seconds / 60.0);

				Map<Integer, Double> durationPerRoomNew = calculateCurrentDurationPerRoom();
    
				ElementIdMapper.SystemRangeData rangeData = ElementIdMapper.getSystemRangeData();
				int minRoomId = (int) rangeData.minRoomId;
				int maxRoomId = (int) rangeData.maxRoomId;

				// Ensure all rooms have entries in occupancyPerRoom and durationPerRoom
				for (int roomId = minRoomId; roomId <= maxRoomId; roomId++) {
					durationPerRoomNew.putIfAbsent(roomId, 0.0167); // 1 second in minutes
					if (durationPerRoomNew.get(roomId) == 0.0) {
						durationPerRoomNew.put(roomId, 0.0167);
					}
				}

				if (occupancyPerRoom.isEmpty()) {
					for (int roomId = minRoomId; roomId <= maxRoomId; roomId++) {
						occupancyPerRoom.put(roomId, 1);
					}
				}
				else {
					for (int roomId = minRoomId; roomId <= maxRoomId; roomId++) {
						occupancyPerRoom.putIfAbsent(roomId, 0);
					}
				}

				if (durationPerRoomNew.isEmpty()) {
					for (int roomId = minRoomId; roomId <= maxRoomId; roomId++) {
						durationPerRoomNew.put(roomId, 1.0);
					}
				}
				
				// Obtain favorite artworks of current user
				PreferenceArray userPreferences = dataModelMuseumDB.getPreferencesFromUserSortedByRating(currentUser.userID);

				// Sorted by rating
				List<Long> favoriteArtworks = new ArrayList<>();
				int maxFavorites = Math.min(20, userPreferences.length());
				for (int i = 0; i < maxFavorites; i++) {
					long itemId = userPreferences.getItemID(i);
					favoriteArtworks.add(itemId);
				}
				
				try {
					recommendedItems = recommender.recommend(currentUser.userID, 
															itemObservedOfUsers.get(currentUser.userID), 
															occupancyPerRoom, 
															// durationPerRoom,
															durationPerRoomNew, // Modified by Nacho Palacio 2025-12-16
															favoriteArtworks
															);

				    // Added by Nacho Palacio 2025-12-10
					if (recommendedItems != null && recommendedItems.size() > getHowMany()) {
						recommendedItems = recommendedItems.subList(0, getHowMany());
					}

					recommendedItems = filterAlreadyObservedItems(recommendedItems, itemObservedOfUsers, currentUser.userID);
					
				} catch (IOException e) {
					recommendedItems = new ArrayList<>();
				}


				if (recommendedItems == null || recommendedItems.isEmpty()) {
					currentPath = "";
				} else {
					postfiltering.recommendBaseline(recommendedItems);
					currentPath = postfiltering.getFinalPath();
				}

				log.log(Level.SEVERE, "Finished: Risk-Aware");
			} 
			else if (recommendationType.equalsIgnoreCase("Non-Risk-Aware (Non-Risk-Aware)")) {
				String pythonScriptPath = "/home/nacho/universidad/cuarto/TFG/RecMobiSimPlus/Backups/moma.recommender-main-snapshot20251005/moma.recommender-main/src/recommender/recommender_bridge.py";
				RiskAwareRecommendation recommender = new RiskAwareRecommendation(
					pythonScriptPath, false
				);
				
				List<Long> favoriteArtworks = new ArrayList<>();
				PreferenceArray userPreferences = dataModelMuseumDB.getPreferencesFromUserSortedByRating(currentUser.userID);
				for (int i = 0; i < Math.min(10, userPreferences.length()); i++) {
					favoriteArtworks.add(userPreferences.getItemID(i));
				}
				
				recommendedItems = recommender.recommend(
					currentUser.userID, 
					itemObservedOfUsers.get(currentUser.userID),
					null, null, favoriteArtworks
				);

				// Added by Nacho Palacio 2025-12-10
				if (recommendedItems != null && recommendedItems.size() > getHowMany()) {
					recommendedItems = recommendedItems.subList(0, getHowMany());
				}
				
				if (recommendedItems != null && !recommendedItems.isEmpty()) {
					recommendedItems = filterAlreadyObservedItems(recommendedItems, itemObservedOfUsers, currentUser.userID);
					postfiltering.recommendBaseline(recommendedItems);
					currentPath = postfiltering.getFinalPath();
				} else {
					currentPath = "";
				}
				
				log.log(Level.SEVERE, "Finished: Non-Risk-Aware");
			} else {
				if (recommendationType.equalsIgnoreCase("Exhaustive visit (ALL)")) {
					ExhaustiveRecommendation recommender = new ExhaustiveRecommendation(dataModelSpecialUser, dataAccesLayerDBMuseum);
					recommendedItems = recommender.recommend(currentUser.userID, 1);
					// The path is obtained from the recommended items.
					recommendedItems = filterAlreadyObservedItems(recommendedItems, itemObservedOfUsers, currentUser.userID);
					
					postfiltering.recommendBaseline(recommendedItems); // NoSuchUserException
					// The path is obtained from the recommended items.
					currentPath = postfiltering.getFinalPath();
					
					log.log(Level.SEVERE, "Finished: Exhaustive (ALL)");
				} else {
					if (recommendationType.equalsIgnoreCase("Near POI (NPOI)")) {
						// NPOI exception -> to catch block
						throw new Exception("Debe ejecutarse solo NPOI");
					}
				}
			}
			// Added by Nacho Palacio 2025-05-13
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
			}
			else { // Added by Nacho Palacio 2025-05-12
				try {
					if (finishPath) {
						currentPath = pathStrategy.generatePath(endVertex);
					} else {
						// Modified by Nacho Palacio 2025-05-17
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

			// It is asigned to the RS user graph
			pathSpecialUser = Arrays.asList(currentPath.split(", ")); // Added by Nacho Palacio 2025-11-01

			graphSpecialUser.paths.set(((int) currentUser.userID) - 1, pathSpecialUser);

			// Store predicted ratings by the RS user
			storePredictedRatings(recommendedItems, currentUser);
			
			/*
			 * Connection reused -> Don't disconnect till the end of the simulation
			// Close DB connections
			dataModelSpecialUser.disconnect();
			dataAccesLayerDBMuseum.disconnect();
			*/

			// graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
			
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
			// Added by Nacho Palacio 2025-06-10
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
		finally { // Added by Nacho Palacio 2025-12-08
            if (dataModelSpecialUser != null) {
                dataModelSpecialUser.returnToPool();
                dataModelSpecialUser = null;
            }
            dataAccesLayerDBMuseum = null;
        }
		
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
		// System.out.println("Después del bloque try-catch de updateSpecialUserPath");
		
		initialTimeTotal = System.currentTimeMillis();
		
		/*
		 * THE PURPOSE OF THIS FUNCTION: SET RS user'S PATH
		 */
		// Modified by Nacho Palacio 2025-11-21
		finalPath = Arrays.asList(currentPath.split(", "));

		List<String> cleaned1 = new ArrayList<>();
		String lastEdge = null;
		for (String edge : finalPath) {
			if (!edge.equals(lastEdge)) {
				cleaned1.add(edge);
				lastEdge = edge;
			}
		}
		finalPath = cleaned1;

		graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
		if (finalPath == null) finalPath = new LinkedList<>();
		List<String> cleaned = new LinkedList<>();
		for (String e : finalPath) {
			String[] parts = cleanEdge(e);
			if (parts != null && parts.length >= 2) {
				if (!parts[0].equals(parts[1])) cleaned.add(e);
			} else {
				cleaned.add(e);
			}
		}
		finalPath = cleaned;

		graphSpecialUser.paths.set(((int) currentUser.userID) - 1, finalPath);
		idx = (int) currentUser.userID - 1;

        // finalPath no nulo
        if (finalPath == null) finalPath = new LinkedList<>();

        if (graphSpecialUser != null) {
            if (graphSpecialUser.paths == null) {
                graphSpecialUser.paths = new ArrayList<>();
            }
            while (graphSpecialUser.paths.size() <= idx) {
                graphSpecialUser.paths.add(new LinkedList<String>());
            }
            if (graphSpecialUser.paths.get(idx) == null) {
                graphSpecialUser.paths.set(idx, new LinkedList<String>());
            }
            graphSpecialUser.paths.set(idx, finalPath);
        }

        // userPositionInPath must be updated according to the new path length
        if (userPositionInPath != null && idx >= 0 && idx < userPositionInPath.length) {
            if (finalPath.isEmpty()) {
                userPositionInPath[idx] = 0;
            } else if (userPositionInPath[idx] >= finalPath.size()) {
                userPositionInPath[idx] = Math.max(0, finalPath.size() - 1);
            }
            if (userPositionInPath[idx] < 0) userPositionInPath[idx] = 0;
        }

        // reset flags to avoid loops
        if (voting != null && idx >= 0 && idx < voting.length) voting[idx] = false;
        isChangedItemByRecommender = false;

		// Print in file the paths.
		stopTime = System.currentTimeMillis();
		// Divide by 1000 to print the result in seconds.
		elapsedTime = (stopTime - startTime) / 1000;
		
		finalTimeTotal = System.currentTimeMillis();
		log.log(Level.FINE, "[updateSpecialUserPath]: POST - " + (finalTimeTotal - initialTimeTotal));
		
		// System.out.println("End recommendation");
	}

	/**
	 * Combines the old path with the recommended current path.
	 * Merges previously traveled path segments with newly recommended path,
	 * ensuring continuity in user navigation.
	 * 
	 * @param startVertex the start vertex
	 * @param endVertex the end vertex
	 * @param pathSpecialUser the user path
	 * @param finishPath whether this is the finish path
	 * @return the combined path as a list of edges
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

	/**
	 * Combines the old path with the recommended current path when there is disobedience.
	 * 
	 * @param nextItemSelected the next item selected by the user
	 * @param startVertex the start vertex
	 * @param endVertex the end vertex
	 * @param pathSpecialUser the user path
	 * @param specialUserID the RS user ID
	 * @return the combined path accounting for disobedience
	 */
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

	/**
	 * Filters out items that the user has already observed.
	 * Removes previously visited items from recommendation list to ensure
	 * only novel items are recommended.
	 * 
	 * @param recommendedItems the list of recommended items
	 * @param itemObservedOfUsers a map of user IDs to lists of observed item IDs
	 * @param userID the user ID
	 * @return a filtered list of recommended items (not yet observed)
	 */
	public List<RecommendedItem> filterAlreadyObservedItems(List<RecommendedItem> recommendedItems, Map<Integer, List<Long>> itemObservedOfUsers, int userID) {
		List<Long> observed = itemObservedOfUsers.get(userID);
		if (observed == null || observed.isEmpty()) {
			return recommendedItems; // Nada que filtrar
		}
		List<RecommendedItem> filtered = new ArrayList<>();
		for (RecommendedItem item : recommendedItems) {
			if (!observed.contains(item.getItemID())) {
				filtered.add(item);
			}
		}
		return filtered;
	}
	

	////////////////////////////////////////////////////////
	// MÉTODOS DE GENERACIÓN DE RUTAS
	////////////////////////////////////////////////////////
	
	/**
	 * Generate non-RS user paths.
	 * 
	 * @param strategy the path generation strategy
	 */
	public void generate_non_special_user_path(Path strategy) {	
		try {
			Map<Integer, List<Long>> roomItems = new HashMap<>();
			
			for (int i = 1; i <= dataAccessGraphFile.getNumberOfRoom(); i++) {
				List<Long> items = new LinkedList<>();
				for (int j = 1; j <= dataAccessGraphFile.getNumberOfItemsByRoom(i); j++) {
					long itemId = dataAccessGraphFile.getItemOfRoom(j, i);
					
					if (itemId > 0) {
						items.add(itemId);
					}
				}
				
				for (int j = 1; j <= dataAccessGraphFile.getNumberOfDoorsByRoom(i); j++) {
					long doorId = dataAccessGraphFile.getDoorOfRoom(j, i);
					if (doorId > 0) {
						items.add(doorId);
					}
				}
				
				long stairsId = dataAccessGraphFile.getStairsOfRoom(i);
				if (stairsId > 0) {
					items.add(stairsId);
				}
				
				if (!items.isEmpty()) {
					roomItems.put(i, items);
				}
			}
			
			strategy.initializeItemsByRoom(roomItems); // Modified by Nacho Palacio 2025-06-28
			
		} catch (Exception e) {
			e.printStackTrace();
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

				// Added by Nacho Palacio 2025-04-23
				long internalStartItem = ElementIdMapper.convertToRangeId(start_item, ElementIdMapper.CATEGORY_ITEM);

				int tryCount = 0;
				while(strategy.generatePath(start_item).toString().isEmpty() && tryCount < 20) {
					tryCount++;
					start_item = random.nextInt((strategy.accessItemFile.getNumberOfItems() - 1) + 1) + 1;
					if (start_item <= 0)
						System.out.println("ERROR: " + start_item);
				}
				// MainSimulator.printConsole("User generated path: " + (i + 1) + "; " + "Starting point: " + start_item, Level.INFO);
				// pw.writeBytes(strategy.generatePath(start_item) + "\n");

				// Modified by Nacho Palacio 2025-04-23
				MainSimulator.printConsole("User generated path: " + (i + 1) + "; " + "Starting point interno: " + internalStartItem + " (externo: " + start_item + ")", Level.INFO);
				// pw.writeBytes(strategy.generatePath(internalStartItem) + "\n");

				// Modified by Nacho Palacio 2025-04-24
				String generatedPath = strategy.generatePath(internalStartItem);

				generatedPath = convertPathIdsToExternal(generatedPath);

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
	 * Creates a fallback path when no path is available.
	 * Generates a minimal default path to prevent simulation errors.
	 * 
	 * @param userId the user ID
	 * @return a fallback path as a list of edges
	 */
	private List<String> createFallbackPath(int userId) {
		System.out.println("   Warning! building fallback path for user " + userId);
		List<String> fallbackPath = new ArrayList<>();
		fallbackPath.add("(1 : 1)"); // Minimal path
		return fallbackPath;
	}

	/**
	 * Converts a path string into a list of edges.
	 * 
	 * @param pathString the path string to convert
	 * @return a list of edge strings
	 */
	private List<String> pathStringToList(String pathString) {
		List<String> pathList = new ArrayList<>();
		
		if (pathString == null || pathString.isEmpty()) {
			return pathList;
		}
		
		String[] edges = pathString.split("\\), ");
		
		for (String edge : edges) {
			edge = edge.trim();
			if (!edge.isEmpty()) {
				if (!edge.endsWith(")")) {
					edge += ")";
				}
				pathList.add(edge);
			}
		}
		
		return pathList;
	}


	////////////////////////////////////////////////////////
	// CONTEXT AND RATING GENERATION METHODS
	////////////////////////////////////////////////////////

	/**
	 * Gets the current user's context.
	 * 
	 * @param currentUser the current user
	 * @return the context ID representing current conditions
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
		
		log.log(Level.INFO, "!!! CONTEXT [getContextIDFor]: " + (finalTimeContext - initialTimeContext));
		
		return currentContext;
	}

	/**
	 * Gets the number of people in a room.
	 * 
	 * @param room the room number
	 * @param currentUser the user making the query
	 * @return the number of other people in the room
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
	 * The user generates a rating to the item seen in a specific context.
	 * 
	 * @param uID the ID of the current user
	 * @param itemIDinternal the internal ID of the item seen
	 * @param itemIDexternal the external ID of the item seen
	 * @param contextID the ID of the user's context
	 * @return the generated rating value
	 */
	public float generateRating(long uID, long itemIDinternal, long itemIDexternal, long contextID) {
		float rating = 0;
		try {
			// Retrieves the external IDs (id_item) that the user has rated
			FastIDSet itemsFromUser = dataModelMuseumDB.getItemIDsFromUser(uID);

			if (itemsFromUser.contains(itemIDexternal)) {
				rating = dataModelMuseumDB.getPreferenceValue(uID, itemIDexternal, contextID);
			}
		} catch (TasteException ex) {
			ex.printStackTrace();
		}
		return rating;
	}

	////////////////////////////////////////////////////////
	// P2P PROPAGATION METHODS
	////////////////////////////////////////////////////////

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
	 * @param currentUser the current user
	 * @param users all users in the simulation
	 * @return list of users within communication range
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
	 * The neighbors listen the information.
	 * 
	 * @param informationToPropagateList the list with information to propagate
	 * @param neighborsInTheAllowedDistance the list of neighbors in communication range
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
	 * @param currentUser the current user
	 * @param neighborsOfTheCurrentUser the neighbors of the current user
	 * @return the farthest user among neighbors
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
	 * @param farthestUser the ID of the farthest user
	 * @param id_user the current user ID
	 * @return the number of propagated ratings
	 */
	public int passPropagationToken(long farthestUser, long id_user) {
		int countRatings = dataManagementQueueDBList_P2P.get((int) id_user - 1).updateExchange(farthestUser, id_user);
		return countRatings;
	}

	/**
	 * The information is propagated to the all neighbors.
	 * 
	 * @param neighborsInTheAllowedDistance list of neighbors to receive information
	 * @param id_user the current user ID
	 * @return the number of propagated ratings
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
	 * The RS user listen the information.
	 * 
	 * @param informationToPropagate the information to propagate
	 * @param currentUser the user receiving the information
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

	////////////////////////////////////////////////////////s
	// EPIDEMIC MANAGEMENT METHODS: Risk calculation
	////////////////////////////////////////////////////////

	/**
	 * Calculates individual risk for the current epidemic model.
	 * Delegates to model-specific risk calculator based on configured epidemic model.
	 * Added by Nacho Palacio 2025-09-18
	 * 
	 * @return average individual infection risk across all users
	 */
	private double calculateIndividualRiskForCurrentModel() {
        return EpidemicRiskCalculator.calculateCurrentModelRisk();
    }

	/**
	 * Calculates the average combined individual risk for all rooms based on current epidemic model.
	 * Aggregates risk across all rooms using model-specific calculations.
	 * 
	 * @return average individual risk across all rooms
	 */
	public double calculateCombinedIndividualRiskForAllRooms() {
		List<User> allUsers = getAllUsers();
		double totalRisk = 0.0;
		int susceptibleCount = 0;

		if (epidemicManager.getEpidemicModel() instanceof PengTransmissionModel) {
			PengTransmissionModel pengModel = (PengTransmissionModel) epidemicManager.getEpidemicModel();
			for (User user : allUsers) {
				UserEpidemicExtension ext = user.getEpidemicExtension();
				if (ext != null && ext.getHealthStatus() == HealthStatus.SUSCEPTIBLE && !ext.isImmune()) {
					double risk = pengModel.calculateCombinedInfectionRiskForUser(user);
					totalRisk += risk;
					susceptibleCount++;
				}
			}
		} else if (epidemicManager.getEpidemicModel() instanceof LelieveldTransmissionModel) {
			LelieveldTransmissionModel lelieveldModel = (LelieveldTransmissionModel) epidemicManager.getEpidemicModel();
			for (User user : allUsers) {
				UserEpidemicExtension ext = user.getEpidemicExtension();
				if (ext != null && ext.getHealthStatus() == HealthStatus.SUSCEPTIBLE && !ext.isImmune()) {
					double risk = lelieveldModel.calculateCombinedInfectionRiskForUser(user);
					totalRisk += risk;
					susceptibleCount++;
				}
			}
		}

		return susceptibleCount > 0 ? totalRisk / susceptibleCount : 0.0;
	}

	/**
	 * Calculates the average theoretical risk for all rooms based on current epidemic model.
	 * Computes theoretical infection risk using room-specific parameters and aerosol/proximity models.
	 * Modified by Nacho Palacio 2025-10-06
	 * 
	 * @return average theoretical risk across all rooms
	 */
	public double calculateAverageTheoreticalRiskForAllRooms() {
		EpidemicModel model = epidemicManager.getEpidemicModel();
		
		if (model == null) {
			System.err.println("Warning! No active epidemic model");
			return 0.0;
		}
		
		// Only susceptibles are considered
		List<User> susceptibles = getAllUsers().stream()
			.filter(u -> {
				UserEpidemicExtension ext = u.getEpidemicExtension();
				return ext != null && 
					!ext.isImmune() && 
					ext.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
			})
			.collect(Collectors.toList());
		
		if (susceptibles.isEmpty()) {
			System.err.println("Warning! No susceptible users to calculate risk");
			return 0.0;
		}
		
		System.out.printf("\n Calculating average risk for %d susceptible users...\n", susceptibles.size());
		
		double totalRisk = 0.0;
		
		if (model instanceof PengTransmissionModel) {
			PengTransmissionModel pengModel = (PengTransmissionModel) model;
			
			for (User user : susceptibles) {
				double userRisk = pengModel.calculateCombinedInfectionRiskForUser(user);
				totalRisk += userRisk;
			}
			
		} else if (model instanceof LelieveldTransmissionModel) {
			LelieveldTransmissionModel lelieveldModel = (LelieveldTransmissionModel) model;
			
			for (User user : susceptibles) {
				double userRisk = lelieveldModel.calculateCombinedInfectionRiskForUser(user);
				totalRisk += userRisk;
			}
			
		} else {
			System.err.println("Warning! Unsupported model for accumulated risk calculation");
			return 0.0;
		}
		
		double averageRisk = (totalRisk / susceptibles.size()) * 100.0;
		
		System.out.printf("\n FINAL AVERAGE RISK AVG: %.2f%%\n", averageRisk);
		System.out.printf("   (Average accumulated risk of %d susceptible users)\n\n", susceptibles.size());
		
		return averageRisk;
	}

	/**
	 * Calculates retrospective risk including all users (infected and susceptible).
	 * Infected users are assigned 100% risk since they were infected.
	 * Susceptible users get risk calculated from their exposure history.
	 * Added by Nacho Palacio 2025-10-11
	 * 
	 * @return average retrospective infection risk across all users
	 */
	public double calculateRetrospectiveRiskForAllUsers() {
		EpidemicModel model = epidemicManager.getEpidemicModel();
		
		if (model == null) {
			System.err.println("Warning! No active epidemic model");
			return 0.0;
		}
		
		List<User> allUsers = getAllUsers();
		if (allUsers.isEmpty()) {
			System.err.println("Warning! No users in the simulation");
			return 0.0;
		}
		
		double totalRisk = 0.0;
		int validUsers = 0;
		
		System.out.printf("\n Calculating retrospective risk for %d total users...\n", allUsers.size());
		
		for (User user : allUsers) {
			UserEpidemicExtension ext = user.getEpidemicExtension();
			
			// Ignore immune users
			if (ext == null || ext.isImmune()) {
				continue;
			}
			
			double userRisk = 0.0;
			
			if (ext.getHealthStatus() != HealthStatus.SUSCEPTIBLE) {
				userRisk = 1.0; // 100% because they are infected
				System.out.printf("    User %d (INFECTED): Risk = 100.00%%\n", user.userID);
			} else {
				if (model instanceof PengTransmissionModel) {
					PengTransmissionModel pengModel = (PengTransmissionModel) model;
					userRisk = pengModel.calculateCombinedInfectionRiskForUser(user);
				} else if (model instanceof LelieveldTransmissionModel) {
					LelieveldTransmissionModel lelieveldModel = (LelieveldTransmissionModel) model;
					userRisk = lelieveldModel.calculateCombinedInfectionRiskForUser(user);
				}
				
				System.out.printf("    User %d (SUSCEPTIBLE): Risk = %.2f%%\n", 
								user.userID, userRisk * 100);
			}
			
			totalRisk += userRisk;
			validUsers++;
		}
		
		if (validUsers == 0) {
			System.err.println("Warning! No valid users to calculate risk");
			return 0.0;
		}
		
		double averageRisk = (totalRisk / validUsers) * 100.0;
		
		System.out.printf("\n AVERAGE RETROSPECTIVE RISK: %.2f%%\n", averageRisk);
		System.out.printf("   (Average of %d users: infected + susceptible)\n", validUsers);
		System.out.printf("   - Infected: risk = 100%%\n");
		System.out.printf("   - Susceptible: risk = calculated by model\n\n");
		
		return averageRisk;
	}

	////////////////////////////////////////////////////////
	// EPIDEMIC MANAGEMENT METHODS: Tracking and statistics
	////////////////////////////////////////////////////////

	/**
	 * Infects one user per clique from the selected cliques.
	 * Randomly selects one member from each clique to be initially infected,
	 * ensuring diverse infection distribution across social groups.
	 * Added by Nacho Palacio 2025-11-10
	 * 
	 * @param selectedCliques the list of selected cliques
	 */
	private void infectOneUserPerClique(List<List<String>> selectedCliques) {
		if (selectedCliques == null || selectedCliques.isEmpty()) {
			System.err.println("   Warning! No cliques to infect");
			return;
		}
		
		System.out.println(" Infecting one user per clique...");
		
		Set<Integer> simulationUserIds = userList.stream()
			.map(u -> u.userID)
			.collect(Collectors.toSet());
				
		int cliquesProcessed = 0;
		int cliquesSkipped = 0;
		Set<Integer> infectedUsers = new HashSet<>();
		
		for (List<String> clique : selectedCliques) {
			if (clique.isEmpty()) {
				cliquesSkipped++;
				continue;
			}
			
			// Search a user in the clique who has not been infected yet
			User patientZero = null;
			int selectedSimulationId = -1;
			
			for (String userIdStr : clique) {
				try {
					int realUserId = Integer.parseInt(userIdStr);
					
					if (!ContactTrajectoryBuilder.hasSimulationId(realUserId)) {
						continue;
					}
					
					int simulationUserId = ContactTrajectoryBuilder.getSimulationId(realUserId);
					
					if (infectedUsers.contains(simulationUserId)) {
						continue;
					}
					
					if (!simulationUserIds.contains(simulationUserId)) {
						continue;
					}
					
					patientZero = userList.stream()
						.filter(u -> u.userID == simulationUserId)
						.findFirst()
						.orElse(null);
					
					if (patientZero != null) {
						selectedSimulationId = simulationUserId;
						break; // User found
					}
					
				} catch (NumberFormatException e) {
					System.err.println("Warning! Invalid ID in clique: " + userIdStr);
				}
			}
			
			// Infect the user
			if (patientZero != null && patientZero.getEpidemicExtension() != null) {
				boolean isSuperSpreader = Math.random() < 
					EpidemicConfiguration.getInstance().getSuperSpreaderProbability();
				
				if (isSuperSpreader) {
					patientZero.getEpidemicExtension().setHealthStatus(
						HealthStatus.SUPER_SPREADER
					);
					System.out.printf("    Clique %d: User %d (SUPERSPREADER) - Size: %d\n",
						cliquesProcessed + 1, selectedSimulationId, clique.size());
				} else {
					patientZero.getEpidemicExtension().setHealthStatus(
						HealthStatus.INFECTIOUS_SYMPTOMATIC
					);
					System.out.printf("    Clique %d: User %d (infected) - Size: %d\n",
						cliquesProcessed + 1, selectedSimulationId, clique.size());
				}
				
				infectedUsers.add(selectedSimulationId);
				cliquesProcessed++;
			} else {
				cliquesSkipped++;
			}
		}
		
		System.out.printf("\n✅ Total initially infected: %d (of %d cliques)\n",
			infectedUsers.size(), selectedCliques.size());
		System.out.printf("    Cliques processed: %d\n", cliquesProcessed);
		System.out.printf("     Cliques skipped: %d\n", cliquesSkipped);
		System.out.printf("    Unique cliques with infection: %d\n", infectedUsers.size());
	}

	/**
	 * Initializes the inter-clique coincidence tracker.
	 * Sets up tracking system to monitor when users from different cliques
	 * coincide in the same room, which is important for analyzing
	 * cross-clique infection patterns.
	 * Added by Nacho Palacio 2025-12-14
	 */
	private void initializeCoincidenceTracker() {
		if (cliqueUserMapping != null && !cliqueUserMapping.isEmpty()) {
			System.out.println("\n Initializing inter-clique coincidence tracker...");
			
			try {
				this.coincidenceTracker = new es.unizar.epidemic.data.InterCliqueCoincidenceTracker(
					cliqueUserMapping
				);
				
				System.out.println("   ✅ Tracker initialized successfully");
				System.out.println("      - Cliques tracked: " + cliqueUserMapping.size());
				System.out.println("      - Users in tracking: " + 
								cliqueUserMapping.values().stream()
									.mapToInt(List::size)
									.sum());
				
			} catch (Exception e) {
				System.err.println("    Error initializing tracker: " + e.getMessage());
				this.coincidenceTracker = null;
			}
		} else {
			System.out.println("\nWarning! Tracker not initialized (no clique data)");
			this.coincidenceTracker = null;
		}
	}

	/**
	 * Records initial susceptible count per clique.
	 * Stores baseline susceptible population for each clique before
	 * simulation begins, used to calculate attack rates per clique.
	 * Added by Nacho Palacio 2025-11-10
	 */
	private void recordInitialSusceptiblesByClique() {
		this.initialSusceptiblesByClique = new HashMap<>();
		
		System.out.println("\n Recording initial susceptibles by clique...");
		
		for (Map.Entry<Integer, List<Integer>> entry : cliqueUserMapping.entrySet()) {
			int cliqueIndex = entry.getKey();
			List<Integer> userIds = entry.getValue();
			
			int susceptibleCount = 0;
			
			for (int userId : userIds) {
				User user = userList.stream()
					.filter(u -> u.userID == userId)
					.findFirst()
					.orElse(null);
				
				if (user != null) {
					UserEpidemicExtension ext = user.getEpidemicExtension();
					
					if (ext != null && ext.getHealthStatus() == HealthStatus.SUSCEPTIBLE) {
						susceptibleCount++;
					}
				}
			}
			
			initialSusceptiblesByClique.put(cliqueIndex, susceptibleCount);
			
			System.out.printf("    Clique %d: %d initial susceptibles (of %d total users)\n",
							cliqueIndex + 1, susceptibleCount, userIds.size());
		}
		
		System.out.println("   ✅ Recording completed\n");
	}

	/**
	 * Prints final epidemic statistics when simulation ends.
	 * Displays comprehensive infection data including total infections,
	 * attack rates (global and per-clique), contact statistics, and
	 * model-specific metrics.
	 * Added by Nacho Palacio 2025-09-18
	 */
	private void printFinalEpidemicStatistics() {
		System.out.println("Entering printFinalEpidemicStatistics");
		if (!manualSimulation) {
			return;
		}	

		try {
			if (epidemicManager.getEpidemicModel() instanceof es.unizar.epidemic.models.PengTransmissionModel) {
				epidemicManager.evaluateFinalAerosolTransmissions(getAllUsers());
			}
			else if (epidemicManager.getEpidemicModel() instanceof es.unizar.epidemic.models.LelieveldTransmissionModel) {
				epidemicManager.evaluateFinalAerosolTransmissions(getAllUsers());
			}


			System.out.println("\n" + "=".repeat(100));
			System.out.println(" FINAL SIMULATION STATISTICS");
			System.out.println("=".repeat(100));

			if (coincidenceTracker != null) {
				System.out.println("\n Ending inter-clique coincidence tracking...");
				
				try {
					// Close all active coincidences
					coincidenceTracker.closeAllActiveCoincidences(simulationIterationCounter);
					
					coincidenceTracker.printAttackRatesByClique(
						userList,
						initialSusceptiblesByClique,     
						cliqueUserMapping          
					);

					coincidenceTracker.printIsolationMetrics();
                	coincidenceTracker.printDetailedUserCoincidences();
					
					// Export to CSV
					String csvPath = "./inter_clique_coincidences.csv";
					coincidenceTracker.exportToCSV(csvPath);
					System.out.println("   ✅ Coincidences exported to: " + csvPath);
					
				} catch (Exception e) {
					System.err.println("    Error in coincidence tracking: " + e.getMessage());
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Warning! Warning: Inter-clique coincidence tracking was not started");
			}
			
			EpidemicConfiguration config = es.unizar.epidemic.general.EpidemicConfiguration.getInstance();
			String model = config.getSelectedModel();
			
			List<User> users = getAllUsers();
			int totalUsers = users.size();
			int susceptible = 0, infectiousSymp = 0, infectiousAsymp = 0, superSpreaders = 0;
			
			for (User user : users) {
				if (user.getEpidemicExtension() != null) {
					switch (user.getEpidemicExtension().getHealthStatus()) {
						case SUSCEPTIBLE:
							susceptible++;
							break;
						case INFECTIOUS_SYMPTOMATIC:
							infectiousSymp++;
							break;
						case SUPER_SPREADER:
							superSpreaders++;
							break;
					}
				} else {
					susceptible++;
				}
			}
			
			int initialInfected = EpidemicConfiguration.getInstance().getInitialInfectedUsers();
			int newInfected = (totalUsers - susceptible) - initialInfected;
			config.setFinalInfectedUsers(initialInfected + newInfected);
			config.setTotalUsers(totalUsers);
			
			int initialSusceptibles = totalUsers - initialInfected;
			double attackRate = initialSusceptibles > 0 ? (double)newInfected / initialSusceptibles : 0.0;
			int totalInfectious = infectiousSymp + infectiousAsymp + superSpreaders;

			int totalContacts = 0;
			int infectiousContacts = 0;
			double averageConcentration = 0.0;
			
			try {
				es.unizar.epidemic.statistics.EpidemicStatistics stats = 
					es.unizar.epidemic.statistics.EpidemicStatistics.getInstance();
				totalContacts = stats.getTotalContacts();
				infectiousContacts = stats.getInfectiousContacts();
				
				// double concentration = stats.getAverageAerosolConcentration();
				// Modified by Nacho Palacio 2025-12-13
				double concentrationSimple = stats.getAverageAerosolConcentration();
				double concentrationPonderada = stats.getTimeWeightedAverageAerosolConcentration();
				
				System.out.println(" COMPARISON OF CONCENTRATIONS:");
				System.out.println("   - Simple average: " + String.format("%.6e", concentrationSimple));
				System.out.println("   - Weighted average: " + String.format("%.6e", concentrationPonderada));
				
				// Use the weighted average for the rest of the statistics
				double concentration = concentrationPonderada;

				averageConcentration = Double.isNaN(concentration) ? 0.0 : concentration;
			} catch (Exception e) {
				System.out.println("Warning! Warning: Could not obtain detailed epidemic statistics");
			}

			double individualRisk = calculateIndividualRiskForCurrentModel();
			double avgRisk = calculateAverageTheoreticalRiskForAllRooms();

			if ("SIMPLE_PROXIMITY".equals(model)) {
				System.out.printf("%-20s %-12s %-12s %-12s %-12s\n",
					"CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONTACTS", "INFECTIOUS CONT.");
			} else if ("AEROSOL_PENG".equals(model)) {
				System.out.printf("%-20s %-12s %-12s %-22s %-12s\n",
					"CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONCENTR. (quanta/m³)", "INDIVIDUAL RISK");
			} else if ("AEROSOL_LELIEVELD".equals(model)) {
				System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
					"CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONCENTR. (RNA copies/m³)", "INDIVIDUAL RISK");
			}


			if ("SIMPLE_PROXIMITY".equals(model)) {
				System.out.printf("%-20s %-12.2f %-12d %-12d %-12d\n",
					model,
					attackRate * 100,
					totalInfectious,
					totalContacts,
					infectiousContacts
				);
			} 
			else if ("AEROSOL_PENG".equals(model)) {
				System.out.printf("%-20s %-12.2f %-12d %-22.6f %-12.2f\n",
					model,
					attackRate * 100,
					totalInfectious,
					averageConcentration,
					avgRisk
				);
			} 
			else if ("AEROSOL_LELIEVELD".equals(model)) {
				System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f\n",
					model,
					attackRate * 100,
					totalInfectious,
					averageConcentration,
					avgRisk
				);
			}
			
			System.out.println("-".repeat(90));
			System.out.println("\n SIMULATION DETAILS:");
			System.out.printf("    Total users: %d\n", totalUsers);

			double elapsedSimulatedTime = 0.0;

			try {
				config = es.unizar.epidemic.general.EpidemicConfiguration.getInstance();
				double timePerIteration = getTimeForIterationInSecond();
				elapsedSimulatedTime = getCurrentSimulationIteration() * timePerIteration;
				
				System.out.printf("     Configured duration: %d minutes (%.0f seconds)\n", 
								config.getSimulationDuration(), 
								(double) config.getSimulationDurationSeconds());
				System.out.printf("     Elapsed simulated time: %.1f seconds (%.1f minutes)\n", 
								elapsedSimulatedTime, elapsedSimulatedTime / 60.0);
				System.out.printf("   Executed iterations: %d\n", getCurrentSimulationIteration());
				System.out.printf("   ⚡ Time per iteration: %.1f simulated seconds\n", timePerIteration);
			} catch (Exception e) {
				System.err.println("Warning! Error obtaining duration information: " + e.getMessage());
			}

			System.out.println("\n USER RATINGS:");
			double totalSum = 0.0;
			int totalCount = 0;
			// Debug userRatings
			for (Map.Entry<Integer, List<Float>> entry : userRatings.entrySet()) {
				int userId = entry.getKey();
				List<Float> ratings = entry.getValue();
				double sum = 0.0;
				for (float r : ratings) sum += r;
				double avg = ratings.isEmpty() ? 0.0 : sum / ratings.size();
				if (ratings.isEmpty()) {
					System.out.printf("    User %d: No ratings\n", userId);
				}
				System.out.printf("    User %d: %.2f (%d ratings)\n", userId, avg, ratings.size());
				totalSum += sum;
				totalCount += ratings.size();
			}
			double globalAvg = totalCount > 0 ? totalSum / totalCount : 0.0;
			System.out.printf("\n   ⭐ Global average rating: %.2f (%d total ratings)\n", globalAvg, totalCount);

			System.out.println("=".repeat(100));

			double globalAverageDistance = calculateGlobalAverageDistanceBetweenVisitedItems();

			System.out.println("=".repeat(100));

		} catch (Exception e) {
			System.err.println(" Error printing final statistics: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Gets infection statistics of all users.
	 * Returns summary string with counts of susceptible, infected, and recovered users.
	 * Added by Nacho Palacio 2025-07-30
	 * 
	 * @return formatted string with infection statistics
	 */
	private String getInfectionStatistics() {
		int susceptible = 0, infectiousAsymp = 0, infectiousSymp = 0, superSpreader = 0, recovered = 0;
		
		for (User user : userList) {
			UserEpidemicExtension extension = user.getEpidemicExtension();
			if (extension != null) {
				switch (extension.getHealthStatus()) {
					case SUSCEPTIBLE:
						susceptible++;
						break;
					case INFECTIOUS_SYMPTOMATIC:
						infectiousSymp++;
						break;
					case SUPER_SPREADER:
						superSpreader++;
						break;
				}
			}
		}

		return String.format("S:%d, I_A:%d, I_S:%d, SS:%d, R:%d", 
    		susceptible, infectiousAsymp, infectiousSymp, superSpreader, recovered);
	}

	////////////////////////////////////////////////////////
	// STATISTICS AND ANALYSIS METHODS: User statistics
	////////////////////////////////////////////////////////
	
	/**
	 * Ratings predicted and number of users watching same item. Called when the RS user is going to vote the item (because he/she has already seen it).
	 * 
	 * @param informationToPropagate the information being propagated
	 */
	private void updateSpecialUserItemStatistics(InformationToPropagate informationToPropagate) {
		updateRatingsPredicted(informationToPropagate);
		updateNumberUsersWatchingSameItem(informationToPropagate);
		
	}

	/**
	 * Stores all ratings predicted for an item, its current rating and timestamp.
	 * Maintains history of predicted ratings for analysis and evaluation.
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
	 * @param informationToPropagate the information about item viewing
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
	 * @param specialUserItemWatched the item ID being watched
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
	 * Stores in CSV file the information about the predicted ratings to calculate MAE.
	 * 
	 * @param recommendedItems the list of recommended items with predictions
	 * @param currentUser the user receiving recommendations
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
			}
		}
	}

	////////////////////////////////////////////////////////
	// STATISTICS AND ANALYSIS METHODS: Distances and metrics
	////////////////////////////////////////////////////////

	/**
	 * Update user distances checking if they are under DIST_THRESHOLD at least TIME_THRESHOLD seconds. Creates a list of users under DIST_THRESHOLD to call automaton.
	 * 
	 * @param timeStamp		current time to check TIME_THRESHOLD restriction
	 */
	public void updateUserDistances(int timeStamp) {
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
	 * Calculates the average distance between consecutively visited items.
	 * Added by Nacho Palacio 2025-11-06
	 * 
	 * @return Map with userId -> average distance in pixels
	 */
	public Map<Integer, Double> calculateAverageDistanceBetweenVisitedItems() {
		Map<Integer, Double> averageDistances = new HashMap<>();
		
		for (Map.Entry<Integer, List<String>> entry : actualPathTraveled.entrySet()) {
			int userId = entry.getKey();
			List<String> traveledPath = entry.getValue();
			
			if (traveledPath == null || traveledPath.size() < 1) {
				averageDistances.put(userId, 0.0);
				continue;
			}
			
			double totalDistance = 0.0;
			int itemPairs = 0;
			
			Long lastItem = null;
			double distanceSinceLastItem = 0.0;
			
			for (String edge : traveledPath) {
				String[] vertices = cleanEdge(edge);
				if (vertices.length < 2) continue;
				
				try {
					long v1 = Long.parseLong(vertices[0]);
					long v2 = Long.parseLong(vertices[1]);
					
					String loc1 = MainSimulator.floor.diccionaryItemLocation.get(v1);
					String loc2 = MainSimulator.floor.diccionaryItemLocation.get(v2);
					
					if (loc1 == null && ElementIdMapper.isInCorrectRange(v1, ElementIdMapper.CATEGORY_ITEM)) {
						loc1 = MainSimulator.floor.diccionaryItemLocation.get(ElementIdMapper.getBaseId(v1));
					}
					if (loc2 == null && ElementIdMapper.isInCorrectRange(v2, ElementIdMapper.CATEGORY_ITEM)) {
						loc2 = MainSimulator.floor.diccionaryItemLocation.get(ElementIdMapper.getBaseId(v2));
					}
					
					if (loc1 == null || loc2 == null) continue;
					
					double edgeDistance = distanceBetweenUsers(loc1, loc2);
					
					long v1External = ElementIdMapper.getBaseId(v1);
					long v2External = ElementIdMapper.getBaseId(v2);
					
					boolean v1IsItem = v1External <= this.numberOfITems;
					boolean v2IsItem = v2External <= this.numberOfITems;
					
					if (v1IsItem && v2IsItem) {
						if (lastItem != null) {
							totalDistance += distanceSinceLastItem + edgeDistance;
							itemPairs++;
						}
						lastItem = v2External;
						distanceSinceLastItem = 0.0;
					}
					else if (v1IsItem && !v2IsItem) {
						lastItem = v1External;
						distanceSinceLastItem = edgeDistance;
					}
					else if (!v1IsItem && !v2IsItem) {
						distanceSinceLastItem += edgeDistance;
					}
					else if (!v1IsItem && v2IsItem) {
						if (lastItem != null) {
							totalDistance += distanceSinceLastItem + edgeDistance;
							itemPairs++;
						}
						lastItem = v2External;
						distanceSinceLastItem = 0.0;
					}
					
				} catch (NumberFormatException e) {
				}
			}
			
			double avgDistance = itemPairs > 0 ? totalDistance / itemPairs : 0.0;
			
			averageDistances.put(userId, avgDistance);
		}
		
		return averageDistances;
	}

	/**
	 * Calculates the global average distance between visited items across all users
	 * Added by Nacho Palacio 2025-11-06
	 * 
	 * @return Global average distance in pixels
	 */
	public double calculateGlobalAverageDistanceBetweenVisitedItems() {
		Map<Integer, Double> userAverages = calculateAverageDistanceBetweenVisitedItems();
		
		if (userAverages.isEmpty()) {
			return 0.0;
		}
		
		double totalAverage = 0.0;
		int validUsers = 0;
		
		for (Double avgDistance : userAverages.values()) {
			if (avgDistance > 0) {
				totalAverage += avgDistance;
				validUsers++;
			}
		}
		
		double globalAverage = validUsers > 0 ? totalAverage / validUsers : 0.0;
		double globalAverageMeters = globalAverage * 1000 / getKmToPixel();
		
		System.out.println("\n📏 GLOBAL AVERAGE DISTANCE BETWEEN VISITED ITEMS (REAL PATH):");
		System.out.printf("    Average: %.2f pixels (%.2f meters)\n", globalAverage, globalAverageMeters);
		System.out.printf("    Users analyzed: %d\n", validUsers);
		
		return globalAverage;
	}

	////////////////////////////////////////////////////////
	// STATISTICS AND ANALYSIS METHODS: Room metrics
	////////////////////////////////////////////////////////
	
	/**
	 * Updates the entry time of a user into a room
	 * 
	 * @param user			User whose room entry is to be updated
	 * @param previousRoom	Previous room of the user
	 * @param currentRoom	Current room of the user
	 */
	private void updateUserRoomEntry(User user, int previousRoom, int currentRoom) {
		if (previousRoom != currentRoom && currentRoom >= 0) {
			userCurrentRoomEntry.put(user.userID, 
				new Pair<>(currentRoom, getCurrentSimulationTime()));
			System.out.printf("    User %d entered room %d at time %d seconds\n", 
				user.userID, currentRoom, getCurrentSimulationTime());
		}
	}

	/**
	 * Calculates the current duration spent by users in each room
	 */
	public Map<Integer, Double> calculateCurrentDurationPerRoom() {
		Map<Integer, Double> durationPerRoom = new HashMap<>();
		long currentTime = getCurrentSimulationTime(); // In seconds
		
		ElementIdMapper.SystemRangeData rangeData = ElementIdMapper.getSystemRangeData();
		int minRoomId = (int) rangeData.minRoomId;
		int maxRoomId = (int) rangeData.maxRoomId;
		
		for (int roomId = minRoomId; roomId <= maxRoomId; roomId++) {
			durationPerRoom.put(roomId, 0.0);
		}
		
		for (User user : userList) {
			int room = getUserRoomWithAdjustment(user);
			if (room < 0) continue;
			
			int adjustedRoom = room + minRoomId;
			
			Pair<Integer, Long> entry = userCurrentRoomEntry.get(user.userID);
			double timeInRoom = 0.0;
			
			if (entry != null && entry.getF() == room) {
				timeInRoom = (currentTime - entry.getS()) / 60.0;
				if (timeInRoom < 0) timeInRoom = 0;
			} else {
				timeInRoom = getTimeForIterationInSecond() / 60.0;
			}
			
			durationPerRoom.merge(adjustedRoom, timeInRoom, Double::sum);
		}
		
		return durationPerRoom;
	}

	/**
	 * Exports metrics for Python recommender system
	 * 
	 * @param timeUsersInRooms	Map with (userId, roomId) -> duration in seconds
	 * @param nVisitors			Number of visitors (for file naming)
	 */
	public void exportMetricsForPythonRecommender(Map<Pair<Integer, Integer>, Double> timeUsersInRooms, String nVisitors) {
		try {
			for (Map.Entry<Pair<Integer, Integer>, Double> entry : timeUsersInRooms.entrySet()) {
				Pair<Integer, Integer> userRoom = entry.getKey();
				double duration = entry.getValue();
				System.out.printf("    User %d: Room %d: %.2f seconds\n", userRoom.getF(), userRoom.getS(), duration);
			}

			Map<Integer, Integer> occupancyPerRoom = new HashMap<>();
			Map<Integer, Double> durationPerRoom = new HashMap<>();

			// Occupancy per room
			for (User user : userList) {
				if (!user.hasFinishedVisit && user.room > 0) {
					int currentRoom = user.room;
					occupancyPerRoom.put(currentRoom, occupancyPerRoom.getOrDefault(currentRoom, 0) + 1);
				}
			}

			// Duration per room
			for (Map.Entry<Pair<Integer, Integer>, Double> entry : timeUsersInRooms.entrySet()) {
				Pair<Integer, Integer> userRoom = entry.getKey();
				int room = userRoom.getS();
				double duration = entry.getValue();

				durationPerRoom.put(room, durationPerRoom.getOrDefault(room, 0.0) + duration);
			}

			for (Integer room : durationPerRoom.keySet()) {
				int occ = occupancyPerRoom.getOrDefault(room, 1);
				durationPerRoom.put(room, durationPerRoom.get(room) / occ);
			}

			File exportDir = new File("exports");
			if (!exportDir.exists()) exportDir.mkdir();


			File occupancyFile = new File(exportDir, "occupancy_" + nVisitors + ".csv");
			File durationFile = new File(exportDir, "duration_" + nVisitors + ".csv");

			try (PrintWriter occWriter = new PrintWriter(new FileWriter(occupancyFile));
				PrintWriter durWriter = new PrintWriter(new FileWriter(durationFile))) {

				occWriter.println("room_id,occupancy");
				durWriter.println("room_id,duration");

				int roomNumber = 1;
				int totalRooms = occupancyPerRoom.size();
				if (es.unizar.gui.MainSimulator.floor != null) {
                	roomNumber =  es.unizar.gui.MainSimulator.floor.getRoomCount();
            	}

				for (int i = 1; i <= roomNumber; i++) {
					int occ = occupancyPerRoom.getOrDefault(i, 0);
					double dur = durationPerRoom.getOrDefault(i, 0.0);
					occWriter.println(i + "," + occ);
					durWriter.println(i + "," + String.format(java.util.Locale.US, "%.3f", dur));
				}
			}

			System.out.println("✅ Files exported for the recommender: " +
				occupancyFile.getAbsolutePath() + " and " + durationFile.getAbsolutePath());

		} catch (Exception e) {
			System.err.println("Warning! Error exporting metrics for Python recommender: " + e.getMessage());
			e.printStackTrace();
		}
	}

	////////////////////////////////////////////////////////
	// HELPER METHODS: Conversion of ids and cleaning of edges
	////////////////////////////////////////////////////////

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
	 * Converts internal path ids to external ids.
	 * 
	 * @param path The path.
	 * @return The external path.
	 */
	private String convertPathIdsToExternal(String path) {
		if (path == null || path.isEmpty())
			return path;
			
		StringBuilder externalPath = new StringBuilder();
		String[] edges = path.split(", ");

		for (String edge : edges) {
			// Added by Nacho Palacio 2025-06-06

			if (edge.trim().isEmpty())
				continue;
				
			String[] vertices = cleanEdge(edge);
			if (vertices.length == 2) {
				long v1 = Long.parseLong(vertices[0]);
				long v2 = Long.parseLong(vertices[1]);
				
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

	////////////////////////////////////////////////////////
	// HELPER METHODS: Management of doors, stairs, and rooms
	////////////////////////////////////////////////////////
	
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

	/**
	 * Find the room of a door with corrected ID mapping.
	 * 
	 * @param doorVertex The door vertex.
	 * @return The room number.
	 * Modified by Nacho Palacio 2025-05-29
	 */
	private int findDoorRoomCorrected(long doorVertex) {
		try {
			if (ElementIdMapper.isInCorrectRange(doorVertex, ElementIdMapper.CATEGORY_DOOR)) {
				long baseId = ElementIdMapper.getBaseId(doorVertex);

				// Added by Nacho Palacio 2025-07-10
				ElementIdMapper.SystemRangeData rangeData = ElementIdMapper.getSystemRangeData();
				long doorStart = rangeData.totalItems + 1;
				long doorEnd = rangeData.totalItems + rangeData.totalDoors;
				
				long mappedBaseId;

				// Modified by Nacho Palacio 2025-07-10
				if (baseId >= doorStart && baseId <= doorEnd) {
					mappedBaseId = baseId;
				} else {
					mappedBaseId = doorStart + (baseId % (doorEnd - doorStart + 1));
				}
				
				int room = searchDoorInRooms(mappedBaseId);
				if (room > 0) {
					return room;
				}
				
				return 1;
			} else {
				return searchDoorInRooms(doorVertex);
			}
			
		} catch (Exception e) {
			// System.err.println("ERROR in findDoorRoomCorrected for ID " + doorVertex + ": " + e.getMessage());
		}
		
		return 1;
	}

	/**
	 * Searchs for an external door in all rooms and returns the room number if found, otherwise returns 0.
	 * 
	 * @param doorId The door ID.
	 * @return The room number.
	 * Added by Nacho Palacio 2025-05-28.
	 */
	private int findExternalDoorRoom(int doorId) {
		try {
			int numberOfRooms = dataAccessGraphFile.getNumberOfRoom();
			
			for (int room = 1; room <= numberOfRooms; room++) {
				int numberOfDoors = dataAccessGraphFile.getNumberOfDoorsByRoom(room);
				
				for (int door = 1; door <= numberOfDoors; door++) {
					long roomDoorId = dataAccessGraphFile.getDoorOfRoom(door, room);
					if (roomDoorId == doorId) {
						return room;
					}
				}
			}
		} catch (Exception e) {
			// System.err.println("ERROR searching external door room for ID " + doorId + ": " + e.getMessage());
		}
		
		return 0;
	}

	/**
	 * Searchs for a door in all rooms and returns the room number if found, otherwise returns 0.
	 * 
	 * @param doorId The door ID.
	 * @return The room number.
	 * Added by Nacho Palacio 2025-05-28.
	 */
	private int searchDoorInRooms(long doorId) {
		try {
			int numberOfRooms = dataAccessGraphFile.getNumberOfRoom();
			
			for (int room = 1; room <= numberOfRooms; room++) {
				int numDoors = dataAccessGraphFile.getNumberOfDoorsByRoom(room);
				
				for (int j = 1; j <= numDoors; j++) {
					long doorInRoom = dataAccessGraphFile.getDoorOfRoom(j, room);
					
					if (doorInRoom == doorId) {
						return room;
					}
				}
			}
		} catch (Exception e) {
		}
		
		return 0;
	}

	/**
	 * Gets user's room with proper ID adjustment for contact trajectories mode
	 * 
	 * @param user The user
	 * @return The room number
	 * Added by Nacho Palacio 2025-10-08
	 */
	public int getUserRoomWithAdjustment(User user) {
		int previousRoom = user.room;
		int roomCount = MainSimulator.floor.getRoomCount();
		user.getRoomOfTheUser();
		
		if (user.room == -1 && previousRoom >= 0 && previousRoom < roomCount) {
			user.room = previousRoom; // Previous room if current is invalid
		}
		
		if (user.room < 0 || user.room >= roomCount) {
			System.err.println("    User " + user.userID + " in invalid room (" + 
							user.room + "), forcing to room 1");
			user.room = 1; 
		}
		
		return user.room;
	}

	/**
	 * Gets the room from vertex. Get property directly from DataAccessItemFile (which has properties already loaded).
	 * 
	 * @param vertex: The vertex.
	 * @return The room.
	 */
	public int getRoom(long vertex) {
		if (vertex >= ElementIdMapper.ITEM_ID_START) {
			// Internal ID
			if (ElementIdMapper.isInCorrectRange(vertex, ElementIdMapper.CATEGORY_ITEM)) {
				long baseId = ElementIdMapper.getBaseId(vertex);
				String roomString = MainSimulator.floor.getGraphItemRoom((int) baseId);
				if (roomString != null && !roomString.trim().isEmpty()) {
					try {
						int room = Integer.valueOf(roomString.trim());
						return room;
					} catch (NumberFormatException e) {
					}
				}
				return 1;
				
			} else if (ElementIdMapper.isInCorrectRange(vertex, ElementIdMapper.CATEGORY_DOOR)) {
				int doorRoom = findDoorRoomCorrected(vertex); // Modified by Nacho Palacio 2025-05-29
				if (doorRoom > 0) {
					return doorRoom;
				}
				return 1;
				
			} else if (ElementIdMapper.isInCorrectRange(vertex, ElementIdMapper.CATEGORY_STAIRS)) {
				return 1;
				
			} else {
				return 1;
			}
		} else {
			// External ID
			if (vertex <= numberOfITems) {
				String roomString = MainSimulator.floor.getGraphItemRoom((int) vertex);
				if (roomString != null && !roomString.trim().isEmpty()) {
					try {
						int room = Integer.valueOf(roomString.trim());
						return room;
					} catch (NumberFormatException e) {
						// System.err.println("ERROR: Invalid room format for external item " + vertex + ": " + roomString);
					}
				}
				return 1;
			} else {
				int doorRoom = findExternalDoorRoom((int) vertex);
				if (doorRoom > 0) {
					return doorRoom;
				}
				
				return 1;
			}
		}
	}

	/**
	 * Builds item map by room for RandomPath
	 * Added by Nacho Palacio 2025-01-15
	 */
	private Map<Integer, List<Long>> buildRoomItemsMap() {
		Map<Integer, List<Long>> roomItems = new HashMap<>();
		
		try {
			DataAccessItemFile itemFile = new DataAccessItemFile(
				new File(es.unizar.util.Literals.ITEM_FLOOR_COMBINED));
			
			int numRooms = dataAccessGraphFile.getNumberOfRoom();
			int numItems = itemFile.getNumberOfItems();
			
			// Initialize lists for each room
			for (int roomId = 1; roomId <= numRooms; roomId++) {
				roomItems.put(roomId, new ArrayList<>());
			}
			
			// Assign items to rooms
			for (long itemId = 1; itemId <= numItems; itemId++) {
				String roomIdStr = itemFile.getItemRoom((int) itemId);
				
				int roomId = -1;
				try {
					roomId = Integer.parseInt(roomIdStr.trim());
				} catch (NumberFormatException e) {
					System.err.println("    Error parsing roomId for item " + itemId + 
									": '" + roomIdStr + "'");
					continue;
				}
				
				if (roomId > 0 && roomId <= numRooms) {
					roomItems.get(roomId).add(itemId);
				}
			}
			
			// Add doors to each room
			for (int roomId = 1; roomId <= numRooms; roomId++) {
				int numDoors = dataAccessGraphFile.getNumberOfDoorsByRoom(roomId);
				
				for (int doorIdx = 1; doorIdx <= numDoors; doorIdx++) {
					long doorId = dataAccessGraphFile.getDoorOfRoom(doorIdx, roomId);
					roomItems.get(roomId).add(doorId);
				}
			}	
		} catch (Exception e) {
			System.err.println("    Error constructing items map: " + e.getMessage());
		}
		
		return roomItems;
	}

	////////////////////////////////////////////////////////
	// MÉTODOS DE HELPERS: Helpers epidémicos
	////////////////////////////////////////////////////////

	/**
     * Builds the mapping from cliques to users from the selected cliques.
     * Added by Nacho Palacio 2025-12-03
     */
    private Map<Integer, List<Integer>> buildCliqueUserMappingFromSelectedCliques(
            List<List<String>> selectedCliques) {
        
        Map<Integer, List<Integer>> mapping = new HashMap<>();
        
        for (int cliqueIndex = 0; cliqueIndex < selectedCliques.size(); cliqueIndex++) {
            List<Integer> cliqueUsers = new ArrayList<>();
            
            for (String userIdStr : selectedCliques.get(cliqueIndex)) {
                try {
                    int realUserId = Integer.parseInt(userIdStr);
                    if (ContactTrajectoryBuilder.hasSimulationId(realUserId)) {
                        cliqueUsers.add(ContactTrajectoryBuilder.getSimulationId(realUserId));
                    }
                } catch (NumberFormatException e) {
                }
            }
            
            mapping.put(cliqueIndex, cliqueUsers);
        }
        
        return mapping;
    }

	/**
     * Prints a summary of the simplified assignment model. DEBUG
     * Added by Nacho Palacio 2025-12-03
     */
    private void printSimplifiedAssignmentSummary(
            List<List<String>> selectedCliques,
            Map<Integer, List<ContactTrajectoryBuilder.UserRoomEvent>> trajectories) {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ASSIGNMENT SUMMARY (SIMPLIFIED MODEL)");
        System.out.println("=".repeat(80));
        
        System.out.println("\n CIRCULAR ROTATION MODEL:");
        System.out.println("   - Event duration: " + EVENT_DURATION_SECONDS + "s (" + 
                         (EVENT_DURATION_SECONDS / 60) + " min)");
        System.out.println("   - Rooms: " + NUM_ROOMS);
        System.out.println("   - Cliques: " + selectedCliques.size());
        
        System.out.println("\n INITIAL ASSIGNMENT (t=0):");
        for (int cliqueIndex = 0; cliqueIndex < Math.min(10, selectedCliques.size()); cliqueIndex++) {
            int initialRoom = ContactTrajectoryBuilder.getRoomForCliqueAtTime(
                cliqueIndex, 0, EVENT_DURATION_SECONDS, NUM_ROOMS);
            System.out.printf("    Clique %d -> Room %d (%d users)\n",
                            cliqueIndex, initialRoom, selectedCliques.get(cliqueIndex).size());
        }
        if (selectedCliques.size() > 10) {
            System.out.println("   ... and " + (selectedCliques.size() - 10) + " more cliques");
        }
        
        System.out.println("\n✅ MODEL GUARANTEES:");
        System.out.println("   - 0% inter-clique overlaps (by design)");
        System.out.println("   - 100% intra-clique co-presence");
        System.out.println("   - Fair room rotation");
        
        System.out.println("=".repeat(80) + "\n");
    }

	/**
	 * Prints a summary of the mixed mode assignment model. DEBUG
	 * Added by Nacho Palacio 2025-01-15
	 */
	private void printMixedModeAssignmentSummary(
			int cliqueUsers,
			int independentUsers,
			List<List<String>> selectedCliques,
			List<Integer> independentUserIds) {
		
		System.out.println("\n" + "=".repeat(80));
		System.out.println(" ASSIGNMENT SUMMARY (MIXED MODE)");
		System.out.println("=".repeat(80));
		
		System.out.println("\n USERS IN CLIQUES:");
		System.out.println("   - Total: " + cliqueUsers);
		System.out.println("   - Cliques: " + selectedCliques.size());
		System.out.println("   - Model: Synchronized circular rotation");
		
		System.out.println("\n INDEPENDENT USERS:");
		System.out.println("   - Total: " + independentUsers);
		System.out.println("   - IDs: " + independentUserIds.get(0) + " - " + 
						independentUserIds.get(independentUserIds.size() - 1));
		System.out.println("   - Model: RandomPath (free movement)");
		System.out.println("   - cliqueId: -1 (marked as independent)");
		
		System.out.println("\n DISTRIBUTION:");
		System.out.printf("   - Cliques:        %d users (%.1f%%)\n", 
						cliqueUsers, (cliqueUsers * 100.0) / (cliqueUsers + independentUsers));
		System.out.printf("   - Independents: %d users (%.1f%%)\n", 
						independentUsers, (independentUsers * 100.0) / (cliqueUsers + independentUsers));
		
		System.out.println("\n HYPOTHESES TO VALIDATE:");
		System.out.println("   1. Users in cliques -> Higher intra-clique attack rate");
		System.out.println("   2. Independent users -> Lower global infection rate");
		System.out.println("   3. Peng and Lelieveld models -> Similar trends");
		
		System.out.println("=".repeat(80) + "\n");
	}	



	////////////////////////////////////////////////////////
	// MÉTODOS DE HELPERS: Debugging y monitoreo
	////////////////////////////////////////////////////////

	/**
	 * Prints the current PC time.
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
	 * Sets maximum number of users for testing purposes
	 * 
	 * @param maxUsers Maximum number of users
	 * Added by Nacho Palacio 2025-10-08
	 */
	public void setMaxUsersForTest(int maxUsers) {
		this.maxUsersForTest = maxUsers;
		System.out.println("   ✅ User limit for testing set to: " + maxUsers);
	}


	////////////////////////////////////////////////////////
	// FINALIZATION AND CLEANUP
	////////////////////////////////////////////////////////

	public void disconnect() {
		// printFinalEpidemicStatistics();
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


		// Added by Nacho Palacio 2024-12-08
        DBDataModel.clearAllPools();
        
        cachedTrajectoryStrategy = null;
        lastUsedGraph = null;
        
        if (graphSpecialUser != null) {
            graphSpecialUser.invalidateGraphCache();
        }
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

	////////////////////////////////////////////////////////
	// MÉTODOS DE INICIALIZACIÓN DE BASES DE DATOS
	////////////////////////////////////////////////////////

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
		System.out.println("Database instance created for centralized user DB: " + dataInstanceUserDB_Centralized);

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
	

	////////////////////////////////////////////////////////
	// GETTERS AND SETTERS
	////////////////////////////////////////////////////////
	
	/**
	 * Gets the time available for the user in hours.
	 * 
	 * @return time available in hours
	 */
	public int getTimeAvailableUserInHour() {
		return timeAvailableUser;
	}

	/**
	 * Gets the time available for the user in seconds.
	 * 
	 * @return time available in seconds
	 */
	public int getTimeAvailableUserInSecond() {
		return timeAvailableUser * 3600;
	}

	/**
	 * Gets the delay for observing a painting in hours.
	 * 
	 * @return delay observing painting in hours
	 */
	public int getDelayObservingPaintingInHour() {
		return delayObservingPainting / 3600;
	}

	/**
	 * Gets the delay for observing a painting in seconds.
	 * 
	 * @return delay observing painting in seconds
	 */
	public int getDelayObservingPaintingInSecond() {
		return delayObservingPainting;
	}

	/**
	 * Gets the time for an iteration in hours.
	 * 
	 * @return time for iteration in hours
	 */
	public double getTimeForIterationInHour() {
		return timeForIteration / 3600;
	}

	/**
	 * Gets the time for an iteration in seconds.
	 * 
	 * @return time for iteration in seconds
	 */
	public double getTimeForIterationInSecond() {
		return timeForIteration;
	}

	/**
	 * Gets the number of iterations for the specified simulated seconds.
	 * 
	 * @param seconds: The simulated seconds.
	 * @return number of iterations
	 */
	private int getIterationsForSimulatedSeconds(double seconds) {
		double timePerIteration = getTimeForIterationInSecond();
		return Math.max(1, (int) Math.ceil(seconds / timePerIteration));
	}

	/**
	 * Gets the screen refresh time in seconds.
	 * 
	 * @return screen refresh time in seconds
	 */
	public double getScreenRefreshTimeInSecond() {
		return screenRefreshTime;
	}

	/**
	 * Gets the time for the paths in hours.
	 * 
	 * @return time for the paths in hours
	 */
	public double getTimeForThePathsInHour() {
		return timeForThePaths;
	}

	/**
	 * Gets the time for the paths in seconds.
	 * 
	 * @return time for the paths in seconds
	 */
	public double getTimeForThePathsInSecond() {
		return timeForThePaths * 3600;
	}

	/**
	 * Gets the user velocity in km/h.
	 * 
	 * @return user velocity in km/h
	 */
	public double getUserVelocityInKmByHour() {
		return userVelocity;
	}

	/**
	 * Gets the user velocity in pixels/hour.
	 * 
	 * @return user velocity in pixels/hour
	 */
	public double getUserVelocityInPixelByHour() {
		return userVelocity * getKmToPixel();
	}

	/**
	 * Gets the user velocity in pixels/second.
	 * 
	 * @return user velocity in pixels/second
	 */
	public double getUserVelocityInPixelBySecond() {
		return userVelocity * getKmToPixel() / 3600;
	}

	/**
	 * Gets the conversion factor from kilometers to pixels.
	 * 
	 * @return kilometers to pixels conversion factor
	 */
	public double getKmToPixel() {
		return kmToPixel;
	}

	/**
	 * Gets the TTL (Time To Live) for network messages.
	 * 
	 * @return TTL value
	 */
	public int getTtl() {
		return ttl;
	}

	/**
	 * Gets the time users spend on stairs in seconds.
	 * 
	 * @return time on stairs in seconds
	 */
	public int getTimeOnStairs() {
		return timeOnStairs;
	}

	/**
	 * Gets the minimum time required to update recommendations.
	 * 
	 * @return minimum update time in seconds
	 */
	public int getMinimumTimeToUpdateRecommendation() {
		return minimumTimeToUpdateRecommendation;
	}

	/**
	 * Gets the communication range for P2P data exchange.
	 * 
	 * @return communication range in simulation units
	 */
	public int getCommunicationRange() {
		return communicationRange;
	}

	/**
	 * Gets the maximum knowledge base size for user data.
	 * 
	 * @return maximum knowledge base size
	 */
	public int getMaxKnowledgeBaseSize() {
		return maxKnowledgeBaseSize;
	}

	/**
	 * Gets the communication bandwidth for data transmission.
	 * 
	 * @return bandwidth in simulation units
	 */
	public int getCommunicationBandwidth() {
		return communicationBandwidth;
	}

	/**
	 * Gets the latency of transmission for network communication.
	 * 
	 * @return latency in milliseconds
	 */
	public int getLatencyOfTransmission() {
		return latencyOfTransmission;
	}

	/**
	 * Gets the time required to change user mood.
	 * 
	 * @return time to change mood in seconds
	 */
	public int getTimeToChangeMood() {
		return timeToChangeMood;
	}
	
	/**
	 * Gets the random seed used for simulation.
	 * 
	 * @return simulation seed value
	 */
	public long getSeed() {
		return simulationSeed.getSeed();
	}

	// Getters of Experimentation:
	/**
	 * Gets the number of special users in the simulation.
	 * 
	 * @return number of special users
	 */
	public int getNumberOfSpecialUser() {
		return numberOfSpecialUser;
	}

	/**
	 * Gets the number of non-special users in the simulation.
	 * 
	 * @return number of non-special users
	 */
	public int getNumberOfNonSpecialUser() {
		return numberOfNonSpecialUser;
	}

	/**
	 * Gets the paths for non-special users.
	 * 
	 * @return non-special user paths as string
	 */
	public String getNonSpecialUserPaths() {
		return nonSpecialUserPaths;
	}

	/**
	 * Gets the path generation strategy being used.
	 * 
	 * @return path strategy name
	 */
	public String getPathStrategy() {
		return pathStrategy;
	}

	/**
	 * Gets the recommendation algorithm being used.
	 * 
	 * @return recommendation algorithm name
	 */
	public String getRecommendationAlgorithm() {
		return recommendationAlgorithm;
	}

	/**
	 * Gets the threshold for recommendation acceptance.
	 * 
	 * @return recommendation threshold value
	 */
	public float getThresholdRecommendation() {
		return thresholdRecommendation;
	}

	/**
	 * Gets the similarity threshold for user matching.
	 * 
	 * @return similarity threshold value
	 */
	public double getThresholdSimilarity() {
		return thresholdSimilarity;
	}

	/**
	 * Gets the number of items to recommend.
	 * 
	 * @return number of items
	 */
	public int getHowMany() {
		return howMany;
	}

	/**
	 * Gets the network type for P2P communication.
	 * 
	 * @return network type (e.g., flooding, opportunistic)
	 */
	public String getNetworkType() {
		return networkType;
	}

	/**
	 * Gets the propagation strategy for data dissemination.
	 * 
	 * @return propagation strategy name
	 */
	public String getPropagationStrategy() {
		return propagationStrategy;
	}

	/**
	 * Gets the probability of user disobedience to recommendations.
	 * 
	 * @return disobedience probability (0.0 to 1.0)
	 */
	public double getProbabilityUserDisobedience() {
		return probabilityUserDisobedience;
	}

	/**
	 * Gets the number of votes received in the simulation.
	 * 
	 * @return number of votes received
	 */
	public int getNumberVoteReceived() {
		return numberVoteReceived;
	}

	/**
	 * Gets the total number of users in the simulation.
	 * 
	 * @return total number of users
	 */
	public int getNumberOfUser() {
		return numberOfUser;
	}
	
	/**
	 * Gets all users participating in the simulation.
	 * 
	 * @return list copy of all users
	 */
	public List<User> getAllUsers() {
		return new ArrayList<>(userList);
	}

	/**
	 * Gets the current iteration number.
	 * 
	 * @return current iteration since simulation start
	 */
	private int getCurrentIteration() {
		return (int) ((System.currentTimeMillis() - startTime) / 1000);
	}

	/**
	 * Gets the epidemic manager for disease spread tracking.
	 * 
	 * @return epidemic simulation manager instance
	 */
	public es.unizar.epidemic.general.EpidemicSimulationManager getEpidemicManager() {
		return epidemicManager;
	}

	/**
	 * Gets the current simulation time in seconds.
	 * 
	 * @return current simulated time
	 */
	private long getCurrentSimulationTime() {
		double timePerIteration = getTimeForIterationInSecond();
		return (long) (getCurrentSimulationIteration() * timePerIteration);
	}

	/**
	 * Checks if simulation is in mixed mode (independent + special users).
	 * 
	 * @return true if mixed mode is enabled, false otherwise
	 */
	public boolean isMixedMode() {
        return mixCliqueAndIndependentUsers;
    }
    
    /**
     * Gets the ratio of independent users in mixed mode.
     * 
     * @return independent user ratio (0.0 to 1.0)
     */
    public double getIndependentUserRatio() {
        return independentUserRatio;
    }
}