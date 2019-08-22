package engine.util;

public class Pair<T1, T2> {
	private T1 value1;
	private T2 value2;
	
	public Pair(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public T1 value1() {
		return value1;
	}

	public T2 value2() {
		return value2;
	}

	
	@Override
	public String toString() {
		return "(" + value1 + ", " + value2 + ")";
	}
}
