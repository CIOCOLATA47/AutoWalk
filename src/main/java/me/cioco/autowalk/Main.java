package me.cioco.autowalk;

import me.cioco.autowalk.config.AutoWalkConfig;
import me.cioco.autowalk.gui.AutoWalkScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static final KeyBinding.Category CATEGORY_AUTOWALK = KeyBinding.Category.create(Identifier.of("autowalk", "key_category"));
    public static KeyBinding toggleKey;
    public static KeyBinding guiKey;

    @Override
    public void onInitialize() {

        AutoWalkConfig.getInstance().loadOrSave();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auto-walk.toggle",
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_AUTOWALK
        ));

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.auto-walk.open_gui",
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_AUTOWALK
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (toggleKey.wasPressed()) {
                AutoWalkConfig config = AutoWalkConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();

                String status = config.enabled ? "Enabled" : "Disabled";
                Formatting color = config.enabled ? Formatting.GREEN : Formatting.RED;

                client.player.sendMessage(
                        Text.literal("AutoWalk: " + status).formatted(color),
                        true
                );
            }

            while (guiKey.wasPressed()) {
                client.setScreen(new AutoWalkScreen(client.currentScreen));
            }
        });
    }
}