package com.ravex.civilization.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LangTest {
    @Test void detectsLanguages() {
        assertEquals("ar", Lang.detect("ياسين تعال", "en"));
        assertEquals("fr", Lang.detect("récolte du bois s'il te plaît", "en"));
        assertEquals("en", Lang.detect("please collect wood", "fr"));
    }

    @Test void picksTranslation() {
        assertEquals("a", Lang.t("en", "a", "b", "c"));
        assertEquals("b", Lang.t("fr", "a", "b", "c"));
        assertEquals("c", Lang.t("ar", "a", "b", "c"));
    }
}
