#version 150 core

// CONSTANTS FOR NOW
const float shadow = 1.0;
const float PI = 3.1515926535;

// PBR RELATED
float DistributionGGX(vec3 N, vec3 H, float roughness);
float GeometrySchlickGGX(float NdotV, float roughness);
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness);
vec3 fresnelSchlick(float cosTheta, vec3 F0);

vec3 light( vec3 N, vec3 L, vec4 eyeSpace, vec3 dPos, vec3 albedo, float metallic, float roughness, float radius, vec3 lightColor, float intensity ) {
	float NdotL = max(dot(N, L), 0.0);

	vec3 color = vec3(0.0);
	if (NdotL > 0.0) {
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
	
	return color;
}
