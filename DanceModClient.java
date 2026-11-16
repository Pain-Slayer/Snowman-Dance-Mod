package dev.mcdance;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DanceModClient implements ClientModInitializer {
    public static final String MOD_ID = "mcdance";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String BUNDLED_ANIMATION = "/assets/mcdance/animations/snowman_dance.json";

    @Override
    public void onInitializeClient() {
        DanceController.config = loadConfig();
        reloadTimeline();

        KeyMapping.Category category = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));
        DanceController.danceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.mcdance.dance", InputConstants.Type.KEYSYM, DanceController.keyCodeFor(DanceController.config), category));

        ClientTickEvents.END_CLIENT_TICK.register(DanceController::onClientTick);
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcdance.json");
    }

    /** Optional user copy of the animation: config/mcdance/dance.json overrides the bundled one. */
    private static Path animationOverride() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcdance").resolve("dance.json");
    }

    private static DanceConfig loadConfig() {
        Path file = configFile();
        try {
            if (Files.exists(file)) {
                return DanceConfig.fromJson(Files.readString(file, StandardCharsets.UTF_8));
            }
            DanceConfig defaults = new DanceConfig();
            Files.createDirectories(file.getParent());
            Files.writeString(file, defaults.toJson(), StandardCharsets.UTF_8);
            return defaults;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read {}; using default settings: {}", file, e.toString());
            return new DanceConfig();
        }
    }

    /** (Re)loads the animation: user override file first, bundled animation otherwise. */
    static void reloadTimeline() {
        Path override = animationOverride();
        if (Files.exists(override)) {
            try {
                DanceController.timeline = DanceTimeline.parse(Files.readString(override, StandardCharsets.UTF_8));
                return;
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not use {} ({}); falling back to the bundled dance", override, e.toString());
            }
        }
        try (InputStream in = DanceModClient.class.getResourceAsStream(BUNDLED_ANIMATION)) {
            if (in == null) throw new IOException("missing resource " + BUNDLED_ANIMATION);
            DanceController.timeline = DanceTimeline.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not load the bundled dance animation: {}", e.toString());
            DanceController.timeline = null;
        }
    }
}
