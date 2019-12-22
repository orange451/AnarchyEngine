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
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.lib.LuaUtil;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Player;
import ide.layout.windows.icons.Icons;

public class Players extends Service implements TreeViewable {
	
	private static final LuaValue C_LOCALPLAYER = LuaValue.valueOf("LocalPlayer");
	private static final LuaValue C_PLAYERADDED = LuaValue.valueOf("PlayerAdded");
	private static final LuaValue C_PLAYERREMOVED = LuaValue.valueOf("PlayerRemoved");
	private static final LuaValue C_PLAYER = LuaValue.valueOf("Player");

	public Players() {
		super("Players");

		this.defineField(C_LOCALPLAYER.toString(), LuaValue.NIL, true);

		this.rawset(C_PLAYERADDED.tostring(),	new LuaEvent());
		this.rawset(C_PLAYERREMOVED.tostring(),	new LuaEvent());
		
		this.getmetatable().set("GetPlayers", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaUtil.listToTable(getPlayers());
			}
		});
		
		this.getmetatable().set("GetPlayerFromCharacter", new TwoArgFunction() {

			@Override
			public LuaValue call(LuaValue arg1, LuaValue character) {
				if ( character.isnil() || !(character instanceof Instance) )
					return LuaValue.NIL;
				
				Player player = getPlayerFromCharacter((Instance) character);
				if ( player == null )
					return LuaValue.NIL;
				
				return player;
			}
			
		});
		
		// Fire player added when a player is added
		this.childAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Player ) {
					((LuaEvent)Players.this.rawget(C_PLAYERADDED)).fire(arg);
				}
				return LuaValue.NIL;
			}
		});
		
		// Fire player removed when a player is removed
		this.childRemovedEvent().connect((args)->{
			if ( args[0] instanceof Player ) {
				((LuaEvent)Players.this.rawget(C_PLAYERREMOVED)).fire(args[0]);
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
		return Icons.icon_players;
	}
	
	/**
	 * Returns a list of all players currently in the Players service.
	 * @return
	 */
	public List<Player> getPlayers() {
		List<Instance> players = this.getChildrenOfClass(C_PLAYER);
		List<Player> ret = new ArrayList<Player>();
		for (int i = 0; i < players.size(); i++) {
			ret.add((Player) players.get(i));
		}
		return ret;
	}

	/**
	 * Matches a player to a given character instance in the workspace. If no such player exists, null is returned.
	 * @param character
	 * @return
	 */
	public Player getPlayerFromCharacter(Instance character) {
		List<Player> players = getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			if ( player.getCharacter().equals(character) )
				return player;
		}
		
		return null;
	}

	/**
	 * Returns the event that is called when a player is added to the game.
	 * @return
	 */
	public LuaEvent playerAddedEvent() {
		return (LuaEvent) this.get(C_PLAYERADDED);
	}

	/**
	 * Returns the event that is called when a player is removed from the game.
	 * @return
	 */
	public LuaEvent playerRemovedEvent() {
		return (LuaEvent) this.get(C_PLAYERREMOVED);
	}
	
	/**
	 * Returns the local player. The local player points to the player object that belongs to you in the current game instance.
	 * There can be multiple players within the players service, use LocalPlayer reference your player.
	 * @return
	 */
	public Player getLocalPlayer() {
		LuaValue p = this.get(C_LOCALPLAYER);
		return !p.isnil()?(Player)p:null;
	}
}
