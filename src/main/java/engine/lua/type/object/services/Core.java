package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.RunScript;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeInvisible;

public class Core extends Service implements TreeInvisible,RunScript {

	public Core() {
		super("Core");
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public RenderSettings getRenderSettings() {
		return (RenderSettings) this.get("RenderSettings");
	}
}
