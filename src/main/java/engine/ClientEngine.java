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
import engine.util.JVMUtil;
import ide.layout.windows.ErrorWindow;
import lwjgui.glfw.ClientSync;
import lwjgui.glfw.CustomCursor;
import lwjgui.scene.Cursor;
import lwjgui.scene.WindowManager;

public abstract class ClientEngine {

	public static Game game;
	public static InternalRenderThread renderThread;
	public static InternalGameThread gameThread;
	public static ClientEngine engine;

	private static boolean running = true;

	static boolean renderReady, updateReady, waitThreads;

	protected String[] args;

	public ClientEngine(String... args) {
		JVMUtil.restartJVM(true, true, null);
		this.args = args;
		game = new Game();
		engine = this;
		init();
	}

	public void init() {
		Thread.currentThread().setName("GLFWThread");

		Thread initThread = new Thread(() -> {
			while (true) {
				if (updateReady && renderReady) {
					waitThreads = true;
					return;
				} else
					ThreadUtils.sleep(1); // Let's sleep meanwhile
			}
		});
		initThread.setName("Init Thread");
		initThread.start();

		GLFWErrorCallback.createPrint(System.err).set();

		if (!glfwInit()) {
			new ErrorWindow("Unable to initialize GLFW.", true);
			return;
		}
		WindowManager.init();

		WindowManager.addCursor(Cursor.NORMAL, new CustomCursor("assets/cursors/arrow.png", 0, 0));
		WindowManager.addCursor(Cursor.VRESIZE, new CustomCursor("assets/cursors/vresize.png", 5, 9));
		WindowManager.addCursor(Cursor.HRESIZE, new CustomCursor("assets/cursors/hresize.png", 9, 5));
		WindowManager.addCursor(Cursor.IBEAM, new CustomCursor("assets/cursors/ibeam.png", 4, 9));

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
		WindowManager.dispose();
		glfwTerminate();
	}

	public abstract void setupEngine();

	public abstract void render();

	public abstract void update();

	public abstract boolean isMouseGrabbed();
	
	public static void stop() {
		running = false;
	}

	public static boolean isRunning() {
		return running;
	}

}
