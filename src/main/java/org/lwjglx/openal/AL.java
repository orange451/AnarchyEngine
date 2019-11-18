package org.lwjglx.openal;

import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.ALC_FREQUENCY;
import static org.lwjgl.openal.ALC10.ALC_REFRESH;
import static org.lwjgl.openal.ALC10.ALC_SYNC;
import static org.lwjgl.openal.ALC10.ALC_TRUE;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetInteger;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;
import static org.lwjgl.openal.ALC11.ALC_STEREO_SOURCES;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALUtil;

public class AL {

	static long device;
	static long context;

	private static boolean created = false;

	public static void create() throws IllegalStateException {
		device = alcOpenDevice((ByteBuffer) null);
		if (device == NULL)
			throw new IllegalStateException("Failed to open the default device.");

		ALCCapabilities deviceCaps = ALC.createCapabilities(device);

		System.out.println("OpenALC10: " + deviceCaps.OpenALC10);
		System.out.println("OpenALC11: " + deviceCaps.OpenALC11);
		System.out.println("ALC_EXT_EFX = " + deviceCaps.ALC_EXT_EFX);

		if (deviceCaps.OpenALC11) {
			List<String> devices = ALUtil.getStringList(device, ALC_ALL_DEVICES_SPECIFIER);
			if (devices == null)
				throw new IllegalStateException("Unable to initialize devices");
			else {
				for (int i = 0; i < devices.size(); i++)
					System.out.println(i + ": " + devices.get(i));
			}
		}

		String defaultDeviceSpecifier = alcGetString(device, ALC_DEFAULT_DEVICE_SPECIFIER);
		System.out.println("Default device: " + defaultDeviceSpecifier);

		context = alcCreateContext(device, (IntBuffer) null);
		alcMakeContextCurrent(context);
		org.lwjgl.openal.AL.createCapabilities(deviceCaps);

		System.out.println("ALC_FREQUENCY: " + alcGetInteger(device, ALC_FREQUENCY) + "Hz");
		System.out.println("ALC_REFRESH: " + alcGetInteger(device, ALC_REFRESH) + "Hz");
		System.out.println("ALC_SYNC: " + (alcGetInteger(device, ALC_SYNC) == ALC_TRUE));
		System.out.println("ALC_MONO_SOURCES: " + alcGetInteger(device, ALC_MONO_SOURCES));
		System.out.println("ALC_STEREO_SOURCES: " + alcGetInteger(device, ALC_STEREO_SOURCES));
		created = true;
	}

	public static boolean isCreated() {
		return created;
	}

	public static void destroy() {
		if (!created)
			return;
		created = false;
		alcMakeContextCurrent(NULL);
		alcDestroyContext(context);
		alcCloseDevice(device);
	}

}
