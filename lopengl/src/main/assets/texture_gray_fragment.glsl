#version 300 es

precision mediump float;
in vec2 v_texCoord;
layout(location = 0) out vec4 outColor;
uniform sampler2D s_TextureMap;

void main() {
//    outColor = texture(s_TextureMap, v_texCoord);
//    flat gray;
//    gray  = float(texture(s_TextureMap, v_texCoord).r);
    vec3 rgb;
    rgb.r = texture(s_TextureMap, v_texCoord).r;
    rgb.g = rgb.r;
    rgb.b = rgb.r;
    outColor = vec4(rgb, 1.0);
//    outColor = vec4(gray, gray, gray, 1.0);
}