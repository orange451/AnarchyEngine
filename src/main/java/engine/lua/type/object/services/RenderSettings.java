package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.lib.EnumType;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class RenderSettings extends Instance implements TreeViewable {

	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");
	
	public RenderSettings() {
		super("RenderSettings");
		
		this.defineField(C_SHADOWMAPSIZE.toString(), LuaValue.valueOf(1024), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));

		this.setLocked(true);
		this.setInstanceable(false);
		
		// Make sure it's in CORE
		InternalGameThread.runLater(()->{
			if ( destroyed )
				return;
			
			Instance ss = Game.core();
			if ( !this.getParent().eq_b(ss) )
				this.forceSetParent(ss);
		});
	}
	
	public int getShadowMapSize() {
		return this.get(C_SHADOWMAPSIZE).toint();
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
	public Icons getIcon() {
		return Icons.icon_properties;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
	}
}
