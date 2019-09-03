package engine.glv2.v2;

import org.joml.Matrix4f;

import engine.glv2.entities.Sun;
import engine.lua.type.object.insts.Camera;

public class IRenderingData {

	public Camera camera;
	public Matrix4f projectionMatrix;
	public Sun sun;

}
