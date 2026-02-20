package me.cioco.autowalk.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AutoWalkConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoWalk-Config");
    private static final String CONFIG_FILE = "autowalk-config.properties";

    private static final AutoWalkConfig INSTANCE = new AutoWalkConfig();
    public boolean enabled = false;
    public boolean stopOnDamage = true;
    public boolean randomPauseEnabled = false;
    public boolean sprinting = false;
    public boolean walkForward = false;
    public boolean walkBackwards = false;
    public boolean walkLeft = false;
    public boolean walkRight = false;
    public boolean autoEat = false;
    public float eatHungerThreshold = 10.0f;

    public static AutoWalkConfig getInstance() {
        return INSTANCE;
    }

    public void loadOrSave() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            load();
        } else {
            save();
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("stopOnDamage", String.valueOf(stopOnDamage));
        props.setProperty("randomPauseEnabled", String.valueOf(randomPauseEnabled));
        props.setProperty("sprinting", String.valueOf(sprinting));
        props.setProperty("walkForward", String.valueOf(walkForward));
        props.setProperty("walkBackwards", String.valueOf(walkBackwards));
        props.setProperty("walkLeft", String.valueOf(walkLeft));
        props.setProperty("walkRight", String.valueOf(walkRight));

        props.setProperty("autoEat", String.valueOf(autoEat));
        props.setProperty("eatHungerThreshold", String.valueOf(eatHungerThreshold));

        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "AutoWalk Configuration");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save AutoWalk config", e);
        }
    }

    public void load() {
        Path configPath = getConfigPath();
        Properties props = new Properties();

        try (InputStream input = Files.newInputStream(configPath)) {
            props.load(input);
            this.enabled = getBool(props, "enabled", false);
            this.stopOnDamage = getBool(props, "stopOnDamage", true);
            this.randomPauseEnabled = getBool(props, "randomPauseEnabled", false);
            this.sprinting = getBool(props, "sprinting", false);
            this.walkForward = getBool(props, "walkForward", false);
            this.walkBackwards = getBool(props, "walkBackwards", false);
            this.walkLeft = getBool(props, "walkLeft", false);
            this.walkRight = getBool(props, "walkRight", false);

            this.autoEat = getBool(props, "autoEat", false);
            this.eatHungerThreshold = getFloat(props, "eatHungerThreshold", 10.0f);

        } catch (IOException e) {
            LOGGER.error("Failed to load AutoWalk config", e);
        }
    }

    private boolean getBool(Properties props, String key, boolean def) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
    }

    private float getFloat(Properties props, String key, float def) {
        try {
            return Float.parseFloat(props.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}