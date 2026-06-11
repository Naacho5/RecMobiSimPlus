package es.unizar.recommendation;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.dao.DataAccessLayer;
import es.unizar.database.DBDataModel;
import es.unizar.gui.Configuration;
import es.unizar.util.GenericRecommendedItem;
import es.unizar.util.Literals;

public class ExhaustiveRecommendation {

	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;
	public Random random;

	public ExhaustiveRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
		this.random = new Random(Configuration.simulation.getSeed());
	}

	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
		// List<Long> allPreferences = dataAccessLayer.getItemsOrderByRoom();
		List<Long> allPreferences = getItemsOrderByRoom();

		PreferenceArray seenPreferences;
		try {
			seenPreferences = dataModelUserID.getPreferencesFromUser(userID);
		} catch (NoSuchUserException e) {
			seenPreferences = null;
		}

		List<RecommendedItem> topList = new LinkedList<RecommendedItem>();
		int posAll = 0;
		while (topList.size() != howMany && posAll < allPreferences.size()) { // Modificado por Nacho Palacio 2026-06-10
			long itemID = allPreferences.get(posAll);
			long contextID = (long) random.nextInt(9 - 1) + 1;
			float rating = dataAccessLayer.getPreferenceFor(userID, itemID, contextID);

			if ((seenPreferences == null || !seenPreferences.hasPrefWithItemID(itemID))) {
				topList.add(new GenericRecommendedItem(itemID, rating));
			}
			posAll++;
		}
		return topList;
	}

	private List<Long> getItemsOrderByRoom() {
		
		List<Long> allPreferences = new LinkedList<Long>();
		
		DataAccessGraphFile graphFile = new DataAccessGraphFile(new File(Literals.GRAPH_FLOOR_COMBINED));
		
		for (int i = 1; i <= graphFile.getNumberOfRoom(); i++) {
			for (int j = 1; j <= graphFile.getNumberOfItemsByRoom(i); j++) {
				allPreferences.add(graphFile.getItemOfRoom(j, i));
			}
		}
		
		
		return allPreferences;
	}
}
