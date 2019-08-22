package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.RunScript;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class PlayerScripts extends Instance implements TreeViewable,RunScript {

	public PlayerScripts() {
		super("PlayerScripts");

		this.setLocked(true);
		this.setInstanceable(false);
		
		this.rawset("Archivable", LuaValue.valueOf(false));
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
		return Icons.icon_player_scripts;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
	}
}
