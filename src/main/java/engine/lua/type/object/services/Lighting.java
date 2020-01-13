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

import engine.AnarchyEngineClient;
import engine.InternalGameThread;
import engine.gl.IPipeline;
import engine.gl.LegacyPipeline;
import engine.lua.type.LuaConnection;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.tasks.TaskManager;
import ide.layout.windows.icons.Icons;

public class Lighting extends Service implements TreeViewable {

	private static final LuaValue C_AMBIENT = LuaValue.valueOf("Ambient");
	private static final LuaValue C_EXPOSURE = LuaValue.valueOf("Exposure");
	private static final LuaValue C_SATURATION = LuaValue.valueOf("Saturation");
	private static final LuaValue C_GAMMA = LuaValue.valueOf("Gamma");
	private static final LuaValue C_SKYBOX = LuaValue.valueOf("Skybox");

	public Lighting() {
		super("Lighting");
		
		this.defineField(C_AMBIENT.toString(), Color3.newInstance(24, 24, 24), false);
		
		this.defineField(C_SKYBOX.toString(), LuaValue.NIL, false);
		
		this.defineField(C_EXPOSURE.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_EXPOSURE).setClamp(new NumberClampPreferred(0, 25, 0, 2));
		
		this.defineField(C_SATURATION.toString(), LuaValue.valueOf(1.2f), false);
		this.getField(C_SATURATION).setClamp(new NumberClampPreferred(0, 100, 0, 2));
		
		this.defineField(C_GAMMA.toString(), LuaValue.valueOf(2.2f), false);
		this.getField(C_GAMMA).setClamp(new NumberClampPreferred(0, 10, 0, 4));
	}
	
	public Color3 getAmbient() {
		return (Color3) this.get(C_AMBIENT);
	}
	
	public void setAmbient(Color3 color) {
		this.set(C_AMBIENT, color);
	}
	
	public float getExposure() {
		return this.get(C_EXPOSURE).tofloat();
	}
	
	public void setExposure(float value) {
		this.set(C_EXPOSURE, LuaValue.valueOf(value));
	}
	
	public float getSaturation() {
		return this.get(C_SATURATION).tofloat();
	}
	
	public void setSaturation(float value) {
		this.set(C_SATURATION, LuaValue.valueOf(value));
	}
	
	public float getGamma() {
		return this.get(C_GAMMA).tofloat();
	}
	
	public void setGamma(float value) {
		this.set(C_GAMMA, LuaValue.valueOf(value));
	}

	private LuaConnection skyboxDestroyed;
	private LuaConnection skyboxChanged;
	private LuaConnection skyboxImageChanged;
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_SKYBOX) ) {
			return onSetSkybox(value);
		}
		
		return value;
	}
	
	private LuaValue onSetSkybox(LuaValue value) {
		IPipeline pp = AnarchyEngineClient.pipeline;
		
		if ( value.isnil() ) {
			if (AnarchyEngineClient.pipeline instanceof LegacyPipeline) {
			} else {
				pp.setDyamicSkybox(null);
				pp.setStaticSkybox(null);
			}
			
			if ( skyboxDestroyed != null )
				skyboxDestroyed.disconnect();
			
			if ( skyboxChanged != null )
				skyboxChanged.disconnect();
			
			if ( skyboxImageChanged != null )
				skyboxImageChanged.disconnect();

			skyboxDestroyed = null;
			skyboxChanged = null;
			skyboxImageChanged = null;
			return value;
		} else {
			if ( value instanceof DynamicSkybox ) {
				DynamicSkybox skybox = (DynamicSkybox)value;
				pp.setDyamicSkybox(skybox);
				pp.setStaticSkybox(null);
				
				if ( skyboxDestroyed != null )
					skyboxDestroyed.disconnect();
				
				skyboxDestroyed = skybox.destroyedEvent().connect((args)->{
					pp.setDyamicSkybox(null);
					
					if ( skyboxChanged != null ) {
						skyboxChanged.disconnect();
						skyboxChanged = null;
					}
				});
			} else if ( value instanceof Skybox) {
				Skybox skybox = (Skybox)value;
				pp.setStaticSkybox(skybox);
				pp.setDyamicSkybox(null);

				if ( skyboxChanged != null )
					skyboxChanged.disconnect();
				
				skyboxChanged = skybox.changedEvent().connect((args)->{
					LuaValue key = args[0];
					LuaValue val = args[1];
					
					if ( val.isnil() ) {
						pp.setStaticSkybox(null);
					} else {
						// User has changed the image object-reference of the skybox after it's attached
						if ( key.eq_b(LuaValue.valueOf("Image")) ) {
							if ( skyboxImageChanged != null )
								skyboxImageChanged.disconnect();
							
							// Rebind image change event on NEW image
							skyboxImageChanged = skybox.getImage().textureLoadedEvent().connect((args1)->{
								pp.reloadStaticSkybox();
							});
							if (skybox.getImage().hasLoaded()) {
								TaskManager.addTaskRenderThread(() -> pp.reloadStaticSkybox());
							}
						}
					}
				});
				
				if ( skyboxImageChanged != null )
					skyboxImageChanged.disconnect();
				
				// Bind image change event, for when user changes the images URL and new image is loaded.
				InternalGameThread.runLater(()->{
					if(skybox.getImage() != null) {
						if(skybox.getImage().hasLoaded()) {
							TaskManager.addTaskRenderThread(() -> pp.reloadStaticSkybox());
						}
						skyboxImageChanged = skybox.getImage().textureLoadedEvent().connect((args)->{
							pp.reloadStaticSkybox();
						});
					}
				});
				
				if ( skyboxDestroyed != null )
					skyboxDestroyed.disconnect();
				
				// Event for when a skybox is destroyed
				skyboxDestroyed = skybox.destroyedEvent().connect((args)->{
					pp.setStaticSkybox(null);
					
					if ( skyboxChanged != null ) {
						skyboxChanged.disconnect();
						skyboxChanged = null;
					}
				});
			} else {
				LuaValue.error("Skybox field must be of type Skybox");
				return null;
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
		return Icons.icon_light;
	}
	
	public Skybox getSkybox() {
		LuaValue ret = this.get(C_SKYBOX);
		return ret.isnil()?null:(Skybox)ret;
	}

	public void setSkybox(Instance skybox) {
		if ( skybox == null )
			this.set(C_SKYBOX, LuaValue.NIL);
		else
			this.set(C_SKYBOX, skybox);
	}
}
