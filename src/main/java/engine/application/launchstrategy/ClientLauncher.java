/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.application.launchstrategy;

import engine.GameEngine;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.util.JVMUtil;

public class ClientLauncher {

	public static boolean launch(RenderableApplication application) {
		if ( JVMUtil.restartJVM(true, true, null) ) {
			return false;
		}
		
		// Start server game-logic thread
		ServerLauncher.launch(application);
		
		// Start rendering thread
		GameEngine.renderThread = new InternalRenderThread(application);
		return true;
	}
}
