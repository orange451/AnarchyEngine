package engine.lua.type.object.insts;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.gl.shader.BaseShader;
import engine.lua.type.NumberClamp;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.TreeViewable;
import engine.observer.RenderableInstance;
import engine.util.AABBUtil;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class GameObject extends Instance implements RenderableInstance,TreeViewable,Positionable {

	private final static LuaValue C_PREFAB = LuaValue.valueOf("Prefab");
	private final static LuaValue C_WORLDMATRIX = LuaValue.valueOf("WorldMatrix");
	private final static LuaValue C_POSITION = LuaValue.valueOf("Position");
	private final static LuaValue C_TRANSPARENCY = LuaValue.valueOf("Transparency");
	
	private final static String PHYSICSOBJECT = "PhysicsObject";
	private final Matrix4f lastWorldMatrix;
	
	public GameObject() {
		super("GameObject");

		this.lastWorldMatrix = new Matrix4f();
		
		this.defineField(C_PREFAB.toString(), LuaValue.NIL, false);
		this.defineField(C_WORLDMATRIX.toString(), new Matrix4(), false);
		this.defineField(C_POSITION.toString(), new Vector3(), false );
		this.defineField(C_TRANSPARENCY.toString(), LuaValue.valueOf(0), false);
		this.getField(C_TRANSPARENCY).setClamp(new NumberClamp(0, 1));
		
		// Update the last world matrix BEFORE running physics simulation.
		InternalGameThread.runLater(()->{
			Game.runService().heartbeatEvent().connect((args)->{
				lastWorldMatrix.set(this.getWorldMatrix().getInternal());
			});
		});
	}
	
	public void render(BaseShader shader) {
		if ( this.getParent().isnil() )
			return;
		
		if ( this.get(C_PREFAB).isnil() )
			return;
		
		// Get prefab/matrix
		Prefab luaPrefab = (Prefab) this.rawget(C_PREFAB);
		Matrix4 matrix = (Matrix4) this.rawget(C_WORLDMATRIX);
		
		// Render
		PrefabRenderer prefab = luaPrefab.getPrefab();
		prefab.render(shader, matrix.getInternal());
	}
	
	/**
	 * Set the world matrix for this game object.
	 * @param matrix
	 */
	public void setWorldMatrix(Matrix4 matrix) {
		if ( matrix == null )
			matrix = new Matrix4();
		
		this.set(C_WORLDMATRIX, matrix);
	}
	
	/**
	 * Set the world matrix for this game object. Convenience function for joml.
	 * @param matrix
	 */
	public void setWorldMatrix(Matrix4f matrix) {
		this.setWorldMatrix(new Matrix4(matrix));
	}
	
	/**
	 * Get the world matrix for this game object.
	 * @return
	 */
	public Matrix4 getWorldMatrix() {
		LuaValue worldMatrix = this.get(C_WORLDMATRIX);
		if ( worldMatrix.isnil() )
			return new Matrix4();
		return (Matrix4) worldMatrix;
	}
	
	/**
	 * Returns the world matrix from the previous frame in JOML format. NOT LUA FRIENDLY.
	 * @return
	 */
	public Matrix4f getPreviousWorldMatrixJOML() {
		if ( lastWorldMatrix == null )
			return new Matrix4f();
		
		return lastWorldMatrix;
	}
	
	/**
	 * If no physics object exists within this game object, it will create one and return it.<br>
	 * If a physics object already exists, it will return the current one.
	 * @return
	 */
	public PhysicsObject attachPhysicsObject() {
		PhysicsObject r = getPhysicsObject();
		
		if ( r == null ) {
			PhysicsObject p = new PhysicsObject();
			p.forceSetParent(this);
			return p;
		} else {
			return r;
		}
	}
	
	/**
	 * Returns the physics object that is currently attached to this game object.
	 * @return
	 */
	public PhysicsObject getPhysicsObject() {
		Instance t = this.findFirstChildOfClass(PHYSICSOBJECT);
		return t != null?(PhysicsObject)t:null;
	}
	
	@Override
	public void onDestroy() {
		//
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		// Uset sets the position field
		if ( key.eq_b(C_POSITION) && value instanceof Vector3 ) {
			
			// Get current world matrix
			Matrix4 mat = ((Matrix4)this.rawget(C_WORLDMATRIX));
			
			// Update world matrix with new position
			mat.setPosition((Vector3) value);
			
			// This is just to trigger Physics object (if it exists)
			this.set(C_WORLDMATRIX, mat);
			return value;
		}
		
		// User sets the prefab field
		if ( key.eq_b(C_PREFAB) ) {
			if ( !value.isnil() && !(value instanceof Prefab) )
				return null; // Disallow
		}
		
		// Allow
		return value;
	}
	
	@Override
	protected boolean onValueGet(LuaValue key) {
		if ( key.eq_b(C_POSITION) ) {
			this.rawset(C_POSITION, ((Matrix4)this.rawget(C_WORLDMATRIX)).getPosition());
		}
		return true;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_gameobject;
	}

	/**
	 * Set the prefab used to draw this object. Default is nil.
	 * <br>
	 * <br>
	 * nil will not draw anything.
	 * @param p
	 */
	public void setPrefab(Prefab p) {
		if ( p == null ) {
			this.set(C_PREFAB, LuaValue.NIL);
		} else {
			this.set(C_PREFAB, p);
		}
	}

	/**
	 * Return the current prefab used to draw this object.
	 * @return
	 */
	public Prefab getPrefab() {
		LuaValue p = this.get(C_PREFAB);
		return p.equals(LuaValue.NIL)?null:(Prefab)p;
	}

	/**
	 * Returns the vector3 position of this object.
	 */
	public Vector3 getPosition() {
		return (Vector3) this.get(C_POSITION);
	}

	/**
	 * Sets the vector3 position of this object.
	 * If there is a physics object inside this object, that will be updated as well.
	 */
	public void setPosition(Vector3 pos) {
		this.set(C_POSITION, pos);
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		if ( this.getPrefab() == null ) {
			return AABBUtil.newAABB(getPosition().toJoml(), getPosition().toJoml());
		}
		return this.getPrefab().getAABB();
	}

	/**
	 * Returns the transparency of the object.
	 * @return
	 */
	public float getTransparency() {
		return this.get(C_TRANSPARENCY).tofloat();
	}
	
	/**
	 * Sets the transparency of the object.
	 * @param f
	 */
	public void setTransparency(float f) {
		this.set(C_TRANSPARENCY, LuaValue.valueOf(f));
	}

	protected void updateLastMatrix() {
		// TODO Auto-generated method stub
		
	}
}
