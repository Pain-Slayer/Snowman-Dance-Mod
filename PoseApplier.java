package dev.mcdance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Copies the solved dance pose onto the player model's parts. */
public final class PoseApplier {
    private PoseApplier() {}

    public static void apply(HumanoidModel<?> model) {
        // In first person only the hand is drawn, and that must keep its normal animation.
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        float[] channels = new float[DancePose.CHANNELS];
        double weight = DanceController.sample(channels);
        if (weight < 0.0) return;

        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        double[][] pos = new double[6][3];
        double[][] rot = new double[6][3];
        for (int i = 0; i < 6; i++) {
            ModelPart p = parts[i];
            pos[i][0] = p.x; pos[i][1] = p.y; pos[i][2] = p.z;
            rot[i][0] = p.xRot; rot[i][1] = p.yRot; rot[i][2] = p.zRot;
        }

        PoseSolver.solve(channels, pos, rot, weight, DanceController.config.yawOffsetDegrees);

        for (int i = 0; i < 6; i++) {
            ModelPart p = parts[i];
            p.x = (float) pos[i][0]; p.y = (float) pos[i][1]; p.z = (float) pos[i][2];
            p.xRot = (float) rot[i][0]; p.yRot = (float) rot[i][1]; p.zRot = (float) rot[i][2];
        }
    }
}
