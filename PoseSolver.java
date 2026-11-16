package dev.mcdance;

/**
 * Turns one animation pose into model-part transforms for the vanilla humanoid rig.
 *
 * Part order: 0 head, 1 body, 2 right arm, 3 left arm, 4 right leg, 5 left leg.
 * pos[part] = {x, y, z} in model pixels, rot[part] = {xRot, yRot, zRot} in radians (ModelPart convention).
 * On input pos/rot hold the vanilla pose; on output they hold the vanilla pose blended toward the dance by weight.
 */
final class PoseSolver {
    static final int HEAD = 0, BODY = 1, R_ARM = 2, L_ARM = 3, R_LEG = 4, L_LEG = 5;
    static final double LEG_LENGTH = 12.0;

    private PoseSolver() {}

    static void solve(float[] c, double[][] pos, double[][] rot, double weight, double yawOffsetDeg) {
        double[] hip = {0, 0.5 * (pos[R_LEG][1] + pos[L_LEG][1]), 0.5 * (pos[R_LEG][2] + pos[L_LEG][2])};

        Mat3 torso = euler(c, DancePose.TORSO);
        Mat3[] local = new Mat3[6];
        local[BODY] = Mat3.IDENTITY;
        local[HEAD] = euler(c, DancePose.HEAD);
        local[R_ARM] = euler(c, DancePose.R_ARM);
        local[L_ARM] = euler(c, DancePose.L_ARM);
        local[R_LEG] = euler(c, DancePose.R_LEG);
        local[L_LEG] = euler(c, DancePose.L_LEG);

        double[][] p = new double[6][];
        Mat3[] world = new Mat3[6];
        for (int part : new int[] {BODY, HEAD, R_ARM, L_ARM}) {
            // Everything above the hips swings around the hip pivot as one rigid torso.
            double[] r = torso.apply(pos[part][0] - hip[0], pos[part][1] - hip[1], pos[part][2] - hip[2]);
            p[part] = new double[] {hip[0] + r[0], hip[1] + r[1], hip[2] + r[2]};
            world[part] = torso.mul(local[part]);
        }
        for (int part : new int[] {R_LEG, L_LEG}) {
            p[part] = pos[part].clone();
            world[part] = local[part];
        }

        // Keep the lowest foot on the ground: bent/spread legs shorten the leg, so the hips must drop.
        double lowest = Math.max(world[R_LEG].m11, world[L_LEG].m11);
        double drop = LEG_LENGTH * (1.0 - lowest) + c[DancePose.BOUNCE];
        drop = Math.max(0.0, Math.min(13.0, drop));
        for (double[] q : p) q[1] += drop;

        // Whole-body turn about the vertical axis.
        Mat3 yaw = Mat3.ry(Math.toRadians(c[DancePose.ROOT_YAW] + yawOffsetDeg));
        for (int i = 0; i < 6; i++) {
            double[] r = yaw.apply(p[i][0], p[i][1], p[i][2]);
            p[i] = r;
            world[i] = yaw.mul(world[i]);
        }

        for (int i = 0; i < 6; i++) {
            double[] e = world[i].toEulerZYX();
            for (int k = 0; k < 3; k++) {
                pos[i][k] = pos[i][k] + (p[i][k] - pos[i][k]) * weight;
                double d = e[k] - rot[i][k];
                d = d - 2 * Math.PI * Math.floor((d + Math.PI) / (2 * Math.PI)); // shortest way round
                rot[i][k] = rot[i][k] + d * weight;
            }
        }
    }

    private static Mat3 euler(float[] c, int at) {
        return Mat3.eulerZYX(Math.toRadians(c[at]), Math.toRadians(c[at + 1]), Math.toRadians(c[at + 2]));
    }
}
