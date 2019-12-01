package engine.gl.lua;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.MaterialGL;
import engine.gl.LegacyPipeline;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.util.AABBUtil;
import engine.util.Pair;
import lwjgui.paint.Color;

public class OutlineRenderer {
	
	private static final float THICKNESS;
	private static final BufferedMesh MESH;
	private static final MaterialGL MATERIAL;
	
	static {
		THICKNESS = 1/24f;
		MESH = Resources.MESH_CUBE;
		MATERIAL = new MaterialGL().setReflective(0).setMetalness(0).setRoughness(1).setEmissive(Color.AQUA).setColor(Color.WHITE);
	}
	
	public static void render(Instance instance) {
		if ( !(instance instanceof Positionable) )
			return;
		
		Positionable object = (Positionable) instance;
		
		// Get AABB of prefab
		Pair<Vector3f, Vector3f> aabb = object.getAABB();
		
		// Get size
		float width = aabb.value2().x-aabb.value1().x;
		float length = aabb.value2().y-aabb.value1().y;
		float height = aabb.value2().z-aabb.value1().z;
		float j = THICKNESS;
		
		// Get its original world matrix
		Matrix4f worldMatrix = object.getWorldMatrix().toJoml().translate(AABBUtil.center(aabb).mul(-1));
		
		// Stuff
		float a = (aabb.value2().y+aabb.value1().y)/2f;
		Matrix4f t1 = worldMatrix.translate(new Vector3f(aabb.value2().x,a,aabb.value2().z), new Matrix4f());
		t1.scale(new Vector3f(THICKNESS, length-j, THICKNESS));
		Matrix4f t2 = worldMatrix.translate(new Vector3f(aabb.value2().x,a,aabb.value1().z), new Matrix4f());
		t2.scale(new Vector3f(THICKNESS, length-j, THICKNESS));
		Matrix4f t3 = worldMatrix.translate(new Vector3f(aabb.value1().x,a,aabb.value2().z), new Matrix4f());
		t3.scale(new Vector3f(THICKNESS, length-j, THICKNESS));
		Matrix4f t4 = worldMatrix.translate(new Vector3f(aabb.value1().x,a,aabb.value1().z), new Matrix4f());
		t4.scale(new Vector3f(THICKNESS, length-j, THICKNESS));

		// Stuff continued
		float b = (aabb.value2().x+aabb.value1().x)/2f;
		Matrix4f t5 = worldMatrix.translate(new Vector3f(b,aabb.value2().y,aabb.value2().z), new Matrix4f());
		t5.scale(new Vector3f(width+j, THICKNESS, THICKNESS));
		Matrix4f t6 = worldMatrix.translate(new Vector3f(b,aabb.value2().y,aabb.value1().z), new Matrix4f());
		t6.scale(new Vector3f(width+j, THICKNESS, THICKNESS));
		Matrix4f t7 = worldMatrix.translate(new Vector3f(b,aabb.value1().y,aabb.value2().z), new Matrix4f());
		t7.scale(new Vector3f(width+j, THICKNESS, THICKNESS));
		Matrix4f t8 = worldMatrix.translate(new Vector3f(b,aabb.value1().y,aabb.value1().z), new Matrix4f());
		t8.scale(new Vector3f(width+j, THICKNESS, THICKNESS));
		
		// Stuff Last
		float c = (aabb.value2().z+aabb.value1().z)/2f;
		Matrix4f t9 = worldMatrix.translate(new Vector3f(aabb.value2().x,aabb.value2().y,c), new Matrix4f());
		t9.scale(new Vector3f(THICKNESS, THICKNESS, height-j));
		Matrix4f t10 = worldMatrix.translate(new Vector3f(aabb.value1().x,aabb.value2().y,c), new Matrix4f());
		t10.scale(new Vector3f(THICKNESS, THICKNESS, height-j));
		Matrix4f t11 = worldMatrix.translate(new Vector3f(aabb.value1().x,aabb.value1().y,c), new Matrix4f());
		t11.scale(new Vector3f(THICKNESS, THICKNESS, height-j));
		Matrix4f t12 = worldMatrix.translate(new Vector3f(aabb.value2().x,aabb.value1().y,c), new Matrix4f());
		t12.scale(new Vector3f(THICKNESS, THICKNESS, height-j));
		
		// Draw
		BaseShader shader = LegacyPipeline.pipeline_get().shader_get();
		MESH.render(shader, t1, MATERIAL);
		MESH.render(shader, t2, MATERIAL);
		MESH.render(shader, t3, MATERIAL);
		MESH.render(shader, t4, MATERIAL);
		MESH.render(shader, t5, MATERIAL);
		MESH.render(shader, t6, MATERIAL);
		MESH.render(shader, t7, MATERIAL);
		MESH.render(shader, t8, MATERIAL);
		MESH.render(shader, t9, MATERIAL);
		MESH.render(shader, t10, MATERIAL);
		MESH.render(shader, t11, MATERIAL);
		MESH.render(shader, t12, MATERIAL);
	}

}
