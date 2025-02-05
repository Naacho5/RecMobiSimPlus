package es.unizar.util;

public class Pair<F,S> {
	
    private F f; // First element
    private S s; // Second element
    
    public Pair(F f, S s){
        this.f = f;
        this.s = s;
    }

	public F getF() {
		return f;
	}

	public void setF(F f) {
		this.f = f;
	}

	public S getS() {
		return s;
	}

	public void setS(S s) {
		this.s = s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		result = prime * result + ((s == null) ? 0 : s.hashCode());
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
		Pair other = (Pair) obj;
		if (f == null) {
			if (other.f != null)
				return false;
		}
		if (s == null) {
			if (other.s != null)
				return false;
		}
		
		// Compare first with first and second with second
		if (f.getClass() == other.getF().getClass()
				&& s.getClass() == other.getS().getClass()
				&& f.equals(other.getF()) && s.equals(other.getS()))
			return true;
		
		// Compare first with second and second with first
		if (f.getClass() == other.getS().getClass()
				&& s.getClass() == other.getF().getClass()
				&& f.equals(other.getS()) && s.equals(other.getF()))
			return true;
		
		return false;
	}

	@Override
	public String toString() {
		return "Pair [f=" + f + ", s=" + s + "]";
	}
    
}
