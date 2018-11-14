package engine.application;

import engine.Game;
import engine.GameEngine;
import engine.application.launchstrategy.ServerLauncher;
import engine.observer.Tickable;

public abstract class Application extends GameEngine implements Tickable {

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
			//
		}
	}

	public Application() {
		GameEngine.gameEngine = this;
		game = new Game();
	}

	public void attachTickable( Tickable o ) {
		gameThread.attach(o);
	}

	protected void onStart(String[] args) {
		ServerLauncher.launch(this);
		internalInitialize();
		initialize(args);
	}

	public void internalInitialize() {
		// Nothing here.
	}

	public abstract void initialize(String[] args);
}
