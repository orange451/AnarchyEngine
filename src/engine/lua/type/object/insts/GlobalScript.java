package engine.lua.type.object.insts;

import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class GlobalScript extends ScriptBase implements TreeViewable {

	public GlobalScript() {
		super("GlobalScript");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_script_global;
	}

	@Override
	public boolean getCanRun() {
		return true;
	}
}
