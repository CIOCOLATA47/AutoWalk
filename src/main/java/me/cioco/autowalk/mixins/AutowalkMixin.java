package me.cioco.autowalk.mixins;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(Player.class)
public class AutowalkMixin {

    @Unique private final Random autowalkRandom = new Random();

    @Unique private int  autowalkRandomPauseCounter = 0;

    @Unique private boolean autowalkPressedForward = false;
    @Unique private boolean autowalkPressedBack    = false;
    @Unique private boolean autowalkPressedLeft    = false;
    @Unique private boolean autowalkPressedRight   = false;
    @Unique private boolean autowalkPressedSprint  = false;
    @Unique private boolean autowalkPressedJump    = false;

    @Unique private int     autowalkLastSlot  = -1;
    @Unique private boolean autowalkIsEating  = false;

    @Unique private int autowalkRandomTimer = 0;
    @Unique private int     autowalkEvasionPhase   = 0;
    @Unique private int     autowalkEvasionTicker  = 0;
    @Unique private boolean autowalkEvasionLeft    = false;
    @Unique private boolean autowalkSavedForward   = true;
    @Unique private boolean autowalkSavedBack      = false;
    @Unique private boolean autowalkSavedLeft      = false;
    @Unique private boolean autowalkSavedRight     = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (!mc.isSameThread()) return;
        if (!mc.player.equals(this)) return;

        Player         player = mc.player;
        AutoWalkConfig config = AutoWalkConfig.getInstance();

        if (config.enabled && player.hurtTime == 10) {
            autowalkHandleDamageResponse(mc, player, config);
            if (!config.enabled) return;
        }

        if (!config.enabled) {
            autowalkReleaseAllKeys(mc);
            return;
        }

        if (config.autoEat) {
            if (autowalkIsEating) {
                if (player.getFoodData().getFoodLevel() >= 20 || !player.isUsingItem()) {
                    autowalkStopEating(mc);
                } else {
                    mc.options.keyUse.setDown(true);
                }
            } else if (player.getFoodData().getFoodLevel() <= config.eatHungerThreshold) {
                int foodSlot = autowalkFindFoodSlot(player);
                if (foodSlot != -1) {
                    autowalkStartEating(mc, foodSlot);
                }
            }
        }

        if (!autowalkIsEating && config.randomPauseEnabled && autowalkRandom.nextInt(200) == 0) {
            autowalkRandomPauseCounter = 10 + autowalkRandom.nextInt(20);
        }
        if (autowalkRandomPauseCounter > 0 && !autowalkIsEating) {
            autowalkRandomPauseCounter--;
            autowalkReleaseAllKeys(mc);
            return;
        }

        if (autowalkEvasionPhase == 0) {
            if (config.avoidHostileMobs && autowalkNearbyHostile(player, config.hostileAvoidDistance)) {
                autowalkStartEvasion(config);
            } else if (config.avoidPlayers && autowalkNearbyPlayer(player, config.playerAvoidDistance)) {
                autowalkStartEvasion(config);
            }
        }

        if (autowalkEvasionPhase == 1) {
            config.moveForward = false;
            config.moveBack    = true;
            config.moveLeft    = false;
            config.moveRight   = false;
            if (--autowalkEvasionTicker <= 0) {
                autowalkEvasionPhase  = 2;
                autowalkEvasionTicker = 30;
            }
        } else if (autowalkEvasionPhase == 2) {
            config.moveForward = false;
            config.moveBack    = false;
            config.moveLeft    = autowalkEvasionLeft;
            config.moveRight   = !autowalkEvasionLeft;
            if (--autowalkEvasionTicker <= 0) {
                autowalkEvasionPhase = 0;
                config.moveForward   = autowalkSavedForward;
                config.moveBack      = autowalkSavedBack;
                config.moveLeft      = autowalkSavedLeft;
                config.moveRight     = autowalkSavedRight;
            }
        }

        autowalkApplyMovementMode(mc, player, config);

        boolean isGUIOpen = mc.screen != null;
        if (isGUIOpen) {
            if (config.moveForward)  { mc.options.keyUp.setDown(true);    autowalkPressedForward = true; }
            if (config.moveBack)     { mc.options.keyDown.setDown(true);  autowalkPressedBack    = true; }
            if (config.moveLeft)     { mc.options.keyLeft.setDown(true);  autowalkPressedLeft    = true; }
            if (config.moveRight)    { mc.options.keyRight.setDown(true); autowalkPressedRight   = true; }
            if (autowalkPressedForward && !config.moveForward) { mc.options.keyUp.setDown(false);    autowalkPressedForward = false; }
            if (autowalkPressedBack    && !config.moveBack)    { mc.options.keyDown.setDown(false);  autowalkPressedBack    = false; }
            if (autowalkPressedLeft    && !config.moveLeft)    { mc.options.keyLeft.setDown(false);  autowalkPressedLeft    = false; }
            if (autowalkPressedRight   && !config.moveRight)   { mc.options.keyRight.setDown(false); autowalkPressedRight   = false; }
        } else {
            if (config.moveForward)  { mc.options.keyUp.setDown(true);    autowalkPressedForward = true; }
            else if (autowalkPressedForward) { mc.options.keyUp.setDown(false);    autowalkPressedForward = false; }

            if (config.moveBack)     { mc.options.keyDown.setDown(true);  autowalkPressedBack    = true; }
            else if (autowalkPressedBack)    { mc.options.keyDown.setDown(false);  autowalkPressedBack    = false; }

            if (config.moveLeft)     { mc.options.keyLeft.setDown(true);  autowalkPressedLeft    = true; }
            else if (autowalkPressedLeft)    { mc.options.keyLeft.setDown(false);  autowalkPressedLeft    = false; }

            if (config.moveRight)    { mc.options.keyRight.setDown(true); autowalkPressedRight   = true; }
            else if (autowalkPressedRight)   { mc.options.keyRight.setDown(false); autowalkPressedRight   = false; }
        }

        if (config.sprinting && !player.isSwimming() && config.moveForward) {
            mc.options.keySprint.setDown(true);
            autowalkPressedSprint = true;
        } else if (autowalkPressedSprint) {
            mc.options.keySprint.setDown(false);
            autowalkPressedSprint = false;
        }

        if (autowalkPressedJump) {
            mc.options.keyJump.setDown(false);
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
    private void autowalkApplyMovementMode(Minecraft mc, Player player, AutoWalkConfig config) {
        switch (config.movementMode) {

            case MANUAL:
                break;

            case RANDOM:
                if (autowalkRandomTimer <= 0) {
                    autowalkRandomTimer = 60 + autowalkRandom.nextInt(120);
                    int fb = autowalkRandom.nextInt(3);
                    int lr = autowalkRandom.nextInt(3);
                    if (fb == 2 && lr == 2) fb = 0;
                    config.moveForward = (fb == 0);
                    config.moveBack    = (fb == 1);
                    config.moveLeft    = (lr == 0);
                    config.moveRight   = (lr == 1);
                } else {
                    autowalkRandomTimer--;
                }
                break;

            case CIRCLE:
                config.moveForward = true;
                config.moveBack    = false;
                config.moveLeft    = false;
                config.moveRight   = false;
                player.setYRot(player.getYRot() + 2.0f);
                player.setYHeadRot(player.getYRot());
                break;
        }
    }

    @Unique
    private void autowalkHandleDamageResponse(Minecraft mc, Player player, AutoWalkConfig config) {
        switch (config.damageResponse) {

            case STOP:
                autowalkHandleDisable(mc, config, "AutoWalk: Stopped due to damage.");
                break;

            case TURN_BACK:
                config.moveForward = !config.moveForward;
                config.moveBack    = !config.moveBack;
                config.moveLeft    = !config.moveLeft;
                config.moveRight   = !config.moveRight;
                break;

            case RANDOM_TURN:
                if (autowalkEvasionPhase == 0) {
                    autowalkStartEvasion(config);
                }
                break;

            case JUMP:
                if (player.onGround()) {
                    autowalkDoJump(mc);
                }
                break;

            case IGNORE:
                break;
        }
    }

    @Unique
    private void autowalkStartEvasion(AutoWalkConfig config) {
        autowalkSavedForward  = config.moveForward;
        autowalkSavedBack     = config.moveBack;
        autowalkSavedLeft     = config.moveLeft;
        autowalkSavedRight    = config.moveRight;
        autowalkEvasionLeft   = autowalkRandom.nextBoolean();
        autowalkEvasionPhase  = 1;
        autowalkEvasionTicker = 40;
    }

    @Unique
    private boolean autowalkHandleHazard(Minecraft mc, Player player, AutoWalkConfig config, String hazardName) {
        switch (config.damageResponse) {
            case STOP:
                autowalkHandleDisable(mc, config, "AutoWalk: Stopped — " + hazardName + " detected ahead!");
                return true;
            case TURN_BACK:
            case RANDOM_TURN:
                if (autowalkEvasionPhase == 0) autowalkStartEvasion(config);
                return false;
            case JUMP:
                if (player.onGround()) autowalkDoJump(mc);
                return false;
            case IGNORE:
                return false;
        }
        return false;
    }

    @Unique
    private void autowalkHandleAutoJump(Minecraft mc, Player player, AutoWalkConfig config) {
        boolean moving = config.moveForward || config.moveBack || config.moveLeft || config.moveRight;
        if (!moving) return;

        Level  level   = player.level();
        double px      = player.getX();
        double py      = player.getY();
        double pz      = player.getZ();
        float  yaw     = player.getYRot();
        double radY    = Math.toRadians(yaw);
        double dx      = -Math.sin(radY);
        double dz      =  Math.cos(radY);
        int    checkX  = (int) Math.floor(px + dx * 1.1);
        int    checkZ  = (int) Math.floor(pz + dz * 1.1);
        int    playerY = (int) Math.floor(py);

        if (config.avoidLava) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                var block   = level.getBlockState(bp).getBlock();
                var fluid   = level.getFluidState(bp);
                if (block == Blocks.LAVA || block == Blocks.MAGMA_BLOCK
                        || fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
                    if (autowalkHandleHazard(mc, player, config, "lava")) return;
                    break;
                }
            }
        }

        if (config.avoidFire) {
            BlockPos bp = new BlockPos(checkX, playerY, checkZ);
            var block   = level.getBlockState(bp).getBlock();
            if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
                if (autowalkHandleHazard(mc, player, config, "fire")) return;
            }
        }

        if (config.avoidCactus) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                if (level.getBlockState(bp).getBlock() == Blocks.CACTUS) {
                    if (autowalkHandleHazard(mc, player, config, "cactus")) return;
                    break;
                }
            }
        }

        if (config.avoidBerryBush) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                if (level.getBlockState(bp).getBlock() == Blocks.SWEET_BERRY_BUSH) {
                    if (autowalkHandleHazard(mc, player, config, "berry bush")) return;
                    break;
                }
            }
        }

        if (config.avoidDrops) {
            boolean groundUnder = level.getBlockState(new BlockPos(checkX, playerY - 1, checkZ)).isSolid();
            if (!groundUnder) {
                int dropDepth = 0;
                for (int dy = 1; dy <= config.jumpDropThreshold + 1; dy++) {
                    if (level.getBlockState(new BlockPos(checkX, playerY - dy, checkZ)).isSolid()) break;
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

        boolean obstacle   = level.getBlockState(feetAhead).isSolid()
                || level.getBlockState(chestAhead).isSolid();
        boolean clearAbove = !level.getBlockState(headAhead).isSolid();

        if (obstacle && clearAbove && player.onGround()) {
            autowalkDoJump(mc);
        }
    }

    @Unique
    private void autowalkHandleWaterSurface(Minecraft mc, Player player) {
        if (!player.isInWater()) return;
        autowalkDoJump(mc);
    }

    @Unique
    private boolean autowalkNearbyHostile(Player player, float distance) {
        Level level = player.level();
        AABB  box   = player.getBoundingBox().inflate(distance);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e instanceof Monster && !e.equals(player));
        return !entities.isEmpty();
    }

    @Unique
    private boolean autowalkNearbyPlayer(Player self, float distance) {
        Level level = self.level();
        AABB  box   = self.getBoundingBox().inflate(distance);
        List<Player> players = level.getEntitiesOfClass(Player.class, box,
                e -> !e.equals(self));
        return !players.isEmpty();
    }

    @Unique
    private void autowalkDoJump(Minecraft mc) {
        mc.options.keyJump.setDown(true);
        autowalkPressedJump = true;
    }

    @Unique
    private int autowalkFindFoodSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private void autowalkStartEating(Minecraft mc, int slot) {
        autowalkLastSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.options.keyUse.setDown(true);
        autowalkIsEating = true;
    }

    @Unique
    private void autowalkStopEating(Minecraft mc) {
        mc.options.keyUse.setDown(false);
        if (autowalkLastSlot != -1) {
            mc.player.getInventory().setSelectedSlot(autowalkLastSlot);
        }
        autowalkIsEating = false;
        autowalkLastSlot = -1;
    }

    @Unique
    private void autowalkHandleDisable(Minecraft mc, AutoWalkConfig config, String message) {
        autowalkReleaseAllKeys(mc);
        config.enabled = false;
        config.save();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal(message).withStyle(ChatFormatting.RED)
                );
            }
        });
    }

    @Unique
    private void autowalkReleaseAllKeys(Minecraft mc) {
        if (autowalkPressedForward) mc.options.keyUp.setDown(false);
        if (autowalkPressedBack)    mc.options.keyDown.setDown(false);
        if (autowalkPressedLeft)    mc.options.keyLeft.setDown(false);
        if (autowalkPressedRight)   mc.options.keyRight.setDown(false);
        if (autowalkPressedSprint)  mc.options.keySprint.setDown(false);
        if (autowalkPressedJump)    mc.options.keyJump.setDown(false);
        if (autowalkIsEating)       autowalkStopEating(mc);

        autowalkPressedForward = autowalkPressedBack = autowalkPressedLeft
                = autowalkPressedRight = autowalkPressedSprint = autowalkPressedJump = false;
    }
}