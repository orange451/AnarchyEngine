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

public class UniformBoolean extends Uniform {

	private boolean currentBool;

	public UniformBoolean(String name) {
		super(name);
	}

	public void loadBoolean(boolean bool) {
		if (!used || currentBool != bool) {
			glUniform1i(super.getLocation(), bool ? 1 : 0);
			used = true;
			currentBool = bool;
		}
	}

}
