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

	private static final int MAX_FINAL_PATH_LENGTH = 10000;


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

		/* Added by Nacho Palacio 2025-04-14. */
		if (candidateItemsFiltered == null || candidateItemsFiltered.isEmpty()) {
            return candidateItemsFromRecommender;
        }

		// Las lista previamente filtrada se lleva a una lista de entero con solo los items a recomendar
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFiltered);

		// Added by Nacho Palacio 2025-10-22
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			candidateItemsInternal.add(es.unizar.util.ElementIdMapper.convertToRangeId(itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM));
		}
		// System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommend: candidateItemsInternal = " + candidateItemsInternal);
					 
		/* Added by Nacho Palacio 2025-04-14. */
		if (candidateItemsToLong.isEmpty()) {
            return candidateItemsFiltered;
        }

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsInternal); // Modified by Nacho Palacio 2025-10-22

		// System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommend: initialVertex = " + initialVertex);
		
		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsInternal, initialVertex); // Modified by Nacho Palacio 2025-10-22
		// Los items son ordenados teniendo en cuenta una trayectoria
		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);

		// System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommend: sortedItems = " + sortedItems);

		// Obtiene un path
		// finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
		// convertRecommendItemsToPath(sortedItems);

		/* Added by Nacho Palacio 2015-04-14. */
		try {
			finalPath = ShortestTrajectoryStrategy.preprocessingPath(door, 
                      DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex).toString());
            
			// System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommend: finalPath inicial calculado.");
            // Si finalPath es nulo, devuelve los items filtrados sin trayectoria
            if (finalPath == null) {;
                return candidateItemsFiltered;
            }
            
            convertRecommendItemsToPath(sortedItems);
        } catch (Exception e) {
            // System.out.println("Error calculating path: " + e.getMessage());
            return candidateItemsFiltered;
        }

		// Obtiene la lista de RecommendedItem teniendo en cuenta la trayectoria.
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		// for (int i = 0; i < sortedItems.size(); i++) {
		// 	long itemGraph = sortedItems.get(i);
		// 	for (int j = 0; j < candidateItemsFiltered.size(); j++) {
		// 		RecommendedItem itemRecommender = candidateItemsFiltered.get(j);
		// 		if (itemGraph == itemRecommender.getItemID()) {
		// 			if (finalRecommendedItems.isEmpty()) {
		// 				finalRecommendedItems.add(itemRecommender);
		// 			} else {
		// 				if (!finalRecommendedItems.contains(itemRecommender)) {
		// 					finalRecommendedItems.add(itemRecommender);
		// 				}
		// 			}
		// 		}
		// 	}
		// }

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
		// System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommend: finalRecommendedItems = " + finalRecommendedItems);
		return finalRecommendedItems;
	}

	// Para Baseline: K-Ideal
	public List<RecommendedItem> recommendIdeal(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {
		// Las lista de items candidatos se lleva a una lista de entero.
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);
		// Added by Nacho Palacio 2025-10-22
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			candidateItemsInternal.add(es.unizar.util.ElementIdMapper.convertToRangeId(itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM));
		}

		System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommendIdeal: candidateItemsInternal = " + candidateItemsInternal);

		// Candidate items from graph
		long initialVertex = getItemClosestToTheFrontDoor(door, candidateItemsInternal); // Modified by Nacho Palacio 2025-10-22
		List<Long> pathHamiltonianCycle = getTrajectoryStrategy().getOptimalTrajectory(candidateItemsInternal, initialVertex); // Modified by Nacho Palacio 2025-10-22
		// Los items son ordenados teniendo en cuenta una trayectoria
		List<Long> sortedItems = sortingItemsBeginBy(initialVertex, pathHamiltonianCycle);
		System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommendIdeal: sortedItems = " + sortedItems);

		long internalDoor = es.unizar.util.ElementIdMapper.convertToRangeId(door, es.unizar.util.ElementIdMapper.CATEGORY_DOOR);

		if (!trajectoryStrategy.graph.containsVertex(internalDoor)) {
			System.err.println(" El grafo NO contiene el vértice de puerta (start): " + door);
			return candidateItemsFromRecommender;
		}
		if (!trajectoryStrategy.graph.containsVertex(initialVertex)) {
			System.err.println(" El grafo NO contiene el vértice de destino (initialVertex): " + initialVertex);
			return candidateItemsFromRecommender;
		}

		System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommendIdeal: initialVertex = " + initialVertex);

		// Obtiene un path
		finalPath = ShortestTrajectoryStrategy.preprocessingPath(internalDoor, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, internalDoor, initialVertex).toString());
		System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommendIdeal: finalPath inicial calculado.");
		convertRecommendItemsToPath(sortedItems);

		// Obtiene nuevamente la lista de RecommendedItem pero teniendo en
		// cuenta la trayectoria.
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		// for (int i = 0; i < sortedItems.size(); i++) {
		// 	long itemGraph = sortedItems.get(i);
		// 	for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
		// 		RecommendedItem itemRecommender = candidateItemsFromRecommender.get(j);
		// 		if (itemGraph == itemRecommender.getItemID()) {
		// 			if (finalRecommendedItems.isEmpty()) {
		// 				finalRecommendedItems.add(itemRecommender);
		// 			} else {
		// 				if (!finalRecommendedItems.contains(itemRecommender)) {
		// 					finalRecommendedItems.add(itemRecommender);
		// 				}
		// 			}
		// 		}
		// 	}
		// }

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

		System.out.println("✅ TrajectoryPostfilteringBasedRecommendation.recommendIdeal: finalRecommendedItems = " + finalRecommendedItems);
		return finalRecommendedItems;
	}

	// Para Baseline: Random, ALL
	public List<RecommendedItem> recommendBaseline(List<RecommendedItem> candidateItemsFromRecommender) throws TasteException {

		// Added by Nacho Palacio 2025-10-21
		this.finalPath = null; // limpiar antes de calcular
		if (candidateItemsFromRecommender == null || candidateItemsFromRecommender.isEmpty()) {
			System.err.println("[Postfiltering] Lista de ítems vacía, no se genera trayectoria.");
			return new ArrayList<>();
		}

		// Added by Nacho Palacio 2025-10-22
		long internalDoor = es.unizar.util.ElementIdMapper.convertToRangeId(door, es.unizar.util.ElementIdMapper.CATEGORY_DOOR);

		// Las lista de items candidatos se lleva a una lista de entero.
		List<Long> candidateItemsToLong = listRecommendedItemToListLong(candidateItemsFromRecommender);

		// Added by Nacho Palacio 2025-10-22
		List<Long> candidateItemsInternal = new ArrayList<>();
		for (Long itemId : candidateItemsToLong) {
			long internalId = es.unizar.util.ElementIdMapper.convertToRangeId(itemId, es.unizar.util.ElementIdMapper.CATEGORY_ITEM);
			candidateItemsInternal.add(internalId);
		}

		// Candidate items from graph
		// long initialVertex = candidateItemsToLong.get(0);
		long initialVertex = candidateItemsInternal.get(0); // Modified by Nacho Palacio 2025-10-22
		// System.out.println(" [DEBUG recommendBaseline] initialVertex (primer item interno): " + initialVertex);

		// System.out.println(" [DEBUG recommendBaseline] Verificando vértices en el grafo:");
		// System.out.println("   - Total vértices en grafo: " + trajectoryStrategy.graph.vertexSet().size());
		// System.out.println("   - ¿Grafo contiene internalDoor (" + internalDoor + ")? " + trajectoryStrategy.graph.containsVertex(internalDoor));
		// System.out.println("   - ¿Grafo contiene door original (" + door + ")? " + trajectoryStrategy.graph.containsVertex(door));
		// System.out.println("   - ¿Grafo contiene initialVertex (" + initialVertex + ")? " + trajectoryStrategy.graph.containsVertex(initialVertex));
		
		// System.out.println("   - Primeros 20 vértices del grafo:");
		// int count = 0;
		// for (Long vertex : trajectoryStrategy.graph.vertexSet()) {
		// 	if (count < 20) {
		// 		System.out.println("      " + vertex);
		// 		count++;
		// 	} else {
		// 		break;
		// 	}
		// }
		// Obtiene un path
		// System.out.println("Door: " + door + "; shortest path " + DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, door, initialVertex) + "; TrajectoryStrategy.graph: " + trajectoryStrategy.graph + "; initialVertex: " + initialVertex);
		
		// Added by Nacho Palacio 2025-09-28
		if (!trajectoryStrategy.graph.containsVertex(internalDoor)) {
			System.err.println(" [recommendBaseline] El grafo NO contiene internalDoor: " + internalDoor);
			System.err.println("   Intentando con door original: " + door);
			return candidateItemsFromRecommender;
		}
		else {
			// System.out.println("El grafo si contiene el vértice de puerta: " + internalDoor);
		}

		if (!trajectoryStrategy.graph.containsVertex(initialVertex)) {
			System.err.println(" [recommendBaseline] El grafo NO contiene initialVertex: " + initialVertex);
        	return candidateItemsFromRecommender;
		}
		else {
			// System.out.println("El grafo si contiene el vértice de destino: " + initialVertex);
		}

		
		// finalPath = ShortestTrajectoryStrategy.preprocessingPath(internalDoor, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, internalDoor, initialVertex).toString());
		// convertRecommendItemsToPath(candidateItemsInternal); // Modified by Nacho Palacio 2025-10-22

		// Modificado por Nacho Palacio 2026-03-30
		var pathToFirst = DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, internalDoor, initialVertex);
		if (pathToFirst == null) {
			System.err.println(" [recommendBaseline] No existe camino entre internalDoor=" + internalDoor
			+ " e initialVertex=" + initialVertex + ". Se devuelve la recomendación sin path inicial.");
			finalPath = "";
			return candidateItemsFromRecommender;
		}
		finalPath = ShortestTrajectoryStrategy.preprocessingPath(internalDoor, pathToFirst.toString());

		// Obtiene nuevamente la lista de RecommendedItem pero teniendo en
		// cuenta la trayectoria.
		// List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		// for (int i = 0; i < candidateItemsToLong.size(); i++) {
		// 	long itemGraph = candidateItemsToLong.get(i);
		// 	for (int j = 0; j < candidateItemsFromRecommender.size(); j++) {
		// 		RecommendedItem itemRecommender = candidateItemsFromRecommender.get(j);
		// 		if (itemGraph == itemRecommender.getItemID()) {
		// 			if (finalRecommendedItems.isEmpty()) {
		// 				finalRecommendedItems.add(itemRecommender);
		// 			} else {
		// 				if (!finalRecommendedItems.contains(itemRecommender)) {
		// 					finalRecommendedItems.add(itemRecommender);
		// 				}
		// 			}
		// 		}
		// 	}
		// }

		// Modified by Nacho Palacio 2025-10-22
		List<RecommendedItem> finalRecommendedItems = new LinkedList<>();
		for (int i = 0; i < candidateItemsInternal.size(); i++) {
			long itemGraphInternal = candidateItemsInternal.get(i);
			long itemGraphExternal = es.unizar.util.ElementIdMapper.getBaseId(itemGraphInternal); // Convierte a externo si es necesario
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

	private void convertRecommendItemsToPath(List<Long> items) {
		long start = items.get(0);

		StringBuilder pathBuilder = new StringBuilder(finalPath != null ? finalPath : ""); // Added by Nacho Palacio 2025-12-08

		for (int i = 1; i < items.size(); i++) {
			// Added by Nacho Palacio 2025-12-08
			if (pathBuilder.length() > MAX_FINAL_PATH_LENGTH) {
                System.out.println("Warning! Path truncado por exceder límite de " + MAX_FINAL_PATH_LENGTH + " caracteres");
                break;
            }

			long end = items.get(i);
			// if (end != start) {
			// 	finalPath += ", " + ShortestTrajectoryStrategy.preprocessingPath(start, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, start, end).toString());
			// 	start = end;
			// }
			// if (end != start) {
			// 	if (trajectoryStrategy.graph.containsVertex(start) && trajectoryStrategy.graph.containsVertex(end)) {
			// 		// System.out.println("✅ convertRecommendItemsToPath: El grafo contiene el vértice start=" + start + " y end=" + end);
			// 		if (DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, start, end) != null) {
			// 			finalPath += ", " + ShortestTrajectoryStrategy.preprocessingPath(start, DijkstraShortestPath.findPathBetween(trajectoryStrategy.graph, start, end).toString());
			// 		}
			// 		else {
			// 			// System.err.println(" convertRecommendItemsToPath: No se encontró un camino entre start=" + start + " y end=" + end);
			// 		}
			// 	} else {
			// 		// System.err.println(" convertRecommendItemsToPath: El grafo NO contiene el vértice start=" + start + " o end=" + end);
			// 	}
			// 	start = end;
			// }

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
		// Imprimir diccionario de ubicaciones
		// System.out.println(" [getItemClosestToTheFrontDoor] Diccionario de ubicaciones:");
		for (Long key : trajectoryStrategy.diccionaryItemLocation.keySet()) {
			// System.out.println("ID: " + key + " -> " + trajectoryStrategy.diccionaryItemLocation.get(key));
		}

		long itemClosest = 0;
		double shorterDistance = 999999;
		for (int i = 0; i < items.size(); i++) {
			long item = items.get(i);
			// Esta condicion es para que no salga el mismo como el mas cercano.
			// if (item != door) {
			// 	String itemLocation = trajectoryStrategy.diccionaryItemLocation.get(item);
			// 	String doorLocation = trajectoryStrategy.diccionaryItemLocation.get(door);
			// 	double distance = Distance.distanceBetweenTwoPoints(Double.valueOf(itemLocation.split(", ")[0]).doubleValue(), Double.valueOf(itemLocation.split(", ")[1]).doubleValue(), Double.valueOf(doorLocation.split(", ")[0]).doubleValue(), Double.valueOf(doorLocation.split(", ")[1]).doubleValue());
			// 	if (distance < shorterDistance) {
			// 		shorterDistance = distance;
			// 		itemClosest = item;
			// 	}
			// }
			if (item != door) {
				String itemLocation = trajectoryStrategy.diccionaryItemLocation.get(item);
				String doorLocation = trajectoryStrategy.diccionaryItemLocation.get(door);
				if (itemLocation == null || doorLocation == null) {
					System.err.println(" Ubicación no encontrada para el item " + item + " o la puerta " + door);
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
		// return finalPath;
		// Modified by Nacho Palacio 2025-10-21
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
