package es.unizar.epidemic.statistics;

public class SimulationShutdownHandler {
    
    public static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Finalizando simulación...");
            
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            stats.endSimulation();
            // stats.printFinalStatistics();
            
            System.out.println("Simulación finalizada correctamente.");
        }));
        
        System.out.println("🔧 (Ctrl+C para terminar)");
    }
}