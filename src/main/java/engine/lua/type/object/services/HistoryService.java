package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.history.HistoryChange;
import engine.lua.history.HistorySnapshot;
import engine.lua.history.HistoryStack;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;

public class HistoryService extends Service {
	private HistoryStack historyStack;
	
	public HistoryService() {
		super("HistoryService");
		this.setLocked(true);
		
		this.historyStack = new HistoryStack();
		
		this.getmetatable().set("Undo", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				undo();
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("GetCanUndo", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(historyStack.canUndo());
			}
		});
		
		this.getmetatable().set("Redo", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				redo();
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("GetCanRedo", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(historyStack.canRedo());
			}
		});
	}
	
	public HistoryStack getHistoryStack() {
		return this.historyStack;
	}
	
	public void pushChange(Instance object, LuaValue field, LuaValue oldValue, LuaValue newValue) {
		if ( this.checkEquals(oldValue, newValue) )
			return;
		
		System.out.println("Changed " + object + ". " + field + " --> " + newValue + " from " + oldValue);
		
		// Create a history change
		HistoryChange historyChange = new HistoryChange(
				historyStack.getObjectReference(object),
				field,
				oldValue,
				newValue
		);

		// Create a snapshot for this change
		HistorySnapshot snapshot = new HistorySnapshot();
		snapshot.changes.add(historyChange);
		
		this.pushChange(snapshot);
	}
	
	public void pushChange(HistorySnapshot snapshot) {
		// Push to history
		this.historyStack.push(snapshot);
	}
	
	public void undo() {
		try {
			historyStack.undo();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void redo() {
		historyStack.redo();
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
