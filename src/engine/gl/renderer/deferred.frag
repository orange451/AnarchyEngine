#version 330

uniform sampler2D texture_diffuse;
uniform sampler2D texture_normal;
uniform sampler2D texture_metalness;
uniform sampler2D texture_roughness;
uniform samplerCube texture_cubemap;

uniform float uMetalness;
uniform float uRoughness;
uniform float uReflective;

uniform float normalMapEnabled;
uniform float enableSkybox;

uniform vec3 uMaterialColor;
uniform vec3 uMaterialEmissive;

in vec2 passTexCoord;
in vec4 passColor;
in vec3 passNormal;
in vec3 vViewSpacePos;
in vec3 vViewSpaceNor;

const float MAX_REFLECTION_LOD = 14.0;

void write(vec3 diffuse, vec3 normal, float metalness, float roughness, float reflective, vec3 emissive);
vec3 normalmap(vec3 normalSample, vec3 vNormal, vec3 vViewSpacePos, vec2 vTexCoords );
vec3 calculateFresnel( vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float metalness );
vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal );
vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float reflective );

void main(void) {
	vec4 colorSRGB = vec4( pow( uMaterialColor, vec3(2.2)), 1.0 );
	vec4 diffuseSample = texture(texture_diffuse, passTexCoord) * passColor * colorSRGB;
	vec4 normalSample  = texture(texture_normal, passTexCoord);
	vec4 metalnessSample  = texture(texture_metalness, passTexCoord);
	vec4 roughnessSample  = texture(texture_roughness, passTexCoord);
	vec3 nViewSpacePos = normalize(vViewSpacePos.xyz);
	
	// Alpha Test
	if ( diffuseSample.a < 0.25 )
		discard;
	
	// Normal mapping
	vec3 normal = normalize(passNormal);
	if ( normalMapEnabled == 1.0 ) {
		normal = normalmap( normalSample.rgb, passNormal, nViewSpacePos, passTexCoord );
	}
	
	// Get PBR samples
	float fMetalness = metalnessSample.r * uMetalness ;
	float fRoughness = max(0.05, roughnessSample.r*uRoughness );
		
	// Cubemapping
	if ( enableSkybox == 1.0 ) {
		// Compute normal environment map based on roughness
		vec3 cubemapSample = textureLod(texture_cubemap, reflectEnv( nViewSpacePos, normal ).xyz, MAX_REFLECTION_LOD * fRoughness).rgb;
		
		// Apply reflection PBR parameter
		cubemapSample = reflectivePBR( cubemapSample, nViewSpacePos, normal, fRoughness, uReflective );

		// Combine cubemap into diffuse
		diffuseSample.rgb *= cubemapSample;
    }
	
	// Calculate fresnel
	vec3 fresnel = calculateFresnel( nViewSpacePos, normal, fRoughness, fMetalness ) * uReflective;
	vec3 emissive = uMaterialEmissive+fresnel;

	// Write to GBuffer
	write( diffuseSample.rgb, normal, fMetalness, fRoughness, uReflective, emissive );
}
