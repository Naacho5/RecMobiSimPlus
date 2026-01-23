package es.unizar.epidemic.models;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.epidemic.general.HealthStatus;
import es.unizar.epidemic.general.UserEpidemicExtension;
import es.unizar.epidemic.statistics.EpidemicIterationsBinaryWriter;
import es.unizar.gui.Configuration;
import es.unizar.gui.simulation.User;
import es.unizar.util.Literals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for epidemic models with shared functionality
 * Adapted to store exposure iterations on disk for memory efficiency
 * 
 * @author Nacho Palacio
 */
public abstract class AbstractEpidemicModel implements EpidemicModel {
    
    protected Map<Integer, Map<Integer, Double>> userRoomExposureTime;
    protected Map<Integer, Map<Integer, Integer>> roomInfectiousHistory = new HashMap<>();
    // protected Map<Integer, Map<Integer, List<Integer>>> userRoomExposureIterations = new HashMap<>();

    public EpidemicIterationsBinaryWriter iterationsWriter;
    
    public AbstractEpidemicModel() {
        this.userRoomExposureTime = new HashMap<>();
        this.iterationsWriter = new EpidemicIterationsBinaryWriter();
    }
    
    ///////////////////////////////////////////////////////////////////// 
    // EXPOSURE TRACKING
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Initializes exposure tracking for all users.
     * Creates empty exposure time maps for each user.
     * 
     * @param users list of all users in the simulation
     */
    public void initializeExposureTracking(List<User> users) {
        userRoomExposureTime = new HashMap<>();
        for (User user : users) {
            userRoomExposureTime.put(user.userID, new HashMap<>());
        }
    }
    
    /**
     * Updates room exposure time for all users.
     * Increments the exposure time for each user in their current room.
     * 
     * @param users list of users to update exposure for
     * @param deltaTimeHours time increment in hours
     */
    public void updateRoomExposure(List<User> users, double deltaTimeHours) { 
        for (User user : users) {
            int roomId = user.room;
            Map<Integer, Double> roomExposure = userRoomExposureTime.get(user.userID);
            
            if (roomExposure != null) {
                double oldExposure = roomExposure.getOrDefault(roomId, 0.0);
                double newExposure = oldExposure + deltaTimeHours;
                roomExposure.put(roomId, newExposure);
            }
        }
    }
    
    /**
     * Gets the total exposure time for a user in a specific room.
     * 
     * @param userId the user ID to query
     * @param roomId the room ID to query
     * @return total exposure time in hours, or 0.0 if no exposure recorded
     */
    public double getUserRoomExposureTime(int userId, int roomId) {
        Map<Integer, Double> roomExposure = userRoomExposureTime.get(userId);
        return roomExposure != null ? roomExposure.getOrDefault(roomId, 0.0) : 0.0;
    }
    
    ///////////////////////////////////////////////////////////////////// 
    // USER MANAGEMENT
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Gets all users currently in a specific room.
     * 
     * @param roomId the room ID to query
     * @return list of users currently in the room
     */
    protected List<User> getUsersInRoom(int roomId) {
        List<User> usersInRoom = new ArrayList<>();
        
        try {
            if (Configuration.simulation != null) {
                List<User> allUsers = Configuration.simulation.getAllUsers();
                
                for (User user : allUsers) {
                    if (user != null && user.room == roomId) {
                        usersInRoom.add(user);
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return usersInRoom;
    }
    
    /**
     * Counts the number of infectious people in a list of users.
     * 
     * @param users list of users to count
     * @return number of users who are infectious
     */
    protected int countInfectiousPeople(List<User> users) {
        int count = 0;
        for (User user : users) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && isInfectious(extension)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets all infectious users currently in a specific room.
     * 
     * @param roomId the room ID to query
     * @return list of infectious users in the room
     */
    protected List<User> getUsersInfectiousInRoom(int roomId) {
        List<User> infectiousUsers = new ArrayList<>();
        for (User user : getUsersInRoom(roomId)) {
            UserEpidemicExtension extension = getUserEpidemicExtension(user);
            if (extension != null && isInfectious(extension)) {
                infectiousUsers.add(user);
            }
        }
        return infectiousUsers;
    }

    /**
     * Records the number of infectious people in a room at a specific iteration.
     * Stores historical data for later exposure risk calculations.
     * 
     * @param roomId the room ID
     * @param iteration the simulation iteration number
     * @param infectiousCount number of infectious people in the room
     */
    public void recordRoomInfectiousCount(int roomId, int iteration, int infectiousCount) {
        roomInfectiousHistory
            .computeIfAbsent(roomId, k -> new HashMap<>())
            .put(iteration, infectiousCount);

        // System.out.println("Recorded infectious count for room " + roomId + " at iteration " + iteration + ": " + infectiousCount);
    }

    /**
     * Gets the historical count of infectious people in a room at a specific iteration.
     * 
     * @param roomId the room ID to query
     * @param iteration the iteration number to query
     * @return number of infectious people, or 0 if no data recorded
     */
    public int getHistoricalInfectiousCount(int roomId, int iteration) {
        Map<Integer, Integer> roomHistory = roomInfectiousHistory.get(roomId);
        if (roomHistory == null) return 0;
        
        return roomHistory.getOrDefault(iteration, 0);
    }

    // Without disk storage (original in-memory version)
    // public void recordUserRoomExposureIteration(int userId, int roomId, int iteration) {
    //     userRoomExposureIterations
    //         .computeIfAbsent(userId, k -> new HashMap<>())
    //         .computeIfAbsent(roomId, k -> new ArrayList<>())
    //         .add(iteration);
    // }

    public void recordUserRoomExposureIteration(int userId, int roomId, int iteration) {
        // System.out.println("Recording exposure iteration: userId=" + userId + ", roomId=" + roomId + ", iteration=" + iteration);
        iterationsWriter.recordIteration(userId, roomId, iteration);
    }

    /**
     * Records exposure iterations for all users in their current rooms.
     * 
     * @param users list of users to record exposure for
     * @param currentIteration current simulation iteration number
     */
    public void recordExposureIteration(List<User> users, int currentIteration) {
        for (User user : users) {
            if (user.room > 0) {
                recordUserRoomExposureIteration(user.userID, user.room, currentIteration);
            }
        }
    }

    // Without disk storage (original in-memory version)
    // public int getAverageInfectiousCount(int roomId, List<Integer> exposureIterations) {
    //     if (exposureIterations == null || exposureIterations.isEmpty()) {
    //         return 0;
    //     }
        
    //     int totalInfectious = 0;
    //     int validIterations = 0;
        
    //     for (int iteration : exposureIterations) {
    //         int count = getHistoricalInfectiousCount(roomId, iteration);
    //         totalInfectious += count;
    //         validIterations++;
    //     }
        
    //     if (validIterations == 0) return 0;
        
    //     return (int) Math.ceil((double) totalInfectious / validIterations);
    // }

    public int getAverageInfectiousCount(int roomId, List<Integer> exposureIterations) {
        if (exposureIterations == null || exposureIterations.isEmpty()) {
            System.out.println("No exposure iterations provided for roomId=" + roomId);
            return 0;
        }
        
        int totalInfectious = 0;
        int validIterations = 0;
        
        for (int iteration : exposureIterations) {
            int count = getHistoricalInfectiousCount(roomId, iteration);
            totalInfectious += count;
            validIterations++;
        }

        if (validIterations == 0) return 0;
        return (int) Math.ceil((double) totalInfectious / validIterations);
    }
    
    
    ///////////////////////////////////////////////////////////////////// 
    // HEALTH STATUS
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Checks if a user's epidemic extension indicates they are infectious.
     * 
     * @param extension the user's epidemic extension
     * @return true if the user is infectious (symptomatic or super spreader)
     */
    public boolean isInfectious(UserEpidemicExtension extension) {
        return extension.getHealthStatus().equals(HealthStatus.INFECTIOUS_SYMPTOMATIC) ||
               extension.getHealthStatus().equals(HealthStatus.SUPER_SPREADER);
    }
    
    /**
     * Gets the epidemic extension from a user.
     * 
     * @param user the user to get the extension from
     * @return the user's epidemic extension
     */
    protected UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }
    
    ///////////////////////////////////////////////////////////////////// 
    // MASK CALCULATIONS
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Calculates the fraction of users wearing masks in a room.
     * 
     * @param usersInRoom list of users in the room
     * @return fraction of users wearing masks (0.0 to 1.0)
     */
    public double calculateFractionWithMasks(List<User> usersInRoom) {
        if (usersInRoom.isEmpty()) {
            return 0.0;
        }
        
        int usersWithMasks = 0;
        
        for (User user : usersInRoom) {
            UserEpidemicExtension extension = user.getEpidemicExtension();
            if (extension != null && extension.isMaskWearing()) {
                usersWithMasks++;
            }
        }
        
        return (double) usersWithMasks / usersInRoom.size();
    }
    
    ///////////////////////////////////////////////////////////////////// 
    // ROOM DIMENSIONS
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Gets the width of a room by calculating the difference between min and max X coordinates.
     * 
     * @param roomId the room ID to query
     * @return room width in pixels, or default value if calculation fails
     */
    protected double getRoomWidth(int roomId) {
        try {
            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;

            DataAccessRoomFile roomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
            
            int cornerCount = Integer.parseInt(roomFile.getPropertyValue(Literals.NUMBER_CORNER + roomId));
            
            for (int i = 1; i <= cornerCount; i++) {
                String cornerData = roomFile.getPropertyValue(Literals.CORNER + i + "_" + roomId);

                if (cornerData != null) {
                    double x = Double.parseDouble(cornerData.split(",")[0].trim());
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
            
            return maxX - minX;
        } catch (Exception e) {
            return 10.0 * Configuration.getPixelsPerMeter(); // Default width
        }
    }
    
    /**
     * Gets the length of a room by calculating the difference between min and max Y coordinates.
     * 
     * @param roomId the room ID to query
     * @return room length in pixels, or default value if calculation fails
     */
    protected double getRoomLength(int roomId) {
        try {
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            
            DataAccessRoomFile roomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
            
            int cornerCount = Integer.parseInt(roomFile.getPropertyValue(Literals.NUMBER_CORNER + roomId));

            for (int i = 1; i <= cornerCount; i++) {
                String cornerData = roomFile.getPropertyValue(Literals.CORNER + i + "_" + roomId);

                if (cornerData != null) {
                    double y = Double.parseDouble(cornerData.split(",")[1].trim());
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
            
            return maxY - minY;
        } catch (Exception e) {
            return 6.0 * Configuration.getPixelsPerMeter(); // Default length
        }
    }

    /**
     * Gets the map of user room exposure times.
     * 
     * @return map from user ID to map of room ID to exposure time in hours
     */
    public Map<Integer, Map<Integer, Double>> getUserRoomExposureTime() {
        return userRoomExposureTime;
    }

    /**
     * Gets the historical record of infectious counts per room and iteration.
     * 
     * @return map from room ID to map of iteration to infectious count
     */
    public Map<Integer, Map<Integer, Integer>> getRoomInfectiousHistory() {
        return roomInfectiousHistory;
    }

    // Without disk storage (original in-memory version)
    // public Map<Integer, Map<Integer, List<Integer>>> getUserRoomExposureIterations() {
    //     return userRoomExposureIterations;
    // }

    /**
     * Retrieves exposure iterations from disk for a specific user and room.
     * Reads binary data written during the simulation.
     * 
     * @param userId the user ID to query
     * @param roomId the room ID to query
     * @return list of iteration numbers when the user was exposed in the room
     */
    public List<Integer> getUserRoomExposureIterationsFromDisk(int userId, int roomId) {
        System.out.println("Retrieving exposure iterations for userId=" + userId + ", roomId=" + roomId);
        if (iterationsWriter == null) {
            System.err.println(" ERROR: iterationsWriter is NULL");
            return new ArrayList<>();
        }
        
        if (!iterationsWriter.isReady()) {
            System.err.println(" ERROR: iterationsWriter is not ready (closed or not initialized)");
            System.err.println("   - Total records: " + iterationsWriter.getTotalRecords());
            return new ArrayList<>();
        }
        
        System.out.println("Fetching exposure iterations from disk for userId=" + userId + ", roomId=" + roomId);
        System.out.println("   - Total records in file: " + iterationsWriter.getTotalRecords());
        System.out.println("   - Records for this user: " + iterationsWriter.getRecordCountForUser(userId));
        
        List<Integer> iterations = iterationsWriter.getIterations(userId, roomId);
        System.out.println("Retrieved " + iterations.size() + " iterations from disk for userId=" + userId + ", roomId=" + roomId);
        return iterations;
    }
    
    /**
     * Closes the iterations writer and flushes all pending data to disk.
     * Should be called at the end of the simulation to ensure data persistence.
     */
    public void closeIterationsWriter() {
        if (iterationsWriter != null) {
            System.out.println("🔴 closeIterationsWriter() called from:");
            new Exception().printStackTrace(System.out);
            
            iterationsWriter.close();
        }
    }

    /**
     * Gets the total number of user-room exposure time entries.
     * 
     * @return total count of exposure time records across all users and rooms
     */
    public int getUserRoomExposureTimeSize() {
        if (userRoomExposureTime == null) return 0;
        int total = 0;
        for (Map<Integer, Double> m : userRoomExposureTime.values()) {
            if (m != null) total += m.size();
        }
        return total;
    }

    /**
     * Gets the total number of room infectious history entries.
     * 
     * @return total count of historical infectious count records across all rooms and iterations
     */
    public int getRoomInfectiousHistorySize() {
        if (roomInfectiousHistory == null) return 0;
        int total = 0;
        for (Map<Integer, Integer> m : roomInfectiousHistory.values()) {
            if (m != null) total += m.size();
        }
        return total;
    }

    // Without disk storage (original in-memory version)
    // public int getUserRoomExposureIterationsSize() {
    //     if (userRoomExposureIterations == null) return 0;
    //     int total = 0;
    //     for (Map<Integer, List<Integer>> m : userRoomExposureIterations.values()) {
    //         if (m != null) {
    //             for (List<Integer> l : m.values()) {
    //                 if (l != null) total += l.size();
    //             }
    //         }
    //     }
    //     return total;
    // }
}