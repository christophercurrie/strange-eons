package ca.cgjennings.ui;

import org.junit.jupiter.api.Test;

import static ca.cgjennings.ui.WheelZoom.FACTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WheelZoomTest {

    private static final double EPSILON = 1e-9;
    private static final double MIN = 0.125d;
    private static final double MAX = 8d;

    private static double zoom(double current, double rotation, boolean invert) {
        return WheelZoom.apply(current, rotation, invert, MIN, MAX);
    }

    @Test
    void negativeRotationZoomsIn() {
        assertEquals(FACTOR, zoom(1d, -1d, false), EPSILON);
    }

    @Test
    void positiveRotationZoomsOut() {
        assertEquals(1d / FACTOR, zoom(1d, 1d, false), EPSILON);
    }

    @Test
    void invertReversesDirection() {
        assertEquals(FACTOR, zoom(1d, 1d, true), EPSILON);
        assertEquals(1d / FACTOR, zoom(1d, -1d, true), EPSILON);
    }

    @Test
    void smallRotationsAccumulateToOneLargeRotation() {
        double z = 1d;
        for (int i = 0; i < 40; ++i) {
            z = zoom(z, -0.05d, false);
        }
        assertEquals(zoom(1d, -2d, false), z, EPSILON);
    }

    @Test
    void zoomIsClampedToRange() {
        assertEquals(MAX, zoom(1d, -100d, false), EPSILON);
        assertEquals(MIN, zoom(1d, 100d, false), EPSILON);
    }

    @Test
    void manyTrackpadEventsCannotPassLimits() {
        double z = 1d;
        for (int i = 0; i < 500; ++i) {
            z = zoom(z, 1d, false);
        }
        assertEquals(MIN, z, EPSILON);
    }
}
