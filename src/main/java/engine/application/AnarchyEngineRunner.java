/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.application;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.AnarchyEngineClient;
import engine.Game;
import engine.InternalRenderThread;
import lwjgui.LWJGUI;

public abstract class AnarchyEngineRunner extends AnarchyEngineClient {
	@Override
	public void initialize(String[] args) {
		win.getScene().setRoot(pipeline.getDisplayPane());
		pipeline.getDisplayPane().getChildren().add(AnarchyEngineClient.uiNode);
		
		// Tell the game to run
		InternalRenderThread.runLater(()->{
			loadScene(args);
			Game.load();
			Game.getGame().gameUpdate(true);
			Game.setRunning(true);
		});
	}

	@Override
	public void render() {
		if ( GLFW.glfwWindowShouldClose(window) )
			return;
		
		// Pipeline needs to draw our workspace
		pipeline.setRenderableWorld(Game.workspace());
		
		// Render pipeline
		pipeline.setSize(windowWidth, windowHeight);
		pipeline.render();
		
		// Set viewport
		GL11.glViewport(0, 0, viewportWidth, viewportHeight);
		
		LWJGUI.render(false); // Gets directly rendered on-top of buffer (in same FBO)
	}
	
	public abstract void loadScene(String[] args);

	@Override
	public void tick() {
		//
	}
}
