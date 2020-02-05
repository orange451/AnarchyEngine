package engine.lua.type.object.insts;

import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.SceneStorable;

public class SceneInternal extends Instance {
	private Scene scene;
	
	public SceneInternal() {
		super("SceneInternal");
		this.setInstanceable(false);
	}
	
	public SceneInternal(Scene scene) {
		this();
		
		this.scene = scene;
	}
	
	public Scene getScene() {
		return this.scene;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	/**
	 * Stores the current game to the internal data structure of this scene.
	 */
	public void storeGame() {
		//this.clearAllChildren();
		
		List<Instance> potentialServices = Game.game().getChildrenSafe();
		for (int i = 0; i < potentialServices.size(); i++) {
			Instance potentialService = potentialServices.get(i);
			if ( !(potentialService instanceof SceneStorable) )
				continue;
			
			// Create new service of same type
			Instance newService = this.findFirstChild(potentialService.getName());
			if ( newService == null )
				newService = Instance.instance(potentialService.getClassName().toString());
			
			// Parent instances INTO this new service
			List<Instance> c = potentialService.getChildrenSafe();
			for (int j = 0; j < c.size(); j++) {
				Instance temp = c.get(j);
				temp.forceSetParent(newService);
				System.out.println("Parenting " + temp + " " + temp.getUUID() + " to " + newService.getFullName());
			}
			
			// Put service in scene
			newService.forceSetParent(this);
		}
	}
}
