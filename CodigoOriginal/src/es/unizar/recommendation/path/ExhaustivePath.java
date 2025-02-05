package es.unizar.recommendation.path;

import java.util.LinkedList;

import es.unizar.gui.Configuration;

public class ExhaustivePath extends Path {

	// Rooms connected by doors.
	public static String[] roomsConnectedByDoorsInitial = { "1-2;243", "2-3;246", "3-4;248", "4-5;250,249", "5-6;252",
			"6-7;256", "7-8;259", "8-9;264", "9-10;267", "10-11;269", "11-12;271,270", "12-13;274", "13-14;277,273,262",
			"14-15;278,261,274,275", "15-16;281", "16-17;284,287", "17-18;286", "18-19;289", "19-20;291", "20-21;293",
			"21-22;295", "22-23;299,296", "23-24;301", "24-25;304", "25-26;307,298", "26-1;310,297,308" };
	
	// Rooms connected by doors GC.
	public static String[] roomsConnectedByDoorsInitialGC = { "1-2;243", "2-3;246", "3-4;248", "4-5;250,249", "5-6;252",
				"6-7;256", "7-8;259", "8-9;264", "9-10;267", "10-11;269", "11-12;271,270", "12-13;274", "13-14;277,273,262",
				"14-15;278,261,274,275", "15-16;281", "16-17;284,287", "17-18;286", "18-19;289", "19-20;291", "20-21;293",
				"21-22;295", "22-23;299,296", "23-24;301", "24-25;304", "25-26;307,298", "26-1;310,297,308" };

	public ExhaustivePath() {
		super();
	}

	/**
	 * Generate a non-RS user path, applying the sequential strategy and
	 * considering the specified time (in the Configuration form).
	 * 
	 * @param startVertex: the initial position (it will be the position of a item,
	 *                     chosen randomly) of the non-RS user.
	 */
	@Override
	public String generatePath(long startVertex) {
		String finalPath = "";
		long endVertex = 0;
		double currentTime = 0;
		long itemToVisit = 0;
		String[] roomsConnectedByDoors = new String[roomsConnectedByDoorsInitial.length];

		// Get the room to which the initial item (or non-RS user) belongs.
		int room = getRoomFromItem(startVertex);
		// Sorts the rooms taking into account the current position of the non-special
		// user.
		roomsConnectedByDoors = orderByCurrentPosition(room, roomsConnectedByDoorsInitial);
		// Time for the paths (of hour to second).
		double inputTime = Configuration.simulation.getTimeForThePathsInSecond();
		int numberOfItemsInMuseum = accessItemFile.getNumberOfItems();
		itemToVisit = startVertex;

		// For each room, the user visits all the items.
		for (int r = 0; r < roomsConnectedByDoors.length; r++) {
			// While the visit time does not finish.
			if (currentTime < inputTime) {
				String[] rooms = roomsConnectedByDoors[r].split(";");
				room = Integer.valueOf(rooms[0].split("-")[0]).intValue();
				LinkedList<Long> itemsByRoom_temp = itemsDoorVisited.get(room);
				@SuppressWarnings("unchecked")
				LinkedList<Long> itemsByRoom = (LinkedList<Long>) itemsByRoom_temp.clone();
				// Remove the chosen item from the available items (sculptures, paintings, doors
				// and stairs). The user cannot visit seen items (sculptures and paintings) but
				// can pass again through stairs or doors to move between rooms or floors.
				if (startVertex <= numberOfItemsInMuseum) {
					itemsByRoom.remove(startVertex);
				}
				for (int i = 0; i < itemsByRoom.size(); i++) {
					itemToVisit = itemsByRoom.get(i);
					// If the next item to visit is a painting or sculpture (range: 1-240):
					if (itemToVisit <= numberOfItemsInMuseum) {
						endVertex = itemToVisit;
						String vertex = getCurrentVertex(startVertex, endVertex);
						finalPath += vertex;
						// Get the sum of: time the non-RS user needs to move from one item
						// (painting or sculpture) to another + time the non-RS user needs to
						// observe the item (painting or sculpture) to be visited.
						currentTime += getCurrentTime(startVertex, endVertex)
								+ Configuration.simulation.getDelayObservingPaintingInSecond();
						startVertex = endVertex;
					} else {
						// If the next item to visit is a door or stairs (range: 241-312):
						startVertex = endVertex;
						// Choose a door that is connected to the next room.
						String[] doors = rooms[1].split(",");
						for (int d = 0; d < doors.length; d++) {
							itemToVisit = Long.valueOf(doors[d]).longValue();
							endVertex = itemToVisit;
							String vertex = getCurrentVertex(startVertex, endVertex);
							finalPath += vertex;
							
							/*
							if (((startVertex == 241 || startVertex == 242 || startVertex == 275 || startVertex == 276)
									&& (endVertex == 279 || endVertex == 280 || endVertex == 308 || endVertex == 309))
									|| ((startVertex == 279 || startVertex == 280 || startVertex == 308
											|| startVertex == 309)
											&& (endVertex == 241 || endVertex == 242 || endVertex == 275
													|| endVertex == 276))) {
								currentTime += 60;
							}*/
							if (checkDoorsConnectedByStairs(startVertex, endVertex)) {
								currentTime += 60;
							}
							else {
								// Gets the time the non-RS user needs to move from a painting-door,
								// door-door, door-stairs, stair-door.
								currentTime += getCurrentTime(startVertex, endVertex);
							}
							startVertex = endVertex;
							if (room == 26 && startVertex == 308) {
								itemToVisit = 242;
							} else {
								if (room == 14 && startVertex == 275) {
									itemToVisit = 280;
								} else {
									itemToVisit = mapDoorConnected.get(startVertex);
								}
							}
							endVertex = itemToVisit;
							vertex = getCurrentVertex(startVertex, endVertex);
							finalPath += vertex;
							
							/*
							if (((startVertex == 241 || startVertex == 242 || startVertex == 275 || startVertex == 276)
									&& (endVertex == 279 || endVertex == 280 || endVertex == 308 || endVertex == 309))
									|| ((startVertex == 279 || startVertex == 280 || startVertex == 308
											|| startVertex == 309)
											&& (endVertex == 241 || endVertex == 242 || endVertex == 275
													|| endVertex == 276))) {
								currentTime += 60;
							}
							*/
							
							if (checkDoorsConnectedByStairs(startVertex, endVertex)) {
								currentTime += 60;
							}
							else {
								// Gets the time the non-RS user needs to move from a painting-door,
								// door-door, door-stairs, stair-door.
								currentTime += getCurrentTime(startVertex, endVertex);
							}
							startVertex = endVertex;
						}
						break;
					}
				}
			} else {
				// The time is over.
				break;
			}
		}
		// To remove the "," at the end of the generated path.
		return finalPath.substring(0, finalPath.length() - 2);
	}

	/**
	 * Sorts the rooms taking into account the current position of the non-special
	 * user.
	 * 
	 * @param room:                         The current room of the non-special
	 *                                      user.
	 * @param roomsConnectedByDoorsInitial: Rooms connected by doors.
	 * @return A array with the sorted rooms.
	 */
	public String[] orderByCurrentPosition(int room, String[] roomsConnectedByDoorsInitial) {
		String[] roomsConnectedByDoors = new String[roomsConnectedByDoorsInitial.length];
		int pos = 0;
		for (int i = 0; i < roomsConnectedByDoorsInitial.length; i++) {
			String text = roomsConnectedByDoorsInitial[i];
			if (text.startsWith(String.valueOf(room))) {
				pos = i;
				break;
			}
		}
		int j = 0;
		for (int i = pos; i < roomsConnectedByDoorsInitial.length; i++) {
			roomsConnectedByDoors[j] = roomsConnectedByDoorsInitial[i];
			j++;
		}
		for (int i = 0; i < pos; i++) {
			roomsConnectedByDoors[j] = roomsConnectedByDoorsInitial[i];
			j++;
		}
		return roomsConnectedByDoors;
	}
}
