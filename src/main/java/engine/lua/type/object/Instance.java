package engine.lua.type.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.LuaEngine;
import engine.lua.lib.LuaUtil;
import engine.lua.type.DataModel;
import engine.lua.type.LuaEvent;
import engine.lua.type.LuaValuetype;

public abstract class Instance extends DataModel {
	protected static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	protected static final LuaValue C_CLASSNAME = LuaValue.valueOf("ClassName");
	protected static final LuaValue C_NAME = LuaValue.valueOf("Name");
	protected static final LuaValue C_ARCHIVABLE = LuaValue.valueOf("Archivable");
	protected static final LuaValue C_SID = LuaValue.valueOf("SID");
	
	public static void initialize() {
		System.out.println("Loaded instance");
	}

	public Instance(String name) {
		super(name);
		
		this.getmetatable().set("GetChildren", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaUtil.listToTable(getChildren());
			}
		});

		this.getmetatable().set("ClearAllChildren", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				synchronized(children) {
					for (int i = children.size()-1;i>=0; i--) {
						Instance child = children.get(i);
						if ( !child.locked && child.isInstanceable() ) {
							child.destroy();
						}
					}
				}
				return LuaValue.NIL;
			}
		});

		this.getmetatable().set("GetChildrenWithName", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				return LuaUtil.listToTable(getChildrenWithName(arg.toString()));
			}
		});

		this.getmetatable().set("GetChildrenOfClass", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				return LuaUtil.listToTable(getChildrenOfClass(arg.toString()));
			}
		});
		
		this.getmetatable().set("WaitForChild", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue child, LuaValue time) {
				long start = System.currentTimeMillis();
				int t = (int) (time.isnil()?5000:(time.checkdouble()*1000));
				while ( System.currentTimeMillis()-start < t ) {
					Instance c = findFirstChild(child.toString());
					if ( c == null ) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							//
						}
					} else {
						return c;
					}
				}
				LuaEngine.error("Cancelling wait for child loop. Time not specified & taking too long.");
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("WaitForChildOfClass", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue child, LuaValue time) {
				long start = System.currentTimeMillis();
				int t = (int) (time.isnil()?5000:(time.checkdouble()*1000));
				while ( System.currentTimeMillis()-start < t ) {
					Instance c = findFirstChildOfClass(child.toString());
					if ( c == null ) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							//
						}
					} else {
						return c;
					}
				}
				LuaEngine.error("Cancelling wait for child loop. Time not specified & taking too long.");
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("FindFirstChild", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				Instance child = findFirstChild(arg.toString());
				return child == null ? LuaValue.NIL : child;
			}
		});
		
		this.getmetatable().set("FindFirstChildOfClass", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				Instance child = findFirstChildOfClass(arg.toString());
				return child == null ? LuaValue.NIL : child;
			}
		});

		this.getmetatable().set("IsDescendantOf", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignoreme, LuaValue arg) {
				return isDescendantOf(arg)?LuaValue.TRUE:LuaValue.FALSE;
			}
		});

		this.getmetatable().set("Destroy", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if ( !locked && isInstanceable() ) {
					Instance.this.destroy();
				} else {
					LuaValue.error("This object cannot be destroyed.");
				}
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("IsA", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg) {
				String str = arg.checkjstring();
				return Instance.AExtendsB(Instance.this,str);
			}
		});

		this.getmetatable().set("Clone", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if ( !locked && isInstanceable() ) {
					try {
						Instance inst = Instance.this.clone();
						if ( inst == null )
							return LuaValue.NIL;
						
						inst.rawset(C_PARENT, LuaValue.NIL);
						return inst;
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					LuaValue.error("This object cannot be cloned.");
				}
				return LuaValue.NIL;
			}
		});
	}
	
	/**
	 * Returns if class1 comes from or is class2
	 * @param rawget
	 * @param str
	 * @return
	 */
	protected static HashMap<String,ArrayList<String>> extends_cache = new HashMap<String,ArrayList<String>>();
	protected static LuaValue AExtendsB(Instance instance1, String class2) {
		String class1 = instance1.rawget(C_CLASSNAME).toString();
		if ( class1.equals(class2) )
			return LuaValue.TRUE;
		
		// Do a cache check to avoid slow code below
		boolean contains = extends_cache.containsKey(class2);
		if ( contains ) {
			ArrayList<String> subclasses = extends_cache.get(class2);
			if ( subclasses.contains(class1) )
				return LuaValue.TRUE;
		}
		
		// Create an instance of the desired class (SLOW)
		DataModel instance2 = Instance.instance(class2);
		if ( instance2 == null )
			return LuaValue.FALSE;
		if ( !(instance2 instanceof Instance) )
			return LuaValue.FALSE;

		// Get classes
		Class<? extends Instance> c1 = instance1.getClass();
		Class<? extends DataModel> c2 = instance2.getClass();
		
		// If they are the same, or parent (c2) contains this child (c1)
		if ( c1.equals(c2) || c1.isAssignableFrom(c2) ) {
			
			// Update cache so it's faster next time
			if ( !contains )
				extends_cache.put(class2, new ArrayList<String>());
			extends_cache.get(class2).add(class1);
			
			// Return
			return LuaValue.TRUE;
		}
		
		// Delete instance
		instance2.cleanup();
		
		return LuaValue.FALSE;
	}
	
	/**
	 * Instantiate an Instance by type.
	 * @param type
	 * @return
	 */
	public static Instance instance(String type) {
		if ( type == null )
			return null;
		
		LuaInstancetypeData c = TYPES.get(type);
		if ( c == null )
			return null;
		try {
			return (Instance) c.instanceableClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * For every child in the instance, destroy it.
	 */
	public void clearAllChildren() {
		synchronized(children) {
			for (int i = children.size()-1;i>=0; i--) {
				Instance child = children.get(i);
				child.destroy();
			}
		}
		children.clear();
	}


	/**
	 * For every lua connection in the instance, disconnect it.
	 */
	public void clearAllConnections() {
		LuaValue[] keys = this.keys();
		for (int i = 0; i < keys.length; i++) {
			LuaValue value = this.get(keys[i]);
			if ( value instanceof LuaEvent ) {
				LuaEvent event = (LuaEvent) value;
				event.disconnectAll();
			}
		}
	}
	
	@Override
	public Instance clone() {
		try {
			// You need to be archivable to be cloned.
			if ( !this.get(C_ARCHIVABLE).checkboolean() )
				return null;
			
			// Create a new instance of this type
			Instance inst = Instance.this.getClass().newInstance();
			
			// Copy keys into the new instance
			LuaValue[] keys = this.keys();
			for (int i = 0; i < keys.length; i++) {
				LuaValue key = keys[i];
				
				// Make sure to only set the fields.
				if ( !this.containsField(key) )
					continue;
				
				// Parent is special and gets set later
				if ( key.eq_b(C_PARENT) )
					continue;
				
				// SID is special. Do not save
				if ( key.eq_b(C_SID) )
					continue;
				
				// Set
				LuaValue value = this.rawget(key);
				if ( value instanceof LuaValuetype )
					value = (LuaValue) ((LuaValuetype)value).clone();
				
				inst.rawset(key.toString(),value);
				//try{ inst.set(key.toString(), this.rawget(key)); } catch(Exception ee) {}
			}
			
			// Clone all children
			//synchronized(children) {
				for (int i = 0; i < this.children.size(); i++) {
					Instance child = children.get(i);
					Instance t = child.clone();
					if ( t != null ) {
						t.forceSetParent(inst);
					}
				}
			//}
			
			// Return
			return inst;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void rawset(LuaValue key, LuaValue value) {
		super.rawset(key, value);
	}

	public void internalTick() {
		//
	}

	public void destroy() {
		if ( destroyed )
			return;
		
		destroyed = true;
		
		// Force parent to nil (to make sure child/descendant removed events fire)
		this.forceSetParent(LuaValue.NIL);
		
		if ( initialized ) {
			// Destroy children
			List<Instance> ch = getChildren();
			for (int i = 0; i < ch.size(); i++) {
				ch.get(i).destroy();
			}
			children.clear();
			propertySubscribers.clear();
	
			// Call destroy function
			this.onDestroy();
			this.destroyedEvent().fire();
		}
		// Destroy all values
		this.cleanup();
		
		// Make sure the game knows to deselect us! (just in-case)
		Game.deselect(this);
	}
	
	public boolean isDestroyed() {
		return this.destroyed;
	}

	public abstract void onDestroy();

	public List<Instance> getChildren() {
		return children;
	}

	/**
	 * Returns the first child with the matching name.
	 * <br>
	 * It is O(1) time if the name is NOT the name of a field for the object.
	 * <br>
	 * if the name parameter is the name of a field, it is O(n) time, as it has to search for the first child.
	 * <br>
	 * Non field name objects are automatically generated when added as a child.
	 * @param name
	 * @return
	 */
	public Instance findFirstChild(String name) {
		return findFirstChild(LuaValue.valueOf(name));
	}
	
	/**
	 * Returns the first child with the matching name.
	 * <br>
	 * It is O(1) time if the name is NOT the name of a field for the object.
	 * <br>
	 * if the name parameter is the name of a field, it is O(n) time, as it has to search for the first child.
	 * <br>
	 * Non field name objects are automatically generated when added as a child.
	 * @param name
	 * @return
	 */
	public Instance findFirstChild(LuaValue name) {
		if ( this.containsField(name) ) {
			synchronized(children) {
				for (int i = 0; i < children.size(); i++) {
					Instance child = children.get(i);
					if ( child.get(C_NAME).eq_b(name) ) {
						return child;
					}
				}
			}
			return null;
		}
		LuaValue temp = this.get(name);
		if ( temp instanceof Instance && ((Instance)temp).get(C_NAME).eq_b(name) )
			return (Instance)temp;
		
		return null;
	}

	/**
	 * Returns the first child whos class matches the desired class name. This method is O(n) time.
	 * @param name
	 * @return
	 */
	public Instance findFirstChildOfClass(LuaValue name) {
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child.getClassName().eq_b(name) ) {
					return child;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the first child whos class matches the desired class name. This method is O(n) time.
	 * @param name
	 * @return
	 */
	public Instance findFirstChildOfClass(String name) {
		return this.findFirstChildOfClass(LuaValue.valueOf(name));
	}
	
	/**
	 * Returns a list of children whos classes match the desired class name. This method is O(n) time.
	 * @param className
	 * @return
	 */
	public List<Instance> getChildrenOfClass(LuaValue className) {
		List<Instance> ret = new ArrayList<Instance>();
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				LuaValue cName = child.getClassName();
				if ( cName.eq_b(className) ) {
					ret.add(child);
				}
			}
		}

		return ret;
	}
	
	/**
	 * Returns a list of children whos classes match the desired class name. This method is O(n) time.
	 * @param className
	 * @return
	 */
	public List<Instance> getChildrenOfClass(String className) {
		return this.getChildrenOfClass(LuaValue.valueOf(className));
	}

	public List<Instance> getChildrenWithName(String name) {
		return getChildrenWithName(LuaValue.valueOf(name));
	}

	public List<Instance> getChildrenWithName(LuaValue name) {
		List<Instance> ret = new ArrayList<Instance>();
		
		for (int i = 0; i < children.size(); i++) {
			if ( i >= children.size() )
				continue;
			Instance child = children.get(i);
			if ( child == null )
				continue;
			
			LuaValue cName = child.get(C_NAME);
			if ( cName.eq_b(name) ) {
				ret.add(child);
			}			
		}

		return ret;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public LuaValue tostring() {
		return LuaValue.valueOf(getName());
	}

	public void detach(InstancePropertySubscriber propertySubscriber) {
		synchronized(propertySubscribers) {
			propertySubscribers.remove(propertySubscriber);
		}
	}

	public void attachPropertySubscriber(InstancePropertySubscriber propertySubscriber) {
		synchronized(propertySubscribers) {
			propertySubscribers.add(propertySubscriber);
		}
	}
}
