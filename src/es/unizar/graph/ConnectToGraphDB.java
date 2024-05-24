package es.unizar.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import javax.swing.GroupLayout.Alignment;

import es.unizar.graph.GraphManager.DriverHolder;
import es.unizar.spatialDB.DatabaseAccess;

public class ConnectToGraphDB extends JDialog {
	
private static final long serialVersionUID = 1L;
	
	private JTextField title;
	
	public DriverHolder d;
	private GraphManager grMng;
	
	public ConnectToGraphDB(JFrame parent, DriverHolder d, GraphManager grMng) {		
		super(parent, true);
		this.d = d;
		this.grMng = grMng;
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
		this.setTitle("Connect to Neo4j instance");
				        
		JLabel url = new JLabel("URL to Neo4j instance: ");
		url.setFont(new Font("Arial", Font.PLAIN, 18));
		url.setSize(100, 20);
		url.setLocation(100, 100);
		
		JTextField turl = new JTextField();
		turl.setFont(new Font("Arial", Font.PLAIN, 15));
		turl.setSize(190, 20);
		turl.setLocation(200, 100);
        
        JLabel user = new JLabel("User: ");
        user.setFont(new Font("Arial", Font.PLAIN, 18));
        user.setSize(100, 20);
        user.setLocation(100, 100);
		
		JTextField tuser = new JTextField();
		tuser.setFont(new Font("Arial", Font.PLAIN, 15));
		tuser.setSize(190, 20);
		tuser.setLocation(200, 100);
        
        JLabel password = new JLabel("Password: ");
        password.setFont(new Font("Arial", Font.PLAIN, 18));
        password.setSize(100, 20);
        password.setLocation(100, 100);
		
		JPasswordField tpassword = new JPasswordField();
		tpassword.setFont(new Font("Arial", Font.PLAIN, 15));
		tpassword.setSize(190, 20);
		tpassword.setLocation(200, 100);
								
		JButton connect = new JButton("Connect");
		JDialog dialog = this;
		connect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(d.getDriver() != null) {d.getDriver().close();}
				d.setDriver(GraphDatabase.driver(turl.getText(), AuthTokens.basic(tuser.getText(), String.valueOf(tpassword.getPassword()))));
				try(var session = d.getDriver().session()){					
					JOptionPane.showMessageDialog(dialog, "Connected to database", "INFO", JOptionPane.INFORMATION_MESSAGE);
					d.setInfo(turl.getText(),tuser.getText());
					dialog.dispose();
					grMng.loadMapSelector(session);
					session.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					d.setDriver(null);
					JOptionPane.showMessageDialog(dialog, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
				}
				//				try {
//					if(db.isConnected()) {db.disconnect();}
//					db.connect();
//					db.createTables();
//					JOptionPane.showMessageDialog(dialog, "Connected to database", "INFO", JOptionPane.INFORMATION_MESSAGE);
//					dialog.dispose();
//				} catch (SQLException e) {
//					// TODO Auto-generated catch block
//					//e.printStackTrace();
//					JOptionPane.showMessageDialog(dialog, "Couldn't connect to database", "ERROR", JOptionPane.ERROR_MESSAGE);
//				}
			}
		});
		
		JButton disconnect = new JButton("Disconnect");
		disconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				d.getDriver().close();
				d.setDriver(null);
				turl.setText("");
				tuser.setText("");
				tpassword.setText("");
				JOptionPane.showMessageDialog(dialog, "Disconnected from database", "INFO", JOptionPane.INFORMATION_MESSAGE);
				connect.setEnabled(true);
				disconnect.setEnabled(false);
				grMng.clearMapSelector();
//				dialog.dispose();
			}
		});
		
		if(d.getDriver() != null) {
			turl.setText(d.getUrl());
			tuser.setText(d.getUser());
			connect.setEnabled(false);
		}else {
			disconnect.setEnabled(false);
		}
				        			
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		
		groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()  
                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(url).addComponent(user).addComponent(password).addComponent(connect))
                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addComponent(turl,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(tuser,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(tpassword,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(disconnect))
        );  
  
        groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()  
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(url).addComponent(turl))  
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(user).addComponent(tuser))
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(password).addComponent(tpassword))
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(connect).addComponent(disconnect))
		);
        getContentPane().setLayout(groupLayout);
	}

}
