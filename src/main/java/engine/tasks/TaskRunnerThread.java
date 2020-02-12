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

import java.util.Queue;

public class TaskRunnerThread extends Thread implements ITaskRunnerThread {

	protected Queue<Task<?>> tasks;
	protected Object threadsLock;
	protected boolean running = true;

	public TaskRunnerThread(Queue<Task<?>> tasks, Object threadsLock) {
		this.tasks = tasks;
		this.threadsLock = threadsLock;
	}

	@Override
	public void run() {
		while (running) {
			if (!tasks.isEmpty()) {
				Task<?> task = tasks.poll();
				if (task != null)
					task.callI();
			} else
				synchronized (threadsLock) {
					try {
						threadsLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
	}

	@Override
	public void setRunning(boolean running) {
		this.running = running;
	}

}
