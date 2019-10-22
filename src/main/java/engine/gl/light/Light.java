package engine.gl.light;

import org.joml.Vector3f;

public abstract class Light {
	public Vector3f position = new Vector3f();
	public float intensity = 1;
	public boolean visible = true;
	public Vector3f color = new Vector3f(1, 1, 1);

	public Vector3f getPosition() {
		return position;
	}

	public void setPosition(Vector3f position) {
		this.position.set(position);
	}
}
