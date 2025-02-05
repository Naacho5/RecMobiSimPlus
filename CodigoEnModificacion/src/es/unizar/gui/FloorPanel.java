package es.unizar.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class FloorPanel extends JPanel {

    private static final long serialVersionUID = -6291233936414618049L;

    public FloorPanel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
    }
    
    public void modifySize(int width, int height) {
		setMinimumSize(new Dimension(width, height));
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1700, 750); //g.fillRect(0, 10, 1660, 546);
    }
}
