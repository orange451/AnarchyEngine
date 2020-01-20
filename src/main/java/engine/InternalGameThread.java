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

import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.lua.LuaEngine;
import engine.lua.type.ScriptRunner;
import engine.lua.type.object.Service;
import engine.lua.type.object.services.GameECS;
import engine.lua.type.object.services.RunService;
import engine.tasks.TaskManager;
import engine.tasks.ThreadUtils;
import engine.util.Sync;
import lwjgui.LWJGUI;

public class InternalGameThread extends Thread implements IEngineThread {
	public static int tps;
	public static float delta;

	public static int desiredTPS = 60;

	public static void runLater(Runnable runnable) {
		TaskManager.addTaskMainThread(runnable);
	}

	private ClientEngine client;

	public InternalGameThread(ClientEngine client) {
		this.client = client;
		super.setName("Update Thread");
	}

	@Override
	public void init() {
		TaskManager.init();
		LWJGUI.setThreadWindow(ClientEngine.renderThread.getWindow());
		// Turn on lua
		LuaEngine.initialize();

		// Create the game instance
		Game.setGame(new GameECS());

		// Start a new project
		Game.changes = false;
		Game.newProject();
	}

	@Override
	public void run() {
		init();
		ClientEngine.updateReady = true;
		while (!ClientEngine.waitThreads)
			ThreadUtils.sleep(1);
		ClientEngine.updateReady = false;

		int ups = desiredTPS;
		float accumulator = 0f;
		float interval = 1f / ups;

		Sync sync = new Sync();
		while (ClientEngine.isRunning()) {
			TaskManager.updateMainThread();

			delta = sync.getDelta();
			accumulator += delta;
			while (accumulator >= interval) {
				if (Game.game() == null)
					continue;

				if (!Game.isLoaded())
					continue;

				if (Game.core() == null)
					continue;

				RunService runService = Game.runService();
				if (runService != null && Game.isRunning()) {
					runService.heartbeatEvent().fire(LuaValue.valueOf(delta));
				}
				client.update();
				Game.getGame().tick();
				accumulator -= interval;
			}
			sync.sync(ups);
		}
		dispose();
	}

	@Override
	public void dispose() {
		TaskManager.runAndStopMainThread();
		List<Service> services = Game.getServices();

		// Clean up all lua objects
		Game.unload();
		Game.setRunning(false);

		// Stop services
		System.out.println("Destroying services");
		for (int i = 0; i < services.size(); i++) {
			services.get(i).destroy();
		}

		// Turn off lua
		LuaEngine.cleanup();
		ScriptRunner.shutdown();
		LWJGUI.removeThreadWindow();
		ClientEngine.updateReady = true;
	}

}
