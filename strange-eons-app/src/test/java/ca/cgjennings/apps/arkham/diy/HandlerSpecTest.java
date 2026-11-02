package ca.cgjennings.apps.arkham.diy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HandlerSpecTest {

    @Test
    void findsExtensionOfScriptExport() {
        assertThat(HandlerSpec.scriptExtension("a/b/card.mjs:Card")).isEqualTo(".mjs");
        assertThat(HandlerSpec.scriptExtension("res://a/card.mjs:Card")).isEqualTo(".mjs");
        assertThat(HandlerSpec.scriptExtension("project:a/card.mjs:Card")).isEqualTo(".mjs");
        assertThat(HandlerSpec.scriptExtension("c.d/card.mjs:Card")).isEqualTo(".mjs");
    }

    @Test
    void rejectsOtherForms() {
        assertThat(HandlerSpec.scriptExtension("a/b/card.js")).isNull();
        assertThat(HandlerSpec.scriptExtension("res://a/b/card.js")).isNull();
        assertThat(HandlerSpec.scriptExtension("org.example.CardHandler")).isNull();
        assertThat(HandlerSpec.scriptExtension("a/card.mjs:")).isNull();
        assertThat(HandlerSpec.scriptExtension("a/card:Card")).isNull();
        assertThat(HandlerSpec.scriptExtension("a.b/card:Card")).isNull();
        assertThat(HandlerSpec.scriptExtension("a/card.:Card")).isNull();
    }
}
