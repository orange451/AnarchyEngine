/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

import engine.gl.objects.Framebuffer;
import engine.gl.objects.IObject;
import engine.gl.objects.Texture;

public interface IDeferredPipeline extends IObject {
	
	public void process(RendererData rnd, IRenderingData rd);
	
	public void render(Framebuffer fb);
	
	public Texture getDiffuseTex();

	public Texture getMotionTex();

	public Texture getNormalTex();

	public Texture getPbrTex();

	public Texture getMaskTex();

	public Texture getDepthTex();

	public void reloadShaders();

	public void resize(int width, int height);

}
