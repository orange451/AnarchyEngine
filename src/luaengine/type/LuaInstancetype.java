package luaengine.type;

import java.util.HashMap;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import luaengine.LuaEngine;
import luaengine.type.object.Instance;

public abstract class LuaInstancetype extends LuaDatatype {
	protected static HashMap<String,LuaInstancetypeData> TYPES = new HashMap<String,LuaInstancetypeData>();
	class LuaInstancetypeData {
		Class<?> instanceableClass;
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
							return (LuaInstancetype) c.instanceableClass.newInstance();
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

	public LuaInstancetype(String name) {
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
		this.defineField("Archivable",		LuaValue.valueOf(true), true);

		this.rawset("Changed",		new LuaEvent());
		this.rawset("ChildAdded",	new LuaEvent());
		this.rawset("ChildRemoved",	new LuaEvent());
		this.rawset("DescendantAdded",		new LuaEvent());
		this.rawset("DescendantRemoved",	new LuaEvent());
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
		return (LuaEvent)this.rawget("Changed");
	}
	
	public LuaEvent childAddedEvent() {
		return (LuaEvent)this.rawget("ChildAdded");
	}
	
	public LuaEvent childRemovedEvent() {
		return (LuaEvent)this.rawget("ChildRemoved");
	}
	
	public LuaEvent descendantAddedEvent() {
		return (LuaEvent)this.rawget("DescendantAdded");
	}
	
	public LuaEvent descendantRemovedEvent() {
		return (LuaEvent)this.rawget("DescendantRemoved");
	}
	
	public static LuaInstancetype instance(String type) {
		if ( type == null )
			return null;
		
		LuaInstancetypeData c = TYPES.get(type);
		try {
			return (LuaInstancetype) c.instanceableClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public void setInstanceable( boolean instanceable ) {
		TYPES.get(this.get("ClassName").toString()).instanceable = instanceable;
	}

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
