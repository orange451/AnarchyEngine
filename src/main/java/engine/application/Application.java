/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.application;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.application.launchstrategy.ServerLauncher;
import engine.observer.Renderable;
import engine.observer.Tickable;

public abstract class Application implements Tickable {
	public static Application instance = null;
	public static InternalRenderThread renderThread = null;
	public static InternalGameThread gameThread = null;
	public static Game game;
	
	public static boolean isRenderable() {
		return instance instanceof Renderable;
	}
	
	public static void launch(Object o) {
		// Figure out the right class to call
		StackTraceElement[] cause = Thread.currentThread().getStackTrace();

		boolean foundThisMethod = false;
		String callingClassName = null;
		for (StackTraceElement se : cause) {
			// Skip entries until we get to the entry for this class
			String className = se.getClassName();
			String methodName = se.getMethodName();
			if (foundThisMethod) {
				callingClassName = className;
				break;
			} else if (Application.class.getName().equals(className) && "launch".equals(methodName)) {
				foundThisMethod = true;
			}
		}

		if (callingClassName == null) {
			throw new RuntimeException("Error: unable to determine Application class");
		}

		try {
			Class<?> theClass = Class.forName(callingClassName, true, Thread.currentThread().getContextClassLoader());
			Object app = theClass.newInstance();
			Application application = (Application) app;
			application.onStart((String[]) o);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public Application() {
		instance = this;
		game = new Game();
	}

	public void attachTickable( Tickable o ) {
		gameThread.attach(o);
	}

	protected void onStart(String[] args) {
		setupEngine();
		ServerLauncher.launch(this);
		initialize(args);
	}
	
	public void terminate() {
		cleanupEngine();
	}

	protected abstract void setupEngine();
	protected abstract void cleanupEngine();
	public abstract void initialize(String[] args);
}
