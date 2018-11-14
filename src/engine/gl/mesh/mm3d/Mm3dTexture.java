package engine.gl.mesh.mm3d;

import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.util.TextureUtils;

public class Mm3dTexture {
	private int flags;
	private String path;
	
	private Texture2D diffuseTexture;
	
	public Mm3dTexture(String filePath, long getuInt16, String string) {
		this.flags = (int) getuInt16;
		this.path = string;
		
		String path = filePath + string;
		path = path.replace("\\", "/");
		path = path.replace("/./", "/");
		
		if ( path.contains(".") )
			this.diffuseTexture = TextureUtils.loadSRGBTexture(path);
		else
			this.diffuseTexture = Resources.TEXTURE_WHITE_SRGB;
	}
	
	public String getFullPath() {
		return this.path;
	}

	public Texture2D getDiffuseTexture() {
		return diffuseTexture;
	}
}
