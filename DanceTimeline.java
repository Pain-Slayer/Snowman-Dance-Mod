package dev.mcdance;

import java.util.List;
import java.util.Map;

/** Keyframe timeline with smooth (Hermite / Catmull-Rom style) interpolation. */
final class DanceTimeline {
    /** Seconds used to blend from the last keyframe back to the first when looping. */
    static final double LOOP_GAP = 0.6;

    private final double[] times;
    private final float[][] values;

    DanceTimeline(double[] times, float[][] values) {
        if (times.length < 2) throw new IllegalArgumentException("A dance needs at least 2 keyframes");
        for (int i = 1; i < times.length; i++) {
            if (!(times[i] > times[i - 1])) throw new IllegalArgumentException("Keyframe times must strictly increase (keyframe " + i + ")");
        }
        double t0 = times[0];
        double[] shifted = new double[times.length];
        for (int i = 0; i < times.length; i++) shifted[i] = times[i] - t0; // first keyframe is always t = 0
        this.times = shifted;
        this.values = values;
    }

    static DanceTimeline parse(String json) {
        Map<String, Object> root = MiniJson.asObject(MiniJson.parse(json), "animation file");
        List<Object> keys = MiniJson.asArray(root.get("keyframes"), "\"keyframes\"");
        double[] t = new double[keys.size()];
        float[][] v = new float[keys.size()][];
        for (int k = 0; k < keys.size(); k++) {
            String where = "keyframe " + k;
            Map<String, Object> kf = MiniJson.asObject(keys.get(k), where);
            t[k] = MiniJson.asNumber(kf.get("t"), where + " \"t\"");
            float[] p = DancePose.neutral();
            if (kf.containsKey("rootYaw")) p[DancePose.ROOT_YAW] = (float) MiniJson.asNumber(kf.get("rootYaw"), where + " rootYaw");
            if (kf.containsKey("bounce")) p[DancePose.BOUNCE] = (float) MiniJson.asNumber(kf.get("bounce"), where + " bounce");
            triple(kf, "torso", p, DancePose.TORSO, where);
            triple(kf, "head", p, DancePose.HEAD, where);
            triple(kf, "rArm", p, DancePose.R_ARM, where);
            triple(kf, "lArm", p, DancePose.L_ARM, where);
            triple(kf, "rLeg", p, DancePose.R_LEG, where);
            triple(kf, "lLeg", p, DancePose.L_LEG, where);
            v[k] = p;
        }
        return new DanceTimeline(t, v);
    }

    private static void triple(Map<String, Object> kf, String key, float[] p, int at, String where) {
        if (!kf.containsKey(key)) return;
        List<Object> a = MiniJson.asArray(kf.get(key), where + " \"" + key + "\"");
        if (a.size() != 3) throw new IllegalArgumentException(where + " \"" + key + "\" needs exactly 3 numbers");
        for (int i = 0; i < 3; i++) p[at + i] = (float) MiniJson.asNumber(a.get(i), where + " \"" + key + "\"");
    }

    /** Length of one playthrough (last keyframe time), in seconds. */
    double duration() {
        return times[times.length - 1];
    }

    /** Length of one loop cycle including the wrap-around blend. */
    double loopLength() {
        return duration() + LOOP_GAP;
    }

    /**
     * Samples the pose at time t (seconds). When loop is true the time wraps and the last pose blends back into the first.
     */
    void sample(double t, boolean loop, float[] out) {
        int n = times.length;
        double[] ts;
        float[][] vs;
        double period = 0;
        if (loop) {
            period = loopLength();
            t = ((t % period) + period) % period;
            ts = new double[n + 1];
            vs = new float[n + 1][];
            System.arraycopy(times, 0, ts, 0, n);
            System.arraycopy(values, 0, vs, 0, n);
            ts[n] = period;
            vs[n] = values[0];
        } else {
            t = Math.max(0, Math.min(duration(), t));
            ts = times;
            vs = values;
        }
        int m = ts.length;
        int seg = 0;
        while (seg < m - 2 && t > ts[seg + 1]) seg++;
        double t0 = ts[seg], t1 = ts[seg + 1];
        double dt = t1 - t0;
        double u = dt > 0 ? (t - t0) / dt : 0;
        double u2 = u * u, u3 = u2 * u;
        double h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
        for (int c = 0; c < DancePose.CHANNELS; c++) {
            double p0 = vs[seg][c], p1 = vs[seg + 1][c];
            double m0 = tangent(ts, vs, seg, c, loop, period);
            double m1 = tangent(ts, vs, seg + 1, c, loop, period);
            out[c] = (float) (h00 * p0 + h10 * dt * m0 + h01 * p1 + h11 * dt * m1);
        }
    }

    /** Catmull-Rom tangent at point i. Non-looping ends use zero slope (ease in / ease out). */
    private static double tangent(double[] ts, float[][] vs, int i, int c, boolean loop, double period) {
        int m = ts.length;
        if (loop) {
            int cyc = m - 1; // last point duplicates first
            int ip = (i - 1 + cyc) % cyc;
            int in = (i + 1) % cyc;
            double tPrev = ts[ip] - (i - 1 < 0 ? period : 0);
            double tNext = ts[in] + (i + 1 >= cyc ? period : 0);
            // i == cyc (the duplicate of point 0) behaves like point 0 shifted by one period
            if (i == cyc) {
                tPrev = ts[cyc - 1];
                tNext = ts[1] + period;
                return (vs[1][c] - vs[cyc - 1][c]) / (tNext - tPrev);
            }
            return (vs[in][c] - vs[ip][c]) / (tNext - tPrev);
        }
        if (i == 0 || i == m - 1) return 0;
        return (vs[i + 1][c] - vs[i - 1][c]) / (ts[i + 1] - ts[i - 1]);
    }
}
