package engine.lua.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import engine.Game;
import engine.lua.LuaEngine;
import engine.lua.type.object.Instance;
import engine.lua.type.object.InstancePropertySubscriber;

public abstract class DataModel extends LuaDatatype {
	protected List<Instance> children = Collections.synchronizedList(new ArrayList<Instance>());
	private HashSet<Instance> descendents = new HashSet<Instance>();
	private ArrayList<Instance> descendentsList = new ArrayList<Instance>();
	protected List<InstancePropertySubscriber> propertySubscribers = Collections.synchronizedList(new ArrayList<InstancePropertySubscriber>());
	
	protected static HashMap<String,LuaInstancetypeData> TYPES = new HashMap<String,LuaInstancetypeData>();

	private static final LuaValue C_CHANGED = LuaValue.valueOf("Changed");
	private static final LuaValue C_DESTROYED = LuaValue.valueOf("Destroyed");
	private static final LuaValue C_CHILDADDED = LuaValue.valueOf("ChildAdded");
	private static final LuaValue C_CHILDREMOVED = LuaValue.valueOf("ChildRemoved");
	private static final LuaValue C_DESCENDANTADDED = LuaValue.valueOf("DescendantAdded");
	private static final LuaValue C_DESCENDANTREMOVED = LuaValue.valueOf("DescendantRemoved");
	private static final LuaValue C_ARCHIVABLE = LuaValue.valueOf("Archivable");
	private static final LuaValue C_SID = LuaValue.valueOf("SID");

	private static final LuaValue C_CLASSNAME = LuaValue.valueOf("ClassName");
	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	
	protected boolean initialized;
	
	private String internalName;
	
	public class LuaInstancetypeData {
		public Class<?> instanceableClass;
		boolean instanceable = true;

		LuaInstancetypeData( Class<?> cls ) {
			this.instanceableClass = cls;
		}
	}

	static {
		LuaTable table = new LuaTable();
		table.set("new", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				String searchType = arg.toString();
				LuaInstancetypeData c = TYPES.get(searchType);
				if ( c != null ) {
					if ( c.instanceable ) {
						try {
							return (DataModel) c.instanceableClass.newInstance();
						} catch (Exception e) {
							//
						}
					} else {
						LuaValue.error("Cannot instantiate object of type " + searchType);
					}
				}
				return LuaValue.NIL;
			}
		});
		table.set(LuaValue.INDEX, table);
		LuaEngine.globals.set("Instance", table);
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<Class<? extends Instance>> getInstanceableTypes() {
		ArrayList<Class<? extends Instance>> ret = new ArrayList<Class<? extends Instance>>();
		String[] keys = TYPES.keySet().toArray(new String[TYPES.keySet().size()]);
		for (int i = 0; i < keys.length; i++) {
			LuaInstancetypeData datatype = TYPES.get(keys[i]);
			if ( datatype.instanceable ) {
				ret.add((Class<? extends Instance>) datatype.instanceableClass);
			}
		}
		
		return ret;
	}

	public DataModel(String name) {
		if ( !TYPES.containsKey(name) ) {
			TYPES.put(name, new LuaInstancetypeData(this.getClass()));
		}
		LuaTable table = new LuaTable();
		table.set(LuaValue.INDEX, table);
		this.setmetatable(table);

		this.defineField(C_NAME.toString(),			LuaValue.valueOf(name), false);
		this.defineField(C_CLASSNAME.toString(),	LuaValue.valueOf(name), true);
		this.defineField(C_PARENT.toString(),		LuaValue.NIL,			false);
		this.defineField(C_SID.toString(), 			LuaValue.valueOf(-1),   true);
		this.defineField(C_ARCHIVABLE.toString(),	LuaValue.valueOf(true), false);

		this.rawset(C_CHANGED,		new LuaEvent());
		this.rawset(C_DESTROYED,	new LuaEvent());
		this.rawset(C_CHILDADDED,	new LuaEvent());
		this.rawset(C_CHILDREMOVED,	new LuaEvent());
		this.rawset(C_DESCENDANTADDED,		new LuaEvent());
		this.rawset(C_DESCENDANTREMOVED,	new LuaEvent());

		this.internalName = name;
		this.initialized = true;
	}
	
	public boolean isArhivable() {
		return this.get(C_ARCHIVABLE).toboolean();
	}
	
	public void setArchivable(boolean archivable) {
		this.set(C_ARCHIVABLE, LuaValue.valueOf(archivable));
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

	@Override
	public void set(LuaValue key, LuaValue value) {
		LuaValue oldValue = this.rawget(key);
		boolean changed = !oldValue.equals(value);
		
		// Hacked in double comparison... Since luaJ uses == for comparing doubles :(
		if ( value instanceof LuaNumber || oldValue instanceof LuaNumber ) {
			double v1 = value.todouble();
			double v2 = oldValue.todouble();
			if (Math.abs(v1 - v2) < 0.001)
				changed = false;
		}
		
		super.set( key, value );
		
		checkSetParent(key, oldValue, value); // value may have changed
		checkSetName(key, oldValue, value); // value may have changed

		// Call change event only if value changes
		if ( changed ) {
			//if ( key.toString().equals("Height") ) {
				/*System.out.println();
				System.out.println("KEY CHANGE SETTING: " + key.toString() + " from [" + oldValue.toString() + "] to [" + value.toString() + "]");
				System.out.println(oldValue.getClass() + " / " + value.getClass());
				System.out.println(oldValue.equals(value) + " / " + oldValue.eq(value) + " / " + oldValue.eq_b(value));
				System.out.println();*/
			//}
			
			onKeyChange( key, this.get(key) );
		}
	}
	
	@Override
	public void rawset(LuaValue key, LuaValue value) {
		super.rawset(key, value);
		if ( key.eq_b(C_NAME) ) {
			this.internalName = value.toString();
		}
	}
	
	public void forceset(String key, LuaValue value) {
		forceset(LuaValue.valueOf(key),value);
	}
	
	public void forceset(LuaValue key, LuaValue value) {
		LuaValue oldValue = this.get(key);
		this.rawset(key, value);
		
		if ( !oldValue.equals(value) ) {
			onKeyChange( key, value );
		}
	}
	
	public void notifyPropertySubscribers(LuaValue key, LuaValue value) {
		for (int i = 0; i < propertySubscribers.size(); i++) {
			if ( i >= propertySubscribers.size() )
				continue;
			InstancePropertySubscriber t = propertySubscribers.get(i);
			if ( t == null )
				continue;
			t.onPropertyChange((Instance) this, key, value);
		}
	}
	
	public boolean isDescendantOf( LuaValue object ) {
		if ( object == null )
			return false;
		
		if ( object.isnil() && this.getParent().isnil() )
			return true;
		
		if ( object.isnil() )
			return false;
		
		DataModel inst = (DataModel)object;
		return inst.descendents.contains(this);
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
	
	private void onKeyChange(LuaValue key, LuaValue value) {
		LuaEvent event = this.changedEvent();
		event.fire(key, value);
		notifyPropertySubscribers(key, value);
	}
	
	private boolean computeDescendant( LuaValue arg ) {

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
	
	private void checkSetName(LuaValue key, LuaValue oldName, LuaValue newName) {
		if ( key.eq_b(C_NAME) ) {
			LuaValue currentParent = this.get(C_PARENT);
			if ( !currentParent.equals(LuaValue.NIL) && currentParent instanceof Instance ) {
				
				// See if there's still a child pointer with our old name
				List<Instance> children1 = ((Instance)currentParent).getChildrenWithName(oldName.toString());
				if ( children1.size() > 0 ) {
					children1.remove(this);
					((DataModel)currentParent).updateChildPointer(oldName, children1.get(0));
				} else {
					((DataModel)currentParent).updateChildPointer(oldName, LuaValue.NIL);
				}
				
				// If the parent doesn't have a pointer pointing to NEW name
				if ( currentParent.rawget(newName).isnil() ) {
					((DataModel)currentParent).updateChildPointer(newName, this);
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
				((DataModel)descendentsList.get(i)).descendantRemoved(oldParent);
			}
			
			// Add self to new parent
			if ( newParent instanceof Instance ) {
				Instance newParInst = (Instance) newParent;
				
				// Add to children list
				List<Instance> newParentChildren = newParInst.getChildren();
				//synchronized(newParentChildren) {
					newParentChildren.add((Instance) this);
				//}
				
				// Fire added event
				newParInst.childAddedEvent().fire(this);
	
				// Fire descendant added event
				descendantAdded(newParInst);
				for (int i = 0; i < descendentsList.size(); i++) {
					((DataModel)descendentsList.get(i)).descendantAdded(newParInst);
				}
				
				// Add new reference
				LuaValue temp = newParInst.get(name);
				if ( temp.equals(LuaValue.NIL) && newParInst.getField(this.rawget(C_NAME)) == null ) {
					newParInst.rawset(name, this);
				}
			}
			
			// Delete self from old parent reference
			if ( oldParent instanceof Instance ) {
				List<Instance> oldParentChildren = ((Instance)oldParent).getChildren();
				synchronized(oldParentChildren) {
					oldParentChildren.remove(this);
					
					// If the parents reference by name points to this instance...
					if ( oldParent.rawget(this.rawget(C_NAME)) == this ) {
						
						// Get first child remaining with the same name
						LuaValue firstWithName = LuaValue.NIL;
						for (int i = 0; i < oldParentChildren.size(); i++) {
							Instance temp = oldParentChildren.get(i);
							if ( temp.getName().equalsIgnoreCase(name) ) {
								firstWithName = temp;
								break;
							}
						}
						
						// Set the reference to that child. NIL if no child found.
						((DataModel)oldParent).updateChildPointer(this.rawget(C_NAME), firstWithName);
					}
				}
				
				// Child has finished being removed. Fire event.
				((Instance) oldParent).childRemovedEvent().fire(this);
			}
			Game.getGame().gameUpdate(true);
		}
	}

	private void descendantRemoved(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		DataModel r = (Instance)root;
		if ( r.descendents.contains(this) && !this.computeDescendant(r) ) {
			descendantRemovedForce(root);
		}
	}
	
	private void descendantRemovedForce(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		DataModel r = (Instance)root;
		if ( r.descendents.contains(this) ) {
			r.descendantRemovedEvent().fire(this);
			r.descendents.remove(this);
			r.descendentsList.remove(this);
			descendantRemovedForce(r.getParent());
		}
	}

	private void descendantAdded(LuaValue root) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		DataModel r = (DataModel)root;
		
		if ( !r.descendents.contains(this) ) {
			r.descendantAddedEvent().fire(this);
			r.descendents.add((Instance) this);
			r.descendentsList.add((Instance) this);
			//System.out.println(this.getName() + " was added as descendent to " + r.getFullName());
		}
		descendantAdded(r.getParent());
	}
	
	private void updateChildPointer( LuaValue name, LuaValue value ) {
		if ( this.containsField(name) ) {
			return;
		}
		this.rawset(name, value);
	}
	
	public void forceSetName(String name) {
		boolean l = this.locked;
		boolean l2 = !this.getField(C_NAME).canModify();
		
		this.getField(C_NAME).setLocked(false);
		this.setLocked(false);
		this.set(C_NAME, LuaValue.valueOf(name));
		//this.rawset(nameField, name);
		this.setLocked(l);
		this.getField(C_NAME).setLocked(l2);
		
		this.internalName = name;
	}

	public String getName() {
		return internalName;
	}
	
	public void setName(String name) {
		this.set(C_NAME, LuaValue.valueOf(name));
		this.internalName = name;
	}
	
	public String getClassName() {
		return this.get(C_CLASSNAME).toString();
	}

	public LuaValue getParent() {
		return this.get(C_PARENT);
	}
	
	public void setParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;
		
		this.set(C_PARENT, parent);
	}
	
	public void forceSetParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;
		
		//boolean l = this.locked;
		//boolean l2 = !this.getField("Parent").canModify();

		LuaValue oldParent = this.rawget(C_PARENT);
		
		/*LuaField pField = this.getField("Parent");
		if ( pField == null )
			return;*/
		
		//pField.setLocked(false);
		//this.setLocked(false);
		this.rawset(C_PARENT, parent);
		//this.setLocked(l);
		//pField.setLocked(l2);

		this.checkSetParent(C_PARENT, oldParent, parent);
	}
	
	public Long getSID() {
		return this.get(C_SID).tolong();
	}
	
	public LuaEvent changedEvent() {
		return (LuaEvent)this.rawget(C_CHANGED);
	}
	
	public LuaEvent destroyedEvent() {
		return (LuaEvent)this.rawget(C_DESTROYED);
	}
	
	public LuaEvent childAddedEvent() {
		return (LuaEvent)this.rawget(C_CHILDADDED);
	}
	
	public LuaEvent childRemovedEvent() {
		return (LuaEvent)this.rawget(C_CHILDREMOVED);
	}
	
	public LuaEvent descendantAddedEvent() {
		return (LuaEvent)this.rawget(C_DESCENDANTADDED);
	}
	
	public LuaEvent descendantRemovedEvent() {
		return (LuaEvent)this.rawget(C_DESCENDANTREMOVED);
	}

	public void setInstanceable( boolean instanceable ) {
		TYPES.get(this.get(C_CLASSNAME).toString()).instanceable = instanceable;
	}

	/**
	 * Returns whether or not this object-type is user-instancable.
	 * @return
	 */
	public boolean isInstanceable() {
		return TYPES.get(this.get(C_CLASSNAME).toString()).instanceable;
	}
	
	@Override
	public void cleanup() {
		this.setLocked(false);
		
		if ( this.hasKey(C_PARENT) ) {
			this.getField(C_PARENT).setLocked(false);
			this.set(C_PARENT, LuaValue.NIL);
		}
		
		super.cleanup();
	}
}
