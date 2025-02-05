package es.unizar.graph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import es.unizar.graph.GraphManager.DriverHolder;

public class Loading extends JDialog {
	
	private static final long serialVersionUID = 1L;

	public Loading(JFrame parent) {		
		super(parent, false);
		setUndecorated(true);
		setLayout(new BorderLayout());
		setSize(new Dimension(3*parent.getSize().width/10, 3*parent.getSize().height/10));
		JLabel text = new JLabel("Creating graph...");
		text.setSize(new Dimension(3*parent.getSize().width/10, 3*parent.getSize().height/10));
		text.setFont(new Font("Arial",Font.PLAIN,30));
		add(text,BorderLayout.CENTER);
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
	}

}
