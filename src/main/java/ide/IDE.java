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

import engine.AnarchyEngineClient;
import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.io.Load;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.ui.CSS;
import ide.layout.IdeLayout;
import ide.layout.windows.IdeCSSEditor;
import ide.layout.windows.IdeLuaEditor;
import lwjgui.LWJGUI;
import lwjgui.scene.Group;
import lwjgui.scene.Scene;
import lwjgui.scene.Window;
import lwjgui.theme.Theme;
import lwjgui.theme.ThemeDark;

public class IDE extends AnarchyEngineClient {
	public static IdeLayout layout;
	
	public static final String TITLE = "Anarchy Engine - Build " + Game.version();
	
	@Override
	public void initialize(String[] args) {
		// Window title
		GLFW.glfwSetWindowTitle(window, TITLE);
		
		InternalRenderThread.desiredFPS = 60;
		InternalGameThread.desiredTPS = 60;
		
		// Setup background pane
		Group background = new Group();
		win.getScene().setRoot(background);
		
		Theme.setTheme(new ThemeDark());
		
		// Redraw window if resized
		win.addEventListener(new lwjgui.event.listener.WindowSizeListener() {
			@Override
			public void invoke(long arg0, int arg1, int arg2) {
				renderThread.forceUpdate();
			}
		});
		
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
		LWJGUI.render(false);
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub
	}

	public static void openScript(ScriptBase instance) {
		IdeLuaEditor lua = new IdeLuaEditor((ScriptBase) instance);
		layout.getCenter().dock(lua);
	}

	public static void openCSS(CSS instance) {
		IdeCSSEditor lua = new IdeCSSEditor(instance);
		layout.getCenter().dock(lua);
		
		/*Window window = LWJGUI.initialize();
		window.setScene(new Scene(new IdeCSSEditor(instance), 500, 400));
		window.show();*/
	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}
}