package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.services.Assets;
import ide.layout.windows.icons.Icons;

public class AudioSource extends AssetLoadable implements TreeViewable {
	
	public AudioSource() {
		super("AudioSource");
		
		this.getmetatable().set("Play", new ZeroArgFunction() {

			@Override
			public LuaValue call() {
				try {
					Game.soundService().playSound(AudioSource.this);
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key) ) {
			//changed = true;
		}
		return value;
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

	@Override
	public LuaValue getPreferredParent() {
		return Assets.C_AUDIO;
	}

	public static String getFileTypes() {
		return "midi,ogg";
	}
}
