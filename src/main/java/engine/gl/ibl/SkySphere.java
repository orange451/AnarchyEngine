/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.ibl;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.LegacyPipeline;
import engine.gl.SkyBoxDynamic;
import engine.gl.mesh.BufferedMesh;
import engine.io.Image;
import engine.util.TextureUtils;
import lwjgui.paint.Color;

public class SkySphere extends SkyBoxDynamic {
	private BufferedMesh sphere;
	private MaterialGL material;
	
	public SkySphere(Image image) {
		this( image, SKYBOX_TEXTURE_SIZE );
	}
	
	public SkySphere(Image image, int size) {
		super(new Image(Color.WHITE, size*4,size*3));
		
		material = new MaterialGL().setDiffuseTexture(TextureUtils.loadSRGBTextureFromImage(image));
		sphere = BufferedMesh.Import("engine/gl/ibl/skysphere.mesh");
	}
	
	private Matrix4f worldMatrix;

	@Override
	protected void renderGeometry(LegacyPipeline pipeline) {
		if ( worldMatrix == null ) {
			worldMatrix = new Matrix4f();
			worldMatrix.rotateX((float) (Math.PI/2f));
		}
		
		sphere.render(pipeline.shader_get(), worldMatrix, material);
	}

}
