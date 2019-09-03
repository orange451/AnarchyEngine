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
