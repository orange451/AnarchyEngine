package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.io.AsynchronousImage;
import engine.io.FileResource;
import engine.io.Image;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.util.TextureUtils;
import ide.IDEFilePath;
import ide.layout.windows.icons.Icons;

public class Texture extends AssetLoadable implements TreeViewable,FileResource {
	private Texture2D texture;
	private boolean changed;
	private boolean loaded;
	private AsynchronousImage image;
	private Image loadedImage;
	
	public Texture() {
		super("Texture");
		
		this.defineField("SRGB", LuaValue.FALSE, false);
		this.defineField("FlipY", LuaValue.FALSE, false);
		
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
	}
	
	public Image getImage() {
		// Force load the texture
		getTexture();
		
		// return it
		return this.loadedImage;
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
				loadedImage = image.getResource();
				if ( this.get("SRGB").checkboolean() ) {
					setTexture( TextureUtils.loadSRGBTextureFromImage(image.getResource()) );
				} else {
					setTexture( TextureUtils.loadRGBATextureFromImage(image.getResource()) );
				}
				((LuaEvent)this.rawget("TextureLoaded")).fire();
			}
		}
		return this.texture;
	}
	
	private void reloadFromFile(String path) {
		String realPath = IDEFilePath.convertToSystem(path);
		
		loaded = false; // Force image to reload
		image = new AsynchronousImage(realPath, this.rawget("FlipY").toboolean());
		Game.resourceLoader().addResource(image);
		
		loadedImage = null;
		texture = null;
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
				
				final String fTexPath = texturePath;
				this.reloadFromFile(fTexPath);
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
	
	public static String getFileTypes() {
		return "png,bmp,tga,jpg,hdr";
	}
}
