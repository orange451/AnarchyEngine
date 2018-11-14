package luaengine.type.object.services;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import ide.layout.windows.icons.Icons;
import luaengine.type.LuaEvent;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;
import luaengine.type.object.insts.Player;

public class Players extends Service implements TreeViewable {

	public Players() {
		super("Players");

		this.defineField("LocalPlayer", LuaValue.NIL, true);
		
		this.rawset("PlayerAdded",	new LuaEvent());
		
		// Fire player added when a player is added
		LuaEvent added = (LuaEvent) this.rawget("ChildAdded");
		added.connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Player ) {
					((LuaEvent)Players.this.rawget("PlayerAdded")).fire(arg);
				}
				return LuaValue.NIL;
			}
		});
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
		return Icons.icon_players;
	}
}
