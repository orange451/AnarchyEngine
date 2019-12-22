/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
