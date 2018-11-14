package engine.gl.light;

import org.joml.Vector3f;

public class PointLight extends Light {
	public float radius = 64;
	public float intensity = 1;
	
	public PointLight(Vector3f position, float radius, float intensity) {
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
		this.radius = radius;
		this.intensity = intensity;
	}
}
