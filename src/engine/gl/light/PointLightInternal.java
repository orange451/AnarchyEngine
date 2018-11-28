package engine.gl.light;

import org.joml.Vector3f;

public class PointLightInternal extends Light {
	public float radius = 64;
	public float intensity = 1;
	public Vector3f color = new Vector3f(1,1,1);
	
	public PointLightInternal(Vector3f position, float radius, float intensity) {
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
		this.radius = radius;
		this.intensity = intensity;
	}
}
