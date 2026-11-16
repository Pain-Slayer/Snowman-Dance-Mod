package dev.mcdance;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client-side dance state: start / stop, timing, blending and interruption rules. */
public final class DanceController {
    private static final double INTERRUPT_GRACE_SECONDS = 0.25;

    static KeyMapping danceKey;
    static DanceConfig config = new DanceConfig();
    static DanceTimeline timeline;

    private static boolean playing = false;
    private static boolean stopping = false;
    private static long startNanos;
    private static long stopNanos;
    private static CameraType previousCamera = null;

    private DanceController() {}

    // ---------------------------------------------------------------- tick / input

    static void onClientTick(Minecraft mc) {
        LocalPlayer player = mc.player;

        // Drain every queued key press. A single tick can only ever cause one toggle,
        // so one press can never start and instantly stop the dance.
        boolean pressed = false;
        while (danceKey.consumeClick()) pressed = true;

        if (player == null) {
            hardStop(mc);
            return;
        }
        if (pressed && mc.screen == null) toggle(mc);

        if (!playing) return;
        double elapsed = elapsedSeconds();

        if (stopping) {
            if ((System.nanoTime() - stopNanos) / 1.0e9 >= config.blendSeconds) hardStop(mc);
            return;
        }

        if (!config.loop && elapsed * config.speed >= timeline.duration()) {
            beginStop(mc);
            return;
        }

        if (config.interruptible && elapsed > INTERRUPT_GRACE_SECONDS && wasInterrupted(player)) {
            beginStop(mc);
        }
    }

    private static boolean wasInterrupted(LocalPlayer p) {
        return p.getDeltaMovement().horizontalDistanceSqr() > 0.0004 // walking, being pushed
            || !p.onGround()                                          // jumping or falling
            || p.hurtTime > 0                                         // taking damage
            || p.swinging                                             // attacking / using an item
            || p.isPassenger()
            || p.isSleeping()
            || p.isUnderWater();
    }

    // ---------------------------------------------------------------- start / stop

    private static void toggle(Minecraft mc) {
        if (playing && !stopping) {
            beginStop(mc); // the dance key always stops a dance, even when "interruptible" is false
        } else {
            start(mc);
        }
    }

    private static void start(Minecraft mc) {
        if (config.reloadAnimationOnStart) DanceModClient.reloadTimeline();
        if (timeline == null) return;
        playing = true;
        stopping = false;
        startNanos = System.nanoTime();
        if (config.switchToThirdPerson && mc.options.getCameraType().isFirstPerson()) {
            previousCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else {
            previousCamera = null;
        }
    }

    private static void beginStop(Minecraft mc) {
        if (!playing || stopping) return;
        if (config.blendSeconds <= 0.0) {
            hardStop(mc);
            return;
        }
        stopping = true;
        stopNanos = System.nanoTime();
    }

    private static void hardStop(Minecraft mc) {
        if (playing && previousCamera != null && mc.player != null) {
            mc.options.setCameraType(previousCamera);
        }
        previousCamera = null;
        playing = false;
        stopping = false;
    }

    // ---------------------------------------------------------------- queried by the renderer

    private static double elapsedSeconds() {
        return (System.nanoTime() - startNanos) / 1.0e9;
    }

    /**
     * Writes the current pose into out and returns the blend weight (0..1), or a negative number when no dance is active.
     */
    static double sample(float[] out) {
        if (!playing || timeline == null) return -1.0;
        double elapsed = elapsedSeconds();
        timeline.sample(elapsed * config.speed, config.loop, out);
        if (config.mirror) {
            float[] m = DancePose.mirrored(out);
            System.arraycopy(m, 0, out, 0, out.length);
        }
        double blend = Math.max(config.blendSeconds, 1.0e-6);
        double w = Math.min(1.0, elapsed / blend);
        if (stopping) w = Math.min(w, 1.0 - (System.nanoTime() - stopNanos) / 1.0e9 / blend);
        w = Math.max(0.0, Math.min(1.0, w));
        return w * w * (3.0 - 2.0 * w); // smoothstep
    }

    static int keyCodeFor(DanceConfig cfg) {
        try {
            return InputConstants.getKey(cfg.keyTranslationName()).getValue();
        } catch (RuntimeException e) {
            return 71; // GLFW_KEY_G
        }
    }
}
