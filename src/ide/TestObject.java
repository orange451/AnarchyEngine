package ide;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;

import engine.gl.MaterialGL;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.observer.RenderableInstance;
import engine.util.MeshUtils;
import engine.util.TextureUtils;
import luaengine.type.object.Instance;

public class TestObject extends Instance implements RenderableInstance {
	private static BufferedMesh mesh;
	private static MaterialGL material;
	
	public TestObject() {
		super("Teapot");
		
		if ( mesh == null ) {
			mesh = MeshUtils.teapot(14);
			material = new MaterialGL().setDiffuseTexture(TextureUtils.loadRGBATexture("ide/texture1.jpg"));
		}
		
		this.defineField("Rot", LuaValue.valueOf(0.0), false);
	}
	
	public void render(BaseShader shader) {
		if ( this.getParent().equals(LuaValue.NIL) )
			return;
		
		float rot = (float) Math.toRadians(this.get("Rot").checkdouble());
		mesh.render(shader, new Matrix4f().rotateZ(rot), material);
	}
	
	@Override
	public void onDestroy() {
		//
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}
	
	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

}
