/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.tasks;

public abstract class Task<T> {

	private boolean done, internalDone;
	private T value;
	private OnFinished<T> onFinished;
	private Thread thread;

	private Object lock = new Object();

	public boolean isDone() {
		return done;
	}

	public T get() {
		if (!done) {
			if (Thread.currentThread().equals(thread)) {
				System.out.println("Unable to lock current thread.");
				return null;
			}
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return value;
	}

	public void onCompleted(T value) {
	}

	/**
	 * <b>INTERNAL METHOD</b>
	 */
	public boolean callI() {
		if (!internalDone) {
			value = call();
			internalDone = true;
		}
		if (completed()) {
			done = true;
			synchronized (lock) {
				lock.notifyAll();
			}
			onCompleted(value);
			if (onFinished != null)
				onFinished.onFinished(value);
			return true;
		} else {
			return false;
		}
	}

	public Task<T> setOnFinished(OnFinished<T> onFinished) {
		this.onFinished = onFinished;
		return this;
	}

	private boolean completed() {
		return true;
	}

	protected abstract T call();

}
