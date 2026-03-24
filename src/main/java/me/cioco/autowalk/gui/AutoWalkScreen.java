package me.cioco.autowalk.gui;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AutoWalkScreen extends Screen {

    private static final int SPACING_Y      = 24;
    private static final int SECTION_MARGIN = 35;
    private static final int TITLE_HEIGHT   = 20;

    private final Screen parent;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final int ACCENT_COLOR = 0xFF00FBFF;
    private final int PANEL_BG     = 0x90001520;
    private int scrollOffset = 0;
    private int maxScroll;
    private int contentHeight;
    private Button doneButton;
    private Button globalToggleButton;

    private final int[] sectionY    = new int[9];
    private final int[] sectionRows = new int[9];

    public AutoWalkScreen(Screen parent) {
        super(Component.literal("Auto-Walk Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.scrollableWidgets.clear();

        AutoWalkConfig config = AutoWalkConfig.getInstance();
        int centerX  = width / 2;
        int leftCol  = centerX - 155;
        int rightCol = centerX + 5;
        int y        = 70;

        sectionY[0] = y; sectionRows[0] = 2;
        addToggleButton(leftCol,  y, "Forward",  "Walks forward continuously",  config.moveForward, v -> config.moveForward = v);
        addToggleButton(rightCol, y, "Backward", "Walks backward continuously", config.moveBack,    v -> config.moveBack    = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Left",  "Strafes left",  config.moveLeft,  v -> config.moveLeft  = v);
        addToggleButton(rightCol, y, "Right", "Strafes right", config.moveRight, v -> config.moveRight = v);
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[1] = y; sectionRows[1] = 1;
        addCycleButton(leftCol, y, 310, "Mode",
                new String[]{"MANUAL", "RANDOM", "CIRCLE"},
                config.movementMode.name(),
                v -> config.movementMode = AutoWalkConfig.MovementMode.valueOf(v),
                "MANUAL = use direction toggles above.\nRANDOM = randomly change direction.\nCIRCLE = rotate direction over time.");
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[2] = y; sectionRows[2] = 1;
        addToggleButton(leftCol,  y, "Auto Sprint",  "Forces sprinting while walking", config.sprinting,          v -> config.sprinting          = v);
        addToggleButton(rightCol, y, "Random Pause", "Will randomly stop for a bit",   config.randomPauseEnabled, v -> config.randomPauseEnabled = v);
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[3] = y; sectionRows[3] = 1;
        addCycleButton(leftCol, y, 310, "On Damage",
                new String[]{"STOP", "TURN_BACK", "RANDOM_TURN", "JUMP", "IGNORE"},
                config.damageResponse.name(),
                v -> config.damageResponse = AutoWalkConfig.DamageResponse.valueOf(v),
                "What to do when you take damage:\nSTOP = disable AutoWalk\nTURN_BACK = reverse direction\nRANDOM_TURN = random direction\nJUMP = jump\nIGNORE = keep walking");
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[4] = y; sectionRows[4] = 1;
        addToggleButton(leftCol,  y, "Auto Eat", "Automatically eats when hungry", config.autoEat, v -> config.autoEat = v);
        addSlider(rightCol, y, 150, "Eat Threshold", config.eatHungerThreshold, 1.0f, 20.0f, v -> config.eatHungerThreshold = v);
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[5] = y; sectionRows[5] = 2;
        addToggleButton(leftCol,  y, "Auto Jump",   "Jumps over 1-block obstacles", config.autoJump,     v -> config.autoJump     = v);
        addToggleButton(rightCol, y, "Water Float", "Stay afloat in water",         config.waterSurface, v -> config.waterSurface = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Avoid Drops", "Stops before big drops", config.avoidDrops, v -> config.avoidDrops = v);
        addIntSlider(rightCol, y, 150, "Drop Threshold", config.jumpDropThreshold, 1, 30, v -> config.jumpDropThreshold = v);
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[6] = y; sectionRows[6] = 2;
        addToggleButton(leftCol,  y, "Avoid Lava",       "Stops if lava is ahead",       config.avoidLava,      v -> config.avoidLava      = v);
        addToggleButton(rightCol, y, "Avoid Fire",        "Stops if fire is ahead",       config.avoidFire,      v -> config.avoidFire      = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Avoid Cactus",     "Stops if cactus is ahead",     config.avoidCactus,    v -> config.avoidCactus    = v);
        addToggleButton(rightCol, y, "Avoid Berry Bush", "Stops if berry bush is ahead", config.avoidBerryBush, v -> config.avoidBerryBush = v);
        y += SPACING_Y + SECTION_MARGIN;

        sectionY[7] = y; sectionRows[7] = 2;
        addToggleButton(leftCol,  y, "Avoid Mobs",    "Stops near hostile mobs",   config.avoidHostileMobs, v -> config.avoidHostileMobs = v);
        addSlider(rightCol, y, 150, "Mob Distance", config.hostileAvoidDistance, 1.0f, 20.0f, v -> config.hostileAvoidDistance = v);
        y += SPACING_Y;
        addToggleButton(leftCol,  y, "Avoid Players", "Stops near other players", config.avoidPlayers, v -> config.avoidPlayers = v);
        addSlider(rightCol, y, 150, "Player Distance", config.playerAvoidDistance, 1.0f, 20.0f, v -> config.playerAvoidDistance = v);
        y += SPACING_Y + SECTION_MARGIN;

        contentHeight = y + 40;
        maxScroll = Math.max(0, contentHeight - (height - 90));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        globalToggleButton = Button.builder(getGlobalToggleText(), b -> {
            config.enabled = !config.enabled;
            b.setMessage(getGlobalToggleText());
        }).bounds(centerX - 100, height - 60, 200, 20).build();
        addRenderableWidget(globalToggleButton);

        doneButton = Button.builder(
                Component.literal("SAVE & EXIT").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                b -> this.onClose()
        ).bounds(centerX - 100, height - 30, 200, 20).build();
        addRenderableWidget(doneButton);

        for (AbstractWidget widget : scrollableWidgets) {
            widget.setY(widget.getY() - scrollOffset);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);

        int cx     = width / 2;
        int panelW = 325;
        int panelX = cx - (panelW / 2);

        ctx.centeredText(font,
                Component.literal("Auto-Walk Settings").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                cx, 15, 0xFFFFFFFF);

        ctx.enableScissor(0, 40, width, height - 70);

        String[] titles = {
                "Directional Movement",
                "Movement Mode",
                "Utility",
                "Damage Response",
                "Auto Eat",
                "Auto Jump & Drops",
                "Hazard Avoidance",
                "Entity Avoidance"
        };

        for (int i = 0; i < sectionY.length - 1; i++) {
            renderSectionGroup(ctx, panelX, sectionY[i] - scrollOffset, panelW, sectionRows[i], titles[i]);
        }

        for (AbstractWidget widget : scrollableWidgets) {
            widget.visible = (widget.getY() + widget.getHeight() > 40 && widget.getY() < height - 70);
            if (widget.visible) {
                widget.extractRenderState(ctx, mouseX, mouseY, delta);
            }
        }

        ctx.disableScissor();

        globalToggleButton.extractRenderState(ctx, mouseX, mouseY, delta);
        doneButton.extractRenderState(ctx, mouseX, mouseY, delta);

        drawScrollBar(ctx);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            int oldOffset = scrollOffset;
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - (verticalAmount * 25)));
            int diff = oldOffset - scrollOffset;
            for (AbstractWidget widget : scrollableWidgets) {
                widget.setY(widget.getY() + diff);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderSectionGroup(GuiGraphicsExtractor ctx, int x, int y, int w, int rows, String title) {
        int contentH = rows * SPACING_Y;
        drawStyledPanel(ctx, x, y - TITLE_HEIGHT - 5, w, contentH + TITLE_HEIGHT + 10);
        ctx.text(font, "§b§l» §f" + title, x + 8, y - TITLE_HEIGHT + 1, 0xFFFFFFFF);
        ctx.fill(x + 5, y - 6, x + w - 5, y - 5, 0x8000FBFF);
    }

    private void drawStyledPanel(GuiGraphicsExtractor ctx, int x, int y, int width, int height) {
        ctx.fill(x, y, x + width, y + height, PANEL_BG);
        ctx.fill(x, y, x + 2, y + height, ACCENT_COLOR);
        ctx.fill(x + width - 2, y, x + width, y + height, ACCENT_COLOR);
    }

    private void drawScrollBar(GuiGraphicsExtractor ctx) {
        if (maxScroll <= 0) return;
        int trackX      = width - 6;
        int trackY      = 40;
        int trackHeight = height - 110;
        int thumbHeight = Math.max(20, (int) ((float) trackHeight * (trackHeight / (float) contentHeight)));
        int thumbY      = trackY + (int) ((trackHeight - thumbHeight) * ((float) scrollOffset / maxScroll));
        ctx.fill(trackX, trackY, width - 2, trackY + trackHeight, 0x40000000);
        ctx.fill(trackX, thumbY, width - 2, thumbY + thumbHeight, ACCENT_COLOR);
    }

    private void addToggleButton(int x, int y, String label, String desc, boolean val, Consumer<Boolean> action) {
        Button btn = Button.builder(getToggleText(label, val), b -> {
            boolean currentlyOn = b.getMessage().getString().contains("ON");
            action.accept(!currentlyOn);
            b.setMessage(getToggleText(label, !currentlyOn));
        }).bounds(x, y, 150, 20).tooltip(Tooltip.create(Component.literal("§b" + desc))).build();
        scrollableWidgets.add(btn);
        addRenderableWidget(btn);
    }

    private void addCycleButton(int x, int y, int w, String label, String[] options, String current, Consumer<String> action, String desc) {
        int[] idx = {0};
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) { idx[0] = i; break; }
        }
        Button btn = Button.builder(
                getCycleText(label, options[idx[0]]),
                b -> {
                    idx[0] = (idx[0] + 1) % options.length;
                    action.accept(options[idx[0]]);
                    b.setMessage(getCycleText(label, options[idx[0]]));
                }
        ).bounds(x, y, w, 20).tooltip(Tooltip.create(Component.literal("§b" + desc))).build();
        scrollableWidgets.add(btn);
        addRenderableWidget(btn);
    }

    private void addSlider(int x, int y, int w, String label, float cur, float min, float max, Consumer<Float> action) {
        GenericSlider slider = new GenericSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(slider);
        addRenderableWidget(slider);
    }

    private void addIntSlider(int x, int y, int w, String label, int cur, int min, int max, Consumer<Integer> action) {
        IntSlider slider = new IntSlider(x, y, w, 20, label, cur, min, max, action);
        scrollableWidgets.add(slider);
        addRenderableWidget(slider);
    }

    private Component getToggleText(String label, boolean value) {
        return Component.literal(label + ": ").append(
                value ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                        : Component.literal("OFF").withStyle(ChatFormatting.RED));
    }

    private Component getCycleText(String label, String value) {
        return Component.literal(label + ": ").append(
                Component.literal(value).withStyle(ChatFormatting.AQUA));
    }

    private Component getGlobalToggleText() {
        return Component.literal("AutoWalk: ").append(
                AutoWalkConfig.getInstance().enabled
                        ? Component.literal("Enabled").withStyle(ChatFormatting.GREEN)
                        : Component.literal("Disabled").withStyle(ChatFormatting.RED));
    }

    @Override
    public void onClose() {
        AutoWalkConfig.getInstance().save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
    
    private static class GenericSlider extends AbstractSliderButton {
        private final String label;
        private final float min, max;
        private final Consumer<Float> updateAction;

        public GenericSlider(int x, int y, int w, int h, String label, float cur, float min, float max, Consumer<Float> action) {
            super(x, y, w, h, Component.empty(), (double) (cur - min) / (max - min));
            this.label        = label;
            this.min          = min;
            this.max          = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float val = min + (float) (this.value * (max - min));
            setMessage(Component.literal(label + ": §b" + String.format("%.1f", val)));
        }

        @Override
        protected void applyValue() {
            float val = min + (float) (this.value * (max - min));
            updateAction.accept(val);
        }
    }

    private static class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min, max;
        private final Consumer<Integer> updateAction;

        public IntSlider(int x, int y, int w, int h, String label, int cur, int min, int max, Consumer<Integer> action) {
            super(x, y, w, h, Component.empty(), (double) (cur - min) / (max - min));
            this.label        = label;
            this.min          = min;
            this.max          = max;
            this.updateAction = action;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int val = min + (int) Math.round(this.value * (max - min));
            setMessage(Component.literal(label + ": §b" + val));
        }

        @Override
        protected void applyValue() {
            int val = min + (int) Math.round(this.value * (max - min));
            updateAction.accept(val);
        }
    }
}