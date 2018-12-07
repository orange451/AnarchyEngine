package luaengine.lib;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import luaengine.LuaEngine;
import luaengine.type.ScriptData;
import luaengine.type.object.ScriptBase;

public class GameEngineLib extends TwoArgFunction {

	public static GameEngineLib GAMELIB = null;

	public GameEngineLib() {
		GAMELIB = this;
	}

	public LuaValue call(LuaValue modname, LuaValue env) {
		env.set("tick", new tick()); // Returns the elapsed time (seconds) since start of application
		env.set("spawn", new spawn()); // Spawns a new thread
		env.set("wait", new wait()); // Sleeps current thread
		env.set("print", new print(env)); // Override LuaJ's because it doesn't respect multi-threading
		
		// math.clamp( value, min, max )
		env.get("math").set("clamp", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				float f1 = arg2.tofloat();
				float f2 = arg3.tofloat();
				float x = arg1.tofloat();
				
				return LuaValue.valueOf(Math.max(f1, Math.min(f2, x)));
			}
		});
		return env;
	}
	
	static final class tick extends ZeroArgFunction {
		private long startTime = System.currentTimeMillis();
		
		@Override
		public LuaValue call() {
			double tick = (System.currentTimeMillis()-startTime)/1000d;
			return LuaValue.valueOf(tick);
		}
	}
	
	static final class spawn extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue function, LuaValue script) {
			LuaValue.assert_(function.isfunction(), "requires a function argument");
			ScriptBase s = script instanceof ScriptBase ? (ScriptBase)script : null;
			
			ScriptData t = ScriptData.create(function, s, LuaValue.NONE);
			t.start();
			
			return t;
		}
	}
	
	static final class wait extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			try {
				if ( arg == LuaValue.NIL ) {
					arg = LuaValue.valueOf(1/60d);
				}
				double dt = arg.checkdouble();
				long time = (long) (dt*1000);
				
				long start = System.currentTimeMillis();
				Thread.sleep(time);
				return LuaValue.valueOf((System.currentTimeMillis()-start)/1000d);
			} catch (LuaError e) {
				e.printStackTrace();
			} catch(Exception e) {
				//
			}
			return LuaValue.TRUE;
		}
	}
	
	// "print", // (...) -> void
	static class print extends VarArgFunction {
		final LuaValue baselib;
		print(LuaValue env) {
			this.baselib = env;
		}
		public Varargs invoke(Varargs args) {
			String str = "";
			LuaValue tostring = baselib.get("tostring"); 
			for ( int i=1, n=args.narg(); i<=n; i++ ) {
				if ( i>1 ) str += '\t';
				LuaString s = tostring.call( args.arg(i) ).strvalue();
				str += s.tojstring();
			}
			str += "\n";
			LuaEngine.print(str);
			return NONE;
		}
	}
}
