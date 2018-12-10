package engine.gl.ibl;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.Pipeline;
import engine.gl.SkyBoxDynamic;
import engine.gl.mesh.BufferedMesh;
import engine.io.Image;
import engine.util.TextureUtils;
import lwjgui.Color;

public class SkySphere extends SkyBoxDynamic {
	private BufferedMesh sphere;
	private MaterialGL material;
	
	public SkySphere(Image image) {
		this( image, SKYBOX_TEXTURE_SIZE );
	}
	
	public SkySphere(Image image, int size) {
		super(new Image(Color.WHITE, size*4,size*3));
		
		material = new MaterialGL().setDiffuseTexture(TextureUtils.loadSRGBTextureFromImage(image));
		sphere = BufferedMesh.Import("engine/gl/ibl/skysphere.mesh");
	}

	@Override
	protected void renderGeometry(Pipeline pipeline) {
		Matrix4f worldMatrix = new Matrix4f();
		worldMatrix.rotateX((float) (Math.PI/2f));
		sphere.render(pipeline.shader_get(), worldMatrix, material);
	}

}
