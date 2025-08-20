package es.unizar.epidemic.models;

/**
 * Parámetros para el modelo de transmisión por aerosoles basado en Lelieveld et al. (2020)
 * Implementado a partir de modelAerosol2.txt
 * Añadido por Nacho Palacio 2025-07-27
 */
public class ModelParameters2 {
    // PARÁMETROS DEL VIRUS/EPISODIO INFECCIOSO
    private double infectiousEpisodeDays = 2.0;        // días
    private double virusLifetimeHours = 1.7;           // h
    private double virusDecayRateHour = 0.59;          // h⁻¹ (1/lifetime)
    private double aerosolDiameterUm = 5.0;            // μm

    // PARÁMETROS DE GENERACIÓN DE AEROSOLES/VIRUS
    private double concentrationBreathingCm3 = 0.1;    // partículas/cm³
    private double concentrationSpeakingCm3 = 1.1;     // partículas/cm³
    private double speakingBreathingRatio = 0.10;      // fracción de tiempo hablando
    private double respiratoryRateLmin = 10.0;         // L/min
    private double respiratoryRateM3h = 0.6;           // m³/h (calculado)

    // CARGA VIRAL
    private double viralLoadHighCm3 = 5e8;             // copias RNA/cm³
    private double viralLoadSuperCm3 = 5e9;            // copias RNA/cm³

    // PARÁMETROS DE DEPOSICIÓN E INFECCIÓN
    private double depositionProbability = 0.5;        // probabilidad
    private double infectiveDoseD50 = 316;             // copias RNA

    // PARÁMETROS DEL ENTORNO
    private double roomAreaM2 = 60.0;                  // m²
    private double roomHeightM = 3.0;                  // m
    private double roomVolumeM3 = 180.0;               // m³
    private double roomLengthM = 10.0;                 // m
    private double roomWidthM = 6.0;                   // m
    private double temperatureC = 20.0;                // °C
    private double relativeHumidityPct = 50;           // %
    private double backgroundCo2Ppm = 415;             // ppm

    // VENTILACIÓN
    private double ventilationRatePassiveH = 0.35;     // h⁻¹
    private double ventilationRateActiveH = 2.0;       // h⁻¹
    private double hepaRateH = 9.0;                    // h⁻¹
    private double totalVentilationRateH = 2.35;       // h⁻¹ (pasiva + activa)

    // PARÁMETROS DE OCUPANTES
    private int subjectsInRoom = 25;                   // personas
    private int infectivePeople = 1;                   // personas
    private double fractionImmune = 0.0;               // fracción
    private int susceptiblePeople = 24;                // personas

    // MASCARILLAS
    private double maskEfficiencyInh = 0.3;            // fracción
    private double maskEfficiencyExh = 0.4;            // fracción
    private double maskEfficiencyTotal = 0.7;          // fracción
    private String maskType = "surgical";              // tipo

    // EVENTO / TIEMPO DE EXPOSICIÓN
    private double exposureDurationHours = 6.0;        // horas/día
    private int numExposureDays = 2;                   // días

    // CAMPOS AVANZADOS
    private boolean hepaFilterOn = false;              // activado/desactivado
    private String scenarioLabel = "clase";            // etiqueta

    public ModelParameters2() {
        updateDerivedParameters();

        // Añadido para debug
        System.out.println("🔬 PARÁMETROS MODELO LELIEVELD:");
        System.out.println(String.format("   Carga viral alta: %.2e", viralLoadHighCm3));
        System.out.println(String.format("   Carga viral SUPER: %.2e (%.0fx mayor)", 
                        viralLoadSuperCm3, viralLoadSuperCm3 / viralLoadHighCm3));
    }

    /**
     * Updates derived parameters based on the current settings.
     */
    private void updateDerivedParameters() {
        this.roomVolumeM3 = roomLengthM * roomWidthM * roomHeightM;
        this.roomAreaM2 = roomLengthM * roomWidthM;
        this.susceptiblePeople = subjectsInRoom - infectivePeople;
        this.respiratoryRateM3h = respiratoryRateLmin * 60 / 1000;  // L/min → m³/h

        // Añadido para debug
        System.out.println(String.format("🔧 updateDerivedParameters: infectivePeople=%d, susceptiblePeople=%d", 
                      this.infectivePeople, this.susceptiblePeople));
        
        this.totalVentilationRateH = ventilationRatePassiveH + ventilationRateActiveH;
        if (hepaFilterOn) {
            this.totalVentilationRateH += hepaRateH;
        }
        
        if (virusDecayRateHour <= 0) {
            this.virusDecayRateHour = 1.0 / virusLifetimeHours;
        }
    }

    /**
     * Calculates the viral concentration in the air (RNA copies/m³)
     */
    public double calculateViralConcentration(double viralLoadCm3, double fractionWithMasks) {
        // Añadido para debug
        System.out.println(String.format("🔧 === CÁLCULO CONCENTRACIÓN VIRAL ==="));
        System.out.println(String.format("🔧 INPUT: viralLoad=%.2e, fractionMasks=%.2f", viralLoadCm3, fractionWithMasks));
        System.out.println(String.format("🔧 PARÁMETROS: infectivePeople=%d, subjectsInRoom=%d, roomVolume=%.1f", 
                        infectivePeople, subjectsInRoom, roomVolumeM3));

        double avgEmissionConcentration = concentrationBreathingCm3 * (1 - speakingBreathingRatio) + 
                                          concentrationSpeakingCm3 * speakingBreathingRatio;
        
        double viralEmission = avgEmissionConcentration * viralLoadCm3;
        
        double maskFactor = 1.0 - (maskEfficiencyExh * fractionWithMasks);
        
        double totalEmission = viralEmission * respiratoryRateM3h * infectivePeople * maskFactor;
        
        double lossRate = totalVentilationRateH + virusDecayRateHour;

        double finalConcentration = totalEmission / (roomVolumeM3 * lossRate);
        
        // Añadido para debug
        System.out.println(String.format("🔧 CÁLCULO: emission=%.2e, totalEmission=%.2e, finalConc=%.2e", 
                      viralEmission, totalEmission, finalConcentration));
    
        System.out.println(String.format("🧮 CÁLCULO CONCENTRACIÓN: input=%.2e → emisión=%.2e → final=%.2e (vol=%.1f, pérdidas=%.2f)",
                        viralLoadCm3, totalEmission, finalConcentration, roomVolumeM3, lossRate));
        
        return totalEmission / (roomVolumeM3 * lossRate);
    }

    /**
     * Calculates the probability of infection using dose-response model
     */
    public double calculateInfectionProbability(double timeHours, double viralLoadCm3, double maskProtectionFactor) {
        double concentration = calculateViralConcentration(viralLoadCm3, 
                            subjectsInRoom > 0 ? (subjectsInRoom - infectivePeople) / (double)subjectsInRoom : 0);

        System.out.println(String.format("🎯 PROB INFECCIÓN: conc=%.2e, tiempo=%.4f h, dosis=%.2e",
                      concentration, timeHours, concentration * respiratoryRateM3h * timeHours * maskProtectionFactor * depositionProbability));
        
        double inhalaedDose = concentration * respiratoryRateM3h * timeHours * 
                            maskProtectionFactor * depositionProbability;
        
        double PRNA = calculateSingleVirusProbability();
        
        return 1.0 - Math.pow(1.0 - PRNA, inhalaedDose);
    }

    /**
     * Calculates the probability of infection for a group of susceptible people
     */
    public double calculateGroupInfectionProbability(double timeHours, double viralLoadCm3, 
                                                double maskProtectionFactor, int susceptibleCount) {
        double concentration = calculateViralConcentration(viralLoadCm3, 
                            subjectsInRoom > 0 ? (subjectsInRoom - infectivePeople) / (double)subjectsInRoom : 0);
        
        double inhalaedDose = concentration * respiratoryRateM3h * timeHours * 
                            maskProtectionFactor * depositionProbability;
        
        double PRNA = calculateSingleVirusProbability();
        
        return 1.0 - Math.pow(1.0 - PRNA, inhalaedDose * susceptibleCount);
    }

    /**
     * Calcula la concentración de CO2 como indicador de riesgo
     */
    public double calculateCO2Concentration() {
        // Tasa de emisión CO2: ~0.004 L/s/persona = 0.014 m³/h/persona
        double co2EmissionRateM3h = 0.014 * subjectsInRoom;
        
        return backgroundCo2Ppm + (co2EmissionRateM3h * 1000000) / (totalVentilationRateH * roomVolumeM3);
    }

    /**
     * Calculates the probability of inhaling a single virus
     */
    public double calculateSingleVirusProbability() {
        return 1.0 - Math.pow(10, Math.log10(0.5) / infectiveDoseD50);
    }

    // GETTERS
    
    public double getViralLoadHighCm3() { return viralLoadHighCm3; }
    public double getViralLoadSuperCm3() { return viralLoadSuperCm3; }
    public double getInfectiveDoseD50() { return infectiveDoseD50; }
    public double getRoomVolumeM3() { return roomVolumeM3; }
    public double getDepositionProbability() { return depositionProbability; }
    public double getTotalVentilationRateH() { return totalVentilationRateH; }
    public double getRespiratoryRateM3h() { return respiratoryRateM3h; }
    public double getMaskEfficiencyTotal() { return maskEfficiencyTotal; }
    public double getMaskEfficiencyInh() { return maskEfficiencyInh; }
    public double getMaskEfficiencyExh() { return maskEfficiencyExh; }
    public int getInfectivePeople() { return infectivePeople; }

    // SETTERS
    public void setRoomDimensions(double length, double width, double height) {
        this.roomLengthM = length;
        this.roomWidthM = width;
        this.roomHeightM = height;
        updateDerivedParameters();
    }

    public void setVentilationRates(double passive, double active, boolean useHepa) {
        this.ventilationRatePassiveH = passive;
        this.ventilationRateActiveH = active;
        this.hepaFilterOn = useHepa;
        updateDerivedParameters();
    }

    public void setMaskParameters(double inhEff, double exhEff, double fraction) {
        this.maskEfficiencyInh = inhEff;
        this.maskEfficiencyExh = exhEff;
        this.maskEfficiencyTotal = inhEff + exhEff;
        updateDerivedParameters();
    }
    
    public void setPeopleCount(int total, int infective) {
        // Añadido para debug
        System.out.println(String.format("🔧 ANTES setPeopleCount: infectivePeople=%d, subjectsInRoom=%d", 
                      this.infectivePeople, this.subjectsInRoom));

        this.subjectsInRoom = total;
        this.infectivePeople = infective;

        // Añadido para debug
        System.out.println(String.format("🔧 DESPUÉS setPeopleCount: infectivePeople=%d, subjectsInRoom=%d", 
                      this.infectivePeople, this.subjectsInRoom));

        updateDerivedParameters();
    }
    
    public static double pixelsToMeters(double pixels) {
        return pixels / es.unizar.gui.Configuration.getPixelsPerMeter();
    }
    
    public static double metersToPixels(double meters) {
        return meters * es.unizar.gui.Configuration.getPixelsPerMeter();
    }
}