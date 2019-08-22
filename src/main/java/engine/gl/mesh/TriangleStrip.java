package engine.gl.mesh;

import java.util.ArrayList;

public class TriangleStrip {
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private Triangle currentTriangle = new Triangle();

	public void addVertex( Vertex vert ) {
		if ( currentTriangle.isFull() ) {
			Triangle last = currentTriangle;
			triangles.add(last);
			currentTriangle = new Triangle();
			currentTriangle.addVertex(last.getVertex(1).clone());
			currentTriangle.addVertex(last.getVertex(2).clone());
		}

		currentTriangle.addVertex(vert.clone());
	}

	public void snip() {
		if ( currentTriangle.isFull() ) {
			triangles.add( currentTriangle );
			currentTriangle = new Triangle();
		}
	}

	public ArrayList<Triangle> getTriangles() {
		snip();
		return triangles;
	}
}
