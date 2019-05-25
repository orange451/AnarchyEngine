package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.object.Instance;

public class AnimationTrack extends Instance {

	private AnimationController controller;
	
	public AnimationTrack(AnimationController animationController, Animation animation) {
		super("AnimationTrack");
		
		this.defineField("Animation", animation, true);
		this.defineField("Speed", animation.getSpeed(), false);
		this.defineField("Looped", animation.isLooped(), false);
		
		this.setArchivable(false);
		this.setInstanceable(false);
		
		this.controller = animationController;
		
		this.getmetatable().set("Play", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.NIL;
			}
		});
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
