package es.unizar.recommendation;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.util.Literals;

/**
 * Recommendation system that integrates risk factors into the recommendation process from a Python-based recommender.
 * 
 * @author Nacho Palacio
 */
public class RiskAwareRecommendation {

    private final String pythonScriptPath;
    private final boolean considerRisk;

    private static volatile Map<Long, Long> cachedOrigToSeq = null;
    private static volatile Map<Long, Long> cachedSeqToOrig = null;
    private static final Object MAP_CACHE_LOCK = new Object();

    public RiskAwareRecommendation(String pythonScriptPath) {
        this.pythonScriptPath = pythonScriptPath;
        this.considerRisk = true;
    }

    public RiskAwareRecommendation(String pythonScriptPath, boolean considerRisk) {
        this.pythonScriptPath = pythonScriptPath;
        this.considerRisk = considerRisk;
    }

    /**
     * Generates recommendations for a user considering risk factors
     * 
     * @param userId ID of the user to recommend for
     * @param visitedArtworks List of artwork IDs already visited by the user (sequential IDs)
     * @param occupancy Map of artwork ID to current occupancy (only if considerRisk is true)
     * @param duration Map of artwork ID to expected duration of stay (only if considerRisk is true)
     * @param favoriteArtworks List of artwork IDs marked as favorites by the user (sequential IDs)
     * @return List of RecommendedItem objects with recommended artworks (sequential IDs)
     * @throws IOException if there is an error communicating with the Python recommender
     */
    public List<RecommendedItem> recommend(
            int userId,
            List<Long> visitedArtworks,
            Map<Integer, Integer> occupancy,
            Map<Integer, Double> duration,
            List<Long> favoriteArtworks
    ) throws IOException {

        // System.out.println("RiskAwareRecommendation.recommend():");
        // System.out.println("    userId: " + userId);
        
        if (visitedArtworks == null) {
            visitedArtworks = new ArrayList<>();
        }

        Map<Long, Long> origToSeq = getOrigToSeqMap();
        Map<Long, Long> seqToOrig = getSeqToOrigMap();

        // Translate visitedArtworks from sequential IDs to original ones
        List<Long> visitedOriginals = new ArrayList<>();
        for (Long seqId : visitedArtworks) {
            Long origId = seqToOrig.get(seqId);
            if (origId != null) {
                visitedOriginals.add(origId);
            }
        }
        
        Set<Long> visitedOriginalsSet = new HashSet<>(visitedOriginals);

        // System.out.println("[RiskAwareRecommendation] Items already visited (sequential): " + visitedArtworks.size());
        // System.out.println("[RiskAwareRecommendation] Items already visited (original): " + visitedOriginals.size());

        // Translate favoriteArtworks from sequential IDs to original ones
        List<Long> favoriteOriginals = new ArrayList<>();
        for (Long seqId : favoriteArtworks) {
            Long origId = seqToOrig.get(seqId);
            if (origId != null) {
                favoriteOriginals.add(origId);
            }
        }

        // Build input JSON
        Map<String, Object> input = new HashMap<>();
        input.put("user_id", userId);
        input.put("visited", visitedOriginals);
        input.put("favorites", favoriteOriginals);
        input.put("consider_risk", considerRisk);

        if (considerRisk) {
            input.put("occupancy", occupancy);
            input.put("duration", duration);
        }

        Gson gson = new Gson();
        String inputJson = gson.toJson(input);

        // System.out.println("[RiskAwareRecommendation] 📤 Sending to Python:");
        // System.out.println(inputJson);

        // Launch Python process
        ProcessBuilder pb = new ProcessBuilder("python3", pythonScriptPath);
        // pb.environment().put("PYTHONPATH", "/home/nacho/universidad/cuarto/TFG/RecMobiSimPlus/Backups/moma.recommender-main-snapshot20251005/moma.recommender-main/");
        pb.environment().put("PYTHONPATH", "../../Backups/moma.recommender-main-snapshot20251005/moma.recommender-main/");
        // pb.redirectErrorStream(true);
        
        Process process = null;
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        
        try {
            process = pb.start();
            // System.out.println("[RiskAwareRecommendation] 🐍 Python process started.");

            try (OutputStream os = process.getOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                writer.write(inputJson);
                writer.flush();
                // System.out.println("[RiskAwareRecommendation] 📤 JSON sent to Python.");
            } 

            // Read the response
            try (InputStream is = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // System.out.println("[RiskAwareRecommendation] ⏳ Waiting for Python to finish...");
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                System.err.println("⚠️ Python process timeout (30s) - User " + userId);
                process.destroyForcibly();
                throw new IOException("Python process timeout (30s)");
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("[RiskAwareRecommendation] Warning! Python ended with code: " + exitCode);
                throw new IOException("Python process failed with exit code: " + exitCode);
            }

            // System.out.println("[RiskAwareRecommendation] ✅ Python process completed successfully.");

        } 

        // try {
        //     process = pb.start();

        //     final InputStream stdout = process.getInputStream();
        //     final InputStream stderr = process.getErrorStream();

        //     // Escribir input JSON
        //     try (OutputStream os = process.getOutputStream();
        //         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
        //         writer.write(inputJson);
        //         writer.flush();
        //     } 

        //     // ✅ NUEVO: Leer STDOUT y STDERR en hilos separados
        //     Thread stdoutReader = new Thread(() -> {
        //         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
        //             String line;
        //             while ((line = reader.readLine()) != null) {
        //                 output.append(line).append("\n");
        //             }
        //         } catch (IOException e) {
        //             e.printStackTrace();
        //         }
        //     });

        //     Thread stderrReader = new Thread(() -> {
        //         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
        //             String line;
        //             while ((line = reader.readLine()) != null) {
        //                 errorOutput.append(line).append("\n");
        //             }
        //         } catch (IOException e) {
        //             e.printStackTrace();
        //         }
        //     });

        //     stdoutReader.start();
        //     stderrReader.start();

        //     // Esperar a que termine el proceso
        //     int exitCode = process.waitFor();
            
        //     // Esperar a que terminen los threads de lectura
        //     stdoutReader.join(5000);
        //     stderrReader.join(5000);

        //     // ✅ NUEVO: Mostrar STDOUT y STDERR por separado
        //     if (output.length() > 0) {
        //         System.out.println("[RiskAwareRecommendation] 📥 STDOUT from Python:");
        //         System.out.println(output.toString());
        //     }

        //     if (errorOutput.length() > 0) {
        //         System.err.println("[RiskAwareRecommendation] 🔴 STDERR from Python:");
        //         System.err.println(errorOutput.toString());
        //     }

        //     if (exitCode != 0) {
        //         String errorMsg = String.format(
        //             "Python ended with code %d\nSTDERR:\n%s\nSTDOUT:\n%s",
        //             exitCode, errorOutput.toString(), output.toString()
        //         );
        //         throw new IOException(errorMsg);
        //     }

        // }
        
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Python process interrupted", e);
        } finally {
            if (process != null) {
                try { process.getInputStream().close(); } catch (Exception ignored) {}
                try { process.getOutputStream().close(); } catch (Exception ignored) {}
                try { process.getErrorStream().close(); } catch (Exception ignored) {}
                
                process.destroyForcibly();
                
                try {
                    process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Parse JSON response
        List<RecommendedItem> recommendedItems = new ArrayList<>();
        try {
            String outStr = output.toString();
            String jsonCandidate = null;
            
            int idx = outStr.lastIndexOf("{\"recommended_path\"");
            if (idx == -1) idx = outStr.lastIndexOf("{\"error\"");
            if (idx == -1) {
                idx = outStr.indexOf('{');
            }
            if (idx != -1) {
                jsonCandidate = outStr.substring(idx);
            } else {
                jsonCandidate = outStr;
            }

            JsonObject response = JsonParser.parseString(jsonCandidate).getAsJsonObject();

            if (response.has("error")) {
                String errorMsg = response.get("error").getAsString();
                throw new IOException("Error returned by Python recommender: " + errorMsg);
            }

            if (!response.has("recommended_path")) {
                throw new IOException("Recommender JSON response without 'recommended_path' field: " + response);
            }

            JsonArray path = response.getAsJsonArray("recommended_path");
            JsonArray scores = response.has("scores") ? response.getAsJsonArray("scores") : null;

            int skippedCount = 0;
            for (int i = 0; i < path.size(); i++) {
                long itemIdOriginal = path.get(i).getAsLong();

                if (visitedOriginalsSet.contains(itemIdOriginal)) {
                    skippedCount++;
                    continue;
                }

                float score = (scores != null && scores.size() > i) ? scores.get(i).getAsFloat() : 1.0f;
                recommendedItems.add(new es.unizar.util.GenericRecommendedItem(itemIdOriginal, score));
            }

            // System.out.println("[RiskAwareRecommendation] Items from Python: " + path.size() +
            //         ", Filtered (already visited): " + skippedCount +
            //         ", Valid items: " + recommendedItems.size());

        } catch (Exception e) {
            throw new IOException("Error parsing Python recommender response: " + output, e);
        }

        // Translate to internal sequential IDs
        List<RecommendedItem> recmobisimItems = new ArrayList<>();
        for (RecommendedItem item : recommendedItems) {
            long origId = item.getItemID();
            Long seqId = origToSeq.get(origId);
            if (seqId != null) {
                recmobisimItems.add(new es.unizar.util.GenericRecommendedItem(seqId, item.getValue()));
            }
        }
        
        return recmobisimItems;
    }

    /**
     * Gets the map original->secuencial
     * 
     * @return map from original IDs to sequential
     * @throws IOException if there is an error reading the file
     */
    private Map<Long, Long> getOrigToSeqMap() throws IOException {
        if (cachedOrigToSeq == null) {
            synchronized (MAP_CACHE_LOCK) {
                if (cachedOrigToSeq == null) {
                    buildMapsFromFile();
                }
            }
        }
        return cachedOrigToSeq;
    }

    /**
     * Gets the map secuencial->original
     * 
     * @return map from sequential IDs to original
     * @throws IOException if there is an error reading the file
     */
    private Map<Long, Long> getSeqToOrigMap() throws IOException {
        if (cachedSeqToOrig == null) {
            synchronized (MAP_CACHE_LOCK) {
                if (cachedSeqToOrig == null) {
                    buildMapsFromFile();
                }
            }
        }
        return cachedSeqToOrig;
    }

    /**
     * Builds the translation maps from the item file
     * 
     * @throws IOException if there is an error reading the file
     */
    private void buildMapsFromFile() throws IOException {
        String itemFilePath = Literals.ITEM_FLOOR_COMBINED;
        Map<Long, Long> origToSeq = new HashMap<>();
        Map<Long, Long> seqToOrig = new HashMap<>();
        
        List<String> lines = Files.readAllLines(Paths.get(itemFilePath));
        for (String line : lines) {
            if (line.startsWith("item_objectID_")) {
                int idxEq = line.indexOf('=');
                int idxUnd = line.lastIndexOf('_', idxEq);
                if (idxEq > 0 && idxUnd > 0) {
                    long seq = Long.parseLong(line.substring(idxUnd + 1, idxEq));
                    long orig = Long.parseLong(line.substring(idxEq + 1));
                    origToSeq.put(orig, seq);
                    seqToOrig.put(seq, orig);
                }
            }
        }
        
        cachedOrigToSeq = origToSeq;
        cachedSeqToOrig = seqToOrig;
    }

    /**
     * Clears the map caches (call if the item file changes)
     */
    public static void clearMapCaches() {
        synchronized (MAP_CACHE_LOCK) {
            cachedOrigToSeq = null;
            cachedSeqToOrig = null;
        }
        System.out.println("[RiskAwareRecommendation] ✅ Map caches cleared");
    }
}