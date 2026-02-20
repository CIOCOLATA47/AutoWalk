package me.cioco.autowalk.mixins;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(PlayerEntity.class)
public class AutowalkMixin {

    private final Random random = new Random();
    private int randomPauseCounter = 0;

    private boolean modPressedForward = false;
    private boolean modPressedBack = false;
    private boolean modPressedLeft = false;
    private boolean modPressedRight = false;
    private boolean modPressedSprint = false;

    private int lastSlot = -1;
    private boolean isEating = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (!mc.player.equals(this)) return;

        PlayerEntity player = mc.player;
        AutoWalkConfig config = AutoWalkConfig.getInstance();

        if (config.stopOnDamage && config.enabled && player.hurtTime > 0) {
            handleDisable(mc, config, "AutoWalk: Toggled off due to damage.");
            return;
        }

        if (!config.enabled) {
            releaseAllKeys(mc);
            return;
        }

        if (config.autoEat) {
            if (isEating) {
                if (player.getHungerManager().getFoodLevel() >= 20 || !player.isUsingItem()) {
                    stopEating(mc);
                } else {
                    mc.options.useKey.setPressed(true);
                }
            } else if (player.getHungerManager().getFoodLevel() <= config.eatHungerThreshold) {
                int foodSlot = findFoodSlot(player);
                if (foodSlot != -1) {
                    startEating(mc, foodSlot);
                }
            }
        }

        boolean pauseThisTick = false;
        if (!isEating && config.randomPauseEnabled && random.nextInt(200) == 0) {
            randomPauseCounter = 10 + random.nextInt(20);
        }

        if (randomPauseCounter > 0 && !isEating) {
            randomPauseCounter--;
            pauseThisTick = true;
        }

        if (pauseThisTick) {
            releaseAllKeys(mc);
        } else {
            boolean isGUIOpen = mc.currentScreen != null;

            if (isGUIOpen) {
                mc.options.forwardKey.setPressed(config.walkForward);
                mc.options.backKey.setPressed(config.walkBackwards);
                mc.options.leftKey.setPressed(config.walkLeft);
                mc.options.rightKey.setPressed(config.walkRight);

                modPressedForward = config.walkForward;
                modPressedBack = config.walkBackwards;
                modPressedLeft = config.walkLeft;
                modPressedRight = config.walkRight;
            } else {
                modPressedForward = pressKey(mc.options.forwardKey, config.walkForward);
                modPressedBack = pressKey(mc.options.backKey, config.walkBackwards);
                modPressedLeft = pressKey(mc.options.leftKey, config.walkLeft);
                modPressedRight = pressKey(mc.options.rightKey, config.walkRight);
            }
        }

        if (!pauseThisTick && config.sprinting && !player.isSwimming() && config.walkForward) {
            mc.options.sprintKey.setPressed(true);
            modPressedSprint = true;
        } else if (modPressedSprint) {
            mc.options.sprintKey.setPressed(false);
            modPressedSprint = false;
        }
    }

    private int findFoodSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    private void startEating(MinecraftClient mc, int slot) {
        lastSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.options.useKey.setPressed(true);
        isEating = true;
    }

    private void stopEating(MinecraftClient mc) {
        mc.options.useKey.setPressed(false);
        if (lastSlot != -1) {
            mc.player.getInventory().setSelectedSlot(lastSlot);
        }
        isEating = false;
        lastSlot = -1;
    }

    private void handleDisable(MinecraftClient mc, AutoWalkConfig config, String message) {
        releaseAllKeys(mc);
        config.enabled = false;
        config.save();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
        }
    }

    private boolean pressKey(net.minecraft.client.option.KeyBinding key, boolean shouldPress) {
        key.setPressed(shouldPress);
        return shouldPress;
    }

    private void releaseAllKeys(MinecraftClient mc) {
        if (modPressedForward) mc.options.forwardKey.setPressed(false);
        if (modPressedBack) mc.options.backKey.setPressed(false);
        if (modPressedLeft) mc.options.leftKey.setPressed(false);
        if (modPressedRight) mc.options.rightKey.setPressed(false);
        if (modPressedSprint) mc.options.sprintKey.setPressed(false);
        if (isEating) stopEating(mc);

        modPressedForward = modPressedBack = modPressedLeft = modPressedRight = modPressedSprint = false;
    }
}