package engine.lua.type;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import engine.lua.LuaEngine;
import engine.lua.type.object.ScriptBase;

public class ScriptData extends LuaValue implements Runnable {
	private static ScheduledExecutorService THREAD_POOL;
	private static HashMap<Thread,ScriptData> threadToScriptData;
	private static HashMap<ScriptData,Thread> scriptDataToThread;
	private static HashMap<ScriptData,Boolean> interruptedClosures;
	//private static List<LuaThread> THREAD_POOL = Collections.synchronizedList(new ArrayList<LuaThread>());

	private LuaValue function;
	private Varargs arguments;
	private ScriptBase script;
	
	private static final LuaValue C_LASTSCRIPT = LuaValue.valueOf("last_script");
	
	//private Thread thread;

	static {
		THREAD_POOL = Executors.newScheduledThreadPool(2048);
		interruptedClosures = new HashMap<ScriptData,Boolean>();
		threadToScriptData = new HashMap<Thread,ScriptData>();
		scriptDataToThread = new HashMap<ScriptData,Thread>();
	}
	
	private ScriptData(ScriptBase script) {
		this.function = null;
		this.arguments = null;
		this.script = script;
	}

	public static ScriptData create( LuaValue function, ScriptBase script, Varargs vargs ) {
		ScriptData deadThread = new ScriptData(script);
		deadThread.function = function;
		deadThread.arguments = vargs;

		return deadThread;
	}

	@Override
	public int type() {
		return LuaValue.TTHREAD;
	}

	@Override
	public String typename() {
		return "Thread";
	}

	public void start() {
		//this.thread = new Thread(this);//LuaEngine.globals,function);
		//this.thread.start();
		//this.thread.resume(LuaValue.NONE);
		if ( THREAD_POOL.isShutdown() )
			return;
		THREAD_POOL.schedule(this, 0, TimeUnit.MILLISECONDS);
	}
	
	public void interrupt() {
		interruptedClosures.put(ScriptData.this, true);
		System.out.println("Attempting to interrupt closure: " + script+"/"+function);
	}

	@Override
	public void run() {
		String fullName = "CMD";
		if ( script != null )
			fullName = script.getFullName();
		
		try {
			if ( script != null ) {
				System.out.println("Running data: " + this + " on thread: " + Thread.currentThread());
				LuaEngine.globals.set(C_LASTSCRIPT, script);
				threadToScriptData.put(Thread.currentThread(),ScriptData.this);
			}
			scriptDataToThread.put(ScriptData.this, Thread.currentThread());
			if ( function != null ) {
				function.invoke(arguments);
			}
		}catch(LuaError e) {
			
			if ( e.getMessage().contains("ScriptInterruptException") ) {
				interruptedClosures.remove(ScriptData.this);
				threadToScriptData.remove(Thread.currentThread(), ScriptData.this);
				scriptDataToThread.remove(ScriptData.this);
				LuaEngine.error( "[" + fullName + "], Interrupted. Infinite Loop?" );
				return;
			}
			
			LuaEngine.parseError(e, fullName);
		}
	}

	public static boolean isInterrupted(Thread thread) {
		return interruptedClosures.containsKey(threadToScriptData.get(thread));
	}
	
	public boolean isInterrupted() {
		return interruptedClosures.containsKey(this);
	}
	
	public static void cleanup() {
		threadToScriptData.clear();
		interruptedClosures.clear();
	}
	
	public static void shutdown() {
		THREAD_POOL.shutdown();
	}

	public static ScriptBase getScript(Thread thread) {
		ScriptData data = threadToScriptData.get(thread);
		if ( data == null ) {
			return null;
		}
		return data.script;
	}
}
