varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;

vec4 convertColor(vec4 color) {
    if (color.r == 1.0 && color.g == 1.0 && color.b == 1.0) {
        //中心区域 #c3d8ea
//        return vec4(0.49725, 0.847059, 0.917647, 1.0);
        return vec4(0.7647, 0.847059, 0.917647, 1.0);
    }
    if (color.r <= 0.50197 && color.r >= 0.4980
        && color.g <= 0.50197 && color.g >= 0.4980
        && color.b <= 0.50197 && color.b >= 0.4980) {
        //外侧 #ffffff
        return vec4(1.0, 1.0, 1.0, 1.0);
    }
    //墙体、障碍物 #0072ff
    return vec4(0.0, 0.447, 1.0, 1.0);
}

void main() {
    // 将2D纹理inputImageTexture和纹理顶点坐标通过texture2D计算后传给片段着色器
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    //    float red = (gl_FragColor.r - 0.5870 * gl_FragColor.g - 0.1140 * gl_FragColor.b) / 0.2989;
    //    if((color.r > 0.5137 && color.r < 0.5216) ) {
    gl_FragColor = convertColor(color);
    //    if(fr <= 133.0 && fr >= 131.0 && fg <= 131.0 && fg >= 129.0 && fb <= 133.0 && fb >= 131.0) {
}


