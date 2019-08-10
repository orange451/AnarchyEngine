package engine.lua.type;

import java.util.ArrayList;
import java.util.HashMap;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import engine.lua.LuaEngine;
import engine.lua.type.object.Instance;

public abstract class DataModel extends LuaDatatype {
	protected static HashMap<String,LuaInstancetypeData> TYPES = new HashMap<String,LuaInstancetypeData>();

	private static final LuaValue C_CHANGED = LuaValue.valueOf("Changed");
	private static final LuaValue C_DESTROYED = LuaValue.valueOf("Destroyed");
	private static final LuaValue C_CHILDADDED = LuaValue.valueOf("ChildAdded");
	private static final LuaValue C_CHILDREMOVED = LuaValue.valueOf("ChildRemoved");
	private static final LuaValue C_DESCENDANTADDED = LuaValue.valueOf("DescendantAdded");
	private static final LuaValue C_DESCENDANTREMOVED = LuaValue.valueOf("DescendantRemoved");
	
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

		this.defineField("Name",			LuaValue.valueOf(name), false);
		this.defineField("ClassName",		LuaValue.valueOf(name), true);
		this.defineField("SID", 			LuaValue.valueOf(-1),   true);
		this.defineField("Parent",			LuaValue.NIL,			false);
		this.defineField("Archivable",		LuaValue.valueOf(true), false);

		this.rawset(C_CHANGED,		new LuaEvent());
		this.rawset(C_DESTROYED,	new LuaEvent());
		this.rawset(C_CHILDADDED,	new LuaEvent());
		this.rawset(C_CHILDREMOVED,	new LuaEvent());
		this.rawset(C_DESCENDANTADDED,		new LuaEvent());
		this.rawset(C_DESCENDANTREMOVED,	new LuaEvent());
	}
	
	public boolean isArhivable() {
		return this.get("Archivable").toboolean();
	}
	
	public void setArchivable(boolean archivable) {
		this.set("Archivable", LuaValue.valueOf(archivable));
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

	public Long getSID() {
		return this.get("SID").tolong();
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
		TYPES.get(this.get("ClassName").toString()).instanceable = instanceable;
	}

	/**
	 * Returns whether or not this object-type is user-instancable.
	 * @return
	 */
	public boolean isInstanceable() {
		return TYPES.get(this.get("ClassName").toString()).instanceable;
	}
	
	@Override
	public void cleanup() {
		this.setLocked(false);
		
		if ( this.hasKey("Parent") ) {
			this.getField("Parent").setLocked(false);
			this.set("Parent", LuaValue.NIL);
		}
		
		super.cleanup();
	}
}
