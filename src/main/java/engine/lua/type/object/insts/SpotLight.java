package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.gl.Pipeline;
import engine.gl.light.Light;
import engine.lua.type.NumberClamp;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import engine.observer.RenderableWorld;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class SpotLight extends LightBase implements TreeViewable {

	private engine.gl.light.SpotLightInternal light;
	private IPipeline pipeline;

	private static final LuaValue C_INNERFOVSCALE = LuaValue.valueOf("InnerFOVScale");
	private static final LuaValue C_OUTERFOV = LuaValue.valueOf("OuterFOV");
	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");
	private static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");

	public SpotLight() {
		super("SpotLight");
		
		this.defineField(C_DIRECTION.toString(), new Vector3(1, 1, -1), false);
		
		this.defineField(C_RADIUS.toString(), LuaValue.valueOf(8), false);
		this.getField(C_RADIUS).setClamp(new NumberClampPreferred(0, 1024, 0, 64));
		
		this.defineField(C_OUTERFOV.toString(), LuaValue.valueOf(80), false);
		this.getField(C_OUTERFOV).setClamp(new NumberClampPreferred(0, 180, 0, 120));
		
		this.defineField(C_INNERFOVSCALE.toString(), LuaValue.valueOf(0.1), false);
		this.getField(C_INNERFOVSCALE).setClamp(new NumberClamp(0, 1));

		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_POSITION) ) {
					light.setPosition(((Vector3)value).toJoml());
				} else if ( key.eq_b(C_OUTERFOV) ) {
					light.setOuterFOV(value.tofloat());
				} else if ( key.eq_b(C_INNERFOVSCALE) ) {
					light.innerFOV = value.tofloat();
				} else if ( key.eq_b(C_RADIUS) ) {
					light.radius = value.tofloat();
				} else if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				} else if (key.eq_b(C_DIRECTION)) {
					light.direction = ((Vector3) value).toJoml();
				} else if (key.eq_b(C_SHADOWS)) {
					light.shadows = value.toboolean();
				}
			}
			
			if ( key.eq_b(C_PARENT) ) {
				onParentChange();
			}
		});

		InternalGameThread.runLater(() -> {

			Game.lighting().changedEvent().connect((args) -> {
				if (light == null)
					return;

				LuaValue key = args[0];
				LuaValue value = args[1];

				if (key.eq_b(LuaValue.valueOf("ShadowMapSize"))) {
					light.setSize(value.toint());
				}
			});
		});
	}
	
	private void onParentChange() {
		LuaValue t = this.getParent();
		if ( t.isnil() ) {
			destroyLight();
			return;
		}
		
		// Search for renderable world
		while ( t != null && !t.isnil() ) {
			if ( t instanceof RenderableWorld ) {
				IPipeline tempPipeline = Pipeline.get((RenderableWorld)t);
				if ( tempPipeline == null )
					break;
				// Light exists inside old pipeline. No need to recreate.
				if ( pipeline != null && pipeline.equals(tempPipeline) )
					break;
				
				// Destroy old light
				if ( pipeline != null )
					destroyLight();
				
				// Make new light. Return means we can live for another day!
				pipeline = tempPipeline;
				makeLight();
				return;
			}
			
			// Navigate up tree
			LuaValue temp = t;
			t = ((Instance)t).getParent();
			if ( t == temp )
				t = null;
		}
		
		// Cant make light, can't destroy light. SO NO LIGHT!
		destroyLight();
	}

	public void setOuterFOV(float fov) {
		this.set(C_OUTERFOV, LuaValue.valueOf(fov));
	}

	public void setRadius(float radius) {
		this.set(C_RADIUS, LuaValue.valueOf(radius));
	}

	@Override
	public Light getLightInternal() {
		return light;
	}

	@Override
	public void onDestroy() {
		destroyLight();
	}
	
	private void destroyLight() {
		InternalRenderThread.runLater(()->{
			if ( light == null || pipeline == null )
				return;
			
			pipeline.getSpotLightHandler().removeLight(light);
			light = null;
			pipeline = null;

			System.out.println("Destroyed light");
		});
	}
	
	private void makeLight() {		
		// Add it to pipeline
		InternalRenderThread.runLater(()->{
			if ( pipeline == null )
				return;
			
			if ( light != null )
				return;
			
			// Create light
			Vector3f pos = ((Vector3)this.get("Position")).toJoml();
			float radius = this.get(C_RADIUS).tofloat();
			float outerFOV = this.get(C_OUTERFOV).tofloat();
			float innerFOV = this.get(C_INNERFOVSCALE).tofloat();
			float intensity = this.get(C_INTENSITY).tofloat();
			Vector3f direction = ((Vector3) this.get(C_DIRECTION)).toJoml();
			light = new engine.gl.light.SpotLightInternal(direction, pos, outerFOV, innerFOV, radius, intensity);
			
			// Color it
			Color color = ((Color3)this.get("Color")).toColor();
			light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
			
			light.visible = this.get(C_VISIBLE).toboolean();
			
			pipeline.getSpotLightHandler().addLight(light);
		});
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_light_spot;
	}
}
