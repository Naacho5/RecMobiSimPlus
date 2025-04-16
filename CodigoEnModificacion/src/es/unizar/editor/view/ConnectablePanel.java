package es.unizar.editor.view;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Stairs;

public class ConnectablePanel extends JPanel {
	
	
	ObjectPropertiesWindow parent;
	
	public ConnectablePanel(ObjectPropertiesWindow parent) {
		super();
		this.parent = parent;
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setBorder(BorderFactory.createTitledBorder("Connectable's attributes"));
		
		initComponents();
	}

	private void initComponents() {
		Connectable connectable = (Connectable) parent.drawable;
		
		// List of connected objects
		lblConnectedTo = new JLabel("Connected To: ");
		
		// Show connected 
		comboBoxConnectedTo = new JComboBox<Connectable>();
		comboBoxConnectedTo.setEditable(false);
		
		// Add/Connect to a new connectable
		lblAddConnected = new JLabel("Add: ");
		
		comboBoxAddConnected = new JComboBox<Connectable>();
		buttonAddConnected = new JButton("CONNECT");
		buttonAddConnected.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// Get selected item in comboBox
					Connectable con = (Connectable) comboBoxAddConnected.getSelectedItem();
					
					boolean added = false;
					// Id Door connect Door || Stairs connect Stairs
					if (connectable.getClass().equals(con.getClass())) {
						added = connectable.connectTo(con);
					}
					// If Door connect Stairs (Stairs connect Door can never happen)
					else if (!(connectable instanceof Stairs)){ // && !connectable.getClass().equals(con.getClass()) 
						added = connectable.addConnection(con);
					}
					
					if (added) {
						refreshConnectableComboBoxes();
					}
					else {
						System.out.println("NOT ADDED!");
					}
				}
				catch(Exception exception) {
					System.out.println(exception);
				}
			}
			
		});
		
		// Disconnect from connectable
		lblDeleteConnected = new JLabel("Disconnect: ");
		
		comboBoxDeleteConnected = new JComboBox<Connectable>();
		buttonDeleteConnected = new JButton("DISCONNECT");
		buttonDeleteConnected.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Get selected item in comboBox
				Connectable con = (Connectable) comboBoxDeleteConnected.getSelectedItem();
				
				// Disconnect from that item
				if (connectable.disconnectFrom(con)) {
					refreshConnectableComboBoxes();
				}
				else {
					System.out.println("NOT DELETED!");
				}
			}
			
		});
		
		setLayout();

		refreshConnectableComboBoxes();
	}
	
	private void setLayout() {
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblConnectedTo)
							.addGap(5)
							.addComponent(comboBoxConnectedTo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblAddConnected)
							.addGap(5)
							.addComponent(comboBoxAddConnected, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(5)
							.addComponent(buttonAddConnected))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblDeleteConnected)
							.addGap(5)
							.addComponent(comboBoxDeleteConnected, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(5)
							.addComponent(buttonDeleteConnected)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblConnectedTo)
						.addComponent(comboBoxConnectedTo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(20)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(buttonAddConnected)
						.addComponent(comboBoxAddConnected, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblAddConnected))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(comboBoxDeleteConnected, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(buttonDeleteConnected)
						.addComponent(lblDeleteConnected))
					.addContainerGap())
		);
		setLayout(groupLayout);
	}

	private void refreshConnectableComboBoxes() {
		
		Connectable connectable = (Connectable) parent.drawable;
		
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
		addConnections.addAll(parent.getModel().getStairs());
		if (connectable instanceof Door) {
			addConnections.addAll(parent.getModel().getDoors());
		}
		addConnections.removeAll(connectedToObjects);
		addConnections.remove(connectable); // Exclude itself
		
		// Fill AddConnected's items with the remaining Connectable objects
		for (Connectable c: addConnections) { // Add its possible connections
			comboBoxAddConnected.addItem(c); // Add its possible connections
		}
		
	}
	
	private JLabel lblConnectedTo;
	private JComboBox<Connectable> comboBoxConnectedTo;
	private JLabel lblAddConnected, lblDeleteConnected;
	private JComboBox<Connectable> comboBoxAddConnected, comboBoxDeleteConnected;
	private JButton buttonAddConnected, buttonDeleteConnected;
}
