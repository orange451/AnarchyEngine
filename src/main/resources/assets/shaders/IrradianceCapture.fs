/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec3 pass_Position;

out vec4 out_color;

uniform samplerCube envMap;

#include variable pi

void main() {
	vec3 normal = normalize(pass_Position);

	vec3 irradiance = vec3(0.0);

	vec3 up = vec3(0.0, 1.0, 0.0);
	vec3 right = cross(up, normal);
	up = cross(normal, right);

	float sampleDelta = 0.05f; // 0.025f
	uint nrSamples = 0u;
	for (float phi = 0.0; phi < 2.0 * PI; phi += sampleDelta) {
		for (float theta = 0.0; theta < 0.5 * PI; theta += sampleDelta) {
			vec3 tangentSample = vec3(sin(theta) * cos(phi), sin(theta) * sin(phi), cos(theta));
			vec3 sampleVec =
				tangentSample.x * right + tangentSample.y * up + tangentSample.z * normal;
			irradiance += texture(envMap, sampleVec).rgb * cos(theta) * sin(theta);
			nrSamples++;
		}
	}
	irradiance = PI * irradiance * (1.0 / float(nrSamples));
	out_color = vec4(irradiance, 1.0);
}