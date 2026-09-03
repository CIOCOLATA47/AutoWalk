package me.cioco.autowalk.mixins;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class AutowalkHudMixin {

    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void renderAutoWalkStatus(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        AutoWalkConfig config = AutoWalkConfig.getInstance();

        if (!config.enabled) {
            return;
        }

        if (client.player == null || client.gui.screen() != null) {
            return;
        }

        String status = buildStatusText(client, config);

        int x = getHudX(client, config, status);
        int y = getHudY(client, config);

        graphics.text(client.font, status, x, y, 0xFFFFFFFF, true);
    }

    private String buildStatusText(Minecraft client, AutoWalkConfig config) {
        StringBuilder text = new StringBuilder();

        text.append("§a[AutoWalk] §f");
        text.append(getMovementDirection(config));

        if (config.sprinting) {
            text.append(" §b⚡");
        }

        text.append(" §7[§f");
        text.append(getMovementMode(config));
        text.append("§7]");

        if (config.autoEat) {
            text.append(getFoodStatus(client, config));
        }

        if (config.showDetailedHud) {
            text.append("\n§7");
            appendDetailedStatus(text, client, config);
        }

        return text.toString();
    }

    private String getMovementDirection(AutoWalkConfig config) {
        StringBuilder direction = new StringBuilder();

        if (config.moveForward) {
            direction.append("↑");
        }

        if (config.moveBack) {
            direction.append("↓");
        }

        if (config.moveLeft) {
            direction.append("←");
        }

        if (config.moveRight) {
            direction.append("→");
        }

        return direction.isEmpty() ? "⏸" : direction.toString();
    }

    private String getMovementMode(AutoWalkConfig config) {
        String mode = config.movementMode.name();

        return mode.substring(
                0,
                Math.min(3, mode.length())
        ).toLowerCase();
    }

    private String getFoodStatus(Minecraft client, AutoWalkConfig config) {
        int hunger = client.player.getFoodData().getFoodLevel();

        return hunger <= config.eatHungerThreshold
                ? " §c🍖"
                : " §a🍖";
    }

    private void appendDetailedStatus(
            StringBuilder text,
            Minecraft client,
            AutoWalkConfig config
    ) {
        int hunger = client.player.getFoodData().getFoodLevel();
        float health = client.player.getHealth();

        text.append("🍖 ")
                .append(hunger)
                .append("/20 ");

        text.append("❤ ")
                .append(String.format("%.1f", health))
                .append(" ");

        if (config.avoidHostileMobs) {
            text.append("🛡Monster ");
        }

        if (config.avoidPlayers) {
            text.append("🛡Player ");
        }

        if (config.damageResponse != AutoWalkConfig.DamageResponse.IGNORE) {
            text.append("⚔")
                    .append(config.damageResponse.name().charAt(0));
        }
    }

    private int getHudX(
            Minecraft client,
            AutoWalkConfig config,
            String text
    ) {
        if (isRightAligned(config)) {
            int textWidth = client.font.width(text);
            return client.getWindow().getGuiScaledWidth() - textWidth - 5;
        }

        return config.hudX;
    }

    private int getHudY(
            Minecraft client,
            AutoWalkConfig config
    ) {
        if (isBottomAligned(config)) {
            return client.getWindow().getGuiScaledHeight() - 30;
        }

        return config.hudY;
    }

    private boolean isRightAligned(AutoWalkConfig config) {
        return config.hudPosition == AutoWalkConfig.HudPosition.TOP_RIGHT
                || config.hudPosition == AutoWalkConfig.HudPosition.BOTTOM_RIGHT;
    }

    private boolean isBottomAligned(AutoWalkConfig config) {
        return config.hudPosition == AutoWalkConfig.HudPosition.BOTTOM_LEFT
                || config.hudPosition == AutoWalkConfig.HudPosition.BOTTOM_RIGHT;
    }
}