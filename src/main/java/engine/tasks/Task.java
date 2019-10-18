/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.tasks;

import java.util.ArrayList;
import java.util.List;

public abstract class Task<T> {

	private boolean done;
	private T value;
	private List<Thread> ts = new ArrayList<>();
	private OnFinished<T> onFinished;
	private Thread thread;

	public boolean isDone() {
		return done;
	}

	public T get() {
		if (!done) {
			if (Thread.currentThread().equals(thread)) {
				System.out.println("Unable to lock current thread.");
				return null;
			}
			synchronized (ts) {
				ts.add(Thread.currentThread());
			}
			try {
				Thread.sleep(Long.MAX_VALUE);
			} catch (InterruptedException e) {
			}
		}
		return value;
	}

	public void onCompleted(T value) {
	}

	/**
	 * <b>INTERNAL FUNCTION</b>
	 */
	public void callI() {
		if (done)
			return;
		value = call();
		done = true;
		for (Thread t : ts)
			t.interrupt();
		onCompleted(value);
		if (onFinished != null)
			onFinished.onFinished(value);
	}

	public Task<T> setOnFinished(OnFinished<T> onFinished) {
		this.onFinished = onFinished;
		return this;
	}

	protected abstract T call();

}
