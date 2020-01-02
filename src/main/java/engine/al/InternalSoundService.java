/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.al;

import org.joml.Vector3f;
import org.lwjgl.openal.ALC;
import org.lwjgl.system.Configuration;

import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.codecs.CodecJOgg;
import paulscode.sound.codecs.CodecWav;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

public class InternalSoundService {
	private SoundSystem soundSystem;

	static {
		Configuration.OPENAL_EXPLICIT_INIT.set(true); // Move this to another place
		ALC.create();
		try {
			SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
			SoundSystemConfig.setCodec("ogg", CodecJOgg.class);
			SoundSystemConfig.setCodec("wav", CodecWav.class);
		} catch (SoundSystemException e) {
			System.out.println("Error loading libraries or codecs");
		}
	}

	public void startSoundSystem() {
		soundSystem = new SoundSystem();
	}

	public void stopSoundSystem() {
		System.out.println("Stopping sound system...");
		if (soundSystem != null)
			soundSystem.cleanup();
		soundSystem = null;
		System.out.println("Sound system stopped");
	}

	public String quickPlay(String filepath, Vector3f position) {
		if (soundSystem == null)
			return null;

		return soundSystem.quickPlay(false, filepath, false, position.x, position.y, position.z,
				SoundSystemConfig.ATTENUATION_ROLLOFF, SoundSystemConfig.getDefaultRolloff());
	}

	public SoundSystem getSoundSystem() {
		return this.soundSystem;
	}
}
