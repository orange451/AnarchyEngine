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
		System.out.println("Stopped main thread");
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
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
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
			glfwMakeContextCurrent(NULL);
			GL.setCapabilities(null);
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
