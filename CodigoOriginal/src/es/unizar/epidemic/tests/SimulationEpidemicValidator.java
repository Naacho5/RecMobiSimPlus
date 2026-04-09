package es.unizar.epidemic.tests;

import es.unizar.epidemic.data.ContactTrajectoryBuilder;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator for epidemic simulations.
 * Currently deprecated
 * 
 * @author Nacho Palacio
 * @date 2025-09-14
 */
public class SimulationEpidemicValidator {
    static String[] models = {"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"};

    private static int currentIteration = 0;
    
    /**
     * Main entry point for epidemic simulation validation.
     * Executes validation scenarios to test epidemic models and configurations.
     * 
     * @param args command line arguments (not currently used)
     */
    public static void main(String[] args) {
        System.out.println("🧪 === VALIDACIÓN CON SIMULACIONES COMPLETAS ===");
        
        runValidationScenarios();

        // runTestsWithRealContacts();
    }
    
    /**
     * Runs all validation scenarios.
     * Retrieves available test scenarios and executes multiple epidemic
     * configurations to validate simulation behavior across different parameters.
     */
    private static void runValidationScenarios() {
        List<Scenarios.TestScenario> scenarios = Scenarios.getAllScenarios();
        
        if (scenarios.isEmpty()) {
            System.err.println(" No hay escenarios disponibles");
            return;
        }

        // Simulation simulation = new Simulation();
        // simulation.configureElementIdMapperForCurrentScenario();
        
        Scenarios.TestScenario scenario = scenarios.get(0);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" SIMULANDO ESCENARIO: " + scenario.name);
        System.out.println("📝 " + scenario.description);
        System.out.println("=".repeat(80));
        
        // testScenarioWithSingleModel(scenario, 1, "SIMPLE_PROXIMITY");
        // testScenarioWithAllModels(scenario, 1);
        runMultipleConfigurations(scenario);
    }

    /**
     * Runs multiple epidemic configurations for a given scenario.
     * Creates and applies various test configurations (baseline, high ventilation,
     * masks, immunity, superspreader events, etc.), executes all models for each
     * configuration, and compares results across configurations.
     * 
     * @param baseScenario the base test scenario to use for all configurations
     */
    private static void runMultipleConfigurations(Scenarios.TestScenario baseScenario) {
        List<EpidemicConfiguration> configurations = createTestConfigurations();
        
        System.out.println("🔬 Ejecutando " + configurations.size() + " configuraciones diferentes...\n");
        
        java.util.Map<String, java.util.Map<String, SimulationResult>> allResults = new java.util.HashMap<>();
        
        for (int i = 0; i < configurations.size(); i++) {
            EpidemicConfiguration config = configurations.get(i);
            String configName = config.getConfigName();
            
            System.out.println("━".repeat(80));
            System.out.println("⚙️  CONFIGURACIÓN " + (i+1) + "/" + configurations.size() + ": " + configName);
            System.out.println("━".repeat(80));
            
            applyConfiguration(config);
            
            java.util.Map<String, SimulationResult> configResults = runAllModelsWithConfig(baseScenario, configName, i+1);
            allResults.put(configName, configResults);
            
            System.out.println("\n COMPARACIÓN PARA: " + configName);
            System.out.println("-".repeat(60));
            compareModelResults(configResults, configName);
            
            System.out.println("\n");
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🏆 COMPARACIÓN FINAL ENTRE TODAS LAS CONFIGURACIONES");
        System.out.println("=".repeat(100));
        
        compareAllConfigurations(allResults);
    }

    /**
     * Creates a list of diverse epidemic configurations for testing.
     * Generates configurations including baseline control, high ventilation,
     * mandatory masks, high immunity, superspreader events, poor ventilation,
     * resistant virus, combined measures, worst case, and best case scenarios.
     * Each configuration varies key parameters like ventilation rate, mask compliance,
     * and immunity levels to test different intervention strategies.
     * 
     * @return list of EpidemicConfiguration instances representing different test scenarios
     */
    private static List<EpidemicConfiguration> createTestConfigurations() {
        List<EpidemicConfiguration> configurations = new ArrayList<>();
        int simulationDurationSeconds = 2500; // 10800 segundos (aprox. 3 horas)

        int[] visitorNumber = {100};
        
        for (int visitors : visitorNumber) {
            System.out.println("🔢 Preparando configuraciones para " + visitors + " visitantes...");
        
            // *** 1. CONFIGURACIÓN BASELINE (Control) ***
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

            // // *** 2. ALTA VENTILACIÓN ***
            EpidemicConfiguration highVent = baseline.clone();
            highVent.setConfigName("High_Ventilation");
            highVent.setDefaultVentilationRate(12.0);  // 4x más ventilación
            configurations.add(highVent);
            
            // *** 3. MASCARILLAS OBLIGATORIAS ***
            EpidemicConfiguration masks = baseline.clone();
            masks.setConfigName("Mandatory_Masks");
            masks.setMaskComplianceRate(0.95);  // 95% uso de mascarillas
            configurations.add(masks);
            
            // *** 4. POBLACIÓN INMUNE ***
            EpidemicConfiguration immune = baseline.clone();
            immune.setConfigName("High_Immunity");
            immune.setImmunePopulationFraction(1.0);  // 60% inmunes
            configurations.add(immune);

            // *** 5. SUPERSPREADER EVENT ***
            EpidemicConfiguration superEvent = baseline.clone();
            superEvent.setConfigName("SuperSpreader_Event");
            superEvent.setInitialInfectedUsers(5);
            superEvent.setSuperSpreaderProbability(1.0);  // 100% superspreaders
            superEvent.setMaskComplianceRate(0.0);  // Sin mascarillas
            configurations.add(superEvent);
            
            // // *** 6. VENTILACIÓN POBRE ***
            EpidemicConfiguration poorVent = baseline.clone();
            poorVent.setConfigName("Poor_Ventilation");
            poorVent.setDefaultVentilationRate(0.8);  // Ventilación muy pobre
            configurations.add(poorVent);
            
            // *** 7. VIRUS RESISTENTE ***
            EpidemicConfiguration resistantVirus = baseline.clone();
            resistantVirus.setConfigName("Resistant_Virus");
            resistantVirus.setVirusDecayRate(0.1);
            configurations.add(resistantVirus);

            // *** 8. MEDIDAS COMBINADAS ***
            EpidemicConfiguration combined = baseline.clone();
            combined.setConfigName("Combined_Measures");
            combined.setMaskComplianceRate(0.85);
            combined.setDefaultVentilationRate(8.0);
            combined.setImmunePopulationFraction(0.40);
            configurations.add(combined);
            
            // *** 9. ESCENARIO PESIMISTA ***
            EpidemicConfiguration pessimistic = baseline.clone();
            pessimistic.setConfigName("Worst_Case");
            pessimistic.setDefaultVentilationRate(0.5);
            pessimistic.setMaskComplianceRate(0.05);
            pessimistic.setInitialInfectedUsers(12);
            pessimistic.setVirusDecayRate(0.1);
            configurations.add(pessimistic);
            
            // *** 10. ESCENARIO OPTIMISTA ***
            EpidemicConfiguration optimistic = baseline.clone();
            optimistic.setConfigName("Best_Case");
            optimistic.setDefaultVentilationRate(15.0);
            optimistic.setMaskComplianceRate(0.98);
            optimistic.setImmunePopulationFraction(0.70);
            optimistic.setInitialInfectedUsers(2);
            configurations.add(optimistic);
        }
        
        return configurations;
    }

    /**
     * Compares results between different configurations across all models.
     * Generates comprehensive comparison tables showing attack rates, infectious counts,
     * and model-specific metrics (contacts, aerosol concentration, individual risk).
     * Ranks configurations by effectiveness and exports results to timestamped CSV file.
     * 
     * @param allResults nested map: configuration name -> model name -> simulation result
     */
    private static void compareAllConfigurations(java.util.Map<String, java.util.Map<String, SimulationResult>> allResults) {
        // ✅ NUEVO: Crear directorio para resultados si no existe
        File resultsDir = new File("./results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        // ✅ NUEVO: Nombre del archivo con timestamp
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String csvFilename = String.format("./results/comparison_all_configs_%s.csv", timestamp);
        
        try (FileWriter csvWriter = new FileWriter(csvFilename)) {
            // ✅ MODIFICADO: Añadir columna TIEMPO_EJECUCION_SEG
            csvWriter.append("MODELO,CONFIGURACION,TASA_ATAQUE_PCT,INFECTIVOS,METRICA_ESPECIFICA,RIESGO_INDIVIDUAL,TIEMPO_EJECUCION_SEG\n");
            
            for (String model : models) {
                System.out.println("\n🔬 ANÁLISIS PARA MODELO: " + model);
                System.out.println("━".repeat(90));
                
                // ✅ MODIFICADO: Añadir columna de tiempo a la cabecera
                if (model.equals("SIMPLE_PROXIMITY")) {
                    System.out.printf("%-20s %-12s %-12s %-12s %-12s %-15s\n", 
                        "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC", "TIEMPO (seg)");
                } 
                else if (model.equals("AEROSOL_PENG")) {
                    System.out.printf("%-20s %-12s %-12s %-28s %-12s %-15s\n",
                        "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.", "TIEMPO (seg)");
                } else if (model.equals("AEROSOL_LELIEVELD")) {
                    System.out.printf("%-20s %-12s %-12s %-28s %-12s %-15s\n",
                        "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.", "TIEMPO (seg)");
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
                        
                        // ✅ MODIFICADO: Añadir tiempo al CSV y consola
                        if (model.equals("SIMPLE_PROXIMITY")) {
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%d,%d,%.2f\n",
                                model,
                                configName,
                                attackRate,
                                totalInfectious,
                                result.totalContacts,
                                result.infectiousContacts,
                                timeSec
                            ));
                            
                            System.out.printf("%-20s %-12.2f %-12d %-12d %-12d %-15.2f\n",
                                configName,
                                attackRate,
                                totalInfectious,
                                result.totalContacts,
                                result.infectiousContacts,
                                timeSec
                            );
                        } 
                        else {
                            csvWriter.append(String.format("%s,%s,%.2f,%d,%.6f,%.2f,%.2f\n",
                                model,
                                configName,
                                attackRate,
                                totalInfectious,
                                result.averageConcentration,
                                result.individualRisk,
                                timeSec
                            ));
                            
                            System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f %-15.2f\n",
                                configName,
                                attackRate,
                                totalInfectious,
                                result.averageConcentration,
                                result.individualRisk,
                                timeSec
                            );
                        }
                        
                        results.add(new ConfigResult(configName, result.attackRate));
                    }
                }
                
                // Ranking (solo en consola)
                results.sort((a, b) -> Double.compare(a.attackRate, b.attackRate));
                
                if (!results.isEmpty()) {
                    System.out.println("\n   🏆 RANKING PARA " + model + ":");
                    System.out.println("      🟢 Más efectiva: " + results.get(0).configName + 
                                    String.format(" (%.2f%%)", results.get(0).attackRate * 100));
                    System.out.println("      🔴 Menos efectiva: " + results.get(results.size()-1).configName + 
                                    String.format(" (%.2f%%)", results.get(results.size()-1).attackRate * 100));
                    
                    double reduction = (results.get(results.size()-1).attackRate - results.get(0).attackRate) * 100;
                    System.out.printf("      📉 Reducción máxima: %.2f puntos porcentuales\n", reduction);
                }
            }
            
            csvWriter.flush();
            System.out.println("\n✅ Resultados guardados en: " + csvFilename);
            
        } catch (IOException e) {
            System.err.println(" Error al escribir CSV: " + e.getMessage());
            e.printStackTrace();
        }
        
        // analyzeInterventionEffectiveness(allResults);
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

    /**
     * Applies a given epidemic configuration to the global configuration instance.
     * Resets current configuration to defaults and applies all settings from
     * the provided configuration including model selection, infection parameters,
     * ventilation, mask compliance, immunity, and model-specific parameters.
     * 
     * @param config the epidemic configuration to apply globally
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
        
        // Parámetros específicos del modelo
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
     * Executes simulations for each model in the models array with the
     * specified scenario and configuration, collecting results for comparison.
     * Resets epidemic statistics between each model execution.
     * 
     * @param scenario the test scenario to simulate
     * @param configName name of the configuration being tested
     * @param configNumber configuration number identifier for logging
     * @return map of model names to their simulation results
     */
    private static java.util.Map<String, SimulationResult> runAllModelsWithConfig(
            Scenarios.TestScenario scenario, String configName, int configNumber) {

        java.util.Map<String, SimulationResult> configResults = new java.util.HashMap<>();
        
        for (String model : models) {      
            try {
                EpidemicStatistics.resetInstance();
                SimulationResult result = runSingleSimulation(scenario, model, configNumber);
                configResults.put(model, result);
                
            } catch (Exception e) {
                System.err.println("    Error en modelo " + model + ": " + e.getMessage());
                configResults.put(model, new SimulationResult()); // Resultado vacío
            }
        }
        
        return configResults;
    }
    
    /**
     * Runs a single simulation with specified parameters.
     * Configures epidemic parameters, creates simulation instance, executes
     * simulation to completion, and collects results including execution time.
     * Calculates both theoretical and retrospective risk metrics.
     * 
     * @param scenario the test scenario configuration (null for default parameters)
     * @param model the epidemic model to use (SIMPLE_PROXIMITY, AEROSOL_PENG, or AEROSOL_LELIEVELD)
     * @param scenarioNumber scenario identifier for logging purposes
     * @return SimulationResult containing infection counts, attack rate, and metrics
     */
    private static SimulationResult runSingleSimulation(Scenarios.TestScenario scenario, String model, int scenarioNumber) {
        long startTime = System.currentTimeMillis();
        try {
            EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
            configureEpidemicParameters(epidemicConfig, scenario, model);

            // int totalUsers = (scenario != null) ? scenario.standardVisitorCount : 99;
            int totalUsers = epidemicConfig.getTotalUsers();
            int infectiousUsers = epidemicConfig.getInitialInfectedUsers();

            epidemicConfig.printCurrentConfiguration();

            boolean useRealContacts = false;

            System.out.println("   🔬 Ejecutando simulación sintética:");
            System.out.println("      - Total usuarios: " + totalUsers);
            System.out.println("      - Infectados iniciales: " + infectiousUsers);

            // Simulation simulation = createSimulationForScenario(scenario, totalUsers, infectiousUsers,"SIMPLE_SCENARIO", useRealContacts);
            Simulation simulation = createSimulationForScenario(scenario, totalUsers, infectiousUsers,"MoMA_Museum", useRealContacts);
            // Simulation simulation = createSimulationForScenario(scenario, totalUsers, infectiousUsers,"GranCasa", useRealContacts);
            
            if (simulation == null) {
                System.err.println(" Error: No se pudo crear la simulación");
                return new SimulationResult();
            }
            
            System.out.println("   ⏳ Ejecutando simulación...");
            runSimulationToCompletion(simulation);

            if (simulation.getEpidemicManager() != null) {
                simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(simulation.getAllUsers());
            }

            double avgRisk = simulation.calculateAverageTheoreticalRiskForAllRooms();
            double retrospectiveRisk = simulation.calculateRetrospectiveRiskForAllUsers();

            System.out.println("    Riesgo teórico vs. retrospectivo en modelo " + model + ": y escenario " + scenario.name);
            System.out.println("    Riesgo teórico promedio (habitaciones): " + String.format("%.4f", avgRisk));
            System.out.println("    Riesgo retrospectivo promedio (usuarios): " + String.format("%.4f", retrospectiveRisk));

            SimulationResult result = collectSimulationResults(simulation, scenario, model);
        
            long endTime = System.currentTimeMillis();
            result.executionTimeMs = endTime - startTime;
            
            System.out.printf("     Tiempo de ejecución: %.2f segundos\n", result.executionTimeMs / 1000.0);
            
            return result;
            
        } catch (Exception e) {
            System.err.println(" Error en simulación: " + e.getMessage());
            e.printStackTrace();
            return new SimulationResult();
        }
    }
    
    /**
     * Configures epidemic parameters based on the scenario and model.
     * Sets model-specific parameters including mask compliance, ventilation rates,
     * virus decay, quanta emission (Peng model), or viral load (Lelieveld model).
     * Uses scenario parameters if provided, otherwise uses EpidemicConfiguration values.
     * 
     * @param config the epidemic configuration to modify
     * @param scenario the test scenario containing parameter overrides (may be null)
     * @param model the epidemic model being configured
     */
    private static void configureEpidemicParameters(EpidemicConfiguration config, Scenarios.TestScenario scenario, String model) {
        config.setSelectedModel(model);
        
        String configName = config.getConfigName();
        boolean isBaseline = configName == null || configName.equals("Default") || configName.equals("Baseline_Control");
        
        if (scenario == null) {
            System.out.println("    Usando valores de EpidemicConfiguration para " + configName);
            System.out.println("      - Mascarillas: " + (config.getMaskComplianceRate() * 100) + "%");
            System.out.println("      - Ventilación: " + config.getDefaultVentilationRate() + " h⁻¹");
            System.out.println("      - Virus decay: " + config.getVirusDecayRate() + " h⁻¹");
        } else {
            if (isBaseline) {
                config.setDefaultVentilationRate(scenario.ventilationRate);
                config.setVirusDecayRate(scenario.virusDecayRate);
                // config.setMaskComplianceRate(scenario.maskFraction);
                
                System.out.println("    Aplicando valores del escenario para " + configName);
            } else {
                System.out.println("   🔒 Manteniendo valores específicos para " + configName);
                System.out.println("      - Mascarillas: " + (config.getMaskComplianceRate() * 100) + "%");
                System.out.println("      - Ventilación: " + config.getDefaultVentilationRate() + " h⁻¹");
            }
        }
        
        // Configurar parámetros específicos del modelo
        switch (model) {
            case "SIMPLE_PROXIMITY":
                // config.setMaxTransmissionDistance(2.0);
                // config.setBaseTransmissionProbability(0.05);
                // config.setMinContactDuration(10);
                break;
            case "AEROSOL_PENG":
                config.setQuantaEmissionRate(232.5);
                config.setBreathingRate(0.72);
                config.setDepositionRate(0.3);
                break;
            case "AEROSOL_LELIEVELD":
                // config.setViralLoadHigh(1.5E7);
                // config.setViralLoadSuper(5E9);
                config.setViralLoadHigh(5e6);
                config.setViralLoadSuper(5E7);
                config.setInfectiousDose(316);
                config.setDepositionProbability(0.5);
                break;
        }
        
        System.out.println("   ✅ Configuración epidémica aplicada para " + model);
        System.out.println("   🎭 Mascarillas finales: " + (config.getMaskComplianceRate() * 100) + "%");
        System.out.println("   🌬️  Ventilación final: " + config.getDefaultVentilationRate() + " h⁻¹");
    }
    
    /**
     * Creates a simulation instance for the given scenario using scenario-specific files.
     * Loads room, item, graph, and path files from the scenario directory.
     * Initializes simulation with network type, recommendation algorithm, and user counts.
     * Optionally configures simulation to use real contact trajectories from CSV data.
     * 
     * @param scenario the test scenario (may be null)
     * @param totalUsers total number of users in the simulation
     * @param infectiousUsers number of initially infected users
     * @param scenarioName name of the scenario directory (e.g., "MoMA_Museum", "GranCasa")
     * @param useRealContacts whether to use real contact trajectories from CSV data
     * @return configured Simulation instance, or null if setup fails
     */
    private static Simulation createSimulationForScenario(Scenarios.TestScenario scenario, int totalUsers, int infectiousUsers, String scenarioName, boolean useRealContacts) {
        System.out.println("    Cargando " + scenarioName + " desde archivos específicos...");
        
        java.io.File mapDir = new java.io.File("./dist/resources/maps/" + scenarioName + "/");
        if (!mapDir.exists()) {
            mapDir = new java.io.File("./resources/maps/" + scenarioName + "/");
        }
        
        if (!mapDir.exists()) {
            System.err.println(" Error: No se encuentra el directorio: " + scenarioName);
            return null;
        }
        
        java.io.File roomFile = new java.io.File(mapDir, "room_floor_combined.txt");
        java.io.File itemFile = new java.io.File(mapDir, "item_floor_combined.txt");
        java.io.File graphFile = new java.io.File(mapDir, "graph_floor_combined.txt");

        int nonSpecialUsers = 99;
        int specialUsers = 1;

        // String pathsFileName = "rand_non_special_user_paths_" + (totalUsers) + ".txt";

        // int nonSpecialUsers = 1; 
        // int specialUsers = totalUsers - 1;

        // int nonSpecialUsers = totalUsers - 1; 
        // int specialUsers = 1;

        // ✅ AJUSTE: Si usa contactos reales, obtener número real de usuarios del CSV
        if (useRealContacts) {
            try {
                String csvPath = "../src/es/unizar/epidemic/data/contactos.csv";
                int uniqueUsersInCSV = ContactTrajectoryBuilder.getUniqueUserCount(csvPath);
                
                System.out.println("    Usuarios totales en CSV: " + uniqueUsersInCSV);
                
                int usersToSimulate = Math.min(totalUsers, uniqueUsersInCSV);
                
                System.out.println("    Limitando simulación a: " + usersToSimulate + " usuarios");
                
                nonSpecialUsers = usersToSimulate;
                specialUsers = 0; // Sin usuarios especiales en modo contactos
                totalUsers = usersToSimulate;
                
            } catch (Exception e) {
                System.err.println("   Warning! Error contando usuarios del CSV: " + e.getMessage());
            }
        }
        else {
            System.out.println("Creando simulación sin contactos reales...");
        }

        String pathsFileName = "rand_non_special_user_paths_" + totalUsers + ".txt";


        java.io.File pathsFile = new java.io.File(mapDir, pathsFileName);
        
        if (!roomFile.exists() || !itemFile.exists() || !graphFile.exists() || !pathsFile.exists()) {
            System.err.println(" Error: Faltan archivos del: " + scenarioName);
            System.err.println("   - Room file: " + roomFile.exists());
            System.err.println("   - Item file: " + itemFile.exists());
            System.err.println("   - Graph file: " + graphFile.exists());
            System.err.println("   - Paths file: " + pathsFile.exists() + " (" + pathsFile.getAbsolutePath() + ")");
            return null;
        }
        
        System.out.println("   📁 Usando archivo de paths existente: " + pathsFileName);
        
        es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
        es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
        es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();

        try {
            int timeAvailable = 17; // Hours
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
            
            // int specialUsers = 1; 
            
            System.out.println("    Creando simulación con " + nonSpecialUsers + " usuarios no especiales + " + specialUsers + " especial");
            System.out.println("   📁 Archivo de paths: " + pathsFile.getAbsolutePath());

            String recommendationAlgorithm = "Completely-random (FULLY-RAND)";
            // String recommendationAlgorithm = "Non-Risk-Aware (Non-Risk-Aware)";
            // String recommendationAlgorithm = "Risk-Aware (Risk-Aware)";
            String networkType = "Centralized (Centralized)"; // Centralizada fija
            Simulation simulation = new Simulation(
                timeAvailable, delayObserving, timeIteration, screenRefresh, timeForPaths,
                userVelocity, kmToPixel, ttl, timeOnStairs, minTimeUpdate, commRange,
                maxKnowledge, commBandwidth, latency, specialUsers, nonSpecialUsers,
                pathsFile.getAbsolutePath(), "Random Path", recommendationAlgorithm, 2.5f, 10,
                "Opportunistic", 0.4, 40, 0.5, networkType, timeChangeMood,
                false, System.currentTimeMillis(), false, false, 0.0
            );
            
            Configuration.simulation = simulation;
            simulation.configureElementIdMapperForCurrentScenario();

            if (useRealContacts) {
                System.out.println("    Activando modo de contactos reales...");
                
                if (Configuration.instance == null) {
                    Configuration.instance = new Configuration(null, false);
                }
                
                // if (Configuration.instance.useContactTrajectoriesCheckBox == null) {
                //     Configuration.instance.useContactTrajectoriesCheckBox = new javax.swing.JCheckBox();
                // }
                
                // Configuration.instance.useContactTrajectoriesCheckBox.setSelected(true);
                
                System.out.println("    Limitando a " + totalUsers + " usuarios para test...");
                simulation.setMaxUsersForTest(totalUsers);
            }

            
            System.out.println("   🏗️ Inicializando floor panel...");
            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            
            System.out.println("    Cargando " + scenarioName + "...");
            
            es.unizar.gui.graph.DrawFloorGraph floor = new es.unizar.gui.graph.DrawFloorGraph();
            
            boolean ifRemoveVertexLabel = true;
            boolean ifRemoveEdges = false;
            
            com.mxgraph.swing.mxGraphComponent graphComponent = floor.drawFloor(
                roomFile, itemFile, ifRemoveVertexLabel, ifRemoveEdges, 1);
            
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
            
            System.out.println("   ✅ " + scenarioName + " cargado correctamente");
            System.out.println("    Saltando generación de paths - usando archivo existente");
            System.out.println("   📁 Paths serán leídos desde: " + pathsFile.getName());
            
            if (simulation.graphSpecialUser != null) {
                int numItems = simulation.graphSpecialUser.accessItemFile.getNumberOfItems();
                int numRooms = simulation.graphSpecialUser.accessGraphFile.getNumberOfRoom();

                System.out.println("    Información del " + scenarioName + ":");
                System.out.println("      - Items: " + numItems + " (paintings)");
                System.out.println("      - Habitaciones: " + numRooms);
                System.out.println("      - Dimensiones: 500 x 500 píxeles");
            }
            
            return simulation;
            
        } catch (Exception e) {
            System.err.println(" Error creando simulación para " + scenarioName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
            
        } finally {
            es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
            es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
            es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();
        }
    }

    /**
     * Crea simulación con modo mixto activado
     * Added by Nacho Palacio 2025-01-15
     */
    private static Simulation createSimulationWithMixedMode(
            Scenarios.TestScenario scenario,
            int totalUsers,
            double independentRatio,
            String scenarioName) {
        
        System.out.println("    Creando simulación en MODO MIXTO:");
        System.out.println("      - Total usuarios: " + totalUsers);
        System.out.println("      - Ratio independientes: " + (independentRatio * 100) + "%");
        
        try {
            // ✅ PASO 1: Intentar ambas rutas (igual que createSimulationForScenario)
            java.io.File mapDir = new java.io.File("./dist/resources/maps/" + scenarioName + "/");
            if (!mapDir.exists()) {
                mapDir = new java.io.File("./resources/maps/" + scenarioName + "/");
            }
            
            if (!mapDir.exists()) {
                System.err.println(" Error: No se encuentra el directorio: " + scenarioName);
                System.err.println("   Rutas intentadas:");
                System.err.println("      - ./dist/resources/maps/" + scenarioName + "/");
                System.err.println("      - ./resources/maps/" + scenarioName + "/");
                return null;
            }
            
            System.out.println("    Directorio encontrado: " + mapDir.getAbsolutePath());
            
            // ✅ PASO 2: Construir archivos desde mapDir verificado
            File roomFile = new File(mapDir, "room_floor_combined.txt");
            File itemFile = new File(mapDir, "item_floor_combined.txt");
            File graphFile = new File(mapDir, "graph_floor_combined.txt");
            
            // ✅ PASO 3: Verificar que los archivos existan
            if (!roomFile.exists() || !itemFile.exists() || !graphFile.exists()) {
                System.err.println(" Error: Faltan archivos del: " + scenarioName);
                System.err.println("   - Room file: " + roomFile.exists() + 
                                " (" + roomFile.getAbsolutePath() + ")");
                System.err.println("   - Item file: " + itemFile.exists() + 
                                " (" + itemFile.getAbsolutePath() + ")");
                System.err.println("   - Graph file: " + graphFile.exists() + 
                                " (" + graphFile.getAbsolutePath() + ")");
                return null;
            }
            
            System.out.println("   ✅ Archivos verificados:");
            System.out.println("      - room_floor_combined.txt: " + roomFile.getAbsolutePath());
            System.out.println("      - item_floor_combined.txt: " + itemFile.getAbsolutePath());
            System.out.println("      - graph_floor_combined.txt: " + graphFile.getAbsolutePath());
            
            // ✅ PASO 4: CONFIGURAR LITERALS ANTES DE CREAR SIMULATION
            es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
            es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
            es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();
            
            System.out.println("   ✅ Literals configurados correctamente");
            
            // ✅ PASO 5: Parámetros de simulación (igual que antes)
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
            
            // ✅ PASO 6: Verificar archivo de paths
            if (!pathsFile.exists()) {
                System.err.println(" Error: Archivo de paths no existe: " + pathsFile.getAbsolutePath());
                return null;
            }
            
            String recommendationAlgorithm = "Completely-random (FULLY-RAND)";
            String networkType = "Centralized (Centralized)";
            
            // ✅ PASO 7: CONSTRUCTOR DE SIMULATION
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
            
            // ✅ PASO 8: Configurar floor panel
            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            
            // ✅ PASO 9: Cargar escenario con drawFloor()
            System.out.println("    Cargando " + scenarioName + "...");
            
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
            
            System.out.println("   ✅ Simulación en modo mixto creada correctamente");
            
            return simulation;
            
        } catch (Exception e) {
            System.err.println(" Error creando simulación en modo mixto: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Test validation with mixed mode (cliques + independent users).
     * Creates and executes a simulation combining clique-based users with
     * independent users to validate mixed interaction patterns.
     * Reports infection metrics and contact statistics for analysis.
     * Added by Nacho Palacio 2025-01-15
     */
    public static void testMixedModeValidation() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🧪 TEST DE VALIDACIÓN: MODO MIXTO (CLIQUES + INDEPENDIENTES)");
        System.out.println("=".repeat(100));
        
        try {
            // ✅ CONFIGURACIÓN DEL TEST
            int totalUsers = 50;
            double independentRatio = 0.3; // 30% independientes
            String scenarioName = "MoMA_Museum";
            String model = "AEROSOL_PENG";
            
            // ✅ CONFIGURAR PARÁMETROS EPIDÉMICOS
            EpidemicConfiguration config = EpidemicConfiguration.getInstance();
            config.setSelectedModel(model);
            config.setInitialInfectedUsers(5);
            config.setMaskComplianceRate(0.1);
            config.setDefaultVentilationRate(3.0);
            config.setSimulationDurationSeconds(360);
            
            // ✅ CREAR SIMULACIÓN CON MODO MIXTO
            System.out.println("\n Configuración del test:");
            System.out.println("   - Total usuarios: " + totalUsers);
            System.out.println("   - Ratio independientes: " + (independentRatio * 100) + "%");
            System.out.println("   - Usuarios en cliques: " + (int)((1.0 - independentRatio) * totalUsers));
            System.out.println("   - Usuarios independientes: " + (int)(independentRatio * totalUsers));
            System.out.println("   - Modelo epidémico: " + model);
            
            Simulation simulation = createSimulationWithMixedMode(
                null, 
                totalUsers, 
                independentRatio, 
                scenarioName
            );
            
            if (simulation == null) {
                throw new Exception("No se pudo crear simulación en modo mixto");
            }
            
            // ✅ EJECUTAR SIMULACIÓN
            System.out.println("\n⏳ Ejecutando simulación...");
            runSimulationToCompletion(simulation);
            
            // ✅ RECOLECTAR RESULTADOS
            SimulationResult result = collectSimulationResults(simulation, null, model);
            
            // ✅ IMPRIMIR RESULTADOS
            System.out.println("\n" + "=".repeat(100));
            System.out.println(" RESULTADOS DEL TEST");
            System.out.println("=".repeat(100));
            
            System.out.printf("Tasa de ataque global: %.2f%%\n", result.attackRate * 100);
            System.out.printf("Infectivos totales: %d\n", 
                result.infectiousSymp + result.infectiousAsymp + result.superSpreaders);
            
            // ✅ VERIFICAR HIPÓTESIS (implementar después)
            System.out.println("\n VALIDACIÓN DE HIPÓTESIS:");
            System.out.println("   (Implementar métricas específicas cuando funcione correctamente)");
            printContactStatistics(simulation);
            
            System.out.println("\n" + "=".repeat(100));
            System.out.println("✅ TEST COMPLETADO");
            System.out.println("=".repeat(100) + "\n");
            
        } catch (Exception e) {
            System.err.println(" Error en test de modo mixto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * Runs the simulation until completion or max iterations.
     * Initializes users, epidemic system, and executes simulation iterations
     * based on simulated time. Continues until configured simulation duration
     * is reached or maximum iterations exceeded.
     * 
     * @param simulation the simulation instance to execute
     */
    private static void runSimulationToCompletion(Simulation simulation) {
        if (simulation == null) {
            System.err.println(" Error: simulación es null");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        currentIteration = 0;

        Configuration.simulation = simulation;
        
        java.util.Map<Integer, es.unizar.gui.UserInfo.UserState> stateOfUsers = 
            new java.util.HashMap<Integer, es.unizar.gui.UserInfo.UserState>();
        java.util.Map<es.unizar.util.Pair<Integer,Integer>, Double> timeUsersInRooms = 
            new java.util.HashMap<es.unizar.util.Pair<Integer,Integer>, Double>();

        // Added by Nacho Palacio 09-30-2025
        if (simulation.getNetworkType().equalsIgnoreCase("Centralized (Centralized)")) {
            simulation.initializeUserDB_Centralized(simulation.getRecommendationAlgorithm());
        }
        
        try {
            simulation.initializeUsers();
            System.out.println("    Usuarios inicializados correctamente");

            List<User> users = simulation.getAllUsers();
            System.out.println("    Total usuarios en simulación: " + users.size());
            
            User[] userArray = users.toArray(new User[0]);
            
            es.unizar.gui.simulation.UserRunnable realUserRunnable = 
                new es.unizar.gui.simulation.UserRunnable(
                    userArray,
                    stateOfUsers,
                    timeUsersInRooms
                );
            MainSimulator.userRunnable = realUserRunnable;
            
            System.out.println("    Inicializando sistema epidémico...");
            
            if (simulation.epidemicManager == null) {
                simulation.epidemicManager = new es.unizar.epidemic.general.EpidemicSimulationManager();
            }
            
            for (User user : users) {
                if (user.getEpidemicExtension() == null) {
                    user.setEpidemicExtension(new es.unizar.epidemic.general.UserEpidemicExtension());
                }
                
                es.unizar.gui.UserInfo.UserState userState = 
                    new es.unizar.gui.UserInfo.UserState(user.room);
                stateOfUsers.put(user.userID, userState);
            }
            
            simulation.epidemicManager.initializeEpidemicSystem(users);
            System.out.println("   ✅ Sistema epidémico inicializado correctamente");
           
        } catch (Exception e) {
            System.err.println(" Error inicializando usuarios: " + e.getMessage());
            e.printStackTrace();
            
            System.out.println("    Intentando inicialización alternativa...");
            
            try {
                initializeUsersWithBasicLocations(simulation);
                System.out.println("   ✅ Usuarios inicializados con ubicaciones básicas");
                
                List<User> users = simulation.getAllUsers();
                
                User[] userArray = users.toArray(new User[0]);
                es.unizar.gui.simulation.UserRunnable fallbackUserRunnable = 
                    new es.unizar.gui.simulation.UserRunnable(
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
                        user.setEpidemicExtension(new es.unizar.epidemic.general.UserEpidemicExtension());
                    }
                    
                    es.unizar.gui.UserInfo.UserState userState = 
                        new es.unizar.gui.UserInfo.UserState(user.room);
                    stateOfUsers.put(user.userID, userState);
                }
                
                simulation.epidemicManager.initializeEpidemicSystem(users);
                
            } catch (Exception e2) {
                System.err.println(" Error en inicialización alternativa: " + e2.getMessage());
                return;
            }
        }
        
        int maxIterations = Integer.MAX_VALUE;
        int iteration = 0;

        double simulatedTimeElapsed = 0.0;
        double timePerIteration = simulation.getTimeForIterationInSecond();

        System.out.println("   ⏳ Iniciando simulación basada en tiempo simulado...");
        System.out.println("   📏 Tiempo por iteración: " + timePerIteration + " segundos simulados");
        System.out.println("    Tiempo objetivo: " + getMaxSimulatedTime() + " segundos simulados");
        
        startTime = System.currentTimeMillis();    
        try {
            while (!isSimulationComplete(simulation) && iteration < maxIterations) { 
                simulation.updateUsers(stateOfUsers, timeUsersInRooms);
                
                iteration++;
                currentIteration = iteration;
                simulatedTimeElapsed += timePerIteration;
            }
        } catch (Exception e) {
            System.err.println(" Error durante la simulación en iteración " + iteration + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        System.out.println("   ✅ Simulación completada:");
        System.out.println("      - Iteraciones totales: " + iteration);
        System.out.println("      - Tiempo simulado: " + String.format("%.1f", simulatedTimeElapsed) + " segundos");
        System.out.println("      - Tiempo real: " + String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.0) + " segundos\n");

        MainSimulator.userRunnable = null;
    }

    /**
     * Initializes users with basic locations if standard initialization fails.
     * Fallback method that assigns users to predefined room locations when
     * normal initialization encounters errors.
     * 
     * @param simulation the simulation containing users to initialize
     */
    private static void initializeUsersWithBasicLocations(Simulation simulation) {
        System.out.println("    Inicializando usuarios con ubicaciones básicas...");
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
     * Checks if simulated time has exceeded configured duration threshold.
     * Returns true when simulation should terminate.
     * 
     * @param simulation the simulation to check for completion
     * @return true if simulation time exceeded threshold, false otherwise
     */
    private static boolean isSimulationComplete(Simulation simulation) {
        List<User> users = simulation.getAllUsers();
        
        if (users.isEmpty()) {
            return true;
        }
        
        int maxSimulatedTimeSeconds = getMaxSimulatedTime();
        int elapsedSimulatedTime = getElapsedSimulatedTime(simulation);
        
        boolean timeExceeded = elapsedSimulatedTime >= maxSimulatedTimeSeconds;
        
        if (timeExceeded) {
            System.out.println("    Criterio de parada: Tiempo simulado completado (" + 
                            elapsedSimulatedTime + "/" + maxSimulatedTimeSeconds + " segundos)");
        }
        
        return timeExceeded;
    }

    /**
     * Gets the maximum simulated time threshold in seconds.
     * Retrieves the configured simulation duration from EpidemicConfiguration.
     * 
     * @return maximum simulation duration in seconds
     */
    private static int getMaxSimulatedTime() {
        return EpidemicConfiguration.getInstance().getSimulationDurationSeconds();
    }

    /**
     * Gets the elapsed simulated time in seconds.
     * Calculates time by multiplying current iteration count by seconds per iteration.
     * 
     * @param simulation the simulation to calculate elapsed time for
     * @return elapsed simulated time in seconds
     */
    private static int getElapsedSimulatedTime(Simulation simulation) {
        int totalIterations = getCurrentIteration();
        double secondsPerIteration = simulation.getTimeForIterationInSecond();
        
        return (int) (totalIterations * secondsPerIteration);
    }

    /**
     * Gets the current iteration count.
     * 
     * @return current iteration number
     */
    private static int getCurrentIteration() {
        return currentIteration;
    }
        
    /**
     * Collects simulation results into a structured format.
     * Extracts health state counts, epidemic statistics (contacts, aerosol concentration,
     * individual risk), and calculates attack rate and infection rate metrics.
     * 
     * @param simulation the simulation to collect results from
     * @param scenario the test scenario (may be null)
     * @param model the epidemic model used in the simulation
     * @return SimulationResult containing all collected metrics
     */
    private static SimulationResult collectSimulationResults(Simulation simulation, Scenarios.TestScenario scenario, String model) {
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
                    // case EXPOSED:
                    //     result.exposed++;
                    //     break;
                    case INFECTIOUS_SYMPTOMATIC:
                        result.infectiousSymp++;
                        break;
                    // case INFECTIOUS_ASYMPTOMATIC:
                    //     result.infectiousAsymp++;
                    //     break;
                    case SUPER_SPREADER:
                        result.superSpreaders++;
                        break;
                }
            }
        }
        
        try {
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            result.totalContacts = stats.getTotalContacts();
            result.infectiousContacts = stats.getInfectiousContacts();
            
            double concentration = stats.getAverageAerosolConcentration();
            result.averageConcentration = Double.isNaN(concentration) ? 0.0 : concentration;

            System.out.println("\n Calculando riesgo individual promedio...");
            result.individualRisk = simulation.calculateAverageTheoreticalRiskForAllRooms();
            System.out.printf("✅ Riesgo individual calculado: %.2f%%\n\n", result.individualRisk);
            
        } catch (Exception e) {
            System.err.println(" Warning: No se pudieron obtener estadísticas epidémicas: " + e.getMessage());
            result.totalContacts = 0;
            result.infectiousContacts = 0;
            result.averageConcentration = 0.0;
            result.individualRisk = 0.0;
        }
        
        int initialInfected = EpidemicConfiguration.getInstance().getInitialInfectedUsers();
        int susceptiblesIniciales = result.totalUsers - initialInfected;
        int nuevosInfectados = (result.totalUsers - result.susceptible) - initialInfected;
        result.attackRate = susceptiblesIniciales > 0 ? (double) nuevosInfectados / susceptiblesIniciales : 0.0;
        result.infectionRate = (double) (result.infectiousSymp + result.infectiousAsymp + result.superSpreaders) / result.totalUsers;
        
        return result;
    }

    /**
     * Compares results between different models for the same configuration.
     * Generates formatted tables showing attack rates and model-specific metrics
     * (contacts for proximity model, concentration and risk for aerosol models).
     * Exports comparison results to timestamped CSV file.
     * 
     * @param results map of model names to simulation results
     * @param scenarioName name of the scenario/configuration being compared
     */
    private static void compareModelResults(java.util.Map<String, SimulationResult> results, String scenarioName) {
        File resultsDir = new File("./results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String sanitizedScenarioName = scenarioName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String csvFilename = String.format("./results/comparison_models_%s_%s.csv", sanitizedScenarioName, timestamp);
        
        try (FileWriter csvWriter = new FileWriter(csvFilename)) {
            csvWriter.append("MODELO;TASA_ATAQUE_PCT;INFECTIVOS_TOTAL;METRICA_1;METRICA_2;VALORACION_MEDIA;DISTANCIA_MEDIA;TIEMPO_EJECUCION_SEG\n");
            
            System.out.println(" COMPARACIÓN DETALLADA POR MODELO:");

            double globalAvgRating = 0.0;
            double globalAvgDistance = 0.0;
            if (Configuration.simulation != null && Configuration.simulation.userRatings != null) {
                double totalSum = 0.0;
                int totalCount = 0;
                for (Map.Entry<Integer, List<Float>> entry : Configuration.simulation.userRatings.entrySet()) {
                    List<Float> ratings = entry.getValue();
                    for (float r : ratings) totalSum += r;
                    totalCount += ratings.size();
                }
                globalAvgRating = totalCount > 0 ? totalSum / totalCount : 0.0;
            }
            if (Configuration.simulation != null) {
                // globalAvgDistance = Configuration.simulation.calculateGlobalAverageDistanceBetweenVisitedItems();
            }

            // 1. Lelieveld
            System.out.println("\n🔬 MODELO: AEROSOL_LELIEVELD");
            System.out.printf("%-20s %-12s %-12s %-28s %-12s %-15s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.", "TIEMPO (seg)");
            System.out.println("-".repeat(105));
            
            SimulationResult lelieveld = results.get("AEROSOL_LELIEVELD");
            if (lelieveld != null) {
                int totalInfectiousL = lelieveld.infectiousSymp + lelieveld.infectiousAsymp + lelieveld.superSpreaders;
                double attackRateL = lelieveld.attackRate * 100;
                double timeSec = lelieveld.executionTimeMs / 1000.0;
                
                System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f %-15.2f\n",
                    "AEROSOL_LELIEVELD",
                    attackRateL,
                    totalInfectiousL,
                    lelieveld.averageConcentration,
                    lelieveld.individualRisk,
                    timeSec
                );
                
                // Escribir en CSV con separador ";" y las nuevas columnas
                csvWriter.append(String.format("AEROSOL_LELIEVELD;%.2f;%d;%.6f;%.2f;%.2f;%.2f;%.2f\n",
                    attackRateL,
                    totalInfectiousL,
                    lelieveld.averageConcentration,
                    lelieveld.individualRisk,
                    globalAvgRating,
                    globalAvgDistance,
                    timeSec
                ));
            }

            // 2. Peng
            System.out.println("\n🔬 MODELO: AEROSOL_PENG");
            System.out.printf("%-20s %-12s %-12s %-22s %-12s %-15s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.", "TIEMPO (seg)");
            System.out.println("-".repeat(95));
            
            SimulationResult peng = results.get("AEROSOL_PENG");
            if (peng != null) {
                int totalInfectiousP = peng.infectiousSymp + peng.infectiousAsymp + peng.superSpreaders;
                double attackRateP = peng.attackRate * 100;
                double timeSec = peng.executionTimeMs / 1000.0;
                
                System.out.printf("%-20s %-12.2f %-12d %-22.6f %-12.2f %-15.2f\n",
                    "AEROSOL_PENG",
                    attackRateP,
                    totalInfectiousP,
                    peng.averageConcentration,
                    peng.individualRisk,
                    timeSec
                );
                
                csvWriter.append(String.format("AEROSOL_PENG;%.2f;%d;%.6f;%.2f;%.2f;%.2f;%.2f\n",
                    attackRateP,
                    totalInfectiousP,
                    peng.averageConcentration,
                    peng.individualRisk,
                    globalAvgRating,
                    globalAvgDistance,
                    timeSec
                ));
            }

            // 3. Simple Proximity (similar pattern)
            System.out.println("\n🔬 MODELO: SIMPLE_PROXIMITY");
            System.out.printf("%-20s %-12s %-12s %-12s %-12s %-15s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC", "TIEMPO (seg)");
            System.out.println("-".repeat(95));
            
            SimulationResult simple = results.get("SIMPLE_PROXIMITY");
            if (simple != null) {
                int totalInfectiousS = simple.infectiousSymp + simple.infectiousAsymp + simple.superSpreaders;
                double attackRateS = simple.attackRate * 100;
                double timeSec = simple.executionTimeMs / 1000.0;
                
                System.out.printf("%-20s %-12.2f %-12d %-12d %-12d %-15.2f\n",
                    "SIMPLE_PROXIMITY",
                    attackRateS,
                    totalInfectiousS,
                    simple.totalContacts,
                    simple.infectiousContacts,
                    timeSec
                );
                
                csvWriter.append(String.format("SIMPLE_PROXIMITY;%.2f;%d;%d;%d;%.2f;%.2f;%.2f\n",
                    attackRateS,
                    totalInfectiousS,
                    simple.totalContacts,
                    simple.infectiousContacts,
                    globalAvgRating,
                    globalAvgDistance,
                    timeSec
                ));
            }

            // ✅ CAPTURAR VALORACIONES Y DISTANCIAS
            // double globalAvgRating = 0.0;
            // double globalAvgDistance = 0.0;
            
            if (Configuration.simulation != null && Configuration.simulation.userRatings != null) {
                System.out.println("\n VALORACIONES DE USUARIOS:");
                double totalSum = 0.0;
                int totalCount = 0;
                
                for (Map.Entry<Integer, List<Float>> entry : Configuration.simulation.userRatings.entrySet()) {
                    int userId = entry.getKey();
                    List<Float> ratings = entry.getValue();
                    double sum = 0.0;
                    for (float r : ratings) sum += r;
                    double avg = ratings.isEmpty() ? 0.0 : sum / ratings.size();
                    System.out.printf("    Usuario %d: %.2f (%d valoraciones)\n", userId, avg, ratings.size());
                    totalSum += sum;
                    totalCount += ratings.size();
                }
                
                globalAvgRating = totalCount > 0 ? totalSum / totalCount : 0.0;
                System.out.printf("\n   ⭐ Valoración media global: %.2f (%d valoraciones en total)\n", globalAvgRating, totalCount);
            }

            if (Configuration.simulation != null) {
                // globalAvgDistance = Configuration.simulation.calculateGlobalAverageDistanceBetweenVisitedItems();
            }

            
            
            // ✅ ACTUALIZAR CSV CON VALORACIONES Y DISTANCIAS (añadir una fila resumen)
            csvWriter.append(String.format("\n# Métricas globales para configuración: %s\n", scenarioName));
            csvWriter.append(String.format("# Valoración media global: %.2f\n", globalAvgRating));
            csvWriter.append(String.format("# Distancia media entre items visitados: %.2f\n", globalAvgDistance));
            
            csvWriter.flush();
            System.out.println("\n✅ Comparación de modelos guardada en: " + csvFilename);
            
        } catch (IOException e) {
            System.err.println(" Error al escribir CSV de comparación de modelos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a single simulation using real contact trajectories.
     * Configures epidemic parameters, creates simulation with real contact data
     * from CSV files, executes to completion, and collects results.
     * Supports clique-based infection patterns from trajectory data.
     * 
     * @param numUsers number of users to simulate
     * @param configName name of the configuration being tested
     * @param epidemicModel epidemic model to use
     * @param scenarioName scenario directory name
     * @return SimulationResult containing infection metrics and statistics
     * @throws Exception if simulation creation or execution fails
     */
    private static SimulationResult runSimulationWithRealContacts(
            int numUsers, 
            String configName,
            String epidemicModel,
            String scenarioName) throws Exception {
        
        System.out.println("   Iniciando simulación con contactos reales...");
        System.out.println("      Configuración: " + configName);
        System.out.println("      Modelo epidémico: " + epidemicModel);
        
        // 1. Configurar parámetros epidémicos
        EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();

        // DEBUG
        System.out.println("    DEBUG PRE-SIMULACIÓN:");
        System.out.println("      Config actual: " + EpidemicConfiguration.getInstance().getConfigName());
        System.out.println("      Mascarillas: " + (EpidemicConfiguration.getInstance().getMaskComplianceRate() * 100) + "%");
        System.out.println("      Ventilación: " + EpidemicConfiguration.getInstance().getDefaultVentilationRate() + " h⁻¹");
        
        // ✅ CONFIGURAR EL MODELO EPIDÉMICO
        epidemicConfig.setSelectedModel(epidemicModel);
        
        // ✅ CONFIGURAR PARÁMETROS SEGÚN EL MODELO
        configureEpidemicParameters(epidemicConfig, null, epidemicModel);
        
        // ✅ OBTENER INFECTADOS DE LA CONFIGURACIÓN
        int initialInfected = epidemicConfig.getInitialInfectedUsers();
        
        System.out.println("   🔬 Parámetros de simulación:");
        System.out.println("      - Total usuarios: " + numUsers);
        System.out.println("      - Infectados iniciales: " + initialInfected); // ✅ VERIFICAR QUE SE IMPRIMA
        
        // 2. Crear simulación con flag de contactos reales activado
        // Simulation simulation = createSimulationForScenario(
        //     null,
        //     numUsers,
        //     initialInfected,
        //     scenarioName,
        //     true
        // );

        // Added by Nacho Palacio 2025-11-08
        Simulation simulation = createSimulationForScenario(
            null,
            numUsers,
            0, // La infección se hace por cliques internamente
            scenarioName,
            true // useRealContacts
        );

        if (simulation == null) {
            throw new Exception("No se pudo crear la simulación");
        }

        System.out.println("   ✅ Simulación creada con contactos reales (infección por cliques activada)");
        
        System.out.println("   ✅ Simulación creada con contactos reales");
        
        // 3. Ejecutar simulación
        System.out.println("   ⏳ Ejecutando simulación...");
        runSimulationToCompletion(simulation);
        
        // 4. Evaluar transmisiones finales
        if (simulation.getEpidemicManager() != null) {
            simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(
                simulation.getAllUsers()
            );
        }

        printContactStatistics(simulation);
        
        // 5. Recolectar resultados
        SimulationResult result = collectSimulationResults(simulation, null, epidemicModel);
        
        return result;
    }


    /**
     * Compares results between synthetic and real contact trajectories.
     * Executes multiple configurations using both synthetic trajectory generation
     * and real contact data from CSV files. Analyzes differences in infection
     * patterns and attack rates between the two approaches.
     */
    public static void compareApproaches() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("⚖️  COMPARACIÓN: TRAYECTORIAS SINTÉTICAS vs CONTACTOS REALES");
        System.out.println("=".repeat(100));
        
        List<EpidemicConfiguration> configurations = createTestConfigurations();
        
        Map<String, Map<String, Map<String, SimulationResult>>> allResults = new HashMap<>();
        
        for (String model : models) {
            System.out.println("\n" + "=".repeat(100));
            System.out.println("🔬 EJECUTANDO COMPARACIÓN PARA MODELO: " + model);
            System.out.println("=".repeat(100));
            
            Map<String, SimulationResult> syntheticResults = new HashMap<>();
            Map<String, SimulationResult> realResults = new HashMap<>();
            
            List<Scenarios.TestScenario> scenarios = Scenarios.getAllScenarios();
            int totalUsers = 100;
            
            for (int i = 0; i < configurations.size(); i++) {
                EpidemicConfiguration config = configurations.get(i);
                String configName = config.getConfigName();
                
                System.out.println("\n" + "━".repeat(60));
                System.out.println(" CONFIGURACIÓN " + (i+1) + "/" + configurations.size() + ": " + configName);
                System.out.println("━".repeat(60));
                
                try {
                    // ✅ LIMPIEZA ANTES DE TRAYECTORIAS SINTÉTICAS
                    cleanupBetweenSimulations();
                    applyConfiguration(config);

                    // if (Configuration.instance != null && 
                    //     Configuration.instance.useContactTrajectoriesCheckBox != null) {
                    //     Configuration.instance.useContactTrajectoriesCheckBox.setSelected(false);
                    //     System.out.println("    Desactivando modo contactos reales");
                    // }
                    
                    System.out.println("    Ejecutando con TRAYECTORIAS SINTÉTICAS (" + model + ")...");
                    Scenarios.TestScenario scenario = scenarios.get(0);
                    SimulationResult syntheticResult = runSingleSimulation(scenario, model, i+1);
                    
                    // ✅ CAPTURAR ESTADÍSTICAS
                    EpidemicStatistics stats = EpidemicStatistics.getInstance();
                    syntheticResult.averageConcentration = stats.getAverageAerosolConcentration();
                    syntheticResult.totalContacts = stats.getTotalContacts();
                    syntheticResult.infectiousContacts = stats.getInfectiousContacts();
                    
                    System.out.println("    DEBUG - Estadísticas capturadas para sintéticas:");
                    System.out.printf("      - Concentración: %.6f\n", syntheticResult.averageConcentration);
                    System.out.printf("      - Contactos: %d\n", syntheticResult.totalContacts);
                    System.out.printf("      - Infectivos: %d\n", 
                                    syntheticResult.infectiousSymp + syntheticResult.infectiousAsymp + syntheticResult.superSpreaders);
                    
                    syntheticResults.put(configName, syntheticResult);
                    
                    // ✅ LIMPIEZA ANTES DE CONTACTOS REALES
                    cleanupBetweenSimulations();
                    applyConfiguration(config);
                    
                    System.out.println("    Ejecutando con CONTACTOS REALES (" + model + ")...");
                    SimulationResult realResult = runSimulationWithRealContacts(
                        totalUsers, configName, model, "MoMA_Museum"
                    );
                    
                    // ✅ CAPTURAR ESTADÍSTICAS
                    stats = EpidemicStatistics.getInstance();
                    realResult.averageConcentration = stats.getAverageAerosolConcentration();
                    realResult.totalContacts = stats.getTotalContacts();
                    realResult.infectiousContacts = stats.getInfectiousContacts();
                    
                    System.out.println("    DEBUG - Estadísticas capturadas para reales:");
                    System.out.printf("      - Concentración: %.6f\n", realResult.averageConcentration);
                    System.out.printf("      - Contactos: %d\n", realResult.totalContacts);
                    System.out.printf("      - Infectivos: %d\n", 
                                    realResult.infectiousSymp + realResult.infectiousAsymp + realResult.superSpreaders);
                    
                    realResults.put(configName, realResult);

                    cleanupBetweenSimulations();
                    
                } catch (Exception e) {
                    System.err.println("    Error en configuración " + configName + ": " + e.getMessage());
                    e.printStackTrace();
                    cleanupBetweenSimulations();
                }
            }
            
            // Guardar resultados para este modelo
            Map<String, Map<String, SimulationResult>> modelResults = new HashMap<>();
            modelResults.put("synthetic", syntheticResults);
            modelResults.put("real", realResults);
            allResults.put(model, modelResults);
            
            // Imprimir comparación para este modelo
            printFinalComparison(syntheticResults, realResults, model);
        }
        
        // ✅ NUEVO: Comparación entre modelos
        printCrossModelComparison(allResults);
    }

    /**
     * Prints contact-specific statistics after simulation completion.
     * Displays clique assignment information, inter-clique coincidences,
     * and isolation metrics from the coincidence tracker.
     * Added by Nacho Palacio 2025-01-15
     * 
     * @param simulation the simulation to extract contact statistics from
     */
    private static void printContactStatistics(Simulation simulation) {
        if (simulation.coincidenceTracker == null) {
            return; // No es simulación con contactos
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println(" ESTADÍSTICAS DETALLADAS DE CONTACTOS REALES");
        System.out.println("=".repeat(100));
        
        if (simulation.cliqueUserMapping != null && !simulation.cliqueUserMapping.isEmpty()) {
            System.out.println("\n TASAS DE ATAQUE POR CLIQUE:");
            
            // ✅ NUEVO: Pasar 3 parámetros
            simulation.coincidenceTracker.printAttackRatesByClique(
                simulation.getAllUsers(),               // userList
                simulation.initialSusceptiblesByClique, // initialSusceptiblesByClique
                simulation.cliqueUserMapping            // ✅ AÑADIDO
            );
        }
        
        // 2️⃣ Reporte de coincidencias inter-clique
        System.out.println("\n COINCIDENCIAS INTER-CLIQUE:");
        simulation.coincidenceTracker.printIsolationMetrics();
        simulation.coincidenceTracker.printDetailedUserCoincidences();
        
        
        // 3️⃣ Métricas de aislamiento
        if (simulation.coincidenceTracker != null) {
            Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
            double isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
            
            System.out.println("\n VERIFICACIÓN DE CRITERIO DE ÉXITO:");
            System.out.printf("   Tasa de aislamiento: %.2f%%\n", isolationRate * 100);
            
            if (isolationRate >= 0.95) {
                System.out.println("   ✅ CRITERIO CUMPLIDO (≥95% aislamiento)");
            } else {
                System.out.println("    CRITERIO NO CUMPLIDO (<95% aislamiento)");
            }
        }
        
        System.out.println("\n" + "=".repeat(100) + "\n");
    }

    /**
     * Cleans up state between simulations to avoid contamination.
     * Resets epidemic statistics, configuration, and various static state
     * to ensure each simulation starts fresh.
     */
    private static void cleanupBetweenSimulations() {
        try {
            System.out.println("\n🧹 Limpiando estado entre simulaciones...");
            
            es.unizar.epidemic.statistics.EpidemicStatistics.getInstance().reset();
            System.out.println("   ✅ EpidemicStatistics reseteado");
            
            EpidemicConfiguration.getInstance().resetToDefaults();
            
            if (Configuration.simulation != null) {
                Configuration.simulation.epidemicManager = null;
                Configuration.simulation = null;
            }
            
            if (MainSimulator.userRunnable != null) {
                MainSimulator.userRunnable.running = false;
                MainSimulator.userRunnable = null;
            }
            
            ContactTrajectoryBuilder.resetMappings();
            
            System.gc();
            Thread.sleep(150);
            
            System.out.println("   ✅ Estado limpiado correctamente");
            
        } catch (Exception e) {
            System.err.println("   Warning! Error durante limpieza: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Prints comprehensive final comparison of all results.
     * Generates complete tables for synthetic and real trajectories,
     * statistical analysis, configuration rankings, and relative differences.
     * Added by Nacho Palacio 2025-10-10
     * 
     * @param syntheticResults map of configuration names to synthetic trajectory results
     * @param realResults map of configuration names to real contact trajectory results
     * @param model the epidemic model being analyzed
     */
    private static void printFinalComparison(
            Map<String, SimulationResult> syntheticResults,
            Map<String, SimulationResult> realResults,
            String model) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🏆 ANÁLISIS FINAL COMPLETO - TODAS LAS CONFIGURACIONES");
        System.out.println("=".repeat(100));
        
        // 1. TABLA COMPLETA DE TRAYECTORIAS SINTÉTICAS
        System.out.println("\n RESULTADOS CON TRAYECTORIAS SINTÉTICAS:");
        System.out.println("━".repeat(90));
        
        printResultsTable(syntheticResults, model, "SINTÉTICAS");
        
        // 2. TABLA COMPLETA DE CONTACTOS REALES
        System.out.println("\n RESULTADOS CON CONTACTOS REALES:");
        System.out.println("━".repeat(90));
        
        printResultsTable(realResults, model, "REALES");
        
        // 3. ANÁLISIS ESTADÍSTICO
        System.out.println("\n ANÁLISIS ESTADÍSTICO:");
        System.out.println("━".repeat(90));
        
        printStatisticalAnalysis(syntheticResults, realResults);
        
        // 4. RANKING DE CONFIGURACIONES
        System.out.println("\n🏆 RANKING DE CONFIGURACIONES:");
        System.out.println("━".repeat(90));
        
        printConfigurationRanking(syntheticResults, realResults);
        
        // 5. ANÁLISIS DE DIFERENCIAS
        System.out.println("\n ANÁLISIS DE DIFERENCIAS RELATIVAS:");
        System.out.println("━".repeat(90));
        
        printRelativeDifferences(syntheticResults, realResults);
    }

    /**
     * Prints a formatted table with simulation results.
     * Displays configuration names, attack rates, infectious counts,
     * and model-specific metrics in a structured table format.
     * Added by Nacho Palacio 2025-10-10
     * 
     * @param results map of configuration names to simulation results
     * @param model the epidemic model being displayed
     * @param tableTitle descriptive title for the table (e.g., "SINTÉTICAS", "REALES")
     */
    private static void printResultsTable(
            Map<String, SimulationResult> results,
            String model,
            String tableTitle) {
        
        if (model.equals("SIMPLE_PROXIMITY")) {
            System.out.printf("%-20s %-12s %-12s %-12s %-12s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC");
        } 
        else if (model.equals("AEROSOL_PENG")) {
            System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.");
        } 
        else if (model.equals("AEROSOL_LELIEVELD")) {
            System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.");
        }
        
        System.out.println("-".repeat(110));
        
        for (Map.Entry<String, SimulationResult> entry : results.entrySet()) {
            String configName = entry.getKey();
            SimulationResult result = entry.getValue();
            
            int totalInfected = result.infectiousSymp + result.infectiousAsymp + result.superSpreaders;
            
            if (model.equals("SIMPLE_PROXIMITY")) {
                System.out.printf("%-20s %-12.2f %-12d %-12d %-12d\n",
                    configName,
                    result.attackRate * 100,
                    totalInfected,
                    result.totalContacts,
                    result.infectiousContacts);
            } 
            else {
                System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f\n",
                    configName,
                    result.attackRate * 100,
                    totalInfected,
                    result.averageConcentration,
                    result.individualRisk);
            }
        }
        
        System.out.println("-".repeat(110));
    }

    /**
     * Prints statistical analysis comparing both approaches.
     * Calculates and displays average attack rates, ranges, and absolute
     * differences between synthetic and real contact trajectories.
     * Added by Nacho Palacio 2025-10-10
     * 
     * @param syntheticResults map of synthetic trajectory simulation results
     * @param realResults map of real contact trajectory simulation results
     */
    private static void printStatisticalAnalysis(
            Map<String, SimulationResult> syntheticResults,
            Map<String, SimulationResult> realResults) {
        
        List<Double> syntheticAttackRates = new ArrayList<>();
        List<Double> realAttackRates = new ArrayList<>();
        List<Double> differences = new ArrayList<>();
        
        for (String configName : syntheticResults.keySet()) {
            if (realResults.containsKey(configName)) {
                double syntheticRate = syntheticResults.get(configName).attackRate * 100;
                double realRate = realResults.get(configName).attackRate * 100;
                double diff = realRate - syntheticRate;
                
                syntheticAttackRates.add(syntheticRate);
                realAttackRates.add(realRate);
                differences.add(diff);
            }
        }
        
        // Calcular estadísticas
        double avgSynthetic = syntheticAttackRates.stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgReal = realAttackRates.stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgDiff = differences.stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double maxSynthetic = syntheticAttackRates.stream()
            .mapToDouble(Double::doubleValue).max().orElse(0.0);
        double maxReal = realAttackRates.stream()
            .mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        double minSynthetic = syntheticAttackRates.stream()
            .mapToDouble(Double::doubleValue).min().orElse(0.0);
        double minReal = realAttackRates.stream()
            .mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        System.out.println("\n TASA DE ATAQUE PROMEDIO:");
        System.out.printf("   Sintéticas: %.2f%%\n", avgSynthetic);
        System.out.printf("   Reales:     %.2f%%\n", avgReal);
        System.out.printf("   Diferencia: %.2f%% %s\n\n", 
                        Math.abs(avgDiff), 
                        avgDiff > 0 ? "(reales > sintéticas)" : "(sintéticas > reales)");
        
        System.out.println(" RANGO DE TASAS DE ATAQUE:");
        System.out.printf("   Sintéticas: %.2f%% - %.2f%%\n", minSynthetic, maxSynthetic);
        System.out.printf("   Reales:     %.2f%% - %.2f%%\n", minReal, maxReal);
        
        System.out.println("\n DIFERENCIA PROMEDIO ABSOLUTA:");
        double avgAbsDiff = differences.stream()
            .mapToDouble(Math::abs).average().orElse(0.0);
        System.out.printf("   %.2f puntos porcentuales\n", avgAbsDiff);
    }

    /**
     * Prints ranking of configurations by effectiveness.
     * Sorts configurations by attack rate (lower is more effective) and
     * displays top 5 most and least effective configurations for both approaches.
     * Added by Nacho Palacio 2025-10-10
     * 
     * @param syntheticResults map of synthetic trajectory simulation results
     * @param realResults map of real contact trajectory simulation results
     */
    private static void printConfigurationRanking(
            Map<String, SimulationResult> syntheticResults,
            Map<String, SimulationResult> realResults) {
        
        // Crear lista de configuraciones con sus tasas de ataque
        List<ConfigRanking> syntheticRanking = new ArrayList<>();
        List<ConfigRanking> realRanking = new ArrayList<>();
        
        for (Map.Entry<String, SimulationResult> entry : syntheticResults.entrySet()) {
            String configName = entry.getKey();
            double attackRate = entry.getValue().attackRate * 100;
            syntheticRanking.add(new ConfigRanking(configName, attackRate));
        }
        
        for (Map.Entry<String, SimulationResult> entry : realResults.entrySet()) {
            String configName = entry.getKey();
            double attackRate = entry.getValue().attackRate * 100;
            realRanking.add(new ConfigRanking(configName, attackRate));
        }
        
        // Ordenar de menor a mayor tasa de ataque (más efectivo primero)
        syntheticRanking.sort((a, b) -> Double.compare(a.attackRate, b.attackRate));
        realRanking.sort((a, b) -> Double.compare(a.attackRate, b.attackRate));
        
        System.out.println("\n🥇 TOP 5 MÁS EFECTIVAS (TRAYECTORIAS SINTÉTICAS):");
        for (int i = 0; i < Math.min(5, syntheticRanking.size()); i++) {
            ConfigRanking rank = syntheticRanking.get(i);
            System.out.printf("   %d. %-30s %.2f%%\n", i+1, rank.configName, rank.attackRate);
        }
        
        System.out.println("\n🥇 TOP 5 MÁS EFECTIVAS (CONTACTOS REALES):");
        for (int i = 0; i < Math.min(5, realRanking.size()); i++) {
            ConfigRanking rank = realRanking.get(i);
            System.out.printf("   %d. %-30s %.2f%%\n", i+1, rank.configName, rank.attackRate);
        }
        
        System.out.println("\n🔴 TOP 5 MENOS EFECTIVAS (TRAYECTORIAS SINTÉTICAS):");
        for (int i = syntheticRanking.size() - 1; i >= Math.max(0, syntheticRanking.size() - 5); i--) {
            ConfigRanking rank = syntheticRanking.get(i);
            System.out.printf("   %d. %-30s %.2f%%\n", 
                            syntheticRanking.size() - i, rank.configName, rank.attackRate);
        }
        
        System.out.println("\n🔴 TOP 5 MENOS EFECTIVAS (CONTACTOS REALES):");
        for (int i = realRanking.size() - 1; i >= Math.max(0, realRanking.size() - 5); i--) {
            ConfigRanking rank = realRanking.get(i);
            System.out.printf("   %d. %-30s %.2f%%\n", 
                            realRanking.size() - i, rank.configName, rank.attackRate);
        }
    }

    /**
     * Prints relative differences between approaches.
     * Compares attack rates between synthetic and real trajectories,
     * showing absolute and relative percentage differences for each configuration.
     * Added by Nacho Palacio 2025-10-10
     * 
     * @param syntheticResults map of synthetic trajectory simulation results
     * @param realResults map of real contact trajectory simulation results
     */
    private static void printRelativeDifferences(
            Map<String, SimulationResult> syntheticResults,
            Map<String, SimulationResult> realResults) {
        
        System.out.printf("%-30s %-15s %-15s %-15s %-15s\n",
            "CONFIGURACIÓN", "SINT (%)", "REAL (%)", "DIFF ABS", "DIFF REL (%)");
        System.out.println("-".repeat(95));
        
        for (String configName : syntheticResults.keySet()) {
            if (realResults.containsKey(configName)) {
                double syntheticRate = syntheticResults.get(configName).attackRate * 100;
                double realRate = realResults.get(configName).attackRate * 100;
                double absDiff = realRate - syntheticRate;
                
                // Diferencia relativa = (diferencia / sintético) * 100
                double relDiff = syntheticRate != 0 ? (absDiff / syntheticRate) * 100 : 0;
                
                System.out.printf("%-30s %-15.2f %-15.2f %-15.2f %-15.2f\n",
                    configName, syntheticRate, realRate, absDiff, relDiff);
            }
        }
        
        System.out.println("-".repeat(95));
        
        System.out.println("\n💡 INTERPRETACIÓN DE DIFERENCIA RELATIVA:");
        System.out.println("   - Valor positivo: Contactos reales generan más infecciones");
        System.out.println("   - Valor negativo: Trayectorias sintéticas generan más infecciones");
        System.out.println("   - % relativo indica cuánto varía respecto al valor sintético");
    }

    /**
     * Compares results between different epidemic models.
     * Displays side-by-side comparison of synthetic vs real trajectories
     * for each model, showing attack rates and model-specific metrics.
     * 
     * @param allResults triple-nested map: model -> approach (synthetic/real) -> config -> result
     */
    private static void printCrossModelComparison(
            Map<String, Map<String, Map<String, SimulationResult>>> allResults) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🔬 COMPARACIÓN ENTRE MODELOS EPIDÉMICOS");
        System.out.println("=".repeat(100));
        
        // Iterar sobre cada modelo
        for (String model : models) {
            System.out.println("\n🔬 ANÁLISIS PARA MODELO: " + model);
            System.out.println("━".repeat(100));
            
            // Obtener resultados para este modelo
            Map<String, Map<String, SimulationResult>> modelResults = allResults.get(model);
            
            if (modelResults == null) {
                System.err.println("    No hay resultados para modelo " + model);
                continue;
            }
            
            Map<String, SimulationResult> syntheticResults = modelResults.get("synthetic");
            Map<String, SimulationResult> realResults = modelResults.get("real");
            
            if (syntheticResults == null || realResults == null) {
                System.err.println("    Faltan resultados sintéticos o reales para " + model);
                continue;
            }
            
            // ============================================================
            // 1️⃣ TABLA: TRAYECTORIAS SINTÉTICAS
            // ============================================================
            System.out.println("\n TRAYECTORIAS SINTÉTICAS:");
            System.out.println("━".repeat(100));
            
            // Imprimir cabecera según el modelo
            printModelSpecificHeaderForComparison(model);
            System.out.println("-".repeat(100));
            
            // Imprimir filas de resultados sintéticos
            for (Map.Entry<String, SimulationResult> entry : syntheticResults.entrySet()) {
                String configName = entry.getKey();
                SimulationResult result = entry.getValue();
                printModelSpecificRowForComparison(configName, result, model);
            }
            
            System.out.println("-".repeat(100));
            
            // ============================================================
            // 2️⃣ TABLA: CONTACTOS REALES
            // ============================================================
            System.out.println("\n CONTACTOS REALES:");
            System.out.println("━".repeat(100));
            
            // Imprimir cabecera según el modelo
            printModelSpecificHeaderForComparison(model);
            System.out.println("-".repeat(100));
            
            // Imprimir filas de resultados reales
            for (Map.Entry<String, SimulationResult> entry : realResults.entrySet()) {
                String configName = entry.getKey();
                SimulationResult result = entry.getValue();
                printModelSpecificRowForComparison(configName, result, model);
            }
            
            System.out.println("-".repeat(100));
            
            // ============================================================
            // 3️⃣ ANÁLISIS ESTADÍSTICO PARA ESTE MODELO
            // ============================================================
            System.out.println("\n ANÁLISIS ESTADÍSTICO PARA " + model + ":");
            System.out.println("━".repeat(100));
            
            double avgSynthetic = calculateAverageAttackRate(syntheticResults);
            double avgReal = calculateAverageAttackRate(realResults);
            
            System.out.println("\n TASA DE ATAQUE PROMEDIO:");
            System.out.printf("   Sintéticas: %.2f%%\n", avgSynthetic);
            System.out.printf("   Reales:     %.2f%%\n", avgReal);
            System.out.printf("   Diferencia: %.2f%%\n", Math.abs(avgReal - avgSynthetic));
            
            // ============================================================
            // 4️⃣ COMPARACIÓN DIRECTA (opcional)
            // ============================================================
            System.out.println("\n COMPARACIÓN SINTÉTICAS vs REALES:");
            System.out.println("━".repeat(100));
            System.out.printf("%-25s %-15s %-15s %-15s\n",
                "CONFIGURACIÓN", "SINTÉTICAS (%)", "REALES (%)", "DIFERENCIA");
            System.out.println("-".repeat(100));
            
            for (String configName : syntheticResults.keySet()) {
                if (realResults.containsKey(configName)) {
                    double syntheticAttack = syntheticResults.get(configName).attackRate * 100;
                    double realAttack = realResults.get(configName).attackRate * 100;
                    double diff = realAttack - syntheticAttack;
                    
                    System.out.printf("%-25s %-15.2f %-15.2f %+15.2f\n",
                        configName, syntheticAttack, realAttack, diff);
                }
            }
            
            System.out.println("-".repeat(100));
        }
        
        System.out.println("\n" + "=".repeat(100));
    }

    /**
     * Prints model-specific header for cross-model comparison.
     * Displays appropriate column headers based on the model type
     * (contacts for proximity, concentration and risk for aerosol models).
     * Added by Nacho Palacio 2025-10-11
     * 
     * @param model the epidemic model being displayed
     */
    private static void printModelSpecificHeaderForComparison(String model) {
        if (model.equals("SIMPLE_PROXIMITY")) {
            System.out.printf("%-20s %-12s %-12s %-12s %-12s\n", 
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC");
        } 
        else if (model.equals("AEROSOL_PENG")) {
            System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.");
        } 
        else if (model.equals("AEROSOL_LELIEVELD")) {
            System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.");
        }
    }

    /**
     * Prints model-specific row for cross-model comparison.
     * Displays configuration results with metrics appropriate to the model type.
     * Added by Nacho Palacio 2025-10-11
     * 
     * @param configName name of the configuration
     * @param result simulation result data
     * @param model the epidemic model being displayed
     */
    private static void printModelSpecificRowForComparison(String configName, SimulationResult result, String model) {
        double attackRate = result.attackRate * 100;
        int totalInfected = result.infectiousSymp + result.infectiousAsymp + result.superSpreaders;
        
        if (model.equals("SIMPLE_PROXIMITY")) {
            System.out.printf("%-20s %-12.2f %-12d %-12d %-12d\n",
                configName,
                attackRate,
                totalInfected,
                result.totalContacts,
                result.infectiousContacts);
        } 
        else if (model.equals("AEROSOL_PENG")) {
            System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f\n",
                configName,
                attackRate,
                totalInfected,
                result.averageConcentration,
                result.individualRisk);
        } 
        else if (model.equals("AEROSOL_LELIEVELD")) {
            System.out.printf("%-20s %-12.2f %-12d %-28.2f %-12.2f\n",
                configName,
                attackRate,
                totalInfected,
                result.averageConcentration,
                result.individualRisk);
        }
    }

    /**
     * Calculates average attack rate from a map of results.
     * 
     * @param results map of configuration names to simulation results
     * @return average attack rate across all configurations, or 0.0 if map is empty
     */
    private static double calculateAverageAttackRate(Map<String, SimulationResult> results) {
        if (results == null || results.isEmpty()) return 0.0;
        
        double sum = 0.0;
        for (SimulationResult result : results.values()) {
            sum += result.attackRate * 100;
        }
        
        return sum / results.size();
    }

    /**
     * Auxiliary class for ranking configurations
     */
    private static class ConfigRanking {
        String configName;
        double attackRate;
        
        ConfigRanking(String name, double rate) {
            this.configName = name;
            this.attackRate = rate;
        }
    }



    /**
     * Data class to hold simulation results
     */
    static class SimulationResult {
        String scenarioName;
        String modelUsed;
        int totalUsers;
        int susceptible;
        // int exposed;
        int infectiousSymp;
        int infectiousAsymp;
        int superSpreaders;
        int recovered;
        double attackRate;
        double infectionRate;
        int totalContacts;
        int infectiousContacts;
        double averageConcentration;
        double individualRisk;
        long executionTimeMs;
        
        public SimulationResult() {
            totalUsers = 0;
            susceptible = 0;
            // exposed = 0;
            infectiousSymp = 0;
            infectiousAsymp = 0;
            superSpreaders = 0;
            recovered = 0;
            attackRate = 0.0;
            infectionRate = 0.0;
            totalContacts = 0;
            infectiousContacts = 0;
            averageConcentration = 0.0;
            individualRisk = 0.0;
            executionTimeMs = 0L;
        }
    }
}