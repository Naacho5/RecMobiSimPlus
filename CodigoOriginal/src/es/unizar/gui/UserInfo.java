package es.unizar.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import es.unizar.gui.simulation.User;
import es.unizar.util.Pair;

public class UserInfo extends JDialog {

	private static final long serialVersionUID = 1L;
	
	public static class UserState {
		public int room;
		public String action;
		public Long item;
		public UserState(int room) {
			this.room = room;
		}
	}
	
	private Map<Integer,UserInfo.UserState> stateOfUsers;
	private Map<Pair<Integer,Integer>,Double> timeUsersInRooms;
	
	private JTable locationOfUsers,timeOfUsersInRooms,usersInRooms;
	private DefaultTableModel tableModel1,tableModel2,tableModel3;
	private JScrollPane tableScroll1,tableScroll2;
	
	private Timer timer;
	private boolean wasDoubleClick = false;
	
	public int roomFilter = -1;
	public JButton removeFilter;
	
	public UserInfo(JFrame parent, Map<Integer,UserState> stateOfUsers, Map<Pair<Integer, Integer>, Double> timeUsersInRooms) {
		super(parent, true);
		this.stateOfUsers = stateOfUsers;
		this.timeUsersInRooms = timeUsersInRooms;
		initialize();
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));    
	}
	
	private void selectRoomInMap(MouseEvent e, int roomColumn) {
		int buttonPressed = e.getButton();
		if (e.getClickCount() >= 2) {
			wasDoubleClick = true;
			timer.stop();
			
			if (e.getClickCount() == 2) { // If button clicked more than twice, ignore.
				if (buttonPressed == MouseEvent.BUTTON1) {
					System.out.println("double click");
					int row = locationOfUsers.rowAtPoint(e.getPoint());
					int room = (int)locationOfUsers.getValueAt(row, roomColumn);
					mxGraph g = MainSimulator.graphComponent.getGraph();
					Polygon p = new Polygon();
					for(Object o : g.getChildCells(g.getDefaultParent())) {
						mxCell c = (mxCell)o;
						if(c.isVertex()) {
							String[] label = c.getValue().toString().split(", ");
							try {
								if(label[0].equals("corner") && Integer.parseInt(label[1]) == room) {										
									for(int i = 0; i < c.getEdgeCount(); i++) {
										c.getEdgeAt(i).setStyle(c.getEdgeAt(i).getStyle().replace(";"+mxConstants.STYLE_STROKECOLOR+"=black","")+";"+mxConstants.STYLE_STROKECOLOR+"=red");
									}
									p.addPoint((int)c.getGeometry().getX(), (int)c.getGeometry().getY());
								}else if(label[0].equals("corner") && Integer.parseInt(label[1]) != room) {
									for(int i = 0; i < c.getEdgeCount(); i++) {
										c.getEdgeAt(i).setStyle(c.getEdgeAt(i).getStyle().replace(";"+mxConstants.STYLE_STROKECOLOR+"=red","")+";"+mxConstants.STYLE_STROKECOLOR+"=black");
									}
								}else if(Integer.parseInt(c.getValue().toString()) == room) {
									MainSimulator.graphComponent.getVerticalScrollBar().setValue((int) ((c.getGeometry().getCenterY())-(p.getBounds().getHeight()/2)));
									MainSimulator.graphComponent.getHorizontalScrollBar().setValue((int) ((c.getGeometry().getCenterX())-(p.getBounds().getWidth()/2)));
								}
							}catch(NumberFormatException nfe) {
								nfe.printStackTrace();
							}
						}
					}
					MainSimulator.repaintFloorPanelCombined();
				}
			}
		}
		else {
			// Use one of the two delays created below
			Integer multiClickInterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval"); // Value = 500ms
			int timerDelay = 200; // Value = 200ms -> To wait less
			
			// Change first arg to multiClickInterval.intValue() if wanted -> more delay
            timer = new Timer(timerDelay, new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    if (wasDoubleClick)
                        wasDoubleClick = false; // reset flag
                }
                
            });
            timer.setRepeats(false);

            timer.start();
		}
	}
	
	private void initialize() {
		
		setLayout(new GridBagLayout());
		
		JLabel locationOfUsersLabel = new JLabel("State of users");
		tableModel1 = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		locationOfUsers = new JTable(tableModel1);
		tableScroll1 = new JScrollPane(locationOfUsers);
		locationOfUsers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectRoomInMap(e,1);
			}			
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.2;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
		
		removeFilter = new JButton("Remove room filter");
		removeFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				roomFilter = -1;
				removeFilter.setEnabled(false);
			}		
		});
		removeFilter.setEnabled(false);
		add(removeFilter,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;		
		
		add(locationOfUsersLabel,gbc);
		
		tableModel1.addColumn("User");
		tableModel1.addColumn("Room");
		tableModel1.addColumn("Action");
		tableModel1.addColumn("Last item observed");
		for (Map.Entry<Integer,UserState> entry : stateOfUsers.entrySet()) {
//			System.out.println(entry.getKey()+" "+entry.getValue());
			UserState ui = entry.getValue();
	        tableModel1.addRow(new Object[]{entry.getKey(),ui.room,ui.action,ui.item});
	    }
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 0.9;
		gbc.fill = GridBagConstraints.BOTH;
		
		add(tableScroll1,gbc);
		
		JLabel timeOfUsersLabel = new JLabel("Total time spent by users in rooms (seconds)");
		tableModel2 = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		timeOfUsersInRooms = new JTable(tableModel2);
		timeOfUsersInRooms.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectRoomInMap(e,1);
			}			
		});
		tableScroll2 = new JScrollPane(timeOfUsersInRooms);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
				
		add(timeOfUsersLabel,gbc);
		
		tableModel2.addColumn("User");
		tableModel2.addColumn("Room");
		tableModel2.addColumn("Time of user in room (s)");
		timeUsersInRooms.entrySet().stream().sorted((e1,e2) -> e1.getKey().getF().compareTo(e2.getKey().getF())).forEach(entry -> {
//		for (Map.Entry<Pair<Integer,Integer>,Double> entry : timeUsersInRooms.entrySet()) {
	        tableModel2.addRow(new Object[]{entry.getKey().getF(),entry.getKey().getS(),entry.getValue()});
	    });
		
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 0.9;
		gbc.fill = GridBagConstraints.BOTH;
		
		add(tableScroll2,gbc);
		
		tableModel3 = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		tableModel3.addColumn("Room");
		tableModel3.addColumn("Users in room");
		stateOfUsers.values().stream().distinct().sorted(Comparator.comparingInt(r -> r.room)).forEach(r -> {			
			int numUsersInRoom = (int)stateOfUsers.entrySet().stream().filter(e -> e.getValue().room == r.room).count();						
			tableModel3.addRow(new Object[]{r.room,numUsersInRoom});
		});		
		JLabel usersInRoomsLabel = new JLabel("User count in rooms");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		add(usersInRoomsLabel,gbc);
		
		usersInRooms = new JTable(tableModel3);
		usersInRooms.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectRoomInMap(e,0);
			}			
		});
		JScrollPane tableScroll3 = new JScrollPane(usersInRooms);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weightx = 1.0;
		gbc.weighty = 0.9;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;		
		add(tableScroll3,gbc);
	}
	
	public void reloadTables() {
//		System.out.println("update");
		tableModel1.setRowCount(0);
		for (Map.Entry<Integer,UserState> entry : stateOfUsers.entrySet()) {
			UserState ui = entry.getValue();
			if(roomFilter == -1 || ui.room == roomFilter) tableModel1.addRow(new Object[]{entry.getKey(),ui.room,ui.action,ui.item});
	    }
		locationOfUsers.repaint();
		locationOfUsers.revalidate();
		
		tableModel2.setRowCount(0);
		timeUsersInRooms.entrySet().stream().sorted((e1,e2) -> e1.getKey().getF().compareTo(e2.getKey().getF())).forEach(entry -> {
//		for (Map.Entry<Pair<Integer,Integer>,Double> entry : timeUsersInRooms.entrySet()) {
			if(roomFilter == -1 || entry.getKey().getS() == roomFilter) tableModel2.addRow(new Object[]{entry.getKey().getF(),entry.getKey().getS(),entry.getValue()});
	    });
		timeOfUsersInRooms.repaint();
		timeOfUsersInRooms.revalidate();
		
		tableModel3.setRowCount(0);
		List<Integer> roomsWithUsers = new ArrayList<Integer>();
		stateOfUsers.values().stream().distinct().sorted(Comparator.comparingInt(v -> v.room)).forEach(v -> {			
			int numUsersInRoom = (int)stateOfUsers.entrySet().stream().filter(e -> e.getValue().room == v.room).count();						
			if((roomFilter == -1 || v.room == roomFilter) && !roomsWithUsers.contains(v.room)) {
				tableModel3.addRow(new Object[]{v.room,numUsersInRoom});
				roomsWithUsers.add(v.room);
			}
		});
		usersInRooms.repaint();
		usersInRooms.revalidate();
	}
	
	public void filterTables(int room) {
		roomFilter = room;
	}
	
}
