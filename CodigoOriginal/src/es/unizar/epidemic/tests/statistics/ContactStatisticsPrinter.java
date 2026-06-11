package es.unizar.epidemic.tests.statistics;

import es.unizar.gui.simulation.Simulation;
import java.util.Map;

/**
 * Prints contact-specific statistics after simulation completion
 * 
 * @author Nacho Palacio
 */
public class ContactStatisticsPrinter {
    
    /**
     * Prints contact-specific statistics after simulation completion
     * Movido desde SimulationEpidemicValidator.java
     * 
     * @param simulation Simulation instance with contact data
     */
    public static void print(Simulation simulation) {
        if (simulation.coincidenceTracker == null) {
            return; 
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println(" DETAILED CONTACT STATISTICS AFTER SIMULATION:");
        System.out.println("=".repeat(100));
        
        if (simulation.cliqueUserMapping != null && !simulation.cliqueUserMapping.isEmpty()) {
            System.out.println("\n ATTACK RATES BY CLIQUE:");
            
            simulation.coincidenceTracker.printAttackRatesByClique(
                simulation.getAllUsers(),
                simulation.initialSusceptiblesByClique,
                simulation.cliqueUserMapping
            );
        }
        
        System.out.println("\n INTER-CLIQUE COINCIDENCES:");
        simulation.coincidenceTracker.printIsolationMetrics();
        simulation.coincidenceTracker.printDetailedUserCoincidences();
        
        Map<String, Object> metrics = simulation.coincidenceTracker.getGlobalMetrics();
        double isolationRate = (double) metrics.getOrDefault("isolationRate", 0.0);
        
        System.out.println("\n SUCCESS CRITERION VERIFICATION:");
        System.out.printf("   Isolation rate: %.2f%%\n", isolationRate * 100);
        
        if (isolationRate >= 0.95) {
            System.out.println("   ✅ SUCCESS (≥95% isolation)");
        } else {
            System.out.println("    FAILURE (<95% isolation)");
        }
        
        System.out.println("\n" + "=".repeat(100) + "\n");
    }
}