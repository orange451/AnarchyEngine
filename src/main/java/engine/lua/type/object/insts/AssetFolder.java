/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

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
		String name = this.getName();
		if ( name.contains("Mesh"))
			return Icons.icon_asset_folder_mesh;
		if ( name.contains("Material"))
			return Icons.icon_asset_folder_material;
		if ( name.contains("Texture"))
			return Icons.icon_asset_folder_texture;
		if ( name.contains("Prefab"))
			return Icons.icon_asset_folder_prefab;
		
		return Icons.icon_asset_folder;
	}
}
