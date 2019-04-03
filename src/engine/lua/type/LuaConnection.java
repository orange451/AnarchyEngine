package engine.lua.type;

import java.util.List;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.object.insts.Script;

public class LuaConnection extends LuaDatatype {
	private LuaValue function;
	private LuaEvent event;
	public Script script;

	public LuaConnection(LuaValue arg, LuaEvent event) {
		this.function = arg;

		this.rawset("Disconnect", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				disconnect();
				return LuaValue.NIL;
			}
		});

		this.setLocked(true);
	}

	public LuaValue getFunction() {
		return this.function;
	}

	public void disconnect() {
		function = null;

		if ( event != null ) {
			List<LuaConnection> con = event.connections;
			event = null;
			
			if ( con != null ) {
				con.remove(this);
			}
		}
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

}
