package es.unizar.spatialDB;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window.Type;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.Stairs;
import es.unizar.editor.view.ConnectablePanel;
import es.unizar.editor.view.EditVisitableItemView;
import es.unizar.editor.view.MapPanelListener;
import es.unizar.util.EditorLiterals;

public class ObjectPropertiesWindowDB extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DBViewerModel model;
	
	Drawable drawable; // Object with its attributes
	String objectType; // Object's class name
	
	// List<Long> labels; // List of labels of the rest of elements -> IN CASE YOU WANT LABELS TO BE MODIFIED BY USER
	Point originalPosition;

	public ObjectPropertiesWindowDB(DBViewerModel model, Drawable drawable) {
		super();
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
			numRoomField = new JTextField(Long.toString(drawable.getVertex_label()));
			numRoomField.setEditable(false);
		}
		else {
			lblRoom = new JLabel();			
			lblRoom.setEnabled(false);
			lblRoom.setVisible(false);
		}
		
		// Object's vertex_label
		lblVertexLabel = new JLabel("Label: " + Long.toString(drawable.getVertex_label()));
		
		// Object's position (vertex_xy) -> Pixels
		lblPositionPixel = new JLabel("Position in pixels:");
		
		lblPositionPixelX = new JLabel("x (pixels):");
		
		textFieldPositionPixelX = new JTextField(Double.toString(drawable.getVertex_xy().getX()));
		textFieldPositionPixelX.setName(EditorLiterals.POSITION_PIXEL_X);
		textFieldPositionPixelX.setColumns(5);
		textFieldPositionPixelX.setEditable(false);
		
		lblPositionPixelY = new JLabel("y (pixels):");
		
		textFieldPositionPixelY = new JTextField(Double.toString(drawable.getVertex_xy().getY()));
		textFieldPositionPixelY.setName(EditorLiterals.POSITION_PIXEL_Y);
		textFieldPositionPixelY.setColumns(5);
		textFieldPositionPixelY.setEditable(false);
		
		// Object's position (vertex_xy) -> Meters
		lblPositionMeters = new JLabel("Position in meters:");
		
		lblPositionMetersX = new JLabel("x (meters):");
		
		textFieldPositionMetersX = new JTextField(Double.toString(drawable.getVertex_xy_meters(model.getPixelRepresentsInMeters()).getX()));
		textFieldPositionMetersX.setName(EditorLiterals.POSITION_METERS_X);
		textFieldPositionMetersX.setColumns(5);
		textFieldPositionMetersX.setEditable(false);
		
		lblPositionMetersY = new JLabel("y (meters):");
		
		textFieldPositionMetersY = new JTextField(Double.toString(drawable.getVertex_xy_meters(model.getPixelRepresentsInMeters()).getY()));
		textFieldPositionMetersY.setName(EditorLiterals.POSITION_METERS_Y);
		textFieldPositionMetersY.setColumns(5);
		textFieldPositionMetersY.setEditable(false);
		
		// Set the layout for all elements
		setObjectsAttributesLayout();
		
		objectTypePanel = new JPanel();
		
		// ConnectedTo -> Doors and Stairs
//		if (objectType.equals(Door.class.getSimpleName()) || objectType.equals(Stairs.class.getSimpleName())) {
		if (drawable instanceof Door || drawable instanceof Stairs) {
			objectTypePanel.setLayout(new BoxLayout(objectTypePanel, BoxLayout.X_AXIS));
			Connectable connectable = (Connectable)drawable;
			lblConnectedTo = new JLabel("Connected To: ");
			objectTypePanel.add(lblConnectedTo);
			
			comboBoxConnectedTo = new JComboBox<Connectable>();
			comboBoxConnectedTo.setEditable(false);
			
			List<Connectable> connectedToObjects = new LinkedList<Connectable>();
			connectedToObjects.addAll(connectable.getConnectedTo());
			for (Connectable c: connectedToObjects) {
				comboBoxConnectedTo.addItem(c); // Add connected objects
			}
			
			objectTypePanel.add(comboBoxConnectedTo);
		}
		else if (objectType.equals(Item.class.getSimpleName())) {
			
			Item i = (Item) drawable;
			
			objectTypePanel = new EditVisitableItemView(i, i.getItemLabel(),false);
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
							.addComponent(numRoomField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
								.addComponent(numRoomField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
	
	public DBViewerModel getModel() {
		return model;
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
	private JTextField numRoomField;
	private JLabel lblVertexLabel;
	//private JTextField textFieldVertexLabel;
	private JLabel lblPositionPixel, lblPositionPixelX, lblPositionPixelY;
	private JTextField textFieldPositionPixelX, textFieldPositionPixelY;
	private JLabel lblPositionMeters, lblPositionMetersX, lblPositionMetersY;
	private JTextField textFieldPositionMetersX, textFieldPositionMetersY;
	private JLabel lblConnectedTo;
	private JComboBox<Connectable> comboBoxConnectedTo;
	
	private JPanel objectTypePanel;
	
	
}
