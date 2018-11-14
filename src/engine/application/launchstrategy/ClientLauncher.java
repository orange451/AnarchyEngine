package engine.application.launchstrategy;

import engine.GameEngine;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.util.JVMUtil;

public class ClientLauncher {

	public static void launch(RenderableApplication application) {
		if ( JVMUtil.restartJVM(true, true, null) ) {
			return;
		}
		
		// Start server game-logic thread
		ServerLauncher.launch(application);
		
		// Start rendering thread
		GameEngine.renderThread = new InternalRenderThread(application);
	}
}
