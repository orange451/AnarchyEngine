package engine.application.launchstrategy;

import engine.GameEngine;
import engine.InternalGameThread;
import engine.application.Application;

public abstract class ServerLauncher {
	public static void launch(Application application) {
		GameEngine.gameThread = new InternalGameThread(application);
	}
}
