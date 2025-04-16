package es.unizar.util;

import java.io.File;
import java.util.logging.Level;

public class Literals {

	/**
	 * File paths
	 */
	public static final File file = new File("");
	/* Modified by silarri (2022-07-15). */
	public static final String RELATIVE_PATH_IMAGES = File.separator + "resources" + File.separator + "images" + File.separator;
	public static final String PATH = file.getAbsolutePath() + File.separator + "resources" + File.separator;
	public static final String PATH_MAPS = PATH + "maps" + File.separator;
	public static final String PATH_RECOMMENDERS = PATH + "recommenders" + File.separator;
	public static final String PATH_SIMULATIONS = PATH + "simulations" + File.separator;
	
	/* Modified by silarri (2022-07-13). */
	public static final String IMAGES_PATH = file.getAbsolutePath() + File.separator + "resources" + File.separator + "images" + File.separator;
	//public static final String IMAGES_PATH = file.getAbsolutePath() + File.separator + "src" + File.separator + "es" + File.separator + "unizar" + File.separator + "images" + File.separator;
	public static final String LOGO_PATH = IMAGES_PATH + "RecMobiSim.png";

	/**
	 * File names
	 */
	public static final String ROOM_FILE_NAME = "room_floor_combined.txt";
	public static final String ITEM_FILE_NAME = "item_floor_combined.txt";
	public static final String GRAPH_FILE_NAME = "graph_floor_combined.txt";
	
	public static final String ROOM_FLOOR_4 = PATH_MAPS + "room_floor4.txt";
	public static final String ROOM_FLOOR_5 = PATH_MAPS + "room_floor5.txt";
	public static String ROOM_FLOOR_COMBINED = PATH_MAPS + ROOM_FILE_NAME;

	public static final String ITEM_FLOOR_4 = PATH_MAPS + "item_floor4.txt";
	public static final String ITEM_FLOOR_5 = PATH_MAPS + "item_floor5.txt";
	public static String ITEM_FLOOR_COMBINED = PATH_MAPS + ITEM_FILE_NAME;

	public static final String GRAPH_FLOOR_4 = PATH_MAPS + "graph_floor4.txt";
	public static final String GRAPH_FLOOR_5 = PATH_MAPS + "graph_floor5.txt";
	public static String GRAPH_FLOOR_COMBINED = PATH_MAPS + GRAPH_FILE_NAME;

	public static final String SPECIAL_USER_PATH = PATH_MAPS + "special_user_path.txt"; // OJO
	
	public static final String RECOMMENDERS_FILE = PATH_RECOMMENDERS + "RecommenderDefinition.txt";

	/**
	 * Database name
	 */
	// Drivers from DBs.
	public static final String SQL_DRIVER = "jdbc:sqlite:" + File.separatorChar;
	// It contains all the items voted by the users. Users: 176, Items: 240, Contexts: 9
	public static final String DB_ALL_USERS_PATH = PATH + "db" + File.separatorChar + "db_museum.db";
	// It is an example of RS user database.
	public static final String DB_USER_PATH = PATH + "db" + File.separatorChar + "db_user.db";
	// It contains item ratings of the current user for a P2P architecture.
	public static final String DB_P2P_USER_PATH = PATH + "db" + File.separatorChar + "db_p2p_user_";
	// It contains item ratings of users for a centralized architecture.
	public static final String DB_CENTRALIZED_USER_PATH = PATH + "db" + File.separatorChar + "db_centralized_user_all.db";
	// It an empty database that supports the exchange of information between users in a P2P way.
	public static final String DB_QUEUE_PATH = PATH + "db" + File.separatorChar + "queue.db";
	// It a database for user that supports the exchange of information between users in a P2P way.
	public static final String DB_NEW_QUEUE_PATH = PATH + "db" + File.separatorChar + "db_p2p_queue_user_";
	// It contains all ratings and ratings predicted for every seen item.
	public static final String CSV_RATINGS = PATH + "db" + File.separatorChar + "items_ratings_predicted.csv";
	// It contains the the number of users that were watching the same item as RS user
	public static final String CSV_USERS_SAME_ITEM = PATH + "db" + File.separatorChar + "users_watching_same_item.csv";
	// It contains the the number of users that were watching the same item as RS user
	public static final String CSV_USERS_DISTANCES_STATS = PATH + "db" + File.separatorChar + "distances_under_threshold_for_minTimeThreshold.csv";
	
	/**
	 * Constant "org.sqlite.JDBC"
	 */
	public static final String SQLITE = "org.sqlite.JDBC";

	/**
	 * Common attributes
	 */
	public static final String NAME = "name";
	public static final String MAP_WIDTH = "map_width";
	public static final String MAP_HEIGHT = "map_height";
	public static final String MAP_PIXEL_REPRESENTS_METERS = "map_pixel_represents_meters";

	/**
	 * Item file
	 */
	public static final String NUMBER_ITEMS = "number_items";
	public static final String VERTEX_DIMENSION_HEIGHT = "vertex_dimension_height";
	public static final String VERTEX_DIMENSION_WIDTH = "vertex_dimension_width";
	public static final String ITEM_ID = "item_itemID_";
	public static final String ITEM_TITLE = "item_title_";
	public static final String ITEM_ARTIST = "item_artist_";
	public static final String ITEM_CONSTITUENTID = "item_constituentID_";
	public static final String ITEM_ARTISTBIO = "item_artistBio_";
	public static final String ITEM_NATIONALITY = "item_nationality_";
	public static final String ITEM_BEGINDATE = "item_beginDate_";
	public static final String ITEM_ENDDATE = "item_endDate_";
	public static final String ITEM_GENDER = "item_gender_";
	public static final String ITEM_DATE = "item_date_";
	public static final String ITEM_MEDIUM = "item_medium_";
	public static final String ITEM_DIMENSIONS = "item_dimensions_";
	public static final String ITEM_CREDITLINE = "item_creditLine_";
	public static final String ITEM_ACCESSIONNUMBER = "item_accessionNumber_";
	public static final String ITEM_DEPARTMENT = "item_department_";
	public static final String ITEM_DATEACQUIRED = "item_dateAcquired_";
	public static final String ITEM_CATALOGED = "item_cataloged_";
	public static final String ITEM_DEPTH = "item_depth_";
	public static final String ITEM_DIAMETER = "item_diameter_";
	public static final String ITEM_HEIGHT = "item_height_";
	public static final String ITEM_WEIGHT = "item_weight_";
	public static final String ITEM_WIDTH = "item_width_";
	public static final String ITEM_ROOM = "item_room_";
	public static final String VERTEX_LABEL = "vertex_label_";
	public static final String VERTEX_XY = "vertex_xy_";
	public static final String VERTEX_URL = "vertex_url_";
	public static final String VISITABLE = "visitable_";

	/**
	 * Room file
	 */
	public static final String NUMBER_ROOM = "number_room";
	public static final String LABEL = "room_label_";
	public static final String NUMBER_CORNER = "room_number_corner_";
	public static final String CORNER = "room_corner_xy_";
	public static final String NUMBER_DOOR = "room_number_door_";
	public static final String DOOR = "room_door_xy_";
	public static final String NUMBER_STAIRS = "number_room_stairs";
	public static final String STAIRS = "room_stairs_";
	
	// TO STORE AND CREATE SUBROOMS -> NOT VISIBLE IN SIMULATOR, JUST FOR BUILDING GRAPH
	public static final String NUMBER_SUBROOMS = "room_number_subrooms_";
	public static final String SUBROOM_LABEL = "subroom_label_";
	public static final String ROOM_NUMBER_INVISIBLE_DOOR = "room_number_invisible_door_";
	public static final String ROOM_INVISIBLE_DOOR = "room_invisible_door_xy_";
	// IN ORDER TO BE ABLE TO SAVE ROOMSEPARATORS IN MAPS (EDITOR)
	public static final String ROOM_NUMBER_ROOMSEPARATORS = "room_number_roomseparators_";
	public static final String ROOM_ROOMSEPARATOR_CORNERS = "room_roomseparator_corners_";

	/**
	 * Graph file
	 */
	public static final String ROOM = "room_";
	public static final String NUMBER_ITEMS_BY_ROOM = "number_items_";
	public static final String ITEM_OF_ROOM = "item_";
	public static final String NUMBER_DOORS_BY_ROOM = "number_doors_";
	public static final String DOOR_OF_ROOM = "door_";
	public static final String NUMBER_STAIRS_BY_MAP = "number_stairs";
	public static final String STAIRS_OF_ROOM = "stairs_";
	public static final String NUMBER_CONNECTED_DOOR = "number_connected_door";
	public static final String CONNECTED_DOOR = "connected_door_";
	public static final String NUMBER_CONNECTED_DOOR_STAIRS = "number_connected_door_stairs";
	public static final String CONNECTED_DOOR_STAIRS = "connected_door_stairs_";
	public static final String NUMBER_CONNECTED_STAIRS = "number_connected_stairs";
	public static final String CONNECTED_STAIRS = "connected_stairs_";
	
	public static final String NUMBER_ROOM_SUBROOMS = "number_room_subrooms";
	public static final String SUBROOM = "subroom_";
	public static final String NUMBER_ITEMS_BY_SUBROOM = "number_items_subroom_";
	public static final String ITEM_OF_SUBROOM = "item_subroom_";
	public static final String NUMBER_DOORS_BY_SUBROOM = "number_doors_subroom_";
	public static final String DOOR_OF_SUBROOM = "door_subroom_";
	public static final String NUMBER_INVISIBLE_DOORS_BY_SUBROOM = "number_invisible_doors_subroom_";
	public static final String INVISIBLE_DOOR_OF_SUBROOM = "invisible_door_subroom_";
	public static final String NUMBER_CONNECTED_INVISIBLE_DOOR = "number_connected_invisible_door";
	public static final String CONNECTED_INVISIBLE_DOOR = "connected_invisible_door_";
	
	/**
	 * Recommender's file
	 */
	public static final String RECOMMENDER_NAME = "Recommender_name_";
	public static final String RECOMMENDER_USES_P2P = "Recommender_uses_P2P_";
	
	/**
	 * Simulations' file
	 */
	public static final String NUMBER_SIMULATIONS = "number_simulations";
	public static final String TIME_AVAILABLE_USER = "timeAvailableUser_";
	public static final String DELAY_OBSERVING_PAINTING = "delayObservingPainting_";
	public static final String TIME_FOR_ITERATION = "timeForIteration_";
	public static final String SCREEN_REFRESH_TIME = "screenRefreshTime_";
	public static final String TIME_FOR_PATHS = "timeForThePaths_";
	public static final String USER_VELOCITY = "userVelocity_";
	public static final String KM_TO_PIXEL = "kmToPixel_";
	public static final String TTL = "ttl_";
	public static final String TIME_ON_STAIRS = "timeOnStairs_";
	public static final String MINIMUM_TIME_TO_UPDATE_RECOMMENDATION = "minimumTimeToUpdateRecommendation_";
	public static final String COMMUNICATION_RANGE = "communicationRange_";
	public static final String MAX_KNOWLEDGE_BASE_SIZE = "maxKnowledgeBaseSize_";
	public static final String COMMUNICATION_BANDWIDTH = "communicationBandwidth_";
	public static final String LATENCY_OF_TRANSMISSION = "latencyOfTransmission_";
	public static final String TIME_TO_CHANGE_MOOD = "timeToChangeMood_";
	public static final String USE_FIXED_SEED = "useFixedSeed_";
	public static final String SEED = "seed_";
	public static final String NUMBER_OF_SPECIAL_USERS = "numberOfSpecialUser_";
	public static final String NUMBER_OF_NON_SPECIAL_USERS = "numberOfNonSpecialUser_";
	public static final String NON_SPECIAL_USER_PATHS = "nonSpecialUserPaths_";
	public static final String PATH_STRATEGY = "pathStrategy_";
	public static final String RECOMMENDATION_ALGORITHM = "recommendationAlgorithm_";
	public static final String THRESHOLD_RECOMMENDATION = "thresholdRecommendation_";
	public static final String THRESHOLD_SIMILARITY = "thresholdSimilarity_";
	public static final String HOW_MANY = "howMany_";
	public static final String NETWORK_TYPE = "networkType_";
	public static final String PROPAGATION_STRATEGY = "propagationStrategy_";
	public static final String PROBABILITY_USER_DISOBEDIENCE = "probabilityUserDisobedience_";
	public static final String NUMBER_VOTES_RECEIVED = "numberVoteReceived_";
	public static final String GENERATE_USER_PATHS = "generateUserPaths_";
	

	/**
	 * File of experiments
	 */
	public static final String EXPERIMENTAL_FILE = file.getAbsolutePath() + File.separator + "experimental.csv";
	public static final String EXPERIMENTAL_RATED_FILE = file.getAbsolutePath() + File.separator + "experimental_rateditems.csv";
	public static final String EXPERIMENTAL_LOG_NEIGHBORS = file.getAbsolutePath() + File.separator + "log_neighbors.csv";
	public static final String EXPERIMENTAL_LOG_PROPAGATE = file.getAbsolutePath() + File.separator + "log_propagate.csv";
	public static final String EXPERIMENTAL_LOG_SEND_QUEUE_MAP = file.getAbsolutePath() + File.separator + "log_sendQueueMap.csv";
	public static final String EXPERIMENTAL_LOG_TIME_EXCHANGE_DATA = file.getAbsolutePath() + File.separator + "log_time_exchangeData.csv";

	/**
	 * Other parameter settings:
	 */
	public static final long ITEM_FICTITIOUS = 400;
	public static final int TOTAL_USERS = 200;
	
	/**
	 * Loggers:
	 */
	// Console info messages
	public static final String CONSOLE_LOGGER = "CONSOLE_LOGGER";
	// Debug messages used to measure times
	public static final String DEBUG_MESSAGES = "DEBUG_MESSAGES";
	
	// Logger's levels (level value or higher will be printed)
	// IMPORTANT: the lower the level set, the higher the messages printed -> The higher the buffer delay (to print all messages), affects simulator's performance
	public static final Level CONSOLE_DEFAULT_LEVEL = Level.WARNING;
	public static final Level DEBUG_DEFAULT_LEVEL = Level.SEVERE; // SEVERE/WARNING to ignore, ALL to debug
	
	// If the time consumed is remarkable (>= than this value), this message should be printed no matter its level
	// IMPORTANT: the lower the time (ms) set, the higher the messages printed -> The higher the buffer delay (to print all messages), affects simulator's performance
	public static final int DEBUG_MIN_TIME_TO_BE_PRINTED = 20; // in ms
	
	// Beginning message -> Print it always when debugging (DebugFilter) -> To know where a whole cycle begins (UserRunnable iteration)
	public static final String BEGINNING_DEBUG_MESSAGE = "--- VUELTA ---";
	// Ending message -> Print it always when debugging (DebugFilter) -> To know where a whole cycle ends (UserRunnable iteration)
	public static final String ENDING_DEBUG_MESSAGE = "--------------\n";
	
	/**
	 * Multiple databases
	 */
	// Different DB possibilities
	public static enum Databases{
		SQLITE,
		//MEMORY
	}
	
	// DB being used.
	public static final Databases currentDBUsed = Databases.SQLITE;
	// Config value to persist or not queue values in db.
	public static final boolean queuePersistanceConfigValue = true;
	
	/**
	 * Stats
	 */
	public static final boolean COMPILE_ITEM_STATS = true; // Store item stats (ratings predicted FOR UBCF P2P and number of people watching same item) -> ADDS SOME DELAY
	public static final boolean COMPILE_DISTANCES_STATS = true; // Store distances' stats in file or not -> ADDS DELAY
	
	public static final int DIST_THRESHOLD = 25; // in meters
	public static final int TIME_THRESHOLD = 60; // in seconds
}
