package engine.gl.renderer;

import java.net.URL;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import engine.Game;
import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.SkyBox;
import engine.gl.Surface;
import engine.gl.Texture2D;
import engine.gl.ibl.SkySphereIBL;
import engine.gl.shader.BaseShader;
import engine.util.TextureUtils;
import lwjgui.paint.Color;

public class MergeProcessor implements PostProcessor {
	private BaseShader shader;
	private SkyBox skyBox;
	private Matrix3f skyBoxRotation = new Matrix3f();
	private Surface surface;
	
	public MergeProcessor() {
		shader = new MergeShader();
		
		TextureUtils.autoResize = false;
		//skyBox = new SkyBox(TextureUtils.loadImage("Resources/Testing/skybox.png"));
		//skyBox = SkySphereIBL.create("Resources/Testing/IBL/apartment.hdr");
		skyBox = SkySphereIBL.create("Resources/Testing/IBL/office.hdr");
		//skyBox = SkySphereIBL.create("Resources/Testing/IBL/noon_grass.hdr");
		((SkySphereIBL)skyBox).setLightMultiplier(10);
		TextureUtils.autoResize = true;
		skyBoxRotation.rotateX((float) (-Math.PI/2f));
	}
	
	@Override
	public void process(Pipeline pipeline) {
		int buffer0 = pipeline.getGBuffer().getBuffer0(); // Albedo
		int buffer3 = pipeline.getGBuffer().getBuffer3(); // Emissive
		int accumulation = pipeline.getGBuffer().getAccumulationBuffer().getTextureId(); // Light Accumulation buffer
		int ssao = -1; // SSAO - Not yet implemented
		int buffer2 = pipeline.getGBuffer().getBuffer2(); // PBR stuff
		int transparency = pipeline.getTransparencyRenderer().getBuffer();
		Texture2D depth = pipeline.getGBuffer().getBufferDepth();
		
		// Refresh merge surface if window was resized
		if ( surface == null || surface.getWidth() != pipeline.getWidth() || surface.getHeight() != pipeline.getHeight() ) {
			if ( surface != null ) {
				surface.cleanup();
			}
			Surface.filter = GL11.GL_LINEAR;
			surface = new Surface(pipeline.getWidth(),pipeline.getHeight(), GL30.GL_R11F_G11F_B10F);
		}
		
		// Update skybox if it needs to be updated.
		// This will only run on a skybox that implements this method
		// Such as a dynamic skybox.
		if ( skyBox != null ) {
			skyBox.draw(pipeline);
		}
		
		// Merge!
		surface.bind();
		{
			surface.draw_clear_alpha(Color.BLACK, 0.0f);
			
			pipeline.shader_set(shader);
			shader.texture_set_stage(shader.shader_get_uniform("texture_albedo"), buffer0, 0);
			shader.texture_set_stage(shader.shader_get_uniform("texture_emissive"), buffer3, 1);
			shader.texture_set_stage(shader.shader_get_uniform("texture_accumulation"), accumulation, 2);
			shader.texture_set_stage(shader.shader_get_uniform("texture_ssao"), ssao, 3);
			shader.texture_set_stage(shader.shader_get_uniform("texture_pbr"), buffer2, 4);
			shader.texture_set_stage(shader.shader_get_uniform("texture_depth"), depth, 5);
			shader.texture_set_stage(shader.shader_get_uniform("texture_transparency"), transparency, 6);
			shader.texture_set_stage(shader.shader_get_uniform("texture_skybox"), skyBox, 7);
			shader.shader_set_uniform_f(shader.shader_get_uniform("uAmbient"), pipeline.getGBuffer().getAmbient());
			shader.shader_set_uniform_matrix(shader.shader_get_uniform("inverseProjectionMatrix"), pipeline.getGBuffer().getInverseProjectionMatrix());
			shader.shader_set_uniform_matrix(shader.shader_get_uniform("inverseViewMatrix"), pipeline.getGBuffer().getInverseViewMatrix());
			shader.shader_set_uniform_matrix(shader.shader_get_uniform("uSkyBoxRotation"), skyBoxRotation);
			Vector3f pos = Game.workspace().getCurrentCamera().getPosition().toJoml();
			shader.shader_set_uniform_f(shader.shader_get_uniform("cameraPosition"), pos);
			
			pipeline.fullscreenQuad();
		}
		surface.unbind();
	}

	class MergeShader extends BaseShader {
		public MergeShader() {
			super(
				new URL[] {MergeProcessor.class.getResource("merge.vert")},
				new URL[] {MergeProcessor.class.getResource("merge.frag")}
			);
		}
	}

	public SkyBox getSkybox() {
		return skyBox;
	}

	public Surface getBuffer() {
		return surface;
	}
}
