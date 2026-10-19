package ca.cgjennings.apps.arkham;

import org.junit.jupiter.api.Test;

import static ca.cgjennings.apps.arkham.AbstractViewer.MAX_ZOOM;
import static ca.cgjennings.apps.arkham.AbstractViewer.MIN_ZOOM;
import static ca.cgjennings.apps.arkham.AbstractViewer.WHEEL_ZOOM_FACTOR;
import static ca.cgjennings.apps.arkham.AbstractViewer.wheelZoom;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractViewerTest {

    private static final double EPSILON = 1e-9;

    @Test
    void negativeRotationZoomsIn() {
        assertEquals(WHEEL_ZOOM_FACTOR, wheelZoom(1d, -1d, false), EPSILON);
    }

    @Test
    void positiveRotationZoomsOut() {
        assertEquals(1d / WHEEL_ZOOM_FACTOR, wheelZoom(1d, 1d, false), EPSILON);
    }

    @Test
    void invertReversesDirection() {
        assertEquals(WHEEL_ZOOM_FACTOR, wheelZoom(1d, 1d, true), EPSILON);
        assertEquals(1d / WHEEL_ZOOM_FACTOR, wheelZoom(1d, -1d, true), EPSILON);
    }

    @Test
    void smallRotationsAccumulateToOneLargeRotation() {
        double zoom = 1d;
        for (int i = 0; i < 40; ++i) {
            zoom = wheelZoom(zoom, -0.05d, false);
        }
        assertEquals(wheelZoom(1d, -2d, false), zoom, EPSILON);
    }

    @Test
    void zoomIsClampedToRange() {
        assertEquals(MAX_ZOOM, wheelZoom(1d, -100d, false), EPSILON);
        assertEquals(MIN_ZOOM, wheelZoom(1d, 100d, false), EPSILON);
    }

    @Test
    void manyTrackpadEventsCannotPassLimits() {
        double zoom = 1d;
        for (int i = 0; i < 500; ++i) {
            zoom = wheelZoom(zoom, 1d, false);
        }
        assertEquals(MIN_ZOOM, zoom, EPSILON);
    }
}
