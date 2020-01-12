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

import engine.InternalRenderThread;
import engine.lua.type.data.Vector2;
import ide.layout.windows.icons.Icons;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.Pane;

public class Gui extends GuiBase {
	
	public Pane root;
	private Map<GuiBase, Pane> uiMap = new HashMap<>();
	private Map<Pane,Pane> nodeToNodeMap = new HashMap<>();
	
	public Gui() {
		super("Gui");

		this.getField(LuaValue.valueOf("Size")).setLocked(true);
		
		this.root = getUINode();
		this.root.setPadding(new Insets(8));
		this.root.setBackgroundLegacy(Color.ORANGE);
		uiMap.put(this, this.root);
		
		this.descendantAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof GuiBase ) {
				
				// Create new pane
				Pane pane = ((GuiBase)arg).getUINode();
				pane.setBackgroundLegacy(new Color(Math.random(),Math.random(),Math.random()));
				
				// Add it to list
				uiMap.put((GuiBase) arg, pane);
				onGuiChange((GuiBase)arg);
				
				// Track changes
				((GuiBase)arg).changedEvent().connect((cargs)->{
					onGuiChange((GuiBase)arg);
				});
			}
		});
		
		this.descendantRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof GuiBase ) {
				Pane pane = uiMap.get(((GuiBase)arg));
				Pane parent = nodeToNodeMap.get(pane);
				if ( parent != null ) {
					parent.getChildren().remove(pane);
					nodeToNodeMap.remove(pane);
				}
				
				uiMap.remove(arg);
			}
		});

		// Setup root node tracking
		onGuiChange((GuiBase)this);
		this.changedEvent().connect((args)->{
			onGuiChange((GuiBase)this);
		});
	}

	private void onGuiChange(GuiBase GuiBase) {
		Pane pane = uiMap.get(GuiBase);
		if ( GuiBase == this ) {
			pane = root;
		}
		
		if ( pane == null || GuiBase == null )
			return;
		
		// Update size
		pane.setPrefSize(GuiBase.getWidth(), GuiBase.getHeight());
		pane.setAlignment(Pos.valueOf(GuiBase.getAlignment()));
		
		// If parent change, remove from old parent
		Pane parentPane = uiMap.get(GuiBase.getParent());
		Pane currentParentPane = nodeToNodeMap.get(pane);
		if ( currentParentPane != null && !currentParentPane.equals(parentPane) )
			currentParentPane.getChildren().remove(pane);
		
		// Make sure we have a parent
		if ( parentPane == null )
			return;

		// Add us to new parent
		if ( !parentPane.getChildren().contains(pane) ) {
			parentPane.getChildren().add(pane);
			nodeToNodeMap.put((Pane) pane, parentPane);
		}
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

	@Override
	public Pane getUINode() {
		return new StackPane() {
			@Override
			public Pos usingAlignment() {
				return this.getAlignment()==null?Pos.CENTER:this.getAlignment();
			}
		};
	}
}
