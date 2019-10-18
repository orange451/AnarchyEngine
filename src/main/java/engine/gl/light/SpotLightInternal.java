package engine.gl.light;

import org.joml.Vector3f;

public class SpotLightInternal extends Light {
	public float outerFOV = 90;
	public float innerFOV = 70;
	public float radius = 10;
	public Vector3f direction = new Vector3f(1, 1, 1);
	
	public SpotLightInternal(Vector3f direction,Vector3f position, float outerFOV, float innerFOV, float radius, float intensity) {
		this.direction.set(direction);
		this.setPosition(position);
		this.outerFOV = outerFOV;
		this.innerFOV = innerFOV;
		this.radius= radius;
		this.intensity = intensity;
	}
}
