#version 300 es

precision mediump float;
in vec2 v_texCoord;
layout(location = 0) out vec4 outColor;
uniform sampler2D s_TextureMap;

void main() {
//    outColor = texture(s_TextureMap, v_texCoord);
//    flat gray;
//    gray  = float(texture(s_TextureMap, v_texCoord).r);
    float gray = texture(s_TextureMap, v_texCoord).r;
    if(132.0 / 255.0 == gray) {
        outColor = vec4(1.0, 1.0, 1.0, 1.0);
    } else if (1.0 == gray) {
        outColor = vec4(0.49725, 0.847059, 0.917647, 1.0);
    } else {
        outColor = vec4(0.0, 0.447, 1.0, 1.0);
    }

//    outColor = vec4(gray, gray, gray, 1.0);
}