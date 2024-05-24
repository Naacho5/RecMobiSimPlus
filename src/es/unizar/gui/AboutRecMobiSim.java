package es.unizar.gui;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.SwingConstants;

import es.unizar.util.Literals;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.Icon;

public class AboutRecMobiSim extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private JTextField txtAboutRecmobisim;
	
	public AboutRecMobiSim(JFrame parent) {
		
		super(parent, true);
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
		txtAboutRecmobisim = new JTextField();
		txtAboutRecmobisim.setOpaque(false);
		txtAboutRecmobisim.setBackground(new Color(0, 0, 0, 0));
		txtAboutRecmobisim.setBorder(null);
		//txtAboutRecmobisim.setEnabled(false);
		txtAboutRecmobisim.setEditable(false);
		txtAboutRecmobisim.setHorizontalAlignment(SwingConstants.CENTER);
		txtAboutRecmobisim.setFont(new Font("Tahoma", Font.PLAIN, 30));
		txtAboutRecmobisim.setText("About RecMobiSim");
		txtAboutRecmobisim.setColumns(10);
		
		JTextArea txtrYearProject = new JTextArea();
		txtrYearProject.setBorder(null);
		txtrYearProject.setOpaque(false);
		txtrYearProject.setBackground(new Color(0, 0, 0, 0));
		//txtrYearProject.setEnabled(false);
		txtrYearProject.setEditable(false);
		txtrYearProject.setText("Project: NEAT-AMBIENCE\r\n(\"Project PID2020-113037RB-I00 / AEI / 10.13039/501100011033\r\n(NEAT-AMBIENCE)\")\r\nAuthors: Alejandro Piedrafita Barrantes and Sergio Ilarri Artigas\r\nYear: 2021/2022");
		
		ImageIcon neatAmbience = new ImageIcon(Literals.IMAGES_PATH + "NEAT-AMBIENCE.png");
		Image scaledNeatAmbience = neatAmbience.getImage().getScaledInstance(200, 200,  java.awt.Image.SCALE_SMOOTH);
		neatAmbience = new ImageIcon(scaledNeatAmbience);
		
		JLabel lblNewLabel = new JLabel(neatAmbience);
		
		ImageIcon cosmos = new ImageIcon(Literals.IMAGES_PATH + "COS2MOS-LOGO-800px.png");
		Image scaledCosmos = cosmos.getImage().getScaledInstance(200, 100,  java.awt.Image.SCALE_SMOOTH);
		cosmos = new ImageIcon(scaledCosmos);
		
		JLabel lblNewLabel2 = new JLabel(cosmos);
		
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 213, GroupLayout.PREFERRED_SIZE)
								.addGap(18)
								.addComponent(lblNewLabel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addComponent(txtrYearProject, GroupLayout.PREFERRED_SIZE, 533, GroupLayout.PREFERRED_SIZE))
						.addComponent(txtAboutRecmobisim, GroupLayout.PREFERRED_SIZE, 434, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(23, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(7)
					.addComponent(txtAboutRecmobisim, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(txtrYearProject, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
						.addComponent(lblNewLabel2, GroupLayout.PREFERRED_SIZE, 183, GroupLayout.PREFERRED_SIZE)))
		);
		getContentPane().setLayout(groupLayout);
	}
}
