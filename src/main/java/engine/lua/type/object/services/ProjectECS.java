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

import java.util.UUID;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import engine.Game;
import engine.lua.type.object.Instance;

public class ProjectECS extends Instance {
	
	public ProjectECS() {
		super("project");
		
		// LOCK HER UP
		setLocked(true);
		setInstanceable(false);
		
		this.descendantAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				if ( object instanceof Instance ) {
					Instance inst = (Instance)object;
					
					// Generate UUID
					if ( inst.getUUID() == null ) {
						UUID tuid = Game.generateUUID();
						inst.setUUID(tuid);
						Game.game().uniqueInstances.put(tuid, inst);
					} else {
						Instance t = Game.game().uniqueInstances.get(inst.getUUID());
						if ( t == null ) {
							Game.game().uniqueInstances.put(inst.getUUID(),inst);
						} else {
							if ( t != inst ) {
								UUID tuid = Game.generateUUID();
								inst.setUUID(tuid);
								Game.game().uniqueInstances.put(tuid, inst);
							}
						}
					}
				}
				return LuaValue.NIL;
			}
		});
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

	private static final LuaValue C_SCENES = LuaValue.valueOf("Scenes");
	private static final LuaValue C_ASSETS = LuaValue.valueOf("Assets");
	private static final LuaValue C_STORAGE = LuaValue.valueOf("Storage");
	private static final LuaValue C_SCRIPTSERVICE = LuaValue.valueOf("ScriptService");

	public Scenes scenes() {
		LuaValue t = this.get(C_SCENES);
		if ( t.isnil() )
			return null;
		
		if ( t instanceof Scenes )
			return (Scenes)t;
		
		return null;
	}

	public Assets assets() {
		LuaValue t = this.get(C_ASSETS);
		if ( t.isnil() )
			return null;
		
		if ( t instanceof Assets )
			return (Assets)t;
		
		return null;
	}

	public Storage storage() {
		LuaValue t = this.get(C_STORAGE);
		if ( t.isnil() )
			return null;
		
		if ( t instanceof Storage )
			return (Storage)t;
		
		return null;
	}

	public ScriptService scriptService() {
		LuaValue t = this.get(C_SCRIPTSERVICE);
		if ( t.isnil() )
			return null;
		
		if ( t instanceof ScriptService )
			return (ScriptService)t;
		
		return null;
	}
}