package engine;

import java.util.List;

/*
*
* Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*
*/

import engine.application.Application;
import engine.lua.LuaEngine;
import engine.lua.type.object.Service;
import engine.lua.type.object.services.GameECS;

public abstract class AnarchyEngine extends Application {
	@Override
	protected void setupEngine() {
		// Turn on lua
		LuaEngine.initialize();
		
		// Create the game instance
		Game.setGame(new GameECS());
		
		// Start a new project
		Game.changes = false;
		Game.newProject();
	}
	
	@Override
	protected void cleanupEngine() {
		System.out.println("Cleaning up ECS");
		List<Service> services = Game.getServices();
		
		// Clean up all lua objects
		Game.unload();
		Game.setRunning(false);
		
		// Stop services
		System.out.println("Destroying services");
		for (int i = 0; i < services.size(); i++) {
			services.get(i).destroy();
		}
		
		// Stop Game thread
		InternalGameThread.terminate();
		
		// Turn off lua
		LuaEngine.cleanup();
		
		// Shut down resource loader
		System.out.println("Shutting down resources");
		Game.resourceLoader().shutdown();
	}
}
