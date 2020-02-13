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
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.SceneInternal;

public class GameECS extends Instance {
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
		this.defineField(LuaValue.valueOf("Running"), LuaValue.valueOf(false), true);
		this.defineField(LuaValue.valueOf("IsServer"), LuaValue.valueOf(false), true);
		
		// GetService convenience method
		getmetatable().set("LoadScene", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue scene) {
				if ( scene.isnil() )
					return LuaValue.NIL;
				
				if ( !(scene instanceof Scene) )
					return LuaValue.NIL;
				
				loadScene((Scene)scene);
				return LuaValue.NIL;
			}
		});
		
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
					if ( object instanceof Instance ) {
						Instance inst = (Instance) object;
						UUID UUID = inst.getUUID();
						if ( UUID == null )
							return LuaValue.NIL;
						
						// Remove unique reference
						uniqueInstances.remove(UUID);
					}
				return LuaValue.NIL;
			}
		});
		
		this.descendantAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				if ( object instanceof Instance ) {
					Instance newInst = (Instance)object;
					
					// Generate UUID
					if ( newInst.getUUID() == null ) {
						UUID tuid = Game.generateUUID();
						newInst.setUUID(tuid);
						uniqueInstances.put(tuid, newInst);
					} else {
						Instance oldInst = uniqueInstances.get(newInst.getUUID());
						if ( oldInst == null ) {
							uniqueInstances.put(newInst.getUUID(),newInst);
						} else if ( oldInst != newInst ) {
							UUID tuid = Game.generateUUID();
							newInst.setUUID(tuid);
							uniqueInstances.put(tuid, newInst);
						}
					}
				}
				return LuaValue.NIL;
			}
		});
		
		// LOCK HER UP
		setLocked(true);
		setInstanceable(false);
	}
	
	/*@Deprecated
	public Map<Long, Instance> getInstanceMapOld() {
		Map<Long, Instance> map = new HashMap<>();
		map.putAll(this.serverSidedInstances);
		return map;
	}*/
	
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
		
		System.out.println("Attempting to load scene: " + scene.getFullName() + " / " + scene.getUUID());
		
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
		
		// Fire load event
		Game.loadEvent().fire();
		
		// Copy in new starter player stuff to existing players...
		List<Player> players = Game.players().getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Game.starterPlayer().startPlayer(players.get(i));
		}
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
			for (int j = 0; j < servChild.size(); j++) {
				Instance child = servChild.get(j);
				child.forceSetParent(toService);
			}
			
			// Copy fields over
			LuaField[] fields = service.getFields();
			for (int j = 0; j < fields.length; j++) {
				LuaField field = fields[j];
				if ( field.hasFlag(LuaFieldFlag.CORE_FIELD) )
					continue;
				
				toService.rawset(field.getName(), service.get(field.getName()));
			}
		}
		
		// Make sure external scripts are added
		Load.loadExternalObjects(Game.saveFile);
	}
}