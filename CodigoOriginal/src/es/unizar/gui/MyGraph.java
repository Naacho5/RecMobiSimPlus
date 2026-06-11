/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.unizar.gui;

import java.io.File;
import java.text.NumberFormat;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import es.unizar.access.DataAccessItemFile;
import es.unizar.util.Literals;

/**
 *
 * @author Maria del Carmen Rodriguez Hernandez
 */
public class MyGraph extends mxGraph {

	/**
	 * Holds the shared number formatter.
	 *
	 * @see NumberFormat#getInstance()
	 */
	public static final NumberFormat numberFormat = NumberFormat.getInstance();

	/**
	 * Prints out some useful information about the cell in the tooltip.
	 */
	@Override
	public String getToolTipForCell(Object cell) {
		String tip = "<html>";
		try {
			mxGeometry geo = getModel().getGeometry(cell);
			// EDGE
			if (getModel().isEdge(cell)) {
				tip += "type=[edge]<br>";
				// WEIGHT
				if (getModel().getValue(cell) != null) {
					tip += "weight=[" + getModel().getValue(cell) + "]";
				}
			} else {
				// VERTEX
				if (getModel().getValue(cell) != null) {
					
					String[] array = getModel().getValue(cell).toString().split(", ");
					
					// If cell is a user
					if (array.length >= 4 && array[0].startsWith("User")) { // Modificado por Nacho Palacio 2026-06-02
						String userID = array[1].toString();
						String type = array[2].toString();
						String room = array[3].toString();
						
						tip += "userID=[" + userID + "]<br>";
						tip += "RS user=[" + type + "]<br>";
						tip += "room=[" + room + "]<br>";
					}
					else if (array.length >= 2 && array[0].startsWith("corner")) {// Cell is a corner, Modificado por Nacho Palacio 2026-06-02
						tip += "type=[" + array[0] + "]<br>";
						tip += "room=[" + array[1] + "]<br>";
					}
					else if (array.length >= 3) { // Cell is an item, Modificado por Nacho Palacio 2026-06-02
						
						DataAccessItemFile itemFile = new DataAccessItemFile(new File(Literals.ITEM_FLOOR_COMBINED));
						
						String itemType = array[0];
						String room = array[1];
						String itemID = array[2];
						// Painting or Sculpture
						if (itemType.equalsIgnoreCase("Painting") || itemType.equalsIgnoreCase("Sculpture")) {
							tip += "itemID=[" + itemID + "]<br>";
							String[] titleAuthor = itemFile.getTitleAndAuthorFrom(Integer.valueOf(itemID).intValue()).split(", ");
							if (titleAuthor.length >= 2) { // Modificado por Nacho Palacio 2026-06-02
								String title = titleAuthor[0];
								String author = titleAuthor[1];
								tip += "title=[" + title + "]<br>";
								tip += "author=[" + author + "]<br>";
							} else if (titleAuthor.length == 1) {
								tip += "title=[" + titleAuthor[0] + "]<br>";
							}
						}
						// door, corner or stairs
						tip += "type=[" + itemType + "]<br>";
						tip += "room=[" + room + "]<br>";
					}
					else { // Añadido por Nacho Palacio 2026-06-02
						tip += "type=[Invalid cell data]<br>";
					}
				}
				// GEO
				tip += "geo=[";
				if (geo != null) {
					tip += "x=" + numberFormat.format(geo.getX()) + ",y=" + numberFormat.format(geo.getY()) + ",width=" + numberFormat.format(geo.getWidth()) + ",height="
							+ numberFormat.format(geo.getHeight());
				}
				tip += "]";

				mxPoint trans = getView().getTranslate();
        		tip += "<br>scale=" + numberFormat.format(getView().getScale()) + ", translate=[x=" + numberFormat.format(trans.getX()) + ",y=" + numberFormat.format(trans.getY()) + "]";
			}
		} catch (Exception e) {
        	tip += "Error loading tooltip: " + e.getMessage();
    	}

		tip += "</html>";
		return tip;
	}
}
