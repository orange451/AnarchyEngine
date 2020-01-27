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
import java.util.Map;
import java.util.UUID;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;

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
}