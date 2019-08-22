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
import engine.lua.type.object.insts.Player;
import ide.layout.windows.icons.Icons;

public class Players extends Service implements TreeViewable {
	
	private static final LuaValue C_LOCALPLAYER = LuaValue.valueOf("LocalPlayer");
	private static final LuaValue C_PLAYERADDED = LuaValue.valueOf("PlayerAdded");
	private static final LuaValue C_PLAYERREMOVED = LuaValue.valueOf("PlayerRemoved");

	public Players() {
		super("Players");

		this.defineField(C_LOCALPLAYER.toString(), LuaValue.NIL, true);

		this.rawset(C_PLAYERADDED.tostring(),	new LuaEvent());
		this.rawset(C_PLAYERREMOVED.tostring(),	new LuaEvent());
		
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
		return (LuaEvent) this.get(C_PLAYERADDED);
	}

	public LuaEvent playerRemovedEvent() {
		return (LuaEvent) this.get(C_PLAYERREMOVED);
	}
	
	public Player getLocalPlayer() {
		LuaValue p = this.get(C_LOCALPLAYER);
		return (!p.isnil()&&p instanceof Player)?(Player)p:null;
	}
}
