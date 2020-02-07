/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct DynamicSky

in vec2 pass_textureCoords;
in vec3 pass_position;
in vec3 pass_normal;
in vec4 clipSpace;
in vec4 clipSpacePrev;

out vec4[5] out_Color;

uniform int renderSun;
uniform vec3 lightPosition;
uniform vec3 cameraPosition;
uniform DynamicSky dynamicSky;
uniform vec3 ambient;
uniform int frame;

#include function noise

#include variable MASK

#define SUN_LOWER_LIMIT 0.51
#define SUN_UPPER_LIMIT 0.5

#include variable pi
#define iSteps 16
#define jSteps 8

vec2 rsi(vec3 r0, vec3 rd, float sr) {
	// ray-sphere intersection that assumes
	// the sphere is centered at the origin.
	// No intersection when result.x > result.y
	float a = dot(rd, rd);
	float b = 2.0 * dot(rd, r0);
	float c = dot(r0, r0) - (sr * sr);
	float d = (b * b) - 4.0 * a * c;
	if (d < 0.0)
		return vec2(1e5, -1e5);
	return vec2((-b - sqrt(d)) / (2.0 * a), (-b + sqrt(d)) / (2.0 * a));
}

vec3 atmosphere(vec3 r, vec3 r0, vec3 pSun, float iSun, float rPlanet, float rAtmos, vec3 kRlh,
				float kMie, float shRlh, float shMie, float g) {
	// Normalize the sun and view directions.
	pSun = normalize(pSun);
	r = normalize(r);

	// Calculate the step size of the primary ray.
	vec2 p = rsi(r0, r, rAtmos);
	if (p.x > p.y)
		return vec3(0, 0, 0);
	p.y = min(p.y, rsi(r0, r, rPlanet).x);
	float iStepSize = (p.y - p.x) / float(iSteps);

	// Initialize the primary ray time.
	float iTime = 0.0;

	// Initialize accumulators for Rayleigh and Mie scattering.
	vec3 totalRlh = vec3(0, 0, 0);
	vec3 totalMie = vec3(0, 0, 0);

	// Initialize optical depth accumulators for the primary ray.
	float iOdRlh = 0.0;
	float iOdMie = 0.0;

	// Calculate the Rayleigh and Mie phases.
	float mu = dot(r, pSun);
	float mumu = mu * mu;
	float gg = g * g;
	float pRlh = 3.0 / (16.0 * PI) * (1.0 + mumu);
	float pMie = 3.0 / (8.0 * PI) * ((1.0 - gg) * (mumu + 1.0)) / (pow(1.0 + gg - 2.0 * mu * g, 1.5) * (2.0 + gg));

	// Sample the primary ray.
	for (int i = 0; i < iSteps; i++) {

		// Calculate the primary ray sample position.
		vec3 iPos = r0 + r * (iTime + iStepSize * 0.5);

		// Calculate the height of the sample.
		float iHeight = length(iPos) - rPlanet;

		// Calculate the optical depth of the Rayleigh and Mie scattering for this step.
		float odStepRlh = exp(-iHeight / shRlh) * iStepSize;
		float odStepMie = exp(-iHeight / shMie) * iStepSize;

		// Accumulate optical depth.
		iOdRlh += odStepRlh;
		iOdMie += odStepMie;

		// Calculate the step size of the secondary ray.
		float jStepSize = rsi(iPos, pSun, rAtmos).y / float(jSteps);

		// Initialize the secondary ray time.
		float jTime = 0.0;

		// Initialize optical depth accumulators for the secondary ray.
		float jOdRlh = 0.0;
		float jOdMie = 0.0;

		// Sample the secondary ray.
		for (int j = 0; j < jSteps; j++) {

			// Calculate the secondary ray sample position.
			vec3 jPos = iPos + pSun * (jTime + jStepSize * 0.5);

			// Calculate the height of the sample.
			float jHeight = length(jPos) - rPlanet;

			// Accumulate the optical depth.
			jOdRlh += exp(-jHeight / shRlh) * jStepSize;
			jOdMie += exp(-jHeight / shMie) * jStepSize;

			// Increment the secondary ray time.
			jTime += jStepSize;
		}

		// Calculate attenuation.
		vec3 attn = exp(-(kMie * (iOdMie + jOdMie) + kRlh * (iOdRlh + jOdRlh)));

		// Accumulate scattering.
		totalRlh += odStepRlh * attn;
		totalMie += odStepMie * attn;

		// Increment the primary ray time.
		iTime += iStepSize;
	}

	// Calculate and return the final color.
	return iSun * (pRlh * kRlh * totalRlh + pMie * kMie * totalMie);
}

#define MAX_STEPS 200
#define MAX_DIST 50000.
#define SURF_DIST .01

float GetDist(vec3 p) {
	float planeDist = abs(dynamicSky.cloudHeight - p.z);
	return planeDist;
}

float RayMarch(vec3 ro, vec3 rd) {
	float dO = 0.0;
	for (int i = 0; i < MAX_STEPS; i++) {
		vec3 p = ro + rd * dO;
		float dS = GetDist(p);
		dO += dS;
		if (dO > MAX_DIST)
			return -1.0;
		if (dS < SURF_DIST)
			break;
	}
	return dO;
}
/*
#include function random

// Simplex 2D noise
//
vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v){
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
           -0.577350269189626, 0.024390243902439);
  vec2 i  = floor(v + dot(v, C.yy) );
  vec2 x0 = v -   i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
  + i.x + vec3(0.0, i1.x, 1.0 ));
  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
    dot(x12.zw,x12.zw)), 0.0);
  m = m*m ;
  m = m*m ;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
  vec3 g;
  g.x  = a0.x  * x0.x  + h.x  * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

bool rayHitCloud(vec3 p, out float volume) {
	float height = max(snoise(p.xy), 0.0);
	if (height <= 0.0)
		return false;
	volume = height;

	float upperHeight = height - 0.5;
	upperHeight *= 800;

	float lowerHeight = height + 0.5;
	lowerHeight *= -800;

	lowerHeight += dynamicSky.cloudHeight + 1600;
	upperHeight += dynamicSky.cloudHeight + 1600;
	return abs(lowerHeight - p.z) < 100 || abs(upperHeight - p.z) < 100;
}*/

void main() {
	vec3 V = normalize(pass_normal);
	vec3 L = normalize(lightPosition);

	vec3 atm = normalize(vec3(pass_normal.x, pass_normal.z, pass_normal.y));
	vec3 atmL = normalize(vec3(lightPosition.x, lightPosition.z, lightPosition.y));

	vec3 color = atmosphere(atm,								// normalized ray direction
							vec3(0, 6372e3 + cameraPosition.z, 0),				// ray origin
							atmL,								// position of the sun
							22.0,							// intensity of the sun
							6371e3,							// radius of the planet in meters
							6471e3,							// radius of the atmosphere in meters
							vec3(5.5e-6, 13.0e-6, 22.4e-6), // Rayleigh scattering coefficient
							21e-6,							// Mie scattering coefficient
							8e3,							// Rayleigh scale height
							1.2e3,							// Mie scale height
							0.758							// Mie preferred scattering direction
	);
	color = max(color, 0.0);

	color = 1.0 - exp(-1.0 * color);

	if (renderSun == 1) {
		float vl = dot(V, L);
		float factorSun =
			clamp((pass_textureCoords.y - SUN_LOWER_LIMIT) / (SUN_UPPER_LIMIT - SUN_LOWER_LIMIT),
				  0.0, 1.0);
		if (vl > 0.999)
			color =/* mix(color,*/ mix(color, vec3(100.0), smoothstep(0.9992, 0.9993, vl))/*, factorSun)*/;
	}

	/*vec3 rayTrace = cameraPosition;
	float rayDist, incr = 1.0;
	vec3 rays;
	vec3 randSample, finalTrace;
	bool hit = false;
	do {
		rayTrace += V * incr;
		incr *= 1.025;

		rayDist = length(rayTrace - cameraPosition);
		//if (rayTrace.z > dynamicSky.cloudHeight + 3200)
			//break;
		//if (rayTrace.z < dynamicSky.cloudHeight)
			//break;

		float noiseMult = 0.00015;

		vec3 randSample = vec3(random(rayTrace.x + dynamicSky.time * frame),
		random(rayTrace.y * dynamicSky.time + frame), random(rayTrace.z * dynamicSky.time * frame));
		randSample -= 0.5;
		randSample *= 0.5;
		//randSample *= 0.0;

		float fade = smoothstep(dynamicSky.cloudHeight, dynamicSky.cloudHeight + 200, rayTrace.z);
		float volume;
		bool hitCloud = rayHitCloud(vec3((rayTrace.xy + randSample.xy * rayDist * 0.125) * noiseMult, rayTrace.z + randSample.z * rayDist * 0.125), volume);

		if(hitCloud) {
			float shadow = 1.0;
			{
				float shadowIncr = 20.0;
				vec3 shadowTrace = rayTrace;
				int i = 0;
				do {
					shadowTrace += L * shadowIncr;
					shadowIncr *= 1.05;
					if (shadowTrace.z < dynamicSky.cloudHeight)
						break;
					if (shadowTrace.z > dynamicSky.cloudHeight + 3200)
						break;
					float shadowVol;
					bool hitCloud = rayHitCloud(vec3((shadowTrace.xy + randSample.xy * rayDist * 0.125) * noiseMult, shadowTrace.z + randSample.z), shadowVol);
					if(hitCloud) {
						shadow -= 0.15;
						//shadow = 0.0;
					}
					i++;
				} while(i < 40);
			}
			rays = vec3(1.0) * max(shadow, 0.0) * smoothstep(-0.05, 0.2, dot(vec3(0,0,1), L));
			hit = true;
			break;
		}
	} while (rayDist < 20000);
	if(hit)
		color = rays;*/

	vec3 rd = V;
	vec3 ro = cameraPosition;

	float d = RayMarch(ro, rd);
	if(d > 0) {
		vec3 p = ro + rd * d;
		// color = vec3(fbm(p.xz * 0.005));

		vec2 st = p.xy * 0.00075 + vec2(dynamicSky.time * 0.0005, dynamicSky.time * 0.00005) * dynamicSky.cloudSpeed;

		vec3 cloudColor = vec3(0.0);
		float cloudTime = dynamicSky.time * 0.025; // Use cloud time instead

		vec2 q = vec2(0.);
		q.x = fbm(st + 0.00 * cloudTime);
		q.y = fbm(st + vec2(1.0));

		vec2 r = vec2(0.);
		r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * cloudTime);
		r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * cloudTime);

		float f = fbm(st + r);

		cloudColor = mix(vec3(0.101961, 0.619608, 0.666667), vec3(0.666667, 0.666667, 0.498039),
					clamp((f * f) * 4.0, 0.0, 1.0));

		cloudColor = mix(cloudColor, vec3(0, 0, 0.164706), clamp(length(q), 0.0, 1.0));

		cloudColor = mix(cloudColor, vec3(1), clamp(length(r.x), 0.0, 1.0));

		// color = mix(color, vec3(1), fbm(p.xz * 0.005));
		float finalF = f * f * f + .6 * f * f + .5 * f;
		color = mix(color, finalF * cloudColor * clamp(dot(vec3(0, 0, 1), L) * 5.0, 0.0, 1.0),
					clamp(length(r.x), 0.0, 1.0));
	}

	vec3 ndcPos = (clipSpace / clipSpace.w).xyz;
    vec3 ndcPosPrev = (clipSpacePrev / clipSpacePrev.w).xyz;

	out_Color[0] = vec4(color * ambient, 0.0);
	out_Color[1] = vec4((ndcPosPrev - ndcPos).xy, 0.0, 0.0);
	out_Color[2] = vec4(0.0);
	out_Color[3] = vec4(0.0);
	out_Color[4] = vec4(0.0, 0.0, 0.0, PBR_BACKGROUND_DYNAMIC);
}