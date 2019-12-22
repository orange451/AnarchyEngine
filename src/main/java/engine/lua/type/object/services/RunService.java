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

import org.luaj.vm2.LuaValue;

import engine.lua.type.LuaEvent;
import engine.lua.type.object.Service;

public class RunService extends Service {

	private static final LuaValue C_HEARTBEAT = LuaValue.valueOf("Heartbeat");
	private static final LuaValue C_RENDERSTEPPED = LuaValue.valueOf("RenderStepped");
	private static final LuaValue C_RENDERPOST = LuaValue.valueOf("RenderPost");
	private static final LuaValue C_RENDERPRE = LuaValue.valueOf("RenderPre");
	private static final LuaValue C_PHYSICSSTEPPED = LuaValue.valueOf("PhysicsStepped");

	public RunService() {
		super("RunService");
		
		this.rawset(C_HEARTBEAT, new LuaEvent());
		this.rawset(C_RENDERSTEPPED, new LuaEvent());
		this.rawset(C_RENDERPOST, new LuaEvent());
		this.rawset(C_RENDERPRE, new LuaEvent());
		this.rawset(C_PHYSICSSTEPPED, new LuaEvent());
		this.setLocked(true);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public LuaEvent heartbeatEvent() {
		return (LuaEvent) this.get(C_HEARTBEAT);
	}
	
	public LuaEvent renderSteppedEvent() {
		return (LuaEvent) this.get(C_RENDERSTEPPED);
	}
	
	public LuaEvent renderPostEvent() {
		return (LuaEvent) this.get(C_RENDERPOST);
	}
	
	public LuaEvent renderPreEvent() {
		return (LuaEvent) this.get(C_RENDERPRE);
	}
	
	public LuaEvent physicsSteppedEvent() {
		return (LuaEvent) this.get(C_PHYSICSSTEPPED);
	}
}
