package es.unizar.util;

public class Seed {
	
	private long seed = 0;
	
	public Seed() {
		this.seed = System.currentTimeMillis();
	}

	public Seed(long seed) {
		super();
		this.seed = seed;
	}

	public long getSeed() {
		return seed;
	}

	/*
	public void setSeed(long seed) {
		this.seed = seed;
	}
	*/

}
