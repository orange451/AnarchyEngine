package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.lib.LuaUtil;
import engine.lua.type.LuaEvent;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.Asset;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.util.AABBUtil;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class Prefab extends Asset implements TreeViewable {
	private PrefabRenderer prefab;
	
	private final static LuaValue C_SCALE = LuaValue.valueOf("Scale");
	private final static LuaValue C_PREFABS = LuaValue.valueOf("Prefabs");
	
	private List<Model> models;

	public Prefab() {
		super("Prefab");
		
		this.defineField(C_SCALE.toString(), LuaValue.valueOf(1.0), false);
		this.getField(C_SCALE).setClamp(new NumberClampPreferred(0, 1024, 0, 4));
		
		prefab = new PrefabRenderer(this);
		models = new ArrayList<Model>();
		
		this.getmetatable().set("GetModels", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaUtil.listToTable(getModels());
			}
		});
		
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
					
					models.add((Model)arg);
				}
				return LuaValue.NIL;
			}
		});
		
		((LuaEvent)this.get("ChildRemoved")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				if ( arg instanceof Model ) {
					prefab.removeModel((Model) arg);
					models.remove((Model) arg);
				}
				return LuaValue.NIL;
			}
		});
	}
	
	/**
	 * Returns immutable list of models attached to this prefab.
	 * @return
	 */
	public List<Model> getModels() {
		return new ArrayList<Model>(models);
	}
	
	/**
	 * Returns the scale of the prefab.
	 * @return scalar value
	 */
	public float getScale() {
		return this.get(C_SCALE).tofloat();
	}
	
	/**
	 * Sets the scale of the prefab.
	 * @param f
	 */
	public void setScale(float f) {
		this.set(C_SCALE, LuaValue.valueOf(f));
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
	public LuaValue getPreferredParent() {
		return C_PREFABS;
	}
}
