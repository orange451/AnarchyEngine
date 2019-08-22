package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;

public class Debris extends Service {
	private List<DebrisInstance> objects = Collections.synchronizedList(new ArrayList<DebrisInstance>());

	public Debris() {
		super("Debris");

		this.rawset("AddItem", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				if ( !(arg2 instanceof Instance) ) {
					LuaValue.error("Debris requires Instance type");
				}
				if ( !arg3.isnumber() ) {
					LuaValue.error("Debris needs a number length of time.");
				}
				addItem((Instance)arg2, arg3.tofloat());
				return LuaValue.NIL;
			}
		});

		this.setLocked(true);
		
		// Wait until game starts
		Game.loadEvent().connect((a)->{
			
			// Check for objects to remove every step
			Game.runService().heartbeatEvent().connect((args)->{
				long currentTime = System.currentTimeMillis();

				ArrayList<DebrisInstance> toRemove = new ArrayList<DebrisInstance>();
				int len = objects.size();
				synchronized(objects) {
					for (int i = 0; i < len; i++) {
						if ( i >= objects.size() )
							continue;
						DebrisInstance obj = objects.get(i);
						if ( obj == null )
							continue;

						if ( obj.removeTime < currentTime ) {
							toRemove.add(obj);
							obj.instance.destroy();
						}
					}
				}

				synchronized(objects) {
					objects.removeAll(toRemove);
				}
			});
		});
	}

	public void addItem(Instance instance, float time) {
		objects.add(new DebrisInstance(instance, (long) (time*1000)));
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

class DebrisInstance {
	Instance instance;
	long removeTime;

	public DebrisInstance(Instance instance, long time) {
		this.instance = instance;
		this.removeTime = System.currentTimeMillis() + time;
	}
}
