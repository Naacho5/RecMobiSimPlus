package es.unizar.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import es.unizar.gui.simulation.User;

public class FloorPanelCombined extends JPanel {

	private static final long serialVersionUID = -6291233936414618049L;
	
	private int width, height;
	private int separation = 5;

	public FloorPanelCombined(int width, int height) {
		this.width = width;
		this.height = height;
		setPreferredSize(new Dimension(width, height));
	}
	
	public void modifySize(int width, int height) {
		this.width = width;
		this.height = height;
		setPreferredSize(new Dimension(width, height));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width + separation, height + separation);

		/*
		 * User cells modified directly while moving in newer version. Not needed anymore.
		 * 
		 * try {
		for (int i = 0; i < user.length; i++) {
			user[i].draw(g);
		}
		} catch (IOException ex) {
			Logger.getLogger(FloorPanelCombined.class.getName()).log(Level.SEVERE, null, ex);
		}*/
	}
}
