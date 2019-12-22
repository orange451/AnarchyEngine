/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.util;

import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.opengl.GL;
import org.lwjgl.system.Platform;

public final class GLCompat {

	public static String GLSL_VERSION;

	public static int GL_MAJOR;
	public static int GL_MINOR;

	public static void init(int minMajor, int minMinor) {
		if (Platform.get() == Platform.MACOSX) {
			GL_MAJOR = 3;
			GL_MINOR = 3;
			GLSL_VERSION = "#version 330 core";
			return; // ¯\_(ツ)_/¯
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);

		long window = glfwCreateWindow(1, 1, "Detection", NULL, NULL);
		if (window == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}
		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		int major = glGetInteger(GL_MAJOR_VERSION);
		int minor = glGetInteger(GL_MINOR_VERSION);
		GL_MAJOR = major;
		GL_MINOR = minor;

		int detected = Integer.parseInt(Integer.toString(major) + Integer.toString(minor));
		int min = Integer.parseInt(Integer.toString(minMajor) + Integer.toString(minMinor));

		if (detected < min) {
			GL_MAJOR = minMajor;
			GL_MINOR = minMinor;
		}
		GLSL_VERSION = String.format("#version %d%d0 core", GL_MAJOR, GL_MINOR);

		System.out.println("Using OpenGL " + GL_MAJOR + "." + GL_MINOR);

		GL.setCapabilities(null);
		glfwMakeContextCurrent(NULL);

		glfwDestroyWindow(window);
	}

	public static boolean isVersionSupported(int major, int minor) {

		return false;
	}

}
