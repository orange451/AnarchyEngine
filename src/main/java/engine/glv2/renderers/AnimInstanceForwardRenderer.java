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

package engine.glv2.renderers;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.light.PointLightInternal;
import engine.gl.mesh.animation.AnimatedModel;
import engine.gl.mesh.animation.AnimatedModelSubMesh;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.renderers.shaders.AnimInstanceFowardShader;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.RendererData;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;

public class AnimInstanceForwardRenderer {

	private AnimInstanceFowardShader shader;

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	public AnimInstanceForwardRenderer() {
		shader = new AnimInstanceFowardShader();
		shader.init();
	}

	public void render(List<Instance> instances, IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera,
			boolean colorCorrect, boolean transparentOnly) {
		shader.start();
		if (cubeCamera == null) // TODO: Improve
			shader.loadCamera(rd.camera, rd.projectionMatrix);
		//else
		//	shader.loadCamera(cubeCamera);
		shader.colorCorrect(colorCorrect);
		shader.loadSettings(true);
		shader.loadExposure(rnd.exposure);
		shader.loadGamma(rnd.gamma);
		shader.loadDirectionalLights(rnd.dlh.getLights());
		glActiveTexture(GL_TEXTURE4);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.irradianceCapture.getTexture());
		glActiveTexture(GL_TEXTURE5);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.environmentMap.getTexture());
		glActiveTexture(GL_TEXTURE6);
		glBindTexture(GL_TEXTURE_2D, rnd.brdfLUT.getTexture());
		synchronized (rnd.dlh.getLights()) {
			for (int x = 0; x < Math.min(8, rnd.dlh.getLights().size()); x++) {
				glActiveTexture(GL_TEXTURE8 + x);
				glBindTexture(GL_TEXTURE_2D_ARRAY,
						rnd.dlh.getLights().get(x).getShadowMap().getShadowMaps().getTexture());
			}
		}
		for (Instance instance : instances) {
			renderInstance(instance, rnd, transparentOnly);
		}
		shader.stop();
	}

	private void renderInstance(Instance inst, RendererData rnd, boolean transparentOnly) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		if ( anim == null )
			return;
		
		GameObject go = anim.getLinkedInstance();
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		AnimatedModel model = anim.getAnimatedModel();
		PrefabRenderer pfr = go.getPrefab().getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		mat.translate(pfr.getAABBOffset());
		mat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrix(mat);
		shader.loadBoneMat(model.getBoneBuffer());

		Vector3f gop = go.getPosition().toJoml();

		List<PointLightInternal> pl = new ArrayList<>();
		synchronized (rnd.plh.getLights()) {
			for (PointLightInternal p : rnd.plh.getLights()) {
				if (p.getPosition().distance(gop) < p.radius)
					pl.add(p);
			}
		}
		pl = pl.subList(0, Math.min(8, pl.size()));
		Collections.sort(pl, new Comparator<PointLightInternal>() {
			@Override
			public int compare(PointLightInternal o1, PointLightInternal o2) {
				float d1 = o1.getPosition().distanceSquared(gop);
				float d2 = o1.getPosition().distanceSquared(gop);
				return (int) (d2 - d1);
			}
		});

		shader.loadPointLights(pl);
		for (int i = 0; i < model.getMeshes().size(); i++) {
			AnimatedModelSubMesh mesh = model.getMeshes().get(i);

			engine.gl.MaterialGL material = Resources.MATERIAL_BLANK;
			Material ECSMat = model.getMeshToModelMap().get(mesh).getMaterial();
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
			mesh.bind();
			glDrawArrays(GL_TRIANGLES, 0, mesh.size());
			mesh.unbind();
		}
	}

	private void prepareMaterial(engine.gl.MaterialGL mat) {
		if (mat.getDiffuseTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, mat.getDiffuseTexture().getID());
		}
		if (mat.getNormalTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, mat.getNormalTexture().getID());
		}
		if (mat.getMetalnessTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, mat.getMetalnessTexture().getID());
		}
		if (mat.getRoughnessTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GL_TEXTURE_2D, mat.getRoughnessTexture().getID());
		}
	}

	public void dispose() {
		shader.dispose();
	}

}
