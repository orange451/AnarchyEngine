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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UniformObject implements IUniform {

	private List<Uniform> uniforms = new ArrayList<>();;

	public void storeUniforms(Uniform... uniforms) {
		this.uniforms.addAll(Arrays.asList(uniforms));
	}

	@Override
	public void storeUniformLocation(int programID) {
		for (Uniform uniform : uniforms) 
			uniform.storeUniformLocation(programID);
	}

	@Override
	public void dispose() {

	}

}
