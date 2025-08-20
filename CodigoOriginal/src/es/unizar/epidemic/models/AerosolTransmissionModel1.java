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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Modelo de transmisión por aerosoles basado en modelExample.txt (Supermkt: https://docs.google.com/spreadsheets/d/16K1OQkLD4BjgBdO8ePj6ytf-RpPMlJ6aXFg3PrIQBbQ/edit?gid=519189277#gid=519189277)
 * Implementa el modelo de quanta basado en el de Wells-Riley
 * Añadido por Nacho Palacio 2025-07-11
 */
public class AerosolTransmissionModel1 implements EpidemicModel {
    
    private ModelParameters1 parameters;
    private String modelName = "Aerosol Transmission Model (Wells-Riley)";
    private Map<Integer, Map<Integer, Double>> userRoomExposureTime;

    
    public AerosolTransmissionModel1() {
        this.parameters = new ModelParameters1();
    }
    

    /**
     * Calculates the probability of airborne transmission in a room
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, double timeInRoomHours) {
        configureModelForRoom(roomId);
        
        List<User> usersInRoom = getUsersInRoom(roomId);
        int infectiousPeopleCount = countInfectiousPeople(usersInRoom);
        
        double quantaConcentration = calculateRoomQuantaConcentration(roomId, infectiousPeopleCount);
    
        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        double maskProtectionFactor = extension.isMaskWearing() ? 
                                    (1.0 - parameters.getInhalationMaskEfficiency()) : 1.0;
       
        double breathingRate = parameters.getBreathingRateSusceptibles();
        double quantaInhaled = quantaConcentration * breathingRate * timeInRoomHours * maskProtectionFactor;
        
        double infectionProb = 1.0 - Math.exp(-quantaInhaled);


        // Statistics
        double concentration = calculateRoomQuantaConcentration(roomId, infectiousPeopleCount);
        EpidemicStatistics.getInstance().recordRoomAerosolConcentration(roomId, concentration);
        
        EpidemicStatistics stats = EpidemicStatistics.getInstance();
        stats.setModelSpecificStat("Modelo utilizado", "Aerosol Transmission Model (Wells-Riley)");
        stats.setModelSpecificStat("Tasa ventilación promedio", parameters.getVentilationRate() + " h⁻¹");
       
        return Math.min(1.0, Math.max(0.0, infectionProb));
    }

    /**
     * Calculates the probability of transmission based on contact records
     */
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        double exposureTimeHours = contact.getDuration() / 3600.0;
        int roomId = contact.getRoomId();
        
        return calculateAirborneTransmissionProbability(susceptible, roomId, exposureTimeHours);
    }
    
    
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
     * Updates the viral load based on the user's health status
     */
    private void updateViralLoadBasedOnState(UserEpidemicExtension extension, int currentHour) {     
        switch (extension.getHealthStatus()) {
            case EXPOSED:
                extension.setViralEmissionRate(0.0);
                break;
                
            case INFECTIOUS_ASYMPTOMATIC:
                double asymptomaticRate = parameters.getBasicQuantaExhalationRate() * 0.8;
                extension.setViralEmissionRate(asymptomaticRate);
                break;
                
            case INFECTIOUS_SYMPTOMATIC:
                double symptomaticRate = parameters.getBasicQuantaExhalationRate() * 1.5;
                extension.setViralEmissionRate(symptomaticRate);
                break;
                
            default:
                extension.setViralEmissionRate(0.0);
                break;
        }
    }
    
    /**
     * Calculates the risk of infection in a room based on the number of infectious users and time spent
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
     * Calculates the CO2 concentration as a risk indicator
     */
    public double calculateCO2Risk(List<User> usersInRoom) {
        double co2Concentration = parameters.calculateCO2Concentration(usersInRoom.size());
        
        // UMBRALES AJUSTADOS PARA SER MÁS SENSIBLES
        if (co2Concentration > 500) {
            return 2.0;
        } else if (co2Concentration > 460) {   // ← Captura el caso de 6 usuarios (464 ppm)
            return 1.8;
        } else if (co2Concentration > 450) {
            return 1.5;
        } else if (co2Concentration > 430) {   // ← Captura el caso de 2 usuarios (431 ppm)
            return 1.2;
        } else {
            return 1.0;
        }
    }

    /**
     * Calculates the quanta concentration in a room based on ventilation and emissions
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
     * Initialize exposure tracking for all users
     */
    public void initializeExposureTracking(List<User> users) {
        userRoomExposureTime = new HashMap<>();
        for (User user : users) {
            userRoomExposureTime.put(user.userID, new HashMap<>());
        }
    }

    /**
     * Updates the exposure time for users in a room
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

    public void configureModelForRoom(int roomId) {
        roomId += 1; 
        double roomWidth = getRoomWidth(roomId);
        double roomLength = getRoomLength(roomId);
        double roomHeight = 3.0; // SE SUPONE ESTA ALTURA
        
        double widthMeters = ModelParameters1.pixelsToMeters(roomWidth);
        double lengthMeters = ModelParameters1.pixelsToMeters(roomLength);
        
        parameters.setRoomDimensions(lengthMeters, widthMeters, roomHeight);

        // double ventilationRate = getVentilationRateForRoomType(getRoomType(roomId));
        // parameters.setVentilationRate(ventilationRate);
        parameters.setVentilationRate(3.0); // Default
        
        List<User> usersInRoom = getUsersInRoom(roomId);
        parameters.setPeopleCount(usersInRoom.size(), countInfectiousPeople(usersInRoom));
        
        double fractionWithMasks = calculateFractionWithMasks(usersInRoom);
        parameters.setMaskParameters(0.5, 0.3, fractionWithMasks);

        double areaFactor = widthMeters * lengthMeters;

    }

    /**
     * Gets the list of users currently in a specific room
     */
    protected List<User> getUsersInRoom(int roomId) {
        List<User> usersInRoom = new ArrayList<>();
        
        try {
            if (es.unizar.gui.Configuration.simulation != null) {
                List<User> allUsers = es.unizar.gui.Configuration.simulation.getAllUsers();
                
                for (User user : allUsers) {
                    if (user != null && user.room == roomId) {
                        usersInRoom.add(user);
                    }
                }
            }
        } catch (Exception e) {
            // System.out.println("❌ Error al obtener usuarios en habitación " + roomId + ": " + e.getMessage());
        }
        
        return usersInRoom;
    }

    /**
     * Counts the number of infectious people in a list of users
     */
    private int countInfectiousPeople(List<User> users) {
        int count = 0;
        for (User user : users) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && isInfectious(extension)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the list of infectious users in a specific room
     */
    private List<User> getUsersInfectiousInRoom(int roomId) {
        List<User> infectiousUsers = new ArrayList<>();
        for (User user : getUsersInRoom(roomId)) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && isInfectious(extension)) {
                infectiousUsers.add(user);
            }
        }
        return infectiousUsers;
    }

    /**
     * Gets the accumulated time of a user in a room
     */
    public double getUserRoomExposureTime(int userId, int roomId) {
        Map<Integer, Double> roomExposure = userRoomExposureTime.get(userId);
        return roomExposure != null ? roomExposure.getOrDefault(roomId, 0.0) : 0.0;
    }
    
    private boolean isInfectious(UserEpidemicExtension extension) {
        return extension.getHealthStatus().equals(HealthStatus.INFECTIOUS_ASYMPTOMATIC) ||
            extension.getHealthStatus().equals(HealthStatus.INFECTIOUS_SYMPTOMATIC);
    }
    
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public ModelParameters1 getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(ModelParameters1 parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Configura el modelo para una habitación específica
     */
    public void configureForRoom(double length, double width, double height, double ventilationRate) {
        parameters.setRoomDimensions(length, width, height);
        parameters.setVentilationRate(ventilationRate);
    }
    
    /**
     * Configura parámetros de mascarillas
     */
    public void configureMasks(double exhalationEff, double inhalationEff, double compliance) {
        parameters.setMaskParameters(exhalationEff, inhalationEff, compliance);
    }


    /**
     * Gets the width of a room in pixels
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
            return 24.4 * Configuration.getPixelsPerMeter();
        }
    }

    /**
     * Gets the length of a room in pixels
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
            return 15.3 * Configuration.getPixelsPerMeter();
        }
    }

    /**
     * Gets the type of a room based on its contents
     * NO SE USARÁ DE MOMENTO
     */
    private String getRoomType(int roomId) {
        try {
            File itemFile = new File(Literals.ITEM_FLOOR_COMBINED);
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(itemFile)) {
                props.load(fis);
            }
            
            int restaurantItems = 0;
            int officeItems = 0;
            int classroomItems = 0;
            
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("item_room_")) {
                    String value = props.getProperty(key);
                    if (value.equals(String.valueOf(roomId))) {
                        String itemId = key.substring("item_room_".length());
                        
                        String itemType = props.getProperty("vertex_label_" + itemId);
                        if (itemType != null) {
                            if (itemType.contains("Restaurante") || itemType.contains("Cafeteria"))
                                restaurantItems++;
                            else if (itemType.contains("Office") || itemType.contains("Oficina"))
                                officeItems++;
                            else if (itemType.contains("Classroom") || itemType.contains("Aula"))
                                classroomItems++;
                        }
                    }
                }
            }
            
            if (restaurantItems > officeItems && restaurantItems > classroomItems)
                return "RESTAURANT";
            else if (officeItems > restaurantItems && officeItems > classroomItems)
                return "OFFICE";
            else if (classroomItems > restaurantItems && classroomItems > officeItems)
                return "CLASSROOM";
            
            double area = getRoomWidth(roomId) * getRoomLength(roomId);
            if (area > 20000) 
                return "HALL";
            else if (area > 5000)
                return "MEDIUM_ROOM";
            else
                return "SMALL_ROOM";
                
        } catch (Exception e) {
            return "STANDARD_ROOM"; 
        }
    }

    /**
     * Gets the ventilation rate for a room type
     * NO SE USARÁ DE MOMENTO
     */
    private double getVentilationRateForRoomType(String roomType) {
        switch (roomType) {
            case "RESTAURANT":
                return 8.0;
            case "CLASSROOM":
                return 6.0;
            case "OFFICE":
                return 4.0;
            case "HALL":
                return 3.0;
            case "SMALL_ROOM":
                return 2.0;
            default:
                return 3.0;
        }
    }

    /**
     * Calculates the fraction of users wearing masks in a room
     */
    private double calculateFractionWithMasks(List<User> usersInRoom) {
        if (usersInRoom.isEmpty()) return 0.0;
        
        int usersWithMasks = 0;
        for (User user : usersInRoom) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null && extension.isMaskWearing()) {
                usersWithMasks++;
            }
        }
        
        return (double) usersWithMasks / usersInRoom.size();
    }
}