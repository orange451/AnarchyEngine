package engine.gl.lua;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import engine.gl.MaterialGL;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.util.AABBUtil;
import engine.util.Pair;
import luaengine.type.data.Matrix4;
import luaengine.type.object.Instance;
import lwjgui.Color;

public class HandlesRenderer {
	
	public static MoveType moveType = MoveType.WORLD_SPACE;
	private static MaterialGL baseMaterial;
	private static Vector3f hoveredHandleDirection;
	private static Vector3f selectedHandleDirection;
	
	static {
		baseMaterial = new MaterialGL();
		baseMaterial.setMetalness(0);
		baseMaterial.setRoughness(1);
		baseMaterial.setReflective(0);
		baseMaterial.setColor(Color.BLACK);
	}

	public static void render(List<Instance> instances) {
		if ( instances.size() == 0 )
			return;
		
		// Get initial AABB
		Pair<Vector3f, Vector3f> aabb = AABBUtil.instanceAABB(instances.toArray(new Instance[instances.size()]));
		
		// Get first object with a matrix
		Matrix4f firstMatrix = null;
		for (int i = 0; i < instances.size(); i++) {
			Instance t = instances.get(i);
			if ( !t.get("WorldMatrix").isnil() ) {
				firstMatrix = ((Matrix4)t.get("WorldMatrix")).toJoml();
				break;
			}
		}
		
		if ( firstMatrix == null )
			return;

		// Draw handles
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		{
			drawArrow( new Vector3f(  0,  0,  1 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  0,  0, -1 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  1,  0,  0 ), firstMatrix, aabb );
			drawArrow( new Vector3f( -1,  0,  0 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  0,  1,  0 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  0, -1,  0 ), firstMatrix, aabb );
		}
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	private static void drawArrow(Vector3f direction, Matrix4f firstMatrix, Pair<Vector3f, Vector3f> aabb) {
		// Get correct handle position
		Matrix4f worldMat = new Matrix4f();
		worldMat.translate(AABBUtil.center(aabb));
		if ( moveType.equals(MoveType.LOCAL_SPACE) ) {
			Matrix4f tempRot = new Matrix4f(firstMatrix);
			tempRot.translate(firstMatrix.getTranslation(new Vector3f()).mul(-1));
			worldMat.mul(tempRot);
		}
		worldMat.translate(direction.mul(0.5f, new Vector3f()));
		
		// Visually rotate it in the right direction
		Vector3f up = new Vector3f(0,-1,0);
		if ( direction.y != 0 )
			up = new Vector3f(0,0,-direction.y);
		if ( direction.z != 0 )
			up = new Vector3f(0,direction.z,0);
		Matrix4f rot = new Matrix4f().lookAlong(direction, up);
		rot.rotateX((float) Math.PI);
		worldMat.mul(rot);
		
		// Finalize matrices
		Matrix4f headMat = worldMat.translate(0, 0, 0.5f, new Matrix4f());

		// Scale it down
		worldMat.scale(1/24f, 1/24f, 0.9f);
		headMat.scale(0.1f,0.1f,0.2f);
		
		// Make material for handle
		boolean selected = direction.equals(hoveredHandleDirection) || direction.equals(selectedHandleDirection);
		Vector3f col = moveType.equals(MoveType.WORLD_SPACE)?direction.absolute(new Vector3f()):new Vector3f(0.2f, 0.6f, 1.0f);
		baseMaterial.setEmissive(selected?col.mul(0.25f):col);

		// Draw
		Resources.MESH_CYLINDER.render(Pipeline.pipeline_get().shader_get(), worldMat, baseMaterial);
		Resources.MESH_CONE.render(Pipeline.pipeline_get().shader_get(), headMat, baseMaterial);
	}

	enum MoveType {
		LOCAL_SPACE, WORLD_SPACE;
	}
}
