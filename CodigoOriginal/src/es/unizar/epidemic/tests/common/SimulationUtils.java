package es.unizar.epidemic.tests.common;

import es.unizar.epidemic.data.ContactTrajectoryBuilder;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.Scenarios;
import es.unizar.gui.Configuration;
import es.unizar.gui.FloorPanelCombined;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.UserInfo;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;
import es.unizar.gui.simulation.UserRunnable;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graphstream.ui.graphicGraph.stylesheet.Color;

import com.mxgraph.swing.mxGraphComponent;

/**
 * Shared utilities for all simulation tests
 * 
 * @author Nacho Palacio
 */
public class SimulationUtils {
    
    private static int currentIteration = 0;
    private static final int TARGET_SIM_SECONDS = 3600;
    public String simulationType;
    
    /**
     * Runs simulation to completion.
     * Initializes users, configures the epidemic system, and executes iterations
     * until the simulation reaches its time limit or maximum iterations.
     * Handles both standard and fallback initialization methods.
     * 
     * @param simulation the simulation instance to run
     */
    public static void runSimulationToCompletion(Simulation simulation) {
        if (simulation == null) {
            System.err.println(" Error: simulation is null");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        currentIteration = 0;

        Configuration.simulation = simulation;
        
        Map<Integer, UserInfo.UserState> stateOfUsers = new HashMap<>();
        Map<Pair<Integer,Integer>, Double> timeUsersInRooms = new HashMap<>();

        // Initialize centralized database if necessary
        if (simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
            simulation.initializeUserDB_Centralized(simulation.getRecommendationAlgorithm());
        }
        
        try {
            simulation.initializeUsers();
            
            List<User> users = simulation.getAllUsers();
            
            User[] userArray = users.toArray(new User[0]);
            
            UserRunnable realUserRunnable = new UserRunnable(
                userArray,
                stateOfUsers,
                timeUsersInRooms
            );
            MainSimulator.userRunnable = realUserRunnable;
            
            if (simulation.epidemicManager == null) {
                simulation.epidemicManager = new es.unizar.epidemic.general.EpidemicSimulationManager();
            }
            
            for (User user : users) {
                if (user.getEpidemicExtension() == null) {
                    user.setEpidemicExtension(new UserEpidemicExtension());
                }
                
                UserInfo.UserState userState = new UserInfo.UserState(user.room);
                stateOfUsers.put(user.userID, userState);
            }        
            
            // simulation.epidemicManager.initializeEpidemicSystem(users);   
            initializeEpidemicIfNeeded(simulation, users);

        } catch (Exception e) {
            System.err.println(" Error initializing users: " + e.getMessage());
            e.printStackTrace();
            
            System.out.println("    Attempting alternative initialization...");
            
            try {
                initializeUsersWithBasicLocations(simulation);
                System.out.println("   ✅ Users initialized with basic locations");
                
                List<User> users = simulation.getAllUsers();
                
                User[] userArray = users.toArray(new User[0]);
                UserRunnable fallbackUserRunnable = new UserRunnable(
                    userArray,
                    stateOfUsers,
                    timeUsersInRooms
                );
                MainSimulator.userRunnable = fallbackUserRunnable;
                
                if (simulation.epidemicManager == null) {
                    simulation.epidemicManager = new es.unizar.epidemic.general.EpidemicSimulationManager();
                }
                
                for (User user : users) {
                    if (user.getEpidemicExtension() == null) {
                        user.setEpidemicExtension(new UserEpidemicExtension());
                    }
                    
                    UserInfo.UserState userState = new UserInfo.UserState(user.room);
                    stateOfUsers.put(user.userID, userState);
                }
                
                // simulation.epidemicManager.initializeEpidemicSystem(users);
                initializeEpidemicIfNeeded(simulation, users);
                
            } catch (Exception e2) {
                System.err.println(" Error in alternative initialization: " + e2.getMessage());
                return;
            }
        }
        
        int maxIterations = Integer.MAX_VALUE;
        int iteration = 0;

        double simulatedTimeElapsed = 0.0;
        double timePerIteration = simulation.getTimeForIterationInSecond();

        startTime = System.currentTimeMillis();    
        try {
            while (!isSimulationComplete(simulation) && iteration < maxIterations) { 
                simulation.updateUsers(stateOfUsers, timeUsersInRooms);
                
                iteration++;
                currentIteration = iteration;
                simulatedTimeElapsed += timePerIteration;
            }
        } catch (Exception e) {
            System.err.println(" Error during simulation at iteration " + iteration + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (simulation.coincidenceTracker != null) {
            System.out.println("\n📊 Closing all active coincidences...");
            simulation.coincidenceTracker.closeAllActiveCoincidences(currentIteration);
            System.out.println("✅ All coincidences finalized");
        }
        
        System.out.println("   ✅ Simulation completed:");
        MainSimulator.userRunnable = null;
    }
    
    /**
     * Initializes users with basic locations if standard initialization fails.
     * Places users in predefined locations as a fallback mechanism when
     * the standard initialization process encounters errors.
     * 
     * @param simulation the simulation instance
     */
    private static void initializeUsersWithBasicLocations(Simulation simulation) {
        System.out.println("    Initializing users with basic locations...");
        List<User> users = simulation.getAllUsers();
        String[] basicLocations = {"100.0, 90.0", "100.0, 350.0", "250.0, 225.0"};
        
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            
            String location = basicLocations[i % basicLocations.length];
            
            user.room = (i % 2) + 1;
            
            user.move(location, user.room);
        }
    }
    
    /**
     * Verifies if the simulation has finished.
     * Checks if the simulated time has exceeded the configured duration
     * or if there are no users in the simulation.
     * 
     * @param simulation the simulation instance to check
     * @return true if the simulation is complete, false otherwise
     */
    private static boolean isSimulationComplete(Simulation simulation) {
        List<User> users = simulation.getAllUsers();
        
        if (users.isEmpty() || simulation.allUsersFinished) {
            System.out.println("\n--- SIMULATION FINISHED: NO USERS IN SIMULATION OR STOP FLAG SET ---");
            return true;
        }
        
        int maxSimulatedTimeSeconds = getMaxSimulatedTime();
        int elapsedSimulatedTime = getElapsedSimulatedTime(simulation);
        
        boolean timeExceeded = elapsedSimulatedTime >= maxSimulatedTimeSeconds;
        
        if (timeExceeded) {
            System.out.println("    Stop criteria: Simulated time completed (" + 
                            elapsedSimulatedTime + "/" + maxSimulatedTimeSeconds + " seconds)");
        }
        
        return timeExceeded;
    }

    /**
     * Gets the maximum simulated time in seconds.
     * Retrieves the configured simulation duration from epidemic configuration.
     * 
     * @return maximum simulated time in seconds
     */
    private static int getMaxSimulatedTime() {
        return EpidemicConfiguration.getInstance().getSimulationDurationSeconds();
    }

    /**
     * Gets the elapsed simulated time in seconds.
     * Calculates the total simulated time based on iterations completed
     * and time per iteration.
     * 
     * @param simulation the simulation instance
     * @return elapsed simulated time in seconds
     */
    private static int getElapsedSimulatedTime(Simulation simulation) {
        int totalIterations = getCurrentIteration();
        double secondsPerIteration = simulation.getTimeForIterationInSecond();
        
        return (int) (totalIterations * secondsPerIteration);
    }

    /**
     * Gets the current iteration number.
     * 
     * @return current iteration count
     */
    private static int getCurrentIteration() {
        return currentIteration;
    }
    
    /**
     * Collects simulation results.
     * Gathers health status counts, contact statistics, aerosol concentration,
     * individual risk, attack rate, and infection rate from the completed simulation.
     * 
     * @param simulation the completed simulation instance
     * @param scenario the test scenario used (or null for real contacts)
     * @param model the epidemic model name used
     * @return SimulationResult object containing all collected metrics
     */
    public static SimulationResult collectSimulationResults(
            Simulation simulation, 
            Scenarios.TestScenario scenario, 
            String model) {
        
        SimulationResult result = new SimulationResult();
        if(scenario == null) {
            result.scenarioName = "Real_Contacts";
        } else {
            result.scenarioName = scenario.name;
        }

        result.modelUsed = model;
        result.totalUsers = simulation.getAllUsers().size();
        
        List<User> users = simulation.getAllUsers();
        for (User user : users) {
            if (user.getEpidemicExtension() != null) {
                switch (user.getEpidemicExtension().getHealthStatus()) {
                    case SUSCEPTIBLE:
                        result.susceptible++;
                        break;
                    case INFECTIOUS_SYMPTOMATIC:
                        result.infectiousSymp++;
                        break;
                    case SUPER_SPREADER:
                        result.superSpreaders++;
                        break;
                }
            }
        }

        // Antes de calcular attackRate:
        int initialInfected = EpidemicConfiguration.getInstance().getInitialInfectedUsers();
        int initialSusceptibles = result.totalUsers - initialInfected;
        int newInfected = (result.totalUsers - result.susceptible) - initialInfected;

        result.attackRate = initialSusceptibles > 0 ? 
            (double) newInfected / initialSusceptibles : 0.0;
        
        try {
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            result.totalContacts = stats.getTotalContacts();
            result.infectiousContacts = stats.getInfectiousContacts();
            
            double concentration = stats.getAverageAerosolConcentration();
            result.averageConcentration = Double.isNaN(concentration) ? 0.0 : concentration;

            result.individualRisk = simulation.calculateAverageTheoreticalRiskForAllRooms();

            if (simulation.coincidenceTracker != null) {
                try {
                    // Get isolation rate from coincidence tracker
                    Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
                
                    result.isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
                    
                    if (result.isolationRate == 0.0 && metrics.containsKey("totalCoincidences")) {
                        int totalCoincidences = (int) metrics.get("totalCoincidences");
                        if (totalCoincidences > 0) {
                            List<es.unizar.epidemic.data.CoincidenceRecord> interCliqueCoincidences = 
                                simulation.coincidenceTracker.getCompletedCoincidences().stream()
                                    .filter(r -> r.getClique1() != r.getClique2())
                                    .collect(java.util.stream.Collectors.toList());
                            
                            if (!interCliqueCoincidences.isEmpty()) {
                                double totalSimulationTime = EpidemicConfiguration.getInstance()
                                    .getSimulationDurationSeconds();
                                
                                double totalCoincidenceTime = interCliqueCoincidences.stream()
                                    .mapToDouble(r -> r.getDurationSeconds(
                                        simulation.getTimeForIterationInSecond()))
                                    .sum();
                                
                                int totalUsers = simulation.getAllUsers().size();
                                double totalUserTime = totalSimulationTime * totalUsers;
                                result.isolationRate = 1.0 - (totalCoincidenceTime / totalUserTime);
                            }
                        }
                    }
                    
                    // Calculate intra-clique attack rate if cliques exist
                    if (simulation.cliqueUserMapping != null && !simulation.cliqueUserMapping.isEmpty()) {
                        double totalIntraAttack = 0.0;
                        int cliqueCount = 0;
                        
                        for (Map.Entry<Integer, List<Integer>> entry : simulation.cliqueUserMapping.entrySet()) {
                            int cliqueId = entry.getKey();
                            List<Integer> userIds = entry.getValue();
                            
                            initialSusceptibles = simulation.initialSusceptiblesByClique
                                .getOrDefault(cliqueId, userIds.size());
                            
                            if (initialSusceptibles == 0) continue;

                            int initialInfectedInClique = simulation.initialInfectedByClique
                                .getOrDefault(cliqueId, 0);
                            
                            int totalInfected = 0;
                            for (int userId : userIds) {
                                User user = simulation.getAllUsers().get(userId - 1);
                                if (user.getEpidemicExtension() != null &&
                                    simulation.isUserInfected(user.getEpidemicExtension())) {
                                    totalInfected++;
                                }
                            }
                            
                            int newInfections = totalInfected - initialInfectedInClique;
                            
                            
                            double cliqueAttackRate = (double) newInfections / initialSusceptibles;
                            totalIntraAttack += cliqueAttackRate;
                            cliqueCount++;
                        }
                        
                        result.intraCliqueAttackRate = cliqueCount > 0 ? totalIntraAttack / cliqueCount : 0.0;
                    } else {
                        result.intraCliqueAttackRate = 0.0;
                    }
                    
                } catch (Exception e) {
                    System.err.println("  Warning: Could not extract contact metrics: " + e.getMessage());
                    e.printStackTrace();
                    result.isolationRate = 0.0;
                    result.intraCliqueAttackRate = 0.0;
                }
            } else {
                result.isolationRate = 0.0;
                result.intraCliqueAttackRate = 0.0;
            }
            
        } catch (Exception e) {
            System.err.println(" Warning: Could not obtain epidemic statistics: " + e.getMessage());
            result.totalContacts = 0;
            result.infectiousContacts = 0;
            result.averageConcentration = 0.0;
            result.individualRisk = 0.0;
        }
        
        initialInfected = EpidemicConfiguration.getInstance().getInitialInfectedUsers();
        initialSusceptibles = result.totalUsers - initialInfected;
        newInfected = (result.totalUsers - result.susceptible) - initialInfected;
        result.attackRate = initialSusceptibles > 0 ? (double) newInfected / initialSusceptibles : 0.0;
        result.infectionRate = (double) (result.infectiousSymp + result.infectiousAsymp + result.superSpreaders) / result.totalUsers;
        result.simulationDurationSeconds = EpidemicConfiguration.getInstance()
            .getSimulationDurationSeconds();
        
        return result;
    }
    
    /**
     * Creates simulation for scenario.
     * Loads map files, configures paths, initializes the simulation environment,
     * and sets up the floor graph visualization. Supports both synthetic trajectories
     * and real contact data from CSV.
     * 
     * @param scenario the test scenario configuration
     * @param totalUsers total number of users to simulate
     * @param infectiousUsers initial number of infectious users
     * @param scenarioName name of the scenario directory
     * @param useRealContacts whether to use real contact data from CSV
     * @return configured Simulation instance, or null if creation fails
     */
        public static Simulation createSimulationForScenario(
            Scenarios.TestScenario scenario,
            int totalUsers,
            int infectiousUsers,
            String scenarioName,
            boolean useRealContacts) {

        return createSimulationForScenario(
            scenario,
            totalUsers,
            infectiousUsers,
            scenarioName,
            useRealContacts,
            null
        );
    }

    /**
     * Creates simulation for scenario with time override.
     * Extended version of createSimulationForScenario that allows overriding the time per iteration.
     * @param scenario
     * @param totalUsers
     * @param infectiousUsers
     * @param scenarioName
     * @param useRealContacts
     * @param timeForIterationOverride
     * @return
     */
    public static Simulation createSimulationForScenario(
            Scenarios.TestScenario scenario,
            int totalUsers,
            int infectiousUsers,
            String scenarioName,
            boolean useRealContacts,
            Double timeForIterationOverride) {

        System.out.println("    Loading " + scenarioName + " from specific files...");

        File mapDir = new File("./dist/resources/maps/" + scenarioName + "/");
        if (!mapDir.exists()) {
            mapDir = new File("./resources/maps/" + scenarioName + "/");
        }

        if (!mapDir.exists()) {
            System.err.println(" Error: Directory not found: " + scenarioName);
            return null;
        }

        File roomFile = new File(mapDir, "room_floor_combined.txt");
        File itemFile = new File(mapDir, "item_floor_combined.txt");
        File graphFile = new File(mapDir, "graph_floor_combined.txt");

        int specialUsers;
        int nonSpecialUsers;
        String recommendationAlgorithm = EpidemicConfiguration.getInstance().getRecommendationAlgorithm();

        if (requiresSpecialUsers(recommendationAlgorithm)) {
            specialUsers = totalUsers;
            nonSpecialUsers = 0;
        } else {
            specialUsers = 0;
            nonSpecialUsers = totalUsers;
        }

        if (useRealContacts) {
            try {
                String csvPath = "../src/es/unizar/epidemic/data/contactos.csv";
                int uniqueUsersInCSV = ContactTrajectoryBuilder.getUniqueUserCount(csvPath);

                System.out.println("    Total users in CSV: " + uniqueUsersInCSV);

                int usersToSimulate = Math.min(totalUsers, uniqueUsersInCSV);

                System.out.println("    Limiting simulation to: " + usersToSimulate + " users");

                nonSpecialUsers = usersToSimulate;
                specialUsers = 0;
                totalUsers = usersToSimulate;

            } catch (Exception e) {
                System.err.println("   Warning! Error counting CSV users: " + e.getMessage());
            }
        }

        String pathsFileName = "rand_non_special_user_paths_" + totalUsers + ".txt";
        File pathsFile = new File(mapDir, pathsFileName);

        boolean allowMissingPathsFile = (Configuration.instance != null &&
            Configuration.instance.getContactTrajectoryMode() ==
                Configuration.ContactTrajectoryMode.REAL_CHRONOLOGY);

        if (!roomFile.exists() || !itemFile.exists() || !graphFile.exists() ||
            (!pathsFile.exists() && !allowMissingPathsFile)) {
            System.err.println(" Error: Missing files for: " + scenarioName);
            return null;
        }

        if (!pathsFile.exists() && allowMissingPathsFile) {
            try {
                pathsFile.createNewFile();
                System.out.println("    Created placeholder paths file: " + pathsFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println(" Error creating placeholder paths file: " + e.getMessage());
                return null;
            }
        }

        es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
        es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
        es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();

        try {
            double timeForIteration = (timeForIterationOverride != null)
                ? timeForIterationOverride
                : 100.0;
            int howMany = 50;

            System.out.println("Configuring simulation with TimeForIteration: " + timeForIteration);

            Simulation simulation = new Simulation(
                17, 30, timeForIteration, 1.0, 1.0,
                3.0, 6597, 180, 60, 30, 250,
                1, 54, 1, specialUsers, nonSpecialUsers,
                pathsFile.getAbsolutePath(), "Random Path", recommendationAlgorithm, 2.5f, howMany,
                "Opportunistic", 0.4, 40, 0.5, "Centralized (Centralized)", 1800,
                false, System.currentTimeMillis(), false, false, 0.0
            );

            System.out.println("    Simulation instance created successfully");

            SimulationUtils.resetAllSimulationState(simulation);

            Configuration.simulation = simulation;
            simulation.configureElementIdMapperForCurrentScenario();
            System.out.println("    Simulation created with " + totalUsers + " users (" + specialUsers + " special, " + nonSpecialUsers + " non-special)");

            if (useRealContacts && Configuration.instance == null) {
                Configuration.instance = new Configuration(null, false);
            }
            System.out.println("    Configuration instance: " + (Configuration.instance != null ? "exists" : "null"));

            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            System.out.println("    FloorPanelCombined instance: " + (MainSimulator.floorPanelCombined != null ? "exists" : "null"));

            DrawFloorGraph floor = null;
            mxGraphComponent graphComponent = null;

            if (!MainSimulator.HEADLESS_MODE) {
                System.out.println("    Initializing floor graph visualization...");
                if (MainSimulator.floorPanelCombined == null) {
                    MainSimulator.floorPanelCombined =
                        new FloorPanelCombined(MainSimulator.DRAWING_WIDTH,
                                                MainSimulator.DRAWING_HEIGHT);
                }
                System.out.println("    FloorPanelCombined instance: " + (MainSimulator.floorPanelCombined != null ? "exists" : "null"));

                floor = new DrawFloorGraph();

                graphComponent = floor.drawFloor(
                    roomFile, itemFile, true, false, 1);

                System.out.println("    Floor graph drawn successfully");

                graphComponent.setToolTips(true);
                graphComponent.getViewport().setBackground(new java.awt.Color(255, 255, 255));
                graphComponent.getViewport().setOpaque(true);

                System.out.println("    Configuring floor graph component...");

                MainSimulator.floorPanelCombined.removeAll();
                MainSimulator.floorPanelCombined.add(graphComponent);
                MainSimulator.floorPanelCombined.revalidate();
                MainSimulator.floorPanelCombined.repaint();

                System.out.println("    Floor graph component configured and added to panel");

                floor.loadDiccionaryItemLocation();

                System.out.println("    Floor graph dictionary loaded");

                MainSimulator.floor = floor;
                MainSimulator.graphComponent = graphComponent;
            }

            return simulation;

        } catch (Exception e) {
            System.err.println(" Error creating simulation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates simulation with specific contact mode.
     * Configures a simulation using the specified contact trajectory mode
     * (Rotational or Complex), enabling mode-specific testing and comparison.
     * 
     * @param totalUsers total number of users to simulate
     * @param scenarioName name of the scenario directory
     * @param contactMode the contact trajectory mode to use
     * @return configured Simulation instance with specified contact mode
     */
    public static Simulation createSimulationWithContactMode(
            int totalUsers,
            String scenarioName,
            Configuration.ContactTrajectoryMode contactMode) {
                
        if (Configuration.instance == null) {
            System.out.println("        Configuration.instance is NULL - creating new instance");
            try {
                Configuration.instance = new Configuration(null, false);
                System.out.println("      ✅ Configuration.instance created successfully");
            } catch (Exception e) {
                System.err.println("      ❌ Error creating Configuration instance: " + e.getMessage());
                e.printStackTrace();
                
                try {
                    Configuration.instance = new Configuration(null, false);
                    System.out.println("      ✅ Configuration.instance created with alternative constructor");
                } catch (Exception e2) {
                    System.err.println("      ❌ Could not create Configuration instance");
                    e2.printStackTrace();
                    return null;
                }
            }
        }
        
        Configuration.instance.contactTrajectoryMode = contactMode;
        System.out.println("      ✅ Contact mode set to: " + Configuration.instance.contactTrajectoryMode.getDisplayName());
        
        
        Simulation simulation = createSimulationForScenario(
            null,
            totalUsers,
            1,
            scenarioName,
            true 
        );
        
        return simulation;
    }

    /**
     * Creates simulation with real chronology.
     * Configures a simulation to use real contact chronology from CSV data,
     * enabling the most realistic trajectory generation based on observed contact patterns.
     * @param totalUsers total number of users to simulate (should not exceed unique users in CSV for realism)
     * @param cliqueId the clique ID to use for chronology (if applicable, otherwise can be set to 0 or ignored)
     * @param scenarioName name of the scenario directory
     * @param timeForIteration the time to use for each iteration (can be adjusted for performance or realism)
     * @return configured Simulation instance with real chronology contact trajectories, or null if creation fails
     */
    public static Simulation createSimulationWithRealChronology(
            int cliqueId,
            String scenarioName) {
        
        try {
            String cliquesPath = Literals.CLIQUES_JSON;
            ContactTrajectoryBuilder.SelectedUsersResult selection =
                ContactTrajectoryBuilder.selectUsersFromCliqueId(cliquesPath, cliqueId);
            
            int totalUsers = selection.users.size();
            
            if (totalUsers <= 0) {
                throw new Exception("No users found in clique " + cliqueId);
            }
            
            // Configurar antes de crear la simulación
            Configuration config = new Configuration(null, false);
            config.contactTrajectoryMode = Configuration.ContactTrajectoryMode.REAL_CHRONOLOGY;
            config.setChronologyCliqueId(cliqueId);
            
            // Ahora crear con el número real de usuarios
            return createSimulationForScenario(
                    null,
                    totalUsers,  // ← Número real obtenido de la clique
                    1,           // infectiousUsers
                    scenarioName,
                    false
            );
            
        } catch (Exception e) {
            System.err.println("Error in createSimulationWithRealChronology: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initializes epidemic system only if NOT in REAL_CHRONOLOGY mode.
     * In REAL_CHRONOLOGY, the initialization is handled by infectOneUserPerCliqueByChronology().
     * 
     * @param simulation the simulation instance
     * @param users list of users to initialize
     */
    private static void initializeEpidemicIfNeeded(Simulation simulation, List<User> users) {
        boolean isRealChronology = (Configuration.instance != null && 
            Configuration.instance.getContactTrajectoryMode() == Configuration.ContactTrajectoryMode.REAL_CHRONOLOGY);
        
        if (isRealChronology) {
            System.out.println("   ⏭️  Skipping epidemic initialization (REAL_CHRONOLOGY mode - will be handled by infectOneUserPerCliqueByChronology)");
            return;
        }
        
        System.out.println("   🦠 Initializing epidemic system...");
        simulation.epidemicManager.initializeEpidemicSystem(users);
    }
    
    /**
     * Configures epidemic parameters.
     * Sets model-specific parameters including ventilation rates, viral loads,
     * quanta emission rates, and other transmission parameters based on the
     * selected epidemic model.
     * 
     * @param config the epidemic configuration instance to modify
     * @param scenario the test scenario containing base parameters
     * @param model the epidemic model identifier (AEROSOL_PENG, AEROSOL_LELIEVELD, etc.)
     */
    public static void configureEpidemicParameters(
            EpidemicConfiguration config, 
            Scenarios.TestScenario scenario, 
            String model) {
        
        config.setSelectedModel(model);

        if (config.getRecommendationAlgorithm() == null) {
            config.setRecommendationAlgorithm("Completely-random (FULLY-RAND)");
        }
        
        String configName = config.getConfigName();
        boolean isBaseline = configName == null || configName.equals("Default") || configName.equals("Baseline_Control");
        
        if (scenario != null && isBaseline) {
            config.setDefaultVentilationRate(scenario.ventilationRate);
            config.setVirusDecayRate(scenario.virusDecayRate);
        }
        
        switch (model) {
            case "AEROSOL_PENG":
                config.setQuantaEmissionRate(232.5);
                config.setBreathingRate(0.72);
                config.setDepositionRate(0.3);
                break;
            case "AEROSOL_LELIEVELD":
                config.setViralLoadHigh(5e6);
                config.setViralLoadSuper(5E7);
                config.setInfectiousDose(316);
                config.setDepositionProbability(0.5);
                break;
        }
    }
    
    /**
     * Runs a single simulation.
     * Configures epidemic parameters, creates the simulation, executes it to completion,
     * evaluates final aerosol transmissions, and collects results with execution timing.
     * 
     * @param scenario the test scenario to simulate
     * @param model the epidemic model to use
     * @param scenarioNumber the scenario number identifier
     * @return SimulationResult containing all metrics and execution time
     */
    public static SimulationResult runSingleSimulation(
            Scenarios.TestScenario scenario, 
            String model, 
            int scenarioNumber) {
        
        long startTime = System.currentTimeMillis();
        try {
            EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
            configureEpidemicParameters(epidemicConfig, scenario, model);

            int totalUsers = epidemicConfig.getTotalUsers();
            int infectiousUsers = epidemicConfig.getInitialInfectedUsers();

            // epidemicConfig.printCurrentConfiguration();

            boolean useRealContacts = false;

            Simulation simulation = createSimulationForScenario(
                scenario, totalUsers, infectiousUsers, "MoMA_Museum", useRealContacts);
            
            if (simulation == null) {
                System.err.println(" Error: Could not create simulation");
                return new SimulationResult();
            }
            
            runSimulationToCompletion(simulation);

            if (simulation.getEpidemicManager() != null) {
                simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(
                    simulation.getAllUsers());
            }

            SimulationResult result = collectSimulationResults(simulation, scenario, model);
        
            long endTime = System.currentTimeMillis();
            result.executionTimeMs = endTime - startTime;
            
            return result;
            
        } catch (Exception e) {
            System.err.println(" Error in simulation: " + e.getMessage());
            e.printStackTrace();
            return new SimulationResult();
        }
    }

    /**
     * Compares model results (used by multiple test classes).
     * Delegates to ModelComparisonAnalyzer to perform comparative analysis
     * of results from different epidemic models.
     * 
     * @param results map of model names to their simulation results
     * @param scenarioName name of the scenario being compared
     * @param recommendationAlgorithm the recommendation algorithm name
     */
    public static void compareModelResults(Map<String, SimulationResult> results, 
                                        String scenarioName,
                                        String recommendationAlgorithm,
                                        int totalUsers,
                                        int simulationDurationSeconds) {
        es.unizar.epidemic.tests.comparison.ModelComparisonAnalyzer.compareModelResults(
            results, scenarioName, recommendationAlgorithm, totalUsers, simulationDurationSeconds);
    }
    
    /**
     * Determines if a recommendation algorithm requires special users.
     * 
     * @param recommendationAlgorithm the recommendation algorithm name
     * @return true if the algorithm requires special users, false otherwise
     */
    private static boolean requiresSpecialUsers(String recommendationAlgorithm) {
        if (recommendationAlgorithm == null) {
            System.out.println(" Warning: recommendation algorithm is null");
            return false;
        }
        
        // Algoritmos que necesitan usuarios especiales
        return recommendationAlgorithm.contains("Risk-Aware") || 
            recommendationAlgorithm.contains("Non-Risk-Aware");
    }

    /**
     * Resets all simulation state (both static and instance).
     * Clears all accumulated metrics, statistics, and collections from previous simulations.
     * Should be called before creating a new Simulation instance.
     * 
     * @param simulation the simulation instance to reset instance-level state, or null to skip
     */
    public static void resetAllSimulationState(Simulation simulation) {
        // Reset static state
        Simulation.resetStaticSimulationState();
        
        // Reset instance state
        if (simulation != null) {
            simulation.resetInstanceState();
        }
    }
}