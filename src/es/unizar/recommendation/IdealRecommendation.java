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


	// Data model donde estan todos los items votados por el userID.
	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;

	public IdealRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		// Data Model con los items vistos hasta el momento.
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
	}

	public List<RecommendedItem> recommend(long userID, int howMany, long context) throws TasteException {
		// Obtiene la preferencia de todos items del userID
		List<String> allPreferences = dataAccessLayer.getUserItemContextRatingFor(userID);
		PreferenceArray seenPreferences;
		try {
			// Quita los items vistos de la lista.
			seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
		} catch (NoSuchUserException e) {
			seenPreferences = null;
		}

		List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
		int posAll = 0;
		while (topList.size() != howMany && posAll <= allPreferences.size()) {
			String[] array = allPreferences.get(posAll).split(";");
			long itemID = Long.valueOf(array[1]).longValue();
			long contextID = Long.valueOf(array[2]).longValue();
			float rating = Float.valueOf(array[3]).floatValue();

			// Obtiene solo los items no vistos.
			if ((seenPreferences == null || !seenPreferences.hasPrefWithItemID(itemID)) && contextID == context) {
				topList.add(new GenericRecommendedItem(itemID, rating));
			}
			posAll++;
		}
		return topList;
	}
}
