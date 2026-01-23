package es.unizar.epidemic.models;

/**
 * Parameters for LelieveldTransmissionModel based on Lelieveld et al. (2020)
 * Implemented based on modelAerosol2.txt
 * 
 * @author Nacho Palacio
 */
public class LelieveldParameters {
    // VIRUS PARAMETERS
    private double virusLifetimeHours = 1.7;           // h
    private double virusDecayRateHour = 0.59;          // h⁻¹ (1/lifetime)

    // AEROSOL GENERATION PARAMETERS
    private double concentrationBreathingCm3 = 0.1;    // particle/cm³
    private double concentrationSpeakingCm3 = 1.1;     // particle/cm³
    private double speakingBreathingRatio = 0.10;      // fraction of time speaking
    private double respiratoryRateLmin = 10.0;         // L/min
    private double respiratoryRateM3h = 0.6;           // m³/h (calculated)

    // VIRAL LOAD
    // Modified because 5e8 is the peak viral load, not the typical load
    private double viralLoadHighCm3 = 5e6;             // RNA copies/cm³ (previously 5e8)
    private double viralLoadSuperCm3 = 5e7;            // RNA copies/cm³ (previously 5e9)

    // private double virusDecayRate = 0.62;               // h⁻¹

    // DEPOSITION AND INFECTION PARAMETERS
    private double depositionProbability = 0.5;        // probability
    private double infectiveDoseD50 = 316;             // RNA copies (previously 316)

    // ENVIRONMENT PARAMETERS
    // private double roomAreaM2 = 60.0;                  // m²
    private double roomHeightM = 3.0;                  // m
    private double roomVolumeM3 = 180.0;               // m³
    private double roomLengthM = 10.0;                 // m
    private double roomWidthM = 6.0;                   // m
    private double backgroundCo2Ppm = 415;             // ppm

    // VENTILATION
    private double ventilationRatePassiveH = 0.35;     // h⁻¹
    private double ventilationRateActiveH = 2.0;       // h⁻¹
    private double hepaRateH = 9.0;                    // h⁻¹
    private double totalVentilationRateH = 2.35;       // h⁻¹ (passive + active)

    // OCCUPANT PARAMETERS
    private int subjectsInRoom = 25;                   // people
    private int infectivePeople = 1;                   // people
    private double fractionImmune = 0.0;               // fraction
    private int susceptiblePeople = 24;                // people

    // MASKS
    private double maskEfficiencyInh = 0.3;            // fraction
    private double maskEfficiencyExh = 0.4;            // fraction
    private double maskEfficiencyTotal = 0.7;          // fraction
    private double fractionPeopleWithMasks = 0.1;     // 10%


    // ADVANCED FIELDS
    private boolean hepaFilterOn = false;

    public LelieveldParameters() {
        updateDerivedParameters();
    }

    /**
     * Updates derived parameters based on the current settings.
     * Recalculates room volume, area, susceptible people count, respiratory rate,
     * total ventilation rate, and virus decay rate based on primary parameters.
     */
    private void updateDerivedParameters() {     
        this.roomVolumeM3 = roomLengthM * roomWidthM * roomHeightM;
        // this.roomAreaM2 = roomLengthM * roomWidthM;  
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
     * Calculates the viral concentration in the air (RNA copies/m³).
     * Takes into account aerosol emission rates, mask usage, respiratory rates,
     * room volume, ventilation, and virus decay.
     * 
     * @param viralLoadCm3 viral load in RNA copies per cm³
     * @param fractionWithMasks fraction of people wearing masks (0.0 to 1.0)
     * @param infectivePeople number of infectious people in the room
     * @return viral concentration in RNA copies per m³
     */
    public double calculateViralConcentration(double viralLoadCm3, double fractionWithMasks, int infectivePeople) {
        double avgEmissionConcentration = concentrationBreathingCm3 * (1 - speakingBreathingRatio) + 
                                        concentrationSpeakingCm3 * speakingBreathingRatio;
        double viralEmission = avgEmissionConcentration * viralLoadCm3;

        double maskFactor = 1.0 - (maskEfficiencyExh * fractionWithMasks);

        if (maskFactor <= 0) {
            System.out.println("    ERROR: maskFactor <= 0, returning 0");
            return 0.0;
        }
       
        double totalEmission = viralEmission * respiratoryRateM3h * infectivePeople * maskFactor;
        double lossRate = totalVentilationRateH + virusDecayRateHour;
        double denominatorFactor = roomVolumeM3 * lossRate;
        double finalConcentration = totalEmission / denominatorFactor;

        return finalConcentration;
    }

    /**
     * Calculates the probability of infection using dose-response model.
     * Uses viral concentration, exposure time, mask protection, and deposition
     * probability to determine infection risk.
     * 
     * @param timeHours exposure time in hours
     * @param viralLoadCm3 viral load in RNA copies per cm³
     * @param maskProtectionFactor mask protection factor (1.0 = no mask)
     * @param infectivePeople number of infectious people in the room
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateInfectionProbability(double timeHours, double viralLoadCm3, double maskProtectionFactor, int infectivePeople) {
        if (timeHours <= 0) {
            return 0.0;
        }
        if (viralLoadCm3 <= 0) {
            return 0.0;
        }
 
        double concentration = calculateViralConcentration(viralLoadCm3, fractionPeopleWithMasks, infectivePeople);
        
        double inhalaedDose = concentration * respiratoryRateM3h * timeHours * maskProtectionFactor * depositionProbability;

        double PRNA = calculateSingleVirusProbability();
        
        double infectionProb = 1.0 - Math.pow(1.0 - PRNA, inhalaedDose);

        double immunityReduction = 1.0 - (fractionImmune * 0.7); // revisar
        double finalInfectionProb = infectionProb * immunityReduction;

        return Math.min(1.0, Math.max(0.0, finalInfectionProb));
    }

    /**
     * Calculates the probability of infection for a group of susceptible people.
     * Considers the cumulative exposure dose for all susceptible individuals.
     * 
     * @param timeHours exposure time in hours
     * @param viralLoadCm3 viral load in RNA copies per cm³
     * @param maskProtectionFactor mask protection factor (1.0 = no mask)
     * @param susceptibleCount number of susceptible people
     * @param infectivePeople number of infectious people in the room
     * @return group infection probability (0.0 to 1.0)
     */
    public double calculateGroupInfectionProbability(double timeHours, double viralLoadCm3, 
                                                double maskProtectionFactor, int susceptibleCount, int infectivePeople) {
        double fractionMasks = subjectsInRoom > 0 ? (subjectsInRoom - infectivePeople) / (double)subjectsInRoom : 0;
        double concentration = calculateViralConcentration(viralLoadCm3, fractionMasks, infectivePeople);
        
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
     * Calculates the concentration of CO2 in the room in ppm.
     * Based on background CO2, emission rates from occupants, ventilation,
     * and room volume.
     * 
     * @return CO2 concentration in parts per million (ppm)
     */
    public double calculateCO2Concentration() {
        double co2EmissionRateM3h = 0.014 * subjectsInRoom;
        return backgroundCo2Ppm + (co2EmissionRateM3h * 1000000) / (totalVentilationRateH * roomVolumeM3);
    }

    /**
     * Calculates the probability of infection from inhaling a single virus particle.
     * Based on the infectious dose D50 parameter.
     * 
     * @return probability of infection from a single virus particle
     */
    public double calculateSingleVirusProbability() {
        return 1.0 - Math.pow(10, Math.log10(0.5) / infectiveDoseD50);
    }

    // GETTERS
    
    /** Gets the high viral load in RNA copies per cm³.
     * 
     *  @return viral load for regular infectious individuals 
     */
    public double getViralLoadHighCm3() { return viralLoadHighCm3; }
    
    /** Gets the super spreader viral load in RNA copies per cm³.
     * 
     *  @return viral load for super spreaders 
     */
    public double getViralLoadSuperCm3() { return viralLoadSuperCm3; }
    
    /** Gets the infectious dose D50 parameter.
     * 
     *  @return D50 in RNA copies 
     */
    public double getInfectiveDoseD50() { return infectiveDoseD50; }
    
    /** Gets the room volume in cubic meters. 
     * 
     * @return room volume in m³ 
     */
    public double getRoomVolumeM3() { return roomVolumeM3; }
    
    /** Gets the deposition probability of aerosol particles. 
     * 
     * @return deposition probability (0.0 to 1.0) 
     */
    public double getDepositionProbability() { return depositionProbability; }
    
    /** Gets the total ventilation rate per hour. 
     * 
     * @return ventilation rate in h⁻¹ 
     */
    public double getTotalVentilationRateH() { return totalVentilationRateH; }
    
    /** Gets the respiratory rate in cubic meters per hour. 
     * 
     * @return respiratory rate in m³/h 
     */
    public double getRespiratoryRateM3h() { return respiratoryRateM3h; }
    
    /** Gets the total mask efficiency (inhalation + exhalation). 
     * 
     * @return mask efficiency (0.0 to 1.0) 
     */
    public double getMaskEfficiencyTotal() { return maskEfficiencyTotal; }
    
    /** Gets the mask inhalation efficiency. 
     * 
     * @return inhalation efficiency (0.0 to 1.0) 
     */
    public double getMaskEfficiencyInh() { return maskEfficiencyInh; }
    
    /** Gets the mask exhalation efficiency. 
     * 
     * @return exhalation efficiency (0.0 to 1.0) 
     */
    public double getMaskEfficiencyExh() { return maskEfficiencyExh; }
    
    /** Gets the number of infectious people. 
     * 
     * @return count of infectious people 
     */
    public int getInfectivePeople() { return infectivePeople; }
    
    /** Gets the fraction of people wearing masks. 
     * 
     * @return fraction (0.0 to 1.0) 
     */
    public double getFractionPeopleWithMasks() { return fractionPeopleWithMasks; }
    
    /** Gets the fraction of immune people. 
     * 
     * @return fraction (0.0 to 1.0) 
     */
    public double getFractionImmune() { return fractionImmune; }
    
    /** Gets the number of immune people. 
     * 
     * @return count of immune people 
     */
    public int getImmunePeople() { return (int) Math.round(subjectsInRoom * fractionImmune); }
    
    /** Gets the virus decay rate per hour. 
     * 
     * @return decay rate in h⁻¹ 
     */
    public double getVirusDecayRateHour() { return virusDecayRateHour; }


    // SETTERS
    
    /**
     * Sets the room dimensions and updates derived parameters.
     * 
     * @param length room length in meters
     * @param width room width in meters
     * @param height room height in meters
     */
    public void setRoomDimensions(double length, double width, double height) {
        if (length <= 0 || width <= 0 || height <= 0) {
            System.out.println(" ERROR: Dimensions <= 0!");
            return;
        }
        
        this.roomLengthM = length;
        this.roomWidthM = width;
        this.roomHeightM = height;

        updateDerivedParameters();
    }

    /**
     * Sets the ventilation rates and HEPA filter status.
     * 
     * @param passive passive ventilation rate in h⁻¹
     * @param active active ventilation rate in h⁻¹
     * @param useHepa whether HEPA filtration is enabled
     */
    public void setVentilationRates(double passive, double active, boolean useHepa) {
        if (passive < 0 || active < 0) {
            System.out.println(" ERROR: Negative ventilation rates!");
            return;
        }

        this.ventilationRatePassiveH = passive;
        this.ventilationRateActiveH = active;
        this.hepaFilterOn = useHepa;
        
        updateDerivedParameters();
    }

    /**
     * Sets the mask parameters including efficiencies and usage fraction.
     * 
     * @param inhEff inhalation efficiency (0.0 to 1.0)
     * @param exhEff exhalation efficiency (0.0 to 1.0)
     * @param fraction fraction of people wearing masks (0.0 to 1.0)
     */
    public void setMaskParameters(double inhEff, double exhEff, double fraction) {
        if (inhEff < 0 || inhEff > 1 || exhEff < 0 || exhEff > 1) {
            System.out.println(" ERROR: Mask efficiencies out of range [0,1]!");
            return;
        }
        
        this.maskEfficiencyInh = inhEff;
        this.maskEfficiencyExh = exhEff;
        this.maskEfficiencyTotal = inhEff + exhEff;
        this.fractionPeopleWithMasks = fraction;
        
        updateDerivedParameters();
    }

    /**
     * Sets the total number of people and infectious people in the room.
     * Automatically calculates susceptible people based on immune fraction.
     * 
     * @param total total number of people in the room
     * @param infective number of infectious people
     */
    public void setPeopleCount(int total, int infective) {   
        if (total < 0 || infective < 0) {
            System.out.println(" ERROR: Negative number of people!");
            return;
        }
        if (infective > total) {
            System.out.println(" ERROR: More infectious than total!");
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

    /**
     * Sets the fraction of immune people in the room.
     * Recalculates susceptible people count accordingly.
     * 
     * @param fractionImmune fraction of immune people (0.0 to 1.0)
     */
    public void setFractionImmune(double fractionImmune) {
        if (fractionImmune < 0 || fractionImmune > 1) {
            System.out.println(" ERROR: Immune fraction out of range [0,1]!");
            return;
        }
        
        this.fractionImmune = fractionImmune;
        
        if (this.subjectsInRoom > 0) {
            int immunePeople = (int) Math.round(subjectsInRoom * fractionImmune);
            this.susceptiblePeople = subjectsInRoom - infectivePeople - immunePeople;
        }
        
        updateDerivedParameters();
    }
    
    /**
     * Converts pixels to meters based on the current configuration.
     * 
     * @param pixels distance in pixels
     * @return distance in meters
     */
    public static double pixelsToMeters(double pixels) {
        return pixels / es.unizar.gui.Configuration.getPixelsPerMeter();
    }
    
    /**
     * Converts meters to pixels based on the current configuration.
     * 
     * @param meters distance in meters
     * @return distance in pixels
     */
    public static double metersToPixels(double meters) {
        return meters * es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    /**
     * Sets the deposition probability of aerosol particles in the respiratory tract.
     * 
     * @param depositionProbability2 deposition probability (0.0 to 1.0)
     */
    public void setDepositionProbability(double depositionProbability2) {
        this.depositionProbability = depositionProbability2;
    }

    /**
     * Sets the infectious dose D50 parameter (dose at which 50% become infected).
     * 
     * @param infectiousDose D50 value in RNA copies
     */
    public void setInfectiveDoseD50(double infectiousDose) {
        this.infectiveDoseD50 = infectiousDose;
    }

    /**
     * Sets the virus decay rate per hour and updates derived parameters.
     * 
     * @param virusDecayRateHour decay rate in h⁻¹
     */
    public void setVirusDecayRateHour(double virusDecayRateHour) {
        this.virusDecayRateHour = virusDecayRateHour;
        updateDerivedParameters();
    }
}