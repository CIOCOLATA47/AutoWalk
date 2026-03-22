package me.cioco.autowalk.mixins;

import me.cioco.autowalk.config.AutoWalkConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(PlayerEntity.class)
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!mc.isOnThread()) return;
        if (!mc.player.equals(this)) return;

        PlayerEntity    player = mc.player;
        AutoWalkConfig  config = AutoWalkConfig.getInstance();

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

        boolean isGUIOpen = mc.currentScreen != null;
        if (isGUIOpen) {
            if (config.moveForward)  { mc.options.forwardKey.setPressed(true);  autowalkPressedForward = true; }
            if (config.moveBack)     { mc.options.backKey.setPressed(true);     autowalkPressedBack    = true; }
            if (config.moveLeft)     { mc.options.leftKey.setPressed(true);     autowalkPressedLeft    = true; }
            if (config.moveRight)    { mc.options.rightKey.setPressed(true);    autowalkPressedRight   = true; }
            if (autowalkPressedForward && !config.moveForward) { mc.options.forwardKey.setPressed(false); autowalkPressedForward = false; }
            if (autowalkPressedBack    && !config.moveBack)    { mc.options.backKey.setPressed(false);    autowalkPressedBack    = false; }
            if (autowalkPressedLeft    && !config.moveLeft)    { mc.options.leftKey.setPressed(false);    autowalkPressedLeft    = false; }
            if (autowalkPressedRight   && !config.moveRight)   { mc.options.rightKey.setPressed(false);   autowalkPressedRight   = false; }
        } else {
            if (config.moveForward)  { mc.options.forwardKey.setPressed(true);  autowalkPressedForward = true; }
            else if (autowalkPressedForward) { mc.options.forwardKey.setPressed(false); autowalkPressedForward = false; }

            if (config.moveBack)     { mc.options.backKey.setPressed(true);     autowalkPressedBack    = true; }
            else if (autowalkPressedBack)    { mc.options.backKey.setPressed(false);    autowalkPressedBack    = false; }

            if (config.moveLeft)     { mc.options.leftKey.setPressed(true);     autowalkPressedLeft    = true; }
            else if (autowalkPressedLeft)    { mc.options.leftKey.setPressed(false);    autowalkPressedLeft    = false; }

            if (config.moveRight)    { mc.options.rightKey.setPressed(true);    autowalkPressedRight   = true; }
            else if (autowalkPressedRight)   { mc.options.rightKey.setPressed(false);   autowalkPressedRight   = false; }
        }

        if (config.sprinting && !player.isSwimming() && config.moveForward) {
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
    private void autowalkApplyMovementMode(MinecraftClient mc, PlayerEntity player, AutoWalkConfig config) {
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
                player.setYaw(player.getYaw() + 2.0f);
                player.setHeadYaw(player.getYaw());
                break;
        }
    }

    @Unique
    private void autowalkHandleDamageResponse(MinecraftClient mc, PlayerEntity player, AutoWalkConfig config) {
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
                if (player.isOnGround()) {
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
    private boolean autowalkHandleHazard(MinecraftClient mc, PlayerEntity player, AutoWalkConfig config, String hazardName) {
        switch (config.damageResponse) {
            case STOP:
                autowalkHandleDisable(mc, config, "AutoWalk: Stopped — " + hazardName + " detected ahead!");
                return true;
            case TURN_BACK:
            case RANDOM_TURN:
                if (autowalkEvasionPhase == 0) autowalkStartEvasion(config);
                return false;
            case JUMP:
                if (player.isOnGround()) autowalkDoJump(mc);
                return false;
            case IGNORE:
                return false;
        }
        return false;
    }

    @Unique
    private void autowalkHandleAutoJump(MinecraftClient mc, PlayerEntity player, AutoWalkConfig config) {
        boolean moving = config.moveForward || config.moveBack || config.moveLeft || config.moveRight;
        if (!moving) return;

        World  world   = player.getEntityWorld();
        double px      = player.getX();
        double py      = player.getY();
        double pz      = player.getZ();
        float  yaw     = player.getYaw();
        double radY    = Math.toRadians(yaw);
        double dx      = -Math.sin(radY);
        double dz      =  Math.cos(radY);
        int    checkX  = (int) Math.floor(px + dx * 1.1);
        int    checkZ  = (int) Math.floor(pz + dz * 1.1);
        int    playerY = (int) Math.floor(py);

        if (config.avoidLava) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockPos   bp    = new BlockPos(checkX, playerY + dy, checkZ);
                Block      block = world.getBlockState(bp).getBlock();
                FluidState fluid = world.getFluidState(bp);
                if (block == Blocks.LAVA || block == Blocks.MAGMA_BLOCK
                        || fluid.isOf(Fluids.LAVA) || fluid.isOf(Fluids.FLOWING_LAVA)) {
                    if (autowalkHandleHazard(mc, player, config, "lava")) return;
                    break;
                }
            }
        }

        if (config.avoidFire) {
            BlockPos bp = new BlockPos(checkX, playerY, checkZ);
            if (world.getBlockState(bp).getBlock() == Blocks.FIRE
                    || world.getBlockState(bp).getBlock() == Blocks.SOUL_FIRE) {
                if (autowalkHandleHazard(mc, player, config, "fire")) return;
            }
        }

        if (config.avoidCactus) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                if (world.getBlockState(bp).getBlock() == Blocks.CACTUS) {
                    if (autowalkHandleHazard(mc, player, config, "cactus")) return;
                    break;
                }
            }
        }

        if (config.avoidBerryBush) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos bp = new BlockPos(checkX, playerY + dy, checkZ);
                if (world.getBlockState(bp).getBlock() == Blocks.SWEET_BERRY_BUSH) {
                    if (autowalkHandleHazard(mc, player, config, "berry bush")) return;
                    break;
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
    private boolean autowalkNearbyHostile(PlayerEntity player, float distance) {
        World world = player.getEntityWorld();
        Box   box   = player.getBoundingBox().expand(distance);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e instanceof HostileEntity && !e.equals(player));
        return !entities.isEmpty();
    }

    @Unique
    private boolean autowalkNearbyPlayer(PlayerEntity self, float distance) {
        World world = self.getEntityWorld();
        Box   box   = self.getBoundingBox().expand(distance);
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, box,
                e -> !e.equals(self));
        return !players.isEmpty();
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