package es.unizar.epidemic.tests.scenarios;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.gui.simulation.Simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests using real contact trajectories from CSV
 * 
 * @author Nacho Palacio
 */
public class ContactBasedTests {
    
    private static final String[] MODELS = {"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"};
    
    /**
     * Runs all contact-based tests.
     * Executes multiple test configurations using real contact trajectories,
     * running all epidemic models for each configuration and comparing results.
     * Generates comprehensive output including statistics and CSV files.
     */
    public static void runAll() {
        System.out.println("\n EXECUTING TESTS WITH REAL CONTACTS\n");
        
        // Configurations to test
        List<EpidemicConfiguration> configurations = createContactTestConfigurations();
        
        int totalUsers = 100;
        String scenarioName = "MoMA_Museum";
        
        Map<String, Map<String, SimulationResult>> allResults = new HashMap<>();
        
        for (int i = 0; i < configurations.size(); i++) {
            EpidemicConfiguration config = configurations.get(i);
            String configName = config.getConfigName();
            
            System.out.println("━".repeat(80));
            System.out.println("⚙️  CONFIGURACIÓN " + (i+1) + "/" + configurations.size() + ": " + configName);
            System.out.println("━".repeat(80));
            
            applyConfiguration(config);
            
            Map<String, SimulationResult> configResults = runAllModelsWithContacts(
                totalUsers, configName, scenarioName, i+1);
            allResults.put(configName, configResults);
            
            System.out.println("\n");
        }
        
        // Comparar resultados
        compareContactResults(allResults);
    }
    
    /**
     * Creates test configurations specifically for contact-based tests.
     * Generates a list of epidemic configurations including baseline,
     * high ventilation, and mandatory mask scenarios for testing with
     * real contact trajectories.
     * 
     * @return list of configured EpidemicConfiguration instances
     */
    private static List<EpidemicConfiguration> createContactTestConfigurations() {
        List<EpidemicConfiguration> configurations = new ArrayList<>();
        int simulationDurationSeconds = 2500;
        
        // Baseline
        EpidemicConfiguration baseline = EpidemicConfiguration.getInstance().clone();
        baseline.setConfigName("Contact_Baseline");
        baseline.setMaskComplianceRate(0.1);
        baseline.setDefaultVentilationRate(3.0);
        baseline.setVirusDecayRate(0.62);
        baseline.setInitialInfectedUsers(5);
        baseline.setSimulationDurationSeconds(simulationDurationSeconds);
        baseline.setTotalUsers(100);
        configurations.add(baseline);
        
        // High ventilation
        EpidemicConfiguration highVent = baseline.clone();
        highVent.setConfigName("Contact_HighVent");
        highVent.setDefaultVentilationRate(12.0);
        configurations.add(highVent);
        
        // Mandatory masks
        EpidemicConfiguration masks = baseline.clone();
        masks.setConfigName("Contact_Masks");
        masks.setMaskComplianceRate(0.95);
        configurations.add(masks);
        
        return configurations;
    }
    
    /**
     * Applies a given epidemic configuration.
     * Resets the current configuration to defaults and then applies
     * all settings from the provided configuration including model selection,
     * infection parameters, mask compliance, and ventilation rates.
     * 
     * @param config the epidemic configuration to apply
     */
    private static void applyConfiguration(EpidemicConfiguration config) {
        EpidemicConfiguration.getInstance().resetToDefaults();
        EpidemicConfiguration currentConfig = EpidemicConfiguration.getInstance();
        
        currentConfig.setConfigName(config.getConfigName());
        currentConfig.setSelectedModel(config.getSelectedModel());
        currentConfig.setInitialInfectedUsers(config.getInitialInfectedUsers());
        currentConfig.setMaskComplianceRate(config.getMaskComplianceRate());
        currentConfig.setDefaultVentilationRate(config.getDefaultVentilationRate());
        currentConfig.setVirusDecayRate(config.getVirusDecayRate());
        currentConfig.setSimulationDurationSeconds(config.getSimulationDurationSeconds());
        currentConfig.setTotalUsers(config.getTotalUsers());
    }
    
    /**
     * Runs all models with contact-based trajectories.
     * Executes simulations for all three epidemic models (Simple Proximity,
     * Peng, and Lelieveld) using real contact data, collecting results for
     * each model.
     * 
     * @param totalUsers total number of users in the simulation
     * @param configName name of the configuration being tested
     * @param scenarioName name of the scenario directory
     * @param configNumber configuration number identifier
     * @return map of model names to their simulation results
     */
    private static Map<String, SimulationResult> runAllModelsWithContacts(
            int totalUsers, 
            String configName, 
            String scenarioName,
            int configNumber) {
        
        Map<String, SimulationResult> results = new HashMap<>();
        
        for (String model : MODELS) {
            try {
                EpidemicStatistics.resetInstance();
                
                SimulationResult result = runSingleContactSimulation(
                    totalUsers, configName, model, scenarioName);
                
                results.put(model, result);
                
            } catch (Exception e) {
                System.err.println("    Error in model " + model + ": " + e.getMessage());
                results.put(model, new SimulationResult());
            }
        }
        
        return results;
    }
    
    /**
     * Runs a single simulation with contact trajectories.
     * Creates and executes a simulation using real contact data from CSV,
     * configures epidemic parameters, evaluates final transmissions,
     * and collects comprehensive results including execution time.
     * 
     * @param numUsers total number of users to simulate
     * @param configName name of the configuration
     * @param epidemicModel name of the epidemic model to use
     * @param scenarioName name of the scenario directory
     * @return SimulationResult containing all metrics and statistics
     * @throws Exception if simulation creation or execution fails
     */
    private static SimulationResult runSingleContactSimulation(
            int numUsers, 
            String configName,
            String epidemicModel,
            String scenarioName) throws Exception {
        
        long startTime = System.currentTimeMillis();
        
        System.out.println("   🔬 Executing simulation with real contacts:");
        System.out.println("      - Configuration: " + configName);
        System.out.println("      - Model: " + epidemicModel);
        
        // Configure epidemic parameters
        EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
        epidemicConfig.setSelectedModel(epidemicModel);
        
        SimulationUtils.configureEpidemicParameters(epidemicConfig, null, epidemicModel);
        
        // Create simulation with real contacts
        Simulation simulation = SimulationUtils.createSimulationForScenario(
            null,
            numUsers,
            0, // Infection by cliques
            scenarioName,
            true 
        );
        
        if (simulation == null) {
            throw new Exception("Could not create simulation");
        }
        
        System.out.println("   ⏳ Executing simulation...");
        SimulationUtils.runSimulationToCompletion(simulation);
        
        // Evaluate final transmissions
        if (simulation.getEpidemicManager() != null) {
            simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(
                simulation.getAllUsers());
        }
        
        // Print contact statistics
        printContactStatistics(simulation);
        
        // Collect results
        SimulationResult result = SimulationUtils.collectSimulationResults(
            simulation, null, epidemicModel);
        
        long endTime = System.currentTimeMillis();
        result.executionTimeMs = endTime - startTime;
        
        System.out.printf("     Execution time: %.2f seconds\n", 
                        result.executionTimeMs / 1000.0);
        
        return result;
    }
    
    /**
     * Prints contact-specific statistics.
     * Displays detailed statistics about real contact patterns including
     * attack rates by clique, inter-clique coincidences, isolation metrics,
     * and verification of success criteria (95% isolation rate).
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
        
        // Success criteria verification
        Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
        double isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
        
        System.out.println("\n SUCCESS CRITERIA VERIFICATION:");
        System.out.printf("   Isolation rate: %.2f%%\n", isolationRate * 100);
        
        if (isolationRate >= 0.95) {
            System.out.println("   ✅ CRITERIA MET (≥95% isolation)");
        } else {
            System.out.println("    CRITERIA NOT MET (<95% isolation)");
        }
        
        System.out.println("\n" + "=".repeat(100) + "\n");
    }
    
    /**
     * Compares contact-based results.
     * Generates comprehensive comparison tables for all models and configurations,
     * displaying model-specific metrics (contacts for Simple Proximity, concentration
     * for aerosol models). Exports results to timestamped CSV file.
     * 
     * @param allResults nested map: configuration name -> model name -> simulation result
     */
    private static void compareContactResults(Map<String, Map<String, SimulationResult>> allResults) {
        File resultsDir = new File("./results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(new java.util.Date());
        String csvFilename = String.format("./results/contact_based_results_%s.csv", timestamp);
        
        try (FileWriter csvWriter = new FileWriter(csvFilename)) {
            csvWriter.append("MODELO,CONFIGURACION,TASA_ATAQUE_PCT,INFECTIVOS,METRICA_ESPECIFICA,TIEMPO_EJECUCION_SEG\n");
            
            for (String model : MODELS) {
                System.out.println("\n🔬 RESULTS FOR MODEL: " + model);
                System.out.println("━".repeat(90));
                
                if (model.equals("SIMPLE_PROXIMITY")) {
                    System.out.printf("%-25s %-12s %-12s %-12s %-15s\n", 
                        "CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONTACTS", "TIME (sec)");
                } else {
                    System.out.printf("%-25s %-12s %-12s %-28s %-15s\n",
                        "CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONCENTRATION", "TIME (sec)");
                }
                System.out.println("-".repeat(90));
                
                for (Map.Entry<String, Map<String, SimulationResult>> configEntry : allResults.entrySet()) {
                    String configName = configEntry.getKey();
                    SimulationResult result = configEntry.getValue().get(model);
                    
                    if (result != null) {
                        int totalInfectious = result.infectiousSymp + result.infectiousAsymp + 
                                            result.superSpreaders;
                        double attackRate = result.attackRate * 100;
                        double timeSec = result.executionTimeMs / 1000.0;
                        
                        if (model.equals("SIMPLE_PROXIMITY")) {
                            System.out.printf("%-25s %-12.2f %-12d %-12d %-15.2f\n",
                                configName, attackRate, totalInfectious, 
                                result.totalContacts, timeSec);
                            
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%d,%.2f\n",
                                model, configName, attackRate, totalInfectious,
                                result.totalContacts, timeSec));
                        } else {
                            System.out.printf("%-25s %-12.2f %-12d %-28.6f %-15.2f\n",
                                configName, attackRate, totalInfectious,
                                result.averageConcentration, timeSec);
                            
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%.6f,%.2f\n",
                                model, configName, attackRate, totalInfectious,
                                result.averageConcentration, timeSec));
                        }
                    }
                }
            }
            
            csvWriter.flush();
            System.out.println("\n✅ Results saved to: " + csvFilename);
            
        } catch (IOException e) {
            System.err.println(" Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}