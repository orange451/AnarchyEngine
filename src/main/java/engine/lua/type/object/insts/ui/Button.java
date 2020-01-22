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

import engine.Game;
import engine.lua.type.LuaEvent;
import ide.layout.windows.icons.Icons;
import lwjgui.scene.Node;
import lwjgui.scene.control.ButtonBase;

public class Button extends GuiBase {

	protected static final LuaValue C_TEXT = LuaValue.valueOf("Text");
	protected static final LuaValue C_ACTION = LuaValue.valueOf("Action");
	
	public Button() {
		super("TextField");

		this.defineField(C_TEXT.toString(), LuaString.valueOf("Label"), false);
		
		this.rawset(C_ACTION, new LuaEvent());
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	/**
	 * Action Event. Fires when the button is clicked. This event works the same as Gui Base Mouse Clicked event.
	 * However, some buttons can have the action event fired without clicking (via Enter key press).
	 * @return
	 */
	public LuaEvent getActionEvent() {
		return (LuaEvent) this.get(C_ACTION);
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_button;
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
		return new lwjgui.scene.control.Button(getText());
	}

	@Override
	public void updateNode(Node node) {
		if ( ((lwjgui.scene.control.ButtonBase)node).getOnAction() == null ) {
			((ButtonBase)node).setOnAction((event)->{
				if ( Game.isRunning() )
					getActionEvent().fire();
			});
		}
		((lwjgui.scene.control.Button)node).setText(this.getText());
	}
}
