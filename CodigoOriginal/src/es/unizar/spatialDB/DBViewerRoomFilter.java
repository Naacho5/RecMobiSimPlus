package es.unizar.spatialDB;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class DBViewerRoomFilter extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	DBViewer viewer;
	ResultSet roomsRS;
	
	public DBViewerRoomFilter(DBViewer viewer, JPanel filterRooms, ResultSet roomsRS, Point location, JFrame parent) {
		super();
		this.viewer = viewer;
		this.roomsRS = roomsRS;
		
		DBViewerRoomFilter dialog = this;
						
		JScrollPane scroll = new JScrollPane(filterRooms);
		Dimension dim = parent.getSize();
		scroll.setPreferredSize(new Dimension(Math.max(200,(int)0.5*dim.width/10), Math.max(400,3*dim.height/10)));
		this.add(scroll);
		
//		JButton filterButton = new JButton("Filter");
//		filterButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				viewer.loadRoomList();
//				dialog.dispose();
//			}
//		});
//		
//		this.add(filterButton);
		
		pack();
        setLocation(location);
	}
	
}
