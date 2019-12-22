/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D image;
uniform float saturation;

#include variable GLOBAL

#include function luma

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float dither(){
	return (-0.5 + rand(textureCoords)) * (1.0 / 100.0); //Noise dithering
}

void main() {
	vec3 color = texture(image, textureCoords).rgb;
	vec3 final = color;
	
	// Apply Dithering
	float lum = luma(final);
	final += dither()/2.0;
	
	// Apply saturation
	final = mix(vec3(lum), final, saturation);
	
	// Write
	out_Color.rgb = final;
	out_Color.a = 1;
}