/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine;

import lwjgui.scene.Window;
import lwjgui.scene.layout.StackPane;

public abstract class ClientRunner extends ClientEngine {

	private Window window;

	@Override
	public void setupEngine() {
		window = renderThread.getWindow();
		StackPane displayPane = renderThread.getPipeline().getDisplayPane();
		renderThread.getWindow().getScene().setRoot(displayPane);
		displayPane.getChildren().add(renderThread.getClientUI());

		game.setServer(false);
		// Tell the game to run
		InternalGameThread.runLater(() -> {
			loadScene(args);
			Game.load();
			Game.getGame().gameUpdate(true);
			Game.setRunning(true);
		});
		renderThread.getPipeline().setRenderableWorld(Game.workspace());
	}

	@Override
	public void render() {
		renderThread.getPipeline().setSize(window.getWidth(), window.getHeight());
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	public abstract void loadScene(String[] args);

}
