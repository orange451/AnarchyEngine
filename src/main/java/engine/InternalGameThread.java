package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.luaj.vm2.LuaValue;

import engine.application.Application;
import engine.lua.LuaEngine;
import engine.lua.type.ScriptData;
import engine.lua.type.object.Service;
import engine.lua.type.object.services.RunService;
import engine.observer.Tickable;
import engine.tasks.TaskManager;
import engine.util.Sync;

public class InternalGameThread extends Observable implements Runnable {
    private static boolean running = false;
    private static boolean done = false;

	public static int tps;
	public static float delta;
	
	public static int desiredTPS = 60;
	
	private static List<Runnable> runnables = Collections.synchronizedList(new ArrayList<Runnable>());
	
	public InternalGameThread(Application observer) {
		attach(observer);
		running = true;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void attach(final Tickable observer) {
		this.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				observer.tick();
			}
		});
	}
	
	public static void runLater(Runnable runnable) {
		synchronized(runnables) {
			runnables.add(runnable);
		}
	}
	
    @Override
    public void notifyObservers(Object o){
        setChanged();
        super.notifyObservers(o);
    }

	@Override
	public void run() {
		TaskManager.init();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			//
		}
		
		double nanoSecond = 1e+9;
		long lastTime = System.nanoTime();
		while(running) {
			TaskManager.updateMainThread();

			// Limit TPS to desiredTPS max
			float cvtps = Math.max(Math.min(120, desiredTPS), 2);

			// Get approximate delta time
			double ticksPerSecond = nanoSecond/cvtps;

			// Handle game logic
			long now = System.nanoTime();
			if (now - lastTime > ticksPerSecond) {

				// Calculate tps time
				int t = (int) Math.round(nanoSecond / (now - lastTime));
				if ( t < 30 ) // Cap to a minimum of 30
					t = 30;
				tps = t;

				delta = (now - lastTime) / (float)nanoSecond;
				lastTime = now;
				
				if ( LuaEngine.globals == null )
					continue;
				
				if ( Game.game() == null )
					continue;
				
				if ( !Game.isLoaded() )
					continue;
				
				if ( Game.core() == null )
					continue;
				
				RunService runService = Game.runService();
				if ( runService != null && Game.isRunning() ) {
					runService.heartbeatEvent().fire(LuaValue.valueOf(delta));
				}
				Game.getGame().tick();
				this.notifyObservers();
				

				// Run runnables
				synchronized(runnables) {
					while ( runnables.size() > 0 ) {
						Runnable r = runnables.get(0);
						r.run();
						runnables.remove(0);
					}
				}

				Sync.sync( (int)cvtps );
			}
		}
		TaskManager.runAndStopMainThread();
		cleanup();
	}
	
	private void cleanup() {
		// Clean up all lua objects
		Game.unload();
		Game.setRunning(false);
		
		// Stop services
		ArrayList<Service> services = Game.getServices();
		for (int i = 0; i < services.size(); i++) {
			services.get(i).destroy();
		}
		
		// Disable scripts
		ScriptData.shutdown();
		Game.resourceLoader().shutdown();
		done = true;
	}

	public static void terminate() {
		running = false;
		System.out.println("Terminating game thread");
	}

	public static boolean isRunning() {
		return running;
	}

	public static boolean isDone() {
		return done;
	}
}
