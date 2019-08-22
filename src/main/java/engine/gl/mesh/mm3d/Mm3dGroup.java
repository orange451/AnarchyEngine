package engine.gl.mesh.mm3d;

import java.nio.ByteBuffer;

public class Mm3dGroup {
	private int FLAGS;
	private String NAME;
	private int TRIANGLES;
	private int[] TRIANGLE_INDICES;
	private int SMOOTHNESS;
	private int MATERIAL;
	
	public Mm3dGroup(int a, String b, int c, int[] d, int e, int f) {
		this.FLAGS = a;
		this.NAME = b;
		this.TRIANGLES = c;
		this.TRIANGLE_INDICES = d;
		this.SMOOTHNESS = e;
		this.MATERIAL = f;
	}
	
	public String getName() {
		return this.NAME;
	}
	
	public int getTriangles() {
		return this.TRIANGLES;
	}
	
	public int[] getIndices() {
		return this.TRIANGLE_INDICES;
	}
	
	public int getMaterial() {
		return this.MATERIAL;
	}

}
