package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.diy.DIYSheet;
import ca.cgjennings.graphics.spi.AwtGraphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.strangeeons.plugins.conventional.ComponentHandler;
import org.strangeeons.plugins.conventional.EditorPanel;
import org.strangeeons.plugins.conventional.Face;
import org.strangeeons.plugins.graphics.Graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ComponentHandlerBridge}. The bridge holds no
 * DIY state — each call obtains settings from the DIY passed in by
 * the framework, and transfers handler config onto the DIY at
 * {@code create} time.
 */
class ComponentHandlerBridgeTest {

    @Test
    void delegateExposed() {
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);
        assertSame(delegate, bridge.getDelegate());
    }

    @Test
    void createForwardsToDelegateWithSettings() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(1);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(delegate).create(any());
    }

    @Test
    void createTransfersFaceCountToDiy() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(4);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(diy).setFaceStyle(DIY.FaceStyle.FOUR_FACES);
    }

    @Test
    void createTransfersTemplateKeysToDiy() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(2);
        when(delegate.getTemplateKey(0)).thenReturn("lore-front");
        when(delegate.getTemplateKey(1)).thenReturn("lore-back");
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(diy).setTemplateKey(0, "lore-front");
        verify(diy).setTemplateKey(1, "lore-back");
    }

    @Test
    void createSkipsNullTemplateKeys() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(2);
        when(delegate.getTemplateKey(0)).thenReturn("lore-front");
        when(delegate.getTemplateKey(1)).thenReturn(null);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(diy).setTemplateKey(0, "lore-front");
        verify(diy, never()).setTemplateKey(org.mockito.Mockito.eq(1), any());
    }

    @Test
    void createTransfersPortraitKeyToDiy() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(1);
        when(delegate.getPortraitKey()).thenReturn("portrait");
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(diy).setPortraitKey("portrait");
    }

    @Test
    void createSkipsNullPortraitKey() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(1);
        when(delegate.getPortraitKey()).thenReturn(null);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.create(diy);
        verify(diy, never()).setPortraitKey(any());
    }

    @Test
    void createRejectsUnmappedFaceCount() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getFaceCount()).thenReturn(5);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        assertThrows(IllegalArgumentException.class, () -> bridge.create(diy));
    }

    @Test
    void onClearForwards() {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.onClear(diy);
        verify(delegate).onClear(any());
    }

    @Test
    void onReadDelegatesStream() throws Exception {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(serialize("x")))) {
            bridge.onRead(diy, in);
        }
        verify(delegate).onRead(any(), any(InputStream.class));
    }

    @Test
    void onReadWrapsIOExceptionAsRuntime() throws Exception {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        doThrow(new IOException("boom"))
                .when(delegate).onRead(any(), any(InputStream.class));
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(serialize("x")))) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> bridge.onRead(diy, in));
            assertEquals("boom", ex.getCause().getMessage());
        }
    }

    @Test
    void onWriteWrapsIOExceptionAsRuntime() throws Exception {
        DIY diy = mock(DIY.class);
        ComponentHandler delegate = mock(ComponentHandler.class);
        doThrow(new IOException("kaboom"))
                .when(delegate).onWrite(any(), any(OutputStream.class));
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> bridge.onWrite(diy, out));
            assertEquals("kaboom", ex.getCause().getMessage());
        }
    }

    @Test
    void portraitCountForwardsToHandler() {
        ComponentHandler delegate = mock(ComponentHandler.class);
        when(delegate.getPortraitCount()).thenReturn(3);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        assertEquals(3, bridge.getPortraitCount());
    }

    // ---- Face dispatch (mock DIYSheet, enabled by null-guard fix) ----

    @Test
    void createFrontPainterRoutesToCreatePainterWithFace() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(0);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.createFrontPainter(diy, sheet);

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(delegate).createPainter(faceCaptor.capture());
        assertEquals(0, faceCaptor.getValue().getFaceIndex());
    }

    @Test
    void createBackPainterRoutesToCreatePainterWithFace() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(1);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.createBackPainter(diy, sheet);

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(delegate).createPainter(faceCaptor.capture());
        assertEquals(1, faceCaptor.getValue().getFaceIndex());
    }

    @Test
    void paintFrontRoutesToPaintWithGraphics() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(0);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        Graphics2D g = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            bridge.paintFront(g, diy, sheet);

            ArgumentCaptor<Graphics> gCaptor = ArgumentCaptor.forClass(Graphics.class);
            verify(delegate).paint(any(Face.class), gCaptor.capture());
            assertNotNull(gCaptor.getValue());
            assertSame(g, ((AwtGraphics) gCaptor.getValue()).graphics2D());
        } finally {
            g.dispose();
        }
    }

    @Test
    void faceInstanceIsStableAcrossCallsForSameSheet() {
        DIY diy = mock(DIY.class);
        DIYSheet sheet = mock(DIYSheet.class);
        when(sheet.getGameComponent()).thenReturn(diy);
        when(sheet.getSheetIndex()).thenReturn(2);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.createFrontPainter(diy, sheet);
        bridge.createFrontPainter(diy, sheet);

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(delegate, times(2)).createPainter(faceCaptor.capture());
        assertSame(faceCaptor.getAllValues().get(0), faceCaptor.getAllValues().get(1));
    }

    @Test
    void differentSheetsProduceDifferentFaces() {
        DIY diy = mock(DIY.class);
        DIYSheet sheetA = mock(DIYSheet.class);
        DIYSheet sheetB = mock(DIYSheet.class);
        when(sheetA.getGameComponent()).thenReturn(diy);
        when(sheetB.getGameComponent()).thenReturn(diy);
        when(sheetA.getSheetIndex()).thenReturn(0);
        when(sheetB.getSheetIndex()).thenReturn(1);
        ComponentHandler delegate = mock(ComponentHandler.class);
        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);

        bridge.createFrontPainter(diy, sheetA);
        bridge.createBackPainter(diy, sheetB);

        ArgumentCaptor<Face> faceCaptor = ArgumentCaptor.forClass(Face.class);
        verify(delegate, times(2)).createPainter(faceCaptor.capture());
        assertEquals(0, faceCaptor.getAllValues().get(0).getFaceIndex());
        assertEquals(1, faceCaptor.getAllValues().get(1).getFaceIndex());
    }

    @Test
    void createInterfaceDispatchesAndAttachesPanel() {
        DIY diy = mock(DIY.class);
        DIYEditor editor = mock(DIYEditor.class);
        JTabbedPane tabs = new JTabbedPane();
        when(editor.getTabbedPane()).thenReturn(tabs);

        ComponentHandler delegate = mock(ComponentHandler.class);
        org.mockito.Mockito.doAnswer(inv -> {
            EditorPanel panel = inv.getArgument(1);
            panel.addControl("Name", new JTextField());
            return null;
        }).when(delegate).createInterface(any(), any(EditorPanel.class));

        ComponentHandlerBridge bridge = new ComponentHandlerBridge(delegate);
        bridge.createInterface(diy, editor);

        verify(delegate).createInterface(any(), any(EditorPanel.class));
        assertEquals(1, tabs.getTabCount());
        assertEquals(DIYEditorPanel.TAB_TITLE, tabs.getTitleAt(0));
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.toByteArray();
    }
}
