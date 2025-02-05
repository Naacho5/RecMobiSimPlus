package es.unizar.editor.view;

import javax.swing.JDialog;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Properties;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import es.unizar.editor.model.MapEditorModel;

import javax.swing.JComboBox;
import javax.swing.JButton;

public class DeleteVisitableView extends JDialog {
	
	MapEditorView parent;
	MapEditorModel model;
	
	public DeleteVisitableView(MapEditorView parent, MapEditorModel model) {
		super(parent.levelEditor, true);
		this.parent = parent;
		this.model = model;
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setTitle("DELETE ONE OF THE EXISTING VISITABLE ICONS");
		
		initComponents();
		
		this.pack();
		
		Rectangle parentBounds = parent.levelEditor.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
	}
	
	private void initComponents() {
		fullPanel = new JPanel();
		getContentPane().add(fullPanel);
		
		comboBox = new JComboBox<String>();
		
		for (Map.Entry<String, Properties> entry: model.getVisitableObjects().entrySet()) {
			comboBox.addItem(entry.getKey());
		}
		
		btnDelete = new JButton("DELETE");
		btnDelete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String selected = (String) comboBox.getSelectedItem();
				
				if (selected != null && !selected.equals("") && parent.existsVisitableObject(selected)) {
					model.removeVisitableObject(selected);
					parent.refreshVisitableButtons();
					dispose();
				}
			}
			
		});
		
		GroupLayout groupLayout = new GroupLayout(fullPanel);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(10)
					.addComponent(btnDelete)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap(20, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnDelete))
					.addContainerGap(20, Short.MAX_VALUE))
		);
		fullPanel.setLayout(groupLayout);
	}

	private JPanel fullPanel;
	
	private JComboBox<String> comboBox;
	private JButton btnDelete;
}
