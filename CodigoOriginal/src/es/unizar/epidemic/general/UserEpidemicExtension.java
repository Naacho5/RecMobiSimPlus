package es.unizar.epidemic.general;

/**
 * Epidemic-related extension data for a user   
 * 
 * @author Nacho Palacio
 */
public class UserEpidemicExtension {
    
    private HealthStatus healthStatus = HealthStatus.SUSCEPTIBLE;
    private int hoursSinceInfection = 0;
    private boolean isMaskWearing = false;
    private double transmissionProbability = 0.05;
    boolean isInfected = false;
    boolean isFutureSuperSpreader = false;
    
    // User attributes
    private int age;
    private boolean isVaccinated = false;

    private boolean isImmune = false;
    private boolean isSuperSpreader = false;

    private int incubationPeriodHours = 0;
    private int infectiousPeriodHours = Integer.MAX_VALUE;

    private double viralEmissionRate = 0.0;
    
    // GETTERS

    /**
     * Gets the current health status of the user.
     * 
     * @return health status (SUSCEPTIBLE, INFECTED, RECOVERED, etc.)
     */
    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    /**
     * Gets the number of hours since the user was infected.
     * 
     * @return hours since infection
     */
    public int getHoursSinceInfection() {
        return hoursSinceInfection;
    }

    /**
     * Checks if the user is wearing a mask.
     * 
     * @return true if wearing a mask, false otherwise
     */
    public boolean isMaskWearing() {
        return isMaskWearing;
    }

    /**
     * Gets the transmission probability for this user.
     * 
     * @return transmission probability (0.0 to 1.0)
     */
    public double getTransmissionProbability() {
        return transmissionProbability;
    }

    /**
     * Gets the age of the user.
     * 
     * @return user age in years
     */
    public int getAge() {
        return age;
    }

    /**
     * Checks if the user is vaccinated.
     * 
     * @return true if vaccinated, false otherwise
     */
    public boolean isVaccinated() {
        return isVaccinated;
    }

    /**
     * Gets the incubation period duration in hours.
     * 
     * @return incubation period in hours
     */
    public int getIncubationPeriodHours() {
        return incubationPeriodHours;
    }

    /**
     * Gets the infectious period duration in hours.
     * 
     * @return infectious period in hours
     */
    public int getInfectiousPeriodHours() {
        return infectiousPeriodHours;
    }

    /**
     * Gets the viral emission rate for this user.
     * 
     * @return viral emission rate
     */
    public double getViralEmissionRate() {
        return viralEmissionRate;
    }

    /**
     * Checks if the user is immune to infection.
     * 
     * @return true if immune, false otherwise
     */
    public boolean isImmune() {
        return isImmune;
    }

    /**
     * Checks if the user is a super-spreader.
     * 
     * @return true if super-spreader, false otherwise
     */
    public boolean isSuperSpreader() {
        return isSuperSpreader;
    }

    /**
     * Checks if the user is currently infected.
     * 
     * @return true if infected, false otherwise
     */
    public boolean isInfected() {
        return isInfected;
    }

    /**
     * Checks if the user is designated to become a super-spreader.
     * 
     * @return true if designated as future super-spreader, false otherwise
     */
    public boolean isFutureSuperSpreader() {
        return isFutureSuperSpreader;
    }


    // SETTERS
    /**
     * Sets the health status of the user.
     * 
     * @param healthStatus the new health status
     */
    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    /**
     * Sets the number of hours since the user was infected.
     * 
     * @param hoursSinceInfection hours since infection
     */
    public void setHoursSinceInfection(int hoursSinceInfection) {
        this.hoursSinceInfection = hoursSinceInfection;
    }

    /**
     * Sets whether the user is wearing a mask.
     * 
     * @param isMaskWearing true if wearing a mask, false otherwise
     */
    public void setMaskWearing(boolean isMaskWearing) {
        this.isMaskWearing = isMaskWearing;
    }

    /**
     * Sets the transmission probability for this user.
     * 
     * @param transmissionProbability transmission probability (0.0 to 1.0)
     */
    public void setTransmissionProbability(double transmissionProbability) {
        this.transmissionProbability = transmissionProbability;
    }

    /**
     * Sets the age of the user.
     * 
     * @param age user age in years
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Sets whether the user is vaccinated.
     * 
     * @param isVaccinated true if vaccinated, false otherwise
     */
    public void setVaccinated(boolean isVaccinated) {
        this.isVaccinated = isVaccinated;
    }

    /**
     * Sets the incubation period duration in hours.
     * 
     * @param incubationPeriodHours incubation period in hours
     */
    public void setIncubationPeriod(int incubationPeriodHours) {
        this.incubationPeriodHours = incubationPeriodHours;
    }

    /**
     * Sets the infectious period duration in hours.
     * 
     * @param infectiousPeriodHours infectious period in hours
     */
    public void setInfectiousPeriod(int infectiousPeriodHours) {
        this.infectiousPeriodHours = infectiousPeriodHours;
    }

    /**
     * Sets the viral emission rate for this user.
     * 
     * @param viralEmissionRate viral emission rate
     */
    public void setViralEmissionRate(double viralEmissionRate) {
        this.viralEmissionRate = viralEmissionRate;
    }

    /**
     * Sets whether the user is immune to infection.
     * 
     * @param isImmune true if immune, false otherwise
     */
    public void setImmune(boolean isImmune) {
        this.isImmune = isImmune;
    }

    /**
     * Sets whether the user is a super-spreader.
     * 
     * @param isSuperSpreader true if super-spreader, false otherwise
     */
    public void setSuperSpreader(boolean isSuperSpreader) {
        this.isFutureSuperSpreader = isSuperSpreader;
    }

    /**
     * Sets whether the user is currently infected.
     * 
     * @param isInfected true if infected, false otherwise
     */
    public void setInfected(boolean isInfected) {
        this.isInfected = isInfected;
    }

    /**
     * Sets whether the user is designated to become a super-spreader.
     * 
     * @param isFutureSuperSpreader true if designated as future super-spreader, false otherwise
     */
    public void setFutureSuperSpreader(boolean isFutureSuperSpreader) {
        this.isFutureSuperSpreader = isFutureSuperSpreader;
    }

    /**
     * Resets the epidemic extension to initial state
     */
    public void reset() {
        this.healthStatus = HealthStatus.SUSCEPTIBLE;
        this.hoursSinceInfection = 0;
        this.isMaskWearing = false;
        this.transmissionProbability = 0.05;
        this.isInfected = false;
        this.isFutureSuperSpreader = false;
        this.age = 0;
        this.isVaccinated = false;
        this.isImmune = false;
        this.isSuperSpreader = false;
        this.incubationPeriodHours = 0;
        this.infectiousPeriodHours = Integer.MAX_VALUE;
        this.viralEmissionRate = 0.0;
    }

}