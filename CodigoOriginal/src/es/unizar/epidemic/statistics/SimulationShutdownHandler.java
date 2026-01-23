package es.unizar.epidemic.statistics;

/** Handles simulation shutdown to ensure proper resource cleanup */
public class SimulationShutdownHandler {
    
    public static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n Finalizing simulation...");
            
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            stats.endSimulation();
            // stats.printFinalStatistics();
            
            System.out.println("Simulation finalized successfully.");
        }));
        
        System.out.println(" (Ctrl+C to terminate the simulation at any time) ");
    }
}