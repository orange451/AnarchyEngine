package engine.lua.type.object.services;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.InternalGameThread;
import engine.al.InternalSoundService;
import engine.lua.type.LuaConnection;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AudioSource;
import engine.lua.type.object.insts.Camera;
import ide.layout.windows.icons.Icons;

public class SoundService extends Service implements TreeViewable {
	private InternalSoundService internalSound;
	private LuaConnection connection;
	
	public SoundService() {
		super("SoundService");
		
		this.internalSound = new InternalSoundService();
		this.internalSound.startSoundSystem();
		
		// Update listener
		InternalGameThread.runLater(()->{
			connection = Game.runService().renderPreEvent().connect((args)->{
				Camera camera = Game.workspace().getCurrentCamera();
				if ( camera == null )
					return;
				
				Vector3 position = camera.getPosition();
				
				this.internalSound.getSoundSystem().setListenerPosition(position.getX(), position.getY(), position.getZ());
			});
		});
	}
	
	@Override
	public void onDestroy() {
		if ( this.internalSound != null )
			this.internalSound.stopSoundSystem();
		
		if ( this.connection != null )
			this.connection.disconnect();
		
		this.internalSound = null;
		this.connection = null;
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

	public void playSound(AudioSource audioSource) {
		this.internalSound.quickPlay(audioSource.getAbsoluteFilePath(), new Vector3f());
	}
}
