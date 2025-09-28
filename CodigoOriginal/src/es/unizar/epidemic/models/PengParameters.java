package es.unizar.epidemic.models;

/**
 * Parameters based on the aerosol model described in Supermkt: https://docs.google.com/spreadsheets/d/16K1OQkLD4BjgBdO8ePj6ytf-RpPMlJ6aXFg3PrIQBbQ/edit?gid=519189277#gid=519189277
 * Añadido por Nacho Palacio 2025-07-11
 */
public class PengParameters {
    // Some parameters are modified depending on the map
    
    // PARÁMETROS AMBIENTALES 
    private double roomLength = 24.4;           // meters
    private double roomWidth = 15.3;            // meters  
    private double roomHeight = 5.5;            // meters
    private double roomVolume = 2043.0;         // m³
    private double roomArea = 372.0;            // m²
    
    private double pressure = 0.95;             // atm
    private double temperature = 20.0;          // °C
    private double relativeHumidity = 50.0;     // %
    private double backgroundCO2 = 415.0;       // ppm
    
    // PARÁMETROS DE VENTILACIÓN 
    private double ventilationRate = 3.0;      // h⁻¹
    private double virusDecayRate = 0.62;       // h⁻¹
    private double depositionRate = 0.3;        // h⁻¹
    private double additionalControlMeasures = 0.0; // h⁻¹
    private double totalFirstOrderLossRate = 3.92;  // h⁻¹
    private double ventilationRatePerPerson = 22.7; // L/s/person
    
    // PARÁMETROS DE PERSONAS 
    private int totalPeople = 75;
    private int infectivePeople = 1;
    private double fractionImmune = 0.06;       // 6%
    private double susceptiblePeople = 69.56;
    
    private double densityAreaPerPerson = 53.0; // sq ft/person (convertir a m²)
    private double densityPeoplePerM2 = 0.20;   // persons/m²
    private double densityVolumePerPerson = 27.2; // m³/person
    
    // PARÁMETROS RESPIRATORIOS 
    private double breathingRateSusceptibles = 0.72; // m³/h
    private double relativeBreathingRateFactor = 2.50;
    private double co2EmissionRatePerPerson = 0.00675; // L/s
    
    // PARÁMETROS DE QUANTA
    private double basicQuantaExhalationRate = 18.6;  // quanta/h
    private double quantaEnhancementVariant = 2.5;
    private double quantaEnhancementActivity = 5.0;
    private double quantaExhalationInfected = 232.5;  // quanta/h
    
    // PARÁMETROS DE MASCARILLAS 
    private double exhalationMaskEfficiency = 0.50;   // 50%
    private double fractionPeopleWithMasks = 0.1;     // 100%
    private double inhalationMaskEfficiency = 0.30;   // 30%
    
    // PARÁMETROS DE ENFERMEDAD 
    private double probabilityBeingInfective = 0.001; // 0.10%
    private double hospitalizationRate = 0.10;        // 10%
    private double deathRate = 0.01;                   // 1%

    // PARÁMETROS DERIVADOS DEL MODELO
    private double netEmissionRate = 116.25;          // quanta/h
    private double avgQuantaConcentration = 0.01;     // quanta/m³
    private double quantaInhaledPerPerson = 0.06;
    
    // Riesgo individual
    private double infectionProbability = 0.055;      // 5.5%
    
    // PARÁMETROS ORIGINALES 
    private double baseTransmissionProbability = 0.055;
    private double virusLifetimeInAerosol = 1.61;      // horas
    private int incubationPeriod = 5;                  // días
    private int infectiousPeriod = 10;                 // días
    

    public PengParameters() {
        this.roomVolume = roomLength * roomWidth * roomHeight;
        this.roomArea = roomLength * roomWidth;
        this.totalFirstOrderLossRate = ventilationRate + virusDecayRate + depositionRate + additionalControlMeasures;
    }
    

    
    /**
     * Calculates the quanta concentration in the room
     */
    public double calculateQuantaConcentration(int infectiousPeople) {
        double netEmission = quantaExhalationInfected * infectiousPeople * (1.0 - exhalationMaskEfficiency * fractionPeopleWithMasks);
        double denominatorFactor = roomVolume * totalFirstOrderLossRate;
        double concentration = netEmission / denominatorFactor;
        
        return concentration;
    }
    
    /**
     * Calculates the infection probability based on exposure time and number of infectious people
     */
    public double calculateInfectionProbability(double exposureTimeHours, int infectiousPeople) {
        double quantaConcentration = calculateQuantaConcentration(infectiousPeople);
        double quantaInhaled = quantaConcentration * breathingRateSusceptibles * exposureTimeHours;
        
        quantaInhaled *= (1.0 - inhalationMaskEfficiency * fractionPeopleWithMasks);
    
        double baseInfectionProb = 1.0 - Math.exp(-quantaInhaled);

        double immunityReduction = 1.0 - (fractionImmune * 0.7);
        double finalInfectionProb = baseInfectionProb * immunityReduction;
        
        return Math.min(1.0, Math.max(0.0, finalInfectionProb));
    }

    /**
     * Calculates the infection probability with a custom mask protection factor
     */
    public double calculateInfectionProbability(double exposureTimeHours, int infectiousPeople, double maskProtectionFactor) {
        double quantaConcentration = calculateQuantaConcentration(infectiousPeople);
        double quantaInhaled = quantaConcentration * breathingRateSusceptibles * exposureTimeHours;
        
        quantaInhaled *= maskProtectionFactor;
        
        return 1.0 - Math.exp(-quantaInhaled);
    }
    
    /**
     * Calculates the CO2 concentration as a risk indicator
     */
    public double calculateCO2Concentration(int totalPeople) {
        if (totalPeople <= 0) return backgroundCO2;

        double totalCO2Emission = co2EmissionRatePerPerson * totalPeople;
        double ventilationVolumeRate = ventilationRate * roomVolume / 3600.0;

        double co2EmissionM3s = totalCO2Emission / 1000.0;
    
        double co2Addition = (co2EmissionM3s / ventilationVolumeRate) * 1000000;
        
        return backgroundCO2 + co2Addition;
    }
    
    
    // GETTERS 
    public double getRoomLength() { return roomLength; }
    public double getRoomWidth() { return roomWidth; }
    public double getRoomHeight() { return roomHeight; }
    public double getRoomVolume() { return roomVolume; }
    public double getRoomArea() { return roomArea; }
    public double getPressure() { return pressure; }
    public double getTemperature() { return temperature; }
    public double getRelativeHumidity() { return relativeHumidity; }
    public double getBackgroundCO2() { return backgroundCO2; }
    
    public double getVentilationRate() { return ventilationRate; }
    public double getVirusDecayRate() { return virusDecayRate; }
    public double getDepositionRate() { return depositionRate; }
    public double getTotalFirstOrderLossRate() { return totalFirstOrderLossRate; }
    public double getVentilationRatePerPerson() { return ventilationRatePerPerson; }
    
    public int getTotalPeople() { return totalPeople; }
    public int getInfectivePeople() { return infectivePeople; }
    public double getFractionImmune() { return fractionImmune; }
    public double getSusceptiblePeople() { return susceptiblePeople; }
    public double getDensityPeoplePerM2() { return densityPeoplePerM2; }
    public double getDensityVolumePerPerson() { return densityVolumePerPerson; }

    public double getBreathingRateSusceptibles() { return breathingRateSusceptibles; }
    public double getRelativeBreathingRateFactor() { return relativeBreathingRateFactor; }
    public double getCO2EmissionRatePerPerson() { return co2EmissionRatePerPerson; }

    public double getBasicQuantaExhalationRate() { return basicQuantaExhalationRate; }
    public double getQuantaEnhancementVariant() { return quantaEnhancementVariant; }
    public double getQuantaEnhancementActivity() { return quantaEnhancementActivity; }
    public double getQuantaExhalationInfected() { return quantaExhalationInfected; }

    public double getExhalationMaskEfficiency() { return exhalationMaskEfficiency; }
    public double getFractionPeopleWithMasks() { return fractionPeopleWithMasks; }
    public double getInhalationMaskEfficiency() { return inhalationMaskEfficiency; }

    public double getProbabilityBeingInfective() { return probabilityBeingInfective; }
    public double getHospitalizationRate() { return hospitalizationRate; }
    public double getDeathRate() { return deathRate; }

    public double getNetEmissionRate() { return netEmissionRate; }
    public double getAvgQuantaConcentration() { return avgQuantaConcentration; }
    public double getQuantaInhaledPerPerson() { return quantaInhaledPerPerson; }
    public double getInfectionProbability() { return infectionProbability; }

    public double getBaseTransmissionProbability() { return baseTransmissionProbability; }
    public double getVirusLifetimeInAerosol() { return virusLifetimeInAerosol; }
    public int getIncubationPeriod() { return incubationPeriod; }
    public int getInfectiousPeriod() { return infectiousPeriod; }

    /**
     * Gets pixels per meter
     */
    public static double getPixelsPerMeter() {
        return es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    /**
     * Converts distance from pixels to meters
     */
    public static double pixelsToMeters(double pixels) {
        return pixels / getPixelsPerMeter();
    }

    /**
     * Converts distance from meters to pixels
     */
    public static double metersToPixels(double meters) {
        return meters * getPixelsPerMeter();
    }
  
    
    // SETTERS 
    public void setRoomDimensions(double length, double width, double height) {
        this.roomLength = length;
        this.roomWidth = width;
        this.roomHeight = height;
        this.roomVolume = length * width * height;
        this.roomArea = length * width;
    }

    public void setRoomDimensionsFromPixels(double lengthPixels, double widthPixels, double heightPixels) {
        double pixelsPerMeter = getPixelsPerMeter();
        this.roomLength = lengthPixels / pixelsPerMeter;
        this.roomWidth = widthPixels / pixelsPerMeter;
        this.roomHeight = heightPixels / pixelsPerMeter;
        this.roomVolume = roomLength * roomWidth * roomHeight;
        this.roomArea = roomLength * roomWidth;
    }
    
    public void setVentilationRate(double ventilationRate) {
        this.ventilationRate = ventilationRate;
        updateTotalFirstOrderLossRate();
    }
    
    public void setVirusDecayRate(double virusDecayRate) {
        this.virusDecayRate = virusDecayRate;
        this.virusLifetimeInAerosol = 1.0 / virusDecayRate;
        updateTotalFirstOrderLossRate();
    }
    
    public void setMaskParameters(double exhalationEff, double inhalationEff, double fraction) {
        this.exhalationMaskEfficiency = exhalationEff;
        this.inhalationMaskEfficiency = inhalationEff;
        this.fractionPeopleWithMasks = fraction;
    }
    
    public void setPeopleCount(int totalPeople, int infectivePeople) {
        this.totalPeople = totalPeople;
        this.infectivePeople = infectivePeople;
        this.susceptiblePeople = totalPeople * (1.0 - fractionImmune) - infectivePeople;
        this.densityPeoplePerM2 = totalPeople / roomArea;
        this.densityVolumePerPerson = roomVolume / totalPeople;
    }

    public void setFractionImmune(double fractionImmune) {
        this.fractionImmune = fractionImmune;
        if (this.totalPeople > 0) {
            this.susceptiblePeople = totalPeople * (1.0 - fractionImmune) - infectivePeople;
        }
    }

    public void setBasicQuantaExhalationRate(double basicQuantaExhalationRate) {
        this.basicQuantaExhalationRate = basicQuantaExhalationRate;
    }
    
    private void updateTotalFirstOrderLossRate() {
        this.totalFirstOrderLossRate = ventilationRate + virusDecayRate + depositionRate + additionalControlMeasures;
    }

    public void setBreathingRateSusceptibles(double breathingRate) {
        this.breathingRateSusceptibles = breathingRate;
    }

    public void setDepositionRate(double depositionRate2) {
        this.depositionRate = depositionRate2;
    }
}