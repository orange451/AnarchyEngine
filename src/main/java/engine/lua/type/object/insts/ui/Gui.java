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

import engine.lua.type.LuaConnection;
import engine.lua.type.data.Vector2;
import ide.layout.windows.icons.Icons;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;

public class Gui extends GuiBase {
	
	public Pane root;
	private Map<GuiBase, Node> uiMap = new HashMap<>();
	private Map<GuiBase, LuaConnection> uiConnections = new HashMap<>();
	private Map<Node,Node> nodeToNodeMap = new HashMap<>();
	
	public Gui() {
		super("Gui");

		this.getField(LuaValue.valueOf("Size")).setLocked(true);
		
		this.root = (Pane) getUINode();
		this.root.setPadding(new Insets(8));
		this.root.setBackgroundLegacy(Color.ORANGE);
		uiMap.put(this, this.root);
		
		this.descendantAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof GuiBase ) {
				// Create new pane
				Node pane = ((GuiBase)arg).getUINode();
				if ( pane instanceof Pane )
					((Pane)pane).setBackgroundLegacy(new Color(Math.random(),Math.random(),Math.random()));
				
				// Add it to list
				uiMap.put((GuiBase) arg, pane);
				onGuiChange((GuiBase)arg);
				
				// Track changes
				LuaConnection connection = ((GuiBase)arg).changedEvent().connect((cargs)->{
					onGuiChange((GuiBase)arg);
				});
				
				// Add connection to map
				uiConnections.put((GuiBase) arg, connection);
			}
		});
		
		this.descendantRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof GuiBase ) {
				Node pane = uiMap.get(((GuiBase)arg));
				Node parent = nodeToNodeMap.get(pane);
				if ( parent != null && parent instanceof Pane ) {
					((Pane)parent).getChildren().remove(pane);
					nodeToNodeMap.remove(pane);
				}
				
				// Remove node from map
				uiMap.remove(arg);
				
				// Disconnect connection
				LuaConnection connection = uiConnections.get((GuiBase)arg);
				if ( connection != null )
					connection.disconnect();
				
				// Remove connection from map
				uiConnections.remove(arg);
			}
		});

		// Setup root node tracking
		onGuiChange((GuiBase)this);
		this.changedEvent().connect((args)->{
			onGuiChange((GuiBase)this);
		});
	}

	private void onGuiChange(GuiBase guiBase) {
		Node node = uiMap.get(guiBase);
		System.out.println("CHANGE EVENT: " + guiBase + " / " + node);
		if ( guiBase == this )
			node = root;
		
		if ( node == null || guiBase == null )
			return;
		
		// Update size
		guiBase.updateNode(node);
		
		// If parent change, remove from old parent
		Node parentPane = uiMap.get(guiBase.getParent());
		Node currentParentPane = nodeToNodeMap.get(node);
		if ( currentParentPane != null && !currentParentPane.equals(parentPane) && currentParentPane instanceof Pane )
			((Pane)currentParentPane).getChildren().remove(node);
		
		// Make sure we have a parent
		if ( parentPane == null )
			return;

		// Add us to new parent
		if ( parentPane instanceof Pane && !((Pane)parentPane).getChildren().contains(node) ) {
			((Pane)parentPane).getChildren().add(node);
			nodeToNodeMap.put(node, parentPane);
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
	public Node getUINode() {
		return new StackPane() {
			@Override
			public Pos usingAlignment() {
				return this.getAlignment()==null?Pos.CENTER:this.getAlignment();
			}
		};
	}

	@Override
	public void updateNode(Node node) {
		node.setPrefSize(getWidth(), getHeight());
		node.setAlignment(Pos.valueOf(getAlignment()));
	}
}
