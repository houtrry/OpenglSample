# 地图渲染：两套 Program 的图层式方案设计

## 背景
- 现状：同一 fragment shader 通过 `isMap` 或 `map_mix` 同时承担“地图颜色映射”和“普通纹理采样”。
- 问题：
  - 运行时分支或冗余计算常驻，性能与可维护性一般；
  - 地图与标记/文字等效果耦合，后续扩展（描边、选中、阴影）复杂。

## 目标
- 将“地图”与“其他图元（标记/路径/文字等）”的渲染职责分离：
  - Program-Map：带“灰度阈值+容差”的颜色映射（已实现于 `map_fragment.glsl`）。
  - Program-Plain：普通纹理采样，无颜色映射。
- 以图层顺序绘制：先地图，后叠加其他图元。

## 改动范围
- 仅限模块：`app`、`common_map`、`ljavaopengles20`。
- 尽量不改其它模块。

## 方案概述
1. 新增/复用两套 Shader：
   - Map Program：`map_vertex.glsl` + `map_fragment.glsl`（保留现有灰度映射逻辑）。
   - Plain Program：`map_vertex.glsl` + `texture_fragment.glsl`（新增：仅采样贴图并输出）。
2. 在 `ljavaopengles20` 新增 `ProgramRepository`：
   - 负责创建/缓存 Program，提供 `getMapProgram()`、`getPlainProgram()`；
   - 负责查询 uniform/attrib 位置，避免每帧查找；
   - 避免多处重复加载/编译。
3. 各 Layer 使用对应 Program：
   - 地图层（`EnhancedMapLayer`/`MapLayer`）使用 Map Program；
   - 标记、路径、文本层使用 Plain Program（若已有专用 Program，保持不变）。
4. 绘制顺序：
   - `MapView`/`MapRender` 中保证“先地图、后标记/文本”。

## 详细设计
### 1) Shader
- Map Fragment（已存在）：使用“灰度阈值+容差”映射，无需修改接口；删除 `isMap` 等分支已完成。
- Plain Fragment（新增）：
  ```glsl
  precision mediump float;
  varying highp vec2 textureCoordinate;
  uniform sampler2D inputImageTexture;
  void main() {
      gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
  }
  ```
  - 共享同一顶点着色器（`map_vertex.glsl`）。

### 2) ProgramRepository（ljavaopengles20）
- 作用：
  - 延迟创建并缓存 Program；
  - 集中管理 uniform/attrib 的 location；
  - 暴露结构化的访问器（如 `MapProgramHandles` 与 `PlainProgramHandles`）。
- 生命周期：
  - 在 `MapRender` 初始化时创建实例；
  - 在 GL 释放时统一销毁。

### 3) Layer 调整
- Map 层：
  - 使用 `MapProgramHandles`（位置同现有：`vPosition`、`inputTextureCoordinate`、`u_TransformMatrix`、`center_color`、`outer_color`、`wall_color`）。
  - 不再设置 `isMap`/`map_mix`。
- 标记/文字等：
  - 使用 `PlainProgramHandles`（位置：`vPosition`、`inputTextureCoordinate`、`u_TransformMatrix`）。
  - 色值/混合按原有逻辑。

### 4) 渲染顺序
```mermaid
flowchart LR
  A[开始帧] --> B[绑定 Map Program]
  B --> C[绘制地图瓦片]
  C --> D[绑定 Plain Program]
  D --> E[绘制标记/路径/文字]
  E --> F[提交本帧]
```

## 实施步骤
1. 新增 `texture_fragment.glsl` 到 `ljavaopengles20/src/main/res/raw/` 与 `app/src/main/res/raw/`。
2. 新增 `ProgramRepository`（`ljavaopengles20`）：
   - `getMapProgram()`、`getPlainProgram()`；
   - 暴露 `MapProgramHandles`/`PlainProgramHandles`。
3. `MapRender`/`MapView` 初始化时创建仓库并注入到各 Layer（或单例访问）。
4. `EnhancedMapLayer`/`MapLayer` 切换到 Map Program；标记/文字层切到 Plain Program。
5. 验证：
   - useCpuGrayscale=true/false；
   - (0,0)/(1,0)/(1,1) 等瓦片边界；
   - 多标记叠加、透明度与混合模式；
   - 性能对比（Program 切换次数、帧时间）。

## 性能与内存
- 移除片元分支/混合路径，ALU 更稳定；
- Program 切换：按图层批量绘制，通常 1~2 次/帧，开销可控；
- 纹理与缓存不变；
- 可选：静态地图使用 FBO 预计算，进一步降低每帧开销。

## 回滚策略
- 保留旧 Program 与切换开关（如在 `MapRender` 提供 `useUnifiedProgram=false`）；
- 如出现兼容问题，可临时回退为旧路径；
- 文档与变更点记录在 `docs/QuestionList.md`。

## 验收清单
- 地图/标记渲染正确，无串色；
- GLSL 编译通过，无运行时错误；
- useCpuGrayscale 两种路径均符合预期；
- 帧率不降或更优；
- 代码改动仅限 `app`、`common_map`、`ljavaopengles20`。


