package engine.al;

import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.codecs.CodecJOgg;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

public class InternalSoundService {
	private SoundSystem soundSystem;

	static {
		try {
			SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
			SoundSystemConfig.setCodec("ogg", CodecJOgg.class);
		} catch (SoundSystemException e) {
			System.out.println("Error loading libraries or codecs");
		}
	}

	public void startSoundSystem() {
		soundSystem = new SoundSystem();
	}

	public void stopSoundSystem() {
		soundSystem.cleanup();
		soundSystem = null;
	}
}
