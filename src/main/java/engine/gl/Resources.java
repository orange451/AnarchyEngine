package engine.gl;

import engine.gl.mesh.BufferedMesh;
import engine.io.Image;
import engine.util.MeshUtils;
import engine.util.TextureUtils;
import lwjgui.paint.Color;

public class Resources {
	public static Texture2D TEXTURE_WHITE_SRGB;
	public static Texture2D TEXTURE_BLACK_SRGB;
	public static Texture2D TEXTURE_WHITE_RGBA;
	public static Texture2D TEXTURE_NORMAL_RGBA;
	public static Texture2D TEXTURE_DEBUG;
	public static BufferedMesh MESH_SPHERE;
	public static BufferedMesh MESH_CUBE;
	public static BufferedMesh MESH_CONE;
	public static BufferedMesh MESH_CYLINDER;
	public static BufferedMesh MESH_UNIT_QUAD;
	public static MaterialGL MATERIAL_BLANK;

	public static void initialize() {
		TEXTURE_WHITE_SRGB = TextureUtils.loadSRGBTextureFromImage(new Image(Color.WHITE, 1, 1));
		TEXTURE_BLACK_SRGB = TextureUtils.loadSRGBTextureFromImage(new Image(Color.BLACK, 1, 1));
		TEXTURE_WHITE_RGBA = TextureUtils.loadRGBATextureFromImage(new Image(Color.WHITE, 1, 1));
		TEXTURE_NORMAL_RGBA = TextureUtils.loadRGBATextureFromImage(new Image(new Color(127, 127, 255), 1, 1));
		TEXTURE_DEBUG = TextureUtils.loadRGBATexture("engine/gl/checker.png");

		MESH_SPHERE = MeshUtils.sphere(1, 16);
		MESH_CUBE = MeshUtils.cube(1);
		MESH_CONE = MeshUtils.cone(1, 1, 16);
		MESH_CYLINDER = MeshUtils.cylinder(1, 1, 16);
		MESH_UNIT_QUAD = MeshUtils.quad(1, 1);

		MATERIAL_BLANK = new MaterialGL().setDiffuseTexture(TEXTURE_WHITE_RGBA).setNormalTexture(TEXTURE_NORMAL_RGBA)
				.setMetalnessTexture(TEXTURE_WHITE_SRGB).setRoughnessTexture(TEXTURE_WHITE_SRGB);
	}
}
