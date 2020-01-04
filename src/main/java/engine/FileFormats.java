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

public class FileFormats {
	public static final String MESHES = "x,obj,fbx,3ds,smd,xml,dae,gltf,ms3d,blend,md5mesh";
	
	public static final String AUDIO = "wav,ogg,midi";

	public static final String TEXTURES = "png,bmp,tga,jpg,hdr";

	public static final String PREFABS = MESHES + ",md5anim";
	
	public static final String ANIMATIONS = "smd,md5anim";
}
