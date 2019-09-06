package engine;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.application.RenderableApplication;
import engine.observer.InternalRenderable;
import engine.observer.PostRenderable;
import engine.observer.Renderable;
import engine.util.Sync;

public class InternalRenderThread {
	public static float delta;
	public static float fps;
	public static int desiredFPS = 60;

	private int fpscounter;

	private Observable observable;
	private ArrayList<Renderable> externalNotify;
	
	private static List<Runnable> runnables = Collections.synchronizedList(new ArrayList<Runnable>());

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
					externalNotify.get(i).render();
				}

				// Do external post-rendering
				for (int i = 0; i < externalNotify.size(); i++) {
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
		long window = RenderableApplication.window;
		long startTime = System.nanoTime();
		long timer = System.currentTimeMillis();
		double nanoSecond = 1e+9;

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
			Sync.sync( Math.max(Math.min(1000, desiredFPS), 30) );
			
			// Check for window closing
			if ( GLFW.glfwWindowShouldClose(window) ) {
				InternalGameThread.terminate();
			}
		}

		cleanup();
	}
	
	private void cleanup() {
		// TODO clean up loaded OpenGL data...
		
		// Close window
		long window = RenderableApplication.window;
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	public static void runLater(Runnable runnable) {
		synchronized(runnables) {
			runnables.add(runnable);
		}
	}
	
	public void forceUpdate() {
		long window = RenderableApplication.window;

		// Clear screen
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
		GL11.glClearColor(1.0f,1.0f,1.0f, 1);

		// Poll user input
		GLFW.glfwPollEvents();
		
		// Set context to the window
		GLFW.glfwMakeContextCurrent(window);

		// Draw event
		if ( Game.isLoaded() ) {
			Game.runService().renderPreEvent().fire(LuaValue.valueOf(delta));
			Game.runService().renderSteppedEvent().fire(LuaValue.valueOf(delta));
			observable.notifyObservers();
			Game.runService().renderPostEvent().fire(LuaValue.valueOf(delta));
		}
		
		// Run runnables
		synchronized(runnables) {
			while ( runnables.size() > 0 ) {
				Runnable r = runnables.get(0);
				r.run();
				runnables.remove(r);
			}
		}

		// Send commands to GPU
		GLFW.glfwSwapBuffers(window);
	}
}
