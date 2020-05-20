/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform vec2 resolution;
uniform vec3 cameraPosition;
uniform sampler2D gNormal;
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D directionalLightData;
uniform sampler2D pointLightData;
uniform sampler2D spotLightData;
uniform sampler2D areaLightData;

uniform bool useAmbientOcclusion;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function getDepth

#include function random

#include variable MASK

#define CONST_PI 3.141592653589793238
#define CONST_POWER 4.0
#define CONST_RADIUS 4.0
#define CONST_SAMPLING_DIRECTIONS 9
#define CONST_SAMPLING_STEPS 6
#define CONST_TANGENT_BIAS 0.2
#define CONST_RANGE 1

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec4 image = vec4(1.0);
	if (useAmbientOcclusion) {

		if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
			float depth = texture(gDepth, textureCoords).r;
			vec3 position =
				positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
			vec3 normal = texture(gNormal, textureCoords).rgb;
			vec3 N = normalize(normal);
			

            float total = 0.0;
            float sample_direction_increment = (CONST_PI * 2) / float(CONST_SAMPLING_DIRECTIONS);
            
            // Calculate noise
            vec2 noiseScale = resolution/4.0;
            //vec2 randVector = normalize( texture( texture_noise, pass_TextureCoord * noiseScale).xy * 2.0 - 1.0);
            //float noiseStep = texture( texture_noise, pass_TextureCoord * noiseScale ).r;
            float noiseStep = random(position.x + position.y + position.z);
            
            float offsetScale = 1.0 / (getDepth(projectionMatrix, gDepth, textureCoords) / 1000000000);
            
            for (int i = 0; i < CONST_SAMPLING_DIRECTIONS; i++) {
                float sampling_angle = (float(i) + noiseStep) * sample_direction_increment; // azimuth angle theta in the paper
                vec2 sampleDir = vec2(cos(sampling_angle), sin(sampling_angle));
                
                // Apply noise
                //sampleDir = reflect(sampleDir, randVector); // If random rotation is used, comment out this line
                
                // March along sampleDir (vector)
                float tangentAngle = acos(dot(vec3(sampleDir, 0.0), N)) - (0.5 * CONST_PI) + CONST_TANGENT_BIAS;
                float horizonAngle = tangentAngle;
                float SAMPLING_STEP = CONST_RADIUS / float(CONST_SAMPLING_STEPS);
                
                // Introduce randomized step
                float randomStep = noiseStep + 0.5;
                SAMPLING_STEP *= randomStep;
                
                // Make it smaller
                SAMPLING_STEP *= 0.1;
                
                // Scale by distance from camera
                SAMPLING_STEP *= offsetScale;
                
                // Force min-max
                SAMPLING_STEP = min(0.01, max( 0.001, SAMPLING_STEP ) ); 
                
                // Start occlusion check
                float occlusion = 0.0;
                for (int j = 0; j < CONST_SAMPLING_STEPS; j++) {
                    // march along the sampling direction and see what the horizon is
                    vec2 sampleOffset = float(j+1) * SAMPLING_STEP * sampleDir;
                    sampleOffset.x *= resolution.y/resolution.x;
                    vec2 offTex = textureCoords + sampleOffset;
                    offTex = clamp( offTex, 0.01, 0.99 );
                    
                    // reconstruct view-space position for this sample
                    vec3 off_viewPos = positionFromDepth(offTex.st , texture(gDepth, offTex.st ).r, inverseProjectionMatrix, inverseViewMatrix);
                    
                    // get difference vector
                    vec3 diff = off_viewPos.xyz - position.xyz;
                    
                    // find length
                    float diffLength = length(diff);
                    
                    // If there is an occlusion
                    float normalCheck = 1.0 - clamp( dot( tangentAngle, horizonAngle ), 0.0, 1.0 );
                    float rangeCheck  = smoothstep(0.0, 1.0, ( float(CONST_RADIUS * CONST_RANGE) * (1/resolution.x) ) / abs(diff.z));
                    
                    // find horizon angle
                    float x = diff.z / length(diff.xy);
                    float elevationAngle = x * inversesqrt(x*x + 1); // ORIGINAL, SLOWER --> atan(diff.z / length(diff.xy));
                    horizonAngle = max(horizonAngle, elevationAngle);

                    // Handle attenuation
                    float normDiff = diffLength / float(CONST_RADIUS);
                    float attenuation = 1 - normDiff*normDiff;
                    
                    // Fade out
                    float fade = 1.0 - clamp( depth * 1.25, 0.0, 1.0);
                    
                    
                    // Apply occlusion
                    occlusion += clamp(attenuation * (sin(horizonAngle) - sin(tangentAngle)), 0.0, 1.0) * normalCheck * rangeCheck * fade;
                }
                occlusion /= float(CONST_SAMPLING_STEPS);
                total += 1.0 - occlusion;
            }
            
            // Divide ao by amount of directions
            total /= CONST_SAMPLING_DIRECTIONS;
            
            // Power
            total = pow( clamp( total, 0.0, 1.0), CONST_POWER );
            // Output ssao
            image = vec4(total, total, depth, 1.0);

		}
	}
	out_Color = image;
}