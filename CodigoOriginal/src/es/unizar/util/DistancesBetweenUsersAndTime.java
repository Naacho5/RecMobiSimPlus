package es.unizar.util;

public class DistancesBetweenUsersAndTime {
	
	long specialUser;
	long nonSpecialUser;
	int startTime;
	int endTime;
	
	public DistancesBetweenUsersAndTime(long specialUser, long nonSpecialUser, int startTime, int endTime) {
		super();
		this.specialUser = specialUser;
		this.nonSpecialUser = nonSpecialUser;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public long getSpecialUser() {
		return specialUser;
	}

	public void setSpecialUser(long specialUser) {
		this.specialUser = specialUser;
	}

	public long getNonSpecialUser() {
		return nonSpecialUser;
	}

	public void setNonSpecialUser(long nonSpecialUser) {
		this.nonSpecialUser = nonSpecialUser;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
	
	public int getTimeTogether() {
		return endTime-startTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (nonSpecialUser ^ (nonSpecialUser >>> 32));
		result = prime * result + (int) (specialUser ^ (specialUser >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DistancesBetweenUsersAndTime other = (DistancesBetweenUsersAndTime) obj;
		if (nonSpecialUser != other.nonSpecialUser)
			return false;
		if (specialUser != other.specialUser)
			return false;
		return true;
	}
	
	
	
	
}
