package es.unizar.editor.view;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.Stairs;
import es.unizar.util.EditorLiterals;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class ObjectPropertiesWindow extends JDialog{
	
	private MapPanel parent;
	private MapPanelListener listener;
	private MapEditorModel model;
	
	Drawable drawable; // Object with its attributes
	String objectType; // Object's class name
	
	// List<Long> labels; // List of labels of the rest of elements -> IN CASE YOU WANT LABELS TO BE MODIFIED BY USER
	Point originalPosition;
	
	public ObjectPropertiesWindow(MapPanel panel, Drawable drawable, MapEditorModel model) {
		super();
		this.parent = panel;
		this.model = model;
		
		this.drawable = drawable;
		// Get the name of the class of the item
		objectType = drawable.getClass().getSimpleName();
		
		// labels = loadLabels(); -> IN CASE YOU WANT LABELS TO BE MODIFIED BY USER
		originalPosition = drawable.getVertex_xy();
		
		// Window settings
		getContentPane().setFont(new Font("SansSerif", Font.PLAIN, 14));
		setType(Type.UTILITY); // Type utility
		setTitle("Attributes: " + objectType);
		initComponents();
		pack();
		
		// Center in the parent
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();;
        
        int x = Math.max(0, (screenSize.width - this.getWidth()) / 2);
        int y = Math.max(0, (screenSize.height - this.getHeight()) / 2);
        setLocation(new java.awt.Point(x, y));
	}

	private void initComponents() {
		
		fullPanel = new JPanel();
		getContentPane().add(fullPanel);

		// Create object's attributes panel
		drawableAttributes = new JPanel();
		drawableAttributes.setBorder(BorderFactory.createTitledBorder("Object's attributes"));
		
		// Type of object -> Just info, not editable nor enabled in any case
		lblTypeOfObject = new JLabel("Type of object: " + objectType);
		
		lblTypeOfObjectIcon = new JLabel(new ImageIcon(new ImageIcon(drawable.getUrlIcon()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)));
		
		// Object's room (if not stairs selected)
		if (!objectType.equals(Stairs.class.getSimpleName())) {
			lblRoom = new JLabel("Room: ");
			
			comboBoxRooms = new JComboBox<Room>();
		
			for (Room r: model.getRooms()) { // Add all existing rooms
				comboBoxRooms.addItem(r);
			}
			comboBoxRooms.setRenderer(new ComboBoxClass());
			
			if (drawable.getRoom() != null) { // Set selected if not null
				comboBoxRooms.setSelectedItem(drawable.getRoom());
			}
			
			comboBoxRooms.setEnabled(objectType.equals(Door.class.getSimpleName()));
			
			comboBoxRooms.addActionListener(changeRoom);
		
		}
		else {
			lblRoom = new JLabel();
			comboBoxRooms = new JComboBox<Room>();
			
			lblRoom.setEnabled(false);
			lblRoom.setVisible(false);
			
			comboBoxRooms.setEnabled(false);
			comboBoxRooms.setVisible(false);
		}
		
		// Object's vertex_label
		lblVertexLabel = new JLabel("Label: " + Long.toString(drawable.getVertex_label()));
		
		// Object's position (vertex_xy) -> Pixels
		lblPositionPixel = new JLabel("Position in pixels:");
		
		lblPositionPixelX = new JLabel("x (pixels):");
		
		textFieldPositionPixelX = new JTextField(Double.toString(drawable.getVertex_xy().getX()));
		textFieldPositionPixelX.setName(EditorLiterals.POSITION_PIXEL_X);
		textFieldPositionPixelX.setColumns(5);
		textFieldPositionPixelX.addActionListener(changePositions);
		
		lblPositionPixelY = new JLabel("y (pixels):");
		
		textFieldPositionPixelY = new JTextField(Double.toString(drawable.getVertex_xy().getY()));
		textFieldPositionPixelY.setName(EditorLiterals.POSITION_PIXEL_Y);
		textFieldPositionPixelY.setColumns(5);
		textFieldPositionPixelY.addActionListener(changePositions);
		
		// Object's position (vertex_xy) -> Meters
		lblPositionMeters = new JLabel("Position in meters:");
		
		lblPositionMetersX = new JLabel("x (meters):");
		
		textFieldPositionMetersX = new JTextField(Double.toString(drawable.getVertex_xy_meters(model.getPixelRepresentsInMeters()).getX()));
		textFieldPositionMetersX.setName(EditorLiterals.POSITION_METERS_X);
		textFieldPositionMetersX.setColumns(5);
		textFieldPositionMetersX.addActionListener(changePositionsMeters);
		
		lblPositionMetersY = new JLabel("y (meters):");
		
		textFieldPositionMetersY = new JTextField(Double.toString(drawable.getVertex_xy_meters(model.getPixelRepresentsInMeters()).getY()));
		textFieldPositionMetersY.setName(EditorLiterals.POSITION_METERS_Y);
		textFieldPositionMetersY.setColumns(5);
		textFieldPositionMetersY.addActionListener(changePositionsMeters);
		
		// Set the layout for all elements
		setObjectsAttributesLayout();
		
		objectTypePanel = new JPanel();
		
		// ConnectedTo -> Doors and Stairs
		if (objectType.equals(Door.class.getSimpleName()) || objectType.equals(Stairs.class.getSimpleName())) {
			
			objectTypePanel = new ConnectablePanel(this);
			
		}
		else if (objectType.equals(Item.class.getSimpleName())) {
			
			Item i = (Item) drawable;
			
			objectTypePanel = new EditVisitableItemView(i, i.getItemLabel(),true);
		}
		
		
		
		setLayout();
	}
	
	/**
	 * Set layout for the 3 main panels.
	 */
	private void setLayout() {
		GroupLayout gl_fullPanel = new GroupLayout(fullPanel);
		gl_fullPanel.setHorizontalGroup(
			gl_fullPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_fullPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_fullPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(drawableAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(objectTypePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		gl_fullPanel.setVerticalGroup(
			gl_fullPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, gl_fullPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(drawableAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(objectTypePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		fullPanel.setLayout(gl_fullPanel);
	}
	
	/**
	 * Adds all elements in "Object's Attributes" and places them in their correct positions.
	 */
	private void setObjectsAttributesLayout() {
		GroupLayout gl_drawableAttributes = new GroupLayout(drawableAttributes);
		gl_drawableAttributes.setHorizontalGroup(
			gl_drawableAttributes.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_drawableAttributes.createSequentialGroup()
					.addGap(5)
					.addGroup(gl_drawableAttributes.createParallelGroup(Alignment.LEADING)
						.addComponent(lblPositionPixel)
						.addGroup(gl_drawableAttributes.createSequentialGroup()
								.addComponent(lblPositionPixelX)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(textFieldPositionPixelX, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addGap(18)
								.addComponent(lblPositionPixelY)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(textFieldPositionPixelY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addComponent(lblPositionMeters)
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addComponent(lblPositionMetersX)
							.addGap(5)
							.addComponent(textFieldPositionMetersX, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(5)
							.addComponent(lblPositionMetersY)
							.addGap(5)
							.addComponent(textFieldPositionMetersY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addComponent(lblTypeOfObject)
							.addGap(5)
							.addComponent(lblTypeOfObjectIcon)
							.addGap(5)
							.addComponent(lblRoom)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboBoxRooms, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblVertexLabel))
						)
					.addContainerGap())
		);
		gl_drawableAttributes.setVerticalGroup(
			gl_drawableAttributes.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_drawableAttributes.createSequentialGroup()
					.addGroup(gl_drawableAttributes.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addGap(8)
							.addGroup(gl_drawableAttributes.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblTypeOfObject)
								.addComponent(lblRoom)
								.addComponent(comboBoxRooms, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblVertexLabel)))
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addGap(15)
							.addComponent(lblTypeOfObjectIcon)))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblPositionPixel)
					.addPreferredGap(ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
					.addGroup(gl_drawableAttributes.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblPositionPixelX)
						.addComponent(textFieldPositionPixelX, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPositionPixelY)
						.addComponent(textFieldPositionPixelY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblPositionMeters)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_drawableAttributes.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addGap(3)
							.addComponent(lblPositionMetersX))
						.addComponent(textFieldPositionMetersX, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addGroup(gl_drawableAttributes.createSequentialGroup()
							.addGap(3)
							.addComponent(lblPositionMetersY))
						.addComponent(textFieldPositionMetersY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				)
		);
		drawableAttributes.setLayout(gl_drawableAttributes);
	}

	/**
	 * Adds a MapPanel Listener (with map panel ops).
	 * 
	 * @param listener
	 */
	public void addMapPanelListener(MapPanelListener listener) {
		this.listener = listener;
	}
	
	public MapEditorModel getModel() {
		return model;
	}
	
	/**
	 * @return List of labels from the rest of the drawables.
	 */
	/*private List<Long> loadLabels() {
		List<Long> list = new LinkedList<Long>();
		
		for (Drawable d: model.getPaintedElements()) {
			if (d != drawable) {
				list.add(d.getVertex_label());
			}
		}
		
		return list;
	}*/

	/**
	 * Update text field's texts with the drawable's new position
	 */
	private void updatePositionTextFields(Point pointPixels, Point pointMeters) {
		
		// Refresh textfields
		textFieldPositionPixelX.setText(Double.toString(pointPixels.getX()));
		textFieldPositionPixelY.setText(Double.toString(pointPixels.getY()));
		
		textFieldPositionMetersX.setText(Double.toString(pointMeters.getX()));
		textFieldPositionMetersY.setText(Double.toString(pointMeters.getY()));
	}
	
	private void refreshConnectableComboBoxes() {
		
		Connectable connectable = (Connectable) drawable;
		
		// Remove all elements
		comboBoxConnectedTo.removeAllItems();;
		comboBoxAddConnected.removeAllItems();
		comboBoxDeleteConnected.removeAllItems();
		
		// Create list of connected objects
		List<Connectable> connectedToObjects = new LinkedList<Connectable>();
		connectedToObjects.addAll(connectable.getConnectedTo());
		
		// Fill ConnectedTo and DeleteConnected's items
		for (Connectable c: connectedToObjects) {
			comboBoxConnectedTo.addItem(c); // Add connected objects
			comboBoxDeleteConnected.addItem(c); // Add removable objects
		}
		
		// Create list of objects that can be added
		List<Connectable> addConnections = new LinkedList<Connectable>();
		addConnections.addAll(model.getStairs());
		if (connectable instanceof Door) {
			addConnections.addAll(model.getDoors());
		}
		addConnections.removeAll(connectedToObjects);
		addConnections.remove(connectable); // Exclude itself
		
		// Fill AddConnected's items with the remaining Connectable objects
		for (Connectable c: addConnections) { // Add its possible connections
			comboBoxAddConnected.addItem(c); // Add its possible connections
		}
		
	}
	
	
	
	/**
	 * ALL ELEMENTS IN WINDOW
	 */
	private JPanel fullPanel;

	private JPanel drawableAttributes;
	
	private JLabel lblTypeOfObject;
	private JLabel lblTypeOfObjectIcon;
	//private JTextField textFieldTypeOfObject;
	private JLabel lblRoom;
	private JComboBox<Room> comboBoxRooms;
	private JLabel lblVertexLabel;
	//private JTextField textFieldVertexLabel;
	private JLabel lblPositionPixel, lblPositionPixelX, lblPositionPixelY;
	private JTextField textFieldPositionPixelX, textFieldPositionPixelY;
	private JLabel lblPositionMeters, lblPositionMetersX, lblPositionMetersY;
	private JTextField textFieldPositionMetersX, textFieldPositionMetersY;
	//private JLabel lblConnectedTo;
	private JComboBox<Connectable> comboBoxConnectedTo;
	//private JLabel lblAddConnected, lblDeleteConnected;
	private JComboBox<Connectable> comboBoxAddConnected, comboBoxDeleteConnected;
	//private JButton buttonAddConnected, buttonDeleteConnected;
	
	private JPanel objectTypePanel;
	
	
	
	/**
	 * 
	 * JComboBox's renderer.
	 * 
	 * Inspiration taken from: http://www.java2s.com/Code/Java/Swing-JFC/CustomComboBoxwithImage.htm
	 *
	 */
	class ComboBoxClass extends JLabel implements ListCellRenderer<Room> {
		
		public ComboBoxClass() {
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Room> list, Room value, int index,
				boolean isSelected, boolean cellHasFocus) {
			
			//Get the selected index. (The index param isn't
            //always valid, so just use the value.)
            int selectedIndex = model.getRooms().indexOf(value);
            
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            setText("Room " + selectedIndex);
            
			return this;
		}
		
	}
	
	Action changeRoom = new AbstractAction() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (objectType.equals(Door.class.getSimpleName())) {
					Door d = (Door) drawable;
					Room r = (Room) comboBoxRooms.getSelectedItem();
					
					model.updateDoorRoom(d, r);
				}
        	}
        	catch (Exception exception) {
        		System.out.println(exception);
        	}
			
		}
	};
	
	Action changePositions = new AbstractAction() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			changePositions();
		}
	};
	
	public void changePositions() {
		try {
			// Set old coordinates
			model.setOldObjectCoordinates(drawable.getVertex_xy());
			
			// Get new point
    		double newX = Double.valueOf(textFieldPositionPixelX.getText());
    		double newY = Double.valueOf(textFieldPositionPixelY.getText());
    		Point p = new Point(newX, newY);
    		
    		model.diffPoint = new Point(0, 0); // Reset diffPoint because we are not moving by clicking some part of the icon
    		
    		// Move and validate movement
    		listener.move(p);
			listener.validateMovement();
			
			updatePositionTextFields(drawable.getVertex_xy(), drawable.getVertex_xy_meters(model.getPixelRepresentsInMeters()));
			
			this.dispose();
    	}
    	catch (Exception exception) {
    		System.out.println(exception);
    	}
	}
	
	Action changePositionsMeters = new AbstractAction() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			changePositionsMeters();
		}
	};
	
	public void changePositionsMeters() {
		try {
			// Get new point
    		double newX = Double.valueOf(textFieldPositionMetersX.getText());
    		double newY = Double.valueOf(textFieldPositionMetersY.getText());
    		Point pointInMeters = new Point(newX, newY);
    		Point pointInPixels = new Point(newX/model.getPixelRepresentsInMeters(), newY/model.getPixelRepresentsInMeters());
    		
    		// Update positions -> Movement will be checked in pixel ActionListener
    		updatePositionTextFields(pointInPixels, pointInMeters);
    		
    		textFieldPositionPixelX.postActionEvent();
			
    		this.dispose();
    	}
    	catch (Exception exception) {
    		System.out.println(exception);
    	}
	}
}
