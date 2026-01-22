package es.unizar.epidemic.statistics;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Class to write epidemic iterations data in a binary format
 * 
 * Each record consists of:
 * - int userId (4 bytes)
 * - int roomId (4 bytes)
 * - int iteration (4 bytes)
 * 
 * @author Nacho Palacio
 */
public class EpidemicIterationsBinaryWriter {
    
    private static final String OUTPUT_FILE = "epidemic_iterations.bin";
    private static final int VERSION = 1;
    private static final int RECORD_SIZE = 12; // 4 bytes userId + 4 bytes roomId + 4 bytes iteration
    
    private RandomAccessFile raf;
    private FileChannel channel;
    private long currentOffset;
    private boolean isInitialized = false;
    
    // Record counter per user (for statistics)
    private Map<Integer, Integer> userRecordCount;
    
    public EpidemicIterationsBinaryWriter() {
        userRecordCount = new HashMap<>();
        try {
            // Delete old file
            File file = new File(OUTPUT_FILE);
            if (file.exists()) {
                file.delete();
            }
            
            raf = new RandomAccessFile(OUTPUT_FILE, "rw");
            channel = raf.getChannel();
            
            ByteBuffer header = ByteBuffer.allocate(4);
            header.putInt(VERSION);
            header.flip();
            channel.write(header);
            
            currentOffset = 4; // After header
            isInitialized = true;
            
            System.out.println("[EpidemicIterationsBinaryWriter] Initialized. File: " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("[EpidemicIterationsBinaryWriter] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Records an iteration for a user in a room.
     * Writes a binary record to the file containing the user ID, room ID,
     * and iteration number. Updates internal counters and performs periodic
     * flushing for data persistence.
     * 
     * @param userId the ID of the user
     * @param roomId the ID of the room
     * @param iteration the iteration number to record
     */
    public synchronized void recordIteration(int userId, int roomId, int iteration) {
        if (!isInitialized) return;
        
        try {
            ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
            buffer.putInt(userId);
            buffer.putInt(roomId);
            buffer.putInt(iteration);
            buffer.flip();
            
            // Write at current position
            channel.write(buffer, currentOffset);
            currentOffset += RECORD_SIZE;
            
            // Update record counter per user
            userRecordCount.merge(userId, 1, Integer::sum);
            
            // Periodic flush to avoid data loss
            if (iteration % 1000 == 0) {
                channel.force(false);
            }
        } catch (IOException e) {
            System.err.println("[EpidemicIterationsBinaryWriter] Error writing: " + e.getMessage());
        }
    }
    
    /**
     * Gets all iterations for a specific user in a specific room.
     * Reads through the entire binary file and collects all iteration numbers
     * where the user ID and room ID match the specified values.
     * 
     * @param userId the ID of the user
     * @param roomId the ID of the room
     * @return list of iteration numbers for the user in the specified room
     */
    public synchronized List<Integer> getIterations(int userId, int roomId) {
        if (!isInitialized) {
            return new ArrayList<>();
        }
        
        List<Integer> result = new ArrayList<>();
        
        try {
            channel.force(false);
            
            // Read from beginning (after header)
            long position = 4; // After VERSION header
            ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
            
            while (position < currentOffset) {
                buffer.clear();
                int bytesRead = channel.read(buffer, position);
                
                if (bytesRead < RECORD_SIZE) {
                    break;
                }
                
                buffer.flip();
                int uId = buffer.getInt();
                int rId = buffer.getInt();
                int iteration = buffer.getInt();
                
                // Add iteration if user and room match
                if (uId == userId && rId == roomId) {
                    result.add(iteration);
                }
                
                position += RECORD_SIZE;
            }
        } catch (IOException e) {
            System.err.println("[EpidemicIterationsBinaryWriter] Error reading: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Gets all iterations for a specific user across all rooms.
     * Reads through the entire binary file and organizes iteration numbers
     * by room ID for the specified user.
     * 
     * @param userId the ID of the user
     * @return map where keys are room IDs and values are lists of iteration numbers
     */
    public synchronized Map<Integer, List<Integer>> getAllIterationsForUser(int userId) {
        if (!isInitialized) {
            return new HashMap<>();
        }
        
        Map<Integer, List<Integer>> result = new HashMap<>();
        
        try {
            channel.force(false);
            
            long position = 4;
            ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
            
            while (position < currentOffset) {
                buffer.clear();
                int bytesRead = channel.read(buffer, position);
                
                if (bytesRead < RECORD_SIZE) {
                    break;
                }
                
                buffer.flip();
                int uId = buffer.getInt();
                int rId = buffer.getInt();
                int iteration = buffer.getInt();
                
                if (uId == userId) {
                    result.computeIfAbsent(rId, k -> new ArrayList<>()).add(iteration);
                }
                
                position += RECORD_SIZE;
            }
        } catch (IOException e) {
            System.err.println("[EpidemicIterationsBinaryWriter] Error reading: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Gets the total number of records written.
     * Calculates the count based on the current file offset and record size.
     * 
     * @return total number of records in the file
     */
    public long getTotalRecords() {
        return (currentOffset - 4) / RECORD_SIZE;
    }
    
    /**
     * Gets the number of records for a specific user.
     * Uses internal counter for efficient lookup without reading the entire file.
     * 
     * @param userId the ID of the user
     * @return number of records written for the specified user
     */
    public int getRecordCountForUser(int userId) {
        return userRecordCount.getOrDefault(userId, 0);
    }
    
    /**
     * Closes the writer and releases resources.
     * Flushes all pending data to disk, prints statistics about the written data,
     * and closes the file channel and random access file.
     */
    public synchronized void close() {
        if (!isInitialized) return;
        
        try {
            channel.force(true);
            
            long totalRecords = getTotalRecords();
            long fileSizeBytes = currentOffset;
            
            System.out.println("[EpidemicIterationsBinaryWriter] Closing file.");
            System.out.println("  Total records: " + totalRecords);
            System.out.println("  Unique users: " + userRecordCount.size());
            System.out.println("  File size: " + fileSizeBytes + " bytes");
            
            channel.close();
            raf.close();
            
            isInitialized = false;
            
            System.out.println("[EpidemicIterationsBinaryWriter] File closed correctly.");
            
        } catch (IOException e) {
            System.err.println("[EpidemicIterationsBinaryWriter] Error closing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the writer is initialized and ready to use.
     * Verifies that the writer is initialized and the file channel is open.
     * 
     * @return true if the writer is ready to write or read data, false otherwise
     */
    public boolean isReady() {
        return isInitialized && channel != null && channel.isOpen();
    }
}