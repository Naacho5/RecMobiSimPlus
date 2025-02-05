package es.unizar.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RegisterSimInDB extends JDialog {

	public RegisterSimInDB(JFrame parent) {
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
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		RegisterSimInDB regSim = this;
		JLabel text = new JLabel("The map selected for the simulation has been found in the PostgreSQL database. Would you like to register simulation data in the database?");
		JButton yes = new JButton("Yes");
		yes.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Configuration.simulation.registerSimInDB = true;
				regSim.dispose();
			}
		});
		JButton no = new JButton("No");
		no.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				regSim.dispose();
			}
		});
		JPanel buttons = new JPanel();
		buttons.add(yes);
		buttons.add(no);
		add(text);
		add(buttons);
	}
		
}
