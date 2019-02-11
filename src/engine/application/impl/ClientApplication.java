package engine.application.impl;

import java.io.IOException;

import engine.Game;

public class ClientApplication extends ServerApplication {
	
	public ClientApplication() {
		game = new Game(); // Overwrite the game object with the client one.
		game.setServer(false);
	}
	
	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
