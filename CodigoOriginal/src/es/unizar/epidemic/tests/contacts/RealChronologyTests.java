package es.unizar.epidemic.tests.contacts;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.tests.common.SimulationUtils;
import es.unizar.epidemic.tests.common.SimulationResult;
import es.unizar.gui.simulation.Simulation;
import es.unizar.epidemic.data.ContactTrajectoryBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealChronologyTests {

    private static final String[] MODELS = {"AEROSOL_PENG"/*, "AEROSOL_LELIEVELD"*/};
    private static final String RESULTS_DIR = "./results/test_contacts/real_chronology/";

    private static final String SCENARIO_NAME = "GENERAL_SCENARIO";

    private static final int TARGET_SIM_SECONDS = 3600;
    private static final int[] CLIQUE_IDS = {0};

    private static String executionTimestamp;

    public static void run() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("REAL CHRONOLOGY CONTACT TESTS");
        System.out.println("=".repeat(100));

        executionTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(new java.util.Date());

        new File(RESULTS_DIR).mkdirs();

        List<TestConfiguration> configurations = createTestConfigurations();

        for (TestConfiguration testConfig : configurations) {
            System.out.println("\n" + "-".repeat(100));
            System.out.println("TEST CONFIGURATION: cliqueId=" + testConfig.cliqueId +
                " users=" + testConfig.totalUsers +
                " realDuration=" + testConfig.realDurationSeconds + "s" +
                " timeForIteration=" + String.format("%.2f", testConfig.timeForIteration));
            System.out.println("-".repeat(100));

            Map<String, SimulationResult> results = runChronologyTests(testConfig);
            exportResults(testConfig, results);
            printResults(testConfig, results);
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("REAL CHRONOLOGY CONTACT TESTS COMPLETED");
        System.out.println("=".repeat(100) + "\n");
    }

    private static List<TestConfiguration> createTestConfigurations() {
            List<TestConfiguration> configs = new ArrayList<>();
            for (int cliqueId : CLIQUE_IDS) {
                configs.add(new TestConfiguration(cliqueId));
            }
            return configs;
        }

        private static Map<String, SimulationResult> runChronologyTests(
            TestConfiguration testConfig) {

        Map<String, SimulationResult> results = new HashMap<>();

        for (String model : MODELS) {
            try {
                EpidemicStatistics.resetInstance();

                EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
                epidemicConfig.setSelectedModel(model);
                epidemicConfig.setMaskComplianceRate(0.1);
                epidemicConfig.setDefaultVentilationRate(3.0);
                epidemicConfig.setInitialInfectedUsers(0);

                Simulation simulation = SimulationUtils.createSimulationWithRealChronology(
                        testConfig.cliqueId,
                        SCENARIO_NAME
                );

                if (simulation == null) {
                    throw new Exception("Could not create simulation");
                }

                int totalUsers = simulation.getAllUsers().size();
                long realDuration = simulation.getRealChronologyDurationSeconds();

                // Configurar epidemic con esos datos
                // EpidemicConfiguration epidemicConfig = EpidemicConfiguration.getInstance();
                // epidemicConfig.setSelectedModel(model);
                // epidemicConfig.setMaskComplianceRate(0.1);
                // epidemicConfig.setDefaultVentilationRate(3.0);
                epidemicConfig.setTotalUsers(totalUsers);
                epidemicConfig.setSimulationDurationSeconds((int) realDuration);
                // epidemicConfig.setInitialInfectedUsers(0);

                SimulationUtils.configureEpidemicParameters(epidemicConfig, null, model);

                long startTime = System.currentTimeMillis();
                SimulationUtils.runSimulationToCompletion(simulation);

                if (simulation.getEpidemicManager() != null) {
                    simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(
                            simulation.getAllUsers());
                }

                SimulationResult result = SimulationUtils.collectSimulationResults(
                        simulation, null, model);
                result.executionTimeMs = System.currentTimeMillis() - startTime;

                results.put(model, result);

            } catch (Exception e) {
                System.err.println("Error in model " + model + ": " + e.getMessage());
                e.printStackTrace();
                results.put(model, new SimulationResult());
            }
        }

        return results;
    }

    private static void exportResults(
            TestConfiguration testConfig,
            Map<String, SimulationResult> results) {

        String filename = String.format("%sreal_chronology_%s_clique_%d.csv",
                RESULTS_DIR, executionTimestamp, testConfig.cliqueId);

        try (FileWriter writer = new FileWriter(filename)) {
            writer.append("timestamp;contact_mode;clique_id;num_users;real_duration_sec;" +
                    "time_for_iteration;epidemic_model;attack_rate_global;" +
                    "attack_rate_intra_clique;isolation_rate;concentration;" +
                    "individual_risk;execution_time_sec\n");

            for (Map.Entry<String, SimulationResult> entry : results.entrySet()) {
                String model = entry.getKey();
                SimulationResult result = entry.getValue();

                writer.append(String.format("%s;REAL_CHRONOLOGY;%d;%d;%d;%.2f;%s;%.4f;%.4f;%.4f;%.6f;%.4f;%.2f\n",
                        executionTimestamp,
                        testConfig.cliqueId,
                        result.totalUsers,
                        result.simulationDurationSeconds,
                        result.simulationDurationSeconds / 3600.0,
                        model,
                        result.attackRate * 100,
                        result.intraCliqueAttackRate * 100,
                        result.isolationRate * 100,
                        result.averageConcentration,
                        result.individualRisk,
                        result.executionTimeMs / 1000.0
                ));
            }

            writer.flush();
            System.out.println("Results exported to: " + filename);

        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prints test results to console in a formatted table.
     * 
     * @param testConfig the test configuration
     * @param results map of model names to their simulation results
     */
    private static void printResults(
            TestConfiguration testConfig,
            Map<String, SimulationResult> results) {
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 RESULTS SUMMARY: REAL CHRONOLOGY - Clique " + testConfig.cliqueId);
        System.out.println("=".repeat(100));
        System.out.printf("%-25s %-15s %-20s %-18s %-18s %-18s %-15s\n",
            "EPIDEMIC MODEL", "GLOBAL ATTACK", "INTRA-CLIQUE ATTACK", "ISOLATION RATE", 
            "CONCENTRATION", "INDIVIDUAL RISK", "TIME (s)");
        System.out.println("-".repeat(100));
        
        for (Map.Entry<String, SimulationResult> entry : results.entrySet()) {
            String model = entry.getKey();
            SimulationResult result = entry.getValue();
            
            System.out.printf("%-25s %-15.2f%% %-20.2f%% %-18.2f%% %-18.6f %-18.4f %-15.2f\n",
                model,
                result.attackRate * 100,
                result.intraCliqueAttackRate * 100,
                result.isolationRate * 100,
                result.averageConcentration,
                result.individualRisk,
                result.executionTimeMs / 1000.0
            );
        }
        
        System.out.println("=".repeat(100));
    }

    private static class TestConfiguration {
        int cliqueId;
        int totalUsers;
        long realDurationSeconds;
        double timeForIteration;

        TestConfiguration(int cliqueId) {
            this.cliqueId = cliqueId;
            // this.totalUsers = totalUsers;
            // this.realDurationSeconds = realDurationSeconds;
            // this.timeForIteration = timeForIteration;
        }
    }
}