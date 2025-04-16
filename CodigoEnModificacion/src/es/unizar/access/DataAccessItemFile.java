package es.unizar.access;

import es.unizar.util.Literals;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Access to the values of the properties stored in the floor files. Getters and setters.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public class DataAccessItemFile extends DataAccess {

    public DataAccessItemFile(File file) {
        super(file);
        if(file != null && file.getName().endsWith(".svg")) {
			new SVGParserSimulation(this,file);
			loadProperties();
		}
    }

	//--------------------------------------------------------------------------------------------------------------------
	//-- GETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
    
    public String getName() {
        return getPropertyValue(Literals.NAME);
    }

    public int getNumberOfItems() {
        return Integer.valueOf(getPropertyValue(Literals.NUMBER_ITEMS)).intValue();
    }

    public int getVertexDimensionHeight() {
        return Integer.valueOf(getPropertyValue(Literals.VERTEX_DIMENSION_HEIGHT)).intValue();
    }

    public int getVertexDimensionWidth() {
        return Integer.valueOf(getPropertyValue(Literals.VERTEX_DIMENSION_WIDTH)).intValue();
    }

    public int getItemID(int position) {
        return Integer.valueOf(getPropertyValue(Literals.ITEM_ID + position)).intValue();
    }

    public String getItemTitle(int position) {
        return getPropertyValue(Literals.ITEM_TITLE + position);
    }
    
    public String getVisitableVertexURL(int position) {
    	return getPropertyValue(Literals.VERTEX_URL + position);
    }
    
    public String getItemArtist(int position) {
        return getPropertyValue(Literals.ITEM_ARTIST + position);
    }

    public int getItemConstituentID(int position) {
        return Integer.valueOf(getPropertyValue(Literals.ITEM_CONSTITUENTID + position)).intValue();
    }

    public String getItemArtistBio(int position) {
        return getPropertyValue(Literals.ITEM_ARTISTBIO + position);
    }

    public String getItemNationality(int position) {
        return getPropertyValue(Literals.ITEM_NATIONALITY + position);
    }

    public String getItemBeginDate(int position) {
        return getPropertyValue(Literals.ITEM_BEGINDATE + position);
    }

    public String getItemEndDate(int position) {
        return getPropertyValue(Literals.ITEM_ENDDATE + position);
    }

    public String getItemGender(int position) {
        return getPropertyValue(Literals.ITEM_GENDER + position);
    }

    public String getItemDate(int position) {
        return getPropertyValue(Literals.ITEM_DATE + position);
    }

    public String getItemMedium(int position) {
        return getPropertyValue(Literals.ITEM_MEDIUM + position);
    }

    public String getItemDimensions(int position) {
        return getPropertyValue(Literals.ITEM_DIMENSIONS + position);
    }

    public String getItemCreditLine(int position) {
        return getPropertyValue(Literals.ITEM_CREDITLINE + position);
    }

    public String getItemAccessionNumber(int position) {
        return getPropertyValue(Literals.ITEM_ACCESSIONNUMBER + position);
    }

    public String getItemDepartment(int position) {
        return getPropertyValue(Literals.ITEM_DEPARTMENT + position);
    }

    public String getItemDateAcquired(int position) {
        return getPropertyValue(Literals.ITEM_DATEACQUIRED + position);
    }

    public String getItemCataloged(int position) {
        return getPropertyValue(Literals.ITEM_CATALOGED + position);
    }

    public String getItemDepth(int position) {
        return getPropertyValue(Literals.ITEM_DEPTH + position);
    }

    public String getItemDiameter(int position) {
        return getPropertyValue(Literals.ITEM_DIAMETER + position);
    }

    public String getItemHeight(int position) {
        return getPropertyValue(Literals.ITEM_HEIGHT + position);
    }

    public String getItemWeight(int position) {
        return getPropertyValue(Literals.ITEM_WEIGHT + position);
    }

    public String getItemWidth(int position) {
        return getPropertyValue(Literals.ITEM_WIDTH + position);
    }

    public String getItemRoom(int position) {
        return getPropertyValue(Literals.ITEM_ROOM + position);
    }

    public String getVertexLabel(int position) {
        return getPropertyValue(Literals.VERTEX_LABEL + position);
    }

    public String getVertexXY(int position) {
        return getPropertyValue(Literals.VERTEX_XY + position);
    }

    public String getTitleAndAuthorFrom(int itemID) {
        return getItemTitle(itemID) + ", " + getItemArtist(itemID);
    }
    
    //--------------------------------------------------------------------------------------------------------------------
	//-- END GETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
	
	
	//--------------------------------------------------------------------------------------------------------------------
	//-- SETTERS																										--
	//--------------------------------------------------------------------------------------------------------------------
    public void setName(String name) {
        setPropertyValue(Literals.NAME, name);
    }

    public void setNumberOfItems(int numberOfItems) {
        setPropertyValue(Literals.NUMBER_ITEMS, Integer.toString(numberOfItems));
    }

    public void setVertexDimensionHeight(int height) {
        setPropertyValue(Literals.VERTEX_DIMENSION_HEIGHT, Integer.toString(height));
    }

    public void setVertexDimensionWidth(int width) {
    	setPropertyValue(Literals.VERTEX_DIMENSION_WIDTH, Integer.toString(width));
    }

    public void setItemID(int position, int id) {
        setPropertyValue(Literals.ITEM_ID + position, Integer.toString(id));
    }
    
    public void setItemTitle(int position, String title) {
        setPropertyValue(Literals.ITEM_TITLE + position, title);
    }
    
    public void setVertexURL(int position, String url) {
        setPropertyValue(Literals.VERTEX_URL + position, url);
    }

    /*
    public String getItemArtist(int position) {
        return getPropertyValue(Literals.ITEM_ARTIST + position);
    }

    public int getItemConstituentID(int position) {
        return Integer.valueOf(getPropertyValue(Literals.ITEM_CONSTITUENTID + position)).intValue();
    }

    public String getItemArtistBio(int position) {
        return getPropertyValue(Literals.ITEM_ARTISTBIO + position);
    }
    */

    public void setItemNationality(int position, String nationality) {
        setPropertyValue(Literals.ITEM_NATIONALITY + position, nationality);
    }

    public void setItemBeginDate(int position, String beginDate) {
        setPropertyValue(Literals.ITEM_BEGINDATE + position, beginDate);
    }

    public void setItemEndDate(int position, String endDate) {
        setPropertyValue(Literals.ITEM_ENDDATE + position, endDate);
    }

    /*
    public String getItemGender(int position) {
        return getPropertyValue(Literals.ITEM_GENDER + position);
    }
    */

    public void setItemDate(int position, String date) {
        setPropertyValue(Literals.ITEM_DATE + position, date);
    }

    /*
    public String getItemMedium(int position) {
        return getPropertyValue(Literals.ITEM_MEDIUM + position);
    }

    public String getItemDimensions(int position) {
        return getPropertyValue(Literals.ITEM_DIMENSIONS + position);
    }

    public String getItemCreditLine(int position) {
        return getPropertyValue(Literals.ITEM_CREDITLINE + position);
    }

    public String getItemAccessionNumber(int position) {
        return getPropertyValue(Literals.ITEM_ACCESSIONNUMBER + position);
    }
    */

    public void setItemDepartment(int position, String department) {
        setPropertyValue(Literals.ITEM_DEPARTMENT + position, department);
    }

    /*
    public String getItemDateAcquired(int position) {
        return getPropertyValue(Literals.ITEM_DATEACQUIRED + position);
    }

    public String getItemCataloged(int position) {
        return getPropertyValue(Literals.ITEM_CATALOGED + position);
    }

    public String getItemDepth(int position) {
        return getPropertyValue(Literals.ITEM_DEPTH + position);
    }

    public String getItemDiameter(int position) {
        return getPropertyValue(Literals.ITEM_DIAMETER + position);
    }
    */

    public void setItemHeight(int position, double height) {
        setPropertyValue(Literals.ITEM_HEIGHT + position, Double.toString(height));
    }

    /*
    public String getItemWeight(int position) {
        return getPropertyValue(Literals.ITEM_WEIGHT + position);
    }
    */

    public void setItemWidth(int position, double width) {
        setPropertyValue(Literals.ITEM_WIDTH + position, Double.toString(width));
    }

    public void setItemRoom(int position, int roomLabel) {
        setPropertyValue(Literals.ITEM_ROOM + position, Integer.toString(roomLabel));
    }

    public void setVertexLabel(int position, String itemLabel) {
        setPropertyValue(Literals.VERTEX_LABEL + position, itemLabel);
    }

    public void setVertexXY(int position, String itemLocation) {
        setPropertyValue(Literals.VERTEX_XY + position, itemLocation);
    }

    /*
    public String getTitleAndAuthorFrom(int itemID) {
        return getItemTitle(itemID) + ", " + getItemArtist(itemID);
    }
    */
    
	//--------------------------------------------------------------------------------------------------------------------
	//-- END SETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------

    
    //--------------------------------------------------------------------------------------------------------------------
  	//-- VISITABLE GETTERS																										--
  	//--------------------------------------------------------------------------------------------------------------------
    
    public String getVisitable(int index) {
    	return getPropertyValue(Literals.VISITABLE + index);
    }
    
    public String getVisitableVertexLabel(String label) {
        return getPropertyValue(Literals.VERTEX_LABEL + label);
    }
    
    /*
    public void setVisitableItemID(String label, int id) {
        setPropertyValue(Literals.ITEM_ID + label, Integer.toString(id));
    }
    
    
    public void setVisitableItemTitle(int position, String title) {
        setPropertyValue(Literals.ITEM_TITLE + position, title);
    }
    */
    
    public String getVisitableVertexURL(String label) {
        return getPropertyValue(Literals.VERTEX_URL + label);
    }
    
    public String getVisitableItemNationality(String label) {
        return getPropertyValue(Literals.ITEM_NATIONALITY + label);
    }

    public String getVisitableItemBeginDate(String label) {
        return getPropertyValue(Literals.ITEM_BEGINDATE + label);
    }

    public String getVisitableItemEndDate(String label) {
        return getPropertyValue(Literals.ITEM_ENDDATE + label);
    }
    
    public String getVisitableItemDate(String label) {
        return getPropertyValue(Literals.ITEM_DATE + label);
    }
    
    public String getVisitableItemDepartment(String label) {
        return getPropertyValue(Literals.ITEM_DEPARTMENT + label);
    }
    
    public Double getVisitableItemHeight(String label) {
        return Double.valueOf(getPropertyValue(Literals.ITEM_HEIGHT + label));
    }
    
    public Double getVisitableItemWidth(String label) {
        return Double.valueOf(getPropertyValue(Literals.ITEM_WIDTH + label));
    }
    
    public Properties getAllVisitableProperties(String label) {
    	
    	Properties p = new Properties();
    	
    	// Item Label
    	String value = getVisitableVertexLabel(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.VERTEX_LABEL + label, value);
    	
    	// Item URL
    	value = getVisitableVertexURL(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.VERTEX_URL + label, value);
    	
    	// Item Nationality
    	value = getVisitableItemNationality(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.ITEM_NATIONALITY + label, value);
    	
    	// Item BeginDate
    	value = getVisitableItemBeginDate(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.ITEM_BEGINDATE + label, value);
    	
    	// Item EndDate
    	value = getVisitableItemEndDate(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.ITEM_ENDDATE + label, value);
    	
    	// Item Date
    	value = getVisitableItemDate(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.ITEM_DATE + label, value);
    	
    	// Item Department
    	value = getVisitableItemDepartment(label);
    	if (value != null && !value.equals(""))
    		p.put(Literals.ITEM_DEPARTMENT + label, value);
    	
    	// Item Height
    	try {
	    	value = Double.toString(getVisitableItemHeight(label));
	    	if (value != null && !value.equals(""))
	    		p.put(Literals.ITEM_HEIGHT + label, value);
    	}
    	catch (Exception e) {
    		// Double.valueOf() didn't work because there was no value
    	}
    	
    	// Item Width
    	try {
	    	value = Double.toString(getVisitableItemWidth(label));
	    	if (value != null && !value.equals(""))
	    		p.put(Literals.ITEM_WIDTH + label, value);
	    }
		catch (Exception e) {
			// Double.valueOf() didn't work because there was no value
		}
    	
    	return p;
    }
    
	//--------------------------------------------------------------------------------------------------------------------
	//-- END VISITABLE GETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
    
    
    //--------------------------------------------------------------------------------------------------------------------
  	//-- VISITABLE SETTERS																										--
  	//--------------------------------------------------------------------------------------------------------------------
    
    public void setVisitable(int index, String visitable) {
    	setPropertyValue(Literals.VISITABLE + index, visitable);
    }
    
    public void setVisitableVertexLabel(String label, String itemLabel) {
        setPropertyValue(Literals.VERTEX_LABEL + label, itemLabel);
    }
    
    /*
    public void setVisitableItemID(String label, int id) {
        setPropertyValue(Literals.ITEM_ID + label, Integer.toString(id));
    }
    
    
    public void setVisitableItemTitle(int position, String title) {
        setPropertyValue(Literals.ITEM_TITLE + position, title);
    }
    */
    
    public void setVisitableVertexURL(String label, String url) {
        setPropertyValue(Literals.VERTEX_URL + label, url);
    }
    
    public void setVisitableItemNationality(String label, String nationality) {
        setPropertyValue(Literals.ITEM_NATIONALITY + label, nationality);
    }

    public void setVisitableItemBeginDate(String label, String beginDate) {
        setPropertyValue(Literals.ITEM_BEGINDATE + label, beginDate);
    }

    public void setVisitableEndDate(String label, String endDate) {
        setPropertyValue(Literals.ITEM_ENDDATE + label, endDate);
    }
    
    public void setVisitableItemDate(String label, String date) {
        setPropertyValue(Literals.ITEM_DATE + label, date);
    }
    
    public void setVisitableItemDepartment(String label, String department) {
        setPropertyValue(Literals.ITEM_DEPARTMENT + label, department);
    }
    
    public void setVisitableItemHeight(String label, double height) {
        setPropertyValue(Literals.ITEM_HEIGHT + label, Double.toString(height));
    }
    
    public void setVisitableItemWidth(String label, double width) {
        setPropertyValue(Literals.ITEM_WIDTH + label, Double.toString(width));
    }
    
    public void setVisitableProperties(int index, String visitable, Properties prop) {
    	
	    setVisitable(index, visitable);
	    properties.putAll(prop);
	    
    }
    
	//--------------------------------------------------------------------------------------------------------------------
	//-- END VISITABLE SETTERS																									--
	//--------------------------------------------------------------------------------------------------------------------
    
}
