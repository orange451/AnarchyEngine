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

package engine.glv2.v2.lights;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY;

import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;

public class DirectionalLightShadowMap {

	private Framebuffer framebuffer;

	private Texture shadowMaps;

	private int size = 256;

	public DirectionalLightShadowMap(int size) {
		this.size = size;
		generateFramebuffer();
	}

	public void resize(int size) {
		this.size = size;
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void bind() {
		framebuffer.bind();
	}

	public void unbind() {
		framebuffer.unbind();
	}

	public void dispose() {
		disposeFramebuffer();
	}

	public void swapTexture(int index) {
		framebuffer.swapTextureLayer(GL_DEPTH_ATTACHMENT, shadowMaps, 0, index);
	}

	public Texture getShadowMaps() {
		return shadowMaps;
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D_ARRAY).bindTexture();
		tb.sizeTexture(size, size, 4).texImage3D(0, GL_DEPTH_COMPONENT, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		shadowMaps = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTextureLayer(GL_DEPTH_ATTACHMENT, shadowMaps, 0, 0);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		shadowMaps.dispose();
	}

}
