package engine.observer;

import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Camera;

public interface RenderableWorld {
	public Camera getCurrentCamera();
	public Instance getInstance();
}
