/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec2 blurTexCoords[17];

out vec4 out_Color;

uniform sampler2D composite0;

void main() {
	vec4 result = vec4(0.0);
	result += texture(composite0, blurTexCoords[0]) * 0.003924;
	result += texture(composite0, blurTexCoords[1]) * 0.008962;
	result += texture(composite0, blurTexCoords[2]) * 0.018331;
	result += texture(composite0, blurTexCoords[3]) * 0.033585;
	result += texture(composite0, blurTexCoords[4]) * 0.055119;
	result += texture(composite0, blurTexCoords[5]) * 0.081029;
	result += texture(composite0, blurTexCoords[6]) * 0.106701;
	result += texture(composite0, blurTexCoords[7]) * 0.125858;
	result += texture(composite0, blurTexCoords[8]) * 0.13298;
	result += texture(composite0, blurTexCoords[9]) * 0.125858;
	result += texture(composite0, blurTexCoords[10]) * 0.106701;
	result += texture(composite0, blurTexCoords[11]) * 0.081029;
	result += texture(composite0, blurTexCoords[12]) * 0.055119;
	result += texture(composite0, blurTexCoords[13]) * 0.033585;
	result += texture(composite0, blurTexCoords[14]) * 0.018331;
	result += texture(composite0, blurTexCoords[15]) * 0.008962;
	result += texture(composite0, blurTexCoords[16]) * 0.003924;
	out_Color = result;
}