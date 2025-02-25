package me.cioco.autowalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WalkStatus {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autowalk")
                .then(ClientCommandManager.literal("status").executes(WalkStatus::walkStatus)));
    }

    private static int walkStatus(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("--- AutoWalking Status ---"));
        context.getSource().sendFeedback(getStatusText("Forward", WalkForward.walkforward));
        context.getSource().sendFeedback(getStatusText("Left", WalkLeft.walkleft));
        context.getSource().sendFeedback(getStatusText("Right", WalkRight.walkright));
        context.getSource().sendFeedback(getStatusText("Backwards", WalkBackwards.walkbackwards));
        context.getSource().sendFeedback(getStatusText("StopOnDamage", StopOnDamage.stopondamage));
        context.getSource().sendFeedback(Text.literal("-----------------------"));
        return 1;
    }

    private static Text getStatusText(String direction, boolean status) {
        String statusMessage = status ? "Enabled" : "Disabled";
        Formatting statusColor = status ? Formatting.GREEN : Formatting.RED;
        return Text.literal("AutoWalking " + direction + ": " + statusMessage).formatted(statusColor);
    }
}