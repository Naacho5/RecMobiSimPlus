package es.unizar.epidemic.statistics;

import java.time.LocalDateTime;
import java.util.*;

public class EpidemicStatistics {

    private int totalUsers = 0;
    private int initialInfected = 0;
    private int totalInfections = 0;
    private int totalRecoveries = 0;
    private long simulationStartTime;
    private long simulationEndTime;

    private Map<String, Object> modelSpecificStats = new HashMap<>();
    
    // SimpleProximityModel
    private int totalContacts = 0;
    private int infectiousContacts = 0;
    private double averageContactDuration = 0.0;
    
    // Rooms
    private Map<Integer, RoomStatistics> roomStats = new HashMap<>();
    
    // States
    private List<HealthStateSnapshot> stateHistory = new ArrayList<>();
    
    private static EpidemicStatistics instance;
    
    public static EpidemicStatistics getInstance() {
        if (instance == null) {
            instance = new EpidemicStatistics();
        }
        return instance;
    }
    
    public void startSimulation(int totalUsers, int initialInfected) {
        this.totalUsers = totalUsers;
        this.initialInfected = initialInfected;
        this.simulationStartTime = System.currentTimeMillis();
        System.out.println("📊 Estadísticas iniciadas - Usuarios: " + totalUsers + ", Infectados iniciales: " + initialInfected);
    }
    
    public void endSimulation() {
        this.simulationEndTime = System.currentTimeMillis();
    }
    
    public void recordInfection(int userId, String transmissionSource) {
        totalInfections++;
        System.out.println("🦠 Nueva infección registrada - Usuario " + userId + " (Total: " + totalInfections + ")");
    }
    
    public void recordRecovery(int userId) {
        totalRecoveries++;
    }
    
    public void recordContact(int user1, int user2, double duration, int roomId) {
        totalContacts++;
        averageContactDuration = ((averageContactDuration * (totalContacts - 1)) + duration) / totalContacts;
        
        RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
        roomStat.addContact();
    }
    
    public void recordInfectiousContact(int infectiousUser, int susceptibleUser) {
        infectiousContacts++;
    }
    
    public void recordRoomAerosolConcentration(int roomId, double concentration) {
        RoomStatistics roomStat = roomStats.computeIfAbsent(roomId, k -> new RoomStatistics(roomId));
        roomStat.addAerosolMeasurement(concentration);
    }
    
    public void recordHealthStateSnapshot(int susceptible, int exposed, int infectious, int recovered) {
        stateHistory.add(new HealthStateSnapshot(susceptible, exposed, infectious, recovered));
    }
    
    public void setModelSpecificStat(String key, Object value) {
        modelSpecificStats.put(key, value);
    }
    
    public void printFinalStatistics() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 FINAL SIMULATION STATISTICS");
        System.out.println("=".repeat(60));
        
        // Time statistics
        long durationMs = simulationEndTime - simulationStartTime;
        double durationSeconds = durationMs / 1000.0;
        System.out.println("⏱️  SIMULATION TIME");
        System.out.println("   Duration: " + String.format("%.2f seconds", durationSeconds));
        System.out.println("   Start: " + new Date(simulationStartTime));
        System.out.println("   End: " + new Date(simulationEndTime));
        
        // Population statistics
        System.out.println("\n👥 POPULATION STATISTICS");
        System.out.println("   Total users: " + totalUsers);
        System.out.println("   Initial infected: " + initialInfected);
        System.out.println("   Total infections: " + totalInfections);
        System.out.println("   Total recoveries: " + totalRecoveries);
        System.out.println("   Attack rate: " + String.format("%.2f%%", (totalInfections * 100.0) / totalUsers));
        
        // Contact statistics
        if (totalContacts > 0) {
            System.out.println("\n🤝 CONTACT STATISTICS");
            System.out.println("   Total contacts: " + totalContacts);
            System.out.println("   Infectious contacts: " + infectiousContacts);
            System.out.println("   Average duration: " + String.format("%.2f seconds", averageContactDuration));
            System.out.println("   Transmission efficiency: " + String.format("%.2f%%", 
                (infectiousContacts > 0 ? (totalInfections * 100.0) / infectiousContacts : 0)));
        }
        
        // Room statistics
        if (!roomStats.isEmpty()) {
            System.out.println("\n🏠 ROOM STATISTICS");
            for (RoomStatistics roomStat : roomStats.values()) {
                roomStat.printStats();
            }
        }
        
        // Model-specific statistics
        if (!modelSpecificStats.isEmpty()) {
            System.out.println("\n🔬 MODEL-SPECIFIC STATISTICS");
            for (Map.Entry<String, Object> entry : modelSpecificStats.entrySet()) {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Health state evolution
        if (!stateHistory.isEmpty()) {
            System.out.println("\n📈 HEALTH STATE EVOLUTION");
            HealthStateSnapshot last = stateHistory.get(stateHistory.size() - 1);
            System.out.println("   Final state:");
            System.out.println("     Susceptible: " + last.susceptible);
            System.out.println("     Exposed: " + last.exposed);
            System.out.println("     Infectious: " + last.infectious);
            System.out.println("     Recovered: " + last.recovered);
        }
        
        System.out.println("\n" + "=".repeat(60));
    }
    
    
    public static class RoomStatistics {
        private int roomId;
        private int totalContacts = 0;
        private List<Double> aerosolConcentrations = new ArrayList<>();
        
        public RoomStatistics(int roomId) {
            this.roomId = roomId;
        }
        
        public void addContact() {
            totalContacts++;
        }
        
        public void addAerosolMeasurement(double concentration) {
            aerosolConcentrations.add(concentration);
        }
        
        public void printStats() {
            System.out.println("   Room " + roomId + ":");
            System.out.println("     Contacts: " + totalContacts);
            
            if (!aerosolConcentrations.isEmpty()) {
                double avgConcentration = aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double maxConcentration = aerosolConcentrations.stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);

                System.out.println("     Average aerosol concentration: " + String.format("%.2e", avgConcentration));
                System.out.println("     Maximum aerosol concentration: " + String.format("%.2e", maxConcentration));
            }
        }
    }
    
    public static class HealthStateSnapshot {
        public final int susceptible, exposed, infectious, recovered;
        public final LocalDateTime timestamp;
        
        public HealthStateSnapshot(int susceptible, int exposed, int infectious, int recovered) {
            this.susceptible = susceptible;
            this.exposed = exposed;
            this.infectious = infectious;
            this.recovered = recovered;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    // Getters
    public int getTotalInfections() { return totalInfections; }
    public int getTotalContacts() { return totalContacts; }
    public double getSimulationDurationSeconds() { 
        return (simulationEndTime - simulationStartTime) / 1000.0; 
    }
}