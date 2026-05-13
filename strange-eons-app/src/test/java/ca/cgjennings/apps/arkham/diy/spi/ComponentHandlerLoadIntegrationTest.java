package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JTabbedPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.strangeeons.plugins.SettingsReadWrite;
import org.strangeeons.plugins.conventional.EditorPanel;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration coverage for the {@link ComponentHandlerBridge}
 * dispatch path. One smoke test exercises the production class-name
 * load path; the rest construct a real {@link DIY} via the existing
 * public constructor, build a bridge around a spy
 * {@link TestComponentHandler}, and verify dispatch with Mockito.
 */
class ComponentHandlerLoadIntegrationTest {

    private DIY diy;
    private TestComponentHandler handler;
    private ComponentHandlerBridge bridge;

    @BeforeEach
    void setUp() throws Exception {
        diy = new DIY(TestComponentHandler.class.getName(), null, false);
        handler = spy(new TestComponentHandler());
        bridge = new ComponentHandlerBridge(handler);
    }

    @Test
    void classNameLoadPathConstructsDiy() {
        assertNotNull(diy);
    }

    @Test
    void paintFrontReachesSpiHandlerWithFaceAndGraphics() {
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(0);

        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            bridge.paintFront(g, diy, sheet);
        } finally {
            g.dispose();
        }

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(handler).paint(faceCaptor.capture(), any(Graphics.class));
        assertEquals(0, faceCaptor.getValue().getFaceIndex());
    }

    @Test
    void createBackPainterRoutesToCreatePainterWithFaceIndex() {
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(1);

        bridge.createBackPainter(diy, sheet);

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(handler).createPainter(faceCaptor.capture());
        assertEquals(1, faceCaptor.getValue().getFaceIndex());
    }

    @Test
    void createInterfaceReachesSpiHandlerWithEditorPanel() {
        DIYEditor editor = mock(DIYEditor.class);
        when(editor.getTabbedPane()).thenReturn(new JTabbedPane());

        bridge.createInterface(diy, editor);

        verify(handler).createInterface(any(SettingsReadWrite.class), any(EditorPanel.class));
    }

    @Test
    void onClearReachesSpiHandler() {
        bridge.onClear(diy);
        verify(handler).onClear(any(SettingsReadWrite.class));
    }
}
