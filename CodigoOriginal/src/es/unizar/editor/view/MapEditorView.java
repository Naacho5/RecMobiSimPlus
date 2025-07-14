package es.unizar.editor.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.MediaTracker;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.opencsv.CSVWriter;

import es.unizar.editor.controller.MapEditorController;
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
import es.unizar.spatialDB.DatabaseAccess;
import es.unizar.util.EditorLiterals;
import es.unizar.util.Literals;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;

/**
 * Editor's main GUI screen.
 *
 * @author Alejandro Piedrafita Barrantes
 */
public class MapEditorView {
	
	// MVC's Model and Controller
	private final MapEditorModel model;
	private final MapEditorController controller;
	
	// Main frame -> contains all of the rest of level editor.
	public static JFrame levelEditor;
	
	// Size of the icons IN BUTTONS
	private static int ICON_SIZE = 16;
	
	// Directory (path) selected to load/save files.
	private File directory = new File(EditorLiterals.PATH_EDITOR);
	private File visitablesDirectory = new File(EditorLiterals.PATH_VISITABLE_ITEMS);
	
	// CSV writter for storing item's properties
	public CSVWriter csvWriter;
	
	public DatabaseAccess db;
	
	private String mapToEdit = null;

	/**
	 * Create MapEditorView with controller.
	 * 
	 * @param controller
	 */
	public MapEditorView(MapEditorModel model, MapEditorController controller, DatabaseAccess db, String mapToEdit) {
		
		this.model = model;
		this.controller = controller;
		this.db = db;
		
		initialize();
		
		levelEditor.pack();
		
		// After all has been initialized, set visible
		levelEditor.setVisible(true);
		
		this.mapToEdit = mapToEdit;
		if(mapToEdit != null) {
			loadMapFromDB();
			newOrOpenMap(model.getMAP_W(), model.getMAP_H(), model.getPixelRepresentsInMeters());
			refresh();
		}
	}

	/**
	 * Initialize JFrame's parameters and elements.
	 */
	private void initialize() {
		
		// Create JFrame
		levelEditor = new JFrame();
		
		// JFrame occupies fullscreen
		levelEditor.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// Set minimum size as window's dimensions / 4
		// Avoids minimum size
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		levelEditor.setMinimumSize(new Dimension(dim.width/4, dim.height/4));
		
		// JFrame's title and icon
		levelEditor.setTitle("RecMobiSim - Map Editor");
		levelEditor.setIconImage(new ImageIcon(Literals.LOGO_PATH).getImage());
		
		// Set close operation -> Comment. If not, both editor and simulator close!!!
		// TODO: in case exit without save -> Window asking do you want to exit without saving
		//levelEditor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow choosing the directory (to save the 3 files)
		fileChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		txtChooser = new JFileChooser();
		txtChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.txt", "txt"));
		txtChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		csvChooser = new JFileChooser();
		csvChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.csv", "csv"));
		csvChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		svgChooser = new JFileChooser();
		svgChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.svg", "svg"));
		svgChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		initFrames();
		
		initializeMenuBar();
		
		initStatusBar();
		
		initTools();
		
		initMapSettings();
		
		initIconsContainer();
		
		toolContainerLayout();
		
		initEditorView();
		
		newOrOpenMap(500, 500, 1);
		
	}

	/**
	 * Inits all frames (main containers in window) in their positions.
	 * 
	 * Width: toolContainer = 2/10 ; editorContainer = 8/10
	 */
	private void initFrames() {
		
		levelEditor.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		/**
		 * Tools container
		 */
        toolContainer = new JPanel();
        toolContainer.setBorder(BorderFactory.createTitledBorder("Tools"));
        toolContainer.setPreferredSize(new Dimension(2*dim.width/10, dim.height));
        //levelEditor.getContentPane().add(toolContainer, BorderLayout.WEST);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.2; // Occupies 20% of the width
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		levelEditor.getContentPane().add(toolContainer, gbc);

		/**
		 * Editor Container
		 */
        editorContainer = new JPanel();
        editorContainer.setBorder(BorderFactory.createTitledBorder("Map Editor"));
        editorContainer.setPreferredSize(new Dimension(8*dim.width/10, dim.height));
        //levelEditor.getContentPane().add(editorContainer, BorderLayout.CENTER);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 1; // To the right (2nd column)
		gbc.gridy = 0;
		gbc.weightx = 0.8; // Occupies 80% of the width
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		levelEditor.getContentPane().add(editorContainer, gbc);

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
		levelEditor.getContentPane().add(statusBar, gbc);
    }

	/**
	 * Initializes the Menu Bar (located on top of the window).
	 */
	private void initializeMenuBar() {
		// Menu.
		JMenuBar menuBar = new JMenuBar();
		levelEditor.setJMenuBar(menuBar);
		
		/**
		 * File menu.
		 */
		JMenu fileMenu = new JMenu("File");
		fileMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(fileMenu);
		
		
		// File items.
		
		// New map
		newMapItem = new JMenuItem("New");
		newMapItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		newMapItem.addActionListener(new ActionListener() {
			// New Map event
			public void actionPerformed(ActionEvent arg0) {
				NewMap newMap = new NewMap(levelEditor);
				newMap.setVisible(true);
				
				int w = newMap.width;
				int h = newMap.height;
				double representsInMeters = newMap.pixelRepresentsMeters;
				
				model.clearAll();
				
				newOrOpenMap(w, h, representsInMeters);
				
			}
			
		});
		
		// Open map
		//
		openMapMenu = new JMenu("Open");
		openMapMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		
		MapEditorView view = this;
		openMapSVGItem = new JMenuItem("Open SVG file");
		openMapSVGItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		openMapSVGItem.addActionListener(new ActionListener() {
			// Open Map event
			public void actionPerformed(ActionEvent arg0) {
				
				svgChooser.setCurrentDirectory(directory);
				svgChooser.setDialogTitle("Open map");
				
				switch(svgChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = svgChooser.getSelectedFile();
						
						model.clearAll();
						SVGParser p = new SVGParser(model,view,f.getAbsolutePath());
						p.parse();
						newOrOpenMap(model.getMAP_W(), model.getMAP_H(), model.getPixelRepresentsInMeters());
						
						System.out.println("parser");
						for(Room r : model.getRooms()) {
							//System.out.println(r.getLabel()+" "+r.getCorners());
						}
						
						refresh();
						
						// If concave rooms: Inform user
						int numConcaveRooms = model.checkConcaveRooms();
						if (numConcaveRooms > 0)
							showMessage(JOptionPane.WARNING_MESSAGE, "There are concave rooms. Please, add room separators if needed.");
						
						break;
						
					default:
						break;
						
				}
			}
		});
		
		//
		
		
		
		
		openMapItem = new JMenuItem("Open text files");
		openMapItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		
		openMapItem.addActionListener(new ActionListener() {
			// Open Map event
			public void actionPerformed(ActionEvent arg0) {
				
				fileChooser.setCurrentDirectory(directory);
				fileChooser.setDialogTitle("Open map");
				
				switch(fileChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = fileChooser.getSelectedFile();
						
						model.setFiles(f.getAbsolutePath());
						model.clearAll();
						model.load(0, 0);
						
						newOrOpenMap(model.getMAP_W(), model.getMAP_H(), model.getPixelRepresentsInMeters());
						
						refresh();
						
						// If concave rooms: Inform user
						int numConcaveRooms = model.checkConcaveRooms();
						if (numConcaveRooms > 0)
							showMessage(JOptionPane.WARNING_MESSAGE, "There are concave rooms. Please, add room separators if needed.");
						
						break;
						
					default:
						break;
						
				}
			}
		});
				
				
		// Save map
		
		//
		saveMapMenu = new JMenu("Save as");
		saveMapMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		
		saveMapSVGItem = new JMenuItem("Save as SVG file");
		saveMapSVGItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		
		saveMapSVGItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				// If concave rooms: Inform user
				int numConcaveRooms = model.checkConcaveRooms();
				if (numConcaveRooms > 0)
					showMessage(JOptionPane.WARNING_MESSAGE, "There are concave rooms. Please, add room separators if needed before saving.");
				
				fileChooser.setCurrentDirectory(directory);
				fileChooser.setDialogTitle("Save");
				
				switch(fileChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						directory = fileChooser.getSelectedFile();
						
						if(!filesExistInDirectory(directory)) {
							//model.createFiles();
							if(model.saveSVG(directory.getAbsolutePath()))
								showMessage(JOptionPane.INFORMATION_MESSAGE, "SVG file saved in " + directory);
							else
								showMessage(JOptionPane.ERROR_MESSAGE, "Constraints violated. Please, check:\n - All rooms have at least one door.\n - All doors are connected to at least one element.\n - All items are inside a room.");
						}
						else {
							showMessage(JOptionPane.WARNING_MESSAGE, "File(s) already exist in directory");
						}
						
						break;
						
					default:
						break;
					
				}
			}
		});		
		//
		
		saveMapItem = new JMenuItem("Save as text files");
		saveMapItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		saveMapItem.addActionListener(new ActionListener() {
			// Save Map event
			public void actionPerformed(ActionEvent arg0) {
				
				// If concave rooms: Inform user
				int numConcaveRooms = model.checkConcaveRooms();
				if (numConcaveRooms > 0)
					showMessage(JOptionPane.WARNING_MESSAGE, "There are concave rooms. Please, add room separators if needed before saving.");
				
				fileChooser.setCurrentDirectory(directory);
				fileChooser.setDialogTitle("Save");
				
				switch(fileChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						directory = fileChooser.getSelectedFile();
						
						if(!filesExistInDirectory(directory)) {
							model.setFiles(directory.getAbsolutePath());
							//model.createFiles();
							if(model.save(true))
								showMessage(JOptionPane.INFORMATION_MESSAGE, "Item, room and graph files saved in " + directory);
							else
								showMessage(JOptionPane.ERROR_MESSAGE, "Constraints violated. Please, check:\n - All rooms have at least one door.\n - All doors are connected to at least one element.\n - All items are inside a room.");
						}
						else {
							showMessage(JOptionPane.WARNING_MESSAGE, "File(s) already exist in directory");
						}
						
						break;
						
					default:
						break;
						
				}
				
			}
		});
		
		// Exit menu item.
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setIcon(new ImageIcon(Literals.IMAGES_PATH + "exit.png"));
		exitMenuItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		exitMenuItem.addActionListener(new ActionListener() {
			// Exit event.
			public void actionPerformed(ActionEvent arg0) {
				levelEditor.dispose();
				// TODO: in case exit without save -> Window asking do you want to exit without saving
			}
		});
		
		
		saveDatabaseItem = new JMenuItem("Save map in spatial database");
		saveDatabaseItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		saveDatabaseItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(db.isConnected()) {
					try {
						//db.createTables();
						int add_upd = db.saveMap(model);
						if(add_upd == 0) {
							showMessage(JOptionPane.INFORMATION_MESSAGE, "Map added");
						}else {
							showMessage(JOptionPane.INFORMATION_MESSAGE, "Map updated");
						}
					} catch (SQLException|NullPointerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						showMessage(JOptionPane.ERROR_MESSAGE, "Error when updating database");
					}
				}else {
					showMessage(JOptionPane.WARNING_MESSAGE, "There is not a connection to a PostgreSQL (with PostGIS) database");
				}
			}
		});
		
		fileMenu.add(newMapItem);
		fileMenu.add(openMapMenu);
		openMapMenu.add(openMapItem);
		openMapMenu.add(openMapSVGItem);
		fileMenu.add(saveMapMenu);
		//fileMenu.add(saveMapItem);
		saveMapMenu.add(saveMapItem);
		saveMapMenu.add(saveMapSVGItem);
		fileMenu.add(saveDatabaseItem);
		fileMenu.add(exitMenuItem);
		
		/**
		 * Edit menu.
		 */
		JMenu editMenu = new JMenu("Edit");
		editMenu.setFont(new Font("SansSerif", Font.PLAIN, 16));
		menuBar.add(editMenu);
		
		// File items.
		
		// Add x items in random positions
		addItemsRandomPositionItem = new JMenuItem("Add items in random positions");
		addItemsRandomPositionItem.setFont(new Font("SansSerif", Font.PLAIN, 16));
		addItemsRandomPositionItem.addActionListener(new ActionListener() {
			// Add items in random positions
			public void actionPerformed(ActionEvent arg0) {
				AddItemsRandomPositions addItemsRandomPositions = new AddItemsRandomPositions(levelEditor, model);
				addItemsRandomPositions.setVisible(true);
				refresh();
			}
			
		});
		
		// Add x items in random positions
		combineFloors = new JMenuItem("Combine floors");
		combineFloors.setFont(new Font("SansSerif", Font.PLAIN, 16));
		combineFloors.addActionListener(new ActionListener() {
			// Combine floors
			public void actionPerformed(ActionEvent arg0) {
				combineFloors();
			}
			
		});
		
		// Add x items in random positions
		exportItemFile = new JMenuItem("Export items to CSV");
		exportItemFile.setFont(new Font("SansSerif", Font.PLAIN, 16));
		exportItemFile.addActionListener(new ActionListener() {
			// Combine floors
			public void actionPerformed(ActionEvent arg0) {
				exportItemsAsCSV();
			}
			
		});
		
		// Add x items in random positions
		reverseCoordinates = new JMenuItem("Reverse coordinates");
		reverseCoordinates.setFont(new Font("SansSerif", Font.PLAIN, 16));
		reverseCoordinates.addActionListener(new ActionListener() {
			// Reverse all coordinates
			public void actionPerformed(ActionEvent arg0) {
				revertMap();
				
			}
			
		});
		
		editMenu.add(addItemsRandomPositionItem);
		editMenu.add(combineFloors);
		editMenu.add(exportItemFile);
		editMenu.add(reverseCoordinates);
		
	}

	/**
	 * Initializes the status bar (located on the bottom of the frame).
	 */
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
	
	/**
	 * Init the tools container (tools to paint, erase, etc. + elements to be painted, erased, etc.)
	 */
	private void initTools() {
		
		// Create panel where all drawing tools (buttons) will be located.
		tools = new JPanel(new GridLayout(1, 4)); // numColumns has to be == numTools/Buttons
		tools.setBorder(BorderFactory.createTitledBorder("Drawing Tools"));
		
		// Create tools/buttons
		JButton pencil = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "pencil.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		pencil.setName(EditorLiterals.PENCIL);
		pencil.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		pencil.addActionListener(controller);
		pencil.setToolTipText("Drawing tool. Click it and then click the object you want to draw. If you are drawing a room (several corners), use right click to complete the room.");
		
		// <a target="_blank" href="https://iconos8.es/icon/OsZpOKw5ekrl/eraser">Eraser</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		JButton rubber = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "eraser.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		rubber.setName(EditorLiterals.ERASER);
		rubber.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		rubber.addActionListener(controller);
		rubber.setToolTipText("Eraser tool. Click it and then click the object you want to erase from the map panel. If you click a corner, the complete room will be erased. To erase a room separator click in one point of the segment.");
		
		JButton cursor = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "cursor.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		cursor.setName(EditorLiterals.CURSOR);
		cursor.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		cursor.addActionListener(controller);
		cursor.setToolTipText("Selection tool. With double click in an object located in the panel, it will display it's properties, which can be edited.");
		
		// <a target="_blank" href="https://iconos8.es/icon/42174/mover">Mover</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		JButton mover = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "mover.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		mover.setName(EditorLiterals.MOVER);
		mover.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		mover.addActionListener(controller);
		mover.setToolTipText("Moving tool. By clicking this tool, you can move (drag and drop) items in the map if the movement is correct and it doesn't violate real-world conditions.");
		
		tools.add(pencil);
		tools.add(rubber);
		tools.add(cursor);
		tools.add(mover);
		
	}
	
	/**
	 * Specifies (if wanted) from where to where to paint.
	 */
	private void initMapSettings() {
		
		/**
		 * All text fields will be of the specified value.
		 */
		int TEXT_FIELD_COLS = 10;
		
		// Create panel with Layout and (titled) border
		mapSettings = new JPanel(); // numColumns has to be == numTools/Buttons
		mapSettings.setBorder(BorderFactory.createTitledBorder("Map settings"));
		
		// Create map settings labels
		name_Label = new JLabel("Name:");
		name_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		name_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		floor_Label = new JLabel("Floor:");
		floor_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		floor_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		width_Label = new JLabel("Width:");
		width_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		width_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		height_Label = new JLabel("Height:");
		height_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		height_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		pixelRepresentsInMeters_Label = new JLabel("1 pixel represents:");
		pixelRepresentsInMeters_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		pixelRepresentsInMeters_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		JLabel meters_Label = new JLabel(" m");
		meters_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		meters_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		zoom_Label = new JLabel("Zoom:");
		zoom_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		zoom_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		iconDimensions_Label = new JLabel("Icon Dimensions:");
		iconDimensions_Label.setFont(new Font("SansSerif", Font.PLAIN, 14));
		iconDimensions_Label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		
		// Create map settings
		name = new JTextField();
		name.setName(EditorLiterals.MAP_NAME);
		name.setFont(new Font("SansSerif", Font.PLAIN, 14));
		name.setColumns(TEXT_FIELD_COLS/2);
		name.addFocusListener(controller);
		name.addActionListener(controller);
		
		floor = new JTextField();
		floor.setName(EditorLiterals.MAP_FLOOR);
		floor.setFont(new Font("SansSerif", Font.PLAIN, 14));
		floor.setColumns(TEXT_FIELD_COLS/3);
		floor.addFocusListener(controller);
		floor.addActionListener(controller);
		
		width = new JTextField();
		width.setName(EditorLiterals.MAP_WIDTH);
		width.setFont(new Font("SansSerif", Font.PLAIN, 14));
		width.setColumns(TEXT_FIELD_COLS);
		//width.setEditable(false);
		width.addFocusListener(controller);
		width.addActionListener(controller);
		// height.getDocument().addDocumentListener(controller)); // For document changes
		
		height = new JTextField();
		height.setName(EditorLiterals.MAP_HEIGHT);
		height.setFont(new Font("SansSerif", Font.PLAIN, 14));
		height.setColumns(TEXT_FIELD_COLS);
		//height.setEditable(false);
		height.addFocusListener(controller);
		height.addActionListener(controller);
		
		pixelRepresentsInMeters = new JTextField();
		pixelRepresentsInMeters.setName(EditorLiterals.MAP_PIXEL_REPRESENTS_IN_METERS);
		pixelRepresentsInMeters.setFont(new Font("SansSerif", Font.PLAIN, 14));
		pixelRepresentsInMeters.setColumns(TEXT_FIELD_COLS);
		//pixelRepresentsInMeters.setEditable(false);
		pixelRepresentsInMeters.addFocusListener(controller);
		pixelRepresentsInMeters.addActionListener(controller);
		
		zoom = new JTextField();
		zoom.setName(EditorLiterals.MAP_ZOOM);
		zoom.setFont(new Font("SansSerif", Font.PLAIN, 14));
		zoom.setColumns(TEXT_FIELD_COLS);
		zoom.addFocusListener(controller);
		zoom.addActionListener(controller);
		
		iconDimensions = new JTextField();
		iconDimensions.setName(EditorLiterals.MAP_ICONDIMENSIONS);
		iconDimensions.setFont(new Font("SansSerif", Font.PLAIN, 14));
		iconDimensions.setColumns(TEXT_FIELD_COLS);
		iconDimensions.addFocusListener(controller);
		iconDimensions.addActionListener(controller);
		
		/*
		JButton btn = new JButton("Draw!");
		btn.setIcon(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "pencil.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		btn.setFont(new Font("SansSerif", Font.BOLD, 20));
		*/
		
		// Group Layout
		GroupLayout gl_fromTo = new GroupLayout(mapSettings);
		gl_fromTo.setHorizontalGroup(
			gl_fromTo.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_fromTo.createSequentialGroup()
					.addGroup(gl_fromTo.createParallelGroup(Alignment.LEADING)
						.addComponent(name_Label)
						.addComponent(width_Label)
						.addComponent(height_Label)
						.addComponent(pixelRepresentsInMeters_Label)
						.addComponent(zoom_Label)
						.addComponent(iconDimensions_Label))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_fromTo.createSequentialGroup()
								.addComponent(name)
								.addGap(10))
						.addGroup(gl_fromTo.createSequentialGroup()
							.addComponent(width)
							.addGap(10))
						.addGroup(gl_fromTo.createSequentialGroup()
							.addComponent(pixelRepresentsInMeters)
							.addGap(10))
						.addGroup(gl_fromTo.createSequentialGroup()
							.addComponent(height)
							.addGap(10))
						.addGroup(gl_fromTo.createSequentialGroup()
							.addComponent(zoom)
							.addGap(10))
						.addGroup(gl_fromTo.createSequentialGroup()
								.addComponent(iconDimensions)
								.addGap(10))
					)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.LEADING)
						.addComponent(floor_Label)
					)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.LEADING)
						.addComponent(floor)
					)
				)
				/*
				.addGroup(gl_fromTo.createSequentialGroup()
					.addGroup(gl_fromTo.createParallelGroup(Alignment.TRAILING)
						.addComponent(btn)
					)
				)*/
		);
		gl_fromTo.setVerticalGroup(
			gl_fromTo.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_fromTo.createSequentialGroup()
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(name_Label)
						.addComponent(name, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(floor_Label)
						.addComponent(floor, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(width_Label)
						.addComponent(width, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(height, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(height_Label))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(pixelRepresentsInMeters, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(pixelRepresentsInMeters_Label))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(zoom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(zoom_Label))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
							.addComponent(iconDimensions, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(iconDimensions_Label))
					/*
					.addGroup(gl_fromTo.createParallelGroup(Alignment.BASELINE)
						.addComponent(btn, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					*/
				)
		);
		mapSettings.setLayout(gl_fromTo);
	}
	
	/**
	 * Contains all icons that can be painted.
	 */
	private void initIconsContainer() {
		// Initialize Panel
		iconContainer = new JPanel(new GridBagLayout()); 
		iconContainer.setBorder(BorderFactory.createTitledBorder("Objects:"));
		
		// Scenario label, where basic items for scenario creating 
		// (rooms and elements to move from one room to another) will be located.
		lblScenario = new JLabel("Scenario");
		lblScenario.setVerticalAlignment(SwingConstants.TOP);
		lblScenario.setHorizontalAlignment(SwingConstants.LEFT);
		lblScenario.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		GridBagConstraints gbc_lblScenario = new GridBagConstraints();
		gbc_lblScenario.gridx = 0;
		gbc_lblScenario.gridy = 0;
		gbc_lblScenario.weightx = 1;
		gbc_lblScenario.weighty = 0.1;
		gbc_lblScenario.fill = GridBagConstraints.BOTH;
		iconContainer.add(lblScenario, gbc_lblScenario);
		
		// Where main icons (not visitable) will be located. Fixed list.
		scenarioIcons = new JPanel();
		scenarioIcons.setLayout(new GridLayout(0, 4, 0, 0));
		
		// Create fixed scenario icons
		JButton corner = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "corner.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		corner.setName(EditorLiterals.CORNER);
		corner.addActionListener(controller);
		corner.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		corner.setToolTipText("Corner (room's corner)");
		
		/*
		JButton edge = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "edges.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		edge.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		edge.setToolTipText("Edge (to connect corners and create rooms)");
		*/
		
		JButton door = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "door.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		door.setName(EditorLiterals.DOOR);
		door.addActionListener(controller);
		door.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		door.setToolTipText("Door (change room, same floor)");
		
		JButton stairs = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "stairs.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		stairs.setName(EditorLiterals.STAIRS);
		stairs.addActionListener(controller);
		stairs.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		stairs.setToolTipText("Stairs (to change floor)");
		
		// <a target="_blank" href="https://iconos8.es/icon/120152/linea-discontinua">Linea discontinua</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		/* silarri (2022-07-15). */
		JButton roomSeparator = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "dashedLine.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		roomSeparator.setName(EditorLiterals.ROOMSEPARATOR);
		roomSeparator.addActionListener(controller);
		roomSeparator.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		roomSeparator.setToolTipText("Room separator. For concave rooms");
		
		scenarioIcons.add(corner);
		//scenarioIcons.add(edge);
		scenarioIcons.add(door);
		scenarioIcons.add(stairs);
		scenarioIcons.add(roomSeparator);
		
		GridBagConstraints gbc_scenarioIcons = new GridBagConstraints();
		gbc_scenarioIcons.gridx = 0;
		gbc_scenarioIcons.gridy = 1;
		gbc_scenarioIcons.weightx = 1;
		gbc_scenarioIcons.weighty = 0.2;
		gbc_scenarioIcons.fill = GridBagConstraints.BOTH;
		iconContainer.add(scenarioIcons, gbc_scenarioIcons);
		
		// Visitable icons label.
		lblVisitable = new JLabel("Visitable items");
		//lblVisitable.setHorizontalAlignment(SwingConstants.LEFT);
		lblVisitable.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		GridBagConstraints gbc_lblVisitable = new GridBagConstraints();
		gbc_lblVisitable.gridx = 0;
		gbc_lblVisitable.gridy = 2;
		gbc_lblVisitable.weightx = 1;
		gbc_lblVisitable.weighty = 0.1;
		gbc_lblVisitable.fill = GridBagConstraints.BOTH;
		iconContainer.add(lblVisitable, gbc_lblVisitable);
		
		// Where visitable icons will be located. Not fixed list.
		visitableIcons = new JPanel(new FlowLayout());
		JScrollPane visitableIconsScroll = new JScrollPane(visitableIcons);
		
		GridBagConstraints gbc_visitableIcons = new GridBagConstraints();
		gbc_visitableIcons.gridx = 0;
		gbc_visitableIcons.gridy = 3;
		gbc_visitableIcons.weightx = 1;
		gbc_visitableIcons.weighty = 0.6;
		gbc_visitableIcons.fill = GridBagConstraints.BOTH;
		iconContainer.add(visitableIconsScroll, gbc_visitableIcons);
		
		// TOOLS FOR ADDING, DELETING AND MODIFYING VISITABLE OBJECTS
		
		// Visitable icons' tools label.
		lblVisitableTools = new JLabel("Visitable items' tools");
		lblVisitableTools.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		GridBagConstraints gbc_lblVisitableTools = new GridBagConstraints();
		gbc_lblVisitableTools.gridx = 0;
		gbc_lblVisitableTools.gridy = 4;
		gbc_lblVisitableTools.weightx = 1;
		gbc_lblVisitableTools.weighty = 0.1;
		gbc_lblVisitableTools.fill = GridBagConstraints.BOTH;
		iconContainer.add(lblVisitableTools, gbc_lblVisitableTools);
		
		// Where main icons (not visitable) will be located. Fixed list.
		visitableIconsTools = new JPanel();
		visitableIconsTools.setLayout(new GridLayout(0, 5, 0, 0));
		
		// Create visitable icon's tools
		
		// <a target="_blank" href="https://iconos8.es/icon/84991/a%C3%B1adir">A�adir</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		JButton addVisitable = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "add.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		addVisitable.setName(EditorLiterals.ADD_VISITABLE);
		addVisitable.addActionListener(controller);
		addVisitable.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		addVisitable.setToolTipText("Add a new visitable object");
		
		JButton deleteVisitable = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "bin.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		deleteVisitable.setName(EditorLiterals.DELETE_VISITABLE);
		deleteVisitable.addActionListener(controller);
		deleteVisitable.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		deleteVisitable.setToolTipText("Delete an existing visitable object");
		
		JButton editVisitable = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "edit.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		editVisitable.setName(EditorLiterals.EDIT_VISITABLE);
		editVisitable.addActionListener(controller);
		editVisitable.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		editVisitable.setToolTipText("Edit an existing visitable object");
		
		// <a target="_blank" href="https://iconos8.es/icon/59875/guardar">Guardar</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		JButton saveVisitable = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "save.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		saveVisitable.setName(EditorLiterals.SAVE_VISITABLE);
		//saveVisitable.addActionListener(controller);
		saveVisitable.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		saveVisitable.setToolTipText("Save the existing visitable objects to file");
		saveVisitable.addActionListener(new ActionListener() {
			// Save visitable event
			public void actionPerformed(ActionEvent arg0) {
				
				txtChooser.setCurrentDirectory(visitablesDirectory);
				txtChooser.setDialogTitle("Save set of visitable items");
				
				switch(txtChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = txtChooser.getSelectedFile();
						
						visitableIcons.removeAll();
						model.saveVisitableObjectsToFile(f);
						//refreshVisitableButtons();
						//refresh();
						break;
						
					default:
						break;
						
				}
			}
		});
		
		
		// <a target="_blank" href="https://iconos8.es/icon/45785/cargar-documento-de-enlace">Cargar documento de enlace</a> icono de <a target="_blank" href="https://iconos8.es">Icons8</a>
		JButton loadVisitable = new JButton(new ImageIcon(new ImageIcon(Literals.IMAGES_PATH + "load.png").getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		loadVisitable.setName(EditorLiterals.LOAD_VISITABLE);
		//loadVisitable.addActionListener(controller);
		loadVisitable.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		loadVisitable.setToolTipText("Load one or more visitable objects from file");
		loadVisitable.addActionListener(new ActionListener() {
			// Load visitable event
			public void actionPerformed(ActionEvent arg0) {
				
				txtChooser.setCurrentDirectory(visitablesDirectory);
				txtChooser.setDialogTitle("Load set of visitable items");
				
				switch(txtChooser.showOpenDialog(levelEditor)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = txtChooser.getSelectedFile();
						
						visitableIcons.removeAll();
						model.loadVisitableObjectsToFile(f);
						refreshVisitableButtons();
						refresh();
						break;
						
					default:
						break;
						
				}
			}
		});
		
		
		
		visitableIconsTools.add(addVisitable);
		visitableIconsTools.add(deleteVisitable);
		visitableIconsTools.add(editVisitable);
		visitableIconsTools.add(saveVisitable);
		visitableIconsTools.add(loadVisitable);
		
		GridBagConstraints gbc_visitableIconsTools = new GridBagConstraints();
		gbc_visitableIconsTools.gridx = 0;
		gbc_visitableIconsTools.gridy = 5;
		gbc_visitableIconsTools.weightx = 1;
		gbc_visitableIconsTools.weighty = 0.2;
		gbc_visitableIconsTools.fill = GridBagConstraints.BOTH;
		iconContainer.add(visitableIconsTools, gbc_visitableIconsTools);
		
	}
	
	/**
	 * Set the Tools' panel layout.
	 */
	private void toolContainerLayout() {
		
		toolContainer.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		// Tools
		gbc.gridx = 0;
		gbc.gridy = 0;
		//gbc.gridwidth = 1;
		//gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
		toolContainer.add(tools, gbc);
		
		// FromTo
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
		toolContainer.add(mapSettings, gbc);
		
		// Icons
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 0.8;
		gbc.fill = GridBagConstraints.BOTH;
		toolContainer.add(iconContainer, gbc);
		
	}
	
	/**
	 * Inits the panel where the map will be drawn.
	 */
	private void initEditorView() {
		
		this.editorContainer.setLayout(new BoxLayout(this.editorContainer, BoxLayout.PAGE_AXIS));
		
		this.mapPanel = new MapPanel(model);
		this.mapPanel.addMapPanelListener(controller);
		this.mapPanel.setVisible(false);
		
		// http://www.fredosaurus.com/notes-java/GUI/layouts/20borderlayout.html
		// To prevent component resizing, add a component to a JPanel with FlowLayout, and then add 
		// that panel to the BorderLayout. This is a common way to prevent resizing. The FlowLayout 
		// panel will stretch, but the component in it will not.
		mapPanelContainer = new JPanel(new FlowLayout());
		mapPanelContainer.add(mapPanel);
        
		//mapPanel.addListener(controller);

        JScrollPane scroll = new JScrollPane(mapPanelContainer);
		
		scroll.setAutoscrolls(true);
        
        editorContainer.add(scroll, BorderLayout.CENTER);
	}
	
	/**
	 * Set map visible with its settings. ZOOM 1.0 BY DEFAULT (MODIFY IT LATER IF WANTED).
	 * 
	 * @param w
	 * @param h
	 * @param representsInMeters
	 */
	public void newOrOpenMap(int w, int h, double representsInMeters) {
		
		// Set values
		mapPanel.setMAP_W(w);
		mapPanel.setMAP_H(h);
		mapPanel.setZOOM(1.0); // By default
		mapPanel.setPixelRepresentsInMeters(representsInMeters);
		
		// Show values in map settings
		name.setText(model.getName());
		floor.setText(model.getFloor());
		width.setText(Integer.toString(w));
		height.setText(Integer.toString(h));
		pixelRepresentsInMeters.setText(Double.toString(representsInMeters));
		zoom.setText(Double.toString(1.0));
		iconDimensions.setText(Integer.toString(model.getDRAWING_ICON_DIMENSION()));
		
		//mapPanel.setEnabled(true);
		mapPanel.setVisible(true);
		
		mapPanel.addMapPanelListener(controller);
	}
	
	// END INIT
	//----------------------------------------------------------------
	
	/**
	 * Show a message with its type and content.
	 * 
	 * @param messageType	JOptionPane.messageType
	 * @param message		Content of the message
	 */
	public void showMessage(int messageType, String message) {
		String title = "";
		
		switch (messageType) {
			case JOptionPane.ERROR_MESSAGE:
				title = "ERROR";
				break;
			case JOptionPane.INFORMATION_MESSAGE:
				title = "INFO";
				break;
			case JOptionPane.PLAIN_MESSAGE:
				title = "MESSAGE";
				break;
			case JOptionPane.QUESTION_MESSAGE:
				title = "QUESTION";
				break;
			case JOptionPane.WARNING_MESSAGE:
				title = "WARNING";
				break;
			default:
				title = "INFO";
				break;
		}
		
		JOptionPane.showMessageDialog(levelEditor, message, title, messageType);
	}
	
	/**
	 * Refresh view when changes have happened.
	 */
	public void refresh() {
		levelEditor.repaint();
        mapPanel.repaint();
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
	
	

	/**
	 * Main Swing Elements in window
	 */
	public MapPanel mapPanel;
	private JPanel mapPanelContainer; // To avoid auto-resizing when panel's size < editorContainer's size.
	private JPanel toolContainer;
    private JPanel editorContainer;
    private JPanel statusBar;
    
    private JPanel tools;
    private JPanel mapSettings;
    private JPanel iconContainer;
    
    public JLabel statusBarX;
    public JLabel statusBarY;
    public JLabel hoveredElement;
    public JLabel selectedElement;
    
    //
    private JMenu saveMapMenu, openMapMenu;
    //
    private JMenuItem newMapItem, openMapItem, openMapSVGItem, saveMapItem, saveMapSVGItem, exitMenuItem, saveDatabaseItem;
    private JMenuItem addItemsRandomPositionItem, combineFloors, exportItemFile, reverseCoordinates;
    private JLabel name_Label, floor_Label;
    private JLabel width_Label;
    private JTextField name, floor;
    private JTextField width;
    private JLabel height_Label;
    private JTextField height;
    private JLabel pixelRepresentsInMeters_Label;
    private JTextField pixelRepresentsInMeters;
    private JLabel zoom_Label;
    private JLabel iconDimensions_Label;
    private JTextField zoom;
    private JTextField iconDimensions;
    private JLabel lblScenario;
    private JPanel scenarioIcons;
    private JLabel lblVisitable;
    private JPanel visitableIcons;
    private JLabel lblVisitableTools;
    private JPanel visitableIconsTools;
    
    private JFileChooser fileChooser;
    private JFileChooser txtChooser;
    private JFileChooser csvChooser;
    private JFileChooser svgChooser;

    //
    public void setMapConf(int w, int h, double z, int icDim, String nameString) {
    	width.setText(Integer.toString(w));
    	mapPanel.setMAP_W(w);
    	height.setText(Integer.toString(h));
    	mapPanel.setMAP_H(h);
    	zoom.setText(Double.toString(z));
    	mapPanel.setZOOM(z);
    	iconDimensions.setText(Integer.toString(icDim));
    	name.setText(nameString);
    }
    //
    
	public boolean existsVisitableObject(String vertex_label) {
		return model.alreadyExistsVisitableObject(vertex_label);
	}
	
	public boolean createVisitableObject(String key, Properties value) {
		return model.addVisitableObject(key, value);
	}

	/**
	 * Refresh/update visitable buttons when buttons are modified.
	 */
	public void refreshVisitableButtons() {

		System.out.println("=== DEBUG refreshVisitableButtons() ==="); // Añadido por Nacho Palacio 2025-07-04
		System.out.println("📁 RUTAS CONFIGURADAS:");
		System.out.println("  - Literals.IMAGES_PATH: " + Literals.IMAGES_PATH);
		System.out.println("  - Literals.LOGO_PATH: " + Literals.LOGO_PATH);
		System.out.println("  - Literals.VERTEX_URL: " + Literals.VERTEX_URL);
		System.out.println("  - ICON_SIZE: " + ICON_SIZE);
		
		visitableIcons.removeAll();
		
		Map<String, Properties> buttons = model.getVisitableObjects();

		System.out.println("📊 Total objetos visitables cargados: " + buttons.size()); // Añadido por Nacho Palacio 2025-07-04
		
		for (Map.Entry<String, Properties> entry : buttons.entrySet()) {
		    JButton visitableButton = new JButton();
		    visitableButton.setName(EditorLiterals.VISITABLE + EditorLiterals.SEPARATOR + entry.getKey());
		    visitableButton.setToolTipText(entry.getKey());
		    
		    String iconURL = entry.getValue().getProperty(Literals.VERTEX_URL + entry.getKey());

		    if (iconURL == null || iconURL.equals("")) {
		    	iconURL = Literals.LOGO_PATH;
		    }

			// Añadido por Nacho Palacio 2025-07-04
        	String finalIconURL = iconURL;

			// Añadido por Nacho Palacio 2025-07-04
			File iconFile = new File(iconURL);
			System.out.println("  - Archivo original existe: " + iconFile.exists());
        	System.out.println("  - Ruta absoluta original: " + iconFile.getAbsolutePath());

			if (!iconFile.exists()) {
				String fileName = iconURL;
				if (fileName.contains("\\")) {
					// Es ruta de Windows
					String[] parts = fileName.split("\\\\");
					fileName = parts[parts.length - 1];
				} else if (fileName.contains("/")) {
					// Es ruta Unix
					String[] parts = fileName.split("/");
					fileName = parts[parts.length - 1];
				}
				
				finalIconURL = Literals.IMAGES_PATH + fileName;
				File correctedFile = new File(finalIconURL);
				
				if (!correctedFile.exists()) {
					finalIconURL = Literals.LOGO_PATH;
				}
			}

		    try {
				// Añadido por Nacho Palacio 2025-07-04
				File finalFile = new File(finalIconURL);
				if (finalFile.exists()) {
					ImageIcon originalIcon = new ImageIcon(finalIconURL);
					
					if (originalIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
						Image scaledImage = originalIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
						visitableButton.setIcon(new ImageIcon(scaledImage));
					} else {
						ImageIcon defaultIcon = new ImageIcon(Literals.LOGO_PATH);
						if (defaultIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
							Image scaledImage = defaultIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
							visitableButton.setIcon(new ImageIcon(scaledImage));
						}
					}
				} else {
					visitableButton.setText(entry.getKey().substring(0, Math.min(entry.getKey().length(), 3)));
				}

		    	// visitableButton.setIcon(new ImageIcon(new ImageIcon(iconURL).getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT)));
		    }
		    catch (Exception e) {
		    	System.out.println(e);
		    }
		    visitableButton.addActionListener(controller);
		    visitableIcons.add(visitableButton);
		    
		}
		
		levelEditor.pack();
		levelEditor.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
	}
	
	/**
	 * Create "Combine Floors" window.
	 */
	private void combineFloors() {
		CombineFloors combineFloors = new CombineFloors(this, model);
		combineFloors.setVisible(true);
		refresh();
	}
	
	/**
	 * Export all items with their values to csv.
	 */
	private void exportItemsAsCSV() {
		// Open Map event
		csvChooser.setCurrentDirectory(directory);
		csvChooser.setDialogTitle("Select csv file to store item's properties");
		
		switch(csvChooser.showOpenDialog(levelEditor)) {
		
			case JFileChooser.APPROVE_OPTION:
				
				File f = csvChooser.getSelectedFile();
				
				try {
					FileWriter output = new FileWriter(f);
					csvWriter = new CSVWriter(output, ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
					
					/**
					 * HEADER
					 */
					// Write header
			        String[] header = { "id_item", "type", "title", "nationality", "beginDate", "endDate", "date", "width", "height" };
			        csvWriter.writeNext(header);
					
			        
			        /**
			         * CSV ENTRIES
			         */
			        for (Item i: model.getItems()) {
						String[] csvEntry = new String[] { Long.toString(i.getVertex_label()), i.getItemLabel(), i.getTitle(),
								i.getNationality(), i.getBeginDate(), i.getEndDate(), i.getDate(), Double.toString(i.getWidth()),
								Double.toString(i.getHeight())};
						
						// Write all entries
						csvWriter.writeNext(csvEntry);
			        }
					
					csvWriter.close();
					
					showMessage(JOptionPane.INFORMATION_MESSAGE, "Item file \"" + f.getName() +"\" saved in: " + directory);
				}
				catch (IOException e) {
					e.printStackTrace();
					showMessage(JOptionPane.ERROR_MESSAGE, "Exception trying to save item's data");
				}
				
				break;
				
			default:
				break;
				
		}
	}
	
	/**
	 * Revert map's dimensions (map panel and variables) and item's coordinates
	 */
	protected void revertMap() {
		// Revert map's dimensions
		int auxVar = model.getMAP_H();
		mapPanel.setMAP_H(model.getMAP_W());
		mapPanel.setMAP_W(auxVar);
		// Revert all element's coordinates
		model.revertCoordinates();
		refresh();
	}
    
	public void setDbAccess(DatabaseAccess dbAcc) {
		db = dbAcc;
	}
	
	public DatabaseAccess getDbAccess() {
		return db;
	}
	
	public void loadMapFromDB() {
		try {
			ResultSet rs = db.getMap(mapToEdit);
			if(rs.next()) {
				model.setMAP_W(rs.getInt("width"));
				model.setMAP_H(rs.getInt("height"));
				model.setName(mapToEdit);
				model.setPixelRepresentsInMeters(rs.getDouble("pixel_represents_in_meters"));
				model.setDRAWING_ICON_DIMENSION(rs.getInt("draw_icon_dimension"));
				int mapID = rs.getInt("id");
				List<Corner> corners = new LinkedList<Corner>();
				rs = db.getRoomsOfMap(mapID);
				while(rs.next()) {
					String[] roomCorners = rs.getString(1).replace(")", "").substring(9).split(",");
					List<Corner> cornerList = new LinkedList<Corner>();
					int i = 0;
					for(String c : roomCorners) {
						String[] coord = c.split(" ");
						Corner corner = new Corner(null,i,new Point(Double.parseDouble(coord[0]),Double.parseDouble(coord[1])));
						cornerList.add(corner);
						corners.add(corner);
						i++;
					}
					Room r = new Room(rs.getInt("label"),cornerList);
					model.addRoom(r);
				}
				rs = db.getConnectablesOfMap(mapID);
				while(rs.next()) {
					long id = rs.getLong("id");
					String type = rs.getString("type");
					String[] location = rs.getString(1).replace(")", "").substring(6).split(" ");
					if(type.equals("DOOR")) {
						Door d = new Door(model.getRoom(rs.getInt("room_label")),id,new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])));
						model.addDoor(d);						
					}else {
						Stairs s = new Stairs(model.getRoom(rs.getInt("room_label")),id,new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])));
						model.addStairs(s);
					}										
				}
				rs = db.getConnectionsOfConnectables(mapID);
				while(rs.next()) {
					Connectable c, c2;
					if(rs.getString("type_conn1").equals("DOOR")) {
						c = model.getDoor(rs.getLong("id_conn1"));
					}else {
						c = model.getStairs(rs.getLong("id_conn1"));
					}
					if(rs.getString("type_conn2").equals("DOOR")) {
						c2 = model.getDoor(rs.getLong("id_conn2"));
					}else {
						c2 = model.getStairs(rs.getLong("id_conn2"));
					}
					c.connectTo(c2);
					c2.connectTo(c);
				}
				rs = db.getItemsOfMap(mapID);
				while(rs.next()) {
					String[] location = rs.getString(1).replace(")", "").substring(6).split(" ");
					Item i = new Item(model.getRoom(rs.getInt("room_label")),rs.getLong("id"),new Point(Double.parseDouble(location[0]),Double.parseDouble(location[1])),rs.getString("url_image"));
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
				rs = db.getSeparatorsOfMap(mapID);
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
					RoomSeparator sep = new RoomSeparator(model.getRoom(rs.getInt("room_label")), rs.getLong("label"), findCorner(corners,xpoints[0],ypoints[0]), findCorner(corners,xpoints[1],ypoints[1]));
					model.addRoomSeparator(sep);										
				}							
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}
