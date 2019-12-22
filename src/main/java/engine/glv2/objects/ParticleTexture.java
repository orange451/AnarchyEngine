/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.objects;

public class ParticleTexture {

	private int numbreOfRows;
	private int id;

	public ParticleTexture(int id, int numbreOfRows) {
		this.id = id;
		this.numbreOfRows = numbreOfRows;
	}
	
	public int getID() {
		return id;
	}

	public int getNumbreOfRows() {
		return numbreOfRows;
	}

}
