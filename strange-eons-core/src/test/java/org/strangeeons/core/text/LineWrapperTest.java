package org.strangeeons.core.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LineWrapperTest {

    @Test
    void shortTextIsUnchanged() {
        String s = "short";
        assertThat(new LineWrapper("|", 10, 10, 0).wrap(s)).isSameAs(s);
    }

    @Test
    void breaksAfterSpaces() {
        LineWrapper w = new LineWrapper("|", 10, 10, 0);
        assertThat(w.wrap("aaaa bbbb cccc dddd")).isEqualTo("aaaa bbbb |cccc dddd");
    }

    @Test
    void followingLinesUseTheirOwnLength() {
        LineWrapper w = new LineWrapper("|", 6, 3, 0);
        assertThat(w.wrap("aaaaa bb cc dd")).isEqualTo("aaaaa |bb |cc |dd");
    }

    @Test
    void breaksMidWordWithoutSpaces() {
        LineWrapper w = new LineWrapper("|", 4, 4, 0);
        assertThat(w.wrap("abcdefghij")).isEqualTo("abcde|fghij");
    }

    @Test
    void toleranceAvoidsShortFinalLines() {
        LineWrapper w = new LineWrapper("|", 10, 10, 5);
        assertThat(w.wrap("aaaa bbbb ccc")).isEqualTo("aaaa bbbb ccc");
    }

    @Test
    void spacesAfterTheBreakStayOnTheFirstLine() {
        LineWrapper w = new LineWrapper("|", 5, 5, 0);
        assertThat(w.wrap("aaaa   bbbb cccc")).isEqualTo("aaaa   |bbbb |cccc");
    }

    @Test
    void rejectsInvalidSettings() {
        LineWrapper w = new LineWrapper();
        assertThatThrownBy(() -> w.setWrapLengthTolerance(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> w.setFirstLineWrapLength(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> w.setFollowingLineWrapLength(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propertiesAreAdjustable() {
        LineWrapper w = new LineWrapper();
        w.setBreakText("/");
        w.setFirstLineWrapLength(4);
        w.setFollowingLineWrapLength(4);
        w.setWrapLengthTolerance(0);
        assertThat(w.getBreakText()).isEqualTo("/");
        assertThat(w.getFirstLineWrapLength()).isEqualTo(4);
        assertThat(w.getFollowingLineWrapLength()).isEqualTo(4);
        assertThat(w.getWrapLengthTolerance()).isZero();
        assertThat(w.wrap("aaa bbb ccc")).isEqualTo("aaa /bbb /ccc");
    }
}
