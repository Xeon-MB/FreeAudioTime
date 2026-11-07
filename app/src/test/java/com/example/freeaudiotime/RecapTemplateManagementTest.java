package com.example.freeaudiotime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RecapTemplateManagementTest {

    @Test
    public void testDefaultBrutalistPresetCreation() {
        RecapTemplate template = DefaultTemplatesFactory.createDefaultBrutalist();
        assertNotNull(template);
        assertEquals(RecapTemplate.DEFAULT_TEMPLATE_ID, template.getId());
        assertTrue(template.isSystemPreset());
        assertFalse(template.getElements().isEmpty());
    }

    @Test
    public void testTemplateDuplicationCreatesIndependentCopy() {
        RecapTemplate original = DefaultTemplatesFactory.createDefaultBrutalist();
        RecapTemplate duplicated = original.duplicate("Minha Cópia");

        assertNotNull(duplicated);
        assertNotEquals(original.getId(), duplicated.getId());
        assertEquals("Minha Cópia", duplicated.getName());
        assertFalse(duplicated.isSystemPreset());
        assertEquals(original.getElements().size(), duplicated.getElements().size());

        // Modifying duplicated element does not modify original
        RecapElementModel dupElem = duplicated.getElements().get(0);
        dupElem.setCustomText("Novo Texto Modificado");

        assertNotEquals(dupElem.getCustomText(), original.getElements().get(0).getCustomText());
    }

    @Test
    public void testTemplateSerialization() throws Exception {
        RecapTemplate original = DefaultTemplatesFactory.createDefaultBrutalist();
        String jsonString = original.toJsonObject().toString();

        RecapTemplate restored = RecapTemplate.fromJsonObject(new org.json.JSONObject(jsonString));
        assertNotNull(restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getElements().size(), restored.getElements().size());
    }

    @Test
    public void testCategoryDefaultMapping() {
        assertEquals("dia", RecapCategory.DAILY.getId());
        assertEquals("semana", RecapCategory.WEEKLY.getId());
        assertEquals("mes", RecapCategory.MONTHLY.getId());
        assertEquals("ano", RecapCategory.YEARLY.getId());

        assertEquals(RecapCategory.DAILY, RecapCategory.fromPeriodInt(3));
        assertEquals(RecapCategory.MONTHLY, RecapCategory.fromPeriodInt(1));
        assertEquals(RecapCategory.YEARLY, RecapCategory.fromPeriodInt(2));
        assertEquals(RecapCategory.WEEKLY, RecapCategory.fromPeriodInt(0));
    }

    @Test
    public void testHideableOrReplaceableMissingData() {
        RecapElementModel elem = new RecapElementModel(RecapElementBinding.ACHIEVEMENTS_LIST, 0f, 0f, 1f, 0.1f);
        elem.setHideIfMissing(true);
        elem.setFallbackReplacementText("Nenhuma conquista");

        assertTrue(elem.isHideIfMissing());
        assertEquals("Nenhuma conquista", elem.getFallbackReplacementText());
    }
}
