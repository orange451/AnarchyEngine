/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders.data;

import engine.gl.objects.MaterialGL;

public class UniformMaterial extends UniformObject {

	private UniformSampler diffuseTex;
	private UniformSampler normalTex;
	private UniformSampler roughnessTex;
	private UniformSampler metallicTex;
	private UniformFloat roughness;
	private UniformFloat metallic;
	private UniformFloat reflective;
	private UniformVec3 diffuse;
	private UniformVec3 emissive;

	public UniformMaterial(String name) {
		diffuseTex = new UniformSampler(name + ".diffuseTex");
		normalTex = new UniformSampler(name + ".normalTex");
		metallicTex = new UniformSampler(name + ".metallicTex");
		roughnessTex = new UniformSampler(name + ".roughnessTex");
		roughness = new UniformFloat(name + ".roughness");
		metallic = new UniformFloat(name + ".metallic");
		reflective = new UniformFloat(name + ".reflective");
		diffuse = new UniformVec3(name + ".diffuse");
		emissive = new UniformVec3(name + ".emissive");
		super.storeUniforms(diffuseTex, normalTex, metallicTex, roughnessTex, roughness, metallic, reflective, diffuse,
				emissive);
	}

	public void loadMaterial(MaterialGL material) {
		diffuseTex.loadTexUnit(0);
		normalTex.loadTexUnit(1);
		metallicTex.loadTexUnit(2);
		roughnessTex.loadTexUnit(3);
		roughness.loadFloat(material.getRoughness());
		metallic.loadFloat(material.getMetalness());
		reflective.loadFloat(material.getReflective());
		diffuse.loadVec3(material.getColor());
		emissive.loadVec3(material.getEmissive());
	}

}
