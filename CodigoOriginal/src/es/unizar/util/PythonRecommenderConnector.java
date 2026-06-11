package es.unizar.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PythonRecommenderConnector {

    public class RecommenderResult {
        public List<String> no_risk;
        public List<String> risk;
        public double avg_risk;
        public double avg_rating;
        public double avg_distance;
    }


    public static RecommenderResult callRecommender(File occFile, File durFile, List<Integer> favoriteIDs) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("python", "recommender_bridge.py",
                occFile.getAbsolutePath(), durFile.getAbsolutePath());
        pb.directory(new File("path/a/la/carpeta/python"));
        Process process = pb.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Proceso interrumpido", e);
        }

        // Leer el JSON de salida
        String json = Files.readString(Paths.get("recommendations.json"));
        return new Gson().fromJson(json, RecommenderResult.class);
    }
}
