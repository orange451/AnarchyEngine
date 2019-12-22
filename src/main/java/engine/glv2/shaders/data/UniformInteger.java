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

import static org.lwjgl.opengl.GL20C.glUniform1i;

public class UniformInteger extends Uniform {

	private int currentValue;

	public UniformInteger(String name) {
		super(name);
	}

	public void loadInteger(int value) {
		if (!used || currentValue != value) {
			glUniform1i(super.getLocation(), value);
			used = true;
			currentValue = value;
		}
	}

}
