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

import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30C.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;
import static org.lwjgl.opengl.GL30C.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30C.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30C.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30C.glFramebufferTextureLayer;
import static org.lwjgl.opengl.GL30C.glGenFramebuffers;
import static org.lwjgl.opengl.GL32C.glFramebufferTexture;

import engine.glv2.exceptions.FrameBufferException;

public class FramebufferBuilder {

	private int framebuffer;
	private boolean working;
	private int width, height;

	public FramebufferBuilder() {
	}

	public FramebufferBuilder genFramebuffer() {
		if (working)
			throw new IllegalStateException("Already working on a Framebuffer.");
		working = true;
		framebuffer = glGenFramebuffers();
		return this;
	}

	public FramebufferBuilder bindFramebuffer() {
		check();
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		return this;
	}

	public FramebufferBuilder sizeFramebuffer(int width, int height) {
		check();
		this.width = width;
		this.height = height;
		return this;
	}

	public FramebufferBuilder framebufferTexture(int attachment, Texture texture, int level) {
		check();
		glFramebufferTexture(GL_FRAMEBUFFER, attachment, texture.getTexture(), level);
		return this;
	}

	public FramebufferBuilder framebufferTextureLayer(int attachment, Texture texture, int level, int layer) {
		check();
		glFramebufferTextureLayer(GL_FRAMEBUFFER, attachment, texture.getTexture(), level, layer);
		return this;
	}

	public FramebufferBuilder framebufferTexture2D(int attachment, int target, Texture texture, int level) {
		check();
		glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, target, texture.getTexture(), level);
		return this;
	}

	public FramebufferBuilder framebufferRenderbuffer(int attachment, Renderbuffer renderbuffer) {
		check();
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment, GL_RENDERBUFFER, renderbuffer.getRenderbuffer());
		return this;
	}

	public FramebufferBuilder drawBuffers(int... bufs) {
		check();
		glDrawBuffers(bufs);
		return this;
	}

	public Framebuffer endFramebuffer() {
		check();
		if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
			throw new FrameBufferException("Incomplete FrameBuffer.");
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		working = false;
		return new Framebuffer(framebuffer, width, height);
	}

	private void check() throws IllegalStateException {
		if (!working)
			throw new IllegalStateException("Not working on a Framebuffer.");
	}

}
