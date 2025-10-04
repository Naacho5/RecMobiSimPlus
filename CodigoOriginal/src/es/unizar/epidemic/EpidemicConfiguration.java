package es.unizar.epidemic;

/**
 * Añadido por Nacho Palacio 2025-07-09
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

    private double maxTransmissionDistance = 3.0;     // meters  
    private double baseTransmissionProbability = 0.15; // 15%
    private int minContactDuration = 5;  

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

    public double getDefaultVentilationRate() {
        return defaultVentilationRate;
    }

    public double getVirusDecayRate() {
        return virusDecayRate;
    }

    public double getMaskExhalationEfficiency() {
        return maskExhalationEfficiency;
    }

    public double getMaskInhalationEfficiency() {
        return maskInhalationEfficiency;
    }

    public double getQuantaEmissionRate() {
        return quantaEmissionRate;
    }

    public double getBreathingRate() {
        return breathingRate;
    }

    public double getDepositionRate() {
        return depositionRate;
    }

    public double getViralLoadHigh() {
        return viralLoadHigh;
    }

    public double getViralLoadSuper() {
        return viralLoadSuper;
    }

    public double getInfectiousDose() {
        return infectiousDose;
    }

    public double getDepositionProbability() {
        return depositionProbability;
    }

    public String getConfigName() {
        return configName;
    }
    
    public double getImmunePopulationFraction() {
        return immunePopulationFraction;
    }
    
    public double getSuperSpreaderProbability() {
        return superSpreaderProbability;
    }

    public int getSimulationDuration() {
        return simulationDurationMinutes;
    }
    
    public int getSimulationDurationSeconds() {
        return simulationDurationMinutes * 60;
    }

    public int getFinalInfectedUsers() {
        return finalInfectedUsers;
    }

    public int getTotalUsers() {
        return totalUsers;
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

    public void setDefaultVentilationRate(double defaultVentilationRate) {
        this.defaultVentilationRate = defaultVentilationRate;
    }

    public void setVirusDecayRate(double virusDecayRate) {
        this.virusDecayRate = virusDecayRate;
    }

    public void setMaskExhalationEfficiency(double maskExhalationEfficiency) {
        this.maskExhalationEfficiency = maskExhalationEfficiency;
    }

    public void setMaskInhalationEfficiency(double maskInhalationEfficiency) {
        this.maskInhalationEfficiency = maskInhalationEfficiency;
    }

    public void setQuantaEmissionRate(double quantaEmissionRate) {
        this.quantaEmissionRate = quantaEmissionRate;
    }

    public void setBreathingRate(double breathingRate) {
        this.breathingRate = breathingRate;
    }

    public void setDepositionRate(double depositionRate) {
        this.depositionRate = depositionRate;
    }

    public void setViralLoadHigh(double viralLoadHigh) {
        this.viralLoadHigh = viralLoadHigh;
    }

    public void setViralLoadSuper(double viralLoadSuper) {
        this.viralLoadSuper = viralLoadSuper;
    }

    public void setInfectiousDose(double infectiousDose) {
        this.infectiousDose = infectiousDose;
    }

    public void setDepositionProbability(double depositionProbability) {
        this.depositionProbability = depositionProbability;
    }

    public static void setInstance(EpidemicConfiguration config) {
        instance = config;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }
    
    public void setImmunePopulationFraction(double immunePopulationFraction) {
        this.immunePopulationFraction = immunePopulationFraction;
    }
    
    public void setSuperSpreaderProbability(double superSpreaderProbability) {
        this.superSpreaderProbability = superSpreaderProbability;
    }

    public void setSimulationDuration(int minutes) {
        this.simulationDurationMinutes = minutes;
    }

    public void setSimulationDurationSeconds(int seconds) {
        this.simulationDurationMinutes = seconds / 60;
    }
    
    public void setFinalInfectedUsers(int finalInfectedUsers) {
        this.finalInfectedUsers = finalInfectedUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public void printCurrentConfiguration() {
        System.out.println("\n== CONFIGURACIÓN EPIDÉMICA ACTUAL ===");
        System.out.println("Modelo seleccionado: " + selectedModel);
        System.out.println("Usuarios inicialmente infectados: " + initialInfectedUsers);
        System.out.println("Tasa cumplimiento mascarillas: " + maskComplianceRate);
        System.out.println("Ventilación por defecto: " + defaultVentilationRate);
        System.out.println("Tasa decaimiento virus: " + virusDecayRate);
        System.out.println("Eficiencia mascarilla exhalación: " + maskExhalationEfficiency);
        System.out.println("Eficiencia mascarilla inhalación: " + maskInhalationEfficiency);
        
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
        
        return cloned;
    }

}