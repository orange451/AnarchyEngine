/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.GameSubscriber;
import engine.lua.LuaEngine;
import engine.lua.type.LuaConnection;
import engine.lua.type.LuaEvent;
import engine.lua.type.ScriptRunner;
import engine.lua.type.object.Instance;
import engine.util.FileIO;
import engine.util.IOUtil;

public abstract class ScriptBase extends Instance implements GameSubscriber {
	private ScriptRunner scriptInstance;
	private AtomicBoolean running;
	public HashMap<LuaEvent,LuaConnection> connections;

	private static final LuaValue C_SOURCE = LuaValue.valueOf("Source");
	private static final LuaValue C_DISABLED = LuaValue.valueOf("Disabled");

	public ScriptBase(String typename) {
		super(typename);

		this.defineField(C_SOURCE.toString(), LuaValue.valueOf(""), false);
		this.defineField(C_DISABLED.toString(), LuaValue.valueOf(false), false);
		
		this.getmetatable().set("LoadFromFile", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue file) {
				if ( !file.isnil() ) {
					setSourceFromFile(file.toString());
				}
				return LuaValue.NIL;
			}
		});
		
		Game.getGame().subscribe(this);
		
		connections = new HashMap<LuaEvent,LuaConnection>();
		running = new AtomicBoolean(false);
	}

	private void execute() {
		if ( running.get() )
			return;
		
		if ( this.get(C_DISABLED).checkboolean())
			return;

		running.set(true);
		String source = "local script=_G.last_script;"+(this.get(C_SOURCE).toString());
		scriptInstance = LuaEngine.runLua(source, this);
	}

	public void setSource(String source) {
		this.set(C_SOURCE, LuaValue.valueOf(source));
	}
	
	public String getSource() {
		return this.get(C_SOURCE).toString();
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_DISABLED) && value.checkboolean() ) {
			stop();
		}
		
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		stop();
	}
	
	public void stop() {
		// Interrupt script-thread
		if ( scriptInstance != null ) {
			scriptInstance.interrupt();
			scriptInstance = null;
		}
		
		// Clear all connections
		synchronized(connections) {
			connections.forEach((key,value) -> {
				key.disconnect(value);
			});
			connections.clear();
		}

		// Set us to not running state
		running.set(false);
	}
	
	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		
		if ( !Game.isLoaded() )
			return;
		
		// Check if we're in a runnable service
		boolean inRunnableService = false;
		int a = 0;
		LuaValue parent = this.getParent();
		while (!parent.isnil() && a < 32) {
			if ( parent instanceof ScriptExecutor ) {
				inRunnableService = true;
				break;
			}
			
			parent = ((Instance)parent).getParent();
			a++;
		}
		
		// Calculate whether or not we can run
		boolean inCore = this.isDescendantOf(Game.core());
		boolean canRun = inRunnableService || inCore;
		
		// Stop script if game is not running and we're supposed to be running (but not overridden), or if we can't run.
		if ( (!Game.isRunning() && canRun && !inCore) || !canRun ) {
			stop();
			return;
		}
		
		// Don't continue if we don't have a script object backing us.
		if ( scriptInstance != null )
			return;
		
		// Stop script if it says cannot run (normally used so local scripts dont run in server, and server scripts don't run in client).
		if (!getCanRun())
			canRun = false;
		
		// Run the script!
		synchronized(running) {
			if ( canRun ) {
				execute();
			}
		}
	}

	public abstract boolean getCanRun();

	public void setSourceFromFile(String filePath) {
		BufferedReader out = FileIO.file_text_open_read(IOUtil.ioResourceGetURL(filePath));
		setSource(FileIO.file_text_read_line_all(out));
		FileIO.file_text_close(out);
	}

	public boolean isRunning() {
		return running.get();
	}
}
