/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import engine.lua.RunnableArgs;
import engine.lua.type.object.ScriptBase;

public class LuaEvent extends LuaDatatype {

	protected List<LuaConnection> connections = Collections.synchronizedList(new ArrayList<LuaConnection>());
	protected List<LuaConnection> disconnectQueue = Collections.synchronizedList(new ArrayList<LuaConnection>());

	public LuaEvent() {
		this.rawset("Connect", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				return connectLua(arg);
			}
		});
		this.setLocked(true);
	}
	
	/**
	 * The same as the Lua version of connect(). Lambda friendly for java-use. CANNOT RETURN VALUE TO LUA.
	 * @param runnableArgs
	 * @return 
	 */
	public LuaConnection connect(RunnableArgs runnableArgs) {
		VarArgFunction f = new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				LuaValue[] arr = new LuaValue[args.narg()];
				for (int i = 0; i < arr.length; i++) {
					arr[i] = args.arg(i+1);
				}
				runnableArgs.run(arr);
				return LuaValue.NIL;
			}
		};
		LuaValue t = connectLua(f);
		if ( t instanceof LuaConnection )
			return (LuaConnection) t;
		return null;
	}

	public LuaValue connectLua(LuaValue function) {
		if ( function instanceof LuaFunction ) {
			LuaConnection cnt = new LuaConnection((LuaFunction) function, LuaEvent.this);
			synchronized(connections) {
				connections.add(cnt);
			}
			
			ScriptBase runningScript = ScriptRunner.getScript(Thread.currentThread());
			if ( runningScript != null && runningScript.isRunning() ) {
				runningScript.connections.put(LuaEvent.this,cnt);
			}
			//cnt.script = ScriptData.getScript(Thread.currentThread());
			return cnt;
		} else {
			LuaValue.error("Connecting to an event requires a function type.");
		}
		return LuaValue.NIL;
	}
	
	public void fire() {
		fire(new LuaValue[] {});
	}

	public void fire(LuaValue...args) {
		Varargs vargs = LuaValue.varargsOf(args);
		
		synchronized(connections) {
			// Remove the queued connections
			synchronized( disconnectQueue ) {
				while ( disconnectQueue.size() > 0 ) {
					LuaConnection c = disconnectQueue.get(0);
					connections.remove(c);
					disconnectQueue.remove(c);
				}
			}
			
			// Fire remaining connections
			int len = connections.size();
			for (int i = 0; i < len; i++) {
				if ( i >= connections.size() )
					continue;
				LuaConnection temp = connections.get(i);
				if ( temp == null )
					continue;

				//LuaThread lt = (LuaThread) LuaEngine.globals.get("coroutine").get("create").call(temp.getFunction());
				//LuaEngine.globals.get("coroutine").get("resume").invoke(LuaValue.varargsOf( lt, vargs ));
				//LuaEngine.globals.get("spawn").call(temp.getFunction());
				//LuaEngine.spawn(temp.getFunction(), null, vargs);
				
				try {
					ScriptRunner t = ScriptRunner.create(temp.getFunction(), null, vargs);
					t.run();
				}catch(Exception e) {
					//
				}
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

	public void disconnect(LuaConnection value) {
		disconnectQueue.add(value);
	}

	public void disconnectAll() {
		synchronized(connections) {
			connections.clear();
			disconnectQueue.clear();
		}
	}
}
