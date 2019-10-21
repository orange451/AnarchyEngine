package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.IPipeline;
import engine.gl.Pipeline;
import engine.gl.ibl.SkySphereIBL;
import engine.io.Image;
import engine.lua.lib.EnumType;
import engine.lua.type.LuaConnection;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.lua.type.object.insts.Texture;
import ide.layout.windows.icons.Icons;

public class Lighting extends Service implements TreeViewable {

	private static final LuaValue C_AMBIENT = LuaValue.valueOf("Ambient");
	private static final LuaValue C_EXPOSURE = LuaValue.valueOf("Exposure");
	private static final LuaValue C_SATURATION = LuaValue.valueOf("Saturation");
	private static final LuaValue C_GAMMA = LuaValue.valueOf("Gamma");
	private static final LuaValue C_SKYBOX = LuaValue.valueOf("Skybox");
	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");

	public Lighting() {
		super("Lighting");
		
		this.defineField(C_AMBIENT.toString(), Color3.newInstance(128, 128, 128), false);
		
		this.defineField(C_SKYBOX.toString(), LuaValue.NIL, false);
		
		this.defineField(C_EXPOSURE.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_EXPOSURE).setClamp(new NumberClampPreferred(0, 25, 0, 2));
		
		this.defineField(C_SATURATION.toString(), LuaValue.valueOf(1.2f), false);
		this.getField(C_SATURATION).setClamp(new NumberClampPreferred(0, 100, 0, 2));
		
		this.defineField(C_GAMMA.toString(), LuaValue.valueOf(2.2f), false);
		this.getField(C_GAMMA).setClamp(new NumberClampPreferred(0, 10, 0, 4));
		
		this.defineField(C_SHADOWMAPSIZE.toString(), LuaValue.valueOf(1024), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));
	}
	
	public int getShadowMapSize() {
		return this.get(C_SHADOWMAPSIZE).toint();
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
		IPipeline pp = RenderableApplication.pipeline;
		
		if ( value.isnil() ) {
			if (RenderableApplication.pipeline instanceof Pipeline) {
				((Pipeline)pp).getGBuffer().getMergeProcessor().setSkybox(null);
			} else {
				pp.setDyamicSkybox(null);
				pp.setStaticSkybox(null);
			}
			
			skyboxDestroyed.disconnect();
			skyboxChanged.disconnect();
			skyboxImageChanged.disconnect();
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
						// User has changed the image of the skybox after it's attached
						if ( key.eq_b(LuaValue.valueOf("Image")) ) {
							if ( skyboxImageChanged != null )
								skyboxImageChanged.disconnect();
							
							// Rebind image change event on NEW image
							skyboxImageChanged = skybox.getImage().textureLoadedEvent().connect((args1)->{
								System.out.println("SKYBOX IMAGE HAS LOADED");
							});
							
							System.out.println("SKYBOX IMAGE HAS BEEN CHANGED BY USER");
						}
					}
				});
				
				if ( skyboxImageChanged != null )
					skyboxImageChanged.disconnect();
				
				// Bind image change event, for when user changes the images URL and new image is loaded.
				skyboxImageChanged = skybox.getImage().textureLoadedEvent().connect((args)->{
					System.out.println("SKYBOX IMAGE HAS LOADED");
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
