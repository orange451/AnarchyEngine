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

import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.v2.lights.SpotLightCamera;

public class AnimInstanceSpotShadowShader extends AnimInstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");

	public AnimInstanceSpotShadowShader() {
		super("assets/shaders/renderers/AnimInstanceSpotShadow.vs", "assets/shaders/renderers/InstanceShadow.fs",
				new Attribute(0, "position"), new Attribute(1, "normals"), new Attribute(2, "textureCoords"),
				new Attribute(3, "inColor"), new Attribute(4, "boneIndices"), new Attribute(5, "boneWeights"));
		super.storeUniforms(projectionMatrix);
		super.validate();
	}

	public void loadSpotLight(SpotLightCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
	}

}
