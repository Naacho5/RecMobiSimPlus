package es.unizar.recommendation.path;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.unizar.gui.Configuration;

public class RandomPath extends Path {

	public RandomPath() {
		super();
	}

	/**
	 * Generate a non-RS user path, applying the random strategy and
	 * considering the specified time (in the Configuration form).
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String generatePath(long startVertex) {
		String finalPath = "";
		long endVertex = 0;
		long itemToVisit = 0;
		// Store the visited rooms.
		List<Integer> roomVisited = new LinkedList<>();
		// Store the visited items (sculptures and paintings).
		List<Long> itemVisited = new LinkedList<>();
		String vertex = null;
		double currentTime = 0;
		// Time for the paths (of hour to second).
		double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
		int numberOfItemsInMuseum = accessItemFile.getNumberOfItems();

		// Get the room to which the item (or non-RS user) belongs.
		int room = getRoomFromItem(startVertex);
		roomVisited.add(room);
		// Get the items (sculptures, paintings and doors) from a specified room.
		Map<Object, Object> itemsDoorVisited_cloned = itemsDoorVisited.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> new LinkedList<Object>(e.getValue())));
		LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
		
		itemsByRoom.remove(startVertex);
		itemToVisit = startVertex;
		itemVisited = addItemVisited(itemToVisit, itemVisited);

		// While the visit time does not finish.
		while (currentTime < inputTime && itemVisited.size() < numberOfItems) {
			
			int items = itemsByRoom.size();
			
			if (items > 0) {
				// Get an item to visit by the non-RS user randomly (without repeating).
				int indexRandom = random.nextInt(itemsByRoom.size());
				itemToVisit = itemsByRoom.get(indexRandom);
				// If the next item to visit is a painting or sculpture (range: 1-240):
				if (itemToVisit <= numberOfItemsInMuseum) {
					// Remove the chosen item from the available items (sculptures, paintings, doors
					// and stairs). The user cannot visit seen items (sculptures and paintings) but
					// can pass again through stairs or doors to move between rooms or floors.
					itemsByRoom.remove(itemToVisit);
					endVertex = itemToVisit;
					// Get a new vertex.
					vertex = getCurrentVertex(startVertex, endVertex);
					// Add the new vertex to the final path.
					finalPath += vertex;
					// Stores the visited item (painting or sculpture).
					itemVisited = addItemVisited(itemToVisit, itemVisited);
					// Get the sum of: time the non-RS user needs to move from one item
					// (painting or sculpture) to another + time the non-RS user needs to
					// observe the item (painting or sculpture) to be visited.
					currentTime += getCurrentTime(startVertex, endVertex)
							+ Configuration.simulation.getDelayObservingPaintingInSecond();
					startVertex = endVertex;
				} else {
					// If the next item to visit is a door or stairs (range: 241-312):
					// Get the connection of the current door.
					long connectedDoor = getConnectedDoor(itemToVisit);
					if (connectedDoor > 0) {
						// Get the sub-path necessary to go from one item (sculpture or painting) to
						// another through doors.
						String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
						// Get the room to which the item (or non-RS user) or door belongs.
						room = getRoomFromItem(connectedDoor);
						// Get the items (sculptures, paintings and doors) from a specified room.
						itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
						// Get the time the user needs to move from an item (sculpture, painting, door
						// or stairs) to a door or stair.
						currentTime += getCurrentTimeConnectedDoors(subpath);
						// Add the new sub-path to the final path.
						finalPath += subpath;
						startVertex = getEndVertex(subpath);
					}
					else {
						// This element is wrong -> remove
						itemsByRoom.remove(itemToVisit);
					}
				}
			}
			else {
				break;
			}
		}
		
		finalPath = eraseRepeatedObjects(finalPath);
		
		if (finalPath.length() >= 2) {
			// To remove the "," at the end of the generated path.
			return finalPath.substring(0, finalPath.length() - 2);
		}
		else {
			return finalPath;
		}
	}
}
