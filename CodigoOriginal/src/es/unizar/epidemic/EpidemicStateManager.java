package es.unizar.epidemic;

import es.unizar.gui.simulation.User;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class EpidemicStateManager {
    
    private Random random = new Random();
    private double asymptomaticProbability = 0.3;
    private double superSpreaderProbability = 0.075;

    // AJUSTAR ESTOS VALORES
    private int minIncubationHours = 0;
    private int maxIncubationHours = 0;

    // Don't used now since infected users don't recover.
    private int minInfectiousHours = 120;
    private int maxInfectiousHours = 240;
    
    /**
     * Updates health states of all users based on elapsed time
     */
    public void updateHealthStates(List<User> userList, int currentHour) {
        for (User user : userList) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null) {
                updateIndividualHealthState(extension, currentHour);
            }
        }
    }

    /**
     * Updates health state of an individual user
     */
    private void updateIndividualHealthState(UserEpidemicExtension extension, int currentHour) {
        // Increment time since infection
        if (isInfected(extension)) {
            extension.setHoursSinceInfection(extension.getHoursSinceInfection() + 1);
        }
        
        switch (extension.getHealthStatus()) {
            case EXPOSED:
                handleExposedState(extension);
                break;
                
            case INFECTIOUS_ASYMPTOMATIC:
            case INFECTIOUS_SYMPTOMATIC:
                // handleInfectiousState(extension);
                break;
                
            default:
                // SUSCEPTIBLE doesn't require temporal updates
                break;
        }
    }

    /**
     * Handles transition from EXPOSED state
     */
    private void handleExposedState(UserEpidemicExtension extension) {
        int hoursSinceInfection = extension.getHoursSinceInfection();
        int incubationPeriod = extension.getIncubationPeriodHours();
        
        // Check if incubation period is complete
        // if (hoursSinceInfection >= incubationPeriod) {
        double randomValue = random.nextDouble();
        
        if (randomValue < superSpreaderProbability) {
            extension.setHealthStatus(HealthStatus.SUPER_SPREADER);
            double superRate = 18.6 * 10.0; // 10x mayor que normal
            extension.setViralEmissionRate(superRate);
        } 
        else if (randomValue < superSpreaderProbability + asymptomaticProbability) {
            extension.setHealthStatus(HealthStatus.INFECTIOUS_ASYMPTOMATIC);  
            double asymptomaticRate = 18.6 * 0.8;
            extension.setViralEmissionRate(asymptomaticRate);
        } else {
            extension.setHealthStatus(HealthStatus.INFECTIOUS_SYMPTOMATIC);
            double symptomaticRate = 18.6 * 1.5;
            extension.setViralEmissionRate(symptomaticRate);
        }

        // }
    }

    /**
     * Handles transition from infectious states
     * Don't used now since infected users don't recover.
     */
    private void handleInfectiousState(UserEpidemicExtension extension) {
        int hoursSinceInfection = extension.getHoursSinceInfection();
        int totalDiseaseTime = extension.getIncubationPeriodHours() + extension.getInfectiousPeriodHours();
        
        // Check if infectious period is complete
        if (hoursSinceInfection >= totalDiseaseTime) {
            extension.setHealthStatus(HealthStatus.SUSCEPTIBLE);
            extension.setTransmissionProbability(0.0);
            System.out.println("User recovers after " + hoursSinceInfection + " hours of illness");
        }
    }
    
    /**
     * Infects a susceptible user
     */
    public void infectUser(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        
        if (extension != null && extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE) {
            // extension.setHealthStatus(HealthStatus.EXPOSED);
            // extension.setViralEmissionRate(0.0); // Añadido por Nacho Palacio 2025-07-23

            // Cambiado para pasar directamente a Infectado
            handleExposedState(extension);
            extension.setHoursSinceInfection(0);

            int incubationPeriod;
            if (maxIncubationHours <= minIncubationHours) {
                incubationPeriod = minIncubationHours;
            } else {
                incubationPeriod = minIncubationHours + random.nextInt(maxIncubationHours - minIncubationHours);
            }
            
            extension.setIncubationPeriod(incubationPeriod);
        }
    }

    /**
     * Infects a given number of initial users randomly from the susceptible population
     */
    public void infectInitialUsers(List<User> userList, int numberOfInitialInfected) {
        if (userList.isEmpty() || numberOfInitialInfected <= 0) return;
        
        List<User> susceptibleUsers = userList.stream()
            .filter(user -> {
                UserEpidemicExtension ext = getUserEpidemicExtension(user);
                return ext != null && ext.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
            })
            .collect(Collectors.toList());

        Collections.shuffle(susceptibleUsers, random);
        
        int toInfect = Math.min(numberOfInitialInfected, susceptibleUsers.size());
        
        for (int i = 0; i < toInfect; i++) {
            User user = susceptibleUsers.get(i);
            infectUser(user);
        }
        
        System.out.println("Initially infected: " + toInfect + " users");
    }
    
    /**
     * Checks if a user is infectious (can transmit the virus)
     */
    public boolean isInfectious(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        if (extension == null) return false;
        
        return extension.getHealthStatus() == HealthStatus.INFECTIOUS_ASYMPTOMATIC ||
           extension.getHealthStatus() == HealthStatus.INFECTIOUS_SYMPTOMATIC ||
           extension.getHealthStatus() == HealthStatus.SUPER_SPREADER;
    }
    
    /**
     * Checks if a user is susceptible (can be infected)
     */
    public boolean isSusceptible(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        if (extension == null) return false;
        
        return extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
    }

    /**
     * Checks if a user is infected
     */
    private boolean isInfected(UserEpidemicExtension extension) {
        if (extension == null) return false;
        return extension.getHealthStatus() != HealthStatus.SUSCEPTIBLE;
    }

    /**
     * Checks if a user is infected (any post-susceptible state)
     */
    public boolean isInfected(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        return isInfected(extension);
    }

    /**
     * Gets epidemic extension from a user
     */
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
}
