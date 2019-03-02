package engine.gl.renderer;

import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.Surface;
import engine.gl.ibl.IBLHandler;
import engine.gl.light.PointLightHandler;
import lwjgui.paint.Color;

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
			pointLight.handle(pipeline);
		}
		buffer.unbind();
	}

	public PointLightHandler getPointLightHandler() {
		return pointLight;
	}
}
