package es.unizar.epidemic;

import es.unizar.gui.simulation.User;
import java.util.List;
import java.util.Random;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class EpidemicStateManager {
    
    private Random random = new Random();
    private double asymptomaticProbability = 0.3;
    private double superSpreaderProbability = 0.075; // Between 5-10% chance of being a super-spreader

    // AJUSTAR ESTOS VALORES
    private int minIncubationHours = 0;
    private int maxIncubationHours = 0;
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
     * REVISAR: Quiza si estas infectado, no recuperarse.
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
        if (hoursSinceInfection >= incubationPeriod) {
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
            
            int infectiousPeriod = minInfectiousHours + random.nextInt(maxInfectiousHours - minInfectiousHours);
            extension.setInfectiousPeriod(infectiousPeriod);
        }
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
            extension.setHealthStatus(HealthStatus.EXPOSED);

            extension.setViralEmissionRate(0.0); // Añadido por Nacho Palacio 2025-07-23

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
     * Infects multiple users at simulation start
     */
    public void infectInitialUsers(List<User> userList, int numberOfInitialInfected) {
        int infected = 0;
        
        while (infected < numberOfInitialInfected && infected < userList.size()) {
            User randomUser = userList.get(random.nextInt(userList.size()));
            UserEpidemicExtension extension = getUserEpidemicExtension(randomUser);
            
            if (extension != null && extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE) {
                infectUser(randomUser);
                infected++;
            }
        }
        
        System.out.println("Initially infected: " + infected + " users");
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
