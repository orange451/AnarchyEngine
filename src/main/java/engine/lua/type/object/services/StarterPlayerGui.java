/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.ClientEngine;
import engine.Game;
import engine.InternalGameThread;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.ui.Gui;
import ide.layout.windows.icons.Icons;
import lwjgui.scene.layout.Pane;

public class StarterPlayerGui extends Instance implements TreeViewable {
	
	private List<Pane> guis = new ArrayList<>();

	public StarterPlayerGui() {
		super("StarterPlayerGui");

		this.setLocked(true);
		this.setInstanceable(false);
		
		InternalGameThread.runLater(()->{
			if ( destroyed )
				return;
			
			Instance ss = Game.starterPlayer();
			
			if ( !this.getParent().eq_b(ss) ) {
				this.forceSetParent(ss);
			}
		});
		
		this.childAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Gui ) {
				Pane gui = ((Gui)arg).root;
				ClientEngine.renderThread.getClientUI().getChildren().add(gui);
				guis.add(gui);
			}
		});
		
		this.childRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof Gui ) {
				Pane gui = ((Gui)arg).root;
				ClientEngine.renderThread.getClientUI().getChildren().remove(gui);
				guis.remove(gui);
			}
		});
		
		Game.startEvent().connect((args)->{
			for (Pane gui : guis ) {
				gui.setVisible(false);
			}
		});
		
		Game.stoppingEvent().connect((args)->{
			for (Pane gui : guis ) {
				gui.setVisible(true);
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
			ClientEngine.renderThread.getClientUI().getChildren().remove(gui);
		}
		
		guis.clear();
	}
}
