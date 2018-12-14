package luaengine.type.object.insts;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import ide.layout.windows.icons.Icons;
import luaengine.type.NumberClamp;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;

public class Camera extends Instance implements TreeViewable {

	public Camera() {
		super("Camera");

		this.defineField("Position",	Vector3.newInstance(0, 0, 0), false);
		this.defineField("LookAt",		Vector3.newInstance(0, 0, 0), false);
		this.defineField("Pitch",		LuaValue.valueOf(0), false);
		this.defineField("Yaw",			LuaValue.valueOf(0), false);
		
		this.defineField("Fov",			LuaValue.valueOf(70), false);
		this.getField("Fov").setClamp(new NumberClamp(1, 120));
		
		this.defineField("ViewMatrix",	new Matrix4(), false);
		
		this.set("LookAt", Vector3.newInstance(0, 0, 0));
		this.set("Position", Vector3.newInstance(4, 4, 4));
		
		this.getmetatable().set("Translate", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				Vector3 offset = (Vector3)arg2;
				Vector3 current1 = Camera.this.getPosition();
				Vector3 current2 = Camera.this.getLookAt();
				Vector3 t1 = (Vector3) current1.add(offset);
				Vector3 t2 = (Vector3) current2.add(offset);
				
				Camera.this.setPosition(Vector3.newInstance(t1.getX(), t1.getY(), t1.getZ()));
				Camera.this.setLookAt(Vector3.newInstance(t2.getX(), t2.getY(), t2.getZ()));
				return null;
			}
		});
		
		this.getmetatable().set("MoveTo", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				Vector3 position = (Vector3)arg2;
				Vector3 lookAt = Camera.this.getLookAt();
				Vector3 offset = (Vector3) Camera.this.getPosition().sub(position);
				Vector3 t2 = (Vector3) lookAt.add(offset);
				
				float yaw = Camera.this.getYaw();
				float pitch = Camera.this.getPitch();
				
				Camera.this.rawset("Position",position);
				Camera.this.rawset("LookAt",t2.clone());
				Camera.this.setPosition(position);
				Camera.this.setYaw(yaw);
				Camera.this.setPitch(pitch);
				return null;
			}
		});
		updateMatrix();
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public void setViewMatrix( Matrix4 matrix ) {
		if ( matrix == null || matrix.isnil() )
			return;
		
		this.set("ViewMatrix", matrix);
	}
	
	public Matrix4 getViewMatrix() {
		return (Matrix4)this.get("ViewMatrix");
	}

	public void setYaw( float yaw ) {
		this.set("Yaw", yaw);
	}
	
	public void setPitch( float pitch ) {
		this.set("Pitch", pitch);
	}

	public float getYaw() {
		return this.get("Yaw").tofloat();
	}
	
	public float getPitch() {
		return this.get("Pitch").tofloat();
	}
	
	public void setFov( float f ) {
		this.set("Fov", f);
	}
	
	public float getFov() {
		return this.get("Fov").tofloat();
	}
	
	public void setPosition( Vector3 position ) {
		this.set("Position", position.clone());
	}
	
	public Vector3 getPosition() {
		return (Vector3)this.get("Position");
	}
	
	public void setLookAt( Vector3 lookat ) {
		this.set("LookAt", lookat.clone());
	}
	
	public Vector3 getLookAt() {
		return (Vector3)this.get("LookAt");
	}

	@Override
	public void onValueUpdated( LuaValue key, LuaValue value ) {
		if ( !Game.isLoaded() )
			return;
		
		if ( !Game.workspace().get("CurrentCamera").equals(this) )
			return;

		// If viewmatrix is directly changed, change sub variables
		// Directly updates position and lookat
		// Indirectly updates pitch and yaw
		if ( key.toString().equals("ViewMatrix") ) {
			float dist = ((Vector3)this.get("LookAt")).toJoml().distance(((Vector3)this.get("Position")).toJoml());
			
			Matrix4f view = ((Matrix4)this.get("ViewMatrix")).toJoml();
			Vector3f t = view.invert(new Matrix4f()).getTranslation(new Vector3f());
			Vector3f l = new Vector3f(0,0,dist).mulProject(view);

			this.rawset("Position", Vector3.newInstance(t.x, t.x, t.z));
			this.set("LookAt", Vector3.newInstance(l.x, l.y, l.z));
		}
		
		// If lookat/position is changed, recalculate view matrix
		if ( key.toString().equals("LookAt") || key.toString().equals("Position") ) {
			Vector3f eye = ((Vector3)this.get("Position")).toJoml();
			Vector3f look = ((Vector3)this.get("LookAt")).toJoml();
			look = eye.sub(look, look);
			look.normalize();

			float pitch = (float) Math.asin(-look.z);
			if ( pitch == 0 )
				pitch = 0.000001f;
			float yaw = (float) (-Math.atan2(look.x, look.y));

			this.rawset("Pitch", pitch);
			this.rawset("Yaw", yaw);
			updateMatrix();
		}

		// Recalculate Look At Matrix if yaw/pitch are changed.
		if ( key.toString().equals("Yaw") || key.toString().equals("Pitch") ) {
			float dist = ((Vector3)this.get("LookAt")).toJoml().distance(((Vector3)this.get("Position")).toJoml());
			
			float yaw = (float) this.get("Yaw").checkdouble() - (float)Math.PI/2f;
			float pitch = (float) this.get("Pitch").checkdouble();
			Vector3f position = ((Vector3)this.get("Position")).toJoml();

			float lookX = position.x + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist;
			float lookY = position.y + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist;
			float lookZ = position.z + (float) Math.sin(pitch) * dist;

			this.rawset("LookAt", Vector3.newInstance(lookX, lookY, lookZ));
			updateMatrix();
		}
	}

	private void updateMatrix() {
		Matrix4f mat = new Matrix4f();
		mat.lookAt(((Vector3)this.rawget("Position")).toJoml(), ((Vector3)this.rawget("LookAt")).toJoml(), new Vector3f(0,0,1));
		this.rawset("ViewMatrix", new Matrix4(mat));
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_camera;
	}
}
