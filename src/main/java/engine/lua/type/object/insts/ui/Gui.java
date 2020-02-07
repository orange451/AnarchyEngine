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

import engine.Game;
import engine.lua.type.LuaConnection;
import engine.lua.type.data.Vector2;
import engine.lua.type.object.Instance;
import ide.layout.windows.icons.Icons;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;
import lwjgui.style.BoxShadow;

public class Gui extends GuiBase {
	
	public Pane root;
	private Map<GuiBase, Node> uiMap = new HashMap<>();
	private Map<Node,Node> nodeToNodeMap = new HashMap<>();
	
	public Gui() {
		super("Gui");
		
		this.gui = this;

		this.getField(C_SIZE).setLocked(true);
		
		this.root = (Pane) getUINode();
		uiMap.put(this, this.root);
		
		this.descendantAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof GuiBase ) {
				
				// Create new pane
				Node pane = ((GuiBase)arg).getUINode();
				
				// Add it to list
				((GuiBase)arg).gui = this;
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
				Node pane = getNode((GuiBase)arg);
				Node parent = nodeToNodeMap.get(pane);
				if ( parent != null && parent instanceof Pane ) {
					((Pane)parent).getChildren().remove(pane);
					nodeToNodeMap.remove(pane);
				}
				
				// Remove node from map
				uiMap.remove(arg);
				
				// Disconnect connection
				LuaConnection connection = uiConnections.get((Instance)arg);
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
	
	/**
	 * Returns the lwjgui node related to this guibase object
	 * @param guiObject
	 * @return
	 */
	public Node getNode(GuiBase guiObject) {
		return this.uiMap.get(guiObject);
	}

	private void onGuiChange(GuiBase guiBase) {
		Node node = getNode(guiBase);
		if ( guiBase == this )
			node = root;
		
		if ( node == null || guiBase == null )
			return;
		
		try {
			// Update size
			guiBase.updateNodeInternal(node);
			guiBase.updateNode(node);
			
			// If parent change, remove from old parent
			Node parentPane = uiMap.get(guiBase.getParent());
			//System.out.println("CHANGE EVENT: " + guiBase + " / " + node + " :: " + parentPane);
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
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		if ( root != null ) {
			this.rawset(C_SIZE, new Vector2((float)root.getWidth(), (float)root.getHeight()));
			//this.notifyPropertySubscribers(C_SIZE, this.rawget(C_SIZE));
		}
		
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
			
			@Override
			public void position(Node parent) {
				super.position(parent);

				// Handle selection graphic
				this.getBoxShadowList().clear();
				if ( Game.isSelected(Gui.this) )
					this.getBoxShadowList().add(new BoxShadow(0, 0, 1, 2, Color.ORANGE, false));
			}
		};
	}

	@Override
	public void updateNode(Node node) {
		node.setPrefSize(getWidth(), getHeight());
		node.setAlignment(Pos.valueOf(getAlignment()));
	}
}
