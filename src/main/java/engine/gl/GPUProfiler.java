package engine.gl;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Pool;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;

public class GPUProfiler {

	public static final boolean PROFILING_ENABLED = false;

	private static Pool<GPUTaskProfile> taskPool;
	private static ArrayList<Integer> queryObjects;

	private static int frameCounter;

	private static GPUTaskProfile currentTask;

	private static ArrayList<GPUTaskProfile> completedFrames;

	static {
		taskPool = new Pool<GPUTaskProfile>() {
			@Override
			protected GPUTaskProfile newObject() {
				return new GPUTaskProfile();
			}
		};
		queryObjects = new ArrayList<>();

		frameCounter = 0;

		completedFrames = new ArrayList<>();
	}

	public static void startFrame() {

		if (currentTask != null) {
			throw new IllegalStateException("Previous frame not ended properly!");
		}
		if (PROFILING_ENABLED) {
			currentTask = taskPool.obtain().init(null, "Frame " + (++frameCounter), getQuery());
		}
	}

	public static void start(String name) {
		if (PROFILING_ENABLED && currentTask != null) {
			currentTask = taskPool.obtain().init(currentTask, name, getQuery());
		}
	}

	public static void end() {
		if (PROFILING_ENABLED && currentTask != null) {
			currentTask = currentTask.end(getQuery());
		}

	}

	public static void endFrame() {

		if (PROFILING_ENABLED) {
			if (currentTask.getParent() != null) {
				throw new IllegalStateException("Error ending frame. Not all tasks finished.");
			}
			currentTask.end(getQuery());

			if (completedFrames.size() < 5) {
				completedFrames.add(currentTask);
			} else {
				recycle(currentTask);
			}

			currentTask = null;
		}
	}

	public static GPUTaskProfile getFrameResults() {
		if (completedFrames.isEmpty()) {
			return null;
		}

		GPUTaskProfile frame = completedFrames.get(0);
		if (frame.resultsAvailable()) {
			return completedFrames.remove(0);
		} else {
			return null;
		}
	}

	public static void recycle(GPUTaskProfile task) {
		queryObjects.add(task.getStartQuery());
		queryObjects.add(task.getEndQuery());

		ArrayList<GPUTaskProfile> children = task.getChildren();
		for (int i = 0; i < children.size(); i++) {
			recycle(children.get(i));
		}
		taskPool.free(task);
	}

	private static int getQuery() {
		int query;
		if (!queryObjects.isEmpty()) {
			query = queryObjects.remove(queryObjects.size() - 1);
		} else {
			query = glGenQueries();
		}

		glQueryCounter(query, GL_TIMESTAMP);

		return query;
	}
}