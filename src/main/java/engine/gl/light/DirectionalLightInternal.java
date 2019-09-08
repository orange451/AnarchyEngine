package engine.gl.light;

import org.joml.Vector3f;

public class DirectionalLightInternal extends Light {
	public Vector3f direction = new Vector3f(1,1,1);
	
	public DirectionalLightInternal(Vector3f direction, float intensity) {
		this.direction.set(direction);
		this.intensity = intensity;
	}
}
