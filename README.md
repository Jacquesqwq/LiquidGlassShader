# Liquid Glass

一个练手项目，面向Minecraft 1.8.9的single-pass liquid glass片源着色器实现，在不依赖现代Core Profile的前提下，模仿某个风格玻璃材质的折射，磨砂，色散，边缘高光和Fresnel反射

---
### Liquid Glass v3 <sub>(Updating…)</sub>
<p align="center">
  <img alt="Liquid Glass v3" src="https://github.com/user-attachments/assets/e1620432-c31e-4b92-8ea5-535cb6b0af07" width="90%" />
</p>
<details>
<summary><b>Liquid Glass v2</b></summary>
<br/>
**Clear**
<p align="center">
  <img alt="Clear" src="https://github.com/user-attachments/assets/7ce523f7-7672-4bed-ad53-081799ebfb44" width="90%" />
</p>
**Tinted**
<table>
  <tr>
    <td align="center"><b>Day</b></td>
    <td align="center"><b>Night</b></td>
  </tr>
  <tr>
    <td><img alt="Tinted Day" src="https://github.com/user-attachments/assets/45afd8b8-4357-4865-a94f-0f9eccf91375" /></td>
    <td><img alt="Tinted Night" src="https://github.com/user-attachments/assets/744bb7db-25a5-4837-b170-2c0aa2d19db5" /></td>
  </tr>
</table>
</details>

---

## 架构

### 兼容性

| 项目 | 说明 |
|---|---|
| 着色语言 | **GLSL 1.20** (`#version 120`) |
| 管线 | 兼容固定功能;使用 `gl_TexCoord[0]`、`gl_FragColor`、`varying` |
| 运行环境 | Minecraft 1.8.9 / LWJGL2 / 老旧 GPU 与驱动 |
| 帧缓冲 | 8-bit SDR(非浮点 HDR) |

### 扩展

```glsl
#extension GL_OES_standard_derivatives : enable   // dFdx / dFdy:片元屏幕空间偏导
#extension GL_ARB_shader_texture_lod    : enable   // texture2DLod:片元阶段显式 LOD 采样
```

## Usage
[Usage Guide](docs/USAGE.md)
