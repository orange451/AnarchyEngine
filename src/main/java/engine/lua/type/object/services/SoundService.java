package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

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
import paulscode.sound.Vector3D;

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
				Vector3 look = camera.getLookVector();
				
				this.internalSound.getSoundSystem().setListenerPosition(position.getX(), position.getY(), position.getZ());
				this.internalSound.getSoundSystem().setListenerOrientation(look.getX(), look.getY(), look.getZ(), 0, 0, 1);
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

	/**
	 * Plays a simple sound in 2d.
	 * @param audioSource
	 * @return
	 */
	public String playSound2D(AudioSource audioSource) {
		return this.playSound2D(audioSource, 1.0f, 1.0f);
	}
	
	/**
	 * Plays a 2d sound with specified volume and pitch multiplier.
	 * @param audioSource
	 * @param volumeMultiplier
	 * @param pitchMultiplier
	 * @return
	 */
	public String playSound2D(AudioSource audioSource, float volumeMultiplier, float pitchMultiplier) {
		Vector3D position = this.internalSound.getSoundSystem().getListenerData().position;
		Vector3 p = new Vector3(position.x, position.y, position.z);
		return this.playSound3D(audioSource, p, volumeMultiplier, pitchMultiplier, 64.0f);
	}
	
	/**
	 * Plays a 3d sound with specified volume and pitch multiplier.
	 * @param audioSource
	 * @param position
	 * @param volumeMultiplier
	 * @param pitchMultiplier
	 * @param range
	 * @return
	 */
	public String playSound3D(AudioSource audioSource, Vector3 position, float volumeMultiplier, float pitchMultiplier, float range) {
		if ( audioSource.getAbsoluteFilePath().length() == 0 )
			return null;
		
		String source = this.internalSound.quickPlay(audioSource.getAbsoluteFilePath(), position.getInternal());
		this.internalSound.getSoundSystem().setVolume(source, audioSource.getVolume()*volumeMultiplier);
		this.internalSound.getSoundSystem().setPitch(source, audioSource.getPitch()*pitchMultiplier);
		
		return source;
	}

	/**
	 * Stops a specific sound
	 * @param source
	 */
	public void stopSound(String source) {
		this.internalSound.getSoundSystem().stop(source);
	}
}
