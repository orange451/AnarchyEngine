package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.services.Assets;
import ide.layout.windows.icons.Icons;

public class AudioSource extends AssetLoadable implements TreeViewable {
	
	public AudioSource() {
		super("AudioSource");
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
		return "ogg";
	}
}
