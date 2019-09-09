package engine.glv2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Pool.Poolable;

public class GPUTaskProfile implements Poolable {

	private GPUTaskProfile parent;

	private String name;

	private int startQuery, endQuery;

	private ArrayList<GPUTaskProfile> children;

	public GPUTaskProfile() {

		children = new ArrayList<>();

	}

	public GPUTaskProfile init(GPUTaskProfile parent, String name, int startQuery) {

		this.parent = parent;
		this.name = name;
		this.startQuery = startQuery;

		if (parent != null) {
			parent.addChild(this);
		}

		return this;
	}

	private void addChild(GPUTaskProfile profilerTask) {
		children.add(profilerTask);
	}

	public GPUTaskProfile end(int endQuery) {
		this.endQuery = endQuery;
		return parent;
	}

	public GPUTaskProfile getParent() {
		return parent;
	}

	public boolean resultsAvailable() {
		return glGetQueryObjectui(endQuery, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
	}

	public String getName() {
		return name;
	}

	public int getStartQuery() {
		return startQuery;
	}

	public int getEndQuery() {
		return endQuery;
	}

	public long getStartTime() {
		return glGetQueryObjectui64(startQuery, GL_QUERY_RESULT);
	}

	public long getEndTime() {
		return glGetQueryObjectui64(endQuery, GL_QUERY_RESULT);
	}

	public long getTimeTaken() {
		return getEndTime() - getStartTime();
	}

	public ArrayList<GPUTaskProfile> getChildren() {
		return children;
	}

	@Override
	public void reset() {
		startQuery = -1;
		endQuery = -1;
		children.clear();
	}

	public void dump() {
		dump(0);
	}

	public String dumpS() {
		return dumpS(0);
	}

	private void dump(int indentation) {
		for (int i = 0; i < indentation; i++) {
			System.out.print("    ");
		}
		System.out.println(name + " : " + getTimeTaken() / 1000 / 1000f + "ms");
		for (int i = 0; i < children.size(); i++) {
			children.get(i).dump(indentation + 1);
		}
	}

	private String dumpS(int indentation) {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < indentation; i++) {
			ret.append("--");
		}
		ret.append(name + " : " + getTimeTaken() / 1000 / 1000f + "ms\n");
		for (int i = 0; i < children.size(); i++) {
			ret.append(children.get(i).dumpS(indentation + 1));
		}
		return ret.toString();
	}
}