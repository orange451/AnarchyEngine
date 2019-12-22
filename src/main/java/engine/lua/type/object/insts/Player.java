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

import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Player extends Instance implements TreeViewable {
	
	private static final LuaValue C_CHARACTER = LuaValue.valueOf("Character");
	private static final LuaValue C_CONNECTION = LuaValue.valueOf("Connection");
	private static final LuaValue C_CLIENTOWNSPHYSICS = LuaValue.valueOf("ClientOwnsPhysics");
	private static final LuaValue C_PLAYERSCRIPTS = LuaValue.valueOf("PlayerScripts");
	
	public Player() {
		super("Player");
		
		this.defineField(C_CHARACTER.toString(), LuaValue.NIL, false);
		this.defineField(C_CONNECTION.toString(), LuaValue.NIL, true);
		this.defineField(C_CLIENTOWNSPHYSICS.toString(), LuaValue.TRUE, false);

		this.setArchivable(false);
		
		this.setInstanceable(false);
		this.setLocked(true);
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
		if ( this.getCharacter() != null ) {
			this.getCharacter().destroy();
		}
	}
	
	public Instance getCharacter() {
		LuaValue c = this.get(C_CHARACTER);
		return (!c.isnil() && c instanceof Instance)?(Instance)c:null;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_player;
	}

	public Connection getConnection() {
		LuaValue connection = this.get(C_CONNECTION);
		return (!connection.isnil()&&connection instanceof Connection)?(Connection)connection:null;
	}

	public boolean doesClientOwnPhysics() {
		LuaValue p = this.get(C_CLIENTOWNSPHYSICS);
		return p.toboolean();
	}


	public Instance playerScripts() {
		return findFirstChild(C_PLAYERSCRIPTS);
	}

	public void start() {
		// Setup local player stuff
		Game.players().rawset("LocalPlayer", this);
		engine.lua.type.object.insts.Connection localConnection = Game.connections().getLocalConnection();
		if ( localConnection != null ) {
			localConnection.rawset("Player", this);
			this.rawset("Connection", localConnection);
		}
		
		// Copy starter player scripts in to player
		Instance starterScripts = Game.starterPlayer().starterPlayerScripts();
		List<Instance> cc = starterScripts.getChildren();
		for (int j = 0; j < cc.size(); j++) {
			Instance obj = cc.get(j);
			Instance clo = obj.clone();
			clo.forceSetParent(this.playerScripts());
		}
	}
}
