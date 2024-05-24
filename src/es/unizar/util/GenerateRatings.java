package es.unizar.util;

import java.awt.EventQueue;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

import com.opencsv.CSVWriter;

import es.unizar.gui.MainSimulator;

public class GenerateRatings {
	
	public static String CSV_FILE_PATH = "D:\\DATASET\\ratings.csv";
	
	public static CSVWriter csvWriter;
	public static int NUM_USERS = 176;
	public static int NUM_ITEMS = 283;
	public static int NUM_CONTEXTS = 9;
	
	public static double PROB_RATING_1 = 0.064;
	public static double PROB_RATING_2 = 0.229;
	public static double PROB_RATING_3 = 0.3486;
	public static double PROB_RATING_4 = 0.2241;

	public static void main(String[] args) {
		
		try {
			// Create CSV Writter
			FileWriter output = new FileWriter(CSV_FILE_PATH);
			csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			// Write header
	        String[] header = { "id_user", "id_item", "id_context", "rating", "opinion", "user_provided" };
	        csvWriter.writeNext(header);
	        
	        for (int i = 1; i <= NUM_USERS; i++) {
	        	for (int j = 1; j <= NUM_ITEMS; j++) {
	        		for (int k = 1; k <= NUM_CONTEXTS; k++) {
	        			
	        			double rating = getRandomRating();
	        			// Write header
	        	        String[] entry = { Integer.toString(i), Integer.toString(j), Integer.toString(k), Double.toString(rating), "null", "1" };
	        	        csvWriter.writeNext(entry);
	        		}
	        	}
	        }
	        
	        csvWriter.close();
	        
	        System.out.println("Finished rating generation!");
		}
		catch (IOException ioexception) {
			MainSimulator.printConsole(ioexception.getMessage(), Level.SEVERE);
			ioexception.printStackTrace();
		}
		
	}

	private static double getRandomRating() {
		
		Random rand = new Random();
		double random = rand.nextDouble();
		
		if (random <= PROB_RATING_1) {
			return 1.0;
		}
		else if (random <= PROB_RATING_1 + PROB_RATING_2){
			return 2.0;
		}
		else if (random <= PROB_RATING_1 + PROB_RATING_2 + PROB_RATING_3) {
			return 3.0;
		}
		else if (random <= PROB_RATING_1 + PROB_RATING_2 + PROB_RATING_3 + PROB_RATING_4) {
			return 4.0;
		}
		else {
			return 5.0;
		}
		
	}
}
