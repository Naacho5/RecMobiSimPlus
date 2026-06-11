package es.unizar.epidemic.tests.macro;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.epidemic.tests.Scenarios;
import es.unizar.gui.graph.GraphForSpecialUser;
import es.unizar.gui.simulation.Simulation;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comparison tests between Risk-Aware and Non-Risk-Aware recommendation algorithms.
 * Reproduces experiments from macro simulator for comparison with micro-level results.
 * Uses only synthetic trajectories with aerosol models (Peng and Lelieveld).
 * 
 * @author Nacho Palacio
 */
public class MacroComparisonTests {
    
    private static final String[] MODELS = {"AEROSOL_PENG"/*, "AEROSOL_LELIEVELD"*/};
    private static final String[] ALGORITHMS = {"Risk-Aware (Risk-Aware)", "Non-Risk-Aware (Non-Risk-Aware)"};
    private static final String RESULTS_DIR = "./results/test_macro/";

    private static String executionTimestamp;
    
    /**
     * Runs all macro comparison tests.
     * Executes systematic comparison of Risk-Aware and Non-Risk-Aware algorithms
     * across multiple configurations and user counts, generating CSV reports
     * for comparison with macro simulator results.
     */
    public static void runAll() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🧪 MACRO COMPARISON TESTS: Risk-Aware vs Non-Risk-Aware");
        System.out.println("=".repeat(100));

        executionTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(new java.util.Date());
        
        // Create results directory
        new File(RESULTS_DIR).mkdirs();
        
        // Test configurations
        List<MacroTestConfiguration> configurations = createMacroTestConfigurations();
        
        for (MacroTestConfiguration testConfig : configurations) {
            System.out.println("\n" + "━".repeat(100));
            System.out.println("⚙️  TEST CONFIGURATION: " + testConfig.name);
            System.out.println("   Users: " + testConfig.numUsers + 
                             " | Duration: " + (testConfig.durationSeconds/60) + " min");
            System.out.println("━".repeat(100));
            
            // Run both algorithms
            Map<String, Map<String, SimulationResult>> results = new HashMap<>();
            
            // 1. RISK-AWARE
            System.out.println("\n📊 TESTING RISK-AWARE ALGORITHM");
            Map<String, SimulationResult> riskAwareResults = runAlgorithmTests(
                testConfig, "Risk-Aware (Risk-Aware)");
            results.put("Risk-Aware", riskAwareResults);
            
            // 2. NON-RISK-AWARE
            System.out.println("\n📊 TESTING NON-RISK-AWARE ALGORITHM");
            Map<String, SimulationResult> nonRiskAwareResults = runAlgorithmTests(
                testConfig, "Non-Risk-Aware (Non-Risk-Aware)");
            results.put("Non-Risk-Aware", nonRiskAwareResults);
            
            // Compare and export
            compareAndExportMacroResults(testConfig, results);
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("✅ ALL MACRO COMPARISON TESTS COMPLETED");
        System.out.println("=".repeat(100) + "\n");

        generatePlotsAutomatically();
    }
    
    /**
     * Creates test configurations for macro comparison.
     * Generates baseline configurations with varying user counts and durations,
     * maintaining all other parameters constant for fair algorithm comparison.
     * 
     * @return list of macro test configurations
     */
    private static List<MacroTestConfiguration> createMacroTestConfigurations() {
        List<MacroTestConfiguration> configs = new ArrayList<>();
        
        int[] userCounts = {100, 200, 300, 400, 500};
        int[] durations = {3600, 7200, 10800};
        
        for (int users : userCounts) {
            for (int duration : durations) {
                String name = String.format("baseline_%dusers_%dmin", users, duration/60);
                configs.add(new MacroTestConfiguration(name, users, duration));
            }
        }
        
        return configs;
    }
    
    /**
     * Runs tests for a specific recommendation algorithm.
     * Executes simulations using synthetic trajectories with the specified
     * recommendation algorithm for both aerosol models, collecting comprehensive
     * metrics including ratings and distances.
     * 
     * @param testConfig the test configuration
     * @param algorithm the recommendation algorithm to test
     * @return map of model names to their simulation results
     */
    private static Map<String, SimulationResult> runAlgorithmTests(
            MacroTestConfiguration testConfig,
            String algorithm) {
        
        Map<String, SimulationResult> results = new HashMap<>();
        
        for (String model : MODELS) {
            try {
                EpidemicStatistics.resetInstance();
                
                // Configure epidemic with baseline parameters
                EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
                epidemicConfig.setConfigName("Baseline_Macro");
                epidemicConfig.setSelectedModel(model);
                epidemicConfig.setRecommendationAlgorithm(algorithm);
                epidemicConfig.setInitialInfectedUsers(1);
                epidemicConfig.setMaskComplianceRate(0.1);
                epidemicConfig.setDefaultVentilationRate(3.0);
                epidemicConfig.setVirusDecayRate(0.62);
                epidemicConfig.setSimulationDurationSeconds(testConfig.durationSeconds);
                epidemicConfig.setTotalUsers(testConfig.numUsers);
                
                SimulationUtils.configureEpidemicParameters(epidemicConfig, null, model);
                
                // Create simulation with synthetic trajectories
                Simulation simulation = SimulationUtils.createSimulationForScenario(
                    null,  // No specific scenario, use baseline
                    testConfig.numUsers,
                    1,     // Initial infected
                    "MoMA_Museum",
                    false  // Use synthetic trajectories
                );
                
                if (simulation == null) {
                    throw new Exception("Could not create simulation");
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

                System.out.printf(
                    "%n[ROOM ANALYSIS] algorithm=%s model=%s%n",
                    algorithm, model
                );

                es.unizar.epidemic.statistics.EpidemicStatistics stats = 
					es.unizar.epidemic.statistics.EpidemicStatistics.getInstance();
                stats.printRoomVolumeAnalysis();

                
                // Collect additional macro metrics
                collectMacroMetrics(simulation, result);
                
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
     * Collects macro-specific metrics.
     * Extracts average user ratings and average distances traveled between items,
     * metrics needed for comparison with macro simulator results.
     * 
     * @param simulation the completed simulation
     * @param result the result object to populate
     */
    private static void collectMacroMetrics(Simulation simulation, SimulationResult result) {
        // Calculate average rating
        if (Simulation.userRatings != null && !Simulation.userRatings.isEmpty()) {
            double totalSum = 0.0;
            int totalCount = 0;
            
            for (Map.Entry<Integer, List<Float>> entry : Simulation.userRatings.entrySet()) {
                List<Float> ratings = entry.getValue();
                for (float r : ratings) {
                    totalSum += r;
                }
                totalCount += ratings.size();
            }

            result.averageRating = totalCount > 0 ? totalSum / totalCount : 0.0;
            System.out.printf("\n   ✅ Overall average rating: %.2f (from %d ratings)\n", result.averageRating, totalCount);
        } else {
            result.averageRating = 0.0;
        }

        System.out.println("\n[RECOMMENDED ITEMS DISTANCE ANALYSIS]");
        Pair<Double, Double> recommendedDistances = simulation.getRecommendedItemsAverageDistance();
        result.averageDistance = recommendedDistances.getF();        // En metros
        result.averageDistanceRooms = recommendedDistances.getS();   // Número de salas

        System.out.printf("   ✅ NEW METRIC (Real-time): Avg distance to recommended items: %.2f meters (%.2f rooms)%n",
            result.averageDistance, result.averageDistanceRooms);

        // ========== CALCULATE RISK FOR RECOMMENDED ARTWORKS ==========
        System.out.println("\n[RISK CALCULATION ANALYSIS]");

        // Get accumulated risk from simulation
        long riskSumMicroUnits = simulation.recommendationRiskSum.get();
        int riskCount = simulation.recommendationRiskCount.get();
        
        if (riskCount > 0) {
            // Convert from micro-units back to percentage
            double riskSum = riskSumMicroUnits / 1000000.0;
            result.averageRecommendationRisk = riskSum / riskCount;
            
            System.out.printf("   ✅ Total recommendation events: %d%n", riskCount);
            System.out.printf("   ✅ Total accumulated risk: %.6f%%%n", riskSum);
            System.out.printf("   ✅ Average recommendation risk: %.6f%%%n", result.averageRecommendationRisk);
        } else {
            System.out.println("   ⚠️  No recommendation events recorded - skipping risk calculation");
            result.averageRecommendationRisk = 0.0;
        } 

        // ========== CALCULATE BRIDGE RISK (FROM RECOMMENDER) ==========
        System.out.println("\n[BRIDGE RISK CALCULATION]");

        List<Double> bridgeRisks = simulation.allBridgeRisks;
        if (bridgeRisks != null && !bridgeRisks.isEmpty()) {
            double bridgeRiskAverage = bridgeRisks.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            
            result.averageBridgeRisk = bridgeRiskAverage;
            // print total risk
            System.out.printf("   ✅ Total bridge risk events: %d%n", bridgeRisks.size());
            System.out.printf("   ✅ Total accumulated bridge risk: %.6f%%%n", bridgeRisks.stream().mapToDouble(Double::doubleValue).sum());
            System.out.printf("   ✅ Bridge risks from recommender: %d measurements%n", bridgeRisks.size());
            System.out.printf("   ✅ Average bridge risk: %.6f%%%n", result.averageBridgeRisk);
        } else {
            System.out.println("   ⚠️  No bridge risks recorded - skipping calculation");
            result.averageBridgeRisk = 0.0;
        }
    }
    
    /**
     * Compares and exports macro results to CSV.
     * Generates timestamped CSV files with comprehensive comparison data
     * for both recommendation algorithms, including all metrics needed for
     * macro simulator comparison.
     * 
     * @param testConfig the test configuration
     * @param results nested map: algorithm -> model -> result
     */
    private static void compareAndExportMacroResults(
            MacroTestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        String filename = String.format("%smacro_comparison_%s_%s.csv",
            RESULTS_DIR, testConfig.name, executionTimestamp);
        
        try (FileWriter writer = new FileWriter(filename)) {
            // Header
            writer.append("timestamp;algorithm;num_users;duration_sec;epidemic_model;" +
                         "attack_rate;concentration;individual_risk;average_recommendation_risk;average_bridge_risk;average_rating;" +
                         "average_distance;average_distance_rooms;execution_time_sec\n");
            
            // Data rows
            for (Map.Entry<String, Map<String, SimulationResult>> algEntry : results.entrySet()) {
                String algorithm = algEntry.getKey();
                
                for (Map.Entry<String, SimulationResult> modelEntry : algEntry.getValue().entrySet()) {
                    String model = modelEntry.getKey();
                    SimulationResult result = modelEntry.getValue();
                    
                    writer.append(String.format("%s;%s;%d;%d;%s;%.4f;%.6f;%.4f;%.6f;%.6f;%.4f;%.2f;%.2f;%.2f\n",
                        executionTimestamp,
                        algorithm,
                        testConfig.numUsers,
                        testConfig.durationSeconds,
                        model,
                        result.attackRate * 100,
                        result.averageConcentration,
                        result.individualRisk,
                        result.averageRecommendationRisk,
                        result.averageBridgeRisk,
                        result.averageRating,
                        result.averageDistance,
                        result.averageDistanceRooms,
                        result.executionTimeMs / 1000.0
                    ));

                    // Imprimir todo por pantalla tambien
                    System.out.printf(
                        "%n[EXPORT ROW] algorithm=%s model=%s%n",
                        algorithm, model
                    );
                    System.out.printf("   Attack Rate: %.2f%%\n", result.attackRate * 100);
                    System.out.printf("   Average Concentration: %.6f\n", result.averageConcentration);
                    System.out.printf("   Individual Risk: %.2f%%\n", result.individualRisk * 100);
                    System.out.printf("   Average Recommendation Risk: %.6f%%\n", result.averageRecommendationRisk);
                    System.out.printf("   Average Bridge Risk: %.6f%%\n", result.averageBridgeRisk);
                    System.out.printf("   Average Rating: %.2f\n", result.averageRating);
                    System.out.printf("   Average Distance: %.2f pixels\n", result.averageDistance);
                    System.out.printf("   Average Distance Rooms: %.2f pixels\n", result.averageDistanceRooms);
                    System.out.printf("   Execution Time: %.2f seconds\n", result.executionTimeMs / 1000.0);
                }
            }
            
            writer.flush();
            System.out.println("\n✅ Results exported to: " + filename);
            
            // Print comparison table
            printMacroComparisonTable(testConfig, results);
            
        } catch (IOException e) {
            System.err.println("❌ Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates plots automatically using external Python script.
     */
    private static void generatePlotsAutomatically() {
        System.out.println("\n📊 Generating plots automatically for current test...");
        System.out.println("   Timestamp: " + executionTimestamp);
        
        String scriptPath = "./results/test_macro/compare_macro_mode.py";
        
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
     * Prints macro comparison table to console.
     * Displays formatted comparison of Risk-Aware vs Non-Risk-Aware algorithms
     * showing attack rates, ratings, distances, and other key metrics.
     * 
     * @param testConfig the test configuration
     * @param results nested map: algorithm -> model -> result
     */
    private static void printMacroComparisonTable(
            MacroTestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 MACRO COMPARISON: " + testConfig.name);
        System.out.println("=".repeat(100));
        
        for (String model : MODELS) {
            System.out.println("\n🔬 MODEL: " + model);
            System.out.println("-".repeat(100));
            System.out.printf(
                "%-20s %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s %-15s%n",
                "ALGORITHM", "ATTACK RATE", "CONC.", "RISK", "RECOMMENDATION_RISK", "BRIDGE_RISK", "RATING", "DISTANCE", "DISTANCE_ROOMS", "TIME (s)"
            );
            System.out.println("-".repeat(150));

            for (String algorithm : new String[] { "Risk-Aware", "Non-Risk-Aware" }) {
                SimulationResult result = results.get(algorithm).get(model);
                System.out.printf(
                    "%-20s %-15.2f%% %-15.6f %-15.2f%% %-15.6f%% %-15.6f%% %-15.2f %-15.2f %-15.2f %-15.2f%n",
                    algorithm,
                    result.attackRate * 100,
                    result.averageConcentration,
                    result.individualRisk,
                    result.averageRecommendationRisk,
                    result.averageBridgeRisk,
                    result.averageRating,
                    result.averageDistance,
                    result.averageDistanceRooms,
                    result.executionTimeMs / 1000.0
                );
            }

            
            // Print differences
            SimulationResult riskAware = results.get("Risk-Aware").get(model);
            SimulationResult nonRiskAware = results.get("Non-Risk-Aware").get(model);
            
            System.out.println("-".repeat(100));
            System.out.println("📈 DIFFERENCES (Risk-Aware - Non-Risk-Aware):");
            System.out.printf("   Attack Rate: %+.2f%%\n", 
                (riskAware.attackRate - nonRiskAware.attackRate) * 100);
            System.out.printf("   Avg Rating: %+.2f\n",
                riskAware.averageRating - nonRiskAware.averageRating);
            System.out.printf("   Avg Distance: %+.2f pixels\n",
                riskAware.averageDistance - nonRiskAware.averageDistance);
            System.out.printf("   Avg Distance Rooms: %+.2f pixels\n",
                riskAware.averageDistanceRooms - nonRiskAware.averageDistanceRooms);
        }
        
        System.out.println("=".repeat(100));
    }
    
    /**
     * Macro test configuration data class.
     * Holds parameters for macro comparison testing including user count
     * and simulation duration.
     */
    private static class MacroTestConfiguration {
        String name;
        int numUsers;
        int durationSeconds;
        
        MacroTestConfiguration(String name, int numUsers, int durationSeconds) {
            this.name = name;
            this.numUsers = numUsers;
            this.durationSeconds = durationSeconds;
        }
    }
}