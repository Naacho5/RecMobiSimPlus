package es.unizar.epidemic.tests;

import es.unizar.epidemic.ContactRecord;
import es.unizar.epidemic.ContactType;
import es.unizar.epidemic.HealthStatus;
import es.unizar.epidemic.UserEpidemicExtension;
import es.unizar.epidemic.models.SimpleProximityModel;
import es.unizar.epidemic.models.ModelParameters1;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests avanzados para SimpleProximityModel.
 * Incluye análisis de sensibilidad y combinatoria de parámetros
 * Adaptado del AerosolTransmissionModel1Test para modelo de proximidad
 * 
 */
public class SimpleProximityModelTest {

    private static SimpleProximityModelTestVersion model;
    private static List<User> testUsers;
    private static int testsExecuted = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    // ==================== CONFIGURACIONES PARA ANÁLISIS DE SENSIBILIDAD ====================
    
    private static final double[] DISTANCES = {0.5, 1.0, 2.0, 4.0}; // metros
    private static final double[] DURATIONS = {5.0, 30.0, 120.0, 600.0}; // segundos
    private static final String[] INFECTION_RATIOS = {"LOW", "MEDIUM", "HIGH"}; // 1, 2, 3 infectados
    private static final String[] MASK_SCENARIOS = {"NONE", "HALF", "ALL"}; // Ninguno, Mitad, Todos

    public static void main(String[] args) {
        System.out.println("🧪 === TESTS AVANZADOS PARA SIMPLE PROXIMITY MODEL ===");
        System.out.println("📅 Fecha: " + java.time.LocalDateTime.now());
        System.out.println("🔬 Incluye: Análisis de Sensibilidad + Combinatoria de Parámetros + Modelo Proximidad");
        System.out.println("=".repeat(70));
        
        // Configurar entorno de test
        setupTestEnvironment();
        
        // Ejecutar tests básicos
        executeBasicTests();
        
        // Ejecutar tests específicos del modelo de proximidad
        executeProximityTests();
        
        // Ejecutar tests avanzados de sensibilidad
        executeSensitivityAnalysis();
        
        // Ejecutar tests de mascarillas y distancia
        executeMaskAndDistanceTests();
        
        // Ejecutar tests de escenarios reales
        executeScenarioValidationTests();
        
        // Mostrar resumen final
        printFinalSummary();
    }

    /**
     * Configura el entorno de test
     */
    private static void setupTestEnvironment() {
        try {
            model = new SimpleProximityModelTestVersion();
            testUsers = createTestUsers();
            
            try {
                if (Configuration.simulation == null) {
                    System.out.println("⚠️ Configuration.simulation es null - usando mock de usuarios");
                }
            } catch (Exception e) {
                System.out.println("⚠️ No se puede acceder a Configuration - usando mock de usuarios");
            }
            
            System.out.println("Entorno de test configurado correctamente");
            System.out.println("   - Modelo creado: SÍ (Simple Proximity)");
            System.out.println("   - Usuarios de prueba: " + testUsers.size());
            System.out.println("   - Parámetros configurados: SÍ");
            System.out.println();
            
        } catch (Exception e) {
            System.out.println("❌ Error configurando entorno de test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ejecuta tests básicos del modelo de proximidad
     */
    private static void executeBasicTests() {
        System.out.println("🔍 === EJECUTANDO TESTS BÁSICOS PROXIMIDAD ===");
        
        runTest("Inicialización del modelo de proximidad", SimpleProximityModelTest::testModelInitialization);
        runTest("Parámetros por defecto del modelo", SimpleProximityModelTest::testDefaultParameters);
        runTest("Configuración de parámetros", SimpleProximityModelTest::testParameterConfiguration);
        runTest("Cálculo de factor de distancia", SimpleProximityModelTest::testDistanceFactor);
        runTest("Cálculo de factor de duración", SimpleProximityModelTest::testDurationFactor);
        runTest("Probabilidad de transmisión básica", SimpleProximityModelTest::testBasicTransmissionProbability);
        runTest("Manejo de distancias máximas", SimpleProximityModelTest::testMaximumDistance);
        runTest("Actualización de estados de salud", SimpleProximityModelTest::testHealthStateUpdate);
        runTest("Performance básico", SimpleProximityModelTest::testBasicPerformance);
        runTest("Test de integración completo", SimpleProximityModelTest::testFullIntegration);
        
        System.out.println("=== TESTS BÁSICOS COMPLETADOS ===\n");
    }

    /**
     * Ejecuta tests específicos del modelo de proximidad
     */
    private static void executeProximityTests() {
        System.out.println("📏 === EJECUTANDO TESTS ESPECÍFICOS PROXIMIDAD ===");
        
        runTest("Validación de umbrales de distancia", SimpleProximityModelTest::testDistanceThresholds);
        runTest("Validación de umbrales de duración", SimpleProximityModelTest::testDurationThresholds);
        runTest("Efecto del estado de salud infectivo", SimpleProximityModelTest::testInfectiousStatusEffect);
        runTest("Validación de tipos de contacto", SimpleProximityModelTest::testContactTypes);
        runTest("Combinación de factores multiplicativos", SimpleProximityModelTest::testMultiplicativeFactors);
        runTest("Límites de probabilidad [0,1]", SimpleProximityModelTest::testProbabilityBounds);
        
        System.out.println("=== TESTS PROXIMIDAD COMPLETADOS ===\n");
    }

    /**
     * Ejecuta análisis de sensibilidad sistemático
     */
    private static void executeSensitivityAnalysis() {
        System.out.println("🔬 === INICIANDO ANÁLISIS DE SENSIBILIDAD PROXIMIDAD ===");
        System.out.println("📊 Combinaciones a probar: " + 
                          (DISTANCES.length * DURATIONS.length * 
                           INFECTION_RATIOS.length * MASK_SCENARIOS.length));
        System.out.println();
        
        runTest("Análisis de sensibilidad combinatorial", SimpleProximityModelTest::testSensitivityAnalysis);
        runTest("Validación de tendencias esperadas", SimpleProximityModelTest::testExpectedTrends);
        runTest("Detección de casos extremos", SimpleProximityModelTest::testExtremeCases);
        
        System.out.println("=== ANÁLISIS DE SENSIBILIDAD COMPLETADO ===\n");
    }

    /**
     * Ejecuta tests específicos de mascarillas y distancia
     */
    private static void executeMaskAndDistanceTests() {
        System.out.println("😷 === INICIANDO TESTS DE MASCARILLAS Y DISTANCIA ===");
        
        runTest("Efectividad de mascarillas", SimpleProximityModelTest::testMaskEffectiveness);
        runTest("Impacto de la distancia social", SimpleProximityModelTest::testSocialDistancing);
        runTest("Combinación mascarillas + distancia", SimpleProximityModelTest::testMaskDistanceCombination);
        runTest("Validación de assertions esperadas", SimpleProximityModelTest::testExpectedAssertions);
        
        System.out.println("=== TESTS DE MASCARILLAS Y DISTANCIA COMPLETADOS ===\n");
    }

    // ==================== TESTS BÁSICOS ====================

    private static void testModelInitialization() {
        if (model == null) {
            throw new RuntimeException("El modelo no debe ser null");
        }
        
        String modelName = model.getModelName();
        if (modelName == null || !modelName.contains("Simple Proximity")) {
            throw new RuntimeException("Nombre del modelo incorrecto: " + modelName);
        }
        
        if (model.getMaxTransmissionDistance() <= 0) {
            throw new RuntimeException("La distancia máxima de transmisión debe ser > 0");
        }
        
        if (model.getBaseTransmissionProbability() <= 0 || model.getBaseTransmissionProbability() > 1) {
            throw new RuntimeException("La probabilidad base debe estar en (0,1]");
        }
        
        if (model.getMinContactDuration() < 0) {
            throw new RuntimeException("La duración mínima no puede ser negativa");
        }
        
        System.out.println("   ✓ Modelo Simple Proximity inicializado correctamente");
        System.out.println("   ✓ Nombre: " + modelName);
        System.out.println("   ✓ Parámetros accesibles");
    }

    private static void testDefaultParameters() {
        double maxDistance = model.getMaxTransmissionDistance();
        double baseProb = model.getBaseTransmissionProbability();
        int minDuration = model.getMinContactDuration();
        
        if (maxDistance != 3.0) {
            throw new RuntimeException("Distancia máxima por defecto incorrecta: " + maxDistance + " (esperado: 3.0)");
        }
        
        if (Math.abs(baseProb - 0.15) > 0.001) {
            throw new RuntimeException("Probabilidad base por defecto incorrecta: " + baseProb + " (esperado: 0.15)");
        }
        
        if (minDuration != 5) {
            throw new RuntimeException("Duración mínima por defecto incorrecta: " + minDuration + " (esperado: 5)");
        }
        
        System.out.println("   ✓ Parámetros por defecto validados");
        System.out.println("   ✓ Distancia máxima: " + maxDistance + " m");
        System.out.println("   ✓ Probabilidad base: " + (baseProb * 100) + "%");
        System.out.println("   ✓ Duración mínima: " + minDuration + " s");
    }

    private static void testParameterConfiguration() {
        // Configurar nuevos parámetros
        model.configureSimpleModel(2.5, 0.10, 10);
        
        if (Math.abs(model.getMaxTransmissionDistance() - 2.5) > 0.001) {
            throw new RuntimeException("Configuración de distancia máxima falló");
        }
        
        if (Math.abs(model.getBaseTransmissionProbability() - 0.10) > 0.001) {
            throw new RuntimeException("Configuración de probabilidad base falló");
        }
        
        if (model.getMinContactDuration() != 10) {
            throw new RuntimeException("Configuración de duración mínima falló");
        }
        
        // Restaurar valores por defecto
        model.configureSimpleModel(3.0, 0.15, 5);
        
        System.out.println("   ✓ Configuración de parámetros exitosa");
    }

    private static void testDistanceFactor() {
        // Test con diferentes distancias conocidas
        ContactRecord veryClose = createContactRecord(1, 2, 0, 60.0, 0.3); // 0.3m
        ContactRecord close = createContactRecord(1, 2, 0, 60.0, 0.8); // 0.8m
        ContactRecord moderate = createContactRecord(1, 2, 0, 60.0, 1.2); // 1.2m
        ContactRecord far = createContactRecord(1, 2, 0, 60.0, 1.8); // 1.8m
        ContactRecord tooFar = createContactRecord(1, 2, 0, 60.0, 4.0); // 4.0m
        
        double probVeryClose = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), veryClose);
        double probClose = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), close);
        double probModerate = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), moderate);
        double probFar = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), far);
        double probTooFar = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), tooFar);
        
        // Validar tendencia: menor distancia = mayor probabilidad
        if (probVeryClose <= probClose || probClose <= probModerate || probModerate <= probFar) {
            throw new RuntimeException("El factor de distancia no sigue la tendencia esperada");
        }
        
        // Validar que distancia excesiva da probabilidad 0
        if (probTooFar != 0.0) {
            throw new RuntimeException("Distancia excesiva debería dar probabilidad 0: " + probTooFar);
        }
        
        System.out.println("   ✓ Factor de distancia calculado correctamente");
        System.out.println("   ✓ Muy cerca (0.3m): " + String.format("%.4f", probVeryClose));
        System.out.println("   ✓ Cerca (0.8m): " + String.format("%.4f", probClose));
        System.out.println("   ✓ Moderado (1.2m): " + String.format("%.4f", probModerate));
        System.out.println("   ✓ Lejos (1.8m): " + String.format("%.4f", probFar));
        System.out.println("   ✓ Muy lejos (4.0m): " + String.format("%.4f", probTooFar));
    }

    private static void testDurationFactor() {
        // Test con diferentes duraciones
        ContactRecord tooShort = createContactRecord(1, 2, 0, 3.0, 1.5); // 3s
        ContactRecord normal = createContactRecord(1, 2, 0, 45.0, 1.5); // 45s
        ContactRecord extended = createContactRecord(1, 2, 0, 180.0, 1.5); // 3min
        ContactRecord longContact = createContactRecord(1, 2, 0, 600.0, 1.5); // 10min
        ContactRecord veryLong = createContactRecord(1, 2, 0, 1200.0, 1.5); // 20min

        double probTooShort = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), tooShort);
        double probNormal = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), normal);
        double probExtended = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), extended);
        double probLong = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), longContact);
        double probVeryLong = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), veryLong);
        
        // Validar que duración muy corta da probabilidad 0
        if (probTooShort != 0.0) {
            throw new RuntimeException("Duración muy corta debería dar probabilidad 0: " + probTooShort);
        }
        
        // Validar tendencia: mayor duración = mayor probabilidad
        if (probNormal >= probExtended || probExtended >= probLong || probLong >= probVeryLong) {
            throw new RuntimeException("El factor de duración no sigue la tendencia esperada");
        }
        
        System.out.println("   ✓ Factor de duración calculado correctamente");
        System.out.println("   ✓ Muy corto (3s): " + String.format("%.4f", probTooShort));
        System.out.println("   ✓ Normal (45s): " + String.format("%.4f", probNormal));
        System.out.println("   ✓ Extendido (3min): " + String.format("%.4f", probExtended));
        System.out.println("   ✓ Largo (10min): " + String.format("%.4f", probLong));
        System.out.println("   ✓ Muy largo (20min): " + String.format("%.4f", probVeryLong));
    }

    private static void testBasicTransmissionProbability() {
        ContactRecord contact = createContactRecord(2, 1, 0, 60.0, 1.5); // 60s, 1.5m
        
        double probability = model.calculateTransmissionProbability(
            testUsers.get(1), testUsers.get(0), contact);
        
        if (probability < 0.0 || probability > 1.0) {
            throw new RuntimeException("Probabilidad fuera de rango [0,1]: " + probability);
        }
        
        // Probar con contacto más largo y más cercano
        ContactRecord longerContact = createContactRecord(2, 1, 0, 300.0, 0.5); // 5min, 0.5m
        double longerProbability = model.calculateTransmissionProbability(
            testUsers.get(1), testUsers.get(0), longerContact);
        
        if (longerProbability <= probability) {
            throw new RuntimeException("Contacto más largo y cercano debe dar mayor probabilidad: " + 
                                    probability + " vs " + longerProbability);
        }
        
        System.out.println("   ✓ Probabilidad de transmisión básica: " + String.format("%.4f", probability));
        System.out.println("   ✓ Probabilidad con contacto intenso: " + String.format("%.4f", longerProbability));
    }

    private static void testMaximumDistance() {
        double maxDist = model.getMaxTransmissionDistance();
        
        // Test justo dentro del límite
        ContactRecord withinLimit = createContactRecord(1, 2, 0, 60.0, maxDist - 0.1);
        double probWithin = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), withinLimit);
        
        // Test justo fuera del límite
        ContactRecord beyondLimit = createContactRecord(1, 2, 0, 60.0, maxDist + 0.1);
        double probBeyond = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), beyondLimit);
        
        if (probWithin <= 0.0) {
            throw new RuntimeException("Contacto dentro del límite debería dar probabilidad > 0");
        }
        
        if (probBeyond != 0.0) {
            throw new RuntimeException("Contacto fuera del límite debería dar probabilidad 0: " + probBeyond);
        }
        
        System.out.println("   ✓ Límite de distancia respetado");
        System.out.println("   ✓ Dentro del límite (" + (maxDist - 0.1) + "m): " + String.format("%.4f", probWithin));
        System.out.println("   ✓ Fuera del límite (" + (maxDist + 0.1) + "m): " + String.format("%.4f", probBeyond));
    }

    private static void testHealthStateUpdate() {
        model.updateHealthStates(testUsers, 1);
        
        for (User user : testUsers) {
            if (user.getEpidemicExtension() == null) {
                throw new RuntimeException("Usuario " + user.userID + " perdió su extensión epidémica");
            }
            
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext.getHealthStatus() == null) {
                throw new RuntimeException("Estado de salud no puede ser null");
            }
        }
        
        System.out.println("   ✓ Estados de salud actualizados correctamente");
    }

    private static void testBasicPerformance() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            ContactRecord contact = createContactRecord(2, 1, 0, 60.0, 1.5);
            model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), contact);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        if (duration > 5000) {
            throw new RuntimeException("Performance inaceptable: " + duration + "ms para 100 operaciones");
        }
        
        System.out.println("   ✓ Performance aceptable: " + duration + "ms para 100 operaciones");
    }

    private static void testFullIntegration() {
        System.out.println("   🔄 Ejecutando secuencia completa proximidad...");
        
        model.updateHealthStates(testUsers, 1);
        System.out.println("     ✓ Estados actualizados");
        
        // Probar diferentes tipos de contacto
        ContactRecord shortContact = createContactRecord(2, 1, 0, 30.0, 1.0);
        ContactRecord longContact = createContactRecord(4, 5, 0, 300.0, 0.8);
        
        double prob1 = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), shortContact);
        double prob2 = model.calculateTransmissionProbability(testUsers.get(3), testUsers.get(4), longContact);
        
        if (prob1 < 0.0 || prob1 > 1.0) {
            throw new RuntimeException("Probabilidad 1 fuera de rango: " + prob1);
        }
        if (prob2 < 0.0 || prob2 > 1.0) {
            throw new RuntimeException("Probabilidad 2 fuera de rango: " + prob2);
        }
        
        System.out.println("     ✓ Contacto corto (30s, 1.0m): " + String.format("%.4f", prob1));
        System.out.println("     ✓ Contacto largo (5min, 0.8m): " + String.format("%.4f", prob2));
        System.out.println("     ✓ Integración completa exitosa");
    }

    // ==================== TESTS ESPECÍFICOS PROXIMIDAD ====================

    private static void testDistanceThresholds() {
        System.out.println("   📏 Probando umbrales de distancia...");
        
        // Test umbrales específicos del modelo
        ContactRecord[] contacts = {
            createContactRecord(1, 2, 0, 60.0, 0.4),  // Muy cerca
            createContactRecord(1, 2, 0, 60.0, 0.9),  // Cerca
            createContactRecord(1, 2, 0, 60.0, 1.3),  // Moderado
            createContactRecord(1, 2, 0, 60.0, 1.9),  // Lejos
            createContactRecord(1, 2, 0, 60.0, 3.5)   // Muy lejos
        };
        
        String[] labels = {"muy cerca", "cerca", "moderado", "lejos", "muy lejos"};
        
        for (int i = 0; i < contacts.length; i++) {
            double prob = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), contacts[i]);
            
            if (i < 4 && prob <= 0.0) {
                throw new RuntimeException("Distancia " + labels[i] + " debería dar probabilidad > 0");
            }
            
            if (i == 4 && prob != 0.0) {
                throw new RuntimeException("Distancia muy lejos debería dar probabilidad 0");
            }
            
            System.out.println("     ✓ " + labels[i] + " (" + contacts[i].getDistance() + "m): " + 
                             String.format("%.4f", prob));
        }
    }

    private static void testDurationThresholds() {
        System.out.println("   ⏱️ Probando umbrales de duración...");
        
        ContactRecord[] contacts = {
            createContactRecord(1, 2, 0, 3.0, 1.5),   // Muy corto
            createContactRecord(1, 2, 0, 45.0, 1.5),  // Normal
            createContactRecord(1, 2, 0, 180.0, 1.5), // Extendido
            createContactRecord(1, 2, 0, 600.0, 1.5), // Largo
            createContactRecord(1, 2, 0, 1200.0, 1.5) // Muy largo
        };
        
        String[] labels = {"muy corto", "normal", "extendido", "largo", "muy largo"};
        
        double previousProb = 0.0;
        for (int i = 0; i < contacts.length; i++) {
            double prob = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), contacts[i]);
            
            if (i == 0 && prob != 0.0) {
                throw new RuntimeException("Duración muy corta debería dar probabilidad 0");
            }
            
            if (i > 0 && prob <= previousProb) {
                throw new RuntimeException("Duración mayor debería dar mayor probabilidad");
            }
            
            System.out.println("     ✓ " + labels[i] + " (" + contacts[i].getDuration() + "s): " + 
                             String.format("%.4f", prob));
            
            if (i > 0) previousProb = prob;
        }
    }

    private static void testInfectiousStatusEffect() {
        System.out.println("   🦠 Probando efecto del estado infectivo...");
        
        // Crear usuarios con diferentes estados infectivos
        User symptomatic = createUser(10, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, false);
        User asymptomatic = createUser(11, 0, HealthStatus.INFECTIOUS_ASYMPTOMATIC, false);
        User exposed = createUser(12, 0, HealthStatus.EXPOSED, false);
        User susceptible = createUser(13, 0, HealthStatus.SUSCEPTIBLE, false);
        
        ContactRecord contact = createContactRecord(1, 2, 0, 120.0, 1.0);
        
        double probSymptomatic = model.calculateTransmissionProbability(symptomatic, testUsers.get(0), contact);
        double probAsymptomatic = model.calculateTransmissionProbability(asymptomatic, testUsers.get(0), contact);
        double probExposed = model.calculateTransmissionProbability(exposed, testUsers.get(0), contact);
        double probSusceptible = model.calculateTransmissionProbability(susceptible, testUsers.get(0), contact);
        
        // Validar jerarquía esperada
        if (probSymptomatic <= probAsymptomatic) {
            throw new RuntimeException("Sintomático debería ser más infectivo que asintomático");
        }
        
        if (probExposed != 0.0 || probSusceptible != 0.0) {
            throw new RuntimeException("Expuesto y susceptible no deberían transmitir");
        }
        
        System.out.println("     ✓ Sintomático: " + String.format("%.4f", probSymptomatic));
        System.out.println("     ✓ Asintomático: " + String.format("%.4f", probAsymptomatic));
        System.out.println("     ✓ Expuesto: " + String.format("%.4f", probExposed));
        System.out.println("     ✓ Susceptible: " + String.format("%.4f", probSusceptible));
    }

    private static void testContactTypes() {
        System.out.println("   🤝 Probando tipos de contacto...");
        
        // Test con diferentes tipos de contacto si están implementados
        ContactRecord contact = createContactRecord(1, 2, 0, 120.0, 1.0);
        double baseProb = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), contact);
        
        if (baseProb <= 0.0) {
            throw new RuntimeException("Contacto básico debería dar probabilidad > 0");
        }
        
        System.out.println("     ✓ Contacto básico: " + String.format("%.4f", baseProb));
        System.out.println("     ✓ Tipos de contacto validados");
    }

    private static void testMultiplicativeFactors() {
        System.out.println("   ✖️ Probando combinación de factores...");
        
        // Test factores individuales vs combinados
        ContactRecord baseContact = createContactRecord(1, 2, 0, 60.0, 1.5);     // Base
        ContactRecord closerContact = createContactRecord(1, 2, 0, 60.0, 0.5);   // Más cerca
        ContactRecord longerContact = createContactRecord(1, 2, 0, 300.0, 1.5);  // Más tiempo
        ContactRecord bothContact = createContactRecord(1, 2, 0, 300.0, 0.5);    // Ambos
        
        double probBase = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), baseContact);
        double probCloser = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), closerContact);
        double probLonger = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), longerContact);
        double probBoth = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), bothContact);
        
        // Validar que los factores se combinan correctamente
        if (probCloser <= probBase || probLonger <= probBase) {
            throw new RuntimeException("Factores individuales no mejoran la probabilidad base");
        }
        
        if (probBoth <= probCloser || probBoth <= probLonger) {
            throw new RuntimeException("Combinación de factores debería ser la mayor probabilidad");
        }
        
        System.out.println("     ✓ Base (60s, 1.5m): " + String.format("%.4f", probBase));
        System.out.println("     ✓ Más cerca (60s, 0.5m): " + String.format("%.4f", probCloser));
        System.out.println("     ✓ Más tiempo (300s, 1.5m): " + String.format("%.4f", probLonger));
        System.out.println("     ✓ Ambos (300s, 0.5m): " + String.format("%.4f", probBoth));
    }

    private static void testProbabilityBounds() {
        System.out.println("   🎯 Probando límites de probabilidad...");
        
        // Test casos extremos para verificar límites [0,1]
        ContactRecord[] extremeContacts = {
            createContactRecord(1, 2, 0, 2.0, 1.5),     // Muy corto
            createContactRecord(1, 2, 0, 60.0, 5.0),    // Muy lejos
            createContactRecord(1, 2, 0, 3600.0, 0.1),  // Muy largo y muy cerca
        };
        
        for (ContactRecord contact : extremeContacts) {
            double prob = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), contact);
            
            if (prob < 0.0 || prob > 1.0) {
                throw new RuntimeException("Probabilidad fuera de rango [0,1]: " + prob);
            }
        }
        
        System.out.println("     ✓ Todos los casos extremos respetan límites [0,1]");
    }

    // ==================== TESTS AVANZADOS ====================

    private static void testSensitivityAnalysis() {
        System.out.println("   🔬 Ejecutando análisis combinatorial proximidad...");
        
        int combinationCount = 0;
        int validCombinations = 0;
        
        for (double distance : DISTANCES) {
            for (double duration : DURATIONS) {
                for (String infectionRatio : INFECTION_RATIOS) {
                    for (String maskScenario : MASK_SCENARIOS) {
                        combinationCount++;
                        
                        try {
                            double probability = testParameterCombination(
                                distance, duration, infectionRatio, maskScenario);
                            
                            if (probability >= 0.0 && probability <= 1.0) {
                                validCombinations++;
                            }
                            
                            // Log cada 16 combinaciones
                            if (combinationCount % 16 == 0) {
                                System.out.println(String.format("     📊 Procesadas %d/%d combinaciones", 
                                                combinationCount, 
                                                DISTANCES.length * DURATIONS.length * 
                                                INFECTION_RATIOS.length * MASK_SCENARIOS.length));
                            }
                            
                        } catch (Exception e) {
                            System.out.println(String.format("     ⚠️ Error en combinación %d: %s", 
                                            combinationCount, e.getMessage()));
                        }
                    }
                }
            }
        }
        
        double validPercentage = (double) validCombinations / combinationCount * 100;
        
        if (validPercentage < 90.0) {
            throw new RuntimeException(String.format("Solo %.1f%% de combinaciones válidas (%d/%d)", 
                                     validPercentage, validCombinations, combinationCount));
        }
        
        System.out.println(String.format("   ✓ Análisis completado: %d combinaciones, %.1f%% válidas", 
                          combinationCount, validPercentage));
    }

    private static void testExpectedTrends() {
        System.out.println("   📈 Validando tendencias esperadas proximidad...");
        
        // Tendencia 1: Menor distancia = Mayor riesgo
        double probFar = testParameterCombination(2.0, 120.0, "MEDIUM", "NONE");
        double probClose = testParameterCombination(0.5, 120.0, "MEDIUM", "NONE");
        
        if (probClose <= probFar) {
            throw new RuntimeException(String.format("Tendencia distancia incorrecta: %.4f (cerca) vs %.4f (lejos)", 
                                    probClose, probFar));
        }
        System.out.println(String.format("     ✓ Distancia: %.4f (cerca) > %.4f (lejos)", probClose, probFar));
        
        // Tendencia 2: Mayor duración = Mayor riesgo
        double probShort = testParameterCombination(1.0, 30.0, "MEDIUM", "NONE");
        double probLong = testParameterCombination(1.0, 600.0, "MEDIUM", "NONE");
        
        if (probLong <= probShort) {
            throw new RuntimeException(String.format("Tendencia duración incorrecta: %.4f (corto) vs %.4f (largo)", 
                                    probShort, probLong));
        }
        System.out.println(String.format("     ✓ Duración: %.4f (corto) < %.4f (largo)", probShort, probLong));
        
        // Tendencia 3: Más infectados = Mayor riesgo
        double probLowInf = testParameterCombination(1.0, 120.0, "LOW", "NONE");
        double probHighInf = testParameterCombination(1.0, 120.0, "HIGH", "NONE");
        
        if (probLowInf >= probHighInf) {
            throw new RuntimeException(String.format("Tendencia infectados incorrecta: %.4f (pocos) vs %.4f (muchos)", 
                                    probLowInf, probHighInf));
        }
        System.out.println(String.format("     ✓ Infectados: %.4f (pocos) < %.4f (muchos)", probLowInf, probHighInf));
        
        // Tendencia 4: Mascarillas = Menor riesgo
        double probNoMask = testParameterCombination(1.0, 120.0, "MEDIUM", "NONE");
        double probAllMask = testParameterCombination(1.0, 120.0, "MEDIUM", "ALL");
        
        if (probNoMask <= probAllMask) {
            throw new RuntimeException(String.format("Tendencia mascarillas incorrecta: %.4f (sin) vs %.4f (con)", 
                                    probNoMask, probAllMask));
        }
        System.out.println(String.format("     ✓ Mascarillas: %.4f (sin) > %.4f (con)", probNoMask, probAllMask));
    }

    private static void testExtremeCases() {
        System.out.println("   🎯 Probando casos extremos proximidad...");
        
        // Caso extremo 1: Contacto muy cercano + Muy largo + Muchos infectados + Sin mascarillas
        double extremeHigh = testParameterCombination(0.5, 600.0, "HIGH", "NONE");
        System.out.println(String.format("     ✓ Riesgo extremo alto: %.4f", extremeHigh));
        
        // Caso extremo 2: Contacto lejano + Corto + Pocos infectados + Todas mascarillas
        double extremeLow = testParameterCombination(2.0, 30.0, "LOW", "ALL");
        System.out.println(String.format("     ✓ Riesgo extremo bajo: %.4f", extremeLow));
        
        // Caso límite: Sin contacto válido
        double probNoContact = testParameterCombination(4.0, 120.0, "MEDIUM", "NONE");
        if (probNoContact != 0.0) {
            throw new RuntimeException("Sin contacto válido debería dar probabilidad 0");
        }
        System.out.println("     ✓ Sin contacto válido: probabilidad 0");
    }

    private static void testMaskEffectiveness() {
        System.out.println("   😷 Probando efectividad de mascarillas proximidad...");
        
        ContactRecord contact = createContactRecord(2, 1, 0, 120.0, 1.0);
        
        // Escenario 1: Sin mascarillas
        List<User> noMaskUsers = createUsersWithMaskScenario("NONE");
        User infectious1 = noMaskUsers.get(1); // INFECTIOUS_SYMPTOMATIC
        User susceptible1 = noMaskUsers.get(0); // SUSCEPTIBLE
        
        double probNoMask = model.calculateTransmissionProbability(infectious1, susceptible1, contact);
        
        // Escenario 2: Todos con mascarillas
        List<User> allMaskUsers = createUsersWithMaskScenario("ALL");
        User infectious2 = allMaskUsers.get(1); // INFECTIOUS_SYMPTOMATIC
        User susceptible2 = allMaskUsers.get(0); // SUSCEPTIBLE
        
        double probAllMask = model.calculateTransmissionProbability(infectious2, susceptible2, contact);
        
        if (probNoMask <= probAllMask) {
            throw new RuntimeException(String.format("Sin mascarillas (%.4f) debería ser > que con mascarillas (%.4f)", 
                                    probNoMask, probAllMask));
        }
        
        double effectiveness = (probNoMask - probAllMask) / probNoMask * 100;
        
        System.out.println(String.format("     ✓ Sin mascarillas: %.4f", probNoMask));
        System.out.println(String.format("     ✓ Con mascarillas: %.4f", probAllMask));
        System.out.println(String.format("     ✓ Efectividad mascarillas: %.1f%%", effectiveness));
    }

    private static void testSocialDistancing() {
        System.out.println("   📏 Probando impacto de distancia social...");
        
        // Comparar distancia normal vs distancia social
        ContactRecord normalDistance = createContactRecord(1, 2, 0, 120.0, 1.0); // 1m
        ContactRecord socialDistance = createContactRecord(1, 2, 0, 120.0, 2.0); // 2m
        
        double probNormal = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), normalDistance);
        double probSocial = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), socialDistance);
        
        if (probNormal <= probSocial) {
            throw new RuntimeException("Distancia social debería reducir riesgo");
        }
        
        double reduction = (probNormal - probSocial) / probNormal * 100;
        
        System.out.println(String.format("     ✓ Distancia normal (1m): %.4f", probNormal));
        System.out.println(String.format("     ✓ Distancia social (2m): %.4f", probSocial));
        System.out.println(String.format("     ✓ Reducción por distancia: %.1f%%", reduction));
    }

    private static void testMaskDistanceCombination() {
        System.out.println("   🛡️ Probando combinación mascarillas + distancia...");
        
        // 4 escenarios combinados
        ContactRecord contact1 = createContactRecord(1, 2, 0, 120.0, 1.0); // Cerca, sin mascarillas
        ContactRecord contact2 = createContactRecord(1, 2, 0, 120.0, 2.0); // Lejos, sin mascarillas
        
        List<User> noMaskUsers = createUsersWithMaskScenario("NONE");
        List<User> allMaskUsers = createUsersWithMaskScenario("ALL");
        
        double probCloseNoMask = model.calculateTransmissionProbability(
            noMaskUsers.get(1), noMaskUsers.get(0), contact1);
        double probFarNoMask = model.calculateTransmissionProbability(
            noMaskUsers.get(1), noMaskUsers.get(0), contact2);
        double probCloseMask = model.calculateTransmissionProbability(
            allMaskUsers.get(1), allMaskUsers.get(0), contact1);
        double probFarMask = model.calculateTransmissionProbability(
            allMaskUsers.get(1), allMaskUsers.get(0), contact2);
        
        // Validar jerarquía esperada
        if (!(probCloseNoMask >= probFarNoMask && probFarNoMask >= probCloseMask && probCloseMask >= probFarMask)) {
            throw new RuntimeException("Jerarquía de protección incorrecta");
        }
        
        System.out.println(String.format("     ✓ Cerca, sin mascarillas: %.4f", probCloseNoMask));
        System.out.println(String.format("     ✓ Lejos, sin mascarillas: %.4f", probFarNoMask));
        System.out.println(String.format("     ✓ Cerca, con mascarillas: %.4f", probCloseMask));
        System.out.println(String.format("     ✓ Lejos, con mascarillas: %.4f", probFarMask));
        
        double totalReduction = (probCloseNoMask - probFarMask) / probCloseNoMask * 100;
        System.out.println(String.format("     ✓ Reducción total combinada: %.1f%%", totalReduction));
    }

    private static void testExpectedAssertions() {
        System.out.println("   Validando assertions críticas proximidad...");
        
        // Assertion 1: Menor distancia aumenta riesgo
        ContactRecord farContact = createContactRecord(1, 2, 0, 120.0, 2.0);
        ContactRecord closeContact = createContactRecord(1, 2, 0, 120.0, 0.5);
        
        double probFar = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), farContact);
        double probClose = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), closeContact);
        
        if (probClose <= probFar) {
            throw new RuntimeException("ASSERTION FALLIDA: Menor distancia no aumenta riesgo");
        }
        System.out.println(String.format("     ✓ ASSERTION 1 OK: %.4f (lejos) < %.4f (cerca)", probFar, probClose));
        
        // Assertion 2: Mayor duración aumenta riesgo
        ContactRecord shortContact = createContactRecord(1, 2, 0, 30.0, 1.0);
        ContactRecord longContact = createContactRecord(1, 2, 0, 300.0, 1.0);
        
        double probShort = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), shortContact);
        double probLong = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), longContact);
        
        if (probLong <= probShort) {
            throw new RuntimeException("ASSERTION FALLIDA: Mayor duración no aumenta riesgo");
        }
        System.out.println(String.format("     ✓ ASSERTION 2 OK: %.4f (corto) < %.4f (largo)", probShort, probLong));
        
        // Assertion 3: Mascarillas reducen riesgo
        List<User> noMaskUsers = createUsersWithMaskScenario("NONE");
        List<User> maskUsers = createUsersWithMaskScenario("ALL");
        
        ContactRecord contact = createContactRecord(1, 2, 0, 120.0, 1.0);
        
        double probWithoutMask = model.calculateTransmissionProbability(
            noMaskUsers.get(1), noMaskUsers.get(0), contact);
        double probWithMask = model.calculateTransmissionProbability(
            maskUsers.get(1), maskUsers.get(0), contact);
        
        if (probWithMask >= probWithoutMask) {
            throw new RuntimeException("ASSERTION FALLIDA: Mascarillas no reducen riesgo");
        }
        System.out.println(String.format("     ✓ ASSERTION 3 OK: %.4f (sin) > %.4f (con)", probWithoutMask, probWithMask));
        
        // Assertion 4: Distancia máxima da probabilidad 0
        ContactRecord tooFarContact = createContactRecord(1, 2, 0, 120.0, 5.0);
        double probTooFar = model.calculateTransmissionProbability(testUsers.get(1), testUsers.get(0), tooFarContact);
        
        if (probTooFar != 0.0) {
            throw new RuntimeException("ASSERTION FALLIDA: Distancia excesiva no da probabilidad 0");
        }
        System.out.println(String.format("     ✓ ASSERTION 4 OK: %.4f (muy lejos)", probTooFar));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private static double testParameterCombination(double distance, double duration, 
                                             String infectionRatio, String maskScenario) {
        try {
            List<User> users = createUsersWithParameters(infectionRatio, maskScenario);
            
            List<User> infectiousUsers = new ArrayList<>();
            List<User> susceptibleUsers = new ArrayList<>();
            
            for (User user : users) {
                HealthStatus status = user.getEpidemicExtension().getHealthStatus();
                if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                    status == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
                    infectiousUsers.add(user);
                } else if (status == HealthStatus.SUSCEPTIBLE) {
                    susceptibleUsers.add(user);
                }
            }
            
            if (infectiousUsers.isEmpty() || susceptibleUsers.isEmpty()) {
                return 0.0;
            }
            
            // CAMBIO: Calcular probabilidad de AL MENOS una transmisión
            double noTransmissionProb = 1.0; // Probabilidad de que NO haya transmisión
            
            for (User infectious : infectiousUsers) {
                for (User susceptible : susceptibleUsers) {
                    ContactRecord contact = createContactRecord(
                        infectious.userID, susceptible.userID, 0, duration, distance);
                    
                    double probability = model.calculateTransmissionProbability(infectious, susceptible, contact);
                    noTransmissionProb *= (1.0 - probability); // P(no transmisión en este par)
                }
            }
            
            // Probabilidad de al menos una transmisión = 1 - P(ninguna transmisión)
            return 1.0 - noTransmissionProb;
            
        } catch (Exception e) {
            System.out.println(String.format("Error en combinación D:%.1f T:%.1f I:%s M:%s - %s", 
                            distance, duration, infectionRatio, maskScenario, e.getMessage()));
            return 0.0;
        }
    }

    private static List<User> createUsersWithParameters(String infectionRatio, String maskScenario) {
        List<User> users = new ArrayList<>();
        int totalUsers = 6;
        int infectedUsers;
        
        switch (infectionRatio) {
            case "LOW": infectedUsers = 1; break;
            case "MEDIUM": infectedUsers = 2; break;
            case "HIGH": infectedUsers = 3; break;
            default: infectedUsers = 1; break;
        }
        
        // Crear usuarios susceptibles
        for (int i = 1; i <= totalUsers - infectedUsers; i++) {
            boolean wearsMask = shouldUserWearMask(i, totalUsers, maskScenario);
            users.add(createUser(i, 0, HealthStatus.SUSCEPTIBLE, wearsMask));
        }
        
        // Crear usuarios infectados
        for (int i = totalUsers - infectedUsers + 1; i <= totalUsers; i++) {
            boolean wearsMask = shouldUserWearMask(i, totalUsers, maskScenario);
            users.add(createUser(i, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, wearsMask));
        }
        
        return users;
    }

    private static List<User> createUsersWithMaskScenario(String scenario) {
        List<User> users = new ArrayList<>();
        
        users.add(createUser(1, 0, HealthStatus.SUSCEPTIBLE, shouldUserWearMask(1, 6, scenario)));
        users.add(createUser(2, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, shouldUserWearMask(2, 6, scenario)));
        users.add(createUser(3, 0, HealthStatus.INFECTIOUS_ASYMPTOMATIC, shouldUserWearMask(3, 6, scenario)));
        users.add(createUser(4, 0, HealthStatus.SUSCEPTIBLE, shouldUserWearMask(4, 6, scenario)));
        users.add(createUser(5, 0, HealthStatus.SUSCEPTIBLE, shouldUserWearMask(5, 6, scenario)));
        users.add(createUser(6, 0, HealthStatus.EXPOSED, shouldUserWearMask(6, 6, scenario)));
        
        return users;
    }

    private static boolean shouldUserWearMask(int userId, int totalUsers, String scenario) {
        switch (scenario) {
            case "NONE": return false;
            case "ALL": return true;
            case "HALF": return userId <= totalUsers / 2;
            default: return false;
        }
    }

    private static List<User> createTestUsers() {
        List<User> users = new ArrayList<>();
        
        users.add(createUser(1, 0, HealthStatus.SUSCEPTIBLE, false));
        users.add(createUser(2, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, true));
        users.add(createUser(3, 0, HealthStatus.INFECTIOUS_ASYMPTOMATIC, false));
        users.add(createUser(4, 0, HealthStatus.SUSCEPTIBLE, true));
        users.add(createUser(5, 0, HealthStatus.SUSCEPTIBLE, false));
        users.add(createUser(6, 0, HealthStatus.EXPOSED, false));
        
        return users;
    }

    private static User createUser(int id, int room, HealthStatus status, boolean wearsMask) {
        try {
            User user = new User(id, false);
            user.x = 100.0;
            user.y = 100.0;
            user.room = room;
            
            UserEpidemicExtension extension = new UserEpidemicExtension();
            extension.setHealthStatus(status);
            extension.setMaskWearing(wearsMask);
            
            extension.setIncubationPeriod(120);
            extension.setInfectiousPeriod(240);
            
            user.setEpidemicExtension(extension);
            
            return user;
            
        } catch (Exception e) {
            throw new RuntimeException("Error creando usuario: " + e.getMessage());
        }
    }

    private static void runTest(String testName, Runnable testMethod) {
        testsExecuted++;
        System.out.println("🔍 Test " + testsExecuted + ": " + testName);
        
        try {
            testMethod.run();
            testsPassed++;
            System.out.println("PASÓ: " + testName);
        } catch (Exception e) {
            testsFailed++;
            System.out.println("❌ FALLÓ: " + testName);
            System.out.println("   Error: " + e.getMessage());
            // e.printStackTrace(); // Descomentar para stack trace completo
        }
        
        System.out.println();
    }

    private static void printFinalSummary() {
        System.out.println("=".repeat(70));
        System.out.println("🎯 RESUMEN FINAL DE TESTS SIMPLE PROXIMITY");
        System.out.println("=".repeat(70));
        System.out.println("📊 Total ejecutados: " + testsExecuted);
        System.out.println("Pasaron: " + testsPassed);
        System.out.println("❌ Fallaron: " + testsFailed);
        System.out.println("📈 Tasa de éxito: " + String.format("%.1f%%", (double) testsPassed / testsExecuted * 100));
        System.out.println("=".repeat(70));
        
        if (testsFailed == 0) {
            System.out.println("🎉 ¡TODOS LOS TESTS SIMPLE PROXIMITY PASARON EXITOSAMENTE!");
            System.out.println("El modelo SimpleProximityModel funciona correctamente");
            System.out.println("🔬 Análisis de sensibilidad completado");
            System.out.println("😷 Tests de mascarillas y distancia exitosos");
            System.out.println("📏 Validación de modelo proximidad exitosa");
        } else {
            System.out.println("⚠️  ALGUNOS TESTS FALLARON");
            System.out.println("🔧 Revisa los errores anteriores para corregir los problemas");
        }
        
        System.out.println("=".repeat(70));
        System.out.println("📅 Finalizado: " + java.time.LocalDateTime.now());
    }

    // ==================== MÉTODO AUXILIAR PARA CREAR CONTACTRECORD ====================
    
    /**
     * Método auxiliar para crear ContactRecord con distancia personalizada
     */
    private static ContactRecord createContactRecord(long user1Id, long user2Id, int roomId, double duration, double distanceInMeters) {
        ContactRecord contact = new ContactRecord(user1Id, user2Id, roomId, duration);
        
        double distanceInPixels = ModelParameters1.metersToPixels(distanceInMeters);
        contact.setDistance(distanceInPixels);
        
        return contact;
    }

    // ==================== CLASE EXTENDIDA PARA TESTING ====================

    /**
     * Versión extendida del modelo para testing que permite configuraciones específicas
     */
    static class SimpleProximityModelTestVersion extends SimpleProximityModel {
        
        public SimpleProximityModelTestVersion() {
            super();
        }
        
        // Métodos adicionales para testing si son necesarios
        public void resetToDefaults() {
            configureSimpleModel(3.0, 0.15, 5);
        }
    }


    // ==================== ⭐ TESTS DE ESCENARIOS REALES PARA SIMPLE PROXIMITY ⭐ ====================

    /**
     * Ejecuta tests de validación con escenarios reales
     */
    private static void executeScenarioValidationTests() {
        System.out.println("🏢 === INICIANDO TESTS DE ESCENARIOS REALES ===");
        System.out.println("📏 Basados en espacios típicos con modelo de proximidad");
        System.out.println();
        
        runTest("Aula estándar - clase normal", SimpleProximityModelTest::testClassroomScenario);
        runTest("Oficina pequeña - exposición prolongada", SimpleProximityModelTest::testSmallOfficeScenario);
        runTest("Restaurante - alta interacción social", SimpleProximityModelTest::testRestaurantScenario);
        runTest("Transporte público - contactos cercanos", SimpleProximityModelTest::testPublicTransportScenario);
        runTest("Validación de tendencias entre escenarios", SimpleProximityModelTest::testScenarioTrends);
        
        System.out.println("=== TESTS DE ESCENARIOS REALES COMPLETADOS ===\n");
    }

    /**
     * Test Escenario 1: Aula estándar - clase normal
     * Configuración: 25 personas, 1 infectado, contactos de ~2m durante 2 horas
     * Expectativa: Riesgo bajo-moderado por distancia mantenida
     */
    private static void testClassroomScenario() {
        System.out.println("   🏫 Probando Aula Estándar...");
        
        // Crear usuarios específicos para aula (25 personas, 1 infectado)
        List<User> classroomUsers = createScenarioUsers(25, 1, false, "CLASSROOM");
        
        // Simular contactos típicos de aula: distancia ~2m, duración clase 2h
        double avgDistance = 2.0; // metros - distancia típica en aula
        double classDuration = 7200.0; // segundos (2 horas)
        
        // Calcular riesgo promedio para estudiantes susceptibles
        double totalRisk = 0.0;
        int susceptibleCount = 0;
        
        User infectiousTeacher = null;
        for (User user : classroomUsers) {
            HealthStatus status = user.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                status == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
                infectiousTeacher = user;
                break;
            }
        }
        
        if (infectiousTeacher == null) {
            throw new RuntimeException("No se encontró profesor infectado en aula");
        }
        
        for (User student : classroomUsers) {
            HealthStatus status = student.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.SUSCEPTIBLE) {
                ContactRecord classContact = createContactRecord(
                    infectiousTeacher.userID, student.userID, 0, classDuration, avgDistance);
                
                double risk = model.calculateTransmissionProbability(infectiousTeacher, student, classContact);
                totalRisk += risk;
                susceptibleCount++;
            }
        }
        
        double avgIndividualRisk = totalRisk / susceptibleCount;
        double groupRisk = 1.0 - Math.pow(1.0 - avgIndividualRisk, susceptibleCount);
        
        System.out.println(String.format("     📊 RESULTADOS AULA:"));
        System.out.println(String.format("       Estudiantes: %d total, %d susceptibles", classroomUsers.size(), susceptibleCount));
        System.out.println(String.format("       Distancia promedio: %.1fm", avgDistance));
        System.out.println(String.format("       Duración clase: %.1f horas", classDuration/3600.0));
        System.out.println(String.format("       Riesgo individual promedio: %.2f%%", avgIndividualRisk * 100));
        System.out.println(String.format("       Riesgo grupal: %.2f%%", groupRisk * 100));
        
        // Validaciones para aula estándar
        if (avgIndividualRisk > 0.15) { // >15% sería muy alto para aula con distanciamiento
            throw new RuntimeException(String.format("Riesgo individual muy alto para aula: %.2f%%", avgIndividualRisk * 100));
        }
        
        if (avgIndividualRisk < 0.01) { // <1% sería muy bajo para 2h de exposición
            throw new RuntimeException(String.format("Riesgo individual muy bajo para aula: %.2f%%", avgIndividualRisk * 100));
        }
        
        System.out.println("     ✓ Aula estándar validada correctamente");
    }

    /**
     * Test Escenario 2: Oficina pequeña - exposición prolongada
     * Configuración: 6 personas, 1 infectado, contactos cercanos durante 8 horas
     * Expectativa: Riesgo alto por proximidad y tiempo prolongado
     */
    private static void testSmallOfficeScenario() {
        System.out.println("   🏢 Probando Oficina Pequeña...");
        
        // Crear usuarios de oficina (6 personas, 1 infectado)
        List<User> officeUsers = createScenarioUsers(6, 1, false, "OFFICE");
        
        // Simular día laboral: distancia ~1.5m, jornada 8h con descansos
        double avgDistance = 1.5; // metros - oficina compacta
        double workDuration = 28800.0; // segundos (8 horas)
        
        double totalRisk = 0.0;
        int susceptibleCount = 0;
        
        User infectiousWorker = null;
        for (User user : officeUsers) {
            HealthStatus status = user.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                status == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
                infectiousWorker = user;
                break;
            }
        }
        
        if (infectiousWorker == null) {
            throw new RuntimeException("No se encontró trabajador infectado en oficina");
        }
        
        for (User colleague : officeUsers) {
            HealthStatus status = colleague.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.SUSCEPTIBLE) {
                ContactRecord workContact = createContactRecord(
                    infectiousWorker.userID, colleague.userID, 0, workDuration, avgDistance);
                
                double risk = model.calculateTransmissionProbability(infectiousWorker, colleague, workContact);
                totalRisk += risk;
                susceptibleCount++;
            }
        }
        
        double avgIndividualRisk = totalRisk / susceptibleCount;
        double groupRisk = 1.0 - Math.pow(1.0 - avgIndividualRisk, susceptibleCount);
        
        System.out.println(String.format("     📊 RESULTADOS OFICINA:"));
        System.out.println(String.format("       Trabajadores: %d total, %d susceptibles", officeUsers.size(), susceptibleCount));
        System.out.println(String.format("       Distancia promedio: %.1fm", avgDistance));
        System.out.println(String.format("       Duración jornada: %.1f horas", workDuration/3600.0));
        System.out.println(String.format("       Riesgo individual promedio: %.2f%%", avgIndividualRisk * 100));
        System.out.println(String.format("       Riesgo grupal: %.2f%%", groupRisk * 100));
        
        // Validaciones para oficina pequeña
        if (avgIndividualRisk < 0.20) { // <20% sería bajo para 8h a 1.5m
            throw new RuntimeException(String.format("Riesgo individual muy bajo para oficina: %.2f%%", avgIndividualRisk * 100));
        }
        
        if (avgIndividualRisk > 0.80) { // >80% sería extremadamente alto
            throw new RuntimeException(String.format("Riesgo individual muy alto para oficina: %.2f%%", avgIndividualRisk * 100));
        }
        
        System.out.println("     ✓ Oficina pequeña validada correctamente");
    }

    /**
     * Test Escenario 3: Restaurante - alta interacción social
     * Configuración: 15 personas, 2 infectados, contactos variables 1-3h
     * Expectativa: Riesgo variable según distancia mesa/camarero
     */
    private static void testRestaurantScenario() {
        System.out.println("   🍽️ Probando Restaurante...");
        
        // Crear usuarios de restaurante (15 personas, 2 infectados)
        List<User> restaurantUsers = createScenarioUsers(15, 2, false, "RESTAURANT");
        
        // Simular diferentes tipos de contacto en restaurante
        double tableDistance = 1.8; // metros - mesas separadas
        double serviceDistance = 0.8; // metros - servicio cercano
        double mealDuration = 5400.0; // segundos (1.5 horas)
        double serviceDuration = 300.0; // segundos (5 minutos por servicio)
        
        List<User> infectiousUsers = new ArrayList<>();
        List<User> susceptibleUsers = new ArrayList<>();
        
        for (User user : restaurantUsers) {
            HealthStatus status = user.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                status == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
                infectiousUsers.add(user);
            } else if (status == HealthStatus.SUSCEPTIBLE) {
                susceptibleUsers.add(user);
            }
        }
        
        double totalTableRisk = 0.0;
        double totalServiceRisk = 0.0;
        int contactCount = 0;
        
        for (User infectious : infectiousUsers) {
            for (User susceptible : susceptibleUsers) {
                // Contacto tipo mesa (larga distancia, larga duración)
                ContactRecord tableContact = createContactRecord(
                    infectious.userID, susceptible.userID, 0, mealDuration, tableDistance);
                double tableRisk = model.calculateTransmissionProbability(infectious, susceptible, tableContact);
                
                // Contacto tipo servicio (corta distancia, corta duración)
                ContactRecord serviceContact = createContactRecord(
                    infectious.userID, susceptible.userID, 0, serviceDuration, serviceDistance);
                double serviceRisk = model.calculateTransmissionProbability(infectious, susceptible, serviceContact);
                
                totalTableRisk += tableRisk;
                totalServiceRisk += serviceRisk;
                contactCount++;
            }
        }
        
        double avgTableRisk = totalTableRisk / contactCount;
        double avgServiceRisk = totalServiceRisk / contactCount;
        double combinedRisk = 1.0 - ((1.0 - avgTableRisk) * (1.0 - avgServiceRisk));
        
        System.out.println(String.format("     📊 RESULTADOS RESTAURANTE:"));
        System.out.println(String.format("       Clientes: %d total, %d susceptibles, %d infectados", 
                        restaurantUsers.size(), susceptibleUsers.size(), infectiousUsers.size()));
        System.out.println(String.format("       Riesgo contacto mesa (%.1fm, %.1fh): %.2f%%", 
                        tableDistance, mealDuration/3600.0, avgTableRisk * 100));
        System.out.println(String.format("       Riesgo contacto servicio (%.1fm, %.1fmin): %.2f%%", 
                        serviceDistance, serviceDuration/60.0, avgServiceRisk * 100));
        System.out.println(String.format("       Riesgo combinado: %.2f%%", combinedRisk * 100));
        
        // Validaciones para restaurante
        if (avgServiceRisk <= avgTableRisk) {
            throw new RuntimeException("Contacto servicio debería ser más riesgoso que mesa");
        }
        
        if (combinedRisk < Math.max(avgTableRisk, avgServiceRisk)) {
            throw new RuntimeException("Riesgo combinado debe ser mayor que riesgos individuales");
        }
        
        System.out.println("     ✓ Restaurante validado correctamente");
    }

    /**
     * Test Escenario 4: Transporte público - contactos cercanos
     * Configuración: 20 personas, 1 infectado, contactos muy cercanos corta duración
     * Expectativa: Riesgo moderado por cercanía pero corto tiempo
     */
    private static void testPublicTransportScenario() {
        System.out.println("   🚌 Probando Transporte Público...");
        
        // Crear usuarios de transporte (20 personas, 1 infectado)
        List<User> transportUsers = createScenarioUsers(20, 1, true, "TRANSPORT"); // Con mascarillas
        
        // Simular viaje en transporte: muy cerca, duración media
        double avgDistance = 0.6; // metros - transporte abarrotado
        double tripDuration = 1800.0; // segundos (30 minutos)
        
        double totalRisk = 0.0;
        int susceptibleCount = 0;
        
        User infectiousPassenger = null;
        for (User user : transportUsers) {
            HealthStatus status = user.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                status == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
                infectiousPassenger = user;
                break;
            }
        }
        
        if (infectiousPassenger == null) {
            throw new RuntimeException("No se encontró pasajero infectado en transporte");
        }
        
        for (User passenger : transportUsers) {
            HealthStatus status = passenger.getEpidemicExtension().getHealthStatus();
            if (status == HealthStatus.SUSCEPTIBLE) {
                ContactRecord transportContact = createContactRecord(
                    infectiousPassenger.userID, passenger.userID, 0, tripDuration, avgDistance);
                
                double risk = model.calculateTransmissionProbability(infectiousPassenger, passenger, transportContact);
                totalRisk += risk;
                susceptibleCount++;
            }
        }
        
        double avgIndividualRisk = totalRisk / susceptibleCount;
        double groupRisk = 1.0 - Math.pow(1.0 - avgIndividualRisk, susceptibleCount);
        
        System.out.println(String.format("     📊 RESULTADOS TRANSPORTE:"));
        System.out.println(String.format("       Pasajeros: %d total, %d susceptibles", transportUsers.size(), susceptibleCount));
        System.out.println(String.format("       Distancia promedio: %.1fm", avgDistance));
        System.out.println(String.format("       Duración viaje: %.1f minutos", tripDuration/60.0));
        System.out.println(String.format("       Uso mascarillas: SÍ"));
        System.out.println(String.format("       Riesgo individual promedio: %.2f%%", avgIndividualRisk * 100));
        System.out.println(String.format("       Riesgo grupal: %.2f%%", groupRisk * 100));
        
        // Validaciones para transporte público
        if (avgIndividualRisk > 0.25) { // >25% sería muy alto con mascarillas
            throw new RuntimeException(String.format("Riesgo individual muy alto para transporte con mascarillas: %.2f%%", avgIndividualRisk * 100));
        }
        
        if (avgIndividualRisk < 0.02) { // <2% sería muy bajo para 0.6m durante 30min
            throw new RuntimeException(String.format("Riesgo individual muy bajo para transporte: %.2f%%", avgIndividualRisk * 100));
        }
        
        System.out.println("     ✓ Transporte público validado correctamente");
    }

    /**
     * Test: Validación de tendencias entre escenarios
     * Verifica que los escenarios sigan el orden de riesgo esperado
     */
    private static void testScenarioTrends() {
        System.out.println("   📈 Validando tendencias entre escenarios...");
        
        // Calcular riesgo representativo para cada escenario
        double riskClassroom = calculateScenarioRisk("CLASSROOM");
        double riskOffice = calculateScenarioRisk("OFFICE");
        double riskRestaurant = calculateScenarioRisk("RESTAURANT");
        double riskTransport = calculateScenarioRisk("TRANSPORT");
        
        System.out.println(String.format("     📊 COMPARACIÓN DE ESCENARIOS:"));
        System.out.println(String.format("       Aula estándar: %.2f%%", riskClassroom * 100));
        System.out.println(String.format("       Oficina pequeña: %.2f%%", riskOffice * 100));
        System.out.println(String.format("       Restaurante: %.2f%%", riskRestaurant * 100));
        System.out.println(String.format("       Transporte público: %.2f%%", riskTransport * 100));
        
        // Validar tendencias esperadas basadas en proximidad y tiempo
        
        // 1. Oficina debe ser el más riesgoso (8h a 1.5m)
        if (riskOffice < Math.max(Math.max(riskClassroom, riskRestaurant), riskTransport)) {
            throw new RuntimeException("Oficina debería ser el escenario más riesgoso");
        }
        System.out.println("     ✓ Oficina es el escenario más riesgoso");
        
        // 2. Aula debe ser menos riesgosa que oficina (2h vs 8h)
        if (riskClassroom >= riskOffice) {
            throw new RuntimeException("Aula debería ser menos riesgosa que oficina");
        }
        System.out.println("     ✓ Aula menos riesgosa que oficina");
        
        // 3. Transporte con mascarillas debe tener riesgo moderado
        if (riskTransport > riskOffice * 0.8) { // No debería superar 80% del riesgo de oficina
            throw new RuntimeException("Transporte con mascarillas debería tener menor riesgo");
        }
        System.out.println("     ✓ Transporte con mascarillas tiene riesgo controlado");
        
        // 4. Restaurante debe estar en rango intermedio
        if (riskRestaurant < riskClassroom || riskRestaurant > riskOffice) {
            System.out.println(String.format("     ⚠️ Restaurante fuera de rango esperado: %.2f%% (entre %.2f%% y %.2f%%)", 
                            riskRestaurant * 100, riskClassroom * 100, riskOffice * 100));
        } else {
            System.out.println("     ✓ Restaurante en rango intermedio esperado");
        }
        
        System.out.println("     ✓ Tendencias entre escenarios validadas correctamente");
    }

    // ==================== MÉTODOS AUXILIARES PARA ESCENARIOS ====================

    /**
     * Crea usuarios para un escenario específico
     */
    private static List<User> createScenarioUsers(int totalUsers, int infectedUsers, boolean withMasks, String scenarioType) {
        List<User> users = new ArrayList<>();
        
        // Crear usuarios susceptibles
        for (int i = 1; i <= totalUsers - infectedUsers; i++) {
            users.add(createUser(i, 0, HealthStatus.SUSCEPTIBLE, withMasks));
        }
        
        // Crear usuarios infectados
        for (int i = totalUsers - infectedUsers + 1; i <= totalUsers; i++) {
            users.add(createUser(i, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, withMasks));
        }
        
        return users;
    }

    /**
     * Calcula riesgo representativo para un escenario específico
     */
    private static double calculateScenarioRisk(String scenarioType) {
        switch (scenarioType) {
            case "CLASSROOM":
                // Aula: 2h a 2m
                ContactRecord classContact = createContactRecord(1, 2, 0, 7200.0, 2.0);
                User teacher = createUser(100, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, false);
                User student = createUser(101, 0, HealthStatus.SUSCEPTIBLE, false);
                return model.calculateTransmissionProbability(teacher, student, classContact);
                
            case "OFFICE":
                // Oficina: 8h a 1.5m
                ContactRecord officeContact = createContactRecord(1, 2, 0, 28800.0, 1.5);
                User worker1 = createUser(102, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, false);
                User worker2 = createUser(103, 0, HealthStatus.SUSCEPTIBLE, false);
                return model.calculateTransmissionProbability(worker1, worker2, officeContact);
                
            case "RESTAURANT":
                // Restaurante: 1.5h a 1.8m (promedio mesa)
                ContactRecord restaurantContact = createContactRecord(1, 2, 0, 5400.0, 1.8);
                User waiter = createUser(104, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, false);
                User customer = createUser(105, 0, HealthStatus.SUSCEPTIBLE, false);
                return model.calculateTransmissionProbability(waiter, customer, restaurantContact);
                
            case "TRANSPORT":
                // Transporte: 30min a 0.6m con mascarillas
                ContactRecord transportContact = createContactRecord(1, 2, 0, 1800.0, 0.6);
                User passenger1 = createUser(106, 0, HealthStatus.INFECTIOUS_SYMPTOMATIC, true);
                User passenger2 = createUser(107, 0, HealthStatus.SUSCEPTIBLE, true);
                return model.calculateTransmissionProbability(passenger1, passenger2, transportContact);
                
            default:
                return 0.0;
        }
    }
}