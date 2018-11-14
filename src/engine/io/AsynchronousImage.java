package engine.io;

import engine.util.TextureUtils;
import lwjgui.Color;

public class AsynchronousImage extends AsynchronousResource<Image> {
	private static Image TEMP;
	
	static {
		TEMP = new Image(Color.WHITE,1,1);
	}
	
	private Image image;
	private boolean flipY;
	
	public AsynchronousImage(String path, boolean flipY) {
		super(path);
		this.flipY = flipY;
	}

	@Override
	public Image getResource() {
		if ( !isLoaded() ) {
			return TEMP;
		}
		
		return image;
	}

	@Override
	public boolean isLoaded() {
		return image != null;
	}

	@Override
	protected void internalLoad() {
		Image i = TextureUtils.loadImage(this.filePath, flipY);
		if ( i.loaded() ) {
			image = i;
		}
	}
}
