package es.unizar.epidemic.data;

/**
 * Represents a temporal range with start and end times
 * 
 * @author Nacho Palacio
 */
public class TemporalRange {
    public final long startTime;
    public final long endTime;
    
    public TemporalRange(long startTime, long endTime) {
        if (endTime <= startTime) {
            throw new IllegalArgumentException(
                String.format("endTime (%d) must be > startTime (%d)", endTime, startTime)
            );
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    /**
     * Verifies if this range overlaps with another
     * 
     * @param other Other temporal range
     * @return true if they overlap
     */
    public boolean overlaps(TemporalRange other) {
       return !(this.endTime <= other.startTime || this.startTime >= other.endTime);
    }
    
    /**
     * Calculates the duration of this temporal range
     * 
     * @return duration in seconds
     */
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * Calculates the overlap duration with another temporal range
     * 
     * @param other Other temporal range
     * @return duration of overlap in seconds
     */
    public long getOverlapDuration(TemporalRange other) {
        if (!overlaps(other)) {
            return 0;
        }
        
        long overlapStart = Math.max(this.startTime, other.startTime);
        long overlapEnd = Math.min(this.endTime, other.endTime);
        return overlapEnd - overlapStart;
    }
    
    /**
     * Verifies if this range fully contains another
     * 
     * @param other Other temporal range
     * @return true if this range fully contains the other
     */
    public boolean contains(TemporalRange other) {
        return this.startTime <= other.startTime && this.endTime >= other.endTime;
    }
    
    @Override
    public String toString() {
        return String.format("[%d -> %d] (%d seconds, %.2f hours)", 
                           startTime, endTime, getDuration(), getDuration() / 3600.0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TemporalRange other = (TemporalRange) obj;
        return startTime == other.startTime && endTime == other.endTime;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(startTime) * 31 + Long.hashCode(endTime);
    }
}