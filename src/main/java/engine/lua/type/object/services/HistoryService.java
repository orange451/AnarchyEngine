/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

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
	
	/**
	 * Push a generic change of an Instance. Creates a single HistorySnapshot.
	 * @param object
	 * @param field
	 * @param oldValue
	 * @param newValue
	 */
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
	
	/**
	 * Push a History Snapshot to the stack.
	 * @param snapshot
	 */
	public void pushChange(HistorySnapshot snapshot) {
		// Push to history
		this.historyStack.push(snapshot);
	}
	
	/**
	 * Pushes a Instance's parent change from A to B. This generates a new History Snapshot.
	 * @param object
	 * @param from
	 * @param to
	 */
	public void pushChangeParent(Instance object, LuaValue from, LuaValue to) {
		pushChange(object, LuaValue.valueOf("Parent"), from, to);
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
