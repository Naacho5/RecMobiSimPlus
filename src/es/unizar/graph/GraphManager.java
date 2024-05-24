package es.unizar.graph;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.geom.Point2;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.camera.Camera;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.value.Uncoercible;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.Record;

import es.unizar.access.DataAccessGraphFile;
import es.unizar.access.DataAccessItemFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.editor.view.MapEditorView;
import es.unizar.editor.view.SVGParser;
import es.unizar.gui.FloorPanelCombined;
import es.unizar.gui.MainSimulator;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

public class GraphManager {
	
	public static JFrame graphManager;
	private final MapEditorModel model;
	
	public DataAccessGraphFile accessGraphFile;
	public DataAccessItemFile accessItemFile;
	public DataAccessRoomFile accessRoomFile;
	
	private JPanel selector, infoPanel;
	private JList selectionList;
	private JButton saveGraphAsMapFile, saveGraphAsMapSVGFile, deleteGraph;
	private JPanel buttons, loadButtons;
	
//	private Session session;
//	Driver d = null;
	
	private JPanel controlPanelContainer;
	private JPanel viewerContainer;
	private JPanel resultsPanel;
	private JScrollPane scrollGraph;
	private JTextArea infoText;
	
	private int expandedLevel = 0;
	
	private JButton openMap;
	
	int numRooms = 0;
	int numCorners = 0;
	int numConnectables = 0;
	int numItems = 0;
	int numRoomSeparators = 0;
	
	public class DriverHolder {
		private Driver driver = null;
		private String url, user;
		public Driver getDriver() {
			return driver;
		}
		public void setDriver(Driver driver) {
			this.driver = driver;
		}
		public void setInfo(String url, String user) {
			this.url = url;
			this.user = user;
		}
		public String getUrl() {
			return url;
		}
		public String getUser() {
			return user;
		}
	}
	
	private DriverHolder d;
	
	private Graph graph;
	private View view;
		
	public GraphManager() {
		d = new DriverHolder();
		model = new MapEditorModel();
		initialize();
		graphManager.pack();
		graphManager.setVisible(true);
	}

	private void initialize() {
		graphManager = new JFrame();
		initFrames();
	}
		
	
	private void initFrames() {
		
		graphManager.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		/**
		 * Tools container
		 */
		controlPanelContainer = new JPanel();
		controlPanelContainer.setBorder(BorderFactory.createTitledBorder("Graph manager"));
		controlPanelContainer.setPreferredSize(new Dimension(dim.width/2, 9*dim.height/10));
//		controlPanelContainer.setLayout(new BoxLayout(controlPanelContainer, BoxLayout.Y_AXIS));
        //levelEditor.getContentPane().add(toolContainer, BorderLayout.WEST);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		graphManager.getContentPane().add(controlPanelContainer, gbc);

//		/**
//		 * Editor Container
//		 */
//        viewerContainer = new JPanel();
//        viewerContainer.setBorder(BorderFactory.createTitledBorder("Map Viewer"));
//        viewerContainer.setPreferredSize(new Dimension(7*dim.width/10, dim.height));
//        //levelEditor.getContentPane().add(editorContainer, BorderLayout.CENTER);
//        
//        gbc = new GridBagConstraints();
//        gbc.gridx = 1;
//		gbc.gridy = 0;
//		gbc.weightx = 0.7;
//		gbc.weighty = 1.0;
//		gbc.fill = GridBagConstraints.BOTH;
//		graphManager.getContentPane().add(viewerContainer, gbc);
		
		controlPanelContainer.setLayout(new GridLayout(1,2));
				
		selector = new JPanel();
		selector.setLayout(new BorderLayout());
		selector.setBorder(BorderFactory.createTitledBorder("Graph selector"));
		JScrollPane listScroller = new JScrollPane();
//		listScroller.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
		selector.add(listScroller,BorderLayout.CENTER);
		
		saveGraphAsMapFile = new JButton("Save graph as map text files");
		saveGraphAsMapFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
								
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setCurrentDirectory(new File(Literals.PATH_MAPS));
				fileChooser.setDialogTitle("Select where to save files");
				fileChooser.setApproveButtonText("Save map");
				
				switch(fileChooser.showOpenDialog(graphManager)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File directory = fileChooser.getSelectedFile();
						
						if(!filesExistInDirectory(directory)) {
							model.setFiles(directory.getAbsolutePath());
							saveGraphInModel((String)selectionList.getSelectedValue());
							if(model.save(true))
								JOptionPane.showMessageDialog(graphManager, "Item, room and graph files saved in " + directory, "INFO", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(graphManager, "Constraints violated. Please, check:\n - All rooms have at least one door.\n - All doors are connected to at least one element.\n - All items are inside a room.", "ERROR", JOptionPane.ERROR_MESSAGE);
						}
						else {
							JOptionPane.showMessageDialog(graphManager, "File(s) already exist in directory", "WARNING", JOptionPane.WARNING_MESSAGE);
						}
						
						break;
//						if(f.isDirectory()) {
//							File file1 = new File(f.getAbsolutePath() + File.separator + Literals.GRAPH_FILE_NAME);
//							File file2 = new File(f.getAbsolutePath() + File.separator + Literals.ROOM_FILE_NAME);
//							File file3 = new File(f.getAbsolutePath() + File.separator + Literals.ITEM_FILE_NAME);
//							model.setFiles(file1,file2,file3);
//						}else if(f.isFile() && f.getName().endsWith(".svg")){
//							File file1 = new File(f.getAbsolutePath());
//							File file2 = new File(f.getAbsolutePath());
//							File file3 = new File(f.getAbsolutePath());
//							model.setFiles(file1,file2,file3);
//						}
					default:
						break;
				}
			}
		});
		saveGraphAsMapSVGFile = new JButton("Save graph as map SVG file");
		saveGraphAsMapSVGFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
								
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setCurrentDirectory(new File(Literals.PATH_MAPS));
				fileChooser.setDialogTitle("Select where to save files");
				fileChooser.setApproveButtonText("Save map");
				
				switch(fileChooser.showOpenDialog(graphManager)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File directory = fileChooser.getSelectedFile();
						
						if(!filesExistInDirectory(directory)) {
							model.setFiles(directory.getAbsolutePath());
							saveGraphInModel((String)selectionList.getSelectedValue());
							if(model.saveSVG(directory.getAbsolutePath()))
								JOptionPane.showMessageDialog(graphManager, "Item, room and graph files saved in " + directory, "INFO", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(graphManager, "Constraints violated. Please, check:\n - All rooms have at least one door.\n - All doors are connected to at least one element.\n - All items are inside a room.", "ERROR", JOptionPane.ERROR_MESSAGE);
						}
						else {
							JOptionPane.showMessageDialog(graphManager, "File(s) already exist in directory", "WARNING", JOptionPane.WARNING_MESSAGE);
						}
						
						break;
//						if(f.isDirectory()) {
//							File file1 = new File(f.getAbsolutePath() + File.separator + Literals.GRAPH_FILE_NAME);
//							File file2 = new File(f.getAbsolutePath() + File.separator + Literals.ROOM_FILE_NAME);
//							File file3 = new File(f.getAbsolutePath() + File.separator + Literals.ITEM_FILE_NAME);
//							model.setFiles(file1,file2,file3);
//						}else if(f.isFile() && f.getName().endsWith(".svg")){
//							File file1 = new File(f.getAbsolutePath());
//							File file2 = new File(f.getAbsolutePath());
//							File file3 = new File(f.getAbsolutePath());
//							model.setFiles(file1,file2,file3);
//						}
					default:
						break;
				}
			}
		});
		
		deleteGraph = new JButton("Delete graph");
		deleteGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try(var s = d.getDriver().session()){
					deleteNodes(s,(String)selectionList.getSelectedValue());
					loadMapSelector(s);
					scrollGraph.setViewportView(null);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		saveGraphAsMapFile.setEnabled(false);
		saveGraphAsMapSVGFile.setEnabled(false);
		deleteGraph.setEnabled(false);
		buttons = new JPanel();
	    buttons.setBorder(BorderFactory.createEmptyBorder());
	    buttons.setLayout(new GridLayout(3,1));		
	    buttons.add(saveGraphAsMapFile);
	    buttons.add(saveGraphAsMapSVGFile);
	    buttons.add(deleteGraph);
	    selector.add(buttons,BorderLayout.PAGE_END);
	    
	    JButton connectDB = new JButton("Connect to DB");
		connectDB.setFont(new Font("SansSerif", Font.PLAIN, 16));
		GraphManager grMng = this;
		connectDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ConnectToGraphDB connect = new ConnectToGraphDB(graphManager,d,grMng);
				connect.setVisible(true);
			}
		});
		
		openMap = new JButton("Create graph with file");
		openMap.setFont(new Font("SansSerif", Font.PLAIN, 16));
		openMap.addActionListener(new ActionListener() {
			// Open Map event
			public void actionPerformed(ActionEvent arg0) {
				
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.svg", "svg"));
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setCurrentDirectory(new File(Literals.PATH_MAPS));
				fileChooser.setDialogTitle("Open map (select SVG file or directory with text files)");
				fileChooser.setApproveButtonText("Create graph of map in Neo4j");
				
				switch(fileChooser.showOpenDialog(graphManager)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = fileChooser.getSelectedFile();
						if(f.isDirectory()) {
							File file1 = new File(f.getAbsolutePath() + File.separator + Literals.GRAPH_FILE_NAME);
							File file2 = new File(f.getAbsolutePath() + File.separator + Literals.ROOM_FILE_NAME);
							File file3 = new File(f.getAbsolutePath() + File.separator + Literals.ITEM_FILE_NAME);
							model.setFiles(file1,file2,file3);
						}else if(f.isFile() && f.getName().endsWith(".svg")){
							File file1 = new File(f.getAbsolutePath());
							File file2 = new File(f.getAbsolutePath());
							File file3 = new File(f.getAbsolutePath());
							model.setFiles(file1,file2,file3);
						}
					
						model.clearAll();
						Loading loading = new Loading(graphManager);
						loading.setVisible(true);
						System.out.println("load model");
						model.load(0, 0);
						try(var s = d.getDriver().session()){
							System.out.println("create nodes");
							createNodes(s);
							loadMapSelector(s);
						}catch(Exception e) {
							e.printStackTrace();
						}
						loading.dispose();
					default:
						break;
						
				}
			}
		});
		openMap.setEnabled(false);
		
		loadButtons = new JPanel(new GridLayout(1,2));
		loadButtons.add(connectDB);
		loadButtons.add(openMap);
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
				graphManager.dispose();
				MainSimulator.splitFrame.dispose();
				MainSimulator.splitFrame = null;
			}
		});
		loadButtons.add(close);
		selector.add(loadButtons,BorderLayout.PAGE_START);
	    
	    JPanel sel_info = new JPanel(new GridLayout(2,1));
	    	  
		sel_info.add(selector);
		
		infoPanel = new JPanel();
		infoPanel.setBorder(BorderFactory.createTitledBorder("Graph info"));
		infoPanel.setLayout(new BorderLayout());
		infoText = new JTextArea();
		infoText.setEditable(false);
		infoPanel.add(infoText,BorderLayout.CENTER);
//		infoPanel.setMaximumSize(new Dimension(3*dim.width/10, 3*dim.height/10));
		JScrollPane scrollInfo = new JScrollPane(infoPanel);
		JPanel scrollInfoPanel = new JPanel(new BorderLayout());
		scrollInfoPanel.add(scrollInfo,BorderLayout.CENTER);
		
		sel_info.add(scrollInfoPanel);
		
		controlPanelContainer.add(sel_info);
		
		JPanel queryContainer = new JPanel(new GridBagLayout());
		queryContainer.setMinimumSize(new Dimension(dim.width*5/10,9*dim.height/10));
//		queryContainer.setBorder(BorderFactory.createTitledBorder("Query area"));
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		graphManager.getContentPane().add(queryContainer,gbc);
		
		JTextArea queryTextArea = new JTextArea(50,50);
		JScrollPane scroll = new JScrollPane(queryTextArea);
		scroll.setBorder(BorderFactory.createTitledBorder("Query area"));
		
		JPanel queryAndButtonsPanel = new JPanel();
		queryAndButtonsPanel.setLayout(new BoxLayout(queryAndButtonsPanel, BoxLayout.X_AXIS));
		queryAndButtonsPanel.add(scroll);
		
//		gbc.gridx = 0;
//		gbc.gridy = 0;
//		gbc.weightx = 1.0;
//		gbc.weighty = 0.4;
//		gbc.fill = GridBagConstraints.BOTH;
//		queryContainer.add(scroll,gbc);
		
		resultsPanel = new JPanel();
		resultsPanel.setLayout(new GridBagLayout());
				
		DefaultTableModel tableModel = new DefaultTableModel();
		JTable resultTable = new JTable(tableModel);
		JScrollPane resultScroll = new JScrollPane(resultTable);
		resultScroll.setBorder(BorderFactory.createTitledBorder("Query result"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.6;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		resultsPanel.add(resultScroll,gbc);
				
		gbc = new GridBagConstraints();
        gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.6;
		gbc.fill = GridBagConstraints.BOTH;
//		queryContainer.add(resultScroll,gbc);
		
		JButton runQuery = new JButton("Run query");
		runQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(d.getDriver() != null) {
					try {
						try(var session = d.getDriver().session()){
							int mapID = session.executeWrite(tx -> {
					            Query query = new Query(queryTextArea.getText());
					            Result result = tx.run(query);
					            tableModel.setRowCount(0);
								tableModel.setColumnCount(0);
								for(String k : result.keys()) {
									tableModel.addColumn(k);
				            	}
					            for(Record r : result.list()) {
					            	String[] resultsRow = new String[r.fields().size()];
					            	for(int i = 0; i < r.fields().size(); i++) {
					            		switch(r.get(i).type().name()) {
					            			case "NODE":
					            				Node n = r.get(i).asNode();
						            			resultsRow[i] = "(:"+n.labels().iterator().next()+" {";
						            			Iterator<String> it1 = n.keys().iterator();
						            			Iterator<Value> it2 = n.values().iterator();
						            			while(it1.hasNext() && it2.hasNext()) {				            				
						            				resultsRow[i] = resultsRow[i] + it1.next()+": "+it2.next()+", ";
						            			}
						            			resultsRow[i] = resultsRow[i].substring(0,resultsRow[i].length()-2);
						            			resultsRow[i] = resultsRow[i]+"})";
						            			break;
					            			case "LIST":
					            				List<Object> l = r.get(i).asList();
					            				resultsRow[i] = l.toString();
					            				break;
					            			case "STRING":
					            				String s = r.get(i).asObject().toString();
						            			resultsRow[i] = s;
						            			break;					            				
					            		}
//					            		try {
//					            			Node n = r.get(i).asNode();
//					            			resultsRow[i] = "(:"+n.labels().iterator().next()+" {";
//					            			Iterator<String> it1 = n.keys().iterator();
//					            			Iterator<Value> it2 = n.values().iterator();
//					            			while(it1.hasNext() && it2.hasNext()) {				            				
//					            				resultsRow[i] = resultsRow[i] + it1.next()+": "+it2.next()+", ";
//					            			}
//					            			resultsRow[i] = resultsRow[i].substring(0,resultsRow[i].length()-2);
//					            			resultsRow[i] = resultsRow[i]+"})";
//					            		}catch(Uncoercible e1) {
//					            			try {
//					            				List<Object> l = r.get(i).asList();
//					            				resultsRow[i] = l.toString();
//					            			}catch(Uncoercible e2) {
//					            				String s = r.get(i).asObject().toString();
//						            			resultsRow[i] = s;
//					            			}				            							            							        
//					            		}				      
					            	}
					            	tableModel.addRow(resultsRow);
					            }
					            resultTable.repaint();
								resultTable.revalidate();
								return 0;
					        });
						}
					}catch(Exception e) {
						JOptionPane.showMessageDialog(graphManager, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
					}
				}else {
					JOptionPane.showMessageDialog(graphManager, "Not connected to Neo4j database", "WARNING", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		
		JButton clearQuery = new JButton("Clear query");
		clearQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				queryTextArea.setText("");
				tableModel.setRowCount(0);
				tableModel.setColumnCount(0);
				resultTable.repaint();
				resultTable.revalidate();
			}
		});
		
		JPanel buttons2 = new JPanel();
	    buttons2.setBorder(BorderFactory.createEmptyBorder());
	    buttons2.setLayout(new GridLayout(2, 1));
	    buttons2.add(runQuery);
	    buttons2.add(clearQuery);
	    
	    queryAndButtonsPanel.add(buttons2);
	    
	    gbc = new GridBagConstraints();
        gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.45;
		gbc.fill = GridBagConstraints.BOTH;
		queryContainer.add(queryAndButtonsPanel,gbc);
		
//		gbc = new GridBagConstraints();
//        gbc.gridx = 0;
//		gbc.gridy = 1;
//		gbc.weightx = 1.0;
//		gbc.weighty = 0.1;
//		gbc.fill = GridBagConstraints.BOTH;
//		queryContainer.add(buttons2,gbc);
				
		System.setProperty("org.graphstream.ui", "swing");
			
		graph = new SingleGraph("Graph");
		SwingViewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.enableAutoLayout();
		view = viewer.addDefaultView(false);
//		resultsPanel.add((Component) view,gbc);
		scrollGraph = new JScrollPane((Component) view);
		JPanel graphPanel = new JPanel(new BorderLayout());
		graphPanel.setBorder(BorderFactory.createTitledBorder("Graph view"));
		graphPanel.add(scrollGraph,BorderLayout.CENTER);
		controlPanelContainer.add(graphPanel);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.55;
		gbc.fill = GridBagConstraints.BOTH;
		queryContainer.add(resultsPanel,gbc);
		
		
    }
	
	
	
	/**
	 * Checks if rooms, items and graph files already exist in directory.
	 * 
	 * @param f
	 * @return
	 */
	private boolean filesExistInDirectory(File f) {
		String name = f.getAbsolutePath();
		
		// Check Graph file
		if (new File(name + File.separator + Literals.GRAPH_FLOOR_COMBINED).exists())
			return true;
		// Check Room file
		if (new File(name + File.separator + Literals.ROOM_FLOOR_COMBINED).exists())
			return true;
		// Check Item file
		if (new File(name + File.separator + Literals.ITEM_FLOOR_COMBINED).exists())
			return true;
		
		return false;
	}
	
	
	private void deleteNodes(Session session, String mapName) {
		Object delete = session.executeWrite(tx -> {
            Query query = new Query("MATCH (i:Item)-[:ITEM_LOCATED_IN]->(:Room)-[:IS_PART_OF]->(m:Map {name: $mapName}) DETACH DELETE i",Values.parameters("mapName",mapName));
            Result result = tx.run(query);
            
            query = new Query("MATCH (c:Corner)-[:CORNER_OF]->(r:Room)-[:IS_PART_OF]->(m:Map {name: $mapName}) DETACH DELETE c",Values.parameters("mapName",mapName));
            result = tx.run(query);
            
            query = new Query("MATCH (rs:RoomSeparator)-[:SEPARATOR_OF]->(r:Room)-[:IS_PART_OF]->(m:Map {name: $mapName}) DETACH DELETE rs",Values.parameters("mapName",mapName));
            result = tx.run(query);
            
            query = new Query("MATCH (conn:Connectable)-[:CONNECTABLE_LOCATED_IN]->(r:Room)-[:IS_PART_OF]->(m:Map {name: $mapName}) DETACH DELETE conn",Values.parameters("mapName",mapName));
            result = tx.run(query);
            
            query = new Query("MATCH (conn:Connectable)-[:CONNECTABLE_LOCATED_IN]->(m:Map {name: $mapName}) DETACH DELETE conn",Values.parameters("mapName",mapName));
            result = tx.run(query);
            	            
            query = new Query("MATCH (r:Room)-[:IS_PART_OF]->(m:Map {name: $mapName}) DETACH DELETE r",Values.parameters("mapName",mapName));
            result = tx.run(query);
            
            query = new Query("MATCH (m:Map {name: $mapName}) DETACH DELETE m",Values.parameters("mapName",mapName));
            result = tx.run(query);
            
            query = new Query("MATCH (il:ItemLabel) WHERE NOT EXISTS((:Item)-[]->(il)) DELETE il");
            result = tx.run(query);
            
            query = new Query("MATCH (h:Hour) WHERE NOT EXISTS((:Item)-[]->(h)) DELETE h");
            result = tx.run(query);
            
            query = new Query("MATCH (n:Nationality) WHERE NOT EXISTS((:Item)-[]->(n)) DELETE n");
            result = tx.run(query);
            
            query = new Query("MATCH (d:Date) WHERE NOT EXISTS((:Item)-[]->(d)) DELETE d");
            result = tx.run(query);
            return null;
        });
	}
	
	private void createNodes(Session session) {
		try {
			System.out.println("delete graph");												
			deleteNodes(session,model.getName());
			
			int mapID = session.executeRead(tx -> {
	            Query query = new Query("MATCH (m:Map) RETURN max(m.id)");
	            Result result = tx.run(query);
	            try {
	            	return result.single().get(0).asInt()+1;
	            }catch(Uncoercible e) {
	            	return 0;
	            }
	        });
			
			System.out.println("add map");
			Object mapNode = session.executeWrite(tx -> {
	            Query query = new Query("CREATE (:Map {id: $mapID, name: $mapName, width: $mapWidth, height: $mapHeight, pixelRepresentsInMeters: $PRM, drawIconDimension : $DID})",Values.parameters("mapID",mapID,"mapName",model.getName(),"mapWidth",model.getMAP_W(),"mapHeight",model.getMAP_H(),"PRM",model.getPixelRepresentsInMeters(),"DID",model.getDRAWING_ICON_DIMENSION()));
	            Result result = tx.run(query);
	            return null;
	        });
			
			System.out.println("add rooms");
			for(Room r : model.getRooms()) {
				
				Object roomNode = session.executeWrite(tx -> {
		            Query query = new Query("MATCH (m:Map {id: $mapID}) CREATE (:Room {label: $roomLabel})-[:IS_PART_OF]->(m)",Values.parameters("roomLabel",r.getLabel(),"mapID",mapID));
		            Result result = tx.run(query);
		            return null;
		        });
				
				int i = 0;
				
				List<Pair<Corner,Integer>> corners_id = new ArrayList<Pair<Corner,Integer>>();
				
				for(Corner c: r.getCorners()) {
					i++;
					corners_id.add(new Pair<Corner,Integer>(c,i));
					Object cornerNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (r:Room {label: $roomLabel})-[:IS_PART_OF]->(:Map {id: $mapID}) CREATE (:Corner {vertexLabel: $vertexLabel, coordX: $coordX, coordY: $coordY})-[:CORNER_OF]->(r)",Values.parameters("vertexLabel",c.getVertex_label(),"coordX",c.getVertex_xy().getX(),"coordY",c.getVertex_xy().getY(),"roomLabel",r.getLabel(),"mapID",mapID));
			            Result result = tx.run(query);
			            return null;
			        });
				}
								
				for(RoomSeparator rs : r.getRoomSeparators()) {
					Corner c1 = r.getCorners().stream().filter(c -> c == rs.getC1()).findAny().orElse(null);
					Corner c2 = r.getCorners().stream().filter(c -> c == rs.getC2()).findAny().orElse(null);
					Object roomSeparatorNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (r:Room {label: $roomLabel})-[:IS_PART_OF]->(:Map {id: $mapID}),(c1:Corner {coordX: $coordX_c1, coordY: $coordY_c1})-[:CORNER_OF]->(r),(c2:Corner {coordX: $coordX_c2, coordY: $coordY_c2})-[:CORNER_OF]->(r) CREATE (rs:RoomSeparator {vertexLabel: $vertexLabel})-[:SEPARATOR_OF]->(r),(c1)-[:CORNER_OF]->(rs),(c2)-[:CORNER_OF]->(rs)",Values.parameters("roomLabel",r.getLabel(),"mapID",mapID,"coordX_c1",c1.getVertex_xy().getX(),"coordY_c1",c1.getVertex_xy().getY(),"coordX_c2",c2.getVertex_xy().getX(),"coordY_c2",c2.getVertex_xy().getY(),"vertexLabel",rs.getVertex_label()));
			            Result result = tx.run(query);
			            return null;
			        });
				}
				
			}
						
			System.out.println("add connectables");												
			Set<Pair<Connectable, Connectable>> connected = new HashSet<Pair<Connectable, Connectable>>();
			for(Door d : model.getDoors()) {				
				for (Connectable c: d.getConnectedTo()) {
					Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
					Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
					
					if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
						connected.add(pair);						
				}
								
				if(d.getRoom() == null) {
					Object doorNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (m:Map {id: $mapID}) CREATE (:Connectable {vertexLabel: $vertexLabel,type: $type,coordX: $coordX, coordY: $coordY})-[:CONNECTABLE_LOCATED_IN]->(m)",Values.parameters("mapID",mapID,"vertexLabel",(int)d.getVertex_label(),"type","DOOR","coordX",d.getVertex_xy().getX(),"coordY",d.getVertex_xy().getY()));
			            Result result = tx.run(query);
			            return null;
			        });
				}else {
					Object doorNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (r:Room {label: $roomLabel})-[:IS_PART_OF]->(:Map {id: $mapID}) CREATE (:Connectable {vertexLabel: $vertexLabel,type: $type,coordX: $coordX, coordY: $coordY})-[:CONNECTABLE_LOCATED_IN]->(r)",Values.parameters("roomLabel",d.getRoom().getLabel(),"mapID",mapID,"vertexLabel",(int)d.getVertex_label(),"type","DOOR","coordX",d.getVertex_xy().getX(),"coordY",d.getVertex_xy().getY()));
			            Result result = tx.run(query);
			            return null;
			        });
				}
			}
			
			for(Stairs d : model.getStairs()) {				
				for (Connectable c: d.getConnectedTo()) {
					Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
					Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
					
					if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
						connected.add(pair);						
				}
				
				if(d.getRoom() == null) {
					Object stairsNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (m:Map {id: $mapID}) CREATE (:Connectable {vertexLabel: $vertexLabel,type: $type,coordX: $coordX, coordY: $coordY})-[:CONNECTABLE_LOCATED_IN]->(m)",Values.parameters("mapID",mapID,"vertexLabel",(int)d.getVertex_label(),"type","STAIRS","coordX",d.getVertex_xy().getX(),"coordY",d.getVertex_xy().getY()));
			            Result result = tx.run(query);
			            return null;
			        });
				}else {
					Object stairsNode = session.executeWrite(tx -> {
			            Query query = new Query("MATCH (r:Room {label: $roomLabel})-[:IS_PART_OF]->(:Map {id: $mapID}) CREATE (:Connectable {vertexLabel: $vertexLabel,type: $type,coordX: $coordX, coordY: $coordY})-[:CONNECTABLE_LOCATED_IN]->(r)",Values.parameters("roomLabel",d.getRoom().getLabel(),"mapID",mapID,"vertexLabel",(int)d.getVertex_label(),"type","STAIRS","coordX",d.getVertex_xy().getX(),"coordY",d.getVertex_xy().getY()));
			            Result result = tx.run(query);
			            return null;
			        });
				}
			}
			
			for(Pair<Connectable, Connectable> pair: connected) {
				Drawable c1 = (Drawable)pair.getF();
				Drawable c2 = (Drawable)pair.getS();
				Object connection = session.executeWrite(tx -> {
		            Query query = new Query("MATCH (c1:Connectable {coordX: $coordX1, coordY: $coordY1}),(c2:Connectable {coordX: $coordX2, coordY: $coordY2}) CREATE (c1)-[:CONNECTED_TO]->(c2),(c2)-[:CONNECTED_TO]->(c1)",Values.parameters("coordX1",c1.getVertex_xy().getX(),"coordY1",c1.getVertex_xy().getY(),"coordX2",c2.getVertex_xy().getX(),"coordY2",c2.getVertex_xy().getY()));
		            Result result = tx.run(query);
		            return null;
		        });
			}
			
			System.out.println("add items");
			for(Item i : model.getItems()) {
				Object itemNode = session.executeWrite(tx -> {
		            Query query = new Query("MATCH (r:Room {label: $roomLabel})-[:IS_PART_OF]->(:Map {id: $mapID}) CREATE (:Item {vertexLabel: $vertexLabel,coordX: $coordX, coordY: $coordY, title: $title, width: $width, height: $height, iconURL: $iconURL})-[:ITEM_LOCATED_IN]->(r)",Values.parameters("roomLabel",i.getRoom().getLabel(),"mapID",mapID,"vertexLabel",(int)i.getVertex_label(),"coordX",i.getVertex_xy().getX(),"coordY",i.getVertex_xy().getY(),"title",i.getTitle(),"width",i.getWidth(),"height",i.getHeight(),"iconURL",i.getUrlIcon()));
		            Result result = tx.run(query);
		            
		            query = new Query("MATCH (il:ItemLabel {itemLabel: $itemLabel}) RETURN il",Values.parameters("itemLabel",i.getItemLabel()));
		            result = tx.run(query);
		            if(result.hasNext()) {
		            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}),(il:ItemLabel {itemLabel: $itemLabel}) CREATE (i)-[:LABEL_IS]->(il)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"itemLabel",i.getItemLabel()));
		            }else {
		            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}) CREATE (il:ItemLabel {itemLabel: $itemLabel})<-[:LABEL_IS]-(i)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"itemLabel",i.getItemLabel()));
		            }
		            result = tx.run(query);
		            
		            if(i.getBeginDate() != null && !i.getBeginDate().equals("null")) {
			            query = new Query("MATCH (h:Hour {hour: $hour}) RETURN h",Values.parameters("hour",i.getBeginDate()));
			            result = tx.run(query);
			            if(result.hasNext()) {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}),(h:Hour {hour: $hour}) CREATE (i)-[:OPENS_AT]->(h)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"hour",i.getBeginDate()));
			            }else {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}) CREATE (h:Hour {hour: $hour})<-[:OPENS_AT]-(i)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"hour",i.getBeginDate()));
			            }
			            result = tx.run(query);
		            }
		            
		            if(i.getEndDate() != null && !i.getEndDate().equals("null")) {
			            query = new Query("MATCH (h:Hour {hour: $hour}) RETURN h",Values.parameters("hour",i.getEndDate()));
			            result = tx.run(query);
			            if(result.hasNext()) {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}),(h:Hour {hour: $hour}) CREATE (i)-[:CLOSES_AT]->(h)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"hour",i.getEndDate()));
			            }else {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}) CREATE (h:Hour {hour: $hour})<-[:CLOSES_AT]-(i)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"hour",i.getEndDate()));
			            }
			            result = tx.run(query);
		            }
		            
		            if(i.getNationality() != null && !i.getNationality().equals("null")) {
			            query = new Query("MATCH (n:Nationality {nationality: $nationality}) RETURN n",Values.parameters("nationality",i.getNationality()));
			            result = tx.run(query);
			            if(result.hasNext()) {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}),(n:Nationality {nationality: $nationality}) CREATE (i)-[:HAS_NATIONALITY]->(n)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"nationality",i.getNationality()));
			            }else {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}) CREATE (n:Nationality {nationality: $nationality})<-[:HAS_NATIONALITY]-(i)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"nationality",i.getNationality()));
			            }
			            result = tx.run(query);
		            }
		            	            
		            if(i.getDate() != null && !i.getDate().equals("null")) {
			            query = new Query("MATCH (d:Date {date: $date}) RETURN d",Values.parameters("date",i.getDate()));
			            result = tx.run(query);
			            if(result.hasNext()) {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}),(d:Date {date: $date}) CREATE (i)-[:DATES_FROM]->(d)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"date",i.getDate()));
			            }else {
			            	query = new Query("MATCH (i:Item {vertexLabel: $itemVertexLabel})--{,2}(m:Map {id: $mapID}) CREATE (d:Date {date: $date})<-[:DATES_FROM]-(i)",Values.parameters("itemVertexLabel",i.getVertex_label(),"mapID",mapID,"date",i.getDate()));
			            }
			            result = tx.run(query);
		            }
//		            query = new Query("MATCH (i:Item {vertexLabel: $vertexLabel})--{,2}(m:Map {name: $mapName}) MERGE (il:ItemLabel {itemLabel: $itemLabel})--{,2}(m) ON MATCH      <-[:LABEL_IS]-(i)");
		            return null;
		        });
			}
			System.out.println("end");
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(graphManager, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public void loadMapSelector(Session s) {
		openMap.setEnabled(true);
		List<String> graphMapNames = s.executeRead(tx -> {
            Query query = new Query("MATCH (m:Map) WHERE m.id IS NOT NULL AND m.name IS NOT NULL AND m.width IS NOT NULL AND m.height IS NOT NULL AND m.pixelRepresentsInMeters IS NOT NULL AND m.drawIconDimension IS NOT NULL RETURN m.name");
            Result result = tx.run(query);
            List<String> mapNames = new ArrayList<String>();
            for(Record r : result.list()) {
            	mapNames.add(r.get(0).toString().replace("\"", ""));
            }
            return mapNames;            
        });
		selectionList = new JList<Object>(graphMapNames.toArray());
		selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(selectionList);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		listScroller.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
		selectionList.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
			        if (selectionList.getSelectedIndex() != -1) {
			        	try(var s = d.getDriver().session()){	
			        		JPanel map = new JPanel();
			        		map.setLayout(new BoxLayout(map, BoxLayout.Y_AXIS));
//				        	Object graphMap = s.executeRead(tx -> {
//					            Query query = new Query("MATCH (m:Map {name: $name}) RETURN m",Values.parameters("name",(String)selectionList.getSelectedValue()));
//					            Result result = tx.run(query);
//					            List<String> mapNames = new ArrayList<String>();
//					            Node n = result.single().get(0).asNode();
//					            map.add(new JLabel(n.get("name").toString()));
//					            String st = "";
//					            for(String key: n.keys()) {
//					            	if(!key.equals("name")) st += key+": "+n.get(key)+System.lineSeparator();
//					            }
//					            map.add(new JTextArea(st));
//					            JButton expand = new JButton("Expand");
//					            expand.addActionListener(new ActionListener() {
//					    			public void actionPerformed(ActionEvent arg0) {
//					    				switch(expandedLevel) {
//					    					case 0:
//					    						try(var s = d.getDriver().session()){
//								    				Object graphMap = s.executeRead(tx -> {
//											            Query query = new Query("MATCH (r:Room)-[:IS_PART_OF]->(m:Map {name: $name}) RETURN r",Values.parameters("name",(String)selectionList.getSelectedValue()));
//											            Result result = tx.run(query);
//											            JPanel rooms = new JPanel();
//											            rooms.setBorder(BorderFactory.createTitledBorder("Rooms"));
//											            rooms.setLayout(new BoxLayout(rooms, BoxLayout.Y_AXIS));	
//											            while(result.hasNext()) {
//											            	Node n = result.next().get(0).asNode();
//											            	rooms.add(new JLabel("label: "+n.get("label").toString()));								       
//											            }
//											            JScrollPane roomListScroller = new JScrollPane(rooms);
//											            JPanel roomListContainer = new JPanel();
//											    		roomListContainer.setBorder(BorderFactory.createEmptyBorder());
//											    		roomListContainer.add(roomListScroller);
//											            viewerContainer.add(roomListContainer);
//											            viewerContainer.repaint();
//											            viewerContainer.revalidate();
//											            expandedLevel++;
//											            return null;
//								    				});
//							    				}
//					    						break;					    						
//					    				}				
//					    			}
//					            });
//					            map.add(expand);
//					            viewerContainer.removeAll();
//					            viewerContainer.add(map);
//					            viewerContainer.repaint();
//					            viewerContainer.revalidate();
//					            return null;        
//					        });
			        		Object graphCount = s.executeRead(tx -> {
					            Query query = new Query("MATCH (n)--{1,2}(m:Map {name: $mapName}) RETURN n",Values.parameters("mapName",(String)selectionList.getSelectedValue()));
					            Result result = tx.run(query);
					            numRooms = 0;
				            	numCorners = 0;
				            	numConnectables = 0;
				            	numItems = 0;
				            	numRoomSeparators = 0;
					            while(result.hasNext()) {
					            	Node n = result.next().get(0).asNode();				         
					            	switch(n.labels().toString()) {
					            		case "[Room]":
					            			numRooms++;
					            			break;
					            		case "[Corner]":
					            			numCorners++;
					            			break;
					            		case "[Connectable]":
					            			numConnectables++;
					            			break;
					            		case "[Item]":
					            			numItems++;
					            			break;
					            		case "[RoomSeparator]":
					            			numRoomSeparators++;
					            			break;
					            	}
					            }					            
					    		
					    		saveGraphAsMapFile.setEnabled(true);
					    		saveGraphAsMapSVGFile.setEnabled(true);
					    		deleteGraph.setEnabled(true);
					            
					            return null;
			        		});
			        	
			        		showGraph();
			        	}
			        }else {
			        	saveGraphAsMapFile.setEnabled(false);
			    		saveGraphAsMapSVGFile.setEnabled(false);
			    		deleteGraph.setEnabled(false);
			    		infoPanel.removeAll();
			    		infoPanel.repaint();
			    		infoPanel.revalidate();
			        }
				}
			}
		});
		selector.removeAll();
		selector.add(loadButtons,BorderLayout.PAGE_START);
		selector.add(listScroller,BorderLayout.CENTER);
		selector.add(buttons,BorderLayout.PAGE_END);
		selector.repaint();
		selector.revalidate();		
	}
		
	
	public void clearMapSelector() {
		selector.removeAll();
		JScrollPane listScroller = new JScrollPane();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		listScroller.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
		selector.add(listScroller,BorderLayout.CENTER);
		selector.add(buttons,BorderLayout.PAGE_END);
		selector.repaint();
		selector.revalidate();
		saveGraphAsMapFile.setEnabled(false);
		saveGraphAsMapSVGFile.setEnabled(false);
		deleteGraph.setEnabled(false);
		openMap.setEnabled(false);
		infoPanel.removeAll();
		infoPanel.repaint();
		infoPanel.revalidate();
		scrollGraph.setViewportView(null);
	}
	
	
	Corner findCorner(List<Corner> cornerList, double x, double y) {
		if(!cornerList.isEmpty()) {
			for(Corner c : cornerList) {
				if(c.getVertex_xy().getX() == x && c.getVertex_xy().getY() == y) {
					return c;
				}
			}
		}
		return null;
	}
	
	private void saveGraphInModel(String mapName) {
		try(var s = d.getDriver().session()){
			Object graphMap = s.executeRead(tx -> {
	            Query query = new Query("MATCH (r:Room)-[:IS_PART_OF]->(m:Map {name: $name}) RETURN m,r",Values.parameters("name",mapName));
	            Result result = tx.run(query);
	            Node mapNode = result.peek().get(0).asNode();
	            model.setName(mapName);
	            model.setMAP_W(mapNode.get("width").asInt());
	            model.setMAP_H(mapNode.get("height").asInt());
	            model.setPixelRepresentsInMeters(mapNode.get("pixelRepresentsInMeters").asDouble());
	            model.setDRAWING_ICON_DIMENSION(mapNode.get("drawIconDimension").asInt());
	            while(result.hasNext()) {
	            	Node roomNode = result.next().get(1).asNode();
	            	try(var s2 = d.getDriver().session()){
	            	Object corners = s2.executeRead(tx2 -> {
	    	            Query query2 = new Query("MATCH (c:Corner)-[:CORNER_OF]->(r:Room {label: $roomLabel})-[:IS_PART_OF]->(m:Map {name: $mapName}) RETURN c ORDER BY c.vertexLabel",Values.parameters("roomLabel",roomNode.get("label").asInt(),"mapName",mapName));
	    	            Result result2 = tx2.run(query2);
	    	            List<Corner> roomCorners = new ArrayList<Corner>();
	    	            while(result2.hasNext()) {
	    	            	Node cornerNode = result2.next().get(0).asNode();
	    	            	roomCorners.add(new Corner(null,cornerNode.get("vertexLabel").asLong(),new Point(cornerNode.get("coordX").asDouble(),cornerNode.get("coordY").asDouble())));
	    	            }	    	            
	    	            Room r = new Room(roomNode.get("label").asInt(),roomCorners);
		            	model.addRoom(r);
		            	query2 = new Query("MATCH (c:Connectable)-[:CONNECTABLE_LOCATED_IN]->(r:Room {label: $roomLabel})-[:IS_PART_OF]->(m:Map {name: $mapName}) RETURN c",Values.parameters("roomLabel",roomNode.get("label").asInt(),"mapName",mapName));
	    	            result2 = tx2.run(query2);
	    	            while(result2.hasNext()) {
	    	            	Node connNode = result2.next().get(0).asNode();
	    	            	if(connNode.get("type").toString().equals("\"DOOR\"")) {
	    	            		Door d = new Door(r,connNode.get("vertexLabel").asLong(),new Point(connNode.get("coordX").asDouble(),connNode.get("coordY").asDouble()));
	    	            		model.addDoor(d);    	     
	    	            	}else if(connNode.get("type").toString().equals("\"STAIRS\"")) {
	    	            		Stairs st = new Stairs(r,connNode.get("vertexLabel").asLong(),new Point(connNode.get("coordX").asDouble(),connNode.get("coordY").asDouble()));
	    	            		model.addStairs(st);
	    	            	}    	            	
	    	            }	    	            	    	            	    	            	    	            	    	            	    	            	    	            
	    	            query2 = new Query("MATCH (i:Item)-[:ITEM_LOCATED_IN]->(r:Room {label: $roomLabel})-[:IS_PART_OF]->(m:Map {name: $mapName}) RETURN i",Values.parameters("roomLabel",roomNode.get("label").asInt(),"mapName",mapName));
	    	            result2 = tx2.run(query2);
	    	            while(result2.hasNext()) {
	    	            	Node connNode = result2.next().get(0).asNode();
	    	            	Item i = new Item(r,connNode.get("vertexLabel").asLong(),new Point(connNode.get("coordX").asDouble(),connNode.get("coordY").asDouble()));
	    	            	i.setTitle(connNode.get("title").toString());
	    					i.setNationality(connNode.get("nationality").toString());
	    					i.setBeginDate(connNode.get("beginDate").toString());
	    					i.setEndDate(connNode.get("endDate").toString());
	    					i.setDate(connNode.get("date").toString());
	    					try {
	    						i.setHeight(connNode.get("height").asDouble());
	    						i.setWidth(connNode.get("width").asDouble());
	    					}
	    					catch (Exception ex) {
	    						System.out.println(ex);
	    						i.setHeight(0.0);
	    						i.setWidth(0.0);
	    					}
	    					if(!connNode.get("itemLabel").toString().equals("null")) i.setItemLabel(connNode.get("itemLabel").toString());
	    					if(!connNode.get("iconURL").toString().equals("null")) i.setUrlIcon(connNode.get("iconURL").toString());
	    					model.addItem(i);
	    	            }
	    	            query2 = new Query("MATCH (rs:RoomSeparator)-[:SEPARATOR_OF]->(r:Room {label: $roomLabel})-[:IS_PART_OF]->(m:Map {name: $mapName}),(c1:Corner)-[:CORNER_OF]->(rs),(c2:Corner)-[:CORNER_OF]->(rs) WHERE c1 <> c2 RETURN rs,c1,c2 ORDER BY rs.vertexLabel",Values.parameters("roomLabel",roomNode.get("label").asInt(),"mapName",mapName));
	    	            result2 = tx2.run(query2);
	    	            boolean ignore = false;
	    	            while(result2.hasNext()) {
	    	            	Record row = result2.next();
	    	            	if(!ignore) {
		    	            	Node roomSepNode = row.get(0).asNode();
		    	            	Node c1Node = row.get(1).asNode();
		    	            	Node c2Node = row.get(2).asNode();
		    	            	RoomSeparator rs = new RoomSeparator(r,roomSepNode.get("vertexLabel").asLong(), findCorner(roomCorners,c1Node.get("coordX").asDouble(),c1Node.get("coordY").asDouble()), findCorner(roomCorners,c2Node.get("coordX").asDouble(),c2Node.get("coordY").asDouble()));
		    	            	model.addRoomSeparator(rs);
	    	            	}
	    	            	ignore = !ignore;
	    	            }
	    	            return null;
	            	});
	            	}
	            }
	            query = new Query("MATCH (c:Connectable)-[:CONNECTABLE_LOCATED_IN]->(m:Map {name: $mapName}) RETURN c",Values.parameters("mapName",mapName));
	            result = tx.run(query);
	            while(result.hasNext()) {
	            	Node connNode = result.next().get(0).asNode();
	            	if(connNode.get("type").toString().equals("\"DOOR\"")) {
	            		Door d = new Door(null,connNode.get("vertexLabel").asLong(),new Point(connNode.get("coordX").asDouble(),connNode.get("coordY").asDouble()));
	            		model.addDoor(d);    	     
	            	}else if(connNode.get("type").toString().equals("\"STAIRS\"")) {
	            		Stairs st = new Stairs(null,connNode.get("vertexLabel").asLong(),new Point(connNode.get("coordX").asDouble(),connNode.get("coordY").asDouble()));
	            		model.addStairs(st);
	            	}    	            	
	            }
	            for(Door d: model.getDoors()) {
	            	query = new Query("MATCH (c:Connectable)-[:CONNECTED_TO]->(c2:Connectable {vertexLabel: $vertexLabel, type: $type})--{1,2}(m:Map {name: $mapName}) RETURN c",Values.parameters("vertexLabel",d.getVertex_label(),"type","DOOR","mapName",mapName));
		            result = tx.run(query);
//		            if(result.hasNext()) {
			            while(result.hasNext()) {
			            	Node connNode = result.next().get(0).asNode();
			            	if(connNode.get("type").toString().equals("\"DOOR\"")) {	            		
			            		Door d2 = model.getDoor(connNode.get("vertexLabel").asLong());
			            		if(d2 != null) d.connectTo(d2);
	    	            	}else if(connNode.get("type").toString().equals("\"STAIRS\"")) {
	    	            		Stairs st = model.getStairs(connNode.get("vertexLabel").asLong());
	    	            		if(st != null) d.connectTo(st);
	    	            	}
			            }
//		            }else {
//		            	query = new Query("MATCH (c:Connectable)-[:CONNECTED_TO]->(c2:Connectable {vertexLabel: $vertexLabel, type: $type})-[:CONNECTABLE_LOCATED_IN]->(:Room)-[:IS_PART_OF]->(m:Map {name: $name}) RETURN c",Values.parameters("vertexLabel",d.getVertex_label(),"type","DOOR","mapName",mapName));
//			            result = tx.run(query);
//			            while(result.hasNext()) {
//			            	Node connNode = result.next().get(0).asNode();
//			            	if(connNode.get("type").toString().equals("DOOR")) {	            		
//			            		Door d2 = model.getDoor(connNode.get("vertexLabel").asLong());
//			            		if(d2 != null) d.connectTo(d2);
//	    	            	}else if(connNode.get("type").toString().equals("STAIRS")) {
//	    	            		Stairs st = model.getStairs(connNode.get("vertexLabel").asLong());
//	    	            		if(st != null) d.connectTo(st);
//	    	            	}
//			            }
//		            }
	            }
	            for(Stairs st: model.getStairs()) {
	            	query = new Query("MATCH (c:Connectable)-[:CONNECTED_TO]->(c2:Connectable {vertexLabel: $vertexLabel, type: $type})--{1,2}(m:Map {name: $mapName}) RETURN c",Values.parameters("vertexLabel",st.getVertex_label(),"type","STAIRS","mapName",mapName));
		            result = tx.run(query);
//		            if(result.hasNext()) {
			            while(result.hasNext()) {
			            	Node connNode = result.next().get(0).asNode();
			            	if(connNode.get("type").toString().equals("\"DOOR\"")) {	            		
			            		Door d = model.getDoor(connNode.get("vertexLabel").asLong());
			            		if(d != null) st.connectTo(d);
	    	            	}else if(connNode.get("type").toString().equals("\"STAIRS\"")) {
	    	            		Stairs st2 = model.getStairs(connNode.get("vertexLabel").asLong());
	    	            		if(st2 != null) st.connectTo(st2);
	    	            	}
			            }
//		            }else {
//		            	query = new Query("MATCH (c:Connectable)-[:CONNECTED_TO]->(c2:Connectable {vertexLabel: $vertexLabel, type: $type})-[:CONNECTABLE_LOCATED_IN]->(:Room)-[:IS_PART_OF]->(m:Map {name: $name}) RETURN c",Values.parameters("vertexLabel",st.getVertex_label(),"type","STAIRS","mapName",mapName));
//			            result = tx.run(query);
//			            while(result.hasNext()) {
//			            	Node connNode = result.next().get(0).asNode();
//			            	if(connNode.get("type").toString().equals("DOOR")) {	            		
//			            		Door d = model.getDoor(connNode.get("vertexLabel").asLong());
//			            		if(d != null) st.connectTo(d);
//	    	            	}else if(connNode.get("type").toString().equals("STAIRS")) {
//	    	            		Stairs st2 = model.getStairs(connNode.get("vertexLabel").asLong());
//	    	            		if(st2 != null) st.connectTo(st2);
//	    	            	}
//			            }
//		            }
	            }	            	            	            	            	            	            	            
	            return null;
			});
		}
	}

	private class NodeCounter {
		public int countRooms = 0;
		public int countCorners = 0;
		public int countConnectables = 0;
		public int countItems = 0;
		public int countRoomSeparators = 0;
		public int countItemLabel = 0;
		public int countHour = 0;
		public int countDate = 0;
		public int countNationality = 0;
	}
	
	private void showGraph() {
		try(var s = d.getDriver().session()){
			Object graphCount = s.executeRead(tx -> {
	            Query query = new Query("MATCH (n)-[r]->(n2)--{,3}(:Map {name: $mapName}) RETURN DISTINCT n,r,n2 ORDER BY n",Values.parameters("mapName",(String)selectionList.getSelectedValue()));
	            Result result = tx.run(query);
	            List<Node> nodes = new ArrayList<Node>();
	            NodeCounter nodeCounter = new NodeCounter();
	            graph = new SingleGraph("Graph");
	            SwingViewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
	    		graph.setAttribute("ui.stylesheet", "node { stroke-mode: plain; size: 20px; }");
	            while(result.hasNext()) {
	            	Node n = result.peek().get(0).asNode();
	            	Relationship r = result.peek().get(1).asRelationship();
	            	Node n2 = result.next().get(2).asNode();
	            	String N1 = addNodeToGraph(graph, n, nodes, nodeCounter);
	            	String N2 = addNodeToGraph(graph, n2, nodes, nodeCounter);	       
	            	if(N1 != null && N2 != null) {
	            		graph.addEdge(N1+"_"+N2, N1, N2, true);
	            		graph.getEdge(N1+"_"+N2).setAttribute("type",r.type());
	            	}	            	
	            }
	            
	            viewer.enableAutoLayout();
	    		
	    		ViewerPipe pipe = viewer.newViewerPipe();
	    		LoopController control = new LoopController();
	    		pipe.addViewerListener(new GraphController(pipe,graph,control,infoText));
	    		pipe.addSink(graph);
	    		
	    		View view = viewer.addDefaultView(false);
	    		
	    		Component cView = (Component) view;
	    		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    		cView.setPreferredSize(new Dimension(dim.width*2,dim.height*2));
	    		
	            scrollGraph.setViewportView(cView);
	            scrollGraph.getVerticalScrollBar().setValue((scrollGraph.getViewport().getViewSize().height - scrollGraph.getViewport().getViewRect().height)/2);
	            scrollGraph.getHorizontalScrollBar().setValue((scrollGraph.getViewport().getViewSize().width - scrollGraph.getViewport().getViewRect().width)/2);
	            
	            scrollGraph.revalidate();
	            scrollGraph.repaint();
	            				
	            return null;
			});
		}
	}
	
	private String addNodeToGraph(Graph graph, Node n, List<Node> nodes, NodeCounter nodeCounter) {
		if(!nodes.contains(n)) {
    		nodes.add(n);
    		String nodeName = "";
    		org.graphstream.graph.Node node;
    		switch(n.labels().toString()) {
    			case "[Map]":
    				nodeName = "M";
    				graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Map");
    	    		node.setAttribute("id", n.get("id").asInt());
    	    		node.setAttribute("name", n.get("name").asString());
    	    		node.setAttribute("width", n.get("width").asDouble());
    	    		node.setAttribute("height", n.get("height").asDouble());
    	    		node.setAttribute("pixelRepresentsInMeters", n.get("pixelRepresentsInMeters").asDouble());
    	    		node.setAttribute("drawIconDimension", n.get("drawIconDimension").asDouble());
    	    		node.setAttribute("ui.style", "fill-color: red;");
    	    		node.setAttribute("ui.label", node.getId());
    				break;
        		case "[Room]":
        			nodeCounter.countRooms++;
        			nodeName = "R"+Integer.toString(nodeCounter.countRooms);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Room");
    	    		node.setAttribute("label", n.get("label").asInt());
    	    		node.setAttribute("ui.style", "fill-color: #73EA8E;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Corner]":
        			nodeCounter.countCorners++;
        			nodeName = "C"+Integer.toString(nodeCounter.countCorners);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Corner");
    	    		node.setAttribute("vertexLabel", n.get("vertexLabel").asInt());
    	    		node.setAttribute("coordX", n.get("coordX").asDouble());
    	    		node.setAttribute("coordY", n.get("coordY").asDouble());
    	    		node.setAttribute("ui.style", "fill-color: #02711B;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Connectable]":
        			nodeCounter.countConnectables++;
        			nodeName = "Conn"+Integer.toString(nodeCounter.countConnectables);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Connectable");
    	    		node.setAttribute("vertexLabel", n.get("vertexLabel").asInt());
    	    		node.setAttribute("coordX", n.get("coordX").asDouble());
    	    		node.setAttribute("coordY", n.get("coordY").asDouble());
    	    		node.setAttribute("type", n.get("type").asString());
    	    		node.setAttribute("ui.style", "fill-color: darkturquoise;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Item]":
        			nodeCounter.countItems++;
        			nodeName = "I"+Integer.toString(nodeCounter.countItems);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Item");
    	    		node.setAttribute("vertexLabel", n.get("vertexLabel").asInt());
    	    		node.setAttribute("coordX", n.get("coordX").asDouble());
    	    		node.setAttribute("coordY", n.get("coordY").asDouble());
    	    		node.setAttribute("width", n.get("width").asDouble());
    	    		node.setAttribute("height", n.get("height").asDouble());
    	    		node.setAttribute("iconURL", n.get("iconURL").asString());
    	    		node.setAttribute("title", n.get("title").asString());
    	    		node.setAttribute("ui.style", "fill-color: gold;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[RoomSeparator]":
        			nodeCounter.countRoomSeparators++;
        			nodeName = "RS"+Integer.toString(nodeCounter.countRoomSeparators);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "RoomSeparator");
    	    		node.setAttribute("vertexLabel", n.get("vertexLabel").asInt());
    	    		node.setAttribute("ui.style", "fill-color: darkviolet;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[ItemLabel]":
        			nodeCounter.countItemLabel++;
        			nodeName = "IL"+Integer.toString(nodeCounter.countItemLabel);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "ItemLabel");
    	    		node.setAttribute("itemLabel", n.get("itemLabel").asString());
    	    		node.setAttribute("ui.style", "fill-color: sandybrown;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Hour]":
        			nodeCounter.countHour++;
        			nodeName = "H"+Integer.toString(nodeCounter.countHour);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Hour");
    	    		node.setAttribute("hour", n.get("hour").asString());
    	    		node.setAttribute("ui.style", "fill-color: hotpink;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Date]":
        			nodeCounter.countDate++;
        			nodeName = "D"+Integer.toString(nodeCounter.countDate);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Date");
    	    		node.setAttribute("date", n.get("date").asString());
    	    		node.setAttribute("ui.style", "fill-color: silver;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
        		case "[Nationality]":
        			nodeCounter.countNationality++;
        			nodeName = "N"+Integer.toString(nodeCounter.countNationality);
        			graph.addNode(nodeName);
    	    		node = graph.getNode(nodeName);
    	    		node.setAttribute("nodeType", "Nationality");
    	    		node.setAttribute("nationality", n.get("nationality").asString());
    	    		node.setAttribute("ui.style", "fill-color: brown;");
    	    		node.setAttribute("ui.label", node.getId());
        			break;
    		}
    		return nodeName;
    	}else {
    		for(org.graphstream.graph.Node node : graph) {
    			switch(n.labels().toString()) {
	    			case "[Map]":
	    				if(node.getAttribute("nodeType").toString().equals("Map") && node.getAttribute("id").toString().equals(n.get("id").toString())) {
	        				return node.getId();
	        			}
	    				break;
	        		case "[Room]":
	        			if(node.getAttribute("nodeType").toString().equals("Room") && node.getAttribute("label").toString().equals(n.get("label").toString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Corner]":
	        			if(node.getAttribute("nodeType").toString().equals("Corner") && node.getAttribute("vertexLabel").toString().equals(n.get("vertexLabel").toString()) && node.getAttribute("coordX").toString().equals(n.get("coordX").toString()) && node.getAttribute("coordY").toString().equals(n.get("coordY").toString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Connectable]":
	        			if(node.getAttribute("nodeType").toString().equals("Connectable") && node.getAttribute("vertexLabel").toString().equals(n.get("vertexLabel").toString()) && node.getAttribute("coordX").toString().equals(n.get("coordX").toString()) && node.getAttribute("coordY").toString().equals(n.get("coordY").toString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Item]":
	        			if(node.getAttribute("nodeType").toString().equals("Item") && node.getAttribute("vertexLabel").toString().equals(n.get("vertexLabel").toString())) {
	        				return node.getId();
	        			}    	    		
	        			break;
	        		case "[RoomSeparator]":
	        			if(node.getAttribute("nodeType").toString().equals("RoomSeparator") && node.getAttribute("vertexLabel").toString().equals(n.get("vertexLabel").toString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[ItemLabel]":
	        			if(node.getAttribute("nodeType").toString().equals("ItemLabel") && node.getAttribute("itemLabel").toString().equals(n.get("itemLabel").asString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Hour]":
	        			if(node.getAttribute("nodeType").toString().equals("Hour") && node.getAttribute("hour").toString().equals(n.get("hour").asString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Date]":
	        			if(node.getAttribute("nodeType").toString().equals("Date") && node.getAttribute("date").toString().equals(n.get("date").asString())) {
	        				return node.getId();
	        			}
	        			break;
	        		case "[Nationality]":
	        			if(node.getAttribute("nodeType").toString().equals("Nationality") && node.getAttribute("nationality").toString().equals(n.get("nationality").asString())) {
	        				return node.getId();
	        			}
	        			break;
	    		}
    		}
    		return null;
    	}
	}
}
