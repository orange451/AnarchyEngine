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

package engine.glv2.pipeline.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformSampler;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class MotionBlurShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");
	private UniformSampler depth = new UniformSampler("depth");
	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	private UniformVec3 previousCameraPosition = new UniformVec3("previousCameraPosition");
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");
	private UniformMatrix4 previousViewMatrix = new UniformMatrix4("previousViewMatrix");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	public MotionBlurShader(String name) {
		super("postprocess/" + name);
		super.storeUniforms(image, depth, cameraPosition, previousCameraPosition, projectionMatrix,
				inverseProjectionMatrix, inverseViewMatrix, previousViewMatrix);
		super.validate();
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		depth.loadTexUnit(1);
		super.stop();
	}

	public void loadMotionBlurData(Camera camera, Matrix4f projectionMatrix, Matrix4f previousViewMatrix) {
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.projectionMatrix.loadMatrix(projectionMatrix);
		this.inverseProjectionMatrix.loadMatrix(projectionMatrix.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrix().getInternal().invert(viewInv));
		this.previousViewMatrix.loadMatrix(previousViewMatrix);
	}

}
