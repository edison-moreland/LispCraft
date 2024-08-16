package net.devdude.lispcraft.mod.client.components;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.devdude.lispcraft.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class CharGridComponent extends BaseComponent {
    TextRenderer textRenderer;
    Style style;

    int xChars;
    int yChars;

    //    TODO: Screen provider should know the size
    ScreenProvider screenProvider;

    public CharGridComponent(int xChars, int yChars, ScreenProvider screenProvider) {
        super();

        style = Style.EMPTY.withFont(Mod.id("mono"));

        textRenderer = MinecraftClient.getInstance().textRenderer;

        this.xChars = xChars;
        this.yChars = yChars;
        this.screenProvider = screenProvider;

        var glyphWidth = textRenderer.getWidth(Text.literal("t").setStyle(style));
        var glyphHeight = textRenderer.fontHeight;

        this.sizing(
                Sizing.fixed(glyphWidth * xChars),
                Sizing.fixed(glyphHeight * yChars)
        );
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.drawGradientRect(
                this.x, this.y, this.width, this.height,
                0xFF000000, 0xFF000000,
                0xFF000000, 0xFF000000
        );

        var screen = screenProvider.getScreen();
        for (int y = 0; y < yChars; y++) {
            var line = Text.literal(new String(screen[y])).setStyle(style);

            var lineY = this.y + (y * textRenderer.fontHeight);
            var lineX = this.x;

            context.drawText(
                    line, lineX, lineY, 1.0f, 0xFFFFFFFF
            );

        }
    }

    @FunctionalInterface
    public interface ScreenProvider {
        char[][] getScreen();
    }
}
