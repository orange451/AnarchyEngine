package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;

public class Folder extends Instance implements TreeViewable {

	public Folder() {
		super("Folder");
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

	@Override
	public Icons getIcon() {
		return Icons.icon_folder;
	}
}
