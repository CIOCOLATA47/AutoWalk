package me.cioco.autowalk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static KeyBinding keyBinding;
    public static boolean toggled = false;
    private boolean forwardKeyState = false;

    @Override
    public void onInitialize() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autowalk.toggle",
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.autowalk"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                toggled = !toggled;
                String statusMessage = toggled ? "Enabled" : "Disabled";
                Formatting statusColor = toggled ? Formatting.GREEN : Formatting.RED;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("AutoWalk: " + statusMessage).formatted(statusColor), false);
                }
            }

            if (toggled && !forwardKeyState) {
                MinecraftClient.getInstance().options.forwardKey.setPressed(true);
                forwardKeyState = true;
            } else if (!toggled && forwardKeyState) {
                MinecraftClient.getInstance().options.forwardKey.setPressed(false);
                forwardKeyState = false;
            }
        });
    }
}
