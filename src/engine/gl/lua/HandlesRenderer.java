package engine.gl.lua;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.application.RenderableApplication;
import engine.gl.MaterialGL;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.util.AABBUtil;
import engine.util.MatrixUtils;
import engine.util.Pair;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;
import luaengine.type.object.Instance;
import luaengine.type.object.insts.GameObject;
import lwjgui.Color;

public class HandlesRenderer {

	public static MoveType moveType = MoveType.WORLD_SPACE;
	public static float GRID_SIZE = 1/8f;
	
	private static MaterialGL baseMaterial;
	private static Vector3f hoveredHandleDirection;
	private static Vector3f selectedHandleDirection;
	private static Vector3f tempPos;
	private static Vector3f selectedOffset;
	private static boolean found;

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
			found = false;
			drawArrow( new Vector3f(  0,  0,  1 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  1,  0,  0 ), firstMatrix, aabb );
			drawArrow( new Vector3f(  0,  1,  0 ), firstMatrix, aabb );
			if ( !found ) {
				tempPos = null;
				hoveredHandleDirection = null;
			}
		}
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		// Handle clicking/dragging
		boolean holdLeft = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean holdRight = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		
		if ( holdLeft ) {
			if ( !holdRight ) {	
				// ABB Origin
				Vector3f origin = AABBUtil.center(aabb);
				
				if ( selectedHandleDirection == null && hoveredHandleDirection != null ) {
					selectedHandleDirection = hoveredHandleDirection;
					selectedOffset = projectMouseTo3D(origin, selectedHandleDirection).sub(origin);
				}
	
				if ( selectedHandleDirection != null ) {
	
					// Project mouse to 3d position
					Vector3f reprojected = projectMouseTo3D(origin, selectedHandleDirection);
					
					// Snap to grid
					float gs = 1/GRID_SIZE;
					Vector3f delta = reprojected.sub(origin, new Vector3f()).sub(selectedOffset);
					delta.mul(gs);
					delta.x = Math.round(delta.x);
					delta.y = Math.round(delta.y);
					delta.z = Math.round(delta.z);
					delta.mul(GRID_SIZE);
					
					// Final dragged position
					Vector3f reprojectedOffset = origin.add(delta, new Vector3f());
					
					// Reposition
					for (int i = 0; i < instances.size(); i++) {
						Instance t = instances.get(i);
						if ( !( t instanceof GameObject ) )
							continue;
						GameObject g = ((GameObject)t);
						Vector3f cPos = g.getPosition().toJoml();
						Vector3f offset = cPos.sub(origin, new Vector3f());
						
						g.setPosition(Vector3.newInstance(offset.add(reprojectedOffset)));
					}
				}
			}
		} else {
			selectedHandleDirection = null;
		}
	}
	
	/**
	 * Takes the mouses position and an objects origin, and calculates a new position along the moveDirection line.
	 * @param origin
	 * @param moveDirection
	 * @return
	 */
	private static Vector3f projectMouseTo3D(Vector3f origin, Vector3f moveDirection) {
		// Get 2d line
		Vector2f p1 = MatrixUtils.project3Dto2D(origin);
		Vector2f p2 = MatrixUtils.project3Dto2D(origin.add(moveDirection, new Vector3f()));
		Vector2f p3 = MatrixUtils.project3Dto2D(origin.sub(moveDirection, new Vector3f()));
		p2.sub(p1).normalize().mul(256).add(p1);
		p3.sub(p1).normalize().mul(256).add(p1);
	
		// Get closest point to the line from the mouses position
		Vector2f mouse = new Vector2f( (float)RenderableApplication.mouseX, (float)RenderableApplication.mouseY );
		Vector2f closest = getProjectedPointLine( p2, p3, mouse );
		
		// Reproject the closest point to 3d ray
		Vector3f ray = MatrixUtils.project2Dto3D(closest);
		
		// Generate a plane along the moveDirection
		Vector3f normal = getPlaneNormal( moveDirection );
		
		// Return the intersection from ray to normal plane
		return linePlaneIntersection( origin, normal, Pipeline.pipeline_get().getCamera().getPosition().toJoml(), ray).mul(moveDirection);
	}
	
	/**
	 * Calculates a RIGHT VECTOR for a given direction vector.
	 * Essentially the same math used when calculating a view matrix right vector.
	 * @param directionVector
	 * @return
	 */
	private static Vector3f getPlaneNormal( Vector3f directionVector ) {
		Vector3f up = new Vector3f(0, 0, 1);
		if ( directionVector.z == 1 || directionVector.z == -1 )
			up.set(0,1,0);
	
		float dirX = directionVector.x;
		float dirY = directionVector.y;
		float dirZ = directionVector.z;
		float upX = up.x;
		float upY = up.y;
		float upZ = up.z;
		
	    // right = direction x up
	    float rightX, rightY, rightZ;
	    rightX = dirY * upZ - dirZ * upY;
	    rightY = dirZ * upX - dirX * upZ;
	    rightZ = dirX * upY - dirY * upX;
	    // normalize right
	    float invRightLength = 1.0f / (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
	    rightX *= invRightLength;
	    rightY *= invRightLength;
	    rightZ *= invRightLength;
	    
	    return new Vector3f( rightX, rightY, rightZ );
	}
	
	/**
	 * Find the nearest point on line AB from point P
	 * @param A
	 * @param B
	 * @param P
	 * @return
	 */
	private static Vector2f getProjectedPointLine( Vector2f A, Vector2f B, Vector2f P ) {
		Vector2f vectorAP = P.sub(A, new Vector2f());
		Vector2f vectorAB = B.sub(A, new Vector2f());
		
		float magnitudeAB = (float) (Math.pow(vectorAB.x,2)+Math.pow(vectorAB.y,2));
		float ABAPproduct = vectorAB.x*vectorAP.x + vectorAB.y*vectorAP.y;
		float distance = ABAPproduct / magnitudeAB;
		
		return new Vector2f(A.x + vectorAB.x*distance, A.y + vectorAB.y*distance);
	}
	
	/**
	 * Return the point where a line intersects a plane
	 * @param planePoint
	 * @param planeNormal
	 * @param linePoint
	 * @param lineDirection
	 * @return
	 */
	private static Vector3f linePlaneIntersection(Vector3f planePoint, Vector3f planeNormal, Vector3f linePoint, Vector3f lineDirection) {
		if (planeNormal.dot(lineDirection) == 0) {
			return null;
		}
	
		float t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(lineDirection);
		Vector3f ret = lineDirection.mul(t, new Vector3f());
		return ret.add(linePoint);
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
		worldMat.scale(1/64f, 1/64f, 0.9f);
		headMat.scale(0.1f,0.1f,0.3f);

		// Make material for handle
		boolean selected = direction.equals(hoveredHandleDirection) || direction.equals(selectedHandleDirection);
		Vector3f col = moveType.equals(MoveType.WORLD_SPACE)?direction.absolute(new Vector3f()):new Vector3f(0.2f, 0.6f, 1.0f);
		baseMaterial.setEmissive(selected?col.mul(0.25f):col);

		// Draw
		Resources.MESH_CYLINDER.render(Pipeline.pipeline_get().shader_get(), worldMat, baseMaterial);
		Resources.MESH_CONE.render(Pipeline.pipeline_get().shader_get(), headMat, baseMaterial);

		Vector3f arrowPos = headMat.getTranslation(new Vector3f());
		Vector2f pos = MatrixUtils.project3Dto2D(arrowPos);
		Vector2f mou = new Vector2f((float)RenderableApplication.mouseX, (float)RenderableApplication.mouseY);
		boolean closer = true;
		Vector3f cameraPosition = Pipeline.pipeline_get().getCamera().getPosition().toJoml();
		float d1 = arrowPos.distance(cameraPosition);
		if ( tempPos != null ) {
			float d2 = tempPos.distance(cameraPosition);
			closer = d1 < d2;
		}
		if ( pos.distance(mou) < 200/d1 ) {
			found = true;

			if ( closer ) {
				tempPos = arrowPos;
				hoveredHandleDirection = direction;
			}
		}
	}

	enum MoveType {
		LOCAL_SPACE, WORLD_SPACE;
	}
}
