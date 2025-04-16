package es.unizar.editor.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import es.unizar.access.DataAccessRoomFile;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.util.EditorLiterals;
import es.unizar.util.Literals;

public class CombineFloors extends JDialog {

	MapEditorView parent;
	MapEditorModel model;
	
	private File directory = new File(EditorLiterals.PATH_EDITOR);
	
	private static final String below = "BELOW";
	private static final String beside = "BESIDE";
	
	
	public CombineFloors(MapEditorView parent, MapEditorModel model) {
		super(MapEditorView.levelEditor, true);
		this.parent = parent;
		this.model = model;
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setTitle("COMBINE FLOORS");
		
		initComponents();
		
		this.pack();
		
		Rectangle parentBounds = MapEditorView.levelEditor.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new java.awt.Point(x, y));
	}

	private void initComponents() {
		
		fileChooserFirst = new JFileChooser();
		fileChooserFirst.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow choosing the directory (to save the 3 files)
		fileChooserFirst.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		fileChooserSecond = new JFileChooser();
		fileChooserSecond.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Only allow choosing the directory (to save the 3 files)
		fileChooserSecond.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		combineFloorPanel = new JPanel();
		getContentPane().add(combineFloorPanel);
		
		lblCombine = new JLabel("Combine ");
		combineFloorPanel.add(lblCombine);
		
		firstFloor = new JTextField();
		firstFloor.setFont(new Font("SansSerif", Font.PLAIN, 14));
		firstFloor.setColumns(10);
		firstFloor.setEditable(false);
		combineFloorPanel.add(firstFloor);
		
		firstFloorButton = new JButton("Select");
		firstFloorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fileChooserFirst.setCurrentDirectory(directory);
				fileChooserFirst.setDialogTitle("Select first floor to combine");
				
				switch(fileChooserFirst.showOpenDialog(combineFloorPanel)) {
				
					case JFileChooser.APPROVE_OPTION:
						first = fileChooserFirst.getSelectedFile();
						firstFloor.setText(first.getPath());
						
					default:
						break;
						
				}
			}
			
		});
		combineFloorPanel.add(firstFloorButton);
		
		lblWith = new JLabel("With ");
		combineFloorPanel.add(lblWith);
		
		secondFloor = new JTextField();
		secondFloor.setFont(new Font("SansSerif", Font.PLAIN, 14));
		secondFloor.setColumns(10);
		secondFloor.setEditable(false);
		combineFloorPanel.add(secondFloor);
		
		secondFloorButton = new JButton("Select");
		secondFloorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fileChooserSecond.setCurrentDirectory(directory);
				fileChooserSecond.setDialogTitle("Select second floor to combine");
				
				switch(fileChooserSecond.showOpenDialog(combineFloorPanel)) {
				
					case JFileChooser.APPROVE_OPTION:
						second = fileChooserSecond.getSelectedFile();
						secondFloor.setText(second.getPath());
						
					default:
						break;
						
				}
			}
			
		});
		combineFloorPanel.add(secondFloorButton);
		
		
		comboBoxCombinationType = new JComboBox<String>();
		comboBoxCombinationType.addItem(below);
		comboBoxCombinationType.addItem(beside);
		comboBoxCombinationType.setSelectedIndex(0); // Selected "BELOW" by default
		combineFloorPanel.add(comboBoxCombinationType);
		
		combineFloorsButton = new JButton("COMBINE FLOORS");
		combineFloorsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				combineFloors();
			}
		});
		combineFloorPanel.add(combineFloorsButton);
	}
	
	private JPanel combineFloorPanel;
	
	private JLabel lblCombine, lblWith;
	private JTextField firstFloor, secondFloor;
	private JButton firstFloorButton, secondFloorButton;
	private JComboBox<String> comboBoxCombinationType;
	private JButton combineFloorsButton;
	
	private File first, second;
	private JFileChooser fileChooserFirst, fileChooserSecond;
	
	/**
	 * Gets both files selected and combines their floors
	 */
	private void combineFloors() {
		
		// WARNING: THE CURRENT MAP WILL BE DELETED
		JOptionPane.showMessageDialog(combineFloorPanel,
				"The current map will not be saved. Please, ensure you have saved it before combining floors (including or not the current map being created)",
				"SAVE BEFORE COMBINING", JOptionPane.WARNING_MESSAGE);
		
		// Check correct files
		if (first.exists() && second.exists()) {
			
			// Create instances
			DataAccessRoomFile firstAccess = new DataAccessRoomFile(new File(first + File.separator + Literals.ROOM_FILE_NAME));
			DataAccessRoomFile secondAccess = new DataAccessRoomFile(new File(second + File.separator + Literals.ROOM_FILE_NAME));
			
			// Load all the properties
			firstAccess.loadProperties();
			secondAccess.loadProperties();
			
			// Check map settings are equal
			if (checkMapSettings(firstAccess, secondAccess)) {
				
				int xDisplacement = 0, yDisplacement = 0;
				int width = 0, height = 0;
				
				// Combine (load first, check combination type, load second with displacement)
				String type = (String) comboBoxCombinationType.getSelectedItem();
				
				switch (type) {
					case below:
						yDisplacement = firstAccess.getMapHeight();
						width = Math.max(firstAccess.getMapWidth(), secondAccess.getMapWidth()); // Max of the widths
						height = firstAccess.getMapHeight() + secondAccess.getMapHeight(); // Sum of the heights
						break;
					
					case beside:
						xDisplacement = firstAccess.getMapWidth();
						width = firstAccess.getMapWidth() + secondAccess.getMapWidth(); // Sum of the heights
						height = Math.max(firstAccess.getMapHeight(), secondAccess.getMapHeight()); // Max of the widths
						break;
						
					default:
						JOptionPane.showMessageDialog(combineFloorPanel,
								"ERROR IN JCOMBOBOX (NOT CORRECT OPTION)", "ERROR IN JCOMBOBOX (NOT CORRECT OPTION)", JOptionPane.ERROR_MESSAGE);
						break;
				}
				
				// Load first
				model.clearAll();
				model.setFiles(first.getAbsolutePath());
				model.load(0, 0);
				
				// Load second with displacement
				model.setFiles(second.getAbsolutePath());
				model.load(xDisplacement, yDisplacement);
				
				// Update map settings 
				parent.newOrOpenMap(width, height, firstAccess.getMapPixelRepresentsMeters());
				
				parent.refresh();
				
			}
			else {
				JOptionPane.showMessageDialog(combineFloorPanel,
						"The map settings from the selected directories are not compatible, please change one of them to fit: same pixel-meters proportion, same name.",
						"SETTINGS NOT COMPATIBLE", JOptionPane.ERROR_MESSAGE);
			}
		}
		else { // Files don't exist (or not selected in window/null)
			JOptionPane.showMessageDialog(combineFloorPanel,
					"Directories not selected or selected incorrect ones.",
					"NULL OR INCORRECT FILES SELECTED", JOptionPane.ERROR_MESSAGE);
		}
		
		
	}

	private boolean checkMapSettings(DataAccessRoomFile firstAccess, DataAccessRoomFile secondAccess) {
		// Check properties
		
		// Get map's names
		String firstName = firstAccess.getName();
		String secondName = secondAccess.getName();
		
		// Set map's width
		double firstPixelMeters = firstAccess.getMapPixelRepresentsMeters();
		double secondPixelMeters = secondAccess.getMapPixelRepresentsMeters();
		
		return firstName != null && firstPixelMeters > 0 && firstName.equals(secondName) && firstPixelMeters == secondPixelMeters;
	}
	
}
