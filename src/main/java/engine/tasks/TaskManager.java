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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskManager {

	private static Queue<Task<?>> tasksMainThread = new ConcurrentLinkedQueue<>(),
			tasksBackgroundThread = new ConcurrentLinkedQueue<>();

	private static Queue<Task<?>> tasksRenderThread = new ConcurrentLinkedQueue<>(),
			tasksGLBackgroundThread = new ConcurrentLinkedQueue<>();

	private static List<TaskRunnerThread> backgroundThreads;
	private static List<TaskRunnerGLThread> glThreads;

	private static Object backgroundThreadsLock = new Object(), glThreadsLock = new Object();

	private static final int BACKGROUND_THREADS = 4;
	private static final int GL_THREADS = 2;

	public static void init() {
		backgroundThreads = new ArrayList<>();
		for (int i = 0; i < BACKGROUND_THREADS; i++) {
			TaskRunnerThread t = new TaskRunnerThread(tasksBackgroundThread, backgroundThreadsLock);
			t.setName("Background Thread " + i);
			t.start();
			backgroundThreads.add(t);
		}
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

		tasksMainThread.add(t);
		return t;
	}

	public static <T> Task<T> submitBackgroundThread(Task<T> t) {
		if (t == null)
			return null;

		tasksBackgroundThread.add(t);
		synchronized (backgroundThreadsLock) {
			backgroundThreadsLock.notify();
		}
		return t;
	}

	public static void updateMainThread() {
		while (!tasksMainThread.isEmpty())
			tasksMainThread.poll().callI();
	}

	public static void stopMainThread() {
		while (!tasksMainThread.isEmpty())
			tasksMainThread.poll().callI();
	}

	public static void stopBackgroundThreads() {
		for (TaskRunnerThread thread : backgroundThreads)
			thread.setRunning(false);
		synchronized (backgroundThreadsLock) {
			backgroundThreadsLock.notifyAll();
		}
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

		tasksRenderThread.add(t);
		return t;
	}

	public static <T> Task<T> submitRenderBackgroundThread(Task<T> t) {
		if (t == null)
			return null;

		tasksGLBackgroundThread.add(t);
		synchronized (glThreadsLock) {
			glThreadsLock.notify();
		}
		return t;
	}

	public static void updateRenderThread() {
		while (!tasksRenderThread.isEmpty())
			tasksRenderThread.poll().callI();
	}

	public static void stopRenderThread() {
		while (!tasksRenderThread.isEmpty())
			tasksRenderThread.poll().callI();
	}

	public static void stopGLThreads() {
		for (TaskRunnerGLThread thread : glThreads)
			thread.setRunning(false);
		synchronized (glThreadsLock) {
			glThreadsLock.notifyAll();
		}
		for (TaskRunnerGLThread thread : glThreads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread.destroyWindow();
		}
	}

	public static void switchToSharedContext(long parent) {
		glThreads = new ArrayList<>();
		for (int i = 0; i < GL_THREADS; i++) {
			TaskRunnerGLThread t = new TaskRunnerGLThread(tasksGLBackgroundThread, glThreadsLock, parent);
			t.setName("GL Background Thread " + i);
			t.start();
			glThreads.add(t);
		}
	}

}
