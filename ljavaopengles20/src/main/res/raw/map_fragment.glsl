precision mediump float;

varying highp vec2 textureCoordinate;

uniform sampler2D inputImageTexture;
uniform vec4 center_color;
uniform vec4 outer_color;
uniform vec4 origin_outer_color;
uniform vec4 wall_color;

// 基于灰度阈值 + 容差的分类映射，兼容 RGBA 与 LUMINANCE 纹理
const float G_OUTER  = 128.0/255.0;   // 原图外侧 #808080 的灰度
const float G_CENTER = 1.0;           // 原图内侧 #ffffff 的灰度
const float EPS_OUTER  = 3.0/255.0;   // 外侧容差（可按需微调）
const float EPS_CENTER = 5.0/255.0;   // 内侧容差（可按需微调）

vec4 convertMapColor(vec4 color) {
    // 灰度统一处理；对 LUMINANCE 纹理，rgb 已为同一亮度
    float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    // 1) 内侧：接近白色
    if (abs(gray - G_CENTER) <= EPS_CENTER) {
        return center_color;
    }

    // 2) 外侧：接近 #808080
    if (abs(gray - G_OUTER) <= EPS_OUTER) {
        return outer_color;
    }

    // 3) 墙体/障碍：比外侧更暗
    if (gray < (G_OUTER - EPS_OUTER)) {
        return wall_color;
    }

    // 4) 其它：夹在 outer 与 center 之间的噪声，归为外侧
    return outer_color;
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
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor = convertMapColor(color);
}


