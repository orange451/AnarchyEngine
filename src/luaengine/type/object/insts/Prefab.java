package luaengine.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;

import engine.Game;
import engine.util.AABBUtil;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;
import luaengine.type.LuaEvent;
import luaengine.type.object.Asset;
import luaengine.type.object.Instance;
import luaengine.type.object.PrefabRenderer;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;

public class Prefab extends Asset implements TreeViewable {
	private PrefabRenderer prefab;

	public Prefab() {
		super("Prefab");
		
		this.defineField("Scale", LuaValue.valueOf(1.0), false);
		
		prefab = new PrefabRenderer(this);
		
		this.getmetatable().set("AddModel", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue mesh, LuaValue material) {
				Model m = new Model();
				m.set("Mesh", mesh);
				m.set("Material", material);
				m.forceSetParent(Prefab.this);
				return m;
			}
		});
		
		((LuaEvent)this.get("ChildAdded")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Model ) {
					((Model)arg).getMesh(); // Make sure mesh is loaded
					prefab.addModel((Model) arg);

					((Model)arg).changedEvent().connect((args) -> {
						if ( args[0].toString().equals("Mesh") ) {
							prefab.update();
						}
					});
				}
				return LuaValue.NIL;
			}
		});
		
		((LuaEvent)this.get("ChildRemoved")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Model ) {
					prefab.removeModel((Model) arg);
				}
				return LuaValue.NIL;
			}
		});
	}
	
	public Pair<Vector3f, Vector3f> getAABB() {
		if ( this.prefab.isEmpty() ) {
			return AABBUtil.newAABB(new Vector3f(-0.5f), new Vector3f(0.5f));
		}
		return this.prefab.getAABB();
	}
	
	public Model addModel(Mesh mesh, Material material) {
		Model m = new Model();
		if ( mesh != null )
			m.set("Mesh", mesh);
		if ( material != null )
			m.set("Material", material);
		m.forceSetParent(Prefab.this);
		
		return m;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		prefab.cleanup();
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_model;
	}

	public PrefabRenderer getPrefab() {
		return this.prefab;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.getService("Assets");
		if ( assets == null )
			return null;
		
		return assets.findFirstChild("Prefabs");
	}
}
