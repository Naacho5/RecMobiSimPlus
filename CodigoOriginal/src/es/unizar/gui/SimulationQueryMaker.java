package es.unizar.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import es.unizar.spatialDB.DBQueryMaker;
import es.unizar.spatialDB.SuggestedQuery;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

public class SimulationQueryMaker {
	
	public static JFrame simQueryMaker;
	
	JPanel controlPanelContainer;
	JPanel queryContainer;
	public JTextArea queryTextArea;
	
	JTable resultTable;
	DefaultTableModel tableModel;
	JScrollPane resultScroll;
	
	JPanel buttons;
	JPanel attributeButtons;
	
	List<String> selectedTables;
	
	JScrollPane listScroller,listScroller2;
	JPanel wherePanel,wherePanel2,wherePanel3;
	JTextField whereText,whereText2,whereText3;
	DefaultComboBoxModel<String> CB1model,CB2model,CB3model;
	JButton addButton,addButton2,addButton3;
	
	public SimulationQueryMaker() {
		initialize();
		simQueryMaker.pack();
		simQueryMaker.setVisible(true);
	}
	
	private void initialize() {
			
		simQueryMaker = new JFrame();
		simQueryMaker.getContentPane().setLayout(new GridBagLayout());
		
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
				simQueryMaker.dispose();
				MainSimulator.splitFrame.dispose();
				MainSimulator.splitFrame = null;
			}
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.4;
		gbc.weighty = 0.05;
		gbc.fill = GridBagConstraints.BOTH;
		simQueryMaker.getContentPane().add(close, gbc);
		
		controlPanelContainer = new JPanel();
		controlPanelContainer.setBorder(BorderFactory.createTitledBorder("Control panel"));
		controlPanelContainer.setPreferredSize(new Dimension(4*dim.width/10, dim.height));
		controlPanelContainer.setLayout(new BoxLayout(controlPanelContainer, BoxLayout.Y_AXIS));
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.4;
		gbc.weighty = 0.95;
		gbc.fill = GridBagConstraints.BOTH;
		simQueryMaker.getContentPane().add(controlPanelContainer, gbc);
		
		queryContainer = new JPanel(new GridBagLayout());
		queryContainer.setBorder(BorderFactory.createTitledBorder("Query area"));
		queryContainer.setPreferredSize(new Dimension(6*dim.width/10, dim.height));
		
		gbc = new GridBagConstraints();
	    gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.6;
		gbc.weighty = 1.0;
		gbc.gridheight = 2;
		gbc.fill = GridBagConstraints.BOTH;
		simQueryMaker.getContentPane().add(queryContainer, gbc);
		
		queryTextArea = new JTextArea(50,50);
		JScrollPane scroll = new JScrollPane(queryTextArea);
		
		queryTextArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(queryTextArea.getText().length() == 0) {
					for(Component c : buttons.getComponents()) {
						JButton b = (JButton)c;
						b.setEnabled(true);
					}
				}else {
					for(Component c : buttons.getComponents()) {
						JButton b = (JButton)c;
						b.setEnabled(false);
					}
					addButton.setEnabled(false);
					addButton2.setEnabled(false);
					addButton3.setEnabled(false);
					selectedTables.clear();
				}
				loadAttributeButtons();
			}
	
			@Override
			public void keyPressed(KeyEvent e) {
			}
	
			@Override
			public void keyReleased(KeyEvent e) {
				if(queryTextArea.getText().length() == 0) {
					for(Component c : buttons.getComponents()) {
						JButton b = (JButton)c;
						b.setEnabled(true);
					}
				}else {
					for(Component c : buttons.getComponents()) {
						JButton b = (JButton)c;
						b.setEnabled(false);
					}
					addButton.setEnabled(false);
					addButton2.setEnabled(false);
					addButton3.setEnabled(false);
					selectedTables.clear();
				}
				loadAttributeButtons();
			}			
		});
		
		gbc = new GridBagConstraints();
	    gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.6;
		gbc.weighty = 0.4;
		gbc.fill = GridBagConstraints.BOTH;
		queryContainer.add(scroll,gbc);
		
		tableModel = new DefaultTableModel();
		resultTable = new JTable(tableModel);
		resultScroll = new JScrollPane(resultTable);
		resultScroll.setBorder(BorderFactory.createTitledBorder("Query result"));
				
		gbc = new GridBagConstraints();
	    gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 0.6;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		queryContainer.add(resultScroll,gbc);
		
		JButton runQuery = new JButton("Run query");
		runQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					ResultSet rs = MainSimulator.db.runQuery(queryTextArea.getText());
					ResultSetMetaData rsmd = rs.getMetaData();
					tableModel.setRowCount(0);
					tableModel.setColumnCount(0);
					for(int i = 1; i <= rsmd.getColumnCount(); i++) {
						tableModel.addColumn(rsmd.getColumnName(i));					
					}
					while(rs.next()){
						String[] resultsRow = new String[rsmd.getColumnCount()];
						for(int i = 1; i <= rsmd.getColumnCount(); i++) {
							resultsRow[i-1] = rs.getString(i);
						}
						tableModel.addRow(resultsRow);					
					}
					resultTable.repaint();
					resultTable.revalidate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
	//					e.printStackTrace();
					JOptionPane.showMessageDialog(simQueryMaker, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		
		JButton clearQuery = new JButton("Clear query");
		clearQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				queryTextArea.setText("");
				for(Component c : buttons.getComponents()) {
					JButton b = (JButton)c;
					b.setEnabled(true);
				}
				addButton.setEnabled(false);
				addButton2.setEnabled(false);
				addButton3.setEnabled(false);
				selectedTables.clear();
				loadAttributeButtons();
				whereText.setText("");
				whereText2.setText("");
				whereText3.setText("");
				tableModel.setRowCount(0);
				tableModel.setColumnCount(0);
				resultTable.repaint();
				resultTable.revalidate();
			}
		});
		
		
		JPanel buttons2 = new JPanel();
	    buttons2.setBorder(BorderFactory.createEmptyBorder());
	    buttons2.setLayout(new GridLayout(1,2));
	    buttons2.add(runQuery);
	    buttons2.add(clearQuery);
		
		gbc = new GridBagConstraints();
	    gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0.6;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
		queryContainer.add(buttons2,gbc);
				
		initControlPanel();
							
		}
		
private void initControlPanel() {
		
		try {
			List<String> tableNames = MainSimulator.db.getNamesOfSimulationTables();
			selectedTables = new ArrayList<String>();
			buttons = new JPanel();
		    buttons.setBorder(BorderFactory.createEmptyBorder());
		    buttons.setLayout(new GridLayout(tableNames.size()/2,2));
		    for(String table : tableNames) {
		    	JButton addTable = new JButton(table);
		    	addTable.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						addButton.setEnabled(true);
						addButton2.setEnabled(true);
						addButton3.setEnabled(true);
						if(!selectedTables.contains(table)) selectedTables.add(table);
						if(queryTextArea.getText().length() == 0) {
							queryTextArea.setText("select *"+System.lineSeparator()+"from "+table+";");
						}else {
							String text = queryTextArea.getText().toLowerCase();
							int idx = text.indexOf(System.lineSeparator()+"from ");
							int idxWhere = text.indexOf("where");
							if(idxWhere == -1) {
								if(text.charAt(text.length()-1) == ' ') {
									if(text.indexOf(";") == -1) {
										queryTextArea.setText(text+table);
									}else {
										queryTextArea.setText(text.substring(0,text.length()-1)+table+";");
									}
								}else {
									if(text.indexOf(";") == -1) {
										queryTextArea.setText(text+","+table);
									}else {
										queryTextArea.setText(text.substring(0,text.length()-1)+","+table+";");
									}
								}
							}else {
								queryTextArea.setText(text.substring(0,idxWhere-1)+","+table+text.substring(idxWhere-1));								
							}							
						}
						loadAttributeButtons();
					}
				});
		    	buttons.add(addTable);
		    }
			
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			listScroller = new JScrollPane(buttons);
						
			JPanel tableList = new JPanel();
			tableList.setBorder(BorderFactory.createTitledBorder("Tables"));
			tableList.add(listScroller);
			tableList.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
			tableList.setLayout(new GridLayout(1,1));
			controlPanelContainer.add(tableList);
			
			attributeButtons = new JPanel();
			attributeButtons.setBorder(BorderFactory.createEmptyBorder());
			
			listScroller2 = new JScrollPane(attributeButtons);
			
			JPanel columnList = new JPanel();
			columnList.setBorder(BorderFactory.createTitledBorder("Columns"));
			columnList.add(listScroller2);
			columnList.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
			columnList.setLayout(new GridLayout(1,1));
			controlPanelContainer.add(columnList);
			
			wherePanel = new JPanel();
			wherePanel.setBorder(BorderFactory.createTitledBorder("Where conditions"));
			wherePanel.setLayout(new GridLayout(3,4));
			wherePanel.setPreferredSize(new Dimension(3*dim.width/10, 2*dim.height/10));
			
			CB1model = new DefaultComboBoxModel<String>();
			JComboBox<String> columnSelection = new JComboBox<String>(CB1model);
			wherePanel.add(columnSelection);
			
			JComboBox<String> compareSymbols = new JComboBox<String>(new String[] {"=","<>","<","<=",">",">="});
			wherePanel.add(compareSymbols);
			
			CB2model = new DefaultComboBoxModel<String>();
			JComboBox<String> columnSelection2 = new JComboBox<String>(CB2model);
			wherePanel.add(columnSelection2);
						
			addButton = new JButton("Add");
			addButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String text = queryTextArea.getText();
					int idxWhere = (text.indexOf("where") == -1) ? text.indexOf("WHERE") : text.indexOf("where");
					String[] attribute1 = ((String)columnSelection.getSelectedItem()).split(" ");
					String[] attribute2 = ((String)columnSelection2.getSelectedItem()).split(" ");
					if(idxWhere == -1) {
						queryTextArea.setText(text.replace(";","")+System.lineSeparator()+"where "+attribute1[2].substring(1,attribute1[2].length()-1)+"."+attribute1[1]+" "+compareSymbols.getSelectedItem()+" "+attribute2[2].substring(1,attribute2[2].length()-1)+"."+attribute2[1]+";");
					}else {
						queryTextArea.setText(text.replace(";","")+" and "+attribute1[2].substring(1,attribute1[2].length()-1)+"."+attribute1[1]+" "+compareSymbols.getSelectedItem()+" "+attribute2[2].substring(1,attribute2[2].length()-1)+"."+attribute2[1]+";");
					}
				}				
			});
			wherePanel.add(addButton);
						
			CB3model = new DefaultComboBoxModel<String>();
			JComboBox<String> columnSelection3 = new JComboBox<String>(CB3model);
			wherePanel.add(columnSelection3);
			
			JComboBox<String> compareSymbols2 = new JComboBox<String>(new String[] {"=","<>","<","<=",">",">="});
			wherePanel.add(compareSymbols2);
			
			whereText = new JTextField();
			wherePanel.add(whereText);
			
			addButton2 = new JButton("Add");
			addButton2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String text = queryTextArea.getText();
					int idxWhere = (text.indexOf("where") == -1) ? text.indexOf("WHERE") : text.indexOf("where");
					String[] attribute1 = ((String)columnSelection3.getSelectedItem()).split(" ");
					String field = whereText.getText();
					if(idxWhere == -1) {
						queryTextArea.setText(text.replace(";","")+System.lineSeparator()+"where "+attribute1[2].substring(1,attribute1[2].length()-1)+"."+attribute1[1]+" "+compareSymbols2.getSelectedItem()+" "+field+";");
					}else {
						queryTextArea.setText(text.replace(";","")+" and "+attribute1[2].substring(1,attribute1[2].length()-1)+"."+attribute1[1]+" "+compareSymbols2.getSelectedItem()+" "+field+";");
					}
				}				
			});
			wherePanel.add(addButton2);
			
			whereText2 = new JTextField();
			wherePanel.add(whereText2);
			JComboBox<String> compareSymbols3 = new JComboBox<String>(new String[] {"=","<>","<","<=",">",">="});
			wherePanel.add(compareSymbols3);
			whereText3 = new JTextField();
			wherePanel.add(whereText3);
			addButton3 = new JButton("Add");
			addButton3.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String text = queryTextArea.getText();
					int idxWhere = (text.indexOf("where") == -1) ? text.indexOf("WHERE") : text.indexOf("where");
					String field = whereText2.getText();
					String field2 = whereText3.getText();
					if(idxWhere == -1) {
						queryTextArea.setText(text.replace(";","")+System.lineSeparator()+"where "+field+" "+compareSymbols3.getSelectedItem()+" "+field2+";");
					}else {
						queryTextArea.setText(text.replace(";","")+" and "+field+" "+compareSymbols3.getSelectedItem()+" "+field2+";");
					}
				}				
			});
			wherePanel.add(addButton3);
			
			JScrollPane whereScroll = new JScrollPane(wherePanel);			
			controlPanelContainer.add(whereScroll);
			
			addButton.setEnabled(false);
			addButton2.setEnabled(false);
			addButton3.setEnabled(false);
			
			loadAttributeButtons();
			
			SimulationQueryMaker sqm = this;
			
			JPanel buttons2 = new JPanel();
		    buttons2.setBorder(BorderFactory.createEmptyBorder());
		    buttons2.setBorder(BorderFactory.createTitledBorder("Suggested queries"));
//		    buttons2.setPreferredSize(new Dimension(3*dim.width/10, 1*dim.height/10));
		    buttons2.setLayout(new BoxLayout(buttons2, BoxLayout.X_AXIS));
		    JButton mostVisitedItems = new JButton("Most observed items");
		    mostVisitedItems.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
		    		String query = "select it.id, numVisits, it.map, it.room_label, st_astext(it.location) as location, it.title, it.item_label\nfrom item it, (select item_id, o.map, count(*) as numVisits\nfrom item_observation o, item i\nwhere simulation = __ and o.item_id = i.id and o.map = i.map\ngroup by item_id, o.map\norder by numVisits desc) as itemVisits\nwhere it.id = itemVisits.item_id and it.map = itemVisits.map;";
		    		SuggestedQuery sq = new SuggestedQuery(simQueryMaker, sqm, mostVisitedItems.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton mostVisitedRooms = new JButton("Most visited rooms");
		    mostVisitedRooms.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
		    		String query = "select r.label, r.map, numVisits, r.geom\nfrom (select room_label, map, count(*) as numVisits\n\tfrom (select distinct on(room_label,user_id) *\n\tfrom visit v\n\twhere simulation = __) as visitsWithoutUserRepeats\n\tgroup by room_label, map) as roomVisits, room r\nwhere roomVisits.room_label = r.label and roomVisits.map = r.map\norder by numVisits desc;";
		    		SuggestedQuery sq = new SuggestedQuery(simQueryMaker, sqm, mostVisitedRooms.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton usersMostItems = new JButton("Users with most items observed");
		    usersMostItems.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
		    		String query = "select us.id, us.simulation, numItemsObserved, us.is_special, st_astext(us.path) as path\nfrom (select u.id, count(*) as numItemsObserved\nfrom user_sim u, item_observation io\nwhere u.simulation = __ and u.id = io.user_id and u.simulation = io.simulation\ngroup by u.id\norder by numItemsObserved desc) as userVisits, user_sim us\nwhere us.id = userVisits.id and us.simulation = __;";
		    		SuggestedQuery sq = new SuggestedQuery(simQueryMaker, sqm, usersMostItems.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton usersLongestPath = new JButton("Users with longest path");
		    usersLongestPath.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
		    		String query = "select u.id, u.simulation, u.is_special, st_length(u.path)/s.km_to_pixel*1000 as path_length_meters\nfrom user_sim u, simulation s\nwhere u.simulation = __ and u.simulation = s.id\norder by path_length_meters desc;";
		    		SuggestedQuery sq = new SuggestedQuery(simQueryMaker, sqm, usersLongestPath.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton longestStaysRoom = new JButton("Longests stays in a room");
		    longestStaysRoom.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
		    		String query = "select v.id, simulation, room_label, map, user_id, duration as duration_in_seconds, st_length(path)/s.km_to_pixel*1000 as path_length_meters, st_astext(path) as path\nfrom visit v, simulation s\nwhere simulation = __ and simulation = s.id\norder by duration desc;";
		    		SuggestedQuery sq = new SuggestedQuery(simQueryMaker, sqm, longestStaysRoom.getText(),query);
		    		sq.setVisible(true);
//		    		loadAttributeButtons();
		    	}
		    });
//		    JButton averageItemsOfRoom = new JButton("Average number of items per room");
//		    averageItemsOfRoom.addActionListener(new ActionListener() {
//		    	@Override
//				public void actionPerformed(ActionEvent e) {
////		    		queryTextArea.setText("select avg(num) from (\n\tselect count(*) as num\n\tfrom item,map\n\twhere item.map = map.id and map.name = _\n\tgroup by item.room_label\n);");
////		    		for(Component c : buttons.getComponents()) {
////						JButton b = (JButton)c;
////						b.setEnabled(false);
////					}
////					addButton.setEnabled(false);
////					addButton2.setEnabled(false);
////					addButton3.setEnabled(false);
////					selectedTables.clear();
//		    		String query = "select avg(num) from (\n\tselect count(*) as num\n\tfrom item,map\n\twhere item.map = map.id and map.name = __\n\tgroup by item.room_label\n);";
//		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, averageItemsOfRoom.getText(),query);
//		    		sq.setVisible(true);
//		    	}
//		    });
//		    
		    buttons2.add(mostVisitedItems);
		    buttons2.add(mostVisitedRooms);
		    buttons2.add(usersMostItems);
		    buttons2.add(usersLongestPath);
		    buttons2.add(longestStaysRoom);
//		    buttons2.add(averageItemsOfRoom);
		    
		    JScrollPane suggestedQueries = new JScrollPane(buttons2,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		    controlPanelContainer.add(suggestedQueries);
		    
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
						
	}
	
	public void updateControlPanelAfterSuggestedQuery(String queryDesc) {
		if(queryDesc.equals("Average number of items per room") || queryDesc.equals("Most observed items") || queryDesc.equals("Most visited rooms") || queryDesc.equals("Users with most items observed")) {				
			for(Component c : buttons.getComponents()) {
				JButton b = (JButton)c;
				b.setEnabled(false);
			}
			addButton.setEnabled(false);
			addButton2.setEnabled(false);
			addButton3.setEnabled(false);
			selectedTables.clear();
		}else {
    		for(Component c : buttons.getComponents()) {
				JButton b = (JButton)c;
				b.setEnabled(true);
			}
    		selectedTables.clear();
    		switch(queryDesc) {
    			case "Total items":
    	    		selectedTables.add("item");
    	    		selectedTables.add("map");
    				break;
    			case "Items of room":
    	    		selectedTables.add("room");
    	    		selectedTables.add("map");
    	    		selectedTables.add("item");
    	    		break;
    			default:
    	    		selectedTables.add("room");
    	    		selectedTables.add("map");
    				break;
    		}
    		loadAttributeButtons();
		}
	}
	
	private void loadAttributeButtons() {
		for(Component c : buttons.getComponents()) {
			JButton b = (JButton)c;
			if(selectedTables.size() > 0 && selectedTables.getLast().equals(b.getText())) b.setEnabled(false);
		}
		List<Pair<String,String>> columns = MainSimulator.db.getAttributesOfTables(selectedTables);
		attributeButtons.removeAll();
		attributeButtons.setLayout(new GridLayout(0,2));
		List<String> columnNames = new ArrayList<String>();
		int i = 0;
		for(Pair<String,String> column_info : columns) {
			String[] info = column_info.getS().split(" ");
	    	JButton addSelectedColumn = new JButton(info[1]+" "+column_info.getF()+" ("+info[0]+")");
	    	addSelectedColumn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					String text = queryTextArea.getText().toLowerCase();
					int idx = text.indexOf("select");
					if(text.charAt(idx+7) == '*') {
						queryTextArea.setText(text.substring(0,idx+7)+info[0]+"."+column_info.getF()+text.substring(idx+8));
					}else {							
						int fromIdx = text.indexOf("from");
						if(fromIdx != -1) {
							queryTextArea.setText(text.substring(0,fromIdx-1)+","+info[0]+"."+column_info.getF()+text.substring(fromIdx-1));
						}					
					}						
				}
	    	});
	    	attributeButtons.add(addSelectedColumn);
	    	columnNames.add(info[1]+" "+column_info.getF()+" ("+info[0]+")");
	    	i++;
		}
		listScroller2.repaint();
		listScroller2.revalidate();
		
		CB1model.removeAllElements();
		CB1model.addAll(columnNames);
		
		CB2model.removeAllElements();
		CB2model.addAll(columnNames);
		
		CB3model.removeAllElements();
		CB3model.addAll(columnNames);
		
		wherePanel.repaint();
		wherePanel.revalidate();
		
	}
	
}
