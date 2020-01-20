/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import org.lwjgl.glfw.GLFWErrorCallback;

import engine.tasks.ThreadUtils;
import engine.util.GLCompat;
import ide.layout.windows.ErrorWindow;
import lwjgui.LWJGUI;
import lwjgui.glfw.ClientSync;
import lwjgui.scene.WindowManager;

public abstract class ClientEngine {

	public static Game game;
	public static InternalRenderThread renderThread;
	public static InternalGameThread gameThread;

	private static boolean running = true;

	static boolean renderReady, updateReady, waitThreads;

	protected String[] args;

	public ClientEngine(String... args) {
		this.args = args;
		game = new Game();
		init();
	}

	public void init() {
		Thread.currentThread().setName("GLFWThread");
		WindowManager.setMainThread(Thread.currentThread());

		Thread initThread = new Thread(() -> {
			while (true) {
				if (updateReady && renderReady) {
					waitThreads = true;
					return;
				} else
					ThreadUtils.sleep(1); // Let's sleep meanwhile
			}
		});
		initThread.start();

		GLFWErrorCallback.createPrint(System.err).set();

		if (!glfwInit()) {
			new ErrorWindow("Unable to initialize GLFW.", true);
			return;
		}

		GLCompat.init(3, 3);

		renderThread = new InternalRenderThread(this);
		gameThread = new InternalGameThread(this);
		renderThread.start();
		gameThread.start();

		run();
		dispose();
	}

	public void run() {
		ClientSync sync = new ClientSync();
		while (!WindowManager.isEmpty()) {
			WindowManager.update();
			sync.sync(60);
		}
	}

	public void dispose() {
		LWJGUI.dispose();
		glfwTerminate();
	}

	public abstract void setupEngine();

	public abstract void render();

	public abstract void update();

	public static void stop() {
		running = false;
	}

	public static boolean isRunning() {
		return running;
	}

}
