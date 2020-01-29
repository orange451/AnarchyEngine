/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers.shaders;

import java.nio.FloatBuffer;

import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformMatrix4;

public class AnimInstanceBaseShadowShader extends InstanceBaseShadowShader {

	private UniformMatrix4 boneMat = new UniformMatrix4("boneMat");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.setAttributes(new Attribute(0, "position"), new Attribute(1, "normals"), new Attribute(2, "tangent"),
				new Attribute(3, "textureCoords"), new Attribute(4, "inColor"), new Attribute(5, "boneIndices"),
				new Attribute(6, "boneWeights"));
		super.storeUniforms(boneMat);
	}

	public void loadBoneMat(FloatBuffer mat) {
		boneMat.loadMatrix(mat);
	}

}
