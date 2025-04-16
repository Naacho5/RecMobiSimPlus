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


	// Data model donde estan todos los items votados por el userID.
	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;
	public Random random;

	public ExhaustiveRecommendation(final DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer) {
		// Data Model con los items vistos hasta el momento.
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
		this.random = new Random(Configuration.simulation.getSeed());
	}

	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
		// Obtiene la preferencia de todos items del userID
		// List<Long> allPreferences = dataAccessLayer.getItemsOrderByRoom();
		List<Long> allPreferences = getItemsOrderByRoom();

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
			long itemID = allPreferences.get(posAll);
			long contextID = (long) random.nextInt(9 - 1) + 1;
			float rating = dataAccessLayer.getPreferenceFor(userID, itemID, contextID);

			// Obtiene solo los items no vistos.
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

//	public static void main(String[] args) {
//		try {
//			DBDataModel dataModel = new DBDataModel(Literals.DB_SPECIAL_USER_PATH);
//			DataAccessLayer dataAcces = new DataAccessLayer(Literals.DB_ALL_USERS_PATH);
//			ExhaustiveRecommendation recommender = new ExhaustiveRecommendation(dataModel, dataAcces);
//
//			long items[] = { 79, 81, 82, 83, 84, 85, 87, 88, 144, 160, 165, 216, 235, 89, 90, 91, 92, 93, 95, 212, 96, 98, 107, 111, 196, 226, 112, 113, 115, 117, 168, 172, 204, 208, 119, 120, 123, 125, 126, 128, 129, 130, 149, 184, 188, 198, 206, 215, 219, 131, 132, 189, 213, 133, 134, 135, 137, 138, 139, 148, 166, 173, 195, 197, 240, 5, 7, 9, 10, 11, 22, 23, 25, 27, 30, 33, 37, 39, 43, 44, 46, 52, 55, 56, 61, 63, 65, 66, 72, 76, 77, 80, 86, 94, 97, 99, 100, 101, 102, 103, 104, 105, 106, 108,
//					109, 110, 114, 116, 118, 121, 122, 124, 127, 136, 140, 142, 143, 145, 147, 239, 151, 164, 169, 170, 171, 182, 183, 186, 190, 192, 193, 199, 200, 201, 203, 205, 207, 209, 211, 217, 218, 222, 223, 224, 227, 228, 229, 230, 231, 232, 233, 234, 236, 237, 238 };
//			long contexts[] = { 7, 1, 5, 9, 5, 7, 1, 4, 5, 2, 9, 7, 6, 4, 6, 7, 8, 2, 9, 6, 7, 3, 9, 1, 8, 8, 1, 9, 3, 7, 3, 9, 9, 1, 2, 8, 5, 5, 7, 2, 6, 3, 9, 6, 2, 7, 1, 7, 4, 1, 4, 9, 6, 5, 5, 2, 4, 4, 5, 5, 2, 8, 5, 7, 5, 3, 9, 7, 3, 9, 8, 8, 8, 5, 5, 2, 5, 7, 7, 8, 2, 8, 3, 3, 9, 5, 4, 3, 4, 2, 3, 6, 9, 1, 2, 2, 6, 1, 7, 4, 5, 3, 3, 7, 5, 5, 8, 5, 3, 7, 1, 5, 6, 9, 5, 2, 8, 2, 6, 4, 3, 4, 8, 2, 4, 1, 9, 3, 7, 7, 9, 6, 2, 3, 7, 9, 4, 1, 1, 9, 7, 7, 4, 6, 1, 2, 7, 6, 7, 7, 2, 5, 9, 7, 1 };
//
//			long userID = 176;
//			long itemID = 0;
//			long contextID = 0;
//
//			for (int i = 0; i < items.length; i++) {
//				itemID = items[i];
//				contextID = contexts[i];
//				float rating = recommender.dataAccessLayer.getPreferenceFor(userID, itemID, contextID);
//
//				System.out.println(userID + ";" + itemID + ";" + rating + ";" + contextID);
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//
//	}
}
