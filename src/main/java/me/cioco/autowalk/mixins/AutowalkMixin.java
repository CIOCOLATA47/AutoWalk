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

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity player = mc.player;

        if (StopOnDamage.stopondamage && Main.toggled && player.hurtTime > 0) {
            releaseKeys(mc);
            Main.toggled = false;
            mc.player.sendMessage(
                    Text.literal("AutoWalk: The mod has been toggled off due to damage.").formatted(Formatting.RED),
                    false
            );
            return;
        }

        boolean isGUIOpen = mc.currentScreen != null;
        boolean isGUIClosed = mc.currentScreen == null;

        boolean pauseThisTick = false;
        if (Main.toggled && RandomPause.randomPauseEnabled && random.nextInt(200) == 0) {
            randomPauseCounter = 10 + random.nextInt(20);
        }
        if (randomPauseCounter > 0) {
            randomPauseCounter--;
            pauseThisTick = true;
        }

        if (Main.toggled && !pauseThisTick && isGUIClosed) {
            modPressedForward = pressKey(mc.options.forwardKey, WalkForward.walkforward);
            modPressedBack = pressKey(mc.options.backKey, WalkBackwards.walkbackwards);
            modPressedLeft = pressKey(mc.options.leftKey, WalkLeft.walkleft);
            modPressedRight = pressKey(mc.options.rightKey, WalkRight.walkright);
        }

        if (Main.toggled && isGUIOpen) {
            if (WalkForward.walkforward) mc.options.forwardKey.setPressed(true);
            if (WalkBackwards.walkbackwards) mc.options.backKey.setPressed(true);
            if (WalkLeft.walkleft) mc.options.leftKey.setPressed(true);
            if (WalkRight.walkright) mc.options.rightKey.setPressed(true);
        }

        if (Main.toggled && !pauseThisTick && ToggleSprint.sprinting) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }

        if (!Main.toggled || pauseThisTick) {
            releaseKeys(mc);
        }
    }

    private boolean pressKey(net.minecraft.client.option.KeyBinding key, boolean shouldPress) {
        if (shouldPress) {
            key.setPressed(true);
            return true;
        }
        return false;
    }

    private void releaseKeys(MinecraftClient mc) {
        if (modPressedForward) mc.options.forwardKey.setPressed(false);
        if (modPressedBack) mc.options.backKey.setPressed(false);
        if (modPressedLeft) mc.options.leftKey.setPressed(false);
        if (modPressedRight) mc.options.rightKey.setPressed(false);

        modPressedForward = modPressedBack = modPressedLeft = modPressedRight = false;
    }
}
