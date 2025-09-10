package es.unizar.epidemic.models;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.epidemic.ContactRecord;
import es.unizar.epidemic.HealthStatus;
import es.unizar.epidemic.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.User;
import es.unizar.util.Literals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modelo de transmisión por aerosoles basado en Lelieveld et al. (2020)
 * Añadido por Nacho Palacio 2025-07-27
 */
public class LelieveldTransmissionModel implements EpidemicModel {
    
    private LelieveldParameters parameters;
    private String modelName = "Lelieveld Aerosol Transmission Model";
    private Map<Integer, Map<Integer, Double>> userRoomExposureTime;

    private boolean autoConfigureRooms = true;
    
    public LelieveldTransmissionModel() {
        this.parameters = new LelieveldParameters();
        this.userRoomExposureTime = new HashMap<>();
    }

    public void setAutoConfigureRooms(boolean enable) {
        this.autoConfigureRooms = enable;
    }
    
    /**
     * Calculates the airborne transmission probability for a susceptible user
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, double timeInRoomHours) {
        List<User> usersInRoom = getUsersInRoom(roomId);
        int infectiousPeopleCount = countInfectiousPeople(usersInRoom);
        
        if (infectiousPeopleCount == 0) {
            return 0.0;
        }

        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension == null) {
            return 0.0;
        }

        double maskProtectionFactor = extension.isMaskWearing() ? 
                                    (1.0 - parameters.getMaskEfficiencyInh()) : 1.0;
        
        boolean hasSuperSpreaders = false;
        for (User user : usersInRoom) {
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext != null && ext.getHealthStatus() == HealthStatus.SUPER_SPREADER) {
                hasSuperSpreaders = true;
            }
        }
        
        double viralLoad = hasSuperSpreaders ? 
                        parameters.getViralLoadSuperCm3() : 
                        parameters.getViralLoadHighCm3();
        
        double infectionProb = parameters.calculateInfectionProbability(
                            timeInRoomHours, viralLoad, maskProtectionFactor);
 

        // Statistics
        EpidemicStatistics stats = EpidemicStatistics.getInstance();
        double realConcentration = parameters.calculateViralConcentration(viralLoad, calculateFractionWithMasks(usersInRoom));
        stats.recordRoomAerosolConcentration(roomId, realConcentration);

        stats.setModelSpecificStat("Modelo utilizado", "Aerosol Transmission Model (Lelieveld et al., 2020)");
        stats.setModelSpecificStat("Carga viral alta", parameters.getViralLoadHighCm3() + " copias/cm³");
        stats.setModelSpecificStat("Ventilación total", parameters.getTotalVentilationRateH() + " h⁻¹");
                        
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }
    
    /**
     * Calculates the airborne transmission probability for a susceptible user
     */
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        double exposureTimeHours = contact.getDuration() / 3600.0;
        int roomId = contact.getRoomId();

        if (roomId < 0) {
            return 0.0;
        }

        if (autoConfigureRooms) {
            configureModelForRoom(roomId);
        }
        
        return calculateAirborneTransmissionProbability(susceptible, roomId, exposureTimeHours);
    }
    
    /**
     * Updates health states of users based on their current health status
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
     * Updates viral load based on health status
     */
    private void updateViralLoadBasedOnState(UserEpidemicExtension extension, int currentHour) {     
        switch (extension.getHealthStatus()) {
            case EXPOSED:
                extension.setViralEmissionRate(0.0);
                break;
                
            case INFECTIOUS_ASYMPTOMATIC:
                extension.setViralEmissionRate(parameters.getViralLoadHighCm3() * 0.7);
                break;
                
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
     * Configures the model parameters for a specific room
     */
    public void configureModelForRoom(int roomId) {
        List<User> usersInRoom = getUsersInRoom(roomId);
        if (usersInRoom.isEmpty()) {
            return;
        }
        
        roomId += 1; // Correction
        
        double roomWidthPixels = getRoomWidth(roomId);
        double roomLengthPixels = getRoomLength(roomId);
        double roomHeight = 3.0;
        
        double widthMeters = LelieveldParameters.pixelsToMeters(roomWidthPixels);
        double lengthMeters = LelieveldParameters.pixelsToMeters(roomLengthPixels);     
             
        int infectiousCount = countInfectiousPeople(usersInRoom);

        double currentImmunity = parameters.getFractionImmune();
        
        parameters.setPeopleCount(usersInRoom.size(), infectiousCount);

        if (currentImmunity > 0) {
            parameters.setFractionImmune(currentImmunity);
        }

        parameters.setRoomDimensions(lengthMeters, widthMeters, roomHeight);
        
        double currentVentilation = parameters.getTotalVentilationRateH();
        
        if (currentVentilation <= 2.5) {
            parameters.setVentilationRates(0.35, 2.0, false);
        }
        
        double fractionWithMasks = calculateFractionWithMasks(usersInRoom);
        
        parameters.setMaskParameters(0.3, 0.4, fractionWithMasks);
    }
    
    /**
     * Initializes exposure tracking for users in rooms
     */
    public void initializeExposureTracking(List<User> users) {
        userRoomExposureTime = new HashMap<>();
        for (User user : users) {
            userRoomExposureTime.put(user.userID, new HashMap<>());
        }
    }
    
    /**
     * Updates exposure time for users in a room
     */
    public void updateRoomExposure(List<User> users, double deltaTimeHours) { 
        for (User user : users) {
            int roomId = user.room;
            Map<Integer, Double> roomExposure = userRoomExposureTime.get(user.userID);
            
            if (roomExposure != null) {
                double oldExposure = roomExposure.getOrDefault(roomId, 0.0);
                double newExposure = oldExposure + deltaTimeHours;
                roomExposure.put(roomId, newExposure);
            }
        }
    }
    
    /**
     * Gets the exposure time for a user in a specific room
     */
    public double getUserRoomExposureTime(int userId, int roomId) {
        Map<Integer, Double> roomExposure = userRoomExposureTime.get(userId);
        return roomExposure != null ? roomExposure.getOrDefault(roomId, 0.0) : 0.0;
    }
    
    /**
     * Gets the exposure time for a user in a specific room
     */
    protected List<User> getUsersInRoom(int roomId) {
        List<User> usersInRoom = new ArrayList<>();
        
        try {
            if (Configuration.simulation != null) {
                List<User> allUsers = Configuration.simulation.getAllUsers();
                
                for (User user : allUsers) {
                    
                    if (user != null && user.room == roomId) {
                        usersInRoom.add(user);
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return usersInRoom;
    }
    
    /**
     * Counts the number of infectious users in a list
     */
    private int countInfectiousPeople(List<User> users) {  
        int count = 0;
        
        Set<Integer> userIds = new HashSet<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            
            if (userIds.contains(user.userID)) {
            } else {
                userIds.add(user.userID);
            }
            
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            
            if (extension == null) {
                continue;
            }
            
            HealthStatus status = extension.getHealthStatus();

            switch (status) {
                case SUSCEPTIBLE:
                    break;
                case EXPOSED:
                    break;
                case INFECTIOUS_ASYMPTOMATIC:
                    count++;
                    break;
                case INFECTIOUS_SYMPTOMATIC:
                    count++;
                    break;
                case SUPER_SPREADER:
                    count++;
                    break;
                default:
                    break;
            }
        }
 
        return count;
    }
    
    /**
     * Calculates the fraction of users wearing masks
     */
    private double calculateFractionWithMasks(List<User> usersInRoom) {
        if (usersInRoom.isEmpty()) {
            System.out.println("   ⚠️ Lista vacía - retornando 0.0");
            return 0.0;
        }
        
        int usersWithMasks = 0;
        
        for (int i = 0; i < usersInRoom.size(); i++) {
            User user = usersInRoom.get(i);
            UserEpidemicExtension extension = user.getEpidemicExtension();
            
            if (extension == null) {
                continue;
            }
            
            boolean wearsMask = extension.isMaskWearing();
            if (wearsMask) {
                usersWithMasks++;
            } 
        }
        
        double fraction = (double) usersWithMasks / usersInRoom.size();
        
        return fraction;
    }
    
    /**
     * Gets the width of a room in meters
     */
    private double getRoomWidth(int roomId) {
        try {
            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;

            DataAccessRoomFile roomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
            
            int cornerCount = Integer.parseInt(roomFile.getPropertyValue(Literals.NUMBER_CORNER + roomId));
            
            for (int i = 1; i <= cornerCount; i++) {
                String cornerData = roomFile.getPropertyValue(Literals.CORNER + i + "_" + roomId);

                if (cornerData != null) {
                    double x = Double.parseDouble(cornerData.split(",")[0].trim());
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
            
            return maxX - minX;
        } catch (Exception e) {
            return 10.0 * Configuration.getPixelsPerMeter();
        }
    }
    
    /**
     * Gets the length of a room in meters
     */
    private double getRoomLength(int roomId) {
        try {
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            
            DataAccessRoomFile roomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
            
            int cornerCount = Integer.parseInt(roomFile.getPropertyValue(Literals.NUMBER_CORNER + roomId));

            for (int i = 1; i <= cornerCount; i++) {
                String cornerData = roomFile.getPropertyValue(Literals.CORNER + i + "_" + roomId);

                if (cornerData != null) {
                    double y = Double.parseDouble(cornerData.split(",")[1].trim());
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
            
            return maxY - minY;
        } catch (Exception e) {
            return 6.0 * Configuration.getPixelsPerMeter();
        }
    }
    
    
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
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
    
    @Override
    public void setParameters(PengParameters parameters) {}

    /**
     * Gets the parameters specific to the Lelieveld model
     */
    public LelieveldParameters getLelieveldParameters() {
        return parameters;
    }
    
    /**
     * Sets the parameters specific to the Lelieveld model
     */
    public void setLelieveldParameters(LelieveldParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Configures the immune fraction for the model
     */
    public void configureImmunity(double fractionImmune) {
        parameters.setFractionImmune(fractionImmune);
    }
}