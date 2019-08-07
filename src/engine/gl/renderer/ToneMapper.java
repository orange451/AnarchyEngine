package engine.gl.renderer;

import java.net.URL;

import engine.Game;
import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.Surface;
import engine.gl.shader.BaseShader;
import lwjgui.paint.Color;

public class ToneMapper implements PostProcessor {
	private BaseShader shader;
	
	public ToneMapper() {
		shader = new ToneMapperShader();
	}
	
	@Override
	public void process(Pipeline pipeline) {
		if ( Game.workspace().getCurrentCamera() == null )
			return;
		
		int buffer0 = pipeline.getGBuffer().getMergeProcessor().getBuffer().getTextureId();
		Surface buffer = pipeline.getGBuffer().getBufferFinal();
		
		// Woah brah! u toned!
		buffer.bind();
		{
			buffer.draw_clear_alpha(Color.BLACK, 1.0f);
			pipeline.shader_set(shader);
			shader.texture_set_stage(shader.shader_get_uniform("texture_diffuse"), buffer0, 0);
			shader.shader_set_uniform_f(shader.shader_get_uniform("uExposure"), pipeline.getGBuffer().getExposure());
			shader.shader_set_uniform_f(shader.shader_get_uniform("uGamma"), 1.0f/pipeline.getGBuffer().getGamma());
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSaturation"), pipeline.getGBuffer().getSaturation());
			pipeline.fullscreenQuad();
		}
		buffer.unbind();
	}

	class ToneMapperShader extends BaseShader {
		public ToneMapperShader() {
			super(
				new URL[] {ToneMapper.class.getResource("toneMap.vert")},
				new URL[] {ToneMapper.class.getResource("toneMap.frag")}
			);
		}
	}
}
