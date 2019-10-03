package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;
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
		
		InternalGameThread.runLater(()->{
			Game.userInputService().inputBeganEvent().connect((args)->{
				LuaValue inputObject = args[0];
				if ( inputObject.get("KeyCode").toint() == GLFW.GLFW_KEY_Z ) {
					if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) ) {
						undo();
					}
				}
			});
			
			Game.game().descendantAddedEvent().connect((args)->{
				Instance added = (Instance) args[0];
				System.out.println("INSTANCE ADDED TO GAME: " + added);
				
				added.changedEvent().connect((changedargs)->{
					System.out.println("Changed " + added + ". " + changedargs[0] + " --> " + changedargs[1] + " / " + changedargs[2]);
					// Create a history change
					HistoryChange historyChange = new HistoryChange(
							historyStack.getObjectReference(added),
							changedargs[0],
							changedargs[2],
							changedargs[1]
					);
					
					// Create a snapshot for this change
					HistorySnapshot snapshot = new HistorySnapshot();
					snapshot.changes.add(historyChange);
					
					// Push to history
					this.historyStack.push(snapshot);
				});
			});
		});
	}
	
	public void undo() {
		historyStack.undo();
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
