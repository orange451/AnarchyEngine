package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.FilePath;
import engine.Game;
import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.io.AsynchronousImage;
import engine.io.FileResource;
import engine.io.Image;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.util.TextureUtils;
import ide.layout.windows.icons.Icons;

public class Texture extends AssetLoadable implements TreeViewable,FileResource {
	private Texture2D texture;
	private boolean changed;
	private boolean loaded;
	private AsynchronousImage image;
	private Image loadedImage;
	
	private static final LuaValue C_TEXTURELOADED = LuaValue.valueOf("TextureLoaded");
	private static final LuaValue C_SRGB = LuaValue.valueOf("SRGB");
	private static final LuaValue C_FLIPY = LuaValue.valueOf("FlipY");
	private static final LuaValue C_TEXTURES = LuaValue.valueOf("Textures");
	
	public Texture() {
		super("Texture");
		
		this.defineField(C_SRGB.toString(), LuaValue.FALSE, false);
		this.defineField(C_FLIPY.toString(), LuaValue.FALSE, false);
		
		this.rawset(C_TEXTURELOADED, new LuaEvent());
	}
	
	public LuaEvent textureLoadedEvent() {
		return (LuaEvent) this.rawget(C_TEXTURELOADED);
	}
	
	public void setSRGB(boolean b) {
		this.set(C_SRGB, LuaValue.valueOf(b));
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
			if ( image == null || !image.isLoaded() || this.get(C_FILEPATH).isnil() || this.get(C_FILEPATH).toString().length() == 0 ) {
				if ( this.get(C_SRGB).checkboolean() ) {
					setTexture( Resources.TEXTURE_WHITE_SRGB );
				} else {
					setTexture( Resources.TEXTURE_WHITE_RGBA );
				}
				this.loaded = false;
			} else {
				this.loadedImage = image.getResource();
				if ( this.get(C_SRGB).checkboolean() ) {
					setTexture( TextureUtils.loadSRGBTextureFromImage(image.getResource()) );
				} else {
					setTexture( TextureUtils.loadRGBATextureFromImage(image.getResource()) );
				}
				((LuaEvent)this.rawget(C_TEXTURELOADED)).fire();
			}
		}
		return this.texture;
	}
	
	private void reloadFromFile(String path) {
		String realPath = FilePath.convertToSystem(path);
		
		loaded = false; // Force image to reload
		image = new AsynchronousImage(realPath, this.rawget(C_FLIPY).toboolean());
		Game.resourceLoader().addResource(image);
		
		loadedImage = null;
		texture = null;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key) ) {
			if ( key.eq_b(C_FILEPATH) || key.eq_b(C_FLIPY) ) {
				if ( key.eq_b(C_FLIPY) ) {
					this.rawset(key, value);
				}
				
				String texturePath = value.toString();
				if ( !key.eq_b(C_FILEPATH) )
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
	public LuaValue getPreferredParent() {
		return C_TEXTURES;
	}
	
	public static String getFileTypes() {
		return "png,bmp,tga,jpg,hdr";
	}

	public boolean hasLoaded() {
		return loaded;
	}
}
