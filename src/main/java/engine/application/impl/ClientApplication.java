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
