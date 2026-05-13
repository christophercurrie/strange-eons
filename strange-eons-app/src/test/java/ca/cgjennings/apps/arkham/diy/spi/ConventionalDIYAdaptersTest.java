package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@link ConventionalDIYFace} adapter delegates
 * correctly. Component-level adaptation now happens in
 * {@link ComponentHandlerBridge} at create time and is covered by
 * {@link ComponentHandlerBridgeTest}.
 */
class ConventionalDIYAdaptersTest {

    @Test
    void faceExposesSheetIndex() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(3);

        ConventionalDIYFace face = new ConventionalDIYFace(sheet);
        assertEquals(3, face.getFaceIndex());
    }

    @Test
    void faceDelegatesTemplateKeyThroughDiy() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(0);
        when(diy.getTemplateKey(0)).thenReturn("lore-front");

        ConventionalDIYFace face = new ConventionalDIYFace(sheet);
        assertEquals("lore-front", face.getTemplateKey());
    }

    @Test
    void faceRoundsTemplateResolution() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getTemplateResolution()).thenReturn(149.6);

        ConventionalDIYFace face = new ConventionalDIYFace(sheet);
        assertEquals(150, face.getTemplateResolution());
    }

    @Test
    void faceExposesDiySettings() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        resources.Settings stub = mock(resources.Settings.class);
        when(diy.getSettings()).thenReturn(stub);

        ConventionalDIYFace face = new ConventionalDIYFace(sheet);
        assertSame(stub, face.getSettings());
    }
}
