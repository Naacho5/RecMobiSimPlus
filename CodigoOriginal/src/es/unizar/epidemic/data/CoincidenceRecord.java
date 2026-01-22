package es.unizar.epidemic.data;

/**
 * Record of a coincidence between two users in the simulation
 * 
 * @author Nacho Palacio
 * @date 2025-11-28
 */
public class CoincidenceRecord {
    
    // User identifiers (simulation IDs)
    private final int user1;
    private final int user2;
    
    // Clique identifiers
    private final int clique1;
    private final int clique2;
    
    // Spatial context
    private final int room;
    
    // Temporal context (simulation iterations)
    private final int startIteration;
    private int endIteration;  // Mutable: updated while the coincidence continues
    
    // State
    private boolean isActive;  // true if the coincidence is ongoing
    
    public CoincidenceRecord(int user1, int user2, int clique1, int clique2, 
                            int room, int startIteration) {
        this.user1 = Math.min(user1, user2);
        this.user2 = Math.max(user1, user2);
        this.clique1 = clique1;
        this.clique2 = clique2;
        this.room = room;
        this.startIteration = startIteration;
        this.endIteration = startIteration; 
        this.isActive = true;
    }
    
    /**
     * Updateds the end iteration to the current iteration if it's greater
     * 
     * @param currentIteration Current simulation iteration
     */
    public void updateEndIteration(int currentIteration) {
        if (currentIteration > this.endIteration) {
            this.endIteration = currentIteration;
        }
    }
    
    /**
     * Closes the coincidence record
     * 
     * @param finalIteration Last iteration of the coincidence
     */
    public void close(int finalIteration) {
        this.isActive = false;
        this.endIteration = finalIteration;
    }
    
    /**
     * Gets the duration in seconds
     * 
     * @param secondsPerIteration Seconds represented by each iteration
     * @return Duration in seconds
     */
    public double getDurationSeconds(double secondsPerIteration) {
        return (endIteration - startIteration + 1) * secondsPerIteration;
    }
    
    /**
     * Gets the duration in iterations
     * 
     * @return Duration in iterations
     */
    public int getDurationIterations() {
        return endIteration - startIteration + 1;
    }
    
    /**
     * Checks if this coincidence involves a specific user
     * 
     * @param userId ID of the user to check
     * @return true if the user is involved
     */
    public boolean involvesUser(int userId) {
        return this.user1 == userId || this.user2 == userId;
    }
    
    /**
     * Checks if this coincidence involves a specific clique
     * 
     * @param cliqueId ID of the clique to check
     * @return true if the clique is involved
     */
    public boolean involvesClique(int cliqueId) {
        return this.clique1 == cliqueId || this.clique2 == cliqueId;
    }
    
    /**
     * Generates a unique key to identify this coincidence
     * Format: "user1-user2-room"
     * 
     * @return Unique key string
     */
    public String getKey() {
        return user1 + "-" + user2 + "-" + room;
    }
    
    // GETTERS
    
    public int getUser1() { return user1; }
    public int getUser2() { return user2; }
    public int getClique1() { return clique1; }
    public int getClique2() { return clique2; }
    public int getRoom() { return room; }
    public int getStartIteration() { return startIteration; }
    public int getEndIteration() { return endIteration; }
    public boolean isActive() { return isActive; }
    
    // UTILITY METHODS

    @Override
    public String toString() {
        return String.format(
            "CoincidenceRecord{users=(%d,%d), cliques=(%d,%d), room=%d, " +
            "iterations=[%d->%d], duration=%d, active=%s}",
            user1, user2, clique1, clique2, room, 
            startIteration, endIteration, getDurationIterations(), isActive
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CoincidenceRecord other = (CoincidenceRecord) obj;
        
        return this.user1 == other.user1 &&
               this.user2 == other.user2 &&
               this.room == other.room &&
               this.startIteration == other.startIteration;
    }
    
    @Override
    public int hashCode() {
        int result = Integer.hashCode(user1);
        result = 31 * result + Integer.hashCode(user2);
        result = 31 * result + Integer.hashCode(room);
        result = 31 * result + Integer.hashCode(startIteration);
        return result;
    }
    
    /**
     * Converts the record to a CSV format string
     * 
     * @param secondsPerIteration Seconds represented by each iteration
     * @return CSV formatted string
     */
    public String toCSV(double secondsPerIteration) {
        return String.format("%d,%d,%d,%d,%d,%d,%d,%.2f",
            user1, user2, clique1, clique2, room, 
            startIteration, endIteration, getDurationSeconds(secondsPerIteration)
        );
    }
    
    /**
     * Header for CSV file
     * 
     * @return Header in CSV format
     */
    public static String csvHeader() {
        return "user1,user2,clique1,clique2,room,startIteration,endIteration,durationSeconds";
    }
}