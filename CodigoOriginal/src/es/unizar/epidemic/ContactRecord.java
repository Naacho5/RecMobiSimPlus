package es.unizar.epidemic;

import java.time.LocalDateTime;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class ContactRecord {
    public long user1Id;
    public long user2Id;
    public LocalDateTime timestamp;
    public double distance;
    public int duration;
    public int roomId;
    public ContactType contactType;
    

    public ContactRecord(long user1Id, long user2Id, LocalDateTime timestamp, double distance, int duration, int roomId, ContactType contactType) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.timestamp = timestamp;
        this.distance = distance;
        this.duration = duration;
        this.roomId = roomId;
        this.contactType = contactType;
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
    public long getUser1Id() {
        return user1Id;
    }

    public long getUser2Id() {
        return user2Id;
    }   
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getDistance() {
        return distance;
    }

    public int getDuration() {
        return duration;
    }

    public int getRoomId() {
        return roomId;
    }

    public ContactType getContactType() {
        return contactType;
    }


    // SETTERS
    public void setUser1Id(long user1Id) {
        this.user1Id = user1Id;
    }

    public void setUser2Id(long user2Id) {
        this.user2Id = user2Id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
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