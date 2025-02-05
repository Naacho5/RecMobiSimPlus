package es.unizar.gui.simulation;

import java.io.File;
import java.io.IOException;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.gui.MainSimulator;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.util.Literals;

public class User {

	public int userID;
	public double x;
	public double y;
	//public BufferedImage nonSpecialUserImage;
	//public BufferedImage specialUserImage;
	//public ImageLabel userImage;
	public boolean drawImage;
	public boolean isSpecialUser;
	public int room = -1;
	public DataAccessRoomFile dataAccessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
	
	public boolean hasFinishedVisit;
	
	public mxCell userCell;
	
	// Move user X_DISPLACEMENT pixels in order to not collapse with item while watching it.
	private static final int X_DISPLACEMENT = -5; // Move user a bit to the left

	public User(int userID, boolean isSpecialUser)  throws IOException {
		this.userID = userID;
		this.x = -50;
		this.y = -50;
		this.drawImage = true;
		this.isSpecialUser = isSpecialUser;
		this.hasFinishedVisit = false;
		
		createUserCell();
	}

	/**
	 * The user's next move in the current room.
	 * 
	 * @param nextLocation: The user's next location.
	 * @param room:         The current room.
	 */
	public void move(String nextLocation, int room) {
		this.x = Double.valueOf(nextLocation.split(", ")[0]).doubleValue();
		this.y = Double.valueOf(nextLocation.split(", ")[1]).doubleValue();
		this.room = room;
		
		// Change userCell's positions
		mxGeometry geo = new mxGeometry((int) x + X_DISPLACEMENT, (int) y, DrawFloorGraph.WITDH, DrawFloorGraph.HEIGHT);
		this.userCell.setGeometry(geo);
		
		// Modify 
//		String cellLabel = "User, " + Integer.toString(this.userID) + ", " + this.isSpecialUser + ", " + this.room;
//		userCell.setValue(cellLabel); // set cell's value
		
		//MainMuseumSimulator.floor.changeUserPosition(userID,x,y);
	}

	/**
	 * Draw the users in their current positions.
	 * 
	 * NOT USED IN NEW VERSION -> USERS INCLUDED AS CELLS IN GRAPH
	 * 
	 * @param g
	 * @throws IOException
	 */
	/*public void draw(Graphics g) {
		//
		if (isSpecialUser) {
			g.drawImage(specialUserImage, (int) x + X_DISPLACEMENT, (int) y, null);
			//g.drawString(Integer.toString(this.userID), (int) x + X_DISPLACEMENT, (int) y);
		} else {
			g.drawImage(nonSpecialUserImage, (int) x + X_DISPLACEMENT, (int) y, null);
			//g.drawString(Integer.toString(this.userID), (int) x + X_DISPLACEMENT, (int) y);
		}
		g.drawString(Integer.toString(this.userID), (int) x + X_DISPLACEMENT, (int) y);
		g.setColor(Color.DARK_GRAY);
		g.setFont(new Font("default", Font.BOLD, 10));
		
		// CORRECT 
		//System.out.println("Painting user " + this.userID + ": " + this.x + "," + this.y + " (room " + this.room + ")");
	}*/

	/**
	 * Get room from current user, by using his/her location.
	 * 
	 * @param userLocation The current user location.
	 */
	public void getRoomOfTheUser() {
		// int numberRooms = dataAccessRoomFile.getNumberOfRoom();
		
		// for (int room = 1; room <= numberRooms; room++) {
			
			/*
			List<Integer> xpoints = new ArrayList<Integer>();
			List<Integer> ypoints = new ArrayList<Integer>();
			
			int numCorners = dataAccessRoomFile.getRoomNumberCorner(room);
			for(int i = 1; i <= numCorners; i++) {
				String xy = dataAccessRoomFile.getRoomCornerXY(i, room);
				String[] array = xy.split(", ");
				xy = array[1] + ", " + array[0];
				
				int x = (int) Double.valueOf(xy.split(", ")[0]).doubleValue();
				int y = (int) Double.valueOf(xy.split(", ")[1]).doubleValue();
				
				xpoints.add(x);
				ypoints.add(y);
				
			}
			
			int[] xpointsarr = xpoints.stream().mapToInt(i->i).toArray();
			int[] ypointsarr = ypoints.stream().mapToInt(i->i).toArray();
			Shape shape = new Polygon(xpointsarr, ypointsarr, xpoints.size()); // no matter xpoints.size() or ypoints.size()
			Area area = new Area(shape);

			if (area.contains(Double.valueOf(this.x).doubleValue(), Double.valueOf(this.y).doubleValue())) {
				this.room = room;
				// If room is found, stop looking for the room
				break;
			}
			*/
			
			
			/*
			// Corners of the room.
			String xy1 = dataAccessRoomFile.getRoomCornerXY(1, room);
			String[] array = xy1.split(", ");
			xy1 = array[1] + ", " + array[0];
			String xy2 = dataAccessRoomFile.getRoomCornerXY(2, room);
			array = xy2.split(", ");
			xy2 = array[1] + ", " + array[0];
			String xy3 = dataAccessRoomFile.getRoomCornerXY(3, room);
			array = xy3.split(", ");
			xy3 = array[1] + ", " + array[0];
			String xy4 = dataAccessRoomFile.getRoomCornerXY(4, room);
			array = xy4.split(", ");
			xy4 = array[1] + ", " + array[0];
			// Array of 4 points of the room.
			int[] xpoints = new int[4];
			xpoints[0] = (int) Double.valueOf(xy1.split(", ")[0]).doubleValue();
			xpoints[1] = (int) Double.valueOf(xy2.split(", ")[0]).doubleValue();
			xpoints[2] = (int) Double.valueOf(xy3.split(", ")[0]).doubleValue();
			xpoints[3] = (int) Double.valueOf(xy4.split(", ")[0]).doubleValue();
			int[] ypoints = new int[4];
			ypoints[0] = (int) Double.valueOf(xy1.split(", ")[1]).doubleValue();
			ypoints[1] = (int) Double.valueOf(xy2.split(", ")[1]).doubleValue();
			ypoints[2] = (int) Double.valueOf(xy3.split(", ")[1]).doubleValue();
			ypoints[3] = (int) Double.valueOf(xy4.split(", ")[1]).doubleValue();
			
			
			if(MainMuseumSimulator.floor.rooms.get(room).contains(Double.valueOf(this.x).doubleValue(), Double.valueOf(this.y).doubleValue())) {
				this.room = room;
				break;
			};
			
		}*/
		this.room = MainSimulator.floor.getRoomFromPosition((int) x, (int) y);
	}
	
	/**
	 * Creates the user's mxCell to be inserted in the scenario graph.
	 * IOException can be thrown if 
	 * 
	 * mxCell doc: https://jgraph.github.io/mxgraph/docs/js-api/files/model/mxCell-js.html#mxCell.mxCell
	 * 
	 * @throws IOException
	 */
	public void createUserCell() {
		
		userCell = new mxCell();
		userCell.setVertex(true); // Set user cell as vertex -> If not, not printed
		
		// Set icon: special/non-RS user images
		String icon; // Icon is userCell's style (insertVertex last param)
		if (isSpecialUser) {
			icon = "shape=image;image=" + "/resources/images/special_user.png";
		} else {
			icon = "shape=image;image=" + "/resources/images/non_special_user.png";
		}
		
		// DON'T PRINT USER'S LABEL
//		icon += ";noLabel=1";
		
		// Set cell's style
		userCell.setStyle(icon);
		
		// Label is cell's value: User,id,isSpecialuser,room
//		String cellLabel = "User, " + Integer.toString(this.userID) + ", " + this.isSpecialUser + ", " + this.room;
		String cellLabel = "User " + Integer.toString(this.userID);
		if(this.isSpecialUser) cellLabel += " (special)";
		userCell.setValue(cellLabel); // set cell's value
		
		// Set cell's geometry
		// function mxGeometry(x, y, width, height)
		mxGeometry geo = new mxGeometry((int) x + X_DISPLACEMENT, (int) y, DrawFloorGraph.WITDH, DrawFloorGraph.HEIGHT);
		userCell.setGeometry(geo);
		
		// Set cell's id
		userCell.setId(String.valueOf(userID));
		
		// Set cell as visible if specified
		userCell.setVisible(drawImage);
	}

	@Override
	public String toString() {
		return "[userID: " + userID + ", location: (" + x + ";" + y + "), isSpecialUser: " + isSpecialUser + ", room: " + room + "]";
	}

}
