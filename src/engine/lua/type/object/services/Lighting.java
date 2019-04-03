package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.Texture2D;
import engine.gl.ibl.SkySphereIBL;
import engine.io.Image;
import engine.lua.type.LuaConnection;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Skybox;
import engine.lua.type.object.insts.Texture;
import ide.layout.windows.icons.Icons;

public class Lighting extends Service implements TreeViewable {

	public Lighting() {
		super("Lighting");
		
		this.defineField("Ambient", Color3.newInstance(128, 128, 128), false);
		
		this.defineField("Exposure", LuaValue.valueOf(1.0f), false);
		this.getField("Exposure").setClamp(new NumberClampPreferred(0, 25, 0, 2));
		
		this.defineField("Saturation", LuaValue.valueOf(1.2f), false);
		this.getField("Saturation").setClamp(new NumberClampPreferred(0, 100, 0, 2));
		
		this.defineField("Gamma", LuaValue.valueOf(2.2f), false);
		this.getField("Gamma").setClamp(new NumberClampPreferred(0, 10, 0, 4));
		
		this.defineField("Skybox", LuaValue.NIL, false);
	}
	
	public Color3 getAmbient() {
		return (Color3) this.get("Ambient");
	}
	
	public void setAmbient(Color3 color) {
		this.set("Ambient", color);
	}
	
	public float getExposure() {
		return this.get("Exposure").tofloat();
	}
	
	public void setExposure(float value) {
		this.set("Exposure", value);
	}
	
	public float getSaturation() {
		return this.get("Saturation").tofloat();
	}
	
	public void setSaturation(float value) {
		this.set("Saturation", value);
	}
	
	public float getGamma() {
		return this.get("Gamma").tofloat();
	}
	
	public void setGamma(float value) {
		this.set("Gamma", value);
	}

	private LuaConnection skyboxChanged;
	private LuaConnection textureLoaded;
	private LuaConnection textureChanged;
	private LuaConnection waitForImage;
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		
		if ( key.toString().equals("Skybox") ) {
			if ( value.isnil() ) {
				RenderableApplication.pipeline.getGBuffer().getMergeProcessor().setSkybox(null);
			} else {
				if ( value instanceof Skybox) {
					Skybox skybox = (Skybox)value;
					
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
								RenderableApplication.pipeline.getGBuffer().getMergeProcessor().setSkybox(null);
							}
						}
						
						if ( key1.equals("Power") ) {
							RenderableApplication.pipeline.getGBuffer().getMergeProcessor().getSkybox().setLightPower(value1.tofloat());
						}
						
						if ( key1.equals("Brightness") ) {
							RenderableApplication.pipeline.getGBuffer().getMergeProcessor().getSkybox().setLightMultiplier(value1.tofloat());
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
		RenderableApplication.pipeline.getGBuffer().getMergeProcessor().setSkybox(box);
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
		LuaValue ret = this.get("Skybox");
		return ret.isnil()?null:(Skybox)ret;
	}
}
