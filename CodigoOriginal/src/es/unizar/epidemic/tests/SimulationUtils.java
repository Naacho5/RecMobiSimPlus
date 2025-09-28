package es.unizar.epidemic.tests;

import es.unizar.epidemic.EpidemicConfiguration;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;

import java.util.List;

public class SimulationUtils {
    /**
     * Creates and runs a simulation based on the provided scenario and model.
     */
    public static Simulation createAndRunSimulation(Scenarios.TestScenario scenario, String model) {
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