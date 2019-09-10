//
// This file is part of Light Engine
//
// Copyright (C) 2016-2019 Lux Vacuos
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

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