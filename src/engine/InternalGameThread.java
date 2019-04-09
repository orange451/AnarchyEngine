package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.luaj.vm2.LuaValue;

import engine.application.Application;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.services.RunService;
import engine.observer.Tickable;
import engine.util.Sync;

public class InternalGameThread extends Observable implements Runnable {
    private static boolean running = false;

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
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			//
		}
		
		double nanoSecond = 1e+9;
		long lastTime = System.nanoTime();
		while(running) {

			// Limit TPS to desiredTPS max
			float cvtps = Math.max(Math.min(120, desiredTPS), 10);

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
				
				if ( Game.isLoaded() ) {
					RunService runService = (RunService)Game.getService("RunService");
					if ( runService != null ) {
						((LuaEvent)runService.get("Heartbeat")).fire(LuaValue.valueOf(delta));
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
				}

				Sync.sync( (int)cvtps );
			}
		}

		running = false;
		System.exit(0);
	}

	public boolean isRunning() {
		return running;
	}
}
