package engine.gl.light;

import org.joml.Vector3f;

public abstract class Light {
	public float x;
	public float y;
	public float z;
	public float intensity = 1;
	public boolean visible = true;
	public Vector3f color = new Vector3f(1, 1, 1);

	public Vector3f getPosition() {
		return new Vector3f(x, y, z);
	}

	public void setPosition(Vector3f position) {
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
	}
}
