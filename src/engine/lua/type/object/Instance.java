package engine.lua.type.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.LuaEngine;
import engine.lua.type.DataModel;
import engine.lua.type.LuaEvent;
import engine.lua.type.LuaField;
import engine.lua.type.LuaValuetype;

public abstract class Instance extends DataModel {
	protected List<Instance> children = Collections.synchronizedList(new ArrayList<Instance>());
	private HashSet<Instance> descendents = new HashSet<Instance>();
	private ArrayList<Instance> descendentsList = new ArrayList<Instance>();
	private List<InstancePropertySubscriber> propertySubscribers = Collections.synchronizedList(new ArrayList<InstancePropertySubscriber>());
	protected boolean destroyed;

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
				List<Instance> temp = getChildren();
				LuaTable table = new LuaTable();
				for (int i = 0; i < temp.size(); i++) {
					table.set(i+1, temp.get(i));
				}
				return table;
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
				List<Instance> temp = getChildrenWithName(arg.toString());
				LuaTable table = new LuaTable();
				for (int i = 0; i < temp.size(); i++) {
					table.set(i+1, temp.get(i));
				}
				return table;
			}
		});

		this.getmetatable().set("GetChildrenOfClass", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue root, LuaValue arg) {
				List<Instance> temp = getChildrenOfClass(arg.toString());
				LuaTable table = new LuaTable();
				for (int i = 0; i < temp.size(); i++) {
					table.set(i+1, temp.get(i));
				}
				return table;
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
							Thread.sleep(5);
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
							Thread.sleep(5);
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
		DataModel instance2 = DataModel.instance(class2);
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
	
	public void clearAllChildren() {
		synchronized(children) {
			for (int i = children.size()-1;i>=0; i--) {
				Instance child = children.get(i);
				child.destroy();
			}
		}
		children.clear();
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
				if ( !this.containsField(key.toString()) )
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
						t.setParent(inst);
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

	public boolean isDescendantOf( LuaValue arg ) {

		int tries = 0;
		LuaValue current = this;
		if ( current.equals(arg) ) {
			return false;
		}

		while ( !current.isnil() && tries < 64 ) {
			tries++;
			if ( current.equals(arg) ) {
				return true;
			}
			current = current.get(C_PARENT);
		}

		return false;
	}
	
	public void forceset(String key, LuaValue value) {
		forceset(LuaValue.valueOf(key),value);
	}
	
	private void onKeyChange(LuaValue key, LuaValue value) {
		LuaEvent event = this.changedEvent();
		event.fire(key, value);
		notifyPropertySubscribers(key.toString(), value);
	}
	
	public void forceset(LuaValue key, LuaValue value) {
		LuaValue oldValue = this.get(key);
		this.rawset(key, value);
		
		if ( !oldValue.equals(value) ) {
			onKeyChange( key, value );
		}
	}
	
	@Override
	public void rawset(LuaValue key, LuaValue value) {
		super.rawset(key, value);
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		LuaValue oldValue = this.get(key);
		boolean changed = !oldValue.equals(value);
		
		super.set( key, value );
		
		checkSetParent(key, oldValue, value); // value may have changed
		checkSetName(key, oldValue, value); // value may have changed

		if ( !changed )
			return;
		
		onKeyChange( key, this.get(key) );
	}
	
	public void notifyPropertySubscribers(String key, LuaValue value) {
		for (int i = 0; i < propertySubscribers.size(); i++) {
			if ( i >= propertySubscribers.size() )
				continue;
			InstancePropertySubscriber t = propertySubscribers.get(i);
			if ( t == null )
				continue;
			t.onPropertyChange(this, key, value);
		}
	}

	private void checkSetName(LuaValue key, LuaValue oldValue, LuaValue newValue) {
		if ( key.eq_b(C_NAME) ) {
			LuaValue currentParent = this.get(C_PARENT);
			if ( !currentParent.equals(LuaValue.NIL) && currentParent instanceof Instance ) {
				String oldName = oldValue.toString();
				String newName = newValue.toString();
				
				// See if there's still a child pointer with our old name
				List<Instance> children1 = ((Instance)currentParent).getChildrenWithName(oldName);
				if ( children1.size() > 1 ) {
					children1.remove(this);
					((Instance)currentParent).updateChildPointer(oldName, children1.get(0));
				} else {
					((Instance)currentParent).updateChildPointer(oldName, LuaValue.NIL);
				}
				
				// If the parent doesn't have a pointer pointing to NEW name
				if ( currentParent.rawget(newName).isnil() ) {
					((Instance)currentParent).updateChildPointer(newName, this);
				}
			}
			
			Game.getGame().gameUpdate(false);
		}
	}

	private void checkSetParent(LuaValue key, LuaValue oldParent, LuaValue newParent) {
		if ( key.eq_b(C_PARENT) ) {
			
			// If the parent hasen't changed, don't run code.
			if ( oldParent.equals(newParent) ) {
				return;
			}
			
			String name = this.getName();
			
			// Check for descendant removed
			descendantRemoved(oldParent);
			for (int i = 0; i < descendentsList.size(); i++) {
				descendentsList.get(i).descendantRemoved(oldParent);
			}
			
			// Add self to new parent
			if ( newParent instanceof Instance ) {
				Instance newParInst = (Instance) newParent;
				
				// Add to children list
				List<Instance> newParentChildren = newParInst.getChildren();
				//synchronized(newParentChildren) {
					newParentChildren.add(this);
				//}
				
				// Fire added event
				((LuaEvent)newParInst.rawget("ChildAdded")).fire(this);
	
				// Fire descendant added event
				descendantAdded(newParInst);
				for (int i = 0; i < descendentsList.size(); i++) {
					descendentsList.get(i).descendantAdded(newParInst);
				}
				
				// Add new reference
				LuaValue temp = newParInst.get(name);
				if ( temp.equals(LuaValue.NIL) && newParInst.getField(name) == null ) {
					newParInst.rawset(name, this);
				}
			}
			
			// Delete self from old parent reference
			if ( oldParent instanceof Instance ) {
				List<Instance> oldParentChildren = ((Instance)oldParent).getChildren();
				synchronized(oldParentChildren) {
					oldParentChildren.remove(this);
					
					// Get first child remaining with this name
					LuaValue firstWithName = LuaValue.NIL;
					for (int i = 0; i < oldParentChildren.size(); i++) {
						Instance temp = oldParentChildren.get(i);
						if ( temp.getName().equalsIgnoreCase(name) ) {
							firstWithName = temp;
							break;
						}
					}
					
					// Set the reference to that child. NIL if no child found.
					((Instance)oldParent).updateChildPointer(name, firstWithName);
				}
				
				// Child has finished being removed. Fire event.
				((LuaEvent)oldParent.rawget("ChildRemoved")).fire(this);
			}
			Game.getGame().gameUpdate(true);
		}
	}

	private void descendantRemoved(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		Instance r = (Instance)root;
		if ( r.descendents.contains(this) && !this.isDescendantOf(r) ) {
			descendantRemovedForce(root);
		}
	}
	
	private void descendantRemovedForce(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		Instance r = (Instance)root;
		if ( r.descendents.contains(this) ) {
			((LuaEvent)r.rawget("DescendantRemoved")).fire(this);
			r.descendents.remove(this);
			r.descendentsList.remove(this);
			descendantRemovedForce(r.getParent());
		}
	}

	private void descendantAdded(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		Instance r = (Instance)root;
		
		if ( !r.descendents.contains(this) ) {
			((LuaEvent)r.rawget("DescendantAdded")).fire(this);
			r.descendents.add(this);
			r.descendentsList.add(this);
			//System.out.println(this.getName() + " was added as descendent to " + r.getFullName());
		}
		descendantAdded(r.getParent());
	}

	public LuaValue getParent() {
		return this.get(C_PARENT);
	}
	
	public void setParent(LuaValue parent) {
		this.set(C_PARENT, parent);
	}
	
	public void forceSetParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;
		
		boolean l = this.locked;
		boolean l2 = !this.getField("Parent").canModify();

		LuaValue oldParent = this.get(C_PARENT);
		
		LuaField pField = this.getField("Parent");
		if ( pField == null )
			return;
		
		pField.setLocked(false);
		this.setLocked(false);
		this.set(C_PARENT, parent);
		this.setLocked(l);
		pField.setLocked(l2);
		
		this.checkSetParent(this.get(C_PARENT), oldParent, parent);
	}

	public String getName() {
		return get(C_NAME).toString();
	}
	
	public void setName(String name) {
		this.set(C_NAME, LuaValue.valueOf(name));
	}
	
	public String getClassName() {
		return this.get(C_CLASSNAME).toString();
	}
	
	public void forceSetName(String name) {
		boolean l = this.locked;
		boolean l2 = !this.getField("Name").canModify();
		
		this.getField("Name").setLocked(false);
		this.setLocked(false);
		this.set(C_NAME, LuaValue.valueOf(name));
		//this.rawset("Name", name);
		this.setLocked(l);
		this.getField("Name").setLocked(l2);
	}

	public void internalTick() {
		//
	}

	private void updateChildPointer( String name, LuaValue value ) {
		if ( this.containsField(name) ) {
			return;
		}
		this.rawset(name, value);
	}

	public void destroy() {
		destroyed = true;
		
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
		if ( this.containsField(name) ) {
			synchronized(children) {
				for (int i = 0; i < children.size(); i++) {
					Instance child = children.get(i);
					if ( child.get(C_NAME).toString().equals(name) ) {
						return child;
					}
				}
			}
			return null;
		}
		LuaValue temp = this.get(name);
		if ( temp instanceof Instance && ((Instance)temp).getName().equals(name) )
			return (Instance)temp;
		
		return null;
	}

	public Instance findFirstChildOfClass(String name) {
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child.getClassName().equals(name) ) {
					return child;
				}
			}
		}
		return null;
	}
	
	public List<Instance> getChildrenOfClass(String className) {
		List<Instance> ret = new ArrayList<Instance>();
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				String cName = child.getClassName();
				if ( cName.equals(className) ) {
					ret.add(child);
				}
			}
		}

		return ret;
	}

	public List<Instance> getChildrenWithName(String name) {
		List<Instance> ret = new ArrayList<Instance>();
		
		for (int i = 0; i < children.size(); i++) {
			if ( i >= children.size() )
				continue;
			Instance child = children.get(i);
			if ( child == null )
				continue;
			
			String cName = child.get(C_NAME).toString();
			if ( cName.equals(name) ) {
				ret.add(child);
			}			
		}

		return ret;
	}
	
	@Override
	public String toString() {
		return getName();
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

	public List<Instance> getDescendents() {
		/*ArrayList<Instance> d = new ArrayList<Instance>();
		synchronized(descendents) {
			Iterator<Instance> it = descendents.iterator();
			while ( it.hasNext() ) {
				d.add(it.next());
			}
		}
		return d;*/
		
		return new ArrayList<Instance>(descendentsList);
	}
}
