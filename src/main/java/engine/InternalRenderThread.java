/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine;


import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.lua.type.object.services.RunService;
import engine.observer.InternalRenderable;
import engine.observer.PostRenderable;
import engine.observer.Renderable;
import engine.tasks.TaskManager;
import engine.tasks.ThreadUtils;
import engine.util.Sync;

public class InternalRenderThread {
	public static float delta;
	public static float fps;
	public static int desiredFPS = 60;

	private int fpscounter;

	private Observable observable;
	private ArrayList<Renderable> externalNotify;
	
	public InternalRenderThread(Renderable observer) {
		this.observable = new Observable() {
			@Override
			public void notifyObservers(Object o){
				externalNotify.clear();

				// Do internal rendering
				setChanged();
				super.notifyObservers(o);

				// Do external rendering
				for (int i = 0; i < externalNotify.size(); i++) {
					if ( !InternalGameThread.isRunning() )
						continue;
					externalNotify.get(i).render();
				}

				// Do external post-rendering
				for (int i = 0; i < externalNotify.size(); i++) {
					if ( !InternalGameThread.isRunning() )
						continue;
					
					if ( externalNotify.get(i) instanceof PostRenderable ) {
						((PostRenderable)externalNotify.get(i)).postRender();
					}
				}
			}
		};

		this.externalNotify = new ArrayList<Renderable>();
		attach(observer);
	}

	public void attach(final Renderable observer) {
		observable.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if ( observer instanceof InternalRenderable ) {
					((InternalRenderable)observer).internalRender();
				}
				externalNotify.add(observer);
			}
		});
	}

	public void run() {
		long window = AnarchyEngineClient.window;
		long startTime = System.nanoTime();
		long timer = System.currentTimeMillis();
		double nanoSecond = 1e+9;
		Sync sync = new Sync();

		while ( InternalGameThread.isRunning() ) {
			//long t1 = System.nanoTime();
			forceUpdate();
			//long t2 = System.nanoTime();

			// Calculate FPS
			fpscounter++;
			if (System.currentTimeMillis() - timer >= 1000) {
				timer = System.currentTimeMillis();
				fps = fpscounter;
				fpscounter = 0;
			}

			// Calculate delta
			delta = (float) ((System.nanoTime() - startTime)/nanoSecond);
			startTime = System.nanoTime();

			// Sync
			sync.sync( Math.max(Math.min(1000, desiredFPS), 30) );
			
			// Check for window closing
			if ( GLFW.glfwWindowShouldClose(window) ) {
				AnarchyEngine.instance.terminate();
			}
		}
		TaskManager.stopRenderBackgroundThread();
		while(!InternalGameThread.isDone())
			ThreadUtils.sleep(100);
		TaskManager.runAndStopRenderThread();
		cleanup();
	}
	
	private void cleanup() {
		// TODO clean up loaded OpenGL data...
	}

	public static void runLater(Runnable runnable) {
		TaskManager.addTaskRenderThread(runnable);
	}
	
	public void forceUpdate() {
		long window = AnarchyEngineClient.window;
		GLFW.glfwMakeContextCurrent(window);

		TaskManager.updateRenderThread();

		// Clear screen
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
		GL11.glClearColor(1.0f,1.0f,1.0f, 1);

		// Poll user input
		GLFW.glfwPollEvents();
		
		// Set context to the window

		// Draw event
		if ( Game.isLoaded() ) {
			RunService runService = Game.runService();
			if ( runService == null )
				return;
			
			runService.renderPreEvent().fire(LuaValue.valueOf(delta));
			runService.renderSteppedEvent().fire(LuaValue.valueOf(delta));
			observable.notifyObservers();
			runService.renderPostEvent().fire(LuaValue.valueOf(delta));
		}
		
		// Send commands to GPU
		GLFW.glfwSwapBuffers(window);
	}
}
