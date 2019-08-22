package engine.gl.mesh.animation;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Point {
	private Matrix4f offsetMatrix;
	private Matrix4f absoluteMatrix;
	private String name;
	private Bone parent;

	public Point( String name, Bone parent, Vector3f position, Vector3f rotation ) {
		this.name = name;
		this.parent = parent;

		this.offsetMatrix = new Matrix4f();
		this.offsetMatrix.translate( position );
		//this.offsetMatrix.rotate(rotation.z, new Vector3f(0, 0, 1));
		//this.offsetMatrix.rotate(rotation.y, new Vector3f(0, 1, 0));
		//this.offsetMatrix.rotate(rotation.x, new Vector3f(1, 0, 0));
		this.offsetMatrix.rotate(rotation.x, new Vector3f(1, 0, 0));
		this.offsetMatrix.rotate(rotation.y, new Vector3f(0, 1, 0));
		this.offsetMatrix.rotate(rotation.z, new Vector3f(0, 0, 1));
	}

	public Bone getParent() {
		return this.parent;
	}

	public Matrix4f getOffsetMatrix() {
		return offsetMatrix;
	}

	public void setAbsoluteMatrix(Matrix4f pointAbsoluteMatrix) {
		this.absoluteMatrix = pointAbsoluteMatrix;
	}

	public Matrix4f getAbsoluteMatrix() {
		return this.absoluteMatrix;
	}

	public String getName() {
		return this.name;
	}
}
