package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;

public class Player extends Instance implements TreeViewable {

	public Player() {
		super("Player");
		
		this.defineField("Character", LuaValue.NIL, false);
		this.defineField("Connection", LuaValue.NIL, false);

		this.rawset("Archivable", LuaValue.valueOf(false));
		
		this.setInstanceable(false);
		this.setLocked(true);
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
		if ( this.getCharacter() != null ) {
			this.getCharacter().destroy();
		}
	}
	
	public Instance getCharacter() {
		LuaValue c = this.get("Character");
		return (!c.isnil() && c instanceof Instance)?(Instance)c:null;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_player;
	}
}
