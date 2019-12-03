package engine.lua.type.object;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.insts.AudioSource;
import ide.layout.windows.icons.Icons;

public class AudioPlayerBase extends Instance implements TreeViewable {
	private static final LuaValue C_SOURCE = LuaValue.valueOf("Source");
	private static final LuaValue C_VOLUME = LuaValue.valueOf("Volume");
	private static final LuaValue C_PITCH = LuaValue.valueOf("Pitch");
	
	public AudioPlayerBase(String name) {
		super(name);
		
		this.defineField(C_SOURCE.toString(), LuaValue.NIL, false);
		
		this.defineField(C_VOLUME.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_VOLUME).setClamp(new NumberClampPreferred(0, 10, 0, 2));
		
		this.defineField(C_PITCH.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_PITCH).setClamp(new NumberClampPreferred(0, 16, 0, 4));
		
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
		
		Game.soundService().playSound2D(source, getVolume(), getPitch());
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
