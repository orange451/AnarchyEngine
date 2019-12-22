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

import static org.lwjgl.opengl.GL20C.glUniform1f;

public class UniformFloat extends Uniform {

	private float currentValue;

	public UniformFloat(String name) {
		super(name);
	}

	public void loadFloat(float value) {
		if (!used || currentValue != value) {
			glUniform1f(super.getLocation(), value);
			used = true;
			currentValue = value;
		}
	}

}
