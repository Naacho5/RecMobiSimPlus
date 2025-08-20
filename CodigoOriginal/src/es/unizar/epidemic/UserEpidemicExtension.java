package es.unizar.epidemic;

import es.unizar.epidemic.HealthStatus;
import es.unizar.gui.simulation.User;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class UserEpidemicExtension {
    
    private HealthStatus healthStatus = HealthStatus.SUSCEPTIBLE;
    private int hoursSinceInfection = 0;
    private boolean isMaskWearing = false;
    private double transmissionProbability = 0.05;
    
    // User attributes
    private int age;
    private boolean isVaccinated = false;

    // REVISAR ESTOS VALORES
    private int incubationPeriodHours = 0;
    private int infectiousPeriodHours = 10;

    private double viralEmissionRate = 0.0;
    
    // GETTERS
    private UserEpidemicExtension getUserEpidemicExtension(User user) {
        return user.getEpidemicExtension();
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public int getHoursSinceInfection() {
        return hoursSinceInfection;
    }

    public boolean isMaskWearing() {
        return isMaskWearing;
    }

    public double getTransmissionProbability() {
        return transmissionProbability;
    }

    public int getAge() {
        return age;
    }

    public boolean isVaccinated() {
        return isVaccinated;
    }

    public int getIncubationPeriodHours() {
        return incubationPeriodHours;
    }

    public int getInfectiousPeriodHours() {
        return infectiousPeriodHours;
    }

    public double getViralEmissionRate() {
        return viralEmissionRate;
    }


    // SETTERS
    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public void setHoursSinceInfection(int hoursSinceInfection) {
        this.hoursSinceInfection = hoursSinceInfection;
    }

    public void setMaskWearing(boolean isMaskWearing) {
        this.isMaskWearing = isMaskWearing;
    }

    public void setTransmissionProbability(double transmissionProbability) {
        this.transmissionProbability = transmissionProbability;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setVaccinated(boolean isVaccinated) {
        this.isVaccinated = isVaccinated;
    }

    public void setIncubationPeriod(int incubationPeriodHours) {
        this.incubationPeriodHours = incubationPeriodHours;
    }

    public void setInfectiousPeriod(int infectiousPeriodHours) {
        this.infectiousPeriodHours = infectiousPeriodHours;
    }

    public void setViralEmissionRate(double viralEmissionRate) {
        this.viralEmissionRate = viralEmissionRate;
    }


}