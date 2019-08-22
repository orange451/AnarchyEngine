package engine.lua.type.object.insts;

import engine.Game;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Script extends ScriptBase implements TreeViewable,NonReplicatable {

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
