package es.unizar.epidemic.models;

import es.unizar.epidemic.contact.ContactRecord;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.gui.simulation.User;

import java.util.List;
import java.util.Map;

/**
 * Transmission model based on Lelieveld et al. (2020)
 * Adapted to store exposure iterations on disk for memory efficiency
 * 
 * @author Nacho Palacio
 */
public class LelieveldTransmissionModel extends AbstractEpidemicModel {
    
    private LelieveldParameters parameters;
    private String modelName = "Lelieveld Aerosol Transmission Model";

    private boolean autoConfigureRooms = true;
    
    public LelieveldTransmissionModel() {
        this.parameters = new LelieveldParameters();
    }

    /**
     * Sets whether the model should automatically configure room parameters.
     * When enabled, room dimensions and parameters are set based on room data.
     * 
     * @param enable true to enable automatic room configuration, false otherwise
     */
    public void setAutoConfigureRooms(boolean enable) {
        this.autoConfigureRooms = enable;
    }
    
    /**
     * Calculates the airborne transmission probability for a susceptible user.
     * Takes into account viral load from infectious people, room configuration,
     * mask usage, and exposure time.
     * 
     * @param susceptible the susceptible user to calculate risk for
     * @param roomId the ID of the room where exposure occurs
     * @param timeInRoomHours exposure time in hours
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, double timeInRoomHours) {
        if (roomId < 0 || timeInRoomHours <= 0) {
            return 0.0;
        }
        configureModelForRoom(roomId);    
        List<User> usersInRoom = getUsersInRoom(roomId);
        int infectiousPeopleCount = countInfectiousPeople(usersInRoom);

        double totalViralLoad = 0.0;
        for (User user : usersInRoom) {
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext != null && isInfectious(ext)) {
                if (ext.isSuperSpreader()) {
                    totalViralLoad += parameters.getViralLoadSuperCm3();
                }
                else {
                    totalViralLoad += parameters.getViralLoadHighCm3();
                }
            }
        }

        if (infectiousPeopleCount == 0) {
            return 0.0;
        }

        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension == null) {
            return 0.0;
        }

        // Added by Nacho Palacio 2025-09-21
        extension = susceptible.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return 0.0;
        }

        double maskProtectionFactor = extension.isMaskWearing() ? 
                                    (1.0 - parameters.getMaskEfficiencyInh()) : 1.0;

        double infectionProb = parameters.calculateInfectionProbability(
                            timeInRoomHours, totalViralLoad, maskProtectionFactor, infectiousPeopleCount);
 
        // Without disk storage (original in-memory version)
        // Statistics
        // EpidemicStatistics stats = EpidemicStatistics.getInstance();
        // double realConcentration = parameters.calculateViralConcentration(totalViralLoad, calculateFractionWithMasks(usersInRoom));

        // stats.recordRoomAerosolConcentration(roomId, realConcentration);

        // stats.setModelSpecificStat("Modelo utilizado", "Aerosol Transmission Model (Lelieveld et al., 2020)");
        // stats.setModelSpecificStat("Carga viral alta", parameters.getViralLoadHighCm3() + " copias/cm³");
        // stats.setModelSpecificStat("Ventilación total", parameters.getTotalVentilationRateH() + " h⁻¹");
              
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }

    /**
     * Calculates the airborne transmission probability using historical data.
     * This version uses pre-calculated infectious people count and viral load
     * instead of querying current room state.
     * 
     * @param susceptible the susceptible user to calculate risk for
     * @param roomId the ID of the room where exposure occurs
     * @param timeInRoomHours exposure time in hours
     * @param infectiousPeopleCount historical average count of infectious people
     * @param totalViralLoad total viral load in the room
     * @return infection probability (0.0 to 1.0)
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, 
                                                           double timeInRoomHours, 
                                                           int infectiousPeopleCount,
                                                           double totalViralLoad) {
        if (roomId < 0 || timeInRoomHours <= 0) {
            return 0.0;
        }
        
        // Configurar parámetros de la habitación
        configureModelForRoom(roomId);
        
        // Verificar inmunidad
        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return 0.0;
        }
        
        // Si no hay infectados históricos, no hay riesgo
        if (infectiousPeopleCount <= 0) {
            return 0.0;
        }
        
        // Factor de protección por mascarilla del susceptible
        double maskProtectionFactor = (extension != null && extension.isMaskWearing()) ? 
                                    (1.0 - parameters.getMaskEfficiencyInh()) : 1.0;
        
        // Calcular probabilidad usando parámetros históricos
        double infectionProb = parameters.calculateInfectionProbability(
                            timeInRoomHours, totalViralLoad, maskProtectionFactor, infectiousPeopleCount);
        
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }



    // Without disk storage (original in-memory version)
    // public double calculateCombinedInfectionRiskForUser(User user) {
    //     Map<Integer, Double> exposureByRoom = userRoomExposureTime.get(user.userID);
    //     if (exposureByRoom == null || exposureByRoom.isEmpty()) {
    //         return 0.0;
    //     }
        
    //     // Obtener iteraciones de exposición por habitación
    //     // Map<Integer, List<Integer>> exposureIterationsByRoom = userRoomExposureIterations.get(user.userID);
    //     Map<Integer, List<Integer>> exposureIterationsByRoom = null; // SOLO DEBUG: PENDIENTE
        
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
            
    //         List<Integer> roomIterations = exposureIterationsByRoom.get(roomId);
            
    //         int avgInfectious = getAverageInfectiousCount(roomId, roomIterations);

    //         System.out.println("Usuario " + user.userID + 
    //                            " - Habitación " + roomId + 
    //                            " - Iteraciones exposición: " + 
    //                            (roomIterations != null ? roomIterations.size() : 0));
    //         System.out.println("   -> Promedio histórico de infectados en habitación " + 
    //                            roomId + ": " + avgInfectious);
            
    //         // Usar carga viral alta como referencia
    //         double avgViralLoad = avgInfectious * parameters.getViralLoadHighCm3();
            
    //         double pRoom = calculateAirborneTransmissionProbability(
    //             user, roomId, exposureTime, avgInfectious, avgViralLoad);

    //         System.out.println("   -> Probabilidad infección en habitación " + 
    //                            roomId + ": " + pRoom);
            
    //         pNoInfect *= (1.0 - pRoom);
    //     }
        
    //     return 1.0 - pNoInfect;
    // }

    public double calculateCombinedInfectionRiskForUser(User user) {
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
            
            int avgInfectious = getAverageInfectiousCount(roomId, roomIterations);

            double avgViralLoad = avgInfectious * parameters.getViralLoadHighCm3();
            
            double pRoom = calculateAirborneTransmissionProbability(
                user, roomId, exposureTime, avgInfectious, avgViralLoad);

            pNoInfect *= (1.0 - pRoom);
        }

        return 1.0 - pNoInfect;
    }

    /**
     * Fallback method to calculate combined infection risk without historical data.
     * Uses current room state instead of historical averages.
     * 
     * @param user the user to calculate risk for
     * @return combined infection probability (0.0 to 1.0)
     */
    // private double calculateCombinedInfectionRiskForUserFallback(User user) {
    //     Map<Integer, Double> exposureByRoom = userRoomExposureTime.get(user.userID);
    //     if (exposureByRoom == null || exposureByRoom.isEmpty()) return 0.0;

    //     double pNoInfect = 1.0;
    //     for (Map.Entry<Integer, Double> entry : exposureByRoom.entrySet()) {
    //         int roomId = entry.getKey();
    //         double exposureTime = entry.getValue();
    //         if (exposureTime <= 0) continue;

    //         double pRoom = calculateAirborneTransmissionProbability(user, roomId, exposureTime);
    //         pNoInfect *= (1.0 - pRoom);
    //     }
    //     return 1.0 - pNoInfect;
    // }
    
    /**
     * Calculates the airborne transmission probability for a susceptible user.
     * Overrides the base method to use Lelieveld aerosol transmission model.
     * 
     * @param infectious the infectious user (not used in aerosol model)
     * @param susceptible the susceptible user at risk
     * @param contact the contact record (not used in aerosol model)
     * @return transmission probability (0.0 to 1.0)
     */
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        // Modified by Nacho Palacio 2025-09-23
        int roomId = susceptible.room;
        double exposureTimeHours = getUserRoomExposureTime(susceptible.userID, roomId);

        if (roomId < 0 || exposureTimeHours <= 0) {
            return 0.0;
        }

        if (autoConfigureRooms) {
            configureModelForRoom(roomId);
        }

        return calculateAirborneTransmissionProbability(susceptible, roomId, exposureTimeHours);
    }
    
    /**
     * Updates health states of users based on their current health status.
     * Updates viral emission rates according to infection state.
     * 
     * @param users list of users to update
     * @param currentDay current simulation day (used for viral load calculations)
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
     * Updates viral load based on health status.
     * Sets emission rate according to whether user is symptomatic, super spreader, or other.
     * 
     * @param extension the user's epidemic extension to update
     * @param currentHour current simulation hour
     */
    private void updateViralLoadBasedOnState(UserEpidemicExtension extension, int currentHour) {     
        switch (extension.getHealthStatus()) {
            case INFECTIOUS_SYMPTOMATIC:
                extension.setViralEmissionRate(parameters.getViralLoadHighCm3());
                break;
                
            case SUPER_SPREADER:
                extension.setViralEmissionRate(parameters.getViralLoadSuperCm3());
                break;
                
            default:
                extension.setViralEmissionRate(0.0);
                break;
        }
    }
    
    /**
     * Configures the model parameters for a specific room.
     * Sets room dimensions, ventilation rates, immunity fraction, and mask parameters
     * based on room data and configuration settings.
     * 
     * @param roomId the ID of the room to configure parameters for
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
            parameters.setVentilationRates(config.getDefaultVentilationRate(), 0.0, false);
            parameters.setFractionImmune(config.getImmunePopulationFraction());
            parameters.setDepositionProbability(config.getDepositionProbability());
            parameters.setInfectiveDoseD50(config.getInfectiousDose());
            parameters.setVirusDecayRateHour(config.getVirusDecayRate());
        }

        List<User> usersInRoom = getUsersInRoom(roomId);

        parameters.setPeopleCount(usersInRoom.size(), countInfectiousPeople(usersInRoom));

        double fractionWithMasks = calculateFractionWithMasks(usersInRoom);
        double exhalationEff = config != null ? config.getMaskExhalationEfficiency() : 0.5;
        double inhalationEff = config != null ? config.getMaskInhalationEfficiency() : 0.3;
        parameters.setMaskParameters(inhalationEff, exhalationEff, fractionWithMasks);
    }
    
    /**
     * Gets the name of this transmission model.
     * 
     * @return the model name
     */
    @Override
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Gets parameters in Peng-compatible format.
     * Converts Lelieveld parameters to PengParameters for compatibility.
     * 
     * @return compatible PengParameters object
     */
    @Override
    public PengParameters getParameters() {
        PengParameters compatParams = new PengParameters();
        
        compatParams.setRoomDimensions(parameters.getRoomVolumeM3(), 
                                      parameters.getRoomVolumeM3(), 
                                      3.0);
        compatParams.setVentilationRate(parameters.getTotalVentilationRateH());

        compatParams.setFractionImmune(parameters.getFractionImmune());
        
        return compatParams;
    }
    
    /**
     * Sets parameters from Peng format.
     * Not implemented as this model uses LelieveldParameters.
     * 
     * @param parameters Peng parameters (ignored)
     */
    @Override
    public void setParameters(PengParameters parameters) {}

    /**
     * Gets the parameters specific to the Lelieveld model.
     * 
     * @return the LelieveldParameters object
     */
    public LelieveldParameters getLelieveldParameters() {
        return parameters;
    }
    
    /**
     * Sets the parameters specific to the Lelieveld model.
     * 
     * @param parameters the LelieveldParameters object to set
     */
    public void setLelieveldParameters(LelieveldParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Configures the immune fraction for the model.
     * 
     * @param fractionImmune the fraction of immune people (0.0 to 1.0)
     */
    public void configureImmunity(double fractionImmune) {
        parameters.setFractionImmune(fractionImmune);
    }
}