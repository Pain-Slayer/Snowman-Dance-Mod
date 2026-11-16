package dev.mcdance;

/**
 * Immutable 3x3 rotation matrix using the same Euler convention as Minecraft's ModelPart:
 * R = Rz(zRot) * Ry(yRot) * Rx(xRot).
 */
final class Mat3 {
    static final Mat3 IDENTITY = new Mat3(1, 0, 0, 0, 1, 0, 0, 0, 1);

    final double m00, m01, m02, m10, m11, m12, m20, m21, m22;

    Mat3(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02;
        this.m10 = m10; this.m11 = m11; this.m12 = m12;
        this.m20 = m20; this.m21 = m21; this.m22 = m22;
    }

    static Mat3 rx(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Mat3(1, 0, 0, 0, c, -s, 0, s, c);
    }

    static Mat3 ry(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Mat3(c, 0, s, 0, 1, 0, -s, 0, c);
    }

    static Mat3 rz(double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Mat3(c, -s, 0, s, c, 0, 0, 0, 1);
    }

    /** Equivalent to JOML's rotationZYX(z, y, x) / ModelPart rotation order. Angles in radians. */
    static Mat3 eulerZYX(double x, double y, double z) {
        return rz(z).mul(ry(y)).mul(rx(x));
    }

    Mat3 mul(Mat3 o) {
        return new Mat3(
            m00 * o.m00 + m01 * o.m10 + m02 * o.m20, m00 * o.m01 + m01 * o.m11 + m02 * o.m21, m00 * o.m02 + m01 * o.m12 + m02 * o.m22,
            m10 * o.m00 + m11 * o.m10 + m12 * o.m20, m10 * o.m01 + m11 * o.m11 + m12 * o.m21, m10 * o.m02 + m11 * o.m12 + m12 * o.m22,
            m20 * o.m00 + m21 * o.m10 + m22 * o.m20, m20 * o.m01 + m21 * o.m11 + m22 * o.m21, m20 * o.m02 + m21 * o.m12 + m22 * o.m22);
    }

    double[] apply(double x, double y, double z) {
        return new double[] {
            m00 * x + m01 * y + m02 * z,
            m10 * x + m11 * y + m12 * z,
            m20 * x + m21 * y + m22 * z
        };
    }

    /** Returns {xRot, yRot, zRot} in radians such that eulerZYX(x, y, z) reproduces this matrix. */
    double[] toEulerZYX() {
        double sy = Math.max(-1.0, Math.min(1.0, -m20));
        double y = Math.asin(sy);
        double x, z;
        if (Math.abs(sy) < 0.999999) {
            x = Math.atan2(m21, m22);
            z = Math.atan2(m10, m00);
        } else { // gimbal lock
            x = Math.atan2(-m12, m11);
            z = 0;
        }
        return new double[] {x, y, z};
    }
}
