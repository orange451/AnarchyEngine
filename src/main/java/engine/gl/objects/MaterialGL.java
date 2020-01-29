/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.objects;

import org.joml.Vector3f;

import engine.gl.Resources;
import lwjgui.paint.Color;

public class MaterialGL {
	private Texture diffuseTexture;
	private Texture normalTexture;
	private Texture metalnessTexture;
	private Texture roughnessTexture;

	private float metalness = 0.2f;
	private float roughness = 0.3f;
	private float reflective = 0.1f;
	private float transparency = 0;
	
	private Vector3f emissive = new Vector3f();
	private Vector3f color = new Vector3f();
	
	public MaterialGL() {
		setDiffuseTexture(null);
		setNormalTexture(null);
		setMetalnessTexture(null);
		setRoughnessTexture(null);
		setColor(Color.LIGHT_GRAY);
		setEmissive(Color.BLACK);
	}
	
	public MaterialGL setColor(Color color) {
		this.color.set(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		return this;
	}
	
	public MaterialGL setColor(Vector3f vector) {
		this.color.set(vector);
		return this;
	}	
	
	public MaterialGL setEmissive(Color color) {
		this.emissive.set(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		return this;
	}
	
	public MaterialGL setEmissive(Vector3f vector) {
		this.emissive.set(vector);
		return this;
	}
	
	public float getReflective() {
		return this.reflective;
	}

	public float getMetalness() {
		return this.metalness;
	}
	
	public float getRoughness() {
		return this.roughness;
	}
	
	public MaterialGL setReflective(float value) {
		this.reflective = value;
		return this;
	}
	
	public MaterialGL setMetalness(float value) {
		this.metalness = value;
		return this;
	}
	
	public MaterialGL setRoughness(float value) {
		this.roughness = value;
		return this;
	}
	
	public MaterialGL setTransparency(float value) {
		this.transparency = value;
		return this;
	}
	
	public float getTransparency() {
		return this.transparency;
	}
	
	public Vector3f getEmissive() {
		return this.emissive;
	}

	public Vector3f getColor() {
		return this.color;
	}

	public MaterialGL setDiffuseTexture(Texture texture) {
		if ( texture == null ) {
			this.diffuseTexture = Resources.diffuse;
		} else {
			this.diffuseTexture = texture;
		}
		
		return this;
	}	
	
	public MaterialGL setNormalTexture(Texture texture) {
		if ( texture == null ) {
			this.normalTexture = Resources.normal;
		} else {
			this.normalTexture = texture;
		}
		
		return this;
	}
	
	public MaterialGL setMetalnessTexture(Texture texture) {
		if ( texture == null ) {
			this.metalnessTexture = Resources.metallic;
		} else {
			this.metalnessTexture = texture;
		}
		
		return this;
	}
	
	public MaterialGL setRoughnessTexture(Texture texture) {
		if ( texture == null ) {
			this.roughnessTexture = Resources.roughness;
		} else {
			this.roughnessTexture = texture;
		}
		
		return this;
	}

	public Texture getDiffuseTexture() {
		return this.diffuseTexture;
	}

	public Texture getNormalTexture() {
		return this.normalTexture;
	}

	public Texture getMetalnessTexture() {
		return this.metalnessTexture;
	}

	public Texture getRoughnessTexture() {
		return this.roughnessTexture;
	}

}
