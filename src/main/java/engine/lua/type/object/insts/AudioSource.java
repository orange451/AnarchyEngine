package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.services.Assets;
import ide.layout.windows.icons.Icons;

public class AudioSource extends AssetLoadable implements TreeViewable {
	private static final LuaValue C_PLAY = LuaValue.valueOf("Play");
	
	public AudioSource() {
		super("AudioSource");
		
		this.defineField(C_PLAY.toString(), LuaValue.FALSE, false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key) ) {
			//changed = true;
		}
		
		if ( key.eq_b(C_PLAY) ) {
			playSource();
			value = LuaValue.FALSE;
		}
		return value;
	}

	public void playSource() {
		Game.soundService().playSound(this);
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
		return "wav,ogg,midi";
	}
}
