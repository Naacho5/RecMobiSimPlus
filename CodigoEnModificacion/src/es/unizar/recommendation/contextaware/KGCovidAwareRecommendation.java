/**
 * 
 */
package es.unizar.recommendation.contextaware;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.unizar.dao.DataAccessLayer;
import es.unizar.database.DBDataModel;
import es.unizar.recommendation.contextaware.ContextAwareRecommendation;
import es.unizar.util.GenericRecommendedItem;
import es.unizar.http.RecommenderAPIConnector;

/**
 * Knowledge-graph based recommendation with infection probability awareness
 * @author rhermoso
 */
public class KGCovidAwareRecommendation {
		
	private double riskThreshold = 0.1;
	private String URL_KGInfectionRecommenderConnector = "http://localhost:5000";
	private RecommenderAPIConnector connector = new RecommenderAPIConnector();
	
	// Data model donde estan todos los items votados por el userID.
	public DBDataModel dataModelUserID;
	public DataAccessLayer dataAccessLayer;

	public KGCovidAwareRecommendation(DBDataModel dataModelUserID, DataAccessLayer dataAccessLayer, double threshold) {
		this.dataModelUserID = dataModelUserID;
		this.dataAccessLayer = dataAccessLayer;
		this.riskThreshold = threshold;
		
//	Transfer model into Python REST API (Jürgen's code)
//	1. Create users (API function for this - input with
//	2. Set up threshold for risk
//	3. 
	}
	
	/**
	 * 
	 * @param userID
	 * @param list 
	 * @param dataModel
	 * @return
	 * @override
	 */
	public List<RecommendedItem> recommend(long userID, int roomID, Map<String, Integer> occupancies, int howMany, List<Long> itemsVisited) {
//		System.out.println("***** RoomID -> " + roomID);
		List<RecommendedItem> recommendedItems = new LinkedList<RecommendedItem>();
//		RecommenderAPIConnector kgInfectionRecommenderConnector = new RecommenderAPIConnector();
//		String recommenderItemsStr = kgInfectionRecommenderConnector.callGETRecommenderAPI(this.URL_KGInfectionRecommenderConnector + "/recommendations/" + userID);
		String recommenderItemsStr = connector.getRecommendation((int)userID);
//		System.out.println(recommenderItemsStr);
		String recommendedPath = connector.getRecommendationPath((int)userID, roomID);
//		System.out.println(recommendedPath);
		String recommendedFilteredPath = connector.filterOutRiskRooms((int)userID, occupancies, itemsVisited);
//		System.out.println("***************** RecommendedFilteredItems -> \n");
//		System.out.println(recommendedFilteredPath);
//		XXX Hay que parsear el JSON y convertirlo en una lista de RecommendedItems
		

		ObjectMapper mapper = new ObjectMapper();

        // read the json strings and convert it into JsonNode
        try {
			JsonNode node = mapper.readTree(recommendedFilteredPath);
//			System.out.println("INFO");
//	        System.out.println(node.get(0));
			int itemID = 0;
//			Long itemLongID = new Long(0);
			for (int i = 0; i < node.size() && recommendedItems.size() < howMany; i++) {
				itemID = Integer.valueOf(node.get(i).get("moma_id").asText());
//				itemLongID = Long.valueOf(node.get(i).get("node_id").asText());
//				System.out.println("itemsVisited --> " + itemsVisited);
//				System.out.println("itemLongID --> " + itemLongID);
				
//				if (!itemsVisited.contains(itemLongID)) {
				RecommendedItem ri = new GenericRecommendedItem(itemID, 0);
				recommendedItems.add(ri);
//				}
			}
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return recommendedItems;
		
	}


	public float estimatePreference(long userID, long itemID) throws TasteException {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
