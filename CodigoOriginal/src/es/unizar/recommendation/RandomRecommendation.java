package es.unizar.recommendation;

import java.util.LinkedList;
import java.util.List;

import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.dao.DataAccessLayer;
import es.unizar.database.DBDataModel;
import es.unizar.util.ElementIdMapper;
import es.unizar.util.GenericRecommendedItem;

public class RandomRecommendation {


	// Data model donde estan todos los items votados por el userID.
	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;

	public RandomRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		// Data Model con los items vistos hasta el momento.
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
	}

	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
		// Obtiene la preferencia de todos items del userID
		List<String> allPreferences = dataAccessLayer.getUserItemContextRatingRandomFor(userID);
		
		PreferenceArray seenPreferences;
		try {
			// Quita los items vistos de la lista.
			seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
		} catch (NoSuchUserException e) {
			seenPreferences = null;
		}

		List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
		int posAll = 0;
		while (topList.size() != howMany && posAll < allPreferences.size()) { // Menor o igual no, si no -> EXCEPCI�N (y no usa el recomendador)
			
			String[] array = allPreferences.get(posAll).split(";");
			long itemID = Long.valueOf(array[1]).longValue();
			float rating = Float.valueOf(array[3]).floatValue();

			// Obtiene solo los items no vistos.
			if ((seenPreferences == null || !seenPreferences.hasPrefWithItemID(itemID))) {
				topList.add(new GenericRecommendedItem(itemID, rating));
			}
			posAll++;
		}

		if (!topList.isEmpty()) {
			StringBuilder sb = new StringBuilder("Primeros elementos: ");
			for (int i = 0; i < Math.min(3, topList.size()); i++) {
				RecommendedItem item = topList.get(i);
				sb.append("[ID=").append(item.getItemID())
				.append(", rating=").append(item.getValue())
				.append(", formato correcto=")
				.append(ElementIdMapper.isInCorrectRange(item.getItemID(), ElementIdMapper.CATEGORY_ITEM))
				.append("] ");
			}
		}

		
		return topList;
	}
}
