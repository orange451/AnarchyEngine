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

package engine.glv2.v2;

public class RenderingSettings {

	public volatile boolean shadowsEnabled = true;
	public volatile int shadowsResolution = 1024;
	public volatile int shadowsDrawDistance = 80;
	public volatile boolean volumetricLightEnabled = false;
	public volatile boolean fxaaEnabled = true;
	public volatile boolean taaEnabled = false;
	public volatile boolean vsyncEnabled = false; // TODO: Implement vsync toggle
	// TODO: Maximize and restore windows
	public volatile boolean motionBlurEnabled = false;
	public volatile boolean depthOfFieldEnabled = false;
	public volatile boolean ssrEnabled = false;
	public volatile boolean parallaxEnabled = false;
	public volatile boolean ambientOcclusionEnabled = true;
	public volatile boolean chromaticAberrationEnabled = false;
	public volatile boolean lensFlaresEnabled = false;

}
