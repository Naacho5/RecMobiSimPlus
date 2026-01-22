package es.unizar.epidemic.tests;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Scenarios for Epidemic Simulation
 * Only the original scenario is currently used.
 * 
 * @author Nacho Palacio
 */
public class Scenarios {
    
    /**
     * Test scenario with configurable parameters
     */
    public static class TestScenario {
        public String name;
        public String description;
        
        // Reference parameters
        public double ventilationRate;
        public double virusDecayRate;
        public double maskFraction;
        public double immuneFraction;
        public double infectiousProbability;
        
        // Space configuration
        public double roomLength;
        public double roomWidth;
        public double roomHeight;
        
        // Reference data for validation
        public int[] visitorCounts;
        public double[] visitorRisksModel;
        public int[] durationMinutes;
        public double[] durationRisksModel;
        
        // Exposure parameters
        public double standardExposureHours;
        public int standardVisitorCount;
        
        public TestScenario(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    /**
     * Original scenario based on graficas.txt
     */
    public static TestScenario getOriginalScenario() {
        TestScenario scenario = new TestScenario(
            "Original Reference", 
            "Original scenario based on reference data from graficas.txt"
        );
        
        // Original reference parameters
        scenario.ventilationRate = 3.0;
        scenario.virusDecayRate = 0.62;
        scenario.maskFraction = 1.0;
        scenario.immuneFraction = 0.06;
        scenario.infectiousProbability = 0.02;
        
        // Original space configuration
        scenario.roomLength = 24.4;
        scenario.roomWidth = 15.3;
        scenario.roomHeight = 5.5;
        
        // Original reference data
        scenario.visitorCounts = new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200};
        scenario.visitorRisksModel = new double[]{0.008, 0.068, 0.141, 0.213, 0.297, 0.379, 0.461, 0.533, 0.612, 0.693, 0.767, 0.847, 0.928, 1.003, 1.083, 1.158, 1.238, 1.319, 1.396, 1.473, 1.547};
        
        scenario.durationMinutes = new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60};
        scenario.durationRisksModel = new double[]{0.000, 0.076, 0.175, 0.284, 0.401, 0.525, 0.653, 0.786, 0.922, 1.062, 1.205, 1.351, 1.500};
        
        scenario.standardExposureHours = 8.0;
        scenario.standardVisitorCount = 98;
        scenario.immuneFraction = 0.05;
        
        return scenario;
    }

    /**
     * Creates a variation of the original scenario by modifying specific parameters
     */
    private static TestScenario createOriginalVariation(String name, String description,
                                                      Double ventilationRate, 
                                                      Double virusDecayRate,
                                                      Double maskFraction, 
                                                      Double immuneFraction,
                                                      Double infectiousProbability,
                                                      Double roomLength, 
                                                      Double roomWidth, 
                                                      Double roomHeight,
                                                      Double standardExposureHours,
                                                      Integer standardVisitorCount) {
        
        // Get base scenario
        TestScenario base = getOriginalScenario();
        
        // Create new scenario
        TestScenario variation = new TestScenario(name, description);
        
        // Apply parameters (use base if null, new value if provided)
        variation.ventilationRate = (ventilationRate != null) ? ventilationRate : base.ventilationRate;
        variation.virusDecayRate = (virusDecayRate != null) ? virusDecayRate : base.virusDecayRate;
        variation.maskFraction = (maskFraction != null) ? maskFraction : base.maskFraction;
        variation.immuneFraction = (immuneFraction != null) ? immuneFraction : base.immuneFraction;
        variation.infectiousProbability = (infectiousProbability != null) ? infectiousProbability : base.infectiousProbability;
        
        variation.roomLength = (roomLength != null) ? roomLength : base.roomLength;
        variation.roomWidth = (roomWidth != null) ? roomWidth : base.roomWidth;
        variation.roomHeight = (roomHeight != null) ? roomHeight : base.roomHeight;
        
        variation.standardExposureHours = (standardExposureHours != null) ? standardExposureHours : base.standardExposureHours;
        variation.standardVisitorCount = (standardVisitorCount != null) ? standardVisitorCount : base.standardVisitorCount;
        
        // Copy base ranges and scale if necessary
        variation.visitorCounts = base.visitorCounts.clone();
        variation.durationMinutes = base.durationMinutes.clone();
        
        // Generate new risks based on modified parameters
        variation.visitorRisksModel = generateVariationRisks(variation, base.visitorCounts);
        variation.durationRisksModel = generateVariationDurationRisks(variation, base.durationMinutes);
        
        return variation;
    }

    /**
     * Simplified version to create variations of epidemiological parameters only
     */
    private static TestScenario createEpidemiologicalVariation(String name, String description,
                                                             Double ventilationRate, 
                                                             Double maskFraction, 
                                                             Double immuneFraction,
                                                             Double infectiousProbability) {
        return createOriginalVariation(name, description, 
                                     ventilationRate, null, maskFraction, immuneFraction, infectiousProbability,
                                     null, null, null, null, null);
    }

    /**
     * Simplified version to create variations of dimensions only
     */
    // private static TestScenario createDimensionalVariation(String name, String description,
    //                                                      double scaleFactor) {
    //     TestScenario base = getOriginalScenario();
        
    //     return createOriginalVariation(name, description,
    //                                  null, null, null, null, null,
    //                                  base.roomLength * scaleFactor,
    //                                  base.roomWidth * scaleFactor, 
    //                                  base.roomHeight * scaleFactor,
    //                                  null, 
    //                                  (int)(base.standardVisitorCount * Math.pow(scaleFactor, 3))); // Cubic scaling for volume
    // }

    /**
     * Generates risks for variations using generic Wells-Riley model
     */
    private static double[] generateVariationRisks(TestScenario scenario, int[] counts) {
        double[] risks = new double[counts.length];
        
        // Base Wells-Riley model parameters
        double quantaEmission = 60.45;              // quanta/h base
        double roomVolume = scenario.roomLength * scenario.roomWidth * scenario.roomHeight;
        double ventilationRate = scenario.ventilationRate;
        double virusDecayRate = scenario.virusDecayRate;
        double exposureTime = scenario.standardExposureHours;
        double breathingRate = 0.72;                // m³/h
        
        // Mask efficiencies
        double exhalationEff = 0.5;
        double inhalationEff = 0.3;
        double maskCompliance = scenario.maskFraction;
        
        for (int i = 0; i < counts.length; i++) {
            int totalPeople = counts[i];
            
            if (totalPeople == 0) {
                risks[i] = 0.0;
                continue;
            }
            
            // Calculate number of infectious
            int infectivePeople = Math.max(1, (int) Math.round(totalPeople * scenario.infectiousProbability));
            
            // Net emission with masks
            double netEmission = quantaEmission * infectivePeople * (1.0 - exhalationEff * maskCompliance);
            
            // Total loss rate
            double totalLossRate = ventilationRate + virusDecayRate;
            
            // Quanta concentration
            double quantaConcentration = netEmission / (roomVolume * totalLossRate);
            
            // Quanta inhaled with protection
            double quantaInhaled = quantaConcentration * breathingRate * exposureTime * 
                                (1.0 - inhalationEff * maskCompliance);
            
            // Infection probability (Wells-Riley model)
            double infectionProb = 1.0 - Math.exp(-quantaInhaled);
            
            risks[i] = infectionProb * 100.0;
        }
        
        return risks;
    }

    /**
     * Generates duration risks for variations
     */
    private static double[] generateVariationDurationRisks(TestScenario scenario, int[] durations) {
        double[] risks = new double[durations.length];
        
        // Fixed parameters using standardVisitorCount
        double quantaEmission = 60.45;
        double roomVolume = scenario.roomLength * scenario.roomWidth * scenario.roomHeight;
        double ventilationRate = scenario.ventilationRate;
        double virusDecayRate = scenario.virusDecayRate;
        double breathingRate = 0.72;
        double exhalationEff = 0.5;
        double inhalationEff = 0.3;
        double maskCompliance = scenario.maskFraction;
        
        int infectivePeople = Math.max(1, (int) Math.round(scenario.standardVisitorCount * scenario.infectiousProbability));
        
        double netEmission = quantaEmission * infectivePeople * (1.0 - exhalationEff * maskCompliance);
        double totalLossRate = ventilationRate + virusDecayRate;
        double quantaConcentration = netEmission / (roomVolume * totalLossRate);
        
        for (int i = 0; i < durations.length; i++) {
            int durationMinutes = durations[i];
            
            if (durationMinutes == 0) {
                risks[i] = 0.0;
                continue;
            }
            
            double exposureHours = durationMinutes / 60.0;
            
            double quantaInhaled = quantaConcentration * breathingRate * exposureHours * 
                                (1.0 - inhalationEff * maskCompliance);
            
            double infectionProb = 1.0 - Math.exp(-quantaInhaled);
            
            risks[i] = infectionProb * 100.0;
        }
        
        return risks;
    }


    /**
     * Gets all predefined scenarios
     */
    public static List<TestScenario> getAllScenarios() {
        List<TestScenario> scenarios = new ArrayList<>();
        
        // Original scenario
        scenarios.add(getOriginalScenario());
        
        // // Variaciones dimensionales (escalado proporcional)
        // scenarios.add(createDimensionalVariation(
        //     "Large Room 2x", 
        //     "Escenario 2x más grande que el original con densidad proporcional", 
        //     2.0));
        
        // scenarios.add(createDimensionalVariation(
        //     "Extra Large Room 3x", 
        //     "Escenario 3x más grande que el original con densidad proporcional", 
        //     3.0));
        
        // //  Variaciones epidemiológicas específicas
        // scenarios.add(createEpidemiologicalVariation(
        //     "Hospital Ward", 
        //     "Sala de hospital: ventilación alta (12h⁻¹), mascarillas obligatorias (95%)",
        //     12.0,    // ventilación alta
        //     0.95,    // mascarillas obligatorias
        //     0.25,    // alta inmunidad (personal vacunado)
        //     0.08));  // alta probabilidad infecciosa (pacientes vulnerables)
        
        // scenarios.add(createEpidemiologicalVariation(
        //     "Industrial Warehouse", 
        //     "Almacén industrial: ventilación limitada (1.2h⁻¹), mascarillas opcionales (30%)",
        //     1.2,     // ventilación muy baja
        //     0.3,     // pocas mascarillas
        //     0.12,    // baja inmunidad
        //     0.025)); // probabilidad media-baja
        
        // scenarios.add(createEpidemiologicalVariation(
        //     "Fine Restaurant", 
        //     "Restaurante de lujo: sin mascarillas (5%), actividad social alta",
        //     4.5,     // ventilación moderada-alta
        //     0.05,    // casi sin mascarillas
        //     0.18,    // inmunidad normal
        //     0.035)); // probabilidad media (actividad social)
        
        // // NUEVOS ESCENARIOS MÁS DISTINTIVOS
        
        // // Escenario de teatro/auditorio
        // scenarios.add(createOriginalVariation(
        //     "Theater Auditorium",
        //     "Teatro/auditorio: espacio alto, ocupación densa, evento largo (2.5h), ventilación moderada",
        //     2.8,      // ventilación moderada-baja
        //     null,     // decaimiento estándar
        //     0.15,     // 15% mascarillas (evento cultural pre-COVID)
        //     0.08,     // baja inmunidad
        //     0.01,     // baja probabilidad infecciosa (audiencia pasiva)
        //     30.0, 20.0, 8.0,  // teatro grande, techo muy alto
        //     2.5,      // función teatral larga
        //     400));    // ocupación densa (400 personas)
        
        // // Escenario de aula universitaria
        // scenarios.add(createOriginalVariation(
        //     "University Classroom",
        //     "Aula universitaria: ventilación limitada, estudiantes jóvenes, clase magistral 1.5h",
        //     1.8,      // ventilación limitada (aula vieja)
        //     null,     // decaimiento estándar
        //     0.2,      // 20% mascarillas (estudiantes poco disciplinados)
        //     0.35,     // alta inmunidad (población joven + vacunados)
        //     0.04,     // probabilidad media (estudiantes activos, hablan)
        //     12.0, 8.0, 3.2,   // aula típica universitaria
        //     1.5,      // clase magistral 1.5h
        //     80));     // aula llena de estudiantes
        
        // // Escenario de gimnasio
        // scenarios.add(createOriginalVariation(
        //     "Fitness Gym",
        //     "Gimnasio: respiración intensa, sin mascarillas, ventilación forzada, actividad física",
        //     8.0,      // ventilación alta (aire acondicionado potente)
        //     0.9,      // decaimiento alto (humedad, temperatura)
        //     0.02,     // casi sin mascarillas (imposible hacer ejercicio)
        //     0.15,     // inmunidad normal-baja
        //     0.06,     // alta probabilidad (respiración intensa, sudor)
        //     25.0, 15.0, 4.0,  // gimnasio mediano
        //     1.0,      // sesión de entrenamiento 1h
        //     60));     // aforo típico de gimnasio
        
        // scenarios.add(createOriginalVariation(
        // "Public Transport Bus",
        // "Autobús urbano: espacio muy confinado, ventilación limitada, alta rotación de personas",
        // 0.8,      // ventilación muy limitada (ventanas cerradas)
        // null,     // decaimiento estándar
        // 0.60,     // 60% mascarillas (transporte público obligatorio)
        // 0.20,     // inmunidad moderada
        // 0.03,     // probabilidad media (viajeros diversos)
        // 12.0, 2.5, 2.2,   // autobús típico: largo, estrecho, bajo
        // 0.75,     // viaje urbano típico 45min
        // 35));     // autobús semi-lleno
    
        // scenarios.add(createOriginalVariation(
        //     "Supermarket",
        //     "Supermercado: espacio grande, actividad comercial intensa, ventilación comercial",
        //     5.5,      // ventilación comercial buena
        //     null,     // decaimiento estándar
        //     0.40,     // 40% mascarillas (clientes mixtos)
        //     0.16,     // inmunidad normal
        //     0.025,    // probabilidad baja-media (actividad comercial)
        //     45.0, 25.0, 3.5,  // supermercado grande
        //     0.5,      // compra típica 30min
        //     120));    // supermercado con actividad normal
        
        return scenarios;
    }

    /**
     * Generates multiple variations for sensitivity analysis
     */
    public static List<TestScenario> getSensitivityAnalysisScenarios() {
        List<TestScenario> scenarios = new ArrayList<>();
        
        // Original scenario as reference
        scenarios.add(getOriginalScenario());
        
        double[] ventilationRates = {0.5, 1.0, 2.0, 5.0, 8.0, 12.0, 20.0};
        for (double rate : ventilationRates) {
            scenarios.add(createEpidemiologicalVariation(
                "Vent_" + rate, 
                "Ventilation analysis: " + rate + " h⁻¹", 
                rate, null, null, null));
        }
         
        double[] maskFractions = {0.0, 0.3, 0.5, 0.7, 0.9, 0.95, 1.0};
        for (double fraction : maskFractions) {
            scenarios.add(createEpidemiologicalVariation(
                "Mask_" + (int)(fraction*100), 
                "Mask analysis: " + (int)(fraction*100) + "%", 
                null, fraction, null, null));
        }
        
        double[] immuneFractions = {0.0, 0.1, 0.2, 0.3, 0.5, 0.7};
        for (double fraction : immuneFractions) {
            scenarios.add(createEpidemiologicalVariation(
                "Immune_" + (int)(fraction*100), 
                "Immunity analysis: " + (int)(fraction*100) + "%", 
                null, null, fraction, null));
        }
        
        double[] infectiousProbs = {0.005, 0.01, 0.03, 0.05, 0.08, 0.1, 0.15};
        for (double prob : infectiousProbs) {
            scenarios.add(createEpidemiologicalVariation(
                "Infect_" + (int)(prob*100), 
                "Infectious prob. analysis: " + (int)(prob*100) + "%", 
                null, null, null, prob));
        }
        
        double[] virusDecayRates = {0.1, 0.3, 0.5, 0.8, 1.0, 1.5, 2.0};
        for (double rate : virusDecayRates) {
            scenarios.add(createOriginalVariation(
                "Decay_" + rate, 
                "Viral decay analysis: " + rate + " h⁻¹",
                null, rate, null, null, null, null, null, null, null, null));
        }
        
        return scenarios;
    }

    /**
     * Scenarios based on real documented outbreaks
     */
    public static List<TestScenario> getRealOutbreakScenarios() {
        List<TestScenario> scenarios = new ArrayList<>();
        
        scenarios.add(createOriginalVariation(
            "Guangzhou Restaurant",
            "Real outbreak: Guangzhou restaurant, ventilation 0.67h⁻¹, attack rate 45%",
            0.67,     // very low ventilation
            null,     // standard decay
            0.0,      // no masks
            0.0,      // no immunity
            0.048,    // 1 infectious out of ~21 people
            10.0, 6.0, 1.6,  // restaurant dimensions
            1.2,      // 1.2h exposure
            21));     // 20 susceptible + 1 infectious
        
        scenarios.add(createOriginalVariation(
            "Skagit Choir",
            "Real outbreak: Skagit choir, superspreader rE=85, attack rate 87%",
            0.7,      // very low ventilation
            null,     // standard decay
            0.0,      // no masks
            0.0,      // no immunity  
            0.016,    // 1 infectious out of ~61 people
            20.0, 15.0, 2.7,  // large room V=810m³
            2.5,      // 2.5h exposure
            61));     // 60 susceptible + 1 infectious
        
        // Call Center
        scenarios.add(createOriginalVariation(
            "Call Center",
            "Real outbreak: call center, 8h work exposure, attack rate 44%",
            6.0,      // good ventilation
            null,     // standard decay
            0.0,      // no masks
            0.0,      // no immunity
            0.0046,   // 1 infectious out of ~217 people
            25.0, 15.0, 1.68,  // office V=630m³
            8.0,      // 8h work shift
            217));    // 216 susceptible + 1 infectious
        
        return scenarios;
    }

    /**
     * Gets scenarios by category
     */
    public static List<TestScenario> getScenariosByCategory(String category) {
        switch (category.toLowerCase()) {
            case "basic":
                return getAllScenarios();
            case "sensitivity":
                return getSensitivityAnalysisScenarios();
            case "real_outbreaks":
                return getRealOutbreakScenarios();
            case "all":
                List<TestScenario> allScenarios = new ArrayList<>();
                allScenarios.addAll(getAllScenarios());
                allScenarios.addAll(getRealOutbreakScenarios());
                return allScenarios;
            default:
                return getAllScenarios();
        }
    }
}