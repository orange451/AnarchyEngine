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

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.ClientEngine;
import engine.Game;
import engine.gl.IPipeline;
import engine.gl.lights.Light;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.services.Lighting;
import engine.observer.RenderableWorld;
import engine.util.AABBUtil;
import engine.util.Pair;

public abstract class LightBase<T extends Light> extends Instance implements Positionable {

	protected static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	protected static final LuaValue C_INTENSITY = LuaValue.valueOf("Intensity");
	protected static final LuaValue C_COLOR = LuaValue.valueOf("Color");
	protected static final LuaValue C_SHADOWS = LuaValue.valueOf("Shadows");
	protected static final LuaValue C_VISIBLE = LuaValue.valueOf("Visible");
	protected IPipeline pipeline;

	protected T light;

	public LightBase(String typename) {
		super(typename);

		this.defineField(C_POSITION.toString(), new Vector3(), false);
		
		this.defineField(C_INTENSITY.toString(), LuaValue.valueOf(1), false);
		this.getField(C_INTENSITY).setClamp(new NumberClampPreferred(0, 100, 0, 8));
		
		this.defineField(C_COLOR.toString(), Color3.white(), false);
		this.defineField(C_SHADOWS.toString(), LuaValue.valueOf(true), false);
		this.defineField(C_VISIBLE.toString(), LuaValue.valueOf(true), false);
		
		this.changedEvent().connect((args)->{
			if ( args[0].eq_b(C_VISIBLE) ) {
				Light l = this.getLightInternal();
				if ( l == null )
					return;
				
				l.visible = args[1].toboolean();
			}

			if ( args[0].eq_b(C_PARENT) ) {
				onParentChange();
			}
		});
	}
	
	protected abstract void destroyLight(IPipeline pipeline);
	protected abstract void makeLight(IPipeline pipeline);
	
	@Override
	public void onDestroy() {
		destroyLight(pipeline);
	}
	
	private void onParentChange() {
		LuaValue t = this.getParent();
		if (t.isnil()) {
			destroyLight(pipeline);
			return;
		}

		// Search for renderable world
		while (t != null && !t.isnil()) {
			if (t instanceof RenderableWorld || t instanceof Lighting) {
				IPipeline tempPipeline = null;

				if ( t instanceof Lighting )
					t = Game.workspace();
				
				if ( t instanceof RenderableWorld )
					tempPipeline = ClientEngine.renderThread.getPipeline();

				if (tempPipeline == null)
					break;

				// Destroy old light
				if (pipeline != null)
					destroyLight(pipeline);

				// Make new light. Return means we can live for another day!
				makeLight(tempPipeline);
				return;
			}

			// Navigate up tree
			LuaValue temp = t;
			t = ((Instance) t).getParent();
			if (t == temp)
				t = null;
		}

		// Cant make light, can't destroy light. SO NO LIGHT!
		destroyLight(pipeline);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public T getLightInternal() {
		return light;
	}
	
	public void setVisible( boolean visible ) {
		this.set(C_VISIBLE, LuaValue.valueOf(visible));
	}
	
	public boolean isVisible() {
		return this.get(C_VISIBLE).toboolean();
	}

	public void setIntensity(float intensity) {
		this.set(C_INTENSITY, LuaValue.valueOf(intensity));
	}
	
	public float getIntensity() {
		return this.get(C_INTENSITY).tofloat();
	}

	public void setPosition(int x, int y, int z) {
		this.set(C_POSITION, new Vector3(x, y, z));
	}
	
	public void setPosition(Vector3 position) {
		if ( !this.getField(C_POSITION).canModify() )
			return;
		this.set(C_POSITION, position.clone());
	}
	
	@Override
	public Vector3 getPosition() {
		return (Vector3) this.get(C_POSITION);
	}
	
	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		return AABBUtil.newAABB(new Vector3f(), new Vector3f());
	}
	
	@Override
	public Matrix4 getWorldMatrix() {
		return new Matrix4(getPosition());
	}
}
