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

public class TextField extends GuiBase {

	protected static final LuaValue C_TEXT = LuaValue.valueOf("Text");
	protected static final LuaValue C_PROMPT = LuaValue.valueOf("Prompt");
	
	public TextField() {
		super("TextField");

		this.defineField(C_TEXT.toString(), LuaString.valueOf("Label"), false);
		this.defineField(C_PROMPT.toString(), LuaString.valueOf("Prompt"), false);
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
		return Icons.icon_text_field;
	}
	
	/**
	 * Return the text for the label.
	 * @return
	 */
	public String getText() {
		return this.get(C_TEXT).toString();
	}
	
	/**
	 * Set the text for the label.
	 * @param text
	 */
	public void setText(String text) {
		this.set(C_TEXT, LuaValue.valueOf(text));
	}

	@Override
	public Node getUINode() {
		return new lwjgui.scene.control.TextField();
	}

	@Override
	public void updateNode(Node node) {
		((lwjgui.scene.control.TextField)node).setText(this.getText());
	}
}
