package org.strangeeons.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MasterPlanTest {

    private static final Rectangle ACT = new Rectangle(1, 2, 3, 4);
    private static final Rectangle REVERSED = new Rectangle(5, 6, 7, 8);

    private static MasterPlan backsShareFronts() {
        Map<String, Rectangle> masks = new LinkedHashMap<>();
        masks.put("components/act/reversed-1.png", REVERSED);
        masks.put("components/act/*", ACT);
        return new MasterPlan("-1.png", "-0.png", List.of("**/filled-1.png", "components/key/**"), masks);
    }

    @Test
    void samePathByDefault() {
        assertEquals("a/b-1.png", MasterPlan.SAME_PATH.masterFor("a/b-1.png"));
        assertFalse(MasterPlan.SAME_PATH.isShared("a/b-1.png"));
        assertNull(MasterPlan.SAME_PATH.maskFor("a/b-1.png"));
    }

    @Test
    void sharedRendersUseTheOtherMaster() {
        MasterPlan p = backsShareFronts();
        assertTrue(p.isShared("components/act/default-1.png"));
        assertEquals("components/act/default-0.png", p.masterFor("components/act/default-1.png"));
        assertEquals("components/act/default-0.png", p.masterFor("components/act/default-0.png"));
    }

    @Test
    void excludedRendersKeepTheirOwn() {
        MasterPlan p = backsShareFronts();
        assertEquals("components/act/filled-1.png", p.masterFor("components/act/filled-1.png"));
        assertEquals("components/key/default-1.png", p.masterFor("components/key/default-1.png"));
        assertNull(p.maskFor("components/act/filled-1.png"));
    }

    @Test
    void firstMatchingMaskApplies() {
        MasterPlan p = backsShareFronts();
        assertEquals(REVERSED, p.maskFor("components/act/reversed-1.png"));
        assertEquals(ACT, p.maskFor("components/act/default-1.png"));
        assertNull(p.maskFor("components/enemy/default-1.png"));
        assertNull(p.maskFor("components/act/default-0.png"));
    }

    @Test
    void needsBothSuffixes() {
        assertThrows(IllegalArgumentException.class, () -> new MasterPlan("-1.png", null, List.of(), Map.of()));
    }
}
