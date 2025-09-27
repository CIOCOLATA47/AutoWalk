package me.cioco.autowalk;

import me.cioco.autowalk.commands.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static KeyBinding keyBinding;
    public static boolean toggled = false;

    @Override
    public void onInitialize() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auto-walk.toggle",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.auto-walk"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                toggled = !toggled;

                if(WalkToggleFeedback.walkToggleFeedback) {
                    String statusMessage = toggled ? "Enabled" : "Disabled";
                    Formatting statusColor = toggled ? Formatting.GREEN : Formatting.RED;
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor), false);
                    }
                }
            }
        });
        addCommands();
    }
    private void addCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            WalkBackwards.register(dispatcher);
            WalkLeft.register(dispatcher);
            WalkForward.register(dispatcher);
            StopOnDamage.register(dispatcher);
            WalkRight.register(dispatcher);
            WalkStatus.register(dispatcher);
            WalkToggleFeedback.register(dispatcher);
            ToggleSprint.register(dispatcher);
            RandomPause.register(dispatcher);
        });
    }
}