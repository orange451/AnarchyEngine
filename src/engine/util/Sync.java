package engine.util;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Sync {
	/** number of nano seconds in a second */
	private static final long NANOS_IN_SECOND = TimeUnit.SECONDS.toNanos(1);

	/** The time to sleep/yield until the next frame */
	private static HashMap<Thread,Long> nextFrameMap;

	/** initialise above fields */
	static {
		nextFrameMap = new HashMap<Thread,Long>();
	}
	
	/**
	 * Attempts to sync the thread to the desired FPS
	 * @param fps
	 */
	public static void sync(int fps) {
		Thread thread = checkThread();
		long CurrentTime = System.nanoTime();
		long NextFrame = nextFrameMap.get(thread).longValue();
		long FrameInNanos = NANOS_IN_SECOND / fps;
		long SleepTime = Math.min(FrameInNanos, Math.max(0, NextFrame-CurrentTime));
		long SleepMillis = TimeUnit.NANOSECONDS.toMillis(SleepTime);
		try {
			Thread.sleep(SleepMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
				
		nextFrameMap.put( thread, System.nanoTime() + FrameInNanos );
	}

	private static Thread checkThread() {
		Thread c = Thread.currentThread();

		if ( !nextFrameMap.containsKey(c) )
			nextFrameMap.put(c, (long) System.nanoTime());
		
		return c;
	}
}
