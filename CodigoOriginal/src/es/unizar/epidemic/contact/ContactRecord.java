package es.unizar.epidemic.contact;

import java.time.LocalDateTime;

/**
 * Represents a record of contact between two users
 * 
 * @author Nacho Palacio
 */
public class ContactRecord {
    public long user1Id;
    public long user2Id;
    public LocalDateTime timestamp;
    public double distance;
    public int duration;
    public int roomId;
    public ContactType contactType;
    public boolean isActive;
    

    public ContactRecord(long user1Id, long user2Id, LocalDateTime timestamp, double distance, int duration, int roomId, ContactType contactType) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.timestamp = timestamp;
        this.distance = distance;
        this.duration = duration;
        this.roomId = roomId;
        this.contactType = contactType;
        this.isActive = true;
    }

    public ContactRecord(long user1Id, long user2Id, int roomId, double duration) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.roomId = roomId;
        this.duration = (int) duration;
        this.timestamp = LocalDateTime.now();
        this.distance = 5.0;
        this.contactType = ContactType.COMMUNITY;
    }

    // GETTERS

    /**
     * Gets the ID of the first user.
     * 
     * @return ID of the first user
     */
    public long getUser1Id() {
        return user1Id;
    }

    /**
     * Gets the ID of the second user.
     * 
     * @return ID of the second user
     */
    public long getUser2Id() {
        return user2Id;
    }   

    /**
     * Gets the timestamp of the contact.
     * 
     * @return Timestamp of the contact
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the distance between the two users during the contact.
     * 
     * @return Distance between the two users
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Gets the duration of the contact.
     * 
     * @return Duration of the contact
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the ID of the room where the contact occurred.
     * 
     * @return ID of the room
     */
    public int getRoomId() {
        return roomId;
    }
    /**
     * Gets the type of contact.
     * 
     * @return Type of contact
     */
    public ContactType getContactType() {
        return contactType;
    }

    /**
     * Checks if the contact is active.
     * 
     * @return true if the contact is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }


    // SETTERS

    /**
     * Sets the ID of the first user.
     * 
     * @param user1Id ID of the first user
     */
    public void setUser1Id(long user1Id) {
        this.user1Id = user1Id;
    }

    /**
     * Sets the ID of the second user.
     * 
     * @param user2Id ID of the second user
     */
    public void setUser2Id(long user2Id) {
        this.user2Id = user2Id;
    }

    /**
     * Sets the timestamp of the contact.
     * 
     * @param timestamp Timestamp of the contact
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the distance between the two users during the contact.
     * 
     * @param distance Distance between the two users
     */
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Sets the duration of the contact.
     * 
     * @param duration Duration of the contact
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Sets the ID of the room where the contact occurred.
     * 
     * @param roomId ID of the room
     */
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    /**
     * Sets the type of contact.
     * 
     * @param contactType Type of contact
     */
    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
    }

    /**
     * Sets the active status of the contact.
     * 
     * @param isActive Active status of the contact
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "ContactRecord{" +
                "user1Id=" + user1Id +
                ", user2Id=" + user2Id +
                ", timestamp=" + timestamp +
                ", distance=" + distance +
                ", duration=" + duration +
                ", roomId=" + roomId +
                ", contactType=" + contactType +
                '}';
    }

}