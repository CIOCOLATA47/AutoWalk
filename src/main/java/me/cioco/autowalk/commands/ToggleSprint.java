package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ToggleSprint {

    public static boolean sprinting = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("sprint").executes(ToggleSprint::toggleSprint)));
    }

    private static int toggleSprint(CommandContext<FabricClientCommandSource> context) {
        sprinting = !sprinting;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(sprinting);
        }

        if (WalkToggleFeedback.walkToggleFeedback) {
            String statusMessage = sprinting ? "Sprint Enabled" : "Sprint Disabled";
            Formatting statusColor = sprinting ? Formatting.GREEN : Formatting.RED;
            context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        }

        return 1;
    }
}
