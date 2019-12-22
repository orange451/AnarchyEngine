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
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.GL;

import engine.util.GLCompat;

public class TaskManager {

	private static Queue<Task<?>> tasksMainThread = new ConcurrentLinkedQueue<>(),
			tasksBackgroundThread = new ConcurrentLinkedQueue<>();
	private static Thread mainThread, backgroundThread;
	private static boolean syncInterruptBackground;

	private static long asyncWindow;

	private static Queue<Task<?>> tasksRenderThread = new ConcurrentLinkedQueue<>(),
			tasksRenderBackgroundThread = new ConcurrentLinkedQueue<>();
	private static Thread renderThread, renderBackgroundThread;
	private static boolean runBackgroundThread = true, syncInterruptRenderBackground = true;

	public static void init() {
		mainThread = Thread.currentThread(); // Let's assume init thread it's main
		backgroundThread = new Thread(() -> {
			while (true) {
				if (!tasksBackgroundThread.isEmpty()) {
					while (!tasksBackgroundThread.isEmpty())
						tasksBackgroundThread.poll().callI();
				} else {
					syncInterruptBackground = false;
					ThreadUtils.sleep(Long.MAX_VALUE);
				}
			}
		});
		backgroundThread.setDaemon(true);
		backgroundThread.setName("Main Background");
		backgroundThread.start();
	}

	public static void addTaskMainThread(Runnable task) {
		if (task == null)
			return;
		submitMainThread(new Task<Void>() {
			@Override
			protected Void call() {
				task.run();
				return null;
			}

		});
	}

	public static void addTaskBackgroundThread(Runnable task) {
		if (task == null)
			return;
		submitBackgroundThread(new Task<Void>() {
			@Override
			protected Void call() {
				task.run();
				return null;
			}
		});
	}

	public static <T> Task<T> submitMainThread(Task<T> t) {
		if (t == null)
			return null;

		if (Thread.currentThread().getId() == mainThread.getId())
			t.callI();
		else
			tasksMainThread.add(t);
		return t;
	}

	public static <T> Task<T> submitBackgroundThread(Task<T> t) {
		if (t == null)
			return null;

		if (Thread.currentThread().getId() == backgroundThread.getId())
			t.callI();
		else {
			tasksBackgroundThread.add(t);
			if (!syncInterruptBackground) {
				syncInterruptBackground = true;
				backgroundThread.interrupt();
			}
		}
		return t;
	}

	public static void updateMainThread() {
		while (!tasksMainThread.isEmpty())
			tasksMainThread.poll().callI();
	}

	public static void runAndStopMainThread() {
		while (!tasksMainThread.isEmpty())
			tasksMainThread.poll().callI();
	}

	public static void addTaskRenderThread(Runnable task) {
		if (task == null)
			return;
		submitRenderThread(new Task<Void>() {
			@Override
			protected Void call() {
				task.run();
				return null;
			}
		});
	}

	public static void addTaskRenderBackgroundThread(Runnable task) {
		if (task == null)
			return;
		submitRenderBackgroundThread(new Task<Void>() {
			@Override
			protected Void call() {
				task.run();
				return null;
			}
		});
	}

	public static <T> Task<T> submitRenderThread(Task<T> t) {
		if (t == null)
			return null;

		if (Thread.currentThread().getId() == renderThread.getId())
			t.callI();
		else
			tasksRenderThread.add(t);
		return t;
	}

	public static <T> Task<T> submitRenderBackgroundThread(Task<T> t) {
		if (t == null)
			return null;

		if (Thread.currentThread().getId() == renderBackgroundThread.getId())
			t.callI();
		else {
			tasksRenderBackgroundThread.add(t);
			if (!syncInterruptRenderBackground) {
				syncInterruptRenderBackground = true;
				renderBackgroundThread.interrupt();
			}
		}
		return t;
	}

	public static void updateRenderThread() {
		while (!tasksRenderThread.isEmpty())
			tasksRenderThread.poll().callI();
	}

	public static void runAndStopRenderThread() {
		while (!tasksRenderThread.isEmpty())
			tasksRenderThread.poll().callI();
	}

	public static void switchToSharedContext(long parent) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GLCompat.GL_MAJOR);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GLCompat.GL_MINOR);
		asyncWindow = glfwCreateWindow(1, 1, "Background Window", NULL, parent);
		if (asyncWindow == NULL)
			throw new RuntimeException("Failed to create background window");

		renderBackgroundThread = new Thread(() -> {
			glfwMakeContextCurrent(asyncWindow);
			GL.createCapabilities(true);
			while (runBackgroundThread) {
				if (!tasksRenderBackgroundThread.isEmpty()) {
					while (!tasksRenderBackgroundThread.isEmpty())
						tasksRenderBackgroundThread.poll().callI();
				} else {
					syncInterruptRenderBackground = false;
					ThreadUtils.sleep(Long.MAX_VALUE);
				}
				glfwSwapBuffers(asyncWindow);
			}
			GL.setCapabilities(null);
			glfwMakeContextCurrent(NULL);
		});
		renderBackgroundThread.setName("Render Background");
		renderBackgroundThread.start();
	}

	public static void stopRenderBackgroundThread() {
		runBackgroundThread = false;
		renderBackgroundThread.interrupt();
		while (renderBackgroundThread.isAlive())
			ThreadUtils.sleep(100);
		glfwDestroyWindow(asyncWindow);
	}

	public static void setRenderThread(Thread renderThread) {
		TaskManager.renderThread = renderThread;
	}

}
