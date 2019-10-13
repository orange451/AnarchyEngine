package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class StarterPlayer extends Service implements TreeViewable {

	private static final LuaValue C_STARTERPLAYERSCRIPTS = LuaValue.valueOf("StarterPlayerScripts");
	
	public StarterPlayer() {
		super("StarterPlayer");
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
		return Icons.icon_starter_player;
	}

	public Instance starterPlayerScripts() {
		return findFirstChild(C_STARTERPLAYERSCRIPTS);
	}
}
