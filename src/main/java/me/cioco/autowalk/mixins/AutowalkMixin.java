package me.cioco.autowalk.mixins;

import me.cioco.autowalk.Main;
import me.cioco.autowalk.commands.WalkBackwards;
import me.cioco.autowalk.commands.WalkForward;
import me.cioco.autowalk.commands.WalkLeft;
import me.cioco.autowalk.commands.WalkRight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(PlayerEntity.class)
public class AutowalkMixin {
    private boolean forwardKeyState = false;
    private boolean backwardKeyState = false;
    private boolean leftKeyState = false;
    private boolean rightKeyState = false;


    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null) return;

        PlayerEntity player = mc.player;
        if (player == null) return;

        boolean isGUIOpen = MinecraftClient.getInstance().currentScreen != null;
        boolean isGUIClosed = MinecraftClient.getInstance().currentScreen == null;

        if (Main.toggled && WalkForward.walkforward && isGUIClosed) {
            if (!forwardKeyState) {
                mc.options.forwardKey.setPressed(true);
                forwardKeyState = true;
            }
        } else {
            if (forwardKeyState) {
                mc.options.forwardKey.setPressed(false);
                forwardKeyState = false;
            }
        }

        if (Main.toggled && WalkBackwards.walkbackwards) {
            if (!backwardKeyState) {
                mc.options.backKey.setPressed(true);
                backwardKeyState = true;
            }
        } else {
            if (backwardKeyState) {
                mc.options.backKey.setPressed(false);
                backwardKeyState = false;
            }
        }

        if (Main.toggled && WalkLeft.walkleft) {
            if (!leftKeyState) {
                mc.options.leftKey.setPressed(true);
                leftKeyState = true;
            }
        } else {
            if (leftKeyState) {
                mc.options.leftKey.setPressed(false);
                leftKeyState = false;
            }
        }

        if (Main.toggled && WalkRight.walkright) {
            if (!rightKeyState) {
                mc.options.rightKey.setPressed(true);
                rightKeyState = true;
            }
        } else {
            if (rightKeyState) {
                mc.options.rightKey.setPressed(false);
                rightKeyState = false;
            }
        }

        if (Main.toggled && WalkForward.walkforward && isGUIOpen) {
            MinecraftClient.getInstance().options.forwardKey.setPressed(true);
        }

        if (Main.toggled && WalkBackwards.walkbackwards && isGUIOpen) {
            MinecraftClient.getInstance().options.backKey.setPressed(true);
        }

        if (Main.toggled && WalkLeft.walkleft && isGUIOpen) {
            MinecraftClient.getInstance().options.leftKey.setPressed(true);
        }

        if (Main.toggled && WalkRight.walkright && isGUIOpen) {
            MinecraftClient.getInstance().options.rightKey.setPressed(true);
        }
    }
}