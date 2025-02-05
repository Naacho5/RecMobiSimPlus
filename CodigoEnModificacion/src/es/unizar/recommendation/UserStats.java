package es.unizar.recommendation;

import java.util.ArrayList;
import java.util.List;

/**
  * Stores user stats for recommender testing
  */
public class UserStats {

	private double totalTime;
	private long distance;
	
	private List<Double> riskInRooms;
	
	/**
	 * Public constructor
	 */
	public UserStats() {
		this.totalTime = 0;
		this.distance = 0;
		
		riskInRooms = new ArrayList<Double>();
	}
	
	public double calculateAvgRisk() {
		double sum = 0.0;
		for (Double x : this.riskInRooms) {
			sum += x;
		}
		return sum/this.riskInRooms.size();
	}

	public void addRisk(double newRisk) {
		this.riskInRooms.add(newRisk);
	}
	
	public void addTime(double time) {
		this.totalTime += time;
	}
	
	public double getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public long getDistance() {
		return distance;
	}

	public void setDistance(long distance) {
		this.distance = distance;
	}

	public List<Double> getRiskInRooms() {
		return riskInRooms;
	}

	public void setRiskInRooms(List<Double> riskInRooms) {
		this.riskInRooms = riskInRooms;
	}

	@Override
	public String toString() {
		return "UserStats [totalTime=" + totalTime + ", distance=" + distance + ", riskInRooms=" + riskInRooms + "]";
	}

	
	
	
	
}
