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

package engine.glv2.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class AmbientSkyShader extends ShaderProgram {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");

	private UniformVec3 ambient = new UniformVec3("ambient");

	private UniformMatrix4 viewMatrixPrev = new UniformMatrix4("viewMatrixPrev");
	private UniformMatrix4 projectionMatrixPrev = new UniformMatrix4("projectionMatrixPrev");

	private Matrix4f temp = new Matrix4f();

	public AmbientSkyShader() {
		super("assets/shaders/sky/Ambient.vs", "assets/shaders/sky/Ambient.fs", new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, transformationMatrix, viewMatrix, ambient, viewMatrixPrev,
				projectionMatrixPrev);
		super.validate();
	}

	public void loadCamera(Camera camera, Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
		temp.set(camera.getViewMatrix().getInternal());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
	}

	public void loadCamera(CubeMapCamera camera) {
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		temp.set(camera.getViewMatrix());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
	}

	public void loadCameraPrev(Matrix4f viewMatrixPrev, Matrix4f projectionMatrixPrev) {
		temp.set(viewMatrixPrev);
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		this.viewMatrixPrev.loadMatrix(temp);
		this.projectionMatrixPrev.loadMatrix(projectionMatrixPrev);
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadAmbient(Vector3f ambient) {
		this.ambient.loadVec3(ambient);
	}

}
