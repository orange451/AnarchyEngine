package engine.gl.ibl;

import java.net.URL;

import engine.gl.Pipeline;
import engine.gl.SkyBox;
import engine.gl.renderer.LightProcessor;
import engine.gl.shader.BaseShader;

public class IBLHandler {
	private BaseShader shader = new IBLShader();

	public void handle(Pipeline pipeline) {
		SkyBox skybox = pipeline.getGBuffer().getMergeProcessor().getSkybox();
		
		if ( !(skybox instanceof SkySphereIBL) )
			return;
		
		pipeline.shader_set(shader);
		shader.texture_set_stage(shader.shader_get_uniform("texture_depth"), pipeline.getGBuffer().getBufferDepth(), 0);
		shader.texture_set_stage(shader.shader_get_uniform("texture_diffuse"), pipeline.getGBuffer().getBuffer0(), 1);
		shader.texture_set_stage(shader.shader_get_uniform("texture_normal"), pipeline.getGBuffer().getBuffer1(), 2);
		shader.texture_set_stage(shader.shader_get_uniform("texture_pbr"), pipeline.getGBuffer().getBuffer2(), 3);
		shader.texture_set_stage(shader.shader_get_uniform("texture_ibl"), ((SkySphereIBL)skybox).getLightSphere(), 4);
		shader.shader_set_uniform_f(shader.shader_get_uniform("uAmbient"), pipeline.getGBuffer().getAmbient());
		shader.shader_set_uniform_matrix(shader.shader_get_uniform("uInverseViewMatrix"), pipeline.getGBuffer().getInverseViewMatrix());
		shader.shader_set_uniform_matrix(shader.shader_get_uniform("uInverseProjectionMatrix"), pipeline.getGBuffer().getInverseProjectionMatrix());
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
					LightProcessor.class.getResource("ibl.frag")
				}
			);
		}
	}
}