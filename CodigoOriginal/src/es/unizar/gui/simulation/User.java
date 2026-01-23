package es.unizar.gui.simulation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.epidemic.data.ContactTrajectoryBuilder;
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
	public int room = -2;
	public DataAccessRoomFile dataAccessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
	
	public boolean hasFinishedVisit;
	
	public mxCell userCell;

	public es.unizar.epidemic.general.UserEpidemicExtension epidemicExtension; // Added by Nacho Palacio 2025-07-15
	
	public double totalObservationTime = 0.0;

	public int totalItemsObserved = 0;

	public List<ContactTrajectoryBuilder.UserRoomEvent> contactTrajectory; // Added by Nacho Palacio 2025-10-05

	// Added by Nacho Palacio 2025-10-12
	public String pathString;
    public List<String> pathList;

	// Move user X_DISPLACEMENT pixels in order to not collapse with item while watching it.
	private static final int X_DISPLACEMENT = -5; // Move user a bit to the left

	public User(int userID, boolean isSpecialUser)  throws IOException {
		this.userID = userID;
		this.x = -50;
		this.y = -50;
		this.drawImage = true;
		this.isSpecialUser = isSpecialUser;
		this.hasFinishedVisit = false;

		// Added by Nacho Palacio 2025-07-15
		this.epidemicExtension = new es.unizar.epidemic.general.UserEpidemicExtension();
		
		createUserCell();
	}

	/**
	 * Gets the user's epidemic extension.
	 * 
	 * @return the user's epidemic extension
	 */
	public es.unizar.epidemic.general.UserEpidemicExtension getEpidemicExtension() {
        return epidemicExtension;
    }

	/**
	 * Gets the contact trajectory of the user.
	 * 
	 * @return the contact trajectory of the user
	 */
	public List<ContactTrajectoryBuilder.UserRoomEvent> getContactTrajectory() {
		return contactTrajectory;
	}
    
	/**
	 * Sets the user's epidemic extension.
	 * 
	 * @param epidemicExtension the epidemic extension to set
	 */
    public void setEpidemicExtension(es.unizar.epidemic.general.UserEpidemicExtension epidemicExtension) {
        this.epidemicExtension = epidemicExtension;
    }

	/**
	 * Sets the contact trajectory of the user.
	 * 
	 * @param trajectory the contact trajectory to set
	 */
	public void setContactTrajectory(List<ContactTrajectoryBuilder.UserRoomEvent> trajectory) {
		this.contactTrajectory = trajectory;
	}
	

	/**
	 * The user's next move in the current room.
	 * 
	 * @param nextLocation: the user's next location
	 * @param room:         the current room
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
	 * @param userLocation the current user location
	 */
	public void getRoomOfTheUser() {
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
