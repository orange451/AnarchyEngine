package engine.lua.type.object.insts;

import java.util.HashMap;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.lua.type.LuaConnection;
import engine.lua.type.NumberClamp;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Asset;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class Material extends Asset implements TreeViewable {	
	private engine.gl.MaterialGL material;
	private boolean changed = true;

	private HashMap<String,LuaConnection> textureLoadsConnections = new HashMap<String,LuaConnection>();

	protected static final LuaValue C_DIFFUSETEXTURE = LuaValue.valueOf("DiffuseTexture");
	protected static final LuaValue C_NORMALTEXTURE = LuaValue.valueOf("NormalTexture");
	protected static final LuaValue C_METALLICTEXTURE = LuaValue.valueOf("MetallicTexture");
	protected static final LuaValue C_ROUGHNESSTEXTURE = LuaValue.valueOf("RoughnessTexture");

	protected static final LuaValue C_METALNESS = LuaValue.valueOf("Metalness");
	protected static final LuaValue C_ROUGHNESS = LuaValue.valueOf("Roughness");
	protected static final LuaValue C_REFLECTIVE = LuaValue.valueOf("Reflective");
	protected static final LuaValue C_COLOR = LuaValue.valueOf("Color");
	protected static final LuaValue C_EMISSIVE = LuaValue.valueOf("Emissive");
	protected static final LuaValue C_TRANSPARENCY = LuaValue.valueOf("Transparency");
	
	protected static final LuaValue C_MATERIALS = LuaValue.valueOf("Materials");
	
	public Material() {
		super("Material");
		
		this.setLocked(false);

		this.defineField(C_DIFFUSETEXTURE.toString(), LuaValue.NIL, false);
		this.defineField(C_NORMALTEXTURE.toString(), LuaValue.NIL, false);
		this.defineField(C_METALLICTEXTURE.toString(), LuaValue.NIL, false);
		this.defineField(C_ROUGHNESSTEXTURE.toString(), LuaValue.NIL, false);
		
		this.defineField(C_METALNESS.toString(), LuaValue.valueOf(0.0f), false);
		this.getField(C_METALNESS).setClamp(new NumberClamp(0, 1));
		
		this.defineField(C_ROUGHNESS.toString(), LuaValue.valueOf(0.4f), false);
		this.getField(C_ROUGHNESS).setClamp(new NumberClamp(0, 1));
		
		this.defineField(C_REFLECTIVE.toString(), LuaValue.valueOf(0.05f), false);
		this.getField(C_REFLECTIVE).setClamp(new NumberClamp(0, 1));
		
		this.defineField(C_COLOR.toString(), Color3.newInstance(255, 255, 255), false);
		this.defineField(C_EMISSIVE.toString(), new Vector3(), false);
		
		this.defineField(C_TRANSPARENCY.toString(), LuaValue.ZERO, false);
		this.getField(C_TRANSPARENCY).setClamp(new NumberClamp(0, 1));
		
		this.material = new engine.gl.MaterialGL();
	}
	
	public float getMetalness() {
		return this.get(C_METALNESS).tofloat();
	}
	
	public float getRoughness() {
		return this.get(C_ROUGHNESS).tofloat();
	}
	
	public float getReflectivness() {
		return this.get(C_REFLECTIVE).tofloat();
	}
	
	public float getTransparency() {
		return this.get(C_TRANSPARENCY).tofloat();
	}
	
	public void setTransparency(float f) {
		this.set(C_TRANSPARENCY, LuaValue.valueOf(f));
	}
	
	public Color3 getColor() {
		return (Color3)this.get(C_COLOR);
	}
	
	public void setColor(Color3 color) {
		this.set(C_COLOR, color);
	}
	
	public void setColor(Color color) {
		this.setColor(Color3.newInstance(color.getRed(), color.getGreen(), color.getBlue()));
	}
	
	public void setEmissive(Vector3 emissive) {
		this.set(C_EMISSIVE, emissive);
	}
	
	public void setEmissive(Vector3f emissive) {
		this.setEmissive(new Vector3(emissive));
	}
	
	public Vector3 getEmissive() {
		return (Vector3) this.get(C_EMISSIVE);
	}
	
	public void setMetalness(float f) {
		this.set(C_METALNESS, LuaValue.valueOf(f));
	}
	
	public void setReflective(float f) {
		this.set(C_REFLECTIVE, LuaValue.valueOf(f));
	}
	
	public void setRoughness(float f) {
		this.set(C_ROUGHNESS, LuaValue.valueOf(f));
	}
	
	public void setDiffuseMap(LuaValue texture) {
		this.set(C_DIFFUSETEXTURE, texture);
	}
	
	public void setNormalMap(LuaValue texture) {
		this.set(C_NORMALTEXTURE, texture);
	}
	
	public void setMetalMap(LuaValue texture) {
		this.set(C_METALLICTEXTURE, texture);
	}
	
	public void setRoughMap(LuaValue texture) {
		this.set(C_ROUGHNESSTEXTURE, texture);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key) )
			changed = true;
		
		if ( key.eq_b(C_DIFFUSETEXTURE)
				|| key.eq_b(C_NORMALTEXTURE)
				|| key.eq_b(C_METALLICTEXTURE)
				|| key.eq_b(C_ROUGHNESSTEXTURE)) {
			if ( !value.isnil() && !(value instanceof Texture) ) {
				return null;
			}
			
			onTextureSet(key, value);
		}
		return value;
	}

	private void onTextureSet(LuaValue key, LuaValue value) {
		String k = key.toString();
		LuaConnection t2 = textureLoadsConnections.get(k);
		
		// Garbage collect old connections
		if ( t2 != null ) {
			textureLoadsConnections.remove(k);
			t2.disconnect();
		}
		
		// Check if we're adding a new texture
		if ( value instanceof Texture ) {
			Texture tex = (Texture)value;
			
			// If the texture has a texture get loaded
			LuaConnection c2 = tex.textureLoadedEvent().connect((args2)->{
				changed = true;
			});
			
			// put it in the active connections
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
			if ( !this.rawget(C_DIFFUSETEXTURE).isnil() ) {
				dt = ((Texture)this.rawget(C_DIFFUSETEXTURE)).getTexture();
			}
			material.setDiffuseTexture(dt);

			Texture2D nt = null;
			if ( !this.rawget(C_NORMALTEXTURE).isnil() ) {
				nt = ((Texture)this.rawget(C_NORMALTEXTURE)).getTexture();
				if ( nt.equals(Resources.TEXTURE_WHITE_RGBA) || nt.equals(Resources.TEXTURE_WHITE_SRGB) )
					nt = null;
			}
			material.setNormalTexture(nt);
			
			Texture2D mt = null;
			if ( !this.rawget(C_METALLICTEXTURE).isnil() )
				mt = ((Texture)this.rawget(C_METALLICTEXTURE)).getTexture();
			material.setMetalnessTexture(mt);
			
			Texture2D rt = null;
			if ( !this.rawget(C_ROUGHNESSTEXTURE).isnil() )
				rt = ((Texture)this.rawget(C_ROUGHNESSTEXTURE)).getTexture();
			material.setRoughnessTexture(rt);

			material.setMetalness(this.getMetalness());
			material.setRoughness(this.getRoughness());
			material.setReflective(this.getReflectivness());
			material.setColor(this.getColor().toColor());
			material.setEmissive(this.getEmissive().toJoml());
			material.setTransparency(this.getTransparency());
		}
		return material;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.assets();
		if ( assets == null )
			return null;
		
		return assets.findFirstChild(C_MATERIALS);
	}
}
