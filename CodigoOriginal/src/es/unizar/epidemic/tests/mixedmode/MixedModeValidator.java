package es.unizar.epidemic.tests.mixedmode;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.simulation.Simulation;

import java.io.File;

/**
 * Validator for mixed mode (cliques + independent users)
 * 
 * @author Nacho Palacio
 */
public class MixedModeValidator {
    
    /**
     * Main entry point for mixed mode validation.
     * Executes a complete test of mixed mode simulation (cliques + independent users),
     * configuring epidemic parameters, creating the simulation, running it to completion,
     * collecting results, and validating hypotheses about contact patterns.
     */
    public static void run() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🧪 VALIDATION TEST: MIXED MODE (CLIQUES + INDEPENDENTS)");
        System.out.println("=".repeat(100));
        
        try {
            //  TEST CONFIGURATION
            int totalUsers = 50;
            double independentRatio = 0.3; // 30% independientes
            String scenarioName = "MoMA_Museum";
            String model = "AEROSOL_PENG";
            
            //  CONFIGURE EPIDEMIC PARAMETERS
            EpidemicConfiguration config = EpidemicConfiguration.getInstance();
            config.setSelectedModel(model);
            config.setInitialInfectedUsers(5);
            config.setMaskComplianceRate(0.1);
            config.setDefaultVentilationRate(3.0);
            config.setSimulationDurationSeconds(360);
            
            //  CREATE SIMULATION WITH MIXED MODE
            System.out.println("\n Test configuration:");
            System.out.println("   - Total users: " + totalUsers);
            System.out.println("   - Independent ratio: " + (independentRatio * 100) + "%");
            System.out.println("   - Users in cliques: " + (int)((1.0 - independentRatio) * totalUsers));
            System.out.println("   - Independent users: " + (int)(independentRatio * totalUsers));
            System.out.println("   - Epidemic model: " + model);
            
            Simulation simulation = createSimulationWithMixedMode(
                totalUsers, 
                independentRatio, 
                scenarioName
            );
            
            if (simulation == null) {
                throw new Exception("Could not create simulation in mixed mode");
            }
            
            //  EXECUTE SIMULATION
            System.out.println("\n⏳ Executing simulation...");
            SimulationUtils.runSimulationToCompletion(simulation);
            
            //  COLLECT RESULTS
            SimulationResult result = SimulationUtils.collectSimulationResults(simulation, null, model);
            
            //  PRINT RESULTS
            System.out.println("\n" + "=".repeat(100));
            System.out.println(" TEST RESULTS");
            System.out.println("=".repeat(100));
            
            System.out.printf("Global attack rate: %.2f%%\n", result.attackRate * 100);
            System.out.printf("Total infectious: %d\n", 
                result.infectiousSymp + result.infectiousAsymp + result.superSpreaders);
            
            //  VERIFY HYPOTHESES
            System.out.println("\n HYPOTHESIS VALIDATION:");
            System.out.println("   (Implement specific metrics when working correctly)");
            printContactStatistics(simulation);
            
            System.out.println("\n" + "=".repeat(100));
            System.out.println(" TEST COMPLETED");
            System.out.println("=".repeat(100) + "\n");
            
        } catch (Exception e) {
            System.err.println(" Error in mixed mode test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates simulation with mixed mode activated.
     * Configures a simulation where a portion of users belong to cliques while
     * the remaining users move independently. Sets up the scenario files, paths,
     * and simulation parameters with the specified independent ratio.
     * 
     * @param totalUsers total number of users in the simulation
     * @param independentRatio fraction of users that move independently (0.0 to 1.0)
     * @param scenarioName name of the scenario directory containing map files
     * @return configured Simulation instance with mixed mode enabled, or null if creation fails
     */
    private static Simulation createSimulationWithMixedMode(
            int totalUsers,
            double independentRatio,
            String scenarioName) {
        
        System.out.println("    Creating simulation in MIXED MODE:");
        System.out.println("      - Total users: " + totalUsers);
        System.out.println("      - Independent ratio: " + (independentRatio * 100) + "%");
        
        try {
            // Try both paths
            java.io.File mapDir = new java.io.File("./dist/resources/maps/" + scenarioName + "/");
            if (!mapDir.exists()) {
                mapDir = new java.io.File("./resources/maps/" + scenarioName + "/");
            }
            
            if (!mapDir.exists()) {
                System.err.println(" Error: Directory not found: " + scenarioName);
                System.err.println("   Attempted paths:");
                System.err.println("      - ./dist/resources/maps/" + scenarioName + "/");
                System.err.println("      - ./resources/maps/" + scenarioName + "/");
                return null;
            }
            
            System.out.println(" Directory found: " + mapDir.getAbsolutePath());
            
            // Build files from verified mapDir
            File roomFile = new File(mapDir, "room_floor_combined.txt");
            File itemFile = new File(mapDir, "item_floor_combined.txt");
            File graphFile = new File(mapDir, "graph_floor_combined.txt");
            
            // Verify that files exist
            if (!roomFile.exists() || !itemFile.exists() || !graphFile.exists()) {
                System.err.println(" Error: Missing files for: " + scenarioName);
                System.err.println("   - Room file: " + roomFile.exists() + 
                                " (" + roomFile.getAbsolutePath() + ")");
                System.err.println("   - Item file: " + itemFile.exists() + 
                                " (" + itemFile.getAbsolutePath() + ")");
                System.err.println("   - Graph file: " + graphFile.exists() + 
                                " (" + graphFile.getAbsolutePath() + ")");
                return null;
            }
            
            System.out.println("    Verified files:");
            System.out.println("      - room_floor_combined.txt: " + roomFile.getAbsolutePath());
            System.out.println("      - item_floor_combined.txt: " + itemFile.getAbsolutePath());
            System.out.println("      - graph_floor_combined.txt: " + graphFile.getAbsolutePath());
            
            // Configure literals before creating Simulation
            es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
            es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
            es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();
            
            System.out.println("    Literals configured correctly");
            
            // Simulation parameters
            int timeAvailable = 1;
            int delayObserving = 30;
            double timeIteration = 1.0;
            double screenRefresh = 1.0;
            double timeForPaths = 1.0;
            double userVelocity = 3.0;
            double kmToPixel = 6597;
            int ttl = 180;
            int timeOnStairs = 60;
            int minTimeUpdate = 30;
            int commRange = 250;
            int maxKnowledge = 1;
            int commBandwidth = 54;
            int latency = 1;
            int timeChangeMood = 1800;
            
            int specialUsers = 1;
            int nonSpecialUsers = totalUsers - 1;
            
            String pathsFileName = "rand_non_special_user_paths_" + totalUsers + ".txt";
            File pathsFile = new File(mapDir, pathsFileName);
            
            // Verify paths file
            if (!pathsFile.exists()) {
                System.err.println(" Error: Paths file does not exist: " + pathsFile.getAbsolutePath());
                return null;
            }
            
            String recommendationAlgorithm = "Completely-random (FULLY-RAND)";
            String networkType = "Centralized (Centralized)";
            
            // SIMULATION CONSTRUCTOR
            Simulation simulation = new Simulation(
                timeAvailable, delayObserving, timeIteration, screenRefresh, timeForPaths,
                userVelocity, kmToPixel, ttl, timeOnStairs, minTimeUpdate, commRange,
                maxKnowledge, commBandwidth, latency, specialUsers, nonSpecialUsers,
                pathsFile.getAbsolutePath(), "Random Path", recommendationAlgorithm, 2.5f, 10,
                "Opportunistic", 0.4, 40, 0.5, networkType, timeChangeMood,
                false, System.currentTimeMillis(), false,
                true,              // ← mixCliqueAndIndependent = TRUE
                independentRatio   // ← independentRatio
            );
            
            Configuration.simulation = simulation;
            simulation.configureElementIdMapperForCurrentScenario();
            
            // Configure floor panel
            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            
            // Load scenario with drawFloor()
            System.out.println(" Loading " + scenarioName + "...");
            
            DrawFloorGraph floor = new DrawFloorGraph();
            com.mxgraph.swing.mxGraphComponent graphComponent = floor.drawFloor(
                roomFile, itemFile, true, false, 1);
            
            graphComponent.setToolTips(true);
            graphComponent.getViewport().setBackground(new java.awt.Color(255, 255, 255));
            graphComponent.getViewport().setOpaque(true);
            
            MainSimulator.floorPanelCombined.removeAll();
            MainSimulator.floorPanelCombined.add(graphComponent);
            MainSimulator.floorPanelCombined.revalidate();
            MainSimulator.floorPanelCombined.repaint();
            
            floor.loadDiccionaryItemLocation();
            
            MainSimulator.floor = floor;
            MainSimulator.graphComponent = graphComponent;
            
            System.out.println("  ✅ Simulation in mixed mode created correctly");
            
            return simulation;
            
        } catch (Exception e) {
            System.err.println(" Error creating simulation in mixed mode: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Prints contact-specific statistics after simulation completion.
     * Displays detailed contact statistics including attack rates by clique,
     * inter-clique coincidences, isolation metrics, and verification of
     * success criteria (95% isolation rate).
     * 
     * @param simulation the completed simulation instance
     */
    private static void printContactStatistics(Simulation simulation) {
        if (simulation.coincidenceTracker == null) {
            return;
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println(" DETAILED REAL CONTACT STATISTICS");
        System.out.println("=".repeat(100));
        
        // Attack rates by clique
        if (simulation.cliqueUserMapping != null && !simulation.cliqueUserMapping.isEmpty()) {
            System.out.println("\n ATTACK RATES BY CLIQUE:");
            
            simulation.coincidenceTracker.printAttackRatesByClique(
                simulation.getAllUsers(),
                simulation.initialSusceptiblesByClique,
                simulation.cliqueUserMapping
            );
        }
        
        // Inter-clique coincidences
        System.out.println("\n INTER-CLIQUE COINCIDENCES:");
        simulation.coincidenceTracker.printIsolationMetrics();
        simulation.coincidenceTracker.printDetailedUserCoincidences();
        
        // Success verification
        java.util.Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
        double isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
        
        System.out.println("\n SUCCESS CRITERIA VERIFICATION:");
        System.out.printf("   Isolation rate: %.2f%%\n", isolationRate * 100);
        
        if (isolationRate >= 0.95) {
            System.out.println("✅ CRITERIA MET (≥95% isolation)");
        } else {
            System.out.println("    CRITERIA NOT MET (<95% isolation)");
        }
        
        System.out.println("\n" + "=".repeat(100) + "\n");
    }
}