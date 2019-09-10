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

package engine.glv2.objects;

import static org.lwjgl.opengl.GL11C.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11C.glGetIntegerv;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;
import static org.lwjgl.opengl.GL30C.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30C.glFramebufferTextureLayer;
import static org.lwjgl.opengl.GL32C.glFramebufferTexture;

public class Framebuffer implements IObject {

	private final int framebuffer;
	private final int width, height;

	private int[] oldViewport = new int[4];

	public Framebuffer(int framebuffer, int width, int height) {
		this.framebuffer = framebuffer;
		this.width = width;
		this.height = height;
	}

	@Override
	public void bind() {
		glGetIntegerv(GL_VIEWPORT, oldViewport);
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
	}

	@Override
	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
	}

	@Override
	public void dispose() {
		glDeleteFramebuffers(framebuffer);
	}

	public void swapTexture(int attachment, Texture texture, int level) {
		glFramebufferTexture(GL_FRAMEBUFFER, attachment, texture.getTexture(), level);
	}

	public void swapTextureLayer(int attachment, Texture texture, int level, int layer) {
		glFramebufferTextureLayer(GL_FRAMEBUFFER, attachment, texture.getTexture(), level, layer);
	}

	public int getFramebuffer() {
		return framebuffer;
	}

}
