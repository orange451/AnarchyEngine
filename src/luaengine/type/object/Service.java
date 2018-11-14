package luaengine.type.object;

import engine.Game;

public abstract class Service extends Instance {

	public Service(String name) {
		super(name);
		this.forceSetParent(Game.game());
		this.setInstanceable(false);
		
		this.getField("Name").setLocked(true);
		this.getField("Parent").setLocked(true);
	}

	public void onDestroy() {
		//
	}
}
