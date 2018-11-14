package engine.gl.mesh;

import java.util.ArrayList;

public class QuadStrip {
	private ArrayList<Quad> quads = new ArrayList<Quad>();
	private Quad currentQuad = new Quad();

	public void addVertex( Vertex vert ) {
		if ( currentQuad.isFull() ) {
			Quad lastQuad = currentQuad;
			quads.add(lastQuad);
			currentQuad = new Quad();
			currentQuad.addVertex(lastQuad.getVertex(2).clone());
			currentQuad.addVertex(lastQuad.getVertex(3).clone());
		}

		currentQuad.addVertex(vert.clone());
	}

	public void snip() {
		if ( currentQuad.isFull() ) {
			quads.add( currentQuad );
			currentQuad = new Quad();
		}
	}

	public ArrayList<Quad> getQuads() {
		snip();
		return quads;
	}
}
