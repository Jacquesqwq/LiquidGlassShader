/**
 * @author Jacques
 * @since 2026/05/15
 */
public class LiquidGlass {
    private static final Minecraft mc = Minecraft.getMinecraft();
    //private static final ShaderUtil gaussianBlur = new ShaderUtil("gaussianBlur.frag", "vertex.vsh");  
    private static final ShaderUtil mipmapCopy = new ShaderUtil("mipmapCopy.frag", "vertex.vsh");
    //private static final ShaderUtil liquidGlassClear = new ShaderUtil("liquidGlassV2Clear.frag", "liquidGlassV2.vsh");
    //private static final ShaderUtil liquidGlassTinted = new ShaderUtil("liquidGlassV2Tinted.frag", "liquidGlassV2.vsh");
    private static final ShaderUtil liquidGlass = new ShaderUtil("liquidGlassV3.frag", "liquidGlassV3.vsh");
    //private static Framebuffer framebuffer;
    private static Framebuffer blurFramebuffer;

    //Liquid Glass V3 - Mipmap blur + Glass pass
    public static void updateMipMapBlurTexture() {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (blurFramebuffer == null || blurFramebuffer.framebufferWidth != width || blurFramebuffer.framebufferHeight != height) {
            if (blurFramebuffer != null) {
                blurFramebuffer.deleteFramebuffer();
            }
            blurFramebuffer = new Framebuffer(width, height, false);
            blurFramebuffer.setFramebufferFilter(GL11.GL_LINEAR);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurFramebuffer.framebufferTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                float max = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                GL11.glTexParameterf(
                        GL11.GL_TEXTURE_2D,
                        EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                        Math.min(16.0F, max)
                );
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        blurFramebuffer.framebufferClear();
        blurFramebuffer.bindFramebuffer(false);

        GlStateManager.disableBlend();
        mipmapCopy.init();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getFramebuffer().framebufferTexture);
        mipmapCopy.setUniformi("textureIn", 0);

        ShaderUtil.drawQuads();
        mipmapCopy.unload();
        blurFramebuffer.unbindFramebuffer();
        GlStateManager.enableBlend();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurFramebuffer.framebufferTexture);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        mc.getFramebuffer().bindFramebuffer(true);
    }

    public static void draw(
            float x, float y, float width, float height, float radius,
            float blurRadius,
            float refractionPower, float refractionEdge,
            float dispersion,
            float globalAlpha,
            float tintR, float tintG, float tintB, float tintStrength,
            float lumaPoint0, float lumaPoint1, float lumaPoint2, float lumaPoint3,
            float lumaMapForce, float extraBrightness
    ) {
        liquidGlass.init();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(blurFramebuffer.framebufferTexture);
        liquidGlass.setUniformf("uQuadSize", width, height);
        liquidGlass.setUniformf("uScreenSize", mc.displayWidth, mc.displayHeight);
        liquidGlass.setUniformf("uRadius", radius);
        liquidGlass.setUniformi("uBlurTex", 0);
        liquidGlass.setUniformf("uBlurRadius", blurRadius);
        liquidGlass.setUniformf("uTexSize", blurFramebuffer.framebufferWidth, blurFramebuffer.framebufferHeight);
        liquidGlass.setUniformf("uRefractionPower", refractionPower);
        liquidGlass.setUniformf("uRefractionEdge", refractionEdge);
        liquidGlass.setUniformf("uDispersion", dispersion);
        liquidGlass.setUniformf("uGlobalAlpha", globalAlpha);
        liquidGlass.setUniformf("uGlassTint", tintR, tintG, tintB, tintStrength);
        liquidGlass.setUniformf("uLumaCurve", lumaPoint0, lumaPoint1, lumaPoint2, lumaPoint3);
        liquidGlass.setUniformf("uLumaMapForce", lumaMapForce);
        liquidGlass.setUniformf("uExtraBrightness", extraBrightness);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        ShaderUtil.drawQuads(x, y, width, height);
        liquidGlass.unload();
        GlStateManager.color(1, 1, 1, 1);
    }

    //Liquid Glass V2 - Gaussian Blur + Glass pass(Clear & Tinted)
    /**
     private static void setupGaussianUniforms(float dir1, float dir2, float radius) {
     gaussianBlur.setUniformi("textureIn", 0);
     gaussianBlur.setUniformf("texelSize", 1.0F / framebuffer.framebufferWidth, 1.0F / framebuffer.framebufferHeight);
     gaussianBlur.setUniformf("direction", dir1, dir2);
     gaussianBlur.setUniformf("radius", radius);
     final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
     for (int i = 0; i <= radius; i++) {
     weightBuffer.put(MathUtil.calculateGaussianValue(i, radius / 2));
     }
     weightBuffer.rewind();
     OpenGlHelper.glUniform1(gaussianBlur.getUniform("weights"), weightBuffer);
     }

     public static void updateBlurTexture(int iterations, float blurRadius, float downScale) {
     int width = (int) (mc.displayWidth * downScale);
     int height = (int) (mc.displayHeight * downScale);
     if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
     if (framebuffer != null) {
     framebuffer.deleteFramebuffer();
     }
     framebuffer = new Framebuffer(width, height, false);
     framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
     }
     if (blurFramebuffer == null || blurFramebuffer.framebufferWidth != width || blurFramebuffer.framebufferHeight != height) {
     if (blurFramebuffer != null) {
     blurFramebuffer.deleteFramebuffer();
     }
     blurFramebuffer = new Framebuffer(width, height, false);
     blurFramebuffer.setFramebufferFilter(GL11.GL_LINEAR);
     }
     if (iterations == 0) {
     blurFramebuffer.framebufferClear();
     blurFramebuffer.bindFramebuffer(false);

     gaussianBlur.init();
     setupGaussianUniforms(0, 0, 1);

     GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.getFramebuffer().framebufferTexture);
     ShaderUtil.drawQuads();

     gaussianBlur.unload();
     blurFramebuffer.unbindFramebuffer();
     mc.getFramebuffer().bindFramebuffer(true);
     return;
     }
     for (int i = 0; i < iterations; i++) {
     framebuffer.framebufferClear();
     framebuffer.bindFramebuffer(false);
     gaussianBlur.init();
     setupGaussianUniforms(1, 0, blurRadius);

     GL11.glBindTexture(GL11.GL_TEXTURE_2D, i == 0 ? mc.getFramebuffer().framebufferTexture : blurFramebuffer.framebufferTexture);
     ShaderUtil.drawQuads();
     framebuffer.unbindFramebuffer();
     gaussianBlur.unload();

     blurFramebuffer.framebufferClear();
     blurFramebuffer.bindFramebuffer(false);
     gaussianBlur.init();
     setupGaussianUniforms(0, 1, blurRadius);

     GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
     ShaderUtil.drawQuads();
     blurFramebuffer.unbindFramebuffer();
     gaussianBlur.unload();
     }
     mc.getFramebuffer().bindFramebuffer(true);
     }

    public static void drawClear(float x, float y, float width, float height, float radius, float noise, float refractionPower, float glowWeight, float glowBias, float glowEdge0, float glowEdge1) {
        liquidGlassClear.init();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(blurFramebuffer.framebufferTexture);
        liquidGlassClear.setUniformi("uBlurTex", 0);
        liquidGlassClear.setUniformf("uPowerFactor", radius);
        liquidGlassClear.setUniformf("uNoise", noise);
        liquidGlassClear.setUniformf("uRefractionPower", refractionPower);
        liquidGlassClear.setUniformf("uGlowWeight", glowWeight);
        liquidGlassClear.setUniformf("uGlowBias", glowBias);
        liquidGlassClear.setUniformf("uGlowEdge0", glowEdge0);
        liquidGlassClear.setUniformf("uGlowEdge1", glowEdge1);
        liquidGlassClear.setUniformf("uQuadSize", width, height);
        liquidGlassClear.setUniformf("uQuadCenter", x + width * 0.5F, y + height * 0.5F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        ShaderUtil.drawQuads(x, y, width, height);
        liquidGlassClear.unload();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(0);
    }

    public static void drawTinted(float x, float y, float width, float height, float power, float noise, float refractionPower, float tintR, float tintG, float tintB, float tintStrength, float chromaStrength, float darkness) {
        liquidGlassTinted.init();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(blurFramebuffer.framebufferTexture);
        liquidGlassTinted.setUniformi("uBlurTex", 0);
        liquidGlassTinted.setUniformf("uPowerFactor", power);
        liquidGlassTinted.setUniformf("uNoise", noise);
        liquidGlassTinted.setUniformf("uRefractionPower", refractionPower);
        liquidGlassTinted.setUniformf("uTintColor", tintR, tintG, tintB);
        liquidGlassTinted.setUniformf("uTintStrength", tintStrength);
        liquidGlassTinted.setUniformf("uChromaStrength", chromaStrength);
        liquidGlassTinted.setUniformf("uDarkness", darkness);
        liquidGlassTinted.setUniformf("uQuadSize", width, height);
        liquidGlassTinted.setUniformf("uQuadCenter", x + width * 0.5F, y + height * 0.5F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        ShaderUtil.drawQuads(x, y, width, height);
        liquidGlassTinted.unload();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(0);
    }
     **/
}
