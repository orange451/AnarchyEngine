/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.tasks;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.Queue;

import org.lwjgl.opengl.GL;

import engine.util.GLCompat;

public class TaskRunnerGLThread extends TaskRunnerThread {

	private long window;

	public TaskRunnerGLThread(Queue<Task<?>> tasks, Object threadsLock, long parentWindow) {
		super(tasks, threadsLock);
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLCompat.GL_MAJOR);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLCompat.GL_MINOR);
		window = glfwCreateWindow(1, 1, "Background Window", NULL, parentWindow);
		if (window == NULL)
			throw new RuntimeException("Failed to create background window");
	}

	@Override
	public void run() {
		glfwMakeContextCurrent(window);
		GL.createCapabilities(true);
		while (running) {
			if (!tasks.isEmpty()) {
				Task<?> task = tasks.poll();
				if (task != null)
					task.callI();
			} else
				synchronized (threadsLock) {
					try {
						threadsLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
		GL.setCapabilities(null);
		glfwMakeContextCurrent(NULL);
	}

	public void destroyWindow() {
		glfwDestroyWindow(window);
	}

}
