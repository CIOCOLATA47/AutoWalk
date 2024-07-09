package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class WalkBackwards {

    public static boolean walkbackwards = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("backwards").executes(WalkBackwards::walkbackwards)));
    }
    private static int walkbackwards(CommandContext<FabricClientCommandSource> context) {
        walkbackwards = !walkbackwards;

        String statusMessage = walkbackwards ? "Walking backwards Enabled" : "Walking backwards Disabled";
        Formatting statusColor = walkbackwards ? Formatting.GREEN : Formatting.RED;
        context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        return 1;

    }
}