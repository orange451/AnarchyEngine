package luaengine.type.object.services;

import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;

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
