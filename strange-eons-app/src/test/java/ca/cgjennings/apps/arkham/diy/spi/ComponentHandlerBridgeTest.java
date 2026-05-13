package ca.cgjennings.apps.arkham.diy.spi;

import ca.cgjennings.apps.arkham.diy.DIY;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.strangeeons.plugins.conventional.ComponentHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.toByteArray();
    }
}
