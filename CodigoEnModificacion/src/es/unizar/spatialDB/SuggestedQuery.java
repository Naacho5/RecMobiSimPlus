package es.unizar.spatialDB;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import es.unizar.gui.MainSimulator;
import es.unizar.gui.SimulationQueryMaker;

public class SuggestedQuery extends JDialog {

	private static final long serialVersionUID = 1L;
	
	DBQueryMaker dbqm = null;
	SimulationQueryMaker sqm = null;
	String queryDesc;
	String idsOrNames;
	String query;

	public SuggestedQuery(JFrame parent, DBQueryMaker dbqm, String queryDesc, String query) {
		super(parent, true);
		this.dbqm = dbqm;
		this.queryDesc = queryDesc;
		this.query = query;
		initComponents();
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
	}
	
	public SuggestedQuery(JFrame parent, SimulationQueryMaker sqm, String queryDesc, String query) {
		super(parent, true);
		this.sqm = sqm;
		this.queryDesc = queryDesc;
		this.query = query;
		initComponents();
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
	}
	
	private void initComponents() {

		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
//		JComboBox<Integer> mapIds = new JComboBox<Integer>(MainSimulator.db.getMapIds().toArray(new Integer[0]));		
		JComboBox<String> mapNames = new JComboBox<String>(MainSimulator.db.getMapNames().toArray(new String[0]));
		DefaultComboBoxModel<Integer> model = new DefaultComboBoxModel<Integer>();
		JComboBox<Integer> roomLabels = new JComboBox<Integer>(model);
		
		List<String> simList = MainSimulator.db.getSimulations();
		List<String> simNumList = MainSimulator.db.getSimulations();
		for(int i = 0; i < simList.size(); i++) {
			simList.set(i,simList.get(i).substring(simList.get(i).indexOf("Simulation:")));
			simNumList.set(i,simNumList.get(i).substring(0,simNumList.get(i).indexOf("Simulation:")));
		}
		JComboBox<String> simulations = new JComboBox<String>(simList.toArray(new String[0]));
//		mapIds.addActionListener(new ActionListener() {		
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				try {
//					model.removeAllElements();					
//					model.addAll(MainSimulator.db.getRoomLabelsOfMap((int)mapIds.getSelectedItem()));										
//				} catch (SQLException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			}
//		});
		
		mapNames.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if(queryDesc.equals("Area of room") || queryDesc.equals("Items of room")) {
					try {
						model.removeAllElements();					
						model.addAll(MainSimulator.db.getRoomLabelsOfMap(mapNames.getSelectedItem().toString()));
						if(model.getSize() > 0)	roomLabels.setSelectedIndex(0);
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		
		if(queryDesc.equals("Area of room") || queryDesc.equals("Items of room")) {
			try {
				model.removeAllElements();					
				model.addAll(MainSimulator.db.getRoomLabelsOfMap(mapNames.getSelectedItem().toString()));
				if(model.getSize() > 0)	roomLabels.setSelectedIndex(0);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		JPanel mapPanel = new JPanel(new GridLayout(1,2));
		mapPanel.add(new JLabel("Select map"));
		mapPanel.add(mapNames);
		
		JPanel roomPanel = new JPanel(new GridLayout(1,2));
		roomPanel.add(new JLabel("Select room"));
		roomPanel.add(roomLabels);
		
		JButton setQuery = new JButton("Set query");
		setQuery.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if(dbqm != null) {
					query = query.replaceFirst("__", "'"+mapNames.getSelectedItem().toString()+"'");
					if((queryDesc.equals("Area of room") || queryDesc.equals("Items of room")) && roomLabels.getSelectedItem() != null) query = query.replaceFirst("__", roomLabels.getSelectedItem().toString());	
					dbqm.queryTextArea.setText(query);
					dbqm.updateControlPanelAfterSuggestedQuery(queryDesc);
				}else {
					query = query.replace("__", simNumList.get(simulations.getSelectedIndex()));
					sqm.queryTextArea.setText(query);
					sqm.updateControlPanelAfterSuggestedQuery(queryDesc);
				}
				dispose();
			}
		});
				
		switch(queryDesc) {
			case "Area of room":
			case "Items of room":
				add(mapPanel);
				add(roomPanel);
				add(setQuery);
				break;
			case "Total area":
			case "Average area of room":
			case "Total items":
			case "Average number of items per room":
				add(mapPanel);
				add(setQuery);
				break;
			default:
				JPanel simPanel = new JPanel(new GridLayout(1,2));
				simPanel.add(new JLabel("Select simulation"));
				simPanel.add(simulations);
				add(simPanel);
				add(setQuery);
				break;
		}
	}
	
}
