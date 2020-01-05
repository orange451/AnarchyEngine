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

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.gl.light.Light;
import engine.gl.light.SpotLightInternal;
import engine.lua.lib.EnumType;
import engine.lua.type.NumberClamp;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class SpotLight extends LightBase<SpotLightInternal> implements TreeViewable {

	private static final LuaValue C_INNERFOVSCALE = LuaValue.valueOf("InnerFOVScale");
	private static final LuaValue C_OUTERFOV = LuaValue.valueOf("OuterFOV");
	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");
	private static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");
	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");

	public SpotLight() {
		super("SpotLight");
		
		this.defineField(C_DIRECTION.toString(), new Vector3(1, 1, -1), false);
		
		this.defineField(C_RADIUS.toString(), LuaValue.valueOf(8), false);
		this.getField(C_RADIUS).setClamp(new NumberClampPreferred(0, 1024, 0, 64));
		
		this.defineField(C_OUTERFOV.toString(), LuaValue.valueOf(80), false);
		this.getField(C_OUTERFOV).setClamp(new NumberClampPreferred(0, 180, 0, 120));
		
		this.defineField(C_INNERFOVSCALE.toString(), LuaValue.valueOf(0.1), false);
		this.getField(C_INNERFOVSCALE).setClamp(new NumberClamp(0, 1));

		this.defineField(C_SHADOWMAPSIZE.toString(), LuaValue.valueOf(1024), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));

		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_POSITION) ) {
					light.setPosition(((Vector3)value).toJoml());
				} else if ( key.eq_b(C_OUTERFOV) ) {
					light.setOuterFOV(value.tofloat());
				} else if ( key.eq_b(C_INNERFOVSCALE) ) {
					light.innerFOV = value.tofloat();
				} else if ( key.eq_b(C_RADIUS) ) {
					light.radius = value.tofloat();
				} else if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				} else if (key.eq_b(C_DIRECTION)) {
					light.direction = ((Vector3) value).toJoml();
				} else if (key.eq_b(C_SHADOWS)) {
					light.shadows = value.toboolean();
				} else if(key.eq_b(C_SHADOWMAPSIZE)) {
					light.setSize(value.toint());
				}
			}
		});
	}

	public void setOuterFOV(float fov) {
		this.set(C_OUTERFOV, LuaValue.valueOf(fov));
	}

	public void setRadius(float radius) {
		this.set(C_RADIUS, LuaValue.valueOf(radius));
	}

	@Override
	public Light getLightInternal() {
		return light;
	}

	@Override
	protected void destroyLight(IPipeline pipeline) {
		InternalRenderThread.runLater(()->{
			if ( light == null || pipeline == null )
				return;

			pipeline.getSpotLightHandler().removeLight(light);
			light = null;
			this.pipeline = null;

			System.out.println("Destroyed light");
		});
	}

	@Override
	protected void makeLight(IPipeline pipeline) {		
		// Add it to pipeline
		InternalRenderThread.runLater(()->{
			this.pipeline = pipeline;
			
			if ( pipeline == null )
				return;

			if ( light != null )
				return;

			// Create light
			Vector3f pos = ((Vector3)this.get("Position")).toJoml();
			float radius = this.get(C_RADIUS).tofloat();
			float outerFOV = this.get(C_OUTERFOV).tofloat();
			float innerFOV = this.get(C_INNERFOVSCALE).tofloat();
			float intensity = this.get(C_INTENSITY).tofloat();
			Vector3f direction = ((Vector3) this.get(C_DIRECTION)).toJoml();
			light = new SpotLightInternal(direction, pos, outerFOV, innerFOV, radius, intensity);

			// Color it
			Color color = ((Color3)this.get("Color")).toColor();
			light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );

			light.visible = this.get(C_VISIBLE).toboolean();

			light.shadowResolution = this.get(C_SHADOWMAPSIZE).toint();

			if ( pipeline.getSpotLightHandler() != null )
				pipeline.getSpotLightHandler().addLight(light);
		});
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_light_spot;
	}
}
