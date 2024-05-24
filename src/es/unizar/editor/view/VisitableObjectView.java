package es.unizar.editor.view;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileNameExtensionFilter;

import es.unizar.editor.model.Item;
import es.unizar.util.Literals;

import javax.swing.JButton;
import javax.swing.JFileChooser;

public class VisitableObjectView extends JPanel {
	
	List<JTextField> visitableObjectsPossibleAttributes;
	
	public VisitableObjectView() {
		super();
		
		visitableObjectsPossibleAttributes = new LinkedList<JTextField>();
		
		this.setFont(new Font("SansSerif", Font.PLAIN, 14));
		this.setBorder(BorderFactory.createTitledBorder("Visitable object's attributes"));
		
		initComponents();
	}

	private void initComponents() {
		
		imageChooser = new JFileChooser();
		imageChooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
		imageChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
		
		lblObjectsTypeLabel = new JLabel("Object's type/label*: ");
		
		textFieldTypeLabel = new JTextField();
		textFieldTypeLabel.setName(Literals.VERTEX_LABEL);
		textFieldTypeLabel.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldTypeLabel);
		
		lblObjectsTitle = new JLabel("Object's title: ");
		
		textFieldTitle = new JTextField();
		textFieldTitle.setName(Literals.ITEM_TITLE);
		textFieldTitle.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldTitle);
		
		lblObjectsURL = new JLabel("Object's image url:");
		
		textFieldURL = new JTextField();
		textFieldURL.setName(Literals.VERTEX_URL);
		textFieldURL.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldURL);
		
		btnCheck = new JButton("CHECK");
		btnCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					lblImage.setText(null);
					lblImage.setIcon(new ImageIcon(new ImageIcon(textFieldURL.getText()).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT)));
				}
				catch(Exception exception) {
					lblImage.setIcon(null);
					lblImage.setText("ERROR");
				}
			}
			
		});
		
		lblImage = new JLabel();
		lblImage.setIcon(null);
		
		lblObjectsNationality = new JLabel("Object's nationality: ");
		
		textFieldNationality = new JTextField();
		textFieldNationality.setName(Literals.ITEM_NATIONALITY);
		textFieldNationality.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldNationality);
		
		lblObjectsBeginDate = new JLabel("Object's begin date: ");
		
		textFieldBeginDate = new JTextField();
		textFieldBeginDate.setName(Literals.ITEM_BEGINDATE);
		textFieldBeginDate.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldBeginDate);
		
		lblObjectsEndDate = new JLabel("Object's end date: ");
		
		textFieldEndDate = new JTextField();
		textFieldEndDate.setName(Literals.ITEM_ENDDATE);
		textFieldEndDate.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldEndDate);
		
		lblObjectsDate = new JLabel("Object's date: ");
		
		textFieldDate = new JTextField();
		textFieldDate.setName(Literals.ITEM_DATE);
		textFieldDate.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldDate);
		
		/*
		lblObjectsDepartment = new JLabel("Object's department/category: ");
		
		textFieldDepartment = new JTextField();
		textFieldDepartment.setName(Literals.ITEM_DEPARTMENT);
		textFieldDepartment.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldDepartment);
		*/
		
		lblObjectsWidth = new JLabel("Object's width: ");
		
		textFieldWidth = new JTextField();
		textFieldWidth.setName(Literals.ITEM_WIDTH);
		textFieldWidth.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldWidth);
		
		lblObjectsHeight = new JLabel("Object's height: ");
		
		textFieldHeight = new JTextField();
		textFieldHeight.setName(Literals.ITEM_HEIGHT);
		textFieldHeight.setColumns(10);
		visitableObjectsPossibleAttributes.add(textFieldHeight);
		
		setLayout();
	}

	private void setLayout() {
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsTypeLabel)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textFieldTypeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsDate, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsBeginDate, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldBeginDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblObjectsEndDate, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textFieldEndDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						/*.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsDepartment, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldDepartment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))*/
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsWidth, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblObjectsHeight, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textFieldHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsTitle, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblObjectsURL)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblObjectsNationality, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textFieldNationality, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(40)
							.addComponent(btnCheck)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblImage)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsTypeLabel)
						.addComponent(textFieldTypeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsTitle)
						.addComponent(textFieldTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblObjectsURL)
						.addComponent(textFieldURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsNationality)
						.addComponent(textFieldNationality, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnCheck)
						.addComponent(lblImage))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsDate)
						.addComponent(textFieldDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsBeginDate)
						.addComponent(textFieldBeginDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addGap(20)
						.addComponent(lblObjectsEndDate)
						.addComponent(textFieldEndDate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					/*.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsDepartment)
						.addComponent(textFieldDepartment, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)*/
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblObjectsWidth)
						.addComponent(textFieldWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblObjectsHeight)
						.addComponent(textFieldHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap())
		);
		setLayout(groupLayout);
		
	}
	
	/**
	 * Load JTextField values from item's attributes
	 * @param i
	 */
	public void loadAttributes(Item i) {
		
		String attribute = (i.getItemLabel() == null || i.getItemLabel().equals("null")) ? "" : i.getItemLabel();
		textFieldTypeLabel.setText(attribute);
		
		attribute = (i.getTitle() == null || i.getTitle().equals("null")) ? "" : i.getTitle();
		textFieldTitle.setText(attribute);
		
		attribute = (i.getUrlIcon() == null || i.getUrlIcon().equals("null")) ? "" : i.getUrlIcon();
		textFieldURL.setText(attribute);
		
		attribute = (i.getNationality() == null || i.getNationality().equals("null")) ? "" : i.getNationality();
		textFieldNationality.setText(attribute);
		
		attribute = (i.getBeginDate() == null || i.getBeginDate().equals("null")) ? "" : i.getBeginDate();
		textFieldBeginDate.setText(attribute);
		
		attribute = (i.getEndDate() == null || i.getEndDate().equals("null")) ? "" : i.getEndDate();
		textFieldEndDate.setText(attribute);
		
		attribute = (i.getDate() == null || i.getDate().equals("null")) ? "" : i.getDate();
		textFieldDate.setText(attribute);
		
		/*
		attribute = (i.getDepartment() == null || i.getDepartment().equals("null")) ? "" : i.getDepartment();
		textFieldDepartment.setText(attribute);
		*/
		
		attribute = i.getWidth() == 0.0 ? "" : Double.toString(i.getWidth());
		textFieldWidth.setText(attribute);
		
		attribute = i.getHeight() == 0.0 ? "" : Double.toString(i.getHeight());
		textFieldHeight.setText(attribute);
	}
	
	/**
	 * Load JTextField values from visitable object's properties
	 * @param key
	 * @param p
	 */
	public void loadAttributes(String key, Properties p) {
		
		for (JTextField tf: getVisitableObjectsPossibleAttributes()) {
			// Load properties that aren't null
			String prop = p.getProperty(tf.getName() + key);
			if (prop != null)
				tf.setText(prop);
			else
				tf.setText("");
		}
		
	}
	
	/**
	 * Components.
	 */
	private JLabel lblObjectsTypeLabel;
	private JTextField textFieldTypeLabel;
	private JLabel lblObjectsTitle;
	private JTextField textFieldTitle;
	private JLabel lblObjectsURL;
	private JTextField textFieldURL;
	private JButton btnCheck;
	private JLabel lblImage;
	private JLabel lblObjectsNationality;
	private JTextField textFieldNationality;
	private JLabel lblObjectsBeginDate;
	private JTextField textFieldBeginDate;
	private JLabel lblObjectsEndDate;
	private JTextField textFieldEndDate;
	private JLabel lblObjectsDate;
	private JTextField textFieldDate;
	//private JLabel lblObjectsDepartment;
	//private JTextField textFieldDepartment;
	private JLabel lblObjectsWidth;
	private JTextField textFieldWidth;
	private JLabel lblObjectsHeight;
	private JTextField textFieldHeight;
	
	private JFileChooser imageChooser;
	
	
	public JTextField getTextFieldTypeLabel() {
		return textFieldTypeLabel;
	}

	public void setTextFieldTypeLabel(JTextField textFieldTypeLabel) {
		this.textFieldTypeLabel = textFieldTypeLabel;
	}

	public JTextField getTextFieldTitle() {
		return textFieldTitle;
	}

	public void setTextFieldTitle(JTextField textFieldTitle) {
		this.textFieldTitle = textFieldTitle;
	}

	public JTextField getTextFieldURL() {
		return textFieldURL;
	}

	public void setTextFieldURL(JTextField textFieldURL) {
		this.textFieldURL = textFieldURL;
	}

	public JTextField getTextFieldNationality() {
		return textFieldNationality;
	}

	public void setTextFieldNationality(JTextField textFieldNationality) {
		this.textFieldNationality = textFieldNationality;
	}

	public JTextField getTextFieldBeginDate() {
		return textFieldBeginDate;
	}

	public void setTextFieldBeginDate(JTextField textFieldBeginDate) {
		this.textFieldBeginDate = textFieldBeginDate;
	}

	public JTextField getTextFieldEndDate() {
		return textFieldEndDate;
	}

	public void setTextFieldEndDate(JTextField textFieldEndDate) {
		this.textFieldEndDate = textFieldEndDate;
	}

	public JTextField getTextFieldDate() {
		return textFieldDate;
	}

	public void setTextFieldDate(JTextField textFieldDate) {
		this.textFieldDate = textFieldDate;
	}

	/*
	public JTextField getTextFieldDepartment() {
		return textFieldDepartment;
	}

	public void setTextFieldDepartment(JTextField textFieldDepartment) {
		this.textFieldDepartment = textFieldDepartment;
	}
	*/

	public JTextField getTextFieldWidth() {
		return textFieldWidth;
	}

	public void setTextFieldWidth(JTextField textFieldWidth) {
		this.textFieldWidth = textFieldWidth;
	}

	public JTextField getTextFieldHeight() {
		return textFieldHeight;
	}

	public void setTextFieldHeight(JTextField textFieldHeight) {
		this.textFieldHeight = textFieldHeight;
	}
	
	public List<JTextField> getVisitableObjectsPossibleAttributes() {
		return visitableObjectsPossibleAttributes;
	}

	public void setVisitableObjectsPossibleAttributes(List<JTextField> visitableObjectsPossibleAttributes) {
		this.visitableObjectsPossibleAttributes = visitableObjectsPossibleAttributes;
	}

	public JButton getBtnCheck() {
		return btnCheck;
	}

	/*
	public void setBtnCheck(JButton btnCheck) {
		this.btnCheck = btnCheck;
	}
	*/
}
