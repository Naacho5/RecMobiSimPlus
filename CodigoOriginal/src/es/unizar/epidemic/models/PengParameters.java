package es.unizar.epidemic.models;

/**
 * Parameters based on the aerosol model described in Supermkt: https://docs.google.com/spreadsheets/d/16K1OQkLD4BjgBdO8ePj6ytf-RpPMlJ6aXFg3PrIQBbQ/edit?gid=519189277#gid=519189277
 * 
 * @author Nacho Palacio
 */
public class PengParameters {
    // Some parameters are modified depending on the map
    
    // AMBIENT PARAMETERS
    private double roomLength = 24.4;           // meters
    private double roomWidth = 15.3;            // meters  
    private double roomHeight = 5.5;            // meters
    private double roomVolume = 2043.0;         // m³
    private double roomArea = 372.0;            // m²
    
    private double pressure = 0.95;             // atm
    private double temperature = 20.0;          // °C
    private double relativeHumidity = 50.0;     // %
    private double backgroundCO2 = 415.0;       // ppm
    
    // VENTILATION PARAMETERS
    private double ventilationRate = 3.0;      // h⁻¹
    private double virusDecayRate = 0.62;       // h⁻¹
    private double depositionRate = 0.3;        // h⁻¹
    private double additionalControlMeasures = 0.0; // h⁻¹
    private double totalFirstOrderLossRate = 3.92;  // h⁻¹
    private double ventilationRatePerPerson = 22.7; // L/s/person
    
    // OCCUPANT PARAMETERS
    private int totalPeople = 75;
    private int infectivePeople = 1;
    private double fractionImmune = 0.06;       // 6%
    private double susceptiblePeople = 69.56;
    
    private double densityPeoplePerM2 = 0.20;   // persons/m²
    private double densityVolumePerPerson = 27.2; // m³/person
    
    // RESPIRATORY PARAMETERS 
    private double breathingRateSusceptibles = 0.72; // m³/h
    private double relativeBreathingRateFactor = 2.50;
    private double co2EmissionRatePerPerson = 0.00675; // L/s
    
    // QUANTA PARAMETERS
    private double basicQuantaExhalationRate = 18.6;  // quanta/h
    private double quantaEnhancementVariant = 2.5;
    private double quantaEnhancementActivity = 5.0;
    private double quantaExhalationInfected = 232.5;  // quanta/h
    
    // MASK PARAMETERS 
    private double exhalationMaskEfficiency = 0.50;   // 50%
    private double fractionPeopleWithMasks = 0.1;     // 100%
    private double inhalationMaskEfficiency = 0.30;   // 30%
    
    // DISEASE PARAMETERS 
    private double probabilityBeingInfective = 0.001; // 0.10%
    private double hospitalizationRate = 0.10;        // 10%
    private double deathRate = 0.01;                   // 1%

    // DERIVED MODEL PARAMETERS
    private double netEmissionRate = 116.25;          // quanta/h
    private double avgQuantaConcentration = 0.01;     // quanta/m³
    private double quantaInhaledPerPerson = 0.06;
    
    // Individual risk
    private double infectionProbability = 0.055;      // 5.5%
    
    // ORIGINAL PARAMETERS 
    private double baseTransmissionProbability = 0.055;
    private double virusLifetimeInAerosol = 1.61;      // hours
    private int incubationPeriod = 5;                  // days
    private int infectiousPeriod = 10;                 // days
    

    public PengParameters() {
        this.roomVolume = roomLength * roomWidth * roomHeight;
        this.roomArea = roomLength * roomWidth;
        this.totalFirstOrderLossRate = ventilationRate + virusDecayRate + depositionRate + additionalControlMeasures;
    }
    

    
    /**
     * Calculates the quanta concentration in the room.
     * Takes into account the number of infectious people, exhalation rates,
     * mask efficiency, room volume, and loss rates.
     * 
     * @param infectiousPeople number of infectious people in the room
     * @return quanta concentration in quanta/m³
     */
    public double calculateQuantaConcentration(int infectiousPeople) {
        double netEmission = quantaExhalationInfected * infectiousPeople * (1.0 - exhalationMaskEfficiency * fractionPeopleWithMasks);
        double denominatorFactor = roomVolume * totalFirstOrderLossRate;
        double concentration = netEmission / denominatorFactor;
        
        return concentration;
    }
    
    /**
     * Calculates the infection probability based on exposure time and number of infectious people.
     * Uses quanta concentration, breathing rate, mask efficiency, and immunity fraction
     * to determine infection risk.
     * 
     * @param exposureTimeHours exposure time in hours
     * @param infectiousPeople number of infectious people in the room
     * @return infection probability (0.0 to 1.0)
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
     * Calculates the infection probability with a custom mask protection factor.
     * Uses quanta concentration and breathing rate multiplied by the protection factor.
     * 
     * @param exposureTimeHours exposure time in hours
     * @param infectiousPeople number of infectious people in the room
     * @param maskProtectionFactor custom mask protection factor (1.0 = no mask)
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateInfectionProbability(double exposureTimeHours, int infectiousPeople, double maskProtectionFactor) {
        double quantaConcentration = calculateQuantaConcentration(infectiousPeople);
        double quantaInhaled = quantaConcentration * breathingRateSusceptibles * exposureTimeHours;
        
        quantaInhaled *= maskProtectionFactor;
        
        return 1.0 - Math.exp(-quantaInhaled);
    }
    
    /**
     * Calculates the CO2 concentration as a risk indicator.
     * Based on CO2 emission rates per person, ventilation rate, and background CO2.
     * 
     * @param totalPeople total number of people in the room
     * @return CO2 concentration in ppm
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
    
    /** Gets the room length in meters. 
     * 
     * @return room length in meters 
     */
    public double getRoomLength() { return roomLength; }
    
    /** Gets the room width in meters. 
     * 
     * @return room width in meters 
     */
    public double getRoomWidth() { return roomWidth; }
    
    /** Gets the room height in meters. 
     * 
     * @return room height in meters 
     */
    public double getRoomHeight() { return roomHeight; }
    
    /** Gets the room volume in cubic meters. 
     * 
     * @return room volume in m³ 
     */
    public double getRoomVolume() { return roomVolume; }
    
    /** Gets the room area in square meters. 
     * 
     * @return room area in m² 
     */
    public double getRoomArea() { return roomArea; }
    
    /** Gets the atmospheric pressure. 
     * 
     * @return pressure in atm 
     */
    public double getPressure() { return pressure; }
    
    /** Gets the room temperature. 
     * 
     * @return temperature in °C 
     */
    public double getTemperature() { return temperature; }
    
    /** Gets the relative humidity. 
     * 
     * @return relative humidity in % 
     */
    public double getRelativeHumidity() { return relativeHumidity; }
    
    /** Gets the background CO2 concentration. 
     * 
     * @return CO2 concentration in ppm 
     */
    public double getBackgroundCO2() { return backgroundCO2; }
    
    /** Gets the ventilation rate per hour. 
     * 
     * @return ventilation rate in h⁻¹ 
     */
    public double getVentilationRate() { return ventilationRate; }
    
    /** Gets the virus decay rate per hour. 
     * 
     * @return virus decay rate in h⁻¹ 
     */
    public double getVirusDecayRate() { return virusDecayRate; }
    
    /** Gets the deposition rate per hour. 
     * 
     * @return deposition rate in h⁻¹ 
     */
    public double getDepositionRate() { return depositionRate; }
    
    /** Gets the total first-order loss rate. 
     * 
     * @return total loss rate in h⁻¹ 
     */
    public double getTotalFirstOrderLossRate() { return totalFirstOrderLossRate; }
    
    /** Gets the ventilation rate per person. 
     * 
     * @return ventilation rate in L/s/person 
     */
    public double getVentilationRatePerPerson() { return ventilationRatePerPerson; }
    
    /** Gets the total number of people. 
     * 
     * @return total number of people 
     */
    public int getTotalPeople() { return totalPeople; }
    
    /** Gets the number of infectious people. 
     * 
     * @return number of infectious people 
     */
    public int getInfectivePeople() { return infectivePeople; }
    
    /** Gets the fraction of immune people. 
     * 
     * @return fraction of immune people (0.0 to 1.0) 
     */
    public double getFractionImmune() { return fractionImmune; }
    
    /** Gets the number of susceptible people. 
     * 
     * @return number of susceptible people 
     */
    public double getSusceptiblePeople() { return susceptiblePeople; }
    
    /** Gets the density of people per square meter. 
     * 
     * @return density in persons/m² 
     */
    public double getDensityPeoplePerM2() { return densityPeoplePerM2; }
    
    /** Gets the volume per person. 
     * 
     * @return volume in m³/person 
     */
    public double getDensityVolumePerPerson() { return densityVolumePerPerson; }

    /** Gets the breathing rate for susceptible people. 
     * 
     * @return breathing rate in m³/h 
    */
    public double getBreathingRateSusceptibles() { return breathingRateSusceptibles; }
    
    /** Gets the relative breathing rate factor. 
     * 
     * @return relative breathing rate factor 
     */
    public double getRelativeBreathingRateFactor() { return relativeBreathingRateFactor; }
    
    /** Gets the CO2 emission rate per person. 
     * 
     * @return CO2 emission rate in L/s 
     */
    public double getCO2EmissionRatePerPerson() { return co2EmissionRatePerPerson; }

    /** Gets the basic quanta exhalation rate. 
     * 
     * @return basic quanta exhalation rate in quanta/h 
     */
    public double getBasicQuantaExhalationRate() { return basicQuantaExhalationRate; }
    
    /** Gets the quanta enhancement factor for variant. 
     * 
     * @return enhancement factor 
     */
    public double getQuantaEnhancementVariant() { return quantaEnhancementVariant; }
    
    /** Gets the quanta enhancement factor for activity. 
     * 
     * @return enhancement factor 
     */
    public double getQuantaEnhancementActivity() { return quantaEnhancementActivity; }
    
    /** Gets the quanta exhalation rate for infected people. 
     * 
     * @return quanta exhalation rate in quanta/h 
     */
    public double getQuantaExhalationInfected() { return quantaExhalationInfected; }

    /** Gets the exhalation mask efficiency. 
     * 
     * @return mask efficiency (0.0 to 1.0) 
     */
    public double getExhalationMaskEfficiency() { return exhalationMaskEfficiency; }
    
    /** Gets the fraction of people wearing masks. 
     * 
     * @return fraction (0.0 to 1.0) 
     */
    public double getFractionPeopleWithMasks() { return fractionPeopleWithMasks; }
    
    /** Gets the inhalation mask efficiency. 
     * 
     * @return mask efficiency (0.0 to 1.0) 
     */
    public double getInhalationMaskEfficiency() { return inhalationMaskEfficiency; }

    /** Gets the probability of being infective. 
     * 
     * @return probability (0.0 to 1.0) 
     */
    public double getProbabilityBeingInfective() { return probabilityBeingInfective; }
    
    /** Gets the hospitalization rate. 
     * 
     * @return hospitalization rate (0.0 to 1.0) 
     */
    public double getHospitalizationRate() { return hospitalizationRate; }
    
    /** Gets the death rate. 
     * 
     * @return death rate (0.0 to 1.0) 
     */
    public double getDeathRate() { return deathRate; }

    /** Gets the net emission rate. 
     * 
     * @return emission rate in quanta/h 
     */
    public double getNetEmissionRate() { return netEmissionRate; }
    
    /** Gets the average quanta concentration. 
     * 
     * @return quanta concentration in quanta/m³ 
     */
    public double getAvgQuantaConcentration() { return avgQuantaConcentration; }
    
    /** Gets the quanta inhaled per person. 
     * 
     * @return quanta inhaled 
     */
    public double getQuantaInhaledPerPerson() { return quantaInhaledPerPerson; }
    
    /** Gets the infection probability. 
     * 
     * @return infection probability (0.0 to 1.0) 
     */
    public double getInfectionProbability() { return infectionProbability; }

    /** Gets the base transmission probability. 
     * 
     * @return transmission probability (0.0 to 1.0) 
     */
    public double getBaseTransmissionProbability() { return baseTransmissionProbability; }
    
    /** Gets the virus lifetime in aerosol. 
     * 
     * @return virus lifetime in hours 
     */
    public double getVirusLifetimeInAerosol() { return virusLifetimeInAerosol; }
    
    /** Gets the incubation period. 
     * 
     * @return incubation period in days 
     */
    public int getIncubationPeriod() { return incubationPeriod; }
    
    /** Gets the infectious period. 
     * 
     * @return infectious period in days 
     */
    public int getInfectiousPeriod() { return infectiousPeriod; }

    /**
     * Gets the pixels per meter conversion factor.
     * 
     * @return pixels per meter ratio
     */
    public static double getPixelsPerMeter() {
        return es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    /**
     * Converts distance from pixels to meters.
     * 
     * @param pixels distance in pixels
     * @return distance in meters
     */
    public static double pixelsToMeters(double pixels) {
        return pixels / getPixelsPerMeter();
    }

    /**
     * Converts distance from meters to pixels.
     * 
     * @param meters distance in meters
     * @return distance in pixels
     */
    public static double metersToPixels(double meters) {
        return meters * getPixelsPerMeter();
    }
  
    
    // SETTERS
    
    /**
     * Sets the room dimensions and updates derived parameters.
     * Automatically calculates room volume and area.
     * 
     * @param length room length in meters
     * @param width room width in meters
     * @param height room height in meters
     */
    public void setRoomDimensions(double length, double width, double height) {
        this.roomLength = length;
        this.roomWidth = width;
        this.roomHeight = height;
        this.roomVolume = length * width * height;
        this.roomArea = length * width;
    }

    /**
     * Sets the room dimensions from pixel measurements.
     * Converts pixel values to meters before setting dimensions.
     * 
     * @param lengthPixels room length in pixels
     * @param widthPixels room width in pixels
     * @param heightPixels room height in pixels
     */
    public void setRoomDimensionsFromPixels(double lengthPixels, double widthPixels, double heightPixels) {
        double pixelsPerMeter = getPixelsPerMeter();
        this.roomLength = lengthPixels / pixelsPerMeter;
        this.roomWidth = widthPixels / pixelsPerMeter;
        this.roomHeight = heightPixels / pixelsPerMeter;
        this.roomVolume = roomLength * roomWidth * roomHeight;
        this.roomArea = roomLength * roomWidth;
    }
    
    /**
     * Sets the ventilation rate and updates total first-order loss rate.
     * 
     * @param ventilationRate ventilation rate in h⁻¹
     */
    public void setVentilationRate(double ventilationRate) {
        this.ventilationRate = ventilationRate;
        updateTotalFirstOrderLossRate();
    }
    
    /**
     * Sets the virus decay rate and updates virus lifetime and total loss rate.
     * 
     * @param virusDecayRate virus decay rate in h⁻¹
     */
    public void setVirusDecayRate(double virusDecayRate) {
        this.virusDecayRate = virusDecayRate;
        this.virusLifetimeInAerosol = 1.0 / virusDecayRate;
        updateTotalFirstOrderLossRate();
    }
    
    /**
     * Sets the mask parameters including efficiencies and usage fraction.
     * 
     * @param exhalationEff exhalation mask efficiency (0.0 to 1.0)
     * @param inhalationEff inhalation mask efficiency (0.0 to 1.0)
     * @param fraction fraction of people wearing masks (0.0 to 1.0)
     */
    public void setMaskParameters(double exhalationEff, double inhalationEff, double fraction) {
        this.exhalationMaskEfficiency = exhalationEff;
        this.inhalationMaskEfficiency = inhalationEff;
        this.fractionPeopleWithMasks = fraction;
    }
    
    /**
     * Sets the number of people and calculates derived parameters.
     * Updates susceptible people count, density, and volume per person.
     * 
     * @param totalPeople total number of people in the room
     * @param infectivePeople number of infectious people
     */
    public void setPeopleCount(int totalPeople, int infectivePeople) {
        this.totalPeople = totalPeople;
        this.infectivePeople = infectivePeople;
        this.susceptiblePeople = totalPeople * (1.0 - fractionImmune) - infectivePeople;
        this.densityPeoplePerM2 = totalPeople / roomArea;
        this.densityVolumePerPerson = roomVolume / totalPeople;
    }

    /**
     * Sets the fraction of immune people and recalculates susceptible count.
     * 
     * @param fractionImmune fraction of immune people (0.0 to 1.0)
     */
    public void setFractionImmune(double fractionImmune) {
        this.fractionImmune = fractionImmune;
        if (this.totalPeople > 0) {
            this.susceptiblePeople = totalPeople * (1.0 - fractionImmune) - infectivePeople;
        }
    }

    /**
     * Sets the basic quanta exhalation rate.
     * 
     * @param basicQuantaExhalationRate basic quanta exhalation rate in quanta/h
     */
    public void setBasicQuantaExhalationRate(double basicQuantaExhalationRate) {
        this.basicQuantaExhalationRate = basicQuantaExhalationRate;
    }
    
    /**
     * Updates the total first-order loss rate.
     * Combines ventilation rate, virus decay rate, deposition rate, and control measures.
     */
    private void updateTotalFirstOrderLossRate() {
        this.totalFirstOrderLossRate = ventilationRate + virusDecayRate + depositionRate + additionalControlMeasures;
    }

    /**
     * Sets the breathing rate for susceptible people.
     * 
     * @param breathingRate breathing rate in m³/h
     */
    public void setBreathingRateSusceptibles(double breathingRate) {
        this.breathingRateSusceptibles = breathingRate;
    }

    /**
     * Sets the deposition rate.
     * 
     * @param depositionRate2 deposition rate in h⁻¹
     */
    public void setDepositionRate(double depositionRate2) {
        this.depositionRate = depositionRate2;
    }
}