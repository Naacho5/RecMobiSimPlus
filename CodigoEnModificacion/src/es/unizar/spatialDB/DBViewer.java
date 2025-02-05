package es.unizar.spatialDB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import es.unizar.editor.MapEditor;
import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.editor.view.MapPanel;
import es.unizar.gui.MainSimulator;
import es.unizar.util.EditorLiterals;
import es.unizar.util.Literals;

public class DBViewer {
	
	public static JFrame dbViewer;
	
	public Dimension MIN_SIZE;
	
	private DatabaseAccess db;
	
	private final DBViewerModel model;
	
	private JPanel controlPanelContainer;
	private JPanel viewerContainer;
	
	public DBMapPanel mapPanel;
	public JScrollPane scroll;
	private JPanel mapPanelContainer; // To avoid auto-resizing when panel's size < editorContainer's size.
	private JPanel selector;
	private JPanel info;
	private JPanel statusBar;
	private JPanel roomList;
	
	private JScrollPane roomListScroller;
//	private JMenu filterRooms;
	private JPanel filterRooms;
	private ResultSet roomsRS;
	
	private ResultSet rs;
	
	private JList selectionList;
	
	public JLabel statusBarX;
    public JLabel statusBarY;
    public JLabel hoveredElement;
    public JLabel selectedElement;
	
	private JPanel mapSelector;
	
	JButton goToEditor, deleteMap, filterButton;
	
	private String selectedMap = "";
	private int selectedMapID = -1;
	
	private DBViewerController controller;
		
	public DBViewer(DatabaseAccess db) {
		this.db = db;
		model = new DBViewerModel();
		mapPanel = new DBMapPanel(model,this);
		controller = new DBViewerController(this, model);
		mapPanel.addMapPanelListener(controller);
		initialize();
		dbViewer.pack();
		dbViewer.setVisible(true);
	}

	private void initialize() {
		
		// Create JFrame
		dbViewer = new JFrame();
		
		// JFrame occupies fullscreen
		dbViewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// Set minimum size as window's dimensions / 4
		// Avoids minimum size
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		dbViewer.setMinimumSize(new Dimension(dim.width/4, dim.height/4));
		
		// JFrame's title and icon
		dbViewer.setTitle("RecMobiSim - Database Viewer");
		dbViewer.setIconImage(new ImageIcon(Literals.LOGO_PATH).getImage());
		
		initFrames();
		initStatusBar();
		initControlPanel();
		initMapView();
	}
	
	
	
	/**
	 * Inits all frames (main containers in window) in their positions.
	 * 
	 * Width: toolContainer = 2/10 ; editorContainer = 8/10
	 */
	private void initFrames() {
		
		dbViewer.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		/**
		 * Tools container
		 */
		controlPanelContainer = new JPanel();
		controlPanelContainer.setBorder(BorderFactory.createTitledBorder("Control panel"));
		controlPanelContainer.setPreferredSize(new Dimension(3*dim.width/10, dim.height));
        //levelEditor.getContentPane().add(toolContainer, BorderLayout.WEST);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.3; // Occupies 20% of the width
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		dbViewer.getContentPane().add(controlPanelContainer, gbc);

		/**
		 * Editor Container
		 */
        viewerContainer = new JPanel();
        viewerContainer.setBorder(BorderFactory.createTitledBorder("Map Viewer"));
        viewerContainer.setPreferredSize(new Dimension(7*dim.width/10, dim.height));
        //levelEditor.getContentPane().add(editorContainer, BorderLayout.CENTER);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 1; // To the right (2nd column)
		gbc.gridy = 0;
		gbc.weightx = 0.7; // Occupies 80% of the width
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		dbViewer.getContentPane().add(viewerContainer, gbc);

		/**
		 * Status Bar
		 */
        statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS)); // Start printing elements as a line (left to right).
        statusBar.setPreferredSize(new Dimension(dim.width, 20));
        statusBar.setMinimumSize(new Dimension(dim.width, 20));
        //levelEditor.getContentPane().add(statusBar, BorderLayout.SOUTH);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
		gbc.gridy = 1; // Below (2nd row)
		gbc.gridwidth = 2; // Two columns (tools and mapEditor)
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL; // Grow just horizontally (fixed below, just extend till screen's width)
		dbViewer.getContentPane().add(statusBar, gbc);
    }
	
	
	private void initStatusBar() {
        
		// Mouse's x position
		this.statusBarX = new JLabel();
        statusBar.add(this.statusBarX);
        this.statusBarX.setText("x:"); // + point.x);
        statusBar.add(Box.createRigidArea(new Dimension(10, 0)));

        // Mouse's y position
        this.statusBarY = new JLabel();
        statusBar.add(this.statusBarY);
        this.statusBarY.setText("y:"); // + point.y);
        statusBar.add(Box.createRigidArea(new Dimension(10, 0)));

        // Mouse's element hovered
        this.hoveredElement = new JLabel();
        this.hoveredElement.setVisible(true);
        this.hoveredElement.setName("hoveredElement");
        this.hoveredElement.setText("Hovered:");
        this.hoveredElement.setHorizontalTextPosition(JLabel.LEFT);
        this.hoveredElement.setIcon(null);
        this.hoveredElement.setIconTextGap(5);
        statusBar.add(this.hoveredElement);
        statusBar.add(Box.createRigidArea(new Dimension(10, 0)));

        // Current element selected
        this.selectedElement = new JLabel();
        this.selectedElement.setVisible(true);
        this.selectedElement.setName("selectedElement");
        this.selectedElement.setText("Selected:");
        this.selectedElement.setHorizontalTextPosition(JLabel.LEFT);
        this.selectedElement.setIcon(null);
        this.selectedElement.setIconTextGap(5);
        statusBar.add(this.selectedElement);
    }
	
	private void initControlPanel() {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		JButton close = new JButton("Close");
		close.setIcon(new ImageIcon((new ImageIcon(Literals.IMAGES_PATH+"close.png")).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH)));
		close.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				MainSimulator.frmSimulator.setContentPane((Container)MainSimulator.splitPane.getLeftComponent());
				MainSimulator.frmSimulator.getContentPane().setMinimumSize(MainSimulator.MIN_SIZE);
				MainSimulator.frmSimulator.setJMenuBar(MainSimulator.menuBar);
				MainSimulator.splitFrame.setVisible(false);
				MainSimulator.frmSimulator.setVisible(true);
				dbViewer.dispose();
				MainSimulator.splitFrame.dispose();
				MainSimulator.splitFrame = null;
			}
		});
		
		controlPanelContainer.add(close);
		
		// Create panel where all drawing tools (buttons) will be located.
		selector = new JPanel(); // numColumns has to be == numTools/Buttons
		selector.setBorder(BorderFactory.createTitledBorder("Map list"));
		
		selectionList = new JList<Object>(db.getMapNames().toArray());
		selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(selectionList);
		listScroller.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
		
		JTextField area = new JTextField(15);
		JTextField items = new JTextField(15);
						
		selectionList.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {				
				if (e.getValueIsAdjusting() == false) {
			        if (selectionList.getSelectedIndex() != -1) {
			        	model.clear();
			        	selectedMap = (String)selectionList.getSelectedValue();
			        	roomList.removeAll();			        	
			        	try {
			        		rs = db.getMap(selectedMap);
			        		if(rs.next()){
			        			int mapID = rs.getInt("id");
			    				selectedMapID = mapID;
			    				roomsRS = db.getRoomsOfMap(selectedMapID);
								loadFilterRoomList();
					        	loadMapFromDB();
			        		}
						} catch (SQLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
			        	
			        	mapPanel.setMAP_W(model.getMAP_W());
			    		mapPanel.setMAP_H(model.getMAP_H());
			    		mapPanel.setZOOM(1.0);
			    		mapPanel.setPixelRepresentsInMeters(model.getPixelRepresentsInMeters());
			        				        	
			        	refresh();
			        	
			        	double totalArea = db.getTotalMapArea(selectedMapID);
			        	//System.out.println(totalArea);
			        	DecimalFormat df = new DecimalFormat("#.###");
//		    			if(totalArea > -1.0) area.setText(df.format(totalArea * Math.pow((1000.0/6597.0),2))+" m^2");
		    			if(totalArea > -1.0) area.setText(df.format(totalArea * Math.pow(model.getPixelRepresentsInMeters(),2))+" m^2");
		    			
		    			int numItems = db.getNumItemsOfMap(selectedMapID);
			        	//System.out.println(totalArea);
		    			if(numItems > -1) items.setText(Integer.toString(numItems));
		    			
		    			goToEditor.setEnabled(true);
		    			deleteMap.setEnabled(true);
		    			filterButton.setEnabled(true);
		    			
		    			loadRoomList();
			        }else {
			        	area.setText("");
			        	items.setText("");
			        	roomList.removeAll();
						roomListScroller.repaint();
						roomListScroller.revalidate();
			        }
			    }				
			}
		});
		
		selector.add(listScroller);
		controlPanelContainer.add(selector);
		
		
		info = new JPanel(); // numColumns has to be == numTools/Buttons
		info.setBorder(BorderFactory.createTitledBorder("Map info"));
		info.setLayout(new GridLayout(2,2));
		
		JLabel area_Label = new JLabel("Total area:");
		area_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		area_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		area.setFont(new Font("SansSerif", Font.PLAIN, 14));
		area.setEditable(false);
		
		JLabel items_Label = new JLabel("Number of items:");
		items_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		items_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		items.setFont(new Font("SansSerif", Font.PLAIN, 14));
		items.setEditable(false);
		
		//area_Label.add(area);
		info.add(area_Label);
		info.add(area);
	    info.add(items_Label);
	    info.add(items);
	    controlPanelContainer.add(info);
	    
	    JPanel buttons = new JPanel();
	    buttons.setBorder(BorderFactory.createEmptyBorder());
	    buttons.setLayout(new GridLayout(3,1));
	    
	    JTextField zoom = new JTextField();
	    zoom.setName(EditorLiterals.MAP_ZOOM);
		zoom.setFont(new Font("SansSerif", Font.PLAIN, 14));
	    zoom.addActionListener(controller);
	    zoom.setText(Double.toString(1.0));
	    
	    JLabel zoom_Label = new JLabel("Zoom:");
		zoom_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		zoom_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		JPanel zoomPanel = new JPanel();
		zoomPanel.setBorder(BorderFactory.createEmptyBorder());
		zoomPanel.setLayout(new GridLayout(1,2));
		zoomPanel.add(zoom_Label);
		zoomPanel.add(zoom);
	    
	    buttons.add(zoomPanel);
	    
		goToEditor = new JButton("Edit map in editor");
		goToEditor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new MapEditor(db,selectedMap);
			}
		});
		goToEditor.setEnabled(false);
		buttons.add(goToEditor);
		
		deleteMap = new JButton("Delete map from database");
		deleteMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					db.deleteMap(selectedMapID);
					JOptionPane.showMessageDialog(dbViewer, "Map deleted from database", "INFO", JOptionPane.INFORMATION_MESSAGE);
					selectionList.setListData(db.getMapNames().toArray());
					selectionList.setSelectedValue(null, false);
					model.clear();
					mapPanel.repaint();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					JOptionPane.showMessageDialog(dbViewer, "Map not deleted", "ERROR", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		deleteMap.setEnabled(false);
		buttons.add(deleteMap);
		
		controlPanelContainer.add(buttons);
		
		DBViewer viewer = this;
		filterButton = new JButton("Filter rooms");
		filterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(filterRooms != null && filterRooms.getComponentCount() > 0) {
					DBViewerRoomFilter rf = new DBViewerRoomFilter(viewer,filterRooms,roomsRS,filterButton.getLocation(),dbViewer);
					rf.setVisible(true);
				}
			}
		});
		filterButton.setEnabled(false);
		controlPanelContainer.add(filterButton);
		
		roomList = new JPanel();
		roomList.setBorder(BorderFactory.createTitledBorder("Rooms of map"));
		roomList.setLayout(new BoxLayout(roomList, BoxLayout.Y_AXIS));
		roomListScroller = new JScrollPane(roomList);
		roomListScroller.setPreferredSize(new Dimension(3*dim.width/10, 4*dim.height/10));
		
		JPanel roomListContainer = new JPanel();
		roomListContainer.setBorder(BorderFactory.createEmptyBorder());
		roomListContainer.add(roomListScroller);
		controlPanelContainer.add(roomListContainer);
	}
	
	private void initMapView() {
		this.viewerContainer.setLayout(new BoxLayout(this.viewerContainer, BoxLayout.PAGE_AXIS));
		
//		this.mapPanel = new MapPanel(model);
//		this.mapPanel.addMapPanelListener(controller);
		mapPanel.setMAP_W(500);
		mapPanel.setMAP_H(500);
		mapPanel.setZOOM(1.0); // By default
		mapPanel.setPixelRepresentsInMeters(1);
		this.mapPanel.setVisible(true);
		
		// http://www.fredosaurus.com/notes-java/GUI/layouts/20borderlayout.html
		// To prevent component resizing, add a component to a JPanel with FlowLayout, and then add 
		// that panel to the BorderLayout. This is a common way to prevent resizing. The FlowLayout 
		// panel will stretch, but the component in it will not.
		mapPanelContainer = new JPanel(new FlowLayout());
		mapPanelContainer.add(mapPanel);
        
		//mapPanel.addListener(controller);

        scroll = new JScrollPane(mapPanelContainer);
		
		scroll.setAutoscrolls(true);
        
		viewerContainer.add(scroll, BorderLayout.CENTER);
	}
	
	private void loadMapFromDB() {
		try {
			if(selectedMapID > 0) {
				model.setMAP_W(rs.getInt("width"));
				model.setMAP_H(rs.getInt("height"));
				model.setPixelRepresentsInMeters(rs.getDouble("pixel_represents_in_meters"));
				model.setDRAWING_ICON_DIMENSION(rs.getInt("draw_icon_dimension"));
//				int mapID = rs.getInt("id");
//				selectedMapID = mapID;
				rs = db.getRoomsOfMap(selectedMapID);
				while(rs.next()) {
					String[] room = rs.getString(1).replace(")", "").substring(9).split(",");
					int[] xpoints = new int[room.length];
					int[] ypoints = new int[room.length];
					int i = 0;
					for(String point : room) {
						String[] coord = point.split(" ");
						xpoints[i] = (int) Double.parseDouble(coord[0]);
						ypoints[i] = (int) Double.parseDouble(coord[1]);
						i++;
					}
					Polygon roomPolygon = new Polygon(xpoints,ypoints,room.length);
					String[] centerCoord = rs.getString("center").replace(")", "").substring(6).split(" ");
					Point center = new Point(Double.parseDouble(centerCoord[0]),Double.parseDouble(centerCoord[1]));
					model.addRoom(new DBRoom(roomPolygon,Integer.toString(rs.getInt("label")),center));
				}
				rs = db.getConnectablesOfMap(selectedMapID);
				while(rs.next()) {
					int id = rs.getInt("id");
					String type = rs.getString("type");
					String[] location = rs.getString(1).replace(")", "").substring(6).split(" ");
					if(type.equals("DOOR")) {
						Door d = new Door(null,id,new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])));
						model.addDrawable(d);						
					}else {
						Stairs s = new Stairs(null,id,new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])));
						model.addDrawable(s);
					}										
				}
				rs = db.getConnectionsOfConnectables(selectedMapID);
				while(rs.next()) {
					Drawable d = model.getDrawable(rs.getInt("id_conn1"),rs.getString("type_conn1"));
					Drawable d2 = model.getDrawable(rs.getInt("id_conn2"),rs.getString("type_conn2"));
					Connectable c, c2;
					if(d instanceof Door) c = (Door)d;
					else c = (Stairs)d;
					if(d2 instanceof Door) c2 = (Door)d2;
					else c2 = (Stairs)d2;
					c.connectTo(c2);
					c2.connectTo(c);
				}
				rs = db.getItemsOfMap(selectedMapID);
				while(rs.next()) {
					String[] location = rs.getString(1).replace(")", "").substring(6).split(" ");
					Item i = new Item(null,rs.getInt("id"),new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])),rs.getString("url_image"));
					// Item attributes
					// Get item label (located in vertexLabel field) and assign it to item
					String attribute = rs.getString("item_label");
					if (attribute == null)
						System.out.println("Item doesn't have ITEM LABEL"); // Throw exception
					else
						i.setItemLabel(attribute);
					
					// Get item url and assign it to item
					attribute = rs.getString("url_image");
					if (attribute == null)
						System.out.println("Item doesn't have ICON URL");
					else
						i.setUrlIcon(attribute);
					
					i.setTitle(rs.getString("title"));
					i.setNationality(rs.getString("nationality"));
					i.setBeginDate(rs.getString("begin_date_string"));
					i.setEndDate(rs.getString("end_date_string"));
					i.setDate(rs.getString("date"));
					i.setHeight(rs.getDouble("height"));
					i.setWidth(rs.getDouble("width"));
					i.setItemLabel(rs.getString("item_label"));
					model.addItem(i);
				}
				rs = db.getSeparatorsOfMap(selectedMapID);
				while(rs.next()) {
					String[] room = rs.getString(1).replace(")", "").substring(11).split(",");
					double[] xpoints = new double[room.length];
					double[] ypoints = new double[room.length];
					int i = 0;
					for(String point : room) {
						String[] coord = point.split(" ");
						xpoints[i] = Double.parseDouble(coord[0]);
						ypoints[i] = Double.parseDouble(coord[1]);
						i++;
					}
					RoomSeparator sep = new RoomSeparator(null, i, new Corner(null,0,new Point(xpoints[0],ypoints[0])), new Corner(null,0,new Point(xpoints[1],ypoints[1])));
					model.addSeparator(sep);										
				}							
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void refresh() {
		dbViewer.repaint();
        mapPanel.repaint();
	}
	
	private void loadFilterRoomList() {
//		JMenuBar filterRoomsBar = new JMenuBar();
//		filterRooms = new JMenu("Filtrar");
//		filterRooms.setAutoscrolls(true);
//		
//		filterRoomsBar.add(filterRooms);
		
		filterRooms = new JPanel();
		filterRooms.setLayout(new BoxLayout(filterRooms, BoxLayout.Y_AXIS));
		filterRooms.setBorder(BorderFactory.createTitledBorder("Rooms filter"));
		JCheckBox checkBox = new JCheckBox("Select all");
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(((JCheckBox)arg0.getSource()).isSelected()) {
					for(Component c : filterRooms.getComponents()) {
						JCheckBox chbx = (JCheckBox)c;
						chbx.setSelected(true);
					}
				}else {
					for(Component c : filterRooms.getComponents()) {
						JCheckBox chbx = (JCheckBox)c;
						chbx.setSelected(false);
					}
				}
				roomList.removeAll();
				loadRoomList();
			}
		});
		checkBox.setSelected(true);
		filterRooms.add(checkBox);
		try {
			while(roomsRS.next()) {
				checkBox = new JCheckBox("Room "+roomsRS.getInt("label"));
				checkBox.setBounds(100,100, 50,50);
				filterRooms.add(checkBox);
				checkBox.setSelected(true);
				checkBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						roomList.removeAll();
						loadRoomList();
					}
				});
			}
			roomsRS.beforeFirst();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadRoomList() {
		
		try {
//			roomList.removeAll();
			roomListScroller.getVerticalScrollBar().setValue(0);
			roomsRS.beforeFirst();
			int i = 1;
			while(roomsRS.next()) {				
				if(((JCheckBox)(filterRooms.getComponent(i))).isSelected()) {									
					JPanel room = new JPanel(); // numColumns has to be == numTools/Buttons
					int roomLabel = roomsRS.getInt("label");
					room.setBorder(BorderFactory.createTitledBorder("Room "+roomLabel));
					room.setLayout(new GridLayout(3,2));
					
					JLabel area_Label = new JLabel("Area of room:");
					area_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
					area_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
					JTextField area = new JTextField(15);
					area.setFont(new Font("SansSerif", Font.PLAIN, 14));
					area.setEditable(false);
					
					JLabel items_Label = new JLabel("Number of items:");
					items_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
					items_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
					JTextField items = new JTextField(15);
					items.setFont(new Font("SansSerif", Font.PLAIN, 14));
					items.setEditable(false);
					
					DecimalFormat df = new DecimalFormat("#.###");
//					area.setText(df.format(db.getAreaOfRoom(selectedMapID, roomsRS.getInt("label")) * Math.pow((1000.0/6597.0),2))+" m^2");
					area.setText(df.format(db.getAreaOfRoom(selectedMapID, roomsRS.getInt("label")) * Math.pow(model.getPixelRepresentsInMeters(),2))+" m^2");
					items.setText(Integer.toString(db.getNumberItemsInRoom(selectedMapID, roomsRS.getInt("label"))));
					
					JButton showItems = new JButton("Show items");
					showItems.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							ItemsOfRoom d = new ItemsOfRoom(dbViewer,db,selectedMapID,roomLabel);
							d.setVisible(true);
						}
					});
					
					JButton showInMap = new JButton("Show in map");
					showInMap.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							if(model.getRoomSelectedLabel() != roomLabel) {
								model.setRoomSelectedLabel(roomLabel);
								DBRoom r = model.findRoom(roomLabel);
								if(r != null) {
									scroll.getVerticalScrollBar().setValue((int) (((r.getLabelLocation().getY() - model.getDRAWING_ICON_DIMENSION()/2) * model.getZOOM())-(r.getPol().getBounds().getHeight()/2)));
									scroll.getHorizontalScrollBar().setValue((int) ((r.getLabelLocation().getX() * model.getZOOM())-(r.getPol().getBounds().getWidth()/2)));
								}
							}else {
								model.setRoomSelectedLabel(0);
							}
							mapPanel.repaint();
						}
					});
					
					room.add(area_Label);
					room.add(area);
					room.add(items_Label);
					room.add(items);
					room.add(showItems);
					room.add(showInMap);
					
					roomList.add(room);
					//roomListScroller.repaint();
					//roomListScroller.revalidate();
					//System.out.println("Room "+roomsRS.getInt("label"));
				}
				i++;
			}
			roomListScroller.repaint();
			roomListScroller.revalidate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void showRoomInList(Drawable roomNumber) {
		try {
			int num = Integer.parseInt(roomNumber.getUrlIcon());
			boolean finished = false;
			for(Component c : roomList.getComponents()) {
				if(c instanceof JPanel) {
					JPanel p = (JPanel)c;
					if(p.getBorder() instanceof TitledBorder) {
						TitledBorder tb = (TitledBorder)p.getBorder();
					    if(Integer.parseInt(tb.getTitle().split(" ")[1]) == num){
					    	roomListScroller.getVerticalScrollBar().setValue(p.getY());
					    	p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED,3),tb.getTitle()));
					    	p.repaint();
					    	finished = true; 
					    	break;
					    }
					}
					if(finished) {break;}
				}
			}
		}catch(NumberFormatException e) {
			e.printStackTrace();
		}		
	}

	
	public void deselectRoomInList(Drawable roomNumber) {
		try {
			int num = Integer.parseInt(roomNumber.getUrlIcon());
			boolean finished = false;
			for(Component c : roomList.getComponents()) {
				if(c instanceof JPanel) {
					JPanel p = (JPanel)c;
					if(p.getBorder() instanceof TitledBorder) {
						TitledBorder tb = (TitledBorder)p.getBorder();
					    if(Integer.parseInt(tb.getTitle().split(" ")[1]) == num){
					    	p.setBorder(BorderFactory.createTitledBorder(tb.getTitle()));
					    	p.repaint();
					    	finished = true; 
					    	break;
					    }
					}
					if(finished) {break;}
				}
			}
		}catch(NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
