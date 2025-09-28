package es.unizar.epidemic.tests;

import es.unizar.epidemic.EpidemicConfiguration;
import es.unizar.epidemic.UserEpidemicExtension;
import es.unizar.epidemic.models.LelieveldParameters;
import es.unizar.epidemic.models.PengParameters;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.statistics.EpidemicRiskCalculator;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validador que ejecuta simulaciones completas y analiza resultados epidémicos reales
 * @author Nacho Palacio
 * @date 2025-09-14
 */
public class SimulationEpidemicValidator {
    static String[] models = {"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"};

    private static int currentIteration = 0;
    
    public static void main(String[] args) {
        System.out.println("🧪 === VALIDACIÓN CON SIMULACIONES COMPLETAS ===");
        
        runValidationScenarios();
    }
    
    /**
     * Runs all validation scenarios
     */
    private static void runValidationScenarios() {
        List<Scenarios.TestScenario> scenarios = Scenarios.getAllScenarios();
        
        if (scenarios.isEmpty()) {
            System.err.println("❌ No hay escenarios disponibles");
            return;
        }
        
        Scenarios.TestScenario scenario = scenarios.get(0);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 SIMULANDO ESCENARIO: " + scenario.name);
        System.out.println("📝 " + scenario.description);
        System.out.println("=".repeat(80));
        
        // testScenarioWithSingleModel(scenario, 1, "SIMPLE_PROXIMITY");
        // testScenarioWithAllModels(scenario, 1);
        runMultipleConfigurations(scenario);
    }

    /**
     * Runs multiple epidemic configurations for a given scenario
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
            
            System.out.println("\n📊 COMPARACIÓN PARA: " + configName);
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
     * Creates a list of diverse epidemic configurations for testing
     */
    private static List<EpidemicConfiguration> createTestConfigurations() {
        List<EpidemicConfiguration> configurations = new ArrayList<>();
        int simulationDurationSeconds = 10800; // 3 horas
        
        // *** 1. CONFIGURACIÓN BASELINE (Control) ***
        EpidemicConfiguration baseline = EpidemicConfiguration.getInstance().clone();
        baseline.setConfigName("Baseline_Control");
        baseline.setMaskComplianceRate(0.1);
        baseline.setDefaultVentilationRate(3.0);
        baseline.setVirusDecayRate(0.62);
        baseline.setInitialInfectedUsers(5);
        baseline.setSuperSpreaderProbability(0.0);
        baseline.setSimulationDurationSeconds(simulationDurationSeconds);
        configurations.add(baseline);
        
        // *** 2. ALTA VENTILACIÓN ***
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
        immune.setImmunePopulationFraction(0.6);  // 60% inmunes (vacunados)
        configurations.add(immune);
        
        // *** 5. SUPERSPREADER EVENT ***
        EpidemicConfiguration superEvent = baseline.clone();
        superEvent.setConfigName("SuperSpreader_Event");
        superEvent.setInitialInfectedUsers(15);  // 15% infectados inicialmente
        superEvent.setSuperSpreaderProbability(0.8);  // 15% superspreaders
        superEvent.setMaskComplianceRate(0.0);  // Sin mascarillas
        configurations.add(superEvent);
        
        // *** 6. VENTILACIÓN POBRE ***
        EpidemicConfiguration poorVent = baseline.clone();
        poorVent.setConfigName("Poor_Ventilation");
        poorVent.setDefaultVentilationRate(0.8);  // Ventilación muy pobre
        configurations.add(poorVent);
        
        // *** 7. VIRUS RESISTENTE ***
        EpidemicConfiguration resistantVirus = baseline.clone();
        resistantVirus.setConfigName("Resistant_Virus");
        resistantVirus.setVirusDecayRate(0.15);  // Virus sobrevive más tiempo
        resistantVirus.setInitialInfectedUsers(8);
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
        
        return configurations;
    }

    /**
     * Compares results between different models for the same configuration
     */
    private static void compareAllConfigurations(java.util.Map<String, java.util.Map<String, SimulationResult>> allResults) {
        for (String model : models) {
            System.out.println("\n🔬 ANÁLISIS PARA MODELO: " + model);
            System.out.println("━".repeat(90));
            
            if (model.equals("SIMPLE_PROXIMITY")) {
                System.out.printf("%-20s %-12s %-12s %-12s %-12s\n", 
                    "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC");
            } 
            else if (model.equals("AEROSOL_PENG")) {
                System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                    "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.");
            } else if (model.equals("AEROSOL_LELIEVELD")) {
                System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
                    "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.");

            }
            System.out.println("-".repeat(110));
            
            List<ConfigResult> results = new ArrayList<>();
            
            for (Map.Entry<String, Map<String, SimulationResult>> configEntry : allResults.entrySet()) {
                String configName = configEntry.getKey();
                SimulationResult result = configEntry.getValue().get(model);
                if (result != null) {
                    // System.out.printf("%-20s %-11.2f%% %-12d %-12d %-12d %-12.6f %-11.2f%%\n",
                    //                 configName.length() > 18 ? configName.substring(0, 18) : configName,
                    //                 result.attackRate * 100,
                    //                 result.infectiousSymp + result.infectiousAsymp + result.superSpreaders,
                    //                 result.totalContacts,
                    //                 result.infectiousContacts,
                    //                 result.averageConcentration,
                    //                 result.individualRisk);

                    if (model.equals("SIMPLE_PROXIMITY")) {
                        System.out.printf("%-20s %-12.2f %-12d %-12d %-12d\n",
                            configName,
                            result.attackRate * 100,
                            result.infectiousSymp + result.infectiousAsymp + result.superSpreaders,
                            result.totalContacts,
                            result.infectiousContacts
                        );
                    } 
                    else {
                        System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f\n",
                            configName,
                            result.attackRate * 100,
                            result.infectiousSymp + result.infectiousAsymp + result.superSpreaders,
                            result.averageConcentration,
                            result.individualRisk
                        );
                    }

                    
                    results.add(new ConfigResult(configName, result.attackRate));
                }
            }
            
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
        
       // analyzeInterventionEffectiveness(allResults);
    }

    /**
     * Analyzes effectiveness of specific interventions compared to baseline
     */
    private static void analyzeInterventionEffectiveness(java.util.Map<String, java.util.Map<String, SimulationResult>> allResults) {
        System.out.println("\n📊 ANÁLISIS DE EFECTIVIDAD DE INTERVENCIONES");
        System.out.println("━".repeat(80));
        
        // Obtener baseline para comparar
        Map<String, SimulationResult> baseline = allResults.get("Baseline_Control");
        if (baseline == null) return;
        
        String[] interventions = {
            "High_Ventilation", "Mandatory_Masks", "High_Immunity", 
            "Combined_Measures", "Best_Case"
        };
        
        String[] interventionNames = {
            "Alta Ventilación", "Mascarillas Obligatorias", "Alta Inmunidad",
            "Medidas Combinadas", "Mejor Caso"
        };
        
        for (String model : new String[]{"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"}) {
            System.out.println("\n🔬 Efectividad para " + model + ":");
            
            double baselineAttack = baseline.get(model).attackRate;
            
            for (int i = 0; i < interventions.length; i++) {
                Map<String, SimulationResult> intervention = allResults.get(interventions[i]);
                if (intervention != null && intervention.get(model) != null) {
                    double interventionAttack = intervention.get(model).attackRate;
                    double reduction = ((baselineAttack - interventionAttack) / baselineAttack) * 100;
                    
                    System.out.printf("   %-25s: %.1f%% reducción (%.2f%% → %.2f%%)\n",
                                    interventionNames[i], reduction, 
                                    baselineAttack * 100, interventionAttack * 100);
                }
            }
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

    /**
     * Applies a given epidemic configuration
     */
    private static void applyConfiguration(EpidemicConfiguration config) {
        EpidemicConfiguration.setInstance(config);
        
        EpidemicConfiguration currentConfig = EpidemicConfiguration.getInstance();

        if (config != currentConfig) {
            System.out.println("   WARNING: config != currentConfig - Problema de instancia!");
        }
        
        if (Math.abs(config.getMaskComplianceRate() - currentConfig.getMaskComplianceRate()) > 0.01) {
            System.out.println("   🔧 FORZANDO configuración de mascarillas...");
            currentConfig.setMaskComplianceRate(config.getMaskComplianceRate());
        }
        
        if (Math.abs(config.getDefaultVentilationRate() - currentConfig.getDefaultVentilationRate()) > 0.1) {
            System.out.println("   🔧 FORZANDO configuración de ventilación...");
            currentConfig.setDefaultVentilationRate(config.getDefaultVentilationRate());
        }
    }

    /**
     * Runs all epidemic models for a given configuration
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
                System.err.println("   ❌ Error en modelo " + model + ": " + e.getMessage());
                configResults.put(model, new SimulationResult()); // Resultado vacío
            }
        }
        
        return configResults;
    }
    
    /**
     * Runs a single simulation
     */
    private static SimulationResult runSingleSimulation(Scenarios.TestScenario scenario, String model, int scenarioNumber) {
        try {
            EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
            configureEpidemicParameters(epidemicConfig, scenario, model);
            
            int totalUsers = scenario.standardVisitorCount;
            int infectiousUsers = 5;// Math.max(1, (int) Math.round(totalUsers * scenario.infectiousProbability));
            
            Simulation simulation = createSimulationForScenario(scenario, totalUsers, infectiousUsers);
            
            if (simulation == null) {
                System.err.println("❌ Error: No se pudo crear la simulación");
                return new SimulationResult();
            }
            
            System.out.println("   ⏳ Ejecutando simulación...");
            runSimulationToCompletion(simulation);

            if (simulation.getEpidemicManager() != null) {
                simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(simulation.getAllUsers());
            }

            double avgRisk = simulation.calculateAverageTheoreticalRiskForAllRooms();
 
            return collectSimulationResults(simulation, scenario, model);
            
        } catch (Exception e) {
            System.err.println("❌ Error en simulación: " + e.getMessage());
            e.printStackTrace();
            return new SimulationResult();
        }
    }
    
    /**
     * Configures epidemic parameters based on the scenario and model
     */
    private static void configureEpidemicParameters(EpidemicConfiguration config, Scenarios.TestScenario scenario, String model) {
        config.setSelectedModel(model);
        
        String configName = config.getConfigName();
        boolean isBaseline = configName == null || configName.equals("Default") || configName.equals("Baseline_Control");
        
        if (isBaseline) {
            config.setDefaultVentilationRate(scenario.ventilationRate);
            config.setVirusDecayRate(scenario.virusDecayRate);
            config.setMaskComplianceRate(scenario.maskFraction);
            
            System.out.println("   📋 Aplicando valores del escenario para " + configName);
        } else {
            System.out.println("   🔒 Manteniendo valores específicos para " + configName);
            System.out.println("      - Mascarillas: " + (config.getMaskComplianceRate() * 100) + "%");
            System.out.println("      - Ventilación: " + config.getDefaultVentilationRate() + " h⁻¹");
        }
        
        switch (model) {
            case "SIMPLE_PROXIMITY":
                config.setMaxTransmissionDistance(1.5);
                config.setBaseTransmissionProbability(0.01);
                config.setMinContactDuration(200);
                break;
            case "AEROSOL_PENG":
                config.setQuantaEmissionRate(232.5);
                config.setBreathingRate(0.72);
                config.setDepositionRate(0.3);
                break;
            case "AEROSOL_LELIEVELD":
                config.setViralLoadHigh(1.5E7);
                config.setViralLoadSuper(5E9);
                config.setInfectiousDose(316);
                config.setDepositionProbability(0.5);
                break;
        }
        
        System.out.println("   ✅ Configuración epidémica aplicada para " + model);
        System.out.println("   🎭 Mascarillas finales: " + (config.getMaskComplianceRate() * 100) + "%");
        System.out.println("   🌬️  Ventilación final: " + config.getDefaultVentilationRate() + " h⁻¹");
    }
    
    /**
     * Creates a simulation instance for the given scenario using SIMPLE_SCENARIO files
     */
    private static Simulation createSimulationForScenario(Scenarios.TestScenario scenario, int totalUsers, int infectiousUsers) {
        System.out.println("   🗺️ Cargando SIMPLE_SCENARIO desde archivos específicos...");
        
        java.io.File mapDir = new java.io.File("./dist/resources/maps/SIMPLE_SCENARIO/");
        if (!mapDir.exists()) {
            mapDir = new java.io.File("./resources/maps/SIMPLE_SCENARIO/");
        }
        
        if (!mapDir.exists()) {
            System.err.println("❌ Error: No se encuentra el directorio SIMPLE_SCENARIO");
            return null;
        }
        
        java.io.File roomFile = new java.io.File(mapDir, "room_floor_combined.txt");
        java.io.File itemFile = new java.io.File(mapDir, "item_floor_combined.txt");
        java.io.File graphFile = new java.io.File(mapDir, "graph_floor_combined.txt");
        
        int nonSpecialUsers = 98; 
        String pathsFileName = "rand_non_special_user_paths_" + (nonSpecialUsers + 1) + ".txt";
        java.io.File pathsFile = new java.io.File(mapDir, pathsFileName);
        
        if (!roomFile.exists() || !itemFile.exists() || !graphFile.exists() || !pathsFile.exists()) {
            System.err.println("❌ Error: Faltan archivos del SIMPLE_SCENARIO");
            System.err.println("   - Room file: " + roomFile.exists());
            System.err.println("   - Item file: " + itemFile.exists());
            System.err.println("   - Graph file: " + graphFile.exists());
            System.err.println("   - Paths file: " + pathsFile.exists() + " (" + pathsFile.getAbsolutePath() + ")");
            return null;
        }
        
        System.out.println("   📁 Usando archivo de paths existente: " + pathsFileName);
        
        String originalRoomPath = es.unizar.util.Literals.ROOM_FLOOR_COMBINED;
        String originalItemPath = es.unizar.util.Literals.ITEM_FLOOR_COMBINED;
        String originalGraphPath = es.unizar.util.Literals.GRAPH_FLOOR_COMBINED;
        
        es.unizar.util.Literals.ROOM_FLOOR_COMBINED = roomFile.getAbsolutePath();
        es.unizar.util.Literals.ITEM_FLOOR_COMBINED = itemFile.getAbsolutePath();
        es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = graphFile.getAbsolutePath();
        
        try {
            int timeAvailable = 1;
            int delayObserving = 10;
            double timeIteration = 1.0;
            double screenRefresh = 1.0;
            double timeForPaths = 1.0;
            double userVelocity = 5.0;
            double kmToPixel = 6597;
            int ttl = 180;
            int timeOnStairs = 30;
            int minTimeUpdate = 15;
            int commRange = 250;
            int maxKnowledge = 1;
            int commBandwidth = 54;
            int latency = 1;
            int timeChangeMood = 1800;
            
            int specialUsers = 1; 
            
            System.out.println("   👥 Creando simulación con " + nonSpecialUsers + " usuarios no especiales + " + specialUsers + " especial");
            System.out.println("   📁 Archivo de paths: " + pathsFile.getAbsolutePath());
            
            Simulation simulation = new Simulation(
                timeAvailable, delayObserving, timeIteration, screenRefresh, timeForPaths,
                userVelocity, kmToPixel, ttl, timeOnStairs, minTimeUpdate, commRange,
                maxKnowledge, commBandwidth, latency, specialUsers, nonSpecialUsers,
                pathsFile.getAbsolutePath(), "Random Path", "Random", 2.5f, 10,
                "Opportunistic", 0.4, 40, 0.5, "P2P", timeChangeMood,
                true, System.currentTimeMillis(), false
            );
            
            Configuration.simulation = simulation;
            
            System.out.println("   🏗️ Inicializando floor panel...");
            if (MainSimulator.floorPanelCombined == null) {
                MainSimulator.floorPanelCombined = new es.unizar.gui.FloorPanelCombined(
                    MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
            }
            
            System.out.println("   📂 Cargando SIMPLE_SCENARIO...");
            
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
            
            System.out.println("   ✅ SIMPLE_SCENARIO cargado correctamente");
            System.out.println("   📋 Saltando generación de paths - usando archivo existente");
            System.out.println("   📁 Paths serán leídos desde: " + pathsFile.getName());
            
            if (simulation.graphSpecialUser != null) {
                int numItems = simulation.graphSpecialUser.accessItemFile.getNumberOfItems();
                int numRooms = simulation.graphSpecialUser.accessGraphFile.getNumberOfRoom();
                
                System.out.println("   📋 Información del SIMPLE_SCENARIO:");
                System.out.println("      - Items: " + numItems + " (paintings)");
                System.out.println("      - Habitaciones: " + numRooms);
                System.out.println("      - Dimensiones: 500 x 500 píxeles");
            }
            
            return simulation;
            
        } catch (Exception e) {
            System.err.println("❌ Error creando simulación para SIMPLE_SCENARIO: " + e.getMessage());
            e.printStackTrace();
            return null;
            
        } finally {
            es.unizar.util.Literals.ROOM_FLOOR_COMBINED = originalRoomPath;
            es.unizar.util.Literals.ITEM_FLOOR_COMBINED = originalItemPath;
            es.unizar.util.Literals.GRAPH_FLOOR_COMBINED = originalGraphPath;
        }
    }

    
    /**
     * Runs the simulation until completion or max iterations
     */
    private static void runSimulationToCompletion(Simulation simulation) {
        if (simulation == null) {
            System.err.println("❌ Error: simulación es null");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        currentIteration = 0;

        Configuration.simulation = simulation;
        
        java.util.Map<Integer, es.unizar.gui.UserInfo.UserState> stateOfUsers = 
            new java.util.HashMap<Integer, es.unizar.gui.UserInfo.UserState>();
        java.util.Map<es.unizar.util.Pair<Integer,Integer>, Double> timeUsersInRooms = 
            new java.util.HashMap<es.unizar.util.Pair<Integer,Integer>, Double>();
        
        try {
            simulation.initializeUsers();
            System.out.println("   👥 Usuarios inicializados correctamente");
            
            List<User> users = simulation.getAllUsers();
            
            User[] userArray = users.toArray(new User[0]);
            
            es.unizar.gui.simulation.UserRunnable realUserRunnable = 
                new es.unizar.gui.simulation.UserRunnable(
                    userArray,
                    stateOfUsers,
                    timeUsersInRooms
                );
            MainSimulator.userRunnable = realUserRunnable;
            
            System.out.println("   🦠 Inicializando sistema epidémico...");
            
            if (simulation.epidemicManager == null) {
                simulation.epidemicManager = new es.unizar.epidemic.EpidemicSimulationManager();
            }
            
            for (User user : users) {
                if (user.getEpidemicExtension() == null) {
                    user.setEpidemicExtension(new es.unizar.epidemic.UserEpidemicExtension());
                }
                
                es.unizar.gui.UserInfo.UserState userState = 
                    new es.unizar.gui.UserInfo.UserState(user.room);
                stateOfUsers.put(user.userID, userState);
            }
            
            simulation.epidemicManager.initializeEpidemicSystem(users);
            System.out.println("   ✅ Sistema epidémico inicializado correctamente");
           
        } catch (Exception e) {
            System.err.println("❌ Error inicializando usuarios: " + e.getMessage());
            e.printStackTrace();
            
            System.out.println("   🔧 Intentando inicialización alternativa...");
            
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
                    simulation.epidemicManager = new es.unizar.epidemic.EpidemicSimulationManager();
                }
                
                for (User user : users) {
                    if (user.getEpidemicExtension() == null) {
                        user.setEpidemicExtension(new es.unizar.epidemic.UserEpidemicExtension());
                    }
                    
                    es.unizar.gui.UserInfo.UserState userState = 
                        new es.unizar.gui.UserInfo.UserState(user.room);
                    stateOfUsers.put(user.userID, userState);
                }
                
                simulation.epidemicManager.initializeEpidemicSystem(users);
                
            } catch (Exception e2) {
                System.err.println("❌ Error en inicialización alternativa: " + e2.getMessage());
                return;
            }
        }
        
        int maxIterations = Integer.MAX_VALUE;
        int iteration = 0;

        double simulatedTimeElapsed = 0.0;
        double timePerIteration = simulation.getTimeForIterationInSecond();

        System.out.println("   ⏳ Iniciando simulación basada en tiempo simulado...");
        System.out.println("   📏 Tiempo por iteración: " + timePerIteration + " segundos simulados");
        System.out.println("   🎯 Tiempo objetivo: " + getMaxSimulatedTime() + " segundos simulados");
        
        startTime = System.currentTimeMillis();    
        try {
            while (!isSimulationComplete(simulation) && iteration < maxIterations) {   
                simulation.updateUsers(stateOfUsers, timeUsersInRooms);
                
                iteration++;
                currentIteration = iteration;
                simulatedTimeElapsed += timePerIteration;
            }
        } catch (Exception e) {
            System.err.println("❌ Error durante la simulación en iteración " + iteration + ": " + e.getMessage());
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
     * Initializes users with basic locations if standard initialization fails
     */
    private static void initializeUsersWithBasicLocations(Simulation simulation) {
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
     * Verifies if the simulation has finished
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
            System.out.println("   🛑 Criterio de parada: Tiempo simulado completado (" + 
                            elapsedSimulatedTime + "/" + maxSimulatedTimeSeconds + " segundos)");
        }
        
        return timeExceeded;
    }

    private static int getMaxSimulatedTime() {
        return EpidemicConfiguration.getInstance().getSimulationDurationSeconds();
    }

    private static int getElapsedSimulatedTime(Simulation simulation) {
        int totalIterations = getCurrentIteration();
        double secondsPerIteration = simulation.getTimeForIterationInSecond();
        
        return (int) (totalIterations * secondsPerIteration);
    }

    private static int getCurrentIteration() {
        return currentIteration;
    }
        
    /**
     * Collects simulation results into a structured format
     */
    private static SimulationResult collectSimulationResults(Simulation simulation, Scenarios.TestScenario scenario, String model) {
        SimulationResult result = new SimulationResult();
        result.scenarioName = scenario.name;
        result.modelUsed = model;
        result.totalUsers = simulation.getAllUsers().size();
        
        List<User> users = simulation.getAllUsers();
        for (User user : users) {
            if (user.getEpidemicExtension() != null) {
                switch (user.getEpidemicExtension().getHealthStatus()) {
                    case SUSCEPTIBLE:
                        result.susceptible++;
                        break;
                    case EXPOSED:
                        result.exposed++;
                        break;
                    case INFECTIOUS_SYMPTOMATIC:
                        result.infectiousSymp++;
                        break;
                    case INFECTIOUS_ASYMPTOMATIC:
                        result.infectiousAsymp++;
                        break;
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

            // result.individualRisk = calculateIndividualRiskForModel(simulation, model);
            result.individualRisk = simulation.calculateAverageTheoreticalRiskForAllRooms() * 100.0;
            
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
     * Compares results between different models for the same configuration
     */
    private static void compareModelResults(java.util.Map<String, SimulationResult> results, String scenarioName) {
        System.out.println("📈 COMPARACIÓN DETALLADA POR MODELO:");

        // 1. Lelieveld
        System.out.println("\n🔬 MODELO: AEROSOL_LELIEVELD");
        System.out.printf("%-20s %-12s %-12s %-28s %-12s\n",
            "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (copias RNA/m³)", "RIESGO INDIV.");
        System.out.println("-".repeat(90));
        SimulationResult lelieveld = results.get("AEROSOL_LELIEVELD");
        if (lelieveld != null) {
            System.out.printf("%-20s %-12.2f %-12d %-28.6f %-12.2f\n",
                "AEROSOL_LELIEVELD",
                lelieveld.attackRate * 100,
                lelieveld.infectiousSymp + lelieveld.infectiousAsymp + lelieveld.superSpreaders,
                lelieveld.averageConcentration,
                lelieveld.individualRisk
            );
        }

        // 2. Peng
        System.out.println("\n🔬 MODELO: AEROSOL_PENG");
        System.out.printf("%-20s %-12s %-12s %-22s %-12s\n",
            "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONCENTR. (quanta/m³)", "RIESGO INDIV.");
        System.out.println("-".repeat(80));
        SimulationResult peng = results.get("AEROSOL_PENG");
        if (peng != null) {
            System.out.printf("%-20s %-12.2f %-12d %-22.6f %-12.2f\n",
                "AEROSOL_PENG",
                peng.attackRate * 100,
                peng.infectiousSymp + peng.infectiousAsymp + peng.superSpreaders,
                peng.averageConcentration,
                peng.individualRisk
            );
        }

        // 3. Simple Proximity
        System.out.println("\n🔬 MODELO: SIMPLE_PROXIMITY");
        System.out.printf("%-20s %-12s %-12s %-12s %-12s\n",
            "CONFIGURACIÓN", "TASA ATAQUE", "INFECTIVOS", "CONTACTOS", "CONT.INFEC");
        System.out.println("-".repeat(90));
        SimulationResult simple = results.get("SIMPLE_PROXIMITY");
        if (simple != null) {
            System.out.printf("%-20s %-12.2f %-12d %-12d %-12d\n",
                "SIMPLE_PROXIMITY",
                simple.attackRate * 100,
                simple.infectiousSymp + simple.infectiousAsymp + simple.superSpreaders,
                simple.totalContacts,
                simple.infectiousContacts
            );
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
        int exposed;
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
        
        public SimulationResult() {
            totalUsers = 0;
            susceptible = 0;
            exposed = 0;
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
        }
    }
}