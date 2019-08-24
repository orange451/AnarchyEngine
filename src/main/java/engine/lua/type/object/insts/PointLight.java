package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.GameSubscriber;
import engine.application.RenderableApplication;
import engine.gl.light.Light;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.IDE;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class PointLight extends LightBase implements TreeViewable,GameSubscriber {

	private engine.gl.light.PointLightInternal light;

	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");

	public PointLight() {
		super("PointLight");
		
		this.defineField(C_RADIUS.toString(), LuaValue.valueOf(8), false);
		this.getField(C_RADIUS).setClamp(new NumberClampPreferred(0, 1024, 0, 64));
		
		Game.getGame().subscribe(this);
		
		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_POSITION) ) {
					Vector3f pos = ((Vector3)value).toJoml();
					light.x = pos.x;
					light.y = pos.y;
					light.z = pos.z;
				} else if ( key.eq_b(C_RADIUS) ) {
					light.radius = value.tofloat();
				} else if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				}
			}
		});
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
		if ( light != null ) {
			IDE.pipeline.getGBuffer().getLightProcessor().getPointLightHandler().removeLight(light);
			light = null;
		}
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_light;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		if (!important)
			return;
		
		if ( RenderableApplication.pipeline == null || RenderableApplication.pipeline.getGBuffer() == null || RenderableApplication.pipeline.getGBuffer().getLightProcessor() == null || RenderableApplication.pipeline.getGBuffer().getLightProcessor().getPointLightHandler() == null ) {
			return;
		}
		
		if ( this.isDescendantOf(Game.workspace()) ) {
			if ( light == null ) {
				// Create light
				Vector3f pos = ((Vector3)this.get("Position")).toJoml();
				float radius = this.get(C_RADIUS).tofloat();
				float intensity = this.get("Intensity").tofloat();
				light = new engine.gl.light.PointLightInternal(pos, radius, intensity);
				
				// Color it
				Color color = ((Color3)this.get("Color")).toColor();
				light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				
				// Add it to pipeline
				RenderableApplication.pipeline.getGBuffer().getLightProcessor().getPointLightHandler().addLight(light);
			}
		} else {
			onDestroy();
		}
	}
}
