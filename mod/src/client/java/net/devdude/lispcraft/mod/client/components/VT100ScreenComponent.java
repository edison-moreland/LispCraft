package net.devdude.lispcraft.mod.client.components;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.devdude.lispcraft.mod.Mod;
import net.devdude.lispcraft.mod.common.vt100.Character;
import net.devdude.lispcraft.mod.common.vt100.Size;
import net.devdude.lispcraft.mod.common.vt100.Style;
import net.devdude.lispcraft.mod.common.vt100.VT100ScreenBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public class VT100ScreenComponent extends BaseComponent {
    static final int cursorColor = 0xFFFFFFFF;
    static final int fullAlpha = 0xFF000000;

    TextRenderer textRenderer;
    net.minecraft.text.Style style;
    net.minecraft.text.Style styleUnderline;

    int glyphWidth;
    int glyphHeight;

    ScreenProvider screenProvider;

    public VT100ScreenComponent(ScreenProvider screenProvider) {
        super();

        style = net.minecraft.text.Style.EMPTY.withFont(Mod.id("mono"));
        styleUnderline = style.withUnderline(true);

        textRenderer = MinecraftClient.getInstance().textRenderer;

        this.screenProvider = screenProvider;

        this.glyphWidth = textRenderer.getWidth(Text.literal("t").setStyle(style));
        this.glyphHeight = textRenderer.fontHeight;

        this.updateSize(this.screenProvider.getScreen().size);
    }

    private void updateSize(Size size) {
        this.sizing(
                Sizing.fixed(glyphWidth * size.width()),
                Sizing.fixed(glyphHeight * size.height())
        );
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {

        // TODO: WE can draw text in one call if we draw the background for a whole line before
        //       We could also draw the background in as many calls as there are colors
        var screen = screenProvider.getScreen();
        this.updateSize(screen.getSize());
        for (int y = 0; y < screen.size.height(); y++) {
            for (int x = 0; x < screen.size.width(); x++) {
                drawGlyph(context, x, y, screen.getCharacterAt(x, y));
            }
        }

        var cursor = screen.getCursor();
        drawCursor(context, cursor.x(), cursor.y());
    }

    private void drawGlyph(OwoUIDrawContext context, int x, int y, Character character) {
        var glyphX = this.x + (x * glyphWidth);
        var glyphY = this.y + (y * glyphHeight);

        var text = styledText(character);

        // Set the alpha value for both colors to 0xFF
        var foreground = fullAlpha | character.style().foreground();
        var background = fullAlpha | character.style().background();
        if (character.style().negative()) {
            var t = foreground;
            foreground = background;
            background = t;
        }

        context.fill(glyphX, glyphY, glyphX + glyphWidth, glyphY + glyphHeight, background);
        context.drawText(text, glyphX, glyphY, 1.0f, foreground);
    }

    private void drawCursor(OwoUIDrawContext context, int x, int y) {
        var cursorX = this.x + (x * glyphWidth);
        var cursorY = this.y + (y * glyphHeight);

        var text = Text.literal(" ").setStyle(styleUnderline);

        context.drawText(text, cursorX, cursorY, 1.0f, cursorColor);
    }

    private Text styledText(Character character) {
        var style = this.style;
        switch (character.style().intensity()) {
            case Style.Intensity.BOLD -> style = style.withBold(true);
            //            case Style.Intensity.FAINT -> style = style.with
        }

        if (character.style().italicized()) {
            style = style.withItalic(true);
        }
        if (character.style().underlined()) {
            style = style.withUnderline(true);
        }
        if (character.style().concealed()) {
            style = style.withObfuscated(true);
        }
        if (character.style().crossedOut()) {
            style = style.withStrikethrough(true);
        }

        return Text.literal(String.valueOf(character.codepoint())).setStyle(style);
    }

    @FunctionalInterface
    public interface ScreenProvider {
        VT100ScreenBuffer getScreen();
    }
}
