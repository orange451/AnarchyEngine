package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.gl.shader.BaseShader;
import engine.observer.RenderableInstance;
import ide.layout.windows.icons.Icons;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;
import luaengine.type.object.Instance;
import luaengine.type.object.PrefabRenderer;
import luaengine.type.object.TreeViewable;

public class GameObject extends Instance implements RenderableInstance,TreeViewable {
	
	public GameObject() {
		super("GameObject");
		
		this.defineField("Prefab", LuaValue.NIL, false);
		this.defineField("WorldMatrix", new Matrix4(), false);
		this.defineField("Position", Vector3.newInstance(0, 0, 0), false );
	}
	
	public void render(BaseShader shader) {
		if ( this.getParent().isnil() )
			return;
		
		if ( this.get("Prefab").isnil() )
			return;
		
		Prefab luaPrefab = (Prefab) this.rawget("Prefab");
		Matrix4 matrix = (Matrix4) this.rawget("WorldMatrix");
		PrefabRenderer prefab = luaPrefab.getPrefab();
		prefab.render(shader, matrix.toJoml());
	}
	
	/**
	 * Set the world matrix for this game object.
	 * @param matrix
	 */
	public void setWorldMatrix(Matrix4 matrix) {
		if ( matrix == null )
			matrix = new Matrix4();
		
		this.set("WorldMatrix", matrix);
	}
	
	/**
	 * Get the world matrix for this game object.
	 * @return
	 */
	public Matrix4 getWorldMatrix() {
		return (Matrix4) this.get("WorldMatrix");
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
		Instance t = this.findFirstChildOfClass("PhysicsObject");
		return t != null?(PhysicsObject)t:null;
	}
	
	@Override
	public void onDestroy() {
		//
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.toString().equals("Position") && value instanceof Vector3 ) {
			Matrix4 mat = ((Matrix4)this.rawget("WorldMatrix"));
			mat.setPosition((Vector3) value);
			
			// This is just to trigger Physics object (if it exists)
			this.set("WorldMatrix", mat);
		}
		if ( key.toString().equals("Prefab") ) {
			if ( !value.isnil() && !(value instanceof Prefab) )
				return null;
		}
		return value;
	}
	
	@Override
	protected boolean onValueGet(LuaValue key) {
		if ( key.toString().equals("Position") ) {
			this.rawset("Position", ((Matrix4)this.rawget("WorldMatrix")).getPosition());
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
			this.set("Prefab", LuaValue.NIL);
		} else {
			this.set("Prefab", p);
		}
	}

	/**
	 * Return the current prefab used to draw this object.
	 * @return
	 */
	public Prefab getPrefab() {
		LuaValue p = this.get("Prefab");
		return p.equals(LuaValue.NIL)?null:(Prefab)p;
	}
}
