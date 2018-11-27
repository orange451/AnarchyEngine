package engine.gl.gbuffer;

import java.net.URL;

import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.SkyBox;
import engine.gl.Surface;
import engine.gl.ibl.SkySphereIBL;
import engine.gl.light.PointLight;
import engine.gl.light.PointLightHandler;
import engine.gl.shader.BaseShader;
import lwjgui.Color;

public class LightProcessor implements PostProcessor {
	private PointLightHandler pointLight = new PointLightHandler();
	private IBLHandler iblHandler = new IBLHandler();
	
	@Override
	public void process(Pipeline pipeline) {
		Surface buffer = pipeline.getGBuffer().getAccumulationBuffer();
		
		buffer.bind();
		{
			buffer.draw_clear_alpha(Color.BLACK, 1.0f);
			
			// Draw IBL
			iblHandler.handle(pipeline);
			
			// Draw point lights
			//pointLight.handle(pipeline);
		}
		buffer.unbind();
	}

	public PointLightHandler getPointLightHandler() {
		return pointLight;
	}
}

class IBLHandler {
	private BaseShader shader = new IBLShader();

	public void handle(Pipeline pipeline) {
		SkyBox skybox = pipeline.getGBuffer().getMergeProcessor().getSkybox();
		
		//if ( !(skybox instanceof SkySphereIBL) )
			//return;
		
		pipeline.shader_set(shader);
		shader.texture_set_stage(shader.shader_get_uniform("texture_normal"), pipeline.getGBuffer().getBuffer1(), 0);
		shader.texture_set_stage(shader.shader_get_uniform("texture_ibl"), ((SkySphereIBL)skybox).getLightSphere().getTextureID(), 1);
		shader.shader_set_uniform_matrix(shader.shader_get_uniform("uInverseViewMatrix"), pipeline.getGBuffer().getInverseViewMatrix());
		pipeline.fullscreenQuad();
	}
	
	class IBLShader extends BaseShader {
		public IBLShader() {
			super(
				new URL[] {
						LightProcessor.class.getResource("ibl.vert")
				},
				new URL[] {
						LightProcessor.class.getResource("ibl.frag")
				}
			);
		}
	}
}
