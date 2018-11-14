package luaengine.type.object.services;

import org.luaj.vm2.LuaValue;

import luaengine.type.object.RunScript;
import luaengine.type.object.Service;
import luaengine.type.object.TreeInvisible;

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
}
