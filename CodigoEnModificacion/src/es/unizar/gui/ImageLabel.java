package es.unizar.gui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

// http://www.java2s.com/Code/JavaAPI/javax.swing/JLabelsetIconIconicon.htm
public class ImageLabel extends JLabel {
	
  public ImageLabel(String img) {
	    this(new ImageIcon(img));
	  }

	  public ImageLabel(ImageIcon icon) {
	    setIcon(icon);
	    // setMargin(new Insets(0,0,0,0));
	    setIconTextGap(0);
	    // setBorderPainted(false);
	    setBorder(null);
	    setText(null);
	    setSize(icon.getImage().getWidth(null), icon.getImage().getHeight(null));
	  }
}
