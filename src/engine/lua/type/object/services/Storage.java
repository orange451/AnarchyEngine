package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Storage extends Service implements TreeViewable {

	public Storage() {
		super("Storage");
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
		return Icons.icon_storage;
	}
}
