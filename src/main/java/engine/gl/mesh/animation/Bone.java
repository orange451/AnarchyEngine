package engine.gl.mesh.animation;

import org.joml.Matrix4f;

public class Bone {
	private Matrix4f offsetMatrix = new Matrix4f();
	private Matrix4f absoluteMatrix = new Matrix4f();
	private Matrix4f bindMatrix = new Matrix4f();
	private String name;
	private int parent;
	
	public Bone( String name, int parent, Matrix4f offsetMatrix ) {
		this.name = name;
		this.parent = parent;
		this.offsetMatrix.set(offsetMatrix);
	}
	
	public void setAbsoluteMatrix(Matrix4f newMat) {
		this.absoluteMatrix.set(newMat);
	}
	
	public Matrix4f getAbsoluteMatrix() {
		return absoluteMatrix;
	}
	
	public void setBindMatrix(Matrix4f newMat) {
		this.bindMatrix.set(newMat);
	}

	public Matrix4f getBindMatrix() {
		return this.bindMatrix;
	}
	
	public int getParent() {
		return this.parent;
	}

	public Matrix4f getOffsetMatrix() {
		return this.offsetMatrix;
	}

	public String getName() {
		return this.name;
	}
}
