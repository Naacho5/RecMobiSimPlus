package es.unizar.editor.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Point;
import es.unizar.editor.model.Room;

import javax.swing.JLabel;

public class AddItemsRandomPositions extends JDialog {
	
	JFrame parent;
	MapEditorModel model;
	
	public AddItemsRandomPositions(JFrame parent, MapEditorModel model) {
		super(parent, true);
		this.parent = parent;
		this.model = model;
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setTitle("ADD X ITEMS IN RANDOM POSITIONS");
		
		initComponents();
		
		this.pack();
		
		Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new java.awt.Point(x, y));
	}

	private void initComponents() {
		
		fullPanel = new JPanel();
		getContentPane().add(fullPanel);
		
		// ComboBox with all visitable objects
		comboBoxObjectTypes = new JComboBox<String>();
		for (Map.Entry<String, Properties> entry: model.getVisitableObjects().entrySet()) {
			comboBoxObjectTypes.addItem(entry.getKey());
		}
		
		lblAdd = new JLabel("Add ");
		fullPanel.add(lblAdd);
		
		numberOfItems = new JTextField();
		numberOfItems.setFont(new Font("SansSerif", Font.PLAIN, 14));
		numberOfItems.setColumns(3);
		fullPanel.add(numberOfItems);
		
		lblItemsOfType = new JLabel(" items of type ");
		fullPanel.add(lblItemsOfType);
		fullPanel.add(comboBoxObjectTypes);
		
		addItemsInRandomPositions = new JButton("ADD ITEMS");
		addItemsInRandomPositions.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addItemsRandPositions();
			}
			
			
		});
		fullPanel.add(addItemsInRandomPositions);
	}
	
	private JPanel fullPanel;
	
	private JComboBox<String> comboBoxObjectTypes;
	private JTextField numberOfItems;
	private JButton addItemsInRandomPositions;
	private JLabel lblAdd;
	private JLabel lblItemsOfType;
	
	private void addItemsRandPositions() {
		String keySelected = (String) comboBoxObjectTypes.getSelectedItem();
		
		if (model.getVisitableObjects().containsKey(keySelected)) {
			String numItems = numberOfItems.getText(); 
			if (numItems != null && !numItems.equals("")) {
				int num = 0;
				try {
					num = Integer.parseInt(numItems);
				}
				catch (Exception exception) {
					System.out.println(exception);
					JOptionPane.showMessageDialog(fullPanel,
							"The \"number of items\" textfield has an incorrect value. Please, fill with an integer (number) value",
							"NUMBER OF ITEMS VALUE NOT CORRECT", JOptionPane.WARNING_MESSAGE);
				}
				
				int numRooms = model.getNumRooms();
				
				for (int i = 0; i < num; i++) {
					
					Random random = new Random();
					int room = random.nextInt(numRooms);
					Room r = model.getRooms().get(room);
					
					Point point = r.getRandomPointInRoom();
					System.out.println(point+"\n");
					
					// Create corner and add it to model
					Item itemVisitable = new Item(r, model.getNumItems()+1, point);
					// Set specific item's properties (visitableItem's attributes).
					model.setItemVisitableProperties(keySelected, itemVisitable);
						
					if (!model.addItem(itemVisitable)) {
						JOptionPane.showMessageDialog(fullPanel, "The item " + itemVisitable + " wasn't correctly added to map", "COULDN'T ADD ITEM TO MAP", JOptionPane.ERROR_MESSAGE);
						break;
					}
				}
				this.dispose();
			}
			else {
				JOptionPane.showMessageDialog(fullPanel,
						"The \"number of items\" textfield wasn't filled",
						"NUMBER OF ITEMS EMPTY", JOptionPane.WARNING_MESSAGE);
			}
		}
		else {
			// Shouldn't happen
			JOptionPane.showMessageDialog(fullPanel,
					"The selected visitable object is not correct",
					"NULL OR INCORRECT VISITABLE OBJECT", JOptionPane.ERROR_MESSAGE);
		}
	}

}
