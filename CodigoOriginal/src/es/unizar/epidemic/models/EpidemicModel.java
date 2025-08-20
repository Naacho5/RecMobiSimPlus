package es.unizar.epidemic.models;

import es.unizar.epidemic.ContactRecord;
import es.unizar.gui.simulation.User;
import java.util.List;

/**
 * Añadido por Nacho Palacio 2025-07-09
 */
public interface EpidemicModel {
    double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact);
    void updateHealthStates(List<User> users, int currentDay);
    String getModelName();
    ModelParameters1 getParameters();
    void setParameters(ModelParameters1 parameters);
}