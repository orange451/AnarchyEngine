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

import engine.observer.Renderable;
import engine.observer.Tickable;

public abstract class GameEngine implements Tickable {
	public static GameEngine gameEngine = null;
	public static InternalRenderThread renderThread = null;
	public static InternalGameThread gameThread = null;
	public static Game game;
	
	public static boolean isRenderable() {
		return gameEngine instanceof Renderable;
	}
}
