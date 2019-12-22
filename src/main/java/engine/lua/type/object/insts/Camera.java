/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.lib.EnumType;
import engine.lua.type.NumberClamp;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.lua.type.object.TreeViewable;
import engine.util.AABBUtil;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class Camera extends Instance implements TreeViewable,Positionable {

	private static final LuaValue C_VIEWMATRIX = LuaValue.valueOf("ViewMatrix");
	private static final LuaValue C_FOV = LuaValue.valueOf("Fov");
	private static final LuaValue C_YAW = LuaValue.valueOf("Yaw");
	private static final LuaValue C_PITCH = LuaValue.valueOf("Pitch");
	private static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	private static final LuaValue C_LOOKAT = LuaValue.valueOf("LookAt");

	private static final LuaValue C_CAMERATYPE = LuaValue.valueOf("CameraType");
	
	public Camera() {
		super("Camera");

		this.defineField(C_CAMERATYPE.toString(),  LuaValue.valueOf("Free"), false);
		this.getField(C_CAMERATYPE).setEnum(new EnumType("CameraType"));
		
		this.defineField(C_POSITION.toString(),	new Vector3(), false);
		this.defineField(C_LOOKAT.toString(),	new Vector3(), false);
		this.defineField(C_PITCH.toString(),	LuaValue.valueOf(0), false);
		this.defineField(C_YAW.toString(),		LuaValue.valueOf(0), false);
		
		this.defineField(C_FOV.toString(),LuaValue.valueOf(70), false);
		this.getField(C_FOV).setClamp(new NumberClamp(1, 120));
		
		this.defineField(C_VIEWMATRIX.toString(),	new Matrix4(), false);
		
		// Default override
		this.set(C_LOOKAT, new Vector3(0, 0, 0));
		this.set(C_POSITION, new Vector3(4, 4, 4));
		
		this.getmetatable().set("GetLookVector", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return ((Vector3) getLookAt().sub(getPosition())).getUnit();
			}
		});
		
		this.getmetatable().set("Translate", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				Camera.this.translate((Vector3)arg2);
				return null;
			}
		});
		
		this.getmetatable().set("MoveTo", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				moveTo((Vector3)arg2);
				return null;
			}
		});
		
		this.getmetatable().set("Orbit", new VarArgFunction() {
			@Override
			public LuaValue invoke(Varargs args) {
				Camera.this.orbit((Vector3)args.arg(2), (float)args.arg(3).checkdouble(), (float)args.arg(4).checkdouble(), (float)args.arg(5).checkdouble());
				return null;
			}
		});
		updateMatrix();
	}
	
	/**
	 * Moves the camera to the specified absolute position. Keeps the look-at vector relatively the same.
	 * @param position
	 */
	public void moveTo(Vector3 position) {
		Vector3 lookAt = Camera.this.getLookAt();
		Vector3 offset = (Vector3) position.sub(Camera.this.getPosition());
		Vector3 t2 = (Vector3) lookAt.add(offset);
		
		synchronized(Camera.this) {
			Camera.this.rawset(C_LOOKAT,t2);
			Camera.this.set(C_POSITION, position);
		}
	}

	/**
	 * Translates both the camera position and look at by the specified amount.
	 * @param offset
	 */
	public void translate(Vector3 offset) {
		Vector3 current1 = Camera.this.getPosition();
		Vector3 current2 = Camera.this.getLookAt();
		Vector3 t1 = (Vector3) current1.add(offset);
		Vector3 t2 = (Vector3) current2.add(offset);

		synchronized(Camera.this) {
			Camera.this.rawset(C_LOOKAT,t2);
			Camera.this.setPosition(t1);
		}
	}
	
	/**
	 * Orbits the camera around the origin by a supplied distance, pitch, and yaw.
	 * @param origin
	 * @param distance
	 * @param yaw
	 * @param f
	 */
	public void orbit(Vector3 origin, float distance, float yaw, float pitch) {
		yaw += Math.PI/2f;
		float xx = (float) (Math.cos(yaw) * Math.cos(pitch) * distance);
		float yy = (float) (Math.sin(yaw) * Math.cos(pitch) * distance);
		float zz = (float) (Math.sin(pitch) * distance);
		
		this.setPosition(new Vector3(xx, yy, zz));
		this.setLookAt(origin);
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
		
		this.set(C_VIEWMATRIX, matrix);
	}
	
	public Matrix4 getViewMatrix() {
		LuaValue viewMat = this.get(C_VIEWMATRIX);
		if ( viewMat.isnil() )
			return new Matrix4();
		
		return (Matrix4)viewMat;
	}

	public void setYaw( float yaw ) {
		this.set(C_YAW, LuaValue.valueOf(yaw));
	}
	
	public void setPitch( float pitch ) {
		this.set(C_PITCH, LuaValue.valueOf(pitch));
	}

	public float getYaw() {
		return this.get(C_YAW).tofloat();
	}
	
	public float getPitch() {
		return this.get(C_PITCH).tofloat();
	}
	
	public void setFov( float f ) {
		this.set(C_FOV, LuaValue.valueOf(f));
	}
	
	public float getFov() {
		return this.get(C_FOV).tofloat();
	}
	
	public void setPosition( Vector3 position ) {
		this.set(C_POSITION, position.clone());
	}
	
	public Vector3 getPosition() {
		if ( this.destroyed )
			return new Vector3();
		return (Vector3)this.get(C_POSITION);
	}
	
	public void setLookAt( Vector3 lookat ) {
		this.set(C_LOOKAT, lookat.clone());
	}
	
	public Vector3 getLookAt() {
		if ( this.destroyed )
			return new Vector3();
		
		return (Vector3)this.get(C_LOOKAT);
	}
	
	public Vector3 getLookVector() {
		return (Vector3) this.getLookAt().sub(this.getPosition());
	}

	@Override
	public void onValueUpdated( LuaValue key, LuaValue value ) {
		// If viewmatrix is directly changed, change sub variables
		// Directly updates position and lookat
		// Indirectly updates pitch and yaw
		if ( key.eq_b(C_VIEWMATRIX) ) {
			float dist = ((Vector3)this.get(C_LOOKAT)).getInternal().distance(((Vector3)this.get(C_POSITION)).getInternal());
			
			Matrix4f view = ((Matrix4)this.get(C_VIEWMATRIX)).getInternal();
			Vector3f t = view.invert(new Matrix4f()).getTranslation(new Vector3f());
			Vector3f l = new Vector3f(0,0,dist).mulProject(view);

			this.rawset(C_POSITION, new Vector3(t));
			this.set(C_LOOKAT, new Vector3(l));
		}
		
		// If lookat/position is changed, recalculate view matrix
		if ( key.eq_b(C_LOOKAT) || key.eq_b(C_POSITION) ) {
			Vector3f eye = ((Vector3)this.get(C_POSITION)).toJoml();
			Vector3f look = ((Vector3)this.get(C_LOOKAT)).toJoml();
			look = eye.sub(look, look);
			look.normalize();

			float pitch = (float) Math.asin(-look.z);
			if ( pitch == 0 )
				pitch = 0.000001f;
			float yaw = (float) (-Math.atan2(look.x, look.y));

			this.rawset(C_PITCH, LuaValue.valueOf(pitch));
			this.rawset(C_YAW, LuaValue.valueOf(yaw));
			updateMatrix();
		}

		// Recalculate Look At Matrix if yaw/pitch are changed.
		if ( key.eq_b(C_YAW) || key.eq_b(C_PITCH) ) {
			float dist = ((Vector3)this.get(C_LOOKAT)).getInternal().distance(((Vector3)this.get(C_POSITION)).getInternal());
			
			float yaw = (float) this.get(C_YAW).checkdouble() - (float)Math.PI/2f;
			float pitch = (float) this.get(C_PITCH).checkdouble();
			Vector3f position = ((Vector3)this.get(C_POSITION)).toJoml();

			float lookX = position.x + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist;
			float lookY = position.y + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist;
			float lookZ = position.z + (float) Math.sin(pitch) * dist;

			this.rawset(C_LOOKAT, new Vector3(lookX, lookY, lookZ));
			updateMatrix();
		}
	}

	private void updateMatrix() {
		Matrix4f mat = new Matrix4f();
		mat.lookAt(((Vector3)this.rawget(C_POSITION)).toJoml(), ((Vector3)this.rawget(C_LOOKAT)).toJoml(), new Vector3f(0,0,1));
		this.rawset(C_VIEWMATRIX, new Matrix4(mat));
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_camera;
	}

	public void setCameraType(String type) {
		this.set(C_CAMERATYPE, LuaValue.valueOf(type));
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		return AABBUtil.newAABB(new Vector3f(), new Vector3f());
	}
	
	@Override
	public Matrix4 getWorldMatrix() {
		return new Matrix4(getPosition());
	}
}
