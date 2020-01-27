/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.light;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.glv2.v2.lights.Light;
import engine.glv2.v2.lights.PointLightInternal;
import engine.lua.lib.EnumType;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class AreaLight extends LightBase<AreaLightInternal> implements TreeViewable {

	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");
	private static final LuaValue C_WORLDMATRIX = LuaValue.valueOf("WorldMatrix");
	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");

	public AreaLight() {
		super("AreaLight");
		
		this.defineField(C_RADIUS.toString(), LuaValue.valueOf(8), false);
		this.getField(C_RADIUS).setClamp(new NumberClampPreferred(0, 1024, 0, 64));
		
		this.defineField(C_WORLDMATRIX.toString(), new Matrix4(), false);

		this.defineField(C_SHADOWMAPSIZE.toString(), LuaValue.valueOf(512), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));
		
		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_POSITION) ) {
					light.setPosition(((Vector3)value).toJoml());
				} else if ( key.eq_b(C_RADIUS) ) {
					light.radius = value.tofloat();
				} else if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				} else if (key.eq_b(C_SHADOWS)) {
					light.shadows = value.toboolean();
				} else if (key.eq_b(C_SHADOWMAPSIZE)) {
					light.setSize(value.toint());
				}
			}
		});
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
			
			pipeline.getAreaLightHandler().removeLight(light);
			this.light = null;
			this.pipeline = null;

			System.out.println("Destroyed light");
		});
	}

	@Override
	protected void makeLight(IPipeline pipeline) {		
		// Add it to pipeline
		InternalRenderThread.runLater(()->{
			
			System.out.println("Creating arealight! " + pipeline + " / " + light);
			if ( pipeline == null )
				return;
			
			if ( light != null )
				return;
			
			this.pipeline = pipeline;
			
			// Get some params
			Vector3f pos = ((Vector3)this.get("Position")).toJoml();
			float radius = this.get(C_RADIUS).tofloat();
			float intensity = this.get("Intensity").tofloat();
			Color color = ((Color3)this.get("Color")).toColor();
			Matrix4f mat = this.getWorldMatrix().getInternal();
			
			// TODO compute 4 points from world Matrix
			Vector3f p1 = new Vector3f(1, 1, 0);
			Vector3f p2 = new Vector3f(-1, 1, 0);
			Vector3f p3 = new Vector3f(-1, -1, 0);
			Vector3f p4 = new Vector3f(-1, 1, 0);
			
			// Create internal light!
			light = new AreaLightInternal(pos, radius, intensity);
			light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
			light.visible = this.get(C_VISIBLE).toboolean();
			light.shadowResolution = this.get(C_SHADOWMAPSIZE).toint();
			light.shadows = this.get(C_SHADOWS).toboolean();

			pipeline.getAreaLightHandler().addLight(light);
		});
	}
	
	@Override
	public Matrix4 getWorldMatrix() {
		return (Matrix4) this.get(C_WORLDMATRIX);
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_light;
	}
}
