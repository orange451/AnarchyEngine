package engine.gl.light;

import org.joml.Vector3f;

public class PointLightInternal extends Light {
	public float radius = 64;
	
	public PointLightInternal(Vector3f position, float radius, float intensity) {
		this.position = position;
		this.radius = radius;
		this.intensity = intensity;
	}
}
