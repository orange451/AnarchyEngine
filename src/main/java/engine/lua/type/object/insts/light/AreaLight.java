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

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.IPipeline;
import engine.gl.lights.AreaLightInternal;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class AreaLight extends LightBase<AreaLightInternal> implements TreeViewable {

	private static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");
	private static final LuaValue C_SIZEX = LuaValue.valueOf("SizeX");
	private static final LuaValue C_SIZEY = LuaValue.valueOf("SizeY");

	public AreaLight() {
		super("AreaLight");

		this.getField(C_SHADOWS).setLocked(true);

		this.getField(C_INTENSITY).setClamp(new NumberClampPreferred(0, 100, 0, 100));

		this.defineField(C_DIRECTION.toString(), new Vector3(1, 1, 1), false);

		this.defineField(C_SIZEX.toString(), LuaValue.valueOf(1), false);
		this.getField(C_SIZEX).setClamp(new NumberClampPreferred(0, 100, 0, 100));

		this.defineField(C_SIZEY.toString(), LuaValue.valueOf(1), false);
		this.getField(C_SIZEY).setClamp(new NumberClampPreferred(0, 100, 0, 100));

		this.changedEvent().connect((args) -> {
			LuaValue key = args[0];
			LuaValue value = args[1];

			if (light != null) {
				if (key.eq_b(C_POSITION)) {
					light.setPosition(((Vector3) value).toJoml());
				} else if (key.eq_b(C_INTENSITY)) {
					light.intensity = value.tofloat();
				} else if (key.eq_b(C_COLOR)) {
					Color color = ((Color3) value).toColor();
					light.color = new Vector3f(Math.max(color.getRed(), 1) / 255f, Math.max(color.getGreen(), 1) / 255f,
							Math.max(color.getBlue(), 1) / 255f);
				} else if (key.eq_b(C_DIRECTION)) {
					light.direction = ((Vector3) value).toJoml();
				} else if (key.eq_b(C_SIZEX)) {
					light.sizeX = value.tofloat();
				} else if (key.eq_b(C_SIZEY)) {
					light.sizeY = value.tofloat();
				}
			}
		});
	}

	@Override
	protected void destroyLight(IPipeline pipeline) {
		if (light == null || pipeline == null)
			return;

		pipeline.getAreaLightHandler().removeLight(light);
		this.light = null;
		this.pipeline = null;

		System.out.println("Destroyed light");
	}

	@Override
	protected void makeLight(IPipeline pipeline) {
		// Add it to pipeline
		System.out.println("Creating arealight! " + pipeline + " / " + light);
		if (pipeline == null)
			return;

		if (light != null)
			return;

		this.pipeline = pipeline;

		// Get some params
		Vector3f pos = ((Vector3) this.get(C_POSITION)).toJoml();
		Vector3f direction = ((Vector3) this.get(C_DIRECTION)).toJoml();
		float intensity = this.get(C_INTENSITY).tofloat();
		Color color = ((Color3) this.get(C_COLOR)).toColor();
		float sizeX = this.get(C_SIZEX).tofloat();
		float sizeY = this.get(C_SIZEY).tofloat();

		// Create internal light!
		light = new AreaLightInternal(direction, pos, intensity);
		light.color = new Vector3f(Math.max(color.getRed(), 1) / 255f, Math.max(color.getGreen(), 1) / 255f,
				Math.max(color.getBlue(), 1) / 255f);
		light.visible = this.get(C_VISIBLE).toboolean();
		light.sizeX = sizeX;
		light.sizeY = sizeY;

		pipeline.getAreaLightHandler().addLight(light);
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_light;
	}
}
