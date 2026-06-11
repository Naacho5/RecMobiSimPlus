package es.unizar.recommendation;

import java.util.LinkedList;
import java.util.List;

import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.dao.DataAccessLayer;
import es.unizar.database.DBDataModel;
import es.unizar.util.GenericRecommendedItem;

public class IdealRecommendation {

	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;

	public IdealRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
	}

	public List<RecommendedItem> recommend(long userID, int howMany, long context) throws TasteException {
		List<String> allPreferences = dataAccessLayer.getUserItemContextRatingFor(userID);
		PreferenceArray seenPreferences;
		try {
			seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
		} catch (NoSuchUserException e) {
			seenPreferences = null;
		}

		List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
		int posAll = 0;
		while (topList.size() != howMany && posAll < allPreferences.size()) { // Modificado por Nacho Palacio 2026-06-10
			String[] array = allPreferences.get(posAll).split(";");
			long itemID = Long.valueOf(array[1]).longValue();
			long contextID = Long.valueOf(array[2]).longValue();
			float rating = Float.valueOf(array[3]).floatValue();

			if ((seenPreferences == null || !seenPreferences.hasPrefWithItemID(itemID))) {
				topList.add(new GenericRecommendedItem(itemID, rating)); // Modified by Nacho Palacio 2025-11-05
			}
			posAll++;
		}
		return topList;
	}
}
