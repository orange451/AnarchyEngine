#version 330

uniform sampler2D texture_diffuse;
uniform sampler2D texture_normal;
uniform sampler2D texture_metalness;
uniform sampler2D texture_roughness;
uniform samplerCube texture_cubemap;

uniform float uMetalness;
uniform float uRoughness;
uniform float uReflective;

uniform float uTransparencyObject;
uniform float uTransparencyMaterial;

uniform vec3 uAmbient;

uniform float normalMapEnabled;
uniform float enableSkybox;

uniform vec3 uMaterialColor;
uniform vec3 uMaterialEmissive;

in vec2 passTexCoord;
in vec4 passColor;
in vec3 passNormal;
in vec4 vViewSpacePos;
in vec3 vViewSpaceNor;
const float MAX_REFLECTION_LOD = 14.0;

struct PointLight {
	vec3 Position;
	vec3 Color;
	float Intensity;
	float Radius;
};

uniform PointLight uPointLights[32];
uniform float uNumPointLights;

vec3 pointLight( vec3 N, vec3 eyeSpace, vec3 lightPosition, vec3 albedo, float metallic, float roughness, float radius, vec3 lightColor, float intensity );

void write(vec3 diffuse, vec3 normal, float metalness, float roughness, float reflective, vec3 emissive);
vec3 normalmap(vec3 normalSample, vec3 vNormal, vec3 vViewSpacePos, vec2 vTexCoords );
vec3 calculateFresnel( vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float metalness );
vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal );
vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float reflective );
float getReflectionIndex( vec3 viewSpacePos, vec3 surfaceNormal, float reflective );

out vec4 outColor;

void main(void) {
	vec4 colorSRGB = vec4( pow( uMaterialColor, vec3(2.2)), 1.0 );
	vec4 diffuseSample = texture(texture_diffuse, passTexCoord) * passColor * colorSRGB;
	vec4 normalSample  = texture(texture_normal, passTexCoord);
	vec4 metalnessSample  = texture(texture_metalness, passTexCoord);
	vec4 roughnessSample  = texture(texture_roughness, passTexCoord);
	vec3 nViewSpacePos = normalize(vViewSpacePos.xyz);
	
	// Alpha Test
	if ( diffuseSample.a < 0.1 )
		discard;
	
	// Normal mapping
	vec3 normal = normalize(passNormal);
	if ( normalMapEnabled == 1.0 ) {
		normal = normalmap( normalSample.rgb, passNormal, nViewSpacePos, passTexCoord );
	}
	
	// Get PBR samples
	float fMetalness = metalnessSample.r * uMetalness ;
	float fRoughness = max(0.05, roughnessSample.r * uRoughness );
		
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
	
	// Initial color
	vec3 finalColor = diffuseSample.rgb;
	
	// Apply global ambient
	finalColor.rgb *= uAmbient;
	
	// Add lighting
	for (int i = 0; i < int(uNumPointLights); i++) {
		PointLight light = uPointLights[i];
		
		vec3 eyeSpace = (vViewSpacePos.xyz / vViewSpacePos.w);
		
		finalColor.rgb += pointLight( normal, eyeSpace.xyz, light.Position, diffuseSample.rgb, fMetalness, fRoughness, light.Radius, light.Color, light.Intensity );
	}
	
	// Apply emissive
	finalColor.rgb += emissive;
	
	// Alpha
	float alpha = (1.0 - uTransparencyObject) * (1.0 - uTransparencyMaterial);
	
	// make non reflective parts more transparent
	float ri = 1.0 - (getReflectionIndex( nViewSpacePos, normal, uReflective + alpha*0.5 ) );
	alpha *= ri;

	// Write
	outColor = vec4(finalColor.rgb, alpha);
}
