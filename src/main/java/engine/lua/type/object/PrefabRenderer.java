package engine.lua.type.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.MaterialGL;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;
import engine.util.AABBUtil;
import engine.util.Pair;

public class PrefabRenderer {
	private List<Model> models;
	private Pair<Vector3f,Vector3f> AABB;
	
	private BufferedMesh combined;
	private Prefab parent;
	
	private boolean updated;
	
	public PrefabRenderer(Prefab parent) {
		models = Collections.synchronizedList(new ArrayList<Model>());
		AABB = new Pair<Vector3f,Vector3f>(new Vector3f(), new Vector3f());
		this.parent = parent;
		
		this.parent.changedEvent().connect((args)-> {
			update();
		});
	}

	private Matrix4f tempWorldMatrix = new Matrix4f();
	
	public void render(BaseShader shader, Matrix4f worldMatrix) {
		// World matrix remains the same for all meshes inside. So you can optimize by setting once.
		Matrix4f wmat = tempWorldMatrix.set(worldMatrix);
		wmat.scale(parent.getScale());
		shader.setWorldMatrix(wmat);
		
		// Loop through each model and render
		synchronized(models) {
			for (int i = 0; i < models.size(); i++) {
				Model p = models.get(i);
				
				// Get mesh
				BufferedMesh mesh = p.getMesh();
				if ( mesh == null )
					mesh = Resources.MESH_SPHERE;
				
				// Get material
				engine.gl.MaterialGL material = Resources.MATERIAL_BLANK;
				Material ECSMat = p.getMaterial();
				if ( ECSMat != null) {
					MaterialGL GLMat = ECSMat.getMaterial();
					if ( GLMat != null ) {
						material = GLMat;
					}
				}
				
				// If transparent send to transparent queue
				if ( material.getTransparency() > 0 ) {
					Pipeline.pipeline_get().addTransparentRenderableToQueue(p, wmat, 0.0f);
					continue;
				}
				
				// Draw
				mesh.render(shader, null, material);
			}
		}
	}
	
	private void calculateAABB() {
		float scale = parent.getScale();
		Model[] temp = models.toArray(new Model[models.size()]);
		this.AABB = AABBUtil.prefabAABB(scale, temp);
	}
	
	public Pair<Vector3f,Vector3f> getAABB() {
		return this.AABB;
	}
	
	public Model getModel(int index) {
		return models.get(index);
	}
	
	public int size() {
		return this.models.size();
	}

	public void addModel(Model model) {
		synchronized(models) {
			models.add(model);
		}
		calculateAABB();
		updated = true;
	}
	
	public void update() {
		calculateAABB();
		updated = true;
	}

	public void removeModel(Model model) {
		synchronized(models) {
			models.remove(model);
		}
		calculateAABB();
		updated = true;
	}
	
	private void calculateCombined() {
		if ( combined != null ) {
			combined.cleanup();
			combined = null;
		}
		
		combined = BufferedMesh.combineMeshes(getMeshes());
		combined.scale(parent.getScale());
		updated = false;
	}

	private BufferedMesh[] getMeshes() {
		int m = 0;
		for (int i = 0; i < models.size(); i++) {
			if ( models.get(i).getMesh() != null )
				m++;
		}
		
		BufferedMesh[] meshes = new BufferedMesh[m];
		int a = 0;
		for (int i = 0; i < models.size(); i++) {
			BufferedMesh mesh = models.get(i).getMesh();
			if ( mesh == null )
				continue;
			meshes[a++] = mesh;
		}
		
		return meshes;
	}

	public BufferedMesh getCombinedMesh() {
		if ( updated )
			calculateCombined();
		
		return this.combined;
	}

	public void cleanup() {
		if ( models != null )
			models.clear();
		if ( combined != null )
			combined.cleanup();
	}

	public boolean isEmpty() {
		return models.size() == 0;
	}
}
