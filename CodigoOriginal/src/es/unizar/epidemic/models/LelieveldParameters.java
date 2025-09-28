package es.unizar.epidemic.models;

/**
 * Parámetros para el modelo de transmisión por aerosoles basado en Lelieveld et al. (2020)
 * Implementado a partir de modelAerosol2.txt
 * Añadido por Nacho Palacio 2025-07-27
 */
public class LelieveldParameters {
    // PARÁMETROS DEL VIRUS
    private double virusLifetimeHours = 1.7;           // h
    private double virusDecayRateHour = 0.59;          // h⁻¹ (1/lifetime)

    // PARÁMETROS DE GENERACIÓN DE AEROSOLES
    private double concentrationBreathingCm3 = 0.1;    // partículas/cm³
    private double concentrationSpeakingCm3 = 1.1;     // partículas/cm³ (antes 1.1 -> 0.11)
    private double speakingBreathingRatio = 0.10;      // fracción de tiempo hablando
    private double respiratoryRateLmin = 10.0;         // L/min
    private double respiratoryRateM3h = 0.6;           // m³/h (calculado)

    // CARGA VIRAL
    // Modificado porque 5e8 es el momento de máxima carga viral, no la carga típica
    private double viralLoadHighCm3 = 1.5e7;             // copias RNA/cm³ (antes 5e8 -> 1.5e6, 1.5e7)
    private double viralLoadSuperCm3 = 5e9;            // copias RNA/cm³

    // PARÁMETROS DE DEPOSICIÓN E INFECCIÓN
    private double depositionProbability = 0.5;        // probabilidad
    private double infectiveDoseD50 = 100;             // copias RNA (antes 316)

    // PARÁMETROS DEL ENTORNO
    private double roomAreaM2 = 60.0;                  // m²
    private double roomHeightM = 3.0;                  // m
    private double roomVolumeM3 = 180.0;               // m³
    private double roomLengthM = 10.0;                 // m
    private double roomWidthM = 6.0;                   // m
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
    private double fractionPeopleWithMasks = 0.1;     // 10%


    // CAMPOS AVANZADOS
    private boolean hepaFilterOn = false;

    public LelieveldParameters() {
        updateDerivedParameters();
    }

    /**
     * Updates derived parameters based on the current settings.
     */
    private void updateDerivedParameters() {     
        this.roomVolumeM3 = roomLengthM * roomWidthM * roomHeightM;
        this.roomAreaM2 = roomLengthM * roomWidthM;  
        this.susceptiblePeople = subjectsInRoom - infectivePeople;
        this.respiratoryRateM3h = respiratoryRateLmin * 60 / 1000;
        
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
        double avgEmissionConcentration = concentrationBreathingCm3 * (1 - speakingBreathingRatio) + 
                                        concentrationSpeakingCm3 * speakingBreathingRatio;
        double viralEmission = avgEmissionConcentration * viralLoadCm3;
        double maskFactor = 1.0 - (maskEfficiencyExh * fractionWithMasks);
    
        if (maskFactor <= 0) {
            System.out.println("   ❌ ERROR: Factor mascarilla <= 0, devolviendo 0");
            return 0.0;
        }
        
        double totalEmission = viralEmission * respiratoryRateM3h * infectivePeople * maskFactor;

        double lossRate = totalVentilationRateH + virusDecayRateHour;
        
        double denominatorFactor = roomVolumeM3 * lossRate;
        
        double finalConcentration = totalEmission / denominatorFactor;
        
        return finalConcentration;
    }

    /**
     * Calculates the probability of infection using dose-response model
     */
    public double calculateInfectionProbability(double timeHours, double viralLoadCm3, double maskProtectionFactor) {
        if (timeHours <= 0) {
            return 0.0;
        }
        if (viralLoadCm3 <= 0) {
            return 0.0;
        }

        // Modificado por Nacho Palacio 2025-09-25
        // double fractionMasks = subjectsInRoom > 0 ? (subjectsInRoom - infectivePeople) / (double)subjectsInRoom : 0;
        // double concentration = calculateViralConcentration(viralLoadCm3, fractionMasks);
        
        double concentration = calculateViralConcentration(viralLoadCm3, fractionPeopleWithMasks);
        
        double inhalaedDose = concentration * respiratoryRateM3h * timeHours * maskProtectionFactor * depositionProbability;

        double PRNA = calculateSingleVirusProbability();
        
        double infectionProb = 1.0 - Math.pow(1.0 - PRNA, inhalaedDose);

        double immunityReduction = 1.0 - (fractionImmune * 0.7); // Inmunidad reduce 70% la transmisión
        double finalInfectionProb = infectionProb * immunityReduction;
        
        return Math.min(1.0, Math.max(0.0, finalInfectionProb));
    }

    /**
     * Calculates the probability of infection for a group of susceptible people
     */
    public double calculateGroupInfectionProbability(double timeHours, double viralLoadCm3, 
                                                double maskProtectionFactor, int susceptibleCount) {
        double fractionMasks = subjectsInRoom > 0 ? (subjectsInRoom - infectivePeople) / (double)subjectsInRoom : 0;
        double concentration = calculateViralConcentration(viralLoadCm3, fractionMasks);
        
        double inhalaedDoseIndividual = concentration * respiratoryRateM3h * timeHours * 
                                    maskProtectionFactor * depositionProbability;

        double inhalaedDoseGroup = inhalaedDoseIndividual * susceptibleCount;
        
        double PRNA = calculateSingleVirusProbability();
        double groupInfectionProb = 1.0 - Math.pow(1.0 - PRNA, inhalaedDoseGroup);
        
        if (groupInfectionProb > 1) {
            groupInfectionProb = 1.0;
        }
        
        return groupInfectionProb;
    }

    /**
     * Calculate concentration of CO2 in ppm
     */
    public double calculateCO2Concentration() {
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
    public double getFractionPeopleWithMasks() { return fractionPeopleWithMasks; }
    public double getFractionImmune() { return fractionImmune; }
    public int getImmunePeople() { return (int) Math.round(subjectsInRoom * fractionImmune); }


    // SETTERS
    public void setRoomDimensions(double length, double width, double height) {
        if (length <= 0 || width <= 0 || height <= 0) {
            System.out.println(" ERROR: Dimensiones <= 0!");
            return;
        }
        
        this.roomLengthM = length;
        this.roomWidthM = width;
        this.roomHeightM = height;

        updateDerivedParameters();
    }

    public void setVentilationRates(double passive, double active, boolean useHepa) {
        if (passive < 0 || active < 0) {
            System.out.println(" ERROR: Tasas de ventilación negativas!");
            return;
        }

        this.ventilationRatePassiveH = passive;
        this.ventilationRateActiveH = active;
        this.hepaFilterOn = useHepa;
        
        updateDerivedParameters();
    }

    public void setMaskParameters(double inhEff, double exhEff, double fraction) {
        if (inhEff < 0 || inhEff > 1 || exhEff < 0 || exhEff > 1) {
            System.out.println(" ERROR: Eficiencias de mascarilla fuera de rango [0,1]!");
            return;
        }
        
        this.maskEfficiencyInh = inhEff;
        this.maskEfficiencyExh = exhEff;
        this.maskEfficiencyTotal = inhEff + exhEff;
        this.fractionPeopleWithMasks = fraction;
        
        updateDerivedParameters();
    }

    public void setPeopleCount(int total, int infective) {   
        if (total < 0 || infective < 0) {
            System.out.println(" ERROR: Número de personas negativo!");
            return;
        }
        if (infective > total) {
            System.out.println(" ERROR: Más infectivos que el total!");
            return;
        }
        
        this.subjectsInRoom = total;
        this.infectivePeople = infective;

        int immunePeople = (int) Math.round(total * fractionImmune);
        this.susceptiblePeople = total - infective - immunePeople;

        if (this.susceptiblePeople < 0) {
            this.susceptiblePeople = Math.max(0, total - infective);
        }
        
        updateDerivedParameters();
    }

    public void setFractionImmune(double fractionImmune) {
        if (fractionImmune < 0 || fractionImmune > 1) {
            System.out.println(" ERROR: Fracción inmune fuera de rango [0,1]!");
            return;
        }
        
        this.fractionImmune = fractionImmune;
        
        if (this.subjectsInRoom > 0) {
            int immunePeople = (int) Math.round(subjectsInRoom * fractionImmune);
            this.susceptiblePeople = subjectsInRoom - infectivePeople - immunePeople;
        }
        
        updateDerivedParameters();
    }
    
    public static double pixelsToMeters(double pixels) {
        return pixels / es.unizar.gui.Configuration.getPixelsPerMeter();
    }
    
    public static double metersToPixels(double meters) {
        return meters * es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    public void setDepositionProbability(double depositionProbability2) {
        this.depositionProbability = depositionProbability2;
    }

    public void setInfectiveDoseD50(double infectiousDose) {
        this.infectiveDoseD50 = infectiousDose;
    }
}