package ca.cgjennings.graphics.filters;

import org.strangeeons.core.graphics.ImageFormats;

/**
 * A filter that creates an identical copy of the source image.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings {@literal <https://cgjennings.ca/contact>}
 */
public final class CloneFilter extends AbstractPixelwiseFilter {

    public CloneFilter() {
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
    }
}
