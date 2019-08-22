package engine.gl.mesh;

import java.util.ArrayList;

public class TriangleFan {
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private Vertex firstVertex;
	private Vertex lastVertex;

	public void addVertex( Vertex vertex ) {
		if ( firstVertex == null ) {
			firstVertex = vertex;
			return;
		}
		if ( lastVertex == null ) {
			lastVertex = vertex;
			return;
		}

		Triangle t = new Triangle();
		t.addVertex( firstVertex.clone() );
		t.addVertex( lastVertex.clone() );
		t.addVertex( vertex.clone() );

		triangles.add(t);
		lastVertex = vertex;
	}

	public ArrayList<Triangle> getTriangles() {
		return triangles;
	}
}
