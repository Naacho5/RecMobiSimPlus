package es.unizar.epidemic.general;

import es.unizar.gui.MainSimulator;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.contact.ContactTracker;
import es.unizar.epidemic.models.EpidemicModel;
import es.unizar.epidemic.models.SimpleProximityModel;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.models.PengTransmissionModel;
import es.unizar.epidemic.models.LelieveldTransmissionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central manager for epidemic simulation that coordinates all epidemic components
 * 
 * @author Nacho Palacio
 */
public class EpidemicSimulationManager {
    
    private EpidemicStateManager stateManager;
    private ContactTracker contactTracker;
    private EpidemicModel epidemicModel;
    private EpidemicConfiguration config;
    
    public EpidemicSimulationManager() {
        this.config = EpidemicConfiguration.getInstance();
        this.stateManager = new EpidemicStateManager();
        this.contactTracker = new ContactTracker(config.getMaxTransmissionDistance(), config.getMinContactDuration());

        this.config.printCurrentConfiguration();
        
        if ("SIMPLE_PROXIMITY".equals(config.getSelectedModel())) {
            double maxDist = config.getMaxTransmissionDistance();
            double baseProb = config.getBaseTransmissionProbability();
            int minDuration = config.getMinContactDuration();                 
            SimpleProximityModel simpleModel = new SimpleProximityModel(maxDist, baseProb, minDuration);
            this.epidemicModel = simpleModel;
        } else if ("AEROSOL_LELIEVELD".equals(config.getSelectedModel())) {
            this.epidemicModel = new LelieveldTransmissionModel();
        } else {
            this.epidemicModel = new PengTransmissionModel();
        }
        
        this.contactTracker.setEpidemicModel(this.epidemicModel);
        this.contactTracker.setEpidemicStateManager(this.stateManager);
    }
    
    /**
     * Initializes epidemic system for all users.
     * Sets up immune status, configures mask wearing, infects initial users,
     * and initializes exposure tracking for aerosol models.
     * 
     * @param users list of all users in the simulation
     */
    public void initializeEpidemicSystem(List<User> users) {
        EpidemicConfiguration config = EpidemicConfiguration.getInstance();
        double immuneFraction = config.getImmunePopulationFraction();
          
        int targetImmuneUsers = (int) Math.round(users.size() * immuneFraction);
        
        List<User> shuffledUsers = new ArrayList<>(users);
        java.util.Collections.shuffle(shuffledUsers);
        
        for (int i = 0; i < shuffledUsers.size(); i++) {
            User user = shuffledUsers.get(i);
            
            if (user.getEpidemicExtension() == null) {
                user.setEpidemicExtension(new UserEpidemicExtension());
            }
            
            UserEpidemicExtension extension = user.getEpidemicExtension();
            
            if (i < targetImmuneUsers) {
                extension.setImmune(true);
            } else {
                extension.setImmune(false);
            }
        }
    
        configureMaskWearing(users);
        
        stateManager.infectInitialUsers(users, config.getInitialInfectedUsers());

        if (epidemicModel instanceof PengTransmissionModel) {
            ((PengTransmissionModel) epidemicModel).initializeExposureTracking(users);
        } else if (epidemicModel instanceof LelieveldTransmissionModel) {
            ((LelieveldTransmissionModel) epidemicModel).initializeExposureTracking(users);
        }

        int immuneCount = 0;
        int susceptibleCount = 0;
        int infectedCount = 0;
        
        for (User user : users) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null) {
                if (extension.isImmune()) {
                    immuneCount++;
                } else {
                    switch (extension.getHealthStatus()) {
                        case SUSCEPTIBLE:
                            susceptibleCount++;
                            break;
                        case INFECTIOUS_SYMPTOMATIC:
                        case SUPER_SPREADER:
                            infectedCount++;
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (epidemicModel != null) {
            epidemicModel.updateHealthStates(users, 0);
        }
        
        System.out.printf(" Initial user state:\n");
        System.out.printf("   - Immune: %d (%.1f%%)\n", immuneCount, (double)immuneCount/users.size()*100);
        System.out.printf("   - Susceptible: %d (%.1f%%)\n", susceptibleCount, (double)susceptibleCount/users.size()*100);
        System.out.printf("   - Infected: %d (%.1f%%)\n", infectedCount, (double)infectedCount/users.size()*100);
    }
    
    /**
     * Updates epidemic state each simulation step.
     * Processes room exposure, tracks contacts, updates visual status,
     * and records aerosol concentrations for statistical analysis.
     * 
     * @param users list of all users in the simulation
     * @param currentIteration current iteration number of the simulation
     */
    public void updateEpidemicState(List<User> users, int currentIteration) {
        // System.out.println("updateEpidemicState called, iteration " + currentIteration);
        if (epidemicModel instanceof PengTransmissionModel) {
            ((PengTransmissionModel) epidemicModel).updateRoomExposure(users, getDeltaTimeHours());
            ((PengTransmissionModel) epidemicModel).recordExposureIteration(users, currentIteration); // Added by Nacho Palacio 2025-12-03
        } else if (epidemicModel instanceof LelieveldTransmissionModel) {
            ((LelieveldTransmissionModel) epidemicModel).updateRoomExposure(users, getDeltaTimeHours());
            ((LelieveldTransmissionModel) epidemicModel).recordExposureIteration(users, currentIteration); // Added by Nacho Palacio 2025-12-03
        }
        else if (epidemicModel instanceof SimpleProximityModel) { // Added by Nacho Palacio 2025-11-12
            contactTracker.trackContacts(users, currentIteration);
        }
        
        updateUserVisualStatus(users);


        // Added by Nacho Palacio 2025-10-03
        LelieveldTransmissionModel lelieveldModel = null;
        if (epidemicModel instanceof LelieveldTransmissionModel) {
            lelieveldModel = (LelieveldTransmissionModel) epidemicModel;
        }

        if (lelieveldModel != null) {
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            double durationSeconds = getDeltaTimeHours() * 3600.0;

            Map<Integer, List<User>> usersByRoom = new HashMap<>();
            for (User user : users) {
                usersByRoom.computeIfAbsent(user.room, k -> new ArrayList<>()).add(user);
            }

            // Calculate and record concentration for each room
            for (Map.Entry<Integer, List<User>> entry : usersByRoom.entrySet()) {
                int roomId = entry.getKey();
                List<User> usersInRoom = entry.getValue();

                lelieveldModel.configureModelForRoom(roomId);

                double totalViralLoad = 0.0;
                int infectivePeople = 0;
                for (User user : usersInRoom) {
                    UserEpidemicExtension ext = user.getEpidemicExtension();
                    if (ext != null && lelieveldModel.isInfectious(ext)) {
                        totalViralLoad += ext.getViralEmissionRate();
                        infectivePeople++;
                    }
                }

                lelieveldModel.recordRoomInfectiousCount(roomId, currentIteration, infectivePeople); //  Added by Nacho Palacio 2025-12-03

                double fractionWithMasks = lelieveldModel.calculateFractionWithMasks(usersInRoom);

                double concentration = lelieveldModel.getLelieveldParameters()
                    .calculateViralConcentration(totalViralLoad, fractionWithMasks, infectivePeople);

                if (infectivePeople > 0 && concentration > 0.0) {
                    stats.recordRoomAerosolConcentration(roomId, concentration, durationSeconds); // Modified by Nacho Palacio 2025-12-13
                }
            }
        }

        PengTransmissionModel pengModel = null;
        if (epidemicModel instanceof PengTransmissionModel) {
            pengModel = (PengTransmissionModel) epidemicModel;
        }

        if (pengModel != null) {
            EpidemicStatistics stats = EpidemicStatistics.getInstance();
            double durationSeconds = getDeltaTimeHours() * 3600.0;

            Map<Integer, List<User>> usersByRoom = new HashMap<>();
            for (User user : users) {
                usersByRoom.computeIfAbsent(user.room, k -> new ArrayList<>()).add(user);
            }

            // Calculate and record concentration for each room
            for (Map.Entry<Integer, List<User>> entry : usersByRoom.entrySet()) {
                int roomId = entry.getKey();
                List<User> usersInRoom = entry.getValue();

                pengModel.configureModelForRoom(roomId);

                int infectivePeople = 0;
                for (User user : usersInRoom) {
                    UserEpidemicExtension ext = user.getEpidemicExtension();
                    if (ext != null && pengModel.isInfectious(ext)) {
                        infectivePeople++;
                    }
                }

                pengModel.recordRoomInfectiousCount(roomId, currentIteration, infectivePeople); //  Added by Nacho Palacio 2025-12-03

                double concentration = pengModel.getParameters()
                    .calculateQuantaConcentration(infectivePeople);

                if (infectivePeople > 0 && concentration > 0.0) {
                    stats.recordRoomAerosolConcentration(roomId, concentration, durationSeconds); // Modified by Nacho Palacio 2025-12-13
                }
            }
        }

    }

    /**
     * Evaluates final aerosol transmissions at the end of the simulation step.
     * Calculates combined infection risk for susceptible users and determines
     * if infection occurs based on accumulated aerosol exposure.
     * Only applicable for Peng and Lelieveld transmission models.
     * 
     * @param users list of all users in the simulation
     */
    public void evaluateFinalAerosolTransmissions(List<User> users) {
        System.out.println("Evaluating final aerosol transmissions...");
        boolean isPeng = epidemicModel instanceof es.unizar.epidemic.models.PengTransmissionModel;
        boolean isLelieveld = epidemicModel instanceof es.unizar.epidemic.models.LelieveldTransmissionModel;

        if (!isPeng && !isLelieveld) {
            return;
        }

        for (User user : users) {
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext == null || ext.isImmune() || ext.getHealthStatus() != HealthStatus.SUSCEPTIBLE)
                continue;

            double combinedRisk = 0.0;
            if (isPeng) {
                System.out.println("Peng: Calculating combined risk for user " + user.userID);
                combinedRisk = ((es.unizar.epidemic.models.PengTransmissionModel)epidemicModel)
                    .calculateCombinedInfectionRiskForUser(user);
            } else if (isLelieveld) {
                System.out.println("Lelieveld: Calculating combined risk for user " + user.userID);
                combinedRisk = ((es.unizar.epidemic.models.LelieveldTransmissionModel)epidemicModel)
                    .calculateCombinedInfectionRiskForUser(user);
            }

            double randomValue = Math.random();
            System.out.printf(" User %d: Combined risk %.4f, random value %.4f\n",
                user.userID, combinedRisk, randomValue);
           
            if (randomValue < combinedRisk) {
                System.out.printf("  User %d infected by aerosol transmission (risk %.4f < %.4f)\n",
                    user.userID, randomValue, combinedRisk);
                stateManager.infectUser(user);
                EpidemicStatistics.getInstance().recordInfection(user.userID, "Aerosol transmission (final)");
            }
        }
    }

    /**
     * Gets the time interval per iteration in hours.
     * Retrieves from Configuration or returns a default value.
     * 
     * @return time interval per iteration in hours
     */
    public double getDeltaTimeHours() {
        if (es.unizar.gui.Configuration.simulation == null) {
            return 1.0 / 3600.0;
        }
        double secondsPerIteration = es.unizar.gui.Configuration.simulation.getTimeForIterationInSecond();
        return secondsPerIteration / 3600.0;
    }
    
    /**
     * Configures mask wearing based on compliance rate.
     * Randomly assigns mask-wearing status to users according to the
     * configured mask compliance rate.
     * 
     * @param users list of users to configure mask wearing for
     */
    private void configureMaskWearing(List<User> users) {
        double maskCompliance = config.getMaskComplianceRate();
        for (User user : users) {
            boolean wearsMask = Math.random() < maskCompliance;
            user.getEpidemicExtension().setMaskWearing(wearsMask);
        }
    }
    
    /**
     * Updates user visual appearance based on health status.
     * Changes cell styles and updates the user info display to reflect
     * current health status of all users.
     * 
     * @param users list of users to update visual status for
     */
    private void updateUserVisualStatus(List<User> users) {
        List<Object> cellsToUpdate = new ArrayList<>();
        
        for (User user : users) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null) {
                if (updateUserCellStyle(user, extension.getHealthStatus())) {
                    cellsToUpdate.add(user.userCell);
                }

                // Added by Nacho Palacio 2025-07-21
                if (MainSimulator.userInfo != null) {
                    String statusDescription = extension.getHealthStatus().getDescription();
                    MainSimulator.userInfo.updateHealthStatus(user.userID, statusDescription);
                    // MainSimulator.userInfo.updateHealthStatus(user.userID, statusDescription, isInfected);
                }
            }
        }
        
        if (!cellsToUpdate.isEmpty()) {
            updateGraphDisplay(cellsToUpdate);
        }
    }
    
    /**
     * Updates user cell style based on health status.
     * Changes the visual representation (image and border color) according to
     * the user's current health status.
     * 
     * @param user the user to update
     * @param status the health status to apply
     * @return true if the style was changed, false if it remained the same
     */
    private boolean updateUserCellStyle(User user, HealthStatus status) {
        String imagePath;
        
        if (status == HealthStatus.SUSCEPTIBLE) {
            imagePath = "/resources/images/special_user.png";
        } else {
            imagePath = "/resources/images/non_special_user.png";
        }
        
        String style = "shape=image;image=" + imagePath;
        
        if (status != HealthStatus.SUSCEPTIBLE) {
            String borderColor;
            switch (status) {
                case INFECTIOUS_SYMPTOMATIC:
                    borderColor = "#FF0000"; // Red
                    break;
                case SUPER_SPREADER:
                    borderColor = "#FF0000"; // HARD Red
                    break;
                default:
                    borderColor = "#FF0000"; // Red default
            }
            style += ";strokeColor=" + borderColor + ";strokeWidth=30";
        }
        
        if (!style.equals(user.userCell.getStyle())) {
            user.userCell.setStyle(style);
            return true;
        }
        return false;
    }

    /**
     * Updates the graph display to reflect cell style changes.
     * Refreshes the graph component to show the updated visual styles.
     * 
     * @param cellsToUpdate list of cells that have been modified
     */
    private void updateGraphDisplay(List<Object> cellsToUpdate) {
        try {
            if (es.unizar.gui.MainSimulator.getDrawFloorGraph() != null) {
                es.unizar.gui.MainSimulator.getDrawFloorGraph().getRoomGraphComponent().getGraph().refresh();
            }
        } catch (Exception e) {
            System.out.println(" Error updating graph display: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    // Getters
    
    /**
     * Gets the epidemic state manager.
     * 
     * @return the epidemic state manager instance
     */
    public EpidemicStateManager getStateManager() { return stateManager; }
    
    /**
     * Gets the contact tracker.
     * 
     * @return the contact tracker instance
     */
    public ContactTracker getContactTracker() { return contactTracker; }
    
    /**
     * Gets the epidemic model.
     * 
     * @return the epidemic model instance
     */
    public EpidemicModel getEpidemicModel() { return epidemicModel; }
    
    /**
     * Gets the epidemic configuration.
     * 
     * @return the epidemic configuration instance
     */
    public EpidemicConfiguration getConfig() { return config; }
}