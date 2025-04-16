package es.unizar.gui.simulation;

/**
 * Information to propagate between the users.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public class InformationToPropagate {

	private long id_user;
	private long user;
	private long item;
	private long context;
	private double rating;
	private int TTL;
	private double TTP; // Time To Propagate
	private int isTTPInitialized;
	private String location;
	private long currentTime;

	public InformationToPropagate(long id_user, long user, long item, long context, double rating, int TTL, String location) {
		this.setId_user(id_user);
		this.setUser(user);
		this.setItem(item);
		this.setContext(context);
		this.setRating(rating);
		this.setTTL(TTL);
		this.setTTP(0);
		this.setIsTTPInitialized(0);
		this.setLocation(location);
		this.setCurrentTime(0);
	}

	public InformationToPropagate(InformationToPropagate information) {
		this.id_user = information.getId_user();
		this.user = information.getUser();
		this.item = information.getItem();
		this.context = information.getContext();
		this.rating = information.getRating();
		this.TTL = information.getTTL();
		this.TTP = information.getTTP();
		this.isTTPInitialized = information.isIsTTPInitialized();
		this.location = information.getLocation();
		this.currentTime = information.getCurrentTime();
	}

	public InformationToPropagate(long id_user, long user, long item, long context, double rating, int TTL, String location, long currentTime) {
		this.setId_user(id_user);
		this.setUser(user);
		this.setItem(item);
		this.setContext(context);
		this.setRating(rating);
		this.setTTL(TTL);
		this.setTTP(0);
		this.setIsTTPInitialized(0);
		this.setLocation(location);
		this.setCurrentTime(currentTime);
	}

	public InformationToPropagate(long id_user, long user, long item, long context, double rating, int TTL, double TTP, int isTTPInitialized, String location, long currentTime) {
		this.setId_user(id_user);
		this.setUser(user);
		this.setItem(item);
		this.setContext(context);
		this.setRating(rating);
		this.setTTL(TTL);
		this.setTTP(TTP);
		this.setIsTTPInitialized(isTTPInitialized);
		this.setLocation(location);
		this.setCurrentTime(currentTime);
	}

	public long getItem() {
		return item;
	}

	public double getRating() {
		return rating;
	}

	public int getTTL() {
		return TTL;
	}

	public double getTTP() {
		return TTP;
	}

	public long getContext() {
		return context;
	}

	public long getUser() {
		return user;
	}

	public void setUser(long user) {
		this.user = user;
	}

	public int isIsTTPInitialized() {
		return isTTPInitialized;
	}

	public void setItem(long item) {
		this.item = item;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public void setTTL(int TTL) {
		this.TTL = TTL;
	}

	public void setTTP(double TTP) {
		this.TTP = TTP;
		isTTPInitialized = 1;
	}

	public void setContext(long context) {
		this.context = context;
	}

	public void setIsTTPInitialized(int isTTPInitialized) {
		this.isTTPInitialized = isTTPInitialized;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public long getId_user() {
		return id_user;
	}

	public void setId_user(long id_user) {
		this.id_user = id_user;
	}

	@Override
	public String toString() {
		return "[id_user: " + id_user + ", user: " + user + ", item: " + item + ", context: " + context + ", rating: " + rating + ", TTL: " + TTL + ", TTP: " + TTP + ", isTTPInitialized: " + isTTPInitialized + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (context ^ (context >>> 32));
		result = prime * result + (int) (id_user ^ (id_user >>> 32));
		result = prime * result + (int) (item ^ (item >>> 32));
		long temp;
		temp = Double.doubleToLongBits(rating);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (user ^ (user >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InformationToPropagate other = (InformationToPropagate) obj;
		if (context != other.context)
			return false;
		if (id_user != other.id_user)
			return false;
		if (item != other.item)
			return false;
		if (Double.doubleToLongBits(rating) != Double.doubleToLongBits(other.rating))
			return false;
		if (user != other.user)
			return false;
		return true;
	}
	
	
}
