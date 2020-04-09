/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

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
import engine.lua.type.object.insts.Folder;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.insts.PlayerGui;
import engine.lua.type.object.insts.PlayerScripts;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.SceneInternal;
import engine.lua.type.object.insts.script.GlobalScript;
import engine.lua.type.object.services.Assets;
import engine.lua.type.object.services.Connections;
import engine.lua.type.object.services.Core;
import engine.lua.type.object.services.Debris;
import engine.lua.type.object.services.GameECS;
import engine.lua.type.object.services.HistoryService;
import engine.lua.type.object.services.Lighting;
import engine.lua.type.object.services.Players;
import engine.lua.type.object.services.ProjectECS;
import engine.lua.type.object.services.RenderSettings;
import engine.lua.type.object.services.RunService;
import engine.lua.type.object.services.Scenes;
import engine.lua.type.object.services.ScriptService;
import engine.lua.type.object.services.SoundService;
import engine.lua.type.object.services.StarterPlayer;
import engine.lua.type.object.services.StarterPlayerGui;
import engine.lua.type.object.services.StarterPlayerScripts;
import engine.lua.type.object.services.Storage;
import engine.lua.type.object.services.UserInputService;
import engine.lua.type.object.services.Workspace;
import engine.observer.Tickable;

public class Game implements Tickable {
	private static Game game;
	private static boolean loaded;
	private static List<Runnable> runnables = Collections.synchronizedList(new ArrayList<Runnable>());
	private static List<Instance> selectedInstances = Collections.synchronizedList(new ArrayList<Instance>());
	
	private ArrayList<GameSubscriber> subscribers = new ArrayList<GameSubscriber>();
	
	public static String saveDirectory = "";
	public static String saveFile = "";
	public static boolean changes;
	public static boolean internalTesting;
	private static boolean running;
	
	public static final String VERSION = "0.7a";

	protected boolean isServer = true;
	public List<SceneInternal> unsavedScenes = new ArrayList<>();
	
	public Game() {
		game = this;
	}
	
	//local j = game.Workspace.GameObject local t = 6  for i=1,t do t = t-1 for a=-t/2,t/2 do local k = j:Clone() k.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k.Parent = game.Workspace.Cubes end end
	//local j = game.Workspace.Cubes:GetChildren() local k = 1 local t = 6 for i=1,t do t = t-1 for a=-t/2,t/2 do local o = j[k] o.PhysicsObject.Velocity = Vector3.new() o.WorldMatrix = Matrix4.new( Vector3.new( 0, a * 1.2, i ) ) k = k + 1 end end

	public static void services() {
		if ( Game.workspace() == null )
			new Workspace().forceSetParent(Game.game());
		
		if ( Game.workspace().getCurrentCamera() == null )
			Game.workspace().setCurrentCamera(new Camera());
		
		if ( Game.lighting() == null )
			new Lighting().forceSetParent(Game.game());
		
		if ( Game.players() == null )
			new Players().forceSetParent(Game.game());

		if ( Game.connections() == null )
			new Connections().forceSetParent(Game.game());
		
		if ( Game.storage() == null )
			new Storage().forceSetParent(Game.game());
		
		if ( Game.scriptService() == null )
			new ScriptService().forceSetParent(Game.game());
		
		if ( Game.starterPlayer() == null ) {
			new StarterPlayer().forceSetParent(Game.game());
		}

		if ( Game.assets() == null )
			new Assets().forceSetParent(Game.game());

		// Asset stuff
		{
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
		}
		
		if ( Game.userInputService() == null )
			new UserInputService().forceSetParent(Game.game());
		
		if ( Game.soundService() == null )
			new SoundService().forceSetParent(Game.game());

		if ( Game.runService() == null )
			new RunService().forceSetParent(Game.game());

		if ( Game.getService("Debris") == null )
			new Debris().forceSetParent(Game.game());
		
		if ( Game.historyService() == null )
			new HistoryService().forceSetParent(Game.game());
		
		if ( Game.core() == null )
			new Core().forceSetParent(Game.game());
		
		if ( Game.core().findFirstChild("RenderSettings") == null )
			new RenderSettings().forceSetParent(Game.core());
		
		if ( Game.core().findFirstChild("CameraController") == null ) {
			GlobalScript camera = new GlobalScript();
			camera.setName("CameraController");
			camera.setSourceFromFile("assets/scripts/camera.lua");
			camera.setArchivable(false);
			camera.setParent(Game.core());
			camera.setLocked(true);
		}

		if ( Game.starterPlayer().starterPlayerScripts() == null)
			new StarterPlayerScripts().forceSetParent(Game.starterPlayer());
		
		if ( Game.starterPlayer().starterPlayerGui() == null)
			new StarterPlayerGui().forceSetParent(Game.starterPlayer());
		
		// SETUP PROJECT BELOW
		{
			if ( Game.project().scenes() == null ) {
				Scenes scene = new Scenes();
				scene.forceSetParent(Game.project());
			}
			
			if ( Game.project().scenes().getChildren().size() == 0 ) {
				Scene s = new Scene();
				s.forceSetName("Primary Scene");
				s.forceSetParent(Game.project().scenes());
				
				Game.project().scenes().setStartingScene(s);
				Game.game().loadScene(s);
			}
			
			if ( Game.project().assets() == null ) {
				Instance.instanceLuaForce(Assets.class.getSimpleName()).forceSetParent(Game.project());
			}
			
			// Asset stuff
			{
				if ( Game.project().assets().findFirstChild(Assets.C_PREFABS) == null )
					Assets.newPackage(Assets.C_PREFABS, Game.project().assets());
		
				if ( Game.project().assets().findFirstChild(Assets.C_MESHES) == null )
					Assets.newPackage(Assets.C_MESHES, Game.project().assets());
		
				if ( Game.project().assets().findFirstChild(Assets.C_MATERIALS) == null )
					Assets.newPackage(Assets.C_MATERIALS, Game.project().assets());
		
				if ( Game.project().assets().findFirstChild(Assets.C_TEXTURES) == null )
					Assets.newPackage(Assets.C_TEXTURES, Game.project().assets());
		
				if ( Game.project().assets().findFirstChild(Assets.C_AUDIO) == null )
					Assets.newPackage(Assets.C_AUDIO, Game.project().assets());
			}
	
			if ( Game.project().storage() == null )
				new Storage().forceSetParent(Game.project());
			
			if ( Game.project().scriptService() == null )
				new ScriptService().forceSetParent(Game.project());
		}
	}
	

	/**
	 * Get a service by name. If it does not exist, wait until it does.
	 * @param serviceName
	 * @return
	 */
	public static Service waitForService(LuaValue serviceName) {
		Service s = getService(serviceName);
		if ( s != null )
			return s;
		
		LuaValue t = Game.game().waitForChild(serviceName, LuaValue.NIL);
		if ( t.isnil() )
			return null;
		
		if ( !(t instanceof Service) )
			return null;
		
		return (Service)t;
	}

	/**
	 * Convenience method to get a service by name via java String.
	 * @param string
	 * @return
	 */
	public static Service getService(String string) {
		return getService(LuaValue.valueOf(string));
	}
	
	/**
	 * Get a service by name.
	 * @param name
	 * @return
	 */
	public static Service getService(LuaValue name) {
		/*if ( !loaded )
			return null;*/
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

	private static final LuaValue C_GAME = LuaValue.valueOf("game");
	private static final LuaValue C_PROJECT = LuaValue.valueOf("project");
	private static final LuaValue C_WORKSPACE = LuaValue.valueOf("Workspace");
	private static final LuaValue C_LIGHTING = LuaValue.valueOf("Lighting");
	private static final LuaValue C_RUNSERVICE = LuaValue.valueOf("RunService");
	private static final LuaValue C_USERINPUTSERVICE = LuaValue.valueOf("UserInputService");
	private static final LuaValue C_SCRIPTSERVICE = LuaValue.valueOf("ScriptService");
	private static final LuaValue C_STARTERPLAYER = LuaValue.valueOf("StarterPlayer");
	private static final LuaValue C_PLAYERS = LuaValue.valueOf("Players");
	private static final LuaValue C_CONNECTIONS = LuaValue.valueOf("Connections");
	private static final LuaValue C_STORAGE = LuaValue.valueOf("Storage");
	private static final LuaValue C_SOUNDSERVICE = LuaValue.valueOf("SoundService");
	private static final LuaValue C_CORE = LuaValue.valueOf("Core");
	private static final LuaValue C_HISTORYSERVICE = LuaValue.valueOf("HistoryService");
	private static final LuaValue C_ASSETS = LuaValue.valueOf("Assets");

	public static GameECS game() {
		LuaValue game = LuaEngine.globals.get(C_GAME);
		return game.isnil()?null:(GameECS) game;
	}
	
	public static ProjectECS project() {
		LuaValue project = LuaEngine.globals.get(C_PROJECT);
		return project.isnil()?null:(ProjectECS)project;
	}
	
	public static void setGame(GameECS game) {
		LuaEngine.globals.set(C_GAME, game);
	}
	
	public static void setProject(ProjectECS project) {
		LuaEngine.globals.set(C_PROJECT, project);
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
	
	public static Storage storage() {
		return (Storage) Game.getService(C_STORAGE);
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
		
		clearServices();
		ScriptRunner.cleanup();
		
		game.unsavedScenes.clear();
		game().uniqueInstances.clear();
		
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
		
		List<Instance> moreServices = Game.project().getChildrenSafe();
		for (int i = 0; i < moreServices.size(); i++) {
			List<Instance> desc = moreServices.get(i).getDescendants();
			for (int j = 0; j < desc.size(); j++) {
				Instance inst = desc.get(j);
				inst.onDestroy();
				inst.cleanup();
			}
			moreServices.get(i).clearAllChildren();
			moreServices.get(i).destroy();
		}
	}
	
	public static void newProject() {
		Runnable r = new Runnable() {
			public void run() {
				saveDirectory = "";
				saveFile = "";
				//if ( IDE.window > -1 )
//					GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE);
				
				// Delete anything old
				unload();
				load();
				
				// Register services (new blank project)
				Game.services();
				
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
		setLoaded(true);
		
		selectedInstances.clear();
		game.gameUpdate(true);
		changes = false;
		services();
		
		System.out.println("Game Loaded");
		
		// Ensure services are loaded
		List<Service> children = Game.getServices();
		for (int i = 0; i < children.size(); i++) {
			Instance c = children.get(i);
			game().uniqueInstances.put( c.getUUID(), c);
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

	public static List<SceneInternal> getUnsavedScenes() {
		return game.unsavedScenes;
	}

	public SceneInternal getUnsavedScene(Scene currentScene) {
		for (int i = 0; i < game.unsavedScenes.size(); i++) {
			if ( game.unsavedScenes.get(i).getScene().equals(currentScene) )
				return game.unsavedScenes.get(i);
		}
		
		return null;
	}
	
	private static final LuaValue C_RUNNING = LuaValue.valueOf("Running");
	private static final LuaValue C_ISSERVER = LuaValue.valueOf("IsServer");
	
	@Override
	public void tick() {
		Game.game().rawset(C_RUNNING, LuaValue.valueOf(running));
		Game.game().rawset(C_ISSERVER, LuaValue.valueOf(Game.isServer()));
		
		if ( !isLoaded() )
			return;
		
		Workspace workspace = Game.workspace();
		if ( workspace == null )
			return;

		LuaEngine.globals.set(C_WORKSPACE, workspace);

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
		while( isSelected(inst) ) {
			r = true;
			selectedInstances.remove(inst);
		}

		if ( r ) {
			game.gameUpdate(false);
			selectionChanged().fire();
		}
	}
	
	public static void deselectAll() {
		if ( selectedInstances.size() <= 0 )
			return;
		
		selectedInstances.clear();
		game.gameUpdate(true);
		selectionChanged().fire();
	}
	
	public static boolean isSelected(Instance inst) {
		return selectedInstances.contains(inst);
	}
	
	public static boolean isDescendantSelected(Instance inst) {
		List<Instance> desc = inst.getDescendantsUnsafe();
		synchronized(desc) {
			for (Instance descendant : desc ) {
				if ( isSelected(descendant) )
					return true;
			}
		}
		
		return false;
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
		
		if ( !running )
			stoppingEvent().fire();
		
		Game.running = running;
		Game.game().rawset(C_RUNNING, LuaValue.valueOf(running));
		game.gameUpdate(true);
		
		if (running) {
			Game.runLater(()->{
				
				// Create local player
				if ( !Game.isServer() && Game.players().getLocalPlayer() == null || internalTesting ) {
					
					// Create the player
					Player p = new Player();
					p.forceSetParent(Game.players());
					
					// Player scripts folder
					Instance sc = new PlayerScripts();
					sc.forceSetParent(p);
					
					// Player gui folder
					Instance pg = new PlayerGui();
					pg.forceSetParent(p);
					
					// Simulate a client connection
					p.start();
				}
				
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
	
	/**
	 * Event that gets fired when the game is finished initializing.
	 * @return
	 */
	public static LuaEvent stoppingEvent() {
		return ((LuaEvent)LuaEngine.globals.get("game").get("Stopping"));
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
			JSONObject json = Save.getInstanceJSONRecursive(false, false, selected.get(i));
			temp.add(json);
		}
		
		// Write to file
        try (FileWriter file = new FileWriter("TEMPCOPY.json")) {
            file.write(temp.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("COpied " + temp.size() + " root instance(s)!");
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
        	Instance tempRoot = new Folder();
        	
        	JSONArray jsonObjects = (JSONArray) parser.parse(reader);
        	for (int i = 0; i < jsonObjects.size(); i++) {
        		JSONObject json = (JSONObject) jsonObjects.get(i);
        		Instance inst = Load.parseJSONInto(json, tempRoot, false);
        		if ( inst == parent ) 
        			continue;
        		
        		if ( inst != null )
        			instances.add(inst);
        		
        		// Force UUIDs to null. This is a new instance!
        		System.out.println("Resetting instance UUID! " + inst.getFullName() + " / " + inst.getUUID()); 
        		inst.setUUID(null);
        		for (Instance tin:inst.getDescendants()) {
        			tin.setUUID(null);
        		}
        		
        		// Move root instance to desired parent
        		inst.forceSetParent(parent);
        	}
        	tempRoot.destroy();
        } catch (Exception e) {
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

	/**
	 * Try to generate random UUID. First one generated SHOULD not have collisions, but wrap in a while loop to make sure!
	 * @return
	 */
	public static UUID generateUUID() {
		UUID tuid = null;
		while ( tuid == null || Game.getInstanceFromUUID(tuid) != null )
			tuid = UUID.randomUUID();
		
		return tuid;
	}
}
