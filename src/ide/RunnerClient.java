package ide;

import java.io.IOException;

import engine.Game;
import luaengine.type.object.insts.Player;

public class RunnerClient extends RunnerServer {
	
	public RunnerClient() {
		game = new Game(); // Overwrite the game object with the client one.
		game.setServer(false);
	}
	
	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
