package es.unizar.editor.model;

import es.unizar.util.Literals;

public class Item extends Drawable{
	
	public enum ItemGender {
		Male, Female, Other;
	}
	
	private String itemLabel; // Type of item
	
	private String title;
	/*
	private String artist;
	private int constituentID;
	private String artistBio;
	*/
	private String nationality;
	private String beginDate; // year. Ex: 1999
	private String endDate; // year. Ex: 1999
	//private ItemGender gender;
	private String date; // period in history. Ex: EarlyCenturyXX
	/*
	private String medium; // Medium of creation. Ex: Oil
	private String dimensions; //Ex: "7' 1"" x 31"" (216 x 78.8 cm)"
	private String creditLine; // Ex: Gift of A. Conger Goodyear
	private int accessionNumber; // Ex: 3.841.955
	*/
	//private String department; // Ex: Painting & Sculpture
	/*
	private String dateAcquired; // Ex: LongTime
	private boolean cataloged; // Ex: Y/N
	private double depth; // Ex: 20.32
	private double diameter; // Ex: 609.601
	*/
	private double height; // Ex: 215.9
	//private double weight; // Ex: 109.2
	private double width; // Ex: 78.7
	
	/**
	 * Constructor of item must have compulsory data:
	 * 
	 * @param room
	 * @param vertex_label
	 * @param vertex_xy
	 */
	public Item(Room room, long vertex_label, Point vertex_xy) {
		super(room, vertex_label, vertex_xy);
		this.setUrlIcon(Literals.IMAGES_PATH + "museum_logo.png");
	}
	
	/**
	 * Item's constructor with url (item's image).
	 * 
	 * @param room
	 * @param vertex_label
	 * @param vertex_xy
	 * @param url
	 */
	public Item(Room room, long vertex_label, Point vertex_xy, String url) {
		super(room, vertex_label, vertex_xy);
		this.setUrlIcon(url);
	}
	
	public String getItemLabel() {
		return itemLabel;
	}

	public void setItemLabel(String itemLabel) {
		this.itemLabel = itemLabel;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/*
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public int getConstituentID() {
		return constituentID;
	}

	public void setConstituentID(int constituentID) {
		this.constituentID = constituentID;
	}

	public String getArtistBio() {
		return artistBio;
	}

	public void setArtistBio(String artistBio) {
		this.artistBio = artistBio;
	}
	*/

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/*
	public ItemGender getGender() {
		return gender;
	}

	public void setGender(ItemGender gender) {
		this.gender = gender;
	}
	*/

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	/*
	public String getMedium() {
		return medium;
	}

	public void setMedium(String medium) {
		this.medium = medium;
	}

	public String getDimensions() {
		return dimensions;
	}

	public void setDimensions(String dimensions) {
		this.dimensions = dimensions;
	}

	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}

	public int getAccessionNumber() {
		return accessionNumber;
	}

	public void setAccessionNumber(int accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getDateAcquired() {
		return dateAcquired;
	}

	public void setDateAcquired(String dateAcquired) {
		this.dateAcquired = dateAcquired;
	}

	public boolean isCataloged() {
		return cataloged;
	}

	public void setCataloged(boolean cataloged) {
		this.cataloged = cataloged;
	}

	public double getDepth() {
		return depth;
	}

	public void setDepth(double depth) {
		this.depth = depth;
	}

	public double getDiameter() {
		return diameter;
	}

	public void setDiameter(double diameter) {
		this.diameter = diameter;
	}
	*/

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	/*
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	*/

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((beginDate == null) ? 0 : beginDate.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		long temp;
		temp = Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((itemLabel == null) ? 0 : itemLabel.hashCode());
		result = prime * result + ((nationality == null) ? 0 : nationality.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		temp = Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (beginDate == null) {
			if (other.beginDate != null)
				return false;
		} else if (!beginDate.equals(other.beginDate))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (endDate == null) {
			if (other.endDate != null)
				return false;
		} else if (!endDate.equals(other.endDate))
			return false;
		if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
			return false;
		if (itemLabel == null) {
			if (other.itemLabel != null)
				return false;
		} else if (!itemLabel.equals(other.itemLabel))
			return false;
		if (nationality == null) {
			if (other.nationality != null)
				return false;
		} else if (!nationality.equals(other.nationality))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Item [itemLabel=" + itemLabel + ", title=" + title + ", nationality=" + nationality + ", beginDate="
				+ beginDate + ", endDate=" + endDate + ", date=" + date + ", height=" + height + ", width=" + width
				+ "]";
	}
	
}
