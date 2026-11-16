package dev.mcdance;

/** Channel layout of one pose (angles in degrees). */
final class DancePose {
    static final int ROOT_YAW = 0;
    static final int BOUNCE = 1;
    static final int TORSO = 2;   // pitch, yaw, roll
    static final int HEAD = 5;    // pitch, yaw, roll
    static final int R_ARM = 8;   // xRot, yRot, zRot
    static final int L_ARM = 11;
    static final int R_LEG = 14;
    static final int L_LEG = 17;
    static final int CHANNELS = 20;

    private DancePose() {}

    static float[] neutral() {
        float[] p = new float[CHANNELS];
        p[R_ARM + 2] = 8f;
        p[L_ARM + 2] = -8f;
        return p;
    }

    /** Left/right mirrored copy of a pose. */
    static float[] mirrored(float[] p) {
        float[] m = new float[CHANNELS];
        m[ROOT_YAW] = -p[ROOT_YAW];
        m[BOUNCE] = p[BOUNCE];
        copyMirrored(p, TORSO, m, TORSO);
        copyMirrored(p, HEAD, m, HEAD);
        copyMirrored(p, L_ARM, m, R_ARM);
        copyMirrored(p, R_ARM, m, L_ARM);
        copyMirrored(p, L_LEG, m, R_LEG);
        copyMirrored(p, R_LEG, m, L_LEG);
        return m;
    }

    private static void copyMirrored(float[] src, int s, float[] dst, int d) {
        dst[d] = src[s];
        dst[d + 1] = -src[s + 1];
        dst[d + 2] = -src[s + 2];
    }
}
