/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2;

import java.util.List;

import org.joml.Matrix4f;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.entities.SunCamera;
import engine.glv2.v2.IRenderingData;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Camera;

public interface IObjectRenderer {

	public void preProcess(List<Instance> instances);

	public void render(Camera camera, Matrix4f projection);

	public void renderReflections(IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera);

	public void renderForward(IRenderingData rd, RendererData rnd);

	public void renderShadow(SunCamera sun);

	public void dispose();

	public void end();

	public int getID();

}
