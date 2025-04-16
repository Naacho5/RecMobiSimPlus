/**
 * 
 */
package es.unizar.http;

//import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;

/**
 * @author Ramón Hermoso
 */
/**
 * 
 */
public class RecommenderAPIConnector {
	
	private static final String URL_http_server = "http://localhost:5000";

	public String callGETRecommenderAPI(String url) {
		HttpClient client = HttpClients.createDefault();
		
		HttpGet	getRequest = new HttpGet(url);
				

        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(getRequest)) {
            return readResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
	

	public String callPOSTRecommenderAPI(String url, String jsonBody) {
		HttpClient client = HttpClients.createDefault();
		
		HttpPost postRequest = new HttpPost(url);
		
		try {
			postRequest.setHeader("Content-type", "application/json");
            // Añadiendo el cuerpo JSON a la solicitud POST
            StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
//            StringEntity entityVisited = new StringEntity(json_itemsVisited, ContentType.APPLICATION_JSON);
            postRequest.setEntity(entity);

//            System.out.println("***** JSON enviado *****");
//            System.out.println(jsonBody);
//            System.out.println(postRequest.getEntity().getContent());

	        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(postRequest)) {
	            return readResponse(response);
	        } 
		} catch (IOException e) {
            e.printStackTrace();
            return null;
        }
		
    }
	
    private String readResponse(CloseableHttpResponse response) throws IOException {
    	try (InputStreamReader streamReader = new InputStreamReader(response.getEntity().getContent());
                BufferedReader bufferedReader = new BufferedReader(streamReader)) {
               StringBuilder responseBuilder = new StringBuilder();
               String line;
               while ((line = bufferedReader.readLine()) != null) {
                   responseBuilder.append(line);
               }
               return responseBuilder.toString();
           }
    }

    /**
     * Generates a new user in the recommender service, id of the user is generated in sequence from 1.
     */
    public void createNewUser() {
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	String response = apiConnector.callGETRecommenderAPI(URL_http_server + "/user");
//    	System.out.println("Respuesta de la API Python: " + response);
    }
    
    /**
     * Gets a list of items to visit
     * @param userId
     * @return A JSON string with recommended artworks 
     * XXX It should return a list of recommended objects List<?> (now just a string)
     */
    public String getRecommendation(int userId) {
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	String response = apiConnector.callGETRecommenderAPI(URL_http_server + "/recommendations/" + userId);
//    	System.out.println("Respuesta de la API Python: " + response);
    	return response;
    }
    
    /**
     * Returns a list of room ids forming a visiting path
     * @param userId
     * @param roomId
     * @return A JSON string value
     */
    public String getRecommendationPath(int userId, int roomId) {
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	String response = apiConnector.callGETRecommenderAPI(URL_http_server + "/path/uid/" + userId + "/start-at/" + roomId);
//    	System.out.println("Respuesta de la API Python: " + response);
    	return response;
    }
    
    /**
     * 
     * @param roomId
     * @param occupancies
     * @param visit Indicates if the user stays visiting an artwork or just passes by.
     * @return
     */
    public String riskInRoom(int roomId, Map<String, Integer> occupancies, int visit) {
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	
    	Map<String, Object> nestedMap = new HashMap<String, Object>();
    	nestedMap.put("occupancy", occupancies);
    	nestedMap.put("visit", visit);
//    	nestedMap.put("duration", duration);    	
//		Converts Map into JSON string
    	ObjectMapper objectMapper = new ObjectMapper();
        String jacksonData = "";
		try {
			jacksonData = objectMapper.writeValueAsString(nestedMap);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return apiConnector.callPOSTRecommenderAPI(URL_http_server + "/room_risk/" + roomId, jacksonData);
    }
    
    
    public String filterOutRiskRooms(int userId, Map<String, Integer> occupancies, List<Long> list) { //, Map<String, Double> sojournTimes, double risk_threshold) {
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	
    	Map<String, Object> nestedMap = new HashMap<String, Object>();
    	
    	nestedMap.put("occupancy", occupancies);
    	nestedMap.put("itemsVisited", list);
//    	nestedMap.put("sojournTimes", sojournTimes);
    	
//    	Map<String, Double> thresholdMap = new HashMap<String, Double>();
//    	thresholdMap.put("risk_threshold", Double.valueOf(risk_threshold));
    	
//    	nestedMap.put("risk_threshold", risk_threshold);
    	
//		Converts Map into JSON string
    	ObjectMapper objectMapper = new ObjectMapper();
        String jacksonData = "";
		try {
			jacksonData = objectMapper.writeValueAsString(nestedMap);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		ObjectMapper objectMapper2 = new ObjectMapper();
//		String json_itemsVisited = "";
//		
//		try {
//			json_itemsVisited = objectMapper2.writeValueAsString(itemsVisited);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    Assert.assertEquals(expectedJsonArray, jsonArray);
		
//        Assertions.assertEquals(jacksonData, occupancies);
    	return apiConnector.callPOSTRecommenderAPI(URL_http_server + "/filter_risk/" + userId, jacksonData);
    }
    
    /**
     * 
     * @param userId
     * @param visited_artworks
     * @return POST method returning a list with a list of rooms already visited by the user with userId
     */
//    public String filterOutVisitedRooms(int userId, Map<Integer, Integer> visited_artworks) {
//    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
//    	
////		Converts Map into JSON string
//    	ObjectMapper objectMapper = new ObjectMapper();
//        String jacksonData = "";
//		try {
//			jacksonData = objectMapper.writeValueAsString(visited_artworks);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////        Assertions.assertEquals(jacksonData, occupancies);
//    	return apiConnector.callPOSTRecommenderAPI(URL_http_server + "/filter_visited/" + userId, jacksonData);
//
//    }
    
    public static void main(String[] args) {
    	int userId = 1;
    	RecommenderAPIConnector apiConnector = new RecommenderAPIConnector();
    	
    	// Creates new user
//    	String response = apiConnector.callGETRecommenderAPI("http://localhost:5000/user");
    	
    	// Gets recommendation for a userId
//    	String response = apiConnector.callGETRecommenderAPI("http://localhost:5000/recommendations/20");
    	
    	// Gets path of a userId from a node (room)
//    	String response = apiConnector.callGETRecommenderAPI("http://127.0.0.1:5000/path/uid/1/start-at/401");
    	
    	// Filters out high-risk rooms POST Manda la occupancy, pero debería mandar también el tiempo y quizá algo más.
//    	String response = apiConnector.callPOSTRecommenderAPI("http://127.0.0.1:5000/filter_risk/20", "{\"401\": 5, \"402\":50, \"403\":10, \"404\":34, \"405\":79, \r\n"
//    			+ "    \"406\":90, \"407\": 12, \"408\":20, \"409\":5, \"410\":57, \r\n"
//    			+ "    \"411\":20, \"412\":140,  \"413\":34, \"414\":20, \"415\":55, \r\n"
//    			+ "    \"416\":12, \"417\":22,  \"418\":34, \"419\":167, \"420\":12, \r\n"
//    			+ "    \"421\":65}");
    	
    	// Filters out visited rooms POST (se le pasa un json con el id de las obras visitadas en orden de visita)
//    	String response = apiConnector.callPOSTRecommenderAPI("http://127.0.0.1:5000/filter_visited/1", "{\"1\": \"75815\",\"2\": \"75803\",\"3\": \"75812\"}");
//        System.out.println("Respuesta de la API Python: " + response);
    }
	
}
