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

import ide.layout.windows.icons.Icons;
import lwjgui.scene.layout.Pane;

public class HBox extends GuiBasePane {
	
	public HBox() {
		super("HBox");
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
		return Icons.icon_hbox;
	}

	@Override
	public Pane getUINode() {
		return new lwjgui.scene.layout.HBox();
	}
}
