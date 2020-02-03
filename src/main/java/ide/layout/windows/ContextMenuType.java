/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDPathSet;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.FileFormats;
import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.*;
import engine.lua.type.object.services.*;
import engine.lua.type.object.insts.script.GlobalScript;
import engine.lua.type.object.insts.script.LocalScript;
import engine.lua.type.object.insts.script.Script;
import engine.lua.type.object.insts.ui.CSS;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.windows.icons.Icons;
import lwjgui.scene.image.ImageView;

@SuppressWarnings("unchecked")
public abstract class ContextMenuType {
	public static final ContextMenuType NEW_AUDIO = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Audio");
		}

		@Override
		public String getMenuName() {
			return "New Audio Source";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_sound.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance prefab = Instance.instanceLua(AudioSource.class.getSimpleName());
			prefab.forceSetParent(instance);
			
			Game.historyService().pushChangeParent(prefab, LuaValue.NIL, prefab.getParent());
		}
	};
	
	public static final ContextMenuType IMPORT_AUDIO = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Audio");
		}

		@Override
		public String getMenuName() {
			return "Import Audio Source";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_sound.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			String path = "";
			PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
			int result = NativeFileDialog.NFD_OpenDialog(AudioSource.getFileTypes(), new File("").getAbsolutePath(), outPath);
			if (result == NativeFileDialog.NFD_OKAY) {
				path = outPath.getStringUTF8(0);
				Instance t = Game.assets().importSound(path, instance);
				File ff = new File(path);
				if ( ff.exists() ) {
					t.forceSetName(FileUtils.getFileNameWithoutExtension(ff.getName()));
				}
				Game.historyService().pushChange(t, LuaValue.valueOf("Parent"), LuaValue.NIL, t.getParent());
			} else {
				return;
			}
		}
	};
	
	public static final ContextMenuType NEW_PREFAB = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Prefabs");
		}

		@Override
		public String getMenuName() {
			return "New Prefab";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_model.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance prefab = (Prefab) Instance.instanceLua(Prefab.class.getSimpleName());
			prefab.forceSetParent(instance);
			
			Game.historyService().pushChange(prefab, LuaValue.valueOf("Parent"), LuaValue.NIL, prefab.getParent());
		}
	};

	public static final ContextMenuType IMPORT_PREFAB = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Prefabs");
		}

		@Override
		public String getMenuName() {
			return "Import Prefab";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_model.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			String path = "";
			PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
			int result = NativeFileDialog.NFD_OpenDialog(FileFormats.PREFABS, new File("").getAbsolutePath(), outPath);
			if ( result == NativeFileDialog.NFD_OKAY ) {
				path = outPath.getStringUTF8(0);
				Prefab prefab = Game.assets().importPrefab(path, instance);
				if ( prefab == null )
					return;
				
				Game.historyService().pushChange(prefab, LuaValue.valueOf("Parent"), LuaValue.NIL, prefab.getParent());
			} else {
				return;
			}
		}
	};
	
	public static final ContextMenuType NEW_TEXTURE = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Textures");
		}

		@Override
		public String getMenuName() {
			return "New Texture";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_texture.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance p = new Texture();
			p.forceSetParent(instance);
			Game.historyService().pushChange(p, LuaValue.valueOf("Parent"), LuaValue.NIL, p.getParent());
		}
	};
	public static final ContextMenuType NEW_SCENE = new ContextMenuType(Scenes.class) {
		{
			this.setMustMatchName("Scenes");
		}

		@Override
		public String getMenuName() {
			return "New Scene";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_world.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance p = new Scene();
			p.forceSetParent(instance);
			Game.historyService().pushChange(p, LuaValue.valueOf("Parent"), LuaValue.NIL, p.getParent());
		}
	};
	
	public static final ContextMenuType IMPORT_TEXTURE = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Textures");
		}

		@Override
		public String getMenuName() {
			return "Import Texture";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_texture.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			NFDPathSet outPaths = NFDPathSet.calloc();
			int result = NativeFileDialog.NFD_OpenDialogMultiple(Texture.getFileTypes(), new File("").getAbsolutePath(), outPaths);
			if ( result == NativeFileDialog.NFD_OKAY ) {
				long count = NativeFileDialog.NFD_PathSet_GetCount(outPaths);
				for (long i = 0; i < count; i++) {
					String path = NativeFileDialog.NFD_PathSet_GetPath(outPaths, i);
					Instance t = Game.assets().importTexture(path, instance);
					File ff = new File(path);
					if ( ff.exists() ) {
						t.forceSetName(FileUtils.getFileNameWithoutExtension(ff.getName()));
					}
					Game.historyService().pushChange(t, LuaValue.valueOf("Parent"), LuaValue.NIL, t.getParent());
				}
			} else {
				return;
			}
		}
	};
	
	public static final ContextMenuType NEW_MATERIAL = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Materials");
		}

		@Override
		public String getMenuName() {
			return "New Material";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_material.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance p = new Material();
			p.forceSetParent(instance);
			Game.historyService().pushChange(p, LuaValue.valueOf("Parent"), LuaValue.NIL, p.getParent());
		}
	};
	
	public static final ContextMenuType NEW_MESH = new ContextMenuType(AssetFolder.class) {
		{
			this.setMustMatchName("Meshes");
		}

		@Override
		public String getMenuName() {
			return "New Mesh";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_mesh.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			Instance p = new Mesh();
			p.forceSetParent(instance);
			Game.historyService().pushChange(p, LuaValue.valueOf("Parent"), LuaValue.NIL, p.getParent());
		}
	};
	
	public static final ContextMenuType NEW_MODEL = new ContextMenuType(Prefab.class) {

		@Override
		public String getMenuName() {
			return "Add Model";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_wat.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			((Prefab)instance).get("AddModel").invoke(LuaValue.NIL,LuaValue.NIL,LuaValue.NIL);
		}
	};
	
	public static final ContextMenuType CREATE_GAMEOBJECT = new ContextMenuType(Prefab.class) {

		@Override
		public String getMenuName() {
			return "Create GameObject";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_gameobject.getViewWithIcon(Icons.icon_plus);
		}

		@Override
		public void onClick(Instance instance) {
			GameObject g = new GameObject();
			g.setPrefab((Prefab) instance);
			g.setParent(Game.workspace());
			Game.historyService().pushChange(g, LuaValue.valueOf("Parent"), LuaValue.NIL, g.getParent());
		}
	};
	
	public static final ContextMenuType EDIT_SCRIPT = new ContextMenuType(ScriptBase.class, LocalScript.class, GlobalScript.class, Script.class) {

		@Override
		public String getMenuName() {
			return "Edit Script";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_script.getView();
		}

		@Override
		public void onClick(Instance instance) {
			IDE.openScript((ScriptBase) instance);
		}
	};
	
	public static final ContextMenuType EDIT_CSS = new ContextMenuType(CSS.class) {

		@Override
		public String getMenuName() {
			return "Edit CSS";
		}

		@Override
		public ImageView getMenuGraphic() {
			return Icons.icon_script_css.getView();
		}

		@Override
		public void onClick(Instance instance) {
			IDE.openCSS((CSS) instance);
		}
	};
	
	private static List<ContextMenuType> contextMenus;
	private String matchName;
	private List<Class<? extends Instance>> classes = new ArrayList<>();
	
	public static List<ContextMenuType> match(Instance instance) {
		List<ContextMenuType> ret = new ArrayList<>();
		
		Class<? extends Instance> clazz = instance.getClass();
		for (int i = 0; i < contextMenus.size(); i++) {
			ContextMenuType t = contextMenus.get(i);
			if ( !t.classes.contains(clazz) )
				continue;
			
			if ( t.matchName != null && !instance.getName().equals(t.matchName) )
				continue;
			
			ret.add(t);
		}
		
		return ret;
	}
	
	public ContextMenuType(Class<? extends Instance>... classes) {
		for (int i = 0; i < classes.length; i++) {
			this.classes.add(classes[i]);
		}
		
		if ( contextMenus == null )
			contextMenus = new ArrayList<ContextMenuType>();
		contextMenus.add(this);
	}
	
	public ContextMenuType setMustMatchName(String name) {
		this.matchName = name;
		return this;
	}

	public abstract String getMenuName();
	public abstract ImageView getMenuGraphic();
	public abstract void onClick(Instance instance);
}