/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.InstancePropertySubscriber;

public abstract class DataModel extends LuaDatatype {
	protected List<Instance> children = Collections.synchronizedList(new ArrayList<Instance>());
	protected HashSet<Instance> descendents = new HashSet<Instance>();
	protected List<Instance> descendentsList = Collections.synchronizedList(new ArrayList<Instance>());
	protected List<InstancePropertySubscriber> propertySubscribers = Collections.synchronizedList(new ArrayList<InstancePropertySubscriber>());
	protected HashMap<LuaValue, DataModel> cachedChildrenOfClass = new HashMap<LuaValue, DataModel>();
	
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
	
	protected final UUID uuid;
	
	protected boolean initialized;
	protected boolean destroyed;

	
	private String internalName;
	
	public class LuaInstancetypeData {
		public Class<?> instanceableClass;
		boolean instanceable = true;

		LuaInstancetypeData( Class<?> cls ) {
			this.instanceableClass = cls;
		}

		public boolean isInstanceable() {
			return this.instanceable;
		}
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
		
		this.uuid = UUID.randomUUID();
	}
	
	public LuaValue setmetatable(LuaValue metatable) {
		if ( initialized )
			return LuaValue.NIL;
		return super.setmetatable(metatable);
	}
	
	/**
	 * Returns whether or not the DataModel is archivable. An archivable DataModel will not be written to disk when saved.
	 * @return
	 */
	public boolean isArchivable() {
		return this.get(C_ARCHIVABLE).toboolean();
	}
	
	/**
	 * Sets the archivable flag of the DataModel. See {@link #isArchivable()}.
	 * @param archivable
	 */
	public void setArchivable(boolean archivable) {
		this.set(C_ARCHIVABLE, LuaValue.valueOf(archivable));
	}
	
	/**
	 * Get the full name for the Datamodel. This includes the names of all ancestors.
	 * @return
	 */
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
	
	protected boolean checkEquals(LuaValue value1, LuaValue value2) {
		boolean changed = !value2.equals(value1);
		
		// Hacked in double comparison... Since luaJ uses == for comparing doubles :(
		if ( value1 instanceof LuaNumber || value2 instanceof LuaNumber ) {
			double v1 = value1.todouble();
			double v2 = value2.todouble();
			if (Math.abs(v1 - v2) < 0.001)
				changed = false;
		}
		
		return !changed;
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		if ( this.destroyed )
			return;
		
		LuaValue oldValue = this.rawget(key);
		boolean changed = !checkEquals( value, oldValue);
		
		// Prevent setting parent to self
		if ( key.eq_b(C_PARENT) && value == this ) {
			throw new LuaError("Instance cannot be its own parent");
		}
		
		super.set( key, value );
		
		checkSetParent(key, oldValue, value); // value may have changed
		checkSetName(key, oldValue, value); // value may have changed

		// Call change event only if value changes
		if ( changed ) {
			if ( key.eq_b(C_NAME)) {
				this.internalName = value.toString();
			}
			onKeyChange( key, this.get(key), oldValue );
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
		if ( this.destroyed )
			return;
		
		LuaValue oldValue = this.get(key);
		boolean changed = !checkEquals( value, oldValue);
		
		this.rawset(key, value);
		
		checkSetParent(key, oldValue, value); // value may have changed
		checkSetName(key, oldValue, value); // value may have changed
		
		if ( changed ) {
			if ( key.eq_b(C_NAME)) {
				this.internalName = value.toString();
			}
			onKeyChange( key, value, oldValue );
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
	
	/**
	 * returns whether a DataModel is the descendant of a LuaValue. This should be checked against an Instance, but nil also works.
	 * @param object
	 * @return
	 */
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
	
	/**
	 * Returns an un-modifyable list of descendants.
	 * @return
	 */
	public List<Instance> getDescendants() {
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
	
	public List<Instance> getDescendantsUnsafe() {
		return this.descendentsList;
	}
	
	/**
	 * Returns the UUID for this DataModel. A UUID is unique to a specific DataModel/Instance in memory.
	 * It will be different each time the game loads, but will remain constant for duration of the game.
	 * @return
	 */
	public UUID getUUID() {
		return this.uuid;
	}
	
	private void onKeyChange(LuaValue key, LuaValue value, LuaValue oldValue) {
		if ( this.destroyed )
			return;
		
		this.onValueUpdated(key, value);
		Game.changes = true;
		
		LuaEvent event = this.changedEvent();
		if ( event != null ) {
			event.fire(key, value, oldValue);
			notifyPropertySubscribers(key, value);
		}
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
			current = current.rawget(C_PARENT);
		}

		return false;
	}
	
	private void checkSetName(LuaValue key, LuaValue oldName, LuaValue newName) {
		if ( key.eq_b(C_NAME) ) {
			LuaValue currentParent = this.get(C_PARENT);
			if ( !currentParent.equals(LuaValue.NIL) && currentParent instanceof Instance ) {
				
				// See if there's still a child pointer with our old name
				List<Instance> children1 = ((Instance)currentParent).getChildrenWithName(oldName.toString());
				if ( children1.size() > 1 ) {
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
			
			if ( newParent == this )
				throw new LuaError("Instance can not be its own parent");
			
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
			if ( newParent instanceof Instance && !((Instance)newParent).isDestroyed() ) {
				Instance newParInst = (Instance) newParent;
				
				// Add to children list
				List<Instance> newParentChildren = newParInst.getChildren();
				//synchronized(newParentChildren) {
					newParentChildren.add((Instance) this);
				//}
				
				// Fire added event
				newParInst.childAddedEvent().fire(this);
	
				// Fire descendant added event
				boolean forceAdd = oldParent.isnil();
				descendantAdded(newParInst, forceAdd);
				for (int i = 0; i < descendentsList.size(); i++) {
					((DataModel)descendentsList.get(i)).descendantAdded(newParInst, forceAdd);
				}
				
				// Set cached children of class lookup table FOR this class
				if ( !((Instance)newParInst).cachedChildrenOfClass.containsKey(this.getClassName()) )
					((Instance)newParInst).cachedChildrenOfClass.put(this.getClassName(), this);
				
				
				// Add new reference
				if ( newParInst.rawget(name).isnil() )
					newParInst.updateChildPointer( LuaValue.valueOf(name), this );
			}
			
			// Delete self from old parent reference
			if ( oldParent instanceof Instance && !((Instance) oldParent).destroyed ) {
				List<Instance> oldParentChildren = ((Instance)oldParent).getChildren();
				synchronized(oldParentChildren) {
					oldParentChildren.remove(this);

					// Reset cached children of class lookup table FOR this class
					if ( ((Instance)oldParent).cachedChildrenOfClass.containsKey(this.getClassName()) )
						((Instance)oldParent).cachedChildrenOfClass.remove(this.getClassName());
					
					// If the parents reference by name points to this instance...
					if ( oldParent.rawget(this.rawget(C_NAME)) == this ) {
						
						// Get first child remaining with the same name
						LuaValue firstWithName = LuaValue.NIL;
						for (int i = 0; i < oldParentChildren.size(); i++) {
							Instance temp = oldParentChildren.get(i);
							if ( temp.getName().equals(name) ) {
								firstWithName = temp;
								break;
							}
						}
						
						// Set the reference to that child. NIL if no child found.
						((DataModel)oldParent).updateChildPointer(LuaValue.valueOf(name), firstWithName);
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
		if ( r.descendents.contains(this) ) {
			boolean isStillDescendent = this.computeDescendant(r);
			
			if (!isStillDescendent) {
				if ( !r.destroyed ) {
					r.descendantRemovedEvent().fire(this);
					r.descendents.remove(this);
					r.descendentsList.remove(this);
					descendantRemoved(r.getParent());
				}
			}
		}
	}

	/**
	 * add THIS datamodel as a descendant of all its ancestors recursively.
	 * @param root
	 */
	private void descendantAdded(LuaValue root, boolean force) {
		if ( root == null || root.isnil() || !(root instanceof Instance) )
			return;
		
		DataModel r = (DataModel)root;
		if ( r.destroyed )
			return;
		
		if ( force || !r.descendents.contains(this) ) {
			r.descendantAddedEvent().fire(this);
			r.descendents.add((Instance) this);
			r.descendentsList.add((Instance) this);
			//System.out.println(this.getName() + " was added as descendent to " + r.getFullName());
		}
		
		LuaValue parent = r.getParent();
		if ( parent == root )
			return;
		
		descendantAdded(parent, force);
	}
	
	protected void updateChildPointer( LuaValue childName, LuaValue instanceReference ) {
		if ( this.containsField(childName) ) {
			return;
		}
		this.rawset(childName, instanceReference);
	}
	
	/**
	 * Force set the name flag of the DataModel even if it is locked.
	 * @param name
	 */
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

	/**
	 * Returns the name of the DataModel as a String.
	 * @return
	 */
	public String getName() {
		return internalName;
	}
	
	/**
	 * Sets the name of the DataModel.
	 * @param name
	 */
	public void setName(String name) {
		this.set(C_NAME, LuaValue.valueOf(name));
		this.internalName = name;
	}
	
	/**
	 * Returns the class-name of the DataModel as LuaValue.
	 * @return
	 */
	public LuaValue getClassName() {
		if ( this.destroyed )
			return LuaValue.NIL;
		
		return this.get(C_CLASSNAME);
	}

	/**
	 * Returns the parent of the DataModel. Normally this returns an Instance, but nil works too.<br>
	 * WILL NEVER BE NULL
	 * @return
	 */
	public LuaValue getParent() {
		if ( this.destroyed )
			return LuaValue.NIL;
		
		return this.get(C_PARENT);
	}
	
	/**
	 * Sets the parent of the DataModel. The parent must be either another DataModel or nil.
	 * @param parent
	 */
	public void setParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;
		
		if ( parent == this )
			throw new LuaError("Instance can not be its own parent");
		
		if ( parent == this.getParent() )
			return;
		
		if ( destroyed )
			return;
		
		this.set(C_PARENT, parent);
	}
	
	/**
	 * Force set the parent of the DataModel. 
	 * @param parent
	 */
	public void forceSetParent(LuaValue parent) {
		if ( parent == null )
			parent = LuaValue.NIL;

		if ( parent == this )
			throw new LuaError("Instance can not be its own parent");
		
		//boolean l = this.locked;
		//boolean l2 = !this.getField("Parent").canModify();

		LuaValue oldParent = this.rawget(C_PARENT);
		if ( oldParent.eq_b(parent) )
			return;
		
		/*LuaField pField = this.getField("Parent");
		if ( pField == null )
			return;*/
		
		//pField.setLocked(false);
		//this.setLocked(false);
		this.rawset(C_PARENT, parent);
		//this.setLocked(l);
		//pField.setLocked(l2);

		this.checkSetParent(C_PARENT, oldParent, parent);
		//this.onValueUpdated(C_PARENT, parent);
		this.onKeyChange(C_PARENT, parent, oldParent);
	}
	
	/**
	 * Returns the ServerID of the DataModel. This Identifier is used when sending packets across the network to reference the DataModel. Each DataModel should be given a unique ID.
	 * @return
	 */
	public Long getSID() {
		return this.get(C_SID).tolong();
	}
	
	/**
	 * The change-event for the DataModel. Whenever a field in this DataModel changes, the event will fire.
	 * @return
	 */
	public LuaEvent changedEvent() {
		LuaValue temp = this.rawget(C_CHANGED);
		return temp.isnil()?null:(LuaEvent)temp;
	}
	
	/**
	 * The destroy-event for the DataModel. When the datamodel is destroyed, this event will fire.
	 * @return
	 */
	public LuaEvent destroyedEvent() {
		LuaValue temp = this.rawget(C_DESTROYED);
		return temp.isnil()?null:(LuaEvent)temp;
	}
	
	/**
	 * The child-added event for the DataModel. When a child is added directly to this DataModel, the event will fire.
	 * @return
	 */
	public LuaEvent childAddedEvent() {
		LuaValue temp = this.rawget(C_CHILDADDED);
		return temp.isnil()?null:(LuaEvent)temp;
	}
	
	/**
	 * The child-removed event for the DataModel. When a child is removed directly from this DataModel, the event will fire.
	 * @return
	 */
	public LuaEvent childRemovedEvent() {
		LuaValue temp = this.rawget(C_CHILDREMOVED);
		return temp.isnil()?null:(LuaEvent)temp;
	}
	
	/**
	 * The descendant added event for the DataModel. DescendantAdded fires when a descendant is added to the Instance.
	 * @return
	 */
	public LuaEvent descendantAddedEvent() {
		LuaValue temp = this.rawget(C_DESCENDANTADDED);
		return temp.isnil()?null:(LuaEvent)temp;
	}

	/**
	 * The descendant removed event for the DataModel. DescendantRemoved fires immediately before the Parent of a descendant of the Instance changes such that the object is no longer a descendant of the Instance.
	 * @return
	 */
	public LuaEvent descendantRemovedEvent() {
		LuaValue temp = this.rawget(C_DESCENDANTREMOVED);
		return temp.isnil()?null:(LuaEvent)temp;
	}

	/**
	 * Sets whether this DataModel is instanceable. A non-instanceable DataModel will not be able to be created via lua's: Instance.new() method.
	 * @param instanceable
	 */
	public void setInstanceable( boolean instanceable ) {
		TYPES.get(this.get(C_CLASSNAME).toString()).instanceable = instanceable;
	}

	/**
	 * Returns whether or not this object-type is user-instancable.
	 * @return
	 */
	public boolean isInstanceable() {
		if ( this.destroyed )
			return false;
		
		return TYPES.get(this.get(C_CLASSNAME).toString()).instanceable;
	}
	
	@Override
	public void cleanup() {
		this.setLocked(false);
		
		if ( this.containsField(C_PARENT) ) {
			this.getField(C_PARENT).setLocked(false);
			this.set(C_PARENT, LuaValue.NIL);
		}
		
		this.children.clear();
		this.descendents.clear();
		this.descendentsList.clear();
		this.destroyed = true;
		this.propertySubscribers.clear();
		this.cachedChildrenOfClass.clear();
		
		super.cleanup();
	}
}
