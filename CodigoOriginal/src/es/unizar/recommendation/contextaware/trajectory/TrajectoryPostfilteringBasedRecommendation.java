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
	private Long currentVertex = null;
	public Long recommendedItem = null; // ID of the recommender item in recommendBaseline
	public Long internalCurrentVertex = null; // Internal ID of current vertex in recommendBaseline

	private static final int MAX_FINAL_PATH_LENGTH = 10000;


	public TrajectoryPostfilteringBasedRecommendation(DBDataModel dataModel, String dbURL, AbstractTrajectoryStrategy trajectoryStrategy, long entranceDoor, float threshold) throws Exception {
		super(dataModel, 0, dbURL, null, 0, 0);
		this.setTrajectoryStrategy(trajectoryStrategy);
		this.door = entranceDoor;
		this.finalPath = null;
		this.threshold = threshold;
	}

	public TrajectoryPostfilteringBasedRecommendation(DBDataModel dataModel, String dbURL, AbstractTrajectoryStrategy trajectoryStrategy, long entranceDoor, float threshold, Long currentVertex) throws Exception {
		super(dataModel, 0, dbURL, null, 0, 0);
		this.setTrajectoryStrategy(trajectoryStrategy);
		this.door = entranceDoor;
		this.finalPath = null;
		this.threshold = threshold;
		this.currentVertex = currentVertex;
	}

	/**
	 * Recommends items based on the trajectory strategy.
	 * @param userID The ID of the user for whom to recommend items.
	 * @param howMany The number of items to recommend.
	 * @return A list of recommended items.
	 * @throws TasteException if an error occurs during the recommendation process.
	 */
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

		try {
			if (getRecommender() instanceof org.apache.mahout.cf.taste.recommender.UserBasedRecommender) {
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

		/* Added by Nacho Palacio 2025-04-14. */
		if (candidateItemsFiltered == null || candidateItemsFiltered.isEmpty()) {
			return candidateItemsFromRecommender;
        }

		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFiltered);

		// Added by Nacho Palacio 2025-10-22
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			candidateItemsInternal.add(es.unizar.util.ElementIdMapper.convertToRangeId(itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM));
		}

		/* Added by Nacho Palacio 2025-04-14. */
		if (candidateItemsToLong.isEmpty()) {
            return candidateItemsFiltered;
        }

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsInternal); // Modified by Nacho Palacio 2025-10-22
		
		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsInternal, initialVertex); // Modified by Nacho Palacio 2025-10-22
		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);

		/* Added by Nacho Palacio 2015-04-14. */
		try {
			finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, 
                      DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
            
			if (finalPath == null) {
                return candidateItemsFiltered;
            }
            
            convertRecommendItemsToPath(sortedItems);
        } catch (Exception e) {
            return candidateItemsFiltered;
        }

		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();

		for (int i = 0; i < sortedItems.size(); i++) {
			long itemGraphInternal = sortedItems.get(i);
			long itemGraphExternal = es.unizar.util.ElementIdMapper.getBaseId(itemGraphInternal);
			for (int j = 0; j < candidateItemsFiltered.size(); j++) {
				RecommendedItem itemRecommender = candidateItemsFiltered.get(j);
				if (itemGraphExternal == itemRecommender.getItemID()) {
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

	/**
	 * Recommends the ideal trajectory based on the candidate items.
	 * @param candidateItemsFromRecommender List of RecommendedItem to consider.
	 * @return List of RecommendedItem representing the ideal trajectory.
	 * @throws TasteException if an error occurs during the recommendation process.
	 */
	public List<RecommendedItem> recommendIdeal(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {
		if (candidateItemsFromRecommender == null || candidateItemsFromRecommender.isEmpty()) {
			return new LinkedList<>();
		}
    
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);
    
		// Added by Nacho Palacio 2025-10-22
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			long internalId = es.unizar.util.ElementIdMapper.convertToRangeId(itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM);
			candidateItemsInternal.add(internalId);
		}
		
		if (candidateItemsInternal.isEmpty()) {
			return candidateItemsFromRecommender;
		}

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsInternal); // Modified by Nacho Palacio 2025-10-22
		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsInternal, initialVertex); // Modified by Nacho Palacio 2025-10-22

		if (pathHamiltonianCycle == null || pathHamiltonianCycle.isEmpty()) {
			return candidateItemsFromRecommender;
		}

		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);

		long internalDoor = es.unizar.util.ElementIdMapper.convertToRangeId(door, es.unizar.util.ElementIdMapper.CATEGORY_DOOR);

		if (!trajectoryStrategy.graph.containsVertex(internalDoor)) {
			return candidateItemsFromRecommender;
		}
		if (!trajectoryStrategy.graph.containsVertex(initialVertex)) {
			return candidateItemsFromRecommender;
		}

		convertRecommendItemsToPath(sortedItems);
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();

		for (int i = 0; i < sortedItems.size(); i++) {
			long itemGraphInternal = sortedItems.get(i);
			long itemGraphExternal = es.unizar.util.ElementIdMapper.getBaseId(itemGraphInternal);
			for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
				RecommendedItem itemRecommender = candidateItemsFromRecommender.get(j);
				if (itemGraphExternal == itemRecommender.getItemID()) {
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

	/**
	 * Recommends items based on the baseline trajectory strategy.
	 * @param candidateItemsFromRecommender List of RecommendedItem to consider for the baseline recommendation.
	 * @return List of RecommendedItem representing the baseline recommendation based on the trajectory strategy.
	 * @throws TasteException
	 */
	public List<RecommendedItem> recommendBaseline(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {
		this.finalPath = null;
		this.recommendedItem = null;
		this.internalCurrentVertex = null;

		if (candidateItemsFromRecommender == null) {
			return new ArrayList<>();
		}

		if (candidateItemsFromRecommender.isEmpty()) {
			return new ArrayList<>();
		}

		long internalDoor = es.unizar.util.ElementIdMapper.convertToRangeId(
				door, es.unizar.util.ElementIdMapper.CATEGORY_DOOR);

		if (this.currentVertex != null) {
			if (trajectoryStrategy.graph.containsVertex(this.currentVertex)) {
				internalCurrentVertex = this.currentVertex;
			} else {
				long asItem = es.unizar.util.ElementIdMapper.convertToRangeId(
						this.currentVertex, es.unizar.util.ElementIdMapper.CATEGORY_ITEM);
				
				if (trajectoryStrategy.graph.containsVertex(asItem)) {
					internalCurrentVertex = asItem;
				} else {
					long asDoor = es.unizar.util.ElementIdMapper.convertToRangeId(
							this.currentVertex, es.unizar.util.ElementIdMapper.CATEGORY_DOOR);
					
					if (trajectoryStrategy.graph.containsVertex(asDoor)) {
						internalCurrentVertex = asDoor;
					}
				}
			}
		} else {
			System.out.println("\ncurrentVertex is null");
		}


		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);
	
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			long internalId = es.unizar.util.ElementIdMapper.convertToRangeId(
					itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM);
			boolean exists = trajectoryStrategy.graph.containsVertex(internalId);

			candidateItemsInternal.add(internalId);
		}


		if (!trajectoryStrategy.graph.containsVertex(internalDoor)) {
			return candidateItemsFromRecommender;
		}

		Long internalOriginVertex = null;
		List<org.jgrapht.graph.DefaultWeightedEdge> pathToFirstVertex = null;

		for (Long candidateVertex : candidateItemsInternal) {
			if (internalCurrentVertex != null && candidateVertex.equals(internalCurrentVertex)) {
				continue;
			}

			if (!trajectoryStrategy.graph.containsVertex(candidateVertex)) {
				continue;
			}

			List<org.jgrapht.graph.DefaultWeightedEdge> pathFromCurrent = null;

			if (internalCurrentVertex != null) {
				try {
					pathFromCurrent = DijkstraShortestPath.findPathBetween(
							trajectoryStrategy.graph, internalCurrentVertex, candidateVertex);

				} catch (Exception e) {
					System.out.println("exception = " + e.getClass().getName() +
							" -> " + e.getMessage());
					throw e;
				}
			}

			if (pathFromCurrent != null) {
				internalOriginVertex = internalCurrentVertex;
				pathToFirstVertex = pathFromCurrent;
			} else {
				try {
					List<org.jgrapht.graph.DefaultWeightedEdge> pathFromDoor =
							DijkstraShortestPath.findPathBetween(
									trajectoryStrategy.graph, internalDoor, candidateVertex);


					if (pathFromDoor != null) {
						internalOriginVertex = internalDoor;
						pathToFirstVertex = pathFromDoor;
					}
				} catch (Exception e) {
					System.out.println("ERROR in Dijkstra(door -> candidate)");
					throw e;
				}
			}

			if (pathToFirstVertex != null) {
				recommendedItem = es.unizar.util.ElementIdMapper.getBaseId(candidateVertex);
				break;
			}
		}

		if (pathToFirstVertex == null) {
			finalPath = "";
			return candidateItemsFromRecommender;
		}

		finalPath = ShortestTrajectoryStrategy.preprocessingPath(
				internalOriginVertex, pathToFirstVertex.toString());

		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();

		for (int i = 0; i < candidateItemsInternal.size(); i++) {
			long itemGraphInternal = candidateItemsInternal.get(i);
			long itemGraphExternal = es.unizar.util.ElementIdMapper.getBaseId(itemGraphInternal);

			for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
				RecommendedItem itemRecommender =
						(RecommendedItem) candidateItemsFromRecommender.get(j);

				if (itemGraphExternal == itemRecommender.getItemID()) {
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

	/**
	 * Convert the list of items in a path, considering the path between them.
	 * @param items List of items to convert to path.
	 */
	private void convertRecommendItemsToPath(List<Long> items) {
		long start = items.get(0);

		StringBuilder pathBuilder = new StringBuilder(finalPath != null ? finalPath : ""); // Added by Nacho Palacio 2025-12-08

		for (int i = 1; i < items.size(); i++) {
			// Added by Nacho Palacio 2025-12-08
			if (pathBuilder.length() > MAX_FINAL_PATH_LENGTH) {
                break;
            }

			long end = items.get(i);
			
			// Modified by Nacho Palacio 2025-12-08
			if (end != start) {
                if (trajectoryStrategy.graph.containsVertex(start) && trajectoryStrategy.graph.containsVertex(end)) {
                    var pathBetween = DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, start, end);
                    if (pathBetween != null) {
                        if (pathBuilder.length() > 0) {
                            pathBuilder.append(", ");
                        }
                        pathBuilder.append(ShortestTrajectoryStrategy.preprocessingPath(start, pathBetween.toString()));
                    }
                }
                start = end;
            }
        }
        
        finalPath = pathBuilder.toString();
	}

	/**
	 * Sort the list of items to start by the initial vertex, which is the closest to the door.
	 * @param initialVertex Initial vertex to start the path.
	 * @param items List of items to sort.
	 * @return Sorted list of items starting by the initial vertex.
	 */
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

	/**
	 * Get the item closest to the front door among a list of items.
	 * @param door ID of the door to calculate the distance.
	 * @param items List of items to calculate the closest one.
	 * @return ID of the item closest to the door. If there are no items, return 0.
	 */
	private long getItemClosestToTheFrontDoor(long door, List<Long> items) {
		long itemClosest = 0;
		double shorterDistance = 999999;
		for (int i = 0; i < items.size(); i++) {
			long item = items.get(i);
			if (item != door) {
				String itemLocation = trajectoryStrategy.diccionaryItemLocation.get(item);
				String doorLocation = trajectoryStrategy.diccionaryItemLocation.get(door);
				if (itemLocation == null || doorLocation == null) {
					continue;
				}
				double distance = Distance.distanceBetweenTwoPoints(
					Double.valueOf(itemLocation.split(", ")[0]).doubleValue(),
					Double.valueOf(itemLocation.split(", ")[1]).doubleValue(),
					Double.valueOf(doorLocation.split(", ")[0]).doubleValue(),
					Double.valueOf(doorLocation.split(", ")[1]).doubleValue());
				if (distance < shorterDistance) {
					shorterDistance = distance;
					itemClosest = item;
				}
			}
		}
		return itemClosest;
	}

	/**
	 * Convert a list of RecommendedItem to a list of Long with the item IDs.
	 * @param candidateItems List of RecommendedItem to convert.
	 * @return List of Long with the item IDs of the RecommendedItem list.
	 */
	public List<Long> listRecommendedItemToListLong(List<RecommendedItem> candidateItems) {
		List<Long> itemList = new LinkedList<>();
		for (RecommendedItem item : candidateItems) {
			itemList.add(item.getItemID());
		}
		return itemList;
	}

	/**
	 * Filter the list of RecommendedItem considering a threshold of rating. Only items with a rating equal or higher than the threshold will be returned. If no item meets the threshold, all items will be returned.
	 * @param candidateItems List of RecommendedItem to filter.
	 * @return List of RecommendedItem filtered considering the threshold. If no item meets the threshold, all items will be returned.
	 */
	private List<RecommendedItem> listRecommendedItemThreshold(List<RecommendedItem> candidateItems) {
		List<RecommendedItem> itemList = new LinkedList<>();
		for (RecommendedItem item : candidateItems) {
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

	public long getDoor() {
		return door;
	}

	public void setDoor(long door) {
		this.door = door;
	}

	public Long getCurrentVertex() {
		return currentVertex;
	}

	public void setCurrentVertex(Long currentVertex) {
		this.currentVertex = currentVertex;
	}
	

	public String getFinalPath() {
		return finalPath != null ? finalPath : "";
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
