package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RandomPause {

    public static boolean randomPauseEnabled = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("randompause").executes(RandomPause::toggleRandomPause)));
    }

    private static int toggleRandomPause(CommandContext<FabricClientCommandSource> context) {
        randomPauseEnabled = !randomPauseEnabled;

        if (WalkToggleFeedback.walkToggleFeedback) {
            String statusMessage = randomPauseEnabled ? "Random Pause Enabled" : "Random Pause Disabled";
            Formatting statusColor = randomPauseEnabled ? Formatting.GREEN : Formatting.RED;
            context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        }

        return 1;
    }
}
