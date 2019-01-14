package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.RunScript;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class ScriptService extends Service implements TreeViewable,RunScript {

	public ScriptService() {
		super("ScriptService");
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
	public Icons getIcon() {
		return Icons.icon_script_service;
	}
}
