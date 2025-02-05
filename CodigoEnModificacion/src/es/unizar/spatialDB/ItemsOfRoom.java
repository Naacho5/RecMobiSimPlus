package es.unizar.spatialDB;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;

public class ItemsOfRoom extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	public DatabaseAccess db;
	private int roomLabel;
	private JFrame parent;
	private int mapID;
	private JScrollPane itemListScroller;

	public ItemsOfRoom(JFrame parent, DatabaseAccess db, int mapID, int roomLabel) {
		super(parent, true);
		this.setTitle("Items of room "+roomLabel);
		this.db = db;
		this.roomLabel = roomLabel;
		this.parent = parent;
		this.mapID = mapID;
		initComponents();
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
        itemListScroller.getVerticalScrollBar().setValue(0);
	}
	
	private void initComponents() {
		JPanel itemList = new JPanel();
		itemList.setBorder(BorderFactory.createEmptyBorder());
		itemList.setLayout(new BoxLayout(itemList, BoxLayout.Y_AXIS));
		Dimension dim = parent.getSize();
		try {
			ResultSet rs = db.getItemsOfMap(mapID, roomLabel);
			while(rs.next()) {
				JPanel item = new JPanel();
				item.setBorder(BorderFactory.createTitledBorder("Item "+(int)rs.getLong("id")));
				item.setLayout(new GridLayout(4,2));
				
				JLabel label_Label = new JLabel("Label:");
				label_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
				label_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
				JTextField label = new JTextField(15);
				label.setFont(new Font("SansSerif", Font.PLAIN, 14));
				label.setEditable(false);
				label.setText(rs.getString("id"));
												
				JLabel location_Label = new JLabel("Location:");
				location_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
				location_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
				JTextField location = new JTextField(15);
				location.setFont(new Font("SansSerif", Font.PLAIN, 14));
				location.setEditable(false);
				String[] locationCoords = rs.getString(1).replace(")", "").substring(6).split(" ");
				location.setText("X: "+locationCoords[0]+", Y: "+locationCoords[1]);
				
				JLabel attributes_Label = new JLabel("Attributes:");
				attributes_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
				attributes_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
				JTextArea attributes = new JTextArea(6,2);
				attributes.setFont(new Font("SansSerif", Font.PLAIN, 14));
				attributes.setEditable(false);
				attributes.setText(
					"Title: "+rs.getString("title")+"\n"+
					"Width: "+rs.getDouble("width")+"\n"+
					"Height: "+rs.getDouble("height")+"\n"+
					"Nationality: "+rs.getString("nationality")+"\n"+
					"Begin date: "+rs.getString("begin_date_string")+"\n"+
					"End date: "+rs.getString("end_date_string")+"\n"+
					"Date: "+rs.getString("date")+"\n"+
					"Item label: "+rs.getString("item_label")+"\n"+
					"Icon URL: "+rs.getString("url_image")
				);
				
				item.add(label_Label);
				item.add(label);
				item.add(location_Label);
				item.add(location);
				item.add(attributes_Label);
				item.add(attributes);
				itemList.add(item);
			}
			
			itemListScroller = new JScrollPane(itemList);
			itemListScroller.setPreferredSize(new Dimension(5*dim.width/10, 5*dim.height/10));
			
			JPanel roomListContainer = new JPanel();
			roomListContainer.setBorder(BorderFactory.createEmptyBorder());
			roomListContainer.add(itemListScroller);
			
			this.add(roomListContainer);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
}
