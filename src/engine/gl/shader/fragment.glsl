#version 330

uniform sampler2D colorSampler;
uniform vec3 uMaterialColor;

in vec2 passTexCoord;
in vec4 passColor;

out vec4 outColor;

void main(void) {
	vec4 color = texture(colorSampler, passTexCoord)*passColor;
	outColor = color*vec4(uMaterialColor, 1.0);
}
