package es.unizar.epidemic;

import es.unizar.gui.MainSimulator;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.models.EpidemicModel;
import es.unizar.epidemic.models.SimpleProximityModel;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.epidemic.models.PengTransmissionModel;
import es.unizar.epidemic.models.LelieveldTransmissionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Central manager for epidemic simulation
 * Coordinates all epidemic components
 * Añadido por Nacho Palacio 2025-07-15
 */
public class EpidemicSimulationManager {
    
    private EpidemicStateManager stateManager;
    private ContactTracker contactTracker;
    private EpidemicModel epidemicModel;
    private EpidemicConfiguration config;
    
    public EpidemicSimulationManager() {
        this.config = EpidemicConfiguration.getInstance();
        this.stateManager = new EpidemicStateManager();
        this.contactTracker = new ContactTracker();

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
     * Initializes epidemic system for all users
     */
    public void initializeEpidemicSystem(List<User> users) {
        EpidemicConfiguration config = EpidemicConfiguration.getInstance();
        double immuneFraction = config.getImmunePopulationFraction();
        double superSpreaderProb = config.getSuperSpreaderProbability();
          
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

        int targetSuperSpreaders = (int) Math.round(users.size() * superSpreaderProb);
        
        int actualSuperSpreaders = 0;
        
        for (User user : users) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null && !extension.isImmune()) {
                if (actualSuperSpreaders < targetSuperSpreaders && Math.random() < superSpreaderProb) {
                    extension.setSuperSpreader(true);
                    actualSuperSpreaders++;
                }
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
                        case INFECTIOUS_ASYMPTOMATIC:
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
        
        System.out.printf(" Estado inicial usuarios:\n");
        System.out.printf("   - Inmunes: %d (%.1f%%)\n", immuneCount, (double)immuneCount/users.size()*100);
        System.out.printf("   - Susceptibles: %d (%.1f%%)\n", susceptibleCount, (double)susceptibleCount/users.size()*100);
        System.out.printf("   - Infectados: %d (%.1f%%)\n", infectedCount, (double)infectedCount/users.size()*100);
    }
    
    /**
     * Updates epidemic state each simulation step
     */
    public void updateEpidemicState(List<User> users, int currentIteration) {
        contactTracker.trackContacts(users, currentIteration);
        
        // Añadido por Nacho Palacio 2025-09-23
        if (epidemicModel instanceof PengTransmissionModel) {
            ((PengTransmissionModel) epidemicModel).updateRoomExposure(users, getDeltaTimeHours());
        } else if (epidemicModel instanceof LelieveldTransmissionModel) {
            ((LelieveldTransmissionModel) epidemicModel).updateRoomExposure(users, getDeltaTimeHours());
        }

        // Modificado por Nacho Palacio 2025-09-23
        if (epidemicModel instanceof SimpleProximityModel) {
            if (currentIteration % 10 == 0) {
                contactTracker.evaluateTransmissions(users);
            }
        }
        
        stateManager.updateHealthStates(users, currentIteration);

        if (epidemicModel != null) {
            epidemicModel.updateHealthStates(users, currentIteration);
        }

        updateUserVisualStatus(users);

    }

    /**
     * Evaluates final aerosol transmissions at the end of the simulation step
     */
    public void evaluateFinalAerosolTransmissions(List<User> users) {
        boolean isPeng = epidemicModel instanceof es.unizar.epidemic.models.PengTransmissionModel;
        boolean isLelieveld = epidemicModel instanceof es.unizar.epidemic.models.LelieveldTransmissionModel;

        if (!isPeng && !isLelieveld) {
            return;
        }

        int nuevosInfectados = 0;
        for (User user : users) {
            UserEpidemicExtension ext = user.getEpidemicExtension();
            if (ext == null || ext.isImmune() || ext.getHealthStatus() != HealthStatus.SUSCEPTIBLE)
                continue;

            double combinedRisk = 0.0;
            if (isPeng) {
                combinedRisk = ((es.unizar.epidemic.models.PengTransmissionModel)epidemicModel)
                    .calculateCombinedInfectionRiskForUser(user);
            } else if (isLelieveld) {
                combinedRisk = ((es.unizar.epidemic.models.LelieveldTransmissionModel)epidemicModel)
                    .calculateCombinedInfectionRiskForUser(user);
            }

            double randomValue = Math.random();
           
            if (randomValue < combinedRisk) {
                stateManager.infectUser(user);
                EpidemicStatistics.getInstance().recordInfection(user.userID, "Aerosol transmission (final)");
                nuevosInfectados++;
            }
        }
    }

    public double getDeltaTimeHours() {
        if (es.unizar.gui.Configuration.simulation == null) {
            return 1.0 / 3600.0;
        }
        double secondsPerIteration = es.unizar.gui.Configuration.simulation.getTimeForIterationInSecond();
        return secondsPerIteration / 3600.0;
    }
    
    /**
     * Configures mask wearing based on compliance rate
     */
    private void configureMaskWearing(List<User> users) {
        double maskCompliance = config.getMaskComplianceRate();
        for (User user : users) {
            boolean wearsMask = Math.random() < maskCompliance;
            user.getEpidemicExtension().setMaskWearing(wearsMask);
        }
    }
    
    /**
     * Updates user visual appearance based on health status
     */
    private void updateUserVisualStatus(List<User> users) {
        List<Object> cellsToUpdate = new ArrayList<>();
        
        for (User user : users) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null) {
                if (updateUserCellStyle(user, extension.getHealthStatus())) {
                    cellsToUpdate.add(user.userCell);
                }

                // Añadido por Nacho Palacio 2025-07-21
                if (MainSimulator.userInfo != null) {
                    String statusDescription = extension.getHealthStatus().getDescription();
                    MainSimulator.userInfo.updateHealthStatus(user.userID, statusDescription);
                }
            }
        }
        
        if (!cellsToUpdate.isEmpty()) {
            updateGraphDisplay(cellsToUpdate);
        }
    }
    
    /**
     * Updates user cell style based on health status
     */
    private boolean updateUserCellStyle(User user, HealthStatus status) {
        String imagePath;
        
        if (status == HealthStatus.SUSCEPTIBLE) {
            imagePath = "/resources/images/special_user.png";
        } else {
            imagePath = "/resources/images/non_special_user.png";
        }
        
        String style = "shape=image;image=" + imagePath;
        
        // REVISAR: No se añade el borde
        if (status != HealthStatus.SUSCEPTIBLE) {
            String borderColor;
            switch (status) {
                case EXPOSED:
                    borderColor = "#FFFF00"; // Yellow
                    break;
                case INFECTIOUS_ASYMPTOMATIC:
                    borderColor = "#FFA500"; // Orange
                    break;
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
     * Updates the graph display to reflect cell style changes
     */
    private void updateGraphDisplay(List<Object> cellsToUpdate) {
        try {
            if (es.unizar.gui.MainSimulator.getDrawFloorGraph() != null) {
                es.unizar.gui.MainSimulator.getDrawFloorGraph().getRoomGraphComponent().getGraph().refresh();
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating graph display: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    // Getters
    public EpidemicStateManager getStateManager() { return stateManager; }
    public ContactTracker getContactTracker() { return contactTracker; }
    public EpidemicModel getEpidemicModel() { return epidemicModel; }
    public EpidemicConfiguration getConfig() { return config; }
}