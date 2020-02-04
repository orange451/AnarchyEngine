/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.ClientEngine;
import engine.Game;
import engine.gl.RenderingSettings;
import engine.lua.lib.EnumType;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class RenderSettings extends Instance implements TreeViewable {

	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");
	private static final LuaValue C_TEXTURESIZE = LuaValue.valueOf("TextureSize");
	private static final LuaValue C_ANTIALIASING = LuaValue.valueOf("AntiAliasing");
	private static final LuaValue C_SHADOWSENABLED = LuaValue.valueOf("ShadowsEnabled");
	private static final LuaValue C_MOTIONBLUR = LuaValue.valueOf("MotionBlur");
	private static final LuaValue C_DEPTHOFFIELD = LuaValue.valueOf("DepthOfField");
	private static final LuaValue C_SSRENABLED = LuaValue.valueOf("Reflections");
	private static final LuaValue C_AOENABLED = LuaValue.valueOf("AmbientOcclusion");
	
	private RenderingSettings settings;
	
	public RenderSettings() {
		super("RenderSettings");

		this.defineField(C_SHADOWSENABLED, LuaValue.valueOf(true), false);
		
		this.defineField(C_SHADOWMAPSIZE, LuaValue.valueOf(1024), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));
		
		this.defineField(C_TEXTURESIZE, LuaValue.valueOf(1024), false);
		this.getField(C_TEXTURESIZE).setEnum(new EnumType("TextureSize"));
		
		this.defineField(C_ANTIALIASING, LuaValue.valueOf("FXAA"), false);
		this.getField(C_ANTIALIASING).setEnum(new EnumType("AntiAliasingType"));

		this.defineField(C_MOTIONBLUR, LuaValue.valueOf(false), false);
		this.defineField(C_DEPTHOFFIELD, LuaValue.valueOf(false), false);
		this.defineField(C_SSRENABLED, LuaValue.valueOf(false), false);
		this.defineField(C_AOENABLED, LuaValue.valueOf(false), false);

		this.setInstanceable(false);
		this.setLocked(true);
		
		this.createdEvent().connect((args) -> {
			if ( destroyed )
				return;
			
			Instance ss = Game.core();
			if ( !this.getParent().eq_b(ss) )
				this.forceSetParent(ss);
			this.settings = ClientEngine.renderThread.getPipeline().getRenderSettings();
			this.settings.shadowsEnabled = this.getShadowsEnabled();
			this.settings.ssrEnabled = this.getSSREnabled();
			this.settings.ambientOcclusionEnabled = this.getAOEnabled();
			this.settings.depthOfFieldEnabled = this.getDepthOfFieldEnabled();
			this.settings.motionBlurEnabled = this.getMotionBlurEnabled();
			if (this.get(C_ANTIALIASING).eq_b(LuaValue.valueOf("FXAA"))) {
				settings.fxaaEnabled = true;
				settings.taaEnabled = false;
			} else if (this.get(C_ANTIALIASING).eq_b(LuaValue.valueOf("TAA"))) {
				settings.fxaaEnabled = true;
				settings.taaEnabled = true;
			} else {
				settings.fxaaEnabled = false;
				settings.taaEnabled = false;
			}
		});
	}
	
	public boolean getShadowsEnabled() {
		return this.get(C_SHADOWSENABLED).toboolean();
	}
	
	public int getShadowMapSize() {
		return this.get(C_SHADOWMAPSIZE).toint();
	}
	
	public int getTextureSize() {
		return this.get(C_TEXTURESIZE).toint();
	}
	
	public boolean getMotionBlurEnabled() {
		return this.get(C_MOTIONBLUR).toboolean();
	}
	
	public boolean getDepthOfFieldEnabled() {
		return this.get(C_DEPTHOFFIELD).toboolean();
	}
	
	public boolean getSSREnabled() {
		return this.get(C_SSRENABLED).toboolean();
	}

	public boolean getAOEnabled() {
		return this.get(C_AOENABLED).toboolean();
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( settings == null )
			return value;
		
		if ( key.eq_b(C_SHADOWSENABLED) )
			settings.shadowsEnabled = value.toboolean();
		if ( key.eq_b(C_SSRENABLED) )
			settings.ssrEnabled = value.toboolean();
		if ( key.eq_b(C_AOENABLED) )
			settings.ambientOcclusionEnabled = value.toboolean();
		if ( key.eq_b(C_DEPTHOFFIELD) )
			settings.depthOfFieldEnabled = value.toboolean();
		if ( key.eq_b(C_MOTIONBLUR) )
			settings.motionBlurEnabled = value.toboolean();

		if ( key.eq_b(C_ANTIALIASING) ) {
			if ( value.eq_b(LuaValue.valueOf("FXAA")) ) {
				settings.fxaaEnabled = true;
				settings.taaEnabled = false;
			} else if ( value.eq_b(LuaValue.valueOf("TAA")) ) {
				settings.fxaaEnabled = true;
				settings.taaEnabled = true;
			} else {
				settings.fxaaEnabled = false;
				settings.taaEnabled = false;
			}
		}
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
