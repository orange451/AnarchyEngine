package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;
import luaengine.type.object.services.Connections;

public class Player extends Instance implements TreeViewable {
	private Instance lastCharacter;
	
	public Player() {
		super("Player");
		
		this.defineField("Character", LuaValue.NIL, false);
		this.defineField("Connection", LuaValue.NIL, true);
		this.defineField("ClientOwnsPhysics", LuaValue.TRUE, false);

		this.rawset("Archivable", LuaValue.valueOf(false));
		
		this.setInstanceable(false);
		this.setLocked(true);
		
		this.changedEvent().connect((args)->{
			if ( args[0].toString().equals("Character") ) {
				
				// Check if we can give this player ownership
				boolean can = false;
				if ( Game.isServer() || (!Game.isServer() && Game.getService("Players").get("LocalPlayer").equals(this)))
					can = true;
				
				// If we can't, get out.
				if ( !can )
					return;
				
				Connections c = (Connections)Game.getService("Connections");
				
				// Remove the last character
				if ( lastCharacter != null ) {
					c.ownedCharacters.remove(lastCharacter);
				}
				
				// Add new character
				LuaValue character = args[1];
				if ( !character.isnil() && character instanceof GameObject ) {
					c.ownedCharacters.add((GameObject) character);
					lastCharacter = (Instance) character;
				} else {
					lastCharacter = null;
				}
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
	public void onDestroy() {
		if ( this.getCharacter() != null ) {
			this.getCharacter().destroy();
		}
	}
	
	public Instance getCharacter() {
		LuaValue c = this.get("Character");
		return (!c.isnil() && c instanceof Instance)?(Instance)c:null;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_player;
	}

	public Connection getConnection() {
		LuaValue connection = this.get("Connection");
		return (!connection.isnil()&&connection instanceof Connection)?(Connection)connection:null;
	}
}
