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

import engine.lua.type.object.services.UserInputService;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.glfw.input.MouseHandler;
import lwjgui.paint.Color;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.StackPane;

public abstract class ClientRunner extends ClientEngine {

	public static final String TITLE = "Anarchy Engine - Client Runner Build " + Game.version();

	public ClientRunner(String... args) {
		super(args);
	}

	private Label fps;

	@Override
	public void setupEngine() {
		renderThread.getWindow().setTitle(TITLE);
		StackPane displayPane = renderThread.getPipeline().getDisplayPane();
		renderThread.getWindow().getScene().setRoot(displayPane);
		displayPane.getChildren().add(renderThread.getClientUI());

		fps = new Label("fps");
		fps.setTextFill(Color.WHITE);
		fps.setMouseTransparent(true);

		displayPane.getChildren().add(fps);
		displayPane.setAlignment(Pos.TOP_LEFT);
		displayPane.setPadding(new Insets(2, 2, 2, 2));

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
		fps.setText(InternalRenderThread.fps + " fps");
	}

	private boolean grabbed;

	@Override
	public void update() {
		if (UserInputService.lockMouse) {
			if (!grabbed) {
				grabbed = true;
				MouseHandler.setGrabbed(renderThread.getWindow().getID(), true);
			}
		} else {
			if (grabbed) {
				MouseHandler.setGrabbed(renderThread.getWindow().getID(), false);
				grabbed = false;
			}
		}
	}

	public abstract void loadScene(String[] args);

}
