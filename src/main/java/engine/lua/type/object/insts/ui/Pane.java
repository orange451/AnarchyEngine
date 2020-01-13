/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.ui;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Vector2;
import ide.layout.windows.icons.Icons;
import lwjgui.scene.layout.StackPane;

public class Pane extends GuiBasePane {
	
	public Pane() {
		super("Pane");
		
		this.setSize(new Vector2(100,100));
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_pane;
	}

	@Override
	public lwjgui.scene.layout.Pane getUINode() {
		return new StackPane();
	}
}
