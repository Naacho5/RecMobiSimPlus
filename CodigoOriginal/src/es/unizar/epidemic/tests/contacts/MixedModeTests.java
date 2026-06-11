package es.unizar.epidemic.tests.contacts;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.simulation.Simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator for mixed mode (cliques + independent users).
 * Tests hypothesis that intra-clique attack rates are higher than global rates
 * due to increased contact time within social groups. Compares Rotational and
 * Complex contact modes with varying proportions of independent users.
 * 
 * @author Nacho Palacio
 */
public class MixedModeTests {
    
    private static final String[] MODELS = {"AEROSOL_PENG", "AEROSOL_LELIEVELD"};
    private static final String RESULTS_DIR = "./results/test_contacts/mixed_mode/";

    private static String executionTimestamp;
    
    /**
     * Main entry point for mixed mode validation.
     * Executes comprehensive tests of mixed mode simulations comparing clique-based
     * and independent user behaviors under both Rotational and Complex contact modes.
     * Validates hypotheses about differential attack rates and generates detailed CSV reports.
     */
    public static void run() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🧪 VALIDATION TEST: MIXED MODE (CLIQUES + INDEPENDENTS)");
        System.out.println("=".repeat(100));
        
        executionTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(new java.util.Date());

        // Create results directory
        new File(RESULTS_DIR).mkdirs();
        
        // Test configurations
        List<MixedTestConfiguration> configurations = createMixedTestConfigurations();
        
        for (MixedTestConfiguration testConfig : configurations) {
            System.out.println("\n" + "━".repeat(100));
            System.out.println("⚙️  TEST CONFIGURATION: " + testConfig.name);
            System.out.println("   Users: " + testConfig.numUsers + 
                             " | Independent ratio: " + (testConfig.independentRatio * 100) + "%" +
                             " | Duration: " + (testConfig.durationSeconds/60) + " min");
            System.out.println("━".repeat(100));
            
            // Run both contact modes
            Map<String, Map<String, SimulationResult>> results = new HashMap<>();
            
            // 1. ROTATIONAL MODE
            System.out.println("\n📊 TESTING ROTATIONAL MODE");
            Map<String, SimulationResult> rotationalResults = runMixedModeTests(
                testConfig, Configuration.ContactTrajectoryMode.SIMPLIFIED_ROTATION);
            results.put("ROTATIONAL", rotationalResults);
            
            // 2. COMPLEX MODE
            System.out.println("\n📊 TESTING COMPLEX MODE");
            Map<String, SimulationResult> complexResults = runMixedModeTests(
                testConfig, Configuration.ContactTrajectoryMode.COMPLEX_REAL_EVENTS);
            results.put("COMPLEX", complexResults);
            
            // Validate hypothesis and export
            validateHypothesisAndExport(testConfig, results);
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("✅ ALL MIXED MODE VALIDATION TESTS COMPLETED");
        System.out.println("=".repeat(100) + "\n");

        generatePlotsAutomatically();
    }
    
    /**
     * Creates test configurations for mixed mode validation.
     * Generates configurations varying user counts, simulation durations,
     * and ratios of independent vs clique-based users.
     * 
     * @return list of mixed mode test configurations
     */
    private static List<MixedTestConfiguration> createMixedTestConfigurations() {
        List<MixedTestConfiguration> configs = new ArrayList<>();
        
        int[] userCounts = {50, 100, 200, 500};
        int[] durations = {300, 900, 1800, 3600};
        double[] independentRatios = {0.3};
        
        for (int users : userCounts) {
            for (int duration : durations) {
                for (double ratio : independentRatios) {
                    String name = String.format("%dusers_%dmin_%.0fpct_indep",
                        users, duration/60, ratio*100);
                    configs.add(new MixedTestConfiguration(name, users, duration, ratio));
                }
            }
        }
        
        return configs;
    }
    
    /**
     * Runs mixed mode tests for a specific contact mode.
     * Executes simulations with mixed clique and independent users using the
     * specified contact trajectory mode, collecting detailed metrics on both
     * population groups.
     * 
     * @param testConfig the test configuration parameters
     * @param contactMode the contact trajectory mode to use
     * @return map of model names to their simulation results
     */
    private static Map<String, SimulationResult> runMixedModeTests(
            MixedTestConfiguration testConfig,
            Configuration.ContactTrajectoryMode contactMode) {
        
        Map<String, SimulationResult> results = new HashMap<>();
        
        for (String model : MODELS) {
            try {
                EpidemicStatistics.resetInstance();
                
                // Configure epidemic
                EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
                epidemicConfig.setSelectedModel(model);
                // epidemicConfig.setInitialInfectedUsers(5);
                epidemicConfig.setMaskComplianceRate(0.1);
                epidemicConfig.setDefaultVentilationRate(3.0);
                epidemicConfig.setSimulationDurationSeconds(testConfig.durationSeconds);
                epidemicConfig.setTotalUsers(testConfig.numUsers);
                
                SimulationUtils.configureEpidemicParameters(epidemicConfig, null, model);
                
                System.out.println("   🔬 Executing simulation: " + model);
                
                // Create mixed mode simulation
                Simulation simulation = createMixedModeSimulation(
                    testConfig, contactMode);
                
                if (simulation == null) {
                    throw new Exception("Could not create mixed mode simulation");
                }
                
                long startTime = System.currentTimeMillis();

                SimulationUtils.runSimulationToCompletion(simulation);
                
                if (simulation.getEpidemicManager() != null) {
                    simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(
                        simulation.getAllUsers());
                }
                
                // Collect results
                SimulationResult result = SimulationUtils.collectSimulationResults(
                    simulation, null, model);
                result.executionTimeMs = System.currentTimeMillis() - startTime;
                
                // Collect mixed mode metrics
                collectMixedModeMetrics(simulation, result, testConfig.independentRatio);
                
                results.put(model, result);
                
                System.out.printf("      ✅ Completed in %.2fs\n", result.executionTimeMs / 1000.0);
                
            } catch (Exception e) {
                System.err.println("      ❌ Error in model " + model + ": " + e.getMessage());
                e.printStackTrace();
                results.put(model, new SimulationResult());
            }
        }
        
        return results;
    }
    
    /**
     * Creates a simulation with mixed mode enabled.
     * Configures simulation with specified proportions of clique-based and
     * independent users, using the designated contact trajectory mode.
     * 
     * @param testConfig the test configuration
     * @param contactMode the contact mode to use
     * @return configured Simulation instance with mixed mode
     */
    private static Simulation createMixedModeSimulation(
            MixedTestConfiguration testConfig,
            Configuration.ContactTrajectoryMode contactMode) {
        
        System.out.println("      Creating mixed mode simulation...");
        
        try {
            String scenarioName = "MoMA_Museum";
            File mapDir = new File("./dist/resources/maps/" + scenarioName + "/");
            if (!mapDir.exists()) {
                mapDir = new File("./resources/maps/" + scenarioName + "/");
            }
            
            if (!mapDir.exists()) {
                System.err.println("      ❌ Scenario directory not found");
                return null;
            }
            
            File roomFile = new File(mapDir, "room_floor_combined.txt");
            File itemFile = new File(mapDir, "item_floor_combined.txt");
            File graphFile = new File(mapDir, "graph_floor_combined.txt");
            String pathsFileName = "rand_non_special_user_paths_" + testConfig.numUsers + ".txt";
            File pathsFile = new File(mapDir, pathsFileName);
            
            if (!roomFile.exists() || !itemFile.exists() || 
                !graphFile.exists() || !pathsFile.exists()) {
                System.err.println("      ❌ Required files not found");
                return null;
            }
            
            // Configure literals
            es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
            es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
            es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();
            
            // Set contact mode
            if (Configuration.instance != null) {
                Configuration.instance.contactTrajectoryMode = contactMode;
            }
            
            // Create simulation with mixed mode enabled
            Simulation simulation = new Simulation(
                1, 30, 1.0, 1.0, 1.0,
                3.0, 6597, 180, 60, 30, 250,
                1, 54, 1, 1, testConfig.numUsers - 1,
                pathsFile.getAbsolutePath(), "Random Path",
                "Completely-random (FULLY-RAND)", 2.5f, 10,
                "Opportunistic", 0.4, 40, 0.5,
                "Centralized (Centralized)", 1800,
                false, System.currentTimeMillis(), false,
                true,  // mixCliqueAndIndependentUsers = TRUE
                testConfig.independentRatio
            );
            
            Configuration.simulation = simulation;
            simulation.configureElementIdMapperForCurrentScenario();
            
            // Configure floor panel
            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            
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
            
            System.out.println("      ✅ Mixed mode simulation created");
            
            return simulation;
            
        } catch (Exception e) {
            System.err.println("      ❌ Error creating simulation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Collects mixed mode specific metrics.
     * Calculates separate attack rates for clique-based users and independent
     * users, enabling comparison of infection patterns between the two groups.
     * 
     * @param simulation the completed simulation
     * @param result the result object to populate
     * @param independentRatio the ratio of independent users
     */
    private static void collectMixedModeMetrics(
            Simulation simulation,
            SimulationResult result,
            double independentRatio) {
        
        List<es.unizar.gui.simulation.User> allUsers = simulation.getAllUsers();
        int totalUsers = allUsers.size();
        int independentCount = (int)(totalUsers * independentRatio);
        int cliqueCount = totalUsers - independentCount;
        
        // Calculate clique attack rate
        int cliqueInfected = 0;
        for (int i = 0; i < cliqueCount; i++) {
            es.unizar.gui.simulation.User user = allUsers.get(i);
            if (user.getEpidemicExtension() != null &&
                simulation.isUserInfected(user.getEpidemicExtension())) {
                cliqueInfected++;
            }
        }
        result.cliqueAttackRate = cliqueCount > 0 ? 
            (double)cliqueInfected / cliqueCount : 0.0;
        
        // Calculate independent attack rate
        int independentInfected = 0;
        for (int i = cliqueCount; i < totalUsers; i++) {
            es.unizar.gui.simulation.User user = allUsers.get(i);
            if (user.getEpidemicExtension() != null &&
                simulation.isUserInfected(user.getEpidemicExtension())) {
                independentInfected++;
            }
        }
        result.independentAttackRate = independentCount > 0 ?
            (double)independentInfected / independentCount : 0.0;
    }
    
    /**
     * Validates hypothesis and exports results.
     * Tests the hypothesis that intra-clique attack rates exceed global rates,
     * generates CSV reports, and prints validation summary to console.
     * 
     * @param testConfig the test configuration
     * @param results nested map: contact mode -> model -> result
     */
    private static void validateHypothesisAndExport(
            MixedTestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        String filename = String.format("%smixed_mode_%s_%s.csv",
            RESULTS_DIR, testConfig.name, executionTimestamp);
        
        try (FileWriter writer = new FileWriter(filename)) {
            // Header
            writer.append("timestamp;contact_mode;num_users;duration_sec;independent_ratio;" +
                         "epidemic_model;attack_rate_global;attack_rate_clique;" +
                         "attack_rate_independent;concentration;individual_risk;" +
                         "execution_time_sec;hypothesis_validated\n");
            
            // Data rows
            for (Map.Entry<String, Map<String, SimulationResult>> modeEntry : results.entrySet()) {
                String mode = modeEntry.getKey();
                
                for (Map.Entry<String, SimulationResult> modelEntry : modeEntry.getValue().entrySet()) {
                    String model = modelEntry.getKey();
                    SimulationResult result = modelEntry.getValue();
                    
                    boolean hypothesisValidated = result.cliqueAttackRate > result.attackRate;
                    
                    writer.append(String.format("%s;%s;%d;%d;%.2f;%s;%.4f;%.4f;%.4f;%.6f;%.4f;%.2f;%s\n",
                        executionTimestamp,
                        mode,
                        testConfig.numUsers,
                        testConfig.durationSeconds,
                        testConfig.independentRatio,
                        model,
                        result.attackRate * 100,
                        result.cliqueAttackRate * 100,
                        result.independentAttackRate * 100,
                        result.averageConcentration,
                        result.individualRisk,
                        result.executionTimeMs / 1000.0,
                        hypothesisValidated ? "YES" : "NO"
                    ));
                }
            }
            
            writer.flush();
            System.out.println("\n✅ Results exported to: " + filename);
            
            // Print validation table
            printValidationTable(testConfig, results);
            
        } catch (IOException e) {
            System.err.println("❌ Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates plots automatically using external Python script.
     * 
     */
    private static void generatePlotsAutomatically() {
        System.out.println("\n📊 Generating plots automatically for current test...");
        System.out.println("   Timestamp: " + executionTimestamp);
        
        String scriptPath = "./results/test_contacts/mixed_mode/compare_mixed_mode.py";
        
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath, executionTimestamp);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("   " + line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Plots generated successfully");
            } else {
                System.err.println("⚠️  Plot generation failed (exit code: " + exitCode + ")");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Could not auto-generate plots: " + e.getMessage());
            System.out.println("   📌 You can generate them manually:");
            System.out.println("      python3 " + scriptPath + " " + executionTimestamp);
        }
    }
    
    /**
     * Prints validation table to console.
     * Displays formatted comparison of attack rates for clique-based vs
     * independent users, with hypothesis validation status.
     * 
     * @param testConfig the test configuration
     * @param results nested map: contact mode -> model -> result
     */
    private static void printValidationTable(
            MixedTestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 HYPOTHESIS VALIDATION: " + testConfig.name);
        System.out.println("   Hypothesis: Clique attack rate > Global attack rate");
        System.out.println("=".repeat(100));
        
        for (String model : MODELS) {
            System.out.println("\n🔬 MODEL: " + model);
            System.out.println("-".repeat(100));
            System.out.printf("%-15s %-15s %-15s %-20s %-15s\n",
                "MODE", "GLOBAL", "CLIQUE", "INDEPENDENT", "VALIDATED?");
            System.out.println("-".repeat(100));
            
            for (String mode : new String[]{"ROTATIONAL", "COMPLEX"}) {
                SimulationResult result = results.get(mode).get(model);
                boolean validated = result.cliqueAttackRate > result.attackRate;
                
                System.out.printf("%-15s %-15.2f%% %-15.2f%% %-20.2f%% %-15s\n",
                    mode,
                    result.attackRate * 100,
                    result.cliqueAttackRate * 100,
                    result.independentAttackRate * 100,
                    validated ? "✅ YES" : "❌ NO"
                );
            }
        }
        
        System.out.println("=".repeat(100));
    }
    
    /**
     * Mixed mode test configuration data class.
     * Holds parameters for mixed mode testing including user proportions
     * and simulation duration.
     */
    private static class MixedTestConfiguration {
        String name;
        int numUsers;
        int durationSeconds;
        double independentRatio;
        
        MixedTestConfiguration(String name, int numUsers, int durationSeconds, double independentRatio) {
            this.name = name;
            this.numUsers = numUsers;
            this.durationSeconds = durationSeconds;
            this.independentRatio = independentRatio;
        }
    }
}