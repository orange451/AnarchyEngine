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
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.BasicPostProcessShader;
import engine.glv2.v2.PostProcesPass;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.RendererData;

public class FXAA extends PostProcesPass<BasicPostProcessShader> {

	public FXAA() {
		super("FXAA");
	}

	@Override
	protected BasicPostProcessShader setupShader() {
		return new BasicPostProcessShader(name) {
			@Override
			protected void setupShader() {
				super.setupShader();
				super.addShader(new Shader("assets/shaders/postprocess/" + name + ".vs", GL_VERTEX_SHADER));
			}
		};
	}

	@Override
	protected void setupTextures(RendererData rnd, PostProcessPipeline pp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, auxTex[0].getTexture());
	}

}
