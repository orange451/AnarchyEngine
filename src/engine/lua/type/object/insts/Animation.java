package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Animation extends Instance implements TreeViewable {

	public Animation() {
		super("Animation");
		
		this.setLocked(true);
		this.setInstanceable(false);
		
		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);

		this.defineField("Speed", LuaValue.valueOf(1.0), false);
		this.defineField("Looped", LuaValue.valueOf(false), false);
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
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_film;
	}

	public float getSpeed() {
		return this.get("Speed").tofloat();
	}

	public boolean isLooped() {
		return this.get("Looped").toboolean();
	}

	/**
	 * Calculates the max time of the animation.
	 * @return
	 */
	public double getMaxTime() {
		double len = 0;
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof AnimationKeyframeSequence ) {
				AnimationKeyframeSequence seq = (AnimationKeyframeSequence)child;
				if ( seq.getTime() > len )
					len = seq.getTime();
			}
		}
		
		return len;
	}
	
	/**
	 * Returns the AnimationKeyframeSequence with the time closest to the specified time, but not greater than the specified time.
	 * @param time
	 * @return
	 */
	public AnimationKeyframeSequence getNearestSequence(double time) {
		double minTime = 0;
		AnimationKeyframeSequence ret = null;
		
		if ( children == null || children.size() == 0 )
			return null;
		
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof AnimationKeyframeSequence ) {
				AnimationKeyframeSequence seq = (AnimationKeyframeSequence)child;
				double seqTime = seq.getTime();
				if ( seqTime < time ) {
					if ( seqTime > minTime ) {
						minTime = seqTime;
						ret = seq;
					}
				}
			}
		}
		
		return ret;
	}
}
