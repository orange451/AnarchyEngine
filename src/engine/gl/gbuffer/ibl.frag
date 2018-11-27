#version 330

uniform sampler2D texture_normal;
uniform samplerCube texture_ibl;

uniform mat4 uInverseViewMatrix;

in vec2 passTexCoord;
in vec4 passColor;

out vec4 outColor;

const float MULTIPLIER = 1024;

void main(void) {
	vec3 normal = texture( texture_normal, passTexCoord ).rgb;
	
	//vec3 ibl = vec3( mat3( uInverseViewMatrix ) * normal );
	//vec3 ibl = reflect( vec3( 0, 0, 1 ), normal );
	vec3 ibl = reflect( vec3( 0, 0, 1 ), normal );
	
	vec3 sample = texture( texture_ibl, ibl ).rgb;
	
	outColor = vec4( sample*MULTIPLIER, 1.0 );
}