/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#variable pi
#define PI 3.14159265359
#end

#function random
//
//	by Spatial
//	05 July 2013
//

// A single iteration of Bob Jenkins' One-At-A-Time hashing algorithm.
uint hash(uint x) {
	x += (x << 10u);
	x ^= (x >> 6u);
	x += (x << 3u);
	x ^= (x >> 11u);
	x += (x << 15u);
	return x;
}

// Compound versions of the hashing algorithm I whipped together.
uint hash(uvec2 v) {
	return hash(v.x ^ hash(v.y));
}
uint hash(uvec3 v) {
	return hash(v.x ^ hash(v.y) ^ hash(v.z));
}
uint hash(uvec4 v) {
	return hash(v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w));
}

// Construct a float with half-open range [0:1] using low 23 bits.
// All zeroes yields 0.0, all ones yields the next smallest representable value below 1.0.
float floatConstruct(uint m) {
	const uint ieeeMantissa = 0x007FFFFFu; // binary32 mantissa bitmask
	const uint ieeeOne = 0x3F800000u;	  // 1.0 in IEEE binary32

	m &= ieeeMantissa; // Keep only mantissa bits (fractional part)
	m |= ieeeOne;	  // Add fractional part to 1.0

	float f = uintBitsToFloat(m); // Range [1:2]
	return f - 1.0;				  // Range [0:1]
}

// Pseudo-random value in half-open range [0:1].
float random(float x) {
	return floatConstruct(hash(floatBitsToUint(x)));
}
float random(vec2 v) {
	return floatConstruct(hash(floatBitsToUint(v)));
}
float random(vec3 v) {
	return floatConstruct(hash(floatBitsToUint(v)));
}
float random(vec4 v) {
	return floatConstruct(hash(floatBitsToUint(v)));
}
#end

#function goldNoise
// Gold Noise ©2017-2018 dcerisano@standard3d.com

const float PHI = 1.61803398874989484820459 * 00000.1; // Golden Ratio
const float PI = 3.14159265358979323846264 * 00000.1;  // PI
const float SQ2 = 1.41421356237309504880169 * 10000.0; // Square Root of Two

float gold_noise(in vec2 coordinate, in float seed) {
	return fract(sin(dot(coordinate * (seed + PHI), vec2(PHI, PI))) * SQ2);
}
#end

#function noise
float fbm_random(in vec2 st) {
	return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Based on Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float fbm_noise(in vec2 st) {
	vec2 i = floor(st);
	vec2 f = fract(st);

	// Four corners in 2D of a tile
	float a = fbm_random(i);
	float b = fbm_random(i + vec2(1.0, 0.0));
	float c = fbm_random(i + vec2(0.0, 1.0));
	float d = fbm_random(i + vec2(1.0, 1.0));

	vec2 u = f * f * (3.0 - 2.0 * f);

	return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

#define OCTAVES 6
float fbm(in vec2 st) {
	// Initial values
	float value = 0.0;
	float amplitude = .5;
	float frequency = 0.;
	//
	// Loop of octaves
	for (int i = 0; i < OCTAVES; i++) {
		value += amplitude * fbm_noise(st);
		st *= 2.;
		amplitude *= .5;
	}
	return value;
}
#end

#function randomS
/*
  http://amindforeverprogramming.blogspot.de/2013/07/random-floats-in-glsl-330.html?showComment=1507064059398#c5427444543794991219
*/
uint hash3(uint x, uint y, uint z) {
	x += x >> 11;
	x ^= x << 7;
	x += y;
	x ^= x << 3;
	x += z ^ (x >> 14);
	x ^= x << 6;
	x += x >> 15;
	x ^= x << 5;
	x += x >> 12;
	x ^= x << 9;
	return x;
}

/*
  Generate a random value in [-1..+1)
*/
float randomS(vec2 pos, float time) {
	uint mantissaMask = 0x007FFFFFu;
	uint one = 0x3F800000u;
	uvec3 u = floatBitsToUint(vec3(pos, time));
	uint h = hash3(u.x, u.y, u.z);
	return uintBitsToFloat((h & mantissaMask) | one) - 1.0;
}
#end

#function cosWeightedRandomHemisphereDirection
vec3 cosWeightedRandomHemisphereDirection(vec3 n, float rand1, float rand2) {
	float Xi1 = rand1;
	float Xi2 = rand2;

	float theta = acos(sqrt(1.0 - Xi1));
	float phi = 2.0 * PI * Xi2;

	float xs = sin(theta) * cos(phi);
	float ys = cos(theta);
	float zs = sin(theta) * sin(phi);

	vec3 y = n;
	vec3 h = y;
	if (abs(h.x) <= abs(h.y) && abs(h.x) <= abs(h.z))
		h.x = 1.0;
	else if (abs(h.y) <= abs(h.x) && abs(h.y) <= abs(h.z))
		h.y = 1.0;
	else
		h.z = 1.0;

	vec3 x = normalize(cross(h, y));
	vec3 z = normalize(cross(x, y));

	vec3 direction = xs * x + ys * y + zs * z;
	return normalize(direction);
}
#end