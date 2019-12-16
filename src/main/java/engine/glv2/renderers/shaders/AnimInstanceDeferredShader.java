/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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