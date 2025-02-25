package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StopOnDamage {

    public static boolean stopondamage = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("stopondamage").executes(StopOnDamage::toggleStopOnDamage)));
    }

    private static int toggleStopOnDamage(CommandContext<FabricClientCommandSource> context) {
        stopondamage = !stopondamage;

        String statusMessage = stopondamage ? "Stop on damage Enabled" : "Stop on damage Disabled";
        Formatting statusColor = stopondamage ? Formatting.GREEN : Formatting.RED;
        context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        return 1;
    }
}
