package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class WalkForward {

    public static boolean walkforward = true;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("forward").executes(WalkForward::walkforward)));
    }
    private static int walkforward(CommandContext<FabricClientCommandSource> context) {
        walkforward = !walkforward;

        String statusMessage = walkforward ? "Walking walkforward Enabled" : "Walking walkforward Disabled";
        Formatting statusColor = walkforward ? Formatting.GREEN : Formatting.RED;
        context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        return 1;

    }
}