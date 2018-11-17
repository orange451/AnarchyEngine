package luaengine.type.object;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.GameSubscriber;
import engine.util.FileIO;
import engine.util.IOUtil;
import luaengine.LuaEngine;
import luaengine.type.LuaConnection;
import luaengine.type.LuaEvent;
import luaengine.type.ScriptData;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;

public abstract class ScriptBase extends Instance implements GameSubscriber {
	private ScriptData scriptInstance;
	private AtomicBoolean running;
	public HashMap<LuaEvent,LuaConnection> connections;

	public ScriptBase(String typename) {
		super(typename);

		this.defineField("Source", LuaValue.valueOf(""), false);
		this.defineField("Disabled", LuaValue.valueOf(false), false);
		
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
		
		if ( this.get("Disabled").checkboolean())
			return;

		running.set(true);
		String source = "local script=_G.last_script;"+(this.get("Source").toString());
		scriptInstance = LuaEngine.runLua(source, this);
	}

	public void setSource(String soure) {
		this.set("Source", soure);
	}
	
	public String getSource() {
		return this.get("Source").toString();
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.toString().equals("Disabled") && value.checkboolean() ) {
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
			if ( parent instanceof RunScript ) {
				inRunnableService = true;
				break;
			}
			
			parent = parent.get("Parent");
			a++;
		}
		
		// Calculate whether or not we can run
		boolean override = this.isDescendantOf(Game.getService("Core"));
		boolean canRun = inRunnableService || override;
		
		// Stop script if game is not running and we're supposed to be running (but not overridden), or if we can't run.
		if ( (!Game.isRunning() && canRun && !override) || !canRun ) {
			stop();
			return;
		}
		
		// Don't continue if we don't have a script object backing us.
		if ( scriptInstance != null )
			return;
		
		// Stop script if it says cannot run (normally used so local scripts dont run in server, and server scripts don't run in client).
		if (!getCanRun())
			canRun = false;
		
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
}
