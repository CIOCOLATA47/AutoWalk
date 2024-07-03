package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class WalkLeft {

    public static boolean walkleft = false;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("left").executes(WalkLeft::walkleft)));
    }
    private static int walkleft(CommandContext<FabricClientCommandSource> context) {
        walkleft = !walkleft;

        String statusMessage = walkleft ? "Walking walkleft Enabled" : "Walking walkleft Disabled";
        Formatting statusColor = walkleft ? Formatting.GREEN : Formatting.RED;
        context.getSource().sendFeedback(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor));
        return 1;

    }
}