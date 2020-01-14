/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.application.impl;

import java.io.IOException;

import org.joml.Vector2f;

import engine.application.AnarchyEngineRunner;
import engine.io.Load;

public class ServerApplication extends AnarchyEngineRunner {
	public ServerApplication() {
		//
	}
	
	@Override
	public void loadScene(String[] args) {
		
		// Load the desired scene file
		if ( args.length == 1 ) {
			Load.load(args[0]);
		}
	}

	@Override
	public void render() {
		
		// Render super (updates the pipeline & draws UI)
		super.render();
	}
	
	public static void main(String[] args) throws IOException {
		launch(args);
	}

	@Override
	protected Vector2f getMouseOffset() {
		return new Vector2f();
	}
}
