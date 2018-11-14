package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.RunScript;
import luaengine.type.object.TreeViewable;

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
