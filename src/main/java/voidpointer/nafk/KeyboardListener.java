/*
 * Copyright (C) 2021 Vasiliy Petukhov <void.pointer@ya.ru>
 */

package voidpointer.nafk;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KeyboardListener implements NativeKeyListener {
    private final List<Integer> quitHotkey;
    private final PreventAfkTaskExecutor preventAfkTask;

    private final LinkedList<Integer> keysPressed = new LinkedList<>();

    public KeyboardListener(final PreventAfkTaskExecutor preventAfkTaskExecutor) {
        this.preventAfkTask = preventAfkTaskExecutor;

        LinkedList<Integer> quitHotkey = new LinkedList<>();
        quitHotkey.add(NativeKeyEvent.VC_ALT);
        quitHotkey.add(NativeKeyEvent.VC_PAUSE);
        this.quitHotkey = Collections.unmodifiableList(quitHotkey);
    }

    public void nativeKeyPressed(final NativeKeyEvent keyEvent) {
        keysPressed.add(keyEvent.getKeyCode());
        quitIfHotkeyIsMet();
    }

    @Override public void nativeKeyReleased(final NativeKeyEvent keyEvent) {
        // if not found -> it was cleared in quitIfHotkeyIsMet -> program is quitting
        if (keysPressed.removeFirstOccurrence(keyEvent.getKeyCode())) {
            quitIfHotkeyIsMet();
        }
    }

    private void quitIfHotkeyIsMet() {
        if (keysPressed.size() != quitHotkey.size())
            return;
        if (!keysPressed.containsAll(quitHotkey))
            return;

        keysPressed.clear();
        System.out.println("The quit hotkey VC_ALT+VC_PAUSE was pressed, exiting.");
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException nativeHookException) {
            nativeHookException.printStackTrace();
        }
        preventAfkTask.stop();
    }
}
