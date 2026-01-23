package es.unizar.epidemic.statistics;

import java.util.List;
import java.util.Map;

import es.unizar.epidemic.models.PengParameters;
import es.unizar.epidemic.models.PengTransmissionModel;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.general.EpidemicConfiguration;
import es.unizar.epidemic.models.LelieveldParameters;
import es.unizar.epidemic.models.LelieveldTransmissionModel;

/**
 * Utility class for calculating individual risk across different epidemic models
 * 
 * @author Nacho Palacio
 */
public class EpidemicRiskCalculator {
    
    /**
     * Calculates individual risk for the specified epidemic model.
     * Delegates to model-specific calculation methods based on the selected
     * transmission model (Simple Proximity, Peng, or Lelieveld).
     * 
     * @param model the name of the epidemic model to use
     * @param config the epidemic configuration containing model parameters
     * @return individual infection risk as a percentage (0.0 to 100.0)
     */
    public static double calculateIndividualRisk(String model, EpidemicConfiguration config) {
        if (model == null || config == null) {
            return 0.0;
        }
        
        try {
            switch (model) {
                case "SIMPLE_PROXIMITY":
                    return calculateSimpleProximityRisk(config);
                    
                case "AEROSOL_PENG":
                    return calculatePengRisk(config);
                    
                case "AEROSOL_LELIEVELD":
                    return calculateLelieveldRisk(config);
                    
                default:
                    System.err.println("Warning! Unknown model for risk calculation: " + model);
                    return 0.0;
            }
        } catch (Exception e) {
            System.err.println("Warning! Error calculating individual risk for " + model + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculates individual risk for Simple Proximity model.
     * Uses base transmission probability adjusted for mask compliance
     * and average mask efficiency.
     * 
     * @param config the epidemic configuration containing model parameters
     * @return individual infection risk as a percentage (0.0 to 100.0)
     */
    public static double calculateSimpleProximityRisk(EpidemicConfiguration config) {
        double baseProb = config.getBaseTransmissionProbability();
        double maskCompliance = config.getMaskComplianceRate();
        
        double maskEfficiencyExh = config.getMaskExhalationEfficiency();
        double maskEfficiencyInh = config.getMaskInhalationEfficiency();
        
        double avgMaskReduction = (maskEfficiencyExh + maskEfficiencyInh) / 2.0;
        double maskFactor = 1.0 - (maskCompliance * avgMaskReduction);
        
        double risk = baseProb * maskFactor * 100.0;
        
        return Math.min(100.0, risk);
    }
    
    /**
     * Calculates individual risk for Peng model.
     * Uses quanta-based Wells-Riley model with ventilation, viral decay,
     * mask parameters, and exposure time to calculate infection probability.
     * 
     * @param config the epidemic configuration containing model parameters
     * @return individual infection risk as a percentage (0.0 to 100.0)
     */
    public static double calculatePengRisk(EpidemicConfiguration config) {
        PengParameters params = new PengParameters();

        double ventilationRate = config.getDefaultVentilationRate();
        double virusDecayRate = config.getVirusDecayRate();
        double maskCompliance = config.getMaskComplianceRate();
        double exhalationEff = config.getMaskExhalationEfficiency();
        double inhalationEff = config.getMaskInhalationEfficiency();
        double fractionImmune = config.getImmunePopulationFraction();
        double quantaEmissionRate = config.getQuantaEmissionRate();
        double breathingRate = config.getBreathingRate();
        double depositionRate = config.getDepositionRate();
        int finalInfectedUsers = config.getFinalInfectedUsers();
        int totalUsers = config.getTotalUsers();

        double roomLength = 10.0;
        double roomWidth = 6.0;
        double roomHeight = 3.0;
        params.setRoomDimensions(roomLength, roomWidth, roomHeight);

        params.setVentilationRate(ventilationRate);
        params.setVirusDecayRate(virusDecayRate);
        params.setMaskParameters(exhalationEff, inhalationEff, maskCompliance);
        params.setFractionImmune(fractionImmune);
        params.setBasicQuantaExhalationRate(quantaEmissionRate);
        params.setBreathingRateSusceptibles(breathingRate);
        params.setDepositionRate(depositionRate);

        params.setPeopleCount(totalUsers, finalInfectedUsers);

        double exposureTimeHours = config.getSimulationDurationSeconds() / 3600.0;

        double maskProtectionFactor = 1.0 - (params.getInhalationMaskEfficiency() * maskCompliance);
        double risk = params.calculateInfectionProbability(exposureTimeHours, finalInfectedUsers, maskProtectionFactor) * 100.0;

        return Math.min(100.0, risk);
    }
    
    /**
     * Calculates individual risk for Lelieveld model.
     * Uses aerosol transmission model based on Lelieveld et al. with viral load,
     * ventilation, deposition probability, and infectious dose parameters.
     * 
     * @param config the epidemic configuration containing model parameters
     * @return individual infection risk as a percentage (0.0 to 100.0)
     */
    public static double calculateLelieveldRisk(EpidemicConfiguration config) {
        LelieveldParameters params = new LelieveldParameters();

        double ventilationRate = config.getDefaultVentilationRate();
        double maskCompliance = config.getMaskComplianceRate();
        double exhalationEff = config.getMaskExhalationEfficiency();
        double inhalationEff = config.getMaskInhalationEfficiency();
        double fractionImmune = config.getImmunePopulationFraction();
        double viralLoadHigh = config.getViralLoadHigh();
        double depositionProbability = config.getDepositionProbability();
        double infectiousDose = config.getInfectiousDose();
        int finalInfectedUsers = config.getFinalInfectedUsers();
        int totalUsers = config.getTotalUsers();

        double roomLength = 10.0;
        double roomWidth = 6.0;
        double roomHeight = 3.0;
        params.setRoomDimensions(roomLength, roomWidth, roomHeight);

        params.setVentilationRates(ventilationRate, 0.0, false);
        params.setMaskParameters(inhalationEff, exhalationEff, maskCompliance);
        params.setFractionImmune(fractionImmune);
        params.setPeopleCount(totalUsers, finalInfectedUsers);

        params.setDepositionProbability(depositionProbability);
        params.setInfectiveDoseD50(infectiousDose);

        double exposureTimeHours = config.getSimulationDurationSeconds() / 3600.0;

        double maskProtectionFactor = 1.0 - (params.getMaskEfficiencyInh() * maskCompliance);
        double risk = params.calculateInfectionProbability(exposureTimeHours, viralLoadHigh, maskProtectionFactor, finalInfectedUsers) * 100.0;

        return Math.min(100.0, risk);
    }
    
    /**
     * Calculates individual risk for the current active model.
     * Retrieves the current epidemic configuration and selected model,
     * then delegates to the appropriate calculation method.
     * 
     * @return individual infection risk as a percentage (0.0 to 100.0)
     */
    public static double calculateCurrentModelRisk() {
        try {
            EpidemicConfiguration config = EpidemicConfiguration.getInstance();
            String model = config.getSelectedModel();
            
            return calculateIndividualRisk(model, config);
            
        } catch (Exception e) {
            System.err.println("Warning! Error obtaining current configuration: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calculates combined individual risk for Peng model.
     * Combines infection probabilities across multiple rooms using the
     * complement probability method to account for exposure in different locations.
     * 
     * @param roomIds list of room IDs to consider
     * @param usersInRoom map of room IDs to lists of users in each room
     * @param exposureTimeHoursPerRoom exposure time in hours per room
     * @param pengModel the Peng transmission model instance
     * @return combined infection probability (0.0 to 1.0)
     */
    public static double calculateCombinedIndividualRiskPeng(
            List<Integer> roomIds,
            Map<Integer, List<User>> usersInRoom,
            double exposureTimeHoursPerRoom,
            PengTransmissionModel pengModel
    ) {
        double pNoInfect = 1.0;
        for (int roomId : roomIds) {
            List<User> users = usersInRoom.get(roomId);
            if (users == null || users.isEmpty()) continue;

            pengModel.configureModelForRoom(roomId);

            int totalPeople = users.size();
            int infectivePeople = 0;
            for (User u : users) {
                if (pengModel.isInfectious(u.getEpidemicExtension())) infectivePeople++;
            }
            if (infectivePeople == 0) continue;

            PengParameters params = pengModel.getParameters();
            params.setPeopleCount(totalPeople, infectivePeople);

            double maskProtectionFactor = 1.0 - (params.getInhalationMaskEfficiency() * params.getFractionPeopleWithMasks());
            double pRoom = params.calculateInfectionProbability(exposureTimeHoursPerRoom, infectivePeople, maskProtectionFactor);

            pNoInfect *= (1.0 - pRoom);
        }
        return 1.0 - pNoInfect;
    }

    /**
     * Calculates combined individual risk for Lelieveld model.
     * Combines infection probabilities across multiple rooms using the
     * complement probability method to account for exposure in different locations.
     * 
     * @param roomIds list of room IDs to consider
     * @param usersInRoom map of room IDs to lists of users in each room
     * @param exposureTimeHoursPerRoom exposure time in hours per room
     * @param lelieveldModel the Lelieveld transmission model instance
     * @return combined infection probability (0.0 to 1.0)
     */
    public static double calculateCombinedIndividualRiskLelieveld(
            List<Integer> roomIds,
            Map<Integer, List<User>> usersInRoom,
            double exposureTimeHoursPerRoom,
            LelieveldTransmissionModel lelieveldModel
    ) {
        double pNoInfect = 1.0;
        for (int roomId : roomIds) {
            List<User> users = usersInRoom.get(roomId);
            if (users == null || users.isEmpty()) continue;

            lelieveldModel.configureModelForRoom(roomId);

            int totalPeople = users.size();
            int infectivePeople = 0;
            for (User u : users) {
                if (lelieveldModel.isInfectious(u.getEpidemicExtension())) infectivePeople++;
            }
            if (infectivePeople == 0) continue;

            LelieveldParameters params = lelieveldModel.getLelieveldParameters();
            params.setPeopleCount(totalPeople, infectivePeople);

            double maskProtectionFactor = 1.0 - (params.getMaskEfficiencyInh() * params.getFractionPeopleWithMasks());
            double pRoom = params.calculateInfectionProbability(exposureTimeHoursPerRoom, params.getViralLoadHighCm3(), maskProtectionFactor, infectivePeople);

            pNoInfect *= (1.0 - pRoom);
        }
        return 1.0 - pNoInfect;
    }

    /**
     * Calculates average Peng risk for all rooms.
     * Computes infection risk for each room individually and returns
     * the arithmetic mean across all rooms with infectious people.
     * 
     * @param roomIds list of room IDs to consider
     * @param usersInRoom map of room IDs to lists of users in each room
     * @param pengModel the Peng transmission model instance
     * @param exposureTimeHours exposure time in hours
     * @return average infection probability across all rooms (0.0 to 1.0)
     */
    public static double calculateAveragePengRiskForAllRooms(
            List<Integer> roomIds,
            Map<Integer, List<User>> usersInRoom,
            PengTransmissionModel pengModel,
            double exposureTimeHours
    ) {
        double totalRisk = 0.0;
        int roomCount = 0;

        for (int roomId : roomIds) {
            List<User> users = usersInRoom.get(roomId);
            if (users == null || users.isEmpty()) continue;

            pengModel.configureModelForRoom(roomId);

            int totalPeople = users.size();
            int infectivePeople = 0;
            for (User u : users) {
                if (pengModel.isInfectious(u.getEpidemicExtension())) infectivePeople++;
            }

            if (infectivePeople == 0) continue;

            PengParameters params = pengModel.getParameters();
            params.setPeopleCount(totalPeople, infectivePeople);

            double maskProtectionFactor = 1.0 - (params.getInhalationMaskEfficiency() * params.getFractionPeopleWithMasks());

            double risk = params.calculateInfectionProbability(exposureTimeHours, infectivePeople, maskProtectionFactor);

            totalRisk += risk;
            roomCount++;
        }
        return roomCount > 0 ? totalRisk / roomCount : 0.0;
    }

    /**
     * Calculates average Lelieveld risk for all rooms.
     * Computes infection risk for each room individually and returns
     * the arithmetic mean across all rooms with infectious people.
     * 
     * @param roomIds list of room IDs to consider
     * @param usersInRoom map of room IDs to lists of users in each room
     * @param lelieveldModel the Lelieveld transmission model instance
     * @param exposureTimeHours exposure time in hours
     * @return average infection probability across all rooms (0.0 to 1.0)
     */
    public static double calculateAverageLelieveldRiskForAllRooms(
            List<Integer> roomIds,
            Map<Integer, List<User>> usersInRoom,
            LelieveldTransmissionModel lelieveldModel,
            double exposureTimeHours
    ) {
        double totalRisk = 0.0;
        int roomCount = 0;

        for (int roomId : roomIds) {
            List<User> users = usersInRoom.get(roomId);
            if (users == null || users.isEmpty()) continue;

            lelieveldModel.configureModelForRoom(roomId);

            int totalPeople = users.size();
            int infectivePeople = 0;
            for (User u : users) {
                if (lelieveldModel.isInfectious(u.getEpidemicExtension())) infectivePeople++;
            }

            if (infectivePeople == 0) continue;

            LelieveldParameters params = lelieveldModel.getLelieveldParameters();
            params.setPeopleCount(totalPeople, infectivePeople);

            double maskProtectionFactor = 1.0 - (params.getMaskEfficiencyInh() * params.getFractionPeopleWithMasks());

            double risk = params.calculateInfectionProbability(exposureTimeHours, params.getViralLoadHighCm3(), maskProtectionFactor, infectivePeople);

            totalRisk += risk;
            roomCount++;
        }
        return roomCount > 0 ? totalRisk / roomCount : 0.0;
    }
    
}