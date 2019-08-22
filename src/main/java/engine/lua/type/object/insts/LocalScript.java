package engine.lua.type.object.insts;

import engine.Game;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

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
