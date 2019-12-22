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

import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Skybox extends Instance implements TreeViewable {

	private static final LuaValue C_BRIGHTNESS = LuaValue.valueOf("Brightness");
	private static final LuaValue C_POWER = LuaValue.valueOf("Power");
	private static final LuaValue C_IMAGE = LuaValue.valueOf("Image");

	public Skybox() {
		super("Skybox");
		
		this.defineField(C_IMAGE.toString(), LuaValue.NIL, false);
		
		this.defineField(C_BRIGHTNESS.toString(), LuaValue.valueOf(2), false);
		this.getField(C_BRIGHTNESS).setClamp(new NumberClampPreferred(0, 10, 0, 5));
		
		this.defineField(C_POWER.toString(), LuaValue.valueOf(2), false);
		this.getField(C_POWER).setClamp(new NumberClampPreferred(0, 10, 0, 5));
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
		return Icons.icon_skybox;
	}
	
	public void setBrightness( float brightness ) {
		this.set(C_BRIGHTNESS, LuaValue.valueOf(brightness));
	}
	
	public void setPower( float power ) {
		this.set(C_POWER, LuaValue.valueOf(power));
	}
	
	public void setImage( Texture texture ) {
		if ( texture == null )
			this.set(C_IMAGE, LuaValue.NIL);
		else
			this.set(C_IMAGE, texture);
	}

	public Texture getImage() {
		LuaValue ret = this.rawget(C_IMAGE);
		return ret.isnil()?null:(Texture)ret;
	}

	public float getPower() {
		return this.rawget(C_POWER).tofloat();
	}

	public float getBrightness() {
		return this.rawget(C_BRIGHTNESS).tofloat();
	}
}
