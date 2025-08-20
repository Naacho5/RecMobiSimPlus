package es.unizar.epidemic;

import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.simulation.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class ContactTracker {
    
    private Map<String, ContactRecord> activeContacts;
    private List<ContactRecord> allContacts;

    private double proximityThresholdPixels = 10.0; 
    private int minContactDurationSeconds = 15;

    private es.unizar.epidemic.models.EpidemicModel epidemicModel;
    private EpidemicStateManager stateManager;
    private List<User> currentUserList;

    public ContactTracker() {
        this.activeContacts = new ConcurrentHashMap<>();
        this.allContacts = new ArrayList<>();

        // Añadido por Nacho Palacio 2025-07-17
        this.minContactDurationSeconds = 3;
    }
    
    public void trackContacts(List<User> userList, int currentIteration) {
        this.currentUserList = userList; // Actual list

        LocalDateTime currentTime = LocalDateTime.now().plusSeconds(currentIteration);
        
        // Verificar todos los pares de usuarios
        for (int i = 0; i < userList.size(); i++) {
            for (int j = i + 1; j < userList.size(); j++) {
                User user1 = userList.get(i);
                User user2 = userList.get(j);
                
                double distance = calculateDistance(user1, user2);
                String contactKey = generateContactKey(user1.userID, user2.userID);
                
                if (distance <= proximityThresholdPixels) {
                    // Los usuarios están en contacto
                    handleActiveContact(user1, user2, distance, currentTime, contactKey);
                } else {
                    // Los usuarios ya no están en contacto
                    handleContactEnd(contactKey, currentTime);
                }
            }
        }
    }

    /**
     * Handles when users are in proximity
     */
    private void handleActiveContact(User user1, User user2, double distance, 
                                   LocalDateTime currentTime, String contactKey) {
        if (activeContacts.containsKey(contactKey)) {
            // Contacto existente - actualizar duración
            ContactRecord contact = activeContacts.get(contactKey);
            contact.setDuration(contact.getDuration() + 1); // +1 segundo por iteración
        } else {
            ContactType contactType = determineContactType(user1, user2);
            int roomId = getCurrentRoomId(user1); // Asumiendo mismo room
            
            ContactRecord newContact = new ContactRecord(
                user1.userID, 
                user2.userID, 
                currentTime, 
                distance, 
                1,
                roomId, 
                contactType
            );
            
            activeContacts.put(contactKey, newContact);
        }
    }
    
    /**
     * Handles when contact ends
     */
    private void handleContactEnd(String contactKey, LocalDateTime currentTime) {
        if (activeContacts.containsKey(contactKey)) {
            ContactRecord contact = activeContacts.remove(contactKey);
            
            // Solo guardar si supera el tiempo mínimo
            if (contact.getDuration() >= minContactDurationSeconds) {
                allContacts.add(contact);
            }
        }
    }
    
    public void evaluateTransmissions(List<User> userList) {
        for (ContactRecord contact : activeContacts.values()) {
            User user1 = findUserById(userList, contact.getUser1Id());
            User user2 = findUserById(userList, contact.getUser2Id());
            
            if (user1 != null && user2 != null) {
                evaluatePotentialTransmission(user1, user2, contact);
            }
        }
    }

    /**
     * Evaluates if transmission occurs between two users
     */
    private void evaluatePotentialTransmission(User user1, User user2, ContactRecord contact) {
        UserEpidemicExtension ext1 = getUserEpidemicExtension(user1);
        UserEpidemicExtension ext2 = getUserEpidemicExtension(user2);
        
        if (ext1 == null || ext2 == null) return;
        
        boolean user1Infectious = isInfectious(ext1);
        boolean user2Infectious = isInfectious(ext2);
        boolean user1Susceptible = isSusceptible(ext1);
        boolean user2Susceptible = isSusceptible(ext2);
        
        if (user1Infectious && user2Susceptible) {
            attemptTransmission(ext1, ext2, contact);
        } else if (user2Infectious && user1Susceptible) {
            attemptTransmission(ext2, ext1, contact);
        }
    }

    /**
     * Attempts transmission from infectious to susceptible user
     */
    private void attemptTransmission(UserEpidemicExtension infectious, 
                               UserEpidemicExtension susceptible, 
                               ContactRecord contact) {
        if (epidemicModel == null) {
            if (Math.random() < 0.1) { // 10%
                infectUserWithStateManager(susceptible);
            }
            return;
        }
        
        User infectiousUser = findUserByExtension(infectious);
        User susceptibleUser = findUserByExtension(susceptible);
        
        if (infectiousUser == null || susceptibleUser == null) {
            System.out.println("⚠️ Could not find users for transmission calculation");
            return;
        }
        
        double transmissionProb = epidemicModel.calculateTransmissionProbability(
            infectiousUser, susceptibleUser, contact);
            
        double randomValue = Math.random();
        if (randomValue < transmissionProb) {

            // Statistics
            infectUserWithStateManager(susceptible);
            User susceptibleUser1 = findUserByExtension(susceptible);
            if (susceptibleUser1 != null) {
                EpidemicStatistics.getInstance().recordInfection(susceptibleUser1.userID, "Contact transmission");
            }
        }
    }

    /**
     * Infects a user using the EpidemicStateManager
     */
    private void infectUserWithStateManager(UserEpidemicExtension extension) {
        if (stateManager == null) {
            System.out.println("⚠️ No EpidemicStateManager configured");
            return;
        }

        User user = findUserByExtension(extension);
        if (user != null) {
            stateManager.infectUser(user);
        } else {
            System.out.println("⚠️ Could not find user for infection");
        }
    }

    /**
     * Calculates transmission probability based on contact characteristics
     */
    private double calculateTransmissionProbability(UserEpidemicExtension infectious, 
                                                  UserEpidemicExtension susceptible, 
                                                  ContactRecord contact) {
        double baseProb = contact.getContactType().getBaseProbability();
        
        // Más probable si es sintomático
        if (infectious.getHealthStatus() == HealthStatus.INFECTIOUS_SYMPTOMATIC) {
            baseProb *= 1.5;
        } else if (infectious.getHealthStatus() == HealthStatus.INFECTIOUS_ASYMPTOMATIC) {
            baseProb *= 0.8;
        }
        
        double durationFactor = Math.min(2.0, contact.getDuration() / 60.0);
        baseProb *= durationFactor;
        
        double distanceFactor = calculateDistanceFactor(contact.getDistance());
        baseProb *= distanceFactor;
        
        // Less probability if wearing masks
        if (infectious.isMaskWearing() && susceptible.isMaskWearing()) {
            baseProb *= 0.25;
        } else if (infectious.isMaskWearing() || susceptible.isMaskWearing()) {
            baseProb *= 0.5;
        }
        
        return Math.min(1.0, baseProb);
    }
    
    /**
     * Returns contacts for a specific user
     */
    public List<ContactRecord> getContactsForUser(long userId) {
        List<ContactRecord> userContacts = new ArrayList<>();
        
        for (ContactRecord contact : allContacts) {
            if (contact.getUser1Id() == userId || contact.getUser2Id() == userId) {
                userContacts.add(contact);
            }
        }
        
        for (ContactRecord contact : activeContacts.values()) {
            if (contact.getUser1Id() == userId || contact.getUser2Id() == userId) {
                userContacts.add(contact);
            }
        }
        
        return userContacts;
    }

    // MÉTODOS AUXILIARES
    private double calculateDistance(User user1, User user2) {
        double dx = user1.x - user2.x;
        double dy = user1.y - user2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private String generateContactKey(long userId1, long userId2) {
        return Math.min(userId1, userId2) + "_" + Math.max(userId1, userId2);
    }
    
    private ContactType determineContactType(User user1, User user2) {
        return ContactType.COMMUNITY;
    }
    
    private int getCurrentRoomId(User user) {
        return user.room;
    }
    
    private double calculateDistanceFactor(double distancePixels) {
        double pixelsPerMeter = es.unizar.gui.Configuration.getPixelsPerMeter();
        double distanceMeters = distancePixels / pixelsPerMeter;
        
        if (distanceMeters <= 1.0) {
            return 2.0;
        } else if (distanceMeters <= 2.0) {
            return 1.5;
        } else if (distanceMeters <= 4.0) {
            return 1.0;
        } else {
            return 0.5;
        }
    }

    private boolean isInfectious(UserEpidemicExtension extension) {
        return extension.getHealthStatus() == HealthStatus.INFECTIOUS_ASYMPTOMATIC ||
               extension.getHealthStatus() == HealthStatus.INFECTIOUS_SYMPTOMATIC;
    }
    
    private boolean isSusceptible(UserEpidemicExtension extension) {
        return extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
    }
    
    private User findUserById(List<User> userList, long userId) {
        return userList.stream()
                      .filter(user -> user.userID == userId)
                      .findFirst()
                      .orElse(null);
    }

    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }

    private User findUserByExtension(UserEpidemicExtension extension) {
        if (currentUserList != null) {
            for (User user : currentUserList) {
                if (user.getEpidemicExtension() == extension) {
                    return user;
                }
            }
        }
        return null;
    }
    
    
    // GETTERS
    public List<ContactRecord> getAllContacts() {
        return new ArrayList<>(allContacts);
    }
    
    public Map<String, ContactRecord> getActiveContacts() {
        return new HashMap<>(activeContacts);
    }

    public double getProximityThreshold() {
        return proximityThresholdPixels;
    }

    public int getMinContactDuration() {
        return minContactDurationSeconds;
    }


    // SETTERS
    public void setProximityThreshold(double proximityThresholdPixels) {
        this.proximityThresholdPixels = proximityThresholdPixels;
    }
    
    public void setMinContactDuration(int minContactDurationSeconds) {
        this.minContactDurationSeconds = minContactDurationSeconds;
    }

    public void setEpidemicModel(es.unizar.epidemic.models.EpidemicModel epidemicModel) {
        this.epidemicModel = epidemicModel;
    }

    public void setEpidemicStateManager(EpidemicStateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    
    /**
     * Clears all contact data
     */
    public void clearContacts() {
        activeContacts.clear();
        allContacts.clear();
    }

}