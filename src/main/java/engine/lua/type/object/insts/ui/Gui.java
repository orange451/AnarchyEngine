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

import java.util.HashMap;
import java.util.Map;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Vector2;
import ide.layout.windows.icons.Icons;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;

public class Gui extends GuiBase {
	
	public StackPane root;
	private Map<GuiBase, StackPane> uiMap = new HashMap<>();
	
	public Gui() {
		super("Gui");

		this.getField(LuaValue.valueOf("Size")).setLocked(true);
		
		this.root = new StackPane();
		this.root.setPadding(new Insets(8));
		this.root.setBackgroundLegacy(Color.ORANGE);
		
		this.descendantAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Frame ) {
				
				// Create new pane
				StackPane pane = new StackPane();
				pane.setBackgroundLegacy(Color.WHITE);
				
				// Add it to list
				uiMap.put((Frame) arg, pane);
				onGuiChange((GuiBase)arg);
				
				// Put it in hierarchy
				StackPane parentPane = uiMap.get(((Frame)arg).getParent());
				if ( parentPane != null ) {
					parentPane.getChildren().add(pane);
				} else {
					root.getChildren().add(pane);
				}
				
				// Track changes
				((Frame)arg).changedEvent().connect((cargs)->{
					onGuiChange((GuiBase)arg);
				});
			}
		});
		
		this.descendantRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Frame ) {
				StackPane pane = uiMap.get(((Frame)arg));
				StackPane parentPane = uiMap.get(((Frame)arg).getParent());
				if ( parentPane != null ) {
					parentPane.getChildren().remove(pane);
				} else {
					root.getChildren().remove(pane);
				}
				
				uiMap.remove(arg);
			}
		});
		
		this.changedEvent().connect((args)->{
			onGuiChange((GuiBase)this);
		});
		onGuiChange((GuiBase)this);
	}

	private void onGuiChange(GuiBase frame) {
		Pane pane = uiMap.get(frame);
		if ( frame == this ) {
			pane = (Pane) root.getParent();
		}
		
		if ( pane == null || frame == null )
			return;
		
		pane.setPrefSize(frame.getWidth(), frame.getHeight());
		pane.setAlignment(Pos.valueOf(frame.getAlignment()));
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		this.rawset(C_SIZE, new Vector2((float)root.getWidth(), (float)root.getHeight()));
		this.notifyPropertySubscribers(C_SIZE, this.rawget(C_SIZE));
		
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_gui;
	}
}
