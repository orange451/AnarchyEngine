#version 150 core

uniform sampler2D texture_albedo;
uniform sampler2D texture_depth;
uniform sampler2D texture_normal;
uniform sampler2D texture_pbr;

uniform vec2 texel;
uniform vec3 lightPosition;
uniform mat4 inverseProjectionMatrix;

uniform float radius;
uniform float intensity;

in vec4 passColor;
in vec2 passTexCoord;
in vec3 passNormal;

out vec4 out_Color;

// CONSTANTS FOR NOW
const vec3 lightColor = vec3(1.0);
const float shadow = 1.0;
const float PI = 3.1515926535;


// From pbr.frag
float DistributionGGX(vec3 N, vec3 H, float roughness);
float GeometrySchlickGGX(float NdotV, float roughness);
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness);
vec3 fresnelSchlick(float cosTheta, vec3 F0);


void main(void) {
	vec2 texCoords = gl_FragCoord.xy * texel;
	vec4 clipPos = vec4(vec3(texCoords, texture(texture_depth, texCoords).r) * 2.0 - 1.0, 1.0);
	vec4 eyeSpace = inverseProjectionMatrix * clipPos;
	eyeSpace.xyz /= eyeSpace.w;
	vec3 dPos = lightPosition - eyeSpace.xyz;
	
	vec3 L = normalize(dPos);
	vec3 N = texture( texture_normal, texCoords ).rgb;
	vec3 PBR = texture( texture_pbr, texCoords ).rgb;
	float NdotL = max(dot(N, L), 0.0);
	float metallic = PBR.x;
	float roughness = PBR.y;
	
	vec3 color = vec3(0.0);
	if (NdotL > 0.0) {
		vec3 albedo = texture(texture_albedo, texCoords).rgb;
		vec3 F0 = mix(vec3(0.04), albedo, metallic);
		vec3 V = normalize(0.0 - eyeSpace.xyz);
		vec3 H = normalize(L + V);
		
		// Compute initial lighting factors (DFG)
		float NDF = DistributionGGX(N, H, roughness);	// Specular component?
		float G = GeometrySmith(N, V, L, roughness);	// Surface light?
		vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
		
		// Calculate Cook-Torrance BDRF
		vec3 numerator = vec3(NDF * G * F);
		float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0);
		vec3 specular = numerator / max(denominator, 0.01);
		
		// Calculate refraction
		vec3 kS = F;
		vec3 kD = vec3(1.0) - kS;
		kD *= 1.0 - metallic;

		// Calculate attenuation
		float distance = length(dPos);
		float attenuation = max(1.0 - distance / radius, 0.0) / distance;
		vec3 radiance = lightColor * attenuation * intensity;

		// Final color
		color = (kD * albedo + specular) * (radiance * NdotL * shadow);
	}
	
	out_Color = vec4( color, 1.0 );
}
