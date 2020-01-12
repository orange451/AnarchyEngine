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

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import lwjgui.scene.Node;

public class Label extends GuiBase {
	
	protected static final LuaValue C_TEXT = LuaValue.valueOf("Text");
	
	public Label() {
		super("Label");
		
		this.defineField(C_TEXT.toString(), LuaString.valueOf("Label"), false);
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
		return Icons.icon_label;
	}
	
	public String getText() {
		return this.get(C_TEXT).toString();
	}
	
	public void setText(String text) {
		this.set(C_TEXT, LuaValue.valueOf(text));
	}

	@Override
	public Node getUINode() {
		return new lwjgui.scene.control.Label();
	}

	@Override
	public void updateNode(Node node) {
		System.out.println("Updating text!");
		((lwjgui.scene.control.Label)node).setText(this.getText());
	}
}
