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

import engine.lua.type.data.Color3;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;
import lwjgui.scene.Node;

public class Label extends GuiBase {
	
	protected static final LuaValue C_TEXT = LuaValue.valueOf("Text");
	protected static final LuaValue C_TEXTCOLOR = LuaValue.valueOf("TextColor");
	
	public Label() {
		super("Label");

		this.defineField(C_TEXT.toString(), LuaString.valueOf("Label"), false);
		this.defineField(C_TEXTCOLOR.toString(), new Color3(Color.BLACK), false);
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
		return new lwjgui.scene.control.Label();
	}	
	
	/**
	 * Get the Text color for this frame.
	 * @return
	 */
	public Color3 getTextColor() {
		LuaValue val = this.get(C_TEXTCOLOR);
		return val.isnil()?null:(Color3)val;
	}
	
	/**
	 * Set the Text color for this frame.
	 * @param color
	 */
	public void setTextColor(Color3 color) {
		this.set(C_TEXTCOLOR, new Color3(color));
	}

	@Override
	public void updateNode(Node node) {
		((lwjgui.scene.control.Label)node).setText(this.getText());
		((lwjgui.scene.control.Label)node).setTextFill(this.getTextColor().toColor());
	}
}
