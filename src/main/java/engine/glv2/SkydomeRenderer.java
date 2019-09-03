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

package engine.glv2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL20C.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.RawModel;
import engine.glv2.shaders.SkydomeShader;
import engine.lua.type.object.insts.Camera;

public class SkydomeRenderer {

	private RawModel dome;
	private SkydomeShader shader;
	private Vector3f pos;

	private Matrix4f infMat, regMat;

	public SkydomeRenderer(GLResourceLoader loader) {
		dome = loader.loadObjModel("SkyDome");
		pos = new Vector3f();
		infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
		regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		shader = new SkydomeShader();
	}

	public void render(Camera camera, Matrix4f projection, Vector3f lightPosition, boolean renderSun,
			boolean infScale) {
		glCullFace(GL_FRONT);
		shader.start();
		shader.loadCamera(camera, projection);
		shader.loadTime(0);
		shader.loadLightPosition(lightPosition);
		shader.renderSun(renderSun);
		if (infScale)
			shader.loadTransformationMatrix(infMat);
		else
			shader.loadTransformationMatrix(regMat);
		glBindVertexArray(dome.getVaoID());
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glDrawElements(GL_TRIANGLES, dome.getVertexCount(), GL_UNSIGNED_INT, 0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindVertexArray(0);
		shader.stop();
		glCullFace(GL_BACK);
	}

	public void render(CubeMapCamera camera, Vector3f lightPosition, boolean renderSun, boolean infScale) {
		glCullFace(GL_FRONT);
		shader.start();
		shader.loadCamera(camera);
		shader.loadTime(0);
		shader.loadLightPosition(lightPosition);
		shader.renderSun(renderSun);
		if (infScale)
			shader.loadTransformationMatrix(infMat);
		else
			shader.loadTransformationMatrix(regMat);
		glBindVertexArray(dome.getVaoID());
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glDrawElements(GL_TRIANGLES, dome.getVertexCount(), GL_UNSIGNED_INT, 0);
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindVertexArray(0);
		shader.stop();
		glCullFace(GL_BACK);
	}

}
