package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WalkRight {

    public static boolean walkright = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("right").executes(WalkRight::walkright)));
    }

    private static int walkright(CommandContext<FabricClientCommandSource> context) {
        walkright = !walkright;

        if (WalkToggleFeedback.walkToggleFeedback) {
            String statusMessage = walkright ? "Walking walkright Enabled" : "Walking walkright Disabled";
            Formatting statusColor = walkright ? Formatting.GREEN : Formatting.RED;
            context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        }

        return 1;
    }
}
