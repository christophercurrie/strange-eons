package ca.cgjennings.apps.arkham.diy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.plugins.TestScriptLanguage;
import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.strangeeons.scripts.ScriptException;

class DIYHandlerCreationTest {

    private static final String SPEC = "cards/card.test:Card";

    @BeforeEach
    void reset() {
        TestScriptLanguage.lastSession = null;
        TestScriptLanguage.nextHandlerFailure = null;
    }

    @Test
    void createsHandlerThroughLanguageClaimingTheExtension() throws IOException {
        DIY diy = new DIY(SPEC, null, false);

        assertThat(TestScriptLanguage.lastSession.lastHandlerSpec).isEqualTo(SPEC);
        assertThat(TestScriptLanguage.lastSession.closed).isFalse();
        assertThat(diy.getHandlerScript()).isEqualTo(SPEC);
    }

    @Test
    void recyclingClosesTheSession() throws IOException {
        DIY diy = new DIY(SPEC, null, false);

        assertThat(diy.recycleScriptMonkey()).isTrue();
        assertThat(TestScriptLanguage.lastSession.closed).isTrue();
        assertThat(diy.recycleScriptMonkey()).isFalse();
    }

    @Test
    void failsWhenNoLanguageClaimsTheExtension() {
        assertThatThrownBy(() -> new DIY("cards/card.unclaimed:Card", null, false))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(".unclaimed");
    }

    @Test
    void classHandlerHasNoSession() throws IOException {
        DIY diy = new DIY(EmptyHandler.class.getName(), null, false);

        assertThat(diy.recycleScriptMonkey()).isFalse();
    }

    @Test
    void closesSessionWhenHandlerCreationFails() {
        ScriptException failure = new ScriptException("bad script");
        TestScriptLanguage.nextHandlerFailure = failure;

        assertThatThrownBy(() -> new DIY(SPEC, null, false))
                .isInstanceOf(IOException.class)
                .hasCause(failure);
        assertThat(TestScriptLanguage.lastSession.closed).isTrue();
    }

    @Test
    void reportsMissingScript() {
        TestScriptLanguage.nextHandlerFailure = new ScriptException(SPEC, new FileNotFoundException(SPEC));

        assertThatThrownBy(() -> new DIY(SPEC, null, false))
                .isInstanceOf(FileNotFoundException.class);
        assertThat(TestScriptLanguage.lastSession.closed).isTrue();
    }

    @Test
    void conversionCallsTheScriptHandler() throws IOException {
        DIY diy = new DIY(SPEC, null, false);
        Handler handler = (Handler) TestScriptLanguage.lastSession.lastHandler;
        ConversionSession conversion = mock(ConversionSession.class);

        diy.convertFrom(conversion);
        diy.convertTo(conversion);

        InOrder order = inOrder(handler);
        order.verify(handler).onConvertFrom(diy, conversion);
        order.verify(handler).onConvertTo(diy, conversion);
    }

    @Test
    void conversionCallsAClassHandler() throws IOException {
        DIY diy = new DIY(ConvertingHandler.class.getName(), null, false);

        diy.convertFrom(null);
        diy.convertTo(null);

        assertThat(diy.getSettings().get("converted")).isEqualTo("from to");
    }

    @Test
    void classHandlerWithoutHooksConvertsQuietly() throws IOException {
        DIY diy = new DIY(EmptyHandler.class.getName(), null, false);

        diy.convertFrom(null);
        diy.convertTo(null);
    }

    public static class ConvertingHandler extends EmptyHandler {
        @Override
        public void onConvertFrom(DIY diy, ConversionSession session) {
            diy.getSettings().set("converted", "from");
        }

        @Override
        public void onConvertTo(DIY diy, ConversionSession session) {
            diy.getSettings().set("converted", diy.getSettings().get("converted") + " to");
        }
    }

    public static class EmptyHandler implements Handler {
        @Override public void create(DIY diy) {}
        @Override public void createInterface(DIY diy, DIYEditor editor) {}
        @Override public void createFrontPainter(DIY diy, DIYSheet sheet) {}
        @Override public void createBackPainter(DIY diy, DIYSheet sheet) {}
        @Override public void paintFront(Graphics2D g, DIY diy, DIYSheet sheet) {}
        @Override public void paintBack(Graphics2D g, DIY diy, DIYSheet sheet) {}
        @Override public void onClear(DIY diy) {}
        @Override public void onRead(DIY diy, ObjectInputStream in) {}
        @Override public void onWrite(DIY diy, ObjectOutputStream out) {}
        @Override public int getPortraitCount() { return 0; }
        @Override public Portrait getPortrait(int index) { return null; }
    }
}
