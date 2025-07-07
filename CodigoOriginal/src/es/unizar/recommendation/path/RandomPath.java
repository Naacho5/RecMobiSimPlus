package es.unizar.recommendation.path;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.unizar.access.DataAccessItemFile;
import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;
import es.unizar.util.ElementIdMapper;
import es.unizar.gui.simulation.*;

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

		// Añadido por Nacho Palacio 2025-04-23.
		startVertex = ensureInternalId(startVertex, ElementIdMapper.CATEGORY_ITEM);

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

		itemsByRoom = convertAndValidateItems(itemsByRoom, "itemsByRoom", numberOfItemsInMuseum);

		// Añadido por Nacho Palacio 2025-04-22.
		// Verify null value
        if (itemsByRoom == null) {
            itemsByRoom = new LinkedList<>();
            itemsDoorVisited_cloned.put(room, itemsByRoom);
        }

		// Modificado por Nacho Palacio 2025-06-28
		if (ElementIdMapper.isInCorrectRange(startVertex, ElementIdMapper.CATEGORY_ITEM)) {
			itemsByRoom.remove(startVertex);
		}

		itemToVisit = startVertex;
		itemVisited = addItemVisited(itemToVisit, itemVisited);

		while (currentTime < inputTime && itemVisited.size() < numberOfItems) {
			
			if (itemsByRoom == null) {
				itemsByRoom = new LinkedList<>();
				break;
			}

			int items = itemsByRoom.size();

			if (items > 0) {
				// Seleccionar CUALQUIER elemento aleatoriamente
				int indexRandom = random.nextInt(itemsByRoom.size());
				itemToVisit = itemsByRoom.get(indexRandom);
				
				if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_ITEM)) {

					itemsByRoom.remove(itemToVisit);
					endVertex = itemToVisit;
					vertex = getCurrentVertex(startVertex, endVertex);

					finalPath += vertex;
					itemVisited = addItemVisited(itemToVisit, itemVisited);
					currentTime += getCurrentTime(startVertex, endVertex) + 
								Configuration.simulation.getDelayObservingPaintingInSecond();
					startVertex = endVertex;
					
				} else if (ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_DOOR) ||
           					ElementIdMapper.isInCorrectRange(itemToVisit, ElementIdMapper.CATEGORY_STAIRS)) {
					long connectedDoor = getConnectedDoor(itemToVisit);
					
					if (connectedDoor > 0) {
						String subpath = getToConnectedDoor(startVertex, itemToVisit, itemVisited, connectedDoor);
						
						room = getRoomFromItem(connectedDoor);

						roomVisited.add(room);
						
						itemsByRoom = (LinkedList<Long>) itemsDoorVisited_cloned.get(room);
						
						itemsByRoom = convertAndValidateItems(itemsByRoom, "newRoom_" + room, numberOfItemsInMuseum);
						
						if (itemsByRoom == null) {
							itemsByRoom = new LinkedList<>();
							itemsDoorVisited_cloned.put(room, itemsByRoom);
						}
						
						currentTime += getCurrentTimeConnectedDoors(subpath);
						finalPath += subpath;
						startVertex = getEndVertex(subpath);

					} else {
						// Puerta sin conexión válida
						itemsByRoom.remove(itemToVisit);
					}

				} else {
					itemsByRoom.remove(itemToVisit);
				}
			} else {
				// No hay más elementos en la habitación
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


	/**
	 * Modificado por Nacho Palacio 2025-04-23.
	 * Inicializa el mapa de ítems por habitación usando IDs externos.
	 */
	public void initializeItemsByRoom(Map<Integer, List<Long>> roomItems) {	
		// Crear nuevo mapa si es nulo
		if (itemsDoorVisited == null) {
			itemsDoorVisited = new HashMap<>();
		}

		// Añadido por Nacho Palacio 2025-04-26.
		if (doorsByRoomMap == null) {
			doorsByRoomMap = new HashMap<>();
		}
		
		// Modificado por Nacho Palacio 2025-06-08
		for (Map.Entry<Integer, List<Long>> entry : roomItems.entrySet()) {
			int roomId = entry.getKey();
			List<Long> items = entry.getValue();
			
			List<Long> allRoomElements = new LinkedList<>();
			
			for (Long itemId : items) {
				if (itemId != null && itemId > 0) {
					allRoomElements.add(itemId);
				}
			}
			
			itemsDoorVisited.put(roomId, new LinkedList<>(allRoomElements));
			
			
			if (allRoomElements.isEmpty()) {
				LinkedList<Long> defaultItems = new LinkedList<>();
				defaultItems.add(1001L); // Un ítem genérico
				itemsDoorVisited.put(roomId, defaultItems);
			}
		}
	}


}
