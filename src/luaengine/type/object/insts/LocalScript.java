package luaengine.type.object.insts;

import engine.Game;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.ScriptBase;
import luaengine.type.object.TreeViewable;

public class LocalScript extends ScriptBase implements TreeViewable {
	
	public LocalScript() {
		super("LocalScript");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_script_local;
	}
	
	@Override
	public boolean getCanRun() {
		return !Game.isServer() || Game.internalTesting;
	}
}
