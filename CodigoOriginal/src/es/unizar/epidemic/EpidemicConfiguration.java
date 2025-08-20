package es.unizar.epidemic;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public class EpidemicConfiguration {
    
    // Simulation
    private int initialInfectedUsers = 1;
    private boolean enableContactTracing = true;
    private String selectedModel = "SIMPLE_PROXIMITY";
    
    // Intervention parameters
    private double maskComplianceRate = 0.5;
    private double socialDistancingCompliance = 0.0;
    private boolean enableVaccination = false;

    private double maxTransmissionDistance = 3.0;     // meters  
    private double baseTransmissionProbability = 0.15; // 15%
    private int minContactDuration = 5;  
    
    private static EpidemicConfiguration instance;
    
    public static EpidemicConfiguration getInstance() {
        if (instance == null) {
            instance = new EpidemicConfiguration();
        }
        return instance;
    }

    // GETTERS
    public int getInitialInfectedUsers() {
        return initialInfectedUsers;
    }

    public boolean isEnableContactTracing() {
        return enableContactTracing;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public double getMaskComplianceRate() {
        return maskComplianceRate;
    }

    public double getSocialDistancingCompliance() {
        return socialDistancingCompliance;
    }

    public boolean isEnableVaccination() {
        return enableVaccination;
    }

    public double getMaxTransmissionDistance() {
        return maxTransmissionDistance;
    }

    public double getBaseTransmissionProbability() {
        return baseTransmissionProbability;
    }

    public int getMinContactDuration() {
        return minContactDuration;
    }

    /**
     * Gets the configured pixels per meter
     */
    public double getPixelsPerMeter() {
        return es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    /**
     * Checks if simulation configuration is available
     */
    public boolean isPixelConversionAvailable() {
        return es.unizar.gui.Configuration.simulation != null;
    }

    /**
     * Gets distance threshold in pixels
     */
    public double getDistanceThresholdInPixels(double distanceInMeters) {
        return distanceInMeters * getPixelsPerMeter();
    }


    // SETTERS
    public void setInitialInfectedUsers(int initialInfectedUsers) {
        this.initialInfectedUsers = initialInfectedUsers;
    }

    public void setEnableContactTracing(boolean enableContactTracing) {
        this.enableContactTracing = enableContactTracing;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public void setMaskComplianceRate(double maskComplianceRate) {
        this.maskComplianceRate = maskComplianceRate;
    }

    public void setSocialDistancingCompliance(double socialDistancingCompliance) {
        this.socialDistancingCompliance = socialDistancingCompliance;
    }

    public void setEnableVaccination(boolean enableVaccination) {
        this.enableVaccination = enableVaccination;
    }

    public void setMaxTransmissionDistance(double maxTransmissionDistance) {
        this.maxTransmissionDistance = maxTransmissionDistance;
    }

    public void setBaseTransmissionProbability(double baseTransmissionProbability) {
        this.baseTransmissionProbability = baseTransmissionProbability;
    }

    public void setMinContactDuration(int minContactDuration) {
        this.minContactDuration = minContactDuration;
    }

}