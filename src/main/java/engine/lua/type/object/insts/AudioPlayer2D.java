package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class AudioPlayer2D extends Instance implements TreeViewable {
	private static final LuaValue C_SOURCE = LuaValue.valueOf("Source");
	
	public AudioPlayer2D() {
		super("AudioPlayer2D");
		
		this.defineField(C_SOURCE.toString(), LuaValue.NIL, false);
		
		this.getmetatable().set("Play", new ZeroArgFunction() {

			@Override
			public LuaValue call() {
				try {
					playSource();
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		
		// Source MUST be AudioSource
		if ( key.eq_b(C_SOURCE) ) {
			if ( !value.isnil() && !(value instanceof AudioSource) )
				return null;
		}
		
		return value;
	}
	
	public AudioSource getSource() {
		LuaValue t = this.get(C_SOURCE);
		return t.isnil()?null:(AudioSource)t;
	}

	public void playSource() {
		AudioSource source = getSource();
		if ( source == null )
			return;
		
		Game.soundService().playSound(source);
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_sound;
	}
}
