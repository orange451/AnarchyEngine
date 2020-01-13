/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.AnarchyEngineClient;
import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptExecutor;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.ui.Gui;
import ide.layout.windows.icons.Icons;
import lwjgui.scene.layout.Pane;

public class PlayerGui extends Instance implements TreeViewable,ScriptExecutor {
	
	private List<Pane> guis = new ArrayList<>();
	
	public PlayerGui() {
		super("PlayerGui");

		this.setLocked(true);
		this.setInstanceable(false);
		
		this.rawset("Archivable", LuaValue.valueOf(false));
		
		this.childAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Gui ) {
				Pane gui = ((Gui)arg).root;
				AnarchyEngineClient.uiNode.getChildren().add(gui);
				guis.add(gui);
			}
		});
		
		this.childRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Gui ) {
				Pane gui = ((Gui)arg).root;
				AnarchyEngineClient.uiNode.getChildren().remove(gui);
				guis.remove(gui);
			}
		});
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
	public Icons getIcon() {
		return Icons.icon_player_gui;
	}

	@Override
	public void onDestroy() {
		for (Pane gui : guis ) {
			AnarchyEngineClient.uiNode.getChildren().remove(gui);
		}
		
		guis.clear();
	}
}
