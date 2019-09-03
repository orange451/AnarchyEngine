package ide.layout.windows;

import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.gl.Surface;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.PointLight;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Texture;
import engine.lua.type.object.services.Workspace;
import engine.observer.RenderableWorld;
import ide.layout.IdePane;
import lwjgui.geometry.Pos;
import lwjgui.gl.GenericShader;
import lwjgui.gl.Renderer;
import lwjgui.paint.Color;
import lwjgui.scene.Context;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.layout.GridView;
import lwjgui.scene.layout.OpenGLPane;
import lwjgui.scene.layout.VBox;

public class IdeMaterialViewer extends IdePane {
	
	private GridView materialBox;
	private HashMap<Material, MaterialNode> materialToNodeMap;

	public IdeMaterialViewer() {
		super("Material Viewer", true);
		
		Game.loadEvent().connect((args)->{
			init();
		});
		init();
	}
	
	private void init() {
		if ( materialBox != null ) {
			materialBox.getItems().clear();
			materialToNodeMap.clear();
		} else {
			materialToNodeMap = new HashMap<>();
			materialBox = new GridView();
			materialBox.setAlignment(Pos.TOP_LEFT);
			
			ScrollPane scroll = new ScrollPane();
			scroll.setFillToParentHeight(true);
			scroll.setFillToParentWidth(true);
			this.getChildren().add(scroll);
			scroll.setContent(materialBox);
		}
		
		System.out.println("INITIALIZING MATERIAL VIEWER");
		
		// Wait until next render...
		InternalGameThread.runLater(()-> {
			InternalRenderThread.runLater(()->{
				
				// Attach materials already in game
				List<Instance> materials = Game.assets().materials().getChildrenOfClass(Material.class.getSimpleName());
				for (int i = 0; i < materials.size(); i++) {
					attachMaterial((Material) materials.get(i));
				}
				
				// Material added
				Game.assets().materials().descendantAddedEvent().connect((materialArgs)->{
					Instance child = (Instance) materialArgs[0];
					if ( child instanceof Material ) {
						attachMaterial((Material) child);
					}
				});
				
				// Material removed
				Game.assets().materials().descendantRemovedEvent().connect((materialArgs)->{
					Instance child = (Instance) materialArgs[0];
					if ( child instanceof Material ) {
						dettachMaterial((Material) child);
					}
				});
			});
		});
	}
	
	private void dettachMaterial(Material material) {
		MaterialNode node = materialToNodeMap.get(material);
		if ( node == null ) 
			return;
		
		materialBox.getItems().remove(node);
		materialToNodeMap.remove(material);
	}

	private void attachMaterial(Material material) {
		MaterialNode p = new MaterialNode(material);
		
		materialBox.getItems().add(p);
		materialToNodeMap.put(material, p);
	}
	
	static class MaterialNode extends VBox {
		private OpenGLPane oglPane;
		private static final int size = 150;
		private Pipeline materialPipeline;
		
		public MaterialNode(Material material) {
			this.setAlignment(Pos.CENTER);
			this.setMaxWidth(size);
			

			// Create secondary world
			RenderableWorld renderableWorld = new Workspace();
			((Workspace)renderableWorld).forceSetParent(LuaValue.NIL);

			// Create secondary pipeline
			materialPipeline = new Pipeline();
			materialPipeline.setRenderableWorld(renderableWorld);
			materialPipeline.setSize(size, size);
			
			// Setup pipeline world
			{
				// Camera
				float d = 1.5f;
				Camera camera = new Camera();
				camera.setParent((LuaValue) renderableWorld);
				camera.setPosition(new Vector3(-d,-d,d/2));
				((Workspace)renderableWorld).setCurrentCamera(camera);

				// Material object
				{
					Prefab prefab = new Prefab();
					prefab.addModel(null, material);
					prefab.forceSetParent((LuaValue) renderableWorld);
	
					GameObject obj = new GameObject();
					obj.setPrefab(prefab);
					obj.setParent((LuaValue) renderableWorld);
				}
				
				// Background object
				{
					float BG_D = 20;
					Mesh BGMESH = new Mesh();
					BGMESH.block(-BG_D, -BG_D, -BG_D);
					BGMESH.forceSetParent((LuaValue) renderableWorld);
					
					Texture BGTEXTURE = new Texture();
					BGTEXTURE.setTexture(Resources.TEXTURE_DEBUG);
					BGTEXTURE.forceSetParent((LuaValue) renderableWorld);
					
					Material BGMATERIAL = new Material();
					BGMATERIAL.setMetalness(0.3f);
					BGMATERIAL.setRoughness(1.0f);
					BGMATERIAL.setDiffuseMap(BGTEXTURE);
					BGMATERIAL.forceSetParent((LuaValue) renderableWorld);
					
					Prefab BGPREFAB = new Prefab();
					BGPREFAB.addModel(BGMESH, BGMATERIAL);
					BGPREFAB.forceSetParent((LuaValue) renderableWorld);
					
					GameObject BG = new GameObject();
					BG.setPrefab(BGPREFAB);
					BG.forceSetParent((LuaValue) renderableWorld);
				}

				// Lights
				{
					int close = 4;
					int r = 18;
					int b = 6;
					int xx = 4;
					PointLight l1 = new PointLight();
					l1.setPosition(-xx, close, xx);
					l1.setRadius(r);
					l1.setIntensity(b);
					l1.setParent( renderableWorld.getInstance() );

					PointLight l2 = new PointLight();
					l2.setPosition(xx, close, xx);
					l2.setRadius(r);
					l2.setIntensity(b);
					l2.setParent(renderableWorld.getInstance() );

					PointLight l3 = new PointLight();
					l3.setPosition(-xx, close, -xx);
					l3.setRadius(r);
					l3.setIntensity(b);
					l3.setParent(renderableWorld.getInstance() );

					PointLight l4 = new PointLight();
					l4.setPosition(xx, close, -xx);
					l4.setRadius(r);
					l4.setIntensity(b);
					l4.setParent(renderableWorld.getInstance() );

					PointLight l5 = new PointLight();
					l5.setPosition(xx, -close*2, -xx);
					l5.setRadius(r);
					l5.setIntensity(b/2);
					l5.setParent(renderableWorld.getInstance() );

					PointLight l6 = new PointLight();
					l6.setPosition(-xx, -xx, xx);
					l6.setRadius(r);
					l6.setIntensity(b*0.77f);
					l6.setParent(renderableWorld.getInstance() );
				}
			}
			
			// OpenGL rendering pane
			this.oglPane = new OpenGLPane();
			this.oglPane.setPrefSize(size, size);
			this.oglPane.setFlipY(true);
			this.oglPane.setRendererCallback(new Renderer() {
				GenericShader shader;
				{
					shader = new GenericShader();
				}
				
				@Override
				public void render(Context context) {
					Surface surface = materialPipeline.getPipelineBuffer();
					surface.render(shader);
				}
			});
			this.getChildren().add(oglPane);
			
			// Label
			Label label = new Label(material.getName());
			this.getChildren().add(label);
			
			// Update on change
			material.changedEvent().connect((args)-> {
				if ( args[0].eq_b(LuaValue.valueOf("Name")) )
					label.setText(args[1].toString());
				
				InternalRenderThread.runLater(()->{
					renderMaterial();
				});
			});
			
			// Texture change in material
			material.materialUpdateEvent().connect((args)->{

				InternalRenderThread.runLater(()->{
					renderMaterial();
				});
			});
			renderMaterial();
		}

		private void renderMaterial() {
			materialPipeline.render();
		}
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}
}