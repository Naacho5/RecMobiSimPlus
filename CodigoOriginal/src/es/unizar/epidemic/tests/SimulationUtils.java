package es.unizar.epidemic.tests;

import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.gui.simulation.Simulation;


/**
 * Utility class for creating and running epidemic simulations for testing
 * 
 * @author Nacho Palacio
 */
public class SimulationUtils {
    /**
     * Creates and runs a simulation based on the provided scenario and model.
     * 
     * @param scenario The test scenario to simulate
     * @param model The epidemic model to use
     * @return The completed Simulation instance
     */
    public static Simulation createAndRunSimulation(Scenarios.TestScenario scenario, String model) {
        System.out.println("Creating and running simulation for scenario: " + scenario.name + " with model: " + model);
        EpidemicConfiguration config = EpidemicConfiguration.getInstance();
        config.setSelectedModel(model);
        config.setDefaultVentilationRate(scenario.ventilationRate);
        config.setMaskComplianceRate(scenario.maskFraction);
        config.setImmunePopulationFraction(scenario.immuneFraction);
        config.setInitialInfectedUsers(Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability)));
        config.setSimulationDurationSeconds((int)(scenario.standardExposureHours * 3600));
        config.setTotalUsers(scenario.standardVisitorCount);

        Simulation simulation = new Simulation();
        simulation.initializeUsers();

        int maxIterations = (int) (scenario.standardExposureHours * 3600 / simulation.getTimeForIterationInSecond());
        for (int i = 0; i < maxIterations; i++) {
            simulation.updateUsers(null, null);
        }

        if (simulation.getEpidemicManager() != null) {
            simulation.getEpidemicManager().evaluateFinalAerosolTransmissions(simulation.getAllUsers());
        }

        return simulation;
    }
}