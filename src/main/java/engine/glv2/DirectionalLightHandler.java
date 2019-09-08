package engine.glv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import engine.gl.light.DirectionalLightInternal;

public class DirectionalLightHandler implements IDirectionalLightHandler {

	private List<DirectionalLightInternal> lights = Collections
			.synchronizedList(new ArrayList<DirectionalLightInternal>());

	@Override
	public void addLight(DirectionalLightInternal l) {
		lights.add(l);
		l.init();
	}

	@Override
	public void removeLight(DirectionalLightInternal l) {
		synchronized (lights) {
			lights.remove(l);
		}
		l.dispose();
	}

	public List<DirectionalLightInternal> getLights() {
		return lights;
	}

}
