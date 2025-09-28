package es.unizar.epidemic.tests;

import es.unizar.epidemic.ContactRecord;
import es.unizar.epidemic.HealthStatus;
import es.unizar.epidemic.UserEpidemicExtension;
import es.unizar.epidemic.models.*;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.Simulation;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.tests.SimulationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

/**
 * Validation tests against reference graphs
 * Based on the parameters and expected results from graficas.txt
 * 
 * Reference parameters:
 * - Air changes per hour: 3 per hour
 * - Virus decay rate: 0.62 per hour
 * - Fraction of people with masks: 10%
 * - Percentage of immune population: 5%
 * - Probability of being infectious: 2%
 * 
 * @author Nacho Palacio
 * @date 2025-08-21
 */
public class TestGraficas {
    
    // Current scenario being tested
    private static Scenarios.TestScenario currentScenario;

    // Results storage for current scenario
    private static double[] pengVisitorResults;
    private static double[] pengDurationResults;
    private static double[] lelieveldVisitorResults;
    private static double[] lelieveldDurationResults;
    private static double[] proximityVisitorResults;
    private static double[] proximityDurationResults;

    private static double pengSpecificResult = 0.0;
    private static double lelieveldSpecificResult = 0.0;
    private static double proximitySpecificResult = 0.0;

    
    public static void main(String[] args) {
        System.out.println("📊 === VALIDATION AGAINST REFERENCE GRAPHS - MULTIPLE SCENARIOS ===");
        System.out.println("📅 Date: " + java.time.LocalDateTime.now());
        System.out.println("🔬 Testing 3 epidemic models across different scenarios");
        System.out.println("=".repeat(80));
        
        // Test all scenarios
        testAllScenarios();
        
        System.out.println("\n=".repeat(80));
        System.out.println("📅 Finalizado: " + java.time.LocalDateTime.now());
    }

    /**
     * Tests all predefined scenarios
     */
    private static void testAllScenarios() {
        List<Scenarios.TestScenario> scenarios = Scenarios.getAllScenarios();
        
        for (int i = 0; i < scenarios.size(); i++) {
            Scenarios.TestScenario scenario = scenarios.get(i);
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🎯 ESCENARIO " + (i + 1) + "/" + scenarios.size() + ": " + scenario.name);
            System.out.println("📝 " + scenario.description);
            System.out.println("=".repeat(80));
            
            testSingleScenario(scenario);
            
            System.out.println("\n=== GENERATING SCENARIO GRAPHS ===");
            generateScenarioGraphs(scenario, i + 1);
        }
        
        // Generate comparison between scenarios
        System.out.println("\n=== GENERATING SCENARIO COMPARISON ===");
        generateScenarioComparison(scenarios);
    }

    /**
     * Tests a single scenario with all three models
     */
    private static void testSingleScenario(Scenarios.TestScenario scenario) {
        currentScenario = scenario;
        
        // Initialize result arrays
        pengVisitorResults = new double[scenario.visitorCounts.length];
        pengDurationResults = new double[scenario.durationMinutes.length];
        lelieveldVisitorResults = new double[scenario.visitorCounts.length];
        lelieveldDurationResults = new double[scenario.durationMinutes.length];
        proximityVisitorResults = new double[scenario.visitorCounts.length];
        proximityDurationResults = new double[scenario.durationMinutes.length];
        
        // Test all models with incremental ranges
        testPengTransmissionModel();
        testLelieveldTransmissionModel();
        // testSimpleProximityModel();
        
        if (!scenario.name.equals("Original Reference")) {
            System.out.println("\n🎯 === PRUEBA ESPECÍFICA CON PARÁMETROS DEL ESCENARIO ===");
            testSpecificScenarioParameters(scenario);
        }
        
        // Print scenario summary
        printScenarioSummary();
    }

    /**
     * Tests the scenario with its specific defined parameters
     */
    private static void testSpecificScenarioParameters(Scenarios.TestScenario scenario) {
        System.out.println("📋 Parámetros específicos del escenario '" + scenario.name + "':");
        System.out.println("   👥 Visitantes estándar: " + scenario.standardVisitorCount);
        System.out.println("   ⏱️  Exposición estándar: " + scenario.standardExposureHours + " horas (" + 
                        (scenario.standardExposureHours * 60) + " minutos)");
        System.out.println("   🏠 Dimensiones: " + scenario.roomLength + "×" + scenario.roomWidth + "×" + scenario.roomHeight + " m");
        System.out.println("   💨 Ventilación: " + scenario.ventilationRate + " h⁻¹");
        System.out.println("   😷 Fracción mascarillas: " + (scenario.maskFraction * 100) + "%");
        System.out.println("   🦠 Probabilidad infecciosa: " + (scenario.infectiousProbability * 100) + "%");
        
        // Test with specific parameters
        testSpecificParameters_Peng(scenario);
        testSpecificParameters_Lelieveld(scenario);
        testSpecificParameters_SimpleProximity(scenario);
        
        printSpecificParametersResults(scenario);
    }

    /**
     * Tests Peng model with specific scenario parameters
     */
    private static void testSpecificParameters_Peng(Scenarios.TestScenario scenario) {
        try {
            PengTransmissionModel model = new PengTransmissionModel();
            
            model.configureForRoom(scenario.roomLength, scenario.roomWidth, 
                    scenario.roomHeight, scenario.ventilationRate);
            model.configureMasks(0.5, 0.3, scenario.maskFraction);

            PengParameters params = model.getParameters();
            params.setVirusDecayRate(scenario.virusDecayRate);
            params.setFractionImmune(scenario.immuneFraction);
            
            int infectiousCount = Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability));
            params.setPeopleCount(scenario.standardVisitorCount, infectiousCount);
            
            double specificRisk = params.calculateInfectionProbability(scenario.standardExposureHours, infectiousCount) * 100;
            
            pengSpecificResult = specificRisk;
            
            System.out.println("   🧪 Peng: " + String.format("%.3f", specificRisk) + "%");

            System.out.println("   🧪 Peng (teórico): " + String.format("%.3f", specificRisk) + "%");

            Simulation simulation = SimulationUtils.createAndRunSimulation(scenario, "AEROSOL_PENG");
            double avgRiskSim = simulation.calculateAverageTheoreticalRiskForAllRooms() * 100;
            System.out.println("   🧪 Peng (simulación real): " + String.format("%.3f", avgRiskSim) + "%");
            
        } catch (Exception e) {
            System.out.println("   ❌ Error en Peng específico: " + e.getMessage());
            pengSpecificResult = -1.0;
        }
    }

    /**
     * Tests Lelieveld model with specific scenario parameters
     */
    private static void testSpecificParameters_Lelieveld(Scenarios.TestScenario scenario) {
        try {
            LelieveldTransmissionModelTestVersion model = new LelieveldTransmissionModelTestVersion();
            
            LelieveldParameters params = model.getLelieveldParameters();
            params.setRoomDimensions(scenario.roomLength, scenario.roomWidth, scenario.roomHeight);
            params.setVentilationRates(scenario.ventilationRate, 0.0, false);
            params.setMaskParameters(0.3, 0.4, scenario.maskFraction);
            params.setFractionImmune(scenario.immuneFraction);
            
            int infectiousCount = Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability));
            params.setPeopleCount(scenario.standardVisitorCount, infectiousCount);
            
            double maskInhalationProtection = 1.0;  // Usar protección completa para consistencia
            double specificRisk = params.calculateInfectionProbability(
                scenario.standardExposureHours, params.getViralLoadHighCm3(), maskInhalationProtection) * 100;
            
            // Store in a global variable for display
            lelieveldSpecificResult = specificRisk;
            
            System.out.println("   🔬 Lelieveld: " + String.format("%.3f", specificRisk) + "%");

            System.out.println("   🧪 Lelieveld (teórico): " + String.format("%.3f", specificRisk) + "%");

            Simulation simulation = SimulationUtils.createAndRunSimulation(scenario, "AEROSOL_LELIEVELD");
            double avgRiskSim = simulation.calculateAverageTheoreticalRiskForAllRooms() * 100;
            System.out.println("   🧪 Lelieveld (simulación real): " + String.format("%.3f", avgRiskSim) + "%");

        } catch (Exception e) {
            System.out.println("   ❌ Error en Lelieveld específico: " + e.getMessage());
            lelieveldSpecificResult = -1.0;
        }
    }

    /**
     * Tests SimpleProximity model with specific scenario parameters
     */
    private static void testSpecificParameters_SimpleProximity(Scenarios.TestScenario scenario) {
        try {
            Configuration.setPixelsPerMeter(6.6);
            
            SimpleProximityModel model = new SimpleProximityModel();
            model.configureSimpleModel(5.0, scenario.infectiousProbability, 10);
            
            int infectiousCount = Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability));
            
            double specificRisk = simulateFixedRoomOccupancy(model, scenario.standardVisitorCount, 
                                                            infectiousCount, scenario.standardExposureHours);
            
            // Store in a global variable for display
            proximitySpecificResult = specificRisk;
            
            System.out.println("   📏 SimpleProximity: " + String.format("%.3f", specificRisk) + "%");
            
        } catch (Exception e) {
            System.out.println("   ❌ Error en SimpleProximity específico: " + e.getMessage());
            proximitySpecificResult = -1.0;
        }
    }
    
    
    // ==================== PENG MODEL TESTS ====================
    
    /**
     * Tests the Peng model against reference graphs
     */
    private static void testPengTransmissionModel() {
        System.out.println("🧪 === TESTING PENG TRANSMISSION MODEL 1 ===");
        
        try {
            PengTransmissionModel model = new PengTransmissionModel();
            
            model.configureForRoom(currentScenario.roomLength, currentScenario.roomWidth, 
                     currentScenario.roomHeight, currentScenario.ventilationRate);
            model.configureMasks(0.5, 0.3, currentScenario.maskFraction);

            PengParameters params = model.getParameters();
            params.setVirusDecayRate(currentScenario.virusDecayRate);
            params.setFractionImmune(currentScenario.immuneFraction);

            System.out.println("   ✓ Peng model configured for scenario: " + currentScenario.name);
            
            // Test 1: Variation by number of visitors
            runTest("Peng: Variation by number of visitors", 
                   () -> testVisitorVariation_Peng(model));
            
            // Test 2: Variation by duration
            runTest("Peng: Variation by exposure duration", 
                   () -> testDurationVariation_Peng(model));
            
        } catch (Exception e) {
            System.out.println("Error configuring Peng: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testVisitorVariation_Peng(PengTransmissionModel model) {
        System.out.println("   Testing variation by number of visitors...");
        
        PengParameters params = model.getParameters();
        
        for (int i = 0; i < currentScenario.visitorCounts.length; i++) {
            int visitors = currentScenario.visitorCounts[i];
            
            if (visitors == 0) {
                pengVisitorResults[i] = 0.0;
                continue;
            }

            double calculatedRisk = calculateRiskWithInterpolation(params, visitors, currentScenario.standardExposureHours);
            pengVisitorResults[i] = calculatedRisk;
        }
        
        System.out.println("     ✓ Visitor variation trend validated");
    }
    
    private static void testDurationVariation_Peng(PengTransmissionModel model) {
        System.out.println("    Testing variation by duration...");
        
        PengParameters params = model.getParameters();
        int standardVisitors = currentScenario.standardVisitorCount;
        int infectiousCount = Math.max(1, (int) Math.round(standardVisitors * currentScenario.infectiousProbability));
        
        params.setPeopleCount(standardVisitors, infectiousCount);
        
        for (int i = 0; i < currentScenario.durationMinutes.length; i++) {
            int durationMinutes = currentScenario.durationMinutes[i];
            
            if (durationMinutes == 0) {
                pengDurationResults[i] = 0.0;
                continue;
            }
            
            double exposureHours = durationMinutes / 60.0;
            double calculatedRisk = params.calculateInfectionProbability(exposureHours, infectiousCount) * 100;
            
            pengDurationResults[i] = calculatedRisk;
        }
        
        System.out.println("     ✓ Duration trend validated");
    }
    
    
    // ==================== LELIEVELD MODEL TESTS ====================
    
    /**
     * Tests the Lelieveld model against reference graphs
     */
    private static void testLelieveldTransmissionModel() {
        System.out.println(" === TESTING LELIEVELD TRANSMISSION MODEL ===");
        
        try {
            LelieveldTransmissionModelTestVersion model = new LelieveldTransmissionModelTestVersion();
            
            LelieveldParameters params = model.getLelieveldParameters();

            params.setRoomDimensions(currentScenario.roomLength, currentScenario.roomWidth, currentScenario.roomHeight);
            params.setVentilationRates(currentScenario.ventilationRate, 0.0, false);
            params.setMaskParameters(0.3, 0.4, currentScenario.maskFraction);
            params.setFractionImmune(currentScenario.immuneFraction);
            
            // Test 1: Variation by number of visitors
            runTest("Lelieveld: Variation by number of visitors", 
                () -> testVisitorVariation_Lelieveld(model));
            
            // Test 2: Variation by duration
            runTest("Lelieveld: Variation by exposure duration", 
                () -> testDurationVariation_Lelieveld(model));
            
        } catch (Exception e) {
            System.out.println("Error configuring Lelieveld: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testVisitorVariation_Lelieveld(LelieveldTransmissionModelTestVersion model) {
        System.out.println("   📈 Testing variation by number of visitors...");
        
        LelieveldParameters params = model.getLelieveldParameters();
        double exposureHours = currentScenario.standardExposureHours;

        System.out.println("   📋 DIAGNÓSTICO - Variación por visitantes:");
        System.out.println("      Exposición estándar: " + exposureHours + " horas");
        System.out.println("      Probabilidad infecciosa: " + currentScenario.infectiousProbability);
        
        
        for (int i = 0; i < currentScenario.visitorCounts.length; i++) {
            int visitors = currentScenario.visitorCounts[i];
            
            if (visitors == 0) {
                lelieveldVisitorResults[i] = 0.0;
                continue;
            }

            // DEBUG
            System.out.println("   🔍 VISITANTE " + visitors + ":");

            double calculatedRisk = calculateRiskWithInterpolation_Lelieveld(params, visitors, exposureHours);
            lelieveldVisitorResults[i] = calculatedRisk;

            // DEBUG
            System.out.println("      Riesgo calculado: " + calculatedRisk + "%");
        }
    }
    
    private static void testDurationVariation_Lelieveld(LelieveldTransmissionModelTestVersion model) {
        System.out.println("    Testing variation by duration...");
        
        LelieveldParameters params = model.getLelieveldParameters();
        int standardVisitors = currentScenario.standardVisitorCount;
        int infectiousCount = Math.max(1, (int) Math.round(standardVisitors * currentScenario.infectiousProbability));
        
        params.setPeopleCount(standardVisitors, infectiousCount);
        
        for (int i = 0; i < currentScenario.durationMinutes.length; i++) {
            int durationMinutes = currentScenario.durationMinutes[i];
            
            if (durationMinutes == 0) {
                lelieveldDurationResults[i] = 0.0;
                continue;
            }
            
            double exposureHours = durationMinutes / 60.0;
            double maskInhalationProtection = 1.0 - (params.getMaskEfficiencyInh() * currentScenario.maskFraction);
            double calculatedRisk = params.calculateInfectionProbability(
                exposureHours, params.getViralLoadHighCm3(), maskInhalationProtection) * 100;
            
            lelieveldDurationResults[i] = calculatedRisk;
        }
    }

    
    // ==================== SIMPLE PROXIMITY MODEL TESTS ====================
    
    /**
     * Tests the Simple Proximity model against reference graphs
     */
    private static void testSimpleProximityModel() {
        System.out.println("📏 === TESTING SIMPLE PROXIMITY MODEL ===");
        debugDistanceDistribution();
        
        try {
            Configuration.setPixelsPerMeter(6.6);
            
            SimpleProximityModel model = new SimpleProximityModel();
            model.configureSimpleModel(5.0, currentScenario.infectiousProbability, 10);
        
            System.out.println("   ✓ Simple Proximity model configured for scenario: " + currentScenario.name);
            
            // Test 1: Variation by number of visitors
            runTest("Simple Proximity: Room occupancy simulation by visitors", 
                () -> testRoomOccupancyVariation_SimpleProximity(model));
            
            // Test 2: Variation duration
            runTest("Simple Proximity: Room occupancy simulation by duration", 
                () -> testExposureDurationVariation_SimpleProximity(model));
            
        } catch (Exception e) {
            System.out.println("Error configuring Simple Proximity: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Tests variation by number of visitors using fixed room occupancy simulation
     */
    private static void testRoomOccupancyVariation_SimpleProximity(SimpleProximityModel model) {        
        double exposureHours = currentScenario.standardExposureHours;
        
        for (int i = 0; i < currentScenario.visitorCounts.length; i++) {
            int visitors = currentScenario.visitorCounts[i];
            
            if (visitors == 0) {
                proximityVisitorResults[i] = 0.0;
                continue;
            }
            
            double exactInfectious = visitors * currentScenario.infectiousProbability;
            double calculatedRisk;
            
            if (exactInfectious < 1.0) {
                double baseRisk = simulateFixedRoomOccupancy(model, visitors, 1, exposureHours);
                calculatedRisk = baseRisk * exactInfectious;
            } else {
                int infectiousCount = (int) Math.round(exactInfectious);
                calculatedRisk = simulateFixedRoomOccupancy(model, visitors, infectiousCount, exposureHours);
            }
            
            proximityVisitorResults[i] = calculatedRisk;
        }
    }

    /**
     * Tests variation by exposure duration using fixed room occupancy simulation
     */
    private static void testExposureDurationVariation_SimpleProximity(SimpleProximityModel model) { 
        int standardVisitors = currentScenario.standardVisitorCount;
        int infectiousCount = Math.max(1, (int) Math.round(standardVisitors * currentScenario.infectiousProbability));
    
        
        for (int i = 0; i < currentScenario.durationMinutes.length; i++) {
            int durationMinutes = currentScenario.durationMinutes[i];
            
            if (durationMinutes == 0) {
                proximityDurationResults[i] = 0.0;
                continue;
            }
            
            double exposureHours = durationMinutes / 60.0;
            double calculatedRisk = simulateFixedRoomOccupancy(model, standardVisitors, infectiousCount, exposureHours);
            
            proximityDurationResults[i] = calculatedRisk;
        }
    }

    /**
     * Simulates fixed room occupancy with N users for entire exposure duration
     */
    private static double simulateFixedRoomOccupancy(SimpleProximityModel model, int totalUsers, 
                                                int infectiousCount, double exposureHours) {
        if (infectiousCount == 0 || totalUsers <= infectiousCount) {
            return 0.0;
        }
        
        int numSimulations = 100; 
        double totalRiskSum = 0.0;
        int validSimulations = 0;
        
        for (int sim = 0; sim < numSimulations; sim++) {
            List<User> users = generateFixedRoomUsers(totalUsers, infectiousCount);
            
            List<User> susceptibles = new ArrayList<>();
            List<User> infectious = new ArrayList<>();
            
            for (User user : users) {
                HealthStatus status = user.getEpidemicExtension().getHealthStatus();
                if (status == HealthStatus.SUSCEPTIBLE) {
                    susceptibles.add(user);
                } else if (status == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
                        status == HealthStatus.INFECTIOUS_ASYMPTOMATIC ||
                        status == HealthStatus.SUPER_SPREADER) {
                    infectious.add(user);
                }
            }
            
            if (susceptibles.isEmpty() || infectious.isEmpty()) {
                continue;
            }
            
            double simulationRiskSum = 0.0;

            // For each susceptible, calculate risk from all infectious users
            for (User susceptible : susceptibles) {
                double pNoInfection = 1.0;
                
                for (User infectiousUser : infectious) {
                    double distance = calculateRealDistanceBetweenUsers(susceptible, infectiousUser);
                    
                    if (distance <= model.getMaxTransmissionDistance()) {
                        double contactDuration = calculateContactDurationForFixedOccupancy(distance, exposureHours);
                        
                        if (contactDuration >= model.getMinContactDuration()) {
                            ContactRecord contact = createContactRecord(
                                infectiousUser.userID, susceptible.userID, 0, contactDuration, distance);
                            
                            double transmissionProb = model.calculateTransmissionProbability(
                                infectiousUser, susceptible, contact);
                            
                            pNoInfection *= (1.0 - transmissionProb);
                        }
                    }
                }
                
                // Calculate total infection probability for this susceptible
                double infectionProbability = 1.0 - pNoInfection;
                simulationRiskSum += infectionProbability;
            }
            
            // Average risk per susceptible
            double avgRiskThisSimulation = (simulationRiskSum / susceptibles.size()) * 100;
            totalRiskSum += avgRiskThisSimulation;
            validSimulations++;
        }
        
        double finalAverageRisk = totalRiskSum / validSimulations;

        return finalAverageRisk;
    }

    /**
     * Generates a fixed set of users positioned randomly in the room
     */
    private static List<User> generateFixedRoomUsers(int totalUsers, int infectiousCount) {
        List<User> users = new ArrayList<>();
        
        for (int i = 1; i <= infectiousCount; i++) {
            User user = createUserAtRandomPosition(i, HealthStatus.INFECTIOUS_SYMPTOMATIC);
            users.add(user);
        }
        
        for (int i = infectiousCount + 1; i <= totalUsers; i++) {
            User user = createUserAtRandomPosition(i, HealthStatus.SUSCEPTIBLE);
            users.add(user);
        }
        
        return users;
    }

    /**
     * Calculates contact duration for fixed room occupancy scenario
     */
    private static double calculateContactDurationForFixedOccupancy(double distance, double totalExposureHours) {
        double totalSeconds = totalExposureHours * 3600;
        
        double interactionFactor;
        
        if (distance <= 0.5) {
            interactionFactor = 0.9;  // Very close
        } else if (distance <= 1.0) {
            interactionFactor = 0.7;  // Close
        } else if (distance <= 1.5) {
            interactionFactor = 0.5;  // Moderate
        } else if (distance <= 2.5) {
            interactionFactor = 0.3;  // Far
        } else if (distance <= 4.0) {
            interactionFactor = 0.2;  // Very far
        } else {
            interactionFactor = 0.05; // Minimal
        }
        
        double contactDuration = totalSeconds * interactionFactor;
       
        return contactDuration;
    }
    
    
    
    // ==================== AUXILIARY METHODS ====================

    /**
     * Verifica la distribución de distancias en la habitación
     */
    private static void debugDistanceDistribution() {    
        int samples = 1000;
        double minDistance = Double.MAX_VALUE;
        double maxDistance = 0;
        
        for (int i = 0; i < samples; i++) {
            double x1 = Math.random() * currentScenario.roomLength;
            double y1 = Math.random() * currentScenario.roomWidth;
            double x2 = Math.random() * currentScenario.roomLength;
            double y2 = Math.random() * currentScenario.roomWidth;
          
            double distance = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
            
            minDistance = Math.min(minDistance, distance);
            maxDistance = Math.max(maxDistance, distance);
            
        }

    }
    

    /**
     * Creates an user at a random position within the room
     */
    private static User createUserAtRandomPosition(int id, HealthStatus status) {
        User user;
        try {
            user = new User(id, false);
        } catch (IOException e) {
            throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
        }
        
        double xMeters = Math.random() * currentScenario.roomLength;
        double yMeters = Math.random() * currentScenario.roomWidth;
    
        
        double pixelsPerMeter = Configuration.getPixelsPerMeter();
        
        user.x = xMeters * pixelsPerMeter;
        user.y = yMeters * pixelsPerMeter;
        user.room = 0;
        
        UserEpidemicExtension extension = new UserEpidemicExtension();
        extension.setHealthStatus(status);
        extension.setMaskWearing(Math.random() < currentScenario.maskFraction);
        extension.setIncubationPeriod(120);
        extension.setInfectiousPeriod(240);
        user.setEpidemicExtension(extension);
         
        return user;
    }

    /**
     * Calculates real distance between two users
     */
    private static double calculateRealDistanceBetweenUsers(User user1, User user2) {
        double deltaX = user1.x - user2.x;
        double deltaY = user1.y - user2.y;
        double distancePixels = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return distancePixels / Configuration.getPixelsPerMeter();
    }

    
    
    /**
     * Creates a ContactRecord with distance in meters
     */
    private static ContactRecord createContactRecord(long user1Id, long user2Id, int roomId, double duration, double distanceInMeters) {
        ContactRecord contact = new ContactRecord(user1Id, user2Id, roomId, duration);
        
        double distanceInPixels = PengParameters.metersToPixels(distanceInMeters);
        contact.setDistance(distanceInPixels);
        
        return contact;
    }
     

    /**
     * Calculates risk using fractional infectious count with linear interpolation
     */
    private static double calculateRiskWithInterpolation(PengParameters params, int visitors, double exposureHours) {
        if (visitors == 0) {
            return 0.0;
        }
        
        double exactInfectious = visitors * currentScenario.infectiousProbability;
        
        // DEBUG
        System.out.println("         Visitantes totales: " + visitors);
        System.out.println("         Infectivos exactos: " + exactInfectious);

        if (exactInfectious < 1.0) {
            params.setPeopleCount(visitors, 1);

            // DEBUG
            System.out.println("         Configurado: " + visitors + " personas, 1 infectivo");
            System.out.println("         Factor protección máscaras: " + (1.0 - currentScenario.maskFraction));
            
            double baseRisk = params.calculateInfectionProbability(exposureHours, 1) * 100;
            
            // DEBUG
            System.out.println("         Riesgo base (1 infectivo): " + baseRisk + "%");

            return baseRisk * exactInfectious;
        } 
        else {
            int lowerInfectious = (int) Math.floor(exactInfectious);
            int upperInfectious = (int) Math.ceil(exactInfectious);
            
            if (lowerInfectious == upperInfectious) {
                params.setPeopleCount(visitors, lowerInfectious);

                return params.calculateInfectionProbability(exposureHours, lowerInfectious) * 100;
            } else {
                double fraction = exactInfectious - lowerInfectious;
                
                params.setPeopleCount(visitors, lowerInfectious);
                double lowerRisk = params.calculateInfectionProbability(exposureHours, lowerInfectious) * 100;
                
                params.setPeopleCount(visitors, upperInfectious);
                double upperRisk = params.calculateInfectionProbability(exposureHours, upperInfectious) * 100;
                
                // DEBUG
                double interpolatedRisk = lowerRisk + fraction * (upperRisk - lowerRisk);
                System.out.println("         Riesgo interpolado: " + interpolatedRisk + "%");
            
                return lowerRisk + fraction * (upperRisk - lowerRisk);
            }
        }
    }

    /**
     * Calculates risk using fractional infectious count for Lelieveld model
     */
    private static double calculateRiskWithInterpolation_Lelieveld(LelieveldParameters params, int visitors, double exposureHours) {
        if (visitors == 0) {
            return 0.0;
        }
        
       double exactInfectious = visitors * currentScenario.infectiousProbability;
        
        if (exactInfectious < 1.0) {
            params.setPeopleCount(visitors, 1);
            double baseRisk = params.calculateInfectionProbability(
                exposureHours, params.getViralLoadHighCm3(), 1.0) * 100;
            return baseRisk * exactInfectious;
        } 
        else {
            int lowerInfectious = (int) Math.floor(exactInfectious);
            int upperInfectious = (int) Math.ceil(exactInfectious);
            
            if (lowerInfectious == upperInfectious) {
                params.setPeopleCount(visitors, lowerInfectious);

                return params.calculateInfectionProbability(
                    exposureHours, params.getViralLoadHighCm3(), 1.0) * 100;
                // return params.calculateInfectionProbability(
                //     exposureHours, params.getViralLoadHighCm3(), 1.0 - currentScenario.maskFraction) * 100;
            } else {
                double fraction = exactInfectious - lowerInfectious;
                
                params.setPeopleCount(visitors, lowerInfectious);
                // double lowerRisk = params.calculateInfectionProbability(
                //     exposureHours, params.getViralLoadHighCm3(), 1.0 - currentScenario.maskFraction) * 100;

                double lowerRisk = params.calculateInfectionProbability(
                    exposureHours, params.getViralLoadHighCm3(), 1.0) * 100;
                
                params.setPeopleCount(visitors, upperInfectious);
                // double upperRisk = params.calculateInfectionProbability(
                //     exposureHours, params.getViralLoadHighCm3(), 1.0 - currentScenario.maskFraction) * 100;

                double upperRisk = params.calculateInfectionProbability(
                    exposureHours, params.getViralLoadHighCm3(), 1.0) * 100;
                
                return lowerRisk + fraction * (upperRisk - lowerRisk);
            }
        }
    }
    
    /**
     * Runs an individual test
     */
    private static void runTest(String testName, Runnable testMethod) {     
        try {
            testMethod.run();
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Shows the final test summary with detailed comparative table
     */
    private static void printFinalSummary() {
        System.out.println("=".repeat(70));
        System.out.println("🎯 RESUMEN FINAL - VALIDACIÓN CONTRA GRÁFICAS");
        System.out.println("=".repeat(70));
        
        printComparisonTables();
        
        System.out.println("=".repeat(70));
     
        System.out.println("=".repeat(70));
        System.out.println("📅 Finalizado: " + java.time.LocalDateTime.now());
    }

    /**
     * Prints detailed comparative tables for all models
     */
    private static void printComparisonTables() {
        System.out.println("\n📊 === TABLA COMPARATIVA: VARIACIÓN POR VISITANTES ===");
        printScenarioVisitorComparisonTable();
        
        System.out.println("\n📊 === TABLA COMPARATIVA: VARIACIÓN POR DURACIÓN ===");
        printScenarioDurationComparisonTable();
    }

    
    // ==================== CLASE AUXILIAR PARA TESTING ====================
    
    static class LelieveldTransmissionModelTestVersion extends LelieveldTransmissionModel {
        
        public LelieveldTransmissionModelTestVersion() {
            super();
            this.setAutoConfigureRooms(false);
        }
        
        @Override
        protected List<User> getUsersInRoom(int roomId) {
            return new ArrayList<>();
        }
    }

    // ==================== AUTOMATIC GRAPH GENERATION ====================

    private static void printScenarioSummary() {
        System.out.println("=".repeat(70));
        System.out.println("🎯 RESUMEN DEL ESCENARIO: " + currentScenario.name);
        System.out.println("=".repeat(70));

        if (!currentScenario.name.equals("Original Reference")) {
            printSpecificParametersResults(currentScenario);
            System.out.println();
        }
        
        printScenarioComparisonTables();
    }

    private static void printScenarioComparisonTables() {
        System.out.println("\n📊 === TABLA COMPARATIVA: VARIACIÓN POR VISITANTES ===");
        printScenarioVisitorComparisonTable();
        
        System.out.println("\n📊 === TABLA COMPARATIVA: VARIACIÓN POR DURACIÓN ===");
        printScenarioDurationComparisonTable();
    }

    private static void printScenarioDurationComparisonTable() {
        System.out.println("┌─────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ Duración    │  Referencia  │     Peng     │   Lelieveld  │   Proximity  │   Error P    │   Error L    │");
        System.out.println("│   (min)     │     (%)      │     (%)      │     (%)      │     (%)      │     (%)      │     (%)      │");
        System.out.println("├─────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤");
        
        for (int i = 0; i < currentScenario.durationMinutes.length; i++) {
            int duration = currentScenario.durationMinutes[i];
            double reference = currentScenario.durationRisksModel[i];
            double peng = pengDurationResults[i];
            double lelieveld = lelieveldDurationResults[i];
            double proximity = proximityDurationResults[i];
            
            double errorP = Math.abs(peng - reference);
            double errorL = Math.abs(lelieveld - reference);
            
            System.out.printf("│ %11d │ %12.3f │ %12.3f │ %12.3f │ %12.3f │ %12.3f │ %12.3f │%n",
                            duration, reference, peng, lelieveld, proximity, errorP, errorL);
        }
        
        System.out.println("└─────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘");
    }

    private static void printScenarioVisitorComparisonTable() {
        System.out.println("┌─────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ Visitantes  │  Referencia  │     Peng     │   Lelieveld  │   Proximity  │   Error P    │   Error L    │");
        System.out.println("│             │     (%)      │     (%)      │     (%)      │     (%)      │     (%)      │     (%)      │");
        System.out.println("├─────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤");
        
        for (int i = 0; i < currentScenario.visitorCounts.length; i++) {
            if (i % 3 == 0 || i < 5 || i >= currentScenario.visitorCounts.length - 3) {
                int visitors = currentScenario.visitorCounts[i];
                double reference = currentScenario.visitorRisksModel[i];
                double peng = pengVisitorResults[i];
                double lelieveld = lelieveldVisitorResults[i];
                double proximity = proximityVisitorResults[i];
                
                double errorP = Math.abs(peng - reference);
                double errorL = Math.abs(lelieveld - reference);
                
                System.out.printf("│ %11d │ %12.3f │ %12.3f │ %12.3f │ %12.3f │ %12.3f │ %12.3f │%n",
                                visitors, reference, peng, lelieveld, proximity, errorP, errorL);
            }
        }
        
        System.out.println("└─────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘");
    }

    private static void generateScenarioGraphs(Scenarios.TestScenario scenario, int scenarioNumber) {
        try {
            File graphicsDir = new File("./graficas_validacion/scenario_" + scenarioNumber + "/");
            if (!graphicsDir.exists()) {
                graphicsDir.mkdirs();
            }
            System.out.println("✅ Gráficas del escenario '" + scenario.name + "' generadas en: " + graphicsDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error generando gráficas del escenario: " + e.getMessage());
        }
    }

    private static void generateScenarioComparison(List<Scenarios.TestScenario> scenarios) {
        System.out.println("Generating cross-scenario comparison graphs...");
    }

    /**
     * Prints results for specific scenario parameters
     */
    private static void printSpecificParametersResults(Scenarios.TestScenario scenario) {
        System.out.println("\n📊 === RESULTADOS CON PARÁMETROS ESPECÍFICOS ===");
        System.out.println("┌─────────────────┬──────────────┬──────────────┬──────────────┐");
        System.out.println("│ Escenario       │     Peng     │   Lelieveld  │   Proximity  │");
        System.out.println("│                 │     (%)      │     (%)      │     (%)      │");
        System.out.println("├─────────────────┼──────────────┼──────────────┼──────────────┤");
        
        String scenarioName = scenario.name.length() > 15 ? scenario.name.substring(0, 15) : scenario.name;
        
        System.out.printf("│ %-15s │ %12.3f │ %12.3f │ %12.3f │%n",
                        scenarioName, 
                        pengSpecificResult >= 0 ? pengSpecificResult : 0.0,
                        lelieveldSpecificResult >= 0 ? lelieveldSpecificResult : 0.0,
                        proximitySpecificResult >= 0 ? proximitySpecificResult : 0.0);
        
        System.out.println("└─────────────────┴──────────────┴──────────────┴──────────────┘");
        
        System.out.println("\n📈 Configuración utilizada:");
        System.out.println("   • " + scenario.standardVisitorCount + " personas (" + 
                        Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability)) + 
                        " infectivos)");
        System.out.println("   • " + scenario.standardExposureHours + " horas de exposición");
        System.out.println("   • Volumen: " + String.format("%.1f", scenario.roomLength * scenario.roomWidth * scenario.roomHeight) + " m³");
        System.out.println("   • Ventilación: " + scenario.ventilationRate + " h⁻¹");
    }

    /**
     * Generates all validation graphs automatically
     */
    private static void generateValidationGraphs() {
        try {
            File graphicsDir = new File("./graficas_validacion/");
            if (!graphicsDir.exists()) {
                graphicsDir.mkdirs();
                System.out.println("📁 Directorio creado: " + graphicsDir.getAbsolutePath());
            }

            generateAerosolModelsComparisonGraph_Visitors();
            generateAerosolModelsComparisonGraph_Duration();
            
            System.out.println("✅ Gráficas generadas exitosamente en: " + graphicsDir.getAbsolutePath());
            System.out.println("   Archivos generados:");
            System.out.println("   • aerosol_models_vs_reference_visitors.png - Todos los modelos vs Referencia (Visitantes)");
            System.out.println("   • aerosol_models_vs_reference_duration.png - Todos los modelos vs Referencia (Duración)");

        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates comparison graph: Peng + Lelieveld + SimpleProximity + Reference (Visitors)
     */
   private static void generateAerosolModelsComparisonGraph_Visitors() {
        try {
            System.out.println(" Generando: aerosol_models_vs_reference_visitors.png...");
            
            int width = 1400, height = 900;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            
            int margin = 80;
            int graphWidth = width - 2 * margin;
            int graphHeight = height - 2 * margin;
            int graphX = margin;
            int graphY = margin;
            
            // Título
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 26));
            FontMetrics fm = g2d.getFontMetrics();
            String title = "Todos los Modelos vs Datos de Referencia - Visitantes";
            int titleWidth = fm.stringWidth(title);
            g2d.drawString(title, (width - titleWidth) / 2, 40);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g2d.getFontMetrics();
            String subtitle = "Comparación de Peng, Lelieveld, SimpleProximity y datos experimentales";
            int subtitleWidth = fm.stringWidth(subtitle);
            g2d.setColor(Color.GRAY);
            g2d.drawString(subtitle, (width - subtitleWidth) / 2, 65);
            
            // Ejes
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(graphX, graphY + graphHeight, graphX + graphWidth, graphY + graphHeight); // X axis
            g2d.drawLine(graphX, graphY, graphX, graphY + graphHeight); // Y axis
            
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g2d.getFontMetrics();
            g2d.drawString("Número de Visitantes", graphX + (graphWidth - fm.stringWidth("Número de Visitantes")) / 2, height - 20);
            
            Graphics2D g2dRotated = (Graphics2D) g2d.create();
            g2dRotated.rotate(-Math.PI / 2, 25, graphY + graphHeight / 2);
            g2dRotated.drawString("Riesgo de Infección (%)", 25 - fm.stringWidth("Riesgo de Infección (%)") / 2, graphY + graphHeight / 2);
            g2dRotated.dispose();
            
            double maxX = Arrays.stream(currentScenario.visitorCounts).max().orElse(200);
            double maxY = Math.max(
                Math.max(
                    Math.max(Arrays.stream(currentScenario.visitorRisksModel).max().orElse(2.0),
                            Arrays.stream(pengVisitorResults).max().orElse(2.0)),
                    Arrays.stream(lelieveldVisitorResults).max().orElse(2.0)),
                Arrays.stream(proximityVisitorResults).max().orElse(2.0)
            ) * 1.15;
            
            g2d.setColor(new Color(245, 245, 245));
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 10; i++) {
                int x = graphX + (int) (i * graphWidth / 10.0);
                int y = graphY + graphHeight - (int) (i * graphHeight / 10.0);
                g2d.drawLine(x, graphY, x, graphY + graphHeight);
                g2d.drawLine(graphX, y, graphX + graphWidth, y);
            }
            
            // Referencia
            drawDataLine(g2d,  currentScenario.visitorCounts, currentScenario.visitorRisksModel, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, Color.BLACK, new BasicStroke(5), "Referencia");
            
            // Peng
            drawDataLine(g2d,  currentScenario.visitorCounts, pengVisitorResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(30, 144, 255), new BasicStroke(4), "Peng");
            
            // Lelieveld
            drawDataLine(g2d,  currentScenario.visitorCounts, lelieveldVisitorResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(220, 20, 60), new BasicStroke(4), "Lelieveld");

            // SimpleProximity
            drawDataLine(g2d,  currentScenario.visitorCounts, proximityVisitorResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(34, 139, 34), new BasicStroke(4), "SimpleProximity");

            drawAllModelsLegend(g2d, width - 350, graphY + 20); 
            
            drawScales(g2d, graphX, graphY, graphWidth, graphHeight, maxX, maxY);
            
            g2d.dispose();
            
            ImageIO.write(image, "PNG", new File("./graficas_validacion/aerosol_models_vs_reference_visitors.png"));
            
        } catch (Exception e) {
            System.err.println("Error generando aerosol_models_vs_reference_visitors.png: " + e.getMessage());
        }
    }

    /**
     * Generates comparison graph: Peng + Lelieveld + Reference (Duration)
     */
   private static void generateAerosolModelsComparisonGraph_Duration() {
        try {
            System.out.println(" Generando: aerosol_models_vs_reference_duration.png...");
            
            int width = 1400, height = 900;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            
            int margin = 80;
            int graphWidth = width - 2 * margin;
            int graphHeight = height - 2 * margin;
            int graphX = margin;
            int graphY = margin;
            
            // Título
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 26));
            FontMetrics fm = g2d.getFontMetrics();
            String title = "Todos los Modelos vs Datos de Referencia - Duración";
            int titleWidth = fm.stringWidth(title);
            g2d.drawString(title, (width - titleWidth) / 2, 40);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g2d.getFontMetrics();
            String subtitle = "Comparación de Peng, Lelieveld, SimpleProximity y datos experimentales";
            int subtitleWidth = fm.stringWidth(subtitle);
            g2d.setColor(Color.GRAY);
            g2d.drawString(subtitle, (width - subtitleWidth) / 2, 65);
            
            // Ejes
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(graphX, graphY + graphHeight, graphX + graphWidth, graphY + graphHeight); // X axis
            g2d.drawLine(graphX, graphY, graphX, graphY + graphHeight); // Y axis
            
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            fm = g2d.getFontMetrics();
            g2d.drawString("Duración (minutos)", graphX + (graphWidth - fm.stringWidth("Duración (minutos)")) / 2, height - 20);
            
            Graphics2D g2dRotated = (Graphics2D) g2d.create();
            g2dRotated.rotate(-Math.PI / 2, 25, graphY + graphHeight / 2);
            g2dRotated.drawString("Riesgo de Infección (%)", 25 - fm.stringWidth("Riesgo de Infección (%)") / 2, graphY + graphHeight / 2);
            g2dRotated.dispose();
            
            double maxX = Arrays.stream(currentScenario.durationMinutes).max().orElse(60);
            double maxY = Math.max(
                Math.max(
                    Math.max(Arrays.stream(currentScenario.durationRisksModel).max().orElse(2.0),
                            Arrays.stream(pengDurationResults).max().orElse(2.0)),
                    Arrays.stream(lelieveldDurationResults).max().orElse(2.0)),
                Arrays.stream(proximityDurationResults).max().orElse(2.0)
            ) * 1.15;
            
            g2d.setColor(new Color(245, 245, 245));
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 10; i++) {
                int x = graphX + (int) (i * graphWidth / 10.0);
                int y = graphY + graphHeight - (int) (i * graphHeight / 10.0);
                g2d.drawLine(x, graphY, x, graphY + graphHeight);
                g2d.drawLine(graphX, y, graphX + graphWidth, y);
            }
            
            // Referencia
            drawDataLine(g2d, currentScenario.durationMinutes, currentScenario.durationRisksModel, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, Color.BLACK, new BasicStroke(5), "Referencia");
            
            // Peng
            drawDataLine(g2d, currentScenario.durationMinutes, pengDurationResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(30, 144, 255), new BasicStroke(4), "Peng");
            
            // Lelieveld 
            drawDataLine(g2d, currentScenario.durationMinutes, lelieveldDurationResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(220, 20, 60), new BasicStroke(4), "Lelieveld");
            
            // SimpleProximity
            drawDataLine(g2d, currentScenario.durationMinutes, proximityDurationResults, graphX, graphY, graphWidth, graphHeight, 
                        maxX, maxY, new Color(34, 139, 34), new BasicStroke(4), "SimpleProximity");
            
            drawAllModelsLegend(g2d, width - 350, graphY + 20);
            
            drawScales(g2d, graphX, graphY, graphWidth, graphHeight, maxX, maxY);
            
            g2d.dispose();
            
            ImageIO.write(image, "PNG", new File("./graficas_validacion/aerosol_models_vs_reference_duration.png"));
            
        } catch (Exception e) {
            System.err.println("Error generando aerosol_models_vs_reference_duration.png: " + e.getMessage());
        }
    }

    /**
     * Draws the legend for all models comparison graphs (Reference + Peng + Lelieveld + SimpleProximity)
     */
    private static void drawAllModelsLegend(Graphics2D g2d, int x, int y) {
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.fillRect(x - 8, y - 8, 330, 130);
        g2d.setColor(new Color(250, 250, 250));
        g2d.fillRect(x - 10, y - 10, 330, 130);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x - 10, y - 10, 330, 130);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLACK);
        g2d.drawString("LEYENDA", x + 125, y + 10);
        
        // Referencia
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(5));
        g2d.drawLine(x, y + 25, x + 40, y + 25);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Datos de Referencia", x + 50, y + 30);
        
        // Peng
        g2d.setColor(new Color(30, 144, 255));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x, y + 45, x + 40, y + 45);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Peng (Wells-Riley)", x + 50, y + 50);
        
        // Lelieveld
        g2d.setColor(new Color(220, 20, 60));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x, y + 65, x + 40, y + 65);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Lelieveld", x + 50, y + 70);
        
        // SimpleProximity
        g2d.setColor(new Color(34, 139, 34));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(x, y + 85, x + 40, y + 85);
        g2d.setColor(Color.BLACK);
        g2d.drawString("SimpleProximity", x + 50, y + 90);
        
        g2d.setFont(new Font("Arial", Font.ITALIC, 11));
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawString("Modelos de transmisión: aerosoles vs proximidad", x + 30, y + 110);
    }


    // ==================== AUXILIARY METHODS FOR GRAPHS ====================

    /**
     * Draws a data line in the graph
     */
    private static void drawDataLine(Graphics2D g2d, int[] xData, double[] yData, 
                                    int graphX, int graphY, int graphWidth, int graphHeight,
                                    double maxX, double maxY, Color color, Stroke stroke, String label) {
        g2d.setColor(color);
        g2d.setStroke(stroke);
        
        for (int i = 0; i < xData.length - 1; i++) {
            int x1 = graphX + (int) (xData[i] / maxX * graphWidth);
            int y1 = graphY + graphHeight - (int) (yData[i] / maxY * graphHeight);
            int x2 = graphX + (int) (xData[i + 1] / maxX * graphWidth);
            int y2 = graphY + graphHeight - (int) (yData[i + 1] / maxY * graphHeight);
            
            g2d.drawLine(x1, y1, x2, y2);
            
            g2d.fillOval(x1 - 3, y1 - 3, 6, 6);
            if (i == xData.length - 2) {
                g2d.fillOval(x2 - 3, y2 - 3, 6, 6);
            }
        }
    }


    /**
     * Draws the axis scales
     */
    private static void drawScales(Graphics2D g2d, int graphX, int graphY, int graphWidth, int graphHeight, double maxX, double maxY) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        
        for (int i = 0; i <= 10; i++) {
            int x = graphX + (int) (i * graphWidth / 10.0);
            int value = (int) (i * maxX / 10.0);
            String label = String.valueOf(value);
            g2d.drawString(label, x - fm.stringWidth(label) / 2, graphY + graphHeight + 15);
            
            g2d.drawLine(x, graphY + graphHeight, x, graphY + graphHeight + 5);
        }
        
        for (int i = 0; i <= 10; i++) {
            int y = graphY + graphHeight - (int) (i * graphHeight / 10.0);
            double value = i * maxY / 10.0;
            String label = String.format("%.1f", value);
            g2d.drawString(label, graphX - fm.stringWidth(label) - 10, y + fm.getHeight() / 3);
            
            g2d.drawLine(graphX - 5, y, graphX, y);
        }
    }

}