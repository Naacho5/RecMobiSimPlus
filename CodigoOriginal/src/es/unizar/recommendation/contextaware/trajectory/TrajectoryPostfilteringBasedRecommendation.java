package es.unizar.recommendation.contextaware.trajectory;

import java.util.LinkedList;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.jgrapht.alg.DijkstraShortestPath;

import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import es.unizar.database.DBDataModel;
import es.unizar.recommendation.RandomRecommendation;
import es.unizar.recommendation.contextaware.PostfilteringBasedRecommendation;
import es.unizar.util.Distance;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Recommendations based in Post-filtering and trajectory.
 * 
 * @author Maria del Carmen Rodriguez-Hernandez
 *
 */
public class TrajectoryPostfilteringBasedRecommendation extends PostfilteringBasedRecommendation {

	// private static final Logger log =
	// LoggerFactory.getLogger(TrajectoryPostfilteringBasedRecommendation.class);

	private AbstractTrajectoryStrategy trajectoryStrategy;
	private long door;
	private String finalPath;
	private float threshold;

	public TrajectoryPostfilteringBasedRecommendation(DBDataModel dataModel, String dbURL, AbstractTrajectoryStrategy trajectoryStrategy, long entranceDoor, float threshold) throws Exception {
		super(dataModel, 0, dbURL, null, 0, 0);
		this.setTrajectoryStrategy(trajectoryStrategy);
		this.door = entranceDoor;
		this.finalPath = null;
		this.threshold = threshold;
	}

	// Para P2P
	@Override
	public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
		try {
			LongPrimitiveIterator userIdsIterator = dataModel.getUserIDs();
			List<Long> userIdsList = new ArrayList<>();
			while (userIdsIterator.hasNext()) {
				userIdsList.add(userIdsIterator.nextLong());
			}
			long[] userIds = new long[userIdsList.size()];
			for (int i = 0; i < userIdsList.size(); i++) {
				userIds[i] = userIdsList.get(i);
			}

			// Verificar si el usuario actual existe en el modelo
			boolean exists = false;
			for (long id : userIds) {
				if (id == userID) {
					exists = true;
					break;
				}
			}

		} catch (Exception e) {
			System.out.println("Error al obtener usuarios: " + e.getMessage());
		}

		// Verificar vecinos y similitud con otros usuarios
		try {
			if (getRecommender() instanceof org.apache.mahout.cf.taste.recommender.UserBasedRecommender) {
				// Probar diferentes umbrales de similitud
				try {
					UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
					double[] thresholds = {0.5, 0.3, 0.2, 0.1, 0.05, 0.01};
					
					for (double testThreshold : thresholds) {
						UserNeighborhood neighborhood = 
							new ThresholdUserNeighborhood(testThreshold, similarity, dataModel);
						
						long[] neighbors = neighborhood.getUserNeighborhood(userID);
						
						if (neighbors != null && neighbors.length > 0) {
							break;
						}
					}
				} catch (Exception e) {
					System.out.println("Error al probar umbrales: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println("Error general: " + e.getMessage());
		}


		// Traditional recommendation
		List<RecommendedItem> candidateItemsFromRecommender = getRecommender().recommend(userID, howMany);

		// Filtra los items teniendo en cuenta un umbral de rating
		List<RecommendedItem> candidateItemsFiltered = listRecommendedItemThreshold(candidateItemsFromRecommender);

		/* Añadido por Nacho Palacio 2025-04-14. */
		if (candidateItemsFiltered == null || candidateItemsFiltered.isEmpty()) {
            // System.out.println("Warning: No items passed the threshold filter for user " + userID);
            return candidateItemsFromRecommender;
        }

		// Las lista previamente filtrada se lleva a una lista de entero con solo los items a recomendar
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFiltered);
					 
		/* Añadido por Nacho Palacio 2025-04-14. */
		if (candidateItemsToLong.isEmpty()) {
            // System.out.println("Warning: Empty candidate items list for user " + userID);
            return candidateItemsFiltered;
        }

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsToLong);

		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsToLong, initialVertex);
		// Los items son ordenados teniendo en cuenta una trayectoria
		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);

		// Obtiene un path
		// finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
		// convertRecommendItemsToPath(sortedItems);

		/* Añadido por Nacho Palacio 2015-04-14. */
		try {
            finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, 
                      DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
            
            // Si finalPath es nulo, devolver los items filtrados sin trayectoria
            if (finalPath == null) {
                // System.out.println("Warning: Could not calculate path for user " + userID);
                return candidateItemsFiltered;
            }
            
            convertRecommendItemsToPath(sortedItems);
        } catch (Exception e) {
            // System.out.println("Error calculating path: " + e.getMessage());
            return candidateItemsFiltered;
        }

		// Obtiene nuevamente la lista de RecommendedItem pero teniendo en cuenta la trayectoria.
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		for (int i = 0; i < sortedItems.size(); i++) {
			long itemGraph = sortedItems.get(i);
			for (int j = 0; j < candidateItemsFiltered.size(); j++) {
				RecommendedItem itemRecommender = candidateItemsFiltered.get(j);
				if (itemGraph == itemRecommender.getItemID()) {
					if (finalRecommendedItems.isEmpty()) {
						finalRecommendedItems.add(itemRecommender);
					} else {
						if (!finalRecommendedItems.contains(itemRecommender)) {
							finalRecommendedItems.add(itemRecommender);
						}
					}
				}
			}
		}

		return finalRecommendedItems;
	}

	// Para Baseline: K-Ideal
	public List<RecommendedItem> recommendIdeal(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {
		// Las lista de items candidatos se lleva a una lista de entero.
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsToLong);
		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsToLong, initialVertex);
		// Los items son ordenados teniendo en cuenta una trayectoria
		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);

		// Obtiene un path
		finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
		convertRecommendItemsToPath(sortedItems);

		// Obtiene nuevamente la lista de RecommendedItem pero teniendo en
		// cuenta la trayectoria.
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		for (int i = 0; i < sortedItems.size(); i++) {
			long itemGraph = sortedItems.get(i);
			for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
				RecommendedItem itemRecommender = candidateItemsFromRecommender.get(j);
				if (itemGraph == itemRecommender.getItemID()) {
					if (finalRecommendedItems.isEmpty()) {
						finalRecommendedItems.add(itemRecommender);
					} else {
						if (!finalRecommendedItems.contains(itemRecommender)) {
							finalRecommendedItems.add(itemRecommender);
						}
					}
				}
			}
		}
		return finalRecommendedItems;
	}

	// Para Baseline: Random, ALL
	public List<RecommendedItem> recommendBaseline(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {

		// Las lista de items candidatos se lleva a una lista de entero.
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);

		// Candidate items from graph
		long initialVertex = candidateItemsToLong.get(0);

		// Obtiene un path
		//System.out.println("Door: " + door + "; shortest path " + DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex) + "; TrajectoryStrategy.graph: " + trajectoryStrategy.graph + "; initialVertex: " + initialVertex);
		finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
		convertRecommendItemsToPath(candidateItemsToLong);

		// Obtiene nuevamente la lista de RecommendedItem pero teniendo en
		// cuenta la trayectoria.
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		for (int i = 0; i < candidateItemsToLong.size(); i++) {
			long itemGraph = candidateItemsToLong.get(i);
			for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
				RecommendedItem itemRecommender = candidateItemsFromRecommender.get(j);
				if (itemGraph == itemRecommender.getItemID()) {
					if (finalRecommendedItems.isEmpty()) {
						finalRecommendedItems.add(itemRecommender);
					} else {
						if (!finalRecommendedItems.contains(itemRecommender)) {
							finalRecommendedItems.add(itemRecommender);
						}
					}
				}
			}
		}
		return finalRecommendedItems;
	}

	private void convertRecommendItemsToPath(List<Long> items) {
		long start = items.get(0);
		for (int i = 1; i < items.size(); i++) {
			long end = items.get(i);
			if (end != start) {
				finalPath += ", " + ShortestTrajectoryStrategy.preprocessingPath(start, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, start, end).toString());
				start = end;
			}
		}
	}

	private List<Long> sortingItemsBeginBy(long initialVertex, List<Long> items) {
		List<Long> sortedItems = new LinkedList<>();
		List<Long> pathTemp = new LinkedList<>();
		for (int i = 0; i < items.size(); i++) {
			long vertex = items.get(i);
			if (vertex == initialVertex) {
				if (i == 0) {
					return items;
				} else {
					sortedItems = items.subList(i, items.size());
					pathTemp = items.subList(0, i);
					break;
				}
			}
		}
		sortedItems.addAll(pathTemp);
		return sortedItems;
	}

	private long getItemClosestToTheFrontDoor(long door, List<Long> items) {
		long itemClosest = 0;
		double shorterDistance = 999999;
		for (int i = 0; i < items.size(); i++) {
			long item = items.get(i);
			// Esta condicion es para que no salga el mismo como el mas cercano.
			if (item != door) {
				String itemLocation = trajectoryStrategy.diccionaryItemLocation.get(item);
				String doorLocation = trajectoryStrategy.diccionaryItemLocation.get(door);
				double distance = Distance.distanceBetweenTwoPoints(Double.valueOf(itemLocation.split(", ")[0]).doubleValue(), Double.valueOf(itemLocation.split(", ")[1]).doubleValue(), Double.valueOf(doorLocation.split(", ")[0]).doubleValue(), Double.valueOf(doorLocation.split(", ")[1]).doubleValue());
				if (distance < shorterDistance) {
					shorterDistance = distance;
					itemClosest = item;
				}
			}
		}
		return itemClosest;
	}

	// private long getItemClosestToTheFrontDoor(long door, List<Long> items) {
	// long itemClosest = 0;
	// double shorterDistance = 999999;
	// for (int i = 0; i < items.size(); i++) {
	// long item = items.get(i);
	// String itemLocation =
	// trajectoryStrategy.diccionaryItemLocation.get(item);
	// String doorLocation =
	// trajectoryStrategy.diccionaryItemLocation.get(door);
	// double distance =
	// distanceBetweenTwoPoints(Double.valueOf(itemLocation.split(",
	// ")[0]).doubleValue(),
	// Double.valueOf(itemLocation.split(", ")[1]).doubleValue(),
	// Double.valueOf(doorLocation.split(", ")[0]).doubleValue(),
	// Double.valueOf(doorLocation.split(", ")[1]).doubleValue());
	// if (distance < shorterDistance) {
	// shorterDistance = distance;
	// itemClosest = item;
	// }
	// }
	// return itemClosest;
	// }

	//public static double distanceBetweenTwoPoints(double x1, double y1, double x2, double y2) {
	//	return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	//}

	// Repeated
	public List<Long> listRecommendedItemToListLong(List<RecommendedItem> candidateItems) {
		List<Long> itemList = new LinkedList<>();
		// Descomentar para SVD y comentar para Random
		for (RecommendedItem item : candidateItems) {
			itemList.add(item.getItemID());
		}
		return itemList;
	}

	private List<RecommendedItem> listRecommendedItemThreshold(List<RecommendedItem> candidateItems) {
		List<RecommendedItem> itemList = new LinkedList<>();
		// Descomentar para SVD y comentar para Random
		for (RecommendedItem item : candidateItems) {
			// Umbral de rating para que no recomiende items con predicciones
			// muy bajas.
			if (item.getValue() >= threshold) {
				itemList.add(item);
			}
		}

		if (itemList.isEmpty()) {
			for (RecommendedItem item : candidateItems) {
				itemList.add(item);
			}
		}
		return itemList;
	}

	/*private List<RecommendedItem> listRecommendedItemWithoutThreshold(List<RecommendedItem> candidateItems) {
		List<RecommendedItem> itemList = new LinkedList<>();
		for (RecommendedItem item : candidateItems) {
			itemList.add(item);
		}
		return itemList;
	}*/

	public long getDoor() {
		return door;
	}

	public void setDoor(long door) {
		this.door = door;
	}

	public String getFinalPath() {
		return finalPath;
	}

	public void setFinalPath(String finalPath) {
		this.finalPath = finalPath;
	}

	// Repeated
	public AbstractTrajectoryStrategy getTrajectoryStrategy() {
		return trajectoryStrategy;
	}

	public void setTrajectoryStrategy(AbstractTrajectoryStrategy trajectoryStrategy) {
		this.trajectoryStrategy = trajectoryStrategy;
	}
}
