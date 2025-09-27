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

    private boolean forwardKeyState = false;
    private boolean backwardKeyState = false;
    private boolean leftKeyState = false;
    private boolean rightKeyState = false;

    private final Random random = new Random();
    private int randomPauseCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity player = mc.player;

        if (StopOnDamage.stopondamage && Main.toggled && player.hurtTime > 0) {
            resetKeys(mc);
            Main.toggled = false;
            mc.player.sendMessage(Text.literal("AutoWalk: The mod has been toggled off due to damage.").formatted(Formatting.RED), false);
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

        if (Main.toggled && !pauseThisTick) {
            if (WalkForward.walkforward && isGUIClosed) {
                setKey(mc.options.forwardKey, true, forwardKeyState);
                forwardKeyState = true;
            } else if (forwardKeyState) {
                setKey(mc.options.forwardKey, false, forwardKeyState);
                forwardKeyState = false;
            }

            if (WalkBackwards.walkbackwards) {
                setKey(mc.options.backKey, true, backwardKeyState);
                backwardKeyState = true;
            } else if (backwardKeyState) {
                setKey(mc.options.backKey, false, backwardKeyState);
                backwardKeyState = false;
            }

            if (WalkLeft.walkleft) {
                setKey(mc.options.leftKey, true, leftKeyState);
                leftKeyState = true;
            } else if (leftKeyState) {
                setKey(mc.options.leftKey, false, leftKeyState);
                leftKeyState = false;
            }

            if (WalkRight.walkright) {
                setKey(mc.options.rightKey, true, rightKeyState);
                rightKeyState = true;
            } else if (rightKeyState) {
                setKey(mc.options.rightKey, false, rightKeyState);
                rightKeyState = false;
            }
        } else if (pauseThisTick) {
            resetKeys(mc);
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
    }

    private void resetKeys(MinecraftClient mc) {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        forwardKeyState = backwardKeyState = leftKeyState = rightKeyState = false;
    }

    private void setKey(net.minecraft.client.option.KeyBinding key, boolean pressed, boolean currentState) {
        if (!currentState && pressed) key.setPressed(true);
        if (currentState && !pressed) key.setPressed(false);
    }
}
