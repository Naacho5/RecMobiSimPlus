package es.unizar.epidemic.tests.common;

/**
 * Data class to hold simulation results
 * 
 * @author Nacho Palacio
 */
public class SimulationResult {

    // IDENTIFICATION
    public String scenarioName;
    public String modelUsed;
    
    // POPULATION
    public int totalUsers;
    public int susceptible;
    public int infectiousSymp;
    public int infectiousAsymp;
    public int superSpreaders;
    public int recovered;
    
    // EPIDEMIC METRICS
    public double attackRate;               // Attack rate
    public double infectionRate;            // Infection rate
    
    // CONTACT METRICS (SIMPLE_PROXIMITY)
    public int totalContacts;               // Total contacts
    public int infectiousContacts;          // Infectious contacts
    
    // AEROSOL METRICS (PENG, LELIEVELD)
    public double averageConcentration;     // Average concentration
    public double individualRisk;           // Individual risk
    
    // PERFORMANCE
    public long executionTimeMs;            // Execution time in ms
    public int simulationDurationSeconds;  // Simulation duration in seconds

    public double intraCliqueAttackRate;    // Attack rate within cliques
    public double isolationRate;            // Isolation rate between cliques

    // MIXED MODE METRICS
    public double cliqueAttackRate;         // Attack rate for clique users
    public double independentAttackRate;    // Attack rate for independent users

    // MACRO COMPARISON METRICS
    public double averageRating;            // Average user rating
    public double averageDistance;          // Average distance between items
    public double averageDistanceRooms;     // Average rooms between visited items
    public double averageRecommendationRisk; // Average risk of recommended items (same risk as the macro simulator)
    public double averageBridgeRisk;        // Average risk of recommended items (risk calculated in the recommender bridge)
    
    public SimulationResult() {
        this.scenarioName = "";
        this.modelUsed = "";
        this.totalUsers = 0;
        this.susceptible = 0;
        this.infectiousSymp = 0;
        this.infectiousAsymp = 0;
        this.superSpreaders = 0;
        this.recovered = 0;
        this.attackRate = 0.0;
        this.infectionRate = 0.0;
        this.totalContacts = 0;
        this.infectiousContacts = 0;
        this.averageConcentration = 0.0;
        this.individualRisk = 0.0;
        this.executionTimeMs = 0L;
        this.intraCliqueAttackRate = 0.0;
        this.isolationRate = 0.0;
        this.cliqueAttackRate = 0.0;
        this.independentAttackRate = 0.0;
        this.averageRating = 0.0;
        this.averageDistance = 0.0;
        this.averageDistanceRooms = 0.0;
        this.averageRecommendationRisk = 0.0;
        this.averageBridgeRisk = 0.0;
    }
    
    public SimulationResult(String scenarioName, String modelUsed, int totalUsers) {
        this();
        this.scenarioName = scenarioName;
        this.modelUsed = modelUsed;
        this.totalUsers = totalUsers;
    }
    
    /**
     * Returns total number of infectious individuals
     * 
     * @return Total of infectious individuals
     */
    public int getTotalInfectious() {
        return infectiousSymp + infectiousAsymp + superSpreaders;
    }
    
    /**
     * Returns the attack rate as a percentage (0-100)
     * 
     * @return Attack rate as a percentage
     */
    public double getAttackRatePercent() {
        return attackRate * 100.0;
    }
    
    /**
     * Returns the infection rate as a percentage (0-100)
     * 
     * @return Infection rate as a percentage
     */
    public double getInfectionRatePercent() {
        return infectionRate * 100.0;
    }
    
    /**
     * Returns the execution time in seconds
     * 
     * @return Execution time in seconds
     */
    public double getExecutionTimeSeconds() {
        return executionTimeMs / 1000.0;
    }
    
    /**
     * String representation of the result
     */
    @Override
    public String toString() {
        return String.format("SimulationResult[scenario=%s, model=%s, users=%d, attackRate=%.2f%%, execTime=%.2fs]",
            scenarioName, modelUsed, totalUsers, getAttackRatePercent(), getExecutionTimeSeconds());
    }
}