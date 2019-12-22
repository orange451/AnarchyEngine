/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import java.nio.FloatBuffer;

import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformMatrix4;

public class AnimInstanceDeferredShader extends InstanceDeferredShader {

	private UniformMatrix4 boneMat = new UniformMatrix4("boneMat");
	private UniformMatrix4 boneMatPrev = new UniformMatrix4("boneMatPrev");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/AnimInstanceDeferred.vs", GL_VERTEX_SHADER));
		super.setAttributes(new Attribute(0, "position"), new Attribute(1, "normals"),
				new Attribute(2, "textureCoords"), new Attribute(3, "inColor"), new Attribute(4, "boneIndices"),
				new Attribute(5, "boneWeights"));
		super.storeUniforms(boneMat, boneMatPrev);
	}

	public void loadBoneMat(FloatBuffer mat) {
		boneMat.loadMatrix(mat);
	}

	public void loadBoneMatPrev(FloatBuffer mat) {
		boneMatPrev.loadMatrix(mat);
	}

}