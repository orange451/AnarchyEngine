package engine;

import engine.observer.Renderable;
import engine.observer.Tickable;

public abstract class GameEngine implements Tickable {
	public static GameEngine gameEngine = null;
	public static InternalRenderThread renderThread = null;
	public static InternalGameThread gameThread = null;
	public static Game game;
	
	public static boolean isRenderable() {
		return gameEngine instanceof Renderable;
	}
}
