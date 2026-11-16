package dev.mcdance;

import java.util.Locale;
import java.util.Map;

/** User settings, stored as JSON in config/mcdance.json. */
final class DanceConfig {
    String activationKey = "G";
    boolean loop = false;
    double speed = 1.0;
    boolean interruptible = true;
    double blendSeconds = 0.35;
    boolean mirror = false;
    double yawOffsetDegrees = 0.0;
    boolean switchToThirdPerson = true;
    boolean reloadAnimationOnStart = true;

    static DanceConfig fromJson(String json) {
        Map<String, Object> o = MiniJson.asObject(MiniJson.parse(json), "config");
        DanceConfig c = new DanceConfig();
        if (o.get("activationKey") instanceof String) c.activationKey = ((String) o.get("activationKey")).trim();
        if (o.get("loop") instanceof Boolean) c.loop = (Boolean) o.get("loop");
        if (o.get("speed") instanceof Double) c.speed = (Double) o.get("speed");
        if (o.get("interruptible") instanceof Boolean) c.interruptible = (Boolean) o.get("interruptible");
        if (o.get("blendSeconds") instanceof Double) c.blendSeconds = (Double) o.get("blendSeconds");
        if (o.get("mirror") instanceof Boolean) c.mirror = (Boolean) o.get("mirror");
        if (o.get("yawOffsetDegrees") instanceof Double) c.yawOffsetDegrees = (Double) o.get("yawOffsetDegrees");
        if (o.get("switchToThirdPerson") instanceof Boolean) c.switchToThirdPerson = (Boolean) o.get("switchToThirdPerson");
        if (o.get("reloadAnimationOnStart") instanceof Boolean) c.reloadAnimationOnStart = (Boolean) o.get("reloadAnimationOnStart");
        c.sanitize();
        return c;
    }

    void sanitize() {
        speed = Math.max(0.1, Math.min(4.0, speed));
        blendSeconds = Math.max(0.0, Math.min(2.0, blendSeconds));
        yawOffsetDegrees = Math.max(-360.0, Math.min(360.0, yawOffsetDegrees));
        if (activationKey == null || activationKey.isEmpty()) activationKey = "G";
    }

    /** Minecraft key translation name, e.g. "G" -> "key.keyboard.g", "F9" -> "key.keyboard.f9". */
    String keyTranslationName() {
        String k = activationKey.toLowerCase(Locale.ROOT).replace(' ', '.');
        return k.startsWith("key.") ? k : "key.keyboard." + k;
    }

    String toJson() {
        return "{\n"
            + "  \"activationKey\": \"" + activationKey + "\",\n"
            + "  \"loop\": " + loop + ",\n"
            + "  \"speed\": " + speed + ",\n"
            + "  \"interruptible\": " + interruptible + ",\n"
            + "  \"blendSeconds\": " + blendSeconds + ",\n"
            + "  \"mirror\": " + mirror + ",\n"
            + "  \"yawOffsetDegrees\": " + yawOffsetDegrees + ",\n"
            + "  \"switchToThirdPerson\": " + switchToThirdPerson + ",\n"
            + "  \"reloadAnimationOnStart\": " + reloadAnimationOnStart + "\n"
            + "}\n";
    }
}
