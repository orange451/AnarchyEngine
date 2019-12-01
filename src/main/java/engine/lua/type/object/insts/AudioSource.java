package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.services.Assets;
import ide.layout.windows.icons.Icons;

public class AudioSource extends AssetLoadable implements TreeViewable {
	private static final LuaValue C_PLAY = LuaValue.valueOf("Play");
	private static final LuaValue C_VOLUME = LuaValue.valueOf("Volume");
	private static final LuaValue C_PITCH = LuaValue.valueOf("Pitch");
	
	public AudioSource() {
		super("AudioSource");

		this.defineField(C_PLAY.toString(), LuaValue.FALSE, false);
		
		this.defineField(C_VOLUME.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_VOLUME).setClamp(new NumberClampPreferred(0, 10, 0, 1));
		
		this.defineField(C_PITCH.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_PITCH).setClamp(new NumberClampPreferred(0, 8, 0, 2));
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
		Game.soundService().playSound2D(this);
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

	public float getVolume() {
		return this.get(C_VOLUME).tofloat();
	}
	
	public void setVolume(float volume) {
		this.set(C_VOLUME, LuaValue.valueOf(volume));
	}

	public float getPitch() {
		return this.get(C_PITCH).tofloat();
	}
	
	public void setPitch(float pitch) {
		this.set(C_PITCH, LuaValue.valueOf(pitch));
	}
}
