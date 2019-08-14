package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.Game;

public abstract class Service extends Instance {

	public Service(String name) {
		super(name);
		this.forceSetParent(Game.game());
		this.setInstanceable(false);
		
		this.getField(LuaValue.valueOf("Name")).setLocked(true);
		this.getField(LuaValue.valueOf("Parent")).setLocked(true);
	}

	public void onDestroy() {
		//
	}
}
