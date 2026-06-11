package es.unizar.epidemic.tests.contacts;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.Simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests comparing Rotational vs Complex contact trajectory modes.
 * Executes simulations using both contact generation strategies and compares
 * epidemic outcomes including attack rates by clique and isolation metrics.
 * 
 * @author Nacho Palacio
 */
public class ContactBasedTests {
    
    private static final String[] MODELS = {"AEROSOL_PENG", "AEROSOL_LELIEVELD"};
    private static final String RESULTS_DIR = "./results/test_contacts/contact_modes/";

    private static String executionTimestamp;
    
    /**
     * Runs all contact-based comparison tests.
     * Executes multiple configurations comparing Rotational and Complex contact modes,
     * testing different user counts and simulation durations. Generates comprehensive
     * CSV reports with attack rates, clique statistics, and isolation metrics.
     */
    public static void runAll() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🧪 EXECUTING CONTACT MODE COMPARISON TESTS (Rotational vs Complex)");
        System.out.println("=".repeat(100));

        executionTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(new java.util.Date());
        
        // Create results directory
        new File(RESULTS_DIR).mkdirs();
        
        // Test configurations
        List<TestConfiguration> configurations = createTestConfigurations();
        
        for (TestConfiguration testConfig : configurations) {
            System.out.println("\n" + "━".repeat(100));
            System.out.println("⚙️  TEST CONFIGURATION: " + testConfig.name);
            System.out.println("   Users: " + testConfig.numUsers + " | Duration: " + 
                             testConfig.durationSeconds + "s (" + (testConfig.durationSeconds/60) + " min)");
            System.out.println("━".repeat(100));
            
            // Run both contact modes
            Map<String, Map<String, SimulationResult>> results = new HashMap<>();
            
            // 1. ROTATIONAL MODE
            System.out.println("\n📊 TESTING ROTATIONAL MODE");
            Map<String, SimulationResult> rotationalResults = runContactModeTests(
                testConfig, Configuration.ContactTrajectoryMode.SIMPLIFIED_ROTATION);
            results.put("ROTATIONAL", rotationalResults);
            
            // 2. COMPLEX MODE
            System.out.println("\n📊 TESTING COMPLEX MODE");
            Map<String, SimulationResult> complexResults = runContactModeTests(
                testConfig, Configuration.ContactTrajectoryMode.COMPLEX_REAL_EVENTS);
            results.put("COMPLEX", complexResults);
            
            // Compare and export results
            compareAndExportResults(testConfig, results);
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("✅ ALL CONTACT MODE COMPARISON TESTS COMPLETED");
        System.out.println("=".repeat(100) + "\n");

        generatePlotsAutomatically();
    }
    
    /**
     * Creates test configurations with varying users and durations.
     * Generates configurations for systematic testing of contact modes across
     * different simulation scales and time periods.
     * 
     * @return list of test configurations to execute
     */
    private static List<TestConfiguration> createTestConfigurations() {
        List<TestConfiguration> configs = new ArrayList<>();
        
        int[] userCounts = {50, 100, 200, 500};
        int[] durations = {300, 900, 1800, 3600};

        for (int users : userCounts) {
            for (int duration : durations) {
                String name = String.format("%dusers_%dmin", users, duration/60);
                configs.add(new TestConfiguration(name, users, duration));
            }
        }
        
        return configs;
    }
    
    /**
     * Runs tests for a specific contact mode.
     * Executes simulations using the specified contact trajectory mode for all
     * epidemic models, collecting comprehensive results including attack rates
     * and clique-specific metrics.
     * 
     * @param testConfig the test configuration parameters
     * @param contactMode the contact trajectory mode to test
     * @return map of model names to their simulation results
     */
    private static Map<String, SimulationResult> runContactModeTests(
            TestConfiguration testConfig,
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
                
                // Create simulation with specific contact mode
                Simulation simulation = SimulationUtils.createSimulationWithContactMode(
                    testConfig.numUsers,
                    "MoMA_Museum",
                    contactMode
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
                
                // Collect clique-specific metrics
                collectCliqueMetrics(simulation, result);
                results.put(model, result);                
            } catch (Exception e) {
                System.err.println("      ❌ Error in model " + model + ": " + e.getMessage());
                e.printStackTrace();
                results.put(model, new SimulationResult());
            }
        }
        
        return results;
    }
    
    /**
     * Collects clique-specific metrics from simulation.
     * Extracts attack rates by clique, inter-clique isolation rates, and
     * coincidence statistics for detailed contact pattern analysis.
     * 
     * @param simulation the completed simulation instance
     * @param result the result object to populate with clique metrics
     */
    private static void collectCliqueMetrics(Simulation simulation, SimulationResult result) {
        if (simulation.coincidenceTracker == null) {
            if (result.intraCliqueAttackRate == 0.0) {
                result.intraCliqueAttackRate = 0.0;
            }
            result.isolationRate = 0.0;
            return;
        }

        Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
        result.isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
    }
    
    /**
     * Compares and exports results to CSV.
     * Generates timestamped CSV files containing comprehensive comparison data
     * for both contact modes, including attack rates, clique metrics, and
     * isolation statistics.
     * 
     * @param testConfig the test configuration used
     * @param results nested map: contact mode -> model -> simulation result
     */
    private static void compareAndExportResults(
            TestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        String filename = String.format("%scomparison_%s_%s.csv",
            RESULTS_DIR, testConfig.name, executionTimestamp);
        
        try (FileWriter writer = new FileWriter(filename)) {
            // Header
            writer.append("timestamp;contact_mode;num_users;duration_sec;epidemic_model;" +
                         "attack_rate_global;attack_rate_intra_clique;isolation_rate;" +
                         "concentration;individual_risk;execution_time_sec\n");
            
            // Data rows
            for (Map.Entry<String, Map<String, SimulationResult>> modeEntry : results.entrySet()) {
                String mode = modeEntry.getKey();
                
                for (Map.Entry<String, SimulationResult> modelEntry : modeEntry.getValue().entrySet()) {
                    String model = modelEntry.getKey();
                    SimulationResult result = modelEntry.getValue();

                    writer.append(String.format("%s;%s;%d;%d;%s;%.4f;%.4f;%.4f;%.6f;%.4f;%.2f\n",
                        executionTimestamp,
                        mode,
                        testConfig.numUsers,
                        testConfig.durationSeconds,
                        model,
                        result.attackRate * 100,
                        result.intraCliqueAttackRate * 100,
                        result.isolationRate * 100,
                        result.averageConcentration,
                        result.individualRisk,
                        result.executionTimeMs / 1000.0
                    ));
                }
            }
            
            writer.flush();
            System.out.println("\n✅ Results exported to: " + filename);
            
            // Print comparison table
            printComparisonTable(testConfig, results);
            
        } catch (IOException e) {
            System.err.println("❌ Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generatePlotsAutomatically() {
        System.out.println("\n📊 Generating plots automatically for current test...");
        System.out.println("   Timestamp: " + executionTimestamp);
        
        String scriptPath = "./results/test_contacts/contact_modes/compare_contact_mode.py";
        
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
     * Prints comparison table to console.
     * Displays formatted table comparing Rotational and Complex modes across
     * all tested epidemic models with key metrics.
     * 
     * @param testConfig the test configuration used
     * @param results nested map: contact mode -> model -> simulation result
     */
    private static void printComparisonTable(
            TestConfiguration testConfig,
            Map<String, Map<String, SimulationResult>> results) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 COMPARISON: " + testConfig.name);
        System.out.println("=".repeat(100));
        
        for (String model : MODELS) {
            System.out.println("\n🔬 MODEL: " + model);
            System.out.println("-".repeat(100));
            System.out.printf("%-15s %-15s %-20s %-15s %-15s\n",
                "MODE", "GLOBAL ATTACK", "INTRA-CLIQUE ATTACK", "ISOLATION", "TIME (s)");
            System.out.println("-".repeat(100));
            
            for (String mode : new String[]{"ROTATIONAL", "COMPLEX"}) {
                SimulationResult result = results.get(mode).get(model);
                System.out.printf("%-15s %-15.2f%% %-20.2f%% %-15.2f%% %-15.2f\n",
                    mode,
                    result.attackRate * 100,
                    result.intraCliqueAttackRate * 100,
                    result.isolationRate * 100,
                    result.executionTimeMs / 1000.0
                );
            }
        }
        
        System.out.println("=".repeat(100));
    }
    
    /**
     * Test configuration data class.
     * Holds parameters for a single test configuration including name,
     * user count, and simulation duration.
     */
    private static class TestConfiguration {
        String name;
        int numUsers;
        int durationSeconds;
        
        TestConfiguration(String name, int numUsers, int durationSeconds) {
            this.name = name;
            this.numUsers = numUsers;
            this.durationSeconds = durationSeconds;
        }
    }
}