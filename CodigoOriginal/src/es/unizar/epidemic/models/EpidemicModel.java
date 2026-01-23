package es.unizar.epidemic.models;

import es.unizar.epidemic.contact.ContactRecord;
import es.unizar.gui.simulation.User;
import java.util.List;

/**
 * Interface for epidemic models
 * 
 * @author Nacho Palacio
 */
public interface EpidemicModel {
    double calculateTransmissionProbability(User infectious, User susceptible, ContactRecord contact);
    void updateHealthStates(List<User> users, int currentDay);
    String getModelName();
    PengParameters getParameters();
    void setParameters(PengParameters parameters);
}