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

	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;

	public RandomRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
	}

	public List<RecommendedItem> recommend(long userID, int howMany, List<Long> observedItems) throws TasteException {
        List<String> allPreferences = dataAccessLayer.getUserItemContextRatingRandomFor(userID);
        
        PreferenceArray seenPreferences;
        try {
            seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
        } catch (NoSuchUserException e) {
            seenPreferences = null;
        }

        List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
        int posAll = 0;
        
        while (topList.size() != howMany && posAll < allPreferences.size()) {
            String[] array = allPreferences.get(posAll).split(";");
            
            long objectId = Long.valueOf(array[1]).longValue();
            long internalId = Long.valueOf(array[1]).longValue();
            long itemID = ElementIdMapper.convertToRangeId(internalId, ElementIdMapper.CATEGORY_ITEM);
            float rating = Float.valueOf(array[3]).floatValue();

            boolean alreadySeen = false;
            
            if (seenPreferences != null && seenPreferences.hasPrefWithItemID(itemID)) {
                alreadySeen = true;
            }
            
            if (observedItems != null && 
                (observedItems.contains(internalId) || observedItems.contains(itemID) || observedItems.contains(objectId))) {
                alreadySeen = true;
            }

            if (!alreadySeen) {
                topList.add(new GenericRecommendedItem(internalId, rating));
            }
            else {
            }
            
            posAll++;
        }

        return topList;
    }

	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
        return recommend(userID, howMany, null);
    }
}
