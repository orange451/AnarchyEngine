/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.objects;

import static org.lwjgl.opengl.GL30C.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30C.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30C.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30C.glRenderbufferStorage;

public class RenderbufferBuilder {

	private int renderbuffer;
	private boolean working;
	private int width, height;

	public RenderbufferBuilder() {
	}

	public RenderbufferBuilder genRenderbuffer() {
		if (working)
			throw new IllegalStateException("Already working on a Renderbuffer.");
		working = true;
		renderbuffer = glGenRenderbuffers();
		return this;
	}

	public RenderbufferBuilder bindRenderbuffer() {
		check();
		glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
		return this;
	}

	public RenderbufferBuilder sizeRenderbuffer(int width, int height) {
		check();
		this.width = width;
		this.height = height;
		return this;
	}

	public RenderbufferBuilder renderbufferStorage(int internalformat) {
		check();
		glRenderbufferStorage(GL_RENDERBUFFER, internalformat, width, height);
		return this;
	}

	public Renderbuffer endRenderbuffer() {
		check();
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
		working = false;
		return new Renderbuffer(renderbuffer);
	}

	private void check() throws IllegalStateException {
		if (!working)
			throw new IllegalStateException("Not working on a Renderbuffer.");
	}

}
