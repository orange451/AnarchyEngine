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

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE10;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE9;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

import java.util.List;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.shaders.EntityFowardShader;
import engine.glv2.v2.IRenderingData;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;

public class EntityForwardRenderer {

	private EntityFowardShader shader;

	public EntityForwardRenderer() {
		shader = new EntityFowardShader();
	}

	public void render(List<Instance> instances, IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera,
			boolean colorCorrect, boolean transparentOnly) {
		shader.start();
		if (cubeCamera == null) // TODO: Improve
			shader.loadCamera(rd.camera, rd.projectionMatrix);
		else
			shader.loadCamera(cubeCamera);
		shader.loadLightPosition(rd.sun.getSunPosition());
		shader.colorCorrect(colorCorrect);
		shader.loadSettings(true);
		shader.loadBiasMatrix(rd.sun.getCamera().getProjectionArray());
		shader.loadLightMatrix(rd.sun.getCamera().getViewMatrix());
		shader.loadExposure(rnd.exposure);
		shader.loadGamma(rnd.gamma);
		shader.loadAmbient(rnd.ambient);
		glActiveTexture(GL_TEXTURE4);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.irradianceCapture.getTexture());
		glActiveTexture(GL_TEXTURE5);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.environmentMap.getTexture());
		glActiveTexture(GL_TEXTURE6);
		glBindTexture(GL_TEXTURE_2D, rnd.brdfLUT.getTexture());
		glActiveTexture(GL_TEXTURE7);
		glBindTexture(GL_TEXTURE_2D, rnd.dlsm.getShadowMaps()[0].getTexture());
		glActiveTexture(GL_TEXTURE8);
		glBindTexture(GL_TEXTURE_2D, rnd.dlsm.getShadowMaps()[1].getTexture());
		glActiveTexture(GL_TEXTURE9);
		glBindTexture(GL_TEXTURE_2D, rnd.dlsm.getShadowMaps()[2].getTexture());
		glActiveTexture(GL_TEXTURE10);
		glBindTexture(GL_TEXTURE_2D, rnd.dlsm.getShadowMaps()[3].getTexture());
		for (Instance instance : instances) {
			renderInstance(instance, transparentOnly);
		}
		shader.stop();
	}

	private void renderInstance(Instance inst, boolean transparentOnly) {
		GameObject go = (GameObject) inst;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab().isnil())
			return;
		PrefabRenderer pfr = go.getPrefab().getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		mat.scale(pfr.getParent().getScale());
		shader.loadTransformationMatrix(mat);
		for (int i = 0; i < pfr.size(); i++) {
			Model p = pfr.getModel(i);
			BufferedMesh m = p.getMesh();

			if (m == null)
				m = Resources.MESH_SPHERE;

			engine.gl.MaterialGL material = Resources.MATERIAL_BLANK;
			Material ECSMat = p.getMaterial();
			if (ECSMat != null) {
				MaterialGL GLMat = ECSMat.getMaterial();
				if (GLMat != null) {
					material = GLMat;
				}
			}
			float iMatTrans = 1.0f - material.getTransparency();
			float iObjTrans = 1.0f - go.getTransparency();
			float trans = iMatTrans * iObjTrans;
			if (trans == 1.0 && transparentOnly)
				continue;

			prepareMaterial(material);
			shader.loadMaterial(material);
			shader.loadTransparency(trans);
			m.render(null, null, null);
		}
	}

	private void prepareMaterial(engine.gl.MaterialGL mat) {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, mat.getDiffuseTexture().getID());
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, mat.getNormalTexture().getID());
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, mat.getMetalnessTexture().getID());
		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GL_TEXTURE_2D, mat.getRoughnessTexture().getID());
	}

	public void dispose() {
		shader.dispose();
	}

}
