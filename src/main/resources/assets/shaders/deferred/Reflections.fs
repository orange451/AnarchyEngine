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
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D gDiffuse;
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gPBR;
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform samplerCube environmentCube;
uniform sampler2D brdfLUT;
uniform sampler2D pass;

uniform int useReflections;

#include variable GLOBAL

#include function positionFromDepth

#include function fresnelSchlickRoughness

#include function getDepth

#include variable MASK

#define MAX_STEPS 100
#define MAX_DIST 100.0
#define SURF_DIST 0.01

void main(void) {
	vec2 texcoord = textureCoords;
	vec4 image = texture(pass, texcoord);
	vec4 mask = texture(gMask, texcoord);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		if (useReflections == 1) {
			vec4 diffuse = texture(gDiffuse, textureCoords);
			vec2 pbr = texture(gPBR, textureCoords).rg;
			float frameDepth = texture(gDepth, textureCoords).r;
			vec3 position = positionFromDepth(textureCoords, frameDepth, inverseProjectionMatrix,
											  inverseViewMatrix);
			vec3 normal = texture(gNormal, textureCoords).rgb;

			vec3 N = normalize(normal);
			vec3 V = normalize(cameraPosition - position);
			vec3 R = reflect(-V, N);

			float roughness = pbr.r;
			float metallic = pbr.g;

			vec3 F0 = vec3(0.04);
			F0 = mix(F0, diffuse.rgb, metallic);
			vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);

			vec3 prefilteredColor =
				textureLod(environmentCube, R, roughness * MAX_REFLECTION_LOD).rgb;
			vec2 envBRDF = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
			vec3 specular = prefilteredColor * (F * envBRDF.x + envBRDF.y);

			image.rgb += specular;

			vec3 camToWorld = position - cameraPosition.xyz;
			vec3 camToWorldNorm = normalize(camToWorld);
			vec3 newPos;
			vec4 newScreen;
			vec2 newCoords;

			float depth, newDepth;

			float tmpDepth;

			float dO = 0.0;
			float odS;

			vec3 rd = normalize(reflect(camToWorldNorm, N));
			vec3 ro = position + rd * SURF_DIST;
			int i = 0;

			bool hit = false;
			vec3 newNorm;

			for (i = 0; i < MAX_STEPS; i++) {
				// Move point
				vec3 p = ro + rd * dO;

				// Convert world to screen
				newScreen = viewMatrix * vec4(p, 1);
				newScreen = projectionMatrix * newScreen;
				newScreen /= newScreen.w;
				newCoords = newScreen.xy * 0.5 + 0.5;

				if (newCoords.x < 0 || newCoords.x > 1 || newCoords.y < 0 || newCoords.y > 1)
					break;

				// Get new pos
				tmpDepth = texture(gDepth, newCoords).r;
				newPos = positionFromDepth(newCoords, tmpDepth, inverseProjectionMatrix,
										   inverseViewMatrix);
				newNorm = texture(gNormal, newCoords).rgb;

				// Calculate point and new pos depths
				depth = length(newPos - cameraPosition);
				newDepth = length(p - cameraPosition);

				// Calculate distance from newPos to point
				float dS = min(length(newPos - p), 0.5);

				float diff = newDepth - depth;

				if (diff > 0.0 && diff < 2.5) {
					if (dot(newNorm, p - ro) < 0.0)
						hit = true;
					break;
				}

				dO += dS; // Add distance to distance from origin

				if (dO > MAX_DIST)
					break;
			}
			if (hit) {
				vec3 newColor = texture(pass, newCoords).xyz;
				image.rgb -= specular;
				image.rgb += newColor * (F * envBRDF.x + envBRDF.y);
			}
		}
	}
	out_Color = image;
}