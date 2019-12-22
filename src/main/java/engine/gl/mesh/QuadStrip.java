/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

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
