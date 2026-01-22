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

	// public List<RecommendedItem> recommend(long userID, int howMany, List<Long> observedItems) throws TasteException {
	// 	// Obtiene la preferencia de todos items del userID
	// 	List<String> allPreferences = dataAccessLayer.getUserItemContextRatingRandomFor(userID);
	// 	System.out.println("✅ RandomRecommendation.recommend: allPreferences size = " + allPreferences.size());
		
	// 	PreferenceArray seenPreferences;
	// 	try {
	// 		// Quita los items vistos de la lista.
	// 		seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
	// 		System.out.println("✅ RandomRecommendation.recommend: seenPreferences = " + seenPreferences);
	// 	} catch (NoSuchUserException e) {
	// 		seenPreferences = null;
	// 	}

	// 	List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
	// 	int posAll = 0;
	// 	while (topList.size() != howMany && posAll < allPreferences.size()) {
			
	// 		String[] array = allPreferences.get(posAll).split(";");
	// 		// System.out.println("✅ RandomRecommendation.recommend: Processing preference string: " + allPreferences.get(posAll));


	// 		long objectId = Long.valueOf(array[1]).longValue();
	// 		// // Added by Nacho Palacio 2025-10-25
	// 		// long internalId = dataAccessLayer.getInternalItemId(objectId);
	// 		// if (internalId == -1) {
	// 		// 	posAll++;
	// 		// 	continue;
	// 		// }

	// 		System.out.println("✅ RandomRecommendation.recommend: Processing item with objectId=" + objectId);

	// 		long internalId = Long.valueOf(array[1]).longValue(); // Modified by Nacho Palacio 2025-11-05
	// 		long itemID = ElementIdMapper.convertToRangeId(internalId, ElementIdMapper.CATEGORY_ITEM);

	// 		// long itemID = Long.valueOf(array[1]).longValue();
	// 		float rating = Float.valueOf(array[3]).floatValue();

	// 		System.out.println("✅ RandomRecommendation.recommend: Mapped internalId=" + internalId + " to itemID=" + itemID + " with rating=" + rating);


	// 		// Obtiene solo los items no vistos.
	// 		if ((seenPreferences == null || !seenPreferences.hasPrefWithItemID(itemID) /*|| !seenPreferences.hasPrefWithItemID(objectId)*/)) {
	// 			topList.add(new GenericRecommendedItem(internalId, rating));
	// 			System.out.println("✅ RandomRecommendation.recommend: Recomendando itemID " + itemID + " (internalId " + internalId + ") con rating " + rating + " para userID " + userID);
	// 		}
	// 		posAll++;
	// 	}

	// 	// if (!topList.isEmpty()) {
	// 	// 	StringBuilder sb = new StringBuilder("Primeros elementos: ");
	// 	// 	for (int i = 0; i < Math.min(3, topList.size()); i++) {
	// 	// 		RecommendedItem item = topList.get(i);
	// 	// 		sb.append("[ID=").append(item.getItemID())
	// 	// 		.append(", rating=").append(item.getValue())
	// 	// 		.append(", formato correcto=")
	// 	// 		.append(ElementIdMapper.isInCorrectRange(item.getItemID(), ElementIdMapper.CATEGORY_ITEM))
	// 	// 		.append("] ");
	// 	// 	}
	// 	// 	// System.out.println("✅ RandomRecommendation.recommend: " + sb.toString());
	// 	// }

		
	// 	return topList;
	// }

	public List<RecommendedItem> recommend(long userID, int howMany, List<Long> observedItems) throws TasteException {
        List<String> allPreferences = dataAccessLayer.getUserItemContextRatingRandomFor(userID);
        System.out.println("✅ RandomRecommendation.recommend: allPreferences size = " + allPreferences.size());
        
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

            // ✅ MEJORADO: Verificar AMBAS fuentes de ítems visitados
            boolean alreadySeen = false;
            
            // 1. Verificar en seenPreferences (DB)
            if (seenPreferences != null && seenPreferences.hasPrefWithItemID(itemID)) {
                alreadySeen = true;
            }
            
            // 2. Verificar en observedItems (memoria)
            if (observedItems != null && 
                (observedItems.contains(internalId) || observedItems.contains(itemID) || observedItems.contains(objectId))) {
                alreadySeen = true;
            }

            if (!alreadySeen) {
                topList.add(new GenericRecommendedItem(internalId, rating));
                System.out.println("✅ RandomRecommendation: Recomendando item " + internalId + " con rating " + rating);
            } else {
                System.out.println(" RandomRecommendation: Saltando item " + internalId + " (ya visitado)");
            }
            
            posAll++;
        }

        System.out.println("✅ RandomRecommendation: Total recomendados = " + topList.size());
        return topList;
    }

	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
        return recommend(userID, howMany, null);
    }
}
