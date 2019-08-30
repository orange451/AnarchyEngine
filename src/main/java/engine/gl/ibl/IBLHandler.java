package engine.gl.ibl;

import java.net.URL;

import engine.gl.Pipeline;
import engine.gl.SkyBox;
import engine.gl.renderer.LightProcessor;
import engine.gl.shader.BaseShader;

public class IBLHandler {
	private BaseShader shader = new IBLShader();

	private static final String U_TEXTURE_DEPTH = "texture_depth";
	private static final String U_TEXTURE_DIFFUSE = "texture_diffuse";
	private static final String U_TEXTURE_NORMAL = "texture_normal";
	private static final String U_TEXTURE_PBR = "texture_pbr";
	private static final String U_TEXTURE_IBL = "texture_ibl";
	private static final String U_AMBIENT = "uAmbient";
	private static final String U_INVERSE_VIEW_MATRIX = "uInverseViewMatrix";
	private static final String U_INVERSE_PROJ_MATRIX = "uInverseProjectionMatrix";
	private static final String U_SKYBOX_POWER = "uSkyBoxLightPower";
	private static final String U_SKYBOX_MULTIPLIER = "uSkyBoxLightMultiplier";

	public void handle(Pipeline pipeline) {
		SkyBox skybox = pipeline.getGBuffer().getMergeProcessor().getSkybox();
		
		if ( skybox == null || !(skybox instanceof SkySphereIBL) )
			return;
		
		pipeline.shader_set(shader);
		shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_DEPTH), pipeline.getGBuffer().getBufferDepth(), 0);
		shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_DIFFUSE), pipeline.getGBuffer().getBuffer0(), 1);
		shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_NORMAL), pipeline.getGBuffer().getBuffer1(), 2);
		shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_PBR), pipeline.getGBuffer().getBuffer2(), 3);
		shader.texture_set_stage(shader.shader_get_uniform(U_TEXTURE_IBL), ((SkySphereIBL)skybox), 4);
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_AMBIENT), pipeline.getGBuffer().getAmbient());
		shader.shader_set_uniform_matrix(shader.shader_get_uniform(U_INVERSE_VIEW_MATRIX), pipeline.getGBuffer().getInverseViewMatrix());
		shader.shader_set_uniform_matrix(shader.shader_get_uniform(U_INVERSE_PROJ_MATRIX), pipeline.getGBuffer().getInverseProjectionMatrix());
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_SKYBOX_POWER), ((SkySphereIBL)skybox).getLightPower());
		shader.shader_set_uniform_f(shader.shader_get_uniform(U_SKYBOX_MULTIPLIER), ((SkySphereIBL)skybox).getLightMultiplier());
		pipeline.fullscreenQuad();
	}
	
	class IBLShader extends BaseShader {
		public IBLShader() {
			super(
				new URL[] {
					LightProcessor.class.getResource("ibl.vert")
				},
				new URL[] {
					LightProcessor.class.getResource("reflect.frag"),
					LightProcessor.class.getResource("fresnel.frag"),
					LightProcessor.class.getResource("ibl.frag")
				}
			);
		}
	}
}