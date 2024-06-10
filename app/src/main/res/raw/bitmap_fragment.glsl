varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;

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
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
}


