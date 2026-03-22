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

    public boolean enabled            = false;
    public boolean randomPauseEnabled = false;
    public boolean sprinting          = false;

    public boolean moveForward = true;
    public boolean moveBack    = false;
    public boolean moveLeft    = false;
    public boolean moveRight   = false;

    public boolean autoEat            = false;
    public float   eatHungerThreshold = 10.0f;

    public boolean autoJump          = false;
    public boolean avoidDrops        = false;
    public int     jumpDropThreshold = 3;

    public boolean avoidLava        = false;
    public boolean avoidFire        = true;
    public boolean avoidCactus      = true;
    public boolean avoidBerryBush   = true;

    public boolean waterSurface     = false;

    public boolean avoidHostileMobs      = false;
    public float   hostileAvoidDistance  = 6.0f;

    public boolean avoidPlayers          = false;
    public float   playerAvoidDistance   = 4.0f;


    public enum DamageResponse {
        STOP,
        TURN_BACK,
        RANDOM_TURN,
        JUMP,
        IGNORE
    }

    public DamageResponse damageResponse = DamageResponse.STOP;

    public enum MovementMode {
        MANUAL,
        RANDOM,
        CIRCLE
    }

    public MovementMode movementMode = MovementMode.MANUAL;


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
        props.setProperty("randomPauseEnabled", String.valueOf(randomPauseEnabled));
        props.setProperty("sprinting", String.valueOf(sprinting));

        props.setProperty("movementMode", movementMode.name());
        props.setProperty("moveForward", String.valueOf(moveForward));
        props.setProperty("moveBack", String.valueOf(moveBack));
        props.setProperty("moveLeft", String.valueOf(moveLeft));
        props.setProperty("moveRight", String.valueOf(moveRight));

        props.setProperty("autoEat", String.valueOf(autoEat));
        props.setProperty("eatHungerThreshold", String.valueOf(eatHungerThreshold));


        props.setProperty("autoJump", String.valueOf(autoJump));
        props.setProperty("avoidDrops", String.valueOf(avoidDrops));
        props.setProperty("jumpDropThreshold", String.valueOf(jumpDropThreshold));

        props.setProperty("avoidLava", String.valueOf(avoidLava));
        props.setProperty("avoidFire", String.valueOf(avoidFire));
        props.setProperty("avoidCactus", String.valueOf(avoidCactus));
        props.setProperty("avoidBerryBush", String.valueOf(avoidBerryBush));

        props.setProperty("waterSurface", String.valueOf(waterSurface));

        props.setProperty("avoidHostileMobs", String.valueOf(avoidHostileMobs));
        props.setProperty("hostileAvoidDistance", String.valueOf(hostileAvoidDistance));

        props.setProperty("avoidPlayers", String.valueOf(avoidPlayers));
        props.setProperty("playerAvoidDistance", String.valueOf(playerAvoidDistance));

        props.setProperty("damageResponse", damageResponse.name());

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

            this.enabled            = getBool(props, "enabled", false);
            this.randomPauseEnabled = getBool(props, "randomPauseEnabled", false);
            this.sprinting          = getBool(props, "sprinting", false);

            this.movementMode = getEnum(props, "movementMode", MovementMode.class, MovementMode.MANUAL);
            this.moveForward  = getBool(props, "moveForward", true);
            this.moveBack     = getBool(props, "moveBack", false);
            this.moveLeft     = getBool(props, "moveLeft", false);
            this.moveRight    = getBool(props, "moveRight", false);

            this.autoEat            = getBool(props, "autoEat", false);
            this.eatHungerThreshold = getFloat(props, "eatHungerThreshold", 10.0f);

            this.autoJump          = getBool(props, "autoJump", false);
            this.avoidDrops        = getBool(props, "avoidDrops", false);
            this.jumpDropThreshold = getInt(props, "jumpDropThreshold", 3);

            this.avoidLava       = getBool(props, "avoidLava", false);
            this.avoidFire       = getBool(props, "avoidFire", true);
            this.avoidCactus     = getBool(props, "avoidCactus", true);
            this.avoidBerryBush  = getBool(props, "avoidBerryBush", true);

            this.waterSurface    = getBool(props, "waterSurface", false);

            this.avoidHostileMobs     = getBool(props, "avoidHostileMobs", false);
            this.hostileAvoidDistance = getFloat(props, "hostileAvoidDistance", 6.0f);

            this.avoidPlayers         = getBool(props, "avoidPlayers", false);
            this.playerAvoidDistance  = getFloat(props, "playerAvoidDistance", 4.0f);

            this.damageResponse = getEnum(props, "damageResponse", DamageResponse.class, DamageResponse.STOP);

            normalizeMovement();

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

    private int getInt(Properties props, String key, int def) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private <T extends Enum<T>> T getEnum(Properties props, String key, Class<T> enumClass, T def) {
        try {
            return Enum.valueOf(enumClass, props.getProperty(key, def.name()));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private void normalizeMovement() {
        if (moveForward && moveBack) moveBack = false;
        if (moveLeft && moveRight) moveRight = false;
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}