package es.unizar.editor.view;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class NewMap extends JDialog {
	
	private JTextField textFieldWidth;
	private JTextField textFieldHeight;
	private JTextField textFieldPixelRepresents;
	
	public int width, height;
	public double pixelRepresentsMeters;
	
	public NewMap(JFrame parent) {
		super(parent, true);
		getContentPane().setFont(new Font("SansSerif", Font.PLAIN, 14));
		setType(Type.POPUP);
		setTitle("New Map");
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
		
		JLabel lblX = new JLabel("Width: ");
		lblX.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		JLabel lblHeight = new JLabel("Height:");
		lblHeight.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		textFieldWidth = new JTextField();
		textFieldWidth.setToolTipText("Introduce an integer value. DEFAULT: 20");
		textFieldWidth.setColumns(10);
		textFieldWidth.setText("500");
		
		textFieldHeight = new JTextField();
		textFieldHeight.setToolTipText("Introduce an integer value. DEFAULT: 20");
		textFieldHeight.setColumns(10);
		textFieldHeight.setText("500");
		
		JLabel lblPx_W = new JLabel("px");
		lblPx_W.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		JLabel lblPx_H = new JLabel("px");
		lblPx_H.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		JLabel lblPixelRepresents = new JLabel("1 pixel represents:");
		lblPixelRepresents.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		textFieldPixelRepresents = new JTextField();
		textFieldPixelRepresents.setToolTipText("Introduce a double value.");
		textFieldPixelRepresents.setColumns(10);
		textFieldPixelRepresents.setText("1.0");
		
		JButton save = new JButton();
		save.setFont(new Font("SansSerif", Font.PLAIN, 14));
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newMapEvent();
			}
		});
		save.setText("Save");
		
		JLabel lblMetres = new JLabel("metres");
		lblMetres.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(lblPixelRepresents)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(textFieldPixelRepresents, Alignment.LEADING)
								.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
									.addComponent(lblHeight, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(textFieldHeight))
								.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
									.addComponent(lblX)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(textFieldWidth, GroupLayout.PREFERRED_SIZE, 145, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblMetres)
								.addComponent(lblPx_H, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblPx_W)))
						.addComponent(save, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblX)
						.addComponent(textFieldWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPx_W))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(textFieldHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblHeight, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPx_H))
					.addGap(18)
					.addComponent(lblPixelRepresents, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
					.addGap(3)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(textFieldPixelRepresents, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblMetres))
					.addGap(18)
					.addComponent(save)
					.addContainerGap())
		);
		getContentPane().setLayout(groupLayout);
	}
	
	private void newMapEvent() {
		boolean correct = true;
		
		if (!textFieldWidth.getText().isEmpty()) {
			try {
				width = Integer.valueOf(textFieldWidth.getText());
			} catch (Exception e) {
				correct = false;
				JOptionPane.showMessageDialog(this, "Width: Value must be int", "ERROR SAVING", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			width = 0;
		}
		
		if (!textFieldHeight.getText().isEmpty()) {
			try {
				height = Integer.valueOf(textFieldHeight.getText());
			} catch (Exception e) {
				correct = false;
				JOptionPane.showMessageDialog(this, "Height: Value must be int", "ERROR SAVING", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			height = 0;
		}
		
		if (!textFieldPixelRepresents.getText().isEmpty()) {
			try {
				pixelRepresentsMeters = Double.valueOf(textFieldPixelRepresents.getText());
			} catch (Exception e) {
				correct = false;
				JOptionPane.showMessageDialog(this, "1 pixel represents: Value must be double", "ERROR SAVING", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			correct = false;
			JOptionPane.showMessageDialog(this, "Represents in meters must not be empty", "ERROR SAVING", JOptionPane.ERROR_MESSAGE);
		}
		
		if (correct) {
			setVisible(false);
	        dispose();
		}
	}
}
