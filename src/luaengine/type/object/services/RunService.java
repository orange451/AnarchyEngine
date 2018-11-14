package luaengine.type.object.services;

import org.luaj.vm2.LuaValue;

import luaengine.type.LuaEvent;
import luaengine.type.object.Service;

public class RunService extends Service {

	public RunService() {
		super("RunService");

		this.rawset("Heartbeat", new LuaEvent());
		this.rawset("RenderStepped", new LuaEvent());
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
	
	public LuaEvent getHeartbeatEvent() {
		return (LuaEvent) this.get("Heartbeat");
	}
	
	public LuaEvent getRenderSteppedEvent() {
		return (LuaEvent) this.get("RenderStepped");
	}
}
