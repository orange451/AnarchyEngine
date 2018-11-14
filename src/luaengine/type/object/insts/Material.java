package luaengine.type.object.insts;

import java.util.HashMap;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.gl.Resources;
import engine.gl.Texture2D;
import ide.layout.windows.icons.Icons;
import luaengine.type.LuaConnection;
import luaengine.type.data.Color3;
import luaengine.type.object.Asset;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;

public class Material extends Asset implements TreeViewable {	
	private engine.gl.MaterialGL material;
	private boolean changed = true;

	private HashMap<String,LuaConnection> textureChangesConnections = new HashMap<String,LuaConnection>();
	private HashMap<String,LuaConnection> textureLoadsConnections = new HashMap<String,LuaConnection>();
	
	public Material() {
		super("Material");
		
		this.setLocked(false);

		this.defineField("DiffuseTexture", LuaValue.NIL, false);
		this.defineField("NormalTexture", LuaValue.NIL, false);
		this.defineField("MetallicTexture", LuaValue.NIL, false);
		this.defineField("RoughnessTexture", LuaValue.NIL, false);
		this.defineField("Metalness", LuaValue.valueOf(0.3f), false);
		this.defineField("Roughness", LuaValue.valueOf(0.3f), false);
		this.defineField("Reflective", LuaValue.valueOf(0.3f), false);
		this.defineField("Color", Color3.newInstance(255, 255, 255), false);
		
		this.material = new engine.gl.MaterialGL();
	}
	
	public void setColor(Color3 color) {
		this.set("Color", color);
	}
	
	public void setMetalness(float f) {
		this.set("Metalness", f);
	}
	
	public void setReflective(float f) {
		this.set("Reflective", f);
	}
	
	public void setRoughness(float f) {
		this.set("Roughness", f);
	}
	
	public void setDiffuseMap(LuaValue texture) {
		this.set("DiffuseTexture", texture);
	}
	
	public void setNormalMap(LuaValue texture) {
		this.set("NormalTexture", texture);
	}
	
	public void setMetalMap(LuaValue texture) {
		this.set("MetallicTexture", texture);
	}
	
	public void setRoughMap(LuaValue texture) {
		this.set("RoughnessTexture", texture);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key.toString()) )
			changed = true;
		
		if ( key.toString().equals("DiffuseTexture")
				|| key.toString().equals("NormalTexture")
				|| key.toString().equals("MetallicTexture")
				|| key.toString().equals("RoughnessTexture")) {
			onTextureChange(key, value);
		}
		return value;
	}

	private void onTextureChange(LuaValue key, LuaValue value) {
		String k = key.toString();
		LuaConnection t1 = textureChangesConnections.get(k);
		LuaConnection t2 = textureLoadsConnections.get(k);
		
		// Garbage collect old connections
		if ( t1 != null ) {
			textureChangesConnections.remove(k);
			t1.disconnect();
		}
		if ( t2 != null ) {
			textureLoadsConnections.remove(k);
			t2.disconnect();
		}
		
		// Check if we're adding a new texture
		if ( value instanceof Texture ) {
			Texture tex = (Texture)value;
			
			// If the texture has any variables changed...
			LuaConnection c1 = tex.changedEvent().connect((args)->{
				//changed = true;
			});
			
			// If the texture has a texture get loaded
			LuaConnection c2 = tex.textureLoadedEvent().connect((args2)->{
				changed = true;
			});
			
			// put it in the active connections
			textureChangesConnections.put(k, c1);
			textureLoadsConnections.put(k, c2);
		}
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_material;
	}

	private long lastUpdate = System.currentTimeMillis();
	public engine.gl.MaterialGL getMaterial() {
		if ( changed || (System.currentTimeMillis()-lastUpdate > 50)) {
			lastUpdate = System.currentTimeMillis();
			changed = false;
			
			Texture2D dt = null;
			if ( !this.rawget("DiffuseTexture").isnil() ) {
				dt = ((Texture)this.rawget("DiffuseTexture")).getTexture();
			}
			material.setDiffuseTexture(dt);

			Texture2D nt = null;
			if ( !this.rawget("NormalTexture").isnil() ) {
				nt = ((Texture)this.rawget("NormalTexture")).getTexture();
				if ( nt.equals(Resources.TEXTURE_WHITE_RGBA) || nt.equals(Resources.TEXTURE_WHITE_SRGB) )
					nt = null;
			}
			material.setNormalTexture(nt);
			
			Texture2D mt = null;
			if ( !this.rawget("MetallicTexture").isnil() )
				mt = ((Texture)this.rawget("MetallicTexture")).getTexture();
			material.setMetalnessTexture(mt);
			
			Texture2D rt = null;
			if ( !this.rawget("RoughnessTexture").isnil() )
				rt = ((Texture)this.rawget("RoughnessTexture")).getTexture();
			material.setRoughnessTexture(rt);

			material.setMetalness((float) this.rawget("Metalness").checkdouble());
			material.setRoughness((float) this.rawget("Roughness").checkdouble());
			material.setReflective((float) this.rawget("Reflective").checkdouble());
			material.setColor(((Color3)this.rawget("Color")).toColor());
		}
		return material;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.getService("Assets");
		if ( assets == null )
			return null;
		
		return assets.findFirstChild("Materials");
	}
}
