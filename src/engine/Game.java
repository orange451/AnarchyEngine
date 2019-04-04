package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.io.AsynchronousResourceLoader;
import engine.io.Save;
import engine.lua.LuaEngine;
import engine.lua.type.LuaEvent;
import engine.lua.type.ScriptData;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.GlobalScript;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.insts.PlayerScripts;
import engine.lua.type.object.insts.PlayerScriptsStart;
import engine.lua.type.object.insts.Script;
import engine.lua.type.object.services.Assets;
import engine.lua.type.object.services.Connections;
import engine.lua.type.object.services.Core;
import engine.lua.type.object.services.Debris;
import engine.lua.type.object.services.Lighting;
import engine.lua.type.object.services.Players;
import engine.lua.type.object.services.RunService;
import engine.lua.type.object.services.ScriptService;
import engine.lua.type.object.services.Storage;
import engine.lua.type.object.services.UserInputService;
import engine.lua.type.object.services.Workspace;
import engine.observer.Tickable;

public class Game implements Tickable {
	private static Game game;
	private static boolean loaded;
	private static AsynchronousResourceLoader resourceLoader;
	private static List<Runnable> runnables = Collections.synchronizedList(new ArrayList<Runnable>());
	private static List<Instance> selectedInstances = Collections.synchronizedList(new ArrayList<Instance>());
	
	private ArrayList<GameSubscriber> subscribers = new ArrayList<GameSubscriber>();
	
	public static String saveDirectory = "";
	public static String saveFile = "";
	public static boolean changes;
	public static Instance copiedInstance;
	public static boolean internalTesting;
	private static boolean running;
	
	private static HashMap<Long,Instance> createdInstances = new HashMap<Long,Instance>();
	private static AtomicLong instanceCounter = new AtomicLong(0);

	protected boolean isServer = true;
	
	public Game() {
		game = this;
		
		// Turn on lua
		LuaEngine.initialize();
		
		// Create the game instance
		new GameInstance();
		
		// Start a new project
		changes = false;
		newProject();
	}
	
	//local j = game.Workspace.GameObject local t = 6  for i=1,t do t = t-1 for a=-t/2,t/2 do local k = j:Clone() k.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k.Parent = game.Workspace.Cubes end end
	//local j = game.Workspace.Cubes:GetChildren() local k = 1 local t = 6 for i=1,t do t = t-1 for a=-t/2,t/2 do local o = j[k] o.PhysicsObject.Velocity = Vector3.new() o.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k = k + 1 end end

	private static void services() {
		if ( Game.getService("Workspace") == null )
			new Workspace();
		
		if ( Game.getService("Lighting") == null )
			new Lighting();
		
		if ( Game.getService("Players") == null )
			new Players();

		if ( Game.getService("Connections") == null )
			new Connections();
		
		if ( Game.getService("Storage") == null )
			new Storage();
		
		if ( Game.getService("Storage").findFirstChild("StarterPlayerScripts") == null ) {
			PlayerScriptsStart pls = new PlayerScriptsStart();
			pls.forceSetParent(Game.getService("Storage"));
		}
		
		if ( Game.getService("ScriptService") == null )
			new ScriptService();

		if ( Game.getService("Assets") == null )
			new Assets();
			
		if ( Game.getService("Assets").findFirstChild("Prefabs") == null )
			((Assets)Game.getService("Assets")).newPackage("Prefabs");

		if ( Game.getService("Assets").findFirstChild("Meshes") == null )
			((Assets)Game.getService("Assets")).newPackage("Meshes");

		if ( Game.getService("Assets").findFirstChild("Materials") == null )
			((Assets)Game.getService("Assets")).newPackage("Materials");

		if ( Game.getService("Assets").findFirstChild("Textures") == null )
			((Assets)Game.getService("Assets")).newPackage("Textures");

		if ( Game.getService("Assets").findFirstChild("Audio") == null )
			((Assets)Game.getService("Assets")).newPackage("Audio");

		if ( Game.getService("UserInputService") == null )
			new UserInputService();

		if ( Game.getService("RunService") == null )
			new RunService();

		if ( Game.getService("Debris") == null )
			new Debris();
		
		if ( Game.getService("Core") == null )
			new Core();
		
		if ( Game.getService("Core").findFirstChild("CameraController") == null ) {
			GlobalScript camera = new GlobalScript();
			camera.setName("CameraController");
			camera.setSourceFromFile("engine/camera.lua");
			camera.setArchivable(false);
			camera.setParent(Game.getService("Core"));
			camera.setLocked(true);
		}
	}

	public static Service getService(String string) {
		if ( !loaded )
			return null;
		if ( Game.game().isnil() )
			return null;
		
		LuaValue service = Game.game().get(string);
		if ( service != null && !service.equals(LuaValue.NIL) && service instanceof Service ) {
			return (Service) service;
		}

		return null;
	}
	
	public static ArrayList<Service> getServices() {
		ArrayList<Service> servs = new ArrayList<Service>();
		
		if ( !loaded )
			return servs;
		if ( Game.game().isnil() )
			return servs;
		
		List<Instance> children = ((GameInstance)Game.game()).getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof Service ) {
				servs.add((Service) child);
			}
		}
		
		return servs;
	}
	
	public static AsynchronousResourceLoader resourceLoader() {
		return resourceLoader;
	}

	public static Instance game() {
		return (Instance) LuaEngine.globals.get("game");
	}

	public static Workspace workspace() {
		return (Workspace) Game.getService("Workspace");
	}

	public static Lighting lighting() {
		return (Lighting) Game.getService("Lighting");
	}
	
	public static RunService runService() {
		return (RunService) Game.getService("RunService");
	}
	
	public static UserInputService userInputService() {
		return (UserInputService) Game.getService("UserInputService");
	}
	
	public static Players players() {
		return (Players) Game.getService("Players");
	}
	
	public static Connections connections() {
		return (Connections) Game.getService("Connections");
	}
	
	public static Assets assets() {
		return (Assets) Game.getService("Assets");
	}

	public static Game getGame() {
		return game;
	}

	public void gameUpdate(boolean important) {
		for (int i = 0; i < subscribers.size(); i++) {
			subscribers.get(i).gameUpdateEvent(important);
		}
	}
	
	public void subscribe( GameSubscriber sub ) {
		synchronized(subscribers) {
			this.subscribers.add(sub);
		}
	}
	
	public void unsubscribe( GameSubscriber sub ) {
		synchronized(subscribers) {
			this.subscribers.remove(sub);
		}
	}

	public static boolean isLoaded() {
		return loaded;
	}
	
	public static Instance getInstanceFromSID(long sid) {
		if ( sid == -1 ) 
			return null;
		return createdInstances.get(sid);
	}
	
	static class GameInstance extends Instance {
		
		public GameInstance() {
			super("Game");
			LuaEngine.globals.set("game", this);
			
			// On load event
			this.rawset("Loaded", new LuaEvent());
			
			// Fields
			this.defineField("Running", LuaValue.valueOf(false), true);
			this.defineField("IsServer", LuaValue.valueOf(false), true);
			
			// GetService convenience method
			getmetatable().set("GetService", new TwoArgFunction() {
				@Override
				public LuaValue call(LuaValue arg, LuaValue arg2) {
					Service service = getService(arg2.toString());
					if ( service == null )
						return LuaValue.NIL;
					return service;
				}
			});
			
			((LuaEvent)this.rawget("DescendantRemoved")).connectLua(new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue object) {
					synchronized(createdInstances) {
						if ( object instanceof Instance ) {
							Instance inst = (Instance) object;
							long sid = inst.getSID();
							
							createdInstances.remove(sid);
						}
					}
					return LuaValue.NIL;
				}
			});
			((LuaEvent)this.rawget("DescendantAdded")).connectLua(new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue object) {
					synchronized(createdInstances) {
						if ( object instanceof Instance ) {
							Instance inst = (Instance)object;
							if ( Game.isServer() ) 
								inst.rawset("SID", Game.generateSID());
							
							long sid = inst.getSID();
							
							if ( sid != -1 ) {
								createdInstances.put(sid, inst);
							}
						}
					}
					return LuaValue.NIL;
				}
			});
			
			// LOCK HER UP
			setLocked(true);
			setInstanceable(false);
		}
		
		public String getName() {
			return this.get("Name").toString().toLowerCase();
		}

		@Override
		public void onDestroy() {
			//
		}

		@Override
		protected LuaValue onValueSet(LuaValue key, LuaValue value) {
			return null;
		}

		@Override
		protected boolean onValueGet(LuaValue key) {
			return true;
		}
	}

	public static void unload() {
		synchronized(runnables) {
			runnables.clear();
		}
		synchronized(selectedInstances) {
			selectedInstances.clear();
		}
		instanceCounter = new AtomicLong(0);
		clearServices();
		ScriptData.cleanup();
		
		loaded = false;
	}
	
	public static void clearServices() {
		ArrayList<Service> services = Game.getServices();
		for (int i = 0; i < services.size(); i++) {
			services.get(i).clearAllChildren();
		}
	}
	
	public static void newProject() {
		Runnable r = new Runnable() {
			public void run() {
				// Delete anything old
				unload();
				load();
				
				// Register services (new blank project)
				Game.services();
				
				// Create a camera object
				Camera c = new Camera();
				c.set("Parent", workspace());
				workspace().set("CurrentCamera", c);
				
				// Set changes to false, so we're not prompted with save dialog later.
				InternalGameThread.runLater(()-> {
					changes = false;
				});
			}
		};
		
		if ( changes ) {
			Save.requestSave(r);
		} else {
			r.run();
		}
	}

	public static void load() {
		loaded = true;
		
		if ( resourceLoader == null )
			resourceLoader = new AsynchronousResourceLoader();
		
		selectedInstances.clear();
		game.gameUpdate(true);
		changes = false;
		services();
		
		System.out.println("Game Loaded");
		
		List<Service> children = Game.getServices();
		for (int i = 0; i < children.size(); i++) {
			Instance c = children.get(i);
			createdInstances.put( c.getSID(), c);
		}
	}

	public static void runLater(Runnable object) {
		synchronized(runnables) {
			runnables.add(object);
		}
	}

	@Override
	public void tick() {
		Game.game().rawset("Running", LuaValue.valueOf(running));
		Game.game().rawset("IsServer", LuaValue.valueOf(Game.isServer()));
		
		if ( !isLoaded() || !running )
			return;
		
		synchronized(runnables) {
			while(runnables.size() > 0) {
				runnables.get(0).run();
				runnables.remove(0);
			}
		}
		Game.workspace().tick();
	}

	public static void select(Instance inst) {
		//synchronized(selectedInstances) {
			if ( selectedInstances.contains(inst) )
				return;
			selectedInstances.add(inst);
		//}
		
		game.gameUpdate(false);
	}

	public static void deselect(Instance inst) {
		if ( inst == null )
			return;
		
		boolean r = false;
		
		synchronized(selectedInstances) {
			while( selectedInstances.contains(inst) ) {
				r = true;
				selectedInstances.remove(inst);
			}
		}

		if ( r ) {
			game.gameUpdate(false);
		}
	}
	
	public static void deselectAll() {
		selectedInstances.clear();
		game.gameUpdate(true);
	}
	
	public static boolean isSelected(Instance inst) {
		return selectedInstances.contains(inst);
	}

	public static List<Instance> selected() {
		return selectedInstances;
	}
	
	public static List<Instance> selectedExtended() {
		List<Instance> sel = new ArrayList<Instance>(selected());
		List<Instance> extended = new ArrayList<Instance>();
		
		// Add all selected instances into extended (including descendents);
		synchronized(selectedInstances) {
			for (int i = 0; i < selectedInstances.size(); i++) {
				Instance t = selectedInstances.get(i);
				List<Instance> desc = t.getDescendents();
				extended.add(t);
				sel.remove(t);
				
				// Add all descendents in. Remove them from the remaining list if already selected
				for (int j = 0; j < desc.size(); j++) {
					Instance k = desc.get(j);
					extended.add(k);
					sel.remove(k);
				}
			}
		}
		
		// Add all remaining selected instances in.
		for (int i = 0; i < sel.size(); i++) {
			extended.add(sel.get(i));
		}
		
		return extended;
	}
	
	public static void setRunning( boolean running ) {
		if ( Game.running == running )
			return;
		
		Game.running = running;
		Game.game().rawset("Running", LuaValue.valueOf(running));
		game.gameUpdate(true);
		
		if (running) {
			// Create local player
			if ( !Game.isServer() && Game.players().getLocalPlayer() == null ) {
				
				// Create the player
				Player p = new Player();
				p.forceSetParent(Game.players());
				
				// Player scripts folder
				Instance sc = new PlayerScripts();
				sc.forceSetParent(p);
				
				// Set him as local
				new engine.lua.network.internal.ClientConnectFinishTCP().clientProcess();
			}
			
			Game.runLater(()->{
				loadEvent().fire();
			});
		}
	}
	
	public static boolean isRunning() {
		return running;
	}

	public static boolean isServer() {
		return game.isServer;
	}

	public static long generateSID() {
		if ( !Game.isServer() && Game.running )
			return -1;
		long t = instanceCounter.incrementAndGet();
		while (createdInstances.containsKey(t) ) {
			t = instanceCounter.incrementAndGet();
		}
		return t;
	}

	public void setServer(boolean b) {
		this.isServer = b;
	}
	
	public static String version() {
		return "0.5a";
	}

	public static LuaEvent loadEvent() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("Loaded"));
	}
}
