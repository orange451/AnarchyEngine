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

import engine.AnarchyEngineClient;
import engine.InternalRenderThread;
import engine.gl.IPipeline;
import ide.IDE;
import ide.layout.IdePane;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Context;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.StackPane;

public class IdeGameView extends IdePane {
	private StackPane gameView;
	private IPipeline pipeline;
	private Label fps;
	
	public IdeGameView(IPipeline pipeline) {
		super("Game", false);
		
		this.pipeline = pipeline;
		this.setAlignment(Pos.CENTER);
		
		gameView = pipeline.getDisplayPane();
		this.getChildren().add(gameView);
		
		this.fps = new Label("fps");
		this.fps.setTextFill(Color.WHITE);
		this.fps.setMouseTransparent(true);
		this.gameView.getChildren().add(fps);
		this.gameView.setAlignment(Pos.TOP_LEFT);
		this.gameView.setPadding(new Insets(2,2,2,2));
		
		this.gameView.getChildren().add(AnarchyEngineClient.uiNode);
		
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
