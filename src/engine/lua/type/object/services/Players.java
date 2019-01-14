package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Player;
import ide.layout.windows.icons.Icons;

public class Players extends Service implements TreeViewable {

	public Players() {
		super("Players");

		this.defineField("LocalPlayer", LuaValue.NIL, true);

		this.rawset("PlayerAdded",		new LuaEvent());
		this.rawset("PlayerRemoved",	new LuaEvent());
		
		this.getmetatable().set("GetPlayers", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				List<Player> temp = getPlayers();
				LuaTable table = new LuaTable();
				for (int i = 0; i < temp.size(); i++) {
					table.set(i+1, temp.get(i));
				}
				
				return table;
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
		LuaEvent added = (LuaEvent) this.rawget("ChildAdded");
		added.connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Player ) {
					((LuaEvent)Players.this.rawget("PlayerAdded")).fire(arg);
				}
				return LuaValue.NIL;
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
	
	public List<Player> getPlayers() {
		List<Player> players = new ArrayList<Player>();
		List<Instance> children = this.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof Player ) {
				players.add((Player) child);
			}
		}
		
		return players;
	}

	public Player getPlayerFromCharacter(Instance character) {
		List<Player> players = getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			if ( player.getCharacter().equals(character) )
				return player;
		}
		
		return null;
	}

	public LuaEvent playerAddedEvent() {
		return (LuaEvent) this.get("PlayerAdded");
	}
	
	public Player getLocalPlayer() {
		LuaValue p = this.get("LocalPlayer");
		return (!p.isnil()&&p instanceof Player)?(Player)p:null;
	}
}
