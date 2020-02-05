/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.io.Load;
import engine.lua.type.LuaEvent;
import engine.lua.type.LuaField;
import engine.lua.type.LuaFieldFlag;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.SceneInternal;

public class GameECS extends Instance {
	
	public Map<Long,Instance> serverSidedInstances = new HashMap<>();
	public Map<UUID,Instance> uniqueInstances = new HashMap<>();
	
	public GameECS() {
		super("Game");
		
		// On load event
		this.rawset("Loaded", new LuaEvent());
		this.rawset("Started", new LuaEvent());
		this.rawset("Stopping", new LuaEvent());
		this.rawset("ResetEvent", new LuaEvent());
		this.rawset("SelectionChanged", new LuaEvent());
		
		// Fields
		this.defineField("Running", LuaValue.valueOf(false), true);
		this.defineField("IsServer", LuaValue.valueOf(false), true);
		
		// GetService convenience method
		getmetatable().set("GetService", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg, LuaValue arg2) {
				Service service = Game.getService(arg2.toString());
				if ( service == null )
					return LuaValue.NIL;
				return service;
			}
		});
		
		this.descendantRemovedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(serverSidedInstances) {
					if ( object instanceof Instance ) {
						Instance inst = (Instance) object;
						long sid = inst.getSID();
						
						// Remove server sided instance
						serverSidedInstances.remove(sid);
						
						// Remove unique reference
						uniqueInstances.remove(inst.getUUID());
					}
				}
				return LuaValue.NIL;
			}
		});
		
		this.descendantAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(serverSidedInstances) {
					if ( object instanceof Instance ) {
						Instance inst = (Instance)object;
						
						// Generate UUID
						if ( inst.getUUID() == null ) {
							UUID tuid = Game.generateUUID();
							inst.setUUID(tuid);
							uniqueInstances.put(tuid, inst);
						} else {
							Instance t = uniqueInstances.get(inst.getUUID());
							if ( t == null ) {
								uniqueInstances.put(inst.getUUID(),inst);
							} else {
								if ( t != inst ) {
									UUID tuid = Game.generateUUID();
									inst.setUUID(tuid);
									uniqueInstances.put(tuid, inst);
								}
							}
						}
						
						// Generate server sided id if server instance
						if ( Game.isServer() )
							inst.rawset(C_SID, LuaValue.valueOf(Game.generateSID()));
						
						// Get server id and store it
						long sid = inst.getSID();
						if ( sid != -1 )
							serverSidedInstances.put(sid, inst);
					}
				}
				return LuaValue.NIL;
			}
		});
		
		// LOCK HER UP
		setLocked(true);
		setInstanceable(false);
	}
	
	@Deprecated
	public Map<Long, Instance> getInstanceMapOld() {
		Map<Long, Instance> map = new HashMap<>();
		map.putAll(this.serverSidedInstances);
		return map;
	}
	
	public Map<UUID, Instance> getInstanceMap() {
		Map<UUID, Instance> map = new HashMap<>();
		map.putAll(this.uniqueInstances);
		return map;
	}
	
	public String getName() {
		return this.get(C_NAME).toString().toLowerCase();
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return null;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	public void loadScene(Scene scene) {
		if ( scene == null )
			return;
		
		// Store current scene
		Scene currentScene = Game.project().scenes().getCurrentScene();
		if ( currentScene != null ) {
			SceneInternal unsaved = Game.getGame().getUnsavedScene(currentScene);
			if ( unsaved != null )
				unsaved.storeGame();
		}
		
		// Get the internal scene (data)
		SceneInternal internal = Game.getGame().getUnsavedScene(scene);
		if ( internal == null )
			internal = Load.loadScene(scene);
		if ( internal == null )
			internal = new SceneInternal(scene);
		
		// Set it as loaded
		Game.project().scenes().setCurrentScene(scene);
		if ( !Game.getGame().unsavedScenes.contains(internal) )
			Game.getGame().unsavedScenes.add(internal);
		
		// Load it into game
		extractScene(internal);
		
		// Make sure we have all the services...
		Game.services();
	}
	
	public void extractScene(SceneInternal internal) {
		List<Instance> services = internal.getChildrenSafe();
		for (int i = 0; i < services.size(); i++) {
			Instance service = services.get(i);
			Instance toService = Game.getService(service.getName());
			if ( toService == null )
				continue;
			
			// Move children into service
			List<Instance> servChild = service.getChildrenSafe();
			System.out.println("LOOKING AT: " + service + " " + Arrays.toString(servChild.toArray()));
			for (int j = 0; j < servChild.size(); j++) {
				Instance child = servChild.get(j);
				child.forceSetParent(toService);
				System.out.println("Restoring " + child + " " + child.getUUID() + " to " + toService.getFullName());
			}
			
			// Copy fields over
			LuaField[] fields = service.getFields();
			for (int j = 0; j < fields.length; j++) {
				LuaField field = fields[j];
				if ( field.hasFlag(LuaFieldFlag.BASE_FIELD) )
					continue;
				
				System.out.println("SETTING " + field.getName() + " to " + service.get(field.getName()));
				toService.rawset(field.getName(), service.get(field.getName()));
			}
		}
	}
}