package engine.gl.mesh.mm3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Mm3dJoint {
	private int flags; // Unused
	private String name;
	private int parent;
	private Matrix4f offsetMatrix;
	
	private Matrix4f absoluteMatrix;
	private Matrix4f bindMatrix;
	
	public Matrix4f customRotation;
	
	public Mm3dJoint(long flags, String name, long parent, float local_rot_x, float local_rot_y, float local_rot_z, float local_trans_x, float local_trans_y, float local_trans_z) {
		this.flags = (int) flags;
		this.name  = name;
		this.parent = (int) parent;
		
		/*System.out.println("  Loaded Joint:");
		System.out.println("    - Name:        " + name);
		System.out.println("    - Parent:      " + parent);
		System.out.println("    - Rotation:    " + local_rot_x + ", " + local_rot_y + ", " + local_rot_z);
		System.out.println("    - Translation: " + local_trans_x + ", " + local_trans_y + ", " + local_trans_z);*/
		
		this.offsetMatrix = new Matrix4f();
		this.offsetMatrix.translate(new Vector3f(local_trans_x, local_trans_y, local_trans_z));
		this.offsetMatrix.rotate(local_rot_z, new Vector3f(0, 0, 1));
		this.offsetMatrix.rotate(local_rot_y, new Vector3f(0, 1, 0));
		this.offsetMatrix.rotate(local_rot_x, new Vector3f(1, 0, 0));
		
		this.customRotation = new Matrix4f();
		
		this.absoluteMatrix = new Matrix4f();
		this.bindMatrix     = new Matrix4f();
	}

	public int getParent() {
		return this.parent;
	}

	public String getName() {
		return this.name;
	}
	
	public void setAbsoluteMatrix(Matrix4f newMat) {
		absoluteMatrix.set(newMat);
	}
	
	public Matrix4f getAbsoluteMatrix() {
		return absoluteMatrix;
	}

	public Matrix4f getOffsetMatrix() {
		return this.offsetMatrix;
	}

	public void setBindMatrix(Matrix4f newMat) {
		bindMatrix.set(newMat);
	}

	public Matrix4f getBindMatrix() {
		return this.bindMatrix;
	}
}
