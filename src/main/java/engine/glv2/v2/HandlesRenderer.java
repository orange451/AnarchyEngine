package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.AnarchyEngineClient;
import engine.Game;
import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.glv2.shaders.OutlineShader;
import engine.lua.history.HistoryChange;
import engine.lua.history.HistorySnapshot;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.lua.type.object.insts.Camera;
import engine.util.AABBUtil;
import engine.util.MatrixUtils;
import engine.util.Pair;
import lwjgui.paint.Color;

public class HandlesRenderer {

	private static final float THICKNESS;
	private static final BufferedMesh MESH;
	private static final MaterialGL MATERIAL;

	public static MoveType moveType = MoveType.WORLD_SPACE;
	public static float GRID_SIZE = 1 / 8f;

	private static MaterialGL baseMaterial;
	private static Vector3f hoveredHandleDirection;
	private static Vector3f selectedHandleDirection;
	private static Vector3f tempPos;
	private static Vector3f selectedOffset;
	private static boolean found;

	static {
		THICKNESS = 1 / 32f;
		MESH = Resources.MESH_CUBE;
		MATERIAL = new MaterialGL().setReflective(0).setMetalness(0).setRoughness(1).setColor(Color.AQUA);
		baseMaterial = new MaterialGL();
		baseMaterial.setMetalness(0);
		baseMaterial.setRoughness(1);
		baseMaterial.setReflective(0);
		baseMaterial.setColor(Color.BLACK);
	}

	private OutlineShader shader;
	private ArrayList<Instance> DRAGGING_OBJECTS;
	private HashMap<Instance, Vector3> TRACK_POSITIONS = new HashMap<Instance, Vector3>();

	public HandlesRenderer() {
		shader = new OutlineShader();
		shader.init();
		
		Game.loadEvent().connect((loadargs)->{
			Game.userInputService().inputBeganEvent().connect((args)->{
				LuaTable input = (LuaTable) args[0];
				if ( input.get("KeyCode").toint() == GLFW.GLFW_KEY_T ) {
					if ( moveType == MoveType.LOCAL_SPACE ) {
						moveType = MoveType.WORLD_SPACE;
					} else {
						moveType = MoveType.LOCAL_SPACE;
					}
				}
			});
		});
	}

	public void render(Camera camera, Matrix4f projection, List<Instance> instances, Vector2f size) {
		glDepthMask(false);
		glDisable(GL_DEPTH_TEST);
		shader.start();
		shader.loadCamera(camera, projection);
		MESH.bind();
		for (Instance instance : instances) {
			renderInstance(instance);
		}
		MESH.unbind();
		renderInstances(instances, camera, projection, size);
		shader.stop();
		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
	}

	public void dispose() {
		shader.dispose();
	}

	private void renderInstance(Instance instance) {
		if (!(instance instanceof Positionable))
			return;

		Positionable object = (Positionable) instance;

		// Get AABB of prefab
		Pair<Vector3f, Vector3f> aabb = object.getAABB();

		// Get size
		float width = aabb.value2().x - aabb.value1().x;
		float length = aabb.value2().y - aabb.value1().y;
		float height = aabb.value2().z - aabb.value1().z;
		float j = THICKNESS;

		// Get its original world matrix
		Matrix4f worldMatrix = object.getWorldMatrix().toJoml().translate(AABBUtil.center(aabb).mul(-1));

		// Stuff
		float a = (aabb.value2().y + aabb.value1().y) / 2f;
		Matrix4f t1 = worldMatrix.translate(new Vector3f(aabb.value2().x, a, aabb.value2().z), new Matrix4f());
		t1.scale(new Vector3f(THICKNESS, length - j, THICKNESS));
		Matrix4f t2 = worldMatrix.translate(new Vector3f(aabb.value2().x, a, aabb.value1().z), new Matrix4f());
		t2.scale(new Vector3f(THICKNESS, length - j, THICKNESS));
		Matrix4f t3 = worldMatrix.translate(new Vector3f(aabb.value1().x, a, aabb.value2().z), new Matrix4f());
		t3.scale(new Vector3f(THICKNESS, length - j, THICKNESS));
		Matrix4f t4 = worldMatrix.translate(new Vector3f(aabb.value1().x, a, aabb.value1().z), new Matrix4f());
		t4.scale(new Vector3f(THICKNESS, length - j, THICKNESS));

		// Stuff continued
		float b = (aabb.value2().x + aabb.value1().x) / 2f;
		Matrix4f t5 = worldMatrix.translate(new Vector3f(b, aabb.value2().y, aabb.value2().z), new Matrix4f());
		t5.scale(new Vector3f(width + j, THICKNESS, THICKNESS));
		Matrix4f t6 = worldMatrix.translate(new Vector3f(b, aabb.value2().y, aabb.value1().z), new Matrix4f());
		t6.scale(new Vector3f(width + j, THICKNESS, THICKNESS));
		Matrix4f t7 = worldMatrix.translate(new Vector3f(b, aabb.value1().y, aabb.value2().z), new Matrix4f());
		t7.scale(new Vector3f(width + j, THICKNESS, THICKNESS));
		Matrix4f t8 = worldMatrix.translate(new Vector3f(b, aabb.value1().y, aabb.value1().z), new Matrix4f());
		t8.scale(new Vector3f(width + j, THICKNESS, THICKNESS));

		// Stuff Last
		float c = (aabb.value2().z + aabb.value1().z) / 2f;
		Matrix4f t9 = worldMatrix.translate(new Vector3f(aabb.value2().x, aabb.value2().y, c), new Matrix4f());
		t9.scale(new Vector3f(THICKNESS, THICKNESS, height - j));
		Matrix4f t10 = worldMatrix.translate(new Vector3f(aabb.value1().x, aabb.value2().y, c), new Matrix4f());
		t10.scale(new Vector3f(THICKNESS, THICKNESS, height - j));
		Matrix4f t11 = worldMatrix.translate(new Vector3f(aabb.value1().x, aabb.value1().y, c), new Matrix4f());
		t11.scale(new Vector3f(THICKNESS, THICKNESS, height - j));
		Matrix4f t12 = worldMatrix.translate(new Vector3f(aabb.value2().x, aabb.value1().y, c), new Matrix4f());
		t12.scale(new Vector3f(THICKNESS, THICKNESS, height - j));

		shader.loadMaterial(MATERIAL);
		// Draw
		shader.loadTransformationMatrix(t1);
		MESH.render();
		shader.loadTransformationMatrix(t2);
		MESH.render();
		shader.loadTransformationMatrix(t3);
		MESH.render();
		shader.loadTransformationMatrix(t4);
		MESH.render();
		shader.loadTransformationMatrix(t5);
		MESH.render();
		shader.loadTransformationMatrix(t6);
		MESH.render();
		shader.loadTransformationMatrix(t7);
		MESH.render();
		shader.loadTransformationMatrix(t8);
		MESH.render();
		shader.loadTransformationMatrix(t9);
		MESH.render();
		shader.loadTransformationMatrix(t10);
		MESH.render();
		shader.loadTransformationMatrix(t11);
		MESH.render();
		shader.loadTransformationMatrix(t12);
		MESH.render();
	}

	private void renderInstances(List<Instance> instances, Camera camera, Matrix4f projection, Vector2f size) {
		if (instances.size() == 0)
			return;
		// Get initial AABB
		Pair<Vector3f, Vector3f> aabb = AABBUtil.instanceAABB(instances.toArray(new Instance[instances.size()]));

		// Get first object with a matrix
		Matrix4f firstMatrix = null;
		for (int i = 0; i < instances.size(); i++) {
			Instance t = instances.get(i);
			if (t instanceof Positionable) {
				firstMatrix = ((Positionable) t).getWorldMatrix().toJoml();
				break;
			}
		}

		if (firstMatrix == null)
			return;

		// Draw handles
		found = false;
		drawArrow(new Vector3f(0, 0, 1), firstMatrix, aabb, camera, projection, size);
		drawArrow(new Vector3f(1, 0, 0), firstMatrix, aabb, camera, projection, size);
		drawArrow(new Vector3f(0, 1, 0), firstMatrix, aabb, camera, projection, size);
		if (!found) {
			tempPos = null;
			hoveredHandleDirection = null;
		}

		// Handle clicking/dragging
		// TODO: Use callbacks
		boolean holdLeft = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(),
				GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean holdRight = GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(),
				GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (holdLeft) {
			if (!holdRight) {
				// ABB Origin
				Vector3f origin = AABBUtil.center(aabb);

				if (selectedHandleDirection == null && hoveredHandleDirection != null) {
					selectedHandleDirection = hoveredHandleDirection;
					selectedOffset = projectMouseTo3D(origin, selectedHandleDirection, camera, projection, size)
							.sub(origin);
				}

				if (selectedHandleDirection != null) {

					// Project mouse to 3d position
					Vector3f reprojected = projectMouseTo3D(origin, selectedHandleDirection, camera, projection, size);

					// Snap to grid
					float gs = 1 / GRID_SIZE;
					Vector3f delta = reprojected.sub(origin, new Vector3f()).sub(selectedOffset);
					delta.mul(gs);
					delta.x = Math.round(delta.x);
					delta.y = Math.round(delta.y);
					delta.z = Math.round(delta.z);
					delta.mul(GRID_SIZE);

					// Final dragged position
					Vector3f reprojectedOffset = origin.add(delta, new Vector3f());

					// Reposition
					ArrayList<Instance> dragging = new ArrayList<Instance>();
					for (int i = 0; i < instances.size(); i++) {
						Instance t = instances.get(i);
						if (!(t instanceof Positionable))
							continue;
						Positionable g = ((Positionable) t);
						Vector3f cPos = g.getPosition().toJoml();
						Vector3f offset = cPos.sub(origin, new Vector3f());

						if ( !TRACK_POSITIONS.containsKey(t) ) {
							TRACK_POSITIONS.put(t, g.getPosition());
						}
						g.setPosition(new Vector3(offset.add(reprojectedOffset)));
						dragging.add(t);
					}
					
					DRAGGING_OBJECTS = dragging;
				}
			}
		} else {
			selectedHandleDirection = null;
			
			// Update history
			if ( DRAGGING_OBJECTS != null ) {
				HistorySnapshot snapshot = new HistorySnapshot();
				
				for (int i = 0; i < DRAGGING_OBJECTS.size(); i++) {
					Instance inst = DRAGGING_OBJECTS.get(i);
					Vector3 oldPosition = TRACK_POSITIONS.get(inst);
					if ( oldPosition == null )
						continue;
					Vector3 newPosition = ((Positionable)inst).getPosition();
					
					HistoryChange change = new HistoryChange(
							Game.historyService().getHistoryStack().getObjectReference(inst),
							LuaValue.valueOf("Position"),
							oldPosition,
							newPosition
					);
					snapshot.changes.add(change);
				}
				
				Game.historyService().pushChange(snapshot);
				
				TRACK_POSITIONS.clear();
				DRAGGING_OBJECTS = null;
			}
		}
	}

	/**
	 * Takes the mouses position and an objects origin, and calculates a new
	 * position along the moveDirection line.
	 * 
	 * @param origin
	 * @param moveDirection
	 * @return
	 */
	private Vector3f projectMouseTo3D(Vector3f origin, Vector3f moveDirection, Camera camera, Matrix4f projection,
			Vector2f size) {
		// Get 2d line
		Vector2f p1 = MatrixUtils.project3Dto2D(origin, projection, camera.getViewMatrix().getInternal(), size);
		Vector2f p2 = MatrixUtils.project3Dto2D(origin.add(moveDirection, new Vector3f()), projection,
				camera.getViewMatrix().getInternal(), size);
		Vector2f p3 = MatrixUtils.project3Dto2D(origin.sub(moveDirection, new Vector3f()), projection,
				camera.getViewMatrix().getInternal(), size);
		p2.sub(p1).normalize().mul(256).add(p1);
		p3.sub(p1).normalize().mul(256).add(p1);

		// Get closest point to the line from the mouses position
		Vector2f mouse = new Vector2f((float) AnarchyEngineClient.mouseX, (float) AnarchyEngineClient.mouseY);
		Vector2f closest = getProjectedPointLine(p2, p3, mouse);

		// Reproject the closest point to 3d ray
		Vector3f ray = MatrixUtils.project2Dto3D(closest, projection, camera, size);

		// Generate a plane along the moveDirection
		Vector3f normal = getPlaneNormal(moveDirection);

		// Return the intersection from ray to normal plane
		return linePlaneIntersection(origin, normal, camera.getPosition().toJoml(), ray).mul(moveDirection);
	}

	/**
	 * Calculates a RIGHT VECTOR for a given direction vector. Essentially the same
	 * math used when calculating a view matrix right vector.
	 * 
	 * @param directionVector
	 * @return
	 */
	private Vector3f getPlaneNormal(Vector3f directionVector) {
		Vector3f up = new Vector3f(0, 0, 1);
		if (directionVector.z == 1 || directionVector.z == -1)
			up.set(0, 1, 0);

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

		return new Vector3f(rightX, rightY, rightZ);
	}

	/**
	 * Find the nearest point on line AB from point P
	 * 
	 * @param A
	 * @param B
	 * @param P
	 * @return
	 */
	private Vector2f getProjectedPointLine(Vector2f A, Vector2f B, Vector2f P) {
		Vector2f vectorAP = P.sub(A, new Vector2f());
		Vector2f vectorAB = B.sub(A, new Vector2f());

		float magnitudeAB = (float) (Math.pow(vectorAB.x, 2) + Math.pow(vectorAB.y, 2));
		float ABAPproduct = vectorAB.x * vectorAP.x + vectorAB.y * vectorAP.y;
		float distance = ABAPproduct / magnitudeAB;

		return new Vector2f(A.x + vectorAB.x * distance, A.y + vectorAB.y * distance);
	}

	/**
	 * Return the point where a line intersects a plane
	 * 
	 * @param planePoint
	 * @param planeNormal
	 * @param linePoint
	 * @param lineDirection
	 * @return
	 */
	private Vector3f linePlaneIntersection(Vector3f planePoint, Vector3f planeNormal, Vector3f linePoint,
			Vector3f lineDirection) {
		if (planeNormal.dot(lineDirection) == 0) {
			return null;
		}

		float t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(lineDirection);
		Vector3f ret = lineDirection.mul(t, new Vector3f());
		return ret.add(linePoint);
	}

	private void drawArrow(Vector3f direction, Matrix4f firstMatrix, Pair<Vector3f, Vector3f> aabb, Camera camera,
			Matrix4f projection, Vector2f size) {
		// Get correct handle position
		Matrix4f worldMat = new Matrix4f();
		worldMat.translate(AABBUtil.center(aabb));
		if (moveType.equals(MoveType.LOCAL_SPACE)) {
			Matrix4f tempRot = new Matrix4f(firstMatrix);
			tempRot.translate(firstMatrix.getTranslation(new Vector3f()).mul(-1));
			worldMat.mul(tempRot);
		}
		worldMat.translate(direction.mul(0.5f, new Vector3f()));

		// Visually rotate it in the right direction
		Vector3f up = new Vector3f(0, -1, 0);
		if (direction.y != 0)
			up = new Vector3f(0, 0, -direction.y);
		if (direction.z != 0)
			up = new Vector3f(0, direction.z, 0);
		Matrix4f rot = new Matrix4f().lookAlong(direction, up);
		rot.rotateX((float) Math.PI);
		worldMat.mul(rot);

		// Finalize matrices
		Matrix4f headMat = worldMat.translate(0, 0, 0.5f, new Matrix4f());

		// Scale it down
		worldMat.scale(1 / 64f, 1 / 64f, 0.9f);
		headMat.scale(0.1f, 0.1f, 0.3f);

		// Make material for handle
		boolean selected = direction.equals(hoveredHandleDirection) || direction.equals(selectedHandleDirection);
		Vector3f col = moveType.equals(MoveType.WORLD_SPACE) ? direction.absolute(new Vector3f())
				: new Vector3f(0.2f, 0.6f, 1.0f);
		baseMaterial.setColor(selected ? col.mul(0.25f) : col);

		// Draw
		shader.loadMaterial(baseMaterial);
		shader.loadTransformationMatrix(worldMat);
		Resources.MESH_CYLINDER.bind();
		Resources.MESH_CYLINDER.render();
		Resources.MESH_CYLINDER.unbind();
		shader.loadTransformationMatrix(headMat);
		Resources.MESH_CONE.bind();
		Resources.MESH_CONE.render();
		Resources.MESH_CONE.unbind();

		Vector3f arrowPos = headMat.getTranslation(new Vector3f());
		Vector2f pos = MatrixUtils.project3Dto2D(arrowPos, projection, camera.getViewMatrix().getInternal(), size);
		Vector2f mou = new Vector2f((float) AnarchyEngineClient.mouseX, (float) AnarchyEngineClient.mouseY);
		boolean closer = true;
		float d1 = arrowPos.distance(camera.getPosition().toJoml());
		if (tempPos != null) {
			float d2 = tempPos.distance(camera.getPosition().toJoml());
			closer = d1 < d2;
		}
		if (pos.distance(mou) < 200 / d1) {
			found = true;

			if (closer) {
				tempPos = arrowPos;
				hoveredHandleDirection = direction;
			}
		}
	}

	enum MoveType {
		LOCAL_SPACE, WORLD_SPACE;
	}

}
