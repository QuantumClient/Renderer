package org.quantumclient.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class GlyphPage {

    private final float scale;
    private int width;
    private int height;
    private final Font font;
    private HashMap<Character, Glyph> glyphCharacterMap = new HashMap<>();

    private BufferedImage bufferedImage;
    private AbstractTexture texture;

    public GlyphPage(Font font, int scale) {
        this.font = font;
        this.scale = 256 / scale * 0.04f;
    }

    public void make() {
        char[] chars = new char[256];
        for(int i = 0; i < chars.length; i++) chars[i] = (char) i;
        AffineTransform affineTransform = new AffineTransform();
        FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, true, true);

        float charWidth = 0;
        float charHeight = 0;
        for (char c: chars) {
            Rectangle2D bounds = font.getStringBounds(Character.toString(c), fontRenderContext);

            float width = (float) bounds.getWidth();
            float height = (float) bounds.getHeight();

            if(width > charWidth) charWidth = width;
            if(height > charHeight) charHeight = height;
        }
        width = (int) (charWidth * 16);
        height = (int) (charHeight * 16);

        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();

        graphics2D.setFont(font);
        graphics2D.setColor(new Color(255, 255, 255, 0));
        graphics2D.fillRect(0, 0, width, height);
        graphics2D.setColor(Color.white);

        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        for(int i = 0; i < chars.length; i++) {
            int x = (int) (i % 16 * charWidth);
            int y = (int) (i / 16 * charHeight);
            Rectangle2D bounds = fontMetrics.getStringBounds(Character.toString(chars[i]), graphics2D);

            Glyph glyph = new Glyph(x, y, (float) bounds.getWidth(), (float) bounds.getHeight());
            glyphCharacterMap.put(chars[i], glyph);

            graphics2D.drawString(Character.toString(chars[i]), x, y + fontMetrics.getAscent());
        }

        AbstractTexture texture1;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] bytes = baos.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();

            texture1 = new NativeImageBackedTexture(NativeImage.read(data));
        } catch (Exception e) {
            texture1 = null;
            e.printStackTrace();
        }
        texture = texture1;

    }


    public float drawChar(MatrixStack matrix, char c, float x, float y) {
        Glyph glyph = glyphCharacterMap.get(c);
        if(glyph == null) return 0;

        float texX = glyph.x / width;
        float texY = glyph.y /  height;
        float texWidth = glyph.width / width;
        float texHeight = glyph.height / height;

        float scaledWidth = glyph.width * scale;
        float scaledHeight = glyph.height * scale;

        if(texture != null) {
            RenderSystem.bindTexture(texture.getGlId());

            Matrix4f matrices = matrix.peek().getModel();
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
            bufferBuilder.vertex(matrices, x, y + scaledHeight, 0).texture( texX, texY + texHeight).next();
            bufferBuilder.vertex(matrices, x + scaledWidth,  y + scaledHeight, 0).texture( texX + texWidth,  texY + texHeight).next();
            bufferBuilder.vertex(matrices, x + scaledWidth, y, 0).texture( texX + texWidth, texY).next();
            bufferBuilder.vertex(matrices,  x, y, 0).texture( texX, texY).next();
            bufferBuilder.end();

            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.enableTexture();
            RenderSystem.disableLighting();
            RenderSystem.disableCull();
            RenderSystem.lineWidth(1);
            BufferRenderer.draw(bufferBuilder);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.enableTexture();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
        return glyph.width * scale;
    }

    public float getCharWidth(char c) {
        Glyph glyph = glyphCharacterMap.get(c);
        if(glyph == null) return 0;
        return glyph.width * scale;
    }

    public float getStringWidth(String text) {
        float width = 0;
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if(c == 167 && i + 1 < text.length()) i++;
            else width += getCharWidth(c);
        }

        return width;
    }

    AbstractTexture getTexture() {
        return texture;
    }

}
