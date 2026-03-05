package me.cioco.autowalk.mixins;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(PlayerEntity.class)
public class AutowalkMixin {

    @Unique private final Random autowalkRandom = new Random();
    @Unique private int autowalkRandomPauseCounter = 0;
    @Unique private boolean autowalkPressedForward = false;
    @Unique private boolean autowalkPressedBack = false;
    @Unique private boolean autowalkPressedLeft = false;
    @Unique private boolean autowalkPressedRight = false;
    @Unique private boolean autowalkPressedSprint = false;
    @Unique private boolean autowalkPressedJump = false;
    @Unique private int autowalkLastSlot = -1;
    @Unique private boolean autowalkIsEating = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!mc.isOnThread()) return;
        if (!mc.player.equals(this)) return;

        PlayerEntity player = mc.player;
        AutoWalkConfig config = AutoWalkConfig.getInstance();

        if (config.stopOnDamage && config.enabled && player.hurtTime > 0) {
            autowalkHandleDisable(mc, config, "AutoWalk: Stopped due to damage.");
            return;
        }

        if (!config.enabled) {
            autowalkReleaseAllKeys(mc);
            return;
        }

        if (config.autoEat) {
            if (autowalkIsEating) {
                if (player.getHungerManager().getFoodLevel() >= 20 || !player.isUsingItem()) {
                    autowalkStopEating(mc);
                } else {
                    mc.options.useKey.setPressed(true);
                }
            } else if (player.getHungerManager().getFoodLevel() <= config.eatHungerThreshold) {
                int foodSlot = autowalkFindFoodSlot(player);
                if (foodSlot != -1) {
                    autowalkStartEating(mc, foodSlot);
                }
            }
        }

        boolean pauseThisTick = false;
        if (!autowalkIsEating && config.randomPauseEnabled && autowalkRandom.nextInt(200) == 0) {
            autowalkRandomPauseCounter = 10 + autowalkRandom.nextInt(20);
        }
        if (autowalkRandomPauseCounter > 0 && !autowalkIsEating) {
            autowalkRandomPauseCounter--;
            pauseThisTick = true;
        }

        if (pauseThisTick) {
            autowalkReleaseAllKeys(mc);
            return;
        }

        boolean isGUIOpen = mc.currentScreen != null;
        if (isGUIOpen) {
            mc.options.forwardKey.setPressed(config.walkForward);
            mc.options.backKey.setPressed(config.walkBackwards);
            mc.options.leftKey.setPressed(config.walkLeft);
            mc.options.rightKey.setPressed(config.walkRight);
            autowalkPressedForward = config.walkForward;
            autowalkPressedBack    = config.walkBackwards;
            autowalkPressedLeft    = config.walkLeft;
            autowalkPressedRight   = config.walkRight;
        } else {
            autowalkPressedForward = autowalkPressKey(mc.options.forwardKey, config.walkForward);
            autowalkPressedBack    = autowalkPressKey(mc.options.backKey,    config.walkBackwards);
            autowalkPressedLeft    = autowalkPressKey(mc.options.leftKey,    config.walkLeft);
            autowalkPressedRight   = autowalkPressKey(mc.options.rightKey,   config.walkRight);
        }

        if (config.sprinting && !player.isSwimming() && config.walkForward) {
            mc.options.sprintKey.setPressed(true);
            autowalkPressedSprint = true;
        } else if (autowalkPressedSprint) {
            mc.options.sprintKey.setPressed(false);
            autowalkPressedSprint = false;
        }

        if (autowalkPressedJump) {
            mc.options.jumpKey.setPressed(false);
            autowalkPressedJump = false;
        }

        if (config.autoJump) {
            autowalkHandleAutoJump(mc, player, config);
        }

        if (config.waterSurface) {
            autowalkHandleWaterSurface(mc, player);
        }
    }

    @Unique
    private void autowalkHandleAutoJump(MinecraftClient mc, PlayerEntity player, AutoWalkConfig config) {
        boolean moving = config.walkForward || config.walkBackwards || config.walkLeft || config.walkRight;
        if (!moving) return;

        World world = player.getEntityWorld();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        float yaw   = player.getYaw();
        double radY = Math.toRadians(yaw);
        double dx   = -Math.sin(radY);
        double dz   =  Math.cos(radY);

        int checkX  = (int) Math.floor(px + dx * 1.1);
        int checkZ  = (int) Math.floor(pz + dz * 1.1);
        int playerY = (int) Math.floor(py);

        if (config.avoidLava) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                Block block = world.getBlockState(bp).getBlock();
                if (block == Blocks.LAVA || block == Blocks.MAGMA_BLOCK) {
                    autowalkHandleDisable(mc, config, "AutoWalk: Stopped — lava detected ahead!");
                    return;
                }
                FluidState fluid = world.getFluidState(bp);
                if (fluid.isOf(Fluids.LAVA) || fluid.isOf(Fluids.FLOWING_LAVA)) {
                    autowalkHandleDisable(mc, config, "AutoWalk: Stopped — lava detected ahead!");
                    return;
                }
            }
        }

        if (config.avoidDrops) {
            boolean groundUnder = world.getBlockState(new BlockPos(checkX, playerY - 1, checkZ)).isSolid();
            if (!groundUnder) {
                int dropDepth = 0;
                for (int dy = 1; dy <= config.jumpDropThreshold + 1; dy++) {
                    if (world.getBlockState(new BlockPos(checkX, playerY - dy, checkZ)).isSolid()) break;
                    dropDepth++;
                }
                if (dropDepth > config.jumpDropThreshold) {
                    autowalkHandleDisable(mc, config, "AutoWalk: Stopped — big drop detected ahead!");
                    return;
                }
            }
        }

        BlockPos feetAhead  = new BlockPos(checkX, playerY,     checkZ);
        BlockPos chestAhead = new BlockPos(checkX, playerY + 1, checkZ);
        BlockPos headAhead  = new BlockPos(checkX, playerY + 2, checkZ);

        boolean obstacle   = world.getBlockState(feetAhead).isSolid()
                || world.getBlockState(chestAhead).isSolid();
        boolean clearAbove = !world.getBlockState(headAhead).isSolid();

        if (obstacle && clearAbove && player.isOnGround()) {
            autowalkDoJump(mc);
        }
    }

    @Unique
    private void autowalkHandleWaterSurface(MinecraftClient mc, PlayerEntity player) {
        if (!player.isTouchingWater()) return;
        autowalkDoJump(mc);
    }

    @Unique
    private void autowalkDoJump(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(true);
        autowalkPressedJump = true;
    }

    @Unique
    private int autowalkFindFoodSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private void autowalkStartEating(MinecraftClient mc, int slot) {
        autowalkLastSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.options.useKey.setPressed(true);
        autowalkIsEating = true;
    }

    @Unique
    private void autowalkStopEating(MinecraftClient mc) {
        mc.options.useKey.setPressed(false);
        if (autowalkLastSlot != -1) {
            mc.player.getInventory().setSelectedSlot(autowalkLastSlot);
        }
        autowalkIsEating = false;
        autowalkLastSlot = -1;
    }

    @Unique
    private void autowalkHandleDisable(MinecraftClient mc, AutoWalkConfig config, String message) {
        autowalkReleaseAllKeys(mc);
        config.enabled = false;
        config.save();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
            }
        });
    }

    @Unique
    private boolean autowalkPressKey(net.minecraft.client.option.KeyBinding key, boolean shouldPress) {
        key.setPressed(shouldPress);
        return shouldPress;
    }

    @Unique
    private void autowalkReleaseAllKeys(MinecraftClient mc) {
        if (autowalkPressedForward) mc.options.forwardKey.setPressed(false);
        if (autowalkPressedBack)    mc.options.backKey.setPressed(false);
        if (autowalkPressedLeft)    mc.options.leftKey.setPressed(false);
        if (autowalkPressedRight)   mc.options.rightKey.setPressed(false);
        if (autowalkPressedSprint)  mc.options.sprintKey.setPressed(false);
        if (autowalkPressedJump)    mc.options.jumpKey.setPressed(false);
        if (autowalkIsEating)       autowalkStopEating(mc);

        autowalkPressedForward = autowalkPressedBack = autowalkPressedLeft
                = autowalkPressedRight = autowalkPressedSprint = autowalkPressedJump = false;
    }
}