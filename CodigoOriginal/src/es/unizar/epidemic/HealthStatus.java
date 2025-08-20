package es.unizar.epidemic;

// Añadido por Nacho Palacio 2025-07-09
public enum HealthStatus {
    SUSCEPTIBLE("Susceptible", "Verde"),
    EXPOSED("Expuesto", "Amarillo"), 
    INFECTIOUS_ASYMPTOMATIC("Infectado Asintomático", "Naranja"),
    INFECTIOUS_SYMPTOMATIC("Infectado Sintomático", "Rojo"),
    SUPER_SPREADER("Superdiseminador", "Morado"); // Añadido para modelo 2
    
    private final String description;
    private final String color;
    
    HealthStatus(String description, String color) {
        this.description = description;
        this.color = color;
    }
    
    public String getDescription() { 
        return description; 
    }

    public String getColor() { 
        return color; 
    }
}
