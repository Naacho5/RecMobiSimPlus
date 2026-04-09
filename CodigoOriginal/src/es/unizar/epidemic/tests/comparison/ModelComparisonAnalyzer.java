package es.unizar.epidemic.tests.comparison;

import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.util.Pair;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Analyzer for comparing results between different epidemic models
 * 
 * @author Nacho Palacio
 */
public class ModelComparisonAnalyzer {
    
    private static final String[] MODELS = {"SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD"};
    
    /**
     * Main entry point for model comparison.
     * Placeholder function for future implementation of comprehensive
     * model comparison functionality.
     */
    public static void compare() {
        System.out.println("\n🔬 EXECUTING COMPARISON BETWEEN MODELS");
        System.out.println("   (Placeholder function - implement as needed)");
    }
    
    /**
     * Compares results between different models for the same configuration.
     * Generates detailed comparison tables for each model (Lelieveld, Peng, Simple Proximity)
     * showing attack rates, infectious counts, concentrations, and execution times.
     * Exports results to CSV file for further analysis.
     * Moved from SimulationEpidemicValidator
     * 
     * @param results map of model names to their simulation results
     * @param scenarioName name of the scenario being compared
     */
    public static void compareModelResults(Map<String, SimulationResult> results, 
                                        String scenarioName, 
                                        String recommendationAlgorithm,
                                        int totalUsers,
                                        int simulationDurationSeconds) {
        File resultsDir = new File("./results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String sanitizedScenarioName = scenarioName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String sanitizedAlgorithm = recommendationAlgorithm.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        int durationMinutes = simulationDurationSeconds / 60;
        String csvFilename = String.format("./results/comparison_models_%s_%s_%dusers_%dmin_%s.csv", 
                                        sanitizedScenarioName, 
                                        sanitizedAlgorithm, 
                                        totalUsers,
                                        durationMinutes,
                                        timestamp);

        try (FileWriter csvWriter = new FileWriter(csvFilename)) {
            csvWriter.append("MODELO;TASA_ATAQUE_PCT;INFECTIVOS_TOTAL;METRICA_1;METRICA_2;VALORACION_MEDIA;DISTANCIA_MEDIA;TIEMPO_EJECUCION_SEG\n");
            
            System.out.println(" DETAILED COMPARISON BY MODEL:");

            double globalAvgRating = 0.0;
            double globalAvgDistance = 0.0;
            double averageDistanceRooms = 0.0;
            
            if (es.unizar.gui.Configuration.simulation != null && 
                es.unizar.gui.Configuration.simulation.userRatings != null) {
                double totalSum = 0.0;
                int totalCount = 0;
                for (Map.Entry<Integer, List<Float>> entry : 
                        es.unizar.gui.Configuration.simulation.userRatings.entrySet()) {
                    List<Float> ratings = entry.getValue();
                    for (float r : ratings) totalSum += r;
                    totalCount += ratings.size();
                }
                globalAvgRating = totalCount > 0 ? totalSum / totalCount : 0.0;
            }
            
            if (es.unizar.gui.Configuration.simulation != null) {
                // globalAvgDistance = es.unizar.gui.Configuration.simulation
                //     .calculateGlobalAverageDistanceBetweenVisitedItems();
                Pair<Double, Double> distances = es.unizar.gui.Configuration.simulation.calculateGlobalAverageDistanceBetweenVisitedItems();
                globalAvgDistance = distances.getF();
                averageDistanceRooms = distances.getS();
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
                
                csvWriter.append(String.format("AEROSOL_LELIEVELD;%.2f;%d;%.6f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    attackRateL,
                    totalInfectiousL,
                    lelieveld.averageConcentration,
                    lelieveld.individualRisk,
                    globalAvgRating,
                    globalAvgDistance,
                    averageDistanceRooms,
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
                
                csvWriter.append(String.format("AEROSOL_PENG;%.2f;%d;%.6f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    attackRateP,
                    totalInfectiousP,
                    peng.averageConcentration,
                    peng.individualRisk,
                    globalAvgRating,
                    globalAvgDistance,
                    averageDistanceRooms,
                    timeSec
                ));
            }

            // 3. Simple Proximity
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
                
                csvWriter.append(String.format("SIMPLE_PROXIMITY;%.2f;%d;%d;%d;%.2f;%.2f;%.2f;%.2f\n",
                    attackRateS,
                    totalInfectiousS,
                    simple.totalContacts,
                    simple.infectiousContacts,
                    globalAvgRating,
                    globalAvgDistance,
                    averageDistanceRooms,
                    timeSec
                ));
            }

            // User ratings and distances
            if (es.unizar.gui.Configuration.simulation != null && 
                es.unizar.gui.Configuration.simulation.userRatings != null) {
                System.out.println("\n USER RATINGS:");
                double totalSum = 0.0;
                int totalCount = 0;
                
                for (Map.Entry<Integer, List<Float>> entry : 
                        es.unizar.gui.Configuration.simulation.userRatings.entrySet()) {
                    int userId = entry.getKey();
                    List<Float> ratings = entry.getValue();
                    double sum = 0.0;
                    for (float r : ratings) sum += r;
                    double avg = ratings.isEmpty() ? 0.0 : sum / ratings.size();
                    System.out.printf("    User %d: %.2f (%d ratings)\n", 
                                    userId, avg, ratings.size());
                    totalSum += sum;
                    totalCount += ratings.size();
                }
                
                globalAvgRating = totalCount > 0 ? totalSum / totalCount : 0.0;
                System.out.printf("\n   ⭐ Global average rating: %.2f (%d total ratings)\n", 
                                globalAvgRating, totalCount);
            }

            if (es.unizar.gui.Configuration.simulation != null) {
                // globalAvgDistance = es.unizar.gui.Configuration.simulation
                //     .calculateGlobalAverageDistanceBetweenVisitedItems();
                Pair<Double, Double> distances = es.unizar.gui.Configuration.simulation.calculateGlobalAverageDistanceBetweenVisitedItems();
                globalAvgDistance = distances.getF();
                averageDistanceRooms = distances.getS();
            }
            
            // Resumen en CSV
            csvWriter.append(String.format("\n# Métricas globales para configuración: %s\n", scenarioName));
            csvWriter.append(String.format("# Algoritmo de recomendación: %s\n", recommendationAlgorithm));
            csvWriter.append(String.format("# Número de usuarios: %d\n", totalUsers));
            csvWriter.append(String.format("# Duración de simulación: %d segundos (%d minutos)\n", 
                                        simulationDurationSeconds, durationMinutes));
            csvWriter.append(String.format("# Valoración media global: %.2f\n", globalAvgRating));
            csvWriter.append(String.format("# Distancia media entre items visitados: %.2f\n", globalAvgDistance));
            csvWriter.append(String.format("# Distancia media entre habitaciones: %.2f\n", averageDistanceRooms));
            
            csvWriter.flush();
            System.out.println("\n✅ Model comparison saved to: " + csvFilename);     
        } catch (IOException e) {
            System.err.println(" Error writing model comparison CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Compares results between different epidemic models (cross-model comparison).
     * Analyzes and prints detailed comparisons between synthetic trajectories and
     * real contact data for each model. Includes statistical analysis and direct
     * comparison of attack rates.
     * Moved from SimulationEpidemicValidator
     * 
     * @param allResults nested map structure: model -> trajectory type (synthetic/real) -> config -> result
     */
    public static void printCrossModelComparison(
            Map<String, Map<String, Map<String, SimulationResult>>> allResults) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🔬 COMPARISON BETWEEN EPIDEMIC MODELS");
        System.out.println("=".repeat(100));
        
        for (String model : MODELS) {
            System.out.println("\n🔬 ANALYSIS FOR MODEL: " + model);
            System.out.println("━".repeat(100));
            
            Map<String, Map<String, SimulationResult>> modelResults = allResults.get(model);
            
            if (modelResults == null) {
                System.err.println("    No results for model " + model);
                continue;
            }
            
            Map<String, SimulationResult> syntheticResults = modelResults.get("synthetic");
            Map<String, SimulationResult> realResults = modelResults.get("real");
            
            if (syntheticResults == null || realResults == null) {
                System.err.println("    Missing synthetic or real results for " + model);
                continue;
            }
            
            // 1. Table: Synthetic trajectories
            System.out.println("\n SYNTHETIC TRAJECTORIES:");
            System.out.println("━".repeat(100));
            
            printModelSpecificHeaderForComparison(model);
            System.out.println("-".repeat(100));
            
            for (Map.Entry<String, SimulationResult> entry : syntheticResults.entrySet()) {
                String configName = entry.getKey();
                SimulationResult result = entry.getValue();
                printModelSpecificRowForComparison(configName, result, model);
            }
            
            System.out.println("-".repeat(100));
            
            // 2. Table: Real contacts
            System.out.println("\n REAL CONTACTS:");
            System.out.println("━".repeat(100));
            
            printModelSpecificHeaderForComparison(model);
            System.out.println("-".repeat(100));
            
            for (Map.Entry<String, SimulationResult> entry : realResults.entrySet()) {
                String configName = entry.getKey();
                SimulationResult result = entry.getValue();
                printModelSpecificRowForComparison(configName, result, model);
            }
            
            System.out.println("-".repeat(100));
            
            // 3. Statistical analysis
            System.out.println("\n STATISTICAL ANALYSIS FOR " + model + ":");
            System.out.println("━".repeat(100));
            
            double avgSynthetic = calculateAverageAttackRate(syntheticResults);
            double avgReal = calculateAverageAttackRate(realResults);
            
            System.out.println("\n AVERAGE ATTACK RATE:");
            System.out.printf("   Synthetic: %.2f%%\n", avgSynthetic);
            System.out.printf("   Real:      %.2f%%\n", avgReal);
            System.out.printf("   Difference: %.2f%%\n", Math.abs(avgReal - avgSynthetic));
            
            // 4. Direct comparison
            System.out.println("\n SYNTHETIC vs REAL COMPARISON:");
            System.out.println("━".repeat(100));
            System.out.printf("%-25s %-15s %-15s %-15s\n",
                "CONFIGURATION", "SYNTHETIC (%)", "REAL (%)", "DIFFERENCE");
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
     * Outputs column headers appropriate for the specific epidemic model,
     * with different metrics for Simple Proximity, Peng, and Lelieveld models.
     * 
     * @param model the epidemic model name (SIMPLE_PROXIMITY, AEROSOL_PENG, or AEROSOL_LELIEVELD)
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
     * Outputs a formatted data row with metrics appropriate for the specific
     * epidemic model, including attack rate, infected count, and model-specific metrics.
     * 
     * @param configName name of the configuration being displayed
     * @param result simulation result containing metrics to display
     * @param model the epidemic model name (SIMPLE_PROXIMITY, AEROSOL_PENG, or AEROSOL_LELIEVELD)
     */
    private static void printModelSpecificRowForComparison(
            String configName, SimulationResult result, String model) {
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
     * Computes the arithmetic mean of attack rates across all simulation
     * results in the provided map.
     * 
     * @param results map of configuration names to simulation results
     * @return average attack rate as a percentage (0.0 to 100.0), or 0.0 if map is empty
     */
    private static double calculateAverageAttackRate(Map<String, SimulationResult> results) {
        if (results == null || results.isEmpty()) return 0.0;
        
        double sum = 0.0;
        for (SimulationResult result : results.values()) {
            sum += result.attackRate * 100;
        }
        
        return sum / results.size();
    }
}