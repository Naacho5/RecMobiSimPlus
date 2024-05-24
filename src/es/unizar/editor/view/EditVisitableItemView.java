package es.unizar.editor.view;

import javax.swing.JPanel;
import javax.swing.JButton;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import es.unizar.editor.model.Door;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.Room;

public class EditVisitableItemView extends JPanel {
	
	Item item;
	String type;
	
	public EditVisitableItemView(Item i, String visitableItemType, boolean editable) {
		
		this.item = i;
		this.type = visitableItemType;
		
		objectAttributes = new VisitableObjectView();
		loadAttributes();
		objectAttributes.getTextFieldTypeLabel().setEnabled(false);
		objectAttributes.getTextFieldURL().setEnabled(false);
		objectAttributes.getBtnCheck().setEnabled(false);
		objectAttributes.getBtnCheck().setVisible(false);
		
		if(!editable) {
			objectAttributes.getTextFieldTypeLabel().setEditable(false);
			objectAttributes.getTextFieldTitle().setEditable(false);
			objectAttributes.getTextFieldURL().setEditable(false);
			objectAttributes.getTextFieldNationality().setEditable(false);
			objectAttributes.getTextFieldBeginDate().setEditable(false);
			objectAttributes.getTextFieldEndDate().setEditable(false);
			objectAttributes.getTextFieldDate().setEditable(false);
			objectAttributes.getTextFieldWidth().setEditable(false);
			objectAttributes.getTextFieldHeight().setEditable(false);
		}
		
		JButton btnNewButton = new JButton("Edit visitable object's attributes");
		btnNewButton.addActionListener(editAttributes);
		
		GroupLayout groupLayout = new GroupLayout(this);
		if(editable) {
			groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addComponent(objectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(btnNewButton))
						.addContainerGap())
			);
			groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(objectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(btnNewButton)
						.addContainerGap())
			);
		}else {
			groupLayout.setHorizontalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(objectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.addContainerGap())
				);
				groupLayout.setVerticalGroup(
					groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(objectAttributes, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addContainerGap())
				);
		}
		setLayout(groupLayout);
	}
	
	private VisitableObjectView objectAttributes; // Panel with object attributes
	
	private void loadAttributes() {
		objectAttributes.loadAttributes(item);
	}
	
	Action editAttributes = new AbstractAction() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				item.setTitle(objectAttributes.getTextFieldTitle().getText());
				item.setNationality(objectAttributes.getTextFieldNationality().getText());
				item.setBeginDate(objectAttributes.getTextFieldBeginDate().getText());
				item.setEndDate(objectAttributes.getTextFieldEndDate().getText());
				item.setDate(objectAttributes.getTextFieldDate().getText());
				//item.setDepartment(objectAttributes.getTextFieldDepartment().getText());
				try {
					item.setWidth(Double.valueOf(objectAttributes.getTextFieldWidth().getText()));
					item.setHeight(Double.valueOf(objectAttributes.getTextFieldHeight().getText()));
				}
				catch (Exception exception) {
					System.out.println(exception);
				}
        	}
        	catch (Exception exception) {
        		System.out.println(exception);
        	}
			
		}
	};

}
