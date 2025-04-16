package es.unizar.spatialDB;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import es.unizar.editor.MapEditor;
import es.unizar.gui.MainSimulator;
import es.unizar.util.Literals;
import es.unizar.util.Pair;

public class DBQueryMaker {
	
	public static JFrame dbQueryMaker;
	
	public Dimension MIN_SIZE;
	
	private DatabaseAccess db;
	
	JPanel controlPanelContainer;
	JPanel queryContainer;
	JTextArea queryTextArea;
	
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

	public DBQueryMaker(DatabaseAccess db) {
		this.db = db;
		initialize();
		dbQueryMaker.pack();
		dbQueryMaker.setVisible(true);
	}
	
	private void initialize() {
		
		dbQueryMaker = new JFrame();
		dbQueryMaker.getContentPane().setLayout(new GridBagLayout());
		
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
				dbQueryMaker.dispose();
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
		dbQueryMaker.getContentPane().add(close, gbc);
		
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
		dbQueryMaker.getContentPane().add(controlPanelContainer, gbc);
		
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
		dbQueryMaker.getContentPane().add(queryContainer, gbc);
		
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
					ResultSet rs = db.runQuery(queryTextArea.getText());
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
					JOptionPane.showMessageDialog(dbQueryMaker, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
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
	

	private boolean isTable(String name, List<String> tableNames) {
		return tableNames.contains(name);
	}
	
	private boolean isSeparation(String text, int i) {
		return(text.charAt(i) == ' ' || ((i+System.lineSeparator().length()) < text.length() && text.substring(i, i+System.lineSeparator().length()).equals(System.lineSeparator())) || text.charAt(i) == ';');
	}
	
	private void initControlPanel() {
		
		try {
			List<String> tableNames = db.getNamesOfTables();
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
							String text = queryTextArea.getText();
							int idx = (text.indexOf(System.lineSeparator()+"from ") == -1) ? text.indexOf(System.lineSeparator()+"FROM ") : text.indexOf(System.lineSeparator()+"from ");
							int idxWhere = (text.indexOf("where") == -1) ? text.indexOf("WHERE") : text.indexOf("where");
//							if(idxWhere == -1) idxWhere = text.length();
//							if(idx > -1) {
//								int i = idx + (System.lineSeparator()+"from ").length();
//								int j = i;
//								int lastFound = i;
//								boolean end = false;
//								while(!end) {
//									if(text.charAt(i) != ',') {
//										if(isTable(text.substring(j,i+1),tableNames)){
//											lastFound = i+1;
//											if(i+1 < text.length() && text.charAt(i+1) != ',') {
//												end = true;
//												text = text.substring(0, i+1)+((idxWhere == -1) ? "" : text.substring(idxWhere));
//											}
//										}
//									}else {
//										j = i+1;
//										i = j-1;
//									}
//									i++;
//									if(i == text.length()) end = true;
//								}
//								if(lastFound == idx + (System.lineSeparator()+"from ").length()) {
//									queryTextArea.setText(text.substring(0,lastFound)+table+((idxWhere == -1) ? "" : text.substring(idxWhere)));
//								}else {								
//									queryTextArea.setText(text.substring(0,lastFound)+","+table+((idxWhere == -1) ? "" : text.substring(idxWhere)));
//								}
//							}
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
//			listScroller.setPreferredSize(new Dimension(3*dim.width/10, 2*dim.height/10));
						
			JPanel tableList = new JPanel();
			tableList.setBorder(BorderFactory.createTitledBorder("Tables"));
			tableList.add(listScroller);
			tableList.setPreferredSize(new Dimension(3*dim.width/10, 3*dim.height/10));
			tableList.setLayout(new GridLayout(1,1));
			controlPanelContainer.add(tableList);
			
			attributeButtons = new JPanel();
			attributeButtons.setBorder(BorderFactory.createEmptyBorder());
			
			listScroller2 = new JScrollPane(attributeButtons);
//			listScroller2.setPreferredSize(new Dimension(3*dim.width/10, 2*dim.height/10));
			
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
			
			DBQueryMaker dbqm = this;
			
			JPanel buttons2 = new JPanel();
		    buttons2.setBorder(BorderFactory.createEmptyBorder());
		    buttons2.setBorder(BorderFactory.createTitledBorder("Suggested queries"));
//		    buttons2.setPreferredSize(new Dimension(3*dim.width/10, 1*dim.height/10));
		    buttons2.setLayout(new BoxLayout(buttons2, BoxLayout.X_AXIS));
		    JButton totalArea = new JButton("Total area");
		    totalArea.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select sum(ST_Area(geom))\nfrom room,map\nwhere map.id = room.map and map.name = _");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(true);
//					}
//		    		selectedTables.clear();
//		    		selectedTables.add("room");
//		    		selectedTables.add("map");
//		    		loadAttributeButtons();
		    		String query = "select sum(ST_Area(geom))*pow(pixel_represents_in_meters,2)\nfrom room,map\nwhere map.id = room.map and map.name = __";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, totalArea.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton areaOfRoom = new JButton("Area of room");
		    areaOfRoom.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select ST_Area(geom)\nfrom room,map\nwhere map.id = room.map and map.name = _ and room.label = _");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(true);
//					}
//		    		selectedTables.clear();
//		    		selectedTables.add("room");
//		    		selectedTables.add("map");
//		    		loadAttributeButtons();
		    		String query = "select ST_Area(geom)*pow(pixel_represents_in_meters,2)\nfrom room,map\nwhere map.id = room.map and map.name = __ and room.label = __";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, areaOfRoom.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    JButton averageAreaOfRoom = new JButton("Average area of room");
		    averageAreaOfRoom.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select avg(ST_Area(geom))\nfrom room,map\nwhere map.id = room.map and map.name = _");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(true);
//					}
//		    		selectedTables.clear();
//		    		selectedTables.add("room");
//		    		selectedTables.add("map");
		    		String query = "select avg(ST_Area(geom))*pow(pixel_represents_in_meters,2)\nfrom room,map\nwhere map.id = room.map and map.name = __";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, averageAreaOfRoom.getText(),query);
		    		sq.setVisible(true);
//		    		loadAttributeButtons();
		    	}
		    });
		    JButton totalItems = new JButton("Total items");
		    totalItems.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select item.id,item.map,item.room_label,item.location,item.url_image,item.title,item.width,item.height,item.nationality,item.begin_date,item.end_date,item.date,item.item_label\nfrom item,map\nwhere map.id = item.map and map.name = _");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(true);
//					}
//		    		selectedTables.clear();
//		    		selectedTables.add("item");
//		    		selectedTables.add("map");
		    		String query = "select item.id,item.map,item.room_label,item.location,item.url_image,item.title,item.width,item.height,item.nationality,item.begin_date,item.end_date,item.date,item.item_label\nfrom item,map\nwhere map.id = item.map and map.name = __";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, totalItems.getText(),query);
		    		sq.setVisible(true);
//		    		loadAttributeButtons();
		    	}
		    });
		    JButton itemsOfRoom = new JButton("Items of room");
		    itemsOfRoom.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select item.id,item.map,item.room_label,item.location,item.url_image,item.title,item.width,item.height,item.nationality,item.begin_date,item.end_date,item.date,item.item_label\nfrom item,map,room\nwhere map.id = item.map and map.id = room.map and map.name = _ and room.label = _");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(true);
//					}
//		    		selectedTables.clear();
//		    		selectedTables.add("room");
//		    		selectedTables.add("map");
//		    		selectedTables.add("item");
		    		String query = "select item.id,item.map,item.room_label,item.location,item.url_image,item.title,item.width,item.height,item.nationality,item.begin_date,item.end_date,item.date,item.item_label\nfrom item,map,room\nwhere map.id = item.map and map.id = room.map and map.name = __ and room.label = __";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, itemsOfRoom.getText(),query);
		    		sq.setVisible(true);
//		    		loadAttributeButtons();
		    	}
		    });
		    JButton averageItemsOfRoom = new JButton("Average number of items per room");
		    averageItemsOfRoom.addActionListener(new ActionListener() {
		    	@Override
				public void actionPerformed(ActionEvent e) {
//		    		queryTextArea.setText("select avg(num) from (\n\tselect count(*) as num\n\tfrom item,map\n\twhere item.map = map.id and map.name = _\n\tgroup by item.room_label\n);");
//		    		for(Component c : buttons.getComponents()) {
//						JButton b = (JButton)c;
//						b.setEnabled(false);
//					}
//					addButton.setEnabled(false);
//					addButton2.setEnabled(false);
//					addButton3.setEnabled(false);
//					selectedTables.clear();
		    		String query = "select avg(num) from (\n\tselect count(*) as num\n\tfrom item,map\n\twhere item.map = map.id and map.name = __\n\tgroup by item.room_label\n);";
		    		SuggestedQuery sq = new SuggestedQuery(dbQueryMaker, dbqm, averageItemsOfRoom.getText(),query);
		    		sq.setVisible(true);
		    	}
		    });
		    
		    buttons2.add(totalArea);
		    buttons2.add(areaOfRoom);
		    buttons2.add(averageAreaOfRoom);
		    buttons2.add(totalItems);
		    buttons2.add(itemsOfRoom);
		    buttons2.add(averageItemsOfRoom);
		    
		    JScrollPane suggestedQueries = new JScrollPane(buttons2,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		    controlPanelContainer.add(suggestedQueries);
		    
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
						
	}
	
	public void updateControlPanelAfterSuggestedQuery(String queryDesc) {
		if(queryDesc.equals("Average number of items per room")) {				
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
		List<Pair<String,String>> columns = db.getAttributesOfTables(selectedTables);
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
