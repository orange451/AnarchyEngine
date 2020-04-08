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

import engine.io.Load;

public class ClientLocalRunner extends ClientRunner {

	public ClientLocalRunner(String... args) {
		super(args);
	}

	@Override
	public void setupEngine() {
		super.setupEngine();
		game.setServer(false);
	}

	@Override
	public void loadScene(String[] args) {
		if (args.length == 1) {
			Load.load(args[0]);
		}
	}

	public static void main(String[] args) {
		new ClientLocalRunner(args);
	}
}
