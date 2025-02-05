package es.unizar.access;

import java.io.File;
import java.util.Properties;

import es.unizar.util.Literals;

/**
 * Access to the values of the available recommenders stored in file. Getters and setters.
 * 
 * @author Alejandro Piedrafita Barrantes
 *
 */
public class DataAccessRecommendersFile extends DataAccess {

	public DataAccessRecommendersFile(File file) {
		super(file);
	}
	
	/*
	public static void printProperties(Properties prop)
    {
        for (Object key: prop.keySet()) {
            System.out.println(key + ": " + prop.getProperty(key.toString()));
        }
    }*/
	
	public String getRecommenderName(int position) {
        return getPropertyValue(Literals.RECOMMENDER_NAME + position);
    }
	
	public boolean getRecommenderUsesP2P(int position) {
        return Boolean.valueOf(getPropertyValue(Literals.RECOMMENDER_USES_P2P + position));
    }
	
	/**
	 * If recommender founded, returns if it uses P2P network (to enable the checkboxes).
	 * 
	 * @param recommender
	 * @return T/F
	 */
	public boolean checkRecommenderUsesP2P(String recommender) {
		
		int i = 0;
		
		for (Object key: this.properties.keySet()) {
			if (this.properties.getProperty(key.toString()).equals(recommender)) {
				i = Character.getNumericValue((key.toString().charAt(key.toString().length() -1)));
				break;
			}
		}
		
		if (i>0)
			return getRecommenderUsesP2P(i);
		else
			return false;
		
	}

}
