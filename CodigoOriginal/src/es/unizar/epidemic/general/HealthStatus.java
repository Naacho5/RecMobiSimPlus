package es.unizar.epidemic.general;

/**
 * Enumeration of different health statuses
 *
 * @author Nacho Palacio
 */
public enum HealthStatus {
    SUSCEPTIBLE("Susceptible", "Verde"),
    INFECTIOUS_SYMPTOMATIC("Infectado Sintomático", "Rojo"),
    SUPER_SPREADER("SuperSpreader", "Morado"); // Único de Lelieveld
    
    private final String description;
    private final String color;
    
    HealthStatus(String description, String color) {
        this.description = description;
        this.color = color;
    }
    
    /**
     * Gets the description of the health status
     * 
     * @return Description of the health status
     */
    public String getDescription() { 
        return description; 
    }

    /**
     * Gets the color associated with the health status
     * 
     * @return Color associated with the health status
     */
    public String getColor() { 
        return color; 
    }
}
