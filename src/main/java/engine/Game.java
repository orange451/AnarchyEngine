package engine;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.io.AsynchronousResourceLoader;
import engine.io.Load;
import engine.io.Save;
import engine.lua.LuaEngine;
import engine.lua.history.HistoryChange;
import engine.lua.history.HistorySnapshot;
import engine.lua.type.LuaEvent;
import engine.lua.type.ScriptRunner;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.GlobalScript;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.insts.PlayerScripts;
import engine.lua.type.object.services.Assets;
import engine.lua.type.object.services.Connections;
import engine.lua.type.object.services.Core;
import engine.lua.type.object.services.Debris;
import engine.lua.type.object.services.GameECS;
import engine.lua.type.object.services.HistoryService;
import engine.lua.type.object.services.Lighting;
import engine.lua.type.object.services.Players;
import engine.lua.type.object.services.RenderSettings;
import engine.lua.type.object.services.RunService;
import engine.lua.type.object.services.ScriptService;
import engine.lua.type.object.services.SoundService;
import engine.lua.type.object.services.StarterPlayer;
import engine.lua.type.object.services.StarterPlayerScripts;
import engine.lua.type.object.services.Storage;
import engine.lua.type.object.services.UserInputService;
import engine.lua.type.object.services.Workspace;
import engine.observer.Tickable;
import engine.util.FileUtils;
import ide.IDE;

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
	public static boolean internalTesting;
	private static boolean running;
	
	private static AtomicLong instanceCounter = new AtomicLong(0);
	
	public static final String VERSION = "0.6";

	protected boolean isServer = true;
	
	public Game() {
		game = this;
	}
	
	//local j = game.Workspace.GameObject local t = 6  for i=1,t do t = t-1 for a=-t/2,t/2 do local k = j:Clone() k.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k.Parent = game.Workspace.Cubes end end
	//local j = game.Workspace.Cubes:GetChildren() local k = 1 local t = 6 for i=1,t do t = t-1 for a=-t/2,t/2 do local o = j[k] o.PhysicsObject.Velocity = Vector3.new() o.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k = k + 1 end end

	private static void services() {
		if ( Game.getService("Workspace") == null )
			new Workspace().forceSetParent(Game.game());
		
		if ( Game.getService("Lighting") == null )
			new Lighting().forceSetParent(Game.game());
		
		if ( Game.getService("Players") == null )
			new Players().forceSetParent(Game.game());

		if ( Game.getService("Connections") == null )
			new Connections().forceSetParent(Game.game());
		
		if ( Game.getService("Storage") == null )
			new Storage().forceSetParent(Game.game());
		
		if ( Game.getService("ScriptService") == null )
			new ScriptService().forceSetParent(Game.game());
		
		if ( Game.getService("StarterPlayer") == null )
			new StarterPlayer().forceSetParent(Game.game());

		if ( Game.getService("Assets") == null )
			new Assets().forceSetParent(Game.game());

		if ( Game.getService("UserInputService") == null )
			new UserInputService().forceSetParent(Game.game());
		
		if ( Game.getService("SoundService") == null )
			new SoundService().forceSetParent(Game.game());

		if ( Game.getService("RunService") == null )
			new RunService().forceSetParent(Game.game());

		if ( Game.getService("Debris") == null )
			new Debris().forceSetParent(Game.game());
		
		if ( Game.getService("HistoryService") == null )
			new HistoryService().forceSetParent(Game.game());
		
		if ( Game.getService("Core") == null )
			new Core().forceSetParent(Game.game());
		
		if ( Game.getService("Core").findFirstChild("RenderSettings") == null )
			new RenderSettings().forceSetParent(Game.core());
		
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
		return getService(LuaValue.valueOf(string));
	}

	public static Service getService(LuaValue name) {
		if ( !loaded )
			return null;
		if ( Game.game().isnil() )
			return null;
		
		LuaValue service = Game.game().get(name);
		if ( service != null && !service.equals(LuaValue.NIL) && service instanceof Service ) {
			return (Service) service;
		}

		return null;
	}
	
	public static List<Service> getServices() {
		List<Service> servs = new ArrayList<Service>();
		
		if ( !loaded )
			return servs;
		if ( Game.game().isnil() )
			return servs;
		
		List<Instance> children = ((GameECS)Game.game()).getChildren();
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

	private static final LuaValue C_GAME = LuaValue.valueOf("game");
	private static final LuaValue C_WORKSPACE = LuaValue.valueOf("Workspace");
	private static final LuaValue C_LIGHTING = LuaValue.valueOf("Lighting");
	private static final LuaValue C_RUNSERVICE = LuaValue.valueOf("RunService");
	private static final LuaValue C_USERINPUTSERVICE = LuaValue.valueOf("UserInputService");
	private static final LuaValue C_SCRIPTSERVICE = LuaValue.valueOf("ScriptService");
	private static final LuaValue C_STARTERPLAYER = LuaValue.valueOf("StarterPlayer");
	private static final LuaValue C_PLAYERS = LuaValue.valueOf("Players");
	private static final LuaValue C_CONNECTIONS = LuaValue.valueOf("Connections");
	private static final LuaValue C_SOUNDSERVICE = LuaValue.valueOf("SoundService");
	private static final LuaValue C_CORE = LuaValue.valueOf("Core");
	private static final LuaValue C_HISTORYSERVICE = LuaValue.valueOf("HistoryService");
	private static final LuaValue C_ASSETS = LuaValue.valueOf("Assets");

	public static GameECS game() {
		LuaValue game = LuaEngine.globals.get(C_GAME);
		return game.isnil()?null:(GameECS) game;
	}
	
	public static void setGame(GameECS game) {
		LuaEngine.globals.set(C_GAME, game);
	}

	public static Workspace workspace() {
		return (Workspace) Game.getService(C_WORKSPACE);
	}

	public static Lighting lighting() {
		return (Lighting) Game.getService(C_LIGHTING);
	}
	
	public static RunService runService() {
		return (RunService) Game.getService(C_RUNSERVICE);
	}
	
	public static UserInputService userInputService() {
		return (UserInputService) Game.getService(C_USERINPUTSERVICE);
	}
	
	public static ScriptService scriptService() {
		return (ScriptService) Game.getService(C_SCRIPTSERVICE);
	}
	
	public static StarterPlayer starterPlayer() {
		return (StarterPlayer) Game.getService(C_STARTERPLAYER);
	}
	
	public static Players players() {
		return (Players) Game.getService(C_PLAYERS);
	}
	
	public static SoundService soundService() {
		return (SoundService) Game.getService(C_SOUNDSERVICE);
	}
	
	public static Connections connections() {
		return (Connections) Game.getService(C_CONNECTIONS);
	}
	
	public static Assets assets() {
		return (Assets) Game.getService(C_ASSETS);
	}
	
	public static Core core() {
		return (Core) Game.getService(C_CORE);
	}
	
	public static HistoryService historyService() {
		return (HistoryService) Game.getService(C_HISTORYSERVICE);
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
	
	/**
	 * Returns an instance from the server-generated id
	 * @param sid
	 * @return
	 */
	public static Instance getInstanceFromSID(long sid) {
		if ( sid == -1 ) 
			return null;
		return game().serverSidedInstances.get(sid);
	}
	
	/**
	 * Returns an instance from the unique id. Every instance has a unique ID.
	 * The same instance shared between applications will have different UUIDs.
	 * @param uuid
	 * @return
	 */
	public static Instance getInstanceFromUUID(UUID uuid) {
		if ( uuid == null )
			return null;
		return game().uniqueInstances.get(uuid);
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
		ScriptRunner.cleanup();
		
		loaded = false;
	}
	
	public static void clearServices() {
		List<Service> services = Game.getServices();
		for (int i = 0; i < services.size(); i++) {
			List<Instance> desc = services.get(i).getDescendants();
			for (int j = 0; j < desc.size(); j++) {
				Instance inst = desc.get(j);
				inst.onDestroy();
				inst.cleanup();
			}
			services.get(i).clearAllChildren();
		}
	}
	
	public static void newProject() {
		Runnable r = new Runnable() {
			public void run() {
				saveDirectory = "";
				saveFile = "";
				if ( IDE.window > -1 )
					GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE);
				
				// Delete anything old
				unload();
				load();
				
				// Register services (new blank project)
				Game.services();
				
				if ( Game.starterPlayer().findFirstChild("StarterPlayerScripts") == null ) {
					StarterPlayerScripts pls = new StarterPlayerScripts();
					pls.forceSetParent(Game.starterPlayer());
				}
				
				if ( Game.assets().findFirstChild(Assets.C_PREFABS) == null )
					Assets.newPackage(Assets.C_PREFABS, Game.assets());

				if ( Game.assets().findFirstChild(Assets.C_MESHES) == null )
					Assets.newPackage(Assets.C_MESHES, Game.assets());

				if ( Game.assets().findFirstChild(Assets.C_MATERIALS) == null )
					Assets.newPackage(Assets.C_MATERIALS, Game.assets());

				if ( Game.assets().findFirstChild(Assets.C_TEXTURES) == null )
					Assets.newPackage(Assets.C_TEXTURES, Game.assets());

				if ( Game.assets().findFirstChild(Assets.C_AUDIO) == null )
					Assets.newPackage(Assets.C_AUDIO, Game.assets());
				
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
		boolean didLoad = false;
		if ( !loaded ) {
			didLoad = true;
		}
		loaded = true;
		
		if ( resourceLoader == null )
			resourceLoader = new AsynchronousResourceLoader();
		
		selectedInstances.clear();
		game.gameUpdate(true);
		changes = false;
		services();
		
		System.out.println("Game Loaded");
		
		// Ensure services are loaded
		List<Service> children = Game.getServices();
		for (int i = 0; i < children.size(); i++) {
			Instance c = children.get(i);
			game().serverSidedInstances.put( c.getSID(), c);
		}
		
		// Fire load event
		if ( didLoad ) {
			loadEvent().fire();
		}
		
		resetEvent().fire();
	}

	public static void runLater(Runnable object) {
		synchronized(runnables) {
			runnables.add(object);
		}
	}
	
	private static final LuaValue C_RUNNING = LuaValue.valueOf("Running");
	private static final LuaValue C_ISSERVER = LuaValue.valueOf("IsServer");
	private int ticksNoCamera = 0;
	
	@Override
	public void tick() {
		Game.game().rawset(C_RUNNING, LuaValue.valueOf(running));
		Game.game().rawset(C_ISSERVER, LuaValue.valueOf(Game.isServer()));
		
		if ( !isLoaded() )
			return;
		
		if ( Game.workspace() == null )
			return;

		LuaEngine.globals.set(C_WORKSPACE, Game.workspace());

		// Make sure there's a camera
		if ( Game.workspace().getCurrentCamera() == null ) {
			ticksNoCamera++;
			if ( ticksNoCamera > 2 ) {
				Camera c = new Camera();
				c.setArchivable(false);
				c.forceSetParent(Game.workspace());
				Game.workspace().setCurrentCamera(c);
				ticksNoCamera = 0;
			}
		} else {
			ticksNoCamera = 0;
		}

		// Tick workspace
		Game.workspace().tick();
		
		// Tick runnables in game
		if ( running ) {
			synchronized(runnables) {
				while(runnables.size() > 0) {
					runnables.get(0).run();
					runnables.remove(0);
				}
			}
		}
	}

	public static void select(Instance inst) {
		//synchronized(selectedInstances) {
			if ( selectedInstances.contains(inst) )
				return;
			selectedInstances.add(inst);
		//}
		
		game.gameUpdate(false);
		selectionChanged().fire();
	}

	public static void deselect(Instance inst) {
		if ( inst == null )
			return;
		
		boolean r = false;
		
		//synchronized(selectedInstances) {
			while( selectedInstances.contains(inst) ) {
				r = true;
				selectedInstances.remove(inst);
			}
		//}

		if ( r ) {
			game.gameUpdate(false);
			selectionChanged().fire();
		}
	}
	
	public static void deselectAll() {
		selectedInstances.clear();
		game.gameUpdate(false);
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
				List<Instance> desc = t.getDescendants();
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
		Game.game().rawset(C_RUNNING, LuaValue.valueOf(running));
		game.gameUpdate(true);
		
		if (running) {
			// Create local player
			if ( !Game.isServer() && Game.players().getLocalPlayer() == null || internalTesting ) {
				
				// Create the player
				Player p = new Player();
				p.forceSetParent(Game.players());
				
				// Player scripts folder
				Instance sc = new PlayerScripts();
				sc.forceSetParent(p);
				
				// Simulate a client connection
				p.start();
			}
			
			Game.runLater(()->{
				startEvent().fire();
				loadEvent().fire();
			});
		}
	}
	
	/**
	 * Returns whether or not the game is currently running. A running game will have scripts executed and physics updated.
	 * @return
	 */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * Returns whether or not the game is marked as a server. A server cannot run LocalScripts, and it used to house multiple player-connections.
	 * @return
	 */
	public static boolean isServer() {
		return game.isServer;
	}

	public static long generateSID() {
		if ( !Game.isServer() && Game.running )
			return -1;
		long t = instanceCounter.incrementAndGet();
		while (game().serverSidedInstances.containsKey(t) ) {
			t = instanceCounter.incrementAndGet();
		}
		return t;
	}

	public void setServer(boolean b) {
		this.isServer = b;
	}
	
	public static String version() {
		return VERSION;
	}
	
	public static LuaEvent selectionChanged() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("SelectionChanged"));
	}
	
	/**
	 * Event that gets fired whenever a game is reset via loading a new project.
	 * @return
	 */
	public static LuaEvent resetEvent() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("ResetEvent"));
	}

	/**
	 * Event that gets fired whenever new large chunks of data are loaded. i.e. new worlds.
	 * @return
	 */
	public static LuaEvent loadEvent() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("Loaded"));
	}
	
	/**
	 * Event that gets fired when the game is finished initializing.
	 * @return
	 */
	public static LuaEvent startEvent() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("Started"));
	}

	public static void setLoaded(boolean loaded) {
		Game.loaded = loaded;
	}
	
	/**
	 * Trims a list of instances by getting rid of all descendant instances.
	 * @param instances
	 * @return
	 */
	public static List<Instance> getRootInstances(List<Instance> instances) {
		ArrayList<Instance> ret = new ArrayList<>(instances);
		
		// Find duplicate entities
		List<Instance> toRemove = new ArrayList<>();
		for (int i = 0; i < instances.size(); i++) {
			Instance t = instances.get(i);
			for (int j = 0; j < instances.size(); j++) {
				Instance p = instances.get(j);
				if ( p == t )
					continue;
				
				if ( p.isDescendantOf(t) )
					toRemove.add(p);
			}
		}
		
		// Delete duplicate entities
		while (toRemove.size() > 0 ) {
			ret.remove(toRemove.get(0));
			toRemove.remove(0);
		}
		
		return ret;
	}

	/**
	 * Copies instances to temp file. Used to communicate between programs.
	 * @param selected
	 */
	@SuppressWarnings("unchecked")
	public static void copy(List<Instance> selected) {
		JSONArray temp = new JSONArray();
		
		// Remove descendant instances
		selected = getRootInstances(selected);
		
		// Generate json for each root object
		for (int i = 0; i < selected.size(); i++) {
			JSONObject json = Save.getInstanceJSONRecursive(true, false, selected.get(i));
			temp.add(json);
		}
		
		// Write to file
        try (FileWriter file = new FileWriter("TEMPCOPY.json")) {
            file.write(temp.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
	 * Paste instances into parent instance
	 * @return
	 */
	public static List<Instance> paste(Instance parent) {
		ArrayList<Instance> instances = new ArrayList<>();
        JSONParser parser = new JSONParser();

        // Parse objects
        try (Reader reader = new FileReader("TEMPCOPY.json")) {
        	JSONArray jsonObjects = (JSONArray) parser.parse(reader);
        	for (int i = 0; i < jsonObjects.size(); i++) {
        		JSONObject json = (JSONObject) jsonObjects.get(i);
        		Instance inst = Load.parseJSON(false, json);
        		if ( inst == parent ) 
        			continue;
        		
        		if ( inst != null ) {
        			instances.add(inst);
        		}
        		inst.forceSetParent(parent);
        	}
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
        
		// History snapshot for pasting
		HistorySnapshot snapshot = new HistorySnapshot();
		{
			for (int j = 0; j < instances.size(); j++) {
				Instance root = instances.get(j);
				List<Instance> desc = root.getDescendants();
				desc.add(0, root);
				for (int i = 0; i < desc.size(); i++) {
					Instance tempInstance = desc.get(i);
					
					HistoryChange change = new HistoryChange(
							Game.historyService().getHistoryStack().getObjectReference(tempInstance),
							LuaValue.valueOf("Parent"),
							LuaValue.NIL,
							tempInstance.getParent()
					);
					snapshot.changes.add(change);
				}
			}
		}
		Game.historyService().pushChange(snapshot);
        
        return instances;
	}

	/**
	 * Duplicates the current selected objects
	 */
	public static void duplicateSelection() {
		List<Instance> instances = Game.getRootInstances(Game.selected());
		
		// History snapshot for duplicating
		HistorySnapshot snapshot = new HistorySnapshot();
		{
			for (int j = 0; j < instances.size(); j++) {
				Instance root = instances.get(j);
				if ( !root.isInstanceable() ) {
					instances.remove(j--);
					continue;
				}

				// Clone the root instance
				Instance t = root.clone();
				if ( t == null || t.isnil() )
					continue;
				
				// Try to put it in the same parent
				t.setParent(root.getParent());
				if ( !t.getParent().eq_b(root.getParent()) )
					continue;
				
				// Add snapshot change for root clone & all descendents
				List<Instance> desc = t.getDescendants();
				desc.add(0, t);
				for (int i = 0; i < desc.size(); i++) {
					Instance tempInstance = desc.get(i);
					
					HistoryChange change = new HistoryChange(
							Game.historyService().getHistoryStack().getObjectReference(tempInstance),
							LuaValue.valueOf("Parent"),
							LuaValue.NIL,
							tempInstance.getParent()
					);
					snapshot.changes.add(change);
				}
			}
		}
		Game.historyService().pushChange(snapshot);
	}

	public static void copySelection() {
		List<Instance> selected = Game.selected();
		List<Instance> rootObjects = getRootInstances(selected);
		for (int i = 0; i < rootObjects.size(); i++) {
			Instance t = rootObjects.get(i);
			if ( !t.isInstanceable() )
				rootObjects.remove(i--);
		}
		
		Game.copy(rootObjects);
	}
	
	public static void deleteSelection() {
		List<Instance> instances = Game.getRootInstances(Game.selected());
		
		// History snapshot for deleting
		HistorySnapshot snapshot = new HistorySnapshot();
		{
			for (int j = 0; j < instances.size(); j++) {
				Instance root = instances.get(j);
				if ( root.isLocked() ) {
					instances.remove(j--);
					continue;
				}
				List<Instance> desc = root.getDescendants();
				desc.add(0, root);
				for (int i = 0; i < desc.size(); i++) {
					Instance tempInstance = desc.get(i);
					
					HistoryChange change = new HistoryChange(
							Game.historyService().getHistoryStack().getObjectReference(tempInstance),
							LuaValue.valueOf("Parent"),
							tempInstance.getParent(),
							LuaValue.NIL
					);
					snapshot.changes.add(change);
				}
			}
		}
		Game.historyService().pushChange(snapshot);
		
		// Destroy parent object
		for (int i = 0; i < instances.size(); i++) {
			Game.deselect(instances.get(i));
			instances.get(i).destroy();
		}
	}

	public static void cutSelection() {
		List<Instance> instances = Game.getRootInstances(Game.selected());
		Game.copy(instances);
		deleteSelection();
	}
}
