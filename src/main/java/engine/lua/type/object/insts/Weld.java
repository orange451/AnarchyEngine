/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;

public class Weld extends Instance {
	protected final static LuaValue C_INSTANCE_0 = LuaValue.valueOf("Instance0");
	protected final static LuaValue C_INSTANCE_1 = LuaValue.valueOf("Instance1");
	
	public Weld() {
		super("Weld");

		this.defineField(C_INSTANCE_0, LuaValue.NIL, false);
		this.defineField(C_INSTANCE_1, LuaValue.NIL, false);

		this.setInstanceable(true);
		this.getField(C_NAME).setLocked(false);
		this.getField(C_PARENT).setLocked(false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	public Instance getInstance0() {
		LuaValue val = this.get(C_INSTANCE_0);
		return val.isnil() ? null : (Instance)val;
	}

	public Instance getInstance1() {
		LuaValue val = this.get(C_INSTANCE_1);
		return val.isnil() ? null : (Instance)val;
	}
}
