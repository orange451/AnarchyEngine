package engine.lua.type.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaValue;

import engine.lua.type.LuaConnection;
import engine.lua.type.object.insts.Weld;

public class WeldedStructure {
	protected final static LuaValue C_INSTANCE_0 = LuaValue.valueOf("Instance0");
	protected final static LuaValue C_INSTANCE_1 = LuaValue.valueOf("Instance1");
	
	private Instance rootInstance;
	private List<Weld> welds;
	private Map<Weld, LuaConnection> weldChange;
	private Map<Weld, List<Instance>> weldedInstances;
	private Map<Instance, List<Weld>> instanceWelds;
	
	public WeldedStructure() {
		this.welds = new ArrayList<>();
		this.weldChange = new HashMap<>();
		this.weldedInstances = new HashMap<>();
		this.instanceWelds = new HashMap<>();
		
	}
	
	public Instance getRootInstance() {
		return this.rootInstance;
	}
	
	public void setRootInstance(Instance instance) {
		this.rootInstance = instance;
	}
	
	private boolean checkCanWeldInstance(Instance instance) {
		return false;
	}
	
	private void testAddWeldInstance_internal(Weld weld, Instance newWeld, Instance oldWeld) {
		if ( newWeld != null && oldWeld != null && newWeld.eq_b(oldWeld) )
			return;
		
		final List<Instance> fWelds = weldedInstances.get(weld);

		while ( oldWeld != null && fWelds.contains(oldWeld) )
			fWelds.remove(oldWeld);
		
		if ( newWeld != null && !fWelds.contains(newWeld) )
			fWelds.add(newWeld);
	}
	
	public void addWeld(Weld weld) {
		List<Instance> welds = weldedInstances.get(weld);
		if ( welds != null )
			return;
		
		welds = new ArrayList<Instance>();
		welds.add(weld.getInstance0());
		welds.add(weld.getInstance1());
		
		LuaConnection connection = weld.changedEvent().connect((args)->{
			if ( args[0].eq_b(C_INSTANCE_0) || args[0].eq_b(C_INSTANCE_1) ) {
				Instance newWeld = args[1].isnil() ? null : (Instance) args[1];
				Instance oldWeld = args[2].isnil() ? null : (Instance) args[2];
				
				testAddWeldInstance_internal(weld, newWeld, oldWeld);
			}
		});
		
		//testAddWeldInstance_internal();
		
		welds.add(weld);
		weldChange.put(weld, connection);
		weldedInstances.put(weld, welds);
	}

	public void removeWeld(Weld weld) {
		List<Instance> welds = weldedInstances.get(weld);
		if ( welds == null )
			return;
		
		welds.clear();
		welds.remove(weld);
		weldChange.get(weld).disconnect();
		weldedInstances.remove(weld);
	}
}
