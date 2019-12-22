/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec3 passPositionOut;
in vec4 clipSpaceOut;
in vec2 textureCoordsOut;
in vec3 normal;

out vec4[5] out_Color;

uniform vec3 cameraPosition;
uniform sampler2D dudv;
uniform sampler2D foamMask;
uniform float time;

void main() {
	out_Color[0] = vec4(0.0313725490196078, 0.0549019607843137, 0.1568627450980392, 0.0);
	out_Color[1] = vec4(passPositionOut.xyz, 0.0);
	out_Color[2] = vec4(normal.xyz, 0.0);
	out_Color[3] = vec4(0.0, 0, 0.0, 0.0);
	out_Color[4] = vec4(0.0);
}