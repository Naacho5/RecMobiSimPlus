package es.unizar.epidemic.general;

import es.unizar.gui.simulation.User;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Manages the epidemic state of users in the simulation
 * 
 * @author Nacho Palacio
 */
public class EpidemicStateManager {
    
    private Random random = new Random();

    // Users are instantly infected
    private int minIncubationHours = 0;
    private int maxIncubationHours = 0;

    // Don't used now since infected users don't recover.
    // private int minInfectiousHours = 120;
    // private int maxInfectiousHours = 240;
    
    /**
     * Updates health states of all users based on elapsed time.
     * Iterates through all users and updates their individual health states.
     * 
     * @param userList list of all users in the simulation
     * @param currentHour current simulation hour
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
     * Updates health state of an individual user.
     * Increments time since infection and handles state transitions.
     * 
     * @param extension the user's epidemic extension to update
     * @param currentHour current simulation hour
     */
    private void updateIndividualHealthState(UserEpidemicExtension extension, int currentHour) {
        // Increment time since infection
        if (isInfected(extension)) {
            extension.setHoursSinceInfection(extension.getHoursSinceInfection() + 1);
        }
        
        System.out.println("Health status before update: " + extension.getHealthStatus());
        switch (extension.getHealthStatus()) {
            case INFECTIOUS_SYMPTOMATIC:
                // handleInfectiousState(extension);
                break;
                
            default:
                // SUSCEPTIBLE doesn't require temporal updates
                break;
        }
    }

    /**
     * Handles transition from EXPOSED state.
     * Determines if the user becomes a super spreader or regular infectious case
     * based on configured probabilities.
     * 
     * @param extension the user's epidemic extension to update
     */
    private void handleExposedState(UserEpidemicExtension extension) {
        double randomValue = random.nextDouble();
        extension.setInfected(true);

        double superSpreaderProbability = EpidemicConfiguration.getInstance().getSuperSpreaderProbability();
        
        if (randomValue < superSpreaderProbability) {
            // System.out.println("User becomes a SuperSpreader!");
            extension.setHealthStatus(HealthStatus.SUPER_SPREADER);
        }  
        else {
            extension.setHealthStatus(HealthStatus.INFECTIOUS_SYMPTOMATIC);
        }
    }
    
    /**
     * Infects a susceptible user.
     * Changes the user's health status from susceptible to infected,
     * sets incubation period, and initializes infection tracking.
     * 
     * @param user the user to infect
     */
    public void infectUser(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        
        if (extension != null && extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE) {
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
     * Infects a given number of initial users randomly from the susceptible population.
     * Randomly selects susceptible users and infects them to seed the epidemic.
     * 
     * @param userList list of all users in the simulation
     * @param numberOfInitialInfected number of users to initially infect
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
            user.isInfected = true;
        }        
    }
    
    /**
     * Checks if a user is infectious (can transmit the virus).
     * A user is considered infectious if they are symptomatic or a super spreader.
     * 
     * @param user the user to check
     * @return true if the user can transmit the virus, false otherwise
     */
    public boolean isInfectious(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        if (extension == null) return false;
        
        return extension.getHealthStatus() == HealthStatus.INFECTIOUS_SYMPTOMATIC ||
           extension.getHealthStatus() == HealthStatus.SUPER_SPREADER;
    }
    
    /**
     * Checks if a user is susceptible (can be infected).
     * A user is susceptible if they have not yet been infected.
     * 
     * @param user the user to check
     * @return true if the user can be infected, false otherwise
     */
    public boolean isSusceptible(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        if (extension == null) return false;
        
        return extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
    }

    /**
     * Checks if a user is infected based on their epidemic extension.
     * A user is infected if their health status is not susceptible.
     * 
     * @param extension the user's epidemic extension
     * @return true if the user is infected, false otherwise
     */
    private boolean isInfected(UserEpidemicExtension extension) {
        if (extension == null) return false;
        return extension.getHealthStatus() != HealthStatus.SUSCEPTIBLE;
    }

    /**
     * Checks if a user is infected (any post-susceptible state).
     * Convenience method that extracts the epidemic extension and checks infection status.
     * 
     * @param user the user to check
     * @return true if the user is infected, false otherwise
     */
    public boolean isInfected(User user) {
        UserEpidemicExtension extension = getUserEpidemicExtension(user);
        return isInfected(extension);
    }

    /**
     * Gets epidemic extension from a user.
     * 
     * @param user the user to get the epidemic extension from
     * @return the user's epidemic extension, or null if not available
     */
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
}
