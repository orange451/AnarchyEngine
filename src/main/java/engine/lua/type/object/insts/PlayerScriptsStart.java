package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class PlayerScriptsStart extends Instance implements TreeViewable {

	public PlayerScriptsStart() {
		super("StarterPlayerScripts");

		this.setLocked(true);
		this.setInstanceable(false);
		
		InternalGameThread.runLater(()->{
			if ( destroyed )
				return;
			
			Instance ss = Game.starterPlayer();
			
			if ( !this.getParent().eq_b(ss) ) {
				this.forceSetParent(ss);
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
		return Icons.icon_player_scripts;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
	}
}
