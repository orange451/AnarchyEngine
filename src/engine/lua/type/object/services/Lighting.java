package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.Texture2D;
import engine.gl.ibl.SkySphereIBL;
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
								
								texture.textureLoadedEvent().connect((args2)->{
									InternalRenderThread.runLater(()->{
										updateSkybox( texture );
									});
								});
								updateSkybox(texture);
							} else {
								RenderableApplication.pipeline.getGBuffer().getMergeProcessor().setSkybox(null);
							}
						}
					});
				} else {
					LuaValue.error("Skybox field must be of type Skybox");
					return null;
				}
			}
		}
		
		return value;
	}

	private void updateSkybox(Texture texture) {
		Texture2D internalTexture = texture.getTexture();
		if ( internalTexture == null )
			return;
		
		SkySphereIBL box = new SkySphereIBL(internalTexture);
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
}
