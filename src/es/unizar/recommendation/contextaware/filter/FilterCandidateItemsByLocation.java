/*
 * @(#)Context.java  1.0.0  13/10/14
 *
 * MOONRISE
 * Webpage: http://webdiis.unizar.es/~maria/?page_id=250
 * 
 * University of Zaragoza - Distributed Information Systems Group (SID)
 * http://sid.cps.unizar.es/
 *
 * The contents of this file are subject under the terms described in the
 * MOONRISE_LICENSE file included in this distribution; you may not use this
 * file except in compliance with the License.
 *
 * Contributor(s):
 *  RODRIGUEZ-HERNANDEZ, MARIA DEL CARMEN <692383[3]unizar.es>
 *  ILARRI, SERGIO <silarri[3]unizar.es>
 */
package es.unizar.recommendation.contextaware.filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.GenericRecommendedItem;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.dao.DataAccessLayer;

/**
 * Filters the candidate items to recommend, by using the distance between the
 * cadidate items and the user's location. Filter of Hard Constraints.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public class FilterCandidateItemsByLocation {

	private DataAccessLayer dataAccess;
    /**
     * Filter items closest to the user's location.
     *
     * @param dbURL The path of the database.
     */
    /*public FilterCandidateItemsByLocation(String dbURL) {
        dataAccess = new DataAccessLayer(dbURL);
    }*/
	
	/**
     * Filter items closest to the user's location.
     *
     * @param dataAccessLayer.
     */
	public FilterCandidateItemsByLocation(DataAccessLayer dataAccessLayer) {
        dataAccess = dataAccessLayer;
    }

    /**
     * Filter items closest to the user's location.
     *
     * @param profileId User profile
     * @param transportWayValue
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws TasteException
     */
    public List<RecommendedItem> filteringCandidateItemsByLocation(List<RecommendedItem> candidateItems, long userID, String transportWayValue, long userLatitude_X2, long userLongitude_Y2, double thresholdRating) throws FileNotFoundException, IOException, TasteException {
        List<RecommendedItem> recommendedItems = new LinkedList<RecommendedItem>();
        float ratingValue = 0;
        for (int i = 0; i < candidateItems.size(); i++) {
            RecommendedItem candidateItem = candidateItems.get(i);
            long itemID = candidateItem.getItemID();
            long itemLatitud_X1 = getItemLatitudeGPS(itemID);
            long itemLongitude_Y1 = getItemLongitudeGPS(itemID);
            int distance = distanciaEuclidea(itemLatitud_X1, itemLongitude_Y1, userLatitude_X2, userLongitude_Y2);
            double radious = getUserRadious(userID, transportWayValue);
            if (distance <= radious) {
                //Near
                ratingValue = candidateItem.getValue();
            } else {
                //Far
                ratingValue = (float) (thresholdRating - 1);
            }
            RecommendedItem recommendedItem = new GenericRecommendedItem(itemID, ratingValue);
            recommendedItems.add(recommendedItem);
        }
        Collections.sort(recommendedItems, new RatingComparator());
        return recommendedItems;
    }

    public int distanciaEuclidea(long latitud_item_X1, long longitude_item_Y1, long latitude_user_X2, long longitude_user_Y2) {
        int distance = 0;
        distance = (int) Math.sqrt(Math.pow(latitude_user_X2 - latitud_item_X1, 2) + Math.pow(longitude_user_Y2 - longitude_item_Y1, 2));
        return distance;
    }

    public long getUserRadious(long userID, String transportWayValue) {
        return dataAccess.getRadius(userID, transportWayValue);
    }

    public long getItemLatitudeGPS(long itemID) {
        long itemLatitudeGPS = 0;
        itemLatitudeGPS = dataAccess.getItemLatitude(itemID);
        return itemLatitudeGPS;
    }

    public long getItemLongitudeGPS(long itemID) {
        long itemLongitudeGPS = 0;
        itemLongitudeGPS = dataAccess.getItemLongitude(itemID);
        return itemLongitudeGPS;
    }

    //Hasta ahora no se utiliza.
    //RP: Rating predicted
    //PM: Puntuacion maxima
    //U: Umbral para recomendar
    //NNR: numero de niveles no recomendados
    //NR: numero de niveles recomendados
    //ED: Exceso de distancia
    public double estimatePreference(double thresholdRating, double radious, double distance) {
        double RP = 1.0;
        //double PM = dataAccess.getMaximumRating();
        double U = thresholdRating;
        //double NR = PM - U + 1;
        double NNR = U - 1;
        double ED = distance - radious;
        double penalizationIncrement = NNR / 3;
        if (ED < radious / NNR) {
            RP = U - 1 * penalizationIncrement;
        } else {
            if ((radious / NNR) < ED && ED < ((2 * radious) / NNR)) {
                RP = U - 2 * penalizationIncrement;
            } else {
                if (((2 * radious) / NNR) < ED && ED <= ((3 * radious) / NNR)) {
                    RP = U - 3 * penalizationIncrement;
                }
            }
        }
        return RP;
    }
    
    public static class RatingComparator implements Comparator<RecommendedItem> {
        @Override
        public int compare(RecommendedItem x, RecommendedItem y) {
            if (x.getValue() > y.getValue()) {
                return -1;
            }
            if (x.getValue() < y.getValue()) {
                return 1;
            }
            return 0;
        }
    }
}
