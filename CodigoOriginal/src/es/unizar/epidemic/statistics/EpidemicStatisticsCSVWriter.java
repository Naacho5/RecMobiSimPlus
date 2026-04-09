package es.unizar.epidemic.statistics;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Class to write epidemic statistics to CSV files
 * 
 * @author Nacho Palacio
 */
public class EpidemicStatisticsCSVWriter {
    
    private static final String OUTPUT_DIR = "epidemic_statistics_csv";
    private static final String AEROSOL_CSV = "aerosol_measurements.csv";
    private static final String STATE_HISTORY_CSV = "health_state_history.csv";
    private static final String CONTACTS_CSV = "contacts.csv";
    
    private CSVWriter aerosolWriter;
    private CSVWriter stateHistoryWriter;
    private CSVWriter contactsWriter;
    
    private boolean isInitialized = false;
    
    public EpidemicStatisticsCSVWriter() {
        initializeWriters();
    }
    
    /**
     * Initializes the CSV writers
     */
    private void initializeWriters() {
        try {
            java.nio.file.Files.createDirectories(Paths.get(OUTPUT_DIR));
            
            // Writer for aerosol measurements
            aerosolWriter = new CSVWriter(
                new FileWriter(Paths.get(OUTPUT_DIR, AEROSOL_CSV).toString(), false),
                ',',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
            );
            // Header
            aerosolWriter.writeNext(new String[]{"timestamp", "roomId", "concentration", "durationSeconds"});
            aerosolWriter.flush();
            
            // Writer for health state snapshots
            stateHistoryWriter = new CSVWriter(
                new FileWriter(Paths.get(OUTPUT_DIR, STATE_HISTORY_CSV).toString(), false),
                ',',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
            );
            // Header
            stateHistoryWriter.writeNext(new String[]{"timestamp", "susceptible", "infectious", "recovered"});
            stateHistoryWriter.flush();
            
            // Writer for contacts
            contactsWriter = new CSVWriter(
                new FileWriter(Paths.get(OUTPUT_DIR, CONTACTS_CSV).toString(), false),
                ',',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
            );
            // Header
            contactsWriter.writeNext(new String[]{"timestamp", "user1", "user2", "duration", "roomId", "isInfectious"});
            contactsWriter.flush();
            
            isInitialized = true;
            
        } catch (IOException e) {
            System.err.println("[EpidemicStatisticsCSVWriter] Error initializing writers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Records an aerosol measurement
     * 
     * @param timestamp timestamp of the measurement
     * @param roomId ID of the room
     * @param concentration aerosol concentration
     * @param durationSeconds duration of the measurement in seconds
     */
    public void recordAerosolMeasurement(long timestamp, int roomId, double concentration, double durationSeconds) {
        if (!isInitialized) return;
        try {
            aerosolWriter.writeNext(new String[]{
                String.valueOf(timestamp),
                String.valueOf(roomId),
                String.format("%.6e", concentration),
                String.valueOf(durationSeconds)
            });
            // Flush periodically to avoid data loss
            if (System.currentTimeMillis() % 1000 < 100) {
                aerosolWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[EpidemicStatisticsCSVWriter] Error writing aerosol measurement: " + e.getMessage());
        }
    }
    
    /**
     * Records a health state snapshot
     * 
     * @param timestamp timestamp of the snapshot
     * @param susceptible number of susceptible users
     * @param infectious number of infectious users
     * @param recovered number of recovered users
     */
    public void recordHealthStateSnapshot(long timestamp, int susceptible, int infectious, int recovered) {
        if (!isInitialized) return;
        try {
            stateHistoryWriter.writeNext(new String[]{
                String.valueOf(timestamp),
                String.valueOf(susceptible),
                String.valueOf(infectious),
                String.valueOf(recovered)
            });
            if (System.currentTimeMillis() % 1000 < 100) {
                stateHistoryWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[EpidemicStatisticsCSVWriter] Error writing health snapshot: " + e.getMessage());
        }
    }
    
    /**
     * Records a contact
     * 
     * @param timestamp timestamp of the contact
     * @param user1 ID of the first user
     * @param user2 ID of the second user
     * @param duration duration of the contact
     * @param roomId ID of the room where the contact occurred
     * @param isInfectious whether at least one user was infectious
     */
    public void recordContact(long timestamp, int user1, int user2, double duration, int roomId, boolean isInfectious) {
        if (!isInitialized) return;
        try {
            contactsWriter.writeNext(new String[]{
                String.valueOf(timestamp),
                String.valueOf(user1),
                String.valueOf(user2),
                String.valueOf(duration),
                String.valueOf(roomId),
                String.valueOf(isInfectious ? 1 : 0)
            });
            if (System.currentTimeMillis() % 1000 < 100) {
                contactsWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[EpidemicStatisticsCSVWriter] Error writing contact: " + e.getMessage());
        }
    }
    
    /**
     * Closes all writers (call at the end of the simulation)
     */
    public void close() {
        try {
            if (aerosolWriter != null) aerosolWriter.close();
            if (stateHistoryWriter != null) stateHistoryWriter.close();
            if (contactsWriter != null) contactsWriter.close();
        } catch (IOException e) {
            System.err.println("[EpidemicStatisticsCSVWriter] Error closing writers: " + e.getMessage());
        }
    }
}
