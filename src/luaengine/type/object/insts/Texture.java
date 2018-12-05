package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.io.AsynchronousImage;
import engine.io.FileResource;
import engine.io.Image;
import engine.util.TextureUtils;
import ide.IDEFilePath;
import ide.layout.windows.icons.Icons;
import luaengine.type.LuaEvent;
import luaengine.type.object.AssetLoadable;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;

public class Texture extends AssetLoadable implements TreeViewable,FileResource {
	private Texture2D texture;
	private boolean changed;
	private boolean loaded;
	private AsynchronousImage image;
	
	public Texture() {
		super("Texture");
		
		this.defineField("SRGB", LuaValue.valueOf(false), false);
		this.defineField("FlipY", LuaValue.valueOf(false), false);
		
		this.rawset("TextureLoaded", new LuaEvent());
	}
	
	public LuaEvent textureLoadedEvent() {
		return (LuaEvent) this.rawget("TextureLoaded");
	}
	
	public void setSRGB(boolean b) {
		this.set("SRGB", LuaValue.valueOf(b));
	}
	
	public void setTexture(Texture2D force) {
		this.texture = force;
		this.changed = false;
		this.loaded = true;
		((LuaEvent)this.rawget("TextureLoaded")).fire();
	}
	
	public Texture2D getTexture() {
		if ( image != null && image.isLoaded() && !loaded ) {
			changed = true;
			loaded = true;
		}
		
		if ( this.texture == null || changed ) {
			if ( image == null || !image.isLoaded() || this.get("FilePath").isnil() || this.get("FilePath").toString().length() == 0 ) {
				if ( this.get("SRGB").checkboolean() ) {
					setTexture( Resources.TEXTURE_WHITE_SRGB );
				} else {
					setTexture( Resources.TEXTURE_WHITE_RGBA );
				}
				this.loaded = false;
			} else {
				if ( this.get("SRGB").checkboolean() ) {
					setTexture( TextureUtils.loadSRGBTextureFromImage(image.getResource()) );
				} else {
					setTexture( TextureUtils.loadRGBATextureFromImage(image.getResource()) );
				}
			}
		}
		return this.texture;
	}
	
	private void reloadFromFile(String path) {
		String realPath = IDEFilePath.convertToSystem(path);
		
		loaded = false; // Force image to reload
		image = new AsynchronousImage(realPath, this.get("FlipY").toboolean());
		Game.resourceLoader().addResource(image);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key.toString()) ) {
			if ( key.toString().equals("FilePath") || key.toString().equals("FlipY") ) {
				if ( key.toString().equals("FlipY") ) {
					this.rawset(key, value);
				}
				
				String texturePath = value.toString();
				if ( !key.toString().equals("FilePath") )
					texturePath = this.getFilePath();
				
				this.reloadFromFile(texturePath);
			} else {
				changed = true; // Force image to be re-sent to GPU
			}
		}
		return value;
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
		return Icons.icon_texture;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.getService("Assets");
		if ( assets == null )
			return null;
		
		return assets.findFirstChild("Textures");
	}
}
