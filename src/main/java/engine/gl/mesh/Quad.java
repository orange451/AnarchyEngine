package engine.gl.mesh;

public class Quad {
	private int verts = 0;
	private Vertex[] vertices = new Vertex[4];

	public void addVertex( Vertex vert ) {
		if ( verts > 3 )
			return;

		vertices[verts++] = vert;
	}

	public boolean isFull() {
		return verts == 4;
	}

	public Triangle[] triangulate() {
		if ( !isFull() ) {
			System.err.println("The quad is not full!");
			return null;
		}

		Triangle[] ret = new Triangle[2];

		ret[0] = new Triangle();
		ret[0].vertices[0] = vertices[0];
		ret[0].vertices[1] = vertices[1];
		ret[0].vertices[2] = vertices[2];

		ret[1] = new Triangle();
		ret[1].vertices[0] = vertices[1];
		ret[1].vertices[1] = vertices[3];
		ret[1].vertices[2] = vertices[2];

		return ret;
	}

	public Vertex getVertex(int i) {
		return vertices[i];
	}
}
