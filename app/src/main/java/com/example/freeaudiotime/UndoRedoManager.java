package com.example.freeaudiotime;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Robust Undo/Redo manager for template editing actions.
 * Stores template snapshots as deep clones.
 */
public class UndoRedoManager {

    private static final int MAX_HISTORY = 30;

    private final Deque<RecapTemplate> undoStack = new ArrayDeque<>();
    private final Deque<RecapTemplate> redoStack = new ArrayDeque<>();

    public synchronized void pushState(RecapTemplate currentState) {
        if (currentState == null) return;
        undoStack.push(currentState.duplicate(currentState.getName()));
        redoStack.clear();
        if (undoStack.size() > MAX_HISTORY) {
            // Remove oldest
            RecapTemplate[] arr = undoStack.toArray(new RecapTemplate[0]);
            undoStack.clear();
            for (int i = arr.length - 2; i >= 0; i--) {
                undoStack.push(arr[i]);
            }
        }
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized RecapTemplate undo(RecapTemplate currentState) {
        if (!canUndo()) return null;
        if (currentState != null) {
            redoStack.push(currentState.duplicate(currentState.getName()));
        }
        return undoStack.pop();
    }

    public synchronized RecapTemplate redo(RecapTemplate currentState) {
        if (!canRedo()) return null;
        if (currentState != null) {
            undoStack.push(currentState.duplicate(currentState.getName()));
        }
        return redoStack.pop();
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
