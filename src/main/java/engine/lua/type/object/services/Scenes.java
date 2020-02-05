/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Scene;
import ide.layout.windows.icons.Icons;

public class Scenes extends Service implements TreeViewable {

	private static final LuaValue C_STARTINGSCENE = LuaValue.valueOf("StartingScene");
	private static final LuaValue C_CURRENTSCENE = LuaValue.valueOf("CurrentScene");
	
	public Scenes() {
		super("Scenes");

		this.defineField(C_STARTINGSCENE, LuaValue.NIL, false);
		this.defineField(C_CURRENTSCENE, LuaValue.NIL, true);
		this.setLocked(true);
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_STARTINGSCENE) ) {
			if ( !value.isnil() && !(value instanceof Scene) ) {
				LuaValue.error("StartingScene must be of type Scene");
				return null;
			}
		}
		return value;
	}
	
	/**
	 * Returns the scene that is loaded when the game initializes.
	 * @return
	 */
	public Scene getStartingScene() {
		LuaValue t = this.get(C_STARTINGSCENE);
		return t.isnil()?null:(Scene)t;
	}
	
	/**
	 * Set which scene is the scene loaded upon game load.
	 * @param scene
	 */
	public void setStartingScene(Scene scene) {
		this.set(C_STARTINGSCENE, scene==null?LuaValue.NIL:scene);
	}
	
	/**
	 * Sets the current loaded scene variable. This does not actually handle loading the scene. To load a new scene see: TODO create this method...
	 * @param scene
	 */
	protected void setCurrentScene(Scene scene) {
		this.rawset(C_CURRENTSCENE, scene==null?LuaValue.NIL:scene);
	}
	
	/**
	 * Returns which scene is currently loaded into game.
	 * @return
	 */
	public Scene getCurrentScene() {
		LuaValue t = this.get(C_CURRENTSCENE);
		return t.isnil()?null:(Scene)t;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_scenes;
	}
}