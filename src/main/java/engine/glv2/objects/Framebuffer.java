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
