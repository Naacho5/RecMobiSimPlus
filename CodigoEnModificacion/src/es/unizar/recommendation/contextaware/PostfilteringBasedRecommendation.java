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
package es.unizar.recommendation.contextaware;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.database.DBDataModel;
import es.unizar.recommendation.contextaware.filter.FilterCandidateItemsByLocation;

/**
 * Post-filtering based recommendations.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public class PostfilteringBasedRecommendation extends FilteringBasedRecommendation {

	//private static final Logger log = LoggerFactory.getLogger(PostfilteringBasedRecommendation.class);

	private FilterCandidateItemsByLocation filterLocation;
	private int howManyInitial;
	private String transportWayValue;
	private long userLatitude_X2;
	private long userLongitude_Y2;

	/**
	 * Filter the data model by the item type.
	 *
	 * @param neighborhood
	 *            The neighborhood.
	 * @param similarity
	 *            The similarity metric.
	 * @param userQuery
	 *            The user query.
	 * @param databasePaths
	 *            The paths of several databases.
	 * @throws Exception
	 */
	public PostfilteringBasedRecommendation(DBDataModel dataModel, double ratingThreshold, String dbURL, String transportWayValue, long userLatitude_X2, long userLongitude_Y2) throws Exception {
		super(dataModel, ratingThreshold);
		this.filterLocation = new FilterCandidateItemsByLocation(dataModel.getDataAccessLayer());
		this.setHowManyInitial(100);
		this.transportWayValue = transportWayValue;
		this.userLatitude_X2 = userLatitude_X2;
		this.userLongitude_Y2 = userLongitude_Y2;
	}

	@Override
	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
		List<RecommendedItem> candidateItems = getRecommender().recommend(userID, howManyInitial);
		List<RecommendedItem> recommendedItems = new LinkedList<RecommendedItem>();
		List<RecommendedItem> filteredItems;

		try {
			filteredItems = filterLocation.filteringCandidateItemsByLocation(candidateItems, userID, getTransportWayValue(), getUserLatitude_X2(), getUserLongitude_Y2(), getThreshold());
			for (int i = 0; i < howMany; i++) {
				recommendedItems.add(filteredItems.get(i));
			}
		} catch (IOException e) {
			// log.error(e.getClass().getName() + ": " + e.getMessage());
		}
		return recommendedItems;
	}

	@Override
	public float estimatePreference(long userID, long itemID) throws TasteException {
		return getRecommender().estimatePreference(userID, itemID);
	}

	// Gets and sets:
	public int getHowManyInitial() {
		return howManyInitial;
	}

	public void setHowManyInitial(int howManyInitial) {
		this.howManyInitial = howManyInitial;
	}

	public String getTransportWayValue() {
		return transportWayValue;
	}

	public void setTransportWayValue(String transportWayValue) {
		this.transportWayValue = transportWayValue;
	}

	public long getUserLatitude_X2() {
		return userLatitude_X2;
	}

	public void setUserLatitude_X2(long userLatitude_X2) {
		this.userLatitude_X2 = userLatitude_X2;
	}

	public long getUserLongitude_Y2() {
		return userLongitude_Y2;
	}

	public void setUserLongitude_Y2(long userLongitude_Y2) {
		this.userLongitude_Y2 = userLongitude_Y2;
	}
}
