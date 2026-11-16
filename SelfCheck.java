package dev.mcdance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Plain-Java sanity checks for the Minecraft-independent logic. Run with: java dev.mcdance.SelfCheck <dance.json> */
public final class SelfCheck {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + msg);
        if (!ok) fails++;
    }

    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of(args[0]));
        DanceTimeline tl = DanceTimeline.parse(json);
        check(Math.abs(tl.duration() - 14.0) < 1e-9, "timeline parsed, duration = " + tl.duration() + " s");

        // Euler round trip
        Random rnd = new Random(1);
        double worst = 0;
        for (int i = 0; i < 20000; i++) {
            double x = (rnd.nextDouble() - 0.5) * 3.0, y = (rnd.nextDouble() - 0.5) * 3.0, z = (rnd.nextDouble() - 0.5) * 6.0;
            Mat3 a = Mat3.eulerZYX(x, y, z);
            double[] e = a.toEulerZYX();
            Mat3 b = Mat3.eulerZYX(e[0], e[1], e[2]);
            double d = Math.abs(a.m00 - b.m00) + Math.abs(a.m11 - b.m11) + Math.abs(a.m22 - b.m22) + Math.abs(a.m01 - b.m01) + Math.abs(a.m12 - b.m12) + Math.abs(a.m20 - b.m20);
            worst = Math.max(worst, d);
        }
        check(worst < 1e-9, "Euler ZYX round trip, worst matrix error " + worst);

        // Axis conventions (match Minecraft): right arm at x<0; Rz(+) swings a hanging arm outward (toward -x); Rx(-) swings it forward (-z)
        double[] down = Mat3.rz(Math.toRadians(30)).apply(0, 1, 0);
        check(down[0] < 0, "right arm zRot + swings outward (toward -x): x = " + round(down[0]));
        double[] fwd = Mat3.rx(Math.toRadians(-90)).apply(0, 1, 0);
        check(fwd[2] < -0.99, "arm xRot -90 points forward (-z): z = " + round(fwd[2]));
        double[] lean = Mat3.rx(Math.toRadians(30)).apply(0, -1, 0);
        check(lean[2] < 0, "torso pitch + leans forward (-z): z = " + round(lean[2]));

        // Neutral pose through the solver
        float[] neutral = DancePose.neutral();
        double[][] pos = basePos(), rot = new double[6][3];
        PoseSolver.solve(neutral, pos, rot, 1.0, 0);
        check(Math.abs(rot[PoseSolver.BODY][0]) < 1e-9 && Math.abs(rot[PoseSolver.BODY][1]) < 1e-9 && Math.abs(rot[PoseSolver.BODY][2]) < 1e-9, "neutral pose: body not rotated");
        check(Math.abs(pos[PoseSolver.BODY][1]) < 1e-9 && Math.abs(pos[PoseSolver.R_LEG][1] - 12) < 1e-9, "neutral pose: no hip drop, leg pivot stays at y = 12");
        check(Math.abs(Math.toDegrees(rot[PoseSolver.R_ARM][2]) - 8) < 1e-6, "neutral right arm zRot = 8 deg");

        // Foot-on-ground: both legs -45 deg
        float[] lunge = DancePose.neutral();
        lunge[DancePose.R_LEG] = -45; lunge[DancePose.L_LEG] = -45;
        pos = basePos(); rot = new double[6][3];
        PoseSolver.solve(lunge, pos, rot, 1.0, 0);
        double expected = 12 * (1 - Math.cos(Math.toRadians(45)));
        check(Math.abs(pos[PoseSolver.R_LEG][1] - (12 + expected)) < 1e-9, "legs at -45 deg drop the hips by " + round(expected) + " px");

        // Torso lean keeps the hip pivot fixed
        float[] leanPose = DancePose.neutral();
        leanPose[DancePose.TORSO] = 35;
        pos = basePos(); rot = new double[6][3];
        PoseSolver.solve(leanPose, pos, rot, 1.0, 0);
        double bodyY = pos[PoseSolver.BODY][1], bodyZ = pos[PoseSolver.BODY][2];
        check(Math.abs(bodyZ - (-12 * Math.sin(Math.toRadians(35)))) < 1e-9 && Math.abs(bodyY - (12 - 12 * Math.cos(Math.toRadians(35)))) < 1e-9,
            "torso pitch 35 deg moves body pivot to y=" + round(bodyY) + ", z=" + round(bodyZ));

        // Weight 0 = vanilla untouched
        pos = basePos(); rot = new double[6][3];
        double[][] before = basePos();
        float[] busy = new float[DancePose.CHANNELS];
        tl.sample(7.5, false, busy);
        PoseSolver.solve(busy, pos, rot, 0.0, 0);
        boolean same = true;
        for (int i = 0; i < 6; i++) for (int k = 0; k < 3; k++) same &= Math.abs(pos[i][k] - before[i][k]) < 1e-12 && Math.abs(rot[i][k]) < 1e-12;
        check(same, "weight 0 leaves the vanilla pose untouched");

        // Keyframe fidelity (non-loop): sampling exactly on keyframe times returns the keyframe values
        float[] tmp = new float[DancePose.CHANNELS];
        tl.sample(3.5, false, tmp);
        check(Math.abs(tmp[DancePose.ROOT_YAW] + 42) < 1e-4 && Math.abs(tmp[DancePose.R_ARM] + 60) < 1e-4 && Math.abs(tmp[DancePose.R_ARM + 2] - 50) < 1e-4, "t=3.5 s reproduces keyframe (rootYaw -42, rArm [-60,0,50])");

        // Mirror twice = identity
        float[] m2 = DancePose.mirrored(DancePose.mirrored(tmp));
        boolean id = true;
        for (int i = 0; i < DancePose.CHANNELS; i++) id &= Math.abs(m2[i] - tmp[i]) < 1e-6;
        check(id, "mirroring twice returns the original pose");

        // Smoothness: largest per-channel speed over the whole dance, and loop seam continuity
        double dt = 1.0 / 240.0, maxSpeed = 0, maxAt = 0; int maxCh = 0;
        float[] a = new float[DancePose.CHANNELS], b = new float[DancePose.CHANNELS];
        tl.sample(0, false, a);
        for (double t = dt; t <= tl.duration(); t += dt) {
            tl.sample(t, false, b);
            for (int c = 2; c < DancePose.CHANNELS; c++) {
                double sp = Math.abs(b[c] - a[c]) / dt;
                if (sp > maxSpeed) { maxSpeed = sp; maxAt = t; maxCh = c; }
            }
            System.arraycopy(b, 0, a, 0, a.length);
        }
        System.out.println("INFO  fastest joint motion: " + Math.round(maxSpeed) + " deg/s on channel " + maxCh + " at t=" + round(maxAt) + " s");
        check(maxSpeed < 2500, "no teleporting joints (max speed < 2500 deg/s)");

        double period = tl.loopLength();
        float[] e1 = new float[DancePose.CHANNELS], e2 = new float[DancePose.CHANNELS];
        tl.sample(period - 1e-6, true, e1);
        tl.sample(period + 1e-6, true, e2);
        double seam = 0;
        for (int c = 0; c < DancePose.CHANNELS; c++) seam = Math.max(seam, Math.abs(e1[c] - e2[c]));
        check(seam < 1e-3, "loop seam is continuous (max channel jump " + seam + " deg)");

        float[] first = new float[DancePose.CHANNELS];
        tl.sample(0, true, first);
        check(Math.abs(first[DancePose.ROOT_YAW] + 70) < 1e-4, "looped timeline starts at keyframe 0 again");

        // Config
        DanceConfig cfg = DanceConfig.fromJson("{\"activationKey\":\"F9\",\"loop\":true,\"speed\":9,\"interruptible\":false}");
        check(cfg.keyTranslationName().equals("key.keyboard.f9") && cfg.loop && cfg.speed == 4.0 && !cfg.interruptible, "config parsing and clamping (speed 9 -> 4.0)");
        DanceConfig back = DanceConfig.fromJson(cfg.toJson());
        check(back.loop == cfg.loop && back.speed == cfg.speed && back.activationKey.equals("F9"), "config write/read round trip");
        check(new DanceConfig().keyTranslationName().equals("key.keyboard.g"), "default key is G");

        boolean threw = false;
        try { DanceTimeline.parse("{\"keyframes\":[{\"t\":1},{\"t\":0.5}]}"); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "out-of-order keyframes are rejected with an error");

        System.out.println(fails == 0 ? "\nALL CHECKS PASSED" : "\n" + fails + " CHECK(S) FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }

    static double[][] basePos() {
        return new double[][] {
            {0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9, 12, 0}, {1.9, 12, 0}
        };
    }

    static double round(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
