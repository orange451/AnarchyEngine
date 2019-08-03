#version 330

uniform sampler2D texture_depth;
uniform sampler2D texture_diffuse;
uniform sampler2D texture_normal;
uniform sampler2D texture_pbr;
uniform samplerCube texture_ibl;

uniform vec3 uAmbient;
uniform mat4 uInverseViewMatrix;
uniform mat4 uInverseProjectionMatrix;

uniform float uSkyBoxLightPower;
uniform float uSkyBoxLightMultiplier;

in vec2 passTexCoord;
in vec4 passColor;

out vec4 outColor;

const float MAX_REFLECTION_LOD = 18.0;

vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal );
float calculateFresnel( vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float metalness, float reflectiveness );

void main(void) {
	float depth = texture( texture_depth, passTexCoord ).r;
	if ( depth == 1.0 )
		discard;
		
	vec4 clipPos = vec4(vec3(passTexCoord, depth) * 2.0 - 1.0, 1.0);
	vec4 eyeSpace = uInverseProjectionMatrix * clipPos;
	eyeSpace.xyz /= eyeSpace.w;
	
	vec3 L = normalize(eyeSpace.xyz);
	vec3 N = texture( texture_normal, passTexCoord ).rgb;	// View Space Normals
	vec3 PBR = texture( texture_pbr, passTexCoord ).rgb;	// Material data (roughness/metalness)
	vec3 kD = texture( texture_diffuse, passTexCoord ).rgb;	// Diffuse texture (from gbuffer)
	
	// Some PBR stuff
	float metallic = PBR.x;
	float roughness = PBR.y;
	float reflectiveness = PBR.z;
	kD *= 1.0 - metallic;
	
	// Calculate IBL radiance
	vec3 radiance = textureLod( texture_ibl, reflectEnv( L, N ), MAX_REFLECTION_LOD * roughness ).rgb;
	
	// Correct radiance (boost brightness)
	radiance = max( radiance, 0.0 );
	radiance = pow( radiance, vec3(uSkyBoxLightPower) );
	radiance = radiance * uSkyBoxLightMultiplier;
	
	// Calculate final IBL based on reflectiveness
	vec3 final = kD + mix( radiance, kD * radiance, metallic ) * (0.05+reflectiveness);
	
	// Scale based on ambient
	final = final*((1.0+uAmbient)/8.0);
	
	// Fresnel
	float fresnel = calculateFresnel( L, N, roughness, metallic, reflectiveness );
	final += (radiance*fresnel)*(uAmbient*uSkyBoxLightMultiplier);

	// Write
	outColor = vec4( final, 1.0 );
}