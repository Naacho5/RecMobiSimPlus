package es.unizar.epidemic.models;

import es.unizar.epidemic.ContactRecord;
import es.unizar.epidemic.EpidemicConfiguration;
import es.unizar.epidemic.HealthStatus;
import es.unizar.epidemic.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.simulation.User;
import java.util.List;

/**
 * Simple proximity-based transmission model
 * Transmission occurs based on distance, time, and base probability
 * Añadido por Nacho Palacio 2025-07-14
 */
public class SimpleProximityModel implements EpidemicModel {
    
    private PengParameters parameters;
    private String modelName = "Simple Proximity Model";
    
    // Simple model parameters
    private double maxTransmissionDistance = 1.5;    // meters
    private double baseTransmissionProbability = 0.01; // 5% base probability
    private int minContactDuration = 200;             // seconds
    
    public SimpleProximityModel() {
        this.parameters = new PengParameters();
    }

    public SimpleProximityModel(double maxTransmissionDistance, double baseTransmissionProbability, int minContactDuration) {
        this.parameters = new PengParameters();
        this.maxTransmissionDistance = maxTransmissionDistance;
        this.baseTransmissionProbability = baseTransmissionProbability;
        this.minContactDuration = minContactDuration;

        // DEBUG
        System.out.println("=== SIMPLE PROXIMITY MODEL INICIALIZADO ===");
        System.out.println("Distancia máxima por defecto: " + maxTransmissionDistance);
        System.out.println("Probabilidad base por defecto: " + baseTransmissionProbability);
        System.out.println("Duración mínima por defecto: " + minContactDuration);

        // System.out.println("\n=== SIMPLE PROXIMITY MODEL CREADO ===");
        // EpidemicConfiguration config = EpidemicConfiguration.getInstance();
        // config.printCurrentConfiguration();
    }
    
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        UserEpidemicExtension infExtension = getUserEpidemicExtension(infectious);
        UserEpidemicExtension susExtension = getUserEpidemicExtension(susceptible);
        
        if (infExtension == null) {
            return 0.0;
        }
        
        if (susExtension == null) {
            return 0.0;
        }
                
        HealthStatus infStatus = infExtension.getHealthStatus();
        if (infStatus != HealthStatus.INFECTIOUS_SYMPTOMATIC && 
            infStatus != HealthStatus.INFECTIOUS_ASYMPTOMATIC &&
            infStatus != HealthStatus.SUPER_SPREADER) {
            return 0.0;
        }
        
        HealthStatus susStatus = susExtension.getHealthStatus();
        if (susStatus != HealthStatus.SUSCEPTIBLE) {
            return 0.0;
        }
        
        double pixelsPerMeter = es.unizar.gui.Configuration.getPixelsPerMeter();
        double distanceMeters = contact.getDistance() / pixelsPerMeter;

        
        if (distanceMeters > maxTransmissionDistance) {
            return 0.0;
        }
        
        if (contact.getDuration() < minContactDuration) {
            return 0.0;
        }
        
        double transmissionProb = baseTransmissionProbability;
        
        transmissionProb *= contact.getContactType().getBaseProbability() / 0.05; // Normalize
        
        double distanceFactor = calculateDistanceFactor(distanceMeters);
        transmissionProb *= distanceFactor;

        double durationFactor = calculateDurationFactor(contact.getDuration());
        transmissionProb *= durationFactor;

        double infFactor = getInfectiousFactor(infExtension);
        transmissionProb *= infFactor;

        double maskFactor = getMaskFactor(infExtension, susExtension);
        transmissionProb *= maskFactor;

        double finalProb = Math.min(1.0, Math.max(0.0, transmissionProb));

        EpidemicStatistics stats = EpidemicStatistics.getInstance();
        stats.recordContact(infectious.userID, susceptible.userID, contact.getDuration(), contact.getRoomId());
        
        if (finalProb > 0) {
            stats.recordInfectiousContact(infectious.userID, susceptible.userID);
        }
        
        stats.setModelSpecificStat("Modelo utilizado", "Simple Proximity Model");
        stats.setModelSpecificStat("Distancia máxima transmisión", maxTransmissionDistance + " m");
        stats.setModelSpecificStat("Probabilidad base", (baseTransmissionProbability * 100) + "%");
        
        return finalProb;
    }
    
    /**
     * Calculates distance factor for transmission probability
     */
    private double calculateDistanceFactor(double distance) {
        double distanceMeters = distance; // Already in meters

        double factor;

        if (distanceMeters <= 0.5) {
            factor = 2.0;  // Very close contact
        } else if (distanceMeters <= 1.0) {
            factor = 1.5;  // Close contact
        } else if (distanceMeters <= 1.5) {
            factor = 1.25;  // Moderate distance
        } else if (distanceMeters <= maxTransmissionDistance) {
            factor = 1.0;  // Far distance
        } else {
            factor = 0.0;  // No transmission
        }

        return factor;
    }
    
    /**
     * Calculates duration factor for transmission probability
     */
    private double calculateDurationFactor(int durationSeconds) {
        double factor;
    
        if (durationSeconds < minContactDuration) {
            factor = 0.0;  // Too short
        } else if (durationSeconds <= 60) {
            factor = 1.0;  // Normal contact
        } else if (durationSeconds <= 300) {
            factor = 1.5;  // Extended contact
        } else if (durationSeconds <= 900) {
            factor = 2.0;  // Long contact
        } else {
            factor = 2.5;  // Very long contact
        }
        
        return factor;
    }
    
    /**
     * Gets infectious factor based on health status
     */
    private double getInfectiousFactor(UserEpidemicExtension extension) {
        switch (extension.getHealthStatus()) {
            case INFECTIOUS_SYMPTOMATIC:
                return 1.5;
            case INFECTIOUS_ASYMPTOMATIC:
                return 1.0;
            case SUPER_SPREADER:
                return 2.0;
            case EXPOSED:
                return 0.1; // Antes 0.0 
            default:
                return 0.0;
        }
    }
    
    /**
     * Calculates mask protection factor
     */
    private double getMaskFactor(UserEpidemicExtension infectious, UserEpidemicExtension susceptible) {
        boolean infMask = infectious.isMaskWearing();
        boolean susMask = susceptible.isMaskWearing();

        double factor;
        if (infMask && susMask) {
            factor = 0.25;
        } else if (infMask || susMask) {
            factor = 0.5; 
        } else {
            factor = 1.0;
        }

        return factor;
    }
    
    @Override
    public void updateHealthStates(List<User> users, int currentDay) {
        for (User user : users) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null) {
                updateUserHealthState(extension, currentDay);
            }
        }
    }
    
    /**
     * Updates individual user health state
     */
    private void updateUserHealthState(UserEpidemicExtension extension, int currentHour) {
        if (extension.getHealthStatus() != HealthStatus.SUSCEPTIBLE) {
            extension.setHoursSinceInfection(extension.getHoursSinceInfection() + 1);
        }
        
        switch (extension.getHealthStatus()) {
            case EXPOSED:
                handleExposedTransition(extension);
                break;
                
            case INFECTIOUS_ASYMPTOMATIC:
            case INFECTIOUS_SYMPTOMATIC:
                handleInfectiousTransition(extension);
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Handles transition from EXPOSED state
     */
    private void handleExposedTransition(UserEpidemicExtension extension) {
        if (extension.getHoursSinceInfection() >= extension.getIncubationPeriodHours()) {
            if (Math.random() < 0.7) {
                extension.setHealthStatus(HealthStatus.INFECTIOUS_ASYMPTOMATIC);
            } else {
                extension.setHealthStatus(HealthStatus.INFECTIOUS_SYMPTOMATIC);
            }
        }
    }
    
    /**
     * Handles transition from infectious states
     */
    private void handleInfectiousTransition(UserEpidemicExtension extension) {
        int totalSickTime = extension.getIncubationPeriodHours() + extension.getInfectiousPeriodHours();
        
        if (extension.getHoursSinceInfection() >= totalSickTime) {
            extension.setHealthStatus(HealthStatus.SUSCEPTIBLE);
            extension.setHoursSinceInfection(0);
            extension.setTransmissionProbability(0.0);
        }
    }
    
    /**
     * Gets epidemic extension from user (placeholder)
     */
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public PengParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(PengParameters parameters) {
        this.parameters = parameters;

        this.baseTransmissionProbability = parameters.getBaseTransmissionProbability();
    }
    
    /**
     * Configures simple model parameters
     */
    public void configureSimpleModel(double maxDistance, double baseProbability, int minDuration) {
        this.maxTransmissionDistance = maxDistance;
        this.baseTransmissionProbability = baseProbability;
        this.minContactDuration = minDuration;
    }
    
    // Getters
    public double getMaxTransmissionDistance() {
        return maxTransmissionDistance;
    }
    
    public double getBaseTransmissionProbability() {
        return baseTransmissionProbability;
    }
    
    public int getMinContactDuration() {
        return minContactDuration;
    }
    
    // Setters
    public void setMaxTransmissionDistance(double maxTransmissionDistance) {
        this.maxTransmissionDistance = maxTransmissionDistance;
    }
    
    public void setBaseTransmissionProbability(double baseTransmissionProbability) {
        this.baseTransmissionProbability = baseTransmissionProbability;
    }
    
    public void setMinContactDuration(int minContactDuration) {
        this.minContactDuration = minContactDuration;
    }

}