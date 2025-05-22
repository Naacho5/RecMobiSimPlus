package es.unizar.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EtchedBorder;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraphView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.unizar.controller.AppListener;
import es.unizar.controller.Controller;
import es.unizar.editor.MapEditor;
import es.unizar.graph.GraphManager;
import es.unizar.gui.graph.DrawFloorGraph;
import es.unizar.gui.simulation.NeglectedEvaluations;
import es.unizar.gui.simulation.User;
import es.unizar.gui.simulation.UserRunnable;
import es.unizar.spatialDB.DBQueryMaker;
import es.unizar.spatialDB.DBViewer;
import es.unizar.spatialDB.DatabaseAccess;
import es.unizar.util.Literals;
import es.unizar.util.Pair;
import es.unizar.util.TextAreaHandler;
import java.awt.FlowLayout;

/**
 * Main window of the MoMA Museum Simulation.
 * 
 * @author Maria del Carmen Rodriguez Hernandez and Alejandro Piedrafita Barrantes
 */
public class MainSimulator {

	/**
	 * Panel size.
	 */
	public static final int DRAWING_WIDTH = 818;
	public static final int DRAWING_HEIGHT = 536;
	
	public static int FRAME_WIDTH = 0; // Modified when constructor finishes (extended window's dimensions)
	public static int FRAME_HEIGHT = 0; // Modified when constructor finishes (extended window's dimensions)

	public static Dimension MIN_SIZE;
	
	// AWT variables declaration - do not modify.
	public static JFrame frmSimulator;
	public static JFrame splitFrame;
	public static JSplitPane splitPane;
	public static JPanel simulationPanel;
	public static TextArea textConsole;
	public JMenuItem generatePathsMenuItem;
	public static JMenuItem startMenuItem;
	public static JMenuItem pausePlayMenuItem;
	public static JMenuItem stopMenuItem;
	public static JMenuItem configurationMenuItem;
	public static JMenuItem neglectedEvaluationsMenuItem;
	//public static JMenuItem floor4MenuItem;
	//public static JMenuItem floor5MenuItem;
	
	public static JMenuBar menuBar;

	private static JCheckBoxMenuItem removeVertexCheckItem;
	private static JCheckBoxMenuItem removeEdgeCheckItem;
	public static JCheckBoxMenuItem gui;

	// Other variables.
	public File mapFile;
	public static File roomFile;
	public static File itemFile;

	public static mxGraphComponent graphComponent;
	public static DrawFloorGraph floor;
	public static FloorPanel floorPanel;
	public static FloorPanelCombined floorPanelCombined;
	//public GraphForSpecialUser graphSpecialUser;

	public static User[] user;
	public static UserRunnable userRunnable;
	
	public static Thread userRunnableThread;
	
	private AppListener appListener;
	
	public static final Logger log = Logger.getLogger(Literals.CONSOLE_LOGGER);

	public static DatabaseAccess db;
	
	private static Map<Integer,UserInfo.UserState> stateOfUsers;
	private static Map<Pair<Integer,Integer>,Double> timeUsersInRooms;
	
	public static UserInfo userInfo;
	
	/* silarri (2022-07-13). */
	private void stopSimulation()
	{
		// Kill the thread.
		userRunnable.setRunning(false);
		printConsole("Stop simulation", Level.WARNING);
		Configuration.simulation.currentTime();

		startMenuItem.setEnabled(true);
		pausePlayMenuItem.setEnabled(false);
		stopMenuItem.setEnabled(false);
		userRunnable = null; /* silarri (2022-07-13). */
		
		/*
		MainMuseumSimulator.floor4MenuItem.setEnabled(true);
		MainMuseumSimulator.floor5MenuItem.setEnabled(true);
		*/		
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting
		// code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel. For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf. html
		 */
		try {
			javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels = javax.swing.UIManager.getInstalledLookAndFeels();
			for (int idx = 0; idx < installedLookAndFeels.length; idx++) {
				if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
					javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(MainSimulator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(MainSimulator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(MainSimulator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(MainSimulator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new MainSimulator();
					frmSimulator.setVisible(true);
					
					// Save frame dimensions
					FRAME_WIDTH = frmSimulator.getSize().width;
					FRAME_HEIGHT = frmSimulator.getSize().height;
					
					// set Panel's location
					//setPanelLocation();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainSimulator() {
		initialize();
		db = new DatabaseAccess();
		// Center a window application. -> PREVIOUS
		/*Dimension objDimension = Toolkit.getDefaultToolkit().getScreenSize();
		int iCoordX = (objDimension.width - frmSimulationOfMoma.getWidth()) / 2;
		int iCoordY = (objDimension.height - frmSimulationOfMoma.getHeight()) / 2;
		frmSimulationOfMoma.setLocation(iCoordX, iCoordY);*/

		// Simulation panel.
		simulationPanel = new JPanel();
		simulationPanel.setBackground(Color.WHITE);
		simulationPanel.setMinimumSize(new Dimension(600, 130));
		simulationPanel.setPreferredSize(new Dimension(600, 130));
		simulationPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		simulationPanel.setForeground(Color.BLACK);
		frmSimulator.getContentPane().add(simulationPanel, BorderLayout.SOUTH);
		
		stateOfUsers = new HashMap<Integer,UserInfo.UserState>();
		timeUsersInRooms = new HashMap<Pair<Integer,Integer>,Double>();
		userInfo = new UserInfo(frmSimulator,stateOfUsers,timeUsersInRooms);
		JButton userInfoButton = new JButton("User info");
		userInfoButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				userInfo.setVisible(true);
//				userInfo.reloadTables();
			}
		});
		
		JButton simQueriesButton = new JButton("Simulation queries");
		simQueriesButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(db.isConnected()) {
					if(splitFrame == null) {
						SimulationQueryMaker sqm = new SimulationQueryMaker();
						int widthScreen = frmSimulator.getWidth();
						MIN_SIZE = frmSimulator.getContentPane().getMinimumSize();
						frmSimulator.getContentPane().setMinimumSize(new Dimension(5,MIN_SIZE.height));
						sqm.simQueryMaker.getContentPane().setMinimumSize(new Dimension(5,sqm.simQueryMaker.getContentPane().getMinimumSize().height));
						splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,frmSimulator.getContentPane(),sqm.simQueryMaker.getContentPane());
						splitPane.setDividerLocation((int)(widthScreen*0.05));
						frmSimulator.setVisible(false);
						sqm.simQueryMaker.setVisible(false);
						splitFrame = initializeFrame();
						splitFrame.add(splitPane);
						splitFrame.setJMenuBar(menuBar);
						splitFrame.setVisible(true);
					}
				}else JOptionPane.showMessageDialog(frmSimulator, "There is not a connection to a PostgreSQL (with PostGIS) database", "WARNING", JOptionPane.WARNING_MESSAGE);
			}
		});
		
		JPanel legendPanel = new JPanel();
		legendPanel.setAutoscrolls(true);
		legendPanel.add(userInfoButton);
		legendPanel.add(simQueriesButton);

		// Console panel.
		JPanel consolePanel = new JPanel();
		GroupLayout gl_simulationPanel = new GroupLayout(simulationPanel);
		gl_simulationPanel.setHorizontalGroup(
			gl_simulationPanel.createParallelGroup(Alignment.LEADING)
				.addComponent(consolePanel, GroupLayout.DEFAULT_SIZE, 1378, Short.MAX_VALUE)
				.addGroup(gl_simulationPanel.createSequentialGroup()
					.addComponent(legendPanel, GroupLayout.DEFAULT_SIZE, 1368, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_simulationPanel.setVerticalGroup(
			gl_simulationPanel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_simulationPanel.createSequentialGroup()
					.addComponent(legendPanel, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(consolePanel, GroupLayout.PREFERRED_SIZE, 88, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);

		// Text console TextArea.
		textConsole = new TextArea();
		textConsole.setEditable(false);
		textConsole.setText("========== Info console ==========" + '\n');
		
		/* Remove all handlers (don't use them) */
		log.setUseParentHandlers(false);
		
		/* Create logger with TextAreaHandler */
		TextAreaHandler textAreaHandler = new TextAreaHandler(textConsole);
		
		/* Set logger LEVEL */
		log.setLevel(Literals.CONSOLE_DEFAULT_LEVEL);
		
		/* Add the handler to the logger */
		log.addHandler(textAreaHandler);
		
		/* Some tests...
		 * LogRecord lr = new LogRecord(Level.FINE, "TEST 1");
		 * log.log(lr);
		 * log.log(Level.SEVERE, "TEST 2");
		 */
		
		
		GroupLayout gl_consolePanel = new GroupLayout(consolePanel);
		gl_consolePanel.setHorizontalGroup(gl_consolePanel.createParallelGroup(Alignment.LEADING).addComponent(textConsole, GroupLayout.DEFAULT_SIZE, 1682, Short.MAX_VALUE));
		gl_consolePanel.setVerticalGroup(gl_consolePanel.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, gl_consolePanel.createSequentialGroup()
				.addComponent(textConsole, GroupLayout.PREFERRED_SIZE, 87, GroupLayout.PREFERRED_SIZE).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		consolePanel.setLayout(gl_consolePanel);

		// Legend label.
		JLabel legendLabel = new JLabel("Legend:");
		legendLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

		// Current user label.
		JLabel currentUserLabel = new JLabel("RS users");
		currentUserLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		/* Modified by silarri (2022-07-15). */
		currentUserLabel.setIcon(new ImageIcon(Literals.IMAGES_PATH + "special_user.png"));

		// Other user label.
		JLabel otherUserLabel = new JLabel("non-RS users");
		otherUserLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		/* Modified by silarri (2022-07-15). */
		otherUserLabel.setIcon(new ImageIcon(Literals.IMAGES_PATH + "non_special_user.png"));

		// Painting label.
		/*JLabel paintingLabel = new JLabel("painting");
		paintingLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		paintingLabel.setIcon(new ImageIcon(MainSimulator.class.getResource("/es/unizar/images/painting.png")));

		// Sculpture label.
		JLabel sculptureLabel = new JLabel("sculpture");
		sculptureLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		sculptureLabel.setIcon(new ImageIcon(MainSimulator.class.getResource("/es/unizar/images/sculpture.png")));
		*/

		// Door label.
		JLabel doorLabel = new JLabel("doors");
		/* Modified by silarri (2022-07-15). */
		doorLabel.setIcon(new ImageIcon(Literals.IMAGES_PATH + "door.png"));
		doorLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

		// Stairs label.
		JLabel stairsLabel = new JLabel("stairs");
		/* Modified by silarri (2022-07-15). */
		stairsLabel.setIcon(new ImageIcon(Literals.IMAGES_PATH + "stairs.png"));
		stairsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		legendPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));
		legendPanel.add(legendLabel);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_1);
		legendPanel.add(currentUserLabel);
		
		JSeparator separator_2 = new JSeparator();
		separator_2.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_2);
		legendPanel.add(otherUserLabel);
		
		/*
		JSeparator separator_3 = new JSeparator();
		separator_3.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_3);
		legendPanel.add(paintingLabel);
		
		JSeparator separator_4 = new JSeparator();
		separator_4.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_4);
		legendPanel.add(sculptureLabel);
		*/
		
		JSeparator separator_5 = new JSeparator();
		separator_5.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_5);
		legendPanel.add(doorLabel);
		
		JSeparator separator_6 = new JSeparator();
		separator_6.setPreferredSize(new Dimension(5, 2));
		legendPanel.add(separator_6);
		legendPanel.add(stairsLabel);
		simulationPanel.setLayout(gl_simulationPanel);

		// Menu.
		menuBar = new JMenuBar();
		frmSimulator.setJMenuBar(menuBar);

		// File menu.
		JMenu fileMenu = new JMenu("File");
		fileMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(fileMenu);
		
		// Exit menu item.
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			// Exit event.
			public void actionPerformed(ActionEvent arg0) {
				if (userRunnable != null) stopSimulation(); /* silarri (2022-07-13). */
				frmSimulator.dispose();
				System.exit(0);
			}
		});
		
		// Create map menu item -> Level editor.
		JMenuItem createMapMenuItem = new JMenuItem("Create or Edit Map");
		createMapMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		/* Modified by silarri (2022-07-15). */
		createMapMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "edit.png"));
		createMapMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new MapEditor(db,null);
			}
		});
		
		fileMenu.add(createMapMenuItem);
		
		JMenuItem dvViewerMenuItem = new JMenuItem("View maps in database");
		dvViewerMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		dvViewerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(db.isConnected()) {
					if(splitFrame == null) {
						DBViewer dbViewer = new DBViewer(db);
						int widthScreen = frmSimulator.getWidth();
						MIN_SIZE = frmSimulator.getContentPane().getMinimumSize();
						dbViewer.MIN_SIZE = dbViewer.dbViewer.getContentPane().getMinimumSize();
						frmSimulator.getContentPane().setMinimumSize(new Dimension(5,MIN_SIZE.height));
						dbViewer.dbViewer.getContentPane().setMinimumSize(new Dimension(5,dbViewer.MIN_SIZE.height));
						splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,frmSimulator.getContentPane(),dbViewer.dbViewer.getContentPane());
						splitPane.setDividerLocation((int)(widthScreen*0.05));
						frmSimulator.setVisible(false);
						dbViewer.dbViewer.setVisible(false);
						splitFrame = initializeFrame();
						splitFrame.add(splitPane);
						splitFrame.setJMenuBar(menuBar);
						splitFrame.setVisible(true);
					}
				}
				else JOptionPane.showMessageDialog(frmSimulator, "There is not a connection to a PostgreSQL (with PostGIS) database", "WARNING", JOptionPane.WARNING_MESSAGE);
			}
		});
		
		fileMenu.add(dvViewerMenuItem);
		
		JMenuItem queryMakerMenuItem = new JMenuItem("Make queries to database");
		queryMakerMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		queryMakerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(db.isConnected()) {
					if(splitFrame == null) {
						DBQueryMaker dbqm = new DBQueryMaker(db);
						int widthScreen = frmSimulator.getWidth();
						MIN_SIZE = frmSimulator.getContentPane().getMinimumSize();
						dbqm.MIN_SIZE = dbqm.dbQueryMaker.getContentPane().getMinimumSize();
						frmSimulator.getContentPane().setMinimumSize(new Dimension(5,MIN_SIZE.height));
						dbqm.dbQueryMaker.getContentPane().setMinimumSize(new Dimension(5,dbqm.MIN_SIZE.height));
						splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,frmSimulator.getContentPane(),dbqm.dbQueryMaker.getContentPane());
						splitPane.setDividerLocation((int)(widthScreen*0.05));
						frmSimulator.setVisible(false);
						dbqm.dbQueryMaker.setVisible(false);
						splitFrame = initializeFrame();
						splitFrame.add(splitPane);
						splitFrame.setJMenuBar(menuBar);
						splitFrame.setVisible(true);
					}
				}
				else JOptionPane.showMessageDialog(frmSimulator, "There is not a connection to a PostgreSQL (with PostGIS) database", "WARNING", JOptionPane.WARNING_MESSAGE);
			}
		});
		
		fileMenu.add(queryMakerMenuItem);
		
		JMenuItem graphManagerMenuItem = new JMenuItem("Manage graphs");
		graphManagerMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		graphManagerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(splitFrame == null) {
					GraphManager grMng = new GraphManager();
					int widthScreen = frmSimulator.getWidth();
					MIN_SIZE = frmSimulator.getContentPane().getMinimumSize();
					frmSimulator.getContentPane().setMinimumSize(new Dimension(5,MIN_SIZE.height));
					grMng.graphManager.getContentPane().setMinimumSize(new Dimension(5,grMng.graphManager.getContentPane().getMinimumSize().height));
					splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,frmSimulator.getContentPane(),grMng.graphManager.getContentPane());
					splitPane.setDividerLocation((int)(widthScreen*0.05));
					frmSimulator.setVisible(false);
					grMng.graphManager.setVisible(false);
					splitFrame = initializeFrame();
					splitFrame.add(splitPane);
					splitFrame.setJMenuBar(menuBar);
					splitFrame.setVisible(true);
				}
			}
		});
		
		fileMenu.add(graphManagerMenuItem);
		
		/*
		JMenuItem loadGraphMenuItem = new JMenuItem("Load Graph");
		loadGraphMenuItem.setEnabled(false);
		loadGraphMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		fileMenu.add(loadGraphMenuItem);
		*/
		
		/* Modified by silarri (2022-07-15). */
		exitMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "exit.png"));
		exitMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		fileMenu.add(exitMenuItem);

		// View menu.
		JMenu viewMenu = new JMenu("View");
		viewMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(viewMenu);
		/*
		// Floor 4 menu item.
		floor4MenuItem = new JMenuItem("Floor 4");
		// Load floor 4.
		floor4MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					// Kill the thread.
					if (userRunnable != null) {
						userRunnable.setRunning(false);
					}

					// Load files
					roomFile = new File(Literals.ROOM_FLOOR_4);
					itemFile = new File(Literals.ITEM_FLOOR_4);
					// informationConsoleLabel.setText("Map of " +
					// floor.floorInformation(roomFile));
					boolean ifRemoveVertexLabel = removeVertexCheckItem.isSelected();
					boolean ifRemoveEdges = removeEdgeCheckItem.isSelected();
					// Draw floor graph
					graphComponent = floor.drawFloor(roomFile, itemFile, ifRemoveVertexLabel, ifRemoveEdges, 4);
					graphComponent.setToolTips(true);
					graphComponent.getViewport().setBackground(new Color(0, 0, 0, 0));
					graphComponent.getViewport().setOpaque(true);

					// Add to the panel
					floorPanel.removeAll();
					floorPanel.add(graphComponent);
					floorPanel.revalidate();
					floorPanel.repaint();

					// The loaded floor is printed on the console.
					printConsole("Loaded floor 4 (only to visualize)", Level.WARNING);

					// The simulation is activate (start, stop and pause options).
					startMenuItem.setEnabled(false);
					stopMenuItem.setEnabled(false);
				} catch (Exception except) {
					System.out.println(except.getMessage());
					MainMuseumSimulator.printConsole(except.getMessage(), Level.SEVERE);
				}
			}
		});
		floor4MenuItem.setIcon(new ImageIcon(MainMuseumSimulator.class.getResource("/es/unizar/images/floor.png")));
		floor4MenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		viewMenu.add(floor4MenuItem);

		// Floor 5 menu item.
		floor5MenuItem = new JMenuItem("Floor 5");
		// Load floor 5.
		floor5MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					// Kill the thread.
					if (userRunnable != null) {
						userRunnable.setRunning(false);
					}

					// Load files
					roomFile = new File(Literals.ROOM_FLOOR_5);
					itemFile = new File(Literals.ITEM_FLOOR_5);
					// informationConsoleLabel.setText("Map of " +
					// floor.floorInformation(roomFile));
					boolean ifRemoveVertexLabel = removeVertexCheckItem.isSelected();
					boolean ifRemoveEdges = removeEdgeCheckItem.isSelected();
					// Draw floor graph
					graphComponent = floor.drawFloor(roomFile, itemFile, ifRemoveVertexLabel, ifRemoveEdges, 5);
					graphComponent.setToolTips(true);
					graphComponent.getViewport().setBackground(new Color(0, 0, 0, 0));
					graphComponent.getViewport().setOpaque(true);

					// Add to the panel
					floorPanel.removeAll();
					floorPanel.add(graphComponent);
					floorPanel.revalidate();
					floorPanel.repaint();

					// The loaded floor is printed on the console.
					printConsole("Loaded floor 5 (only to visualize).", Level.WARNING);

					// The simulation is activate (start, stop and pause options).
					startMenuItem.setEnabled(false);
					stopMenuItem.setEnabled(false);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					MainMuseumSimulator.printConsole(e.getMessage(), Level.SEVERE);
				}
			}
		});
		floor5MenuItem.setIcon(new ImageIcon(MainMuseumSimulator.class.getResource("/es/unizar/images/floor.png")));
		floor5MenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		viewMenu.add(floor5MenuItem);
		// Separator.
		JSeparator separator_view_1 = new JSeparator();
		viewMenu.add(separator_view_1);
		*/
		
		
		// Remove edge check item.
		removeEdgeCheckItem = new JCheckBoxMenuItem("Remove edges");
		removeEdgeCheckItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		/* Modified by silarri (2022-07-15). */
		removeEdgeCheckItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "edges.png"));
		viewMenu.add(removeEdgeCheckItem);
		// Remove vertex label check item.
		removeVertexCheckItem = new JCheckBoxMenuItem("Remove vertex labels");
		/* Modified by silarri (2022-07-15). */
		removeVertexCheckItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "vertex.png"));
		removeVertexCheckItem.setSelected(true);
		removeVertexCheckItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		viewMenu.add(removeVertexCheckItem);

		JSeparator separator = new JSeparator();
		viewMenu.add(separator);

		JMenuItem scaleTranslateMenuItem = new JMenuItem("Scale and Translate");
		scaleTranslateMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ScaleAndTranslate st = new ScaleAndTranslate(frmSimulator);
				st.setVisible(true);
				mxGraphView viewGraph = graphComponent.getGraph().getView();
				// Decimal value that specifies the new scale (1 is 100%).
				double scale = st.scaleValue;
				// coordinates of the translation.
				double x = st.xValue;
				double y = st.yValue;
				viewGraph.scaleAndTranslate(scale, x, y);
				
				// NOT NEEDED
				// Modify mxGraphComponent's preferred size (floor panel dimensions * scale for correct scrolling)
				//graphComponent.setPreferredSize(new Dimension( (int) (floorPanel.getWidth()), (int) (floorPanel.getHeight())));
			}
		});
		/* Modified by silarri (2022-07-15). */
		scaleTranslateMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "scale.png"));
		scaleTranslateMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		viewMenu.add(scaleTranslateMenuItem);
		
		JSeparator separator2 = new JSeparator();
		viewMenu.add(separator2);
		
		gui = new JCheckBoxMenuItem("GUI");
		gui.setSelected(true);
		gui.setFont(new Font("SansSerif", Font.PLAIN, 16));
		viewMenu.add(gui);
		 
		 
		// Simulation menu.
		JMenu simulationMenu = new JMenu("Simulation");
		simulationMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(simulationMenu);
		
		/*
		// Separator.
		JSeparator separator_simulation_1 = new JSeparator();
		simulationMenu.add(separator_simulation_1);
		*/
		
		// Start menu item.
		startMenuItem = new JMenuItem("Start");
		startMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				printConsole("Simulation started", Level.WARNING);
				// Create and execute the threads.
				reloadInfoTables();

				userRunnable = new UserRunnable(user,stateOfUsers,timeUsersInRooms);

				userRunnableThread = new Thread(userRunnable);
				

				try {
					if(db.isConnected() && db.getLoadedMapName() != null && db.getMap(db.getLoadedMapName()).next()) {
						RegisterSimInDB regSim = new RegisterSimInDB(frmSimulator);
						regSim.setVisible(true);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				userRunnableThread.start();

				if (gui.isSelected())
					UserRunnable.repaint();
				
				// Enable pause/play
				startMenuItem.setEnabled(false);
				pausePlayMenuItem.setEnabled(true);
				stopMenuItem.setEnabled(true);
				
			}
		});
		startMenuItem.setEnabled(false);
		/* Modified by silarri (2022-07-15). */
		startMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "start.png"));
		startMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		simulationMenu.add(startMenuItem);
		
		// Stop menu item.
		stopMenuItem = new JMenuItem("Stop");
		stopMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				stopSimulation(); /* silarri, 2022-07-13. */
			}
		});
		
		// Pause/Play menu item.
		pausePlayMenuItem = new JMenuItem("Pause");
		pausePlayMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				// Check if it is paused or playing
				if (!userRunnable.paused) { // Play -> switch to pause
					System.out.println("PLAYING: switching to PAUSE");
					
					printConsole("Simulation paused", Level.WARNING);
					
					userRunnable.setPaused(true);
					
					pausePlayMenuItem.setText("Play");
					/* Modified by silarri (2022-07-15). */
					pausePlayMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "start.png"));
					
				}
				else { // Paused -> switch to play
					System.out.println("PAUSED: switching to PLAY");
					
					printConsole("Simulation continued", Level.WARNING);
					
					// Notify lock -> Play
					synchronized (userRunnable.getPauseLock()) {
						userRunnable.setPaused(false);
						userRunnable.getPauseLock().notifyAll(); // Unblocks thread
			        }
					
					pausePlayMenuItem.setText("Pause");
					/* Modified by silarri (2022-07-15). */
					pausePlayMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "pause.png"));
				}
				
				Configuration.simulation.currentTime(); // Print the time when the action has been done
				
				/*
				// Not enabled while running, enabled while not running
				MainMuseumSimulator.floor4MenuItem.setEnabled(userRunnable.paused);
				MainMuseumSimulator.floor5MenuItem.setEnabled(userRunnable.paused);
				*/
			}
		});
		pausePlayMenuItem.setEnabled(false);
		/* Modified by silarri (2022-07-15). */
		pausePlayMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "pause.png"));
		pausePlayMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		simulationMenu.add(pausePlayMenuItem);
		
		stopMenuItem.setEnabled(false);
		/* Modified by silarri (2022-07-15). */
		stopMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "stop.png"));
		stopMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		simulationMenu.add(stopMenuItem);
		// Separator.
		JSeparator separator_simulation_2 = new JSeparator();
		simulationMenu.add(separator_simulation_2);
		// Configuration menu item.
		configurationMenuItem = new JMenuItem("Configuration");
		configurationMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		configurationMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Configuration config = new Configuration(frmSimulator, true);
				config.setVisible(true);
			}
		});
		/* Modified by silarri (2022-07-15). */
		configurationMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "configuration.png"));
		simulationMenu.add(configurationMenuItem);
		
		// Separator.
		JSeparator separator_simulation_3 = new JSeparator();
		simulationMenu.add(separator_simulation_3);
		
		// Configuration menu item.
		neglectedEvaluationsMenuItem = new JMenuItem("Neglected Evaluations");
		neglectedEvaluationsMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		neglectedEvaluationsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				NeglectedEvaluations neglectedEvaluations = new NeglectedEvaluations(frmSimulator);
				neglectedEvaluations.setVisible(true);
			}

		});
		//neglectedEvaluationsMenuItem.setIcon(new ImageIcon(MainMuseumSimulator.class.getResource("/es/unizar/images/***.png")));
		simulationMenu.add(neglectedEvaluationsMenuItem);
		
		// File menu.
		JMenu aboutMenu = new JMenu("About");
		aboutMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(aboutMenu);
		
		// About RecMobiSim
		JMenuItem aboutRecMobiSim = new JMenuItem("About RecMobiSim");
		aboutRecMobiSim.addActionListener(new ActionListener() {
			// Exit event.
			public void actionPerformed(ActionEvent arg0) {
				AboutRecMobiSim aboutRMS = new AboutRecMobiSim(frmSimulator);
				aboutRMS.setVisible(true);
			}
		});
		aboutMenu.add(aboutRecMobiSim);
		
		JMenuItem connDatabase = new JMenuItem("Database connection");
		connDatabase.addActionListener(new ActionListener() {
			// Exit event.
			public void actionPerformed(ActionEvent arg0) {
				ConnectToDB connectDB = new ConnectToDB(frmSimulator,db);
				connectDB.setVisible(true);				
			}
		});
		menuBar.add(connDatabase);
		
		// Initialize the files to null.
		this.mapFile = null;
		roomFile = null;
		itemFile = null;

		// Build an object of GraphForSpecialUser.
		//graphSpecialUser = new GraphForSpecialUser();

		// Build the FloorPanel component on which the floor graph is drawn.
		int w = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
		int h = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		
		//System.out.println("Effective screen size: (" + w + "," + h + ")");
		floorPanel = new FloorPanel(w, h);
		/*floorPanel.setMinimumSize(new Dimension(DRAWING_WIDTH, DRAWING_HEIGHT));
		floorPanel.setPreferredSize(new Dimension(DRAWING_WIDTH, DRAWING_HEIGHT));
		floorPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		frmSimulationOfMoma.getContentPane().add(floorPanel, BorderLayout.CENTER);*/
		frmSimulator.getContentPane().add(floorPanel);

		// Build an object to draw the floor graph.
		floor = new DrawFloorGraph();
		
		// Set App Listener
		Controller controller = new Controller();
		this.setAppListener(controller);
		
		// Add Window listeners
		frmSimulator.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				fireCloseEvent();
			}
		});
		
		/*
		frmSimulator.addWindowStateListener(new WindowStateListener() {
			   public void windowStateChanged(WindowEvent arg0) {
				   System.out.println("State changed");
				   
				   floorPanel.modifySize(Configuration.simulation.graphSpecialUser.accessRoomFile.getMapWidth(), Configuration.simulation.graphSpecialUser.accessRoomFile.getMapHeight());
		        	floorPanelCombined.modifySize(Configuration.simulation.graphSpecialUser.accessRoomFile.getMapWidth(), Configuration.simulation.graphSpecialUser.accessRoomFile.getMapHeight());
		        	repaintFloorPanelCombined();
			   }
			});
		
		frmSimulator.addComponentListener(new ComponentAdapter() {
		    public void componentResized(ComponentEvent componentEvent) {
		    	System.out.println("RESIZED");
		        try {
		        	floorPanel.modifySize(Configuration.simulation.graphSpecialUser.accessRoomFile.getMapWidth(), Configuration.simulation.graphSpecialUser.accessRoomFile.getMapHeight());
		        	floorPanelCombined.modifySize(Configuration.simulation.graphSpecialUser.accessRoomFile.getMapWidth(), Configuration.simulation.graphSpecialUser.accessRoomFile.getMapHeight());
		        	repaintFloorPanelCombined();
		        }
		        catch(Exception e) {
		        	// floorPanelCombined not initialized yet
		        	e.printStackTrace();
		        }
		    }
		});
		*/
	}

	public static void printConsole(String text, Level level) {
		/*
		 * PREVIOUS CODE
		 * 
		 * textConsole.append(text + "\n");
		 * textConsole.setCaretPosition(textConsole.getText().length());
		 * textConsole.paint(textConsole.getGraphics());
		 */
		
		log.log(level, text);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSimulator = new JFrame();
		frmSimulator.setTitle("RecMobiSim");
		
		/* Modified by silarri (2022-07-15). */
		ImageIcon appImg = new ImageIcon(Literals.IMAGES_PATH + "RecMobiSim.png");
		Image img = appImg.getImage();
		Image newimg = img.getScaledInstance(16, 16, Image.SCALE_DEFAULT);
		//appImg = new ImageIcon(newimg);
		//ImageIcon appImg = new ImageIcon(Literals.IMAGES_PATH + "RecMobiSim.png").getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT);
		frmSimulator.setIconImage(newimg);
		//frmSimulator.setIconImage(Toolkit.getDefaultToolkit().getImage(MainSimulator.class.getResource("/es/unizar/images/RecMobiSim.png")));
		
		Dimension d = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
		d.width = d.width/2;
		d.height = d.height/2;
		
		// Set half screen as minimum size
		frmSimulator.setMinimumSize(d);
		
		// Previous -> FIXED NUMBERS
		//frmSimulationOfMoma.setBounds(100, 100, 1700, 750);
		
		setPanelLocation();
		
		// JFrame fullscreen
		frmSimulator.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		//frmSimulationOfMoma.setResizable(false); // IN CASE YOU WANT SCREEN TO BE FULLSCREEN ALL THE TIME
		
		frmSimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private JFrame initializeFrame() {
		JFrame frame = new JFrame();
		frame.setTitle("RecMobiSim");
		
		/* Modified by silarri (2022-07-15). */
		ImageIcon appImg = new ImageIcon(Literals.IMAGES_PATH + "RecMobiSim.png");
		Image img = appImg.getImage();
		Image newimg = img.getScaledInstance(16, 16, Image.SCALE_DEFAULT);
		//appImg = new ImageIcon(newimg);
		//ImageIcon appImg = new ImageIcon(Literals.IMAGES_PATH + "RecMobiSim.png").getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT);
		frame.setIconImage(newimg);
		//frmSimulator.setIconImage(Toolkit.getDefaultToolkit().getImage(MainSimulator.class.getResource("/es/unizar/images/RecMobiSim.png")));
		
		Dimension d = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
		d.width = d.width/2;
		d.height = d.height/2;
		
		// Set half screen as minimum size
		frame.setMinimumSize(d);
		
		// Previous -> FIXED NUMBERS
		//frmSimulationOfMoma.setBounds(100, 100, 1700, 750);
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX()/2 - frame.getWidth()/2;
        int y = 0;
        frame.setLocation(x, y); // x centered, y top
		
		// JFrame fullscreen
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		//frmSimulationOfMoma.setResizable(false); // IN CASE YOU WANT SCREEN TO BE FULLSCREEN ALL THE TIME
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		return frame;
	}
	
	/**
	 * Repaint floor panel combined.
	 */
	public static void repaintFloorPanelCombined() {
		
		// Refresh to update user's positions
		floor.refreshFloorGraph();
		
		graphComponent = floor.getRoomGraphComponent();
		
		
		/*
		
		// Add to graphComponent the floorPanelCombined (that contains the users):
		floorPanelCombined.removeAll();
		floorPanelCombined.add(graphComponent);
		*/
		
		// Revalidate and repaint to update the screen, not needed to remove and add again the graphComponent
		//floorPanelCombined.revalidate();
		//floorPanelCombined.repaint();
		
		floorPanelCombined.removeAll();
		floorPanelCombined.add(graphComponent);
		floorPanelCombined.revalidate();
		floorPanelCombined.repaint();
		
	}

	/**
	 * Load all existing floors (full scenario). Creates graph.
	 */
	public static void loadFloorCombined() {
		try {
			// Load files
			roomFile = new File(Literals.ROOM_FLOOR_COMBINED);
			itemFile = new File(Literals.ITEM_FLOOR_COMBINED);
			boolean ifRemoveVertexLabel = removeVertexCheckItem.isSelected();
			boolean ifRemoveEdges = removeEdgeCheckItem.isSelected();
			// Draw floor graph
			graphComponent = floor.drawFloor(roomFile, itemFile, ifRemoveVertexLabel, ifRemoveEdges, 2);
			graphComponent.setToolTips(true);
			graphComponent.getViewport().setBackground(new Color(0, 0, 0, 0));
			graphComponent.getViewport().setOpaque(true);

			// Add to graphComponent the floorPanelCombined (that contains the users):
			floorPanelCombined.removeAll();
			floorPanelCombined.add(graphComponent);
			if (gui.isSelected()) {
				floorPanelCombined.revalidate();
				floorPanelCombined.repaint();
			}

			// Load the dictionary, the graph of recommendation y gets the paths for number of users.
			floor.loadDiccionaryItemLocation();

			// The loaded floors are printed on the console.
			printConsole("Loaded floors. Ready to start the simulation: Simulation/Start.", Level.WARNING);
		} catch (Exception e) {
			e.printStackTrace();
//			System.out.println(e.getMessage());
			MainSimulator.printConsole(e.getMessage(), Level.SEVERE);
		}
	}
	
	/**
	 * Set panel's location centering its x position and placing it on top (y position = 0).
	 */
	public static void setPanelLocation() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX()/2 - frmSimulator.getWidth()/2;
        int y = 0;
        frmSimulator.setLocation(x, y); // x centered, y top
	}
	
	/**
	 * Sets application listener to react to several events.
	 * 
	 * @param appListener
	 */
	public void setAppListener(AppListener appListener) {
		this.appListener = appListener;
	}
	
	/**
	 * Fires close event when user click window close button.
	 */
	private void fireCloseEvent() {
		if (appListener != null) {
			System.out.println("Close button pressed: disconnect from database");
			appListener.onClose();
		}
	}
	
	public static void reloadInfoTables() {
		stateOfUsers.clear();
		timeUsersInRooms.clear();
		userInfo.reloadTables();
	}
}
