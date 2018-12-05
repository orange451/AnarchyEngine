package engine.io;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import engine.Game;

public class AsynchronousResourceLoader {
	private ScheduledExecutorService THREAD_POOL;
	
	public AsynchronousResourceLoader() {
		THREAD_POOL = Executors.newScheduledThreadPool(32);
	}
	
	public void addResource(AsynchronousResource<?> res) {
		THREAD_POOL.schedule(new AsynchronousLoadTask(res), 2, TimeUnit.MILLISECONDS);
	}
	
	class AsynchronousLoadTask implements Runnable {
		private AsynchronousResource<?> resource;

		public AsynchronousLoadTask(AsynchronousResource<?> res) {
			this.resource = res;
		}

		@Override
		public void run() {
			while(!Game.isLoaded()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			resource.internalLoad();
		}
		
	}
}
