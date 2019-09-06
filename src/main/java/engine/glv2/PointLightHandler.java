package engine.glv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import engine.gl.light.PointLightInternal;

public class PointLightHandler implements IPointLightHandler {

	private List<PointLightInternal> lights = Collections.synchronizedList(new ArrayList<PointLightInternal>());

	@Override
	public void addLight(PointLightInternal l) {
		lights.add(l);
	}

	@Override
	public void removeLight(PointLightInternal l) {
		synchronized (lights) {
			lights.remove(l);
		}
	}

	public List<PointLightInternal> getLights() {
		return lights;
	}

}
