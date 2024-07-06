package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WalkToggleFeedback {

    public static boolean walkToggleFeedback = true;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("feedback")
                        .then(ClientCommandManager.literal("toggle")
                                .executes(WalkToggleFeedback::toggleFeedback))));
    }

    private static int toggleFeedback(CommandContext<FabricClientCommandSource> context) {
        walkToggleFeedback = !walkToggleFeedback;
        String message = "Chat Feedback " + (walkToggleFeedback ? "Enabled" : "Disabled");
        context.getSource().sendFeedback(Text.literal("AutoWalk: " + message).formatted(walkToggleFeedback ? Formatting.GREEN : Formatting.RED));
        return 1;
    }
}