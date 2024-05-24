package es.unizar.util;

public class PredictedRatingsInfo {
	
	private long id_item;
	private float rating;
	private float ratingPredicted;
	private int time;
	
	public PredictedRatingsInfo(long id_item, float ratingPredicted, int time) {
		super();
		this.id_item = id_item;
		this.ratingPredicted = ratingPredicted;
		this.time = time;
	}

	public PredictedRatingsInfo(long id_item, float rating, float ratingPredicted, int time) {
		super();
		this.id_item = id_item;
		this.rating = rating;
		this.ratingPredicted = ratingPredicted;
		this.time = time;
	}

	public long getId_item() {
		return id_item;
	}

	public void setId_item(long id_item) {
		this.id_item = id_item;
	}

	public float getRating() {
		return rating;
	}

	public void setRating(float rating) {
		this.rating = rating;
	}

	public float getRatingPredicted() {
		return ratingPredicted;
	}

	public void setRatingPredicted(float ratingPredicted) {
		this.ratingPredicted = ratingPredicted;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id_item ^ (id_item >>> 32));
		result = prime * result + Float.floatToIntBits(rating);
		result = prime * result + Float.floatToIntBits(ratingPredicted);
		result = prime * result + time;
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
		PredictedRatingsInfo other = (PredictedRatingsInfo) obj;
		if (id_item != other.id_item)
			return false;
		if (Float.floatToIntBits(rating) != Float.floatToIntBits(other.rating))
			return false;
		if (Float.floatToIntBits(ratingPredicted) != Float.floatToIntBits(other.ratingPredicted))
			return false;
		if (time != other.time)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PredictedRatingsInfo [id_item=" + id_item + ", rating=" + rating + ", ratingPredicted="
				+ ratingPredicted + ", time=" + time + "]";
	}
}
