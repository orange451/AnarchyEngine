/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.shaders.data;

import static org.lwjgl.opengl.GL20C.glGetUniformLocation;

public abstract class Uniform implements IUniform {

	protected String name;
	protected boolean used = false;
	private int location;

	protected Uniform(String name) {
		this.name = name;
	}

	@Override
	public void storeUniformLocation(int programID) {
		location = glGetUniformLocation(programID, name);
		used = false;
	}

	@Override
	public void dispose() {
	}

	protected int getLocation() {
		return location;
	}

}
