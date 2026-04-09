package es.unizar.epidemic.models;

import es.unizar.epidemic.contact.ContactRecord;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.gui.simulation.User;

import java.util.List;
import java.util.Map;

/**
 * Aerosol transsmission model based on (Supermkt: https://docs.google.com/spreadsheets/d/16K1OQkLD4BjgBdO8ePj6ytf-RpPMlJ6aXFg3PrIQBbQ/edit?gid=519189277#gid=519189277)
 * Implements the quanta model based on Wells-Riley
 * Adapted to store exposure iterations on disk for memory efficiency
 * 
 * @author Nacho Palacio
 */
public class PengTransmissionModel extends AbstractEpidemicModel {
    
    private PengParameters parameters;
    private String modelName = "Peng Aerosol Transmission Model";
    
    public PengTransmissionModel() {
        this.parameters = new PengParameters();
    }
    

    /**
     * Calculates the probability of airborne transmission in a room.
     * Takes into account quanta concentration, breathing rate, mask usage,
     * room configuration, and exposure time to determine infection risk.
     * 
     * @param susceptible the susceptible user to calculate risk for
     * @param roomId the ID of the room where exposure occurs
     * @param timeInRoomHours exposure time in hours
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, double timeInRoomHours) {
        // Added by Nacho Palacio 2025-10-05
        if (roomId < 0 || timeInRoomHours <= 0) {
            return 0.0;
        }
        
        configureModelForRoom(roomId);

        // Added by Nacho Palacio 2025-09-21
        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return 0.0;
        }
        
        List<User> usersInRoom = getUsersInRoom(roomId);
        int infectiousPeopleCount = countInfectiousPeople(usersInRoom);
        
        double quantaConcentration = calculateRoomQuantaConcentration(roomId, infectiousPeopleCount);
    
        extension = susceptible.getEpidemicExtension();
        double maskProtectionFactor = extension.isMaskWearing() ? 
                                    (1.0 - parameters.getInhalationMaskEfficiency()) : 1.0;

       
        double breathingRate = parameters.getBreathingRateSusceptibles();
        double quantaInhaled = quantaConcentration * breathingRate * timeInRoomHours * maskProtectionFactor;
        
        double infectionProb = 1.0 - Math.exp(-quantaInhaled);

        // Without disk storage (original in-memory version)
        // Statistics
        // double concentration = calculateRoomQuantaConcentration(roomId, infectiousPeopleCount);
        // EpidemicStatistics.getInstance().recordRoomAerosolConcentration(roomId, concentration);
        
        // EpidemicStatistics stats = EpidemicStatistics.getInstance();
        // stats.setModelSpecificStat("Modelo utilizado", "Peng Aerosol Transmission Model");
        // stats.setModelSpecificStat("Tasa ventilación promedio", parameters.getVentilationRate() + " h⁻¹");
       
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }

    /**
     * Calculates the probability of airborne transmission with a specific infectious count.
     * Uses a pre-calculated number of infectious people instead of dynamically counting
     * them in the room. Useful for historical or simulated scenarios.
     * 
     * @param susceptible the susceptible user to calculate risk for
     * @param roomId the ID of the room where exposure occurs
     * @param timeInRoomHours exposure time in hours
     * @param infectiousPeopleCount historical number of infectious people
     * @return infection probability (0.0 to 1.0)
     */
    // Modified by Nacho Palacio 2025-12-03
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, 
                                                           double timeInRoomHours, 
                                                           int infectiousPeopleCount) {
        // Basic validation
        if (roomId < 0 || timeInRoomHours <= 0) {
            return 0.0;
        }
        
        // Configure room parameters
        configureModelForRoom(roomId);
        
        // Check immunity
        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return 0.0;
        }
        
        // If no historical infected people, no risk
        if (infectiousPeopleCount <= 0) {
            return 0.0;
        }
        
        double quantaConcentration = parameters.calculateQuantaConcentration(infectiousPeopleCount);
        
        // Mask protection factor for susceptible
        double maskProtectionFactor = (extension != null && extension.isMaskWearing()) ? 
                                    (1.0 - parameters.getInhalationMaskEfficiency()) : 1.0;
        
        // Calculate inhaled quanta
        double breathingRate = parameters.getBreathingRateSusceptibles();
        double quantaInhaled = quantaConcentration * breathingRate * timeInRoomHours * maskProtectionFactor;
        
        // Infection probability (Wells-Riley model)
        double infectionProb = 1.0 - Math.exp(-quantaInhaled);
        
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }

    /**
     * Calculates the probability of transmission based on contact records.
     * Determines exposure time and delegates to airborne transmission calculation
     * using room-based exposure data.
     * 
     * @param infectious the infectious user (not directly used in aerosol model)
     * @param susceptible the susceptible user to calculate risk for
     * @param contact the contact record between users
     * @return infection probability (0.0 to 1.0)
     */
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        // Modified by Nacho Palacio 2025-09-23
        int roomId = susceptible.room;
        double exposureTimeHours = getUserRoomExposureTime(susceptible.userID, roomId);

        if (roomId < 0 || exposureTimeHours <= 0) {
            return 0.0;
        }
        
        return calculateAirborneTransmissionProbability(susceptible, roomId, exposureTimeHours);
    }
    
    /**
     * Updates health states and viral loads for all users.
     * Adjusts viral emission rates based on current health status
     * for each user in the simulation.
     * 
     * @param users list of all users in the simulation
     * @param currentDay current simulation day
     */
    @Override
    public void updateHealthStates(List<User> users, int currentDay) {        
        for (User user : users) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null) {
                updateViralLoadBasedOnState(extension, currentDay);
            }
        }
    }
    
    /**
     * Updates the viral load based on the user's health status.
     * Sets viral emission rates according to infection state,
     * with different rates for symptomatic and super-spreader cases.
     * 
     * @param extension the user's epidemic extension data
     * @param currentHour current simulation hour
     */
    private void updateViralLoadBasedOnState(UserEpidemicExtension extension, int currentHour) {     
        switch (extension.getHealthStatus()) {
            case INFECTIOUS_SYMPTOMATIC:
                double symptomaticRate = parameters.getBasicQuantaExhalationRate() * 1.0;
                extension.setViralEmissionRate(symptomaticRate);
                break;

            case SUPER_SPREADER:
                double superRate = parameters.getBasicQuantaExhalationRate() * 1.0;
                extension.setViralEmissionRate(superRate);
                break;
                
            default:
                extension.setViralEmissionRate(0.0);
                break;
        }
    }
    
    /**
     * Calculates the risk of infection in a room based on the number of infectious users and time spent.
     * Counts infectious people in the room and applies the infection probability model.
     * 
     * @param roomId the ID of the room
     * @param usersInRoom list of users currently in the room
     * @param timeInRoomHours exposure time in hours
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateRoomInfectionRisk(int roomId, List<User> usersInRoom, double timeInRoomHours) {
        int infectiousCount = 0;
        for (User user : usersInRoom) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && isInfectious(extension)) {
                infectiousCount++;
            }
        }
        
        if (infectiousCount == 0) {
            return 0.0;
        }
        
        return parameters.calculateInfectionProbability(timeInRoomHours, infectiousCount);
    }
    
    /**
     * Calculates the CO2 concentration as a risk indicator.
     * Returns a risk multiplier based on CO2 levels, with higher
     * concentrations indicating poorer ventilation.
     * 
     * @param usersInRoom list of users currently in the room
     * @return risk multiplier (1.0 to 2.0)
     */
    public double calculateCO2Risk(List<User> usersInRoom) {
        double co2Concentration = parameters.calculateCO2Concentration(usersInRoom.size());
        
        if (co2Concentration > 500) {
            return 2.0;
        } else if (co2Concentration > 460) { 
            return 1.8;
        } else if (co2Concentration > 450) {
            return 1.5;
        } else if (co2Concentration > 430) {
            return 1.2;
        } else {
            return 1.0;
        }
    }

    /**
     * Calculates the quanta concentration in a room based on ventilation and emissions.
     * Takes into account viral emission rates, mask usage, CO2 levels for effective
     * ventilation estimation, and loss rates.
     * 
     * @param roomId the ID of the room
     * @param infectiousPeopleCount number of infectious people in the room
     * @return quanta concentration in quanta/m³
     */
    private double calculateRoomQuantaConcentration(int roomId, int infectiousPeopleCount) {
        configureModelForRoom(roomId);

        double ventilationRate = parameters.getVentilationRate();
        
        List<User> usersInRoom = getUsersInRoom(roomId);

        double co2Concentration = parameters.calculateCO2Concentration(usersInRoom.size());
        
        double effectiveVentilation = ventilationRate * (parameters.getBackgroundCO2() / co2Concentration);
       
        double totalEmissionRate = 0.0;
        List<User> infectiousUsers = getUsersInfectiousInRoom(roomId);
        
        for (User user : infectiousUsers) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            
            double maskReductionFactor = extension.isMaskWearing() ? 
                                    parameters.getExhalationMaskEfficiency() : 0.0;

            double userEmission = extension.getViralEmissionRate() * (1.0 - maskReductionFactor);
                            
            totalEmissionRate += userEmission;
        }
        double totalLossRate = effectiveVentilation + parameters.getVirusDecayRate() + 
                            parameters.getDepositionRate();
       
        double concentration = totalEmissionRate / (parameters.getRoomVolume() * totalLossRate);

        return concentration;
    }

    /**
     * Configures the model parameters for a specific room.
     * Sets room dimensions, ventilation rates, immunity fractions,
     * viral parameters, and mask usage based on room data and
     * epidemic configuration.
     * 
     * @param roomId the ID of the room to configure
     */
    public void configureModelForRoom(int roomId) {
        roomId += 1; 
        double roomWidth = getRoomWidth(roomId);
        double roomLength = getRoomLength(roomId);
        double roomHeight = 3.0;

        double widthMeters = PengParameters.pixelsToMeters(roomWidth);
        double lengthMeters = PengParameters.pixelsToMeters(roomLength);

        parameters.setRoomDimensions(lengthMeters, widthMeters, roomHeight);
    
        EpidemicConfiguration config = EpidemicConfiguration.getInstance();
        if (config != null) {
            parameters.setVentilationRate(config.getDefaultVentilationRate());
            parameters.setFractionImmune(config.getImmunePopulationFraction());
            parameters.setVirusDecayRate(config.getVirusDecayRate());
            parameters.setBasicQuantaExhalationRate(config.getQuantaEmissionRate());
            parameters.setBreathingRateSusceptibles(config.getBreathingRate());
            parameters.setDepositionRate(config.getDepositionRate());
        }

        List<User> usersInRoom = getUsersInRoom(roomId);
        
        parameters.setPeopleCount(usersInRoom.size(), countInfectiousPeople(usersInRoom));
        
        double fractionWithMasks = calculateFractionWithMasks(usersInRoom);
        double exhalationEff = config != null ? config.getMaskExhalationEfficiency() : 0.5;
        double inhalationEff = config != null ? config.getMaskInhalationEfficiency() : 0.3;
        parameters.setMaskParameters(exhalationEff, inhalationEff, fractionWithMasks);
    }
    
    /**
     * Gets the name of this transmission model.
     * 
     * @return model name identifier
     */
    @Override
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Gets the parameters object for this model.
     * 
     * @return Peng model parameters
     */
    @Override
    public PengParameters getParameters() {
        return parameters;
    }
    
    /**
     * Sets the parameters object for this model.
     * 
     * @param parameters new Peng model parameters
     */
    @Override
    public void setParameters(PengParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Configures the model for a specific room with custom dimensions.
     * Sets room dimensions and ventilation rate for manual configuration.
     * 
     * @param length room length in meters
     * @param width room width in meters
     * @param height room height in meters
     * @param ventilationRate ventilation rate in h⁻¹
     */
    public void configureForRoom(double length, double width, double height, double ventilationRate) {
        parameters.setRoomDimensions(length, width, height);
        parameters.setVentilationRate(ventilationRate);
    }
    
    /**
     * Configures mask parameters for the simulation.
     * Sets exhalation and inhalation efficiencies and compliance rate.
     * 
     * @param exhalationEff exhalation mask efficiency (0.0 to 1.0)
     * @param inhalationEff inhalation mask efficiency (0.0 to 1.0)
     * @param compliance fraction of people wearing masks (0.0 to 1.0)
     */
    public void configureMasks(double exhalationEff, double inhalationEff, double compliance) {
        parameters.setMaskParameters(exhalationEff, inhalationEff, compliance);
    }

    // Without disk storage (original in-memory version)
    // Modified by Nacho Palacio 2025-12-03
    // public double calculateCombinedInfectionRiskForUser(User user) {
    //     Map<Integer, Double> exposureByRoom = userRoomExposureTime.get(user.userID);
    //     if (exposureByRoom == null || exposureByRoom.isEmpty()) {
    //         return 0.0;
    //     }
        
    //     // Obtener iteraciones de exposición por habitación
    //     Map<Integer, List<Integer>> exposureIterationsByRoom = 
    //         userRoomExposureIterations.get(user.userID);
        
    //     if (exposureIterationsByRoom == null) {
    //         // Fallback: si no hay historial, usar método antiguo
    //         System.err.println("Warning! Usuario " + user.userID + " sin historial de iteraciones, usando fallback");
    //         return calculateCombinedInfectionRiskForUserFallback(user);
    //     }
        
    //     double pNoInfect = 1.0;
        
    //     for (Map.Entry<Integer, Double> entry : exposureByRoom.entrySet()) {
    //         int roomId = entry.getKey();
    //         double exposureTime = entry.getValue();
            
    //         if (exposureTime <= 0) continue;
            
    //         // Obtener iteraciones de exposición para esta habitación
    //         List<Integer> roomIterations = exposureIterationsByRoom.get(roomId);

    //         System.out.println("Usuario " + user.userID + 
    //                            " - Habitación " + roomId + 
    //                            " - Iteraciones exposición: " + 
    //                            (roomIterations != null ? roomIterations.size() : 0));
            
    //         // Calcular promedio de infectados durante la exposición
    //         int avgInfectious = getAverageInfectiousCount(roomId, roomIterations);

    //         System.out.println("   -> Promedio histórico de infectados en habitación " + 
    //                            roomId + ": " + avgInfectious);
            
    //         // Usar método sobrecargado con conteo histórico
    //         double pRoom = calculateAirborneTransmissionProbability(
    //             user, roomId, exposureTime, avgInfectious);

    //         System.out.println("   -> Probabilidad infección en habitación " + 
    //                            roomId + ": " + pRoom);
            
    //         pNoInfect *= (1.0 - pRoom);
    //     }
        
    //     return 1.0 - pNoInfect;
    // }

    /**
     * Calculates the combined infection risk for a user across all rooms.
     * Reads exposure iterations from disk and combines infection probabilities
     * from all rooms where the user was exposed, using historical infectious counts.
     * 
     * @param user the user to calculate combined risk for
     * @return combined infection probability (0.0 to 1.0)
     */
    // Final version using disk read
    public double calculateCombinedInfectionRiskForUser(User user) {
        // System.out.println("Calculating combined risk for user " + user.userID);
        Map<Integer, Double> exposureByRoom = userRoomExposureTime.get(user.userID);
        if (exposureByRoom == null || exposureByRoom.isEmpty()) {
            return 0.0;
        }
        
        double pNoInfect = 1.0;
        
        for (Map.Entry<Integer, Double> entry : exposureByRoom.entrySet()) {
            int roomId = entry.getKey();
            double exposureTime = entry.getValue();
            
            if (exposureTime <= 0) continue;
            
            List<Integer> roomIterations = getUserRoomExposureIterationsFromDisk(user.userID, roomId);
            
            // Calculate average infected during exposure
            int avgInfectious = getAverageInfectiousCount(roomId, roomIterations);
           
            // Use overloaded method with historical count
            double pRoom = calculateAirborneTransmissionProbability(
                user, roomId, exposureTime, avgInfectious);
            
            pNoInfect *= (1.0 - pRoom);
        }

        return 1.0 - pNoInfect;
    }

    /**
     * Calculates the combined infection risk for a user using the fallback method.
     * Used when historical iteration data is not available, calculates risk
     * using current room conditions without historical infectious counts.
     * 
     * @param user the user to calculate combined risk for
     * @return combined infection probability (0.0 to 1.0)
     */
    // Added by Nacho Palacio 2025-12-03
    // private double calculateCombinedInfectionRiskForUserFallback(User user) {
    //     Map<Integer, Double> exposureByRoom = userRoomExposureTime.get(user.userID);
    //     if (exposureByRoom == null || exposureByRoom.isEmpty()) return 0.0;

    //     double pNoInfect = 1.0;
    //     for (Map.Entry<Integer, Double> entry : exposureByRoom.entrySet()) {
    //         int roomId = entry.getKey();
    //         double exposureTime = entry.getValue();
    //         if (exposureTime <= 0) continue;

    //         // Use original method (without history)
    //         double pRoom = calculateAirborneTransmissionProbability(user, roomId, exposureTime);
    //         pNoInfect *= (1.0 - pRoom);
    //     }
    //     return 1.0 - pNoInfect;
    // }
}