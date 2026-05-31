package coolpit.util.render.shader.impl;

import coolpit.util.math.MathUtil;
import coolpit.util.render.shader.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

/**
 * @author Jacques
 * @since 2026/05/15
 */
public class LiquidGlass {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ShaderUtil gaussianBlur = new ShaderUtil("gaussianBlur.frag", "vertex.vsh");
    private static final ShaderUtil liquidGlass = new ShaderUtil("liquidGlass.frag", "liquidGlass.vsh");
    private static Framebuffer framebuffer;
    private static Framebuffer blurFramebuffer;

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

    public static void updateBlurTexture(int iterations, float radius, float downScale) {
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
            setupGaussianUniforms(1, 0, radius);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, i == 0 ? mc.getFramebuffer().framebufferTexture : blurFramebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
            framebuffer.unbindFramebuffer();
            gaussianBlur.unload();

            blurFramebuffer.framebufferClear();
            blurFramebuffer.bindFramebuffer(false);
            gaussianBlur.init();
            setupGaussianUniforms(0, 1, radius);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
            blurFramebuffer.unbindFramebuffer();
            gaussianBlur.unload();
        }
        mc.getFramebuffer().bindFramebuffer(true);
    }

    public static void draw(float x, float y, float width, float height, float radius, float noise, float refractionPower) {
        x = Math.round(x);
        y = Math.round(y);
        width = Math.round(width);
        height = Math.round(height);
        liquidGlass.init();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(blurFramebuffer.framebufferTexture);
        liquidGlass.setUniformi("uBlurTex", 0);
        liquidGlass.setUniformf("uResolution", mc.displayWidth, mc.displayHeight);
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        liquidGlass.setUniformf("uGlassPixelSize", width * scaledResolution.getScaleFactor(), height * scaledResolution.getScaleFactor());
        liquidGlass.setUniformf(
                "uMidPoint",
                (x + width * 0.5f) * scaledResolution.getScaleFactor() / mc.displayWidth * 2.0f - 1.0f,
                1.0f - (y + height * 0.5f) * scaledResolution.getScaleFactor() / mc.displayHeight * 2.0f
        );
        liquidGlass.setUniformf(
                "uQuadNDC2ScreenNDCScale",
                (width * scaledResolution.getScaleFactor()) / mc.displayWidth,
                (height * scaledResolution.getScaleFactor()) / mc.displayHeight
        );
        liquidGlass.setUniformf("uRadius", radius);
        liquidGlass.setUniformf("uNoise", noise);
        liquidGlass.setUniformf("uRefractionPower", refractionPower);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        ShaderUtil.drawQuads(x, y, width, height);
        liquidGlass.unload();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.bindTexture(0);
    }
}
