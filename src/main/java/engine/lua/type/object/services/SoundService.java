package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.al.InternalSoundService;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class SoundService extends Service implements TreeViewable {
	private InternalSoundService internalSound;
	
	public SoundService() {
		super("SoundService");
		
		this.internalSound = new InternalSoundService();
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
		return Icons.icon_sound;
	}
}
