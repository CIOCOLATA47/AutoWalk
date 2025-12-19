package me.cioco.autowalk.mixins;

import me.cioco.autowalk.Main;
import me.cioco.autowalk.commands.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
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

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity player = mc.player;

        if (StopOnDamage.stopondamage && Main.toggled && player.hurtTime > 0) {
            releaseAllKeys(mc);
            Main.toggled = false;
            player.sendMessage(
                    Text.literal("AutoWalk: The mod has been toggled off due to damage.")
                            .formatted(Formatting.RED), false
            );
            return;
        }

        if (!Main.toggled) {
            releaseAllKeys(mc);
            return;
        }

        boolean pauseThisTick = false;
        if (RandomPause.randomPauseEnabled && random.nextInt(200) == 0) {
            randomPauseCounter = 10 + random.nextInt(20);
        }
        if (randomPauseCounter > 0) {
            randomPauseCounter--;
            pauseThisTick = true;
        }

        if (pauseThisTick) {
            releaseAllKeys(mc);
        } else {
            boolean isGUIOpen = mc.currentScreen != null;

            boolean forward = WalkForward.walkforward;
            boolean back = WalkBackwards.walkbackwards;
            boolean left = WalkLeft.walkleft;
            boolean right = WalkRight.walkright;

            if (isGUIOpen) {
                mc.options.forwardKey.setPressed(forward);
                mc.options.backKey.setPressed(back);
                mc.options.leftKey.setPressed(left);
                mc.options.rightKey.setPressed(right);

                modPressedForward = forward;
                modPressedBack = back;
                modPressedLeft = left;
                modPressedRight = right;
            } else {
                modPressedForward = pressKey(mc.options.forwardKey, forward);
                modPressedBack = pressKey(mc.options.backKey, back);
                modPressedLeft = pressKey(mc.options.leftKey, left);
                modPressedRight = pressKey(mc.options.rightKey, right);
            }
        }

        if (!pauseThisTick && ToggleSprint.sprinting && !player.isSwimming()) {
            mc.options.sprintKey.setPressed(true);
            modPressedSprint = true;
        } else if (modPressedSprint) {
            mc.options.sprintKey.setPressed(false);
            modPressedSprint = false;
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

        modPressedForward = modPressedBack = modPressedLeft = modPressedRight = modPressedSprint = false;
    }
}
