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
