package es.unizar.editor.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import es.unizar.editor.model.MapEditorModel;

public class EditVisitableObjectView extends JDialog {
	
	MapEditorView parent;
	MapEditorModel model;
	
	public EditVisitableObjectView(MapEditorView parent, MapEditorModel model) {
		super(parent.levelEditor, true);
		this.parent = parent;
		this.model = model;
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setTitle("EDIT VISITABLE ICON");
		
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
		
		comboBox.addItem("NULL");
		for (Map.Entry<String, Properties> entry: model.getVisitableObjects().entrySet()) {
			comboBox.addItem(entry.getKey());
		}
		comboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String keySelected = (String) comboBox.getSelectedItem();
				
				if (keySelected != null && !keySelected.equals("") && !keySelected.equals("NULL")
						&& model.getVisitableObjects().containsKey(keySelected))
					((VisitableObjectView) visitableObjectAttributes).loadAttributes(keySelected, (Properties) model.getVisitableObjects().get(keySelected)); 
			}
			
			
		});
		
		visitableObjectAttributes = new VisitableObjectView();
		disableTextFields(visitableObjectAttributes);
		
		edit = new JButton("EDIT VALUES");
		edit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String key = ((VisitableObjectView) visitableObjectAttributes).getTextFieldTypeLabel().getText();
				
				if(key != null && parent.existsVisitableObject(key)) {
					// Properties from the new VisitableObject
					Properties p = model.getVisitableObjects().get(key);
					
					List<JTextField> possibleAttributes = ((VisitableObjectView) visitableObjectAttributes).getVisitableObjectsPossibleAttributes();
					for (JTextField tf: possibleAttributes) {
						// Add properties that aren't null
						if (tf.getText() != null && !tf.getText().equals(""))
							p.setProperty(tf.getName() + key, tf.getText());
					}
					
					parent.refreshVisitableButtons();
					parent.refresh();
				}
				else {
					JOptionPane.showMessageDialog(fullPanel,
							"Please, select an existing visitable object for editting attributes",
							"NULL OR INCORRECT VISITABLE OBJECT", JOptionPane.ERROR_MESSAGE);
				}
			}
			
		});
		
		setLayout();
	}

	private void setLayout() {
		
		GroupLayout gl_fullPanel = new GroupLayout(fullPanel);
		gl_fullPanel.setHorizontalGroup(
			gl_fullPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_fullPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_fullPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(visitableObjectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(edit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		gl_fullPanel.setVerticalGroup(
			gl_fullPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, gl_fullPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(visitableObjectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(edit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		fullPanel.setLayout(gl_fullPanel);
	}
	
	/**
	 * Disable visitableObjectAttributes' fields not needed in this JDialog.
	 * 
	 * In this case, title shouldn't be modified, because the title/name of each object should be defined after being
	 * created and placed in the map.
	 */
	private void disableTextFields(JPanel visitable) {
		
		((VisitableObjectView) visitable).getTextFieldTitle().setEnabled(false);
	}
	
	private JPanel fullPanel;
	
	private JComboBox<String> comboBox;
	public JPanel visitableObjectAttributes;
	private JButton edit;

}
