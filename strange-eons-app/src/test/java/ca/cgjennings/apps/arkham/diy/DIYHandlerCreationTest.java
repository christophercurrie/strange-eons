package ca.cgjennings.apps.arkham.diy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.plugins.TestScriptLanguage;
import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    void conversionHooksGoThroughTheSession() throws IOException {
        DIY diy = new DIY(SPEC, null, false);

        diy.convertFrom(null);
        diy.convertTo(null);

        assertThat(TestScriptLanguage.lastSession.optionalCalls).containsExactly("onConvertFrom", "onConvertTo");
    }

    public static final class EmptyHandler implements Handler {
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
