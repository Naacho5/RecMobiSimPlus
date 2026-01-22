package es.unizar.epidemic.contact;

import es.unizar.epidemic.general.EpidemicStateManager;
import es.unizar.epidemic.general.HealthStatus;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicStatistics;
import es.unizar.gui.simulation.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks contacts between users and evaluates transmissions
 * 
 * @author Nacho Palacio
 */
public class ContactTracker {
    
    private Map<String, ContactRecord> activeContacts;
    private List<ContactRecord> allContacts;

    private double proximityThresholdPixels = 10.0; 
    private int minContactDurationSeconds = 300;

    private es.unizar.epidemic.models.EpidemicModel epidemicModel;
    private EpidemicStateManager stateManager;
    private List<User> currentUserList;

    public ContactTracker(double maxTransmissionDistance, int minContactDurationSeconds) {
        this.activeContacts = new ConcurrentHashMap<>();
        this.allContacts = new ArrayList<>();

        // Added by Nacho Palacio 2025-07-17
        this.minContactDurationSeconds = minContactDurationSeconds;

        // Added by Nacho Palacio 2025-11-07
        double pixelsPerMeter = es.unizar.gui.Configuration.getPixelsPerMeter();
        this.proximityThresholdPixels = maxTransmissionDistance * pixelsPerMeter;

        System.out.printf("ContactTracker initialized: threshold = %.2fm (%.2f pixels)%n",
                maxTransmissionDistance, proximityThresholdPixels);
    }
    
    /**
     * Tracks contacts between users
     * 
     * @param userList List of users in the simulation
     * @param currentIteration Current simulation iteration
     */
    public void trackContacts(List<User> userList, int currentIteration) {
        this.currentUserList = userList;
        LocalDateTime currentTime = LocalDateTime.now().plusSeconds(currentIteration);

        List<ContactRecord> contactsEndingNow = new ArrayList<>();
        
        for (int i = 0; i < userList.size(); i++) {
            for (int j = i + 1; j < userList.size(); j++) {
                User user1 = userList.get(i);
                User user2 = userList.get(j);

                UserEpidemicExtension ext1 = getUserEpidemicExtension(user1);
                UserEpidemicExtension ext2 = getUserEpidemicExtension(user2);
                
                if (ext1 == null || ext2 == null) continue;
                
                boolean user1Infectious = isInfectious(ext1);
                boolean user2Infectious = isInfectious(ext2);
                boolean user1Susceptible = isSusceptible(ext1);
                boolean user2Susceptible = isSusceptible(ext2);
                
                // Contacts between non-relevant users are not taken into account
                if (!((user1Infectious && user2Susceptible) || 
                    (user2Infectious && user1Susceptible))) {
                    continue;
                }

                
                double distance = calculateDistance(user1, user2);
                String contactKey = generateContactKey(user1.userID, user2.userID);
                
                ContactRecord existingContact = findContactByKey(contactKey);
                
                if (distance <= proximityThresholdPixels) {
                    if (existingContact != null) {
                        if (existingContact.isActive()) {
                            existingContact.setDuration(existingContact.getDuration() + 1);
                        } else {
                            existingContact.setActive(true);
                            existingContact.setDuration(existingContact.getDuration() + 1);
                        }
                    } else {
                        ContactType contactType = determineContactType(user1, user2);
                        int roomId = getCurrentRoomId(user1);
                        
                        ContactRecord newContact = new ContactRecord(
                            user1.userID, 
                            user2.userID, 
                            currentTime, 
                            distance, 
                            1,
                            roomId, 
                            contactType
                        );
                        
                        allContacts.add(newContact);
                    }
                } else {
                    if (existingContact != null && existingContact.isActive()) {
                        existingContact.setActive(false);

                        contactsEndingNow.add(existingContact);
                    }
                }
            }
        }
        evaluateEndedContacts(contactsEndingNow, userList);
    }

    /**
     * Evaluates ended contacts for potential transmissions
     * 
     * @param endedContacts List of contacts that have ended
     * @param userList List of users in the simulation
     */
    private void evaluateEndedContacts(List<ContactRecord> endedContacts, List<User> userList) {
        if (endedContacts.isEmpty()) {
            return;
        }
        
        for (ContactRecord contact : endedContacts) {
            User user1 = findUserById(userList, contact.getUser1Id());
            User user2 = findUserById(userList, contact.getUser2Id());
            
            if (user1 == null || user2 == null) continue;
           
            evaluatePotentialTransmission(user1, user2, contact);
        }
    }

    /**
     * Evaluates remaining active contacts at the end of the simulation
     * 
     * @param userList List of users in the simulation
     */
    public void evaluateRemainingActiveContacts(List<User> userList) {
        List<ContactRecord> activeContacts = allContacts.stream()
            .filter(ContactRecord::isActive)
            .collect(Collectors.toList());
        
        if (activeContacts.isEmpty()) {
            return;
        }
        
        for (ContactRecord contact : activeContacts) {
            contact.setActive(false);
            
            User user1 = findUserById(userList, contact.getUser1Id());
            User user2 = findUserById(userList, contact.getUser2Id());
            
            if (user1 == null || user2 == null) continue;
            
            evaluatePotentialTransmission(user1, user2, contact);
        }
    }

    /**
     * Finds a contact by its key
     * 
     * @param contactKey Unique key identifying the contact
     * @return ContactRecord if found, otherwise null
     */
    private ContactRecord findContactByKey(String contactKey) {
        String[] parts = contactKey.split("_");
        long userId1 = Long.parseLong(parts[0]);
        long userId2 = Long.parseLong(parts[1]);
        
        for (ContactRecord contact : allContacts) {
            if ((contact.getUser1Id() == userId1 && contact.getUser2Id() == userId2) ||
                (contact.getUser1Id() == userId2 && contact.getUser2Id() == userId1)) {
                return contact;
            }
        }
        return null;
    }
    
    /**
     * Evaluates transmissions for all contacts
     * 
     * @param userList List of users in the simulation
     */
    public void evaluateTransmissions(List<User> userList) {
        for (ContactRecord contact : allContacts) {
            if (!contact.isActive()) continue; // Added by Nacho Palacio 2025-11-11

            User user1 = findUserById(userList, contact.getUser1Id());
            User user2 = findUserById(userList, contact.getUser2Id());
            
            if (user1 != null && user2 != null) {
                evaluatePotentialTransmission(user1, user2, contact);
            }
        }
    }

    /**
     * Evaluates if transmission occurs between two users
     * 
     * @param user1 First user involved in the contact
     * @param user2 Second user involved in the contact
     * @param contact Contact record between the two users
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
     * 
     * @param infectious Infectious user extension
     * @param susceptible Susceptible user extension
     * @param contact Contact record between the two users
     */
    private void attemptTransmission(UserEpidemicExtension infectious, 
                               UserEpidemicExtension susceptible, 
                               ContactRecord contact) {

        if (epidemicModel == null) {
            if (Math.random() < 0.1) {
                infectUserWithStateManager(susceptible);
            }
            return;
        }
        
        User infectiousUser = findUserByExtension(infectious);
        User susceptibleUser = findUserByExtension(susceptible);
        
        if (infectiousUser == null || susceptibleUser == null) {
            return;
        }

        UserEpidemicExtension extension = susceptibleUser.getEpidemicExtension();
        if (extension != null && extension.isImmune()) {
            return;
        }
        
        double transmissionProb = epidemicModel.calculateTransmissionProbability(
            infectiousUser, susceptibleUser, contact);
            
        double randomValue = Math.random();
        if (randomValue < transmissionProb) {
            infectUserWithStateManager(susceptible);
            User susceptibleUser1 = findUserByExtension(susceptible);
            if (susceptibleUser1 != null) {
                EpidemicStatistics.getInstance().recordInfection(susceptibleUser1.userID, "Contact transmission");
            }
        }
    }

    /**
     * Infects a user using the EpidemicStateManager
     * 
     * @param extension User's epidemic extension
     */
    private void infectUserWithStateManager(UserEpidemicExtension extension) {
        if (stateManager == null) {
            System.out.println("Warning! No EpidemicStateManager configured");
            return;
        }

        User user = findUserByExtension(extension);
        if (user != null) {
            stateManager.infectUser(user);
        } else {
            System.out.println("Warning! Could not find user for infection");
        }
    }
    
    /**
     * Returns contacts for a specific user
     * 
     * @param userId ID of the user
     * @return List of ContactRecord objects involving the specified user
     */
    public List<ContactRecord> getContactsForUser(long userId) {
        // Modified by Nacho Palacio 2025-11-11
        return allContacts.stream()
            .filter(c -> c.getUser1Id() == userId || c.getUser2Id() == userId)
            .collect(Collectors.toList());
    }

    // MÉTODOS AUXILIARES

    /**
     * Calculates distance between two users
     * 
     * @param user1 First user
     * @param user2 Second user
     * @return Distance in pixels
     */
    private double calculateDistance(User user1, User user2) {
        double dx = user1.x - user2.x;
        double dy = user1.y - user2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Generates a unique key for a contact between two users
     * 
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return Contact key string
     */
    private String generateContactKey(long userId1, long userId2) {
        return Math.min(userId1, userId2) + "_" + Math.max(userId1, userId2);
    }
    
    /**
     * Determines contact type between two users
     * 
     * @param user1 First user
     * @param user2 Second user
     * @return ContactType
     */
    private ContactType determineContactType(User user1, User user2) {
        return ContactType.COMMUNITY;
    }
    
    /**
     * Gets the current room ID of a user
     * 
     * @param user The user object
     * @return Room ID
     */
    private int getCurrentRoomId(User user) {
        return user.room;
    }
    
    /**
     * Checks if a user is infectious
     * 
     * @param extension User's epidemic extension
     * @return true if infectious
     */
    private boolean isInfectious(UserEpidemicExtension extension) {
        return extension.getHealthStatus() == HealthStatus.INFECTIOUS_SYMPTOMATIC || 
               extension.getHealthStatus() == HealthStatus.SUPER_SPREADER;
    }
    
    /**
     * Checks if a user is susceptible
     * 
     * @param extension User's epidemic extension
     * @return true if susceptible
     */
    private boolean isSusceptible(UserEpidemicExtension extension) {
        return extension.getHealthStatus() == HealthStatus.SUSCEPTIBLE;
    }
    
    /**
     * Finds a user by ID in a list
     * 
     * @param userList List of users
     * @param userId ID of the user to find
     * @return User object or null if not found
     */
    private User findUserById(List<User> userList, long userId) {
        return userList.stream()
                      .filter(user -> user.userID == userId)
                      .findFirst()
                      .orElse(null);
    }

    /**
     * Gets the epidemic extension of a user
     * 
     * @param user The user object
     * @return UserEpidemicExtension
     */
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }

    /**
     * Finds a user by their epidemic extension
     * 
     * @param extension User's epidemic extension
     * @return User object or null if not found
     */
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

    /**
     * Gets all recorded contacts
     * 
     * @return List of all ContactRecord objects
     */
    public List<ContactRecord> getAllContacts() {
        return new ArrayList<>(allContacts);
    }

    /**
     * Gets all active contacts
     * 
     * @return List of active ContactRecord objects
     */
    public List<ContactRecord> getActiveContacts() {
        return allContacts.stream()
            .filter(ContactRecord::isActive)
            .collect(Collectors.toList());
    }

    /**
     * Gets all completed contacts (inactive and meeting duration criteria)
     * 
     * @return List of completed ContactRecord objects
     */
    public List<ContactRecord> getCompletedContacts() {
        return allContacts.stream()
            .filter(c -> !c.isActive() && c.getDuration() >= minContactDurationSeconds)
            .collect(Collectors.toList());
    }

    /**
     * Gets the proximity threshold in pixels
     * 
     * @return Proximity threshold in pixels
     */
    public double getProximityThreshold() {
        return proximityThresholdPixels;
    }

    /**
     * Gets the minimum contact duration in seconds
     * 
     * @return Minimum contact duration in seconds
     */
    public int getMinContactDuration() {
        return minContactDurationSeconds;
    }


    // SETTERS

    /**
     * Sets the proximity threshold in pixels
     * 
     * @param proximityThresholdPixels Proximity threshold in pixels
     */
    public void setProximityThreshold(double proximityThresholdPixels) {
        this.proximityThresholdPixels = proximityThresholdPixels;
    }
    
    /**
     * Sets the minimum contact duration in seconds
     * 
     * @param minContactDurationSeconds Minimum contact duration in seconds
     */
    public void setMinContactDuration(int minContactDurationSeconds) {
        this.minContactDurationSeconds = minContactDurationSeconds;
    }

    /**
     * Sets the epidemic model
     * 
     * @param epidemicModel Epidemic model to use
     */
    public void setEpidemicModel(es.unizar.epidemic.models.EpidemicModel epidemicModel) {
        this.epidemicModel = epidemicModel;
    }

    /**
     * Sets the epidemic state manager
     * 
     * @param stateManager EpidemicStateManager to use
     */
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