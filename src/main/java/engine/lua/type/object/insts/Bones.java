package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Bones extends Instance implements TreeViewable {
	
	public static final LuaValue C_ROOTINVERSE = LuaValue.valueOf("RootInverse");
	
	public Bones() {
		super("Bones");
		
		this.setInstanceable(false);

		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);
		
		this.defineField(C_ROOTINVERSE.toString(), new Matrix4(), true);
		
		this.childAddedEvent().connect((args)->{
			LuaValue c = args[0];
			if ( c instanceof Bone ) {
				System.out.println("Adding bone " + c);
			}
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;	
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_animation_data;
	}
}
