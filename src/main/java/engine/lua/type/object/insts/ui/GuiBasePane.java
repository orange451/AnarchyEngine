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

import engine.lua.type.data.Color4;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.layout.Pane;

public abstract class GuiBasePane extends GuiBase {
	
	protected static final LuaValue C_BACKGROUNDCOLOR = LuaValue.valueOf("BackgroundColor");
	
	public GuiBasePane(String name) {
		super(name);
		
		this.defineField(C_BACKGROUNDCOLOR.toString(), new Color4(Color.WHITE), false);
	}
	
	/**
	 * Get the background color for this frame.
	 * @return
	 */
	public Color4 getBackgroundColor() {
		LuaValue val = this.get(C_BACKGROUNDCOLOR);
		return val.isnil()?null:(Color4)val;
	}
	
	/**
	 * Set the background color for this frame.
	 * @param color
	 */
	public void setBackgroundColor(Color4 color) {
		this.set(C_BACKGROUNDCOLOR, new Color4(color));
	}

	@Override
	public void updateNode(Node node) {
		node.setPrefSize(getWidth(), getHeight());
		node.setAlignment(Pos.valueOf(getAlignment()));
		((Pane)node).setBackgroundLegacy(this.getBackgroundColor().toColor());
	}
}
