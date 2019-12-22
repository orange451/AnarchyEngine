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
uniform float exposure;
uniform float gamma;

#include function toneMap

void main() {
	vec3 color = texture(image, textureCoords).rgb;
	vec3 final = vec3(1.0) - exp(-color * exposure);

	// Apply tone-mapping
	final = toneMap(final);

	// Apply Gamma
	vec3 whiteScale = 1.0 / toneMap(vec3(W));
	final = pow(final * whiteScale, vec3(1.0 / gamma));

	// Write
	out_Color.rgb = final;
	out_Color.a = 0.0;
}