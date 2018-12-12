package engine.gl;

import engine.gl.mesh.BufferedMesh;
import engine.io.Image;
import engine.util.MeshUtils;
import engine.util.TextureUtils;
import lwjgui.Color;

public class Resources {
	public static final Texture2D TEXTURE_WHITE_SRGB;
	public static final Texture2D TEXTURE_WHITE_RGBA;
	public static final Texture2D TEXTURE_NORMAL_RGBA;
	public static final BufferedMesh MESH_SPHERE;
	public static final BufferedMesh MESH_CUBE;
	public static final BufferedMesh MESH_CONE;
	public static final BufferedMesh MESH_CYLINDER;
	public static final MaterialGL MATERIAL_BLANK;
	
	static {
		TEXTURE_WHITE_SRGB = TextureUtils.loadSRGBTextureFromImage(new Image(Color.WHITE,1,1));
		TEXTURE_WHITE_RGBA = TextureUtils.loadRGBATextureFromImage(new Image(Color.WHITE,1,1));
		TEXTURE_NORMAL_RGBA = TextureUtils.loadRGBATextureFromImage(new Image(new Color(127,127,255),1,1));
		
		MESH_SPHERE = MeshUtils.sphere(1, 16);
		MESH_CUBE = MeshUtils.cube(1);
		MESH_CONE = MeshUtils.cone(1, 1, 16);
		MESH_CYLINDER = MeshUtils.cylinder(1, 1, 16);
		
		MATERIAL_BLANK = new MaterialGL();
	}
	
	public static void initialize() {
		// Do nothing. Calling this will force java to initialize static final vars.
	}
}
