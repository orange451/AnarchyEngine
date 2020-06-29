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

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeInvisible;
import engine.lua.type.object.WeldedStructure;
import engine.lua.type.object.insts.Weld;

public class WeldService extends Service implements TreeInvisible {

	private Map<Weld, WeldedStructure> trackedWelds = new HashMap<>();
	
	public WeldService() {
		super("WeldService");
		
		InternalGameThread.runLater(()->{
			Game.game().descendantAddedEvent().connect((args)->{
				LuaValue arg = args[0];
				if ( !(arg instanceof Weld) )
					return;
			});
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
}
