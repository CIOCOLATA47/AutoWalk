package me.cioco.autowalk.gui;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AutoWalkScreen extends Screen {

    private static final int SPACING_Y = 24;
    private static final int SECTION_MARGIN = 35;
    private static final int TITLE_HEIGHT = 20;

    private final Screen parent;
    private final List<ClickableWidget> scrollableWidgets = new ArrayList<>();
    private final int ACCENT_COLOR = 0xFF00FBFF;
    private final int PANEL_BG = 0x90001520;
    private int scrollOffset = 0;
    private int maxScroll;
    private int contentHeight;
    private ButtonWidget doneButton;
    private ButtonWidget globalToggleButton;

    public AutoWalkScreen(Screen parent) {
        super(Text.literal("Auto-Walk Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearChildren();
        this.scrollableWidgets.clear();

        AutoWalkConfig config = AutoWalkConfig.getInstance();
        int centerX = width / 2;
        int leftCol = centerX - 155;
        int rightCol = centerX + 5;
        int startY = 70;
        int currentY = startY;

        addToggleButton(leftCol, currentY, "Forward", "Walks forward continuously", config.walkForward, v -> config.walkForward = v);
        addToggleButton(rightCol, currentY, "Backward", "Walks backward continuously", config.walkBackwards, v -> config.walkBackwards = v);
        currentY += SPACING_Y;
        addToggleButton(leftCol, currentY, "Left", "Strafes left", config.walkLeft, v -> config.walkLeft = v);
        addToggleButton(rightCol, currentY, "Right", "Strafes right", config.walkRight, v -> config.walkRight = v);

        currentY += SPACING_Y + SECTION_MARGIN;

        addToggleButton(leftCol, currentY, "Auto Sprint", "Forces sprinting while walking", config.sprinting, v -> config.sprinting = v);
        addToggleButton(rightCol, currentY, "Stop On Damage", "Disables mod if you get hit", config.stopOnDamage, v -> config.stopOnDamage = v);
        currentY += SPACING_Y;
        addToggleButton(leftCol, currentY, "Random Pause", "Will randomly stop for a bit", config.randomPauseEnabled, v -> config.randomPauseEnabled = v);

        currentY += SPACING_Y + SECTION_MARGIN;

        addToggleButton(leftCol, currentY, "Auto Eat", "Automatically eats when hungry", config.autoEat, v -> config.autoEat = v);
        addSlider(rightCol, currentY, 150, "Eat Threshold", config.eatHungerThreshold, 1.0f, 20.0f, v -> config.eatHungerThreshold = v);

        contentHeight = currentY + 40;
        maxScroll = Math.max(0, contentHeight - (height - 90));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        globalToggleButton = ButtonWidget.builder(
                getGlobalToggleText(),
                b -> {
                    config.enabled = !config.enabled;
                    b.setMessage(getGlobalToggleText());
                }
        ).dimensions(centerX - 100, height - 60, 200, 20).build();
        addDrawableChild(globalToggleButton);

        doneButton = ButtonWidget.builder(
                Text.literal("SAVE & EXIT").formatted(Formatting.AQUA, Formatting.BOLD),
                b -> this.close()
        ).dimensions(centerX - 100, height - 30, 200, 20).build();
        addDrawableChild(doneButton);

        for (ClickableWidget widget : scrollableWidgets) {
            widget.setY(widget.getY() - scrollOffset);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderInGameBackground(ctx);

        int cx = width / 2;
        int panelW = 325;
        int panelX = cx - (panelW / 2);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Auto-Walk Settings").formatted(Formatting.AQUA, Formatting.BOLD, Formatting.UNDERLINE),
                cx, 15, 0xFFFFFFFF);

        ctx.enableScissor(0, 40, width, height - 70);

        int currentY = 70 - scrollOffset;
        renderSectionGroup(ctx, panelX, currentY, panelW, 2, "Directional Movement");
        currentY += (SPACING_Y * 2) + SECTION_MARGIN;
        renderSectionGroup(ctx, panelX, currentY, panelW, 2, "Utility");
        currentY += (SPACING_Y * 2) + SECTION_MARGIN;
        renderSectionGroup(ctx, panelX, currentY, panelW, 1, "Helpers");

        for (ClickableWidget widget : scrollableWidgets) {
            widget.visible = (widget.getY() + widget.getHeight() > 40 && widget.getY() < height - 70);
            if (widget.visible) {
                widget.render(ctx, mouseX, mouseY, delta);
            }
        }
        ctx.disableScissor();

        globalToggleButton.render(ctx, mouseX, mouseY, delta);
        doneButton.render(ctx, mouseX, mouseY, delta);
        drawScrollBar(ctx);
    }

    private void renderSectionGroup(DrawContext ctx, int x, int y, int w, int buttonRows, String title) {
        int contentH = (buttonRows * SPACING_Y);
        drawStyledPanel(ctx, x, y - TITLE_HEIGHT - 5, w, contentH + TITLE_HEIGHT + 10);
        ctx.drawTextWithShadow(textRenderer, "§b§l» §f" + title, x + 8, y - TITLE_HEIGHT + 1, 0xFFFFFFFF);
        ctx.fill(x + 5, y - 6, x + w - 5, y - 5, 0x8000FBFF);
    }

    private void drawStyledPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL_BG);
        context.fill(x, y, x + 2, y + height, ACCENT_COLOR);
        context.fill(x + width - 2, y, x + width, y + height, ACCENT_COLOR);
    }

    private void drawScrollBar(DrawContext ctx) {
        if (maxScroll <= 0) return;
        int trackX = width - 6;
        int trackY = 40;
        int trackHeight = height - 110;
        int thumbHeight = Math.max(20, (int) ((float) trackHeight * (trackHeight / (float) contentHeight)));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * ((float) scrollOffset / maxScroll));
        ctx.fill(trackX, trackY, width - 2, trackY + trackHeight, 0x40000000);
        ctx.fill(trackX, thumbY, width - 2, thumbY + thumbHeight, ACCENT_COLOR);
    }

    private void addToggleButton(int x, int y, String label, String desc, boolean val, Consumer<Boolean> action) {
        ButtonWidget btn = ButtonWidget.builder(getToggleText(label, val), b -> {
            boolean currentlyOn = b.getMessage().getString().contains("ON");
            action.accept(!currentlyOn);
            b.setMessage(getToggleText(label, !currentlyOn));
        }).dimensions(x, y, 150, 20).tooltip(Tooltip.of(Text.literal("§b" + desc))).build();
        scrollableWidgets.add(btn);
        addDrawableChild(btn);
    }

    private void addSlider(int x, int y, int w, String label, float cur, float min, float max, Consumer<Float> action) {
        GenericSlider slider = new GenericSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(slider);
        addDrawableChild(slider);
    }

    private Text getToggleText(String label, boolean value) {
        return Text.literal(label + ": ").append(value ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
    }

    private Text getGlobalToggleText() {
        return Text.literal("AutoWalk: ").append(
                AutoWalkConfig.getInstance().enabled ? Text.literal("Enabled").formatted(Formatting.GREEN)
                        : Text.literal("Disabled").formatted(Formatting.RED)
        );
    }

    @Override
    public void close() {
        AutoWalkConfig.getInstance().save();
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private static class GenericSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final Consumer<Float> updateAction;

        public GenericSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            super(x, y, w, h, Text.empty(), (double) (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float val = min + (float) (this.value * (max - min));
            this.setMessage(Text.literal(label + ": §b" + String.format("%.0f", val)));
        }

        @Override
        protected void applyValue() {
            float val = min + (float) (this.value * (max - min));
            updateAction.accept(val);
        }
    }
}