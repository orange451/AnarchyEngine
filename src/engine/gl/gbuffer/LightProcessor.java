package engine.gl.gbuffer;

import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.Surface;
import engine.gl.light.PointLightHandler;
import lwjgui.Color;

public class LightProcessor implements PostProcessor {
	private PointLightHandler pointLight = new PointLightHandler();
	
	@Override
	public void process(Pipeline pipeline) {
		Surface buffer = pipeline.getGBuffer().getAccumulationBuffer();
		
		buffer.bind();
		{
			buffer.draw_clear_alpha(Color.BLACK, 1.0f);
			pointLight.handle(pipeline);
		}
		buffer.unbind();
	}
}
