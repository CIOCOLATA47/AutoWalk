package me.cioco.autowalk;

import me.cioco.autowalk.config.AutoWalkConfig;
import me.cioco.autowalk.gui.AutoWalkScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class Main implements ModInitializer {
    public static final KeyMapping.Category CATEGORY_AUTOWALK = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autowalk", "key_category"));
    public static KeyMapping toggleKey;
    public static KeyMapping guiKey;

    @Override
    public void onInitialize() {

        AutoWalkConfig.getInstance().loadOrSave();

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.auto-walk.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_AUTOWALK
        ));

        guiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.auto-walk.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY_AUTOWALK
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (toggleKey.consumeClick()) {
                AutoWalkConfig config = AutoWalkConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();

                Component status = Component.literal("AutoWalk: ")
                        .append(Component.literal(config.enabled ? "Enabled" : "Disabled")
                                .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED));

                client.player.sendSystemMessage(status);
            }

            while (guiKey.consumeClick()) {
                client.setScreen(new AutoWalkScreen(client.screen));
            }
        });
    }
}