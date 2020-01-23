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

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.gl.IPipeline;
import engine.gl.Resources;
import engine.glv2.GLRenderer;
import engine.lua.type.object.services.RunService;
import engine.tasks.TaskManager;
import engine.tasks.ThreadUtils;
import engine.util.GLCompat;
import lwjgui.LWJGUI;
import lwjgui.scene.Window;
import lwjgui.scene.WindowHandle;
import lwjgui.scene.WindowManager;

public class InternalRenderThread extends Thread implements IEngineThread {
	public static float delta;
	public static float fps;
	public static int desiredFPS = 60;

	private int fpscounter;

	private Window window;
	private WindowHandle handle;

	private IPipeline pipeline;

	private ClientEngineUI clientUI;

	private ClientEngine client;

	private int targetWidth, targetHeight;
	private boolean resizePipeline;

	public InternalRenderThread(ClientEngine client) {
		this.client = client;
		super.setName("Render Thread");
		handle = WindowManager.generateHandle(1024, 600, "Anarchy Engine - IDE", false);
		handle.isVisible(false);
		handle.setWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, GLCompat.GL_MAJOR);
		handle.setWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, GLCompat.GL_MINOR);
		window = WindowManager.generateWindow(handle);
		TaskManager.switchToSharedContext(window.getID());
	}

	public static void runLater(Runnable runnable) {
		TaskManager.addTaskRenderThread(runnable);
	}

	@Override
	public void init() {
		window.setVisible(true);
		LWJGUI.setThreadWindow(window);
		WindowManager.createWindow(handle, window, true);
		Resources.init();
		pipeline = new GLRenderer(window);
		pipeline.init();
		clientUI = new ClientEngineUI();
		client.setupEngine();
	}

	public void run() {
		init();
		ClientEngine.renderReady = true;
		while (!ClientEngine.waitThreads)
			ThreadUtils.sleep(1);

		long timer = System.currentTimeMillis();
		while (ClientEngine.isRunning()) {
			// Calculate FPS
			fpscounter++;
			if (System.currentTimeMillis() - timer >= 1000) {
				timer = System.currentTimeMillis();
				fps = fpscounter;
				fpscounter = 0;
			}
			delta = window.getDelta();

			TaskManager.updateRenderThread();

			client.render();
			// Draw event
			if (Game.isLoaded()) {
				RunService runService = Game.runService();
				if (runService == null)
					return;

				if (!window.wasResized() && resizePipeline) {
					pipeline.setSize(targetWidth, targetHeight);
					resizePipeline = false;
				}

				runService.renderPreEvent().fire(LuaValue.valueOf(delta));
				runService.renderSteppedEvent().fire(LuaValue.valueOf(delta));
				pipeline.render();
				runService.renderPostEvent().fire(LuaValue.valueOf(delta));
			}
			window.render();
			window.updateDisplay(0);
			if (window.isCloseRequested()) {
				ClientEngine.stop();
			}
		}
		dispose();
	}

	@Override
	public void dispose() {
		while (!ClientEngine.updateReady) // Wait for render thread
			ThreadUtils.sleep(1);
		pipeline.dispose();
		Resources.dispose();
		WindowManager.runLater(() -> TaskManager.stopRenderBackgroundThread());
		TaskManager.runAndStopRenderThread();
		window.dispose();
		LWJGUI.removeThreadWindow();
	}

	public void resizedPipeline(int width, int height) {
		if (this.targetWidth == width && this.targetHeight == height)
			return;
		this.targetWidth = width;
		this.targetHeight = height;
		this.resizePipeline = true;
	}

	public Window getWindow() {
		return window;
	}

	public ClientEngineUI getClientUI() {
		return clientUI;
	}

	public IPipeline getPipeline() {
		return pipeline;
	}
}
