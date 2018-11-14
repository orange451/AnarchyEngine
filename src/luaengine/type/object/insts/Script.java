package luaengine.type.object.insts;

import engine.Game;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.ScriptBase;
import luaengine.type.object.TreeViewable;

public class Script extends ScriptBase implements TreeViewable {

	public Script() {
		super("Script");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_script;
	}

	@Override
	public boolean getCanRun() {
		return Game.isServer() || Game.internalTesting;
	}
}
