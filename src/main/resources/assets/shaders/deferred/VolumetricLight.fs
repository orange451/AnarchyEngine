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

uniform vec3 cameraPosition;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform mat4 projectionLightMatrix[4];
uniform mat4 viewLightMatrix;
uniform mat4 biasMatrix;
uniform vec3 lightPosition;
uniform sampler2DShadow shadowMap[4];

uniform int useShadows;
uniform int useVolumetricLight;

uniform float time;

#include function computeShadow

#include function random

#include variable GLOBAL

#define VOLUMETRIC_MULT 1.0

#define VOLUME_ON_SHADOW 0.2
#define VOLUME_DENSITY 0.01
#define VOLUME_COLOR vec3(0.50980392156862745098039215686275, 0.61568627450980392156862745098039,0.65098039215686274509803921568627)

void main() {
	if (useVolumetricLight == 1 && useShadows == 1) {
		vec4 position = texture(gPosition, textureCoords);
		vec3 normal = texture(gNormal, textureCoords).rgb;

		vec3 cameraToWorld = position.xyz - cameraPosition;
		float cameraToWorldDist = length(cameraToWorld);
		vec3 cameraToWorldNorm = normalize(cameraToWorld);
		vec3 L = normalize(lightPosition);
		vec3 N = normalize(normal);

		vec3 rayTrace = cameraPosition;
		float rayDist, incr = 0.2;
		float rays;
		float bias = max(0.1 * (1.0 - dot(N, L)), 0.005);
		vec3 randSample, finalTrace;
		do {
			rayTrace += cameraToWorldNorm * incr;
			incr *= 1.1;

			randSample =
				vec3(random(rayTrace.x + time), random(rayTrace.y * time), random(rayTrace.z - time)) * incr - (incr * 0.5);
			finalTrace = rayTrace + randSample;
			rayDist = length(finalTrace - cameraPosition);
			if (rayDist > cameraToWorldDist - bias)
				break;
			float curr = max(computeShadow(finalTrace), VOLUME_ON_SHADOW) * VOLUME_DENSITY;
			curr *= smoothstep(150, 0, finalTrace.z);
			rays += curr;
			if (rayDist > MAX_DISTANCE_VOLUME)
				break;
		} while (rayDist < cameraToWorldDist);
		rays = max(rays * VOLUMETRIC_MULT, 0.0);

		vec3 volumeColor = rays * VOLUME_COLOR;
		out_Color = vec4(volumeColor, 0);
	} else {
		out_Color = vec4(0.0);
	}
}