package engine.util;

import org.joml.Vector3f;

import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.lua.type.object.insts.Model;

public class AABBUtil {

	public static Pair<Vector3f, Vector3f> newAABB() {
		Pair<Vector3f,Vector3f> AABB = new Pair<Vector3f, Vector3f>(new Vector3f(), new Vector3f());
		AABB.value1().set(Integer.MAX_VALUE);
		AABB.value2().set(Integer.MIN_VALUE);
		
		return AABB;
	}
	
	public static Pair<Vector3f, Vector3f> newAABB( Vector3f min, Vector3f max ) {
		Pair<Vector3f,Vector3f> AABB = newAABB();
		AABB.value1().set(min);
		AABB.value2().set(max);
		
		return AABB;
	}
	
	public static Pair<Vector3f, Vector3f> instanceAABB( Instance... instances ) {
		Pair<Vector3f,Vector3f> AABB = newAABB();
		
		Vector3f min1 = AABB.value1();
		Vector3f max1 = AABB.value2();
		
		for (int i = 0; i < instances.length; i++) {
			Instance t = instances[i];
			if ( !( t instanceof Positionable ) )
				continue;
			
			Positionable obj = (Positionable)t;
			Pair<Vector3f, Vector3f> taabb = obj.getAABB();
			Vector3f off = AABBUtil.center(taabb).mul(-1);
			
			Vector3f min2 = obj.getWorldMatrix().toJoml().translate(taabb.value1()).getTranslation(new Vector3f()).add(off);
			Vector3f max2 = obj.getWorldMatrix().toJoml().translate(taabb.value2()).getTranslation(new Vector3f()).add(off);
			
			aabb( min1, min2, max1, max2 );
		}
		
		return AABB;
	}
	
	public static Pair<Vector3f, Vector3f> prefabAABB(float scale, Model... models) {
		Pair<Vector3f,Vector3f> AABB = newAABB();
		
		Vector3f minTemp = new Vector3f();
		Vector3f maxTemp = new Vector3f();
		for (int i = 0; i < models.length; i++) {
			BufferedMesh mesh = models[i].getMeshInternal();
			if ( mesh == null )
				mesh = Resources.MESH_SPHERE;
			
			Pair<Vector3f, Vector3f> aabb = mesh.getAABB();
			Vector3f min1 = AABB.value1();
			Vector3f max1 = AABB.value2();
			Vector3f min2 = aabb.value1().mul(scale, minTemp);
			Vector3f max2 = aabb.value2().mul(scale, maxTemp);
			
			aabb( min1, min2, max1, max2 );
		}
		
		return AABB;
	}

	private static void aabb( Vector3f min1, Vector3f min2, Vector3f max1, Vector3f max2 ) {
		if ( min2.x < min1.x )
			min1.x = min2.x;
		if ( min2.y < min1.y )
			min1.y = min2.y;
		if ( min2.z < min1.z )
			min1.z = min2.z;

		if ( max2.x > max1.x )
			max1.x = max2.x;
		if ( max2.y > max1.y )
			max1.y = max2.y;
		if ( max2.z > max1.z )
			max1.z = max2.z;
	}

	public static Vector3f center(Pair<Vector3f, Vector3f> aabb) {
		return aabb.value1().add(aabb.value2(), new Vector3f()).mul(0.5f);
	}

	public static Vector3f extents(Pair<Vector3f, Vector3f> aabb) {
		return aabb.value2().sub(aabb.value1(), new Vector3f());
	}
}
