/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2;

import static org.lwjgl.opengl.GL11.GL_INVALID_ENUM;
import static org.lwjgl.opengl.GL11.GL_INVALID_OPERATION;
import static org.lwjgl.opengl.GL11.GL_INVALID_VALUE;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL11.GL_STACK_OVERFLOW;
import static org.lwjgl.opengl.GL11.GL_STACK_UNDERFLOW;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;
import static org.lwjgl.opengl.GL45.GL_CONTEXT_LOST;

import engine.glv2.exceptions.OpenGLException;

public class DisplayUtils {

	public static void checkErrors() {
		switch (glGetError()) {
		case GL_INVALID_ENUM:
			throw new OpenGLException("GL_INVALID_ENUM");
		case GL_INVALID_VALUE:
			throw new OpenGLException("GL_INVALID_VALUE");
		case GL_INVALID_OPERATION:
			throw new OpenGLException("GL_INVALID_OPERATION");
		case GL_STACK_OVERFLOW:
			throw new OpenGLException("GL_STACK_OVERFLOW");
		case GL_STACK_UNDERFLOW:
			throw new OpenGLException("GL_STACK_UNDERFLOW");
		case GL_OUT_OF_MEMORY:
			throw new OpenGLException("GL_OUT_OF_MEMORY");
		case GL_INVALID_FRAMEBUFFER_OPERATION:
			throw new OpenGLException("GL_INVALID_FRAMEBUFFER_OPERATION");
		case GL_CONTEXT_LOST:
			throw new OpenGLException("GL_CONTEXT_LOST");
		case GL_NO_ERROR:
			break;
		}
	}

}
