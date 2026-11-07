package com.example.freeaudiotime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UndoRedoManagerTest {

    @Test
    public void undoRestoresPreviousStateAndPreservesElementIdentity() {
        RecapTemplate template = new RecapTemplate("template-1", "Teste", false);
        RecapElementModel element = new RecapElementModel();
        element.setCustomText("Texto original");
        template.getElements().add(element);
        String elementId = element.getId();

        UndoRedoManager manager = new UndoRedoManager();
        manager.pushState(template);

        element.setCustomText("Texto alterado");
        RecapTemplate restored = manager.undo(template);

        assertNotNull(restored);
        assertEquals("Texto original", restored.getElements().get(0).getCustomText());
        assertEquals(elementId, restored.getElements().get(0).getId());
        assertTrue(manager.canRedo());
    }

    @Test
    public void redoRestoresTheStateThatWasUndone() {
        RecapTemplate template = new RecapTemplate("template-1", "Teste", false);
        RecapElementModel element = new RecapElementModel();
        element.setCustomText("Antes");
        template.getElements().add(element);

        UndoRedoManager manager = new UndoRedoManager();
        manager.pushState(template);
        element.setCustomText("Depois");

        RecapTemplate restored = manager.undo(template);
        RecapTemplate redone = manager.redo(restored);

        assertNotNull(redone);
        assertEquals("Depois", redone.getElements().get(0).getCustomText());
        assertEquals(element.getId(), redone.getElements().get(0).getId());
        assertFalse(manager.canRedo());
    }

    @Test
    public void newEditAfterUndoClearsRedoHistory() {
        RecapTemplate template = new RecapTemplate("template-1", "Teste", false);
        RecapElementModel element = new RecapElementModel();
        element.setCustomText("Estado 1");
        template.getElements().add(element);

        UndoRedoManager manager = new UndoRedoManager();
        manager.pushState(template);
        element.setCustomText("Estado 2");

        RecapTemplate restored = manager.undo(template);
        assertTrue(manager.canRedo());

        manager.pushState(restored);
        restored.getElements().get(0).setCustomText("Estado 3");

        assertFalse(manager.canRedo());
    }
}
