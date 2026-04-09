package es.unizar.epidemic.tests;

/**
 * Main entry point for all epidemic simulation tests
 * 
 * @author Nacho Palacio
 */
public class SimulationTestRunner {
    
    public static void main(String[] args) {
        System.out.println("🧪 === VALIDACIÓN EPIDÉMICA ===\n");
        
        if (args.length == 0) {
            printMenu();
            return;
        }
        
        String testType = args[0];
        
        switch (testType.toLowerCase()) {
            case "synthetic":
                es.unizar.epidemic.tests.scenarios.SyntheticScenarioTests.runAll();
                break;
                
            case "contacts":
                es.unizar.epidemic.tests.contacts.ContactBasedTests.runAll();
                break;
                
            case "mixed":
                es.unizar.epidemic.tests.contacts.MixedModeTests.run();
                break;
                
            case "compare":
                es.unizar.epidemic.tests.comparison.ModelComparisonAnalyzer.compare();
                break;
                
            case "all":
                runAllTests();
                break;
                
            default:
                System.err.println(" Tipo de test desconocido: " + testType);
                printMenu();
        }
    }
    
    /**
     * Runs all available epidemic simulation tests
     */
    private static void runAllTests() {
        es.unizar.epidemic.tests.scenarios.SyntheticScenarioTests.runAll();
        es.unizar.epidemic.tests.contacts.ContactBasedTests.runAll();
        es.unizar.epidemic.tests.contacts.MixedModeTests.run();
        es.unizar.epidemic.tests.comparison.ModelComparisonAnalyzer.compare();
    }
    
    /**
     * Prints usage menu
     */
    private static void printMenu() {
        System.out.println("Uso: java SimulationTestRunner [tipo]");
        System.out.println("\nTipos disponibles:");
        System.out.println("  synthetic  - Tests con escenarios sintéticos");
        System.out.println("  contacts   - Tests con contactos reales");
        System.out.println("  mixed      - Test de modo mixto");
        System.out.println("  compare    - Comparación entre modelos");
        System.out.println("  all        - Ejecutar todos los tests");
    }
}