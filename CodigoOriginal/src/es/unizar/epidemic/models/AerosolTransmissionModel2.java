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
import java.util.List;
import java.util.Map;

/**
 * Modelo de transmisión por aerosoles basado en Lelieveld et al. (2020)
 * Implementado a partir de modelAerosol2.txt
 * Añadido por Nacho Palacio 2025-07-27
 */
public class AerosolTransmissionModel2 implements EpidemicModel {
    
    private ModelParameters2 parameters;
    private String modelName = "Aerosol Transmission Model (Lelieveld et al., 2020)";
    private Map<Integer, Map<Integer, Double>> userRoomExposureTime;

    private boolean autoConfigureRooms = true;
    
    public AerosolTransmissionModel2() {
        this.parameters = new ModelParameters2();
        this.userRoomExposureTime = new HashMap<>();
    }

    public void setAutoConfigureRooms(boolean enable) {
        this.autoConfigureRooms = enable;
    }
    
    /**
     * Calculates the airborne transmission probability for a susceptible user
     */
    public double calculateAirborneTransmissionProbability(User susceptible, int roomId, double timeInRoomHours) {
        // configureModelForRoom(roomId);
        
        List<User> usersInRoom = getUsersInRoom(roomId);
        int infectiousPeopleCount = countInfectiousPeople(usersInRoom);
        
        if (infectiousPeopleCount == 0) {
            return 0.0;
        }

        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        double maskProtectionFactor = extension.isMaskWearing() ? 
                                    (1.0 - parameters.getMaskEfficiencyInh()) : 1.0;
        
        // CORREGIR: Detectar si hay super spreaders en la habitación
        boolean hasSuperSpreaders = false;
        for (User user : usersInRoom) {
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext != null && ext.getHealthStatus() == HealthStatus.SUPER_SPREADER) {
                hasSuperSpreaders = true;
                break;
            }
        }
        
        // USAR LA CARGA VIRAL CORRECTA SEGÚN EL TIPO DE INFECTADO
        double viralLoad = hasSuperSpreaders ? 
                        parameters.getViralLoadSuperCm3() : 
                        parameters.getViralLoadHighCm3();
        
        // 🔧 DEBUG: Mostrar qué carga viral se está usando
        System.out.println(String.format("🦠 CARGA VIRAL SELECCIONADA: %.2e (%s)", 
                        viralLoad, hasSuperSpreaders ? "SUPER_SPREADER" : "INFECTADO_NORMAL"));
        
        double infectionProb = parameters.calculateInfectionProbability(
                            timeInRoomHours, viralLoad, maskProtectionFactor);
        
        int susceptibleCount = usersInRoom.size() - infectiousPeopleCount;
        if (susceptibleCount > 1) {
            double groupRisk = parameters.calculateGroupInfectionProbability(
                            timeInRoomHours, viralLoad, maskProtectionFactor, susceptibleCount);
            log("INFO", "calculateAirborneTransmissionProbability", 
                String.format("Riesgo grupal: %.5f%% para %d susceptibles", 
                            groupRisk * 100, susceptibleCount));
        }
        
        log("DEBUG", "calculateAirborneTransmissionProbability", 
            String.format("Probabilidad calculada: %.5f%% (tiempo=%.4f h, máscaras=%.2f)",
                        infectionProb * 100, timeInRoomHours, maskProtectionFactor));

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
            System.out.println("Error: Room ID cannot be negative. Contact record: " + contact);
            return 0.0;
        }

        if (autoConfigureRooms) {
            System.out.println("Se llama a configureModelForRoom con habitacion: " + roomId);
            configureModelForRoom(roomId);
        } else {
            System.out.println("🧪 MODO TEST: Saltando auto-configuración de habitación " + roomId);
        }
        
        return calculateAirborneTransmissionProbability(susceptible, roomId, exposureTimeHours);
    }
    
    /**
     * Updates health states of users based on their current health status
     */
    @Override
    public void updateHealthStates(List<User> users, int currentDay) {    
        // Añadido para debug
        int totalSuperSpreaders = 0;
        int totalInfected = 0;
        System.out.println("🔬 === ACTUALIZANDO ESTADOS DE SALUD - DÍA " + currentDay + " ===");

        for (User user : users) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null) {
                // Añadido para debug
                HealthStatus status = extension.getHealthStatus();
                if (status == HealthStatus.SUPER_SPREADER) {
                    totalSuperSpreaders++;
                }
                
                if (isInfectious(extension)) {
                    totalInfected++;
                }

                updateViralLoadBasedOnState(extension, currentDay);
            }
        }

        // Añadido para debug
        System.out.println(String.format("📊 RESUMEN ESTADOS: %d infectados totales, %d SUPER_SPREADERS", 
                      totalInfected, totalSuperSpreaders));
    
        if (totalSuperSpreaders > 0) {
            System.out.println("🚨 ¡ATENCIÓN: HAY " + totalSuperSpreaders + " SUPER_SPREADER(S) ACTIVOS! 🚨");
        } else {
            System.out.println("ℹ️  No hay SUPER_SPREADERS activos en este momento");
        }
        
        System.out.println("🔬 === FIN ACTUALIZACIÓN ESTADOS ===\n");
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
                // 70%
                extension.setViralEmissionRate(parameters.getViralLoadHighCm3() * 0.7);
                break;
                
            case INFECTIOUS_SYMPTOMATIC:
                // Carga viral alta
                extension.setViralEmissionRate(parameters.getViralLoadHighCm3());
                break;
                
            case SUPER_SPREADER:
                // Carga viral muy alta
                System.out.println("🚨 ¡SUPER_SPREADER DETECTADO! 🚨");
                extension.setViralEmissionRate(parameters.getViralLoadSuperCm3());
                System.out.println(String.format("   → Carga viral SUPER: %.2e (%.0f veces mayor)", 
                                parameters.getViralLoadSuperCm3(), 
                                parameters.getViralLoadSuperCm3() / parameters.getViralLoadHighCm3()));
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
        // DEBUG ENTRADA
        System.out.println(String.format("🔧 === CONFIGURANDO HABITACIÓN %d ===", roomId));

        List<User> usersInRoom = getUsersInRoom(roomId);
        if (usersInRoom.isEmpty()) {
            System.out.println("⚠️ Habitación " + roomId + " vacía - saltando configuración");
            System.out.println(String.format("🔧 === FIN CONFIGURACIÓN HABITACIÓN %d (SALTADA) ===", roomId));
            return;
        }
        
        roomId += 1;  // Ajuste de ID 
        double roomWidth = getRoomWidth(roomId);
        double roomLength = getRoomLength(roomId);
        double roomHeight = 3.0;
        
        double widthMeters = ModelParameters2.pixelsToMeters(roomWidth);
        double lengthMeters = ModelParameters2.pixelsToMeters(roomLength);
        
        // parameters.setRoomDimensions(lengthMeters, widthMeters, roomHeight);
        // parameters.setVentilationRates(0.35, 2.0, false);
        
        System.out.println("🔧 PASO 1: Obteniendo usuarios en habitación...");
        // List<User> usersInRoom = getUsersInRoom(roomId);
        
        System.out.println("🔧 PASO 2: Contando infectiosos...");
        int infectiousCount = countInfectiousPeople(usersInRoom);
        
        System.out.println(String.format("🔧 PASO 3: Configurando parámetros con %d usuarios, %d infectiosos", 
                        usersInRoom.size(), infectiousCount));
        
        parameters.setPeopleCount(usersInRoom.size(), infectiousCount);
        
        System.out.println(String.format("🔧 PASO 4: Parámetros establecidos: infectivePeople=%d", 
                        parameters.getInfectivePeople()));

        parameters.setRoomDimensions(lengthMeters, widthMeters, roomHeight);
        // parameters.setVentilationRates(0.35, 2.0, false);
        // Modificado por Nacho Palacio 2025-08-04 para test
        if (parameters.getTotalVentilationRateH() <= 2.5) { // Valor por defecto
            parameters.setVentilationRates(0.35, 2.0, false);
        }
        
        // Super spreaders
        int superSpreadersInRoom = 0;
        for (User user : usersInRoom) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && extension.getHealthStatus() == HealthStatus.SUPER_SPREADER) {
                superSpreadersInRoom++;
            }
        }
        
        if (superSpreadersInRoom > 0) {
            System.out.println(String.format("🚨 HABITACIÓN %d: ¡%d SUPER_SPREADER(S) PRESENTES!", 
                            roomId, superSpreadersInRoom));
        }
        
        double fractionWithMasks = calculateFractionWithMasks(usersInRoom);
        parameters.setMaskParameters(0.3, 0.4, fractionWithMasks);
        
        log("DEBUG", "configureModelForRoom", 
            String.format("Habitación %d: %.1fx%.1fx%.1f m, ventilación: %.1f h⁻¹, %d ocupantes (%d infectados)",
                        roomId, lengthMeters, widthMeters, roomHeight, 
                        parameters.getTotalVentilationRateH(), 
                        usersInRoom.size(), infectiousCount));
        
        System.out.println(String.format("🔧 === FIN CONFIGURACIÓN HABITACIÓN %d ===", roomId));
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

        // Añadido para debug
        System.out.println(String.format("🔍 getUsersInRoom(%d): INICIANDO búsqueda", roomId));
        
        try {
            if (Configuration.simulation != null) {
                List<User> allUsers = Configuration.simulation.getAllUsers();
                
                // Añadido para debug
                System.out.println(String.format("🔍 getUsersInRoom(%d): Total usuarios en simulación=%d", 
                              roomId, allUsers.size()));
                int usersChecked = 0;
                int usersMatched = 0;

                for (User user : allUsers) {
                    usersChecked++;
                    
                    if (user != null && user.room == roomId) {
                        usersInRoom.add(user);
                        usersMatched++;
                        
                        // DEBUG
                        UserEpidemicExtension ext = user.getEpidemicExtension();
                        String status = ext != null ? ext.getHealthStatus().toString() : "NULL";
                        System.out.println(String.format("   User %d: room=%d, status=%s", 
                                        user.userID, user.room, status));
                    } else if (user != null) {
                        // DEBUG
                        if (usersChecked <= 5) {
                            System.out.println(String.format("   ❌ User %d: room=%d (buscando %d)", 
                                            user.userID, user.room, roomId));
                        }
                    }
                }

                // Añadido para debug
                System.out.println(String.format("🔍 getUsersInRoom(%d): Revisados %d usuarios, encontrados %d", 
                              roomId, usersChecked, usersMatched));
            }
        } catch (Exception e) {
            log("ERROR", "getUsersInRoom", "Error al obtener usuarios en habitación " + roomId + ": " + e.getMessage());
        }

        // Añadido para debug
        System.out.println(String.format("🔍 getUsersInRoom(%d): RETORNANDO %d usuarios", 
                      roomId, usersInRoom.size()));
        
        return usersInRoom;
    }
    
    /**
     * Counts the number of infectious users in a list
     */
    private int countInfectiousPeople(List<User> users) {
        // DEBUG INICIAL
        System.out.println(String.format("🔍 countInfectiousPeople: INICIANDO - Recibidos %d usuarios", users.size()));
        
        int count = 0;
        int superSpreaderCount = 0;
        int asymptomaticCount = 0;
        int symptomaticCount = 0;
        int exposedCount = 0;
        int susceptibleCount = 0;
        int nullExtensionCount = 0;

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            
            if (extension == null) {
                nullExtensionCount++;
                System.out.println(String.format("   ⚠️ User %d: Extension es NULL", user.userID));
                continue;
            }
            
            HealthStatus status = extension.getHealthStatus();
            
            // DEBUG POR USUARIO (Solo primeros 10)
            if (i < 10) {
                System.out.println(String.format("   👤 User %d: room=%d, status=%s, infectious=%s", 
                                user.userID, user.room, status, isInfectious(extension)));
            }
            
            // Contar por estado
            switch (status) {
                case SUSCEPTIBLE:
                    susceptibleCount++;
                    break;
                case EXPOSED:
                    exposedCount++;
                    break;
                case INFECTIOUS_ASYMPTOMATIC:
                    asymptomaticCount++;
                    count++;
                    break;
                case INFECTIOUS_SYMPTOMATIC:
                    symptomaticCount++;
                    count++;
                    break;
                case SUPER_SPREADER:
                    superSpreaderCount++;
                    count++;
                    break;
            }
        }

        // DEBUG DETALLADO DE CONTEOS
        System.out.println(String.format("🔍 CONTEO DETALLADO:"));
        System.out.println(String.format("   👥 Total usuarios: %d", users.size()));
        System.out.println(String.format("   😷 Susceptibles: %d", susceptibleCount));
        System.out.println(String.format("   🔄 Expuestos: %d", exposedCount));
        System.out.println(String.format("   😶 Asintomáticos: %d", asymptomaticCount));
        System.out.println(String.format("   🤒 Sintomáticos: %d", symptomaticCount));
        System.out.println(String.format("   💀 Super Spreaders: %d", superSpreaderCount));
        System.out.println(String.format("   ❌ Extensions NULL: %d", nullExtensionCount));
        System.out.println(String.format("   🦠 TOTAL INFECTIOSOS: %d", count));

        // Mensaje final
        if (count > 0) {
            System.out.println(String.format("👥 INFECTADOS EN HABITACIÓN: Total=%d (Asint=%d, Sint=%d, Super=%d)", 
                            count, asymptomaticCount, symptomaticCount, superSpreaderCount));
            
            if (superSpreaderCount > 0) {
                System.out.println("🚨 ¡HAY " + superSpreaderCount + " SUPER_SPREADER(S) EN LA HABITACIÓN! 🚨");
            }
        }

        return count;
    }
    
    /**
     * Calculates the fraction of users wearing masks
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
            log("ERROR", "getRoomWidth", "Error al obtener ancho de habitación " + roomId + ": " + e.getMessage());
            return 10.0 * Configuration.getPixelsPerMeter(); // Valor por defecto
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
            log("ERROR", "getRoomLength", "Error al obtener largo de habitación " + roomId + ": " + e.getMessage());
            return 6.0 * Configuration.getPixelsPerMeter(); // Valor por defecto
        }
    }
    
    private boolean isInfectious(UserEpidemicExtension extension) {
        return extension.getHealthStatus().equals(HealthStatus.INFECTIOUS_ASYMPTOMATIC) ||
               extension.getHealthStatus().equals(HealthStatus.INFECTIOUS_SYMPTOMATIC) ||
               extension.getHealthStatus().equals(HealthStatus.SUPER_SPREADER);
    }
    
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
    /**
     * Logs messages with a specific level and method name
     */
    private void log(String level, String method, String message) {
        System.out.println(String.format("🔬 %s [%s] %s", level, method, message));
    }
    
    // IMPLEMENTACIÓN DE MÉTODOS DE LA INTERFAZ
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public ModelParameters1 getParameters() {
        ModelParameters1 compatParams = new ModelParameters1();
        
        compatParams.setRoomDimensions(parameters.getRoomVolumeM3(), 
                                      parameters.getRoomVolumeM3(), 
                                      3.0);
        compatParams.setVentilationRate(parameters.getTotalVentilationRateH());
        
        return compatParams;
    }
    
    @Override
    public void setParameters(ModelParameters1 parameters) {}

    /**
     * Gets the parameters specific to the Lelieveld model
     */
    public ModelParameters2 getLelieveldParameters() {
        return parameters;
    }
    
    /**
     * Sets the parameters specific to the Lelieveld model
     */
    public void setLelieveldParameters(ModelParameters2 parameters) {
        this.parameters = parameters;
    }
}