package luaengine.type.object;

import org.joml.Vector3f;

import engine.util.AABBUtil;
import engine.util.Pair;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;

public interface Positionable {
	public Vector3 getPosition();
	public void setPosition(Vector3 position);
	public Matrix4 getWorldMatrix();
	public Pair<Vector3f, Vector3f> getAABB();
}
