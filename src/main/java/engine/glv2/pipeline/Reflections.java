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

package engine.glv2.pipeline;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.ReflectionsShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.RendererData;

public class Reflections extends DeferredPass<ReflectionsShader> {

	public Reflections() {
		super("Reflections");
	}

	@Override
	protected ReflectionsShader setupShader() {
		return new ReflectionsShader(name);
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, ReflectionsShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getDiffuseTex().getTexture());
		//super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, dp.getPositionTex().getTexture());
		super.activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
		super.activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, dp.getDepthTex().getTexture());
		super.activateTexture(GL_TEXTURE4, GL_TEXTURE_2D, dp.getPbrTex().getTexture());
		super.activateTexture(GL_TEXTURE5, GL_TEXTURE_2D, dp.getMaskTex().getTexture());
		super.activateTexture(GL_TEXTURE6, GL_TEXTURE_CUBE_MAP, rnd.environmentMap.getTexture());
		super.activateTexture(GL_TEXTURE7, GL_TEXTURE_2D, rnd.brdfLUT.getTexture());
		super.activateTexture(GL_TEXTURE8, GL_TEXTURE_2D, auxTex[0].getTexture());
	}

}
