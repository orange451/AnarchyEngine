/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.mm3d;

import org.joml.Vector4f;

import engine.gl.MaterialGL;
import lwjgui.paint.Color;

public class Mm3dMaterial {
	private int flags;
	private int textureIndex;
	private String name;
	private Vector4f ambient;
	private Vector4f diffuse;
	private Vector4f specular;
	private Vector4f emissive;
	private float shininess;
	
	private MaterialGL modelMaterial;
	
	public Mm3dMaterial(long flags, long id, String string,
						float ambientR, float ambientG, float ambientB, float ambientA,
						float diffuseR, float diffuseG, float diffuseB, float diffuseA, 
						float specularR, float specularG, float specularB, float specularA, 
						float emissiveR, float emissiveG, float emissiveB, float emissiveA,
						float shininess) {
		this.flags         = (int) flags;
		this.textureIndex  = (int) id;
		this.name          = string;
		this.ambient  = new Vector4f((ambientR + 1)/2f,  (ambientG + 1)/2f,  (ambientB + 1)/2f,  (ambientA + 1)/2f);
		//this.diffuse  = new Vector4f((diffuseR + 1)/2f,  (diffuseG + 1)/2f,  (diffuseB + 1)/2f,  (diffuseA + 1)/2f);
		this.diffuse  = new Vector4f(clamp(diffuseR, 0.0f, 1.0f), clamp(diffuseG, 0.0f, 1.0f), clamp(diffuseB, 0.0f, 1.0f), clamp(diffuseA, 0.0f, 1.0f));
		this.specular = new Vector4f((specularR + 1)/2f, (specularG + 1)/2f, (specularB + 1)/2f, (specularA + 1)/2f);
		this.emissive = new Vector4f((emissiveR + 1)/2f, (emissiveG + 1)/2f, (emissiveB + 1)/2f, (emissiveA + 1)/2f);
		
		this.modelMaterial = new MaterialGL();
		this.modelMaterial.setColor( new Color((int)(this.diffuse.x * 255), (int)(this.diffuse.y * 255), (int)(this.diffuse.z * 255)) );
	}
	
	private float clamp(float val, float f, float g) {
		return Math.max(f, Math.min(g, val));
	}

	public Vector4f getAmbient() {
		return ambient;
	}
	
	public Vector4f getDiffuse() {
		return diffuse;
	}
	
	public Vector4f getSpecular() {
		return specular;
	}
	
	public Vector4f getEmissive() {
		return emissive;
	}
	
	public float getShininess() {
		return shininess;
	}
	
	public String getName() {
		return name;
	}
	
	public int getTextureIndex() {
		return textureIndex;
	}

	public MaterialGL getModelMaterial() {
		return modelMaterial;
	}

}
