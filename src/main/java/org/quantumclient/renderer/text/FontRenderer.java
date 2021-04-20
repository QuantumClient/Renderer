package org.quantumclient.renderer.text;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class FontRenderer {

    private GlyphPage font;
    private int[] colorCodes = new int[32];

    public FontRenderer(GlyphPage glyphPage) {
        this.font = glyphPage;

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) {
                k += 85;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCodes[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }
        font.make();
    }

    public void drawString(MatrixStack matrix, String s, float x, float y, boolean shadow, Color color) {

        if(shadow) {
            RenderSystem.color4f(0, 0, 0, 0.8f);

            float shadowX = (x + 0.2f);
            for(int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if(c == 167 && i + 1 < s.length()) ++i;
                else shadowX += font.drawChar(matrix, c, shadowX, y);
            }
            RenderSystem.color4f(1, 1, 1, 1);
        }

        RenderSystem.color4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
        drawString(matrix, s, x, y);

    }

    public void drawString(MatrixStack matrix, String text, float x, float y) {
        if (font.getTexture() == null) {
            font.make();
        }
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if(c == 167 && i + 1 < text.length()) {
                int colorCode = "0123456789abcdefklmnor".indexOf(String.valueOf(text.charAt(i + 1)).toLowerCase().charAt(0));
                formatColor(colorCodes[colorCode]);
                ++i;
            } else x += font.drawChar(matrix, c, x, y);
        }
    }

    private void formatColor(int color) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float blue = (float) (color >> 8 & 255) / 255.0F;
        float green = (float) (color & 255) / 255.0F;
        RenderSystem.color4f(red, blue, green, 1);
    }

    public float getWidth(String s) {
        return font.getStringWidth(s);
    }


}
