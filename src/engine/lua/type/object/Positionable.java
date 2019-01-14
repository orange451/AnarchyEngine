package engine.lua.type.object;

import org.joml.Vector3f;

import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.util.AABBUtil;
import engine.util.Pair;

public interface Positionable {
	public Vector3 getPosition();
	public void setPosition(Vector3 position);
	public Matrix4 getWorldMatrix();
	public Pair<Vector3f, Vector3f> getAABB();
}
