/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.FileFormats;
import engine.FilePath;
import engine.gl.Resources;
import engine.io.FileResource;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.resources.ResourcesManager;
import engine.tasks.TaskManager;
import ide.layout.windows.icons.Icons;

public class Texture extends AssetLoadable implements TreeViewable, FileResource {
	private engine.gl.objects.Texture texture;
	private boolean loaded;
	private String path;

	private static final LuaValue C_TEXTURELOADED = LuaValue.valueOf("TextureLoaded");
	private static final LuaValue C_SRGB = LuaValue.valueOf("SRGB");
	private static final LuaValue C_FLIPY = LuaValue.valueOf("FlipY");
	private static final LuaValue C_TEXTURES = LuaValue.valueOf("Textures");

	public Texture() {
		super("Texture");

		this.defineField(C_SRGB.toString(), LuaValue.FALSE, false);
		this.defineField(C_FLIPY.toString(), LuaValue.FALSE, false);

		this.rawset(C_TEXTURELOADED, new LuaEvent());

		// Check for texture change on create
		this.createdEvent().connect((args) -> {
			internalCheckTexture();
		});
	}
	
	private void internalCheckTexture() {
		if (this.get(C_FILEPATH).isnil() || this.get(C_FILEPATH).toString().length() == 0) {
			if (this.get(C_SRGB).checkboolean())
				texture = Resources.diffuse;
			else
				texture = Resources.diffuseMisc;
		} else {
			this.reloadFromFile();
		}
	}

	public LuaEvent textureLoadedEvent() {
		return (LuaEvent) this.rawget(C_TEXTURELOADED);
	}

	public void setSRGB(boolean b) {
		this.set(C_SRGB, LuaValue.valueOf(b));
	}

	public void setTexture(engine.gl.objects.Texture force) {
		this.texture = force;
		this.loaded = true;
	}

	public engine.gl.objects.Texture getTexture() {
		return this.texture;
	}

	private void reloadFromFile() {
		String realPath = FilePath.convertToSystem(path);
		// image = new AsynchronousImage(realPath, this.rawget(C_FLIPY).toboolean());
		if (this.get(C_SRGB).checkboolean())
			ResourcesManager.loadTexture(realPath, this.rawget(C_FLIPY).toboolean(), this::loaded);
		else
			ResourcesManager.loadTextureMisc(realPath, this.rawget(C_FLIPY).toboolean(), this::loaded);
	}

	private void loaded(engine.gl.objects.Texture texture) {
		if (this.texture != null)
			ResourcesManager.disposeTexture(this.texture);
		this.texture = texture;
		loaded = true;
		TaskManager.addTaskRenderThread(() -> {
			((LuaEvent) this.rawget(C_TEXTURELOADED)).fire();
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if (this.containsField(key)) {
			if (key.eq_b(C_FILEPATH) || key.eq_b(C_FLIPY) || key.eq_b(C_SRGB)) {
				
				// FORCE SET FLIPY AND SRGB before we continue...
				if ( key.eq_b(C_FLIPY) || key.eq_b(C_SRGB) )
					this.rawset(key, value);

				String texturePath = value.toString();
				if (!key.eq_b(C_FILEPATH))
					texturePath = this.getFilePath();

				path = texturePath;
				if (created)
					internalCheckTexture();
			}
		}
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
		if (texture != null)
			ResourcesManager.disposeTexture(texture);
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_texture;
	}

	@Override
	public LuaValue getPreferredParent() {
		return C_TEXTURES;
	}

	public static String getFileTypes() {
		return FileFormats.TEXTURES;
	}

	public boolean hasLoaded() {
		return loaded;
	}
}
