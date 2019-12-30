/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide;

import java.io.IOException;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.LegacyPipeline;
import engine.glv2.GLRenderer;
import engine.io.Load;
import engine.lua.type.object.ScriptBase;
import ide.layout.IdeLayout;
import ide.layout.windows.IdeLuaEditor;
import lwjgui.LWJGUI;
import lwjgui.scene.Group;
import lwjgui.scene.Window;
import lwjgui.scene.layout.Pane;

public class IDE extends RenderableApplication {
	public static IdeLayout layout;
	public static Window win;
	
	public static final String TITLE = "Anarchy Engine - Build " + Game.version();
	
	@Override
	public void initialize(String[] args) {
		// Window title
		GLFW.glfwSetWindowTitle(window, TITLE);
		
		// Setup LWJGUI
		win = LWJGUI.initialize(window, true);
		win.setWindowAutoDraw(false); // We want to draw the main IDE window manually
		win.setWindowAutoClear(false); // We want control of clearing
		
		InternalRenderThread.desiredFPS = 30;
		InternalGameThread.desiredTPS = 60;
		
		// Setup background pane
		Group background = new Group();
		win.getScene().setRoot(background);
		
		//Theme.setTheme(new ThemeDark());
		
		// Redraw window if resized
		win.addEventListener(new lwjgui.event.listener.WindowSizeListener() {
			@Override
			public void invoke(long arg0, int arg1, int arg2) {
				renderThread.forceUpdate();
			}
		});
		
		// Create rendering pipeline
		//this.attachRenderable(pipeline = new LegacyPipeline());
		this.attachRenderable(pipeline = new GLRenderer());
		
		// Setup mane IDE layout
		layout = new IdeLayout(background);
		
		// If someone wants to load a game directly
		if ( args != null && args.length > 0 ) {
			
			// Get project args
			String[] tempArgs = new String[Math.max(0, args.length-1)];
			for (int i = 1; i < args.length; i++) {
				tempArgs[i-1] = args[i];
			}
			
			// Tell game we're a client
			if ( args[0].toLowerCase().equals("client") ) {
				game.setServer(false); // Mark this game as a client
			}
			
			// Load project
			if ( tempArgs.length > 0 ) {
				Load.load(tempArgs[0]);
				InternalRenderThread.runLater(()->{
					InternalGameThread.runLater(() -> {
						InternalRenderThread.runLater(()->{
							Game.setRunning(true);
							InternalGameThread.desiredTPS = 60;
						});
					});
				});
			}
		}
	}
	
	@Override
	protected boolean shouldLockMouse() {
		return Game.isLoaded() && layout.getGamePane().isDescendentHovered();
	}
	
	@Override
	protected Vector2f getMouseOffset() {
		return new Vector2f( (float)layout.getGamePane().getX(), (float)layout.getGamePane().getY() );
	}
	
	@Override
	public void render() {
		if ( GLFW.glfwWindowShouldClose(window) )
			return;
		
		// Pipeline needs to draw our workspace
		pipeline.setRenderableWorld(Game.workspace());
		
		// Render UI:
		//   (final 3d scene gets rendered as a component)
		//   (3d scene is attached to render thread)
		LWJGUI.render();
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub
	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}

	public static void openScript(ScriptBase instance) {
		IdeLuaEditor lua = new IdeLuaEditor((ScriptBase) instance);
		layout.getCenter().dock(lua);
	}
}