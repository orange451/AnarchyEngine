package engine.lua.type.object.insts;

import java.util.ArrayList;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.lua.type.LuaConnection;
import engine.lua.type.object.Instance;

public class AnimationController extends Instance {
	
	protected ArrayList<AnimationTrack> playingAnimations;
	protected LuaConnection animationUpdator;
	
	public AnimationController() {
		super("AnimationController");
		
		this.defineField("Linked", LuaValue.NIL, true);
		
		this.playingAnimations = new ArrayList<AnimationTrack>();
		
		this.getmetatable().set("LoadAnimation", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg1) {
				if ( arg1.isnil() || !(arg1 instanceof Animation) )
					return LuaValue.NIL;
				
				AnimationTrack track = new AnimationTrack(AnimationController.this, (Animation) arg1);
				return track;
			}
		});
		
		// Handle animations
		Game.loadEvent().connect((a)->{
			animationUpdator = Game.runService().heartbeatEvent().connect((args)->{
				animate(args[0].checkdouble());
			});
		});
	}

	@Override
	public void onDestroy() {
		animationUpdator.disconnect();
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
	
	private void animate(double delta) {
		for (int i = 0; i < playingAnimations.size(); i++) {
			if ( i >= playingAnimations.size() )
				continue;
			
			AnimationTrack track = playingAnimations.get(i);
			if ( track == null )
				continue;
			
			track.update(delta);
		}
	}

	/**
	 * Plays an animation track
	 * @param track
	 */
	public void playAnimation(AnimationTrack track) {
		while ( playingAnimations.contains(track) )
			playingAnimations.remove(track);
		
		playingAnimations.add(track);
	}

}
