package engine.lua.type.object.insts;

import java.util.ArrayList;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.lua.type.object.Instance;

public class AnimationController extends Instance {
	
	protected ArrayList<Animation> playingAnimations;
	
	public AnimationController() {
		super("AnimationController");
		
		this.defineField("Linked", LuaValue.NIL, true);
		
		this.playingAnimations = new ArrayList<Animation>();
		
		this.getmetatable().set("LoadAnimation", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg1) {
				if ( arg1.isnil() || !(arg1 instanceof Animation) )
					return LuaValue.NIL;
				
				AnimationTrack track = new AnimationTrack(AnimationController.this, (Animation) arg1);
				return track;
			}
		});
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public void onValueUpdated(LuaValue key, LuaValue value) {
		if ( key.toString().equals("Parent") ) {
			if ( value.isnil() || !(value instanceof GameObject) ) {
				this.forceset("Linked", LuaValue.NIL);
			} else {
				this.forceset("Linked", value);
			}
		}
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
