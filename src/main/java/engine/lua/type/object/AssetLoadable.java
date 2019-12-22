/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.FilePath;
import engine.io.FileResource;

public abstract class AssetLoadable extends Asset implements FileResource {

	protected final static LuaValue C_FILEPATH = LuaValue.valueOf("FilePath");
	
	public AssetLoadable(String type) {
		super(type);
		this.defineField("FilePath", LuaValue.valueOf(""), false);
	}
	
	public void setFilePath(String path) {
		if ( path == null ) {
			this.set(C_FILEPATH, LuaValue.NIL);
		} else {
			this.set(C_FILEPATH, LuaValue.valueOf(path));
		}
	}
	
	@Override
	public String getFilePath() {
		return this.get(C_FILEPATH).toString();
	}
	
	public String getAbsoluteFilePath() {
		return FilePath.convertToSystem(getFilePath());
	}
	
	public void resetFilePath() {
		this.set(C_FILEPATH, LuaValue.valueOf(""));
	}
	
	public static String getFileTypes() {
		return "";
	}
}
