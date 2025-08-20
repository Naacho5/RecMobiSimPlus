package es.unizar.epidemic;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public enum ContactType {
    HOUSEHOLD("Domicilio", 0.15),
    WORKPLACE("Trabajo", 0.10),
    SOCIAL("Social", 0.08),
    COMMUNITY("Comunitario", 0.05),
    UNKNOWN("Desconocido", 0.03);
    
    private final String description;
    private final double baseProbability;
    
    ContactType(String description, double baseProbability) {
        this.description = description;
        this.baseProbability = baseProbability;
    }
    
    
    public String getDescription() {
        return description;
    }

    public double getBaseProbability() {
        return baseProbability;
    }
    
}