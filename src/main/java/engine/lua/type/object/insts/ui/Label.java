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
import engine.lua.type.data.Color4;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;
import lwjgui.scene.Node;

public class Label extends GuiBase {
	
	protected static final LuaValue C_TEXT = LuaValue.valueOf("Text");
	protected static final LuaValue C_TEXTCOLOR = LuaValue.valueOf("TextColor");
	protected static final LuaValue C_FONTSIZE = LuaValue.valueOf("FontSize");
	
	public Label() {
		super("Label");

		this.defineField(C_TEXT.toString(), LuaString.valueOf("Label"), false);
		this.defineField(C_TEXTCOLOR.toString(), new Color4(Color.BLACK), false);
		this.defineField(C_FONTSIZE.toString(), LuaString.valueOf(16), false);
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
	 * Return the font size for the label.
	 * @return
	 */
	public float getFontSize() {
		return this.get(C_FONTSIZE).tofloat();
	}
	
	/**
	 * Set font size.
	 * @param size
	 */
	public void setFontSize(float size) {
		this.set(C_FONTSIZE, LuaValue.valueOf(size));
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
	
	/**
	 * Get the Text color for this frame.
	 * @return
	 */
	public Color4 getTextColor() {
		LuaValue val = this.get(C_TEXTCOLOR);
		return val.isnil()?null:(Color4)val;
	}
	
	/**
	 * Set the Text color for this frame.
	 * @param color
	 */
	public void setTextColor(Color4 color) {
		this.set(C_TEXTCOLOR, new Color4(color));
	}

	@Override
	public Node getUINode() {
		return new lwjgui.scene.control.Label(getText());
	}

	@Override
	public void updateNode(Node node) {
		((lwjgui.scene.control.Label)node).setText(this.getText());
		((lwjgui.scene.control.Label)node).setTextFill(this.getTextColor().toColor());
		((lwjgui.scene.control.Label)node).setFontSize(this.getFontSize());
	}
}
