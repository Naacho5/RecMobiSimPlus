package es.unizar.epidemic.models;

import es.unizar.epidemic.ContactRecord;
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
    
    private ModelParameters1 parameters;
    private String modelName = "Simple Proximity Model";
    
    // Simple model parameters
    private double maxTransmissionDistance = 2.0;    // meters
    private double baseTransmissionProbability = 0.05; // 5% base probability
    private int minContactDuration = 15;             // seconds
    
    public SimpleProximityModel() {
        this.parameters = new ModelParameters1();
    
        this.maxTransmissionDistance = 3.0;
        this.baseTransmissionProbability = 0.15;
        this.minContactDuration = 5;               // segundos 
        
        System.out.println("🔧 SimpleProximityModel initialized:");
        System.out.println("   Max distance: " + maxTransmissionDistance + " m");
        System.out.println("   Base probability: " + (baseTransmissionProbability * 100) + "%");
        System.out.println("   Min duration: " + minContactDuration + " s");
    }
    
    @Override
    public double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact) {
        System.out.println("\n=== SIMPLE PROXIMITY MODEL - TRANSMISSION CALCULATION ===");
        System.out.println("🦠 Infectious User ID: " + infectious.userID);
        System.out.println("👤 Susceptible User ID: " + susceptible.userID);

        double pixelsPerMeter = es.unizar.gui.Configuration.getPixelsPerMeter();
        double distanceMeters = contact.getDistance() / pixelsPerMeter;

        System.out.println("Distance: " + contact.getDistance() + " pixels = " + distanceMeters + " meters");
        System.out.println("Contact duration: " + contact.getDuration() + " seconds");
        
        // No transmission if beyond maximum distance
        if (distanceMeters > maxTransmissionDistance) {
            System.out.println("❌ Distance too far: " + distanceMeters + " > " + maxTransmissionDistance);
            return 0.0;
        }
        
        // No transmission if contact duration is too short
        if (contact.getDuration() < minContactDuration) {
            System.out.println("❌ Contact too short: " + contact.getDuration() + " < " + minContactDuration);
            return 0.0;
        }
        
        double transmissionProb = baseTransmissionProbability;
        System.out.println("Base transmission probability: " + transmissionProb);
        
        // REVISAR SI TENER EN CUENTA EL TIPO DE CONTACTO
        transmissionProb *= contact.getContactType().getBaseProbability() / 0.05; // Normalize to base
        System.out.println("After contact type adjustment: " + transmissionProb);
        
        // More risk if closer contact
        double distanceFactor = calculateDistanceFactor(distanceMeters);
        transmissionProb *= distanceFactor;
        System.out.println("After distance factor (" + distanceFactor + "): " + transmissionProb);
        
        // More risk if longer contact
        double durationFactor = calculateDurationFactor(contact.getDuration());
        transmissionProb *= durationFactor;
        System.out.println("After duration factor (" + durationFactor + "): " + transmissionProb);
        
        // Get infectious user's transmission probability
        UserEpidemicExtension infExtension = getUserEpidemicExtension(infectious);
        if (infExtension != null) {
            double infFactor = getInfectiousFactor(infExtension);
            transmissionProb *= infFactor;
            System.out.println("After infectious factor (" + infFactor + "): " + transmissionProb);
        }
        
        // Mask effects
        UserEpidemicExtension susExtension = getUserEpidemicExtension(susceptible);
        if (infExtension != null && susExtension != null) {
            double maskFactor = getMaskFactor(infExtension, susExtension);
            transmissionProb *= maskFactor;
            System.out.println("After mask factor (" + maskFactor + "): " + transmissionProb);
        }
        
        // Probability between 0 and 1
        double finalProb = Math.min(1.0, Math.max(0.0, transmissionProb));
        System.out.println("🎯 FINAL TRANSMISSION PROBABILITY: " + finalProb);
        System.out.println("===============================");


        // Statistics
        EpidemicStatistics stats = EpidemicStatistics.getInstance();
        stats.recordContact(infectious.userID, susceptible.userID, contact.getDuration(), contact.getRoomId());
        
        if (finalProb > 0) {
            stats.recordInfectiousContact(infectious.userID, susceptible.userID);
        }
        
        // Estadísticas específicas del modelo
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
            factor = 3.0;  // Very close contact
            System.out.println("   📏 Distance classification: Very close (≤0.5m) → factor = " + factor);
        } else if (distanceMeters <= 1.0) {
            factor = 2.0;  // Close contact
            System.out.println("   📏 Distance classification: Close (≤1.0m) → factor = " + factor);
        } else if (distanceMeters <= 1.5) {
            factor = 1.5;  // Moderate distance
            System.out.println("   📏 Distance classification: Moderate (≤1.5m) → factor = " + factor);
        } else if (distanceMeters <= maxTransmissionDistance) {
            factor = 1.0;  // Far but still within transmission range
            System.out.println("   📏 Distance classification: Far (≤" + maxTransmissionDistance + "m) → factor = " + factor);
        } else {
            // Este caso no debería ocurrir porque se verifica antes en calculateTransmissionProbability
            factor = 0.0;  // No transmission
            System.out.println("   📏 Distance classification: Too far (>" + maxTransmissionDistance + "m) → factor = " + factor);
        }

        return factor;
    }
    
    /**
     * Calculates duration factor for transmission probability
     */
    private double calculateDurationFactor(int durationSeconds) {
        double factor;
    
        // REVISAR LOS TIEMPOS
        if (durationSeconds < minContactDuration) {
            factor = 0.0;  // Too short
            System.out.println("   ⏱️ Duration classification: Too short (<" + minContactDuration + "s) → factor = " + factor);
        } else if (durationSeconds <= 60) {
            factor = 1.0;  // Normal contact
            System.out.println("   ⏱️ Duration classification: Normal (≤60s) → factor = " + factor);
        } else if (durationSeconds <= 300) {
            factor = 1.5;  // Extended contact
            System.out.println("   ⏱️ Duration classification: Extended (≤5min) → factor = " + factor);
        } else if (durationSeconds <= 900) {
            factor = 2.0;  // Long contact
            System.out.println("   ⏱️ Duration classification: Long (≤15min) → factor = " + factor);
        } else {
            factor = 2.5;  // Very long contact
            System.out.println("   ⏱️ Duration classification: Very long (>15min) → factor = " + factor);
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
            case EXPOSED:
                return 0.0; 
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
        
        if (infMask && susMask) {
            return 0.25;  // Both wearing masks
        } else if (infMask || susMask) {
            return 0.5;   // One wearing mask
        } else {
            return 1.0;   // No masks
        }
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
                // Doesn't change
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
                System.out.println("🦠 User becomes INSTANTLY infectious asymptomatic"); // Añadido por Nacho Palacio 2025-07-17
            } else {
                extension.setHealthStatus(HealthStatus.INFECTIOUS_SYMPTOMATIC);
                System.out.println("🦠 User becomes INSTANTLY infectious symptomatic"); // Añadido por Nacho Palacio 2025-07-17
            }
        }
    }
    
    /**
     * Handles transition from infectious states
     */
    private void handleInfectiousTransition(UserEpidemicExtension extension) {
        int totalSickTime = extension.getIncubationPeriodHours() + extension.getInfectiousPeriodHours();
        
        if (extension.getHoursSinceInfection() >= totalSickTime) {
            // Recover and become susceptible again
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
    public ModelParameters1 getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(ModelParameters1 parameters) {
        this.parameters = parameters;
        
        // Update simple model parameters from ModelParameters if needed
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