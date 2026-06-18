## Usage

在 `Shader2DEvent` 阶段调用 `LiquidGlass.draw(...)`

### Liquid Glass v3 <sub>(Updating…)</sub>

```java
```

<details>
<summary><b>Liquid Glass v2</b></summary>

<br/>

```java
public class LiquidGlassTest extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ScreenPositionProperty screenPosition = new ScreenPositionProperty("screen-position", 0.01F, 0.01F);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty width = new FloatProperty("width", 50.0F, 0.5F, 500.0F);
    public final FloatProperty height = new FloatProperty("height", 50.0F, 0.5F, 500.0F);

    public final ModeProperty liquidGlassMode = new ModeProperty("liquid-glass", 0, new String[]{"Clear", "Tinted"});

    public final FloatProperty radius = new FloatProperty("radius", 4.0F, 0.0F, 100.0F);
    public final IntProperty blurIterations = new IntProperty("blur-iterations", 1, 0, 5);
    public final FloatProperty blurRadius = new FloatProperty("blur-radius", 2.0F, 0.0F, 12.0F);
    public final FloatProperty blurDownScale = new FloatProperty("blur-downscale", 0.5F, 0.1F, 1.0F);
    public final FloatProperty noise = new FloatProperty("noise", 0.03F, 0.0F, 0.3F);
    public final FloatProperty refractionPower = new FloatProperty("refraction-power", 0.75F, -1.0F, 10.0F);
    public final FloatProperty glowWeight = new FloatProperty("glow-weight", 0.3F, -1.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 0);
    public final FloatProperty glowBias = new FloatProperty("glow-bias", 0.0F, -1.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 0);
    public final FloatProperty glowEdge0 = new FloatProperty("glow-edge0", 0.06F, -1.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 0);
    public final FloatProperty glowEdge1 = new FloatProperty("glow-edge1", 0.0F, -1.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 0);
    public final FloatProperty tintR = new FloatProperty("tint-r", 0.82F, 0.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 1);
    public final FloatProperty tintG = new FloatProperty("tint-g", 0.88F, 0.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 1);
    public final FloatProperty tintB = new FloatProperty("tint-b", 1.0F, 0.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 1);
    public final FloatProperty tintStrength = new FloatProperty("tint-strength", 0.12F, 0.0F, 1.0F, () -> this.liquidGlassMode.getValue() == 1);
    public final FloatProperty chromaStrength = new FloatProperty("chroma-strength", 0.001F, 0.0F, 0.01F, () -> this.liquidGlassMode.getValue() == 1);

    public LiquidGlassTest() {
        super("LiquidGlassTest", true, true);
    }

    @EventTarget
    public void onShader2D(Shader2DEvent event) {
        if (this.isEnabled()) {
            LiquidGlass.draw(
                    this.screenPosition.getScaledX(),
                    this.screenPosition.getScaledY(),
                    this.width.getValue() * this.scale.getValue(),
                    this.height.getValue() * this.scale.getValue(),
                    this.radius.getValue() * this.scale.getValue(),
                    this.blurRadius.getValue()
                    ...
            );
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            this.screenPosition.setWidth(this.width.getValue() * this.scale.getValue());
            this.screenPosition.setHeight(this.height.getValue() * this.scale.getValue());

            float posX = this.screenPosition.getScaledX() / this.scale.getValue();
            float posY = this.screenPosition.getScaledY() / this.scale.getValue();
            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
            GlStateManager.translate(posX, posY, 0.0F);
            GlStateManager.popMatrix();
        }
    }
}
```

</details>
