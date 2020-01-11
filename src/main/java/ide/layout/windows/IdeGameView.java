/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import engine.Game;
import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.gl.Surface;
import engine.lua.type.object.services.UserInputService;
import ide.IDE;
import ide.layout.IdePane;
import lwjgui.event.ScrollEvent;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.gl.GenericShader;
import lwjgui.gl.Renderer;
import lwjgui.paint.Color;
import lwjgui.scene.Context;
import lwjgui.scene.Node;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.OpenGLPane;
import lwjgui.scene.layout.StackPane;

public class IdeGameView extends IdePane {
	private OpenGLPane gameView;
	private IPipeline pipeline;
	private Label fps;
	
	public StackPane uiPane;
	
	public IdeGameView(IPipeline pipeline) {
		super("Game", false);
		
		this.pipeline = pipeline;
		this.setAlignment(Pos.CENTER);
		
		this.uiPane = new StackPane() {
			@Override
			public void position(Node parent) {
				this.forceSize(gameView.getWidth(), gameView.getHeight());
				super.position(parent);
			}
		};
		this.uiPane.setAlignment(Pos.TOP_LEFT);
		
		
		this.gameView = new OpenGLPane();
		gameView.setMinSize(1, 1);
		gameView.setFillToParentHeight(true);
		gameView.setFillToParentWidth(true);
		gameView.setFlipY(true);
		gameView.setRendererCallback(new Renderer() {
			GenericShader shader;
			{
				shader = new GenericShader();
			}
			
			@Override
			public void render(Context context) {
				if ( !IdeGameView.this.pipeline.isInitialized() )
					return;
				
				Surface surface = IdeGameView.this.pipeline.getPipelineBuffer();
				surface.render(shader);
			}
		});
		this.getChildren().add(gameView);
		
		uiPane.setOnKeyPressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			if ( !gameView.isDescendentSelected() )
				return;
			
			uis.onKeyPressed(event.getKey());
		});
		uiPane.setOnKeyReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			if ( !gameView.isDescendentSelected() )
				return;
			
			uis.onKeyReleased(event.getKey());
		});
		uiPane.setOnMousePressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			uis.onMousePress(event.button);
			cached_context.setSelected(gameView);
		});
		uiPane.setOnMouseReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			uis.onMouseRelease(event.button);
		});
		uiPane.setOnMouseScrolled(event ->{
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			
			if ( !gameView.isDescendentHovered() && !this.cached_context.isHovered(gameView) )
				return;
			
			uis.onMouseScroll(((ScrollEvent)event).y > 0 ? 3 : 4 );
		});
		
		this.fps = new Label("fps");
		this.fps.setTextFill(Color.WHITE);
		this.fps.setMouseTransparent(true);
		this.gameView.getChildren().add(fps);
		this.gameView.setAlignment(Pos.TOP_LEFT);
		this.gameView.setPadding(new Insets(2,2,2,2));
		
		this.gameView.getChildren().add(this.uiPane);
		
		StandardUserControls.bind(this);
	}

	@Override
	public void onOpen() {
		IDE.pipeline.setEnabled(true);
	}

	@Override
	public void onClose() {
		IDE.pipeline.setEnabled(false);
	}
	
	@Override
	public void render(Context context) {
		if ( !IdeGameView.this.pipeline.isInitialized() )
			return;
		
		fps.setText(InternalRenderThread.fps + " fps");
		
		super.render(context);

		IDE.pipeline.setSize((int)gameView.getWidth(), (int)gameView.getHeight());
	}
}
