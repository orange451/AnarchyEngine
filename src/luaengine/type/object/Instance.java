package luaengine.type.object;

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
import luaengine.LuaEngine;
import luaengine.type.LuaEvent;
import luaengine.type.LuaField;
import luaengine.type.LuaInstancetype;
import luaengine.type.LuaValuetype;

public abstract class Instance extends LuaInstancetype {
	private List<Instance> children = Collections.synchronizedList(new ArrayList<Instance>());
	private HashSet<Instance> descendents = new HashSet<Instance>();
	private List<InstancePropertySubscriber> propertySubscribers = Collections.synchronizedList(new ArrayList<InstancePropertySubscriber>());
	protected boolean destroyed;
	
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
						
						inst.rawset("Parent", LuaValue.NIL);
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
		String class1 = instance1.rawget("ClassName").toString();
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
		LuaInstancetype instance2 = LuaInstancetype.instance(class2);
		if ( instance2 == null )
			return LuaValue.FALSE;
		if ( !(instance2 instanceof Instance) )
			return LuaValue.FALSE;

		// Get classes
		Class<? extends Instance> c1 = instance1.getClass();
		Class<? extends LuaInstancetype> c2 = instance2.getClass();
		
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
			if ( !this.get("Archivable").checkboolean() )
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
				if ( key.toString().equals("Parent") )
					continue;
				
				// SID is special. Do not save
				if ( key.toString().equals("SID") )
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
			return true;
		}

		while ( !current.isnil() && tries < 64 ) {
			tries++;
			if ( current.equals(arg) ) {
				return true;
			}
			current = current.get("Parent");
		}

		return false;
	}
	
	@Override
	public void rawset(LuaValue key, LuaValue value) {
		super.rawset(key, value);
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		LuaValue oldValue = this.get(key);
		super.set( key, value );
		checkSetParent(key, oldValue, value); // value may have changed
		checkSetName(key, oldValue, value); // value may have changed

		LuaEvent event = this.changedEvent();
		event.fire(key, value);
		
		notifySubscribers(key.toString(), value);
	}
	
	public void notifySubscribers(String key, LuaValue value) {
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
		if ( key.toString().equals("Name") ) {
			LuaValue currentParent = get("Parent");
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
			
			Game.getGame().gameUpdate(true);
		}
	}

	private void checkSetParent(LuaValue key, LuaValue oldParent, LuaValue newParent) {
		if ( key.toString().equals("Parent") ) {
			
			// If the parent hasen't changed, don't run code.
			if ( oldParent.equals(newParent) ) {
				return;
			}
			
			String name = this.getName();
			List<Instance> desc = this.getDescendents();
			
			// Check for descendant removed
			descendantRemoved(oldParent);
			for (int i = 0; i < desc.size(); i++) {
				desc.get(i).descendantRemoved(oldParent);
			}
			
			// Add self to new parent
			if ( newParent instanceof Instance ) {
				Instance newParInst = (Instance) newParent;
				
				// Add to children list
				synchronized((newParInst).getChildren()) {
					(newParInst).getChildren().add(this);
					((LuaEvent)newParInst.rawget("ChildAdded")).fire(this);
				}
	
				descendantAdded(newParInst);
				for (int i = 0; i < desc.size(); i++) {
					desc.get(i).descendantAdded(newParInst);
				}
				
				// Add new value
				LuaValue temp = newParInst.get(name);
				if ( temp.equals(LuaValue.NIL) ) {
					newParInst.rawset(name, this);
				}
			}
			
			// Delete self from old parent
			if ( !oldParent.equals(LuaValue.NIL) && oldParent instanceof Instance ) {
				synchronized(((Instance)oldParent).getChildren()) {
					((Instance)oldParent).getChildren().remove(this);
					((LuaEvent)oldParent.rawget("ChildRemoved")).fire(this);
				}
				
				List<Instance> children1 = ((Instance)oldParent).getChildrenWithName(name);
				if ( children1.size() <= 1 ) {
					((Instance)oldParent).updateChildPointer(name, LuaValue.NIL);
				} else {
					children1.remove(this);
					((Instance)oldParent).updateChildPointer(name, children1.get(0));
				}
			}
			Game.getGame().gameUpdate(true);
		}
	}

	private void descendantRemoved(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		Instance r = (Instance)root;
		if ( r.descendents.contains(this) && !this.isDescendantOf(r) ) {
			((LuaEvent)r.rawget("DescendantRemoved")).fire(this);
			r.descendents.remove(this);
			//System.out.println(this.getName() + " was removed as descendent from " + r.getFullName());
			descendantRemoved(r.getParent());
		}
	}

	private void descendantAdded(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		Instance r = (Instance)root;
		
		if ( !r.descendents.contains(this) ) {
			((LuaEvent)r.rawget("DescendantAdded")).fire(this);
			r.descendents.add(this);
			//System.out.println(this.getName() + " was added as descendent to " + r.getFullName());
		}
		descendantAdded(r.getParent());
	}

	public LuaValue getParent() {
		return this.get("Parent");
	}
	
	public void setParent(LuaValue parent) {
		this.set("Parent", parent);
	}
	
	public void forceSetParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;
		
		boolean l = this.locked;
		boolean l2 = !this.getField("Parent").canModify();

		LuaValue oldParent = this.get("Parent");
		
		LuaField pField = this.getField("Parent");
		if ( pField == null )
			return;
		
		pField.setLocked(false);
		this.setLocked(false);
		this.set("Parent", parent);
		this.setLocked(l);
		pField.setLocked(l2);
		
		this.checkSetParent(this.get(LuaValue.valueOf("Parent")), oldParent, parent);
	}

	public String getName() {
		return get("Name").toString();
	}
	
	public void setName(String name) {
		this.set("Name", name);
	}
	
	public void forceSetName(String name) {
		boolean l = this.locked;
		boolean l2 = !this.getField("Name").canModify();
		
		this.getField("Name").setLocked(false);
		this.setLocked(false);
		this.set("Name", name);
		//this.rawset("Name", name);
		this.setLocked(l);
		this.getField("Name").setLocked(l2);
	}

	public String getFullName() {
		String ret = "";
		LuaValue p = this;
		int i = 0;
		while ( p != null && p instanceof Instance ) {
			Instance inst = ((Instance)p);
			p = inst.getParent();
			
			if ( i > 0 )
				ret = "." + ret;
			ret = inst.getName() + ret;
			
			i++;
		}
		return ret;
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

	public Instance findFirstChild(String name) {
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child.get("Name").toString().equals(name) ) {
					return child;
				}
			}
		}
		return null;
	}

	public Instance findFirstChildOfClass(String name) {
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child.get("ClassName").toString().equals(name) ) {
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
				String cName = child.get("ClassName").toString();
				if ( cName.equals(className) ) {
					ret.add(child);
				}
			}
		}

		return ret;
	}

	public List<Instance> getChildrenWithName(String name) {
		List<Instance> ret = new ArrayList<Instance>();
		synchronized(children) {
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				String cName = child.get("Name").toString();
				if ( cName.equals(name) ) {
					ret.add(child);
				}
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
		ArrayList<Instance> d = new ArrayList<Instance>();
		synchronized(descendents) {
			Iterator<Instance> it = descendents.iterator();
			while ( it.hasNext() ) {
				d.add(it.next());
			}
		}
		return d;
	}
}
