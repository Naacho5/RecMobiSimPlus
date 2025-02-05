package es.unizar.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import es.unizar.editor.view.MapEditorView;
import es.unizar.spatialDB.DatabaseAccess;
import es.unizar.util.Literals;

public class ConnectToDB extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private JTextField title;
	
	public DatabaseAccess db;
	
	public ConnectToDB(JFrame parent, DatabaseAccess db) {		
		super(parent, true);
		this.db = db;
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
		title = new JTextField();
		title.setOpaque(false);
		title.setBackground(new Color(0, 0, 0, 0));
		title.setBorder(null);
		//txtAboutRecmobisim.setEnabled(false);
		title.setEditable(false);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setFont(new Font("Tahoma", Font.PLAIN, 18));
		title.setText("Connect to spatial database (PostgreSQL + PostGIS)");
		title.setColumns(10);
		
		this.setTitle("Connect to spatial database (PostgreSQL + PostGIS)");
				        
		JLabel port = new JLabel("Port: ");
		port.setFont(new Font("Arial", Font.PLAIN, 18));
		port.setSize(100, 20);
		port.setLocation(100, 100);
		
		JTextField tport = new JTextField();
		tport.setFont(new Font("Arial", Font.PLAIN, 15));
		tport.setSize(190, 20);
		tport.setLocation(200, 100);
        
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
		
        JLabel name = new JLabel("Database name: ");
        name.setFont(new Font("Arial", Font.PLAIN, 18));
        name.setSize(100, 20);
        name.setLocation(100, 100);
		
		JTextField tname = new JTextField();
		tname.setFont(new Font("Arial", Font.PLAIN, 15));
		tname.setSize(190, 20);
		tname.setLocation(200, 100);
						
		JButton connect = new JButton("Connect");
		JDialog dialog = this;
		connect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//db.setAttributes(tuser.getText(),String.valueOf(tpassword.getPassword()),tport.getText(),tname.getText());
				if(!tport.getText().equals("")) db.setAttributes(tuser.getText(),String.valueOf(tpassword.getPassword()),tport.getText(),tname.getText());
				else db.setAttributes("postgres","database","5432","RecMobiSimDB");
				try {
					if(db.isConnected()) {db.disconnect();}
					db.connect();
					db.createTables();
					JOptionPane.showMessageDialog(dialog, "Connected to database", "INFO", JOptionPane.INFORMATION_MESSAGE);
					dialog.dispose();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					JOptionPane.showMessageDialog(dialog, "Couldn't connect to database", "ERROR", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		JButton disconnect = new JButton("Disconnect");
		disconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				db.disconnect();
				tport.setText("");
				tuser.setText("");
				tname.setText("");
				JOptionPane.showMessageDialog(dialog, "Disconnected from database", "INFO", JOptionPane.INFORMATION_MESSAGE);
				connect.setEnabled(true);
				disconnect.setEnabled(false);
//				dialog.dispose();
			}
		});
		
		if(db.isConnected()) {
			tport.setText(db.getPort());
			tuser.setText(db.getUser());
			tname.setText(db.getDatabaseName());
			connect.setEnabled(false);
		}else {
			disconnect.setEnabled(false);
		}
				        			
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		
		groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()  
                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(port).addComponent(user).addComponent(password).addComponent(name).addComponent(connect))
                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING).addComponent(tport,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(tuser,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(tpassword,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(tname,GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE).addComponent(disconnect))
        );  
  
        groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()  
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(port).addComponent(tport))  
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(user).addComponent(tuser))
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(password).addComponent(tpassword))
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(name).addComponent(tname))
                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(connect).addComponent(disconnect))
		);
        getContentPane().setLayout(groupLayout);
	}
	
}
