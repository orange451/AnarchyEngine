package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.services.Connections;
import ide.layout.windows.icons.Icons;

public class Player extends Instance implements TreeViewable {
	private Instance lastCharacter;
	
	private static final LuaValue C_CHARACTER = LuaValue.valueOf("Character");
	private static final LuaValue C_CONNECTION = LuaValue.valueOf("Connection");
	private static final LuaValue C_CLIENTOWNSPHYSICS = LuaValue.valueOf("ClientOwnsPhysics");
	
	public Player() {
		super("Player");
		
		this.defineField(C_CHARACTER.toString(), LuaValue.NIL, false);
		this.defineField(C_CONNECTION.toString(), LuaValue.NIL, true);
		this.defineField(C_CLIENTOWNSPHYSICS.toString(), LuaValue.TRUE, false);

		this.setArchivable(false);
		
		this.setInstanceable(false);
		this.setLocked(true);
		
		this.changedEvent().connect((args)->{
			if ( args[0].eq_b(C_CHARACTER)) {
				
				if ( !Game.isServer() )
					return;
				
				Connections c = (Connections)Game.connections();
				
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
}
