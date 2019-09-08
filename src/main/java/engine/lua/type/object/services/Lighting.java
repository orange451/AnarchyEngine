package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.Pipeline;
import engine.gl.ibl.SkySphereIBL;
import engine.io.Image;
import engine.lua.lib.EnumType;
import engine.lua.type.LuaConnection;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
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
		
		this.defineField(C_SHADOWMAPSIZE.toString(), LuaValue.valueOf("s1024"), false);
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
	private LuaConnection textureLoaded;
	private LuaConnection textureChanged;
	private LuaConnection waitForImage;
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		
		if ( key.eq_b(C_SKYBOX) ) {
			return onSetSkybox(value);
		}
		
		return value;
	}
	
	private LuaValue onSetSkybox(LuaValue value) {
		if (RenderableApplication.pipeline instanceof Pipeline) {
			Pipeline pp = (Pipeline) RenderableApplication.pipeline;
			if ( value.isnil() ) {
				pp.getGBuffer().getMergeProcessor().setSkybox(null);
			} else {
				if ( value instanceof Skybox) {
					Skybox skybox = (Skybox)value;
					
					if ( skyboxDestroyed != null )
						skyboxDestroyed.disconnect();
					skyboxDestroyed = skybox.destroyedEvent().connect((args)->{
						pp.getGBuffer().getMergeProcessor().setSkybox(null);
					});

					if ( skyboxChanged != null )
						skyboxChanged.disconnect();
					skyboxChanged = skybox.changedEvent().connect((args)->{
						String key1 = args[0].toString();
						LuaValue value1 = args[1];

						if ( key1.equals("Image") ) {
							if ( !value1.isnil() ) {
								Texture texture = ((Texture)value1);
								attachSkybox( skybox, texture);
							} else {
								pp.getGBuffer().getMergeProcessor().setSkybox(null);
							}
						}

						if ( key1.equals("Power") ) {
							pp.getGBuffer().getMergeProcessor().getSkybox().setLightPower(value1.tofloat());
						}

						if ( key1.equals("Brightness") ) {
							pp.getGBuffer().getMergeProcessor().getSkybox().setLightMultiplier(value1.tofloat());
						}
					});

					// Initial load
					InternalRenderThread.runLater(()->{
						attachSkybox(skybox, skybox.getImage());
					});
				} else {
					LuaValue.error("Skybox field must be of type Skybox");
					return null;
				}
			}
		}
		return value;
	}

	private void attachSkybox(Skybox skybox, Texture texture) {
		if ( textureLoaded != null )
			textureLoaded.disconnect();
		if ( textureChanged != null )
			textureChanged.disconnect();
		if ( waitForImage != null )
			waitForImage.disconnect();
		
		if ( texture != null ) {
			textureLoaded = texture.textureLoadedEvent().connect((args2)->{
				System.out.println("Image loaded!");
				// Reload if image loads
				InternalRenderThread.runLater(()->{
					rebuildSkybox( skybox, texture );
				});
			});
			textureChanged = texture.changedEvent().connect((args)->{
				System.out.println("Texture changed... " + args[0] + " / " + args[1]);
				if ( args[0].toString().equals("FilePath") ) {
					attachSkybox(skybox, texture); // Reattach the skybox!
				}
			});
			waitForImage = Game.runService().renderSteppedEvent().connect((args)->{
				if ( skybox.getImage().getImage() != null && waitForImage != null ) {
					waitForImage.disconnect();
				}
			});
		}
		
		// Reload if image is changed
		rebuildSkybox(skybox, texture);
	}
	
	private void rebuildSkybox(Skybox skybox, Texture texture) {
		if ( texture == null ) {
			return;
		}
		
		Image internalTexture = texture.getImage();
		if ( internalTexture == null ) {
			return;
		}
		
		SkySphereIBL box = new SkySphereIBL(internalTexture);
		box.setLightPower(skybox.getPower());
		box.setLightMultiplier(skybox.getBrightness());
		if(RenderableApplication.pipeline instanceof Pipeline)
			((Pipeline) RenderableApplication.pipeline).getGBuffer().getMergeProcessor().setSkybox(box);
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

	public void setSkybox(Skybox skybox) {
		if ( skybox == null )
			this.set(C_SKYBOX, LuaValue.NIL);
		else
			this.set(C_SKYBOX, skybox);
	}
}
