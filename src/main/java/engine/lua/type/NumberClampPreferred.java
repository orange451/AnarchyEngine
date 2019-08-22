package engine.lua.type;

public class NumberClampPreferred extends NumberClamp {
	private float preferredMin;
	private float preferredMax;
	
	public NumberClampPreferred(float min, float max, float preferredMin, float preferredMax) {
		super(min,max);
		
		this.preferredMin = preferredMin;
		this.preferredMax = preferredMax;
	}
	
	public float getPreferredMin() {
		return this.preferredMin;
	}
	
	public float getPreferredMax() {
		return this.preferredMax;
	}
}
