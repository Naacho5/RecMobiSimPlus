package es.unizar.epidemic;

import es.unizar.gui.MainSimulator;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.models.EpidemicModel;
import es.unizar.epidemic.models.SimpleProximityModel;
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
        
        if ("SIMPLE_PROXIMITY".equals(config.getSelectedModel())) {
            SimpleProximityModel simpleModel = new SimpleProximityModel();
            this.epidemicModel = simpleModel;
            System.out.println("Using SimpleProximityModel with permissive parameters");
        } else if ("AEROSOL_LELIEVELD_2020".equals(config.getSelectedModel())) {
            this.epidemicModel = new LelieveldTransmissionModel();
            System.out.println("Using LelieveldTransmissionModel");
        } else {
            this.epidemicModel = new PengTransmissionModel();
            System.out.println("Using AerosolTransmissionModel");
        }
        
        this.contactTracker.setEpidemicModel(this.epidemicModel);
        this.contactTracker.setEpidemicStateManager(this.stateManager);
    }
    
    /**
     * Initializes epidemic system for all users
     */
    public void initializeEpidemicSystem(List<User> users) {
        // Configure mask wearing based on compliance rate
        configureMaskWearing(users);
        
        // Infect initial users
        stateManager.infectInitialUsers(users, config.getInitialInfectedUsers());

        if (epidemicModel instanceof PengTransmissionModel) {
            ((PengTransmissionModel) epidemicModel).initializeExposureTracking(users);
        }
        
        System.out.println("Epidemic system initialized for " + users.size() + " users");
    }
    
    /**
     * Updates epidemic state each simulation step
     */
    public void updateEpidemicState(List<User> users, int currentIteration) {
        contactTracker.trackContacts(users, currentIteration);
        
        contactTracker.evaluateTransmissions(users);
        
        stateManager.updateHealthStates(users, currentIteration);

        if (epidemicModel != null) {
            epidemicModel.updateHealthStates(users, currentIteration);
        }
        
        // Update user visual appearance based on health status
        updateUserVisualStatus(users);

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
    
    
    // Getters para acceso a componentes
    public EpidemicStateManager getStateManager() { return stateManager; }
    public ContactTracker getContactTracker() { return contactTracker; }
    public EpidemicModel getEpidemicModel() { return epidemicModel; }
    public EpidemicConfiguration getConfig() { return config; }
}