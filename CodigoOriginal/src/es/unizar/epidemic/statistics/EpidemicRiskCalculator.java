package es.unizar.epidemic.statistics;

import java.util.List;
import java.util.Map;

import es.unizar.epidemic.EpidemicConfiguration;
import es.unizar.epidemic.models.PengParameters;
import es.unizar.epidemic.models.PengTransmissionModel;
import es.unizar.gui.simulation.User;
import es.unizar.epidemic.models.LelieveldParameters;
import es.unizar.epidemic.models.LelieveldTransmissionModel;

/**
 * Utility class for calculating individual risk across different epidemic models
 * @author Nacho Palacio
 * @date 2025-09-18
 */
public class EpidemicRiskCalculator {
    
    /**
     * Calculates individual risk for the specified epidemic model
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
                    System.err.println("⚠️ Modelo desconocido para cálculo de riesgo: " + model);
                    return 0.0;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error calculando riesgo individual para " + model + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculates individual risk for Simple Proximity model
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
     * Calculates individual risk for Peng model
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
     * Calculates individual risk for Lelieveld model
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
        double risk = params.calculateInfectionProbability(exposureTimeHours, viralLoadHigh, maskProtectionFactor) * 100.0;

        return Math.min(100.0, risk);
    }
    
    /**
     * Calculates individual risk for the current active model
     */
    public static double calculateCurrentModelRisk() {
        try {
            EpidemicConfiguration config = EpidemicConfiguration.getInstance();
            String model = config.getSelectedModel();
            
            return calculateIndividualRisk(model, config);
            
        } catch (Exception e) {
            System.err.println("⚠️ Error obteniendo configuración actual: " + e.getMessage());
            return 0.0;
        }
    }

    /* Calculates combined individual risk for Peng model */
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

    /* 
     * Calculates combined individual risk for Lelieveld model 
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
            double pRoom = params.calculateInfectionProbability(exposureTimeHoursPerRoom, params.getViralLoadHighCm3(), maskProtectionFactor);

            pNoInfect *= (1.0 - pRoom);
        }
        return 1.0 - pNoInfect;
    }

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
            double risk = params.calculateInfectionProbability(exposureTimeHours, params.getViralLoadHighCm3(), maskProtectionFactor);

            totalRisk += risk;
            roomCount++;
        }
        return roomCount > 0 ? totalRisk / roomCount : 0.0;
    }
    
}