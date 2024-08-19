package net.devdude.lispcraft.mod.client.components;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.runtime.Console;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class CharGridComponent extends BaseComponent {
    static final int foreground = 0xFFFFFFFF;
    static final int background = 0xFF000000;

    TextRenderer textRenderer;
    Style style;
    Style styleUnderline;

    int xChars;
    int yChars;

    int glyphWidth;
    int glyphHeight;

    //    TODO: We should know the screen size before
    BufferProvider bufferProvider;
    CursorProvider cursorProvider;


    public CharGridComponent(int xChars, int yChars, BufferProvider bufferProvider, CursorProvider cursorProvider) {
        super();

        style = Style.EMPTY.withFont(Mod.id("mono"));
        styleUnderline = Style.EMPTY.withUnderline(true);

        textRenderer = MinecraftClient.getInstance().textRenderer;

        this.xChars = xChars;
        this.yChars = yChars;
        this.bufferProvider = bufferProvider;
        this.cursorProvider = cursorProvider;

        this.glyphWidth = textRenderer.getWidth(Text.literal("t").setStyle(style));
        this.glyphHeight = textRenderer.fontHeight;

        this.sizing(
                Sizing.fixed(glyphWidth * xChars),
                Sizing.fixed(glyphHeight * yChars)
        );
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.drawGradientRect(
                this.x, this.y, this.width, this.height,
                background, background, background, background
        );

        var screen = bufferProvider.getBuffer();
        for (int y = 0; y < yChars; y++) {
            var line = Text.literal(new String(screen[y])).setStyle(style);

            var lineY = this.y + (y * glyphHeight);
            var lineX = this.x;

            context.drawText(
                    line, lineX, lineY, 1.0f, foreground
            );

        }

        var cursor = cursorProvider.getCursor();
        var cursorY = this.y + (cursor.y() * glyphHeight);
        var cursorX = this.x + (cursor.x() * glyphWidth);
        context.drawText(Text.literal(" ").setStyle(styleUnderline), cursorX, cursorY, 1.0f, foreground);
    }

    @FunctionalInterface
    public interface BufferProvider {
        char[][] getBuffer();
    }

    @FunctionalInterface
    public interface CursorProvider {
        Console.Location getCursor();
    }
}
