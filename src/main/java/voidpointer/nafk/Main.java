/*
 * Copyright (C) 2021 Vasiliy Petukhov <void.pointer@ya.ru>
 */

package voidpointer.nafk;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

import java.awt.*;

public final class Main {

    public static void main(String[] args) {
        new Main();
    }

    private Main() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException nativeHookException) {
            nativeHookException.printStackTrace();
            System.exit(1);
        }

        PreventAfkTaskExecutor preventAfkTaskExecutor;
        try {
            preventAfkTaskExecutor = new PreventAfkTaskExecutor();
        } catch (AWTException awtException) {
            awtException.printStackTrace();
            System.exit(2);
            // fixes compilation issue with preventAfkTaskExecutor,
            // but doesn't really matter as we'll exit anyway
            return;
        }

        GlobalScreen.addNativeKeyListener(new KeyboardListener(preventAfkTaskExecutor));
        preventAfkTaskExecutor.start();
    }
}
