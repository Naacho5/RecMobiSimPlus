package es.unizar.epidemic.contact;

/**
 * Enumeration of different contact types
 * 
 * @author Nacho Palacio
 */
public enum ContactType {
    // Planified at the beggining of the project but unused
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
    
    /**
     * Gets the description of the contact type
     * 
     * @return Description of the contact type
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the base probability of transmission for this contact type
     * 
     * @return Base probability of transmission
     */
    public double getBaseProbability() {
        return baseProbability;
    }
    
}