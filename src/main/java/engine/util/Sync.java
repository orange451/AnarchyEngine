/*
 * Copyright (c) 2002-2012 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package engine.util;

/**
 * A highly accurate sync method that continually adapts to the system it runs
 * on to provide reliable results.
 *
 * @author Riven
 * @author kappaOne
 */
public class Sync {
	
	static {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Win")) {
			// On windows the sleep functions can be highly inaccurate by
			// over 10ms making in unusable. However it can be forced to
			// be a bit more accurate by running a separate sleeping daemon
			// thread.
			Thread timerAccuracyThread = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(Long.MAX_VALUE);
					} catch (Exception e) {
					}
				}
			});

			timerAccuracyThread.setName("LWJGL Timer");
			timerAccuracyThread.setDaemon(true);
			timerAccuracyThread.start();
		}
	}

	private static final long NANOS_IN_SECOND = 1000L * 1000L * 1000L;
	private long nextFrame = 0;
	private RunningAvg sleepDurations = new RunningAvg(10);
	private RunningAvg yieldDurations = new RunningAvg(10);
	private double lastLoopTime;
	public float timeCount;

	public void sync(int fps) {
		if (fps <= 0)
			return;
		try {
			for (long t0 = getTime(), t1; (nextFrame - t0) > sleepDurations.avg(); t0 = t1) {
				Thread.sleep(1);
				sleepDurations.add((t1 = getTime()) - t0); // update average
															// sleep time
			}
			sleepDurations.dampenForLowResTicker();
			for (long t0 = getTime(), t1; (nextFrame - t0) > yieldDurations.avg(); t0 = t1) {
				Thread.yield();
				yieldDurations.add((t1 = getTime()) - t0); // update average
															// yield time
			}
		} catch (InterruptedException e) {

		}

		nextFrame = Math.max(nextFrame + NANOS_IN_SECOND / fps, getTime());
	}

	public Sync() {
		sleepDurations.init(1000 * 1000);
		yieldDurations.init((int) (-(getTime() - getTime()) * 1.333));
		nextFrame = getTime();
		lastLoopTime = System.nanoTime();
	}

	public float getDelta() {
		long time = System.nanoTime();
		double delta = (float) (time - this.lastLoopTime);
		this.lastLoopTime = time;
		this.timeCount += delta / 1000000000;
		return (float) (delta / 1000000000);
	}

	/**
	 * Get the system time in nano seconds
	 * 
	 * @return will return the current time in nano's
	 */
	private static long getTime() {
		return System.nanoTime();
	}

	private class RunningAvg {
		private final long[] slots;
		private int offset;

		private static final long DAMPEN_THRESHOLD = 10 * 1000L * 1000L; // 10ms
		private static final float DAMPEN_FACTOR = 0.9f; // don't change: 0.9f
															// is exactly right!

		public RunningAvg(int slotCount) {
			this.slots = new long[slotCount];
			this.offset = 0;
		}

		public void init(long value) {
			while (this.offset < this.slots.length) {
				this.slots[this.offset++] = value;
			}
		}

		public void add(long value) {
			this.slots[this.offset++ % this.slots.length] = value;
			this.offset %= this.slots.length;
		}

		public long avg() {
			long sum = 0;
			for (int i = 0; i < this.slots.length; i++) {
				sum += this.slots[i];
			}
			return sum / this.slots.length;
		}

		public void dampenForLowResTicker() {
			if (this.avg() > DAMPEN_THRESHOLD) {
				for (int i = 0; i < this.slots.length; i++) {
					this.slots[i] *= DAMPEN_FACTOR;
				}
			}
		}
	}

}