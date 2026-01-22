package es.unizar.epidemic.tests.scenarios;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.epidemic.tests.Scenarios;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for synthetic scenarios with configurable parameters
 * 
 * @author Nacho Palacio
 */
public class SyntheticScenarioTests {
    
    private static final String[] MODELS = {"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"};
    
    /**
     * Runs all synthetic scenario tests.
     * Executes comprehensive testing with synthetic trajectories, running multiple
     * configurations and comparing results across different epidemic parameters.
     * Uses the first available scenario from the scenario list.
     */
    public static void runAll() {
        System.out.println("\n🔬 EXECUTING SYNTHETIC TESTS\n");
        
        List<Scenarios.TestScenario> scenarios = Scenarios.getAllScenarios();
        
        if (scenarios.isEmpty()) {
            System.err.println(" No scenarios available");
            return;
        }
        
        Scenarios.TestScenario scenario = scenarios.get(0);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" SIMULATING SCENARIO: " + scenario.name);
        System.out.println("📝 " + scenario.description);
        System.out.println("=".repeat(80));
        
        runMultipleConfigurations(scenario);
    }
    
    /**
     * Runs multiple epidemic configurations for a given scenario.
     * Creates and applies various test configurations (baseline, high ventilation,
     * masks, immunity, etc.), executes all models for each configuration,
     * and compares results.
     * 
     * @param baseScenario the base test scenario to use for all configurations
     */
    private static void runMultipleConfigurations(Scenarios.TestScenario baseScenario) {
        List<EpidemicConfiguration> configurations = createTestConfigurations();
        
        System.out.println("🔬 Executing " + configurations.size() + " different configurations...\n");
        
        Map<String, Map<String, SimulationResult>> allResults = new HashMap<>();
        
        for (int i = 0; i < configurations.size(); i++) {
            EpidemicConfiguration config = configurations.get(i);
            String configName = config.getConfigName();
            
            System.out.println("━".repeat(80));
            System.out.println("⚙️  CONFIGURACIÓN " + (i+1) + "/" + configurations.size() + ": " + configName);
            System.out.println("━".repeat(80));
            
            applyConfiguration(config);
            
            Map<String, SimulationResult> configResults = runAllModelsWithConfig(baseScenario, configName, i+1);
            allResults.put(configName, configResults);
            
            System.out.println("\n COMPARISON FOR: " + configName);
            System.out.println("-".repeat(60));
            SimulationUtils.compareModelResults(configResults, configName);
            
            System.out.println("\n");
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🏆 FINAL COMPARISON BETWEEN ALL CONFIGURATIONS");
        System.out.println("=".repeat(100));
        
        compareAllConfigurations(allResults);
    }
    
    /**
     * Creates a list of diverse epidemic configurations for testing.
     * Generates multiple configurations including baseline control, high ventilation,
     * mandatory masks, high immunity, superspreader events, poor ventilation,
     * resistant virus, combined measures, worst case, and best case scenarios.
     * Many configurations are commented out for selective testing.
     * 
     * @return list of EpidemicConfiguration instances for testing
     */
    private static List<EpidemicConfiguration> createTestConfigurations() {
        List<EpidemicConfiguration> configurations = new ArrayList<>();
        int simulationDurationSeconds = 5000;
        int[] visitorNumber = {100};
        
        for (int visitors : visitorNumber) {
            System.out.println("🔢 Preparing configurations for " + visitors + " visitors...");
            
            // *** 1. BASELINE CONFIGURATION (Control) ***
            EpidemicConfiguration baseline = EpidemicConfiguration.getInstance().clone();
            baseline.setConfigName("Baseline_Control");
            baseline.setMaskComplianceRate(0.1);
            baseline.setDefaultVentilationRate(3.0);
            baseline.setVirusDecayRate(0.62);
            baseline.setInitialInfectedUsers(5);
            baseline.setSuperSpreaderProbability(0.0);
            baseline.setSimulationDurationSeconds(simulationDurationSeconds);
            baseline.setTotalUsers(visitors);
            configurations.add(baseline);

            // // *** 2. HIGH VENTILATION ***
            // EpidemicConfiguration highVent = baseline.clone();
            // highVent.setConfigName("High_Ventilation");
            // highVent.setDefaultVentilationRate(12.0);
            // configurations.add(highVent);
            
            // // *** 3. MANDATORY MASKS ***
            // EpidemicConfiguration masks = baseline.clone();
            // masks.setConfigName("Mandatory_Masks");
            // masks.setMaskComplianceRate(0.95);
            // configurations.add(masks);
            
            // // *** 4. IMMUNE POPULATION ***
            // EpidemicConfiguration immune = baseline.clone();
            // immune.setConfigName("High_Immunity");
            // immune.setImmunePopulationFraction(1.0);
            // configurations.add(immune);

            // // *** 5. SUPERSPREADER EVENT ***
            // EpidemicConfiguration superEvent = baseline.clone();
            // superEvent.setConfigName("SuperSpreader_Event");
            // superEvent.setInitialInfectedUsers(5);
            // superEvent.setSuperSpreaderProbability(1.0);
            // superEvent.setMaskComplianceRate(0.0);
            // configurations.add(superEvent);
            
            // // *** 6. POOR VENTILATION ***
            // EpidemicConfiguration poorVent = baseline.clone();
            // poorVent.setConfigName("Poor_Ventilation");
            // poorVent.setDefaultVentilationRate(0.8);
            // configurations.add(poorVent);
            
            // // *** 7. RESISTANT VIRUS ***
            // EpidemicConfiguration resistantVirus = baseline.clone();
            // resistantVirus.setConfigName("Resistant_Virus");
            // resistantVirus.setVirusDecayRate(0.1);
            // configurations.add(resistantVirus);

            // // *** 8. COMBINED MEASURES ***
            // EpidemicConfiguration combined = baseline.clone();
            // combined.setConfigName("Combined_Measures");
            // combined.setMaskComplianceRate(0.85);
            // combined.setDefaultVentilationRate(8.0);
            // combined.setImmunePopulationFraction(0.40);
            // configurations.add(combined);
            
            // // *** 9. PESSIMISTIC SCENARIO ***
            // EpidemicConfiguration pessimistic = baseline.clone();
            // pessimistic.setConfigName("Worst_Case");
            // pessimistic.setDefaultVentilationRate(0.5);
            // pessimistic.setMaskComplianceRate(0.05);
            // pessimistic.setInitialInfectedUsers(12);
            // pessimistic.setVirusDecayRate(0.1);
            // configurations.add(pessimistic);
            
            // // *** 10. OPTIMISTIC SCENARIO ***
            // EpidemicConfiguration optimistic = baseline.clone();
            // optimistic.setConfigName("Best_Case");
            // optimistic.setDefaultVentilationRate(15.0);
            // optimistic.setMaskComplianceRate(0.98);
            // optimistic.setImmunePopulationFraction(0.70);
            // optimistic.setInitialInfectedUsers(2);
            // configurations.add(optimistic);
        }
        
        return configurations;
    }
    
    /**
     * Applies a given epidemic configuration.
     * Resets current configuration to defaults and applies all settings from
     * the provided configuration including model parameters, infection rates,
     * ventilation, masks, immunity, and model-specific parameters.
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
        currentConfig.setMaskExhalationEfficiency(config.getMaskExhalationEfficiency());
        currentConfig.setMaskInhalationEfficiency(config.getMaskInhalationEfficiency());
        currentConfig.setSimulationDurationSeconds(config.getSimulationDurationSeconds());
        currentConfig.setImmunePopulationFraction(config.getImmunePopulationFraction());
        currentConfig.setSuperSpreaderProbability(config.getSuperSpreaderProbability());
        currentConfig.setTotalUsers(config.getTotalUsers());
        
        currentConfig.setMaxTransmissionDistance(config.getMaxTransmissionDistance());
        currentConfig.setBaseTransmissionProbability(config.getBaseTransmissionProbability());
        currentConfig.setMinContactDuration(config.getMinContactDuration());
        currentConfig.setQuantaEmissionRate(config.getQuantaEmissionRate());
        currentConfig.setBreathingRate(config.getBreathingRate());
        currentConfig.setDepositionRate(config.getDepositionRate());
        currentConfig.setViralLoadHigh(config.getViralLoadHigh());
        currentConfig.setViralLoadSuper(config.getViralLoadSuper());
        currentConfig.setInfectiousDose(config.getInfectiousDose());
        currentConfig.setDepositionProbability(config.getDepositionProbability());
    }
    
    /**
     * Runs all epidemic models for a given configuration.
     * Executes simulations for each model in the MODELS array with the
     * specified scenario and configuration, collecting results for comparison.
     * 
     * @param scenario the test scenario to simulate
     * @param configName name of the configuration being tested
     * @param configNumber configuration number identifier
     * @return map of model names to their simulation results
     */
    private static Map<String, SimulationResult> runAllModelsWithConfig(
            Scenarios.TestScenario scenario, String configName, int configNumber) {

        Map<String, SimulationResult> configResults = new HashMap<>();
        
        for (String model : MODELS) {      
            try {
                EpidemicStatistics.resetInstance();
                SimulationResult result = SimulationUtils.runSingleSimulation(scenario, model, configNumber);
                configResults.put(model, result);
                
            } catch (Exception e) {
                System.err.println("    Error in model " + model + ": " + e.getMessage());
                configResults.put(model, new SimulationResult());
            }
        }
        
        return configResults;
    }
    
    /**
     * Compares results between different configurations.
     * Generates comprehensive comparison tables for all models and configurations,
     * showing attack rates, infectious counts, and model-specific metrics.
     * Ranks configurations by effectiveness and exports results to CSV file.
     * 
     * @param allResults nested map: configuration name -> model name -> simulation result
     */
    private static void compareAllConfigurations(Map<String, Map<String, SimulationResult>> allResults) {
        File resultsDir = new File("./results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String csvFilename = String.format("./results/comparison_all_configs_%s.csv", timestamp);
        
        try (FileWriter csvWriter = new FileWriter(csvFilename)) {
            csvWriter.append("MODELO,CONFIGURACION,TASA_ATAQUE_PCT,INFECTIVOS,METRICA_ESPECIFICA,RIESGO_INDIVIDUAL,TIEMPO_EJECUCION_SEG\n");
            
            for (String model : MODELS) {
                System.out.println("\n🔬 ANALYSIS FOR MODEL: " + model);
                System.out.println("━".repeat(90));
                
                if (model.equals("SIMPLE_PROXIMITY")) {
                    System.out.printf("%-20s %-12s %-12s %-12s %-12s %-15s\n", 
                        "CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONTACTS", "INFEC.CONT", "TIME (sec)");
                } 
                else if (model.equals("AEROSOL_PENG")) {
                    System.out.printf("%-20s %-12s %-12s %-28s %-12s %-15s\n",
                        "CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONCENTR. (quanta/m³)", "INDIV. RISK", "TIME (sec)");
                } else if (model.equals("AEROSOL_LELIEVELD")) {
                    System.out.printf("%-20s %-12s %-12s %-28s %-12s %-15s\n",
                        "CONFIGURATION", "ATTACK RATE", "INFECTIOUS", "CONCENTR. (RNA copies/m³)", "INDIV. RISK", "TIME (sec)");
                }
                System.out.println("-".repeat(110));
                
                List<ConfigResult> results = new ArrayList<>();
                
                for (Map.Entry<String, Map<String, SimulationResult>> configEntry : allResults.entrySet()) {
                    String configName = configEntry.getKey();
                    SimulationResult result = configEntry.getValue().get(model);
                    
                    if (result != null) {
                        int totalInfectious = result.infectiousSymp + result.infectiousAsymp + result.superSpreaders;
                        double attackRate = result.attackRate * 100;
                        double timeSec = result.executionTimeMs / 1000.0;
                        
                        if (model.equals("SIMPLE_PROXIMITY")) {
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%d,%d,%.2f\n",
                                model, configName, attackRate, totalInfectious,
                                result.totalContacts, result.infectiousContacts, timeSec));
                            
                            System.out.printf("%-20s %-12.2f %-12d %-12d %-12d %-15.2f\n",
                                configName, attackRate, totalInfectious,
                                result.totalContacts, result.infectiousContacts, timeSec);
                        } 
                        else {
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%.6f,%.2f,%.2f\n",
                                model, configName, attackRate, totalInfectious,
                                result.averageConcentration, result.individualRisk, timeSec));
                            
                            System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f %-15.2f\n",
                                configName, attackRate, totalInfectious,
                                result.averageConcentration, result.individualRisk, timeSec);
                        }
                        
                        results.add(new ConfigResult(configName, result.attackRate));
                    }
                }
                
                results.sort((a, b) -> Double.compare(a.attackRate, b.attackRate));
                
                if (!results.isEmpty()) {
                    System.out.println("\n   🏆 RANKING FOR " + model + ":");
                    System.out.println("      🟢 Most effective: " + results.get(0).configName + 
                                    String.format(" (%.2f%%)", results.get(0).attackRate * 100));
                    System.out.println("      🔴 Least effective: " + results.get(results.size()-1).configName + 
                                    String.format(" (%.2f%%)", results.get(results.size()-1).attackRate * 100));
                    
                    double reduction = (results.get(results.size()-1).attackRate - results.get(0).attackRate) * 100;
                    System.out.printf("      📉 Maximum reduction: %.2f percentage points\n", reduction);
                }
            }
            
            csvWriter.flush();
            System.out.println("\n✅ Results saved to: " + csvFilename);
            
        } catch (IOException e) {
            System.err.println(" Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Auxiliary class to hold configuration results
     */
    private static class ConfigResult {
        String configName;
        double attackRate;
        
        ConfigResult(String name, double rate) {
            this.configName = name;
            this.attackRate = rate;
        }
    }
}