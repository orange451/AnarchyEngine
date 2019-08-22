package engine.gl.mesh.mm3d;

public class Mm3dWeightedInfluence {
	private int POS_TYPE;
	private int POS_INDEX;
	private int INF_INDEX;
	private int INF_TYPE;
	private int INF_WEIGHT;
	
	public Mm3dWeightedInfluence(int vtype, long vIndex, long jIndex, int type, int weight) {
		this.POS_TYPE = vtype;
		this.POS_INDEX = (int) vIndex;
		this.INF_INDEX = (int) jIndex;
		this.INF_TYPE = type;
		this.INF_WEIGHT = weight;
	}
	
	/**
	 * Returns the type of vertex this influence if for.<br><br>0 = Vertex<br>2 = Point
	 * @return
	 */
	public int getVertexType() {
		return this.POS_TYPE;
	}
	
	/**
	 * Returns the index position of the vertex
	 * @return
	 */
	public int getVertexIndex() {
		return this.POS_INDEX;
	}
	
	/**
	 * Returns the index position of the joint
	 * @return
	 */
	public int getJointIndex() {
		return this.INF_INDEX;
	}
	
	/**
	 * Returns the Influence type<br><br>0 = Custom<br>1 = Automatic<br>2 = Remainder
	 * @return
	 */
	public int getInfluenceType() {
		return this.INF_TYPE;
	}
	
	/**
	 * Returns weight of influence (0-100)
	 * @return
	 */
	public int getWeight() {
		return this.INF_WEIGHT;
	}

	public void setWeight(float f) {
		this.INF_WEIGHT = (int) f;
	}

}
