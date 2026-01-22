package es.unizar.epidemic.models;

import es.unizar.epidemic.contact.ContactRecord;
import es.unizar.epidemic.general.HealthStatus;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.simulation.User;
import java.util.List;

/**
 * Simple proximity-based transmission model
 * Transmission occurs based on distance, time, and base probability
 * 
 * @author Nacho Palacio
 */
public class SimpleProximityModel extends AbstractEpidemicModel {
    
    private PengParameters parameters;
    private String modelName = "Simple Proximity Model";
    
    // Simple model parameters
    private double maxTransmissionDistance = 6.5;    // meters
    private double baseTransmissionProbability = 0.1; // 5% base probability
    private int minContactDuration = 300;             // seconds
    
    public SimpleProximityModel() {
        this.parameters = new PengParameters();
    }

    public SimpleProximityModel(double maxTransmissionDistance, double baseTransmissionProbability, int minContactDuration) {
        this.parameters = new PengParameters();
        this.maxTransmissionDistance = maxTransmissionDistance;
        this.baseTransmissionProbability = baseTransmissionProbability;
        this.minContactDuration = minContactDuration;
    }
    
    /**
     * Calculates the transmission probability based on proximity and contact.
     * Takes into account distance, contact duration, health status, infectious factors,
     * and mask usage to determine the probability of transmission between users.
     * 
     * @param infectious the infectious user
     * @param susceptible the susceptible user
     * @param contact the contact record between users
     * @return transmission probability (0.0 to 1.0)
     */
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
            infStatus != HealthStatus.SUPER_SPREADER) {
            return 0.0;
        }
        
        HealthStatus susStatus = susExtension.getHealthStatus();
        if (susStatus != HealthStatus.SUSCEPTIBLE) {
            return 0.0;
        }

        UserEpidemicExtension extension = susceptible.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return 0.0;
        }
        
        double pixelsPerMeter = es.unizar.gui.Configuration.getPixelsPerMeter();
        double distanceMeters = contact.getDistance() / pixelsPerMeter;
        int duration = contact.getDuration();

        
        if (distanceMeters > maxTransmissionDistance) {
            return 0.0;
        }
        
        if (duration < minContactDuration) {
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
     * Calculates distance factor for transmission probability.
     * Returns a multiplier based on distance between users, with closer
     * proximity resulting in higher transmission risk.
     * REVISAR Y AJUSTAR VALORES
     * 
     * @param distance distance between users in meters
     * @return distance factor multiplier (0.0 to 8.0)
     */
    private double calculateDistanceFactor(double distance) {
        double distanceMeters = distance; // Already in meters

        double factor;

        if (distanceMeters <= 0.5) {
            factor = 8.0;  // Very close contact (antes 2.0)
        } else if (distanceMeters <= 1.0) {
            factor = 4.0;  // Close contact (antes 1.5)
        } else if (distanceMeters <= 1.5) {
            factor = 2.0;  // Moderate distance (antes 1.25)
        } else if (distanceMeters <= maxTransmissionDistance) {
            factor = 1.0;  // Far distance
        } else {
            factor = 0.0;  // No transmission
        }

        return factor;
    }
    
    /**
     * Calculates duration factor for transmission probability.
     * Returns a multiplier based on contact duration, with longer
     * exposure resulting in higher transmission risk.
     * REVISAR Y AJUSTAR VALORES
     * 
     * @param durationSeconds contact duration in seconds
     * @return duration factor multiplier (0.0 to 2.5)
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
     * Gets infectious factor based on health status.
     * Returns a multiplier representing the infectiousness level
     * of the user based on their current health state.
     * 
     * @param extension the user's epidemic extension data
     * @return infectious factor multiplier (0.0 to 1.0)
     */
    private double getInfectiousFactor(UserEpidemicExtension extension) {
        switch (extension.getHealthStatus()) {
            case INFECTIOUS_SYMPTOMATIC:
                return 1.0;
            case SUPER_SPREADER:
                return 1.0; 
            default:
                return 0.0;
        }
    }
    
    /**
     * Calculates mask protection factor.
     * Returns a reduction multiplier based on whether the infectious
     * and/or susceptible user is wearing a mask.
     * 
     * @param infectious the infectious user's epidemic extension
     * @param susceptible the susceptible user's epidemic extension
     * @return mask protection factor (0.25 to 1.0)
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
    
    /**
     * Updates health states for all users.
     * Simple model does not implement health state updates.
     * 
     * @param users list of all users in the simulation
     * @param currentDay current simulation day
     */
    @Override
    public void updateHealthStates(List<User> users, int currentDay) {
        // Simple model does not implement health state updates
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
     * @return Peng model parameters (used for compatibility)
     */
    @Override
    public PengParameters getParameters() {
        return parameters;
    }
    
    /**
     * Sets the parameters object for this model.
     * Extracts base transmission probability from the parameters.
     * 
     * @param parameters new Peng model parameters
     */
    @Override
    public void setParameters(PengParameters parameters) {
        this.parameters = parameters;

        this.baseTransmissionProbability = parameters.getBaseTransmissionProbability();
    }
    
    /**
     * Configures simple model parameters.
     * Sets the maximum transmission distance, base probability,
     * and minimum contact duration for the proximity model.
     * 
     * @param maxDistance maximum distance for transmission in meters
     * @param baseProbability base transmission probability (0.0 to 1.0)
     * @param minDuration minimum contact duration in seconds
     */
    public void configureSimpleModel(double maxDistance, double baseProbability, int minDuration) {
        this.maxTransmissionDistance = maxDistance;
        this.baseTransmissionProbability = baseProbability;
        this.minContactDuration = minDuration;
    }
    
    // Getters
    
    /**
     * Gets the maximum transmission distance.
     * 
     * @return maximum transmission distance in meters
     */
    public double getMaxTransmissionDistance() {
        return maxTransmissionDistance;
    }
    
    /**
     * Gets the base transmission probability.
     * 
     * @return base transmission probability (0.0 to 1.0)
     */
    public double getBaseTransmissionProbability() {
        return baseTransmissionProbability;
    }
    
    /**
     * Gets the minimum contact duration.
     * 
     * @return minimum contact duration in seconds
     */
    public int getMinContactDuration() {
        return minContactDuration;
    }
    
    // Setters
    
    /**
     * Sets the maximum transmission distance.
     * 
     * @param maxTransmissionDistance maximum transmission distance in meters
     */
    public void setMaxTransmissionDistance(double maxTransmissionDistance) {
        this.maxTransmissionDistance = maxTransmissionDistance;
    }
    
    /**
     * Sets the base transmission probability.
     * 
     * @param baseTransmissionProbability base transmission probability (0.0 to 1.0)
     */
    public void setBaseTransmissionProbability(double baseTransmissionProbability) {
        this.baseTransmissionProbability = baseTransmissionProbability;
    }
    
    /**
     * Sets the minimum contact duration.
     * 
     * @param minContactDuration minimum contact duration in seconds
     */
    public void setMinContactDuration(int minContactDuration) {
        this.minContactDuration = minContactDuration;
    }

}