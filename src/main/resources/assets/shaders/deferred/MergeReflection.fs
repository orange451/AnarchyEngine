/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D gMask;
uniform sampler2D baseTex;
uniform sampler2D ssrTex;
uniform sampler2D reflectionTex;

uniform bool useReflections;

#include variable MASK

void main(void) {
	vec3 image = texture(baseTex, textureCoords).rgb;
	vec4 mask = texture(gMask, textureCoords);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		vec4 reflectionData = texture(reflectionTex, textureCoords);
		vec3 specular = reflectionData.rgb * reflectionData.a;
		if (useReflections) {
            vec4 ssrData = texture(ssrTex, textureCoords);
            specular = mix(specular, ssrData.rgb, ssrData.a);
			image += specular;
		} else {
			image += specular;
		}
	}
	out_Color.rgb = image;
	out_Color.a = 0.0;
}