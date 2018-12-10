package engine.gl.light;

import org.joml.Vector3f;

public class Light {
	public float x;
	public float y;
	public float z;
	public float intensity = 1;
	public boolean visible = true;
	public Vector3f color = new Vector3f(1,1,1);
}
