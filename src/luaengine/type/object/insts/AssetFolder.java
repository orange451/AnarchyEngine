package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;

public class AssetFolder extends Instance implements TreeViewable {

	public AssetFolder() {
		super("AssetFolder");
		
		this.setLocked(true);
		this.setInstanceable(false);
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
		return Icons.icon_asset_folder;
	}
}
