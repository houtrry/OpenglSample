varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;
uniform vec4 center_color;
uniform vec4 outer_color;
uniform vec4 wall_color;
uniform bool isMap;

vec4 convertMapColor(vec4 color, vec4 color0) {
    if (color.r == 1.0 && color.g == 1.0 && color.b == 1.0) {
        //中心区域 #c3d8ea
        //        return vec4(0.49725, 0.847059, 0.917647, 1.0);
//        return vec4(0.0, 0.0, 1.0, 1.0);
//        return vec4(0.7647058823529412, 0.8470588235294118, 0.9176470588235294, 1.0);
        return center_color;
    }
    if (color.r == color0.r && color.g == color0.g && color.b == color0.b) {
//        return vec4(1.0, 1.0, 1.0, 1.0);
        return outer_color;
    }
//    if (
//    color.r <= 0.50197 && color.r >= 0.4980
//    && color.g <= 0.50197 && color.g >= 0.4980
//    && color.b <= 0.50197 && color.b >= 0.4980
//    ) {
//        //0.5176470588235295，0.5098039215686274，0.5176470588235295
//        //外侧 #ffffff
//        return vec4(1.0, 1.0, 1.0, 1.0);
//    }
    //墙体、障碍物 #0072ff
//    return vec4(0.0, 0.447, 1.0, 1.0);
    return wall_color;
}

vec4 convertToGrayColor(vec4 color) {
    //灰色滤镜
    float average = 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b;
    return vec4(average, average, average, 1.0);
}

vec4 convertInvertedColor(vec4 color) {
    //反色滤镜
//    return vec4(1.0 - color.r, 1.0 - color.g, 1.0 - color.b, 1.0);
    //两个写法等效
    return vec4((1.0 - color.rgb), color.w);
}

void main() {
    // 将2D纹理inputImageTexture和纹理顶点坐标通过texture2D计算后传给片段着色器
    vec4 color0 = texture2D(inputImageTexture, vec2(1.0, 1.0));
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    //    float red = (gl_FragColor.r - 0.5870 * gl_FragColor.g - 0.1140 * gl_FragColor.b) / 0.2989;
    //    if((color.r > 0.5137 && color.r < 0.5216) ) {
    if(isMap) {
        vec4 color0 = texture2D(inputImageTexture, vec2(1.0, 1.0));
        vec4 color = texture2D(inputImageTexture, textureCoordinate);
        gl_FragColor = convertMapColor(color, color0);
    } else {
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }

    //    gl_FragColor = convertToGrayColor(color);
    //    gl_FragColor = color * vec4(0.5, 0.5, 0.5, 1);
//    gl_FragColor = convertInvertedColor(color);
//        gl_FragColor = color;
    //    if(fr <= 133.0 && fr >= 131.0 && fg <= 131.0 && fg >= 129.0 && fb <= 133.0 && fb >= 131.0) {
}


