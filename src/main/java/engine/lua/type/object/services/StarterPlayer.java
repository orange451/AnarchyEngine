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

import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.SceneStorable;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Player;
import ide.layout.windows.icons.Icons;

public class StarterPlayer extends Service implements TreeViewable,SceneStorable {

	private static final LuaValue C_STARTERPLAYERSCRIPTS = LuaValue.valueOf("StarterPlayerScripts");
	private static final LuaValue C_STARTERPLAYERGUI = LuaValue.valueOf("StarterPlayerGui");
	
	public StarterPlayer() {
		super("StarterPlayer");
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
		return Icons.icon_starter_player;
	}

	public StarterPlayerScripts starterPlayerScripts() {
		return (StarterPlayerScripts) findFirstChild(C_STARTERPLAYERSCRIPTS);
	}

	public StarterPlayerGui starterPlayerGui() {
		return (StarterPlayerGui) findFirstChild(C_STARTERPLAYERGUI);
	}

	/**
	 * Copies starter player data into player.
	 * @param player
	 */
	public void startPlayer(Player player) {
		// Copy starter player scripts in to player
		Instance starterScripts = Game.starterPlayer().starterPlayerScripts();
		List<Instance> cc = starterScripts.getChildren();
		for (int j = 0; j < cc.size(); j++) {
			Instance obj = cc.get(j);
			Instance clo = obj.clone();
			clo.forceSetParent(player.playerScripts());
		}
		
		// Copy starter player gui in to player
		Instance starterGui = Game.starterPlayer().starterPlayerGui();
		List<Instance> cg = starterGui.getChildren();
		for (int j = 0; j < cg.size(); j++) {
			Instance obj = cg.get(j);
			Instance clo = obj.clone();
			clo.forceSetParent(player.playerGui());
		}
	}
}
