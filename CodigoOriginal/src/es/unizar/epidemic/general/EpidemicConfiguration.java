package es.unizar.epidemic.general;

/**
 * Singleton class that holds the configuration parameters for the epidemic simulation
 * 
 * @author Nacho Palacio
 */
public class EpidemicConfiguration {
    
    // Simulation
    private int initialInfectedUsers = 1;
    private boolean enableContactTracing = true;
    private String selectedModel = "SIMPLE_PROXIMITY";
    private int simulationDurationMinutes = 7;
    private int finalInfectedUsers = 1;
    private int totalUsers = 100; 
    
    // Intervention parameters
    private double maskComplianceRate = 0.5;
    private double socialDistancingCompliance = 0.0;
    private boolean enableVaccination = false;

    private double maxTransmissionDistance = 6.5;     // meters  
    private double baseTransmissionProbability = 0.1; // 7%
    private int minContactDuration = 300;              // seconds

    private double defaultVentilationRate = 3.0;          // h⁻¹
    private double virusDecayRate = 0.62;                 // h⁻¹  
    private double maskExhalationEfficiency = 0.5;        // 0-1
    private double maskInhalationEfficiency = 0.3;        // 0-1

    private String configName = "Default";
    private double immunePopulationFraction = 0.0;
    private double superSpreaderProbability = 0.05;

    // Peng
    private double quantaEmissionRate = 60.45;            // quanta/h
    private double breathingRate = 0.72;                  // m³/h
    private double depositionRate = 0.3;                  // h⁻¹

    // Lelieveld
    private double viralLoadHigh = 1.5E6;                 // copies/cm³
    private double viralLoadSuper = 5E9;                  // copies/cm³
    private double infectiousDose = 316;                  // copies
    private double depositionProbability = 0.5;          // 0-1
    
    private static EpidemicConfiguration instance;
    
    /**
     * Gets the singleton instance of EpidemicConfiguration.
     * Creates a new instance if it doesn't exist yet.
     * 
     * @return the singleton instance
     */
    public static EpidemicConfiguration getInstance() {
        if (instance == null) {
            instance = new EpidemicConfiguration();
        }
        return instance;
    }

    // GETTERS
    /**
     * Gets the number of initially infected users in the simulation.
     * 
     * @return number of initial infected users
     */
    public int getInitialInfectedUsers() {
        return initialInfectedUsers;
    }

    /**
     * Checks if contact tracing is enabled in the simulation.
     * 
     * @return true if contact tracing is enabled, false otherwise
     */
    public boolean isEnableContactTracing() {
        return enableContactTracing;
    }

    /**
     * Gets the selected epidemic transmission model.
     * 
     * @return model name (e.g., "SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD")
     */
    public String getSelectedModel() {
        return selectedModel;
    }

    /**
     * Gets the mask compliance rate in the population.
     * 
     * @return compliance rate (0.0 to 1.0)
     */
    public double getMaskComplianceRate() {
        return maskComplianceRate;
    }

    /**
     * Gets the social distancing compliance rate.
     * 
     * @return compliance rate (0.0 to 1.0)
     */
    public double getSocialDistancingCompliance() {
        return socialDistancingCompliance;
    }

    /**
     * Checks if vaccination is enabled in the simulation.
     * 
     * @return true if vaccination is enabled, false otherwise
     */
    public boolean isEnableVaccination() {
        return enableVaccination;
    }

    /**
     * Gets the maximum distance for disease transmission.
     * 
     * @return maximum transmission distance in meters
     */
    public double getMaxTransmissionDistance() {
        return maxTransmissionDistance;
    }

    /**
     * Gets the base probability of disease transmission per contact.
     * 
     * @return base transmission probability (0.0 to 1.0)
     */
    public double getBaseTransmissionProbability() {
        return baseTransmissionProbability;
    }

    /**
     * Gets the minimum contact duration required for potential transmission.
     * 
     * @return minimum duration in seconds
     */
    public int getMinContactDuration() {
        return minContactDuration;
    }

    /**
     * Gets the configured pixels per meter
     * 
     * @return pixels per meter
     */
    public double getPixelsPerMeter() {
        return es.unizar.gui.Configuration.getPixelsPerMeter();
    }

    /**
     * Checks if simulation configuration is available
     * 
     * @return true if available, false otherwise
     */
    public boolean isPixelConversionAvailable() {
        return es.unizar.gui.Configuration.simulation != null;
    }

    /**
     * Gets distance threshold in pixels
     * 
     * @param distanceInMeters distance in meters
     */
    public double getDistanceThresholdInPixels(double distanceInMeters) {
        return distanceInMeters * getPixelsPerMeter();
    }

    /**
     * Gets the default ventilation rate for rooms.
     * 
     * @return ventilation rate in air changes per hour (h⁻¹)
     */
    public double getDefaultVentilationRate() {
        return defaultVentilationRate;
    }

    /**
     * Gets the virus decay rate in the air.
     * 
     * @return decay rate per hour (h⁻¹)
     */
    public double getVirusDecayRate() {
        return virusDecayRate;
    }

    /**
     * Gets the mask efficiency for filtering exhaled particles.
     * 
     * @return efficiency value (0.0 to 1.0)
     */
    public double getMaskExhalationEfficiency() {
        return maskExhalationEfficiency;
    }

    /**
     * Gets the mask efficiency for filtering inhaled particles.
     * 
     * @return efficiency value (0.0 to 1.0)
     */
    public double getMaskInhalationEfficiency() {
        return maskInhalationEfficiency;
    }

    /**
     * Gets the quanta emission rate for the Peng aerosol model.
     * 
     * @return emission rate in quanta per hour
     */
    public double getQuantaEmissionRate() {
        return quantaEmissionRate;
    }

    /**
     * Gets the breathing rate for the Peng aerosol model.
     * 
     * @return breathing rate in cubic meters per hour (m³/h)
     */
    public double getBreathingRate() {
        return breathingRate;
    }

    /**
     * Gets the particle deposition rate for the Peng aerosol model.
     * 
     * @return deposition rate per hour (h⁻¹)
     */
    public double getDepositionRate() {
        return depositionRate;
    }

    /**
     * Gets the high viral load threshold for the Lelieveld aerosol model.
     * 
     * @return viral load in copies per cubic centimeter
     */
    public double getViralLoadHigh() {
        return viralLoadHigh;
    }

    /**
     * Gets the super-spreader viral load threshold for the Lelieveld aerosol model.
     * 
     * @return viral load in copies per cubic centimeter
     */
    public double getViralLoadSuper() {
        return viralLoadSuper;
    }

    /**
     * Gets the infectious dose required for infection in the Lelieveld model.
     * 
     * @return infectious dose in viral copies
     */
    public double getInfectiousDose() {
        return infectiousDose;
    }

    /**
     * Gets the probability of viral particle deposition in the respiratory tract.
     * 
     * @return deposition probability (0.0 to 1.0)
     */
    public double getDepositionProbability() {
        return depositionProbability;
    }

    /**
     * Gets the name of this configuration.
     * 
     * @return configuration name
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * Gets the fraction of the population that is immune.
     * 
     * @return immune population fraction (0.0 to 1.0)
     */
    public double getImmunePopulationFraction() {
        return immunePopulationFraction;
    }
    
    /**
     * Gets the probability that an infected user is a super-spreader.
     * 
     * @return super-spreader probability (0.0 to 1.0)
     */
    public double getSuperSpreaderProbability() {
        return superSpreaderProbability;
    }

    /**
     * Gets the simulation duration in minutes.
     * 
     * @return duration in minutes
     */
    public int getSimulationDuration() {
        return simulationDurationMinutes;
    }
    
    /**
     * Gets the simulation duration in seconds.
     * 
     * @return duration in seconds
     */
    public int getSimulationDurationSeconds() {
        return simulationDurationMinutes * 60;
    }

    /**
     * Gets the final count of infected users.
     * 
     * @return number of final infected users
     */
    public int getFinalInfectedUsers() {
        return finalInfectedUsers;
    }

    /**
     * Gets the total number of users in the simulation.
     * 
     * @return total user count
     */
    public int getTotalUsers() {
        return totalUsers;
    }


    // SETTERS
    /**
     * Sets the number of initially infected users.
     * 
     * @param initialInfectedUsers number of initial infected users
     */
    public void setInitialInfectedUsers(int initialInfectedUsers) {
        this.initialInfectedUsers = initialInfectedUsers;
    }

    /**
     * Sets whether contact tracing is enabled.
     * 
     * @param enableContactTracing true to enable contact tracing, false otherwise
     */
    public void setEnableContactTracing(boolean enableContactTracing) {
        this.enableContactTracing = enableContactTracing;
    }

    /**
     * Sets the epidemic transmission model to use.
     * 
     * @param selectedModel model name (e.g., "SIMPLE_PROXIMITY", "AEROSOL_PENG", "AEROSOL_LELIEVELD")
     */
    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    /**
     * Sets the mask compliance rate in the population.
     * 
     * @param maskComplianceRate compliance rate (0.0 to 1.0)
     */
    public void setMaskComplianceRate(double maskComplianceRate) {
        this.maskComplianceRate = maskComplianceRate;
    }

    /**
     * Sets the social distancing compliance rate.
     * 
     * @param socialDistancingCompliance compliance rate (0.0 to 1.0)
     */
    public void setSocialDistancingCompliance(double socialDistancingCompliance) {
        this.socialDistancingCompliance = socialDistancingCompliance;
    }

    /**
     * Sets whether vaccination is enabled.
     * 
     * @param enableVaccination true to enable vaccination, false otherwise
     */
    public void setEnableVaccination(boolean enableVaccination) {
        this.enableVaccination = enableVaccination;
    }

    /**
     * Sets the maximum distance for disease transmission.
     * 
     * @param maxTransmissionDistance maximum transmission distance in meters
     */
    public void setMaxTransmissionDistance(double maxTransmissionDistance) {
        this.maxTransmissionDistance = maxTransmissionDistance;
    }

    /**
     * Sets the base probability of disease transmission per contact.
     * 
     * @param baseTransmissionProbability base transmission probability (0.0 to 1.0)
     */
    public void setBaseTransmissionProbability(double baseTransmissionProbability) {
        this.baseTransmissionProbability = baseTransmissionProbability;
    }

    /**
     * Sets the minimum contact duration required for potential transmission.
     * 
     * @param minContactDuration minimum duration in seconds
     */
    public void setMinContactDuration(int minContactDuration) {
        this.minContactDuration = minContactDuration;
    }

    /**
     * Sets the default ventilation rate for rooms.
     * 
     * @param defaultVentilationRate ventilation rate in air changes per hour (h⁻¹)
     */
    public void setDefaultVentilationRate(double defaultVentilationRate) {
        this.defaultVentilationRate = defaultVentilationRate;
    }

    /**
     * Sets the virus decay rate in the air.
     * 
     * @param virusDecayRate decay rate per hour (h⁻¹)
     */
    public void setVirusDecayRate(double virusDecayRate) {
        this.virusDecayRate = virusDecayRate;
    }

    /**
     * Sets the mask efficiency for filtering exhaled particles.
     * 
     * @param maskExhalationEfficiency efficiency value (0.0 to 1.0)
     */
    public void setMaskExhalationEfficiency(double maskExhalationEfficiency) {
        this.maskExhalationEfficiency = maskExhalationEfficiency;
    }

    /**
     * Sets the mask efficiency for filtering inhaled particles.
     * 
     * @param maskInhalationEfficiency efficiency value (0.0 to 1.0)
     */
    public void setMaskInhalationEfficiency(double maskInhalationEfficiency) {
        this.maskInhalationEfficiency = maskInhalationEfficiency;
    }

    /**
     * Sets the quanta emission rate for the Peng aerosol model.
     * 
     * @param quantaEmissionRate emission rate in quanta per hour
     */
    public void setQuantaEmissionRate(double quantaEmissionRate) {
        this.quantaEmissionRate = quantaEmissionRate;
    }

    /**
     * Sets the breathing rate for the Peng aerosol model.
     * 
     * @param breathingRate breathing rate in cubic meters per hour (m³/h)
     */
    public void setBreathingRate(double breathingRate) {
        this.breathingRate = breathingRate;
    }

    /**
     * Sets the particle deposition rate for the Peng aerosol model.
     * 
     * @param depositionRate deposition rate per hour (h⁻¹)
     */
    public void setDepositionRate(double depositionRate) {
        this.depositionRate = depositionRate;
    }

    /**
     * Sets the high viral load threshold for the Lelieveld aerosol model.
     * 
     * @param viralLoadHigh viral load in copies per cubic centimeter
     */
    public void setViralLoadHigh(double viralLoadHigh) {
        this.viralLoadHigh = viralLoadHigh;
    }

    /**
     * Sets the super-spreader viral load threshold for the Lelieveld aerosol model.
     * 
     * @param viralLoadSuper viral load in copies per cubic centimeter
     */
    public void setViralLoadSuper(double viralLoadSuper) {
        this.viralLoadSuper = viralLoadSuper;
    }

    /**
     * Sets the infectious dose required for infection in the Lelieveld model.
     * 
     * @param infectiousDose infectious dose in viral copies
     */
    public void setInfectiousDose(double infectiousDose) {
        this.infectiousDose = infectiousDose;
    }

    /**
     * Sets the probability of viral particle deposition in the respiratory tract.
     * 
     * @param depositionProbability deposition probability (0.0 to 1.0)
     */
    public void setDepositionProbability(double depositionProbability) {
        this.depositionProbability = depositionProbability;
    }

    /**
     * Sets the singleton instance of EpidemicConfiguration.
     * Used for replacing the current configuration with a new one.
     * 
     * @param config the new configuration instance
     */
    public static void setInstance(EpidemicConfiguration config) {
        instance = config;
    }

    /**
     * Sets the name of this configuration.
     * 
     * @param configName configuration name
     */
    public void setConfigName(String configName) {
        this.configName = configName;
    }
    
    /**
     * Sets the fraction of the population that is immune.
     * 
     * @param immunePopulationFraction immune population fraction (0.0 to 1.0)
     */
    public void setImmunePopulationFraction(double immunePopulationFraction) {
        this.immunePopulationFraction = immunePopulationFraction;
    }
    
    /**
     * Sets the probability that an infected user is a super-spreader.
     * 
     * @param superSpreaderProbability super-spreader probability (0.0 to 1.0)
     */
    public void setSuperSpreaderProbability(double superSpreaderProbability) {
        this.superSpreaderProbability = superSpreaderProbability;
    }

    /**
     * Sets the simulation duration in minutes.
     * 
     * @param minutes duration in minutes
     */
    public void setSimulationDuration(int minutes) {
        this.simulationDurationMinutes = minutes;
    }

    /**
     * Sets the simulation duration in seconds.
     * Converts seconds to minutes internally.
     * 
     * @param seconds duration in seconds
     */
    public void setSimulationDurationSeconds(int seconds) {
        this.simulationDurationMinutes = seconds / 60;
    }
    
    /**
     * Sets the final count of infected users.
     * 
     * @param finalInfectedUsers number of final infected users
     */
    public void setFinalInfectedUsers(int finalInfectedUsers) {
        this.finalInfectedUsers = finalInfectedUsers;
    }

    /**
     * Sets the total number of users in the simulation.
     * 
     * @param totalUsers total user count
     */
    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    /**
     * Prints the current epidemic configuration to console.
     * Displays all relevant parameters including model-specific settings.
     */
    public void printCurrentConfiguration() {
        System.out.println("\n== CONFIGURACIÓN EPIDÉMICA ACTUAL ===");
        System.out.println("Modelo seleccionado: " + selectedModel);
        System.out.println("Usuarios inicialmente infectados: " + initialInfectedUsers);
        System.out.println("Tasa cumplimiento mascarillas: " + maskComplianceRate);
        System.out.println("Ventilación por defecto: " + defaultVentilationRate);
        System.out.println("Tasa decaimiento virus: " + virusDecayRate);
        System.out.println("Eficiencia mascarilla exhalación: " + maskExhalationEfficiency);
        System.out.println("Eficiencia mascarilla inhalación: " + maskInhalationEfficiency);
        System.out.println("Total Users: " + totalUsers);
        System.out.println("Duración simulación (minutos): " + simulationDurationMinutes);
        
        if ("SIMPLE_PROXIMITY".equals(selectedModel)) {
            System.out.println("--- Parámetros Simple Proximity ---");
            System.out.println("Distancia máxima transmisión: " + maxTransmissionDistance);
            System.out.println("Probabilidad base transmisión: " + baseTransmissionProbability);
            System.out.println("Duración mínima contacto: " + minContactDuration);
        } else if ("AEROSOL_PENG".equals(selectedModel)) {
            System.out.println("--- Parámetros Aerosol Peng ---");
            System.out.println("Tasa emisión quanta: " + quantaEmissionRate);
            System.out.println("Tasa respiración: " + breathingRate);
            System.out.println("Tasa deposición: " + depositionRate);
        } else if ("AEROSOL_LELIEVELD".equals(selectedModel)) {
            System.out.println("--- Parámetros Aerosol Lelieveld ---");
            System.out.println("Carga viral alta: " + viralLoadHigh);
            System.out.println("Carga viral super: " + viralLoadSuper);
            System.out.println("Dosis infecciosa: " + infectiousDose);
            System.out.println("Probabilidad deposición: " + depositionProbability);
        }
        System.out.println("=== FIN CONFIGURACIÓN EPIDÉMICA ===\n");
    }

    /**
     * Creates a deep copy of this configuration.
     * All field values are copied to a new instance.
     * 
     * @return a new EpidemicConfiguration with identical values
     */
    public EpidemicConfiguration clone() {
        EpidemicConfiguration cloned = new EpidemicConfiguration();
        
        cloned.configName = this.configName;
        cloned.initialInfectedUsers = this.initialInfectedUsers;
        cloned.enableContactTracing = this.enableContactTracing;
        cloned.selectedModel = this.selectedModel;
        cloned.maskComplianceRate = this.maskComplianceRate;
        cloned.socialDistancingCompliance = this.socialDistancingCompliance;
        cloned.enableVaccination = this.enableVaccination;
        cloned.maxTransmissionDistance = this.maxTransmissionDistance;
        cloned.baseTransmissionProbability = this.baseTransmissionProbability;
        cloned.minContactDuration = this.minContactDuration;
        cloned.defaultVentilationRate = this.defaultVentilationRate;
        cloned.virusDecayRate = this.virusDecayRate;
        cloned.maskExhalationEfficiency = this.maskExhalationEfficiency;
        cloned.maskInhalationEfficiency = this.maskInhalationEfficiency;
        cloned.quantaEmissionRate = this.quantaEmissionRate;
        cloned.breathingRate = this.breathingRate;
        cloned.depositionRate = this.depositionRate;
        cloned.viralLoadHigh = this.viralLoadHigh;
        cloned.viralLoadSuper = this.viralLoadSuper;
        cloned.infectiousDose = this.infectiousDose;
        cloned.depositionProbability = this.depositionProbability;
        cloned.immunePopulationFraction = this.immunePopulationFraction;
        cloned.superSpreaderProbability = this.superSpreaderProbability;
        cloned.simulationDurationMinutes = this.simulationDurationMinutes;
        cloned.totalUsers = this.totalUsers;
        cloned.finalInfectedUsers = this.finalInfectedUsers;
        
        return cloned;
    }

    /**
     * Resets all epidemic parameters to default values
     */
    public void resetToDefaults() {
        this.selectedModel = "SIMPLE_PROXIMITY";
        this.initialInfectedUsers = 2;
        this.maskComplianceRate = 0.3;
        this.defaultVentilationRate = 3.0;
        this.virusDecayRate = 0.62;
        this.maskExhalationEfficiency = 0.5;
        this.maskInhalationEfficiency = 0.3;
        this.simulationDurationMinutes = 7;
        this.immunePopulationFraction = 0.0;
        this.superSpreaderProbability = 0.05;
        
        // Modelo específico: SIMPLE_PROXIMITY
        this.maxTransmissionDistance = 1.5;
        this.baseTransmissionProbability = 0.01;
        this.minContactDuration = 900;
        
        // Modelo específico: PENG
        this.quantaEmissionRate = 232.5;
        this.breathingRate = 0.72;
        this.depositionRate = 0.3;
        
        // Modelo específico: LELIEVELD
        this.viralLoadHigh = 1.5E7;
        this.viralLoadSuper = 5E9;
        this.infectiousDose = 316;
        this.depositionProbability = 0.5;
        
        System.out.println("   EpidemicConfiguration reset to default values.");
    }

}